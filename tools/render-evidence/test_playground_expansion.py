import hashlib
import json
import os
import subprocess
import sys
import textwrap
from pathlib import Path
import tempfile
import unittest

import artifact
import delivery
import test_artifact as artifact_fixtures
import test_validate_figma_run as run_fixtures
import validate_figma_run
from settings_routing import EXPANSION_SCOPES, validate_manual_pair

CONTRACTS = json.loads(Path(__file__).with_name('contracts.json').read_text())
BASE = 'codex/playground-expansion'
HEAD = 'agent-factory/playground-issue-83'


class PlaygroundEvidenceTests(unittest.TestCase):
    def test_publisher_scope_gate_rejects_wrong_candidates_before_credentials(self):
        repo = Path(__file__).resolve().parents[2]
        workflow = (repo / '.github/workflows/publish-builder-delivery.yml').read_text()
        start = workflow.index('          # Expansion scope and route')
        end = workflow.index('          run_meta=', start)
        code = textwrap.dedent(workflow[start:end])
        self.assertLess(end, workflow.index('      - name: Mint Builder installation token'))
        valid = dict(os.environ, base_ref=BASE, head_ref=HEAD,
                     pr_meta=json.dumps({'draft': True}))
        def run(base=BASE, head=HEAD, scope='agenda-suggestions', draft=True):
            env = dict(valid, base_ref=base, head_ref=head, pr_meta=json.dumps({'draft':draft}))
            # The scope array is normally populated from authenticated PR metadata.
            script = 'set -euo pipefail\nscopes=("' + scope + '")\n' + code
            return subprocess.run(['bash','-c',script], env=env,capture_output=True).returncode
        self.assertEqual(run(), 0)
        for kwargs in ({'base':'main'}, {'head':HEAD+'-bad'}, {'draft':False}, {'scope':''}):
            with self.subTest(kwargs=kwargs): self.assertNotEqual(run(**kwargs),0)

    def test_scope_is_bound_to_exact_branch_pair(self):
        for scope in EXPANSION_SCOPES:
            validate_manual_pair(BASE, HEAD, scope)
            for base, head, other_scope in [
                ('main', HEAD, scope), ('codex/settings-integration', HEAD, scope),
                (BASE, 'agent-factory/settings-issue-83', scope), (BASE, HEAD + '-bad', scope),
                (BASE, HEAD, 'settings-foundation'), (BASE, HEAD, 'onboarding-consent'),
            ]:
                with self.subTest(scope=scope, base=base, head=head), self.assertRaises(ValueError):
                    validate_manual_pair(base, head, other_scope)

    def test_manual_run_requires_separate_opt_in_and_exact_source_run(self):
        fixtures = run_fixtures.FigmaWorkflowRunValidationTests()
        for scope in EXPANSION_SCOPES:
            run, item = fixtures.fixtures()
            run.update(event='workflow_dispatch', head_sha=fixtures.base_sha, head_branch=BASE, pull_requests=[])
            item['workflow_run'].update(head_sha=fixtures.base_sha, head_branch=BASE)
            kwargs = dict(repository=fixtures.repository, pr=31, head_sha=fixtures.head_sha,
                          base_sha=fixtures.base_sha, base_ref=BASE, head_ref=HEAD, primary_contract=scope)
            with self.assertRaises(ValueError):
                validate_figma_run.validate(run, item, allow_settings_manual=True, **kwargs)
            validate_figma_run.validate(run, item, allow_playground_manual=True, **kwargs)
            for changes in ({'base_ref': 'main'}, {'head_ref': HEAD + '-bad'}, {'base_sha': 'f' * 40}):
                with self.subTest(scope=scope, changes=changes), self.assertRaises(ValueError):
                    validate_figma_run.validate(run, item, allow_playground_manual=True, **(kwargs | changes))

    def test_pull_request_run_cannot_use_expansion_scope_on_settings_or_main(self):
        fixtures = run_fixtures.FigmaWorkflowRunValidationTests()
        run, item = fixtures.fixtures()
        for scope in EXPANSION_SCOPES:
            with self.assertRaises(ValueError):
                validate_figma_run.validate(run, item, repository=fixtures.repository, pr=31,
                    head_sha=fixtures.head_sha, base_sha=fixtures.base_sha, base_ref='main',
                    head_ref=run['head_branch'], primary_contract=scope)

    def test_expansion_artifact_binds_scope_pair_and_trusted_contract_digest(self):
        for scope in EXPANSION_SCOPES:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                manifest = artifact_fixtures.ArtifactValidationTests().fixture(root)
                manifest.update(event='workflow_dispatch', manual_base_sha='d' * 40,
                                manual_base_ref=BASE, manual_head_ref=HEAD, primary_contract=scope)
                report = root / 'structure/report.json'
                report.write_text(json.dumps({'status': 'completed', 'head': 'a' * 40, 'fileKey': 'file-key', 'primaryContract': scope}))
                manifest['structure_report_sha256'] = hashlib.sha256(report.read_bytes()).hexdigest()
                path = root / 'delivery/manifest.json'; path.write_text(json.dumps(manifest))
                kwargs = dict(pr=31, sha='a' * 40, run_id=123, run_attempt=2,
                    workflow_sha256='b' * 64, media_dirs=('reference',), primary_contract=scope,
                    manual_base_sha='d' * 40, manual_base_ref=BASE, manual_head_ref=HEAD,
                    require_structure_verification=True, trusted_source_sha256=manifest['structure_contract_sha256'])
                artifact.validate(root, **kwargs)
                for changes in ({'trusted_source_sha256': None}, {'trusted_source_sha256': 'f' * 64},
                                {'manual_head_ref': HEAD + '-bad'}, {'manual_base_ref': 'main'},
                                {'primary_contract': 'settings-foundation'}, {'require_structure_verification': False}):
                    with self.subTest(scope=scope, changes=changes), self.assertRaises(ValueError):
                        artifact.validate(root, **(kwargs | changes))
                # A self-consistent artifact digest cannot replace the trusted base source.
                contract = root / 'structure/contract.json'; contract.write_text('{"fileKey":"other"}')
                manifest['structure_contract_sha256'] = hashlib.sha256(contract.read_bytes()).hexdigest()
                path.write_text(json.dumps(manifest))
                with self.assertRaisesRegex(ValueError, 'trusted source contract'):
                    artifact.validate(root, **kwargs)

    def test_mislabeled_structure_report_is_rejected_even_with_valid_report_digest(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); manifest = artifact_fixtures.ArtifactValidationTests().fixture(root)
            manifest['primary_contract'] = 'agenda-suggestions'
            report = root / 'structure/report.json'
            report.write_text(json.dumps({'status':'completed', 'head':'a' * 40, 'primaryContract':'onboarding-voice'}))
            manifest['structure_report_sha256'] = hashlib.sha256(report.read_bytes()).hexdigest()
            (root / 'delivery/manifest.json').write_text(json.dumps(manifest))
            with self.assertRaisesRegex(ValueError, 'unexpected playground scope'):
                artifact.validate(root, pr=31, sha='a' * 40, run_id=123, run_attempt=2,
                    workflow_sha256='b' * 64, media_dirs=('reference',), primary_contract='agenda-suggestions',
                    trusted_source_sha256=manifest['structure_contract_sha256'], require_structure_verification=True)

    def test_delivery_requires_both_native_appearances_journeys_and_three_references(self):
        for scope in EXPANSION_SCOPES:
            contract = CONTRACTS[scope]
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                states = contract['states'] + contract['runtimeGroups'][0]['states']
                for platform in ('swiftui', 'compose', 'reference'):
                    (root / platform).mkdir()
                    for key in contract['references'] if platform == 'reference' else states:
                        (root / platform / (key + '.png')).write_bytes(b'fixture')
                (root / 'structure').mkdir()
                (root / 'structure/report.json').write_text(json.dumps({'status':'completed', 'head':'a' * 40,
                    'structure': {'canonicalScreens':[{'id':ref['node'], 'status':'conformant'} for ref in contract['references'].values()], 'screens':[]}}))
                changed = ['tools/playground-ios/App/Playground.swift']
                status, _, _ = delivery.prepare(root, 'a' * 40, 'success', changed, CONTRACTS, primary_contract=scope)
                self.assertEqual(status, 'ready')
                (root / 'compose' / (contract['runtimeGroups'][0]['states'][0] + '.png')).unlink()
                status, body, _ = delivery.prepare(root, 'a' * 40, 'success', changed, CONTRACTS, primary_contract=scope)
                self.assertEqual(status, 'failed'); self.assertIn('dark / compose', body)


if __name__ == '__main__':
    unittest.main()
