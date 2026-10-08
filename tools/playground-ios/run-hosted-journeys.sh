#!/bin/bash
# Hosted-only native journey execution with a targeted main-thread hang diagnostic.
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true ]] || { echo 'Run this helper on GitHub Actions only.' >&2; exit 2; }
test_args=(test)
if [[ -n "${IOS_TEST:-}" ]]; then
  [[ "$IOS_TEST" =~ ^test[A-Za-z0-9_]+(,test[A-Za-z0-9_]+)*$ ]] || { echo 'Invalid iOS test method list.' >&2; exit 2; }
  IFS=',' read -r -a selected_methods <<< "$IOS_TEST"
  for selected_method in "${selected_methods[@]}"; do
    test_args+=("-only-testing:RemSettingsPlaygroundUITests/SettingsPlaygroundUITests/$selected_method")
  done
fi

# Read only this run's log and sample only the playground process on this disposable runner.
# A sample is diagnostic evidence; it does not change the app or suppress a test failure.
(
  while true; do
    log_file="$RUNNER_TEMP/settings-ios-build.log"
    stale_log=false
    if [[ -f "$log_file" ]]; then
      log_age=$(( $(date +%s) - $(stat -f %m "$log_file") ))
      if (( log_age >= 60 )) && pgrep -x RemSettingsPlayground >/dev/null; then
        stale_log=true
      fi
    fi
    if [[ -f "$log_file" ]] &&
       { grep -q 'App event loop idle notification not received' "$log_file" || [[ "$stale_log" == true ]]; }; then
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

xcodebuild "${test_args[@]}" \
  -project tools/playground-ios/RemSettingsPlayground.xcodeproj \
  -scheme RemSettingsPlayground \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.5' \
  -parallel-testing-enabled NO \
  -test-timeouts-enabled YES \
  -maximum-test-execution-time-allowance 300 \
  -resultBundlePath "$RUNNER_TEMP/SettingsPlayground.xcresult" \
  CODE_SIGNING_ALLOWED=NO 2>&1 | tee "$RUNNER_TEMP/settings-ios-build.log"
