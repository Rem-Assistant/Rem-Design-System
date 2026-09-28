import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("delivery", Path(__file__).with_name("delivery.py"))
delivery = importlib.util.module_from_spec(spec)
spec.loader.exec_module(delivery)
CONTRACTS = json.loads(Path(__file__).with_name("contracts.json").read_text())
CONSENT = "compose/RemDesignSystem/onboarding/ConsentStep.kt"


class PairedDeliveryTests(unittest.TestCase):
    def fixtures(self, root, missing=None):
        for platform in ("swiftui", "compose"):
            (root / platform).mkdir()
            for key in CONTRACTS["onboarding-consent"]["states"]:
                if (platform, key) == missing:
                    continue
                name = key if platform == "swiftui" else "com.rem.designsystem_EvidenceSnapshots_consent_" + key
                (root / platform / (name + ".png")).write_bytes(b"fixture")

    def test_each_required_state_publishes_adjacent_platforms(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            status, body, attachments = delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)
            self.assertEqual(status, "ready")
            self.assertEqual(len(attachments), 10)
            self.assertEqual([p.parent.name for p in attachments], ["swiftui", "compose"] * 5)
            for key in CONTRACTS["onboarding-consent"]["states"]:
                row = next(line for line in body.splitlines() if line.startswith(f"| `{key}`"))
                self.assertIn(f"![swiftui {key}]", row)
                self.assertIn(f"![compose {key}]", row)

    def test_missing_android_state_fails_even_when_workflow_succeeded(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root, ("compose", "consent-error-dark"))
            status, body, _ = delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)
            self.assertEqual(status, "failed")
            self.assertIn("consent-error-dark / compose", body)

    def test_failed_compile_never_becomes_ready_with_partial_screenshots(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            status, _, _ = delivery.prepare(root, "a" * 40, "failure", [CONSENT], CONTRACTS)
            self.assertEqual(status, "failed")

    def test_duplicate_normalized_state_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            (root / "compose/com.rem.designsystem_EvidenceSnapshots_other_consent-error-dark.png").write_bytes(b"fixture")
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)

    def test_unimplemented_other_screen_does_not_block_unrelated_work(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            status, _, _ = delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)
            self.assertEqual(status, "ready")
