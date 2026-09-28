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
        manifest = {
            "version": 1,
            "pr": 31,
            "sha": "a" * 40,
            "run_id": 123,
            "run_attempt": 2,
            "workflow_sha256": "b" * 64,
            "contracts_sha256": "c" * 64,
            "reference_export_status": "success",
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
            require_reference_export=True,
        )

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

    def test_rejects_undeclared_extra_media(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixture(root)
            (root / "reference/extra.png").write_bytes(b"extra")
            with self.assertRaisesRegex(ValueError, "exactly match"):
                self.validate(root)
