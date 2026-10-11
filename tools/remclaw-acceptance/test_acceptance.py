import contextlib
from datetime import datetime, timedelta, timezone
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch, Mock

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('acceptance', HERE / 'acceptance.py')
a = importlib.util.module_from_spec(spec)
spec.loader.exec_module(a)

class AdmissionTests(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 10, 11, 18, 0, tzinfo=timezone.utc)
        self.receipt = dict(sha=a.SHA, project_id=a.PROJECT, environment_id=a.RAILWAY_ENV,
                            service_id=a.SERVICE, deployment_id='11111111-1111-4111-8111-111111111111',
                            checked_at=self.now.isoformat(), provider_calls=3)

    def test_exact_modes(self):
        a.validate_inputs(a.SHA, 'build-only', '0')
        a.validate_inputs(a.SHA, 'chat-inbox', '3')

    def test_other_candidates_refused(self):
        for sha in ['', 'main', a.SHA.upper(), '0' * 40]:
            with self.subTest(sha=sha), self.assertRaises(a.Refused):
                a.validate_inputs(sha, 'build-only', '0')

    def test_call_scope_refused(self):
        for mode, calls in [('build-only', '3'), ('chat-inbox', '0'), ('chat-inbox', '4'),
                            ('chat-inbox', '03'), ('release', '0')]:
            with self.subTest(mode=mode, calls=calls), self.assertRaises(a.Refused):
                a.validate_inputs(a.SHA, mode, calls)

    def test_attestation_valid(self):
        self.assertEqual(a.validate_attestation(json.dumps(self.receipt), self.now), self.receipt['deployment_id'])

    def test_wrong_runtime_identity_refused(self):
        for field in ['sha', 'project_id', 'environment_id', 'service_id', 'deployment_id']:
            receipt = dict(self.receipt, **{field: 'wrong'})
            with self.subTest(field=field), self.assertRaises(a.Refused):
                a.validate_attestation(json.dumps(receipt), self.now)

    def test_attestation_extra_missing_fields_refused(self):
        for receipt in [dict(self.receipt, token='SENTINEL'), {k:v for k,v in self.receipt.items() if k != 'sha'}]:
            with self.assertRaises(a.Refused): a.validate_attestation(json.dumps(receipt), self.now)

    def test_attestation_time_bounds(self):
        for delta in [-3601, 1]:
            receipt = dict(self.receipt, checked_at=(self.now + timedelta(seconds=delta)).isoformat())
            with self.subTest(delta=delta), self.assertRaises(a.Refused):
                a.validate_attestation(json.dumps(receipt), self.now)
        with self.assertRaises(a.Refused):
            a.validate_attestation(json.dumps(dict(self.receipt, checked_at='2026-10-11T18:00:00')), self.now)

    def test_attestation_exact_budget_type(self):
        for value in [True, '3', 0, 3.0, 4]:
            with self.subTest(value=value), self.assertRaises(a.Refused):
                a.validate_attestation(json.dumps(dict(self.receipt, provider_calls=value)), self.now)

    def test_gate_checks_identity_then_environment_and_owner(self):
        release = Mock()
        release.gate.REPO = 'Rem-Assistant/Rem-Design-System'
        release.gate.paged.return_value = [{'name': 'main', 'type': 'branch'}]
        release.gate.validate_environment.return_value = {'reviewer':'samuelalake'}
        with patch.object(a, 'load_release', return_value=release):
            a.check_gate()
            release.run_identity.assert_called_once()
            self.assertTrue(release.gate.gh.call_args.args[0].endswith('/environments/remclaw-staging-acceptance'))
            release.gate.validate_environment.return_value = {'reviewer':'someone-else'}
            with self.assertRaises(a.Refused): a.check_gate()

    def test_gate_api_failure_is_closed(self):
        release = Mock()
        release.gate.REPO = 'Rem-Assistant/Rem-Design-System'
        release.gate.gh.side_effect = a.Refused()
        with patch.object(a, 'load_release', return_value=release), self.assertRaises(a.Refused): a.check_gate()

class PrivacyTests(unittest.TestCase):
    def test_private_root_and_symlink_refusal(self):
        with tempfile.TemporaryDirectory() as temp, patch.dict(os.environ, RUNNER_TEMP=temp):
            self.assertEqual(a.root().stat().st_mode & 0o777, 0o700)
        with tempfile.TemporaryDirectory() as temp, patch.dict(os.environ, RUNNER_TEMP=temp):
            Path(temp, 'remclaw-acceptance-private').symlink_to(Path(temp))
            with self.assertRaises(a.Refused): a.root()

    def test_failed_tool_output_stays_private(self):
        with tempfile.TemporaryDirectory() as temp, patch.dict(os.environ, RUNNER_TEMP=temp):
            public = io.StringIO()
            with contextlib.redirect_stdout(public), contextlib.redirect_stderr(public), self.assertRaises(a.Refused):
                a.quiet(['python3', '-c', 'import sys; print("SENTINEL"); print("SECRET", file=sys.stderr); sys.exit(1)'], 'unit')
            self.assertEqual(public.getvalue(), '')
            self.assertIn('SECRET', (a.root()/'unit.log').read_text())

    def test_summary_rejects_untrusted_values(self):
        with self.assertRaises(a.Refused): a.safe_summary(a.SHA, 'chat-inbox', 'SECRET')
        result = a.safe_summary(a.SHA, 'chat-inbox', 'test_passed_pending_runtime_readback')
        self.assertIn('operator_readback_required', result)

    def test_cleanup_drops_credentials(self):
        with tempfile.TemporaryDirectory() as temp, patch.dict(os.environ, RUNNER_TEMP=temp,
                TEST_RUNNER_SECRET='SENTINEL', RUNTIME_ATTESTATION='SENTINEL'):
            (a.root()/'simulator').write_text('11111111-1111-4111-8111-111111111111')
            with patch.object(a, 'quiet') as quiet, patch.object(a, 'verify_executed_tests') as verify:
                a.cleanup_session()
                verify.assert_called_once_with(cleanup=True)
                env = quiet.call_args.kwargs['env']
                self.assertNotIn('TEST_RUNNER_SECRET', env)
                self.assertNotIn('RUNTIME_ATTESTATION', env)
                self.assertIn('-only-testing:RemClawUITests/StagingAcceptanceCleanupUITests', quiet.call_args.args[0])

    def test_cleanup_erases_files_even_delete_failure(self):
        with tempfile.TemporaryDirectory() as temp, patch.dict(os.environ, RUNNER_TEMP=temp, ACCEPTANCE_MODE='chat-inbox'):
            directory = a.root()
            (directory/'simulator').write_text('11111111-1111-4111-8111-111111111111')
            with patch.object(a, 'quiet', side_effect=a.Refused()), self.assertRaises(a.Refused): a.cleanup_files()
            self.assertFalse(directory.exists())

    def test_cli_never_prints_exception_or_input(self):
        env = dict(os.environ, REMCLAW_SHA='SECRET', ACCEPTANCE_MODE='build-only', APPROVED_PROVIDER_CALLS='0')
        result = subprocess.run(['python3', '-I', '-B', str(HERE/'acceptance.py'), 'admit'], env=env, capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, '')
        self.assertEqual(result.stderr, 'acceptance_control=refused\n')

    def test_control_rejects_changed_or_wrong_checkout(self):
        with patch.dict(os.environ, GITHUB_SHA='a'*40), patch.object(a, 'quiet', side_effect=[b'b'*40]):
            with self.assertRaises(a.Refused): a.check_control()
        with patch.dict(os.environ, GITHUB_SHA='a'*40), patch.object(a, 'quiet', side_effect=[b'a'*40, b' M acceptance.py']):
            with self.assertRaises(a.Refused): a.check_control()

class ExecutionReceiptTests(unittest.TestCase):
    def record(self, name='Suite/test()', status='Success'):
        return {'_type': {'_name': 'ActionTestMetadata'}, 'identifier': {'_value':name}, 'testStatus': {'_value':status}}

    def test_exact_executed_method(self):
        a.validate_test_records({'tests':{'_values':[self.record()]}}, ['Suite/test()'])

    def test_skipped_failed_empty_wrong_or_duplicate_refused(self):
        for records in [[], [self.record(status='Skipped')], [self.record(status='Failure')],
                        [self.record(name='Other/test()')], [self.record(), self.record()]]:
            with self.subTest(records=records), self.assertRaises(a.Refused):
                a.validate_test_records({'tests':records}, ['Suite/test()'])

    def test_exit_zero_but_missing_harness_receipt_refused(self):
        with tempfile.TemporaryDirectory() as temp, patch.dict(os.environ, RUNNER_TEMP=temp,
                GITHUB_RUN_ID='1', TEST_RUNNER_REM_ACCEPTANCE_TEST_ACCOUNT_ID='synthetic',
                TEST_RUNNER_REM_ACCEPTANCE_TEST_LOGIN_SECRET='sentinel'):
            def tool(args, *unused, **kwargs):
                if 'pick_simulator.py' in str(args): return b'runtime device'
                if 'create' in args: return b'11111111-1111-4111-8111-111111111111'
                if 'list' in args: return b'{}'
                if 'export_evidence.py' in str(args): raise a.Refused()
                return None
            with patch.object(a, 'check_control'), patch.object(a, 'validate_attestation'), patch.object(a, 'verify_executed_tests'), patch.object(a, 'quiet', side_effect=tool):
                with self.assertRaises(a.Refused): a.run_tests()
            self.assertFalse((a.root()/'result').exists())

    def test_unknown_schema_refused(self):
        with self.assertRaises(a.Refused):
            a.validate_test_records({'passed':True}, ['Suite/test()'])

    def test_missing_test_reference_refused(self):
        with tempfile.TemporaryDirectory() as temp, patch.dict(os.environ, RUNNER_TEMP=temp):
            with patch.object(a, 'quiet', return_value=b'{"actions":{"_values":[]}}'), self.assertRaises(a.Refused):
                a.verify_executed_tests(cleanup=True)

class WorkflowTests(unittest.TestCase):
    def test_no_public_upload_cache_or_release_actions(self):
        workflow = (HERE.parents[1]/'.github/workflows/remclaw-staging-acceptance.yml').read_text()
        for forbidden in ['upload-artifact@', 'cache@', 'pull_request:', 'push:', 'contents: write', 'fastlane', 'ASC_KEY']:
            self.assertNotIn(forbidden, workflow)
        self.assertEqual(workflow.count('environment: remclaw-staging-acceptance'), 1)
        self.assertIn('needs: admission', workflow)
        self.assertIn('cancel-in-progress: false', workflow)
        self.assertIn('if: always() && inputs.mode', workflow)
        self.assertIn('JOB_STATUS: ${{ job.status }}', workflow)
        self.assertLess(workflow.index('cleanup-files'), workflow.index('acceptance.py summary'))

if __name__ == '__main__': unittest.main()
