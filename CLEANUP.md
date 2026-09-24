# Design System — Cleanup Log (deferred)

Debt found while building. **Do not fix inline** — note it here and defer to a focused cleanup
pass/PR. Guiding principles: **code is the source of truth (screenshots can be stale)**;
**everything is a component property/variant**; **one canonical per concept (no duplicates across
pages)**.

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

## Structure — decision needed
- [ ] **Per-screen pages + domain-clubbed component pages** (proposed): SCREENS → one page per
  screen (Agenda/Chat/Task detail/Settings/Inbox/Connectors/About); COMPONENTS → ~6 domain pages
  (Primitives · Rows & Controls · Chat · Tasks & Agenda · Cards · Platform). Reverses strict
  one-component-per-page. Awaiting sign-off on the grouping before the ~30-page move. Specs live
  on each domain page (no separate Specs section — removed).

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

## Done (kept for trail)
- [x] DateNavigationHeader: real `calendar` glyph, brand blue `#0C50FF`, H-padding removed.
- [x] Agenda + SuggestedTaskRow: drawn icons → real SF Symbols; toolbar overflow fixed.
- [x] Pages reorganized into labeled sections (META · FOUNDATIONS · COMPONENTS · SCREENS · SPECS).
- [x] Figma `ComposerBar` → `RemComposerBar`.
