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

## Make one canonical component

Search the project registry and Figma file before creating anything. If the
concept exists as a loose template, hand-built group, or obsolete component,
migrate its consumers and remove the competing source after the canonical master
is proven.

Treat icons and brand marks the same way: import the codebase's exact SVG or raster
asset. Do not substitute a text glyph, emoji, geometric approximation, or a mark
that belongs to another product role. Record the source path and verify the asset
node in the machine contract.

Use component properties for fixed options and a Figma Slot for a flexible child
region. A list `Section`, for example, can expose boolean Header/Footer properties
and a `Rows` slot whose preferred value is `ListRow`. Slots preserve real nested
instances and allow an arbitrary row count without a variant for every length.

Document each canonical component in a named auto-layout block:

1. canonical master or component set;
2. a lightweight overview/spec surface.

Keep full generated Specs-plugin output optional and separate. It can be created
after a runner batch; its visual depth is not a delivery gate for the component.

## Translate intent across platforms

A Figma component names a product concept, not necessarily one concrete class on
every platform. Preserve the component's intent, properties, slots, and tokens;
let the implementation use the platform-native form.

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
