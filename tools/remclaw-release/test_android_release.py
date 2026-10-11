"""Secret-free tests for android_release.py (the RemClaw Android release control).

No RemClaw source, Gradle, Android SDK, JDK or credentials are needed: packaging and signing checks
run against synthetic bundles, and git cases use throwaway local repositories.
"""
import base64
import hashlib
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile
import unittest
import zipfile
from unittest.mock import patch

import android_release as ar
import release

SHA = 'b' * 40
API = 'https://api.rem.com'  # syntactically valid test host, not the real backend
CLIENT = '123456789012-abcdefghijklmnop0123456789.apps.googleusercontent.com'
OWNER_ENV = dict(protection_rules=[dict(type='required_reviewers', reviewers=[{'type': 'User',
                 'reviewer': {'id': 58840187, 'login': 'samuelalake'}}], prevent_self_review=False)],
                 can_admins_bypass=False, deployment_branch_policy=dict(protected_branches=False, custom_branch_policies=True))
MAIN_ONLY = [dict(name='main', type='branch')]
IDENTITY = dict(GITHUB_REPOSITORY='Rem-Assistant/Rem-Design-System', GITHUB_REF='refs/heads/main',
                GITHUB_EVENT_NAME='workflow_dispatch', GITHUB_ACTOR='samuelalake', GITHUB_TRIGGERING_ACTOR='samuelalake')
A = 'http://schemas.android.com/apk/res/android'


def manifest(package='com.remapp.rem', code='42', name='0.1.0', app_attrs=''):
    return (f'<manifest xmlns:android="{A}" package="{package}" android:versionCode="{code}" '
            f'android:versionName="{name}"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="34"/>'
            f'<application android:label="@string/app_name" {app_attrs}/></manifest>')


def git(root, *args, env=None):
    return subprocess.run(['git', '-C', str(root), *args], check=True, capture_output=True, text=True, env=env).stdout.strip()


class InputTests(unittest.TestCase):
    def test_version_code(self):
        for good in ['1', '42', '2100000000']:
            self.assertEqual(ar.validate_version_code(good), good)
        for bad in ['0', '01', '-1', '1e3', '2100000001', '99999999999', '', '4 2', '42\n', None, 42]:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_version_code(bad)

    def test_only_internal_track(self):
        self.assertEqual(ar.validate_track('internal'), 'internal')
        for bad in ['alpha', 'beta', 'production', 'Internal', 'internal ', '', None, 'internal,production']:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_track(bad)

    def test_production_url_placeholder_fails_closed(self):
        with self.assertRaisesRegex(release.ReleaseError, 'not committed'):
            ar.validate_api_base_url('')
        # The committed constant is either still the empty placeholder or a valid production URL.
        if ar.PRODUCTION_API_BASE_URL:
            ar.validate_api_base_url(ar.PRODUCTION_API_BASE_URL)
        else:
            with patch.dict(os.environ, IDENTITY), self.assertRaises(release.ReleaseError):
                ar.build(Path('/nonexistent'), Path('/nonexistent/out'), SHA, '42')

    def test_production_url_is_the_ios_release_backend_host_only(self):
        # Same backend as RemClaw/.env.Release.xcconfig; scheme + host only, since RemClaw's Android
        # ApiClient appends absolute `/api/v1/...` paths to it.
        self.assertEqual(ar.PRODUCTION_API_BASE_URL, 'https://backend-production-7d876.up.railway.app')
        self.assertEqual(ar.validate_api_base_url(ar.PRODUCTION_API_BASE_URL), ar.PRODUCTION_API_BASE_URL)
        self.assertNotIn('/api', ar.PRODUCTION_API_BASE_URL)
        self.assertFalse(ar.PRODUCTION_API_BASE_URL.endswith('/'))
        for bad in ['https://backend-staging-b87e.up.railway.app', '', 'http://backend-production-7d876.up.railway.app']:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_api_base_url(bad)

    def test_release_status_is_reviewed_draft_or_completed(self):
        self.assertEqual(ar.PLAY_RELEASE_STATUSES, ('draft', 'completed'))
        self.assertIn(ar.PLAY_RELEASE_STATUS, ar.PLAY_RELEASE_STATUSES)
        for good in ['draft', 'completed']:
            self.assertEqual(ar.validate_release_status(good), good)
        for bad in ['', None, 'inProgress', 'halted', 'Draft', 'completed ', ['draft']]:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_release_status(bad)

    def test_production_url_shape(self):
        for good in ['https://api.rem.com', 'https://rem-backend.up.railway.app', 'https://api.rem.com/v1']:
            self.assertEqual(ar.validate_api_base_url(good), good)
        for bad in ['http://api.rem.com', 'https://staging.invalid', 'https://api.staging.rem.com', 'https://localhost',
                    'https://localhost:3000', 'https://rem.test', 'https://user@api.rem.com', 'https://api.rem.com?x=1',
                    'https://api.rem.com/"; rm', 'https://API.REM.COM', 'https://api.rem.com\n', 'ftp://api.rem.com']:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_api_base_url(bad)

    def test_client_id_cert_and_alias(self):
        self.assertEqual(ar.validate_client_id(CLIENT), CLIENT)
        for bad in ['', 'abc.apps.googleusercontent.com', CLIENT + '\n', CLIENT.upper(), '1-x.apps.googleusercontent.com.evil']:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_client_id(bad)
        self.assertEqual(ar.validate_cert_sha256('AB:' * 31 + 'AB'), 'ab' * 32)
        for bad in ['', 'ab' * 31, 'zz' * 32, None]:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_cert_sha256(bad)
        self.assertEqual(ar.validate_alias('rem-upload'), 'rem-upload')
        for bad in ['', 'a b', '-alias;x', 'x' * 65, None]:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                ar.validate_alias(bad)

    def test_gradle_properties_are_exactly_the_four_reviewed_values(self):
        self.assertEqual(ar.gradle_properties(SHA, '42', API, CLIENT), [
            '-PremApiBaseUrl=' + API, '-PgoogleServerClientId=' + CLIENT, '-PsourceSha=' + SHA, '-PversionCode=42'])
        with self.assertRaises(release.ReleaseError):
            ar.gradle_properties(SHA, '42', 'https://staging.invalid', CLIENT)
        with self.assertRaises(release.ReleaseError):
            ar.gradle_properties('HEAD', '42', API, CLIENT)

    def test_gradle_env_carries_no_credentials(self):
        env = dict(REMCLAW_DEPLOY_KEY='k', REMCLAW_ANDROID_UPLOAD_KEYSTORE_BASE64='k', REMCLAW_PLAY_SERVICE_ACCOUNT_JSON='k',
                   REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD='k', GH_TOKEN='t', GITHUB_TOKEN='t', PATH='/usr/bin')
        with tempfile.TemporaryDirectory() as tmp, patch.dict(os.environ, env, clear=True):
            out = ar.gradle_env(Path(tmp))
        self.assertEqual(set(out), {'PATH', 'GRADLE_USER_HOME', 'GRADLE_OPTS'})
        self.assertTrue(out['GRADLE_USER_HOME'].startswith(tmp))


class AdmissionTests(unittest.TestCase):
    def test_admission_record(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / 'a.json'
            record = ar.admit(SHA, '42', 'internal', out, IDENTITY,
                              check=lambda e: {'model': 'owner-approval', 'reviewer': 'samuelalake'})
            self.assertEqual(json.loads(out.read_text()), record)
            self.assertEqual(record['track'], 'internal')
            self.assertEqual(record['distribution'], 'not_started')
            self.assertEqual(record['release_status'], ar.PLAY_RELEASE_STATUS)

    def test_admission_records_first_upload_draft_and_later_completed(self):
        check = lambda e: {'model': 'owner-approval', 'reviewer': 'samuelalake'}
        with tempfile.TemporaryDirectory() as tmp:
            for status in ('draft', 'completed'):
                with self.subTest(status=status):
                    out = Path(tmp) / f'{status}.json'
                    record = ar.admit(SHA, '42', 'internal', out, IDENTITY, check=check, release_status=status)
                    self.assertEqual(record['release_status'], status)
                    self.assertEqual(record['track'], 'internal')  # the status never changes the track
            with self.assertRaises(release.ReleaseError):
                ar.admit(SHA, '42', 'internal', Path(tmp) / 'bad.json', IDENTITY, check=check, release_status='inProgress')
            self.assertFalse((Path(tmp) / 'bad.json').exists())

    def test_bad_inputs_rejected_before_reading_github(self):
        calls = []
        for args, env in [((SHA, '42', 'production'), IDENTITY), ((SHA, '0', 'internal'), IDENTITY),
                          (('main', '42', 'internal'), IDENTITY),
                          ((SHA, '42', 'internal'), dict(IDENTITY, GITHUB_REF='refs/heads/feature')),
                          ((SHA, '42', 'internal'), dict(IDENTITY, GITHUB_EVENT_NAME='push'))]:
            with self.subTest(args=args), tempfile.TemporaryDirectory() as tmp, self.assertRaises(release.ReleaseError):
                ar.admit(*args, Path(tmp) / 'a.json', env, check=lambda e: calls.append(e))
        self.assertEqual(calls, [])

    def test_environment_is_remclaw_android_release(self):
        with patch.object(ar.gate, 'gh', return_value=OWNER_ENV) as gh, \
             patch.object(ar.gate, 'paged', return_value=MAIN_ONLY) as paged:
            self.assertEqual(ar.check_environment(IDENTITY), {'model': 'owner-approval', 'reviewer': 'samuelalake'})
        self.assertEqual(gh.call_args[0][0], 'repos/Rem-Assistant/Rem-Design-System/environments/remclaw-android-release')
        self.assertTrue(paged.call_args[0][0].endswith('remclaw-android-release/deployment-branch-policies'))

    def test_weak_or_unreadable_environment_rejected(self):
        weak = [dict(OWNER_ENV, can_admins_bypass=True), dict(OWNER_ENV, protection_rules=[]),
                dict(OWNER_ENV, deployment_branch_policy=None)]
        for env in weak:
            with self.subTest(env=env), patch.object(ar.gate, 'gh', return_value=env), \
                 patch.object(ar.gate, 'paged', return_value=MAIN_ONLY), \
                 self.assertRaisesRegex(release.ReleaseError, 'remclaw-android-release environment protection rejected'):
                ar.check_environment(IDENTITY)
        with patch.object(ar.gate, 'gh', return_value=OWNER_ENV), \
             patch.object(ar.gate, 'paged', return_value=MAIN_ONLY + [dict(name='release/*', type='branch')]), \
             self.assertRaises(release.ReleaseError):
            ar.check_environment(IDENTITY)
        with patch.object(ar.gate, 'gh', return_value={'protection_rules': [{'type': 'required_reviewers'}]}), \
             patch.object(ar.gate, 'paged', return_value=MAIN_ONLY), self.assertRaises(release.ReleaseError):
            ar.check_environment(IDENTITY)


class DesignSystemFetchTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        base = Path(self.tmp.name)
        self.env = dict(os.environ, GIT_CONFIG_GLOBAL=os.devnull, GIT_CONFIG_NOSYSTEM='1',
                        GIT_AUTHOR_NAME='t', GIT_AUTHOR_EMAIL='t@example.invalid',
                        GIT_COMMITTER_NAME='t', GIT_COMMITTER_EMAIL='t@example.invalid')
        self.ds = base / 'ds'
        (self.ds / 'compose/RemDesignSystem').mkdir(parents=True)
        (self.ds / 'compose/RemDesignSystem/x').write_text('x')
        git(base, 'init', '-q', str(self.ds), env=self.env)
        git(self.ds, 'add', '.', env=self.env)
        git(self.ds, 'commit', '-qm', 'ds', env=self.env)
        git(self.ds, 'config', 'uploadpack.allowAnySHA1InWant', 'true', env=self.env)
        self.ds_sha = git(self.ds, 'rev-parse', 'HEAD', env=self.env)
        self.source = base / 'remclaw'
        (self.source / 'android').mkdir(parents=True)
        self.log = base / 'fetch.log'

    def tearDown(self):
        self.tmp.cleanup()

    def test_fetches_exact_locked_commit_anonymously(self):
        (self.source / 'android/design-system.lock').write_text(self.ds_sha + '\n')
        with patch.object(ar, 'DESIGN_SYSTEM_URL', 'file://' + str(self.ds)):
            self.assertEqual(ar.fetch_design_system(self.source, self.log), self.ds_sha)
        self.assertEqual(git(self.source / 'android/.design-system', 'rev-parse', 'HEAD', env=self.env), self.ds_sha)
        with patch.object(ar, 'DESIGN_SYSTEM_URL', 'file://' + str(self.ds)), \
             self.assertRaisesRegex(release.ReleaseError, 'reuse'):
            ar.fetch_design_system(self.source, self.log)

    def test_missing_or_malformed_lock_or_unknown_commit(self):
        with self.assertRaisesRegex(release.ReleaseError, 'lock is missing'):
            ar.design_system_lock(self.source)
        for bad in ['main', self.ds_sha[:39], self.ds_sha.upper()]:
            (self.source / 'android/design-system.lock').write_text(bad)
            with self.subTest(bad=bad), self.assertRaisesRegex(release.ReleaseError, '40-character'):
                ar.design_system_lock(self.source)
        (self.source / 'android/design-system.lock').write_text('c' * 40)
        with patch.object(ar, 'DESIGN_SYSTEM_URL', 'file://' + str(self.ds)), self.assertRaises(release.StageFailed):
            ar.fetch_design_system(self.source, self.log)

    def test_anonymous_env_drops_tokens_and_keys(self):
        with patch.dict(os.environ, dict(GH_TOKEN='t', REMCLAW_DEPLOY_KEY='k', GIT_SSH_COMMAND='ssh -i k', PATH='/bin')):
            env = ar.anonymous_git_env()
        self.assertNotIn('GH_TOKEN', env)
        self.assertNotIn('REMCLAW_DEPLOY_KEY', env)
        self.assertNotIn('GIT_SSH_COMMAND', env)
        self.assertEqual(env['GIT_TERMINAL_PROMPT'], '0')
        self.assertEqual(env['GIT_CONFIG_GLOBAL'], os.devnull)

    def test_fetch_reuses_ios_deploy_key_fetch_then_records_design_system(self):
        private = Path(self.tmp.name) / 'private'
        private.mkdir()
        def ios_fetch(sha, dest, environ):
            (private / 'checkout.json').write_text(json.dumps({'remclaw_sha': sha, 'release_branch': 'main'}))
            return 'main'
        with patch.dict(os.environ, {'REMCLAW_PRIVATE': str(private)}), \
             patch.object(ar.release, 'fetch', side_effect=ios_fetch) as shared, \
             patch.object(ar, 'fetch_design_system', return_value=self.ds_sha):
            self.assertEqual(ar.fetch(SHA, self.source, {'REMCLAW_DEPLOY_KEY': 'k'}), 'main')
        shared.assert_called_once()
        self.assertEqual(json.loads((private / 'checkout.json').read_text()),
                         {'remclaw_sha': SHA, 'release_branch': 'main', 'design_system_sha': self.ds_sha})


class BundleTests(unittest.TestCase):
    def test_manifest_accepts_release_shape(self):
        self.assertEqual(ar.android_metadata(manifest(), '42'), '0.1.0')
        self.assertEqual(ar.android_metadata(manifest(app_attrs='android:debuggable="false"'), '42'), '0.1.0')

    def test_manifest_rejections(self):
        cases = {
            'package ID': manifest(package='com.remapp.rem.debug'),
            'version code': manifest(code='41'),
            'version name': manifest(name='0.1.0-debug'),
            'Debuggable': manifest(app_attrs='android:debuggable="true"'),
            'Test-only': manifest(app_attrs='android:testOnly="true"'),
            'parsed': '<manifest',
            'application': manifest().replace('<application android:label="@string/app_name" />', ''),
        }
        for message, xml in cases.items():
            with self.subTest(message=message), self.assertRaisesRegex(release.ReleaseError, message):
                ar.android_metadata(xml, '42')

    def aab(self, path, entries):
        with zipfile.ZipFile(path, 'w') as z:
            for name, data in entries.items():
                z.writestr(name, data)
        return path

    def test_dex_stamps(self):
        stamped = b'\x00' + SHA.encode() + b'\x00' + API.encode() + b'\x00' + CLIENT.encode()
        with tempfile.TemporaryDirectory() as tmp:
            good = self.aab(Path(tmp) / 'g.aab', {'base/dex/classes.dex': b'dex\n' + b'x' * 10,
                                                  'base/dex/classes2.dex': stamped, 'base/manifest/AndroidManifest.xml': b''})
            ar.dex_stamps(good, SHA, API, CLIENT)
            for name, entries, message in [
                    ('nosha', {'base/dex/classes.dex': stamped.replace(SHA.encode(), b'c' * 40)}, 'source stamp'),
                    ('stage', {'base/dex/classes.dex': stamped.replace(API.encode(), b'https://staging.invalid')}, 'production backend'),
                    ('noid', {'base/dex/classes.dex': stamped.replace(CLIENT.encode(), b'')}, 'client ID'),
                    ('feature', {'feature/dex/classes.dex': stamped}, 'no base dex'),
                    ('unsafe', {'base/dex/classes.dex': stamped, '../evil': b''}, 'Unsafe')]:
                with self.subTest(name=name), self.assertRaisesRegex(release.ReleaseError, message):
                    ar.dex_stamps(self.aab(Path(tmp) / (name + '.aab'), entries), SHA, API, CLIENT)

    def test_signature_must_be_one_signer_with_the_upload_cert_covering_every_entry(self):
        cert = b'0\x82CERT-DER-BYTES'
        mf = 'Manifest-Version: 1.0\r\n\r\nName: base/dex/classes.dex\r\nSHA-256-Digest: x\r\n\r\n' \
             'Name: base/manifest/AndroidManifest.xm\r\n l\r\nSHA-256-Digest: y\r\n\r\n'
        base = {'base/dex/classes.dex': b'd', 'base/manifest/AndroidManifest.xml': b'm', 'META-INF/MANIFEST.MF': mf,
                'META-INF/UPLOAD.SF': b'sf'}
        with tempfile.TemporaryDirectory() as tmp:
            good = self.aab(Path(tmp) / 'g.aab', dict(base, **{'META-INF/UPLOAD.RSA': b'pkcs7' + cert + b'end'}))
            ar.check_signature(good, cert)
            with self.assertRaisesRegex(release.ReleaseError, 'configured upload key'):
                ar.check_signature(good, b'0\x82OTHER-CERT')
            with self.assertRaisesRegex(release.ReleaseError, 'configured upload key'):
                ar.check_signature(good, b'')
            two = self.aab(Path(tmp) / 't.aab', dict(base, **{'META-INF/UPLOAD.RSA': cert, 'META-INF/OTHER.EC': cert}))
            with self.assertRaisesRegex(release.ReleaseError, 'exactly one'):
                ar.check_signature(two, cert)
            none = self.aab(Path(tmp) / 'n.aab', base)
            with self.assertRaisesRegex(release.ReleaseError, 'exactly one'):
                ar.check_signature(none, cert)
            extra = self.aab(Path(tmp) / 'e.aab', dict(base, **{'META-INF/UPLOAD.RSA': cert, 'base/res/injected': b'x'}))
            with self.assertRaisesRegex(release.ReleaseError, 'not covered'):
                ar.check_signature(extra, cert)

    def test_keystore_material_is_0600_and_rejects_bad_base64(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'upload.p12'
            ar.material('K', path, {'K': base64.b64encode(b'keystore').decode()})
            self.assertEqual(path.read_bytes(), b'keystore')
            self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
            for env, message in [({}, 'Missing'), ({'K': 'not base64!'}, 'not valid base64'), ({'K': ''}, 'Missing')]:
                with self.subTest(env=env), self.assertRaisesRegex(release.ReleaseError, message):
                    ar.material('K', Path(tmp) / 'x.p12', env)

    def test_sign_rejects_missing_configuration_and_never_leaves_the_key(self):
        with tempfile.TemporaryDirectory() as tmp:
            private, out = Path(tmp) / 'private', Path(tmp) / 'out'
            private.mkdir()
            out.mkdir()
            (out / 'packaging.json').write_text(json.dumps({'remclaw_sha': SHA, 'version_code': '42'}))
            (out / 'RemClaw.aab').write_bytes(b'aab')
            env = dict(REMCLAW_ANDROID_UPLOAD_CERT_SHA256='ab' * 32, REMCLAW_ANDROID_UPLOAD_KEY_ALIAS='rem-upload',
                       REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD='pw',
                       REMCLAW_ANDROID_UPLOAD_KEYSTORE_BASE64=base64.b64encode(b'ks').decode())
            with patch.dict(os.environ, {'REMCLAW_PRIVATE': str(private)}), \
                 patch.object(ar.storage, 'require_free'):
                for drop in ['REMCLAW_ANDROID_UPLOAD_CERT_SHA256', 'REMCLAW_ANDROID_UPLOAD_KEY_ALIAS',
                             'REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD']:
                    partial = {k: v for k, v in env.items() if k != drop}
                    with self.subTest(drop=drop), self.assertRaisesRegex(release.ReleaseError, drop):
                        ar.sign(out, SHA, '42', partial)
                with self.assertRaisesRegex(release.ReleaseError, 'Packaging record'):
                    ar.sign(out, SHA, '43', env)
                # Wrong upload identity: keytool output hashes differently. The key file is removed regardless.
                with patch.object(ar.release, 'captured', return_value=b'other-cert'), \
                     self.assertRaisesRegex(release.ReleaseError, 'upload identity'):
                    ar.sign(out, SHA, '42', env)
                self.assertFalse((private / 'signing').exists())
                # Signing tool failure also removes the key.
                cert = b'cert'
                env['REMCLAW_ANDROID_UPLOAD_CERT_SHA256'] = hashlib.sha256(cert).hexdigest()
                with patch.object(ar.release, 'captured', return_value=cert), \
                     patch.object(ar.release, 'quiet', side_effect=release.StageFailed('sign', private / 'sign.log')), \
                     self.assertRaises(release.StageFailed):
                    ar.sign(out, SHA, '42', env)
                self.assertFalse((private / 'signing').exists())


class SummaryTests(unittest.TestCase):
    approval = {'model': 'owner-approval', 'reviewer': 'samuelalake'}

    def test_only_allowlisted_fields_survive(self):
        record = ar.public_record({'remclaw_sha': SHA, 'version_code': '42', 'track': 'internal', 'release_branch': 'main',
                                   'approval': self.approval, 'distribution': 'published_internal',
                                   'design_system_sha': 'c' * 40, 'binary_sha256': 'd' * 64, 'upload_cert_sha256': 'e' * 64,
                                   'version_name': '0.1.0', 'service_account': 'x@y'}, 'UPLOAD')
        self.assertEqual(record, {'remclaw_sha': SHA, 'release_branch': 'main', 'approval': self.approval,
                                  'version_code': '42', 'track': 'internal', 'distribution': 'published_internal',
                                  'failure_class': 'UPLOAD'})

    def test_draft_release_summary(self):
        record = ar.public_record({'remclaw_sha': SHA, 'version_code': '1', 'track': 'internal', 'release_branch': 'main',
                                   'approval': self.approval, 'release_status': 'draft', 'distribution': 'draft_internal'})
        self.assertEqual(record['release_status'], 'draft')
        self.assertEqual(record['distribution'], 'draft_internal')

    def test_injected_values_rejected(self):
        for change in [dict(track='production'), dict(distribution='MARKER'), dict(version_code='0'),
                       dict(release_status='MARKER'), dict(release_status='inProgress'),
                       dict(release_branch='claude/android-app'), dict(remclaw_sha='MARKER'),
                       dict(approval={'model': 'owner-approval', 'reviewer': 'MARKER'})]:
            data = dict({'remclaw_sha': SHA, 'version_code': '42', 'track': 'internal'}, **change)
            with self.subTest(change=change), self.assertRaises(release.ReleaseError):
                ar.public_record(data)

    def test_summary_text(self):
        with tempfile.TemporaryDirectory() as tmp:
            good = Path(tmp) / 'a.json'
            good.write_text(json.dumps({'remclaw_sha': SHA, 'version_code': '42', 'track': 'internal',
                                        'approval': self.approval, 'distribution': 'not_started'}))
            bad = Path(tmp) / 'b.json'
            bad.write_text(json.dumps({'remclaw_sha': SHA, 'track': 'MARKER'}))
            failure = Path(tmp) / 'failure'
            failure.write_text('MARKER\n')
            text = ar.summary_text([good, Path(tmp) / 'missing.json'], failure)
            self.assertIn('"failure_class": "UNKNOWN"', text)
            self.assertIn('RemClaw Android release', text)
            self.assertNotIn('MARKER', text)
            self.assertNotIn('MARKER', ar.summary_text([good, bad], None))
            self.assertIn('suppressed', ar.summary_text([good, bad], None))
            self.assertEqual(ar.summary_text([Path(tmp) / 'none.json'], None), 'No verified RemClaw Android release result.')


class CleanupTests(unittest.TestCase):
    def test_cleanup_removes_runner_temp_items_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            for name in release.RUNNER_TEMP_NAMES:
                if name.endswith('.json'):
                    (base / name).write_text('{}')
                else:
                    (base / name / 'nested').mkdir(parents=True)
            (base / 'remclaw-ephemeral/gradle-home/caches').mkdir(parents=True)
            (base / 'keep').write_text('x')
            with patch.dict(os.environ, {'RUNNER_TEMP': tmp}):
                ar.cleanup(base)
            self.assertEqual(sorted(p.name for p in base.iterdir()), ['keep'])
            with patch.dict(os.environ, {'RUNNER_TEMP': tmp}), self.assertRaises(release.ReleaseError):
                ar.cleanup(base / 'keep')


class PublicOutputTests(unittest.TestCase):
    def run_cli(self, *args, env=None):
        script = Path(ar.__file__)
        return subprocess.run([sys.executable, str(script), *args], capture_output=True, text=True,
                              env=dict(os.environ, **IDENTITY, **(env or {})))

    def test_cli_failures_print_fixed_messages_only(self):
        with tempfile.TemporaryDirectory() as tmp:
            for args in (['admit', '--sha', 'MARKER' * 7, '--version-code', '42', '--track', 'internal', '--out', tmp + '/a'],
                         ['admit', '--sha', SHA, '--version-code', 'MARKER', '--track', 'internal', '--out', tmp + '/a'],
                         ['admit', '--sha', SHA, '--version-code', '42', '--track', 'MARKERproduction', '--out', tmp + '/a']):
                with self.subTest(args=args):
                    p = self.run_cli(*args)
                    self.assertNotEqual(p.returncode, 0)
                    self.assertTrue(p.stderr.startswith('RemClaw Android release rejected: '), p.stderr)
                    self.assertNotIn('MARKER', p.stdout + p.stderr)

    def test_build_and_sign_refuse_outside_hosted_runner(self):
        p = self.run_cli('build', '--source', '/x', '--out', '/y', '--sha', SHA, '--version-code', '42',
                         env={'GITHUB_ACTIONS': 'false'})
        self.assertIn('Hosted runner only', p.stderr)

    def test_stage_failure_prints_classification_not_log(self):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / 'build.log'
            log.write_text('e: file:///x/MARKER_PRIVATE.kt:1:1 Unresolved reference\n')
            with patch.dict(os.environ, {'REMCLAW_PRIVATE': tmp}), \
                 patch.object(ar, 'main', side_effect=release.StageFailed('build', log)), \
                 patch('builtins.print') as printed, self.assertRaises(SystemExit) as raised:
                ar.entry([])
            out = ' '.join(str(a) for c in printed.call_args_list for a in c.args)
            self.assertTrue(out.startswith('classification=COMPILE '))
            self.assertNotIn('MARKER', out + str(raised.exception))
            self.assertEqual((Path(tmp) / 'failure_class').read_text(), 'COMPILE\n')

    def test_unexpected_exception_text_is_never_printed(self):
        with tempfile.TemporaryDirectory() as tmp:
            with patch.dict(os.environ, {'REMCLAW_PRIVATE': tmp}), \
                 patch.object(ar, 'main', side_effect=OSError('/home/runner/PRIVATE_PATH_MARKER')), \
                 self.assertRaises(SystemExit) as raised:
                ar.entry([])
            self.assertNotIn('MARKER', str(raised.exception))
            self.assertEqual((Path(tmp) / 'failure_class').read_text(), 'UNKNOWN\n')


class AndroidClassifierTests(unittest.TestCase):
    def test_android_and_play_fixtures(self):
        fixtures = Path(__file__).resolve().parent / 'fixtures'
        expected = {'android-dependency': 'DEPENDENCY', 'android-compile': 'COMPILE', 'android-signing': 'SIGNING',
                    'play-auth': 'AUTH', 'play-upload': 'UPLOAD'}
        for name, code in expected.items():
            with self.subTest(name=name):
                self.assertEqual(ar.classify_failure.classify_files([fixtures / f'{name}.log'])[0], code)


if __name__ == '__main__':
    unittest.main()
