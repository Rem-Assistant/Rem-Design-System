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
| secret | `REMCLAW_ASC_KEY_ID` | App Store Connect API key ID for com.remapp.rem. Its own key, not the Playground-only key: either a separately approved identity scoped to the Rem app, or an explicitly chosen key covering both apps |
| secret | `REMCLAW_ASC_ISSUER_ID` | Issuer ID for a team key; omit for an individual key |
| secret | `REMCLAW_ASC_KEY_BASE64` | Base64 of the `.p8` key |
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

---

# RemClaw Android release (draft, sibling of the iOS release above)

`.github/workflows/remclaw-android-release.yml` builds the **private** RemClaw Android app
(`com.remapp.rem`, sources under `android/` in RemClaw) at one exact commit on a fresh
`ubuntu-24.04` runner and uploads it to the **Google Play internal track only**.
`android_release.py` reuses `release.py`'s deploy-key fetch, ancestry check, run identity,
private-log runner, classifier and public-output boundary unchanged. It also reuses the
Playground gate (`gate.validate_environment`), storage guard (`storage.py --platform android`)
and locked Fastlane bundle. The Play lane, `android/fastlane/Fastfile`, mirrors the Android
branch of the Playground Fastfile.

**Nothing runs yet.** As with iOS, nothing works until the environment, the deploy key in that
environment and the names below exist. This change creates none of them and dispatches nothing.

## Contract

Dispatch from **`main`** only:

| Input | Required value |
| --- | --- |
| `remclaw_sha` | Full lowercase 40-character RemClaw commit, already in the history of an allowed release branch (`ALLOWED_RELEASE_BRANCHES`, shared with iOS) |
| `version_code` | Play `versionCode`: a positive integer up to 2100000000 that is **above every version code** on any track and every bundle uploaded for `com.remapp.rem` |
| `track` | `internal`, the only option. The control script and the lane both refuse any other track |

The production backend URL is **not** an input. It is the reviewed constant
`PRODUCTION_API_BASE_URL` in `android_release.py`: `https://backend-production-7d876.up.railway.app`,
the same backend as the shipping RemClaw iOS Release build (`RemClaw/.env.Release.xcconfig`
`API_BASE_URL`). It is scheme and host only: RemClaw's Android `ApiClient` trims a trailing `/` and
appends absolute `/api/v1/...` paths, so the constant must not contain `/api/v1`. A blank value is
still refused with `PRODUCTION_API_BASE_URL is not committed` (class `POLICY`), and staging,
`.invalid`, `.test`, `localhost` and non-https values are refused.

The Play **release status** is not an input either. It is the reviewed constant
`PLAY_RELEASE_STATUS` (`draft` or `completed`) in `android_release.py`, recorded in the admission
record and validated again by the lane. Play accepts only `draft` releases while an app is still a
draft app, and the Android Publisher API does not report that app state, so the constant is
`draft` for the **first upload**:

- the bundle lands on the internal track as a **draft release** (`distribution: draft_internal`). It
  is not available to testers until a human reviews it and rolls it out in Play Console
  (Testing > Internal testing);
- until that happens, a later run is refused because the internal track has pending work. The lane
  never stacks on, replaces or promotes a draft;
- after the first rollout takes the app out of draft, a reviewed change sets
  `PLAY_RELEASE_STATUS = 'completed'`;
- if `completed` is set while the app is still a draft, Play's refusal is replaced by a fixed
  instruction (class `UPLOAD`). The lane never retries with another status, and the track is always
  `internal`.

### Jobs

1. **`admission`** (`ubuntu-24.04`, no secrets, no variables, no environment): runs the guard
   tests, then `android_release.py admit`. That checks run identity, SHA, version code, the
   track (`internal`) and the **pre-existing** protection of `remclaw-android-release`.
2. **`release`** (`ubuntu-24.04`, `environment: remclaw-android-release`):
   1. Re-verifies the environment protection.
   2. Fetches RemClaw with `REMCLAW_DEPLOY_KEY` using the iOS `release.py fetch` mechanism (see
      above). It then fetches the **public** Rem Design System anonymously over https at the
      exact commit in RemClaw's `android/design-system.lock`, and verifies `HEAD`. The
      checkout plays the role of `android/scripts/fetch-design-system.sh`.
   3. Sets up Temurin 17 and Ruby. There is no Gradle action and no cache. `GRADLE_USER_HOME`
      is under `$RUNNER_TEMP/remclaw-ephemeral`.
   4. Runs the storage guard before dependencies, build, sign and upload.
   5. Builds an **unsigned** `:app:bundleRelease` with the reviewed Gradle wrapper
      (`gradle-8.14.4-bin.zip`). It passes exactly `-PremApiBaseUrl` (the constant),
      `-PgoogleServerClientId` (the environment variable), `-PsourceSha` and `-PversionCode`.
      RemClaw's own `validateReleaseConfig` refuses staging, a blank client ID or a missing
      SHA. The step then:
      - validates the AAB with a pinned bundletool;
      - checks the base manifest: package `com.remapp.rem`, `versionCode` equal to the input,
        a numeric `versionName`, not `debuggable`, not `testOnly`;
      - checks the base dex for the `BuildConfig` stamps: the source SHA, the production URL
        and the client ID;
      - confirms that tracked source is unchanged.
   6. Signs with the upload key:
      - the keystore is written `0600` under the private directory and deleted in a `finally`;
      - passwords reach `keytool`/`jarsigner` only via `-storepass:env`;
      - the certificate's SHA-256 must equal `REMCLAW_ANDROID_UPLOAD_CERT_SHA256`;
      - after signing, `jarsigner -verify` must pass, there must be exactly one signer block
        containing that certificate, and every entry must be covered by the manifest.
   7. `fastlane internal_upload` takes the following steps:
      - re-checks the package/admission records and the bundle's SHA-256;
      - requires the internal track to **already exist** with no pending release;
      - requires `version_code` to be above every existing code;
      - uploads with `upload_to_play_store`, using `track: internal` and the admitted
        `release_status` (`draft` for the first upload, otherwise `completed`; no metadata, images,
        changelogs or other tracks);
      - reads back that the internal track holds a release with **that status** and version code.
   8. Always writes the allowlisted summary and always cleans up.

Public summary allowlist: `remclaw_sha`, `release_branch`, approval model/reviewer,
`version_code`, `track`, `release_status`, `distribution` (`not_started`, `upload_started`,
`draft_internal`, `published_internal`, `unresolved`) and `failure_class`. Everything else is dropped.
`distribution` set to `upload_started` or `unresolved` is **not** proof of failure: inspect the
Play internal track for that version code before any retry.

## Names-only setup checklist

These are the exact names the workflow consumes. Which of them exist is tracked in the PR's "Setup state", not here; presence is not proof that a run works.

- [ ] Play Console app `com.remapp.rem` exists, and its **internal testing** track exists
      (the lane refuses to create either).
- [ ] An upload key for `com.remapp.rem` (Play App Signing upload certificate, PKCS12).
- [ ] A Google Play service account limited to `com.remapp.rem`, with only the release
      permissions needed for testing tracks. No other apps, and no account-level admin.
- [ ] Environment **`remclaw-android-release`** in this repository with:
      - required reviewers: exactly one, **`samuelalake`** (the owner-approval model, or the
        independent model as for iOS);
      - **Allow administrators to bypass: off**;
      - deployment branches: selected branches, exactly one policy, `main`.
- [ ] Environment secrets in `remclaw-android-release`:

| Kind | Name | Purpose |
| --- | --- | --- |
| secret | `REMCLAW_DEPLOY_KEY` | Private half of a **read-only** RemClaw deploy key. The same name and mechanism as iOS. Environment secrets are per environment, so it must be set here too (the iOS key, or a separate read-only key for independent revocation) |
| secret | `REMCLAW_ANDROID_UPLOAD_KEYSTORE_BASE64` | Base64 of the PKCS12 upload keystore for `com.remapp.rem` (the upload key, never the app-signing key) |
| secret | `REMCLAW_ANDROID_UPLOAD_KEYSTORE_PASSWORD` | Keystore password. For PKCS12 the key password is the same |
| secret | `REMCLAW_PLAY_SERVICE_ACCOUNT_JSON` | Raw JSON key (`"type": "service_account"`) of the least-privilege Play service account above |
| variable | `REMCLAW_ANDROID_UPLOAD_KEY_ALIAS` | Alias of the upload key inside the keystore |
| variable | `REMCLAW_ANDROID_UPLOAD_CERT_SHA256` | SHA-256 of the upload certificate (DER, 64 hex, colons allowed). Signing is refused on mismatch |
| variable | `REMCLAW_ANDROID_GOOGLE_SERVER_CLIENT_ID` | Google OAuth **web** client ID (`<digits>-<id>.apps.googleusercontent.com`) that the backend verifies. Public, not a secret. A blank value fails closed |

- [x] A reviewed change setting `PRODUCTION_API_BASE_URL` in `android_release.py` (this branch; review it).
- [ ] After the first draft internal release is rolled out by a human in Play Console, a reviewed
      change setting `PLAY_RELEASE_STATUS = 'completed'`.

## Unverified until an authorized pilot

- Whether `com.remapp.rem` and its internal track exist in Play Console. The inspected
  account showed only the Playground app.
- Whether the first `draft` upload is accepted end to end, and whether a human rollout of that
  draft takes the app out of draft so that `completed` can be used afterwards.
- Resolving bundletool `1.16.0` through RemClaw's declared repositories (from the init script).
- Whether RemClaw's Android head is releasable. A SHA must first be on an allowed release branch.
- Storage thresholds, which were sized for the Playground Android app.

## Validation (Android additions)

```sh
python3 -m unittest discover -s tools/remclaw-release -p 'test_*.py' -v   # includes test_android_release.py
ruby tools/remclaw-release/test_android_workflow.rb
ruby tools/remclaw-release/test_android_fastfile.rb
python3 tools/playground-distribution/lint_workflows.py                     # now includes remclaw-android-release.yml
```
