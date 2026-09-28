import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "stage_references", Path(__file__).with_name("stage_references.py")
)
stage_references = importlib.util.module_from_spec(spec)
spec.loader.exec_module(stage_references)


class StageReferenceTests(unittest.TestCase):
    def test_full_export_set_stages_only_selected_contract_media(self):
        contracts = {
            "one": {
                "references": {"ready": {"name": "One-ready", "node": "1:2"}},
                "waypoints": {"flow": {"name": "One flow", "node": "1:3"}},
            },
            "two": {
                "references": {"ready": {"name": "Two-ready", "node": "2:2"}},
            },
        }
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); source = root / "full"; destination = root / "staged"
            source.mkdir()
            for name in ("One-ready", "One flow", "Two-ready", "Unrelated Component"):
                (source / f"{name}.png").write_bytes(name.encode())
            staged = stage_references.stage(source, destination, contracts, "one")
            self.assertEqual([path.name for path in staged], ["One flow.png", "One-ready.png"])
            self.assertEqual(
                {path.name for path in destination.iterdir()},
                {"One flow.png", "One-ready.png"},
            )

    def test_missing_or_unknown_contract_fails_closed(self):
        contracts = {"one": {"references": {"ready": {"name": "One-ready", "node": "1:2"}}}}
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); root.mkdir(exist_ok=True)
            with self.assertRaisesRegex(ValueError, "Missing contract reference"):
                stage_references.stage(root, root / "out", contracts, "one")
            with self.assertRaisesRegex(ValueError, "Unknown primary"):
                stage_references.stage(root, root / "other", contracts, "missing")
