#!/usr/bin/env python3
"""Acceptance-only proposal. No deployment, signing, upload, provider configuration or artifacts.
Public output is fixed vocabulary only. Private source and tool output remain ephemeral on runner.
"""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import subprocess
import sys

SHA = '1277d8132678bfa1a279b6da71f3e491c1cce35d'
BACKEND_TREE = 'f3c00642da42a6b0a534867612bbc8716811dbde'
TEST_TREE = 'c166a7fb05a17adfa6f1dd41fb650712f36ba3e6'
ENVIRONMENT = 'remclaw-staging-acceptance'
HOST = 'https://backend-staging-b87e.up.railway.app'
PROJECT = 'f88617f9-a5eb-4f4a-bdb9-0f10ab427e9a'
RAILWAY_ENV = '4b54fc6f-1198-46dd-8fc6-7b23d4365056'
SERVICE = 'bf0e5c69-e609-491c-ab60-16362f49c285'
MODES = ('build-only', 'chat-inbox')
UUID = re.compile(r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')
HERE = Path(__file__).resolve().parent

class Refused(Exception):
    pass

def require(condition):
    if not condition:
        raise Refused()

def validate_inputs(sha, mode, calls):
    require(sha == SHA and mode in MODES)
    require(calls == ('0' if mode == 'build-only' else '3'))

def validate_attestation(raw, now=None):
    """Validate OWNER-SUPPLIED metadata, not a live Railway query. Final attribution still pending."""
    data = json.loads(raw)
    require(type(data) is dict and set(data) == {
        'sha', 'project_id', 'environment_id', 'service_id', 'deployment_id', 'checked_at', 'provider_calls'})
    require(data['sha'] == SHA and data['project_id'] == PROJECT
            and data['environment_id'] == RAILWAY_ENV and data['service_id'] == SERVICE)
    require(type(data['deployment_id']) is str and UUID.fullmatch(data['deployment_id']))
    require(type(data['provider_calls']) is int and data['provider_calls'] == 3)
    checked = datetime.fromisoformat(data['checked_at'].replace('Z', '+00:00'))
    require(checked.tzinfo is not None)
    age = ((now or datetime.now(timezone.utc)) - checked).total_seconds()
    require(0 <= age <= 3600)
    return data['deployment_id']

def safe_summary(sha, mode, state):
    require(sha == SHA and mode in MODES)
    require(state in ('not_started', 'build_passed', 'test_passed_pending_runtime_readback', 'failed'))
    return f'candidate={SHA}\nmode={mode}\nresult={state}\nruntime_attribution=operator_readback_required\n'

def load_release():
    # Reuse the established public controller's quiet subprocess and pinned-host SSH mechanisms.
    root = HERE.parent / 'remclaw-release'
    sys.path.insert(0, str(root))
    import release
    return release

def check_gate():
    release = load_release()
    release.run_identity(os.environ)
    base = f'repos/{release.gate.REPO}/environments/{ENVIRONMENT}'
    environment = release.gate.gh(base)
    policies = [{'name': p['name'], 'type': p['type']}
                for p in release.gate.paged(base + '/deployment-branch-policies', 'branch_policies')]
    receipt = release.gate.validate_environment(environment, policies,
        (os.environ.get('GITHUB_ACTOR'), os.environ.get('GITHUB_TRIGGERING_ACTOR')))
    require(receipt['reviewer'] == 'samuelalake')
    return receipt

def check_control():
    control = HERE.parents[1]
    actual = quiet(['git', 'rev-parse', 'HEAD'], 'control', cwd=control, capture=True)
    require(actual.decode().strip() == os.environ['GITHUB_SHA'])
    changed = quiet(['git', 'status', '--porcelain', '--untracked-files=all'],
                    'control', cwd=control, capture=True)
    require(not changed)

def root():
    base = Path(os.environ['RUNNER_TEMP']).resolve()
    path = base / 'remclaw-acceptance-private'
    require(not path.is_symlink())
    path.mkdir(mode=0o700, exist_ok=True)
    os.chmod(path, 0o700)
    return path

def quiet(args, name, *, cwd=None, env=None, capture=False):
    """Never inherit public stdout/stderr, including failure cases. No exception text escapes."""
    log = root() / (name + '.log')
    with log.open('ab') as output:
        result = subprocess.run(args, cwd=cwd, env=env, stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE if capture else output, stderr=output)
    require(result.returncode == 0)
    return result.stdout if capture else None

def fetch():
    release = load_release()
    directory = root()
    source = directory / 'source'
    require(not source.exists())
    key = directory / 'deploy-key'
    known = directory / 'known-hosts'
    secret = os.environ.get('REMCLAW_DEPLOY_KEY', '')
    require(bool(secret.strip()))
    try:
        key.touch(mode=0o600, exist_ok=False)
        key.write_text(secret.strip() + '\n')
        known.write_text(release.GITHUB_HOST_KEY + '\n')
        env = release.git_env(key, known)
        quiet(['git', 'init', '-q', str(source)], 'fetch', env=env)
        quiet(['git', '-C', str(source), 'remote', 'add', 'origin', release.REMCLAW_SSH_URL], 'fetch', env=env)
        # Acceptance pin is separate from release ancestry policy. No branch or PR input is accepted.
        quiet(['git', '-C', str(source), 'fetch', '--quiet', '--no-tags', '--depth=1', 'origin', SHA], 'fetch', env=env)
        quiet(['git', '-C', str(source), 'checkout', '--quiet', '--detach', SHA], 'fetch', env=env)
        for ref, expected in [('HEAD', SHA), ('HEAD:backend', BACKEND_TREE), ('HEAD:RemClawUITests', TEST_TREE)]:
            actual = quiet(['git', '-C', str(source), 'rev-parse', ref], 'fetch', env=env, capture=True)
            require(actual.decode().strip() == expected)
    finally:
        for path in (key, known):
            path.unlink(missing_ok=True)

def build():
    directory = root()
    source = directory / 'source'
    require(not (source / 'RemClaw/Debug.local.xcconfig').exists())
    phase = source / 'scripts/update-local-ip.sh'
    if phase.exists():
        phase.chmod(phase.stat().st_mode & ~0o111)
    require(shutil.disk_usage(directory).free >= 18 * 1024**3)
    quiet(['xcodebuild', '-project', 'RemClaw.xcodeproj', '-scheme', 'RemClawStagingAcceptance',
        '-configuration', 'Debug', '-sdk', 'iphonesimulator', '-destination', 'generic/platform=iOS Simulator',
        '-derivedDataPath', str(directory / 'DerivedData'), '-clonedSourcePackagesDirPath', str(directory / 'packages'),
        '-onlyUsePackageVersionsFromResolvedFile', '-skipMacroValidation', 'ARCHS=arm64',
        'ONLY_ACTIVE_ARCH=YES', 'CODE_SIGNING_ALLOWED=NO', 'build-for-testing'], 'build', cwd=source)
    changed = quiet(['git', '-c', 'core.filemode=false', 'status', '--porcelain', '--untracked-files=no'],
                    'provenance', cwd=source, capture=True)
    require(not changed)
    products = directory / 'DerivedData/Build/Products'
    plist = plistlib.loads((products / 'Debug-iphonesimulator/RemClaw.app/Info.plist').read_bytes())
    require(plist.get('CFBundleIdentifier') == 'com.remapp.rem' and plist.get('APIBaseURL') == HOST)
    require((products / 'Debug-iphonesimulator/RemClawUITests-Runner.app').is_dir())
    require(len(list(products.glob('*.xctestrun'))) == 1)
    (directory / 'result').write_text('build_passed')

def test_command(udid, cleanup=False):
    directory = root()
    require(UUID.fullmatch(udid.lower()))
    return ['xcodebuild', 'test-without-building', '-project', 'RemClaw.xcodeproj',
        '-scheme', 'RemClawStagingAcceptance', '-destination', 'id=' + udid,
        '-derivedDataPath', str(directory / 'DerivedData'), '-clonedSourcePackagesDirPath', str(directory / 'packages'),
        '-resultBundlePath', str(directory / ('cleanup.xcresult' if cleanup else 'acceptance.xcresult')),
        '-collect-test-diagnostics', 'never',
        '-only-testing:RemClawUITests/' + ('StagingAcceptanceCleanupUITests' if cleanup else 'StagingAcceptanceUITests')]

def validate_test_records(data, expected):
    records = []
    def walk(node):
        if isinstance(node, dict):
            if node.get('_type', {}).get('_name') == 'ActionTestMetadata':
                records.append((node.get('identifier', {}).get('_value'), node.get('testStatus', {}).get('_value')))
            for value in node.values(): walk(value)
        elif isinstance(node, list):
            for value in node: walk(value)
    walk(data)
    require(len(records) == len(expected))
    require({name for name, _ in records} == set(expected))
    require(all(status == 'Success' for _, status in records))

def read_test_records(bundle):
    def read_object(ref=None):
        command = ['xcrun', 'xcresulttool', 'get', 'object', '--legacy', '--format', 'json', '--path', str(bundle)]
        if ref is not None: command += ['--id', ref]
        return json.loads(quiet(command, 'execution-receipt', capture=True))
    top = read_object()
    actions = top.get('actions', {}).get('_values', [])
    refs = [x.get('actionResult', {}).get('testsRef', {}).get('id', {}).get('_value') for x in actions]
    refs = [x for x in refs if isinstance(x, str) and x]
    require(len(refs) == 1)
    return read_object(refs[0])

def verify_executed_tests(cleanup=False):
    bundle = root() / ('cleanup.xcresult' if cleanup else 'acceptance.xcresult')
    expected = ['StagingAcceptanceCleanupUITests/testSignOutIfSignedIn()'] if cleanup else [
        'StagingAcceptanceUITests/testCoreChatInboxAcceptance()',
        'StagingAcceptanceUITests/testIdentityMatcherIsCaseAndSeparatorInsensitive()']
    validate_test_records(read_test_records(bundle), expected)

def run_tests():
    check_control()
    validate_attestation(os.environ.get('RUNTIME_ATTESTATION', ''))
    account = os.environ.get('TEST_RUNNER_REM_ACCEPTANCE_TEST_ACCOUNT_ID', '')
    secret = os.environ.get('TEST_RUNNER_REM_ACCEPTANCE_TEST_LOGIN_SECRET', '')
    require(bool(account and secret))
    directory = root()
    source = directory / 'source'
    # No key, account id, typed text, stdout or exception details are ever printed.
    runtime_file = directory / 'runtimes.json'
    runtime_file.write_bytes(quiet(['xcrun', 'simctl', 'list', '-j', 'runtimes'], 'simulator', capture=True))
    picked = quiet(['python3', '-I', '-B', str(source / 'scripts/staging-acceptance/pick_simulator.py'), str(runtime_file)],
                   'simulator', capture=True).decode().strip().split()
    require(len(picked) == 2 and all(re.fullmatch(r'[A-Za-z0-9.-]+', x) for x in picked))
    udid = quiet(['xcrun', 'simctl', 'create', 'rem-acceptance-' + os.environ['GITHUB_RUN_ID'], picked[1], picked[0]],
                 'simulator', capture=True).decode().strip()
    require(UUID.fullmatch(udid.lower()))
    (directory / 'simulator').write_text(udid)
    quiet(['xcrun', 'simctl', 'boot', udid], 'simulator')
    quiet(['xcrun', 'simctl', 'bootstatus', udid, '-b'], 'simulator')
    quiet(test_command(udid), 'test', cwd=source)
    verify_executed_tests()
    # Exit zero alone can mean skipped/empty tests. Require every expected harness step.
    # Export remains private and ephemeral, including already-redacted screenshots.
    attachments = directory / 'attachments'
    quiet(['xcrun', 'xcresulttool', 'export', 'attachments', '--path',
           str(directory / 'acceptance.xcresult'), '--output-path', str(attachments)], 'evidence')
    quiet(['python3', '-I', '-B', str(source / 'scripts/staging-acceptance/export_evidence.py'),
           str(attachments), str(directory / 'evidence'), str(directory / 'summary.md'), 'not_run'], 'evidence')
    (directory / 'result').write_text('test_passed_pending_runtime_readback')

def cleanup_session():
    directory = root()
    path = directory / 'simulator'
    if not path.exists():
        return
    udid = path.read_text()
    # No credentials passed to cleanup; the app holds its own short-lived delegate in Keychain.
    env = {k: v for k, v in os.environ.items() if not k.startswith('TEST_RUNNER_') and k != 'RUNTIME_ATTESTATION'}
    quiet(test_command(udid, cleanup=True), 'cleanup', cwd=directory / 'source', env=env)
    verify_executed_tests(cleanup=True)

def cleanup_files():
    directory = root()
    path = directory / 'simulator'
    state_path = directory / 'result'
    state = state_path.read_text() if state_path.exists() else 'not_started'
    safe_summary(SHA, os.environ['ACCEPTANCE_MODE'], state)
    try:
        if path.exists():
            udid = path.read_text()
            require(UUID.fullmatch(udid.lower()))
            # Only this run's created simulator. A shutdown refusal must not skip delete.
            try:
                quiet(['xcrun', 'simctl', 'shutdown', udid], 'simulator')
            except Refused:
                pass
            quiet(['xcrun', 'simctl', 'delete', udid], 'simulator')
    finally:
        # Fixed child of RUNNER_TEMP; no input path or public artifact/cache destination.
        shutil.rmtree(directory)
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write('state=' + state + '\n')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('stage', choices=['admit', 'fetch', 'build', 'test', 'cleanup-session', 'summary', 'cleanup-files'])
    args = parser.parse_args()
    sha, mode, calls = (os.environ.get(k, '') for k in ('REMCLAW_SHA', 'ACCEPTANCE_MODE', 'APPROVED_PROVIDER_CALLS'))
    validate_inputs(sha, mode, calls)
    if args.stage == 'admit':
        check_gate()
        if mode == 'chat-inbox': validate_attestation(os.environ.get('RUNTIME_ATTESTATION', ''))
    elif args.stage == 'fetch': fetch()
    elif args.stage == 'build':
        check_control()
        build()
        check_control()
    elif args.stage == 'test':
        require(mode == 'chat-inbox')
        run_tests()
    elif args.stage == 'cleanup-session': cleanup_session()
    elif args.stage == 'cleanup-files': cleanup_files()
    elif args.stage == 'summary':
        state = 'failed' if os.environ.get('JOB_STATUS') != 'success' else os.environ.get('RESULT_STATE', 'not_started')
        text = safe_summary(sha, mode, state)
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as output: output.write(text)
    print('acceptance_control=passed')

if __name__ == '__main__':
    try:
        main()
    except Exception:
        print('acceptance_control=refused', file=sys.stderr)
        sys.exit(1)
