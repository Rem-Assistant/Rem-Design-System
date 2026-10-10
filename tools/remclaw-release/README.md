# RemClaw iOS release through the trusted orchestrator (draft)

`.github/workflows/remclaw-ios-release.yml` builds the **private** `Rem-Assistant/RemClaw`
iOS app (`com.remapp.rem`, with its embedded widget `com.remapp.rem.widget`) at one exact
commit on a fresh GitHub-hosted runner and uploads it to TestFlight. It follows the
approved Decision 6 plan in
[#104](https://github.com/Rem-Assistant/Rem-Design-System/issues/104#issuecomment-6103399933)
and reuses the Playground distribution machinery in `tools/playground-distribution`
(run-identity and environment gate, storage guard, locked Fastlane bundle, allowlisted
summary and cleanup patterns).

**Nothing runs yet.** The workflow cannot fetch RemClaw, sign or upload until the
`remclaw-ios-release` environment, the RemClaw deploy key and the environment secrets
below exist. Each of those is a separate, action-time human approval. This change creates
none of them and dispatches nothing.

## Contract

Dispatch from **`main`** only, with one input:

| Input | Required value |
| --- | --- |
| `remclaw_sha` | Full lowercase 40-character RemClaw commit that is already in the history of an allowed release branch |

Allowed RemClaw release branches are the explicit constant
`ALLOWED_RELEASE_BRANCHES = ('main', 'skip-sent-reply-suggestions')` in `release.py`.
Changing the list is a reviewed code change; the code also refuses any other list at run time.

### Jobs

1. **`admission`** (`ubuntu-24.04`, no secrets, no environment, read-only token):
   runs this directory's and the Playground guard tests, then `release.py admit`:
   repository, `refs/heads/main` and `workflow_dispatch` identity (the Playground gate's
   rule), `^[0-9a-f]{40}$` for `remclaw_sha`, and the **pre-existing** protection of
   `remclaw-ios-release` via the read-only API (`gate.validate_environment`). An absent
   or weak environment fails here, before the release job would make GitHub create an
   unprotected one.
2. **`release`** (`macos-15`, `environment: remclaw-ios-release`, after the reviewer approves):
   1. Re-verifies the environment protection with the gate (same check as admission).
   2. Fetches RemClaw with the read-only deploy key (`release.py fetch`): only the allowed
      branches, full history, no tags, pinned GitHub Ed25519 host key, strict host-key
      checking, key file `0600` and deleted in a `finally`. Then asserts `HEAD == remclaw_sha`,
      a clean tree, and `git merge-base --is-ancestor <sha> origin/<branch>` for an allowed branch.
   3. Selects the newest stable Xcode 26+ on the image (RemClaw targets iOS 26.0).
   4. Storage guard (`storage.py check --platform ios`) before dependencies, build, sign and upload.
   5. Frozen locked Fastlane bundle (`tools/playground-distribution/Gemfile.lock`, reused via `BUNDLE_GEMFILE`).
   6. Reserves the next build number: reads every existing build of the App Store Connect
      app whose bundle ID is exactly `com.remapp.rem` and takes max + 1.
   7. Builds an **unsigned** Release archive of `RemClaw.xcodeproj`, scheme `RemClaw`,
      with only `CURRENT_PROJECT_VERSION` overridden and `-onlyUsePackageVersionsFromResolvedFile`.
      Checks the app and widget bundle IDs and versions, and that tracked source is unchanged.
   8. Signs by exporting with manual App Store signing (team `R6A892D599`) in a temporary
      keychain, then verifies the IPA: one app plus exactly the widget, `codesign --verify`,
      embedded profile UUIDs, and signed entitlements (`aps-environment=production`, Sign in
      with Apple, app group `group.com.remapp.rem`, `get-task-allow=false`).
   9. Uploads with `upload_to_testflight` (`skip_submission`, no external distribution or
      notifications, no expiry of earlier builds), waits for processing and reads back the
      build's `processingState`. The run succeeds only on `VALID`.
   10. Always writes the allowlisted summary, then always cleans up.

## Confidentiality (public repository, private source)

This repository's Actions logs are world-readable. The guarantees:

- **Tool output never reaches the log.** `git`, `xcodebuild`, `security`, `codesign`,
  `ditto`, `bundle` and `fastlane` write only to files under `$RUNNER_TEMP/remclaw-private`
  (`fetch.log`, `bundle.log`, `reserve.log`, `build.log`, `sign.log`, `upload.log`). They are
  never printed with `cat`, `tail` or similar, never uploaded, and are deleted by cleanup.
- **Failures print one fixed line.** `classify_failure.py` maps a private log to
  `classification=<CODE>` plus integer counts. CODE is one of `DEPENDENCY`, `PROVISIONING`,
  `SIGNING`, `COMPILE`, `AUTH`, `UPLOAD` or `UNKNOWN`. Trusted-control rejections (input,
  ancestry, environment, metadata) are `POLICY`. No log line, path, symbol or file name is
  ever echoed. Tests prove this against fixture logs seeded with marker strings.
- **Script messages are fixed strings.** `release.py` prints only its own messages and
  validated identifiers. Unexpected exceptions become a generic message; their text
  (which could contain paths or plist content) is never shown.
- **Not `actions/checkout` for RemClaw.** It logs the checked-out commit's author, email
  and message and every fetched ref. RemClaw is fetched by `release.py fetch` instead,
  with all git output redirected.
- **No artifacts, no caches, no tracing.** There is no `actions/upload-artifact`, no
  `actions/cache`, no `set -x`. The binary goes only to App Store Connect. DerivedData,
  SwiftPM checkouts, gems and the RemClaw checkout live under `$RUNNER_TEMP` and are removed.
- **Allowlisted summary.** The step summary holds only `remclaw_sha`, `build_number`,
  `release_branch` (from the constant), `processing_state` (fixed set), the approval model
  and reviewer, and `failure_class` (fixed set). Anything else is dropped; invalid values
  suppress the whole record.
- **Untrusted code paths.** Forks and PRs cannot reach it: `workflow_dispatch` from `main`
  only, an environment limited to `main`, environment-scoped secrets and a required reviewer.

Residual exposure, accepted by the plan: the RemClaw SHA, build number and approval
identity are public, as are this repository's control scripts. The run's timing and
pass/fail are visible. Building trusts the reviewed RemClaw source. Building before
credentials are injected is **not** a sandbox for hostile code (see the Playground README).

## Environment protection (`remclaw-ios-release`)

Same model as `playground-ios`, re-verified at run time by `gate.validate_environment`:

- Required reviewers: exactly one User. Either the **owner-approval** model (Samuel
  `samuelalake`, ID `58840187`, as sole reviewer with **Prevent self-review off**), or the
  **independent** model (Prevent self-review on, one existing verified reviewer who did not
  dispatch or re-run the run).
- **Allow administrators to bypass** off.
- Deployment branches: selected branches, exactly one branch policy named `main` (no tags or patterns).

## Secrets and variables (environment-scoped; documented, not created)

| Kind | Name | Meaning |
| --- | --- | --- |
| secret | `REMCLAW_DEPLOY_KEY` | Private half (OpenSSH) of a **read-only** deploy key on `Rem-Assistant/RemClaw` |
| secret | `PLAYGROUND_ASC_KEY_ID` | App Store Connect API key ID (same names as the Playground lane) |
| secret | `PLAYGROUND_ASC_ISSUER_ID` | Issuer ID for a team key; omit for an individual key |
| secret | `PLAYGROUND_ASC_KEY_BASE64` | Base64 of the `.p8` key |
| secret | `REMCLAW_IOS_CERTIFICATE_BASE64` | Base64 P12 of the Apple Distribution certificate and private key (the Playground certificate may be reused) |
| secret | `REMCLAW_IOS_CERTIFICATE_PASSWORD` | P12 password |
| secret | `REMCLAW_IOS_PROFILE_BASE64` | Base64 App Store profile for `R6A892D599.com.remapp.rem` (push, Sign in with Apple, app group) |
| secret | `REMCLAW_IOS_WIDGET_PROFILE_BASE64` | Base64 App Store profile for `R6A892D599.com.remapp.rem.widget` (app group) |
| variable | `REMCLAW_IOS_CERTIFICATE_SHA1` | SHA-1 fingerprint of that distribution certificate |

The ASC key must be able to read the app's builds and upload builds for `com.remapp.rem`.
A team key covers every app in the team; disclose that scope before installing it.
The widget profile is required because the RemClaw app embeds `RemClawWidget.appex`.

## Human steps (each needs its own action-time approval)

1. Merge this PR through the normal review lane. Merging grants nothing.
2. Generate a read-only deploy key and add its **public** half to RemClaw → Settings →
   Deploy keys, with **Allow write access unchecked**. Revoke it there at any time.
3. Create the `remclaw-ios-release` environment in this repository with the protection above.
4. Samuel enters the secrets and the variable above directly in GitHub's environment
   secret UI. Never paste them into issues, chat or workflow inputs.
5. Create the App Store profiles for `com.remapp.rem` and `com.remapp.rem.widget` if they
   do not exist (separate Apple account action).
6. Dispatch for a specific reviewed SHA, then approve the environment deployment.

## Failure and recovery

Releases serialize and are never cancelled mid-upload. `upload_started`, `PROCESSING` or
`unresolved` is **not** proof of failure: inspect App Store Connect for that build number
before any retry. The upload lane refuses to run if the reserved number is no longer the
highest. To stop future releases, delete the deploy key or disable the workflow.

Known risks for the first authorized pilot (none are verifiable without the private
credentials): whether an unsigned archive exports with the full entitlements (the signed
entitlement check fails closed if not), whether `Rem-Assistant/ElevenLabsKit` resolves
without credentials (it is a SwiftPM dependency; a private package would fail as
`DEPENDENCY` and need its own read grant), Xcode 26 availability on `macos-15` (selection
fails closed), storage thresholds sized for the smaller Playground app, and export-compliance
answers in App Store Connect (`ITSAppUsesNonExemptEncryption` is not set in RemClaw).

## Validation

```sh
python3 -m unittest discover -s tools/remclaw-release -p 'test_*.py' -v
python3 -m unittest discover -s tools/playground-distribution -p 'test_*.py'
ruby tools/remclaw-release/test_workflow.rb
ruby tools/remclaw-release/test_fastfile.rb
python3 tools/playground-distribution/lint_workflows.py   # checksum-pinned Actionlint 1.7.12
```

`remclaw-release-check.yml` runs these on `ubuntu-24.04` and `macos-15` for pull requests,
plus a frozen install of the locked Fastlane bundle and `fastlane lanes` against the RemClaw
Fastfile. It has a read-only token, no environment and no secrets.
