# Figma is part of screen delivery

The Director clarified on 2026-09-28 that a finished screen PR includes Figma changes.
Consent #30 is the first complete-loop proving case. The earlier consent exclusion is
superseded; approved arrangement, copy, icon/FILL, and platform rules remain authoritative.
Other screens are not dispatched until this loop is proven.

## Required result

Follow `SHAPE-OF-A-TASK.md`, `FILE-ORG.md`, and the repository's `rem-design-system` skill:

- Update editable native screens and every required state in the existing Rem file,
  `af4yDqCzp57jds9lkFiIaO`. Use canonical component instances, shared variables, and text styles.
- For consent, the required authored/evidence destinations are Default, Terms, and Privacy in Light.
  Dark remains reviewable through the shared color-variable mode; loading and error are not shipping
  consent-local states and must not be fabricated.
- Preserve each platform's intentional native treatment while matching the approved
  arrangement, copy, emphasis, glyph meaning, and FILL. Document the platform/mode of each frame.
- Update the consent flow and its legal-sheet interactions so Present works. Reuse the
  existing flow instead of creating another disconnected generation.
- For added or changed components, deliver master/variants and a Preview tile; update
  `REGISTRY.md`, the Figma Component Index, and `FIDELITY.md` with actual verification results.
  The annotated Anatomy/full specification remains the separate manual step already
  excluded by `SHAPE-OF-A-TASK.md`.
- Include screen, flow, and component links plus a reused/new component ledger in the PR.
  Keep Code Connect mappings current where supported. If publication is unavailable,
  accurately record source bindings and the limitation; Code Connect is not a substitute
  for updating the designs.

## Evidence and acceptance

The delivery record must associate the current implementation commit with the Figma file,
node IDs, platform/mode/state, capture time, and file revision or canonical structure
digest. Preserve exported images and a structure/property record showing editability,
component instances, token/style bindings, and prototype connections. Do not accept a
bare mutable URL or a code screenshot placed on the canvas as Figma delivery.

Reviewer compares that evidence against both platform renders and the approved screen
contract. Screenshots establish appearance; interaction recordings or verified prototype
actions establish transitions. A new implementation revision invalidates the prior
association until reverified; a changed Figma node invalidates its prior evidence.
Missing/stale evidence or an observed mismatch blocks screen approval. Green builds and
the platform-only delivery status do not establish complete delivery.

Steward owns dispatch, durable progress, bounded retries, and recovery. Builder owns
authoring; Reviewer owns independent verification; the Director owns direction and final
approval. A worker may execute on an authenticated machine, but its task, source commit,
result, and recovery state must remain in the Factory record. A maintainer manually
repairing the screen is not proof that this handoff works autonomously.

## Verified execution gap (2026-09-28)

The connected Codex Figma tool can read the target file. Inspection found the existing
Onboarding page (`410:15`) and consent flow page (`609:2`), with consent (`609:3`), Terms
(`638:28`), and Privacy (`638:65`) frames. These are starting references, not verified
current-head deliverables; the required state set has not been proven complete.

The hosted Factory Builder currently exposes repository tools only and has no configured
Figma authoring connection. `figma-publish.yml` is a manual Code Connect workflow, not a
canvas authoring worker. `design-drift.yml` is a template and is not proof of this gate.

Figma supports native canvas writes through its remote MCP server in supported clients,
including Claude Code and Codex. It requires a Full seat, file edit access, and an
authenticated connection on the worker. The connection in a maintainer's chat is not
automatically available to a GitHub runner:

- https://developers.figma.com/docs/figma-mcp-server/write-to-canvas/
- https://developers.figma.com/docs/figma-mcp-server/remote-server-installation/

The remaining implementation must connect such a worker to Steward's durable task/result
handoff and publish independently verifiable Figma evidence. Until then, report a named
capability blocker; do not omit Figma, call the screen finished, or spend repeated screen
revision attempts expecting repository-only tools to edit Figma. No Figma update or
autonomous Figma handoff is claimed by this document.
