# Fidelity Verification — Figma component library vs. the real Rem app

This is the record for the **Mac-verify fidelity** step: every Figma component in the
library was checked against the **real SwiftUI render** of the shipping Rem app, using two
grounds of truth in combination:

1. **Real-app screenshots** — the committed iOS 26 captures in the Rem repo
   (`docs/screenshots/*.png`: `01-agenda`, `02-daily-brief`, `03-task-detail`, `05-connectors`,
   `06-chat`) plus the `00-hero-agenda` hero. These are driven from the shipping views via DEBUG
   fixtures (mock data only).
2. **SwiftUI source** — the actual component code (`ChatMessageViews.swift`, `SharedRemChatView.swift`,
   `TaskEventView.swift`, `SuggestedTaskRow.swift`, `ContentView.swift`, `DesignTokens.swift`, …),
   read for exact values (corner radii, font sizes/weights, paddings, colors, glyph names).

The automated leg is the **macOS `visual-verify` CI** in `Rem-Assistant/Rem`
(`.github/workflows/visual-verify.yml`): it builds the iOS app on a macOS runner and screenshots
each `--rem-<screen>-fixture`. As of this pass it is **green** on the design-sync branch
(`claude/rem-design-system-sync-zyfjy5`, run #6, commit `d31e59d`) after two rebrand-miss build
bugs were fixed (see *Build fixes* below), so the pipeline that renders the ground-truth
screenshots is live and reproducible.

Fidelity target: **iOS 26** (the version Rem ships screenshots from today).

---

## Check-in cadence step — paired iOS + Android per contract — 2026-09-29 (issue #52)

- **Screen:** onboarding **Check-in cadence** — "When should Rem check in?" — reproduced to
  `docs/contracts/onboarding-checkin.md`. Authority: the shipping `CheckinsService` / `Checkin` cadence
  model + the founder reference frame `tasks/refs/onboarding/04-checkin.png` (the "Saving…" state).
- **Paired code:** SwiftUI `Sources/RemDesignSystem/Templates/OnboardingCheckinTemplate.swift` and
  Compose `compose/RemDesignSystem/onboarding/CheckinStep.kt` (`OnboardingCheckinScreen` /
  `checkinStep(status:…)`, hosted by `OnboardingSequencer`). Both render the same scaffolded step (hero
  → title → body → grouped cadence list) with an ActionArea inside the fill-height Body whose label +
  affordance track the five save-lifecycle states, and a transient error Toast above the ActionArea.
- **New canonical component:** `RemSwitch` (`Controls/RemSwitch.swift` · `controls/RemSwitch.kt`) — a
  thin native-switch wrapper pinned to the `systemGreen` on-tint, Figma `RemSwitch` set `868:210`
  (legacy iOS-on variant `110:50`), Code Connect
  `code-connect/{swiftui,compose}/RemSwitch.figma.ts`. First consumer is this screen; extracted because
  a grouped-settings toggle recurs across Settings/automations/voice.
- **Reuse:** `RemSection` + `ListRow`, `ContainedIcon` (hero + row leadings), canonical `ActionArea`,
  explicit Button states, and canonical `Toast` (`72:24`, SwiftUI/Compose auto-dismiss after 4s), plus
  generated tokens. The value pill is local (`TimePill` / `CheckinTimePill`), flagged for
  extraction.
- **Icons:** four new registry rows — hero `clock.badge.checkmark.fill` / `alarm_on` (FILL 1) and the
  Morning/Midday/Evening leadings `sunrise`·`sun.max`·`moon.stars` / `wb_twilight`·`wb_sunny`·`bedtime`
  (FILL 0). Android renders these from the Material Icons vector set (Outlined/Filled), a documented
  FILL-honouring divergence from the baked Material Symbols subset (which cannot be regenerated on this
  runner — no `fonttools`/network). See `docs/contracts/icon-registry.md` ‡.
- **Mapping boundary:** the `CheckinSlot` / `Checkin` adapter (`CheckinCadenceAdapter.{swift,kt}`)
  accepts the raw shipping `Checkin` fields (`slot`, `enabled`, `deliveryHour`, `deliveryMinute`,
  `timezone`), validates the slot against `morning | midday | night`, formats the hour/minute into the
  brief-time label internally (no host display string), produces the canonical rows, and forwards toggles
  as a canonical `CheckinSlot` — so the `Checkin` identity (display "Evening" / send `night`) holds
  without the host remapping.
- **Evidence:** the five states — `checkin-default-light`, `checkin-edited-light`, `checkin-saving-light`,
  `checkin-saved-light`, `checkin-failure-light` — via `RenderSnapshotTests` (iOS) and `EvidenceSnapshots`
  (Compose/Paparazzi), declared in `tools/render-evidence/contracts.json` as `onboarding-checkin`. The
  Android `checkin-saving-light` / `checkin-saved-light` CTA now uses the same explicit semantic
  disabled container and label roles as Figma and SwiftUI, with no whole-control opacity. The adapter +
  save-lifecycle model is unit-tested on both platforms (`CheckinCadenceTest`,
  `CheckinInteractionTests` — 8:00 AM, a non-zero minute, the rejected invalid `evening`, and the `night`
  toggle/update payload).
- **Editable Figma delivery:** a founder-authorized direct Codex Figma session authored and read back
  `Screen/Check-in` (`876:1121`) with
  Default / Edited / Saving / Saved / Failure variants, documentation section `02A` (`890:1502`), and
  prototype section `02B` (`885:1121`). Direct 402×874 instances are registered as exact references
  in `tools/render-evidence/contracts.json`; the prototype begins at Default and covers
  Default → Edited → Saving → Saved plus Failure → Saving retry. The canonical screen now uses
  `StatusBar → TopBar → Body → NavigationIndicator`, an official iOS top toolbar, and a fill-height
  Body with one 24pt inset and `VStack/Content` + `VStack/Actions`. It uses HUG Section rows with
  transparent ListRows, official iOS/Material Switch instances behind `RemSwitch`, HUG `ActionArea`
  with Footnote off, canonical Toast in Failure, and explicit disabled Button variants. Inventory
  Section `00` now contains one auto-layout HStack, so its spacing heals after
  insertion or deletion. The exact-head CI rerun binds this live structure to the final commit; it
  does not misattribute the direct interactive edit to the hosted Factory writer.

## Consent flow + component-documentation proving pass — 2026-09-28 (issue #30)

- Current shipping `AIDataSharingConsentView` has no consent-local loading/error state. The speculative
  Figma frames and paired snapshots were removed; evidence now covers Consent, Terms, and Privacy.
  Accept remains without a prototype destination because Deploying is being deprecated and no
  replacement onboarding step is approved.
- The existing `Onboarding` page now holds every current onboarding artifact in named Sections:
  canonical screen components `00` (`760:21`), consent documentation `01A` (`777:432`), and consent
  prototype `01B` (`731:260`). Sign-in and the three consent masters are
  consolidated in `00`. Documentation is
  an attached `Mobile Flow Documentation` instance with nested Sections, Rows, Steps, Placeholder,
  and Screen slots. Prototype screens are direct canonical instances and Consent is a registered flow
  starting point. Later flows add their own `02A` / `02B` pairs on this page.
- Added canonical `Section` (`741:311`) with optional Header/Footer and an editable Rows slot whose
  preferred value is `ListRow`. The grouped surface uses `backgroundSecondary` + 24pt radius with no
  outer stroke. Consent uses Section instances in its documented and prototype frames.
- Added a SwiftUI `RemSection` adapter for custom `ScrollView` surfaces and a Compose `RemSection`
  implementation plus parserless SwiftUI/Compose Code Connect templates. Native SwiftUI `List`/`Form` consumers should
  continue to use `SwiftUI.Section`; both implementations translate the same Figma concept. Swift
  package compilation passes locally; Android compilation remains a hosted-run check because this
  checkout has no Gradle wrapper/toolchain.
- Component documentation now uses the lightweight `Component Documentation` pattern: canonical master
  first and a compact overview second, both nested in one auto-layout block inside `Rows & Sections ·
  Component documentation` (`741:309`). Full Specs-plugin output is preserved separately and is not
  the Builder fidelity bar; obsolete loose Section templates were removed from `Accessories`.
- Added reusable composition masters on the dedicated Compositions page (`826:482`): `ButtonGroup` (`773:17`, Vertical /
  Horizontal with Actions slot), `Lockup` (`773:22`), and `ActionArea` (`773:28`). Their names are
  domain-neutral so the same anatomy can serve later non-onboarding screens.
  The masters live in `Composition components` (`773:2`) as three horizontal documentation
  columns. Each column places the canonical master first and the attached `Component Documentation`
  metadata instance below it. The template has no Component slot and does not repeat the specimen.
  Composition roots inherit their parent surface. `ButtonGroup` fills its
  parent and both horizontal actions divide that width equally. `Lockup` and `ActionArea` fill their
  parent up to the 560-point onboarding content maximum; Lockup text, the Button Group slot, and the
  wrapping Footnote fill that responsive width in Figma, SwiftUI, and Compose rather than freezing
  the reference iPhone's 354-point content width. The screen owns its horizontal inset.
  The Components page now remains the Component Index only. The empty Guide page was removed;
  Foundations remains the visible reference for Rem-owned colors, typography, spacing, radius, and icons.
  Canonical Button labels now bind local styles (`Body/Bold`, `Body/Emphasized`, or
  `Subheadline/Emphasized`). They are the first fixture in a data-driven component-quality audit;
  the governing rubric scores typography, semantic color, layout, reuse, and taxonomy for every
  touched family across Foundations, Primitives, Compositions, Templates, and Screens.
  Primitives now precedes Compositions in the page list. Component-family pages use fill-free,
  stroke-free canvas Sections; the shared documentation template owns the variable-bound
  `background/primary` surface instead of borrowing presentation from its canvas Section.
  The three canonical screen masters (`777:248`, `777:325`, `777:392`) are reused by both documentation
  and prototype, so changing a screen no longer requires synchronizing detached copies.
- The extraction rule is now explicit for Builder: propose a component when a region has one stable
  responsibility plus two plausible consumers or cross-flow recurrence, while leaving one-off
  wrappers local. Theme (Light/Dark) and Platform (iOS/Android) remain independent axes; Status Bar
  uses platform switching and retains theme switching within each platform treatment.
- Added canonical `StatusBar` (`785:389`) and `NavigationIndicator` (`793:379`) component sets with
  iOS/Android Platform and dark/light content options. Every canonical onboarding screen includes
  both; legal sheets render above the status bar. The product layout remains shared across platforms.
- Canonical screen layers now use semantic regions first (`StatusBar`, `Body`, `ActionArea`,
  `NavigationIndicator`) and SwiftUI-first structural names (`VStack`, `HStack`, `ZStack`). Compose
  maps them to `Column`, `Row`, and `Box`. Empty spacer frames were removed in favor of semantic
  groups using auto-layout `SPACE_BETWEEN`.
- Foundations follow-up will map the iOS 26 kit's source color and type styles into Rem-owned
  variables/styles. The kit is a source reference, not a runtime dependency of the finished library.

---

## Sign-in screen — centered, paired iOS + Android per contract — 2026-09-28 (issue #27)

Re-scoped the onboarding **sign-in** screen to conform to `docs/contracts/onboarding-sign-in.md` on
**both** platforms — the first per-screen re-scope of the flow (parent #10) and the first Builder PR
the visual-parity gate polices. Authority: the contract (founder-approved 2026-09-28) + `signInContent`
/ `SignInButton` / `OnboardingLogoView` + `tasks/refs/onboarding/01-sign-in.png`.

- **Arrangement fix (the #21 drift):** sign-in is now its **own centered screen** on both platforms —
  one block centered in the safe area (equal space above/below, **never** bottom-pinned), contents
  left-aligned, ≤560. Android previously hosted sign-in in `OnboardingScaffold`'s bottom-pinned CTA
  bar; it now renders the standalone `OnboardingSignInScreen` (sibling of the SwiftUI
  `OnboardingSignInTemplate`). `signInStep(...)` wraps it as the sequencer's entry.
- **Emphasis:** "Sign in with a different account" is a quiet `labelSecondary` text link in **every**
  state on both platforms — never filled. This resolves an internal contradiction in the contract: its
  state-table originally listed different-account as recovery's *filled primary*, which violated the
  contract's own emphasis rule (and #27's acceptance criterion). Resolved in favour of the emphasis
  invariant (the drift #27 exists to kill): **recovery's filled primary is "Try again"** (re-auth, like
  the error state) and different-account stays the quiet link. Recorded as a bounded amendment in the
  contract, escalated to Steward/founder. Provider order Google→Apple in `new` (fixed on Android, was
  Apple→Google).
- **Notice card:** error/recovery notice sits directly **below** the action group (was above, in the
  Android lockup), `systemRed` @ 12% on a `medium`-radius surface, `caption1` message + a leading
  warning glyph resolved through the icon registry by meaning **and FILL**: iOS
  `exclamationmark.triangle.fill`, Android Material Symbols `error`, both **FILL 1**.
- **Material Symbols on Android (registry resolves):** added the Apache-2.0 Material Symbols variable
  font (`src/main/res/font/material_symbols_outlined.ttf`, subset to `error`/U+E000 with `fontTools`,
  FILL axis preserved) + `icons/RemMaterialSymbols.kt`, bound via `FontVariation` FILL — **not** the
  always-filled legacy `Icons.Filled.*` (registry rule 2).
- **Render evidence (gate input):** all five contract state keys now render on both platforms —
  `SignIn-returning-light`, `SignIn-new-light`, `SignIn-checking-light`, `SignIn-error-dark`,
  `SignIn-recovery-light` — via `RenderSnapshotTests` (iOS) and `EvidenceSnapshots` (Compose/Paparazzi),
  paired by `screenshots.yml`. `checking` + `recovery` cases were added on both sides.
- **Not verifiable in-repo:** no iOS/Android toolchain here (the reference-vs-evidence split) — the
  paired render on the runners is the parity proof the gate diffs.

---

## Onboarding sequencer + sign-in/consent reproduction — 2026-09-26 (issue #11)

Built the native **onboarding sequencer shell** (order · progress · Continue/Skip forward+back) and the
two **reproduced steps** hosted in it, as the Compose siblings of `OnboardingFlow.swift`
(`compose/RemDesignSystem/onboarding/`). **No deploy/provisioning slot** is present in the path (the
sequencer injects no steps of its own — the path is exactly the ordered slots the host passes); the
deploy code itself is retired later with the runtime migration (out of scope here).

- **Reproduce grounds (authoritative reference):** the founder onboarding reference frames committed at
  `tasks/refs/onboarding/01-sign-in.png` (sign-in, returning state) and `02-consent.png` (consent),
  plus the app sources they cite (`OnboardingFlow.signInContent`, `AIDataSharingConsentView`) and the
  Figma masters Login `411:15` / Privacy `410:16`. Consent copy is transcribed **verbatim** into
  `ConsentStep.kt` (title/subtitle, both legal-row titles + subtitles, CTA, footer). Sign-in reproduces
  the left-aligned brand lockup, the "Continue as <name>" Sign-in-with-Apple returning treatment, and
  "Sign in with a different account".
- **Real auth, no mock:** sign-in is state-driven (`SignInState` = returning / new / checking / error /
  recovery); the host maps its real auth to these and advances only on success. checking = disabled
  spinner CTA; error/recovery are an **Extend** treatment (no reference frame) reusing a token-bound
  inline message — flagged for founder confirmation.
- **Token-bound + canonical reuse — honest `composed_of`:** every visual binds to `RemTokens`, and the
  hero + consent-row leading icons reuse the canonical `ContainedIcon` (anti-drift Rule 0). That is the
  **only** canonical reuse this surface performs. The CTA button (`OnboardingActionButton` in
  `OnboardingScaffold.kt`) and the consent legal rows (`ConsentLegalRow` in `ConsentStep.kt`) are
  **hand-rolled, not instances of the canonical `Button` / `ListRow`** — no Compose primitive exists for
  either yet, so both are **forked-pending-native and flagged for extraction** (the same flag
  `OnboardingSupport.kt` already carries for the button now covers the row too). The manifest reflects
  this: `composedOf: [ContainedIcon]`, `pendingNative: [Button, ListRow]`; `OnboardingSequencer.md`
  no longer claims Button/ListRow reuse. Flagged metric debts (centralized, not
  call-site literals): onboarding hero may want a dedicated size token (reuses `ContainedIcon` Large
  today); disabled-CTA alpha and the progress-dot sizes have no token yet; iOS glyphs (`doc.text`,
  `shield`, `lock.shield.fill`, Apple/Google marks) diverge to Material vectors / overridable params
  (brand-asset debt), per the repo convention.
- **Progress indicator is Extend:** the reference frames show no progress affordance; a quiet
  token-bound step-dots indicator was added as part of the Extend shell and flagged for founder
  confirmation of style/placement.

**Not verified in this checkout (reference-vs-evidence split):** Compose compilation, and the iOS +
Android light+dark render evidence for each sign-in state + consent, are the render runners' job
(`visual-verify` / the Android runner) — this repo has no iOS/Android/Figma toolchain. The clean-slate
drive + screenshot diff against `01-sign-in.png` / `02-consent.png` is the Verify stage's gate.

---

## iOS 26 foundation + variable-binding pass — 2026-09-24

Re-based onto the **official iOS 26** library and bound colors to the local **Color** variables
(Light/Dark). Self-verified each via screenshot.
- **Nav → official iOS 26 `Toolbar-Top-iPhone`** (Chat: status bar + centered title + Liquid Glass
  ⋯, set via the button's `Symbol` **text** property). Retired the iOS 18 `Navigation Bar`.
- **VoiceBar — all 6 states**: real SF Symbols (`microphone.fill` / `microphone.slash.fill` /
  `phone.down.fill`), flush bars (no radius), colors bound.
- **Row** — kept custom **`ListRow`**: the kit iOS 26 `Row` has no instance-swap and its leading
  `Image` (`Type`=Fill/Circular/Rounded/Symbol) can't take our `ContainedIcon`, so wholesale
  adoption was rejected (founder-caught). ListRow colors bound; swappable leading preserved.
- **Color-binding sweep** — **97 fills across ~20 components** bound to `Color` variables
  (`label/*`, `background/*`, `separator`, `brand/blue`, `system/*`); opacity preserved (a bind
  resets paint opacity — re-applied), on-fill white text left unbound. Verified ProposalCard (4
  states) + MessageBubble.

Page consolidation **done** (33→15 pages, domain sections). **Screens on the iOS 26 foundation:**
Chat + Task detail (built), Settings/Connectors/About (navs re-based to `Toolbar-Top`, Large Title
for Settings/Connectors, subtitle hidden). Agenda keeps its custom `DateNavigationHeader` (correct —
app-specific). Remaining: build **Inbox**, re-verify **Agenda**, split into per-screen pages; minor
component polish (RemFaceMark, ContainedIcon/Switch/Button symbols, TypingDots color, ListRow 52pt).

## Real SF Symbols pass — 2026-09-23 (icon set unblocked)

The earlier belief that "SF Pro can't render SF Symbols in this file" (note 2 below) was **wrong** —
it came from incorrect codepoints off web tables. `SF Pro` renders SF Symbols correctly with the
right **PUA codepoint**. Established this pass:

- **Verified codepoint map** for the app's full icon set (28 symbols grep'd from `Image(systemName:)`),
  each screenshot-checked at SF Pro Bold, sourced by spatial-pairing in the community SF Symbols file
  and anchored by a known answer (`chevron.right → U+10018A`). Recorded in the skill's
  `references/sf-symbols-map.md`.
- **DateNavigationHeader** (`43:2`): hand-drawn calendar → real `calendar` (`U+100249`), corrected to
  **brand blue `#0C50FF`** (was iOS system blue), horizontal padding removed (Agenda provides the gutter).
- **Agenda screen** (`181:754`): ascii placeholders → real glyphs — brief `›` → `chevron.right`, sort
  `⇅`/`⌄` → `chevron.up.chevron.down`/`chevron.down`, `+ Add New` → real `plus` (also corrected
  system-blue → brand blue); toolbar hand-drawn hamburger/plus → real `line.3.horizontal`/`plus`.
- **Toolbar overflow fixed**: `Content` set to FILL so the toolbar pins flush at the bottom (`y 790`,
  bottom `874`) instead of hanging 71px below the clipped frame — the three circular buttons
  (hamburger · waveform FAB · plus) now match `01-agenda`.

- **SuggestedTaskRow** (`48:25`, both variants): drawn CTA vectors → real `plus` (add) /
  `arrow.turn.up.right` (move), **corrected black → brand blue** (source uses
  `.foregroundStyle(DesignTokens.Color.brandBlue)`); dismiss vector → real `xmark` in
  `labelSecondary`. Propagates to both Agenda suggestion instances.

**Still traced/deferred:** `mic.fill` has no ascii caption in the community file (source via the
icon-request frame). The brief's speaker still renders as the 🔊 emoji — `speaker.wave.2.fill`
(`U+1002A7`) is now in the map but the exact app variant (`.2` vs `.3`) needs a source check before
swapping. The TaskEventRow status rings are component-level vectors pending a source check (may be
an intentional dashed "incomplete" ring, not an SF Symbol).

---

## Consolidation pass — 2026-09-23 (component model + kit adoption)

A second pass reworked the foundation for correctness and to kill duplication. What changed:

- **One row, one slot.** `ListRow`, `ToggleRow`, and `ActionRow` were three near-identical
  components. They are now a **single `ListRow`** with a trailing **Accessory instance-swap slot**:
  **Chevron / Switch / Button / None**. ToggleRow + ActionRow are deleted. The blocker (a nested
  instance's *size* can't be overridden, so a swapped-in Switch stayed chevron-sized) was solved by
  making `Switch` **HUG-wrap a fixed 51×31 track** — instance-swap then resizes it the way the
  HUG-sized Button already did.
- **Row metrics match Apple.** 12pt top/bottom padding → **46pt single-line / 65pt with subtitle**,
  matching Apple's `Row` set (`Height=Regular` 44, `Height=Tall` 60). Divider is bottom-pinned via a
  `Show Separator` toggle (hidden on the last row of a card).
- **Real kit chrome, not hand-drawn.** Screens sit in the **Apple iPhone 16 Pro bezel** (Apple Design
  Resources; 402×874 screen cutout) via a `DeviceFrame` component with a `Screen` instance-swap slot.
  Nav bars use the **Apple Navigation Bar - iPhone (Compact)** component — `Default` (inline) for
  Settings/About, `Large` for Connectors; Agenda keeps `DateNavigationHeader`.
- **Dedupe + organization.** Deleted orphaned duplicate `Button`/`ContainedIcon` masters and the
  bespoke `DateNav` frames (now the canonical `DateNavigationHeader`). Device-framed screens live on
  one **Screens** page in labeled Sections (① Device Kit ② Settings ③ Agenda + states ④ Chat).

**Mac-verify pipeline:** `visual-verify.yml` run **#6** (commit `d31e59d`) is **green** — the
ground-truth iOS screenshot pipeline is live; no Swift regressions.

### Figma ↔ SwiftUI mapping (manual Code Connect)

Figma **Code Connect** needs a paid Dev/Enterprise seat (unavailable on this plan), so the
design↔code binding is recorded in each component's Figma **description** and here:

| Figma component | SwiftUI source |
|---|---|
| ListRow (+ Accessory slot) | insetGrouped List rows — `Rem/Shared/Views/Settings/SharedSettingsView.swift` |
| Switch accessory | `Toggle().labelsHidden().tint(.green)` |
| Button accessory | `Button(.borderedProminent)` / brandBlue capsule |
| TaskEventRow | `Rem/Rem/Sources/Components/TaskEventRowView.swift` |
| SuggestedTaskRow | `Rem/Shared/Views/Tasks/SuggestedTaskRow.swift` |
| MessageBubble | `Rem/Packages/RemKit/Sources/RemChatUI/ChatMessageViews.swift` |
| ComposerBar | `Rem/Shared/Views/Chat/SharedRemChatView.swift` |
| DateNavigationHeader | `SharedDateNavigationHeader` — `Rem/Shared/Views/Tasks/SharedAgendaView.swift` |
| VoiceBar / MiniPlayerBar | `Rem/Shared/Views/Components/MiniPlayerBar.swift` |
| Task detail screen | `Rem/Rem/Sources/Screens/TaskEventView.swift` |

---

## Verified components (25/25)

Legend — **✅ faithful**: matches the real render on structure, layout, type, color and glyphs.
**✱ fixed this pass**: had a real mismatch, now corrected. Notes call out any residual minor gaps.

| Component | Node | Status | Notes |
|---|---|---|---|
| Text | `65-26` | ✅ | Typography scale bound to tokens (`largeTitle`…`caption1`, SF Pro weights). |
| Surface | `8-12` | ✅ | Background tokens (`background/*`), radius tokens. |
| Card | `11-11` | ✅ | `secondarySystemBackground`, `large`/`medium` corner tokens. |
| Pill | `64-14` | ✅ | Subtle gray capsule (`secondarySystemBackground`, `caption1`) + optional colored dot — matches the app; earlier saturated-blob version was corrected. |
| ListRow | `101-18` | ✅ | **Unified row** — leading icon · title · subtitle · **Accessory slot** (Chevron/Switch/Button/None), 12pt padding, bottom-pinned divider. Verified vs `05-connectors`, reused across Settings/Connectors/About. (old `68-6` retired) |
| ContainedIcon | `110-54` | ✅ | Colored rounded-square icon container; per-row fill override for section colors. (old `12-19` deleted) |
| Button | `110-47` | ✅ | Accessory pill (`Label` TEXT prop); brand-blue **Connect**/action. Also used in ProposalCard. (old `66-10` deleted) |
| Switch | `868-210` | ✅ | Platform=iOS/Android × State=On/Off wrapper around official iOS 26 and Material 3 instances; shared `systemGreen` on-tint. |
| DeviceFrame | `128-46` | ✅ | Real Apple iPhone 16 Pro bezel + 402×874 Screen instance-swap slot. |
| MessageBubble | `50-7` | ✅ | Corner **18** (chat constant, not a token); **no tail** in normal style (tail is onboarding-only); user text **14pt**; brand-blue fill + 0.5px white-12% border. Assistant turns render as **bubble-less prose**. Matches `06-chat` + `ChatMessageViews.swift`. |
| ComposerBar | `53-2` | ✅ | "Ask anything" · `+` attach · brand-blue **Speak** pill (`waveform` + "Speak", corner `medium`/12, white semibold) · `↑` send. Matches `SharedRemChatView.speakButton`. |
| ContextualMessage | `73-39` | ✅ | Five states (info/success/warning/error/neutral) with correct colored status glyphs. |
| ThinkingBlock | `63-20` | ✅ | Collapsed "Thought for a moment ⌄" and expanded reasoning body. |
| TypingDots | `17-3` | ✅ | Three-dot typing indicator. |
| Toast | `72-24` | ✅ | Four semantic variants, responsive Message binding, colored status glyphs on gray pill; SwiftUI/Compose auto-dismiss after 4s and announce politely. |
| ToolResultCard | `62-2` | ✅ | Generic tool-result card (calendar-events example). |
| TaskEventRow | `46-21` | ✅ | Dashed-circle status indicator (overdue), two-part time label (`08` `00`/`AM`), gray leading bar for events, bold 2-line title. Chevron is a row sibling, not part of the component — matches `TaskEventView`. |
| SuggestedTaskRow | `48-25` | ✅ | Dashed-border card, blue stacked action (`+ Add` / `↪ Move`), meta line, `×` dismiss. Matches `01-agenda`. |
| ProposalCard | `54-55` | ✅ | Title · body · faint rationale · **Approve**/**Dismiss**. |
| DateNavigationHeader | `43-2` | ✅ | `‹ – – –  📅 Today  – – – ›` + date subtitle. Matches `01-agenda`. |
| CalendarEventsCard | `67-2` | ✅ | Header + colored-dot event rows. |
| RemindersCard | `67-481` | ✅ | Header + checkbox rows + right-aligned due times. |
| AgendaView | `49-67` | ✱ fixed | Bottom toolbar corrected from 5 tofu slots to the real **☰ (glass) · blue chat FAB · + (glass)** — see below. Rest (header, brief, sort, overdue/to-do rows, suggestions) matches `01-agenda`. |
| InboxView | `69-54` | ✅ | Large title + notification rows (icon · title · relative time · chevron). |
| SettingsView | `70-54` | ✱ fixed | Grouped rows + green toggle. Mock **real email replaced** with `avery@example.com` (see below). |
| ChatScreen | `71-533` | ✱ fixed | Full chat: status bar, bubbles, cards, composer. Nav retitled "New conversation" + trailing `⋯` more button added to match the app. |
| ConversationView | `71-35` | ✅ | Composite: user bubbles, assistant prose, CalendarEventsCard, ProposalCard. |

---

## Fixes applied this pass

- **AgendaView bottom toolbar** (`49-67`). The imported iOS 26 *Toolbar - Bottom - iPhone* instance
  rendered **4 side buttons** (2 leading + 2 trailing) whose SF-Symbol glyphs showed as
  missing-glyph boxes; the real app shows **one leading (hamburger) + one trailing (plus) + a center
  brand-blue chat FAB** (`ContentView.remMainBottomToolbar`: `line.3.horizontal` ·
  `message.badge.waveform.fill` · `plus`). Reduced the instance to 1 leading + 1 trailing, hid the
  tofu symbol text, and overlaid app-traced `line.3.horizontal` and `plus` vectors on the two glass
  buttons. Now matches `01-agenda`.

- **SettingsView PII** (`70-54`). The Account row's mock subtitle was a **real email address**; since
  these frames embed on the public doc site, it was replaced with the repo's placeholder
  `avery@example.com`. A scan of all 33 pages confirms no other real-email leak.

- **ChatScreen nav** (`71-533`). Retitled the centered nav from "Rem" to **"New conversation"**
  (recentered) and added the **trailing `⋯` more** glass button, matching `06-chat`.

- **Build fixes (Rem repo)** that unblocked the macOS visual-verify CI:
  - `ReadmeChatFixtureView.swift` still imported the pre-rebrand `OpenClaw*` modules/types →
    renamed to `Rem*` (`RemChatUI`/`RemKit`/`RemProtocol`, `RemChat*`).
  - `ReadmeAgendaFixture.swift` called a shared `remMainBottomToolbar(...)` builder that never
    existed (the de-brand had collapsed it back into an inline property) → re-extracted it as a free
    `@ViewBuilder` and delegated `ContentView.bottomToolbar` to it (behavior-preserving, DRY).

---

## Residual minor mismatches (flagged, non-blocking)

1. **Row leading icons** in SettingsView/InboxView are the generic blue rounded-square; the real app
   uses type-specific SF Symbols (e.g. person/antenna for Account/Gateway, per-notification icons in
   Inbox). They render correctly, just not context-specific.
2. **Icon glyphs — now real SF Symbols.** *(Superseded 2026-09-23 — see the Real SF Symbols pass
   above.)* The earlier claim here — that the SF Pro webfont in this file lacks the private-use
   glyphs — was wrong; it came from incorrect codepoints. `SF Pro` renders SF Symbols with the
   correct **PUA codepoint**, so custom components and screens now use **real SF Symbols** (verified
   map in the skill's `references/sf-symbols-map.md`), not traced vectors. Remaining traced glyphs are
   only those with no caption in the source file (`mic.fill`, `speaker.wave.2.fill`) and the row
   components' status/CTA vectors.

---

## Single source of truth

`tokens/tokens.json` remains the single source: Figma variables are generated from it and the
drift-check contract passes (`tokens: in sync ✓`, `manifest: in sync ✓`). The app's
`DesignTokens.swift` `CornerRadius` scale was realigned to the same monotonic values
(`small 8 < medium 12 < large 16 < xlarge 24`) so the app and the generated Figma variables agree.

---

## Founder-review fidelity pass — 2026-09-24

Worked the founder's full review + the goal's "Done when" bar end-to-end. Every item below was
verified against the shipping SwiftUI (screenshot + source) and committed.

**Components**
- VoiceBar: all 6 states — button bg `color.opacity(0.2)` (not solid), Muted mic glyph red, Reading
  uses `stop.fill`. `MiniPlayerBar.swift` + `ContentView.voiceMiniPlayerBar`.
- Cards: CalendarEventsCard real `calendar` (systemRed); RemindersCard real **AppleRemindersLogo**
  (embedded via `createImage`); ProposalCard `checklist` header (brandBlue) + terminal status icons
  (checkmark.circle.fill / xmark.octagon.fill / clock.badge.exclamationmark.fill) all 4 states;
  RemindersCard copy → "3 reminders".
- **ContainedIcon**: was a blank colored square — now bakes a white `Symbol` glyph prop; Settings +
  Connectors re-glyphed from `SharedSettingsView` / `SharedComposioConnectionsView` fallbacks.
- TaskEventRow: reworked to the 3-slot Row model + **pills** (canonical Pill), pills gated by a
  `Pills` BOOLEAN prop (Agenda on, Inbox off).
- **RemFaceMark** built from `RemFaceMark.swift`'s `CustomFaceShape` SVG path (+ eyes + smile), bound
  to labelPrimary, swapped into Task detail.
- Removed Text / Surface / Card primitives (founder call; zero instances). Descriptions link to source.
- HomeIndicator added to DeviceFrame (transparent band; Chat bg-sync verified — VoiceBar reaches y=874).

**Screens** (all on the finalized components, code-verified)
- Agenda (real no-state, no bogus toolbar, hugged rows, cleaned DateNav) · Chat · Task detail
  (canonical RemComposerBar, hugged frames) · Inbox (title-only nav) · Settings · Connectors · About
  (app-icon hero) · **Billing & Usage** (new) · **Permissions** (new). Agenda-Loading deleted.

**File** — real `createPageDivider()` dividers (Foundations · Components · Screens); **one page per
screen** under the SCREENS divider; Section-in-use specimen (header + rows + footer).

**Deferred (logged in CLEANUP.md, need founder/network/app-repo):** connector *brand* logos (remote
SVGs; SF fallbacks in place) · SwiftUI list-style variants (open design Q) · TaskEventRow leading-state
variant · RemFaceMark `.thinking` mode · global page-bg variable · app dead-code deprecation (verified PR).

### TaskEventRow spacing/clipping fixes — 2026-09-24 (founder-caught)
- **Title-row clipping**: the row's Title Row was FIXED height 32 while a 2-line title is 44 → it
  overflowed and collided with the pills. Set the Title Row to **hug height**
  (`counterAxisSizingMode='AUTO'`) so 2-line titles push the pills down cleanly. Verified on Agenda.
- **Leading badge gap**: the unscheduled `calendar.badge.plus` / `clock` glyph was left-aligned in the
  64pt column; code centers it (`.frame(maxWidth:.infinity, alignment:.center)`). Set the Schedule/Clock
  leading `primaryAxisAlignItems='CENTER'` → glyph centered, gap matches the app. (Time stays left, as
  code left-aligns the HH:MM.)
- **Spacing tokens**: Content title↔pills spacing 6→**8** (`sm`); Pills spacing 6→**4** (`xs`).
