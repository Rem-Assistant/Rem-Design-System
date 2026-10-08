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
    def settings_fixtures(self, root):
        contract = CONTRACTS["settings-foundation"]
        runtime = {state for group in contract["runtimeGroups"] for state in group["states"]}
        for platform in ("swiftui", "compose", "reference"):
            (root / platform).mkdir()
            states = contract["references"] if platform == "reference" else set(contract["states"]) | runtime
            for state in states:
                (root / platform / f"{state}.png").write_bytes(b"fixture")
        (root / "structure").mkdir()
        (root / "structure/report.json").write_text(json.dumps({
            "status": "completed", "head": "a" * 40,
            "structure": {"canonicalScreens": [
                {"id": value["node"], "status": "conformant"}
                for value in contract["references"].values()
            ], "screens": []},
        }))
        return runtime

    def test_full_settings_packet_requires_runtime_pairs_without_inventing_figma_proof(self):
        changed = [f"Sources/RemDesignSystem/Screens/Settings{name}Screen.swift" for name in
                   ("PairedDevices", "Connectors", "CloudBrowser", "Memory", "Models", "Wallet", "Voice")]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); runtime = self.settings_fixtures(root)
            status, body, attachments = delivery.prepare(
                root, "a" * 40, "success", changed, CONTRACTS, "settings-foundation",
                "https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/123",
            )
            self.assertEqual(status, "ready")
            self.assertEqual(len(runtime), 18)
            self.assertEqual(len(attachments), 42)
            self.assertEqual(len([p for p in attachments if p.parent.name == "reference"]), 2)
            self.assertIn("add no authenticated Figma structural coverage", body)
            self.assertIn("Full producing CI run and downloadable artifacts", body)
            for key in runtime:
                row = next(line for line in body.splitlines() if line.startswith(f"| `{key}`"))
                self.assertIn(f"swiftui Render {key}", row)
                self.assertIn(f"compose Render {key}", row)
                self.assertNotIn("Not exported", row)
            for key in runtime:
                for platform in ("swiftui", "compose"):
                    path = root / platform / f"{key}.png"; path.unlink()
                    missing_status, missing_body, _ = delivery.prepare(
                        root, "a" * 40, "success", changed, CONTRACTS, "settings-foundation")
                    self.assertEqual(missing_status, "failed")
                    self.assertIn(f"{key} / {platform}", missing_body)
                    path.write_bytes(b"fixture")

    def test_settings_runtime_requirements_follow_changed_destination_paths(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory); self.settings_fixtures(root)
            status, body, attachments = delivery.prepare(
                root, "a" * 40, "success", ["compose/RemDesignSystem/screens/SettingsMemoryScreen.kt"],
                CONTRACTS, "settings-foundation")
            self.assertEqual(status, "ready")
            self.assertEqual(len(attachments), 10)
            self.assertIn("memory-composer-feedback-light", body)
            self.assertNotIn("wallet-link-consent-light", body)

    def test_shared_wallet_and_voice_components_require_their_runtime_pairs(self):
        for path, state in (
            ("compose/RemDesignSystem/screens/ProviderPreConsentContent.kt", "wallet-link-consent-light"),
            ("Sources/RemDesignSystem/Primitives/RemSlider.swift", "voice-sliders-adjusted-light"),
            ("compose/RemDesignSystem/primitives/RemSlider.kt", "voice-sliders-adjusted-light"),
        ):
            with self.subTest(path=path), tempfile.TemporaryDirectory() as directory:
                root = Path(directory); self.settings_fixtures(root)
                (root / "compose" / f"{state}.png").unlink()
                status, body, _ = delivery.prepare(
                    root, "a" * 40, "success", [path], CONTRACTS, "settings-foundation")
                self.assertEqual(status, "failed")
                self.assertIn(f"{state} / compose", body)

    def test_delivery_declares_cross_renderer_comparison_basis(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for platform in ("swiftui", "compose"):
                (root / platform).mkdir()
                (root / platform / "demo-light.png").write_bytes(b"png")

            status, body, _ = delivery.prepare(
                root,
                "a" * 40,
                "success",
                ["Sources/Demo.swift"],
                {"demo": {"paths": ["Sources/**"], "states": ["demo-light"]}},
            )

        self.assertEqual(status, "ready")
        self.assertIn("iOS uses a 393×852-point viewport at 2×", body)
        self.assertIn("Paparazzi `DeviceConfig.PIXEL_6` viewport", body)
        self.assertIn("raw PNG dimensions are not layout differences", body)

    def test_actual_settings_capture_names_pair_with_source_contract(self):
        contract = CONTRACTS["settings-foundation"]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for platform in ("swiftui", "compose", "reference"):
                (root / platform).mkdir()
                for name in ("SettingsEntry-light", "AgentSettings-light"):
                    (root / platform / (name + ".png")).write_bytes(b"fixture")
            (root / "structure").mkdir()
            (root / "structure/report.json").write_text(json.dumps({
                "status": "completed", "head": "a" * 40,
                "structure": {"canonicalScreens": [
                    {"id": value["node"], "status": "conformant"}
                    for value in contract["references"].values()
                ], "screens": [], "sourceAmendments": ["Subtle is explicitly approved."]},
            }))
            status, body, attachments = delivery.prepare(
                root, "a" * 40, "success", ["tools/playground-ios/App/RemSettingsPlaygroundApp.swift"],
                CONTRACTS, primary_contract="settings-foundation",
            )
        self.assertEqual(status, "ready")
        self.assertEqual(len(attachments), 6)
        self.assertIn("actual Settings playground", body)
        self.assertIn("Subtle is explicitly approved", body)
        self.assertNotIn("Paparazzi", body)

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

    def test_approved_checkin_state_set(self):
        self.assertEqual(
            CONTRACTS["onboarding-checkin"]["states"],
            [
                "checkin-default-light",
                "checkin-edited-light",
                "checkin-saving-light",
                "checkin-saved-light",
                "checkin-failure-light",
            ],
        )
        references = CONTRACTS["onboarding-checkin"]["references"]
        self.assertEqual(
            [value["node"] for value in references.values()],
            ["870:788", "875:857", "875:940", "875:1026", "875:1112"],
        )
        self.assertTrue(all(value["requireDocumentation"] is False for value in references.values()))
        self.assertFalse(CONTRACTS["onboarding-checkin"]["requirePrototype"])

    def test_canonical_only_screen_contract_uses_authenticated_master_proof(self):
        contract = CONTRACTS["onboarding-checkin"]
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "structure").mkdir()
            (root / "structure/report.json").write_text(json.dumps({
                "status": "completed",
                "head": "a" * 40,
                "structure": {
                    "canonicalScreens": [
                        {"id": value["node"], "status": "conformant"}
                        for value in contract["references"].values()
                    ],
                    "screens": [],
                },
            }))
            lines, errors = delivery._authenticated_structure_proof(root, "a" * 40, contract)
        self.assertEqual(errors, [])
        body = "\n".join(lines)
        self.assertIn("`870:788` · conformant", body)
        self.assertIn("not required", body)
        self.assertNotIn("Authenticated prototype proof", body)

    def fixtures(self, root, missing=None, include_references=True, states=None, include_structure=True):
        states = states or CONTRACTS["onboarding-consent"]["states"]
        for platform in ("swiftui", "compose"):
            (root / platform).mkdir()
            for key in states:
                if (platform, key) == missing:
                    continue
                name = key if platform == "swiftui" else "com.rem.designsystem_EvidenceSnapshots_consent_" + key
                (root / platform / (name + ".png")).write_bytes(b"fixture")
        if not include_references:
            return
        (root / "reference").mkdir()
        for key in CONTRACTS["onboarding-consent"]["references"]:
            if ("reference", key) != missing:
                (root / "reference" / (key + ".png")).write_bytes(b"figma")
        for key in CONTRACTS["onboarding-consent"]["waypoints"]:
            if ("reference", key) != missing:
                (root / "reference" / (key + ".png")).write_bytes(b"figma")
        if not include_structure:
            return
        references = CONTRACTS["onboarding-consent"]["references"]
        (root / "structure").mkdir()
        (root / "structure/report.json").write_text(json.dumps({
            "status": "completed",
            "head": "a" * 40,
            "structure": {
                "canonicalScreens": [
                    {"id": value["node"], "status": "conformant"}
                    for value in references.values()
                ],
                "screens": [
                    {
                        "name": key,
                        "componentId": value["node"],
                        "resolvedNode": f"I777:433;769:{index}",
                        "status": "conformant",
                    }
                    for index, (key, value) in enumerate(references.items(), start=1)
                ],
                "prototype": {
                    "flowStartingPoints": [{"nodeId": "781:596", "name": "Consent flow"}],
                    "verifiedFlowStartingPoints": [{"nodeId": "781:596", "name": "Consent flow"}],
                    "flowStartVerification": "figma-rest",
                    "frames": [
                        {
                            "node": "781:596", "name": "Prototype · Consent", "status": "conformant",
                            "destinations": ["781:637", "781:690"], "backCount": 0, "backActions": [],
                            "navigations": [
                                {"sourceNode": "781:600", "sourceName": "ListRow", "trigger": "ON_CLICK", "destinationId": "781:637"},
                                {"sourceNode": "781:601", "sourceName": "ListRow", "trigger": "ON_CLICK", "destinationId": "781:690"},
                            ],
                        },
                        {
                            "node": "781:637", "name": "Prototype · Terms", "status": "conformant",
                            "destinations": [], "backCount": 2, "navigations": [],
                            "backActions": [
                                {"sourceNode": "781:640", "sourceName": "Scrim", "trigger": "ON_CLICK"},
                                {"sourceNode": "781:641", "sourceName": "Done", "trigger": "ON_CLICK"},
                            ],
                        },
                        {
                            "node": "781:690", "name": "Prototype · Privacy", "status": "conformant",
                            "destinations": [], "backCount": 2, "navigations": [],
                            "backActions": [
                                {"sourceNode": "781:693", "sourceName": "Scrim", "trigger": "ON_CLICK"},
                                {"sourceNode": "781:694", "sourceName": "Done", "trigger": "ON_CLICK"},
                            ],
                        },
                    ],
                },
            },
        }), encoding="utf-8")

    def test_each_required_state_publishes_adjacent_platforms(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            for platform in ("swiftui", "compose"):
                (root / platform / "signin-returning-light.png").write_bytes(b"regression")
            status, body, attachments = delivery.prepare(
                root, "a" * 40, "success", [CONSENT], CONTRACTS,
                primary_contract="onboarding-consent",
            )
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
            self.assertIn("Authenticated editable Figma proof", body)
            self.assertIn("Authenticated prototype proof", body)
            self.assertIn("Consent flow (figma-rest) → `781:596`", body)
            self.assertIn("ListRow · `781:600` → `781:637`", body)
            self.assertIn("Scrim · `781:640` · ON_CLICK", body)
            self.assertIn("Done · `781:641` · ON_CLICK", body)
            for value in CONTRACTS["onboarding-consent"]["references"].values():
                self.assertIn(f"`{value['node']}` · conformant", body)
            self.assertNotIn("signin-returning-light", body)

    def test_explicit_contract_ignores_collateral_other_screen_changes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            changed = [
                "docs/contracts/onboarding-consent.md",
                "docs/contracts/onboarding-sign-in.md",
                "compose/RemDesignSystem/onboarding/SignInStep.kt",
                "Sources/RemDesignSystem/Screens/SettingsVoiceScreen.swift",
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
            self.assertEqual(len(attachments), len(CONTRACTS["onboarding-consent"]["states"]) * 2 + 2)

    def test_unscoped_contract_paths_require_platforms_but_not_figma(self):
        all_states = sorted({
            state for contract in CONTRACTS.values() if "tokens/**" in contract["paths"]
            for state in contract["states"]
        })
        cases = [
            ([CONSENT], CONTRACTS["onboarding-consent"]["states"]),
            (["tokens/generated/semantic.json"], all_states),
        ]
        for changed, states in cases:
            with self.subTest(changed=changed), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); self.fixtures(root, include_references=False, states=states)
                status, body, attachments = delivery.prepare(
                    root, "a" * 40, "success", changed, CONTRACTS,
                )
                self.assertEqual(status, "ready")
                self.assertNotIn("Figma Reference", body)
                self.assertNotIn("figma-reference", body)
                self.assertEqual(len(attachments), len(states) * 2)

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

    def test_scoped_delivery_requires_exact_head_structure_proof(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root, include_structure=False)
            status, body, _ = delivery.prepare(
                root, "a" * 40, "success", [CONSENT], CONTRACTS,
                primary_contract="onboarding-consent",
            )
            self.assertEqual(status, "failed")
            self.assertIn("structure report is missing", body)

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); self.fixtures(root)
            report = json.loads((root / "structure/report.json").read_text())
            report["head"] = "b" * 40
            (root / "structure/report.json").write_text(json.dumps(report))
            status, body, _ = delivery.prepare(
                root, "a" * 40, "success", [CONSENT], CONTRACTS,
                primary_contract="onboarding-consent",
            )
            self.assertEqual(status, "failed")
            self.assertIn("does not match the completed exact-head verification", body)

    def test_scoped_delivery_rejects_mismatched_documentation_or_prototype_proof(self):
        mutations = (
            lambda report: report["structure"]["screens"][0].update(componentId="999:999"),
            lambda report: report["structure"]["prototype"]["frames"][1].update(status="invalid"),
            lambda report: report["structure"]["prototype"]["frames"][0].update(destinations=["781:637"]),
            lambda report: report["structure"]["prototype"].pop("flowStartVerification"),
        )
        for mutate in mutations:
            with tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp); self.fixtures(root)
                report = json.loads((root / "structure/report.json").read_text())
                mutate(report)
                (root / "structure/report.json").write_text(json.dumps(report))
                status, body, _ = delivery.prepare(
                    root, "a" * 40, "success", [CONSENT], CONTRACTS,
                    primary_contract="onboarding-consent",
                )
                self.assertEqual(status, "failed")
                self.assertIn("proof invalid", body)

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
            root = Path(tmp); self.fixtures(root, include_references=False)
            status, _, _ = delivery.prepare(root, "a" * 40, "success", [CONSENT], CONTRACTS)
            self.assertEqual(status, "ready")
