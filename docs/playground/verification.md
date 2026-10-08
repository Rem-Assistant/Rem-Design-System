# Verification record

## Expanded integration — October 8, 2026, verification in progress

- All seven designed destinations are connected in the iOS and Android playground hosts. No production integration or real service actions are implied.
- Hosted run [37747643483](https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/37747643483), commit `3e37ab0`: Android's 14 earlier journeys passed; iOS passed seven foundation journeys and failed seven destination journeys. Both library render jobs passed.
- Expanded run [37749685731](https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/37749685731), commit `af733c6`: iOS completed 25 tests, seven passed and 18 failed at destination entry. Its overall job was subsequently cancelled when replaced; retained build log and screenshots establish those individual results. Actual images show a highlighted Agent settings row without navigation. Moving the value-route registration to the root did not repair this. Commit `e9805c3` uses direct native destination links instead; runtime confirmation is pending.
- That expanded run's Android test target failed compilation because text/IME action functions return `Unit`; commit `ee2506e` separates those test calls. App/library render jobs passed. Replacement run [37750545192](https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/37750545192) is tracked separately.
- Root inspected actual light/dark and selected large-text screenshots. The fresh iOS entry shows the approved Agent settings label, question-mark Help & Support icon, and equal profile/agent row heights. Earlier Android captures exposed a purple checked-switch thumb in dark mode (repaired in `97e3d13`) and a stale previous-screen image named Memory summary (platform capture settling added in `f242938`). These repairs require fresh runtime captures; this is not a blanket visual pass.
- Read-only Figma foundation verification [37746071402](https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/37746071402) passed for entry `1964:86819` and Agent settings `1827:50855`. It does not cover every destination/state.
- Local Code Connect type checks and both platform parsers passed, including ConnectorRow mappings. Publishing is not confirmed; Figma plan entitlement remains a blocker.
- Generated design evidence was archived losslessly with an independently verified SHA-256/size manifest. This reduces the PR from 383 to 278 files and removes GitHub's 300-file diff transport blocker. The approximately 1.71 MB diff still exceeds the unchanged 750,000-byte automated review cap; it must not be treated as fully reviewed or merged.
- Pre-existing vendored skill changes remain inconsistent with their recorded upstream pin. They have not been reset or silently repinned.

## Historical two-screen baseline — October 7, 2026 (America/Los_Angeles)

The following results apply only to the original bounded baseline, not the expanded seven-destination integration.

## Established

- iOS native app compiled with Xcode26.3 / iOS26.2 SDK. Final XCTest run on dedicated iPhone16 Pro / iOS18.6: **4 tests, 0 failures**, 56.908s test time. `evidence/ios-interaction-tests.log` ends in TEST SUCCEEDED. Raw bundle: `/Volumes/SatechiSSD/CodexBuilds/rem-settings-20261008/ios18-tests.xcresult`.
- Android debug app and instrumentation APK compiled with existing Java17, Gradle8.14.4, SDK34. Native API34 arm64 emulator: **4 tests passed**, 46.394s. Corrected loading screenshot synchronization then passed the focused cancellation test again: **1 test**, 12.854s. Logs: `evidence/android-interaction-tests.log`, `evidence/android-cancel-final.log`.
- Existing Android library suite: **39 tests, 0 failures** (34 EvidenceSnapshots, 3 Material Symbols checks, 2 sign-in interruption tests). `evidence/android-library-tests.log`. This is the existing library suite, not 39 new Settings checks.
- Interactions: Settings → Rem → Agent settings → Back; simulated error → Cancel and Retry → success; cancel a slow load and remain at Settings beyond its completion deadline; edit name → Cancel preserves original → Save commits. Android also asserts toggle state and accessible name.
- Current paired native captures: entry, Agent settings, loading, error, controls; iOS also records the post-cancellation screen. Source exports are `figma-*.png`. iOS18 uses its native navigation bars, so it does not reproduce iOS26 glass chrome.
- Independent reviewer found **no remaining blocking code or visual defects** under the bounded contract. Reviewer inspected exact current source, the paired five-state images, test logs and all14 imported-source hashes. Original working-copy files still match the import manifest.
- `git diff --check` clean. No SDK/tool installation, Figma write, Factory dispatch, push, merge or deployment.

## Review repairs

Repaired Android switch labeling, decorative glyph semantics and clickable row test target; removed source-inaccurate profile/share chevrons; matched title case; rendered the sample email literally; waited for the actual loading state before Android capture and native transition settling before iOS capture. The original iOS cancellation assertion used a one-second polling expectation after a three-second fixture; final fixture is ten seconds and checks the real destination directly after its deadline.

## Limits — do not collapse these into a full delivery-gate pass

- Independent **manual GUI drive remains unverified**. CUA could not bind Android Emulator; the iOS app binding returned `failedToCreateImageDestination`. Native automated interactions and independent screenshot review did complete. No manual-drive claim is made.
- An earlier iOS26 run passed3/4 tests before the cancellation test was corrected. A second run was interrupted when internal disk space was exhausted; its result bundle is diagnostic only. The final green run is iOS18.6, not iOS26.2. No final iOS26 interaction-pass claim is made.
- Internal storage remains limited. Both task-created iOS simulators were removed after evidence export; original Trove ProMax and RemAgent devices were not changed. The task Android emulator was shut down; its data remains on the SSD. App binaries and result bundles remain on the SSD.
- The Android connector symbol is a recorded native semantic candidate, not final design approval of the encircled-link translation. Dynamic Type extremes, screen-reader driving, tablet layout and production integration have not been verified by this slice.
- Other Settings rows are intentionally visual references. Automations and real account/backend actions remain out of scope.
- Factory was not exercised: forced main baseline and enabled Figma writes conflict with the trial. No comparative performance claim is supported.

## Runnable artifacts

Checksums and absolute artifact paths are recorded in `artifacts.json`.

- `/Volumes/SatechiSSD/CodexBuilds/rem-settings-20261008/RemSettingsPlayground-iOS-simulator.zip` — ARM iOS Simulator app; unzip, install on a dedicated simulator with `xcrun simctl install <UDID> <app>`, then `xcrun simctl launch <UDID> com.rem.playground.settings`.
- `/Volumes/SatechiSSD/CodexBuilds/rem-settings-20261008/android/demo/outputs/apk/debug/demo-debug.apk` — Android debug APK; install on a dedicated device with `adb -s <DEVICE> install -r <APK>` and launch `com.rem.designsystem.demo/.MainActivity`.

[Run/build instructions](README.md) | [Source contract](../contracts/settings-playground.md)

| State | iOS | Android |
|---|---|---|
| Settings entry | [PNG](evidence/ios-settings-entry.png) | [PNG](evidence/android-settings-entry.png) |
| Agent settings | [PNG](evidence/ios-agent-settings.png) | [PNG](evidence/android-agent-settings.png) |
| Loading | [PNG](evidence/ios-loading.png) | [PNG](evidence/android-loading.png) |
| Error | [PNG](evidence/ios-load-error.png) | [PNG](evidence/android-load-error.png) |
| Controls | [PNG](evidence/ios-controls.png) | [PNG](evidence/android-controls.png) |
