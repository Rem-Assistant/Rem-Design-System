Implement approved Settings prototype destinations in both SwiftUI and Compose, using the frozen Settings foundation API. This is a code-only child of Settings integration; no Figma writes, deployment, main merge, agent-routing labels, real service calls or credentials.

## Authority and mandatory references
Read docs/contracts/settings-foundation.md, docs/contracts/settings-foundation-api.md and docs/playground/settings-design/settings-destinations.md. Read the raw node contexts and screenshots for EVERY state in your assigned destination sections; they are source evidence, not app screen images. Follow repository skills. Utility contained icons use explicit Subtle; exact provider marks remain authentic. Use semantic tokens, native iOS List/Section/navigation/control semantics, shared Rem components and Compose equivalents. Never replace source screens with explanatory cards or generic forms.

## Ownership and integration
Create only the specified new screen/component files, destination fixture logic and tests/docs specific to this task. Do not edit central SettingsEntryContent, AgentSettingsContent, playground hosts, workflows, Factory config, tokens or other lanes' files. Root owns registering destination navigation and real-host end-to-end tests; expose a public zero-argument deterministic screen entry point for each destination, with a standard onBack callback on Compose. Each screen owns nested navigation and back behavior; avoid nesting native navigation containers in SwiftUI (outer host NavigationStack owns). Each Compose destination should own its scaffold/title and nested stack. Persistence is in-memory per playground session only; external boundaries are explicit prototype sheets, no invented provider screens/success state.

Add meaningful fixture-state tests where they prove scope/cancel/save, and document accessibility IDs for root journey tests and source-node coverage. All text entry is local illustrative data. Mask credentials/key drafts, never network/Keychain/shared preferences. No process/browser/simulator interactions with Trove.

## Completion
Produce finished code, not a plan. Run available narrow syntax/token/mapping checks and platform compilation if runner supports it. State exact checks run and any unsupported checks; do not claim visual verification without actual renders. Update task coverage doc with each approved source node represented, boundary gaps, API entry names and any source reconciliations. Keep PR draft against codex/settings-integration; do not merge or deploy.

## Assigned scope
Implement Memory and Models plus summary, composer boundary, provider key empty/picker/focus/filled states. Entry names: Swift SettingsMemoryScreen, SettingsModelsScreen; Kotlin equivalents with onBack: () -> Unit. Own corresponding new screen files and uniquely named fixture/helpers. Extend shared RemComposerBar additively only if required to hide model/Speak slots while keeping prior API defaults; coordinate root before touching shared file.

Memory exact three independent toggle defaults, exact summary copy and metadata. Interactive bottom composer Ask or update memory with plus/send only, local deterministic send boundary. No invented post-send chat/attachment flow; explicit fixture feedback. Models Auto and saved Anthropic flag are independent because mutual-exclusion unspecified; don't invent production model policies. Provider picker has Anthropic/OpenAI/Google/Mistral/OpenRouter. Native password-style key input accepts illustrative dummy drafts only in memory, disabled empty Save, Back discards, Save changes local saved-key flag. Never provider verification, secure storage or network. Keep source footer/copy with a playground-wide mock-data explanation rather than extra implementation text in designed rows.

## Delivery coverage (implemented 2026-10-08)

### Files (new, lane-owned — no central/host/shared file was edited)

| File | Role |
|---|---|
| `Sources/RemDesignSystem/Screens/SettingsMemoryScreen.swift` | `SettingsMemoryScreen` (public, zero-arg) + `MemoryFixture` + nested `MemorySummaryView` + interactive composer |
| `Sources/RemDesignSystem/Screens/SettingsModelsScreen.swift` | `SettingsModelsScreen` (public, zero-arg) + `ModelsFixture` + nested `AddProviderKeyView` + provider picker |
| `Sources/RemDesignSystem/Screens/SettingsDestinationSupport.swift` | `PlaygroundMockData.hint`, `View.settingsDestinationList()` |
| `compose/RemDesignSystem/screens/SettingsMemoryScreen.kt` | `SettingsMemoryScreen(onBack)` + `MemoryContent` + nested summary + interactive composer |
| `compose/RemDesignSystem/screens/SettingsModelsScreen.kt` | `SettingsModelsScreen(onBack)` + `ModelsContent` + nested Add provider key + picker |
| `compose/RemDesignSystem/screens/SettingsDestinationSupport.kt` | `PlaygroundMockData.hint` |
| `tools/render-swift/Tests/RenderSnapshotTests/SettingsMemoryModelsFixtureTests.swift` | pure fixture logic tests (not UIKit-guarded; run by `swift test`) |
| `compose/RemDesignSystem/src/test/kotlin/com/rem/designsystem/SettingsMemoryModelsFixtureTest.kt` | pure fixture logic tests (JVM JUnit) |

Entry points: Swift `SettingsMemoryScreen()` / `SettingsModelsScreen()`; Kotlin `SettingsMemoryScreen(onBack)` / `SettingsModelsScreen(onBack)`. SwiftUI relies on the host `NavigationStack` and registers nested routes with `navigationDestination`; each Compose destination owns its `Scaffold`/title and a nested root↔child stack driven by `rememberSaveable` state.

### Source node coverage

| Node | State | Representation |
|---|---|---|
| `1833:5096` | Memory root | three independent toggle rows (Search and reference chats ON, Generate memory ON, Sensitive topics OFF) + controls footer; Overview section with navigable Memory summary row + footer |
| `1865:6193` | Memory summary | exact metadata + Overview / How Rem should work / Current focus copy; bottom composer "Ask or update memory" with plus + send only |
| `1833:5109` | Models root | "Rem" → Auto ON + footer; "API keys" → Anthropic (subtitle "Saved", disclosure paired with content, availability switch OFF in trailing) + "Add provider key" disclosure |
| `1956:8163` | Add provider key (semantic screen) | Provider row + native picker; masked password-style "API key" field; footer; single Save (disabled empty) |
| `1934:9067` | Empty draft | empty key field → Save disabled |
| `1934:9068` | Provider picker | native menu: Anthropic / OpenAI / Google / Mistral / OpenRouter (checkmark on selection) |
| `1934:9069` | Filled draft | masked key value → Save enabled |
| `1934:9070` | Editing API key | focused native password field (caret + keyboard) |
| `1870:54593` | recovered provider menu | the five-provider list in source order |

### Accessibility IDs (for the root journey tests)

- Memory: `settingsMemory`, `memory.toggle.{searchAndReference,generateMemory,sensitiveTopics}` (Swift) / `memory.toggle.{SearchAndReference,GenerateMemory,SensitiveTopics}` (Compose enum names), `memory.summaryRow`, `memorySummary`, `memory.composerField`, `memory.composerSend`, `memory.composerFeedback`.
- Models: `settingsModels`, `models.toggle.auto`, `models.providerRow.anthropic`, `models.toggle.anthropicAvailable`, `models.addProviderKey`, `modelsAddKey`, `models.providerPicker`, `models.providerOption.<Provider>`, `models.keyField`, `models.saveKey`. Compose sub-screen back is `back`.

### Boundary gaps (exposed, not invented)

- Memory composer has **no** post-send conversational reply, attachment picker, or persisted/generated memory — a nonempty draft yields one deterministic fixture line and clears; a blank draft is a no-op.
- Models: Auto↔provider mutual-exclusion, saved-key edit/delete lifecycle, real validation rules, and provider model lists are unsettled in the source and are **not** invented. The Anthropic row and "Add provider key" both open the single authored Add-provider-key destination; a distinct "edit saved key" flow is not fabricated.
- No provider is contacted; Save records only an in-memory dummy saved-key flag and never retains the key draft (never Keychain/shared-preferences/network).

### Source reconciliations

- **Composer:** the shared `RemComposerBar` (iOS + Compose) is a static presentational component that always shows the model selector and Speak pill. Rather than mutate that frozen shared API, the interactive Memory variant (plus + send only, editable field, deterministic send boundary) is owned by the summary screen, reproducing the composer pill's tokens/shape. The additive `showModel`/`showSpeak` extension to the shared file was therefore not required and the shared file was left untouched.
- **Add provider key field states:** the Figma floating subordinate "API key" label in the editing state is a design affordance; the native `SecureField` (iOS) / `BasicTextField` + `PasswordVisualTransformation` (Compose) uses the platform placeholder/caret/keyboard instead. All four authored states (empty, picker, filled, editing) are expressed by this one native masked field plus the native menu.
- **Anthropic saved subtitle:** the source root render clips the "Saved" subtitle (recorded source mismatch); it is rendered correctly here.
- **Mock-data explanation:** surfaced once per destination as a screen-level accessibility hint (`PlaygroundMockData.hint`) rather than as extra text inside the designed rows; the authored footers/copy are kept verbatim.

### Checks run on this runner

- `swiftc -parse` on all four new Swift files — passed (syntax clean). Full SwiftUI compilation is **unsupported here** (no SwiftUI module / no macOS toolchain on Linux); hosted macOS CI owns it.
- `node tools/generate-tokens.mjs --check` — passed (tokens in sync). `git diff --check` — clean.
- `node tools/lint-components.mjs` — the only failure is the pre-existing, baseline-un-ratcheted `ListRowLabel:figma` gap (a foundation-owned file in `Rows/`, present at branch head before this work and outside this lane); `Screens/` is exempt from the component contract, so these new screens add no new violations.
- Kotlin/Compose compilation and both platforms' UI/interaction tests are **unsupported here** (no Android SDK; no SwiftUI) and must run in hosted CI. The pure fixture tests are authored for `swift test` / JVM JUnit and were not executed on this runner for the same toolchain reasons.
