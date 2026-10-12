# Local product-design policy supplement

This repository adopts these additions alongside the pinned upstream product-design-delivery skill. They were inherited from `claude/ds-flows` commit `93d8588`; they are locally maintained, not published upstream. User authorization and task boundaries continue to take precedence. The original added paragraphs are preserved verbatim below. See `provenance.json` for source hashes and insertion locations.

<!-- Addition after upstream line 16; original wording preserved. -->

Treat this as the parent workflow for user-facing product design. Invoke the
design-system child workflow when a decision creates or changes a reusable
component, pattern, template, or cross-platform contract. Invoke the
competitive-audit child workflow when another product supplies reference
evidence. Child workflows inherit this skill's product authority, lifecycle,
composition, and visual-review rules; they do not replace them.

Keep portable guidance at the level of decision boundaries and reusable
relationships. Put screen names, node ids, product copy, exact control lists,
and source-derived sequences in the adopting product's adapter, registry, or
flow contract. When an example helps explain a rule, introduce it explicitly
as an example and state the invariant it illustrates so the example is not
mistaken for the complete rule.


<!-- Addition after upstream line 29; original wording preserved. -->


## Derive the behavior before drawing states

Trace the real navigation and state ownership before turning a screen into a
flow. Read the route, callback, service boundary, and return path, then write a
short state-and-destination graph. For every visible enabled affordance, record
one outcome: a local component change, an in-product destination, an overlay, an
external or system handoff, or an intentionally disabled/no-op state. Do not
publish an enabled action whose outcome is unexplained.

Represent an interaction at the smallest scope that changes. Keep it inside an
interactive component when it does not change navigation, presentation context,
or the screen's information architecture. Add a screen or overlay state when it
does. Figma variables, conditionals, and multiple actions may model local logic;
they do not replace distinct destinations or externally owned surfaces.

Model an external boundary explicitly. Document the handoff, owner, return
conditions, cancellation, and recovery without inventing the unseen surface.
For example, authorization may leave the product for a browser and a native
choice may be owned by the operating system. If the provider, copy, or return
behavior is unsettled, preserve it as a product decision instead of guessing.

Reuse a component across contexts without assuming that the surrounding screen
composition is identical. Onboarding commonly teaches and sequences; Settings
commonly edits an established choice. Compare the two contexts' outcome,
information needs, navigation, and actions before reusing a whole screen.

<!-- Addition after upstream line 95; original wording preserved. -->

Treat create, read, update, and delete as a state family when the product lets
people manage the same entity. Do not call the flow complete with creation,
viewing, and deletion while leaving the update path undesigned. A reusable
inline-edit pattern for a Section or list row may keep the read state as title
+ subtitle/value with a trailing pencil; on edit, preserve the row geometry,
demote the persistent label to the secondary style, and focus an active text
field in the subtitle/value position so the platform keyboard appears. Replace
the pencil with explicit x-circle cancel and checkmark-circle confirm actions.
Cancel restores the prior value; confirm commits it, and keyboard Done may
commit the same action. Promote this to a shared row or field pattern only when
the anatomy and behavior recur or have an approved near-term consumer. Keep
genuinely screen-specific editing behavior in that screen's contract instead of
turning one example into a universal rule.

Keep each row region responsible for one kind of interaction. When a navigable
row's trailing slot is already occupied by a switch or another direct action,
do not stack an unrelated disclosure control beside it. If the product's
established pattern permits it, pair the disclosure indicator with the
text/content cluster and reserve the trailing slot for the direct control.

Build every product screen from canonical components, variants, slots, and
established patterns first. Before drawing any custom layer, inspect the
product's Section, ListRow, Lockup, ButtonGroup, ContainedIcon, and relevant
templates or their local equivalents; extend their slots or variants when the
need is reusable. A one-off component or hand-drawn product layer is a last
resort and must document why canonical components cannot express the
requirement. Configure existing instances and slots for stable responsibilities
such as identity/content lockups, lists and sections, footers, and action areas.
Create a new reusable component only when the structure or behavior recurs or
has an approved near-term consumer.

Before drawing a new layer, verify that no existing foundation, primitive,
platform control, composition, or domain component can express its role through
properties, slots, or composition. Interaction wrappers own behavior and
platform chrome; their content slots keep using the product's canonical content
components. Record a genuine system gap instead of silently detaching or
redrawing a near match.

Figma cannot override scrolling inside a linked or nested canonical screen
instance. Preserve that canonical instance unchanged: do not detach, replace,
hide, or mutate it to simulate scrolling. When scroll behavior must be shown,
create a clearly labeled non-canonical prototype specimen in a clipping
viewport, compose its contents from linked child components, and document both
the intentional clipping and this linked-instance limitation. Treat the
specimen as interaction evidence, not as a canonical screen or replacement
master.

## Define the abstraction before placing it

Use these boundaries across design artifacts and implementation handoff:

- **Component:** a reusable primitive or composition with a bounded
  responsibility and an explicit property, variant, or slot contract.
- **Pattern:** a reusable interaction, hierarchy, or state relationship built
  from canonical components. A pattern describes how parts behave together;
  it is not a copied screen.
- **Template:** a slot-based scaffold that arranges components and patterns
  without claiming a product route, backend state, or finalized copy.
- **Screen:** a product destination or meaningful product state composed from
  canonical components and patterns. Map it to the owning route, view, or source
  file when that implementation exists.
- **Flow:** the connected set of screens, overlays, component states, and
  external boundaries that expresses a user journey. Every branch exit needs a
  visible route label when its destination is not self-evident.

Classify an artifact before creating it. Do not promote a screen-specific layout
to a component, call a static screen a template, or treat a set of disconnected
screens as a flow. Product design owns the product decision and composition;
design-system delivery owns canonicalization, API definition, consumer
migration, and cross-platform synchronization once reuse is justified.

Assign surface styling to the highest reusable container that defines that
surface. Descendant content, scroll containers, and chrome inherit the parent or
use semantic material unless they intentionally introduce another surface.
Verify shadows and overlays with clipping disabled at the owning boundary.
Components that intentionally occlude content or own a system safe-area region,
such as fixed footers, sheets, bars, and navigation indicators, may own an
opaque semantic surface. Resolve that surface from the current screen state and
derive foreground contrast from it.


<!-- Addition after upstream line 147; original wording preserved. -->

Judge the product decision, composition, interaction semantics, and selected
canonical components against the contract. Presence, visibility, node type, or
plausible geometry alone cannot pass a screen whose control ownership, row
anatomy, action hierarchy, or state coverage is wrong.

<!-- Addition after upstream line 150; original wording preserved. -->


When several agents participate, reserve an independent pass to evaluate
whether the skill and raw product evidence were sufficient without corrective
prompting. Record prompt gaps in the project's evolution notes or task record
only when they repeat, reflect an explicit owner correction, or demonstrate a
verified reusable workflow improvement. Put portable decision rules in this
skill and product-specific facts in the adopting adapter or flow contract; do
not encode every isolated miss.

<!-- Addition after upstream line 161; original wording preserved. -->


For product Figma screen, component, and flow work, also follow
[references/figma-product-workflow.md](references/figma-product-workflow.md).
