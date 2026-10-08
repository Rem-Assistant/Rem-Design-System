# Agenda REST zero-default transport repair

Task: https://github.com/Rem-Assistant/Rem-Design-System/issues/91

## Outcome

Repair only the Figma REST evidence transport interpretation of the documented zero defaults for paddingBottom and itemSpacing. This is a separate control-plane repair for Agenda source verification, not product behavior or screenshot naming. Keep this task separate from issue #89 / PR #90.

## Execution and review boundary

- Use the existing GitHub Factory agent-builder.yml route, dispatched on codex/playground-expansion with only issue and base_ref=codex/playground-expansion.
- Trusted starting base: 3ecb282102a7da43daa789a7830e81cafa973113. Existing Factory pin: 8522d84d9cd429c9b8cb9d028cf665a6136403c0.
- Seed one draft PR from agent-factory/playground-issue-<issue> targeting codex/playground-expansion. Seed only docs/contracts/agenda-rest-zero-defaults.md.
- Implement only tools/design-sync/verify-playground-structure.mjs and focused corresponding tests in tools/design-sync/test-playground-structure.mjs. The task contract document may record scope and evidence.
- No .github/workflows edits, config changes, new infrastructure/access/secrets, Figma writes, Settings/Voice/Agenda app changes, tools/render-evidence validation changes, merges, deploys, force pushes, or trusted-base promotion.
- Keep PR draft. Independent source/test review must confirm exact scope before proposing any additive port to the trusted expansion base. Do not change PR #86 or #87.

## Observed failure and corroborating source

The correct manual design-drift route at the trusted base failed run https://github.com/Rem-Assistant/Rem-Design-System/actions/runs/37805203671/job/113407575755. Its bounded diagnostics show existing nodes, absent own property, undefined type:
- paddingBottom on SLOT 2049:10103, 2049:10127, 2052:11463, 2191:12585
- itemSpacing on FRAME 2049:10094, 2049:10116, 2049:10323, 2191:12575
- All eight nodes explicitly have layoutMode VERTICAL.

An independent live Figma Plugin API read at 2026-10-08T16:57:34Z in file af4yDqCzp57jds9lkFiIaO, page 1910:40765, confirmed every exact property exists, is a number, and equals 0. No nodes were mutated. All four SLOT names are Suggestions and their parent FRAME names are VStack/Agenda rows; the pairs in the lists above are in matching order. This attestation corroborates the affected source and is not a replacement for new authenticated current-head REST evidence.

Authoritative sources:
- https://developers.figma.com/docs/rest-api/file-node-types/ declares paddingBottom and itemSpacing numeric default 0 for auto-layout frames.
- https://github.com/figma/rest-api-spec/blob/main/openapi/openapi.yaml defines both as optional default-0 HasFramePropertiesTrait properties. It does not separately enumerate SLOT.
- https://developers.figma.com/docs/plugins/api/SlotNode/ describes Slot as a child frame; https://developers.figma.com/docs/plugins/api/node-properties/ lists SlotNode as supporting both properties. Describe this cross-reference honestly with the independent exact-node Plugin attestation. Do not claim broad absent-to-zero semantics.

## Required implementation

Normalize ONLY an ABSENT own paddingBottom or itemSpacing property to schema default 0, ONLY on an EXISTING node whose ID, name, and type match the trusted expected identity and whose type is FRAME or SLOT and explicit layoutMode is HORIZONTAL or VERTICAL. Apply this only for the trusted corresponding numeric assertion expecting zero. Do not mutate the fetched nodes or contract.

Record EVERY applied default in the returned evidence report: node ID, field, raw absence/presence state, applied numeric 0, actual node type/layout mode, and authoritative schema basis (including SLOT cross-reference where applicable). Preserve bounded raw-field failure diagnostics and make audit data visible through createPlaygroundStructureReport. Normalization must never make failed identity or ancestry appear successful.

Preserve all original numeric assertions, source IDs/names/types, ancestry, top padding 24, first-child local coordinates, source-contract digest and all workflow/head/media guards. Fail closed for missing node or identity, unknown type, unknown/missing/NONE/GRID layout, all other missing fields, explicit undefined/null/string/boolean/NaN/Infinity, and nonzero values where zero is required. Preserve existing valid explicit numbers and original expected comparisons; do not replace arbitrary missing values with zero.

## Required tests and deliverable

Focused positive tests: each allowed field and FRAME/SLOT with HORIZONTAL/VERTICAL, audit entry content, no input/contract mutation, explicit 0 is accepted without a default log, and report serialization includes the audit data.

Focused negative tests: absent node; wrong ID/name/type; known disallowed and unknown types; missing, NONE, GRID, unknown layout; explicit undefined/null/string/boolean/NaN/Infinity/-Infinity/nonzero; zero-default request for a nonzero expectation; other missing numeric field; top-padding drift and missing/changed first-child coordinates. Confirm raw diagnostics remain bounded. Retain all existing structural tests and GET-only/error behavior.

Run node --test tools/design-sync/test-playground-structure.mjs, applicable design-sync tests and existing evidence-validator tests, plus diff checks. Report exact commands, outcomes and immutable run/head links. Do not relabel fixture-only tests as authenticated live evidence.

Return exact commit, changed-path list, focused/broader test outcomes, complete default-normalization semantics/audit schema, and independent review prerequisites. A subsequent reviewed additive control-plane port will require refreshed PR #86/#87 Figma proofs.
