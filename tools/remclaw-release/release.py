#!/usr/bin/env python3
"""Trusted control for releasing the private RemClaw iOS app from this public repository.

Public-log rule: every message this script prints is a fixed string or a validated identifier.
It never prints, quotes or uploads RemClaw source, paths inside the checkout, commit metadata,
tool output, credential material or provider payloads. Tool output goes only to private log files
under $REMCLAW_PRIVATE (inside $RUNNER_TEMP) and is summarized by classify_failure.py.
"""
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
import shlex
import shutil
import subprocess
import sys
import zipfile

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'playground-distribution'))
import gate  # noqa: E402  (shared run-identity and environment-protection checks)
import storage  # noqa: E402  (shared runner storage guard)
import classify_failure  # noqa: E402

REMCLAW_REPO = 'Rem-Assistant/RemClaw'
REMCLAW_SSH_URL = 'git@github.com:Rem-Assistant/RemClaw.git'
# GitHub's published Ed25519 SSH host key (SHA256:+DiY3wvvV6TuJJhbpZisF/zLDA0zPMSvHdkr4UvCOqU).
GITHUB_HOST_KEY = 'github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl'
ENVIRONMENT = 'remclaw-ios-release'
# The only RemClaw branches a release SHA may come from. The SHA must already be in their history.
ALLOWED_RELEASE_BRANCHES = ('main', 'skip-sent-reply-suggestions')

BUNDLE_ID = 'com.remapp.rem'
WIDGET_ID = 'com.remapp.rem.widget'
APP_GROUP = 'group.com.remapp.rem'
TEAM = 'R6A892D599'
PROJECT = 'RemClaw.xcodeproj'
SCHEME = 'RemClaw'
MIN_XCODE_MAJOR = 26  # RemClaw targets iOS 26.0
PLATFORM = 'ios'  # storage.py budget row

PROCESSING_STATES = ('not_started', 'upload_started', 'PROCESSING', 'VALID', 'INVALID', 'FAILED', 'unresolved')
RUNNER_TEMP_NAMES = ('remclaw-source', 'remclaw-package', 'remclaw-ephemeral', 'remclaw-private',
                     'remclaw-gems', 'remclaw-admission.json')


class ReleaseError(ValueError):
    """A fixed, public-safe message. Only these messages are ever printed."""


class StageFailed(Exception):
    def __init__(self, stage, log):
        super().__init__(stage)
        self.stage, self.log = stage, log


def require(ok, message):
    if not ok:
        raise ReleaseError(message)


def validate_sha(sha):
    require(isinstance(sha, str) and re.fullmatch(r'[0-9a-f]{40}', sha), 'remclaw_sha must be 40 lowercase hex characters')
    return sha


def validate_build(number):
    require(isinstance(number, str) and re.fullmatch(r'[1-9][0-9]{0,8}', number),
            'Build number must be a positive integer below 1 billion')
    return number


def run_identity(environ):
    # Same rule as the Playground gate: this repository, manual dispatch, from main.
    require(gate.same_repository(environ.get('GITHUB_REPOSITORY')) and environ.get('GITHUB_REF') == 'refs/heads/main'
            and environ.get('GITHUB_EVENT_NAME') == 'workflow_dispatch',
            'Release control must run by manual dispatch from main of this repository')


def release_branch(sha, is_ancestor, branches=ALLOWED_RELEASE_BRANCHES):
    """First allowed branch whose history contains sha; rejects anything else."""
    validate_sha(sha)
    require(tuple(branches) == ALLOWED_RELEASE_BRANCHES, 'Release branch list is fixed in code')
    for branch in branches:
        if is_ancestor(sha, branch):
            return branch
    raise ReleaseError('remclaw_sha is not in the history of an allowed release branch')


def private_dir():
    path = os.environ.get('REMCLAW_PRIVATE', '')
    require(path and Path(path).is_dir(), 'Missing private runner directory')
    return Path(path).resolve()


def quiet(argv, log, *, cwd=None, env=None, stage):
    """Run a tool with all output appended to a private log. Raises StageFailed, never echoes output."""
    with open(log, 'ab') as handle:
        r = subprocess.run(argv, cwd=cwd, env=env, stdout=handle, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
    if r.returncode != 0:
        raise StageFailed(stage, log)


def captured(argv, log, *, cwd=None, env=None, stage):
    """Run a tool whose stdout we parse; stderr goes to the private log."""
    with open(log, 'ab') as handle:
        r = subprocess.run(argv, cwd=cwd, env=env, stdout=subprocess.PIPE, stderr=handle, stdin=subprocess.DEVNULL)
    if r.returncode != 0:
        raise StageFailed(stage, log)
    return r.stdout


# ---------------------------------------------------------------- admission

def check_environment(environ):
    base = f'repos/{gate.REPO}/environments/{ENVIRONMENT}'
    try:
        env = gate.gh(base)
        policies = [{'name': p['name'], 'type': p['type']}
                    for p in gate.paged(base + '/deployment-branch-policies', 'branch_policies')]
        return gate.validate_environment(env, policies,
                                         (environ.get('GITHUB_ACTOR'), environ.get('GITHUB_TRIGGERING_ACTOR')))
    except ValueError as error:
        # gate messages are fixed strings; keep them, but never anything from a response body.
        raise ReleaseError(f'{ENVIRONMENT} environment protection rejected: ' + str(error))
    except (KeyError, TypeError):
        raise ReleaseError(f'{ENVIRONMENT} environment protection could not be read')


def admit(sha, out, environ=os.environ):
    run_identity(environ)
    validate_sha(sha)
    approval = check_environment(environ)
    record = {'remclaw_sha': sha, 'approval': approval, 'processing_state': 'not_started'}
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(record, indent=2) + '\n')
    return record


# ---------------------------------------------------------------- fetch

def git_env(key, known_hosts):
    ssh = ' '.join(shlex.quote(x) for x in (
        'ssh', '-F', '/dev/null', '-i', str(key), '-o', 'IdentitiesOnly=yes', '-o', 'IdentityAgent=none',
        '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=yes', '-o', 'HostKeyAlgorithms=ssh-ed25519',
        '-o', 'UserKnownHostsFile=' + str(known_hosts)))
    env = {k: v for k, v in os.environ.items() if not k.startswith('GIT_') and k != 'REMCLAW_DEPLOY_KEY'}
    env.update(GIT_SSH_COMMAND=ssh, GIT_TERMINAL_PROMPT='0', GIT_CONFIG_NOSYSTEM='1', GIT_CONFIG_GLOBAL=os.devnull,
               GIT_LFS_SKIP_SMUDGE='1')
    return env


def git_is_ancestor(root, env=None):
    def check(sha, branch):
        r = subprocess.run(['git', '-C', str(root), 'merge-base', '--is-ancestor', sha, f'refs/remotes/origin/{branch}'],
                           capture_output=True, env=env)
        require(r.returncode in (0, 1), 'Release branch history unavailable')
        return r.returncode == 0
    return check


def verify_checkout(root, sha, env=None):
    """HEAD is exactly sha, the tree is clean, and sha is in an allowed release branch's history."""
    head = subprocess.run(['git', '-C', str(root), 'rev-parse', 'HEAD'], capture_output=True, text=True, env=env)
    require(head.returncode == 0 and head.stdout.strip() == sha, 'RemClaw checkout HEAD differs from remclaw_sha')
    status = subprocess.run(['git', '-C', str(root), 'status', '--porcelain', '--untracked-files=all'],
                            capture_output=True, env=env)
    require(status.returncode == 0 and not status.stdout, 'RemClaw checkout must be clean')
    return release_branch(sha, git_is_ancestor(root, env))


def fetch(sha, dest, environ=os.environ):
    validate_sha(sha)
    private = private_dir()
    require(not dest.exists(), 'Refuse to reuse an existing RemClaw checkout')
    key_text = environ.get('REMCLAW_DEPLOY_KEY', '')
    require(key_text.strip(), 'Missing protected-environment secret: REMCLAW_DEPLOY_KEY')
    key, known, log = private / 'deploy_key', private / 'known_hosts', private / 'fetch.log'
    try:
        key.touch(mode=0o600)
        key.write_text(key_text.strip() + '\n')
        known.write_text(GITHUB_HOST_KEY + '\n')
        env = git_env(key, known)
        quiet(['git', 'init', '-q', str(dest)], log, env=env, stage='fetch')
        quiet(['git', '-C', str(dest), 'remote', 'add', 'origin', REMCLAW_SSH_URL], log, env=env, stage='fetch')
        # Full history of the allowed branches only: no tags, no other refs, no credentials persisted.
        refspecs = [f'+refs/heads/{b}:refs/remotes/origin/{b}' for b in ALLOWED_RELEASE_BRANCHES]
        quiet(['git', '-C', str(dest), 'fetch', '--quiet', '--no-tags', '--no-recurse-submodules', 'origin', *refspecs],
              log, env=env, stage='fetch')
    finally:
        for path in (key, known):
            if path.exists():
                path.unlink()
    exists = subprocess.run(['git', '-C', str(dest), 'cat-file', '-e', sha + '^{commit}'], capture_output=True, env=env)
    require(exists.returncode == 0, 'remclaw_sha is not in the history of an allowed release branch')
    quiet(['git', '-C', str(dest), '-c', 'advice.detachedHead=false', 'checkout', '--quiet', '--detach', sha],
          log, env=env, stage='fetch')
    branch = verify_checkout(dest, sha, env)
    (private / 'checkout.json').write_text(json.dumps({'remclaw_sha': sha, 'release_branch': branch}) + '\n')
    return branch


# ---------------------------------------------------------------- toolchain

def xcode_version(name):
    m = re.fullmatch(r'Xcode_(\d+)\.(\d+)(?:\.(\d+))?\.app', name)
    return tuple(int(x or 0) for x in m.groups()) if m else None


def select_xcode(names):
    """Newest stable Xcode_<major>.<minor>[.<patch>].app at or above the minimum; betas are ignored."""
    candidates = sorted((v, n) for n in names if (v := xcode_version(n)) and v[0] >= MIN_XCODE_MAJOR)
    require(candidates, f'No stable Xcode {MIN_XCODE_MAJOR}+ on this runner image')
    return candidates[-1][1]


# ---------------------------------------------------------------- packaging

def ephemeral_root(source):
    path = Path(os.environ.get('REMCLAW_EPHEMERAL', '')).resolve()
    require(os.environ.get('REMCLAW_EPHEMERAL') and path.is_dir(), 'Missing ephemeral build directory')
    source = Path(source).resolve()
    require(source != path and source not in path.parents and path not in source.parents,
            'Ephemeral build directory must be separate from the RemClaw checkout')
    return path


def app_metadata(info, number, bundle_id=BUNDLE_ID):
    require(info.get('CFBundleIdentifier') == bundle_id, 'Bundle identifier mismatch')
    require(info.get('CFBundleVersion') == str(number), 'Build number mismatch')
    version = info.get('CFBundleShortVersionString')
    require(isinstance(version, str) and re.fullmatch(r'\d+(\.\d+){0,2}', version), 'Invalid marketing version')
    return version


def bundle_layout(app_dir):
    """Main app Info.plist plus exactly the expected widget extension."""
    app_dir = Path(app_dir)
    info = plistlib.loads((app_dir / 'Info.plist').read_bytes())
    plugins = sorted((app_dir / 'PlugIns').glob('*.appex')) if (app_dir / 'PlugIns').is_dir() else []
    widgets = [plistlib.loads((p / 'Info.plist').read_bytes()) for p in plugins]
    require([w.get('CFBundleIdentifier') for w in widgets] == [WIDGET_ID], 'Unexpected embedded extensions')
    return info, widgets[0], plugins[0]


def check_layout(info, widget, number):
    version = app_metadata(info, number)
    require(app_metadata(widget, number, WIDGET_ID) == version, 'Widget version differs from app')
    return version


def build(source, out, sha, number):
    validate_sha(sha)
    validate_build(number)
    private = private_dir()
    checkout = json.loads((private / 'checkout.json').read_text())
    require(checkout.get('remclaw_sha') == sha and checkout.get('release_branch') in ALLOWED_RELEASE_BRANCHES,
            'Checkout record mismatch')
    require(verify_checkout(source, sha) == checkout['release_branch'], 'Checkout changed since fetch')
    require(not out.exists(), 'Refuse to overwrite build output')
    storage.require_free(out.parent, PLATFORM, 'build')
    scratch = ephemeral_root(source)
    project = source / PROJECT
    require(project.is_dir(), 'Expected RemClaw Xcode project is missing')
    out.mkdir(mode=0o700)
    log = private / 'build.log'
    archive = out / 'RemClaw.xcarchive'
    # Unsigned archive: no credentials exist on the runner yet. Only the build number is overridden.
    quiet(['xcodebuild', '-project', str(project), '-scheme', SCHEME, '-configuration', 'Release',
           '-destination', 'generic/platform=iOS', '-archivePath', str(archive),
           '-derivedDataPath', str(scratch / 'DerivedData'),
           '-clonedSourcePackagesDirPath', str(scratch / 'SourcePackages'),
           '-onlyUsePackageVersionsFromResolvedFile', '-hideShellScriptEnvironment',
           'CODE_SIGNING_ALLOWED=NO', 'CURRENT_PROJECT_VERSION=' + number, 'archive'],
          log, stage='build')
    apps = list((archive / 'Products/Applications').glob('*.app'))
    require(len(apps) == 1, 'Expected one archived application')
    info, widget, _ = bundle_layout(apps[0])
    version = check_layout(info, widget, number)
    diff = subprocess.run(['git', '-C', str(source), 'diff', '--quiet'], capture_output=True)
    require(diff.returncode == 0, 'Build changed tracked RemClaw source')
    storage.remove_within(scratch, scratch / 'DerivedData')
    (out / 'packaging.json').write_text(json.dumps({
        'remclaw_sha': sha, 'release_branch': checkout['release_branch'], 'build_number': number,
        'marketing_version': version, 'overrides': ['build_number'], 'signing': 'pending'}, indent=2))
    storage.require_budget(out, PLATFORM, 'build')


def validate_profile(profile, certificate_sha1, bundle_id, now=None):
    e = profile.get('Entitlements', {})
    require(profile.get('TeamIdentifier') == [TEAM] and e.get('application-identifier') == f'{TEAM}.{bundle_id}',
            'Wrong provisioning team or bundle identifier')
    require(e.get('get-task-allow') is False and not profile.get('ProvisionedDevices') and
            not profile.get('ProvisionsAllDevices'), 'Require an App Store distribution profile')
    require(APP_GROUP in (e.get('com.apple.security.application-groups') or []), 'Profile lacks the RemClaw app group')
    if bundle_id == BUNDLE_ID:
        require(e.get('aps-environment') == 'production', 'App profile lacks production push')
        require('com.apple.developer.applesignin' in e, 'App profile lacks Sign in with Apple')
    expires = profile.get('ExpirationDate')
    require(isinstance(expires, datetime) and
            expires.replace(tzinfo=timezone.utc) > (now or datetime.now(timezone.utc)), 'Profile expired')
    require(certificate_sha1.lower() in [hashlib.sha1(c).hexdigest() for c in profile.get('DeveloperCertificates', [])],
            'Profile does not authorize the configured distribution certificate')
    require(re.fullmatch(r'[A-Fa-f0-9-]{36}', profile.get('UUID', '')), 'Invalid profile UUID')


def validate_signed_entitlements(entitlements, bundle_id):
    require(entitlements.get('application-identifier') == f'{TEAM}.{bundle_id}' and
            entitlements.get('get-task-allow') is False, 'Wrong signed application identity')
    require(APP_GROUP in (entitlements.get('com.apple.security.application-groups') or []),
            'Signed bundle lost the RemClaw app group')
    if bundle_id == BUNDLE_ID:
        require(entitlements.get('aps-environment') == 'production', 'Signed app lacks production push')
        require('Default' in (entitlements.get('com.apple.developer.applesignin') or []),
                'Signed app lacks Sign in with Apple')


def material(name, path):
    value = os.environ.get(name, '')
    require(value, 'Missing protected-environment secret: ' + name)
    try:
        data = base64.b64decode(value, validate=True)
    except ValueError:
        raise ReleaseError('Protected-environment secret is not valid base64: ' + name)
    path.touch(mode=0o600)
    path.write_bytes(data)


def profiles_dir():
    return Path.home() / 'Library/MobileDevice/Provisioning Profiles'


def sign(out, sha, number):
    validate_sha(sha)
    validate_build(number)
    private = private_dir()
    log = private / 'sign.log'
    storage.require_free(out, PLATFORM, 'sign')
    receipt = json.loads((out / 'packaging.json').read_text())
    require(receipt.get('remclaw_sha') == sha and receipt.get('build_number') == number, 'Packaging record mismatch')
    cert_sha = os.environ.get('REMCLAW_IOS_CERTIFICATE_SHA1', '')
    require(re.fullmatch(r'[A-Fa-f0-9]{40}', cert_sha), 'Missing configured distribution certificate fingerprint')
    require(os.environ.get('REMCLAW_IOS_CERTIFICATE_PASSWORD'), 'Missing distribution certificate password')
    work = private / 'signing'
    work.mkdir(mode=0o700)
    keychain = private / 'signing.keychain-db'
    record = private / 'installed-profiles.json'
    installed = []
    original = captured(['security', 'list-keychains', '-d', 'user'], log, stage='sign').decode()
    old_paths = re.findall(r'"([^"]+)"', original)
    try:
        material('REMCLAW_IOS_CERTIFICATE_BASE64', work / 'certificate.p12')
        uuids = {}
        for bundle_id, secret in ((BUNDLE_ID, 'REMCLAW_IOS_PROFILE_BASE64'), (WIDGET_ID, 'REMCLAW_IOS_WIDGET_PROFILE_BASE64')):
            path = work / (bundle_id + '.mobileprovision')
            material(secret, path)
            profile = plistlib.loads(captured(['security', 'cms', '-D', '-i', str(path)], log, stage='sign'))
            validate_profile(profile, cert_sha, bundle_id)
            target = profiles_dir() / (profile['UUID'] + '.mobileprovision')
            require(not target.exists(), 'Refuse to overwrite an installed profile')
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, target)
            installed.append(str(target))
            record.write_text(json.dumps(installed))
            uuids[bundle_id] = profile['UUID']
        password = secrets.token_urlsafe(32)
        quiet(['security', 'create-keychain', '-p', password, str(keychain)], log, stage='sign')
        quiet(['security', 'set-keychain-settings', '-lut', '3600', str(keychain)], log, stage='sign')
        quiet(['security', 'unlock-keychain', '-p', password, str(keychain)], log, stage='sign')
        quiet(['security', 'import', str(work / 'certificate.p12'), '-k', str(keychain), '-P',
               os.environ['REMCLAW_IOS_CERTIFICATE_PASSWORD'], '-T', '/usr/bin/codesign'], log, stage='sign')
        quiet(['security', 'set-key-partition-list', '-S', 'apple-tool:,apple:,codesign:', '-k', password, str(keychain)],
              log, stage='sign')
        quiet(['security', 'list-keychains', '-d', 'user', '-s', str(keychain), *old_paths], log, stage='sign')
        options = {'method': 'app-store-connect', 'destination': 'export', 'signingStyle': 'manual',
                   'teamID': TEAM, 'signingCertificate': cert_sha.upper(), 'manageAppVersionAndBuildNumber': False,
                   'uploadSymbols': True, 'provisioningProfiles': uuids}
        (work / 'ExportOptions.plist').write_bytes(plistlib.dumps(options))
        quiet(['xcodebuild', '-exportArchive', '-archivePath', str(out / 'RemClaw.xcarchive'),
               '-exportPath', str(out / 'export'), '-exportOptionsPlist', str(work / 'ExportOptions.plist')],
              log, stage='sign')
        ipas = list((out / 'export').glob('*.ipa'))
        require(len(ipas) == 1, 'Expected one exported IPA')
        binary = out / 'RemClaw.ipa'
        shutil.copyfile(ipas[0], binary)
        with zipfile.ZipFile(binary) as z:
            require(all(not n.startswith('/') and '..' not in Path(n).parts for n in z.namelist()), 'Unsafe IPA path')
        extracted = work / 'verified-ipa'
        quiet(['ditto', '-x', '-k', str(binary), str(extracted)], log, stage='sign')
        apps = list((extracted / 'Payload').glob('*.app'))
        require(len(apps) == 1, 'Expected one signed application')
        info, widget, appex = bundle_layout(apps[0])
        require(check_layout(info, widget, number) == receipt['marketing_version'], 'Marketing version changed')
        quiet(['codesign', '--verify', '--deep', '--strict', str(apps[0])], log, stage='sign')
        for bundle_id, bundle in ((BUNDLE_ID, apps[0]), (WIDGET_ID, appex)):
            ent = plistlib.loads(captured(['codesign', '-d', '--entitlements', ':-', str(bundle)], log, stage='sign'))
            validate_signed_entitlements(ent, bundle_id)
            embedded = plistlib.loads(captured(['security', 'cms', '-D', '-i', str(bundle / 'embedded.mobileprovision')],
                                               log, stage='sign'))
            validate_profile(embedded, cert_sha, bundle_id)
            require(embedded['UUID'] == uuids[bundle_id], 'Exported provisioning profile changed')
    finally:
        for path in installed:
            Path(path).unlink(missing_ok=True)
        record.unlink(missing_ok=True)
        subprocess.run(['security', 'list-keychains', '-d', 'user', '-s', *old_paths], capture_output=True)
        if keychain.exists():
            subprocess.run(['security', 'delete-keychain', str(keychain)], capture_output=True)
        shutil.rmtree(work, ignore_errors=True)
    receipt.update(signing='verified', binary_sha256=hashlib.sha256(binary.read_bytes()).hexdigest())
    (out / 'packaging.json').write_text(json.dumps(receipt, indent=2))
    for name in ('RemClaw.xcarchive', 'export'):
        storage.remove_within(out, out / name)
    storage.require_budget(out, PLATFORM, 'sign')


# ---------------------------------------------------------------- summary and cleanup

def public_record(data, failure=None):
    """Allowlisted, validated fields only. Anything else is dropped."""
    record = {'remclaw_sha': validate_sha(data.get('remclaw_sha'))}
    if data.get('build_number') is not None:
        record['build_number'] = validate_build(str(data['build_number']))
    if data.get('release_branch') is not None:
        require(data['release_branch'] in ALLOWED_RELEASE_BRANCHES, 'Unknown release branch')
        record['release_branch'] = data['release_branch']
    state = data.get('processing_state', 'not_started')
    require(state in PROCESSING_STATES, 'Unknown processing state')
    record['processing_state'] = state
    approval = data.get('approval')
    if approval is not None:
        require(isinstance(approval, dict) and set(approval) == {'model', 'reviewer'} and
                approval['model'] in ('owner-approval', 'independent') and
                approval['reviewer'] in gate.REVIEWERS.values() and
                (approval['model'] != 'owner-approval' or approval['reviewer'] == gate.REVIEWERS[gate.OWNER_ID]),
                'Invalid approval metadata')
        record['approval'] = {'model': approval['model'], 'reviewer': approval['reviewer']}
    if failure is not None:
        require(failure in classify_failure.CODES, 'Unknown failure class')
        record['failure_class'] = failure
    return record


def summary_text(paths, failure_file):
    failure = None
    if failure_file and Path(failure_file).is_file():
        code = Path(failure_file).read_text().strip()
        failure = code if code in classify_failure.CODES else 'UNKNOWN'
    merged = {}
    for path in paths:
        p = Path(path)
        if p.is_file():
            try:
                merged.update(json.loads(p.read_text()))
            except ValueError:
                return 'Invalid release metadata suppressed. Reconcile App Store Connect state before retrying.'
    if not merged:
        text = 'No verified RemClaw release result.'
        return text + (f' Failure class: `{failure}`.' if failure else '')
    try:
        record = public_record(merged, failure)
    except (ValueError, TypeError, KeyError):
        return 'Invalid release metadata suppressed. Reconcile App Store Connect state before retrying.'
    return '### RemClaw iOS release\n```json\n' + json.dumps(record, indent=2) + '\n```'


def cleanup(base):
    base = Path(base).resolve()
    require(base == Path(os.environ['RUNNER_TEMP']).resolve(), 'Only clean runner temporary output')
    private = base / 'remclaw-private'
    record = private / 'installed-profiles.json'
    if record.is_file():
        try:
            for path in json.loads(record.read_text()):
                p = Path(path)
                if p.parent == profiles_dir() and re.fullmatch(r'[A-Fa-f0-9-]{36}\.mobileprovision', p.name):
                    p.unlink(missing_ok=True)
        except (ValueError, TypeError):
            pass
    keychain = private / 'signing.keychain-db'
    if keychain.exists():
        subprocess.run(['security', 'delete-keychain', str(keychain)], capture_output=True)
    for name in RUNNER_TEMP_NAMES:
        target = base / name
        if target.is_symlink() or target.is_file():
            target.unlink()
        elif target.is_dir():
            shutil.rmtree(target, ignore_errors=True)


def main(argv=None):
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest='mode', required=True)
    a = sub.add_parser('admit')
    a.add_argument('--sha', required=True)
    a.add_argument('--out', type=Path, required=True)
    f = sub.add_parser('fetch')
    f.add_argument('--sha', required=True)
    f.add_argument('--dest', type=Path, required=True)
    x = sub.add_parser('xcode')
    x.add_argument('--applications', type=Path, default=Path('/Applications'))
    n = sub.add_parser('build-number')
    n.add_argument('file', type=Path)
    for name in ('build', 'sign'):
        s = sub.add_parser(name)
        s.add_argument('--source', type=Path)
        s.add_argument('--out', type=Path, required=True)
        s.add_argument('--sha', required=True)
        s.add_argument('--build', required=True)
    m = sub.add_parser('summary')
    m.add_argument('--failure', type=Path)
    m.add_argument('records', nargs='+')
    c = sub.add_parser('cleanup')
    c.add_argument('base', type=Path)
    args = p.parse_args(argv)
    os.umask(0o077)
    if args.mode == 'admit':
        admit(args.sha, args.out)
        print('RemClaw release admission passed: run identity, SHA format and environment protection verified.')
    elif args.mode == 'fetch':
        run_identity(os.environ)
        branch = fetch(args.sha, args.dest.resolve())
        print(f'RemClaw checkout verified at the requested SHA; ancestry confirmed on allowed branch {branch}.')
    elif args.mode == 'xcode':
        name = select_xcode([d.name for d in args.applications.iterdir()])
        developer = args.applications / name / 'Contents/Developer'
        with open(os.environ['GITHUB_ENV'], 'a') as handle:
            handle.write(f'DEVELOPER_DIR={developer}\n')
        print('Selected ' + name)
    elif args.mode == 'build-number':
        number = validate_build(args.file.read_text().strip())
        with open(os.environ['GITHUB_ENV'], 'a') as handle:
            handle.write(f'REMCLAW_BUILD_NUMBER={number}\n')
        print('Reserved build number ' + number)
    elif args.mode in ('build', 'sign'):
        require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Hosted runner only; do not install credentials on a workstation')
        run_identity(os.environ)
        if args.mode == 'build':
            require(args.source, 'Missing --source')
            build(args.source.resolve(), args.out.resolve(), args.sha, args.build)
        else:
            sign(args.out.resolve(), args.sha, args.build)
        print(f'RemClaw {args.mode} stage completed.')
    elif args.mode == 'summary':
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as handle:
            handle.write(summary_text(args.records, args.failure) + '\n')
    else:
        cleanup(args.base)
        print('RemClaw source, package, keychain, profiles, private logs and credential files removed.')


def record_failure(code):
    try:
        path = private_dir() / 'failure_class'
        if not path.exists():
            path.write_text(code + '\n')
    except ValueError:
        pass


def entry(argv=None):
    """Public-output boundary: only fixed messages and the allowlisted classification line escape."""
    try:
        main(argv)
    except StageFailed as failure:
        classification, counts, total = classify_failure.classify_files([failure.log])
        print(classify_failure.render(classification, counts, total), flush=True)
        record_failure(classification)
        raise SystemExit(f'RemClaw {failure.stage} step failed; private output suppressed. Classification above.')
    except ReleaseError as error:
        record_failure('POLICY')
        raise SystemExit('RemClaw release rejected: ' + str(error))
    except SystemExit:
        raise
    except BaseException:
        # Never echo exception text: it can carry paths, plist content or command lines.
        record_failure('UNKNOWN')
        raise SystemExit('RemClaw release failed closed (internal error; details suppressed).')


if __name__ == '__main__':
    entry()
