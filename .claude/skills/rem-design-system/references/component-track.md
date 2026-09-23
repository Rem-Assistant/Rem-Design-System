# Component track — the Fluent component page

For an **atomic component** (a row, control, card, bubble). Goal: one canonical master plus a
documented page a designer *and* a developer can both read. ListRow (`101:18`) is the worked
exemplar — copy its shape.

## Build the master first (composable, slotted)

- Base it on the iOS 26 kit piece (copy the kit's Row/control in), then layer Rem's design.
- **Make every configurable region an instance-swap slot**, not a boolean or a baked child.
  A row is three slots: **Leading Accessory** · **Content** · **Trailing Accessory**. The
  Content default is its own sub-component (`ListRowLabel`) that carries the text props, so
  the row stays composable (swap Content for a custom block) and the label still hosts
  Title/Subtitle.
- Each slot's default is a **local component node id** (see figma-gotchas: INSTANCE_SWAP).
  Provide a small, real set of options (Chevron/Switch/Button/None for trailing;
  ContainedIcon/Avatar for leading) and, for FIXED-size options like a switch, HUG-wrap the
  inner track so the swap resizes.
- **Bind** text → iOS 26 text styles, colors/spacing/radius → variables. Add the SwiftUI
  source to the master's `description`.
- Metrics from the kit: single-line row **44–46pt** (Apple Row `Regular`), with-subtitle
  **~60–65pt** (`Tall`) via ~12pt top/bottom padding. Separator is a bottom-pinned hairline
  toggled per instance (last row of a card hides it).

## The page = top-anchored vertical auto-layout

One vertical auto-layout frame so placement is automatic and **new examples append to the
bottom** — no hand-positioning. Order:

1. **Header** — big emphasized title (56–64 Bold), a one-line description, and **code-connect
   chips** (`SwiftUI · <file>.swift`, the pattern name). **No node-id pill** — that's for the
   machine, not humans; the id lives in the registry.
2. **Master** (labeled `MASTER`) — the actual component master, in the spine.
3. **Anatomy** — the component centered in a gray *grouped-surface* presentation tile, with
   its parts labeled by **native Figma annotations** carrying measurement properties
   (`{label:"Leading Accessory", properties:[{type:"width"},{type:"height"}]}`). Annotations
   show in Dev Mode, so say "open Dev Mode for the pins" in the subtitle.
4. **Variants** (one super-header) grouping sub-sections: one per **slot** (Leading Accessory,
   Trailing Accessory, …) plus **States** (content on/off, separators, etc.).

## Variant/state matrices = wrapping tile grids

Not a flat horizontal strip. Each demo is a **tile**: a gray grouped-surface frame (radius 16,
~20 padding) with a small-caps label on top and a **white card holding the component instance**
inside (mirrors a real iOS grouped list, so the component reads clearly). Lay tiles out in a
`layoutWrap="WRAP"` grid with even spacing. Build every tile from **instances of the one
master** so the page stays in sync automatically.

Keep the sub-component masters (e.g. `ListRowLabel`, `Avatar`) **out of the doc frame** (they
overlap it) — park them below, or on their own page per the one-component-per-page rule.

## Finish

Add/refresh the component's row in `REGISTRY.md` and the Figma Component Index. Verify each
variant against the app screenshots (visual-verify) and note it in `FIDELITY.md`.
