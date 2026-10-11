#!/usr/bin/env python3
"""Hosted-only XCTest receipt compatibility check. Public fixtures; no app/account/network."""
import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('acceptance', HERE / 'acceptance.py')
acceptance = importlib.util.module_from_spec(spec)
spec.loader.exec_module(acceptance)

PACKAGE = '''// swift-tools-version: 6.0
import PackageDescription
let package = Package(name: "ReceiptFixture", platforms: [.macOS(.v14)],
    products: [.library(name: "ReceiptFixture", targets: ["ReceiptFixture"])],
    targets: [.target(name: "ReceiptFixture"),
              .testTarget(name: "ReceiptFixtureTests", dependencies: ["ReceiptFixture"])])
'''
TESTS = '''import XCTest
@testable import ReceiptFixture
final class StagingAcceptanceUITests: XCTestCase {
    func testCoreChatInboxAcceptance() { XCTAssertEqual(fixtureValue, 42) }
    func testIdentityMatcherIsCaseAndSeparatorInsensitive() { XCTAssertTrue(true) }
}
final class StagingAcceptanceCleanupUITests: XCTestCase {
    func testSignOutIfSignedIn() { XCTAssertTrue(true) }
}
final class SkippedFixtureTests: XCTestCase {
    func testSkipped() throws { throw XCTSkip("Intentional public fixture") }
}
'''

def main():
    assert os.environ.get('CI') == 'true', 'Hosted CI fixture only; do not build locally'
    assert os.environ.get('RUNNER_OS') == 'macOS'
    # Dedicated no-secret job. Do not reuse in an environment-protected acceptance job.
    assert not any(k.startswith('TEST_RUNNER_') or k in ('REMCLAW_DEPLOY_KEY', 'RUNTIME_ATTESTATION') for k in os.environ)
    version = subprocess.check_output(['xcodebuild', '-version'], text=True)
    assert version.splitlines()[0] == 'Xcode 26.3', version
    assert shutil.disk_usage(os.environ['RUNNER_TEMP']).free >= 6 * 1024**3
    print(version.strip())
    with tempfile.TemporaryDirectory(prefix='rem-public-xcresult-fixture-', dir=os.environ['RUNNER_TEMP']) as temp:
        folder = Path(temp)
        (folder / 'Package.swift').write_text(PACKAGE)
        (folder / 'Sources/ReceiptFixture').mkdir(parents=True)
        (folder / 'Sources/ReceiptFixture/Fixture.swift').write_text('public let fixtureValue = 42\n')
        (folder / 'Tests/ReceiptFixtureTests').mkdir(parents=True)
        (folder / 'Tests/ReceiptFixtureTests/ReceiptTests.swift').write_text(TESTS)
        def run(name, selected):
            bundle = folder / (name + '.xcresult')
            command = ['xcodebuild', 'test', '-scheme', 'ReceiptFixture', '-destination', 'platform=macOS',
                       '-derivedDataPath', str(folder/'derived'), '-resultBundlePath', str(bundle),
                       '-parallel-testing-enabled', 'NO', 'CODE_SIGNING_ALLOWED=NO']
            command.extend('-only-testing:ReceiptFixtureTests/' + value for value in selected)
            with (folder/(name+'.log')).open('wb') as log:
                result = subprocess.run(command, cwd=folder, stdout=log, stderr=subprocess.STDOUT)
            if result.returncode:
                # This file contains only public synthetic package build/test diagnostics.
                print((folder/(name+'.log')).read_text()[-12000:])
            assert result.returncode == 0, 'Public fixture failed to execute'
            return acceptance.read_test_records(bundle)
        passed = run('passing', ['StagingAcceptanceUITests', 'StagingAcceptanceCleanupUITests'])
        expected = ['StagingAcceptanceUITests/testCoreChatInboxAcceptance()',
                    'StagingAcceptanceUITests/testIdentityMatcherIsCaseAndSeparatorInsensitive()',
                    'StagingAcceptanceCleanupUITests/testSignOutIfSignedIn()']
        acceptance.validate_test_records(passed, expected)
        skipped = run('skipped', ['SkippedFixtureTests'])
        try:
            acceptance.validate_test_records(skipped, ['SkippedFixtureTests/testSkipped()'])
        except acceptance.Refused:
            print('Xcode26.3 actual passing methods accepted; exit-zero skipped fixture refused.')
        else:
            raise AssertionError('Skipped execution incorrectly accepted')
    print('Fixture only: no signed-in app or staging coverage.')

if __name__ == '__main__': main()
