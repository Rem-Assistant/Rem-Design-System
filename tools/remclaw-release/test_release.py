from contextlib import redirect_stdout
from datetime import datetime, timedelta, timezone
import io
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

import release

SHA = 'a' * 40
OWNER_ENV = dict(protection_rules=[dict(type='required_reviewers', reviewers=[{'type': 'User',
                 'reviewer': {'id': 58840187, 'login': 'samuelalake'}}], prevent_self_review=False)],
                 can_admins_bypass=False, deployment_branch_policy=dict(protected_branches=False, custom_branch_policies=True))
MAIN_ONLY = [dict(name='main', type='branch')]
IDENTITY = dict(GITHUB_REPOSITORY='Rem-Assistant/Rem-Design-System', GITHUB_REF='refs/heads/main',
                GITHUB_EVENT_NAME='workflow_dispatch', GITHUB_ACTOR='samuelalake', GITHUB_TRIGGERING_ACTOR='samuelalake')


def git(root, *args, env=None):
    return subprocess.run(['git', '-C', str(root), *args], check=True, capture_output=True, text=True, env=env).stdout.strip()


class InputTests(unittest.TestCase):
    def test_sha_must_be_full_lowercase_hex(self):
        self.assertEqual(release.validate_sha(SHA), SHA)
        for bad in ['main', SHA.upper(), SHA[:39], SHA + 'a', SHA[:39] + 'g', SHA + '\n', f'{SHA[:20]};{SHA[:19]}',
                    'refs/heads/main', '', None, 'HEAD~1' + SHA[:34], ' ' + SHA[:39]]:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                release.validate_sha(bad)

    def test_build_number(self):
        self.assertEqual(release.validate_build('42'), '42')
        for bad in ['0', '01', '-1', '1e3', '1000000000', '', '4 2', None]:
            with self.subTest(bad=bad), self.assertRaises(release.ReleaseError):
                release.validate_build(bad)

    def test_run_identity_main_dispatch_only(self):
        release.run_identity(IDENTITY)
        release.run_identity(dict(IDENTITY, GITHUB_REPOSITORY='rem-assistant/rem-design-system'))
        for change in [dict(GITHUB_REF='refs/heads/feature'), dict(GITHUB_REF='refs/tags/main'),
                       dict(GITHUB_EVENT_NAME='push'), dict(GITHUB_REPOSITORY='fork/Rem-Design-System')]:
            with self.subTest(change=change), self.assertRaises(release.ReleaseError):
                release.run_identity(dict(IDENTITY, **change))


class BranchAllowlistTests(unittest.TestCase):
    def test_allowed_release_branches_are_an_explicit_constant(self):
        self.assertEqual(release.ALLOWED_RELEASE_BRANCHES, ('main', 'skip-sent-reply-suggestions'))

    def test_first_allowed_branch_containing_sha_wins(self):
        calls = []
        def ancestor(sha, branch):
            calls.append(branch)
            return branch == 'skip-sent-reply-suggestions'
        self.assertEqual(release.release_branch(SHA, ancestor), 'skip-sent-reply-suggestions')
        self.assertEqual(calls, ['main', 'skip-sent-reply-suggestions'])
        self.assertEqual(release.release_branch(SHA, lambda s, b: True), 'main')

    def test_sha_outside_allowed_history_or_altered_list_rejected(self):
        with self.assertRaisesRegex(release.ReleaseError, 'allowed release branch'):
            release.release_branch(SHA, lambda s, b: False)
        with self.assertRaisesRegex(release.ReleaseError, 'fixed in code'):
            release.release_branch(SHA, lambda s, b: True, branches=('main', 'staging'))


class CheckoutTests(unittest.TestCase):
    """Real git repositories: ancestry against refs/remotes/origin/<allowed branch>."""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name) / 'repo'
        self.env = dict(os.environ, GIT_CONFIG_GLOBAL=os.devnull, GIT_CONFIG_NOSYSTEM='1',
                        GIT_AUTHOR_NAME='t', GIT_AUTHOR_EMAIL='t@example.invalid',
                        GIT_COMMITTER_NAME='t', GIT_COMMITTER_EMAIL='t@example.invalid')
        subprocess.run(['git', 'init', '-q', '-b', 'work', str(self.root)], check=True, env=self.env)
        self.commits = {}
        # base -> main_tip on main; base -> side on the release branch; base -> stray elsewhere.
        for name, parent in (('base', None), ('main_tip', 'base'), ('side', 'base'), ('stray', 'base')):
            if parent:
                git(self.root, 'checkout', '-q', '--detach', self.commits[parent], env=self.env)
            (self.root / 'f').write_text(name)
            git(self.root, 'add', 'f', env=self.env)
            git(self.root, 'commit', '-q', '-m', name, env=self.env)
            self.commits[name] = git(self.root, 'rev-parse', 'HEAD', env=self.env)
        git(self.root, 'update-ref', 'refs/remotes/origin/main', self.commits['main_tip'], env=self.env)
        git(self.root, 'update-ref', 'refs/remotes/origin/skip-sent-reply-suggestions', self.commits['side'], env=self.env)
        git(self.root, 'update-ref', 'refs/remotes/origin/staging', self.commits['stray'], env=self.env)

    def tearDown(self):
        self.tmp.cleanup()

    def checkout(self, name):
        git(self.root, 'checkout', '-q', '--detach', self.commits[name], env=self.env)
        return self.commits[name]

    def test_ancestor_of_main_or_release_branch_accepted(self):
        self.assertEqual(release.verify_checkout(self.root, self.checkout('base'), self.env), 'main')
        self.assertEqual(release.verify_checkout(self.root, self.checkout('main_tip'), self.env), 'main')
        self.assertEqual(release.verify_checkout(self.root, self.checkout('side'), self.env), 'skip-sent-reply-suggestions')

    def test_commit_only_on_other_branch_rejected(self):
        with self.assertRaisesRegex(release.ReleaseError, 'allowed release branch'):
            release.verify_checkout(self.root, self.checkout('stray'), self.env)

    def test_head_must_equal_requested_sha(self):
        self.checkout('main_tip')
        with self.assertRaisesRegex(release.ReleaseError, 'HEAD differs'):
            release.verify_checkout(self.root, self.commits['base'], self.env)

    def test_dirty_checkout_rejected(self):
        sha = self.checkout('main_tip')
        (self.root / 'untracked').write_text('x')
        with self.assertRaisesRegex(release.ReleaseError, 'clean'):
            release.verify_checkout(self.root, sha, self.env)

    def test_missing_release_branch_ref_fails_closed(self):
        sha = self.checkout('stray')
        git(self.root, 'update-ref', '-d', 'refs/remotes/origin/main', env=self.env)
        with self.assertRaises(release.ReleaseError):
            release.verify_checkout(self.root, sha, self.env)

    def test_fetch_requires_deploy_key_and_never_reuses_a_checkout(self):
        private = Path(self.tmp.name) / 'private'
        private.mkdir()
        with patch.dict(os.environ, dict(REMCLAW_PRIVATE=str(private)), clear=False):
            with self.assertRaisesRegex(release.ReleaseError, 'REMCLAW_DEPLOY_KEY'):
                release.fetch(SHA, Path(self.tmp.name) / 'new', environ={})
            with self.assertRaisesRegex(release.ReleaseError, 'existing'):
                release.fetch(SHA, self.root, environ={'REMCLAW_DEPLOY_KEY': 'k'})
        self.assertEqual(list(private.iterdir()), [])

    def test_fetch_removes_key_even_when_git_fails_and_pins_host_key(self):
        private = Path(self.tmp.name) / 'private'
        private.mkdir()
        seen = {}
        def failing(argv, log, **kwargs):
            seen.setdefault('env', kwargs.get('env'))
            seen['key_mode'] = oct((private / 'deploy_key').stat().st_mode & 0o777)
            raise release.StageFailed('fetch', log)
        with patch.dict(os.environ, dict(REMCLAW_PRIVATE=str(private))), patch.object(release, 'quiet', side_effect=failing):
            with self.assertRaises(release.StageFailed):
                release.fetch(SHA, Path(self.tmp.name) / 'new', environ={'REMCLAW_DEPLOY_KEY': 'PRIVATE KEY MATERIAL'})
        self.assertFalse((private / 'deploy_key').exists())
        self.assertFalse((private / 'known_hosts').exists())
        self.assertEqual(seen['key_mode'], '0o600')
        ssh = seen['env']['GIT_SSH_COMMAND']
        for option in ('StrictHostKeyChecking=yes', 'IdentitiesOnly=yes', 'BatchMode=yes', 'HostKeyAlgorithms=ssh-ed25519'):
            self.assertIn(option, ssh)
        self.assertNotIn('REMCLAW_DEPLOY_KEY', seen['env'])
        self.assertTrue(release.GITHUB_HOST_KEY.startswith('github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOMqqnkVzrm0SdG6UOoqKLsabgH5C9okWi0dh2l9GKJl'))


class EnvironmentTests(unittest.TestCase):
    def github(self, env=OWNER_ENV, policies=MAIN_ONLY):
        return (patch.object(release.gate, 'gh', return_value=env),
                patch.object(release.gate, 'paged', return_value=policies))

    def test_admission_checks_environment_named_remclaw_ios_release(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / 'admission.json'
            gh, paged = self.github()
            with gh as g, paged as p:
                record = release.admit(SHA, out, IDENTITY)
            g.assert_called_once_with('repos/Rem-Assistant/Rem-Design-System/environments/remclaw-ios-release')
            self.assertIn('/environments/remclaw-ios-release/deployment-branch-policies', p.call_args[0][0])
            self.assertEqual(record['approval'], {'model': 'owner-approval', 'reviewer': 'samuelalake'})
            self.assertEqual(json.loads(out.read_text())['remclaw_sha'], SHA)

    def test_weak_or_missing_environment_rejected_before_any_release(self):
        weak = [dict(OWNER_ENV, can_admins_bypass=True), dict(OWNER_ENV, protection_rules=[]),
                dict(OWNER_ENV, deployment_branch_policy=None)]
        for env in weak:
            gh, paged = self.github(env=env)
            with self.subTest(env=env), gh, paged, self.assertRaises(release.ReleaseError):
                release.check_environment(IDENTITY)
        for policies in ([], [dict(name='*', type='branch')], [dict(name='main', type='tag')]):
            gh, paged = self.github(policies=policies)
            with self.subTest(policies=policies), gh, paged, self.assertRaises(release.ReleaseError):
                release.check_environment(IDENTITY)
        with patch.object(release.gate, 'gh', side_effect=ValueError('GitHub read failed')), \
             self.assertRaisesRegex(release.ReleaseError, 'GitHub read failed'):
            release.check_environment(IDENTITY)

    def test_owner_approval_limited_to_samuel_independent_model_supported(self):
        other = json.loads(json.dumps(OWNER_ENV))
        other['protection_rules'][0]['reviewers'][0]['reviewer'] = {'id': 62378296, 'login': 'oledibefrancis'}
        gh, paged = self.github(env=other)
        with gh, paged, self.assertRaisesRegex(release.ReleaseError, 'limited to Samuel'):
            release.check_environment(IDENTITY)
        other['protection_rules'][0]['prevent_self_review'] = True
        gh, paged = self.github(env=other)
        with gh, paged:
            self.assertEqual(release.check_environment(IDENTITY)['model'], 'independent')

    def test_admission_rejects_bad_sha_before_reading_github(self):
        with patch.object(release.gate, 'gh') as g, self.assertRaises(release.ReleaseError):
            release.admit('main', Path('/nonexistent/x.json'), IDENTITY)
        g.assert_not_called()


class ToolchainTests(unittest.TestCase):
    def test_newest_stable_xcode_26_or_later(self):
        names = ['Xcode_16.4.app', 'Xcode_26.0.1.app', 'Xcode_26.1.app', 'Xcode_26.2_beta.app', 'Xcode.app', 'Xcode_26.0.app']
        self.assertEqual(release.select_xcode(names), 'Xcode_26.1.app')
        with self.assertRaises(release.ReleaseError):
            release.select_xcode(['Xcode_16.4.app', 'Xcode_26.2_beta.app'])


class SigningTests(unittest.TestCase):
    def profile(self, bundle_id, **changes):
        cert = b'certificate'
        entitlements = {'application-identifier': f'{release.TEAM}.{bundle_id}', 'get-task-allow': False,
                        'com.apple.security.application-groups': [release.APP_GROUP]}
        if bundle_id == release.BUNDLE_ID:
            entitlements.update({'aps-environment': 'production', 'com.apple.developer.applesignin': ['Default']})
        p = dict(TeamIdentifier=[release.TEAM], Entitlements=entitlements, DeveloperCertificates=[cert],
                 ExpirationDate=datetime.now(timezone.utc).replace(tzinfo=None) + timedelta(days=30),
                 UUID='12345678-1234-1234-1234-123456789012')
        p.update(changes)
        return p, hashlib.sha1(cert).hexdigest()

    def test_app_and_widget_profiles(self):
        for bundle_id in (release.BUNDLE_ID, release.WIDGET_ID):
            p, sha1 = self.profile(bundle_id)
            release.validate_profile(p, sha1, bundle_id)

    def test_profile_rejections(self):
        p, sha1 = self.profile(release.BUNDLE_ID)
        cases = [
            dict(TeamIdentifier=['OTHERTEAM1']),
            dict(ProvisionedDevices=['device']),
            dict(ExpirationDate=datetime(2000, 1, 1)),
            dict(UUID='not-a-uuid'),
            dict(Entitlements=dict(p['Entitlements'], **{'aps-environment': 'development'})),
            dict(Entitlements=dict(p['Entitlements'], **{'get-task-allow': True})),
            dict(Entitlements=dict(p['Entitlements'], **{'com.apple.security.application-groups': []})),
            dict(Entitlements={k: v for k, v in p['Entitlements'].items() if k != 'com.apple.developer.applesignin'}),
        ]
        for change in cases:
            bad, _ = self.profile(release.BUNDLE_ID, **change)
            with self.subTest(change=list(change)), self.assertRaises(release.ReleaseError):
                release.validate_profile(bad, sha1, release.BUNDLE_ID)
        with self.assertRaises(release.ReleaseError):
            release.validate_profile(p, 'b' * 40, release.BUNDLE_ID)
        widget, _ = self.profile(release.WIDGET_ID)
        with self.assertRaises(release.ReleaseError):
            release.validate_profile(widget, sha1, release.BUNDLE_ID)

    def test_signed_entitlements(self):
        good = {'application-identifier': f'{release.TEAM}.{release.BUNDLE_ID}', 'get-task-allow': False,
                'aps-environment': 'production', 'com.apple.developer.applesignin': ['Default'],
                'com.apple.security.application-groups': [release.APP_GROUP]}
        release.validate_signed_entitlements(good, release.BUNDLE_ID)
        for key in ('aps-environment', 'com.apple.developer.applesignin', 'com.apple.security.application-groups'):
            with self.subTest(key=key), self.assertRaises(release.ReleaseError):
                release.validate_signed_entitlements({k: v for k, v in good.items() if k != key}, release.BUNDLE_ID)

    def test_layout_requires_exact_bundle_ids_and_matching_versions(self):
        info = dict(CFBundleIdentifier=release.BUNDLE_ID, CFBundleVersion='7', CFBundleShortVersionString='1.0')
        widget = dict(info, CFBundleIdentifier=release.WIDGET_ID)
        self.assertEqual(release.check_layout(info, widget, '7'), '1.0')
        for i, w in [(dict(info, CFBundleIdentifier='com.rem.playground.settings'), widget),
                     (info, dict(widget, CFBundleVersion='6')), (info, dict(widget, CFBundleShortVersionString='1.1')),
                     (dict(info, CFBundleShortVersionString='1.0 beta'), widget)]:
            with self.assertRaises(release.ReleaseError):
                release.check_layout(i, w, '7')


class SummaryTests(unittest.TestCase):
    def test_only_allowlisted_fields_survive(self):
        record = release.public_record({
            'remclaw_sha': SHA, 'build_number': '12', 'release_branch': 'main', 'processing_state': 'VALID',
            'approval': {'model': 'owner-approval', 'reviewer': 'samuelalake'},
            'marketing_version': '1.0', 'binary_sha256': 'f' * 64, 'store_build_id': 'x', 'log': 'SOURCE_LEAK_MARKER',
            'source_path': '/Users/runner/PRIVATE_PATH_MARKER'}, 'COMPILE')
        self.assertEqual(set(record), {'remclaw_sha', 'build_number', 'release_branch', 'processing_state',
                                       'approval', 'failure_class'})

    def test_injected_values_rejected(self):
        base = {'remclaw_sha': SHA}
        for change in [dict(remclaw_sha='x'), dict(build_number='1; rm'), dict(release_branch='staging'),
                       dict(processing_state='SOURCE_LEAK_MARKER'), dict(approval={'model': 'owner-approval',
                       'reviewer': 'oledibefrancis'}), dict(approval={'model': 'x', 'reviewer': 'samuelalake', 'note': 'y'})]:
            with self.subTest(change=change), self.assertRaises(ValueError):
                release.public_record(dict(base, **change))
        with self.assertRaises(ValueError):
            release.public_record(base, 'SOURCE_LEAK_MARKER')

    def test_summary_text_suppresses_invalid_metadata_and_unknown_failure_codes(self):
        with tempfile.TemporaryDirectory() as tmp:
            a, b, fail = Path(tmp) / 'a.json', Path(tmp) / 'b.json', Path(tmp) / 'failure_class'
            a.write_text(json.dumps({'remclaw_sha': SHA, 'processing_state': 'not_started'}))
            b.write_text(json.dumps({'release_branch': 'SOURCE_LEAK_MARKER'}))
            fail.write_text('privateBackendToken_SOURCE_LEAK_MARKER\n')
            text = release.summary_text([a, b, Path(tmp) / 'missing.json'], fail)
            self.assertNotIn('SOURCE_LEAK_MARKER', text)
            self.assertIn('suppressed', text)
            text = release.summary_text([a], fail)
            self.assertIn('"failure_class": "UNKNOWN"', text)
            self.assertNotIn('SOURCE_LEAK_MARKER', text)
            self.assertIn('No verified', release.summary_text([Path(tmp) / 'missing.json'], None))


class CleanupTests(unittest.TestCase):
    def test_cleanup_removes_named_items_only_inside_runner_temp(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            for name in ('remclaw-source', 'remclaw-package', 'remclaw-ephemeral', 'remclaw-private', 'remclaw-gems'):
                (base / name / 'nested').mkdir(parents=True)
                (base / name / 'nested' / 'file').write_text('x')
            (base / 'remclaw-admission.json').write_text('{}')
            (base / 'unrelated').write_text('keep')
            outside = base / 'outside'
            outside.mkdir()
            (outside / 'keep').write_text('keep')
            (base / 'remclaw-private' / 'link').symlink_to(outside)
            with patch.dict(os.environ, {'RUNNER_TEMP': tmp}):
                release.cleanup(base)
                self.assertEqual(sorted(p.name for p in base.iterdir()), ['outside', 'unrelated'])
                self.assertTrue((outside / 'keep').exists())
                with self.assertRaises(release.ReleaseError):
                    release.cleanup(outside)


class PublicOutputTests(unittest.TestCase):
    def test_cli_failures_print_fixed_messages_only(self):
        script = Path(release.__file__)
        env = dict(os.environ, **IDENTITY)
        env.pop('REMCLAW_PRIVATE', None)
        p = subprocess.run([sys.executable, str(script), 'admit', '--sha', 'SOURCE_LEAK_MARKER', '--out', '/dev/null'],
                           capture_output=True, text=True, env=env)
        self.assertNotEqual(p.returncode, 0)
        self.assertEqual(p.stdout, '')
        self.assertIn('RemClaw release rejected: remclaw_sha must be', p.stderr)
        self.assertNotIn('SOURCE_LEAK_MARKER', p.stderr)
        self.assertNotIn('Traceback', p.stderr)

    def test_stage_failure_prints_classification_not_log(self):
        log = Path(__file__).resolve().parent / 'fixtures' / 'compile.log'
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with patch.dict(os.environ, {'REMCLAW_PRIVATE': tmp}), \
                 patch.object(release, 'main', side_effect=release.StageFailed('build', log)), \
                 redirect_stdout(out), self.assertRaises(SystemExit) as stop:
                release.entry([])
            self.assertRegex(out.getvalue().strip(), r'\Aclassification=COMPILE( [a-z]+=\d+)+\Z')
            self.assertNotIn('MARKER', out.getvalue() + str(stop.exception.code))
            self.assertIn('private output suppressed', stop.exception.code)
            self.assertEqual((Path(tmp) / 'failure_class').read_text(), 'COMPILE\n')

    def test_unexpected_exception_text_is_never_printed(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with patch.dict(os.environ, {'REMCLAW_PRIVATE': tmp}), \
                 patch.object(release, 'main', side_effect=OSError('/Users/runner/PRIVATE_PATH_MARKER')), \
                 redirect_stdout(out), self.assertRaises(SystemExit) as stop:
                release.entry([])
            self.assertEqual(out.getvalue(), '')
            self.assertNotIn('MARKER', stop.exception.code)
            self.assertEqual((Path(tmp) / 'failure_class').read_text(), 'UNKNOWN\n')

if __name__ == '__main__':
    unittest.main()
