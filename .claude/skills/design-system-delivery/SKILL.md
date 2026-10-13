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

This is a child workflow of `product-design-delivery`. Product design owns the
user outcome, product authority, lifecycle decision, and approved composition.
This workflow begins when that work needs canonical components, patterns,
templates, migrations, or cross-platform synchronization. Competitive audit is
a further child reference workflow: it supplies evidence and never becomes the
target product UI by itself.

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
- **Competitive audit:** organize source-app screenshots into evidence-backed,
  navigable reference flows without treating the source UI as adopted product design.
- **Migration:** reconcile older frames or code with a newly canonical component.

For a competitive audit, read `references/competitive-audit.md`. It is a child workflow of this
skill, alongside the component and flow methods. Keep the parent skill responsible for shared
abstraction, reuse, evidence, and delivery rules; keep project-specific page names, node ids,
tokens, and source-app inventory in the adopting project's adapter.

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

Keep canonical screen masters for that domain in one clearly named component
Section on the same page, aligned with the flow grid. Screen masters are product
artifacts, while reusable primitives and compositions remain on their taxonomy
pages. This keeps one product domain from being split across a flow page and a
second screen-master page without turning the component index into another
canvas.

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

Treat trailing controls according to behavior. A chevron communicates navigation;
a Play, Pause, Retry, or Connect control performs an action in place. Do not pair a
direct action with a chevron, and do not add a decorative leading icon when the row
already communicates its purpose through label, value, and explicit trailing
control.

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
documentation container. Arrange related components as a horizontal auto-layout row of
vertical columns. Each column's scan order is:

1. canonical master or component set;
2. an attached documentation-template instance containing the lightweight overview/spec surface.

Keep the actual master directly above the template so it remains the editable source. The
documentation template does not own or repeat the specimen through a Component slot. Do not detach
the template or invent a bespoke documentation wrapper for each component family.

Treat the live Figma library as the source of truth for mutable canvas anatomy. Before changing a
template or component family, inspect its current canonical node, properties, auto-layout, parent
Section, variables, and instances. Encode durable intent here—scan order, semantic ownership,
responsiveness, and verification—while keeping project-specific node ids and current anatomy in the
adopting system's adapter and registry. When a designer changes the canonical Figma shape, update the
adapter from that live shape rather than recreating an older shape from prose.

Score every touched component family against the same foundations rubric:

1. typography uses the adopting system's text styles;
2. fills, strokes, spacing, radius, and effects use semantic variables or styles where applicable;
3. responsive structure uses auto layout, fill/hug, and explicit caps rather than spacer frames;
4. consumers use canonical instances and exposed properties/slots rather than detached copies;
5. taxonomy matches the abstraction level: Foundations, Primitives, Compositions, Templates, or Screens.

Record which dimensions were machine-verified and which need visual review. A regression fixture for
one component proves that rule for that component; it must not become a Button-specific definition of
design-system quality. Extend the data-driven audit list whenever a touched family is canonicalized.

Treat a reusable element as a composition component when it assembles primitives and
owns layout, semantics, behavior, or a slot contract. Keep atomic controls and assets in
the primitive layer. Composition roots inherit the surface on which they are placed unless
owning a background is part of their contract. In a horizontal action group, equal-priority
actions fill the available width equally rather than sizing from their labels.

Classify controls by interaction semantics before naming a product-specific component. Test
whether the proposed control is an existing primitive with different content, state, or slot
placement. For example, icon-only affordances belong to the Button family. Product state may
change the Button's icon and accessible label, but it does not define a new control.

Keep the component index as an index. Put aggregate composition masters and their documentation on
a dedicated Compositions page; put indivisible controls and assets on Primitives. The page boundary
should communicate abstraction level without making the index itself another component canvas.
Organize each component-family page with top-level native Figma Sections for canvas navigation and
one transparent auto-layout `Content` frame inside each Section. `Content` owns the visible large
header, description, divider, documentation rows, and their spacing, so adding, removing, or
reordering children heals automatically. The native Section is a bounds shell, not a layout engine:
after every content mutation, refit its bounds to `Content` with the established outer inset and
verify neighboring Sections do not overlap. Canvas Sections do not own a fill or stroke; the
documentation surface binds `background/primary`, so the component's real surface remains visible
and theme-aware. If the active Figma runtime cannot refit a native Section reliably, use the same
auto-layout frame and visual treatment as the temporary top-level container rather than positioning
children manually.

Do not turn the width of one reference device into a fixed reusable-component width. When code or
the product layout defines a content cap, make the composition fill its parent up to that maximum;
make internal slots and wrapping text fill the composition. Keep fixed widths for genuinely fixed
assets or device slots. Derive the maximum from the shipping implementation or an approved layout
contract rather than inventing one from the current canvas.

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
Treat authentication providers and system-owned surfaces as platform capabilities,
not visual variants. Verify product and platform support before claiming parity. For
example, do not expose Sign in with Apple on Android unless the product explicitly
implements a supported web authorization path.
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
- every changed or consumed canonical family passes a binding audit at its master: text layers use
  local text styles, semantic colors use variables/styles, related children use auto layout, flexible
  content uses fill/hug rather than spacer frames, and screens consume instances rather than copies;
- a machine contract verifies page/section hierarchy, component ancestry, required
  states, critical style bindings, prototype roots, and starting points;
- current rendered evidence is reviewed against the editable Figma states.

When comparing evidence from different renderers, normalize every image to its declared logical
viewport before judging composition, spacing, or relative scale. Raw PNG dimensions, capture
density, and device pixel ratio are not layout differences. Treat platform status/navigation
chrome, safe-area insets, and native type rasterization as platform adaptations unless the screen
contract marks them exact.

Keep exploratory proposals visibly separate from canonical as-built work. Promote
them only after the product decision and implementation are approved.
