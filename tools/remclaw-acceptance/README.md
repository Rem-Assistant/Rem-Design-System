# Protected staging acceptance proposal

Status: draft implementation on the hub-coordinated isolated branch
`codex/staging-acceptance-controller-20261011`. The no-secret PR checks are separate from
the inactive manual live workflow. No environment, secret, deployment, account, provider allowance,
manual workflow dispatch, merge or access change is part of this draft.
Reviewed reference: public Rem-Assistant/Rem-Design-System main
`1dd3a3d377961e97de63a9884a85c5dd23e0a2ff`. Existing release/Playground files remain unchanged.

## Purpose and trust boundary

Run unsigned RemClaw acceptance on a hosted macOS runner using the established public DS
orchestrator's repository identity, protected-environment validator, read-only private deploy-key
checkout and pinned GitHub SSH host. This adds only a manual workflow and an acceptance controller,
its tests and this document, plus a separate no-secret PR check and public XCTest fixture script. It does not call signing, upload, deployment or configuration tools.

The only candidate admitted is `1277d8132678bfa1a279b6da71f3e491c1cce35d` (RemClaw PR1533).
The checkout additionally verifies backend tree `f3c00642da42a6b0a534867612bbc8716811dbde`
and UI-test tree `c166a7fb05a17adfa6f1dd41fb650712f36ba3e6`. This acceptance-specific pin
allows a reviewed unmerged candidate without widening the release controller's branch policy.
The app is Debug, unsigned, staging URL only, built with Xcode26.3 and the frozen package lock.

Before any protected job starts, read-only admission requires the **pre-existing** dedicated
`remclaw-staging-acceptance` DS environment, Samuel as the sole required reviewer, administrator
bypass disabled, and only the `main` branch allowed. The existing validator determines whether
owner self-review is permitted or requires an independent initiator; this proposal changes neither.
A missing or incompatible environment fails admission. The job checks the protection again after
approval. The gate cannot prevent an administrator changing policy concurrently; owner control of
repository settings remains part of the trust model.

The reviewed app, build scripts, dependencies and UI-test runner execute on the same hosted runner
with its ordinary privileges. They are trusted with the account's test-login secret. Source and
control integrity checks catch accidental drift, **not malicious admitted code or lingering
privileged processes**. This is not a sandbox. No signing/store/Railway credential is referenced.
Only the private fetch step references `REMCLAW_DEPLOY_KEY`; only the test step references
`REM_ACCEPTANCE_TEST_ACCOUNT_ID` and `REM_ACCEPTANCE_TEST_LOGIN_SECRET` as TEST_RUNNER variables.
Owner must approve any credential placement in this dedicated environment; do not copy or expose
values in chat, logs, command lines, PRs or public workflow inputs.

All private git/xcode/simulator output, unsigned apps, typed-text activity logs, xcresult bundles,
and screenshots stay under one mode0700 RUNNER_TEMP child. Nothing is uploaded or cached. Final
cleanup deletes only that run-created simulator and directory. Interrupted runner destruction is
fallback cleanup, not confirmed revocation. Public output is fixed control/result vocabulary.
There is **no downloadable app, retained screenshot, or durable private runtime diagnostic** in
this minimal lane. A separately approved private evidence transport would be necessary for those
artifacts; public DS Actions artifacts must never be used as a substitute. Do not enable runner
or step debug logging for a credentialed run.

## Exact minimal adoption and execution sequence

1. Engineering hub confirms isolated DS branch ownership. Open one draft against DS `main` with
   only these six acceptance-specific added files. Run controller tests and normal DS review gates. Samuel/authorized
   integrator separately approves and merges that narrow change. Do not merge DS123/Build9 drafts
   or the broad inherited RemClaw PR1533 stack to expose a workflow.
2. Separately approve and configure the dedicated DS environment and secure credential handoff.
   It needs a read-only RemClaw checkout key and the synthetic login credentials only. Reusing an
   existing key still requires approval to make it available here; this proposal configures none.
   Missing setup deliberately blocks even build-only dispatch. No permissions are expanded by code.
3. Under the already-presented but **not yet granted** staging setup approval, the owner verifies
   project `f88617f9-a5eb-4f4a-bdb9-0f10ab427e9a`, staging environment
   `4b54fc6f-1198-46dd-8fc6-7b23d4365056`, backend service
   `bf0e5c69-e609-491c-ab60-16362f49c285`, domain
   `backend-staging-b87e.up.railway.app`. Confirm migration159/161 prerequisites; apply162/163;
   deploy the reviewed **exact1277** using an owner-approved exact-commit deployment path, then
   provision only one synthetic account and one synthetic Inbox task and its allowlist/login
   config in that service. The shared database means these are production-DB writes and need the
   explicit bounded approval. Provider allowance stays0. Personal accounts remain untouched.
   If the owner's deployment mechanism requires branch ancestry, stop for a narrow integration
   decision; do not substitute another SHA or merge the inherited stack wholesale.
4. Owner may approve `build-only` with calls0. This compiles the acceptance app but neither signs
   in nor boots a simulator. It is not runtime evidence and produces no distributable artifact.
5. A full `chat-inbox` run is a **later separate approval**: at most3 physical provider calls per
   synthetic account across grants in a rolling24h, fixed GMI endpoint/default model, tools and
   attachments disabled. This is a call/token bound, **not a dollar cap**. Do not set3 or dispatch
   from the initial provider0 setup approval. Verify account-wide unused allowance and all
   replicas' deployedSHA/config through the owner lane immediately before dispatch.
6. Submit the nonsecret operator attestation below and approve the exactSHA/mode/call allowance
   on the run. It expires after60minutes and is checked again before tests. The script never queries
   Railway or accepts a Railway token. Existing protected UI-test harness runs, and a private
   export must show every expected step passed (an empty or skipped suite cannot pass). Always-run
   cleanup signs out and requires the old bearer to receive401; failure fails the workflow.
7. Immediately after the run, owner reads Railway provenance again and records deployment ID,
   exactSHA and service/environment continuity along with public run ID/outcome. `/health` only
   says `ok`; it does not prove provenance. Until the before/after readbacks match, the automated
   result remains `test_passed_pending_runtime_readback`. If a rollout or concurrent change occurred,
   do not attribute the run to1277. Revoke the grant/disable the test login through the approved
   owner lane after testing; revocation is request-boundary invalidation, not provider quiescence.

A direct exact1277 staging deployment avoids a RemClaw merge dependency. If policy requires a
narrow cherry-pick/integration candidate instead, it has a NEW SHA and must be re-reviewed, compiled,
approved and repinned; this proposal cannot silently treat it as1277.

Operator attestation shape (metadata only; never put an account ID or secret here):

```json
{
  "sha": "1277d8132678bfa1a279b6da71f3e491c1cce35d",
  "project_id": "f88617f9-a5eb-4f4a-bdb9-0f10ab427e9a",
  "environment_id": "4b54fc6f-1198-46dd-8fc6-7b23d4365056",
  "service_id": "bf0e5c69-e609-491c-ab60-16362f49c285",
  "deployment_id": "REPLACE_WITH_VERIFIED_DEPLOYMENT_UUID",
  "checked_at": "REPLACE_WITH_CURRENT_UTC_ISO_TIMESTAMP",
  "provider_calls": 3
}
```

The attestation is an operator statement, not cryptographic or automated live proof. Invalid,
future, expired, wrong-SHA/service or extra-field metadata is refused. The caller cannot turn
provider0 into live approval by changing this input; the protected owner's approval is required.

## Evidence boundaries and remaining independent work

At1277: hosted backend216mock tests +68ephemeral PostgreSQL contracts passed; Debug iOS/macOS,
acceptance scheme and unsigned Release iOS/macOS passed. Swift8 composer +9 mock revocation
checks passed. Those are compile/mock/contract results, not signed-in staging E2E.

The pinned live harness covers Chat/Inbox navigation, ordinary send/stream/stop, choosing Automatic
in the model menu, opening Inbox task detail and returning, signout/invalidation and classic fallback. It does **not** yet prove
task-chat send, retry/resend idempotency, model persistence after relaunch, or attachments. Attachments are expressly
rejected for the synthetic delegated runtime. Extending those requires a separately reviewed harness
candidate and, for attachments, an approved scope change; no bypass or personal account is allowed.
Android PR1522 remains separate:12/12 mock emulator passes and133/134 unit tests, with the
stop-before-final resend-idempotency timeout and approval-cache/pagination findings still open.

Next independent work after draft publication: finish the DS normal CI/review gates, and plan a new harness revision for retry/model-persistence without
mislabeling this frozen candidate. Live screenshot review also needs approved private retention.
No local native build or simulator has been used; last known disk space is below the6GiB floor.

Run local controller tests (pure/mocked commands; no native build or provider calls):

```sh
python3 -B -m unittest discover -s tools/remclaw-acceptance -p 'test_*.py' -v
```

The new execution-receipt parser has only been tested with constructed fixtures locally. Its
Xcode26.3 xcresult CLI/schema compatibility still needs a hosted smoke test before credentialed
use. Unknown schema, missing/skipped/duplicate methods or failed statuses refuse success.
Independent scoped review found no remaining blocking code issue after this false-pass fix.


The separate `RemClaw acceptance controller checks` PR workflow uses read-only permissions and
standard public runners. It does not reference an environment or any secret. Its macOS job builds
and runs a temporary, public Swift-package XCTest fixture (not RemClaw), requiring actual passing
method receipts to be accepted and an exit-zero skipped method to be rejected. No simulator is
created. This proves parser compatibility only; it is not signed-in app, staging, or UI coverage.
The fixture's local entry point refuses execution unless running in hosted macOS CI. Its public
synthetic build diagnostics may be printed on failure; no private source or credentials are present.
