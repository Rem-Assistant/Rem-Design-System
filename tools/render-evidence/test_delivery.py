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
    def test_approved_consent_state_set(self):
        self.assertEqual(
            CONTRACTS["onboarding-consent"]["states"],
            ["consent-default-light", "consent-terms-light", "consent-privacy-light"],
        )
        self.assertEqual(
            set(CONTRACTS["onboarding-consent"]["references"]),
            set(CONTRACTS["onboarding-consent"]["states"]),
        )
        self.assertEqual(
            CONTRACTS["onboarding-consent"]["waypoints"],
            {"consent-flow-documentation": {"name": "Consent-flow-documentation", "node": "777:432"}},
        )

    def fixtures(self, root, missing=None):
        for platform in ("swiftui", "compose"):
            (root / platform).mkdir()
            for key in CONTRACTS["onboarding-consent"]["states"]:
                if (platform, key) == missing:
                    continue
                name = key if platform == "swiftui" else "com.rem.designsystem_EvidenceSnapshots_consent_" + key
                (root / platform / (name + ".png")).write_bytes(b"fixture")
        (root / "reference").mkdir()
        for key in CONTRACTS["onboarding-consent"]["references"]:
            if ("reference", key) != missing:
                (root / "reference" / (key + ".png")).write_bytes(b"figma")
        for key in CONTRACTS["onboarding-consent"]["waypoints"]:
            if ("reference", key) != missing:
                (root / "reference" / (key + ".png")).write_bytes(b"figma")

    def test_each_required_state_publishes_adjacent_platforms(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            for platform in ("swiftui", "compose"):
                (root / platform / "signin-returning-light.png").write_bytes(b"regression")
            status, body, attachments = delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)
            self.assertEqual(status, "ready")
            state_count = len(CONTRACTS["onboarding-consent"]["states"])
            self.assertEqual(len(attachments), state_count * 3 + 1)
            self.assertEqual(
                [p.parent.name for p in attachments],
                ["swiftui", "compose", "reference"] * state_count + ["reference"],
            )
            for key in CONTRACTS["onboarding-consent"]["states"]:
                row = next(line for line in body.splitlines() if line.startswith(f"| `{key}`"))
                self.assertIn(f"![swiftui Render {key}]", row)
                self.assertIn(f"![compose Render {key}]", row)
                self.assertIn(f"![Figma Reference {key}]", row)
            self.assertIn("consent-flow-documentation", body)
            self.assertNotIn("signin-returning-light", body)

    def test_explicit_contract_ignores_collateral_other_screen_changes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            changed = [
                "docs/contracts/onboarding-consent.md",
                "docs/contracts/onboarding-sign-in.md",
                "compose/RemDesignSystem/onboarding/SignInStep.kt",
            ]
            status, body, attachments = delivery.prepare(
                root, "a" * 40, "success", changed, CONTRACTS,
                primary_contract="onboarding-consent",
            )
            self.assertEqual(status, "ready")
            self.assertNotIn("signin-", body)
            self.assertEqual(
                len(attachments),
                len(CONTRACTS["onboarding-consent"]["states"]) * 3 + 1,
            )

    def test_unscoped_delivery_keeps_complete_gallery(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            for platform in ("swiftui", "compose"):
                (root / platform / "component-light.png").write_bytes(b"fixture")
            status, body, attachments = delivery.prepare(
                root, "a" * 40, "success", ["README.md"], CONTRACTS,
            )
            self.assertEqual(status, "ready")
            self.assertIn("component-light", body)
            self.assertEqual(len(attachments), len(CONTRACTS["onboarding-consent"]["states"]) * 3 + 2)

    def test_missing_android_state_fails_even_when_workflow_succeeded(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root, ("compose", "consent-privacy-light"))
            status, body, _ = delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)
            self.assertEqual(status, "failed")
            self.assertIn("consent-privacy-light / compose", body)

    def test_failed_compile_never_becomes_ready_with_partial_screenshots(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            status, _, _ = delivery.prepare(root, "a" * 40, "failure", [CONSENT], CONTRACTS)
            self.assertEqual(status, "failed")

    def test_missing_figma_state_or_waypoint_fails_closed(self):
        for key in ("consent-terms-light", "consent-flow-documentation"):
            with self.subTest(key=key), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); self.fixtures(root, ("reference", key))
                status, body, _ = delivery.prepare(
                    root, "a" * 40, "success", [CONSENT], CONTRACTS,
                    primary_contract="onboarding-consent",
                )
                self.assertEqual(status, "failed")
                self.assertIn(f"{key} / figma-reference", body)

    def test_exclusive_prefix_rejects_uncontracted_output_without_hardcoding(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            (root / "swiftui/consent-loading-light.png").write_bytes(b"stale")
            status, body, attachments = delivery.prepare(
                root, "a" * 40, "success", [CONSENT], CONTRACTS,
                primary_contract="onboarding-consent",
            )
            self.assertEqual(status, "failed")
            self.assertIn("Unexpected contract-prefixed output", body)
            self.assertIn("consent-loading-light", body)
            self.assertNotIn(root / "swiftui/consent-loading-light.png", attachments)

    def test_primary_contract_must_be_trusted_configuration(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            with self.assertRaisesRegex(ValueError, "Unknown primary delivery contract"):
                delivery.prepare(
                    root, "a" * 40, "success", [CONSENT], CONTRACTS,
                    primary_contract="arbitrary-pr-value",
                )

    def test_duplicate_normalized_state_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            (root / "compose/com.rem.designsystem_EvidenceSnapshots_other_consent-privacy-light.png").write_bytes(b"fixture")
            with self.assertRaisesRegex(ValueError, "Duplicate"):
                delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)

    def test_unimplemented_other_screen_does_not_block_unrelated_work(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            status, _, _ = delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)
            self.assertEqual(status, "ready")
