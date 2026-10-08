#!/bin/bash
# Hosted-only native journey execution with a targeted main-thread hang diagnostic.
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true ]] || { echo 'Run this helper on GitHub Actions only.' >&2; exit 2; }
test_args=()
if [[ -n "${IOS_TEST:-}" ]]; then
  [[ "$IOS_TEST" =~ ^test[A-Za-z0-9_]+$ ]] || { echo 'Invalid iOS test method.' >&2; exit 2; }
  test_args+=("-only-testing:RemSettingsPlaygroundUITests/SettingsPlaygroundUITests/$IOS_TEST")
fi

# Read only this run's log and sample only the playground process on this disposable runner.
# A sample is diagnostic evidence; it does not change the app or suppress a test failure.
(
  while true; do
    if [[ -f "$RUNNER_TEMP/settings-ios-build.log" ]] &&
       grep -q 'App event loop idle notification not received' "$RUNNER_TEMP/settings-ios-build.log"; then
      for app_pid in $(pgrep -x RemSettingsPlayground || true); do
        sample "$app_pid" 5 1 -file "$RUNNER_TEMP/settings-ios-main-thread-$app_pid.txt" || true
      done
      break
    fi
    sleep 5
  done
) &
sampler_pid=$!
trap 'kill "$sampler_pid" 2>/dev/null || true' EXIT

xcodebuild test \
  -project tools/playground-ios/RemSettingsPlayground.xcodeproj \
  -scheme RemSettingsPlayground \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.5' \
  -parallel-testing-enabled NO \
  -test-timeouts-enabled YES \
  -maximum-test-execution-time-allowance 300 \
  "${test_args[@]}" \
  -resultBundlePath "$RUNNER_TEMP/SettingsPlayground.xcresult" \
  CODE_SIGNING_ALLOWED=NO 2>&1 | tee "$RUNNER_TEMP/settings-ios-build.log"
