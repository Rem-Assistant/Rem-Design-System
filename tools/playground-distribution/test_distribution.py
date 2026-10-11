import copy
from datetime import datetime, timedelta
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import tempfile
import sys
import unittest
from unittest.mock import patch
import zipfile

import gate
import package
import storage
import summary

SHA = 'a' * 40
URL = 'https://github.com/Rem-Assistant/Rem-Design-System/pull/87#issuecomment-123'


class AdmissionTests(unittest.TestCase):
    def setUp(self):
        self.run = dict(id=123, repository={'full_name': gate.REPO}, head_repository={'full_name': gate.REPO},
                        head_sha=SHA, path=gate.WORKFLOW, event='workflow_dispatch', status='completed',
                        conclusion='success', run_attempt=2)
        self.jobs = [dict(name=n, status='completed', conclusion='success')
                     for n in ('preflight', 'ios', 'android', 'native-evidence')]
        self.record = dict(source_sha=SHA, platform='ios', status='passed', unfiltered=True,
                           passed_methods=['Tests/testOne'], passed_count=1, features=['settings'],
                           execution=dict(provider='github_actions', workflow_sha=SHA, event_name='workflow_dispatch',
                                          run_id='123', run_attempt='2', event_sha=SHA))

    def test_exact_run_and_receipt(self):
        gate.validate_run(self.run, self.jobs, SHA, '123')
        gate.validate_receipt(self.record, self.run, SHA, 'ios', {'Tests/testOne'}, ['settings'])

    def test_main_accepts_canonical_or_lowercase_repository_and_rejects_foreign(self):
        with tempfile.TemporaryDirectory() as tmp:
            for repository in (gate.REPO, gate.REPO.lower(), 'foreign/Rem-Design-System'):
                out = Path(tmp) / 'admission.json'
                argv = ['gate', '--source', tmp, '--sha', SHA, '--run', '123', '--build', '5',
                        '--platform', 'ios', '--out', str(out)]
                env = dict(GITHUB_REPOSITORY=repository, GITHUB_REF='refs/heads/main',
                           GITHUB_EVENT_NAME='workflow_dispatch', PLAYGROUND_VISUAL_REVIEW_URL=URL)
                with patch.object(sys, 'argv', argv), patch.dict(os.environ, env, clear=True), \
                     patch.object(gate, 'verify', return_value={}) as verify:
                    if repository.startswith('foreign/'):
                        with self.assertRaises(ValueError):
                            gate.main()
                        verify.assert_not_called()
                    else:
                        gate.main()
                        verify.assert_called_once()
                        self.assertEqual(json.loads(out.read_text())['visual_review_url'], URL)

    def test_run_repository_case_matches_github_identity(self):
        run = dict(self.run, repository={'full_name': gate.REPO.lower()}, head_repository={'full_name': gate.REPO.upper()})
        gate.validate_run(run, self.jobs, SHA, '123')

    def test_inputs_reject_refs_and_injection(self):
        for sha, run, build in [('main', '123', '5'), (SHA, '1;echo', '5'), (SHA, '1', '0'),
                                (SHA, '1', '1000000000'), (SHA.upper(), '1', '5')]:
            with self.subTest(sha=sha, run=run, build=build), self.assertRaises(ValueError):
                gate.arguments(sha, run, build)
        self.assertEqual(gate.visual_review(URL), URL)
        self.assertEqual(gate.visual_review(URL.lower()), URL.lower())
        for url in [URL + '\nsecret', URL.replace('github.com', 'github.com.evil.test'),
                    URL.replace('/Rem-Design-System/', '/other/'), URL.split('#')[0],
                    URL.replace('github.com', 'gıthub.com'), URL.replace('System', 'ſystem')]:
            with self.assertRaises(ValueError):
                gate.visual_review(url)

    def test_stale_foreign_failed_or_wrong_workflow_run(self):
        for field, value in [('head_sha', 'b' * 40), ('path', 'other.yml'), ('event', 'push'),
                             ('conclusion', 'failure'), ('head_repository', {'full_name': 'fork/repo'})]:
            run = dict(self.run, **{field: value})
            with self.subTest(field=field), self.assertRaises(ValueError):
                gate.validate_run(run, self.jobs, SHA, '123')
        for jobs in [self.jobs[:-1], self.jobs + [self.jobs[0]],
                     [dict(j, conclusion='skipped') for j in self.jobs]]:
            with self.assertRaises(ValueError):
                gate.validate_run(self.run, jobs, SHA, '123')

    def test_sharded_ios_jobs(self):
        # Shape of native run 38075668310 (exact head c23521b), which the unsharded check rejected.
        shard = lambda n, c='success': dict(name=n, status='completed', conclusion=c)
        base = [j for j in self.jobs if j['name'] != 'ios']
        sharded = base + [shard('ios (0)'), shard('ios (1)'), shard('ios (2)'), shard('ios-evidence')]
        gate.validate_run(self.run, sharded, SHA, '123')
        for jobs in [base + [shard('ios (0)'), shard('ios (2)'), shard('ios-evidence')],            # gap
                     base + [shard('ios (0)'), shard('ios (0)'), shard('ios-evidence')],            # duplicate
                     base + [shard('ios (0)'), shard('ios (1)', 'failure'), shard('ios-evidence')],  # failed
                     base + [shard('ios (0)')],                                                        # no evidence
                     base + [shard('ios (0)'), shard('ios-evidence', 'skipped')],                    # skipped evidence
                     self.jobs + [shard('ios (0)'), shard('ios-evidence')]]:                          # mixed forms
            with self.subTest(jobs=[j['name'] for j in jobs]), self.assertRaises(ValueError):
                gate.validate_run(self.run, jobs, SHA, '123')

    def test_partial_filtered_duplicate_or_stale_receipt(self):
        for field, value in [('unfiltered', False), ('passed_count', 2), ('passed_methods', []),
                             ('passed_methods', ['Tests/testOne'] * 2), ('source_sha', 'b' * 40),
                             ('features', ['other'])]:
            record = dict(self.record, **{field: value})
            with self.subTest(field=field), self.assertRaises(ValueError):
                gate.validate_receipt(record, self.run, SHA, 'ios', {'Tests/testOne'}, ['settings'])
        for field, value in [('run_attempt', '1'), ('workflow_sha', 'b' * 40), ('event_sha', 'b' * 40)]:
            record = copy.deepcopy(self.record)
            record['execution'][field] = value
            with self.assertRaises(ValueError):
                gate.validate_receipt(record, self.run, SHA, 'ios', {'Tests/testOne'}, ['settings'])

    def test_archive_digest_and_path(self):
        def archive(name):
            data = io.BytesIO()
            with zipfile.ZipFile(data, 'w') as z:
                z.writestr(name, json.dumps(self.record))
            return data.getvalue()
        def meta(data):
            return dict(expired=False, workflow_run=dict(head_sha=SHA, id=123),
                        digest='sha256:' + hashlib.sha256(data).hexdigest())
        data = archive('ios-native.json')
        self.assertEqual(gate.read_receipt_archive(data, meta(data), SHA, 'ios', self.run), self.record)
        with self.assertRaises(ValueError):
            gate.read_receipt_archive(data + b'x', meta(data), SHA, 'ios', self.run)
        for name in ['../ios-native.json', 'folder/ios-native.json', 'android-native.json']:
            data = archive(name)
            with self.assertRaises(ValueError):
                gate.read_receipt_archive(data, meta(data), SHA, 'ios', self.run)

    def test_environment_must_preexist_and_be_protected(self):
        env = dict(protection_rules=[dict(type='required_reviewers', reviewers=[{'type': 'User',
                   'reviewer': {'id': 62378296, 'login': 'oledibefrancis'}}], prevent_self_review=True)],
                   can_admins_bypass=False, deployment_branch_policy=dict(protected_branches=False, custom_branch_policies=True))
        policies = [dict(name='main', type='branch')]
        self.assertEqual(gate.validate_environment(env, policies, ['samuelalake']),
                         {'model': 'independent', 'reviewer': 'oledibefrancis'})
        for changed in [dict(env, can_admins_bypass=True), dict(env, protection_rules=[]),
                        dict(env, deployment_branch_policy=None)]:
            with self.assertRaises(ValueError):
                gate.validate_environment(changed, policies, ['samuelalake'])
        for changed in [[], [dict(name='*', type='branch')], [dict(name='main', type='tag')]]:
            with self.assertRaises(ValueError):
                gate.validate_environment(env, changed, ['samuelalake'])

    def test_owner_can_approve_own_dispatch_only_with_explicit_narrow_exception(self):
        env = dict(protection_rules=[dict(type='required_reviewers', reviewers=[{'type': 'User',
                   'reviewer': {'id': gate.OWNER_ID, 'login': 'samuelalake'}}], prevent_self_review=False)],
                   can_admins_bypass=False, deployment_branch_policy=dict(protected_branches=False, custom_branch_policies=True))
        policies = [dict(name='main', type='branch')]
        self.assertEqual(gate.validate_environment(env, policies, ['samuelalake']),
                         {'model': 'owner-approval', 'reviewer': 'samuelalake'})
        env['protection_rules'][0]['prevent_self_review'] = True
        with self.assertRaisesRegex(ValueError, 'initiated this run'):
            gate.validate_environment(env, policies, ['samuelalake'])
        with self.assertRaisesRegex(ValueError, 'Missing dispatch identity'):
            gate.validate_environment(env, policies, [None])
        self.assertEqual(gate.validate_environment(env, policies, ['oledibefrancis'])['model'], 'independent')

    def test_reviewer_identity_and_rerun_initiator_are_checked(self):
        env = dict(protection_rules=[dict(type='required_reviewers', reviewers=[{'type': 'User',
                   'reviewer': {'id': 74985099, 'login': 'davidolaniran'}}], prevent_self_review=True)],
                   can_admins_bypass=False, deployment_branch_policy=dict(protected_branches=False, custom_branch_policies=True))
        policies = [dict(name='main', type='branch')]
        gate.validate_environment(env, policies, ['samuelalake'])
        with self.assertRaises(ValueError):
            gate.validate_environment(env, policies, ['samuelalake', 'davidolaniran'])
        env['protection_rules'][0]['prevent_self_review'] = False
        with self.assertRaisesRegex(ValueError, 'limited to Samuel'):
            gate.validate_environment(env, policies, ['samuelalake'])
        env['protection_rules'][0]['reviewers'][0]['reviewer']['id'] = gate.OWNER_ID
        with self.assertRaisesRegex(ValueError, 'verified existing'):
            gate.validate_environment(env, policies, ['samuelalake'])

    def test_source_contract_is_data_not_executed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            p = root / 'tools/playground-delivery/delivery.py'
            p.parent.mkdir(parents=True)
            p.write_text("raise Exception('must not execute')\nFEATURES = ['settings']\n")
            self.assertEqual(gate.declared_features(root), ['settings'])

    def test_missing_environment_prevents_admission(self):
        artifacts = [dict(name=f'{p}-native-{SHA}', id=i, digest='digest') for i, p in enumerate(gate.PLATFORMS)]
        def github(path, raw=False):
            if '/environments/' in path:
                raise ValueError('GitHub read failed; missing environment')
            return b'archive' if raw else self.run
        with patch.object(gate.subprocess, 'check_output', side_effect=[SHA + '\n', b'']), \
             patch.object(gate, 'gh', side_effect=github), \
             patch.object(gate, 'paged', side_effect=[self.jobs, artifacts]), \
             patch.object(gate, 'declared_features', return_value=['settings']), \
             patch.object(gate, 'expected_methods', return_value={'Tests/testOne'}), \
             patch.object(gate, 'validate_receipt'), \
             patch.object(gate, 'read_receipt_archive', return_value=self.record):
            with self.assertRaisesRegex(ValueError, 'missing environment'):
                gate.verify(Path('/unused'), SHA, '123', '5', ['ios'])


class PackagingTests(unittest.TestCase):
    def test_profile_requires_existing_store_identity(self):
        cert = b'test certificate'
        sha = hashlib.sha1(cert).hexdigest()
        p = dict(TeamIdentifier=[package.TEAM], Entitlements={'application-identifier': package.TEAM + '.' + package.IOS_ID,
                    'get-task-allow': False}, ExpirationDate=datetime.now() + timedelta(days=2),
                 DeveloperCertificates=[cert], UUID='12345678-1234-1234-1234-123456789012')
        package.validate_profile(p, sha)
        for changed in [dict(p, ProvisionedDevices=['device']), dict(p, ProvisionsAllDevices=True),
                        dict(p, TeamIdentifier=['OTHER']), dict(p, ExpirationDate=datetime.now() - timedelta(days=1))]:
            with self.assertRaises(ValueError):
                package.validate_profile(changed, sha)
        with self.assertRaises(ValueError):
            package.validate_profile(p, '0' * 40)

    def test_ios_branding_stamp_and_export_compliance(self):
        info = dict(CFBundleIdentifier=package.IOS_ID, RemPlaygroundSourceSHA=SHA, CFBundleVersion='5',
                    CFBundleDisplayName='Rem Playground', CFBundleName='Rem Playground', CFBundleShortVersionString='0.1.0',
                    CFBundleIconName='AppIcon', UISupportedInterfaceOrientations=['UIInterfaceOrientationPortrait'],
                    ITSAppUsesNonExemptEncryption=False)
        info['UISupportedInterfaceOrientations~ipad'] = ['UIInterfaceOrientationPortrait']
        package.ios_metadata(info, SHA, '5')
        for key in info:
            changed = dict(info)
            del changed[key]
            with self.subTest(key=key), self.assertRaises(ValueError):
                package.ios_metadata(changed, SHA, '5')

    def test_android_source_sdk_branding_and_debug_rejected(self):
        xml = f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package.ANDROID_ID}"
          android:versionCode="5" android:versionName="1.0"><uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>
          <application android:label="Rem Playground" android:icon="@drawable/icon" android:debuggable="false">
          <meta-data android:name="RemPlaygroundSourceSHA" android:value="git:{SHA}"/></application></manifest>'''
        package.android_metadata(xml, SHA, '5')
        for old, new in [('"36"', '"34"'), ('"false"', '"true"'), ('git:' + SHA, 'git:' + 'b' * 40),
                         ('Rem Playground', 'Settings Playground'), ('versionCode="5"', 'versionCode="1"')]:
            with self.assertRaises(ValueError):
                package.android_metadata(xml.replace(old, new), SHA, '5')

    def test_public_metadata_excludes_sensitive_fields_and_rejects_injection(self):
        data = dict(source_sha=SHA, native_run_id=123, native_run_attempt=2, build_number='5',
                    distribution='not_started', physical_device='unverified', visual_review_url=URL,
                    store_response='private', credentials='secret', tester_email='private@example.com')
        self.assertNotIn('secret', json.dumps(summary.public_record(data)))
        self.assertNotIn('private', json.dumps(summary.public_record(data)))
        with self.assertRaises(ValueError):
            summary.public_record(dict(data, distribution='```\nprivate'))
        approvals = {'ios': {'model': 'owner-approval', 'reviewer': 'samuelalake'}}
        self.assertEqual(summary.public_record(dict(data, approvals=approvals))['approvals'], approvals)
        with self.assertRaises(ValueError):
            summary.public_record(dict(data, approvals={'ios': {'model': 'owner-approval', 'reviewer': 'davidolaniran'}}))

    def test_failure_metadata_is_allowlisted(self):
        data = dict(source_sha=SHA, native_run_id=123, native_run_attempt=1, build_number='8',
                    distribution='not_started', physical_device='unverified', visual_review_url=URL)
        failure = {'phase': 'group_check', 'guard': 'group_other_access_flag_unknown', 'http_status': None}
        record = summary.public_record(dict(data, failure=failure))
        self.assertEqual(record['failure'], failure)
        self.assertEqual(summary.failure_line(record), 'Distribution state: not_started; stopped in phase '
                         'group_check at guard group_other_access_flag_unknown; HTTP status none')
        self.assertEqual(summary.failure_line(summary.public_record(data)), 'Distribution state: not_started')
        http = dict(failure, phase='apple_key', guard='apple_api_http', http_status=401)
        self.assertIn('HTTP status 401', summary.failure_line(summary.public_record(dict(data, failure=http))))
        for bad in (dict(failure, phase='somewhere'), dict(failure, guard='Apple said: forbidden'),
                    dict(failure, guard='x' * 65), dict(failure, http_status=99), dict(failure, http_status='401'),
                    dict(failure, detail='provider body'), {'phase': 'upload', 'guard': 'x'}):
            with self.assertRaises(ValueError):
                summary.public_record(dict(data, failure=bad))

    def test_upload_failure_codes_and_build_record_are_allowlisted(self):
        data = dict(source_sha=SHA, native_run_id=123, native_run_attempt=1, build_number='8',
                    distribution='upload_started', physical_device='unverified', visual_review_url=URL)
        fastfile = (Path(__file__).parent / 'fastlane' / 'Fastfile').read_text()
        classifier = fastfile[fastfile.index('UPLOAD_GUARD_PATTERNS = ['):fastfile.index('def upload_build_record')]
        codes = set(re.findall(r"'(upload_[a-z0-9_]+)'", classifier))
        self.assertEqual(codes, {'upload_transporter_missing', 'upload_app_not_found', 'upload_build_number_used',
                                 'upload_auth_failed', 'upload_processing_timeout', 'upload_user_error_unclassified',
                                 'upload_altool_format_259', 'upload_asc_transfer_failed',
                                 'upload_ipa_error_unclassified', 'upload_itms_', 'upload_apple_code_m',
                                 'upload_altool_exit_', 'upload_altool_flag_rejected',
                                 'upload_output_encoding_error', 'upload_sdk_too_old'})
        self.assertEqual(set(re.findall(r"'(_errlines_[a-z0-9]+)'", classifier)),
                         {'_errlines_1', '_errlines_2to5', '_errlines_6plus'})
        codes = (codes - {'upload_itms_', 'upload_apple_code_m', 'upload_altool_exit_'}) | {
            'upload_itms_90062', 'upload_itms_90186_90062_04238', 'upload_apple_code_m19209',
            'upload_altool_exit_1', 'upload_altool_exit_m1', 'upload_altool_exit_m12345',
            'upload_altool_exit_999999', 'upload_altool_exit_1_errlines_1', 'upload_altool_exit_m1_errlines_2to5',
            'upload_altool_exit_m12345_errlines_6plus', 'upload_ipa_error_unclassified_errlines_1',
            'upload_asc_transfer_failed_errlines_2to5', 'upload_user_error_unclassified_errlines_6plus'}
        for code in sorted(codes):
            for value in summary.UPLOAD_BUILD_RECORDS:
                with self.subTest(code=code, record=value):
                    failure = {'phase': 'upload', 'guard': code, 'http_status': None, 'upload_build_record': value}
                    record = summary.public_record(dict(data, failure=failure))
                    self.assertEqual(record['failure'], failure)
                    note = '; next step: reconcile the existing build, never re-upload' if value == 'present' else ''
                    self.assertEqual(summary.failure_line(record), 'Distribution state: upload_started; stopped in '
                                     f'phase upload at guard {code}; HTTP status none; build record {value}{note}')
        self.assertEqual(summary.UPLOAD_BUILD_RECORDS, ('present', 'absent', 'unknown'))
        base = {'phase': 'upload', 'guard': 'upload_auth_failed', 'http_status': None}
        self.assertNotIn('build record', summary.failure_line(summary.public_record(dict(data, failure=base))))
        secret = 'Authentication credentials are missing or invalid for team 9ZZ'
        for bad in (dict(base, upload_build_record='maybe'), dict(base, upload_build_record=None),
                    dict(base, upload_build_record=True), dict(base, upload_build_record=['absent']),
                    dict(base, upload_build_record=secret), dict(base, guard=secret),
                    dict(base, phase='group_check', upload_build_record='absent'),
                    dict(base, upload_build_record='absent', message=secret)):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                summary.public_record(dict(data, failure=bad))

    def test_present_build_record_after_failure_says_reconcile_never_reupload(self):
        # altool can report a failure for an upload Apple accepted (fastlane/fastlane#30130).
        data = dict(source_sha=SHA, native_run_id=123, native_run_attempt=1, build_number='8',
                    distribution='upload_started', physical_device='unverified', visual_review_url=URL)
        for value in summary.UPLOAD_BUILD_RECORDS:
            failure = {'phase': 'upload', 'guard': 'upload_asc_transfer_failed', 'http_status': None,
                       'upload_build_record': value}
            line = summary.failure_line(summary.public_record(dict(data, failure=failure)))
            with self.subTest(record=value):
                if value == 'present':
                    self.assertTrue(line.endswith('; build record present; next step: reconcile the existing '
                                                  'build, never re-upload'))
                else:
                    self.assertNotIn('reconcile', line)
                    self.assertTrue(line.endswith(f'; build record {value}'))

    def test_altool_exit_codes_publish_only_fixed_text(self):
        secret = 'Failed to get json from defaults for team 9ZZ secret@example.com'
        data = dict(source_sha=SHA, native_run_id=123, native_run_attempt=1, build_number='8',
                    distribution='upload_started', physical_device='unverified', visual_review_url=URL)
        failure = {'phase': 'upload', 'guard': 'upload_altool_exit_m1_errlines_2to5', 'http_status': None,
                   'upload_build_record': 'absent'}
        record = summary.public_record(dict(data, failure=failure, store_response=secret))
        line = summary.failure_line(record)
        self.assertEqual(line, 'Distribution state: upload_started; stopped in phase upload at guard '
                         'upload_altool_exit_m1_errlines_2to5; HTTP status none; build record absent')
        self.assertNotIn('9ZZ', json.dumps(record) + line)
        # A negative exit status must already be written with m; a raw minus or text is rejected.
        for bad in ('upload_altool_exit_-1', 'upload_altool_exit_1_errlines_' + secret, 'upload_altool_exit_' + 'x' * 60):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                summary.public_record(dict(data, failure=dict(failure, guard=bad)))

    def test_upload_failure_summary_never_carries_provider_text(self):
        secret = 'ITMS-90062: team 9ZZ secret@example.com'
        record = dict(source_sha=SHA, native_run_id=123, native_run_attempt=1, build_number='8',
                      distribution='upload_started', physical_device='unverified', visual_review_url=URL,
                      store_response=secret,
                      failure={'phase': 'upload', 'guard': 'upload_itms_90062', 'http_status': None,
                               'upload_build_record': 'absent'})
        with tempfile.TemporaryDirectory() as tmp:
            state, step = Path(tmp) / 'distribution.json', Path(tmp) / 'summary.md'
            state.write_text(json.dumps(record))
            out = io.StringIO()
            with patch.dict(os.environ, {'GITHUB_STEP_SUMMARY': str(step)}), \
                    patch.object(sys, 'argv', ['summary.py', 'result', str(state)]), patch('sys.stdout', out):
                summary.main()
            self.assertEqual(out.getvalue().strip(), 'Distribution state: upload_started; stopped in phase upload '
                             'at guard upload_itms_90062; HTTP status none; build record absent')
            published = out.getvalue() + step.read_text()
            for leak in ('9ZZ', 'secret@example.com', 'ITMS-90062', 'store_response'):
                self.assertNotIn(leak, published)
            self.assertIn('"upload_build_record": "absent"', step.read_text())
            # A leaked message in any failure field suppresses the whole record rather than publishing it.
            record['failure']['upload_build_record'] = secret
            state.write_text(json.dumps(record))
            out = io.StringIO()
            with patch.dict(os.environ, {'GITHUB_STEP_SUMMARY': str(step)}), \
                    patch.object(sys, 'argv', ['summary.py', 'result', str(state)]), patch('sys.stdout', out):
                summary.main()
            self.assertEqual(out.getvalue(), '')
            self.assertNotIn('9ZZ', step.read_text())


class StorageTests(unittest.TestCase):
    def usage(self, free):
        return lambda path: type('Usage', (), {'free': free})()

    def test_free_space_checked_per_stage_and_fails_closed(self):
        for platform, stages in storage.MIN_FREE.items():
            for stage, needed in stages.items():
                storage.require_free('/', platform, stage, usage=self.usage(needed))
                with self.subTest(platform=platform, stage=stage), self.assertRaisesRegex(ValueError, 'Stopped safely'):
                    storage.require_free('/', platform, stage, usage=self.usage(needed - 1))
        with self.assertRaises(ValueError):
            storage.require_free('/', 'ios', 'deploy', usage=self.usage(10 ** 15))

    def test_output_budget_counts_files_without_following_symlinks(self):
        with tempfile.TemporaryDirectory() as tmp:
            out, outside = Path(tmp) / 'out', Path(tmp) / 'outside.bin'
            out.mkdir()
            outside.write_bytes(b'x' * 4096)
            (out / 'Playground.aab').write_bytes(b'x' * 1000)
            (out / 'link').symlink_to(outside)
            self.assertLess(storage.tree_size(out), 1000 + 4096)
            storage.require_budget(out, 'android', 'sign')
            with patch.dict(storage.OUTPUT_BUDGET, {'android': {'sign': 999}}), self.assertRaisesRegex(ValueError, 'budget'):
                storage.require_budget(out, 'android', 'sign')

    def test_toolchain_and_altool_probe_are_published_in_fixed_shapes(self):
        data = dict(source_sha=SHA, native_run_id=123, native_run_attempt=1, build_number='8', platform='ios',
                    distribution='upload_started', physical_device='unverified', visual_review_url=URL,
                    toolchain={'xcode': '26.3', 'xcode_build': '17C529', 'sdk': 'iphoneos26.2'},
                    altool_old_flag='unsupported',
                    failure={'phase': 'upload', 'guard': 'upload_sdk_too_old', 'http_status': None,
                             'upload_build_record': 'absent'})
        record = summary.public_record(data)
        self.assertEqual(record['toolchain'], data['toolchain'])
        self.assertEqual(summary.failure_line(record), 'Distribution state: upload_started; Xcode 26.3 (17C529), SDK '
                         'iphoneos26.2; old altool flag unsupported; stopped in phase upload at guard upload_sdk_too_old; '
                         'HTTP status none; build record absent')
        refused = dict(data, distribution='not_started', toolchain={'xcode': '16.4', 'xcode_build': '16F6',
                       'sdk': 'iphoneos18.5'}, failure={'phase': 'toolchain', 'guard': 'package_sdk_too_old',
                       'http_status': None})
        del refused['altool_old_flag']
        self.assertEqual(summary.failure_line(summary.public_record(refused)), 'Distribution state: not_started; '
                         'Xcode 16.4 (16F6), SDK iphoneos18.5; stopped in phase toolchain at guard package_sdk_too_old; '
                         'HTTP status none')
        secret = 'team 9ZZ'
        for bad in (dict(data, toolchain={'xcode': '26.3', 'xcode_build': '17C529'}),
                    dict(data, toolchain={'xcode': '26.3 ' + secret, 'xcode_build': '17C529', 'sdk': 'iphoneos26.2'}),
                    dict(data, toolchain={'xcode': '26.3', 'xcode_build': '17C529', 'sdk': 'iphoneos26.2',
                                          'developer_dir': '/Applications/Xcode_26.3.app'}),
                    dict(data, toolchain={'xcode': '26.3', 'xcode_build': '17C529', 'sdk': 'macosx26.2'}),
                    dict(data, toolchain='Xcode 26.3'), dict(data, platform='android'),
                    dict(data, altool_old_flag='yes'), dict(data, altool_old_flag=secret)):
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                summary.public_record(bad)

    def test_completed_requires_a_final_state_without_failure(self):
        # The store step also needs this: under `bundle exec` an Errno::EPIPE escaping fastlane exits 0.
        base = dict(source_sha=SHA, native_run_id=123, native_run_attempt=1, build_number='8',
                    physical_device='unverified', visual_review_url=URL)
        failure = {'phase': 'upload', 'guard': 'upload_apple_code_m19209', 'http_status': None,
                   'upload_build_record': 'absent'}
        cases = [(dict(base, distribution='assigned_existing_internal_group'), 0),
                 (dict(base, distribution='published_existing_internal_track'), 0),
                 (dict(base, distribution='upload_started', failure=failure), 1),
                 (dict(base, distribution='assigned_existing_internal_group', failure=failure), 1),
                 (dict(base, distribution='upload_started'), 1),
                 (dict(base, distribution='processed'), 1),
                 (dict(base, distribution='assigned_existing_internal_group', visual_review_url='x'), 1),
                 (None, 1), ('not json', 1)]
        with tempfile.TemporaryDirectory() as tmp:
            for record, status in cases:
                state = Path(tmp) / 'distribution.json'
                if state.exists():
                    state.unlink()
                if isinstance(record, dict):
                    state.write_text(json.dumps(record))
                elif record is not None:
                    state.write_text(record)
                out = io.StringIO()
                with self.subTest(record=record), patch.object(sys, 'argv', ['summary.py', 'completed', str(state)]), \
                        patch('sys.stdout', out), self.assertRaises(SystemExit) as done:
                    summary.main()
                self.assertEqual(done.exception.code, status)
                self.assertEqual(out.getvalue(), '')

    def test_summary_cleanup_removes_only_task_owned_runner_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            runner_temp, outside = Path(tmp) / 'runner-temp', Path(tmp) / 'outside'
            owned = ('playground-package', 'playground-ephemeral', 'playground-store-tmp')
            for name in owned:
                (runner_temp / name / 'nested').mkdir(parents=True)
                (runner_temp / name / 'nested' / 'Playground.ipa').write_text('ipa')
            (runner_temp / 'playground-store-tmp' / 'd1' / 'x.itmsp').mkdir(parents=True)
            (runner_temp / 'playground-store-tmp' / 'd1' / 'x.itmsp' / 'metadata.xml').write_text('xml')
            (runner_temp / 'playground-store-private.log').write_text('private')
            (runner_temp / 'admission.json').write_text('{}')
            (runner_temp / 'other-step').mkdir()
            (runner_temp / 'other-step' / 'keep').write_text('keep')
            outside.mkdir()
            (outside / 'keep').write_text('keep')
            with patch.dict(os.environ, {'RUNNER_TEMP': str(runner_temp)}), \
                    patch.object(sys, 'argv', ['summary.py', 'cleanup', str(runner_temp)]):
                summary.main()
            self.assertEqual(sorted(p.name for p in runner_temp.iterdir()), ['other-step'])
            self.assertTrue((outside / 'keep').exists())
            with patch.dict(os.environ, {'RUNNER_TEMP': str(runner_temp)}), \
                    patch.object(sys, 'argv', ['summary.py', 'cleanup', str(outside)]), self.assertRaises(ValueError):
                summary.main()
            self.assertTrue((outside / 'keep').exists())

    def test_cleanup_is_bounded_to_its_base(self):
        with tempfile.TemporaryDirectory() as tmp:
            base, outside = Path(tmp) / 'base', Path(tmp) / 'keep'
            (base / 'DerivedData/sub').mkdir(parents=True)
            outside.mkdir()
            (outside / 'file').write_text('keep')
            (base / 'escape').symlink_to(outside)
            storage.remove_within(base, base / 'DerivedData')
            self.assertFalse((base / 'DerivedData').exists())
            storage.remove_within(base, base / 'escape')  # unlinks the link, never its target
            self.assertTrue((outside / 'file').exists())
            storage.remove_within(base, base / 'missing')  # absent intermediates are fine
            for target in (base, Path(tmp) / 'keep', base / '..', base / 'nested/../../keep'):
                with self.subTest(target=str(target)), self.assertRaises(ValueError):
                    storage.remove_within(base, target)
            self.assertTrue((outside / 'file').exists())

    def test_build_output_removal_requires_git_ignored_untracked_path(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            run = lambda *a: __import__('subprocess').run(['git', '-C', tmp, *a], check=True, capture_output=True)
            run('init', '-q')
            (root / '.gitignore').write_text('compose/demo/build/\n')
            (root / 'compose/demo/build/intermediates').mkdir(parents=True)
            (root / 'compose/demo/src').mkdir(parents=True)
            (root / 'compose/demo/src/Main.kt').write_text('tracked')
            run('add', '.gitignore', 'compose/demo/src/Main.kt')
            with self.assertRaisesRegex(ValueError, 'does not ignore'):
                storage.remove_ignored_build_output(root, 'compose/demo/src')
            storage.remove_ignored_build_output(root, 'compose/demo/build')
            self.assertFalse((root / 'compose/demo/build').exists())
            self.assertTrue((root / 'compose/demo/src/Main.kt').exists())
            storage.remove_ignored_build_output(root, 'compose/demo/build')  # already gone

    def test_build_refuses_low_storage_or_missing_ephemeral_paths_before_any_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, out = Path(tmp) / 'candidate', Path(tmp) / 'package'
            root.mkdir()
            # Also covers a symlinked temporary path (macOS /var -> /private/var): the candidate is
            # passed through a symlink while the ephemeral path resolves to the real location.
            (Path(tmp) / 'via-link').symlink_to(root)
            with patch.object(storage.shutil, 'disk_usage', self.usage(storage.MIN_FREE['ios']['build'] - 1)), \
                    patch.object(package, 'command') as command, self.assertRaisesRegex(ValueError, 'Insufficient'):
                package.build(root, out, 'ios', SHA, '5')
            command.assert_not_called()
            self.assertFalse(out.exists())
            (root / 'inside').mkdir()
            cases = [(root, ''), (root, str(root / 'inside')), (Path(tmp) / 'via-link', str((root / 'inside').resolve()))]
            for candidate, value in cases:
                with patch.dict(os.environ, {'PLAYGROUND_EPHEMERAL': value}), \
                        patch.object(storage.shutil, 'disk_usage', self.usage(10 ** 15)), \
                        patch.object(package, 'command') as command, self.assertRaisesRegex(ValueError, 'Ephemeral|ephemeral'):
                    package.build(candidate, out, 'ios', SHA, '5')
                command.assert_not_called()
                self.assertFalse(out.exists())


    def test_pinned_xcode_selection_fails_closed(self):
        self.assertEqual(package.XCODE_APP, 'Xcode_26.3.app')
        self.assertGreaterEqual(package.xcode_version(package.XCODE_APP)[:2], (26, 1))
        with tempfile.TemporaryDirectory() as tmp:
            apps = Path(tmp)
            for name in ('Xcode_16.4.app', 'Xcode_26.2.app', 'Xcode_26.4_beta.app'):
                (apps / name / 'Contents/Developer').mkdir(parents=True)
            with self.assertRaisesRegex(ValueError, 'not installed'):
                package.select_xcode(apps)  # never falls back to the default 16.4 or a nearby version
            (apps / 'Xcode_26.3.app/Contents/Developer').mkdir(parents=True)
            self.assertEqual(package.select_xcode(apps), apps / 'Xcode_26.3.app/Contents/Developer')
        with patch.object(package, 'XCODE_APP', 'Xcode_26.0.1.app'), self.assertRaisesRegex(ValueError, 'minimum'):
            package.select_xcode(Path('/Applications'))

    def test_xcode_toolchain_reads_only_fixed_shapes(self):
        def run(version, sdk):
            outputs = iter([version.encode(), sdk.encode()])
            with patch.object(package, 'command', side_effect=lambda argv, **_: next(outputs)):
                return package.xcode_toolchain(Path('/Applications/Xcode_26.3.app/Contents/Developer'))
        self.assertEqual(run('Xcode 26.3\nBuild version 17C529\n', '26.2\n'),
                         {'xcode': '26.3', 'xcode_build': '17C529', 'sdk': 'iphoneos26.2',
                          'developer_dir': '/Applications/Xcode_26.3.app/Contents/Developer'})
        for version, sdk, error in [('Xcode 16.4\nBuild version 16F6\n', '18.5', 'pinned'),
                                    ('Xcode 26.2\nBuild version 17C52\n', '26.2', 'pinned'),
                                    ('Xcode 26.3\nBuild version 17C529\n', '18.5', 'iOS 26 SDK'),
                                    ('Xcode 26.3 team 9ZZ\nBuild version 17C529\n', '26.2', 'Unexpected'),
                                    ('Xcode 26.3\nBuild version 17C529\n', '26.2 secret', 'iOS 26 SDK')]:
            with self.subTest(version=version, sdk=sdk), self.assertRaisesRegex(ValueError, error):
                run(version, sdk)

    def test_archive_must_come_from_the_selected_xcode_26(self):
        xcode = {'xcode': '26.3', 'xcode_build': '17C529', 'sdk': 'iphoneos26.2'}
        good = {'DTXcode': '2630', 'DTXcodeBuild': '17C529', 'DTSDKName': 'iphoneos26.2', 'DTPlatformVersion': '26.2'}
        package.archived_toolchain(good, xcode)
        for change, error in [({'DTXcode': '1640', 'DTPlatformVersion': '18.5'}, 'older than 26'),
                              ({'DTPlatformVersion': '18.5'}, 'older than 26'),
                              ({'DTXcode': None}, 'lacks'), ({'DTPlatformVersion': None}, 'lacks'),
                              ({'DTXcodeBuild': '16F6'}, 'not built by'), ({'DTSDKName': 'iphoneos26.1'}, 'not built by')]:
            info = {k: v for k, v in dict(good, **change).items() if v is not None}
            with self.subTest(change=change), self.assertRaisesRegex(ValueError, error):
                package.archived_toolchain(info, xcode)

    def test_ios_build_requires_the_selected_xcode_first(self):
        with tempfile.TemporaryDirectory() as tmp:
            root, out, scratch = Path(tmp) / 'candidate', Path(tmp) / 'package', Path(tmp) / 'scratch'
            (root / 'tools/playground-ios/RemSettingsPlayground.xcodeproj').mkdir(parents=True)
            scratch.mkdir()
            for developer in ('', '/Applications/Xcode_16.4.app/Contents/Developer', '/x'):
                with self.subTest(developer=developer), \
                        patch.dict(os.environ, {'PLAYGROUND_EPHEMERAL': str(scratch), 'DEVELOPER_DIR': developer}), \
                        patch.object(storage.shutil, 'disk_usage', self.usage(10 ** 15)), \
                        patch.object(package, 'command') as command, self.assertRaises((ValueError, IndexError)):
                    package.build(root, out, 'ios', SHA, '5')
                command.assert_not_called()
                shutil.rmtree(out, ignore_errors=True)


if __name__ == '__main__':
    unittest.main()
