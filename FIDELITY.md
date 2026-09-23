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

**Still traced/deferred:** `mic.fill` and the brief's `speaker.wave.2.fill` have no ascii caption in
the community file (source via the icon-request frame); the TaskEventRow status rings and
SuggestedTaskRow Add/Move CTA glyphs are component-level vectors, not yet swapped.

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
| Switch | `110-50` | ✅ | 51×31 green Toggle, HUG-wrapped track so instance-swap resizes it in the ListRow slot. |
| DeviceFrame | `128-46` | ✅ | Real Apple iPhone 16 Pro bezel + 402×874 Screen instance-swap slot. |
| MessageBubble | `50-7` | ✅ | Corner **18** (chat constant, not a token); **no tail** in normal style (tail is onboarding-only); user text **14pt**; brand-blue fill + 0.5px white-12% border. Assistant turns render as **bubble-less prose**. Matches `06-chat` + `ChatMessageViews.swift`. |
| ComposerBar | `53-2` | ✅ | "Ask anything" · `+` attach · brand-blue **Speak** pill (`waveform` + "Speak", corner `medium`/12, white semibold) · `↑` send. Matches `SharedRemChatView.speakButton`. |
| ContextualMessage | `73-39` | ✅ | Five states (info/success/warning/error/neutral) with correct colored status glyphs. |
| ThinkingBlock | `63-20` | ✅ | Collapsed "Thought for a moment ⌄" and expanded reasoning body. |
| TypingDots | `17-3` | ✅ | Three-dot typing indicator. |
| Toast | `72-24` | ✅ | Four states (info/success/warning/error), colored status glyphs on gray pill. |
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
