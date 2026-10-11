# Product Figma workflow

Use this reference when the durable design artifact is a product Figma file.
The current contracts emphasize mobile screens, flows, and platform controls,
but the evidence, composition, ownership, and topology rules also apply to
larger viewports. The adopting product still owns its exact components, page
names, templates, tokens, and platform rules.

## Exhaust evidence before inference

Use this evidence ladder before deciding where a control belongs, how an action
behaves, or where an overlay points:

1. Inspect the shipping implementation with callers, routes, state ownership,
   callbacks, and service boundaries. A source file without a caller is not
   proof of current product behavior.
2. Search the product's current design evidence: canonical Figma masters,
   approved recordings and captures, specs, registries, decisions, and prior
   feedback.
3. For platform-owned behavior, inspect the official platform component or
   documentation rather than reconstructing it from visual resemblance.
4. Reconcile the sources. If they conflict, record the conflict and do not
   silently choose whichever source is easiest to draw.
5. Ask the product owner only when authoritative evidence is inaccessible,
   conflicts remain material, or the missing answer is a product decision.
6. Infer only after the earlier steps fail. Label the result Proposed, record
   the evidence gap and assumption, and define what evidence would close it.

Never present inferred geometry or interaction as Production. A recording that
shows the target is stronger evidence for spotlight position than a plausible
control inferred from the surrounding layout.

## Compose before drawing

Decompose each screen into canonical primitives and compositions before adding
custom frames. Configure instances, properties, variants, and slots. A custom
region is justified only when no canonical element owns the required layout,
semantics, or behavior. Record the gap so a recurring structure can be promoted
deliberately instead of copied.

Using a canonical component is not sufficient evidence that an instance still
conforms to it. After swapping, resizing, or overriding an instance, compare its
rendered height, supported width behavior, variant and Boolean properties, text
styles, variable bindings, clipping, and visible slots with the canonical
master. Flexible dimensions may adapt only where the component contract allows;
fixed dimensions such as a button's control height must match the selected size
variant. Run this conformance check across every component family used by the
screen, not only the instance that was most recently edited.

When a canonical row exposes content and accessory seams, keep leading and
trailing regions hugging their slotted content and let the center content fill
and wrap. Replace only the responsibility that differs. Validate the row with a
long title, multiline subtitle, wide trailing control, hidden accessories, and
each supported section style before creating a context-specific row.

For a navigable Rem row whose trailing slot is occupied by a switch or direct
action, keep that slot dedicated to the control. Pair the disclosure chevron
with the text/content cluster instead of stacking the chevron beside an
unrelated trailing control. Verify that tapping the content cluster navigates
and that the trailing control performs only its direct action.

When a container accepts a variable number of children, expose that collection
as a slot and make its auto-layout axis hug content. Adding or deleting a child
must reflow every later sibling without a spacer frame, manual gap, overlap, or
clipping repair.

Apply that rule across the whole file, not only to newly created nodes. Before
closing a page, audit existing one-off frames against the current foundations,
primitives, compositions, platform wrappers, and screen templates. When a
canonical component can express the structure, migrate the screen to an
instance. When the component is close but insufficient, extend the canonical
source and migrate its consumers instead of creating a context-named clone.

Default rendered interface copy to sentence case unless verified product copy,
platform convention, locale, or brand guidance requires another treatment.
Layer names, component-property values, API constants, and source enums are
metadata; do not transfer their all-capitalized casing into visible labels or
section headers.

Treat platform controls the same way. Preserve one product-level contract and
place the official iOS and Android controls behind a Platform variant. Bind the
product's accent and semantic surface tokens where the platform control permits
it, while preserving that platform's behavior, shape, and interaction model.
Do not restyle one platform to imitate the other.

Gesture and interaction wrappers own reveal behavior and platform action
chrome; content components own their data and layout. Preserve the content
through a slot instead of rebuilding titles, metadata, badges, or pills inside
each interaction state. Containers around a revealed interaction must not clip
the revealed region.

## Close every action

For each visible enabled affordance, document its outcome as one of:

- a local component state change;
- navigation to an in-product destination;
- an overlay or presentation-context change;
- a handoff to a platform-owned or externally hosted surface; or
- an intentionally disabled or no-op state.

Represent the outcome at the smallest scope that changes. Prefer component
interactivity when navigation, presentation context, and screen information
architecture remain unchanged. Add a destination or overlay state when any of
those change. Use variables, conditionals, and multiple actions where they make
the prototype smaller and clearer, not to hide a real destination.

An action whose confirmed outcome irreversibly deletes, clears, removes, or
resets user data uses the product's semantic destructive treatment. Bind its
label, icon, and tint to the destructive token instead of hardcoding a red.
Navigation into a management screen remains a normal navigation action until
the destructive operation itself is presented.

Choose button emphasis from the action hierarchy before selecting a visual
variant. Give one recommended forward or recovery action the product's primary
accent treatment. Use a secondary treatment for a safe alternative of lower or
similar priority. Use black, inverse, or another brand-specific primary only on
surfaces whose established product treatment calls for it; do not use it as a
generic stronger primary. Give an irreversible reset, removal, or deletion the
destructive treatment and keep it visually subordinate until the user enters a
destructive confirmation context where it becomes the principal action.

Use a minimal action hierarchy and reveal controls only when they are relevant
to the current state. Give one action primary emphasis. When a destructive
action is paired with the primary action, use secondary-destructive styling: a
neutral fill with a red label. Use primary destructive styling only when the
destructive action stands alone and is intentionally dominant. Pair Save or
Done with Remove, Delete, or Revoke in a full-width vertical `ButtonGroup`; do
not use loose actions or emphasize both buttons simultaneously. Treat
minimality and progressive disclosure as review principles rather than showing
every possible control at once.

Editable list-row fields use progressive disclosure. In the read state, show
the title and subtitle or value with a trailing pencil. In the edit state,
replace the editable value in the same row and position with the text field.
Preserve the text field's internal platform padding while aligning its outer
content edge with ordinary row text; never compound the component inset with
the row inset.

Document a transient control with the control that invokes it and the state or
destination it produces. Do not present a menu, picker, popover, dialog, or
sheet as an orphaned artifact. Preserve the base screen and its triggering row
or button, then show the transient control in the resulting presentation state.

The Rem Add/Edit Provider Key surface uses one two-row Section. The first row is
`Provider` with the existing menu-selector pattern in its trailing region; the
second row is the provider's API-key text field. Do not infer or lock the
provider from the previous screen unless the flow contract explicitly requires
that narrower route. Compose cancel, confirm, and save actions with the existing
`ButtonGroup` and inline primary `Button` components, centered at their
component-defined size. Cover create, edit, and delete states, including each
completion, cancellation, and destructive-confirmation outcome.

For iOS system symbols, use the design tool's native SF Symbol lookup or an
official SF Symbol instance. Do not hand-draw, mask, rasterize, or paste an
unverified glyph in place of a platform symbol.

Treat product and provider marks with the same provenance standard. Use an
official brand asset or a verified asset already shipped by the product. Do not
substitute an SF Symbol for a known provider logo. If the official asset cannot
be obtained, record the missing asset as explicit design debt instead of making
an approximation look canonical. Preserve the mark's transparent background
unless the official asset itself includes a container, center it in the leading
slot on both axes, and audit every state master after an instance swap because
old fill and alignment overrides can survive the swap.

Treat an authored sequence length as a coverage contract. If a walkthrough,
setup, or guided overlay declares three steps, deliver three observable states
and three prototype frames unless two steps are proven component-local states
of the same frame. A step label such as `1 of 3` is not evidence for steps 2 and
3. For coachmarks and spotlights, reveal the real underlying control with a
cutout; do not redraw the target above the screen and create a second apparent
entry point.

## Arrange a domain page

Keep Rem design production and code implementation as separate phases. During
a design-only lane, Figma Design owns finished editable screens and components,
and FigJam owns topology, branches, and connectors. Do not modify SwiftUI,
dispatch implementation agents, or treat approved design work as authorization
to start code. Begin implementation only when Samuel explicitly opens the code
phase; carry the approved screen and state contract into that later handoff.

Before designing a missing state, search authoritative prior Figma files and
the current code. Transfer and reconcile existing product decisions before
inventing. Prior screens are evidence, not visible source inventory for the new
topology. Reconcile that prior design evidence with current code and newer
approved states; do not blindly revert or overwrite verified improvements.

Before changing an existing topology state, capture a preservation inventory:
its node ID, visible screen source, fill or component provenance, label,
connectors, and a readable before render. Never clear a verified screen fill,
replace a canonical screen with a gap card, or delete an existing state or
connector merely to expose a TODO. A gap may replace an existing screen only
when the evidence proves that the screen is invalid and the root coordinator
authorizes that bounded replacement. If the source cannot be verified,
preserve the current state and record the verification gap outside the canvas.
When a pass is rejected, stop writing, return the exact changed node IDs, and
let the coordinator restore selectively from the preservation inventory.

During topology migration, leave legacy or source screens in their original
Figma location while the new map is built. Do not paste a source inventory,
migration scaffold, audit trail, or reference board into the new topology.
Transfer each required screen once as an individually placed editable instance
and connect it according to the new state and navigation graph. The new topology
is the forward-looking canonical flow presentation. After screen and state
parity plus connector verification, repoint prototype links and remove the
superseded legacy flow only when Samuel has authorized deletion. Keep internal
source IDs and implementation references in the delivery record, not as visible
migration prose on the canvas.

Serialize cross-file visual transfers. Reselect and verify the target page
immediately before each paste, then verify the resulting node and its parent
page immediately afterward. If the active page changed or a node landed on the
wrong page, stop and reconcile every misplaced node before continuing. Do not
batch cross-file pastes across pages from a stale page selection.

Use the Settings topology exemplar as the visual composition contract. The page
or top-level Section may have one subtle neutral background. Each screen stands
directly on that surface with a small plain neutral-gray title rectangle above
and, when needed, one short note below. The title, screen, and note may be
grouped invisibly for movement and connector attachment. Do not draw a visible
outer card, rounded wrapper, border, header strip, or nested Section around an
individual screen. Do not interpret `group` as visible container chrome.
Connectors leave the source group or screen from BOTTOM and enter the
destination group or screen at TOP. Visible labels and notes must never include
Figma URLs, node IDs, migration status, agent scoring, or process metadata. Keep
that provenance in the delivery record only.

Do not add global app-entry wiring unless the task explicitly includes it. Omit
visible scoring labels such as `speed-adjusted`, `consistency-adjusted`, or
`likeness-adjusted` unless Samuel explicitly requests scoring.

Begin from the product's real entry point when one exists. When a flow has no
real product screen before its first state, do not invent a root screen or use a
full-flow overview as the first node. Use a compact app icon or clearly labeled
entry marker, then branch directly to the first real screens. Treat the marker
as topology chrome, not as a product screen or substitute for one.

Map a stable surface with many interaction or rendering modes as a
scenario/state matrix or local branches from one canonical surface. Give
separate entry contexts, such as Daily Brief, their own branches. Do not turn
scenario and state variants into a fake linear navigation flow.

Build a state family from one canonical base screen. Record the family's
invariants—navigation, safe area, background, composer or toolbar, typography,
and recurring content regions—and change only the cue that defines each state.
Do not independently redraw every state. Before review, compare siblings and
reject unexplained drift such as a missing model affordance, a different
composer, inconsistent user-message bubble, changed keyboard generation, or a
shifted safe-area treatment.

Optimize the topology for human legibility. Organize compact local scenario or
CRUD branches and use short orthogonal connector routes. No connector may
cross, overlap, or pass through an unrelated screen, label, or branch. Show the
primary branch-defining forward transitions. Routine Back, Cancel, Close,
dismiss, and obvious return paths may be omitted unless they change state
materially. When one graph remains dense, split it into multiple labeled
sections on the same page rather than building a giant exhaustive graph.
Always show the material completion route from a successful Save or create
outcome to the saved detail or canonical parent, and from post-delete feedback
to the canonical parent.

Follow the Settings hierarchy by default: center the canonical parent at the
top, place its children beneath it, place grandchildren beneath their parents,
and keep siblings on one row. Use short orthogonal routes that leave the source
from BOTTOM and enter the destination at TOP. Display every screen at one
consistent proportional scale.

Validate connector attachment structurally, not by appearance. The connector
start endpoint must reference the source screen or invisible screen group and
use BOTTOM; the connector end endpoint must reference the destination screen or
group and use TOP. An endpoint ID that resolves to the page or root Section
fails, as does a free-positioned line segment that merely looks aligned with the
screens.

Separate review targets explicitly. FigJam is the review surface for topology,
branch hierarchy, and transitions. Figma Design is the review surface for the
editable canonical screen, component, typography, spacing, and platform-control
construction. Every handoff must link both targets and state which questions
belong in each so the reviewer is not expected to find the full transition map
inside the component-library page.

The label above a screen owns the screen or state name. A connector label owns
only the transition or user action, such as `Open`, `Save`, or `Retry`. Never
repeat the destination screen name on the connector or let connector text
overlap the screen label.

The atomic topology artifact is a complete application screen or modal state
in its real application context. Component changes such as a composer, picker,
voice bar, toast, banner, title edit, or scroll state must be presented within
the full screen that contains them; never connect topology arrows to isolated
component specimens. Do not wrap a complete app screen in an additional
decorative device frame; the app frame itself is the screen.

Keep a state variant, modal, or sheet inside its established parent flow
section and show it over the complete parent-screen context. Never create a new
canvas Section merely for a state. Keep the navigation title invariant across
states of the same destination; change the title only when navigation changes
the destination.

Every labeled state must visibly prove its distinguishing cue inside the full
screen; the label alone is not evidence. For example, an editing state shows
the blue insertion cursor or the appropriate platform focus cue. In
keyboard-visible chat or editor states, keep the bottom composer anchored to
its intended bottom or keyboard edge rather than moving or reflowing it with
the keyboard.

Keep canonical screen masters and destination-specific documentation on the
owning domain page. When the file has a shared prototype page, place the
interconnected runtime graph there and
instance the domain masters into it. Render global navigation once, connect
domain branches to the same destination instances, and create another screen
only when navigation, presentation context, or information architecture
changes. Do not duplicate whole screens merely to demonstrate component-local
state.

Give reusable screen and component masters concise descriptions that record
their product purpose, distinguishing state, canonical source, and any bounded
temporary TODO. Agents must inspect these descriptions before reusing a master.
Remove the TODO as soon as the discrepancy is resolved; descriptions are a
handoff aid, not a permanent issue tracker or a substitute for visible design.

A canonical screen master is incomplete until every visible enabled route it
owns appears in an attached flow or is explicitly documented as local,
platform-owned, external, disabled, or deferred. Compare the canonical-screen
inventory with Mobile Flow references in the delivery record before closing the
page; a master created without a documented route is unfinished work.

Treat completion as rendered coverage. Compare the source-derived route or step
count with the visible canonical screens in its documentation section.
Prototype states elsewhere do not satisfy documentation coverage. The topology
is incomplete if it contains any source-inventory block, uses a placeholder when
an existing screen is available, omits a visible connector for a mapped
transition, or fails to account for any original in-scope screen. A visible
outer card, rounded wrapper, border, header strip, or nested Section around an
individual screen is a blocking visual-grammar violation. Visible Figma URLs,
node IDs, migration status, agent scoring, or process metadata are also
blocking. The topology must contain no component-only nodes and no complete app
screen nested inside an additional decorative device mockup. An isolated
component used as a topology node or a nested decorative device mockup is a
blocking review violation. Completion also requires reconciling prior design
evidence, current code, and newer approved states without reverting a verified
improvement. Every shown connector must use a short orthogonal route without
crossing, overlapping, or passing through an unrelated screen, label, or
branch. Split a graph that remains dense into multiple labeled sections on the
same page. Blindly reverting a newer verified improvement, connector crossings
or overlaps, routes through unrelated content, and a giant exhaustive graph
that remains unreadable are blocking review violations. Verify each connector's
start and end references bind to the actual source and destination screen groups
with BOTTOM and TOP respectively; page or root Section endpoint IDs do not
count. Verify visible completion routes from Save or create outcomes and
post-delete feedback to the saved detail or canonical parent. Completion also
requires state variants to remain in their parent flow section over the full
parent-screen context, stable navigation titles within one destination,
a trailing-pencil read state whose value becomes a text field in the same row
position, and aligned field content without compounded insets. Use one primary
action, secondary-destructive styling for a paired destructive action, and a
full-width vertical `ButtonGroup` for paired save and destructive actions. A
state-only canvas Section, overlay without full context, title drift, compounded
insets, a missing trailing-pencil read state, moving the edit field away from
the value's row position, loose paired actions, primary-destructive styling on
a paired action, two simultaneously emphasized actions, or controls shown
before their state makes them relevant is blocking. Required screens may not be
blank, placeholder-only, component-only, misplaced, or missing. Every labeled
state must visibly show its distinguishing cue, keyboard-visible chat/editor
states must keep the bottom composer anchored, and all screens must use a
consistent displayed scale. Unless verified product evidence requires an
exception, confirm the canonical parent is centered at the top, descendants
step downward by generation, and siblings share one row. Missing state cues,
composer reflow, inconsistent scale, an unexplained hierarchy departure, or
required screens that are blank, placeholder-only, component-only, misplaced,
or missing are blocking review failures. Page- or root-Section-bound connector
endpoints, free-positioned lookalike line segments, wrong endpoint magnets, or
missing Save/create/post-delete completion routes are also blocking.

If required work is incomplete or a capability fails, escalate to the steward
or root coordinator with the exact missing evidence and required action. A lane
must not stop and report completion while required screens are blank,
placeholder-only, component-only, misplaced, or missing; that is a blocking
delivery failure.

Treat structural validation and visual validation as separate completion gates.
After composing a screen or making a meaningful visual change, render that
screen at its native device size or another readable scale and inspect the
pixels. Do not call the screen complete until the latest render passes hierarchy,
spacing, alignment, typography, canonical component state, clipping, and action
priority. If the inspection causes a visual correction, render and inspect the
screen again; the latest passing render is the completion evidence.

The visual gate also verifies that text layers use the canonical text styles,
reusable controls remain component instances, platform controls use the
approved current-platform assets, bottom actions clear the safe area, and
semantic icon colors are preserved. Oversized ad hoc text, lookalike menus,
stale keyboards, missing home indicators, or flattened control substitutes are
blocking failures even when the topology is correct.

Add a provenance gate before accepting either review. For every screen counted
as complete, verify that its visible UI is an approved source screen or is built
from editable instances of the adopting product's canonical components. Reject
SVG imports, flattened snapshots, and ad hoc rectangles and text that merely
imitate the product UI. These may document a clearly labeled product gap, but
they cannot pass as finished design-system screens. Report topology completion
and screen-design completion separately so a correct map cannot conceal fake or
unverified screens.

Canonical components composed directly on a FigJam board can form a legitimate
full-screen topology prototype, but they do not create a canonical screen
master. Count that state as topology evidence only until the composition exists
as an editable screen in Figma Design and the FigJam node references that
master. Record these as `FigJam-composed` rather than calling them fake or
complete; this preserves the useful product work while keeping ownership clear.

For a multi-surface design program with persistent lane chats, collect Director
feedback through one root coordinator. The coordinator must preserve the
Director's wording and evidence reference, classify each item as a product
decision, visual defect, structural defect, missing state, or provenance gap,
and route only the bounded delta to the existing owning lane. Do not create a
new chat, sub-lane, or independent goal for each note or review pass. If the
Director gives feedback directly to a lane, that lane must return the decision
and resulting evidence to the coordinator's shared ledger so later work does
not depend on replaying chat history or asking the Director to repeat it.

Treat a cloud lane brief as a portable evidence packet. Never make an unpushed
local file or workspace path a prerequisite. Inline the complete bounded
correction set, exact Figma/FigJam links and node IDs, acceptance criteria, and
the code-edit boundary. A local ledger may remain the coordinator's durable
record, but the receiving cloud lane must be able to perform the assignment
without opening it. When neither the portable brief nor accessible design
evidence defines a product detail, report that detail as unresolved rather than
inventing a screen.

A navigation affordance is unresolved until its destination is represented, or
explicitly marked external, platform-owned, proposed, or deferred.

Use connector lines to document relationships between frames and branches.
Connector lines document an edge; actual prototype reactions carry the
interaction contract and require valid top-level prototype destinations. Name
connector line layers as `<source> → <destination>` and anchor them near the
source affordance when the mapping would otherwise be ambiguous.

When the adopting design system owns a connector component, every documented
edge must be an instance of that component. Do not substitute rectangles,
lines, or one-off vectors. After resizing an instance, compare its rendered
bounds with its instance bounds; allow only the connector's documented stroke
and arrowhead tolerance. A canonical instance still fails conformance when its
internal path scales beyond the instance and produces an oversized route.

Anchor each flow edge to the visible trigger and destination it represents.
Prefer a direct connector when the endpoints remain legible. When distance or
crossings make the relationship ambiguous, add a concise relationship label or
use a shared branch spine. Do not use an unlabeled rail as a substitute for
known trigger ownership. Express an arrow as the connector vector's terminal
endpoint style rather than as a separate decorative shape.

Treat a shared branch spine as required when one screen fans out to three or
more vertically separated destinations, or when three or more routes from that
screen would each travel more than one device height. Connect every visible
trigger to the spine with the canonical arrowless Join variant, render one
canonical stretchable Spine variant, and connect the spine locally to every
destination with the canonical arrow-bearing connector. Preserve trigger
ownership through source order, route labels, or numbering. Do not emit a bank
of parallel full-height connectors when this threshold is met. A workflow that
mentions a branch spine must also provide canonical spine and join components;
do not leave the pattern as prose that can only be satisfied with hand-drawn
geometry.

Treat custom Design-file connector lines as derived geometry. After inserting,
removing, moving, or resizing a source trigger, destination, or branch
container, regenerate connector endpoints and route labels from current
absolute bounds, then visually verify every edge. Do not treat previous vector
coordinates as authoritative.

Within one documentation flow, render each canonical full-screen destination
once. When multiple actions share a predecessor, keep one predecessor and
branch from its actual trigger controls. Duplicate a full screen only when the
duplicate represents a materially different state, platform, presentation
context, or viewport position.

Represent branching route trees with nested auto layout. Use one instance of
every shared predecessor. An HStack may contain destination VStacks and further
nested HStacks. Connector lines fan out from the shared predecessor. Top-align
the source with the first destination unless verified source evidence requires
another alignment.

Figma cannot override scrolling inside a linked or nested canonical screen
instance. Preserve that canonical instance unchanged: do not detach, replace,
hide, or mutate it to simulate scrolling. When the prototype must demonstrate
scrolling, create a clearly labeled non-canonical prototype specimen in a
clipping viewport and compose its content from linked child components.
Document the intentional clipping and the linked-instance limitation beside the
specimen. It is interaction evidence, not a canonical screen or replacement
master.

An overlay state is incomplete without its presentation context. Show the real
trigger or destination screen beneath the scrim, then place the sheet, dialog,
menu, coachmark, or popover above it. Do not use blank rectangles or generic
placeholder rows as the backing screen when a canonical screen exists.

An overlay state must share the backing screen's origin and dimensions. Verify
that the scrim and overlay root are at `x=0`, `y=0` and match the screen width
and height; bottom-align the presented surface within those bounds.

Keep one same-page prototype graph containing one instance per unique
full-screen destination. Domain pages own canonical masters and documentation;
the prototype graph uses their instances and reuses destinations across
incoming routes. Component-local state belongs in interactive components rather
than duplicated full-screen prototype nodes.

Separate list responsibilities. The parent Body or List owns screen-level
horizontal inset and spacing between sections. Section owns its grouped or plain
surface plus header/footer rhythm. Slotted rows own row density and divider
visibility. Do not add a Section Boolean that cannot actually override arbitrary
slot children. For phone layouts, start with Inset Grouped and Plain; add a
full-width Grouped variant only when source UI proves a distinct surface, and
reserve Sidebar for adaptive tablet or desktop navigation. Treat Automatic as
a code default, not a Figma appearance variant.

No screen, component master, prototype destination, or explanatory frame may
remain as an unowned page-level object. Place it in the domain's established
layout container and verify that deleting or inserting content heals spacing.

Before declaring a page complete, verify both containment and collision. Every
owned descendant's absolute bounds must remain inside its native Section, and
every pair of top-level Sections must have zero intersection. Native Sections
do not reflow with their children, so after moving, inserting, or deleting
content, refit each Section to the union of its owned children plus the
established inset, then rerun both checks.

On component-family pages, audit every canonical master against the file's live
component-documentation model. Families flow horizontally. Each family column
places the master or variant set first and one documentation instance directly
below it. Loose masters, bespoke prose blocks, and context-specific duplicates
fail this check even when the new components on the page are organized.

Variant-specific icons belong inside the component's shared Leading or Trailing
slot. Do not place a loose symbol beside an active slot.

Arrange component documentation in wrapping auto layout with the file's
configured maximum column count; apply this consistently across component
pages.

After adding or changing a shared component property, validate the default
across every variant and at least one existing instance. Specialized variants
override the common default locally; they do not redefine the component
family's default.

When a canonical row contract exposes an accessory slot, configure the
appropriate canonical accessory or record why the row intentionally omits it.
A default-hidden accessory is not evidence that its absence is intentional.

## Show source and ownership without polluting the product UI

Documentation should expose two independent metadata axes:

- **Source status:** Production, Proposed, or Reference.
- **Presentation owner:** Product, Platform, or External.

A production flow may contain a platform-owned or external surface, so ownership
must not be encoded as a peer of Production/Proposed. Show these values in the
documentation layer: a small badge, placeholder caption, or structured metadata
next to the screen name. Do not place the labels inside the device render. When
the external surface is not observable, show the handoff as documentation rather
than inventing its UI.

A source status is not established by a node name, component property, or written
report alone. In the final documentation render, verify that the screen title and
status pill are visible together and that the supporting note below the screen
states what is proposed, observed, or missing. If any of those elements is absent
or clipped, the status has not been communicated and the screen is not complete.

## Preserve reference evidence

Preserve competitive and comparative screenshots as source evidence rather than
redrawing them. Arrange the original captures in their observed interaction order,
connect only transitions supported by the evidence, and label an unavailable next
state as `Missing reference` instead of inferring it. Keep the source product,
platform, capture context, and observation date visible in the documentation
layer. Separate observed behavior from interpretation and from the adopting
product's proposed direction.

Conclude a comparative audit by mapping each relevant pattern to one of `Adopt`,
`Adapt`, `Reject`, or `Unresolved`, and identify the canonical component, flow, or
open product decision it affects. Reference screenshots remain evidence; they do
not become canonical product screens or component masters.

## Preserve surface ownership

The highest reusable container that defines a surface owns its background or
material. Descendant rows, scroll content, bars, and compositions inherit that
surface unless they intentionally introduce another one. Keep the documentation
canvas contrast separate from product surface tokens. Verify that shadows,
overlays, safe-area chrome, and parent clipping still render as intended.

Structural HStacks, VStacks, flow wrappers, and connector rails inherit their
parent surface and have no fill unless the wrapper intentionally represents a
product surface.

Model a fixed bottom region as a sibling overlay constrained to the bottom safe
area and independent of the scrollable body. Do not add spacer frames to push it
down and do not rely on clipping to hide overflow. Because the region
intentionally covers scrolling content, it may own an opaque semantic surface.
Safe-area chrome on that region uses the same resolved surface: primary,
secondary, or inverse according to the visible state.

## Reconcile design, product, and prototype

Keep three explicit ledgers during a production-grounded design pass:

- **Production changes:** behavior or navigation the design requires but the
  shipping app does not yet implement.
- **Design-system changes:** canonical components, variants, slots, assets, or
  tokens that must change before screens can be composed correctly.
- **Prototype topology:** destinations that can be shared, duplicated nodes to
  remove, overlay context, and external or platform-owned boundaries.

Resolve verified mismatches in their owning layer. Do not hide a code gap by
presenting a proposal as production, and do not preserve duplicate prototype
screens because they happen to live on different domain pages. Production
absence is not a ban on design: when product direction is intentional but not
implemented, design the proposed state beside the verified production evidence,
label its source and presentation owner, and add a deferred-gap record with an
owner, implementation trigger, expected code change, and closure evidence. At
an external or platform-owned boundary, design the product-owned handoff and
document the boundary instead of fabricating UI the product does not own.

## Verify slot content and compositing after canonical repairs

Clear a canonical slot's sample children before inserting replacement content.
After swapping a row accessory, verify the accessory host reserves the rendered
control's full width and height; inherited disclosure sizing can clip a switch.
After binding a semantic paint, read back its resolved alpha and render the
overlay over its backing screen. A valid token binding does not prove that the
scrim remains translucent. Add these failures to the task's deterministic checks
and re-render affected instances after repairing the owning component.
