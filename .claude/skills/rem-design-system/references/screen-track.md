# Screen / pattern track — device-framed, native, all states

For a **screen or flow** (Agenda, Chat, Settings, Inbox, a pattern). A screen is a canonical,
full-device component *composed of* the system's lower-level components; it is not a replacement for
those primitives. Instance that one screen master in inventory, documentation, prototype, and
handoff. Ground it in the real SwiftUI view so the structure and every state are faithful, not
invented.

## Ground in BOTH the source and the screenshot — then diff

The single biggest fidelity failure is building from a mock-derived frame or from memory
instead of the ground truth. **Two grounds, used together, before the first node:**

1. **The shipping SwiftUI view** (`Shared/Views/…`, `Rem/Sources/…`) for **exact values** —
   font sizes/weights, colors, paddings, glyph names, and the **state machine**. Write these
   down as a short spec. (Agenda's `DateNavigationHeader` is a blue `calendar` icon (22 bold)
   + "Today" (22 **bold**, not 17 semibold) + date (13 bold) + **three 10×4 dashes** at 50%
   opacity beside 22-bold chevrons; its toolbar is **three separate circular buttons**, not a
   glass pill. Missing any of that = "not faithful" — that exact miss happened by skipping
   this step.)
2. **The committed app screenshot** (`docs/screenshots/*.png`, e.g. `01-agenda`, `05-connectors`,
   `06-chat`) for the **visual truth** — read it with the image reader and note structure,
   proportions, which state it's in (e.g. `06-chat` shows the voice bar **active/Listening**).

The states are the point — "every screen + its states" means all of them: Agenda has
scheduled/empty/loading/jump-to-today/sort-modes; Chat has the 3-way empty gate, run
lifecycle, tool-result error/unknown, and the voice bar's 6 states (`MiniPlayerBar.swift`:
connecting/listening/speaking/muted/reading/closing).

**After building, diff against the screenshot** — screenshot the Figma frame and compare it
to `docs/screenshots/<screen>.png` side by side; fix every mismatch. This is the visual half
of "verified against the app"; skipping it is how unfaithful screens ship.

## A flow is STATES + DESTINATIONS, not one screen

The single biggest scoping failure is delivering **one screen per step**. A flow is a *system of
screens*: for every screen you build, enumerate and build **(a) every state it can be in** and **(b)
every destination its interactive elements lead to** — each as its own 402×874 screen instance, wired
in the prototype so the transitions are walkable. The founder's Codex flows modeled this (a "save"
flow, an "edit" flow — the same surface shown moving through its states). Ask of every screen:

- **What states does this screen have?** empty · loading · populated · error · success · in-progress ·
  the "just acted" state. Build each.
- **What does every tappable thing open?** A row that opens a sheet, a picker, a detail, a confirm —
  that destination is another screen in the flow. Build it and wire it.

Worked example — **Voice** is not one screen, it's a mini-flow:
`Voice (base)` → tap **Voice** row → `Voice Picker (sheet)` (list of voices, one selected) → back;
tap **Hear this voice** → `playing` state; each slider has an interacting state. **Connectors**:
`available` → tap Connect → `connecting` → `connected` (toggle on) → and an `error` state; **See more**
→ the expanded connector list. If you built only the base screen, you built ~30% of the flow.

Deliver these as the flow's **Prototype** section (the wired strip of real screens) with the
**Documentation** section narrating the transitions. The prototype is the primary artifact; documentation
explains it.

## Build native, in auto-layout, on canonical components

- **Screen = a 402×874 component**, `layoutMode="VERTICAL"`, FIXED — the device content size,
  so it drops into the bezel size-safe.
- **Chrome is a TRIAD — include all three, top to bottom:** `StatusBar` · **`TopBar` (the nav bar)**
  · body · `NavigationIndicator` (home indicator). The middle one is the miss to guard against: every
  screen reached *inside a flow* (any step after the first) carries the **`TopBar` with a back
  affordance** — use the canonical back-only TopBar (Check-in's config: Leading = `Type=Back`, title
  and trailing hidden). Only the flow's true entry screen (e.g. Sign-in) omits back. A domain header
  such as `DateNavigationHeader` replaces the TopBar's *content*, not the StatusBar/NavigationIndicator.
  Onboarding flip screens (Connectors, Voice) are mid-flow → they get the back TopBar too. Platform
  switches may change fonts, metrics, and native icons without changing the product layout.
- **Wrap platform-native components — never hand-draw them.** Anything the OS ships natively — sliders,
  pickers, toggles, nav bars, segmented controls, steppers, date/time pickers — is a **canonical Rem
  wrapper that instances the forked platform kit** (like `TimePicker` wraps the platform pickers), with
  a **Platform = iOS / Android** variant. The forked kits are in the file's libraries: **iOS 26**
  (`zPQph1huwOBLDBj5ZchpQE`) and **Material 3** (`tR9wwAVGtwT5s1IJyWqHL9`). Instance the kit's native
  control; do NOT reconstruct a slider from a track rect + ellipse. If a wrapper doesn't exist yet,
  building it is part of the work — do not hand-draw a stand-in. (This is why the Voice sliders were
  wrong: hand-drawn instead of a wrapped `Slider`.)
- **Body from canonical instances only** — `ListRow` (as grouped `Section`s: SectionHeader +
  rows + SectionFooter), `TaskEventRow`, `SuggestedTaskRow`, `MessageBubble`, `ComposerBar`,
  the cards. Set per-row content on the nested Content/Label (see figma-gotchas). Never
  hand-draw a row that a component covers.
- **A single-row `Section` shows NO divider.** Dividers separate sibling rows; a section with one row
  has no sibling to separate, so the divider is noise. Only show row separators between 2+ rows.
- **Everything auto-layout.** If you must reuse an existing absolute-positioned frame,
  `detachInstance()`/clone it and convert to auto-layout — don't ship absolute positioning
  (it can't reflow and it's why the first Agenda had to be rebuilt native).
- **Use semantic names before structural names.** Name major regions `StatusBar`, `Body`,
  `ActionArea`, and `NavigationIndicator`. Within those regions, use SwiftUI-first names for generic
  layout (`VStack`, `HStack`, `ZStack`, `LazyVStack`, `LazyHStack`). The cross-platform docs map
  those to Compose `Column`, `Row`, `Box`, `LazyColumn`, and `LazyRow`.
- **Use auto spacing, not empty spacer frames.** Group the top and bottom regions semantically and
  use `SPACE_BETWEEN` on their parent. Empty frames whose only purpose is vertical or horizontal
  space are invalid because they obscure intent and break when content changes.

## Evaluate flow consistency (think in systems, not per-screen rules)

A flow is a system: every step should share the same structural pattern, and the job is to **evaluate
consistency across steps**, not to memorize per-screen exceptions. Before a flow is done, check each
step against its neighbors on the system's shared dimensions — header/`Lockup` presence and order, the
background↔content relationship, spacing rhythm, and canonical component reuse — and flag or fix any
step that deviates. **The deviation is the signal, not a named screen.** A screen reused from another
context (e.g. a Settings surface pulled into onboarding) is the usual culprit: it arrives carrying its
original skin (grey bg + white cards, no Lockup) and breaks the pattern its neighbors hold (white bg +
grey content + Lockup) — restore consistency with the system rather than applying a screen-specific
recipe.

## Use the file's documentation templates

- **Page scale:** use one page per product domain, not one page per screen or per flow. Onboarding
  flows belong together on `Onboarding`; Settings flows belong together on `Settings`. Keep reusable
  components on component-family pages.
- **Top-level sections:** give each flow a numbered documentation/prototype pair:
  `01A · <Flow> · Documentation` and `01B · <Flow> · Prototype`, then `02A` / `02B` for the next
  flow. Keep older canonical screens inside a named inventory/reference Section until their flow
  documentation replaces them. Each native Figma Section is a navigation/bounds shell containing
  one transparent auto-layout `Content` frame; that frame owns the large header, description,
  divider, flow rows, and spacing. After any child is added, removed, or reordered, refit the native
  Section bounds to `Content` with the established outer inset. Place pairs on a non-overlapping
  grid and verify bounds. The structure contract lists the exact allowed top-level nodes.

- **Component-family pages:** use `Component Documentation` (`663:2270`) for every new or changed
  canonical component. Arrange the family as a horizontal auto-layout row of vertical columns. Each
  column places the canonical master/variant set first and an attached `Component Documentation`
  instance below. The documentation instance contains metadata only and never repeats the specimen
  through a Component slot. Remove
  empty Anatomy/Props/Layout placeholders; the Specs plugin can add those later. Documentation
  chrome uses Light semantic surfaces and text by default; switch it only when the documentation is
  explicitly demonstrating another theme.
- **Flows:** instance `Mobile Flow Documentation` (`769:282`) from Device Kit. Populate its Overview
  slot and the nested flow slots; never detach it. Do not add loose screen frames as siblings.
  Treat its canvas presentation—orientation, widths, padding, fills, and nested section surfaces—as
  master-owned. Do not duplicate those values in each flow or rebuild them from skill prose; attached
  instances should inherit later refinements to the Device Kit components.
- **Flow hierarchy:** `Mobile Flow Documentation` → `Mobile Flow` → `Placeholder Sections` →
  `Sections` slot → `Placeholder Section` → `Rows` slot → `Placeholder Rows` → `Rows` slot →
  `Placeholder Flows` → `Steps` slot → `Mobile Placeholder` → `Screen` slot → canonical screen
  instance. The Screen slot is exactly 402×874. A screen component replaces that slot content; do
  not retain a legacy wrapper or align a loose screen above it.
- Put the primary journey in one `Placeholder Flows` row. Add a second row for system-state
  variants such as loading or submit failure. Add another `Placeholder Section` only when the
  flow needs a distinct titled group.
- Sequential rows keep their arrow vectors and the template's 24-point item gap. Parallel state
  rows omit arrows and use 200-point item spacing, which preserves the same screen-column rhythm
  without implying that one state navigates to the next.
- Author one light-mode screen and rely on the shared variable mode for dark appearance unless a
  contract explicitly requires a separate dark composition. Theme switching remains independent of
  Platform switching; reviewers should be able to inspect iOS-Light, iOS-Dark, Android-Light, and
  Android-Dark without maintaining four detached screen copies.
- `DeviceFrame/iPhone` (`128:46`) remains available when a standalone bezel preview is useful;
  it does not replace the Mobile Flow documentation hierarchy.
- Keep the nested documentation hierarchy, then add a labeled `PROTOTYPE FLOWS` strip of direct
  402×874 canonical screen instances inside the separate numbered prototype section. Wire and name
  real Presentation starting points and set interactions on instance descendants. Keep the
  component mapping in the structure contract so the strip cannot become an untracked duplicate.
- **One generation only.** When a native screen supersedes a legacy template, delete the legacy
  master and its now-empty page. Migrate anything still pointing at retired masters first.

## Finish

Register the screen + its states, verify against the app's fixtures (visual-verify), and note
it in `FIDELITY.md`. If the doc site embeds live Figma frames, point the embed at the section
or bezel node.
