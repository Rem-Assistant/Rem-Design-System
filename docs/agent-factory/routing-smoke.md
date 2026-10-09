# Factory routing smoke task: how branch selection and delivery are verified

This explains the validated non-`main` routing and delivery-verification contract the
Factory already implements. It is the *explanation* produced by one bounded smoke task,
not a claim that live proof has passed — read it as a human inspection checklist for the
identities, authorities, and receipts that confirm a task was routed and delivered
correctly. The smoke task is documentation-only (no executable, contract, workflow,
secret, model, or distribution change), so it exercises the route without touching a
product surface.

## Branch-selection precedence

The controller selects a Builder base branch in a fixed order, stopping at the first
that applies:

1. **Explicit authorized `base_ref`** — a manually supplied `base_ref` wins, but only if
   it names an allowlisted branch (today `main` or `codex/playground-expansion`).
2. **An existing authenticated task route** — a route already recorded by the App for the
   issue's authenticated task is reused.
3. **The repository `builder.base_branch` default** — applied when neither above does.

This smoke task resolves to `codex/playground-expansion` via an explicit authorized
`base_ref`, never by inventing a new branch. Other branches need a reviewed allowlist
change first.

## Three distinct SHAs — never substituted, never invented

The route binds three separate identities; a reviewer must keep them apart:

- **`source_sha`** fixes the *target code* the Builder checks out and builds.
- **`controller_sha`** fixes the *trusted policy* (controller configuration and callers)
  governing the dispatch.
- **Candidate HEAD** identifies the *Builder result* — the exact commit on the draft PR.

These are not interchangeable and may not be guessed or hand-filled. A receipt that
reuses one SHA where another belongs, or carries an invented value, fails closed.

## Authorities and immutable receipts

- **Allowlisted existing branches only** — routing targets a branch already on the
  allowlist; it never creates or rewrites one.
- **Operator + repository write authority** — a manual dispatch or publication requires
  the configured operator together with current write authority here; neither alone.
- **Immutable App receipts** — the App records the route, Builder checkout/source, draft
  PR base/head, and candidate receipt, read as emitted. A mismatched, edited, or forged
  receipt (Steward, Builder, publisher, or Reviewer) is rejected, not reconciled.

## Figma is off for this task

Figma writing and the Builder Figma MCP are disabled (`figma.enabled: false`,
`builder.figma_mcp: false`); Builder gets no Figma OAuth bundle and performs no Figma
call, export, or write. The design skills remain available for future refinement, but no
Figma work is executed here. The `delivery-scope:factory-routing` scope requires only this
document plus a successful exact-head screenshots run; it cannot attest native or Figma
design acceptance.

## Routing and delivery steps a human should inspect

1. Confirm the dispatch ran once, on the trusted controller, selecting
   `codex/playground-expansion` by explicit authorized `base_ref`.
2. Read the unedited App route receipt before the Builder label; confirm the recorded
   `source_sha` and `controller_sha` are the expected distinct values.
3. Confirm Builder's checkout/source, the draft PR base (`codex/playground-expansion`,
   not `main`) and head, and the unedited candidate receipt.
4. Confirm Figma-off behavior: no OAuth bundle, no export, no canvas mutation.
5. Let exact-head native CI (the screenshots run) complete against the actual candidate
   SHA, not a merge ref, and confirm the manifest records that HEAD.
6. Verify the canonical Builder Delivery head/source receipt agreement, the formal
   Reviewer result, and the review-completion gate — none forged or manually edited.

This task's draft PR is not merged, enables no automatic integration, and enqueues no
further work. It is the proof vehicle for the route above, not evidence that the route
has already been proven live.
