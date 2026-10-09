# Rem Factory routing: prepared, not activated

This change targets the default controller branch. It does not merge, run Builder,
write Figma, publish an app distribution, or change the separate private Playground
distribution workflow. Existing local checkouts and PR71/85 branches are untouched.

The trusted default controller selects `codex/playground-expansion` through the
existing `builder.base_branch`; a manual `base_ref` may explicitly select `main`
or that Playground branch. Further branches need a reviewed allowlist change.
New routed task heads use `agent-factory/playground-issue-<issue>` and stay draft.
Operator selection requires `samuelalake` plus current repository write authority.
Controller code/configuration and source SHA are separate; issue/PR receipts bind
Steward, Builder, review and publication to the same task route.

Figma stays off in controller policy (`figma.enabled: false`,
`builder.figma_mcp: false`) and Builder receives no Figma OAuth bundle. Skills are
retained. The automatic integration job is removed, and Builder failure escalation
is suppressed. Existing OpenRouter fallback settings are unchanged. The publisher
only consumes already-produced reference evidence; it does not contact Figma or
receive its secret. It refuses incomplete/mismatched native or reference evidence.

## Reconciled work

- [Factory PR107](https://github.com/samuelalake/agent-factory/pull/107), head
  `8522d84d9cd429c9b8cb9d028cf665a6136403c0`, is an open draft adding optional
  failure-escalation suppression. That small patch and its test are reused in the
  accompanying core correction; the old unmerged pin is not promoted.
- [Rem PR71](https://github.com/Rem-Assistant/Rem-Design-System/pull/71), head
  `61ad6b9d4fbbd6e422b22d2c805e894b85049f14`, targets
  `codex/settings-integration`, with code-only Settings dispatch, Figma-off,
  suppressed failure escalation and owner-controlled integration.
- [Rem PR85](https://github.com/Rem-Assistant/Rem-Design-System/pull/85), observed
  head `3ecb282102a7da43daa789a7830e81cafa973113`, targets
  `codex/settings-review-evidence`, extending the Settings lane to Playground.
- The evidence formatter, contracts, artifact validators and tests are reused from
  candidate `90b43871fc4454c4cd72b704345356521610d338` (which contains the PR71/85
  lane). Its native product changes are not copied. Publisher scope/base/head and
  exact-run checks are retained. The default publisher now executes formatter code
  from its controller SHA, not a target/PR checkout. Only main and the explicit
  Playground scope/head pair are admitted here; Settings activation remains in its
  existing separate lane. The imported workflow-gate test is adapted to the
  publisher because the source branch's Figma workflow is not promoted.

## Activation prerequisites

1. Review and merge the Factory core correction through normal repository gates.
   The consumer draft intentionally pins the accompanying unmerged core commit for
   review; before activation, align all caller/publisher pins to the accepted
   merged Factory revision. A draft pin is not a released version.
2. Review and merge this consumer controller correction into `main`. The currently
   live default callers/publisher remain unchanged until then. No admin bypass.
3. Reconcile the reviewed caller/control-plane files and pin on the admitted
   Playground base before new tasks. PR85 still has its old isolated callers and
   Factory PR107 pin. Do not assume a main merge updates those branch workflows.
   Confirm the branch's native and reference evidence producers match the exact
   trusted workflow digests; this PR does not promote its Figma runner/skills.
4. Existing PR85/candidate child PRs have no new route/head receipts. They may use
   the explicit legacy Playground publication admission, but Builder must not
   execute them as newly authenticated routed tasks. Preserve their evidence;
   coordinate migration/new task issues instead of forging receipts.
5. Confirm read-only route preflight on the activated controller, then ask Samuel
   before one bounded live Steward/Builder dispatch. Verify its controller SHA,
   selected base/source SHA, candidate receipt, Figma-off behavior and exact-head
   review. No such dispatch has occurred in this preparation.
6. Ask before live evidence replay/publication. A valid existing screenshots run can
   be selected with `publish-builder-delivery.yml` on **main**, using
   `evidence_run_id`. Manual publication requires the configured operator and write
   authority. Verify the PR's canonical Delivery contains that exact candidate SHA,
   native attachments and authenticated provenance. A passed test/draft PR does
   not prove this live receipt exists.

Only after these live checks can the original Claude session be told the default
route is fixed. This task does not message Claude. A missing exact-head artifact,
changed scope/head, branch rewrite, untrusted workflow digest, or missing activation
step is a blocker, not permission to weaken validation.
