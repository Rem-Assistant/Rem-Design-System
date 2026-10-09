# Screen / pattern track — connected flows and native context

For a **screen or flow** (Agenda, Chat, Settings, Inbox, a pattern), follow the current
[connected-flow contract](../../../../FILE-ORG.md#connected-flow-presentation). Full-device
screens compose lower-level components; component-local states need not become full screens.
Reuse canonical masters in documentation and handoff. Ground reproductions in source and
screenshots; distinguish approved design changes from as-built fidelity. For Chat, read the
[family jobs and status ownership](../../../../REGISTRY.md#chat-component-jobs-and-principles)
and [Chat review gates](chat-review.md). Keep the connected component/state canvas and
representative full screens made with the shared Chat shell and slots. Review destinations,
return/cancel paths, keyboard, safe areas and combined states in that context.

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

## States and destinations at their actual scope

Enumerate in-scope states and destinations as components, overlays, external boundaries or
full-screen destinations. Connect local updates, navigation and return/cancel paths. Mark
unresolved destinations rather than inventing them. State coverage is required; one mobile
frame per state is not. Natural-size specimens must still be checked in representative
full-screen compositions. Verify prototype destinations when interactions are in scope.

## Build native, in auto-layout, on canonical components

- **Full-screen context:** use the declared device viewport (402×874 for the existing mobile
  chassis), native chrome and auto-layout. Natural-sized component specimens are not screens
  and should not inherit that fixed height.
- **Chrome from the kit:** use canonical navigation/header, `StatusBar`, and
  `NavigationIndicator` components; every standalone full-device screen includes its platform
  chrome unless the product state explicitly hides it. Platform switches may change fonts, metrics,
  and native icons without changing the product layout. Prefer platform-specific nested components
  or verified semantic icon mappings over an unverified icon-font family swap. A domain header such
  as `DateNavigationHeader` may replace navigation content, not required status chrome. Put a legal
  sheet and its scrim above the status bar, while the navigation indicator remains at the bottom of
  the device surface.
- **Body from canonical instances only** — `ListRow` (as grouped `Section`s: SectionHeader +
  rows + SectionFooter), `TaskEventRow`, `SuggestedTaskRow`, `MessageBubble`, `ComposerBar`,
  the cards. Set per-row content on the nested Content/Label (see figma-gotchas). Never
  hand-draw a row that a component covers.
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

## Use the file's documentation templates when appropriate

The connected-flow contract governs new work. The walkthrough recipe below applies when
maintaining an existing walkthrough or when a task requires one; it does not require a device
frame for every component-local state. Preserve attached templates when they are used.

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
- **Optional mobile walkthrough:** instance `Mobile Flow Documentation` (`769:282`) from Device Kit. Populate its Overview
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
- When a runnable mobile walkthrough is in scope, keep its nested documentation hierarchy and add a labeled `PROTOTYPE FLOWS` strip of direct
  402×874 canonical screen instances inside the separate numbered prototype section. Wire and name
  real Presentation starting points and set interactions on instance descendants. Keep the
  component mapping in the structure contract so the strip cannot become an untracked duplicate.
- **One generation only.** When a native screen supersedes a legacy template, delete the legacy
  master and its now-empty page. Migrate anything still pointing at retired masters first.

## Finish

Register the screen + its states, verify against the app's fixtures (visual-verify), and note
it in `FIDELITY.md`. If the doc site embeds live Figma frames, point the embed at the section
or bezel node.
