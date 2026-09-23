# Screen / pattern track — device-framed, native, all states

For a **screen or flow** (Agenda, Chat, Settings, Inbox, a pattern). A screen is *composed of*
canonical components — it is not a new component. Ground it in the real SwiftUI view so the
structure and every state are faithful, not invented.

## Ground in the source first

Open the shipping SwiftUI view (`Shared/Views/…`, `Rem/Sources/…`) and read its actual
structure **and its state machine**. The states are the point: e.g. Agenda has
scheduled/empty/loading/jump-to-today/sort-modes; Chat has the 3-way empty gate, run
lifecycle, tool-result error/unknown, and the voice bar's 6 states (from
`MiniPlayerBar.swift`: connecting/listening/speaking/muted/reading/closing). Enumerate them
before building — "every screen + its states" means all of them.

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
