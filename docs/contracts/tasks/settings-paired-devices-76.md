# Task coverage — Settings · Paired devices (#76)

Code-only implementation of the approved **Paired devices** Agent-settings destination in both
SwiftUI and Compose, on the frozen Settings foundation API
([settings-foundation.md](../settings-foundation.md),
[settings-foundation-api.md](../settings-foundation-api.md)). Source masters and copy:
[settings-destinations.md › Paired devices](../../playground/settings-design/settings-destinations.md).
No Figma writes, no deployment, no main merge, no real service/credential calls.

## API entry points (public, deterministic)

| Platform | Entry point | Navigation ownership |
|---|---|---|
| SwiftUI | `SettingsPairedDevicesScreen()` — zero-arg | Drops into the host's outer `NavigationStack`; owns its **nested** list→detail with `NavigationLink`, never its own stack. |
| Compose | `SettingsPairedDevicesScreen(onBack: () -> Unit)` | Owns its own `Scaffold`, title, nested list↔detail route, and system-back (`BackHandler`): back from detail returns to list, back from list calls `onBack`. |

Root/integration owner wires the `pairedDevices` route to these entry points and owns real-host
end-to-end journey tests. `AgentSettingsDestination.pairedDevices` already exists in the foundation.

### New files owned by this task

- `Sources/RemDesignSystem/Screens/SettingsPairedDevicesScreen.swift`
- `Sources/RemDesignSystem/Screens/SettingsPairedDevicesFixture.swift` (pure model `PairedDevicesState` / `PairedDevicePeer` / `PairedDevicesCopy`)
- `tools/render-swift/Tests/RenderSnapshotTests/SettingsPairedDevicesFixtureTests.swift`
- `compose/RemDesignSystem/screens/SettingsPairedDevicesScreen.kt`
- `compose/RemDesignSystem/screens/SettingsPairedDevicesFixture.kt` (pure model twin)
- `compose/RemDesignSystem/src/test/kotlin/com/rem/designsystem/SettingsPairedDevicesFixtureTest.kt`

No central foundation files, playground hosts, workflows, Factory config, or tokens were edited.
(`compose/RemDesignSystem/build.gradle.kts` gained one dependency, `androidx.activity:activity-compose`,
required by the new screen's `BackHandler`; `tools/component-contract.baseline.json` grandfathers a
pre-existing foundation gap — see *Checks* and *Reconciliations*.)

## Source-node coverage

| Node | State | Represented | Notes |
|---|---|---|---|
| `1833:5031` | `pairedDevices.populated` | ✅ | Title `Paired devices`, trailing `Add`; section `Paired devices`; `Mac Studio` row with green dot + `Connected · Recently active`; footer `Open a device to review its connection or remove its access.` |
| `1833:52316` | `pairedDevices.empty` | ✅ | Centered `No paired devices`; the exact body copy; full-width filled `Refresh devices`; Large Subtle device hero. Reached after the last peer is removed. |
| `1833:52344` | `pairedDevices.device(id)` | ✅ | Hero `Mac Studio` / `Desktop`, Large Subtle device glyph; `Connection` rows `Platform → macOS`, `Status → Recently active`, `Last active → 1 min ago`, `Connected on → —`; destructive `Remove access`; footer `Removing this device revokes its access to Rem.` |
| `1839:52547` | `device.removeConfirmation` | ✅ | `Remove access to Mac Studio?` / `This device will be disconnected and will need to pair again.`; destructive `Remove access`, `Cancel`. Native action sheet (iOS `confirmationDialog`) / Material `AlertDialog`. |

### Behaviour verified by fixture-state tests

- Confirmed removal drops **only** the fixture peer and reaches the designed empty state; removal is scoped when more than one peer exists.
- Cancel preserves the peer and its detail (no mutation).
- The current device is **absent** from the fixture (only other paired devices appear).
- `Refresh` is a deterministic local re-check that never pairs a device (list unchanged, empty stays empty).
- Detail `Connection` rows and the `Connected on → —` em-dash placeholder match the source.
- `Connected · Recently active` summary derives from connection state (offline peers read `Offline · …`).

## Boundary gaps (explicit, not invented)

- **Add has no authored pairing destination.** `Add` opens a named local simulation boundary (a sheet
  on iOS, an `AlertDialog` on Compose) that states pairing happens on the other device and that this
  prototype does **not** simulate a handshake or add a device. No QR/pairing flow, no fake success.
- **No permission-scope editor.** The current masters have no raw permission-scope editor; none added.
- **Loading / error / offline** are test-harness scenarios, not Figma-approved Paired-devices screens;
  not fabricated here. (The foundation host already owns the agent-settings load/error/cancel flow.)
- `approvedAt` / device status values are illustrative fixtures only.

## Source reconciliations

- **Device glyph (Android).** iOS uses distinct SF Symbols — `macbook.and.iphone` (empty hero) and
  `laptopcomputer` (detail/section). The baked Material Symbols subset carries one device glyph
  (`devices`, U+E326, registry row *Paired devices*), so Compose uses `RemMaterialSymbols.Devices`
  for both. Semantic intent (a device) is preserved; adding a second codepoint would require
  regenerating the font subset and is out of scope. Recorded per icon-registry note on native
  silhouette differences.
- **List title casing.** The populated master titles the screen `Paired devices` while the empty
  master shows `Paired Devices`. Since both are one screen in different states, a single title
  (`Paired devices`, matching the populated master and the section header) is used; the empty
  master's capitalisation is treated as a source casing inconsistency.
- **`ListRowLabel` Code Connect gap (pre-existing, foundation lane).** CI
  (`Component architecture contract`) was red at the PR head because `Rows/ListRowLabel.figma.swift`
  is absent — the foundation lane ships `ListRowLabel` via parserless `code-connect/*/ListRowLabel.figma.ts`
  (the repo's current Code Connect path, per README), exactly like its already-grandfathered siblings
  `ListRow`, `RemSection`, `Section*`. The baseline simply hadn't grandfathered it. Resolved via the
  lint's documented pre-existing-gap remedy (`node tools/lint-components.mjs --write-baseline`), which
  added the single entry `ListRowLabel:figma`. No foundation source was changed.

## Accessibility identifiers (for root journey tests)

Shared across platforms (SwiftUI `accessibilityIdentifier` / Compose `testTag`):

| ID | Element |
|---|---|
| `pairedDevices` | Screen root |
| `pairedDevices.add` | Top-bar `Add` action |
| `pairedDevices.peer.<id>` | A device row (e.g. `pairedDevices.peer.mac-studio`) |
| `pairedDevices.empty` | Empty-state container |
| `pairedDevices.refresh` | Empty-state `Refresh devices` button |
| `pairedDevices.removeAccess` | Detail destructive `Remove access` control |
| `pairedDevices.confirmRemove` | Confirmation's destructive `Remove access` button |
| `pairedDevices.addBoundary` | Add simulation boundary (sheet / dialog) |

Compose-only: `pairedDevices.back` (top-bar back), `pairedDevices.removeConfirmation` (dialog).
iOS removal Cancel and the confirmation Cancel use the native `confirmationDialog` Cancel role.

## Checks run on this runner

- `node tools/lint-components.mjs` → **pass** (22 grandfathered, 22 current; no new violations).
- `swiftc -parse` on all three new Swift files → **pass** (syntax parse; SwiftUI/UIKit not importable
  on Linux, so full `swift build`/`swift test` is **not supported here** — hosted iOS-simulator CI
  owns compilation, the `RenderSnapshotTests` run, and rendered evidence).
- **Not supported on this runner:** `npm run check-code-connect` (no `node_modules`/`@figma/code-connect`
  installed; this task touched no `.figma.ts` files). Compose compilation / Android unit + instrumented
  tests require the Android SDK + Gradle and run in hosted CI.
- No visual/render verification is claimed; paired iOS/Android renders are produced by hosted CI.
