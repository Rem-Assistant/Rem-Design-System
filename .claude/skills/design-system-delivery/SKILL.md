---
name: design-system-delivery
description: >-
  Systemize reusable product UI across Figma and code: canonical components,
  domain-organized flow pages, lightweight component documentation, platform
  translations, Code Connect, and machine-verifiable delivery. Use when a
  component, screen family, or design-system migration must remain reusable
  across projects. Pair it with the adopting project's local adapter skill.
---

# Design-system delivery

Build one durable system across design and implementation without forcing Figma,
SwiftUI, Compose, or the web to share the same internal representation.

## Start with the project adapter

Read the adopting project's local design-system skill or configuration first. It
owns the Figma file, registries, tokens, source repositories, canonical templates,
platform targets, evidence commands, and approval boundary. This skill owns the
portable method. Do not copy another project's node ids or visual language into a
new system.

Classify the task as one or more of:

- **Component:** create or repair one canonical reusable concept and migrate its
  consumers away from loose copies.
- **Flow:** document a screen journey, its branches, and a separate interactive
  prototype from real product behavior.
- **Migration:** reconcile older frames or code with a newly canonical component.

## Organize by product domain

Use one Figma page for a product domain whose flows benefit from comparison, such
as `Onboarding`, `Settings`, or `Checkout`. Do not create a page per screen. Within
the page, use a numbered pair of top-level Figma Sections per flow:

- `01A · <Flow> · Documentation`
- `01B · <Flow> · Prototype`

Add the next flow as `02A` / `02B`. Keep older canonical screens in a named
inventory or reference Section until their flow documentation replaces them. No
screen, master, or prototype destination may remain a loose page-level sibling.
Place section pairs on a simple grid and verify their bounds so later additions do
not overlap.

When white device frames sit inside a screen inventory, use the adopting project's
neutral canvas contrast surface for the inventory Section so device bounds remain
scannable. Keep that presentation surface separate from product background tokens;
the project's adapter owns the exact value.

Documentation uses editable layout templates and canonical component instances.
Prototype frames are separate top-level destinations sourced from the same states,
with only real entry points registered as Presentation starting points. Do not
invent a destination for an unfinished or deprecated next step.

Make reusable screens components when the same state appears in an inventory,
documentation flow, prototype, or handoff. Keep one canonical 1:1 screen master and
instance it everywhere else. The flow template itself should also remain attached:
compose Sections, Rows, Steps, Placeholders, and Screens through nested Slots rather
than asking consumers to detach a fixed template. This makes variable-length flows a
data-entry problem instead of a canvas-rebuilding problem.

Keep the flow chassis small and composable:

1. a documentation shell with an Overview slot and a Flow instance;
2. a Flow with a Sections instance;
3. Sections with an arbitrary-count section slot;
4. Section with a Rows slot;
5. Rows with an arbitrary-count flow-row slot;
6. a flow row with a Steps slot accepting alternating Placeholder and Arrow instances;
7. a Placeholder with editable Title/Note and one exact-size Screen slot.

Use the product's real screen dimensions for the final slot. Stretching a screen into a
nearby size can expose unresponsive internal layers even when the aspect ratio looks close.

## Make one canonical component

Search the project registry and Figma file before creating anything. If the
concept exists as a loose template, hand-built group, or obsolete component,
migrate its consumers and remove the competing source after the canonical master
is proven.

Run a pattern-extraction pass while composing each screen; do not wait for the
Director to name every reusable layer. Extract a screen region when it has one
stable responsibility and either (a) at least two plausible consumers or (b) an
observed recurrence across flows. Plausible consumers may include an approved
near-term flow, so the second copy does not need to exist yet. Examples include a
visual/title/body lockup, an action area, or a directional button group whose
content changes while its layout responsibility stays stable.

Avoid premature componentization. Keep a region local when its structure and
change reasons are unique to one screen, when reuse is only speculative, or when
the proposed component would merely wrap one child without owning behavior,
layout, semantics, or a slot contract. Record extracted concepts in the
reused/new component ledger so Reviewer can distinguish deliberate reuse from an
accidental abstraction.

Product-design exploration may discover or test the pattern; this design-system
skill owns the decision to promote it into a canonical component, define its API,
migrate consumers, and keep design and code synchronized.

Treat icons and brand marks the same way: import the codebase's exact SVG or raster
asset. Do not substitute a text glyph, emoji, geometric approximation, or a mark
that belongs to another product role. Record the source path and verify the asset
node in the machine contract.

Use component properties for fixed options and a Figma Slot for a flexible child
region. A list `Section`, for example, can expose boolean Header/Footer properties
and a `Rows` slot whose preferred value is `ListRow`. Slots preserve real nested
instances and allow an arbitrary row count without a variant for every length.

Document each canonical component with the adopting system's reusable component
documentation container. Its scan order is:

1. canonical master or component set;
2. a lightweight overview/spec surface.

Keep both inside one named auto-layout block and extend the container with slots
when that preserves attached masters and specimens. Do not invent a bespoke
documentation wrapper for each component family.

Keep full generated Specs-plugin output optional and separate. It can be created
after a runner batch; its visual depth is not a delivery gate for the component.

## Translate intent across platforms

A Figma component names a product concept, not necessarily one concrete class on
every platform. Preserve the component's intent, properties, slots, and tokens;
let the implementation use the platform-native form.

Treat **Theme** (`Light` / `Dark`) and **Platform** (`iOS` / `Android`) as
independent axes. Theme should usually switch semantic color variables. Platform
may switch typography values, native chrome, icon sources, metrics, or a nested
component. Do not multiply them into four hand-maintained screen copies when
independent variable modes and component properties can express the same system.
For system chrome such as a status bar, expose Platform switching and keep Theme
switching available inside each platform treatment. Map semantic icons to the
platform's native source; do not assume that swapping a font family preserves
glyph identity unless that mapping is explicitly verified.

For example, a Figma `Section` may map to native `SwiftUI.Section` inside a
`List`/`Form`, a small SwiftUI adapter when the same grouped surface is needed in a
custom `ScrollView`, and a token-bound Compose `Column` implementation. A Figma
`Rows` slot maps naturally to SwiftUI `@ViewBuilder` content and Compose
`ColumnScope` content. Record this mapping in Code Connect and the registry so the
translation is explicit rather than inferred from visual similarity.

## Deliver the system change

A system change is complete when:

- the Figma master is editable, token-bound, documented, and used by affected
  screens;
- stale loose copies or templates no longer compete with it;
- platform implementations preserve the agreed intent and native conventions;
- Code Connect or an equivalent source mapping names the exact code owner;
- the registry and human documentation are updated;
- a machine contract verifies page/section hierarchy, component ancestry, required
  states, prototype roots, and starting points;
- current rendered evidence is reviewed against the editable Figma states.

Keep exploratory proposals visibly separate from canonical as-built work. Promote
them only after the product decision and implementation are approved.
