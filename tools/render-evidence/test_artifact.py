import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("artifact", Path(__file__).with_name("artifact.py"))
artifact = importlib.util.module_from_spec(spec)
spec.loader.exec_module(artifact)


class ArtifactValidationTests(unittest.TestCase):
    def fixture(self, root: Path) -> dict:
        (root / "reference").mkdir()
        image = root / "reference/demo.png"
        image.write_bytes(b"png")
        (root / "structure").mkdir()
        structure_report = root / "structure/report.json"
        structure_report.write_text(json.dumps({
            "status": "completed", "head": "a" * 40, "fileKey": "file-key",
        }))
        structure_contract = root / "structure/contract.json"
        structure_contract.write_text(json.dumps({"fileKey": "file-key"}))
        candidate_manifest = root / "structure/manifest.json"
        candidate_manifest.write_text(json.dumps({"figmaFileKey": "file-key"}))
        manifest = {
            "version": 1,
            "pr": 31,
            "sha": "a" * 40,
            "run_id": 123,
            "run_attempt": 2,
            "workflow_sha256": "b" * 64,
            "contracts_sha256": "c" * 64,
            "primary_contract": "demo",
            "reference_export_status": "success",
            "structure_verification_status": "success",
            "structure_report_sha256": hashlib.sha256(structure_report.read_bytes()).hexdigest(),
            "structure_contract_sha256": hashlib.sha256(structure_contract.read_bytes()).hexdigest(),
            "candidate_manifest_sha256": hashlib.sha256(candidate_manifest.read_bytes()).hexdigest(),
            "figma_file_key": "file-key",
            "media_sha256": {
                "reference/demo.png": hashlib.sha256(image.read_bytes()).hexdigest(),
            },
        }
        (root / "delivery").mkdir()
        (root / "delivery/manifest.json").write_text(json.dumps(manifest))
        return manifest

    def validate(self, root: Path):
        return artifact.validate(
            root,
            pr=31,
            sha="a" * 40,
            run_id=123,
            run_attempt=2,
            workflow_sha256="b" * 64,
            media_dirs=("reference",),
            figma_file_key="file-key",
            contracts_sha256="c" * 64,
            primary_contract="demo",
            require_reference_export=True,
            require_structure_verification=True,
        )

    def test_manual_manifest_binds_settings_base_head_and_event(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); manifest = self.fixture(root)
            manifest.update(event="workflow_dispatch", manual_base_sha="d" * 40,
                            manual_base_ref="codex/settings-integration",
                            manual_head_ref="agent-factory/settings-issue-74",
                            primary_contract="settings-foundation")
            path = root / "delivery/manifest.json"
            kwargs = dict(pr=31, sha="a" * 40, run_id=123, run_attempt=2,
                          workflow_sha256="b" * 64, media_dirs=("reference",),
                          primary_contract="settings-foundation", manual_base_sha="d" * 40,
                          manual_base_ref="codex/settings-integration",
                          manual_head_ref="agent-factory/settings-issue-74")
            path.write_text(json.dumps(manifest))
            self.assertEqual(artifact.validate(root, **kwargs), manifest)
            for field, value in (("event", "pull_request_target"), ("manual_base_sha", "e" * 40),
                                 ("manual_base_ref", "main"), ("manual_head_ref", "unrelated"),
                                 ("sha", "e" * 40), ("pr", 99)):
                broken = dict(manifest); broken[field] = value
                path.write_text(json.dumps(broken))
                with self.subTest(field=field), self.assertRaises(ValueError):
                    artifact.validate(root, **kwargs)

    def test_stack_manifest_cannot_substitute_another_admitted_pair(self):
        pairs = (("codex/settings-review-contracts", "codex/settings-review-native"),
                 ("codex/settings-review-native", "codex/settings-review-evidence"))
        for base, head in pairs:
            with tempfile.TemporaryDirectory() as directory:
                root = Path(directory); manifest = self.fixture(root)
                manifest.update(event="workflow_dispatch", manual_base_sha="d" * 40,
                                manual_base_ref=base, manual_head_ref=head,
                                primary_contract="settings-foundation")
                path = root / "delivery/manifest.json"
                kwargs = dict(pr=31, sha="a" * 40, run_id=123, run_attempt=2,
                              workflow_sha256="b" * 64, media_dirs=("reference",),
                              primary_contract="settings-foundation", manual_base_sha="d" * 40,
                              manual_base_ref=base, manual_head_ref=head)
                path.write_text(json.dumps(manifest))
                self.assertEqual(artifact.validate(root, **kwargs), manifest)
                other_base, other_head = next(pair for pair in pairs if pair != (base, head))
                manifest.update(manual_base_ref=other_base, manual_head_ref=other_head)
                path.write_text(json.dumps(manifest))
                with self.assertRaises(ValueError):
                    artifact.validate(root, **kwargs)

    def test_accepts_exact_head_exact_attempt_and_exact_digests(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); expected = self.fixture(root)
            self.assertEqual(self.validate(root), expected)

    def test_rejects_head_attempt_workflow_file_and_digest_mismatches(self):
        mutations = {
            "sha": "c" * 40,
            "run_attempt": 3,
            "workflow_sha256": "d" * 64,
            "contracts_sha256": "e" * 64,
            "figma_file_key": "other-file",
            "reference_export_status": "failure",
            "structure_verification_status": "failure",
            "primary_contract": "other",
        }
        for field, value in mutations.items():
            with self.subTest(field=field), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); manifest = self.fixture(root)
                manifest[field] = value
                (root / "delivery/manifest.json").write_text(json.dumps(manifest))
                with self.assertRaises(ValueError):
                    self.validate(root)
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixture(root)
            (root / "reference/demo.png").write_bytes(b"tampered")
            with self.assertRaisesRegex(ValueError, "digest mismatch"):
                self.validate(root)

    def test_rejects_tampered_or_noncompleted_structure_proof(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixture(root)
            (root / "structure/report.json").write_text(json.dumps({"status": "failed"}))
            with self.assertRaisesRegex(ValueError, "structure_report_sha256 mismatch"):
                self.validate(root)
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); manifest = self.fixture(root)
            report = root / "structure/report.json"
            report.write_text(json.dumps({"status": "failed", "head": "a" * 40, "fileKey": "file-key"}))
            manifest["structure_report_sha256"] = hashlib.sha256(report.read_bytes()).hexdigest()
            (root / "delivery/manifest.json").write_text(json.dumps(manifest))
            with self.assertRaisesRegex(ValueError, "not a completed exact-head"):
                self.validate(root)

    def test_rejects_undeclared_extra_media(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixture(root)
            (root / "reference/extra.png").write_bytes(b"extra")
            with self.assertRaisesRegex(ValueError, "exactly match"):
                self.validate(root)
