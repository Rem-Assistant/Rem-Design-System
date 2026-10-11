# Competitive audit workflow

Use this workflow when screenshots or recordings from another product need to become a legible
reference flow in Figma. The output is evidence for comparison and product decisions. It is not a
visual spec, a canonical component source, or permission to adopt the source product's behavior.

## Organize one source app as one reference system

- Use one FigJam page named `Competitive Audits`. Screen-topology pages remain
  separate from this audit page.
- Create one Section per audited app, not per feature, product domain, or
  research question. Name it `<App> · Reference` and keep all of that app's
  evidence inside it.
- Treat screenshots as the primary artifact. Preserve each screenshot's aspect
  ratio, use FIT scaling, and keep it visibly unclipped.
- Keep prose off the board except for a compact source label and, when useful,
  one short product observation directly beneath the screenshot.
- Start from the source app's real root screen. For a long scrolling root, place the captured
  viewport slices together and label them as viewports of the same root rather than independent
  destinations.
- Nest destination stacks under the root in the same order as their source tap targets. Within a
  branch, preserve the observed navigation depth. Reuse one destination capture when several entry
  points reach the same screen; show the redundant routes instead of duplicating the destination.
- Keep screenshots contained inside the app Section and preserve visible system
  context unless a crop is explicitly needed to isolate a component. Do not
  leave loose captures on the page.

## Draw the observed topology

- Use connectors only for real within-app navigation or state transitions.
  Never connect screenshots merely because they are comparison references.
  When a connector is warranted, use simple horizontal and vertical segments
  that begin at the visible tap target and end at the destination screen edge.
- When one source fans out to three or more destinations, use the canonical branch spine rather than
  independent long connectors. Every spine exit keeps its visible numbered label beside the route;
  an unlabeled branch is incomplete even when its geometry is correct.
- Number root destinations in source-row order. Use nested suffixes such as `6a`, `6b`, and `6c`
  when one row expands into sibling destinations or one grouped concept has several children.
- Keep connector geometry between frames. Do not route lines across screen content, through device
  frames, or from arbitrary canvas coordinates.
- Draw connectors only for navigation or presentation changes. Record toggles and other direct
  actions as in-place states; classify browser, operating-system, or provider UI as an external or
  system handoff.
- If a root or intermediate source screen was not captured, do not invent it. Place the available
  branch in a labeled subtree such as `root source uncaptured` and wait for evidence before drawing
  an incoming route.

## Preserve evidence and product boundaries

- Treat chat attachments, imported image files, simulator captures, and phone-mirroring captures as
  valid inputs only after the pixels are accessible and the resulting images are visibly contained
  in Figma. Record the app, platform, capture source, and capture date when known; leave unknown
  metadata unknown.
- Distinguish observed screens, inferred relationships, and proposed product adaptations. Use clear
  labels such as `Reference`, `Inferred`, and `Proposed`; never silently turn an inferred destination
  into an observed one.
- Preserve reference-only behavior that matters to the comparison, but label it as not adopted when
  it falls outside the target product direction. A source app's SSH path, account model, or redundant
  IA can remain audit evidence without becoming a target-product requirement.
- Compare information architecture, control semantics, hierarchy, state coverage, and handoffs.
  Promote a pattern into the target design only through the normal component or flow workflow, where
  it receives target tokens, platform treatment, code authority, and product approval.

## Verify before reporting the audit complete

1. The page is named `Competitive Audits` and screen topology lives elsewhere.
2. Every audited app has exactly one Section, and every imported capture is
   contained inside that app Section.
3. Screenshots preserve aspect ratio, use FIT scaling, and remain unclipped.
4. Board prose is limited to compact source labels and at most one short product
   observation directly beneath a screenshot.
5. The root and its viewport slices are distinguishable from destinations, and
   destination order matches the source tap-target order.
6. Every connector represents a real within-app navigation or state transition;
   comparison-only references have no connector.
7. Missing source screens, inferred links, duplicate entry points, and external
   handoffs are explicit.
8. A rendered overview remains readable at canvas scale and close inspection
   confirms screenshot containment, scaling, and any real transition endpoints.
