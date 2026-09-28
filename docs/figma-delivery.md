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
updated in place on the shared `Onboarding` page (`410:15`). `00 · Canonical screen inventory`
(`760:21`) contains the existing sign-in/privacy masters; `01A · Consent · Documentation`
(`695:138`) and `01B · Consent · Prototype` (`731:260`) are the consent pair. Later flows add
`02A` / `02B` pairs on the same page rather than creating more onboarding pages.

The inventory Section uses `#F5F5F5` so its white device frames remain legible on the canvas.
Inside those devices, the Sign-in screen uses the actual repository assets (`RemAppIcon` raster and
the four-color Google SVG), with no placeholder text glyphs. Privacy's legal links are an instance
of canonical `Section` (`741:311`), not a hand-built grouped frame. These are structure-contract
requirements, so a later Builder run that restores the white inventory, placeholder marks, or loose
privacy rows fails before review.

The Builder must preserve the template hierarchy: `Placeholder Sections` → `Placeholder Section`
→ `Placeholder Rows` → one or more `Placeholder Flows` rows → `Mobile Placeholder` → screen. The
screen directly replaces the slot area inside each placeholder; nesting it in a leftover slot wrapper
or pixel-aligning a loose screen above the placeholder is invalid even when the canvas looks correct.
Consent `609:3`, Terms `638:28`, the Privacy branch consent frame `695:585`, and Privacy `638:65`
follow that rule in one sequential `Placeholder Flows` row. Current shipping code has no
consent-local loading or submit-failure state, so the previous speculative state row was removed.

Sequential rows retain arrow vectors with the template's 24-point gap. On component-family pages,
`Component container` (`663:2270`) is a vertical auto-layout documentation surface: the canonical
component or variant set first, the lightweight overview panel second. Full Specs-plugin output
is optional follow-up material rather than a Builder fidelity target.

Nested documentation screens cannot be prototype navigation destinations in Figma. The separate
`01B · Consent · Prototype` therefore owns a labeled strip of top-level 402×874 frames sourced
from the documented screens. Consent links to Terms and Privacy; both legal sheets return with Back;
Consent is the one registered Presentation starting point. Accept remains unconnected because the
old Deploying destination is being deprecated and no replacement step is approved yet.

The consent flow introduces canonical `Section` (`741:311`) on Rows & Controls. It exposes optional
Header/Footer properties plus an editable Rows slot restricted by preference to `ListRow`; the grouped
surface uses `backgroundSecondary` and the xlarge radius with no outer stroke. `ListRow` and `Section`
each use the same canonical documentation container style inside `Rows & Sections · Component
documentation` (`741:309`). Placeholder Anatomy panels are omitted because the Specs plugin can
generate them later. Existing long-form ListRow documentation and generated Specs output are
preserved in a separate reference section, not treated as the Builder's required output. The older
loose Section template and long-list workaround were removed from `Accessories`.

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
