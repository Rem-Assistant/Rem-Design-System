# Figma is part of screen delivery

The Director clarified on 2026-09-28 that a finished screen PR includes Figma changes.
Consent #30 is the first complete-loop proving case. The earlier consent exclusion is
superseded; approved arrangement, copy, icon/FILL, and platform rules remain authoritative.
Other screens are not dispatched until this loop is proven.

## Required result

Follow `SHAPE-OF-A-TASK.md`, `FILE-ORG.md`, and the repository's `rem-design-system` skill:

- Update editable native screens and every required state in the existing Rem file,
  `af4yDqCzp57jds9lkFiIaO`. Use canonical component instances, shared variables, and text styles.
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

## Consent proving-case delivery (2026-09-28)

Consent #30 uses an authenticated Codex Builder runtime for the canvas write while Steward keeps
the durable task, source commit, result, review, and recovery record. The existing Rem file is
updated in place: page `609:2` is `Onboarding · Consent`, and its only top-level section is the
detached `Mobile Flow (Detach This)` template at `695:138`.

The Builder must preserve the template hierarchy: `Placeholder Sections` → `Placeholder Section`
→ `Placeholder Rows` → one or more `Placeholder Flows` rows → `Mobile Placeholder` → screen. The
screen directly replaces the slot area inside each placeholder; nesting it in a leftover slot wrapper
or pixel-aligning a loose screen above the placeholder is invalid even when the canvas looks correct.
Consent `609:3`, Terms `638:28`, the Privacy branch consent frame `695:585`, Privacy `638:65`, loading
`700:109`, and submit failure `700:147` now follow that rule. Loading and submit failure use a second
`Placeholder Flows` row inside the same flow section. The separate consent-action component section
was removed.

The hosted GitHub runner remains the repository, export, and comparison worker. Its `FIGMA_TOKEN`
is consumed only by the current-head export/drift job; a token does not provide interactive canvas
authoring. Code Connect and Dev Resources remain separate delivery concerns: Code Connect maps
Figma components to code examples, while a Dev Resource may link any relevant implementation or
documentation. Neither substitutes for an editable canvas update.

Figma supports native canvas writes through its remote MCP server in supported clients, including
Codex. It requires a Full seat, file edit access, and an authenticated connection on that Builder
runtime:

- https://developers.figma.com/docs/figma-mcp-server/write-to-canvas/
- https://developers.figma.com/docs/figma-mcp-server/remote-server-installation/

The PR still needs exact-head Figma exports, a structure/property record, and Reviewer approval.
A successful canvas edit alone is not complete delivery.
