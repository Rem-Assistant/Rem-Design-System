# Private Playground distribution (draft)

This manual workflow is the proposed trusted control plane for building an exact
reviewed Playground commit on hosted runners and uploading directly to the existing
private TestFlight / Google Play destinations. The developer's Mac need not be online.
It is **not commissioned or proven end to end**. No store upload is part of this PR.

## Contract

Dispatch `.github/workflows/playground-distribute.yml` **from `main`**, supplying:

| Input | Required value |
| --- | --- |
| `source_sha` | Full lowercase 40-character application commit in this repository |
| `native_run_id` | Successful full `playground-native-candidate.yml` run at that commit |
| `visual_review_url` | Specific PR comment or review in this repository documenting paired visual acceptance and independent code review at that SHA |
| `build_number` | Unused positive integer below one billion, greater than existing selected-store builds |
| `platform` | `both`, `ios`, or `android` |

Admission downloads both native receipt archives even for a single-platform release.
It checks authenticated artifact SHA-256 digests, run and attempt identity, all four
successful jobs, unfiltered complete source test inventories, and the declared feature
contract. Candidate Python is parsed as data, never imported. Visual review is a
separate **human environment-approval gate**: a syntactically valid URL does not prove
that a person approved the pixels. Approvers must open it and verify the exact SHA,
paired screenshots, interaction evidence, and independent review before approving.

The trusted workflow/scripts must first be reviewed and merged to `main` through the
existing Factory lane. This PR does not repin Factory, promote Factory draft #107,
alter reviewer fallback, or change merge policy. Creating the draft runs the existing
review workflow; draft status does not disable it. Factory review is not store-upload
authorization. No release dispatch, store write, or merge is authorized by this draft.

After admission, each selected platform waits for its own protected environment
approval, repeats admission, builds unsigned, inspects the packaged metadata, signs
with an existing identity, then uploads directly from the same ephemeral hosted
runner. Signed packages never pass through Actions artifacts or GitHub Releases.
The only packaging overrides are build number and immutable source stamp. No local
branding, target SDK, icon, entitlement, or dependency overlay is applied.

## Commissioning prerequisites — currently blocked

An authorized repository/account administrator must separately approve and securely
configure these. Do not paste credentials into issues, comments, shell transcripts,
workflow inputs, or chat. This change does not create environments, install secrets,
mint keys, accept agreements, create apps/groups, or invite testers.

1. Create `playground-ios` and `playground-android` with **Samuel (`samuelalake`,
   GitHub ID `58840187`) as sole required reviewer**, **prevent self-review off**
   under the explicitly approved owner model below, **administrator bypass off**, and one selected
   deployment **branch** policy named `main`. No tag or wildcard policy. Admission
   checks these before referencing an environment, so absent protection fails closed
   instead of letting GitHub implicitly create an unprotected environment. The owner
   exception is restricted to Samuel's verified ID; it does not remove required review.
2. The user must supply sensitive values through secure handoff or an explicitly
   supported secure credential tool that keeps values outside the agent's context.
   Provision only the applicable platform's values below, at environment scope.
   Use least-privilege existing store app access. Do not add repository-wide
   release secrets. GitHub's runner token needs read access to Actions, contents and
   environment/deployment metadata; inability to inspect protections blocks release.
3. Commit the approved store metadata to the application candidate, including icons,
   branding, iOS export declaration/orientations and Android API 36/min 24, then obtain
   fresh full native and visual evidence for that exact commit. The application must
   retain the expected Xcode project/scheme and Gradle output contracts below, or the
   trusted packaging code must be reviewed to match a deliberate rename.
4. Review all candidate build scripts, plugins, dependencies, wrapper checksums and
   executable source for trust before approval. This design trusts the reviewed
   application source. **Building before credentials are injected is not a sandbox
   for hostile code:** a build could leave processes or modify later scripts on the
   same runner. Do not approve unreviewed or adversarial PRs. Stronger separation
   requires a separately designed private artifact handoff and signing service.
5. Review hosted native toolchain availability before commissioning. Ruby 3.3.12,
   Bundler 2.6.9, Fastlane 2.240.1 and every transitive gem/checksum are committed in
   `Gemfile.lock`. CI and release use frozen Bundler installs and `bundle exec`.
   Validate real hosted packaging,
   signing, processing and private assignment in an explicitly authorized pilot.

### Existing destinations and secure values

| Environment | Kind | Name / meaning |
| --- | --- | --- |
| iOS | secret | `PLAYGROUND_IOS_CERTIFICATE_BASE64`: existing exportable distribution certificate + private key as P12 |
| iOS | secret | `PLAYGROUND_IOS_CERTIFICATE_PASSWORD`: P12 password |
| iOS | secret | `PLAYGROUND_IOS_PROFILE_BASE64`: existing matching App Store provisioning profile |
| iOS | variable | `PLAYGROUND_IOS_CERTIFICATE_SHA1`: authorized existing distribution certificate fingerprint |
| iOS | secret | `PLAYGROUND_ASC_KEY_ID`, `PLAYGROUND_ASC_KEY_BASE64`: existing authorized App Store Connect API key; `PLAYGROUND_ASC_ISSUER_ID` is required for team keys, omitted for individual keys |
| iOS | variable | `PLAYGROUND_TESTFLIGHT_GROUP_ID`: existing private **internal**, manual-access group for this app |
| Android | secret | `PLAYGROUND_ANDROID_KEYSTORE_BASE64`: existing upload P12, alias `playground-upload` |
| Android | secret | `PLAYGROUND_ANDROID_KEYSTORE_PASSWORD`: existing P12/key password |
| Android | secret | `PLAYGROUND_GOOGLE_PLAY_JSON`: authorized service-account JSON with AndroidPublisher access to the existing app |

iOS app `6820738808`, bundle `com.rem.playground.settings`, team `R6A892D599`.
Android package `com.rem.designsystem.demo`, existing Play API track `internal`.
The Android upload certificate fingerprint is fixed in `package.py` to the verified
existing identity; Play's separate app-signing key is never downloaded or changed.
An exportable iOS CI distribution identity has **not** yet been verified. Existing
local automatic/cloud signing does not prove that these CI assets are available.
The current CLI Google token lacks AndroidPublisher scope; this draft requires an
authorized service account and does not silently reuse or broaden that token.

### Exact setup request for an authorized administrator

Approve creation/configuration of **only** `playground-ios` and `playground-android`
in this repository with Samuel (`samuelalake`, ID `58840187`) as the sole required
reviewer, **Prevent self-review unchecked**, **Allow administrators to bypass
configured protection rules unchecked**, and selected deployment branches enabled
with exactly the branch `main` (no tags/patterns). This is an explicit owner-approval
exception so Samuel can independently request and approve a release. No repository
rules, Factory configuration, app identifiers or store destinations change.

Read-only identity verification on 2026-10-09 established:

- The connected GitHub CLI is `samuelalake` (ID `58840187`) with repository admin
  access. Existing manual native run `37965150798` has both `actor` and
  `triggering_actor` equal to `samuelalake`. The distribution lane itself has never
  been dispatched. An iOS browser session's signed-in account was not inspected;
  approval must use the designated reviewer account.
- Existing repository administrators `oledibefrancis` (ID `62378296`) and
  `davidolaniran` (ID `74985099`) can be selected as independent reviewers without
  adding repository access. Their availability or agreement to own release reviews
  has not been established; neither has been assigned or contacted for this change.

Recommended model: Samuel dispatches, then deliberately approves each selected
platform's environment. This preserves the pause, exact-SHA evidence checks, audit
trail and required human review, but **does not provide two-person separation**.
Someone controlling Samuel's account could both dispatch and approve. Factory code
review and native/visual evidence remain required, and admin bypass stays disabled.
The workflow never approves its own deployment.

Alternative, only if Samuel selects it: keep Prevent self-review **on** and choose
exactly one existing reviewer, `oledibefrancis` or `davidolaniran`. That person must
approve Samuel's dispatch, so this option depends on another person's availability.
Admission rejects a selected independent reviewer who initiated or re-triggered the
run, instead of creating an impossible approval wait. The gate checks reviewer type,
stable GitHub ID and login; no new user/team is implicitly accepted. The selected
model and reviewer are included in the allowlisted run summary. GitHub documents
the [self-review restriction](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments).

Environment-setting approval covers only ordinary protection settings and separately
approved non-secret variables. It does not authorize agent extraction or transmission
of sensitive credentials. The user supplies the environment-specific secrets in the
table through the secure handoff described below.
Use existing identities with these capabilities; missing capabilities are a separate
grant decision, not permission to create keys or expand access:

- Apple: existing signing P12/private key and matching App Store profile for
  `R6A892D599.com.rem.playground.settings`; existing API identity able to read this
  app's builds/groups, upload a build and assign it to its existing internal group.
  Developer is sufficient for build uploads/internal testing; no Admin/Account Holder
  or production submission permission is requested. Prefer an existing individual
  key whose user's app access is limited to this app. A team API key can cover every
  app in the team: disclose that broader existing scope before installing it, do not
  represent it as app-scoped. No certificate/profile creation or group/tester edits.
- Google: existing service account with AndroidPublisher scope and Play permissions
  limited to `com.rem.designsystem.demo`: **View app information (read-only)** and
  **Release apps to testing tracks**. It must read tracks/bundles and create, upload,
  commit/delete edits for testing releases. No production release, financial data,
  account administration, store-listing management, or tester-list management is
  requested. The existing dedicated upload key is separate from this API identity;
  neither it nor the Play app-signing key is rotated.

The user enters credentials directly through GitHub's protected-environment secret UI,
or through an explicitly supported secure credential tool that accepts them without
exposing their values to the agent. The agent must not extract, copy, upload or relay
`.p8` keys, signing private keys/P12 files, passwords, keystore secrets or service-account
JSON from Keychain or files, even after broad setup approval. The connected Mac does
not authorize secret transfer, and no new desktop connection is required. Do not ask
Samuel to paste secrets from iOS or into chat. If secure user entry or a supported
secure tool is unavailable, keep credential setup blocked and report that boundary.
Return only setup completion and non-sensitive identity fingerprints, never
key/password/JSON values. This request does **not** authorize merging, dispatching
the workflow, uploading a candidate, changing store groups or running the pilot.
Those require a fresh exact-SHA release decision after the candidate's visual gate.

Only an existing private internal TestFlight group is supported in this first lane.
Existing external-test access is not sufficient: no external Beta App Review,
external-group assignment, public links, tester changes, metadata changes or
production release is performed. If a required tester belongs only to an external
group, that tester is outside this lane's current scope; do not silently move them.

### Application build interface

- iOS: `tools/playground-ios/RemSettingsPlayground.xcodeproj`, scheme
  `RemSettingsPlayground`; Release archive; marketing version `0.1.0`;
  `CURRENT_PROJECT_VERSION` and `REM_PLAYGROUND_SOURCE_SHA` consumed by committed
  project metadata. Hosted `macos-15` uses its selected Xcode; the measured version
  is retained in private packaging metadata. No claim of native-test toolchain parity.
- Android: `compose/gradlew`, Gradle `8.14.4`, JDK 17; `:demo:bundleRelease` produces
  `demo/build/outputs/bundle/release/demo-release.aab`; marketing version `1.0`;
  `-PplaygroundSourceSha` produces `git:<SHA>` in `RemPlaygroundSourceSHA` metadata.
  The trusted init script sets the release code through AGP `finalizeDsl` and uses
  the existing AGP bundletool classpath to inspect the embedded manifest.

The last examined source `90b43871fc4454c4cd72b704345356521610d338` has compatible
receipts (run `37965150798`, 57 iOS / 58 Android methods), but **is not an approved
release candidate**. Paired visual review found unresolved issues, and store-ready
metadata must be committed/retested. Passing this draft's receipt parser proves
schema compatibility only.

## Failure and recovery

All releases serialize. Do not cancel an in-flight store upload. A store can accept
a binary before an API call times out: `upload_started`, `processed`, or absent
readback is **unresolved**, not proof of failure. Inspect the exact existing app,
build number and source stamp in the store before any retry. Never blindly upload
again or automatically increment the number to hide uncertainty. If the build exists,
resolve assignment/processing through an explicitly authorized account operation.

Apple first enumerates all app groups and rejects automatic or unknown build access,
because an upload could otherwise reach another group before explicit assignment.
It uploads without automatic submission, waits for valid processing, rechecks
the private internal group, then assigns and reads the relationship back. Google
rejects a pending internal release, uploads only to `internal`, and reads back the
completed version code. Existing default store access and notifications may apply.
No store receipt proves installation or successful launch on a person's device;
physical-device status remains `unverified` until separately measured.

To stop future releases, disable this workflow through an authorized repository
operation; do not rotate existing signing keys as a rollback. Removing a distributed
build or changing tester access is a separate store action, not performed here.
Ephemeral packages, installed temporary profile/keychain and private logs are cleaned
up; hosted-runner destruction is the final boundary after cancellation or runner loss.
Provider logs are suppressed from public Actions output and not retained as artifacts.
Only validated source/run/build identifiers, binary digest, review URL and bounded
distribution/device states are placed in the public job summary. Keep review links
free of personal tester data. The source code and these nonsensitive IDs are public.

## Validation

```sh
python3 -m unittest discover -s tools/playground-distribution -p 'test_*.py' -v
ruby -c tools/playground-distribution/fastlane/Fastfile
ruby tools/playground-distribution/test_store.rb
ruby tools/playground-distribution/test_workflows.rb
# On the pinned Ruby runtime, with the committed frozen bundle installed:
(cd tools/playground-distribution && BUNDLE_FROZEN=true bundle exec ruby test_dependencies.rb)
```

Tests exercise input injection, foreign/stale runs, missing/duplicate jobs, incomplete
or filtered receipts, digest/path tampering, weak environments, wrong/expired signing
profiles, store metadata drift, and summary leakage. Read-only verification against
the real run confirmed both downloaded ZIP digests and exact source method sets.
These checks do not constitute a hosted build, signing proof, store upload, visual
approval, physical-device drive or permission to distribute.

`playground-distribution-check.yml` runs these guards plus frozen installation and
actual locked provider-interface loading on both `ubuntu-24.04` and `macos-15`.
Checksum-pinned Actionlint 1.7.12 validates both workflow schemas and expression
contexts; parsing YAML alone is insufficient for GitHub context availability.
It has a read-only token, no release environment, no store secrets and no package
build/upload step. Intentional dependency updates regenerate the checksummed lock
with Bundler 2.6.9 on Ruby 3.3.12, then rerun both platforms; release jobs never resolve
new versions or rewrite the lock. The temporary lock-bootstrap artifact contains
dependency metadata only and is retained for one day; the final check workflow
publishes no artifacts.

Provider references: [TestFlight upload](https://docs.fastlane.tools/actions/upload_to_testflight/),
[Play upload](https://docs.fastlane.tools/actions/upload_to_play_store/),
[Apple group assignment](https://developer.apple.com/documentation/appstoreconnectapi/post-v1-builds-_id_-relationships-betagroups),
[Play internal track API](https://developers.google.com/android-publisher/api-ref/rest/v3/edits.tracks/update).
[Apple internal testing roles](https://developer.apple.com/help/app-store-connect/test-a-beta-version/add-internal-testers/),
[Apple API key scope](https://developer.apple.com/help/app-store-connect/get-started/app-store-connect-api/),
[Play permissions](https://support.google.com/googleplay/android-developer/answer/9844686).
