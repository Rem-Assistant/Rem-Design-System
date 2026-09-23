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

## Verified components (25/25)

Legend — **✅ faithful**: matches the real render on structure, layout, type, color and glyphs.
**✱ fixed this pass**: had a real mismatch, now corrected. Notes call out any residual minor gaps.

| Component | Node | Status | Notes |
|---|---|---|---|
| Text | `65-26` | ✅ | Typography scale bound to tokens (`largeTitle`…`caption1`, SF Pro weights). |
| Surface | `8-12` | ✅ | Background tokens (`background/*`), radius tokens. |
| Card | `11-11` | ✅ | `secondarySystemBackground`, `large`/`medium` corner tokens. |
| Pill | `64-14` | ✅ | Subtle gray capsule (`secondarySystemBackground`, `caption1`) + optional colored dot — matches the app; earlier saturated-blob version was corrected. |
| ListRow | `68-6` | ✅ | Leading icon · title · subtitle · trailing chevron, inset separator. Verified vs `05-connectors` and reused in Settings/Inbox. Clean text props (`title`/`subtitle`/`separator`). |
| ContainedIcon | `12-19` | ✅ | Colored rounded-square icon container; verified in Settings/Inbox rows. |
| Button | `66-10` | ✅ | Verified via ProposalCard (filled brand-blue **Approve** / outlined **Dismiss**). |
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
| ChatScreen | `71-533` | ✅ | Full chat: status bar, bubbles, cards, composer. Minor nav notes below. |
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

- **Build fixes (Rem repo)** that unblocked the macOS visual-verify CI:
  - `ReadmeChatFixtureView.swift` still imported the pre-rebrand `OpenClaw*` modules/types →
    renamed to `Rem*` (`RemChatUI`/`RemKit`/`RemProtocol`, `RemChat*`).
  - `ReadmeAgendaFixture.swift` called a shared `remMainBottomToolbar(...)` builder that never
    existed (the de-brand had collapsed it back into an inline property) → re-extracted it as a free
    `@ViewBuilder` and delegated `ContentView.bottomToolbar` to it (behavior-preserving, DRY).

---

## Residual minor mismatches (flagged, non-blocking)

1. **ChatScreen nav** (`71-533`): title reads "Rem" and lacks the trailing `⋯` (more) button; the
   real chat shows the conversation title (or "New conversation") plus the `⋯` glass button.
2. **Row leading icons** in SettingsView/InboxView are the generic blue rounded-square; the real app
   uses type-specific SF Symbols (e.g. person/antenna for Account/Gateway, per-notification icons in
   Inbox). They render correctly, just not context-specific.
3. **Icon glyphs — kit chrome vs. custom.** Custom components use **app-traced vector glyphs** and
   render correctly everywhere. Imported iOS 26 kit chrome renders SF Symbols as **SF Pro PUA text**;
   the SF Pro webfont available in this Figma file lacks those private-use glyphs, so kit symbols
   fall back to missing-glyph boxes. The only place this surfaced (the Agenda toolbar) is fixed by
   overlaying traced vectors. **If pixel-exact official SF Symbols are wanted throughout the kit
   chrome, add an SF Symbols component library and instance-swap** — the custom components already
   read correctly without it.

---

## Single source of truth

`tokens/tokens.json` remains the single source: Figma variables are generated from it and the
drift-check contract passes (`tokens: in sync ✓`, `manifest: in sync ✓`). The app's
`DesignTokens.swift` `CornerRadius` scale was realigned to the same monotonic values
(`small 8 < medium 12 < large 16 < xlarge 24`) so the app and the generated Figma variables agree.
