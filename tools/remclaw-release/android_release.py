#!/usr/bin/env python3
"""Trusted control for releasing the private RemClaw Android app to the Play internal track.

The Android sibling of release.py. It reuses release.py's exact-SHA deploy-key fetch, ancestry
check, run identity, private-log runner and public-output boundary, and the Playground gate's
environment-protection check and storage guard. Only Android packaging, signing and the public
record differ.

Public-log rule (same as release.py): every message printed is a fixed string or a validated
identifier. Tool output goes only to private log files under $REMCLAW_PRIVATE.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

import release
from release import ReleaseError, StageFailed, require, gate, storage, classify_failure

ENVIRONMENT = 'remclaw-android-release'
PACKAGE = 'com.remapp.rem'
TRACK = 'internal'  # The only Play track this workflow may ever touch.
PLATFORM = 'android'  # storage.py budget row

# Reviewed production backend for the Play build. It is not a workflow input so that a dispatcher
# cannot point a store build at another backend; changing it is a reviewed change here.
# Same backend as the shipping RemClaw iOS Release build: RemClaw/.env.Release.xcconfig sets
# API_BASE_URL = https:/$()/backend-production-7d876.up.railway.app (the $() splits `//` for xcconfig).
# Form: scheme + host only, no path and no trailing slash. RemClaw's Android ApiClient trims a trailing
# `/` and appends absolute paths such as `/api/v1/auth/login`, so `/api/v1` must not be included here.
# validate_api_base_url still fails closed on a blank, staging, non-https or test-domain value.
PRODUCTION_API_BASE_URL = 'https://backend-production-7d876.up.railway.app'

# Play release status for the internal-track upload. A reviewed constant, never a workflow input.
# Google Play accepts only `draft` releases while an app is still a draft app (never published on any
# track), and the Android Publisher API exposes no field that reports that app state. So the very first
# upload of com.remapp.rem goes up as a `draft` internal release: a human must review and roll it out in
# Play Console (Testing > Internal testing). After that first rollout takes the app out of draft, a
# reviewed change sets this to `completed`. Only the release status changes; the track is always
# `internal`. The lane refuses to stack a new upload on an unpromoted draft (pending work).
PLAY_RELEASE_STATUSES = ('draft', 'completed')
PLAY_RELEASE_STATUS = 'draft'

# RemClaw's committed Gradle wrapper distribution. A different wrapper is a reviewed change here.
EXPECTED_GRADLE_DISTRIBUTION = 'gradle-8.14.4-bin.zip'
ANDROID_DIR = 'android'
AAB_PATH = 'app/build/outputs/bundle/release/app-release.aab'
DESIGN_SYSTEM_URL = 'https://github.com/Rem-Assistant/Rem-Design-System.git'
MAX_VERSION_CODE = 2_100_000_000  # Google Play's upper bound for versionCode.
CLIENT_ID = re.compile(r'[0-9]+-[a-z0-9]+\.apps\.googleusercontent\.com')
DISTRIBUTION_STATES = ('not_started', 'upload_started', 'draft_internal', 'published_internal', 'unresolved')
ANDROID_NS = '{http://schemas.android.com/apk/res/android}'


# ---------------------------------------------------------------- inputs

def validate_version_code(value):
    require(isinstance(value, str) and re.fullmatch(r'[1-9][0-9]{0,9}', value) and int(value) <= MAX_VERSION_CODE,
            'version_code must be a positive integer no greater than 2100000000')
    return value


def validate_track(value):
    require(value == TRACK, 'Only the Play internal track is allowed')
    return value


def validate_release_status(value):
    require(value in PLAY_RELEASE_STATUSES, 'Play release status must be draft or completed')
    return value


def validate_api_base_url(url):
    require(url, 'PRODUCTION_API_BASE_URL is not committed; a reviewed change must set it before any release')
    require(re.fullmatch(r'https://[a-z0-9]([a-z0-9.-]*[a-z0-9])?(:[0-9]{1,5})?(/[A-Za-z0-9._~/-]*)?', url),
            'PRODUCTION_API_BASE_URL must be a plain https URL')
    host = url[len('https://'):].split('/')[0].split(':')[0]
    require('.' in host and not host.endswith(('.invalid', '.test', '.example', '.local', '.localhost')) and
            host != 'localhost' and 'staging' not in host, 'PRODUCTION_API_BASE_URL must be the production backend')
    return url


def validate_client_id(value):
    require(isinstance(value, str) and CLIENT_ID.fullmatch(value),
            'Missing or malformed environment variable: REMCLAW_ANDROID_GOOGLE_SERVER_CLIENT_ID')
    return value


def validate_cert_sha256(value):
    require(isinstance(value, str) and re.fullmatch(r'[0-9A-Fa-f]{64}', value.replace(':', '')),
            'Missing or malformed environment variable: REMCLAW_ANDROID_UPLOAD_CERT_SHA256')
    return value.replace(':', '').lower()


def validate_alias(value):
    require(isinstance(value, str) and re.fullmatch(r'[A-Za-z0-9._-]{1,64}', value),
            'Missing or malformed environment variable: REMCLAW_ANDROID_UPLOAD_KEY_ALIAS')
    return value


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
        raise ReleaseError(f'{ENVIRONMENT} environment protection rejected: ' + str(error))
    except (KeyError, TypeError):
        raise ReleaseError(f'{ENVIRONMENT} environment protection could not be read')


def admit(sha, version_code, track, out, environ=os.environ, check=check_environment, release_status=None):
    release.run_identity(environ)
    release.validate_sha(sha)
    validate_version_code(version_code)
    validate_track(track)
    status = validate_release_status(PLAY_RELEASE_STATUS if release_status is None else release_status)
    approval = check(environ)
    record = {'remclaw_sha': sha, 'version_code': version_code, 'track': track, 'release_status': status,
              'approval': approval, 'distribution': 'not_started'}
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(record, indent=2) + '\n')
    return record


# ---------------------------------------------------------------- fetch

def anonymous_git_env():
    """No credentials, helpers, agents or user config: the design system is a public repository."""
    keep = ('PATH', 'HOME', 'TMPDIR', 'LANG', 'LC_ALL')
    env = {k: os.environ[k] for k in keep if k in os.environ}
    env.update(GIT_TERMINAL_PROMPT='0', GIT_CONFIG_NOSYSTEM='1', GIT_CONFIG_GLOBAL=os.devnull,
               GIT_LFS_SKIP_SMUDGE='1', GIT_ASKPASS='/bin/false', SSH_ASKPASS='/bin/false')
    return env


def design_system_lock(source):
    lock = Path(source) / ANDROID_DIR / 'design-system.lock'
    require(lock.is_file(), 'RemClaw Android design-system lock is missing')
    sha = lock.read_text().strip()
    require(re.fullmatch(r'[0-9a-f]{40}', sha), 'RemClaw Android design-system lock must be a 40-character SHA')
    return sha


def fetch_design_system(source, log):
    """The public Rem Design System at exactly the commit RemClaw locks, as android/scripts does."""
    sha = design_system_lock(source)
    dest = Path(source) / ANDROID_DIR / '.design-system'
    require(not dest.exists(), 'Refuse to reuse an existing design-system checkout')
    env = anonymous_git_env()
    release.quiet(['git', 'init', '-q', str(dest)], log, env=env, stage='fetch')
    release.quiet(['git', '-C', str(dest), 'remote', 'add', 'origin', DESIGN_SYSTEM_URL], log, env=env, stage='fetch')
    release.quiet(['git', '-C', str(dest), 'fetch', '--quiet', '--no-tags', '--depth', '1', 'origin', sha],
                  log, env=env, stage='fetch')
    release.quiet(['git', '-C', str(dest), '-c', 'advice.detachedHead=false', 'checkout', '--quiet', '--detach',
                   'FETCH_HEAD'], log, env=env, stage='fetch')
    head = subprocess.run(['git', '-C', str(dest), 'rev-parse', 'HEAD'], capture_output=True, text=True, env=env)
    require(head.returncode == 0 and head.stdout.strip() == sha, 'Design-system checkout differs from its lock')
    require((dest / 'compose/RemDesignSystem').is_dir(), 'Design-system checkout lacks the Compose library')
    return sha


def fetch(sha, dest, environ=os.environ):
    branch = release.fetch(sha, dest, environ)
    private = release.private_dir()
    ds = fetch_design_system(dest, private / 'fetch.log')
    record = json.loads((private / 'checkout.json').read_text())
    record['design_system_sha'] = ds
    (private / 'checkout.json').write_text(json.dumps(record) + '\n')
    return branch


# ---------------------------------------------------------------- packaging

def gradle_properties(sha, version_code, api_base_url, client_id):
    """The only values the trusted control passes to RemClaw's Gradle build."""
    release.validate_sha(sha)
    return ['-PremApiBaseUrl=' + validate_api_base_url(api_base_url),
            '-PgoogleServerClientId=' + validate_client_id(client_id),
            '-PsourceSha=' + sha, '-PversionCode=' + validate_version_code(version_code)]


def android_metadata(xml, version_code):
    """Checks the base-module manifest dumped by bundletool. Returns versionName."""
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        raise ReleaseError('Bundle manifest could not be parsed')
    require(root.get('package') == PACKAGE, 'Android package ID mismatch')
    require(root.get(ANDROID_NS + 'versionCode') == str(version_code), 'Android version code mismatch')
    name = root.get(ANDROID_NS + 'versionName')
    require(isinstance(name, str) and re.fullmatch(r'\d+(\.\d+){0,2}', name), 'Invalid Android version name')
    app = root.find('application')
    require(app is not None, 'Bundle manifest lacks an application')
    require(app.get(ANDROID_NS + 'debuggable', 'false') == 'false', 'Debuggable bundle rejected')
    require(app.get(ANDROID_NS + 'testOnly', 'false') == 'false', 'Test-only bundle rejected')
    return name


def dex_stamps(aab, sha, api_base_url, client_id):
    """RemClaw stamps SOURCE_SHA, REM_API_BASE_URL and GOOGLE_SERVER_CLIENT_ID into BuildConfig; their
    string constants must be present in the base module's dex. Never prints dex content."""
    with zipfile.ZipFile(aab) as z:
        names = z.namelist()
        require(all(not n.startswith('/') and '..' not in Path(n).parts for n in names), 'Unsafe AAB path')
        dexes = [n for n in names if re.fullmatch(r'base/dex/classes\d*\.dex', n)]
        require(dexes, 'Bundle has no base dex')
        found = {k: False for k in ('sha', 'api', 'client')}
        for name in dexes:
            data = z.read(name)
            found['sha'] |= sha.encode() in data
            found['api'] |= api_base_url.encode() in data
            found['client'] |= client_id.encode() in data
    require(found['sha'], 'Android source stamp mismatch')
    require(found['api'], 'Bundle is not built for the reviewed production backend')
    require(found['client'], 'Bundle lacks the configured Google server client ID')


def gradle_env(scratch):
    env = {k: v for k, v in os.environ.items()
           if not k.startswith(('REMCLAW_ANDROID_UPLOAD', 'REMCLAW_PLAY', 'GH_', 'GITHUB_TOKEN'))
           and k != 'REMCLAW_DEPLOY_KEY'}
    env.update(GRADLE_USER_HOME=str(scratch / 'gradle-home'), GRADLE_OPTS='-Dorg.gradle.daemon=false')
    return env


def build(source, out, sha, version_code, environ=os.environ):
    release.validate_sha(sha)
    validate_version_code(version_code)
    api = validate_api_base_url(PRODUCTION_API_BASE_URL)
    client_id = validate_client_id(environ.get('REMCLAW_ANDROID_GOOGLE_SERVER_CLIENT_ID', ''))
    private = release.private_dir()
    checkout = json.loads((private / 'checkout.json').read_text())
    require(checkout.get('remclaw_sha') == sha and checkout.get('release_branch') in release.ALLOWED_RELEASE_BRANCHES,
            'Checkout record mismatch')
    require(release.verify_checkout(source, sha) == checkout['release_branch'], 'Checkout changed since fetch')
    require(design_system_lock(source) == checkout.get('design_system_sha'), 'Design-system lock changed since fetch')
    require(not out.exists(), 'Refuse to overwrite build output')
    storage.require_free(out.parent, PLATFORM, 'build')
    scratch = release.ephemeral_root(source)
    root = source / ANDROID_DIR
    wrapper = root / 'gradle/wrapper/gradle-wrapper.properties'
    require(wrapper.is_file() and EXPECTED_GRADLE_DISTRIBUTION in wrapper.read_text(), 'Unexpected Gradle wrapper')
    out.mkdir(mode=0o700)
    log = private / 'build.log'
    env = gradle_env(scratch)
    init = (Path(__file__).resolve().parent / 'android/inspect.init.gradle')
    gradlew = ['./gradlew', '--no-daemon', '--no-configuration-cache', '--console=plain', '-I', str(init)]
    # Unsigned: RemClaw has no release signingConfig and no credentials exist on the runner yet.
    release.quiet([*gradlew, ':app:bundleRelease', *gradle_properties(sha, version_code, api, client_id)],
                  log, cwd=root, env=env, stage='build')
    built = root / AAB_PATH
    require(built.is_file(), 'Release AAB missing')
    binary = out / 'RemClaw.aab'
    binary.write_bytes(built.read_bytes())
    env['REM_VERIFY_AAB'] = str(binary)
    release.quiet([*gradlew, 'verifyRemClawBundle'], log, cwd=root, env=env, stage='build')
    dumped = release.captured([*gradlew, '-q', 'inspectRemClawBundle'], log, cwd=root, env=env, stage='build').decode()
    match = re.search(r'<manifest\b[\s\S]*?</manifest>', dumped)
    require(match, 'No embedded bundle manifest')
    version_name = android_metadata(match[0], version_code)
    dex_stamps(binary, sha, api, client_id)
    diff = subprocess.run(['git', '-C', str(source), 'diff', '--quiet'], capture_output=True)
    require(diff.returncode == 0, 'Build changed tracked RemClaw source')
    storage.remove_ignored_build_output(source, ANDROID_DIR + '/app/build')
    (out / 'packaging.json').write_text(json.dumps({
        'remclaw_sha': sha, 'release_branch': checkout['release_branch'], 'version_code': version_code,
        'version_name': version_name, 'design_system_sha': checkout['design_system_sha'],
        'overrides': ['version_code', 'source_sha', 'rem_api_base_url', 'google_server_client_id'],
        'signing': 'pending'}, indent=2))
    storage.require_budget(out, PLATFORM, 'build')


def material(name, path, environ=os.environ):
    value = environ.get(name, '')
    require(value, 'Missing protected-environment secret: ' + name)
    try:
        data = base64.b64decode(value, validate=True)
    except ValueError:
        raise ReleaseError('Protected-environment secret is not valid base64: ' + name)
    require(data, 'Protected-environment secret is empty: ' + name)
    path.touch(mode=0o600)
    path.chmod(0o600)
    path.write_bytes(data)


def signature_blocks(aab):
    """The signer blocks of a jarsigner-signed bundle and whether every entry is covered by the manifest."""
    with zipfile.ZipFile(aab) as z:
        names = z.namelist()
        blocks = [z.read(n) for n in names if re.fullmatch(r'META-INF/[^/]+\.(RSA|EC|DSA)', n)]
        manifest = z.read('META-INF/MANIFEST.MF').decode('utf-8', 'replace') if 'META-INF/MANIFEST.MF' in names else ''
    unfolded = manifest.replace('\r\n', '\n').replace('\r', '\n').replace('\n ', '')  # 72-byte line folding
    listed = set(re.findall(r'^Name: (.+)$', unfolded, re.M))
    payload = [n for n in names if not n.endswith('/') and not n.startswith('META-INF/')]
    return blocks, all(n in listed for n in payload)


def check_signature(aab, certificate_der):
    blocks, covered = signature_blocks(aab)
    require(len(blocks) == 1, 'Expected exactly one AAB signer')
    require(certificate_der and certificate_der in blocks[0], 'AAB is not signed by the configured upload key')
    require(covered, 'AAB has entries not covered by the signature')


def sign(out, sha, version_code, environ=os.environ):
    release.validate_sha(sha)
    validate_version_code(version_code)
    private = release.private_dir()
    log = private / 'sign.log'
    storage.require_free(out, PLATFORM, 'sign')
    receipt = json.loads((out / 'packaging.json').read_text())
    require(receipt.get('remclaw_sha') == sha and receipt.get('version_code') == version_code, 'Packaging record mismatch')
    binary = out / 'RemClaw.aab'
    expected = validate_cert_sha256(environ.get('REMCLAW_ANDROID_UPLOAD_CERT_SHA256', ''))
    alias = validate_alias(environ.get('REMCLAW_ANDROID_UPLOAD_KEY_ALIAS', ''))
    require(environ.get('REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD'),
            'Missing protected-environment secret: REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD')
    work = private / 'signing'
    work.mkdir(mode=0o700)
    key = work / 'upload.p12'
    try:
        material('REMCLAW_ANDROID_UPLOAD_KEYSTORE_BASE64', key, environ)
        # Passwords reach keytool/jarsigner by environment-variable name only, never on a command line.
        cert = release.captured(['keytool', '-exportcert', '-keystore', str(key), '-alias', alias,
                                 '-storepass:env', 'REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD'], log, stage='sign')
        require(hashlib.sha256(cert).hexdigest() == expected, 'Android upload identity mismatch')
        release.quiet(['jarsigner', '-keystore', str(key), '-storepass:env', 'REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD',
                       '-keypass:env', 'REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD', str(binary), alias], log, stage='sign')
    finally:
        key.unlink(missing_ok=True)
        storage.remove_within(private, work)
    verified = release.captured(['jarsigner', '-verify', str(binary)], log, stage='sign').decode('utf-8', 'replace')
    require('jar verified.' in verified and 'unsigned entries' not in verified, 'AAB signature verification failed')
    check_signature(binary, cert)
    receipt.update(signing='verified', upload_cert_sha256=expected,
                   binary_sha256=hashlib.sha256(binary.read_bytes()).hexdigest())
    (out / 'packaging.json').write_text(json.dumps(receipt, indent=2))
    storage.require_budget(out, PLATFORM, 'sign')


# ---------------------------------------------------------------- summary and cleanup

def public_record(data, failure=None):
    """Allowlisted, validated fields only; shared fields are validated by release.public_record."""
    shared = {k: data[k] for k in ('remclaw_sha', 'release_branch', 'approval') if data.get(k) is not None}
    record = release.public_record(shared)
    record.pop('processing_state', None)
    if data.get('version_code') is not None:
        record['version_code'] = validate_version_code(str(data['version_code']))
    if data.get('track') is not None:
        record['track'] = validate_track(data['track'])
    if data.get('release_status') is not None:
        record['release_status'] = validate_release_status(data['release_status'])
    state = data.get('distribution', 'not_started')
    require(state in DISTRIBUTION_STATES, 'Unknown distribution state')
    record['distribution'] = state
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
                return 'Invalid release metadata suppressed. Reconcile the Play internal track before retrying.'
    if not merged:
        return 'No verified RemClaw Android release result.' + (f' Failure class: `{failure}`.' if failure else '')
    try:
        record = public_record(merged, failure)
    except (ValueError, TypeError, KeyError):
        return 'Invalid release metadata suppressed. Reconcile the Play internal track before retrying.'
    return '### RemClaw Android release\n```json\n' + json.dumps(record, indent=2) + '\n```'


def cleanup(base):
    """Same bounded cleanup as iOS: the upload key, Gradle home and logs live under those runner paths."""
    release.cleanup(base)


# ---------------------------------------------------------------- entry

def main(argv=None):
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest='mode', required=True)
    a = sub.add_parser('admit')
    a.add_argument('--sha', required=True)
    a.add_argument('--version-code', required=True)
    a.add_argument('--track', required=True)
    a.add_argument('--out', type=Path, required=True)
    f = sub.add_parser('fetch')
    f.add_argument('--sha', required=True)
    f.add_argument('--dest', type=Path, required=True)
    for name in ('build', 'sign'):
        s = sub.add_parser(name)
        s.add_argument('--source', type=Path)
        s.add_argument('--out', type=Path, required=True)
        s.add_argument('--sha', required=True)
        s.add_argument('--version-code', required=True)
    m = sub.add_parser('summary')
    m.add_argument('--failure', type=Path)
    m.add_argument('records', nargs='+')
    c = sub.add_parser('cleanup')
    c.add_argument('base', type=Path)
    args = p.parse_args(argv)
    os.umask(0o077)
    if args.mode == 'admit':
        admit(args.sha, args.version_code, args.track, args.out)
        print('RemClaw Android release admission passed: run identity, inputs and environment protection verified.')
    elif args.mode == 'fetch':
        release.run_identity(os.environ)
        branch = fetch(args.sha, args.dest.resolve())
        print(f'RemClaw checkout verified at the requested SHA; ancestry confirmed on allowed branch {branch}; '
              'design system pinned at its lock.')
    elif args.mode in ('build', 'sign'):
        require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Hosted runner only; do not install credentials on a workstation')
        release.run_identity(os.environ)
        if args.mode == 'build':
            require(args.source, 'Missing --source')
            build(args.source.resolve(), args.out.resolve(), args.sha, args.version_code)
        else:
            sign(args.out.resolve(), args.sha, args.version_code)
        print(f'RemClaw Android {args.mode} stage completed.')
    elif args.mode == 'summary':
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as handle:
            handle.write(summary_text(args.records, args.failure) + '\n')
    else:
        cleanup(args.base)
        print('RemClaw source, bundle, upload key, Gradle home and private logs removed.')


def entry(argv=None):
    """Public-output boundary: only fixed messages and the allowlisted classification line escape."""
    try:
        main(argv)
    except StageFailed as failure:
        classification, counts, total = classify_failure.classify_files([failure.log])
        print(classify_failure.render(classification, counts, total), flush=True)
        release.record_failure(classification)
        raise SystemExit(f'RemClaw Android {failure.stage} step failed; private output suppressed. Classification above.')
    except ReleaseError as error:
        release.record_failure('POLICY')
        raise SystemExit('RemClaw Android release rejected: ' + str(error))
    except SystemExit:
        raise
    except BaseException:
        release.record_failure('UNKNOWN')
        raise SystemExit('RemClaw Android release failed closed (internal error; details suppressed).')


if __name__ == '__main__':
    entry()
