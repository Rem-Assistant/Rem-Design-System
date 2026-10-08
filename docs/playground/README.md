# Rem Settings New — native engineering trial

This is a local, bounded playground, tracked by [issue70](https://github.com/Rem-Assistant/Rem-Design-System/issues/70). It is not integrated into the production Rem app. [Design and behavior contract](../contracts/settings-playground.md).

## Current integration — October 8, 2026

Draft [PR73](https://github.com/Rem-Assistant/Rem-Design-System/pull/73), branch
`codex/settings-integration`, now connects Paired devices, Connectors, Cloud browser, Memory,
Models, Wallet, and Voice on both platforms. Automations remains unavailable. These are local
fixtures: there is no real pairing, OAuth, payment, audio playback, account deletion, key persistence,
or service access. Entry references such as Billing and Help & Support are still noninteractive.

Open Settings, then **Agent settings**. iOS uses native SwiftUI `List`, `Section`, and
`NavigationLink` containers with shared row content; Compose uses native Material controls and
the shared Rem row/section components. Utility icon containers use Subtle; provider brand marks
and explicit preview/action accents retain their intended treatment.

The entry name, Help & Support question-mark icon, and removal of the entry row's extra minimum
height are approved code amendments. Saved Figma references preserve their original content.
Code Connect mappings are checked locally; publication remains blocked by the Figma plan entitlement.

Three bounded Factory batches delivered Devices, Memory/Models, and Cloud browser candidates.
Their successful delivery is separate from runtime and visual verification. Root integration repairs
and destination tests are tracked in [the verification record](verification.md). The records below
describe the earlier two-screen baseline and are retained for provenance; their four-test results
and zero-Factory-run count do not describe the expanded integration.

## Run

- **iOS:** open `tools/playground-ios/RemSettingsPlayground.xcodeproj`, choose the RemSettingsPlayground scheme and a dedicated simulator, Run. [CLI/test instructions](../../tools/playground-ios/README.md).
- **Android:** open `compose` in Android Studio, select `demo`, Run. Existing wrapper requires JDK17 and Android SDK34. [CLI/test instructions](../../compose/README.md).
- Start at the gallery. Choose Success/Slow/Error, open Settings, tap Agent settings. Use Back/Cancel or Retry as appropriate. Shared controls supports a local toggle and name edit with Save/Cancel.
- Destination removals and saves affect local fixtures only. There is no connection to real accounts or services. Automations remains outside scope.

## Sources and baseline

- Current app: `Rem-Assistant/Rem`, verified main `274a04ccb69928218d0458d3805238cab1e6eaaa`, SwiftUI/RemKit plus Node/TypeScript backend. The trial uses the existing design-system native stack, not a new web framework.
- Design system: `Rem-Assistant/Rem-Design-System`, baseline `claude/ds-flows@93d8588c8a020f2ed25c1ca69b2144c1c06d5838`. A small selected set of pre-existing local row/section changes and Android demo/wrapper files was imported; [hash manifest](imported-baseline.json) records provenance. Original checkout remains untouched.
- Live Figma: [Settings New page1825:29320](https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/?node-id=1825-29320), entry1964:86819 and Agent Settings1827:50855. Saved source exports are in `evidence/figma-*.png`.
- Factory: `samuelalake/agent-factory`; consumer pin `bd7816b3b40938c3103ba93fc75e2fb8ae774ec0`, verified upstream main `3a767861cebce2f08f21639297ab8693b77f9a4f`. [Workflow board](https://www.figma.com/board/QLfiGNM512vYQJgcpE0Um1/Agent-Factory-workflow).

## Experiment boundary and overhead

Reuse one persistent engineering hub and the existing native controls for one settled two-screen flow. Success requires both runnable apps, focused interaction tests, paired native evidence and independent review. Do not infer production readiness from a gallery.

Factory dispatches: **0**. Current builder forces `base_ref: main` and enables Figma writing. This trial needs the current DS flow branch plus preserved local scaffolding, with Figma read-only. No infrastructure was changed to force that mismatch. Future work should explicitly support selecting an exact baseline and a code-only lane before comparing Factory execution against direct work. There is no measured Factory-versus-direct speed result from this trial.

Direct execution costs observed: authorization handoff required a direct user message; installed Java25 was incompatible with Gradle/Kotlin and existing Java17 resolved it; CoreSimulator refused SSD device-set initialization; concurrent runtime testing filled internal storage. Only the task-created iOS simulator was removed, recovering space, and platform tests were serialized. No user simulator was erased, no SDK installed, no Factory/Figma writes, no push/merge/deploy.

Independent review found useful issues before delivery: Android icon/switch accessibility, source-specific chevrons/title, literal iOS email text, and a loading screenshot captured before rendering. These were repaired within the slice. Any remaining independent-drive or visual limitation is recorded in `verification.md`; green builds alone are not treated as full delivery proof.

## Next bounded step

After this slice is accepted, choose exactly one settled Agent Settings subflow and add it to the same gallery using the same primitives. Keep Automations excluded until its design is settled. Record start/end state, Back/Cancel/error behavior and evidence on the single flow issue. Avoid adding more hubs or agents for symmetry.
