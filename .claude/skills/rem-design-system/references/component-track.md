# Component track — canonical master + attached documentation

For a component (an atomic row/control or a composition such as a lockup/action area). Goal: one
canonical master plus a compact block a designer and developer can scan. `Component Documentation`
(`663:2270`) is the required template; `ListRow` and `Section` inside `Rows & Sections · Component
documentation` (`741:309`) are the worked examples.

## Build the master first (composable, slotted)

- Base it on the iOS 26 kit piece (copy the kit's Row/control in), then layer Rem's design.
- **Make fixed component regions instance-swap properties** and use a real Figma **Slot** when the
  region accepts repeated or arbitrary content. `Section` (`741:311`) uses an editable `Rows` slot
  with `ListRow` as the preferred value; Header/Footer are boolean/exposed nested properties.
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
- **Grouped-list ownership is explicit.** `Section.Rows` is a vertical HUG slot. The grouped
  surface belongs to the Section rows container; `ListRow` roots are transparent. Verify the
  master with one, two, and three inserted rows. After correcting a master, clear stale fill and
  fixed-height overrides on existing instances so older screens inherit the repair.
- **Interaction state is a component property.** Model `Enabled`, `Disabled`, and other durable
  interaction states as variants. Bind their visual treatment to semantic variables; never fake a
  disabled state by lowering the opacity of the entire instance.
- Metrics from the kit: single-line row **44–46pt** (Apple Row `Regular`), with-subtitle
  **~60–65pt** (`Tall`) via ~12pt top/bottom padding. Separator is a bottom-pinned hairline
  toggled per instance (last row of a card hides it).

## Required Builder documentation

Keep `Component Documentation` (`663:2270`) attached. Arrange related components in one horizontal
auto-layout row, with one named vertical column per component. Each column's order is:

1. **Canonical master or component set** — first and directly editable.
2. **Attached documentation instance** — second. It contains the lightweight overview metadata and
   no Component slot. Remove empty Anatomy/Props/Layout placeholders; the Specs plugin can create
   those later when requested.

The master and attached documentation instance stay in the same column so they move and scan as one
unit. Put the row on the component's family page inside a clearly named Figma Section.
Atomic controls belong under Primitives; components that assemble primitives and own layout,
semantics, behavior, or slots belong under Components/Compositions. Do not scatter masters, previews,
or generated artifacts as page-level siblings.

Composition roots inherit the surface behind them unless a background is part of the component's
owned contract. Horizontal groups of equal-priority actions use fill sizing for every action so label
length does not create unequal widths.

For platform-owned controls, search the subscribed official Apple and Material libraries first.
Nest official instances behind a thin Rem wrapper when the product needs shared semantics or
Platform switching. Use an official toolbar directly when its exposed API is sufficient; create a
Rem wrapper only for additional labels, slots, or cross-platform behavior. Never fork the whole kit.

The Builder does **not** need to reproduce an EightShapes Specs export. A human or later browser
agent may generate full Anatomy/Props/Layout/Data documentation after the runner batch finishes.
Preserve existing generated output in a named reference section; do not make its depth or visual
fidelity a delivery gate.

## Optional expanded documentation

When the task explicitly requests full documentation, extend the spec UI with:

1. header + description + source/Code Connect reference;
2. anatomy annotations;
3. variant/state matrices built from instances of the one master;
4. layout, data, accessibility, and platform notes.

## Variant/state matrices = wrapping tile grids

Not a flat horizontal strip. Each demo is a **tile**: a gray grouped-surface frame (radius 16,
~20 padding) with a small-caps label on top and a **white card holding the component instance**
inside (mirrors a real iOS grouped list, so the component reads clearly). Lay tiles out in a
`layoutWrap="WRAP"` grid with even spacing. Build every tile from **instances of the one
master** so the page stays in sync automatically.

Keep sub-component masters in their own documentation block or explicitly nested with their owning
component. Never park them loose below a frame where later runners cannot tell whether they are
canonical or scratch work.

## Deliver: Code Connect + Figma sync (required — never skip)

A component is **not delivered** until design and code are bound and in sync. This is the step
most easily forgotten in a delivery, so it is explicit here and belongs in every packet's
delivery contract:

- **Code Connect binding.** Every new or changed component ships current parserless `.figma.ts`
  templates for each platform label, mapping the Figma node and Slot/property API to the shipped
  SwiftUI and Compose source. Use separate config files with `language: swift` and
  `language: kotlin`; the `figma connect` CLI reads the templates without building native parsers.
- **Publish the Figma frame from code.** Regenerate/refresh the component's Figma master + doc
  page so **Figma follows the code**, never the reverse — this system is code-first. Publishing
  is plan-gated.
- **Tokens, not literals.** Any styleable value that isn't already a token goes through
  `tokens.json` + the generator, never a call-site literal.

If a delivery changes a component's shape but adds or updates **no** Code Connect binding, it is
incomplete. (This guard exists because a packet once omitted it — the obligation is structural
now, not a thing to remember.)

## Finish

Add/refresh the component's row in `REGISTRY.md` and the Figma Component Index. Verify each
variant against the app screenshots (visual-verify) and note it in `FIDELITY.md`.
