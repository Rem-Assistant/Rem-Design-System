# Screen / pattern track — device-framed, native, all states

For a **screen or flow** (Agenda, Chat, Settings, Inbox, a pattern). A screen is *composed of*
canonical components — it is not a new component. Ground it in the real SwiftUI view so the
structure and every state are faithful, not invented.

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

## Build native, in auto-layout, on canonical components

- **Screen = a 402×874 component**, `layoutMode="VERTICAL"`, FIXED — the device content size,
  so it drops into the bezel size-safe.
- **Chrome from the kit:** the Apple **Navigation Bar** component (Style=Default inline /
  Large per screen; it includes the status bar) — or a domain header like
  `DateNavigationHeader`. Bottom chrome (toolbar/tab bar) pinned to the bottom via a
  `layoutGrow:1` spacer above it.
- **Body from canonical instances only** — `ListRow` (as grouped `Section`s: SectionHeader +
  rows + SectionFooter), `TaskEventRow`, `SuggestedTaskRow`, `MessageBubble`, `ComposerBar`,
  the cards. Set per-row content on the nested Content/Label (see figma-gotchas). Never
  hand-draw a row that a component covers.
- **Everything auto-layout.** If you must reuse an existing absolute-positioned frame,
  `detachInstance()`/clone it and convert to auto-layout — don't ship absolute positioning
  (it can't reflow and it's why the first Agenda had to be rebuilt native).

## House it in the real device bezel

- `DeviceFrame/iPhone` (`128:46`) = the **real Apple iPhone 16 Pro bezel** wrapping a
  402×874 **Screen instance-swap slot**. Drop a screen in by setting the `Screen` property to
  the screen's node id. All screens are the same size, so swapping is size-safe (no nested
  resize needed).

## Organize on the Screens page (Fluent-style)

- One **Screens** page, split into labeled **Sections** (Figma `createSection`), one per
  screen family: hero bezel + the state screens in a row + the source `Screen/*` components.
  Tighten each section to hug its contents; stack sections with a gap.
- **One generation only.** When a native screen supersedes a legacy template, delete the
  legacy master and its now-empty page. Migrate anything still pointing at retired masters
  first (drop refs → delete). The whole point is to not leave two generations drifting.

## Finish

Register the screen + its states, verify against the app's fixtures (visual-verify), and note
it in `FIDELITY.md`. If the doc site embeds live Figma frames, point the embed at the section
or bezel node.
