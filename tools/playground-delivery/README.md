# Private playground candidates

Scope is **Settings + Onboarding Voice**. Agenda is a separate candidate and is not included.
This is fixture UI: no audio playback or connected services. Continue/Skip report host callbacks.
Package identifiers remain `com.rem.playground.settings` and `com.rem.designsystem.demo`.

## Native evidence before a new package

1. Commit the isolated draft and select the exact reviewed 40-character SHA. Do not resolve a moving branch during packaging.
2. Once this workflow is available on an authorized branch, dispatch **Playground native candidate (no distribution)** at that same revision, with `source_sha` equal to the selected revision. It runs both complete native suites, without the legacy branch allowlist or focused-test filter. It does not trigger on PRs and has read-only repository permissions.
3. Require all native jobs and `native-evidence` to succeed. Download `ios-native-SHA` and `android-native-SHA` from that exact run. The records contain actual method names/counts, source SHA, scope, report hashes, toolchains, and GitHub run/attempt identity. `record-tests` only accepts exact-revision hosted execution. Obtain these records through the authenticated run; JSON alone is not a cryptographic attestation. A workflow-level green badge alone is insufficient. These are test records, not distributable binaries.
4. Run local packaging in a clean checkout at that SHA, using the corresponding test record. This rejects stale or incomplete native results. Use a new output directory on a volume with at least 5 GiB available; internal storage must retain at least 2 GiB. It never overwrites an older package or advances a `latest-ready` pointer.

```sh
REM_DEVELOPMENT_TEAM=EXISTING_AUTHORIZED_TEAM bash tools/playground-delivery/build-local.sh \
  ios APPROVED_SHA /Volumes/External/new-ios-output /absolute/ios-native.json
bash tools/playground-delivery/build-local.sh \
  android APPROVED_SHA /Volumes/External/new-android-output /absolute/android-native.json
```

The iOS command uses existing Xcode-managed development signing and never passes `-allowProvisioningUpdates`. Missing signing is a blocker, not a request to create certificates. The ZIP contains a device `.app`, not an App Store/TestFlight IPA. The app displays version and source SHA. Install only on an explicit request:

```sh
python3 tools/playground-delivery/install-ios.py --sha APPROVED_SHA \
  --manifest /absolute/manifest.json --binary /absolute/rem-playground-SHA.app.zip \
  --device PAIRED_DEVICE_ID --output /Volumes/External/new-device-verification
```

The installer verifies source, tests, ZIP hash, embedded app identity/version/SHA and code signature. It records installation and launch separately in a new private manifest, retaining failed launch status. A locked phone may accept installation but refuse launch.

Android requires an existing JDK 17, SDK, and existing local debug keystore; it stops before Gradle if the key is absent. It uses the checked-in **Gradle 8.14.4 wrapper**, not the legacy screenshot workflow's installed Gradle 8.9. The new native run must establish compatibility before delivery; that discrepancy is not silently normalized. Gradle downloads live in the new output directory, or an explicitly selected existing external `GRADLE_USER_HOME`. The package is `:demo:assembleDebug` output `demo/build/outputs/apk/debug/demo-debug.apk`, never the instrumentation APK. Keep the same signing key for updates. Persistent release signing is intentionally not provisioned here.

The repository is public. Do **not** upload APKs, signed app packages, signing assets, or device records to GitHub Actions/Releases as a private distribution route. Deliver verified packages through the user's private file channel. The new hosted workflow only uploads synthetic native test records.

## Readiness and freshness

`manifest.json` binds source/build SHA, version, feature scope, toolchain, actual native test results and binary SHA-256. New packages are explicitly `native_tested_device_unverified`; this is not a claim of a successful physical-device launch. The explicit iOS installer binds official install/launch JSON hashes to the source and binary. Android physical-device verification remains a separate manual step; no device pass is synthesized. Do not edit a failed device result into a pass.

```sh
python3 tools/playground-delivery/delivery.py verify --sha APPROVED_SHA --platform android \
  --manifest /absolute/manifest.json --binary /absolute/rem-playground-SHA.apk
python3 -m unittest discover -s tools/playground-delivery -p 'test_*.py'
```

No merge, upload, tester invitation, account modification, or automatic promotion is implemented. App Store/TestFlight needs a separate playground identifier and app record, distribution signing/profile, icon and orientation metadata, version sequencing, and explicit authorization for distribution. A development installation is the shortest existing route for the connected iPhone.

## Draft validation limits

The guard tests exercise stale SHA, skipped/filtered/partial/duplicate native results, scope mismatch, binary mutation, and overwrites. Hosted native suites still have to execute on this draft's exact committed revision. Prior PR87 results cannot qualify the changed draft. Device verification of PR87 does not qualify a newer package.
