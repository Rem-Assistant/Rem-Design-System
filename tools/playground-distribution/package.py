#!/usr/bin/env python3
"""Trusted packaging/inspection. Never publishes files or prints credential material."""
import argparse
import base64
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import secrets
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile
from gate import require, arguments
import storage

IOS_ID = 'com.rem.playground.settings'
ANDROID_ID = 'com.rem.designsystem.demo'
TEAM = 'R6A892D599'
UPLOAD_SHA256 = '658de1d6931622c554c564ba25ad1594c63516dc68cd02d860ca85aa0e2b8a4e'
# App Store Connect requires Xcode 26 / the iOS 26 SDK; 26.1+ also has Apple's fix for fastlane#29698
# (new altool picking the wrong app). macos-15 defaults to Xcode 16.4, so select the exact pinned bundle
# (runner-images toolset-15.json installs /Applications/Xcode_26.3.app, 26.3+17C529). Mirrors
# tools/remclaw-release/release.py select_xcode, pinned to one version instead of the newest.
XCODE_APP = 'Xcode_26.3.app'
XCODE_MIN = (26, 1)
SDK_MIN_MAJOR = 26
XCODE_SHAPE = r'\d{2}\.\d{1,2}(?:\.\d{1,2})?'
XCODE_BUILD_SHAPE = r'\d{2}[A-Z]\d{1,5}[a-z]?'
SDK_SHAPE = r'iphoneos\d{2}\.\d{1,2}(?:\.\d{1,2})?'


def command(argv, *, cwd=None, env=None):
    r = subprocess.run(argv, cwd=cwd, env=env, capture_output=True)
    require(r.returncode == 0, 'Packaging command failed: ' + Path(argv[0]).name + ' (output suppressed)')
    return r.stdout


def ios_metadata(info, sha, build):
    require(info.get('CFBundleIdentifier') == IOS_ID, 'iOS bundle ID mismatch')
    require(info.get('RemPlaygroundSourceSHA') == sha, 'iOS source stamp mismatch')
    require(info.get('CFBundleVersion') == str(build), 'iOS build number mismatch')
    require(info.get('CFBundleDisplayName') == 'Rem Playground' and
            info.get('CFBundleName') == 'Rem Playground', 'Commit approved iOS Playground branding before distribution')
    require(info.get('CFBundleShortVersionString') == '0.1.0', 'Unexpected iOS marketing version')
    require(info.get('CFBundleIcons') or info.get('CFBundleIconName'), 'Commit approved iOS app icon before distribution')
    require(info.get('UISupportedInterfaceOrientations') and info.get('UISupportedInterfaceOrientations~ipad'),
            'Commit the approved iPhone/iPad orientations before distribution')
    require('ITSAppUsesNonExemptEncryption' in info, 'Commit verified export-compliance declaration before distribution')


def android_metadata(xml, sha, build):
    root = ET.fromstring(xml)
    a = '{http://schemas.android.com/apk/res/android}'
    require(root.get('package') == ANDROID_ID, 'Android package ID mismatch')
    require(root.get(a + 'versionCode') == str(build), 'Android version code mismatch')
    require(root.get(a + 'versionName') == '1.0', 'Unexpected Android marketing version')
    sdk = root.find('uses-sdk')
    require(sdk is not None and sdk.get(a + 'targetSdkVersion') == '36' and sdk.get(a + 'minSdkVersion') == '24',
            'Commit and natively test the established API36/min24 store configuration first')
    app = root.find('application')
    require(app is not None and app.get(a + 'debuggable', 'false') == 'false', 'Debuggable bundle rejected')
    require(app.get(a + 'label') == 'Rem Playground' and app.get(a + 'icon'), 'Commit approved Android label/icon first')
    stamps = [x.get(a + 'value') for x in app.findall('meta-data') if x.get(a + 'name') == 'RemPlaygroundSourceSHA']
    require(stamps == ['git:' + sha], 'Android source stamp mismatch')


def validate_profile(profile, certificate_sha1):
    e = profile.get('Entitlements', {})
    require(profile.get('TeamIdentifier') == [TEAM] and e.get('application-identifier') == TEAM + '.' + IOS_ID,
            'Wrong provisioning team or app')
    require(e.get('get-task-allow') is False and not profile.get('ProvisionedDevices') and
            not profile.get('ProvisionsAllDevices'), 'Require an existing App Store distribution profile')
    require(profile['ExpirationDate'].replace(tzinfo=timezone.utc) > datetime.now(timezone.utc), 'Profile expired')
    require(certificate_sha1.lower() in [hashlib.sha1(c).hexdigest() for c in profile['DeveloperCertificates']],
            'Profile does not authorize configured distribution certificate')
    require(re.fullmatch(r'[A-Fa-f0-9-]{36}', profile.get('UUID', '')), 'Invalid profile UUID')


def xcode_version(name):
    m = re.fullmatch(r'Xcode_(\d+)\.(\d+)(?:\.(\d+))?\.app', name)
    return tuple(int(x or 0) for x in m.groups()) if m else None


def select_xcode(applications):
    """The exact pinned Xcode bundle, failing closed when this runner image does not have it."""
    version = xcode_version(XCODE_APP)
    require(version and version[:2] >= XCODE_MIN, 'Pinned Xcode is below the supported minimum')
    developer = Path(applications) / XCODE_APP / 'Contents/Developer'
    require(developer.is_dir(), 'Pinned ' + XCODE_APP + ' is not installed on this runner image')
    return developer


def xcode_toolchain(developer):
    """Version, build and iOS SDK of the selected Xcode, in fixed shapes only."""
    env = dict(os.environ, DEVELOPER_DIR=str(developer))
    text = command(['xcodebuild', '-version'], env=env).decode()
    m = re.fullmatch(r'Xcode (' + XCODE_SHAPE + r')\nBuild version (' + XCODE_BUILD_SHAPE + r')\n?', text)
    require(m, 'Unexpected xcodebuild -version output')
    require(m[1] == XCODE_APP[len('Xcode_'):-len('.app')], 'Selected Xcode is not the pinned version')
    sdk = command(['xcodebuild', '-version', '-sdk', 'iphoneos', 'SDKVersion'], env=env).decode().strip()
    require(re.fullmatch(SDK_SHAPE[len('iphoneos'):], sdk) and int(sdk.split('.')[0]) >= SDK_MIN_MAJOR,
            'Selected Xcode lacks the iOS 26 SDK')
    return {'xcode': m[1], 'xcode_build': m[2], 'sdk': 'iphoneos' + sdk, 'developer_dir': str(developer)}


def archived_toolchain(info, toolchain):
    """The archive was built by the selected toolchain, with Xcode 26+ and an iOS 26+ SDK."""
    dt_xcode, dt_platform = str(info.get('DTXcode', '')), str(info.get('DTPlatformVersion', ''))
    require(re.fullmatch(r'\d{4}', dt_xcode) and re.fullmatch(r'\d{2}\.\d{1,2}(?:\.\d{1,2})?', dt_platform),
            'Archive lacks Xcode/SDK build metadata')
    require(int(dt_xcode) >= SDK_MIN_MAJOR * 100 and int(dt_platform.split('.')[0]) >= SDK_MIN_MAJOR,
            'Archive built with an Xcode/SDK older than 26')
    require(info.get('DTXcodeBuild') == toolchain['xcode_build'] and info.get('DTSDKName') == toolchain['sdk'],
            'Archive was not built by the selected Xcode')


def ephemeral_root(root):
    # Dependency, DerivedData and Gradle state live under the runner's temporary directory, outside the
    # candidate checkout and the package output, and are removed by the workflow's cleanup step.
    path = Path(os.environ.get('PLAYGROUND_EPHEMERAL', '')).resolve()
    require(os.environ.get('PLAYGROUND_EPHEMERAL') and path.is_dir(), 'Missing ephemeral build directory')
    # Compare resolved paths: macOS temporary directories sit behind the /var -> /private/var symlink.
    root = Path(root).resolve()
    require(root != path and root not in path.parents, 'Ephemeral build directory must be outside the candidate')
    return path


def build(root, out, platform, sha, number):
    require(not out.exists(), 'Refuse to overwrite build output')
    storage.require_free(out.parent, platform, 'build')
    scratch = ephemeral_root(root)
    out.mkdir(mode=0o700)
    if platform == 'ios':
        project = root / 'tools/playground-ios/RemSettingsPlayground.xcodeproj'
        require(project.is_dir(), 'Expected committed Playground Xcode project is missing')
        developer = Path(os.environ.get('DEVELOPER_DIR', ''))
        require(os.environ.get('DEVELOPER_DIR') and developer == select_xcode(developer.parents[2]),
                'Select the pinned Xcode (package.py xcode) before building')
        xcode = xcode_toolchain(developer)
        # Only build number and source stamp vary; no target SDK/resources/dependency patches.
        command(['xcodebuild', '-project', str(project), '-scheme', 'RemSettingsPlayground',
                 '-configuration', 'Release', '-destination', 'generic/platform=iOS',
                 '-archivePath', str(out / 'Playground.xcarchive'), '-derivedDataPath', str(scratch / 'DerivedData'),
                 '-clonedSourcePackagesDirPath', str(scratch / 'SourcePackages'),
                 'CODE_SIGNING_ALLOWED=NO', 'CURRENT_PROJECT_VERSION=' + number,
                 'REM_PLAYGROUND_SOURCE_SHA=' + sha, 'archive'])
        apps = list((out / 'Playground.xcarchive/Products/Applications').glob('*.app'))
        require(len(apps) == 1, 'Expected one archived application')
        info = plistlib.loads((apps[0] / 'Info.plist').read_bytes())
        ios_metadata(info, sha, number)
        archived_toolchain(info, xcode)
        toolchain = command(['xcodebuild', '-version']).decode().strip()
        storage.remove_within(scratch, scratch / 'DerivedData')  # redundant once the archive is verified
    else:
        wrapper = root / 'compose/gradle/wrapper/gradle-wrapper.properties'
        require('gradle-8.14.4-bin.zip' in wrapper.read_text(), 'Unexpected Gradle wrapper')
        init = Path(__file__).with_name('version.init.gradle').resolve()
        gradle = dict(os.environ, GRADLE_USER_HOME=str(scratch / 'gradle-home'))
        command(['./gradlew', '--no-daemon', '--no-configuration-cache', '-I', str(init),
                 ':demo:bundleRelease', '-PplaygroundSourceSha=' + sha], cwd=root / 'compose', env=gradle)
        binary = root / 'compose/demo/build/outputs/bundle/release/demo-release.aab'
        require(binary.is_file(), 'Release AAB missing')
        shutil.copyfile(binary, out / 'Playground.aab')
        gradle['REM_VERIFY_AAB'] = str(out / 'Playground.aab')
        command(['./gradlew', '--no-daemon', '-I', str(init), 'verifyPlaygroundBundle'], cwd=root / 'compose', env=gradle)
        dumped = command(['./gradlew', '--no-daemon', '-q', '-I', str(init), 'inspectPlaygroundBundle'],
                         cwd=root / 'compose', env=gradle).decode()
        match = re.search(r'<manifest\b[\s\S]*?</manifest>', dumped)
        require(match, 'No embedded bundle manifest')
        android_metadata(match[0], sha, number)
        toolchain = command(['./gradlew', '--version'], cwd=root / 'compose', env=gradle).decode().strip()
        storage.remove_ignored_build_output(root, 'compose/demo/build')  # the AAB was copied and verified
    require(not command(['git', '-C', str(root), 'diff', '--name-only']), 'Build changed tracked source')
    record = {'source_sha': sha, 'platform': platform,
        'build_number': number, 'toolchain': toolchain, 'overrides': ['build_number', 'source_stamp'],
        'signing': 'pending', 'physical_device': 'unverified'}
    if platform == 'ios':
        record['xcode'] = xcode
    (out / 'packaging.json').write_text(json.dumps(record, indent=2))
    storage.require_budget(out, platform, 'build')


def material(name, path):
    value = os.environ.get(name, '')
    require(value, 'Missing protected-environment secret: ' + name)
    path.write_bytes(base64.b64decode(value, validate=True))
    path.chmod(0o600)


def sign(out, platform, sha, number):
    storage.require_free(out, platform, 'sign')
    if platform == 'android':
        with tempfile.TemporaryDirectory(dir=out) as tmp:
            key = Path(tmp) / 'upload.p12'
            material('PLAYGROUND_ANDROID_KEYSTORE_BASE64', key)
            require(os.environ.get('PLAYGROUND_ANDROID_KEYSTORE_PASSWORD'), 'Missing existing upload-key password')
            cert = command(['keytool', '-exportcert', '-keystore', str(key), '-alias', 'playground-upload',
                            '-storepass:env', 'PLAYGROUND_ANDROID_KEYSTORE_PASSWORD'])
            require(hashlib.sha256(cert).hexdigest() == UPLOAD_SHA256, 'Existing Android upload identity mismatch')
            binary = out / 'Playground.aab'
            command(['jarsigner', '-keystore', str(key), '-storepass:env', 'PLAYGROUND_ANDROID_KEYSTORE_PASSWORD',
                     '-keypass:env', 'PLAYGROUND_ANDROID_KEYSTORE_PASSWORD', str(binary), 'playground-upload'])
            verified = command(['jarsigner', '-verify', str(binary)]).decode()
            require('jar verified.' in verified and 'unsigned entries' not in verified, 'AAB signature verification failed')
    else:
        require(re.fullmatch(r'[A-Fa-f0-9]{40}', os.environ.get('PLAYGROUND_IOS_CERTIFICATE_SHA1', '')),
                'Missing configured existing distribution certificate fingerprint')
        cert_sha = os.environ['PLAYGROUND_IOS_CERTIFICATE_SHA1']
        require(os.environ.get('PLAYGROUND_IOS_CERTIFICATE_PASSWORD'), 'Missing distribution certificate password')
        with tempfile.TemporaryDirectory(dir=out) as tmp:
            tmp = Path(tmp)
            keychain, installed = tmp / 'signing.keychain-db', None
            installed_owned = False
            original = command(['security', 'list-keychains', '-d', 'user']).decode()
            old_paths = re.findall(r'"([^"]+)"', original)
            try:
                material('PLAYGROUND_IOS_CERTIFICATE_BASE64', tmp / 'certificate.p12')
                material('PLAYGROUND_IOS_PROFILE_BASE64', tmp / 'profile.mobileprovision')
                profile = plistlib.loads(command(['security', 'cms', '-D', '-i', str(tmp / 'profile.mobileprovision')]))
                validate_profile(profile, cert_sha)
                installed = Path.home() / 'Library/MobileDevice/Provisioning Profiles' / (profile['UUID'] + '.mobileprovision')
                require(not installed.exists(), 'Refuse to overwrite an installed profile')
                installed.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(tmp / 'profile.mobileprovision', installed)
                installed_owned = True
                password = secrets.token_urlsafe(32)
                command(['security', 'create-keychain', '-p', password, str(keychain)])
                command(['security', 'set-keychain-settings', '-lut', '3600', str(keychain)])
                command(['security', 'unlock-keychain', '-p', password, str(keychain)])
                command(['security', 'import', str(tmp / 'certificate.p12'), '-k', str(keychain), '-P',
                         os.environ['PLAYGROUND_IOS_CERTIFICATE_PASSWORD'], '-T', '/usr/bin/codesign'])
                command(['security', 'set-key-partition-list', '-S', 'apple-tool:,apple:,codesign:', '-k', password, str(keychain)])
                command(['security', 'list-keychains', '-d', 'user', '-s', str(keychain), *old_paths])
                options = {'method': 'app-store-connect', 'destination': 'export', 'signingStyle': 'manual',
                           'teamID': TEAM, 'signingCertificate': cert_sha, 'manageAppVersionAndBuildNumber': False,
                           'provisioningProfiles': {IOS_ID: profile['UUID']}}
                (tmp / 'ExportOptions.plist').write_bytes(plistlib.dumps(options))
                command(['xcodebuild', '-exportArchive', '-archivePath', str(out / 'Playground.xcarchive'),
                         '-exportPath', str(out / 'export'), '-exportOptionsPlist', str(tmp / 'ExportOptions.plist')])
                ipas = list((out / 'export').glob('*.ipa'))
                require(len(ipas) == 1, 'Expected one exported IPA')
                binary = out / 'Playground.ipa'
                shutil.copyfile(ipas[0], binary)
                with zipfile.ZipFile(binary) as z:
                    names = [n for n in z.namelist() if re.fullmatch(r'Payload/[^/]+\.app/Info.plist', n)]
                    require(len(names) == 1, 'Invalid IPA application layout')
                    ios_metadata(plistlib.loads(z.read(names[0])), sha, number)
                    require(all(not n.startswith('/') and '..' not in Path(n).parts for n in z.namelist()),
                            'Unsafe IPA path')
                extracted = tmp / 'verified-ipa'
                command(['ditto', '-x', '-k', str(binary), str(extracted)])
                apps = list((extracted / 'Payload').glob('*.app'))
                require(len(apps) == 1, 'Expected one signed application')
                command(['codesign', '--verify', '--deep', '--strict', str(apps[0])])
                entitlements = plistlib.loads(command(['codesign', '-d', '--entitlements', ':-', str(apps[0])]))
                require(entitlements.get('application-identifier') == TEAM + '.' + IOS_ID and
                        entitlements.get('get-task-allow') is False, 'Wrong signed application entitlements')
                embedded = plistlib.loads(command(['security', 'cms', '-D', '-i', str(apps[0] / 'embedded.mobileprovision')]))
                validate_profile(embedded, cert_sha)
                require(embedded['UUID'] == profile['UUID'], 'Exported provisioning profile changed')
            finally:
                if installed_owned and installed is not None and installed.exists():
                    installed.unlink()
                subprocess.run(['security', 'list-keychains', '-d', 'user', '-s', *old_paths], capture_output=True)
                if keychain.exists():
                    subprocess.run(['security', 'delete-keychain', str(keychain)], capture_output=True)
    receipt = json.loads((out / 'packaging.json').read_text())
    receipt.update(signing='verified', binary_sha256=hashlib.sha256(binary.read_bytes()).hexdigest())
    (out / 'packaging.json').write_text(json.dumps(receipt, indent=2))
    # Upload reads only packaging.json and the signed binary; drop the archive and export copy.
    for name in ('Playground.xcarchive', 'export'):
        storage.remove_within(out, out / name)
    storage.require_budget(out, platform, 'sign')


def main():
    p = argparse.ArgumentParser()
    p.add_argument('mode', choices=['build', 'sign', 'xcode'])
    p.add_argument('--source', type=Path)
    p.add_argument('--out', type=Path)
    p.add_argument('--platform', choices=['ios', 'android'])
    p.add_argument('--sha')
    p.add_argument('--build')
    p.add_argument('--applications', type=Path, default=Path('/Applications'))
    a = p.parse_args()
    if a.mode == 'xcode':
        # Credential-free: select the pinned Xcode for later steps and print its fixed-shape identity.
        developer = select_xcode(a.applications)
        xcode = xcode_toolchain(developer)
        with open(os.environ['GITHUB_ENV'], 'a') as handle:
            handle.write(f'DEVELOPER_DIR={developer}\n')
        print(f"Selected {XCODE_APP}: Xcode {xcode['xcode']} ({xcode['xcode_build']}), SDK {xcode['sdk']}")
        return
    require(a.source and a.out and a.platform and a.sha and a.build, 'Missing packaging arguments')
    arguments(a.sha, '1', a.build)
    require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Hosted runner only; do not install credentials on a workstation')
    os.umask(0o077)
    if a.mode == 'build':
        build(a.source.resolve(), a.out.resolve(), a.platform, a.sha, a.build)
    else:
        sign(a.out.resolve(), a.platform, a.sha, a.build)
    print('Packaging stage completed; distribution and device status remain separate.')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        # Never echo subprocess commands, passwords, credential values, or provider payloads.
        raise SystemExit('Packaging failed closed. Check committed store metadata, toolchains and protected-environment setup; no upload attempted.')
