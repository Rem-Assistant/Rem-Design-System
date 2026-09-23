---
name: rem-design-system
description: >-
  Build and maintain the Rem design system in Figma via the Figma MCP plugin
  (`mcp__Figma__use_figma`) — iOS-26-grounded components, screens, and docs that
  the SwiftUI app is verified against. Use this whenever the task involves the Rem
  Figma file (key `af4yDqCzp57jds9lkFiIaO`), a Rem component or screen in Figma,
  design tokens/variables, the iOS 26 kit, SF Symbols in Figma, or "make/refresh
  the design system / a component page / a screen". Trigger it even when Figma
  isn't named but the work is clearly Rem design-system work (e.g. "add a Switch
  variant to the row", "document the composer", "put the settings screen in a
  device frame", "why do my swapped switches render tiny in Figma"). It encodes
  the anti-drift discipline and the hard-won Figma-plugin gotchas that make this
  work correctly instead of producing duplicates and broken layouts.
---

# Rem design system (Figma)

You are building/maintaining a **living** design system in one Figma file
(`af4yDqCzp57jds9lkFiIaO`) that mirrors the iOS 26 look and is **verified against the
real SwiftUI app**. The single hardest failure mode is **drift** — creating a second
version of something that already exists, or hand-drawing what should be a reused
component. Everything below exists to prevent that and to avoid the Figma-plugin traps
that silently produce broken output.

## Rule 0 — reuse before you create (anti-drift)

Before creating **any** component, style, or screen element:

1. **Check the registry.** `REGISTRY.md` (repo) and the **Component Index** page in Figma
   list every canonical component with its node id + SwiftUI source. If it exists,
   **instance the canonical master** — never re-draw it, never make a "v2".
2. **One canonical per concept, on its own named page.** No second generation, no bespoke
   copy. The worst messes this session came from breaking this (two ListRows, a bespoke
   `DateNav` beside the real `DateNavigationHeader`, a `Card` that should have been
   `Section`).
3. **Name to match SwiftUI.** The name *is* the code-connect: `Section` (not Card),
   `ContentUnavailableView` (not EmptyState), `Leading Accessory` / `Content` /
   `Trailing Accessory`. When you add a concept, add its row to `REGISTRY.md` **and** the
   Figma Component Index in the same change.

## Before the first `use_figma` call

Load `/figma-use` (or the `skill://figma/figma-use/SKILL.md` resource). Then read
**`references/figma-gotchas.md`** — it lists the plugin traps that waste hours if you
hit them cold (nested-instance size can't be overridden; `resize()` locks an axis;
`INSTANCE_SWAP` defaults want a *local node id*; SF Pro *does* render SF Symbols; etc.).
Skipping it reliably produces collapsed tiles, tofu glyphs, and rolled-back calls.

## Two tracks — pick by what you're making

The pattern that works for an **atomic component** is different from what a **screen**
needs. Don't force one onto the other.

- **Atomic component** (Row, Button, Card, MessageBubble, a control): follow
  **`references/component-track.md`** — a documented Fluent-style page (title · anatomy
  with native annotations · Variants matrices as tile grids), built on the iOS 26 kit
  with instance-swap slots for every configurable region.
- **Screen or pattern** (Agenda, Chat, Settings, a flow): follow
  **`references/screen-track.md`** — a full-device (402×874) screen built **native, in
  auto-layout, on the canonical components**, dropped into the real Apple device bezel via
  its Screen slot, organized in labeled Sections with all real states.

## Foundations (the layers everything binds to)

- **Tokens are the source.** `tokens/tokens.json` generates the Figma variable collections
  (Color Light/Dark, Spacing, Radius, Typography) plus a **Platform** collection (iOS /
  Android modes) that swaps values like `font/family` (SF Pro ↔ Roboto). Components bind to
  variables/styles, never hard-coded values, so a mode switch re-skins the whole file.
- **Type layer = the copied iOS 26 text styles** (full Apple Dynamic Type ramp, with the
  line-height + tracking bare size tokens drop). Bind every text node to a style, not a raw
  size.
- **Components sit on the iOS 26 kit.** Copy the kit's Row/List/controls in, put Rem's
  custom components on top, bind to the styles/variables above.

## Verification (this is why it's "verified against the app")

- The **`visual-verify` CI** (`Rem-Assistant/Rem`, `.github/workflows/visual-verify.yml`)
  builds the iOS app on a macOS runner and screenshots each `--rem-<screen>-fixture`. Those
  screenshots are the ground truth; compare each Figma component/screen to them and fix
  mismatches. Record the pass in `FIDELITY.md`.
- **Code Connect** (Figma↔SwiftUI) needs a paid Dev/Enterprise seat. Until then, record the
  binding in each component's Figma **description** and the `REGISTRY.md` mapping table —
  that's the manual equivalent, and it keeps the "app verified against" contract legible.

## After a meaningful change

Update `FIDELITY.md` and `REGISTRY.md`, refresh the Figma Component Index row, and (for app
changes) let visual-verify run. Commit docs to the design-system repo. Keep the file's page
list clean — one component per page, screens on the Screens page in Sections, no empty or
scratch pages.

## Reference files

- `references/figma-gotchas.md` — the `use_figma` plugin traps + how to work around each.
- `references/component-track.md` — the Fluent component-page recipe (anatomy, slots, matrices).
- `references/screen-track.md` — the device-bezel screen recipe (native auto-layout, sections, states).
- `references/sf-symbols-map.md` — verified SF Symbol → PUA codepoint table (the app's icon set)
  + how it was sourced. Read it before adding any icon so you reuse a verified codepoint.
