# Design System — Cleanup Log (deferred)

Debt found while building. **Do not fix inline** — note it here and defer to a focused cleanup
pass/PR. Guiding principles: **code is the source of truth (screenshots can be stale)**;
**everything is a component property/variant**; **one canonical per concept (no duplicates across
pages)**.

## Row decision — KEEP custom `ListRow` (swappable leading), re-base to iOS 26 *(reversed)*
- **The kit Row can't take our `ContainedIcon` leading.** The iOS 26 `Row`
  (`5587f1ebded12290ac2731cef9d68f98a4a0ac61`) has **no instance-swap props**; its leading is a
  nested `Image` instance (`Type` = Fill/Circular/Rounded/Symbol) — image fills or a plain symbol,
  never our colored-square `ContainedIcon` component, and nested mains can't be swapped without an
  exposed swap prop. Settings rows need `ContainedIcon` (branded, variable-bound), so **adopting the
  kit Row wholesale is rejected.** (Founder caught this before build — good.)
- **Decision:** keep our custom **`ListRow`** (`101:18`) with its swappable slots (Leading
  Accessory = ContainedIcon/Avatar/Symbol/None · Content = ListRowLabel · Trailing Accessory =
  Chevron/Switch/Button/None), and **re-base it to iOS 26**: match the kit Row's metrics
  (Regular ~52 / Tall), bind fills to the local **Color** variables, use real SF Symbols. "Built on
  the iOS 26 Row" = its metrics/tokens/look, while keeping the swappable leading the app requires.
- [ ] Re-base `ListRow`: iOS 26 metrics + bind colors (bg→`background/primary`, hairline→`separator`,
  labels→`label/*`) + real chevron/switch symbols. Then screens re-verify on it.
- Standalone kit pieces still worth using where there's no swappable-leading need: `Section Title`
  (`216bce9b…`), `Header` (`e86f40bf…`), `Grouped Table Footer` (`19a81fe0…`).

## iOS 18 → iOS 26 kit re-base (HIGH — core fidelity)
Kit components were imported from the **iOS 18** library (nav `d299571689…`, Row
`8bb9d297eb…`). The **official "iOS and iPadOS 26"** library is subscribed to the file AND
importable by key (confirmed: imported iOS 26 `Status bar - iPhone` live). **Do not publish a
copy** of iOS 26 — the live subscription tracks Apple's updates; a copy is a fork that drifts.
Re-base every kit usage to iOS 26:
- [ ] **Nav → `Toolbar - Top - iPhone`** (`e68d32a867ed6d4d8620f1614cd2d4b9d544b92d`, a set;
  `Style` = Default / Compact Large / Large Title / Title 2 Line / Title 2 Line Left — use
  **Default** for inline). Rebuild Chat (`256:35`) + Settings/Connectors/About navs on it; retire
  the iOS 18 `Navigation Bar`. (In iOS 26 the nav bar *is* the Toolbar-Top, as the founder noted.)
- [ ] **Row / List → the iOS 26 Row/List** (find key in the iOS 26 library); rebuild `ListRow`
  on it (currently on the iOS 18 Row set).
- [ ] Audit any other iOS 18 imports (controls, bezel) and move to iOS 26.

## Figma
- [ ] **Nav reconciliation (drift).** File mixes two nav approaches: hand-built `nav` frames
  (e.g. Chat `71:544` — transparent + baked hairline) and the imported kit `NavigationBar`
  instance (e.g. Settings `154:790`). Standardize on ONE. Prefer **configuring the kit nav**
  (mirror-upstream): transparent bg that inherits the page bg, hairline only in a `Scrolled`
  state, title/leading/trailing via props. Build a thin `RemNavigationBar` wrapper **only** if the
  kit can't express transparent-bg + hairline-on-scroll — and if so, document the exact kit
  limitation in its description + `REGISTRY.md`. Verify kit capability before deciding.
- [ ] **Global page-bg variable.** Introduce `Surface/Background` (primary vs secondary/grouped)
  so screens + navs inherit it instead of hardcoding white. Platform-mode ready.
- [ ] **Move spec sheets to the Specs page.** The plugin's ListRow spec (`222:137`) and future
  sheets belong under `———  SPECS  ———`, not loose on a component page.
- [ ] **Audit duplicate components across pages.** Confirm single-canonical; fold any dupes.
- [ ] **REGISTRY.md sync:** `ComposerBar` → `RemComposerBar` (renamed in Figma).

## Nav — RESOLVED (standardize on the kit nav)
- [x] **Kit nav trailing DOES take our glyphs.** (Earlier "needs an Apple symbol component" was
  wrong.) The kit `NavigationBar` trailing/leading button has a **`Symbol#…` TEXT property** — set
  it to the SF Pro glyph directly (ellipsis `U+100360`). Config the kit nav via its props: `Title`
  (text), `Show Leading/Trailing`, the buttons' `Symbol` text, and `Show Background`
  (transparent-vs-material). **No `RemNavigationBar` wrapper needed** — standardize every screen on
  the kit nav; retire hand-built `nav` frames.
- [ ] Minor: Chat nav ⋯ renders blue (kit default tint); `06-chat` shows it in a grey circle
  button. Polish the trailing button style/color to match if we want exact parity.
- [ ] **Home indicator absent.** DeviceFrame bezel (`128:46`) has none; the iOS 26 kit omits it
  (system overlay). Decide whether to add a simple home-indicator bar to the bezel (would overlay
  bottom bars like the voice bar, as on-device) or leave it kit-faithful.

## Structure
- [x] **Component domain pages — DONE.** Consolidated ~20 one-per-page components into 5 domain
  pages (Primitives · Rows & Controls · Chat · Tasks & Agenda · Cards) + Platform Controls, under
  the real `COMPONENTS` divider. Each stacked component-under-page-header, verified no overlap.
  File down from ~33 → 15 pages. Specs live per domain page (no separate Specs section).
- [ ] **Per-screen pages** (Phase 2): split the single `Screens` page into one page per screen
  (Agenda/Chat/Task detail/Settings/Inbox/Connectors/About) under the `SCREENS` divider as screens
  are built/re-verified.

## Components still needed
- [ ] **RemFaceMark** — Task detail's activity avatar is a placeholder (brand-blue tint circle +
  drawn eyes/smile). The app uses `RemFaceMark` (the Rem brand face, `.idle`/`.thinking` modes,
  tinted). Build a canonical `RemFaceMark` component and swap it into Task detail + the empty-state
  face + the voice-bar/thinking marks.

## Icons
- [ ] `mic.fill` — no caption in the community SF Symbols file; source via the icon-request frame
  (needs a Mac). Voice bar currently renders its own mic.
- [ ] Brief speaker: confirm exact variant (`speaker.wave.2.fill` `U+1002A7` vs `.3.fill`) from
  source before swapping the 🔊 emoji.

## App code (Rem / RemClaw — verified PR, never blind-delete)
- [ ] Dead default-bubble path: `ChatMessageViews.swift` (`ChatMessageBubble` / `ChatBubbleShape`)
  + `RemGatewayChatView` — `SharedRemChatView` replaced them and draws its own bubbles
  (speechBubble @3400, streamingBubble @4879). grep-confirm truly unreferenced, then deprecate in a
  focused PR with build + test. (Confirmed already: there is **no** legacy `ComposerBar` — only
  `RemComposerBar` — so that one is just the Figma rename above.)

## Founder review — 2026-09-24 (catch-all; fidelity-first, screens paused)

Standing redirect from this review: **component fidelity before more screens.** Founder does not
trust state fidelity — **do not build speculative states** (Inbox-Loading explicitly cut; Agenda
loading to be deleted). Verify every state against the **running app**, not just code, before
building it. Main intent restated: (1) **code-sync** so the file is true to the app *today*;
(2) design is cheap — a faithful design lets an agent execute faster. Link component descriptions to
their **codebase source** (path) so reviewers can find them.

### Primitives / set membership (decide — some are removals)
- [ ] **Text primitive** — founder: "why do we need Text at all, we already have text styles."
  Lean **remove** `Text` (65:26); the type ramp lives in the local text styles. Confirm, then delete +
  drop from REGISTRY/Index.
- [ ] **Surface / Card** — founder questions both; a surface is already expressed by foundation
  color/background styles. Lean **remove Surface**, *maybe* keep **Card**. Confirm scope before cutting.
- [ ] **ErrorBanner** — founder doesn't know where it's used. **Grep the app** for its real usage
  (name + file); if unused, remove. Add the source path to its description either way.
- [ ] **Descriptions → code links.** Every component description should name its codebase source
  (e.g. `SharedInboxView.swift`), not just prose. Backfill across the set.

### Component fidelity bugs (do these — verifiable vs code)
- [x] **VoiceBar button backgrounds fixed** (all 6 states verified). Bugs were: (1) button-bg
  circles at opacity 1 on Speaking/Muted/Reading → set to **0.2** per `MiniPlayerBar` (`color.opacity(0.2)`);
  (2) **Muted** mic glyph was `labelPrimary` but muted mic color = **`.red`** → rebound red;
  (3) **Reading** stop icon was `phone.down.fill` but reading uses **`stop.fill`** (`U+1006F7`) → fixed.
- [x] **CalendarEventsCard icon fixed** — drawn vectors → real `calendar` glyph (`U+100249`),
  `.subheadline` 15pt, **systemRed** (per `CalendarCard.headerView`). Verified.
- [x] **RemindersCard icon fixed** — drawn vectors → the real branded **AppleRemindersLogo** image
  (`Rem/Assets.xcassets/AppleRemindersLogo.imageset/Apple_Reminders_Logo.png`), embedded via
  `figma.createImage` (the `upload_assets` host `mcp.figma.com` is **network-blocked** here — 403 CONNECT).
  Verified. NOTE: header copy shows "Reminders · 3" but code is **"3 reminders"** (`Text("\(count) reminder(s)")`) —
  minor copy drift, fix in a text pass. Same pattern to check on CalendarCard headerText.
- [ ] **ProposalCard icon** — main icon is `checklist` (`RemProposalCardView` L61, `.subheadline`);
  drawn vectors still there. Needs the `checklist` PUA codepoint (not yet in sf-symbols-map) — source
  via the community-file lookup, then swap.
- [ ] **TaskEventRow pills missing.** task + event variants support pills (list badge, duration,
  overdue) per `TaskEventRowView.taskContent`. Add a pills row (as a variant/optional slot).
- [ ] **Height-100 layout bug.** Frames default to fixed height 100 instead of hug — founder saw it
  on **Task detail** (dates/meta, and a frame literally named "action" still unfixed) and on
  **Agenda TaskEventRows**. Sweep every screen/component for FIXED-100 frames → set hug
  (`primaryAxisSizingMode='AUTO'`).
- [ ] **Card icons wrong.** CalendarEventsCard, RemindersCard, **ProposalCard** icons don't match
  fidelity — replace drawn/placeholder icons with the real SF Symbols the app uses.
- [ ] **Row hierarchy idea.** TaskEventRow is conceptually a **Row** with re-slotted
  leading/content/trailing (leading = time/schedule/clock, content = status+title, trailing = pills).
  Consider modeling it as a descendant of the base Row rather than a parallel component.

### Screens (paused — fidelity + arrangement first)
- [ ] **Sections don't contain their screens.** The Figma SECTIONs (③ Agenda, ⑤ Inbox, …) don't
  actually enclose the screen frames, so the section labels are useless. Move each screen inside its
  section (and this feeds the per-screen-pages split).
- [ ] **Arrangement is scattered** — "things are all over the place, I have to hunt for the right
  screen." Impose a clean, findable layout before adding more.
- [ ] **Home indicator + safe area.** Adopt the bottom home-indicator/safe-area in the device frame;
  its background must **sync to whatever sits at the bottom** (e.g. the voice bar's bg on Chat).
- [ ] **Agenda no-state is wrong** — build it from the **actual app** behavior, not a guess. **Delete
  Screen/Agenda-Loading** (140:1643).
- [ ] **Agenda bottom toolbar wrong across all** agenda screens — and it should **not** use glass
  buttons. Fix to match the app.
- [ ] **Task detail composer is the wrong one.** It uses a "composer doc"-style field; must use the
  canonical **RemComposerBar** (`53:2`) — the one we are keeping, not the one slated for deprecation.
- [ ] **List styles.** SwiftUI has many list styles (plain/inset/grouped/insetGrouped/sidebar).
  Open question how the system represents them component-side (founder may share docs). `Section`
  should also be shown **in combination with a real list** (header + rows + footer), not header/footer
  alone.
- [ ] **Settings sub-screens** (Billing & Usage, Permissions, About detail) — candidate builds for
  code-sync fidelity; low priority, confirm appetite.
- [x] **Inbox nav actions removed** — header now title-only ("Inbox" Large Title), Leading/Trailing
  control frames hidden, matching `InboxHeader` (title-only). Populated list = TaskEventRow rows with
  `calendar.badge.plus` unscheduled leading. **Inbox-Loading intentionally NOT built** (founder cut).

## Done (kept for trail)
- [x] DateNavigationHeader: real `calendar` glyph, brand blue `#0C50FF`, H-padding removed.
- [x] Agenda + SuggestedTaskRow: drawn icons → real SF Symbols; toolbar overflow fixed.
- [x] Pages reorganized into labeled sections (META · FOUNDATIONS · COMPONENTS · SCREENS · SPECS).
- [x] Figma `ComposerBar` → `RemComposerBar`.
