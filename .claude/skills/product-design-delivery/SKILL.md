---
name: product-design-delivery
description: Shape, explore, approve, hand off, or review a user-facing product change against the adopting product's real design system. Use when work needs a design decision, an existing design must be reproduced faithfully, or Builder needs an approved interaction and visual contract before implementation. Do not use for purely internal changes with no user-facing behavior.
---

# Product design delivery

Move a user-facing change from intent to an approved, implementable design
without inventing product direction or treating implementation output as its
own specification.

The adopting product owns its design system, platform conventions, artifact
locations, approval authority, and verification tools. Discover and follow
that local context. This skill supplies the decision process and handoff
contract, not a universal visual style.

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

## Establish the authority

Before proposing a design, inspect the current product and the sources the
repository identifies as authoritative. These may include shipped behavior,
Figma, Claude Design, an Origami or motion prototype, screenshots, component
catalogs, design tokens, code, decision records, and prior user feedback.

Name the authority in the task. When sources disagree, do not silently choose
one. Surface the conflict and identify the decision needed.

Do not infer that an artifact is approved, current, shipped, or canonical from
its existence. Preserve the distinction between a concept, prototype,
reconstruction, approved design, implemented change, and verified release.

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

## Classify the work

Choose the mode that best describes the requested outcome:

- **Reproduce:** an authoritative design already exists. Preserve its
  composition and behavior; do not redesign it under the guise of improvement.
- **Extend:** add a state or flow by reusing established components and
  conventions. Prefer existing product grammar over a novel direction.
- **Design:** resolve a new product or interaction decision. Explore enough
  alternatives to expose the meaningful tradeoff, then converge.
- **Systemize:** create or change a reusable component, token, or pattern.
  Evaluate its family of states and downstream consumers, not only the first
  screen that needs it.

A task may move from Design to Systemize, but avoid expanding a bounded
Reproduce or Extend request without approval.

## Decide whether design is blocking Build

Design work is required before Build when an unresolved choice would
materially change layout, interaction, product behavior, content hierarchy,
or a reusable system rule.

Design work is not automatically required when:

- the authoritative artifact already specifies the requested behavior;
- the change is a faithful implementation or repair;
- the product gives Builder explicit, bounded design latitude; or
- the decision is an ordinary application of an established component.

When design is required, keep the item out of the build-ready queue until the
design authority approves the design contract. An agent may recommend a
direction but must not approve its own proposal. Design approval does not by
itself authorize unrelated implementation, publication, or release actions.

## Explore at the right fidelity

Start with the cheapest artifact that can answer the open question. Use flows
or wireframes for structure, interactive prototypes for behavior, and
high-fidelity compositions for visual decisions. Do not polish a direction
whose product logic is still unsettled.

When alternatives are useful, make them meaningfully different and explain
the tradeoff each tests. Stop generating options after a direction is chosen.
Carry the approved decisions forward instead of restarting from a blank canvas.

Keep distinct review passes when that improves judgment:

- product outcome and information hierarchy;
- layout, grouping, spacing, and responsive behavior;
- interaction, motion, and state transitions;
- content and voice;
- visual finish and system consistency.

Show the artifact before implementation at the decisions where visual or
interaction judgment matters. A textual description alone is not design
approval when the decision is inherently visual.

## Use the product's system

Reuse real components, tokens, assets, platform behavior, and naming. Inspect
their implemented states rather than relying only on a catalog thumbnail.
Account for relevant empty, loading, error, success, disabled, selected,
permission, interruption, and recovery states.

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

Treat motion as behavior: specify trigger, transition, continuity, completion,
interruption, and reduced-motion behavior where relevant. Treat accessibility
and platform conventions as part of the design rather than post-build cleanup.

Choose tools by the durable outcome:

- use a fast design canvas or interactive prototype to explore and gain
  alignment;
- use the product's canonical design tool for lasting system decisions and
  editable product artifacts;
- use the supplied source artifact for fidelity work;
- use code as the design medium when it is the clearest faithful expression of
  the interaction.

Do not translate an approved artifact into another tool merely to satisfy a
preferred workflow. Record where the authoritative result lives.

## Produce a design-ready task

Before handoff, create or validate the packet in
[references/design-ready-task.md](references/design-ready-task.md). Keep it in
the project's configured source of truth and link rather than duplicate large
artifacts.

The packet must make clear:

- the user outcome and bounded scope;
- the work mode and authoritative references;
- the approved composition, behavior, and relevant states;
- existing system elements to reuse;
- what must be exact, what Builder may adapt, and what is excluded;
- implementation constraints already known;
- the evidence required to judge the delivered result;
- approval state and any remaining human decision.

If a material decision remains open, return the item to design rather than
writing acceptance criteria that conceal the ambiguity.

## Handoff and review

Builder implements the approved contract and may surface constraints or
propose a bounded amendment. Builder must not silently substitute a different
product decision. Record approved amendments in the task so Reviewer judges
the current contract rather than an obsolete artifact.

Bind delivery evidence to the exact version under review. Use the correct
device, viewport, content, and environment. Screenshots prove states;
recordings prove transitions and interaction. Prefer a compact set that shows
the requested behavior over repeated or unrelated media.

Review both system consistency and perceptual fidelity. Metrics, snapshots,
tests, and successful builds are evidence, not substitutes for visual judgment.
Judge the product decision, composition, interaction semantics, and selected
canonical components against the contract. Presence, visibility, node type, or
plausible geometry alone cannot pass a screen whose control ownership, row
anatomy, action hierarchy, or state coverage is wrong.
When a material mismatch remains, describe it concretely and return it to
Builder or Design according to whether the cause is implementation or an
unresolved product decision.

When several agents participate, reserve an independent pass to evaluate
whether the skill and raw product evidence were sufficient without corrective
prompting. Record prompt gaps in the project's evolution notes or task record
only when they repeat, reflect an explicit owner correction, or demonstrate a
verified reusable workflow improvement. Put portable decision rules in this
skill and product-specific facts in the adopting adapter or flow contract; do
not encode every isolated miss.

## Preserve project control

Keep observations and alternatives in the project's intake or design view.
Promote only approved, bounded work into the executable issue queue. Update an
existing concern when possible instead of creating a new issue for every
comment or iteration.

Do not broaden scope, create external artifacts, publish designs, dispatch
Builder, or mark design approved unless the current task and project authority
permit that action.

For product Figma screen, component, and flow work, also follow
[references/figma-product-workflow.md](references/figma-product-workflow.md).
