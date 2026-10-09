Implement the approved **Onboarding New → Voice** playground shell and chooser on native SwiftUI and Compose with controlled local fixture state. This is a bounded UI slice, not the full onboarding sequencer or production voice integration.

## Source and reuse

Figma file `af4yDqCzp57jds9lkFiIaO`, Onboarding New page `2213:9168`, Voice section `2213:9346`. Read the versioned source archive, coverage and builder boundaries linked in trusted-base `docs/contracts/playground-expansion.md` before implementation. Its nine current states supersede old Screen/Voice `488:251` and old task-reference screenshots.

States: default `2219:22520`, previewing `2219:22800`, selected `2219:23081`, speed `2219:23361`, consistency `2219:23641`, likeness `2219:23921`, chooser default `2221:83686`, chooser preview `2221:83798`, chooser selected `2221:84017`.

Reuse the existing public Settings **VoiceControlsContent**, **VoiceChooserContent** and **VoiceSettingsFixture**, corresponding to canonical cores `2217:1571` and `2217:1901`. Set `showConversationEntry` false for onboarding. Existing `Sources/RemDesignSystem/Templates/OnboardingVoiceTemplate.swift` and `compose/RemDesignSystem/onboarding/VoiceScreen.kt` duplicate an older control tree: adapt the surrounding template compatibly to shared content instead of creating another copy or regressing Settings. Follow Lockup `773:22` and ActionArea `773:28`.

Extract `docs/playground/expansion-design/source-evidence.tar.gz` to a temporary directory and verify its SHA256 `494a323503cf281b37df737c9e987c9b97b48fcb3a79511aba33c8de1b7da2ae` and enclosed file hashes. Read the adjacent coverage and boundary documents; trusted structural identity checks live in `tools/design-sync/playground-source-contracts.json`.

## Behavior to implement and prove

- Authored title “Choose how Rem sounds”; subtitle “Preview a voice, choose the one Rem uses, then fine-tune its delivery.” Default Aria, speed50%, consistency75%, likeness50%.
- Fixture choices Aria (Warm), Sol (Bright), Rowan (Calm), Juniper (Expressive), Vale (Neutral). Preserve semantic icons and existing assets; no new bitmap requirement.
- Tapping Voice opens the chooser. Preview and selection are separate targets/state. Selecting Sol stops preview; native Back returns to the shell reflecting Sol, preserving all slider values. Preview can independently start and stop; preserve the explicit no-audio fixture boundary.
- Reproduce authored changes: speed75%, consistency50%, likeness75%. Represent values through the authored native slider state and accessibility value, without inventing new numeric labels; preserve them through chooser navigation.
- Outer Back exits to the playground host. Continue and Skip invoke separately observable host callbacks. Current masters provide no downstream prototype wiring: do not invent subsequent product screens or route into Check-in.
- Scroll all content above safe-area actions. Continue/Skip remain reachable at large text. Native controls and accessibility labels/actions must remain usable; do not hardcode screenshot heights or use visual overlays.

## Delivery contract

Work only on the seeded `agent-factory/playground-issue-<issue>` draft PR targeting `codex/playground-expansion`. Delivery scope is `onboarding-voice`. Keep Settings behavior and Settings PRs unchanged. Wire a discoverable route in both native playground hosts and register new iOS UI-test classes in the Xcode project.

Produce paired captures for every state in the trusted `tools/render-evidence/contracts.json` scope, including `OnboardingVoice-light`, `OnboardingVoice-chooser-light`, `OnboardingVoice-selected-light`, dark/large-text and journey outcomes. Capture names must be unique per run. Test default→chooser→preview→Sol→Back, independent preview/selection, three persistent slider changes, preview stop, correct Back/Continue/Skip callbacks, scrolling and large-text reachability. Render screenshots alone do not prove these interactions.

Update bounded coverage/fidelity notes stating which of nine source states were implemented and visually compared. Missing screenshots or visual inspection must not be reported as full fidelity. Preserve review caps, required gates, artifact/workflow authentication and image limits. Keep draft; no merge, promotion or Figma writes.

Excluded: Connectors (current Notion consent versus Google Calendar success/failure mismatch), Check-in, Automations, experimental conversational onboarding, real audio/TTS/microphone, provider authentication, live app integration, global icon rules, scheduled conversations and CallKit. Existing #10/#12 cover broader real-service onboarding; this local fixture slice does not complete or close them.
