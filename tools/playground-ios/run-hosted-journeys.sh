#!/bin/bash
# Hosted-only native journey execution with a targeted main-thread hang diagnostic.
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true ]] || { echo 'Run this helper on GitHub Actions only.' >&2; exit 2; }
test_args=(test)
if [[ -n "${IOS_TEST:-}" ]]; then
  # Each entry is testMethod (SettingsPlaygroundUITests, as screenshots.yml passes) or Class/testMethod.
  [[ "$IOS_TEST" =~ ^([A-Za-z]+UITests/)?test[A-Za-z0-9_]+(,([A-Za-z]+UITests/)?test[A-Za-z0-9_]+)*$ ]] || { echo 'Invalid iOS test method list.' >&2; exit 2; }
  IFS=',' read -r -a selected_methods <<< "$IOS_TEST"
  for selected_method in "${selected_methods[@]}"; do
    [[ "$selected_method" == */* ]] || selected_method="SettingsPlaygroundUITests/$selected_method"
    test_args+=("-only-testing:RemSettingsPlaygroundUITests/$selected_method")
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

# Stamp the build with the exact candidate revision when the caller provides one (the same
# REM_PLAYGROUND_SOURCE_SHA input build-local.sh uses); other callers keep "unversioned".
source_stamp=unversioned
if [[ "${SOURCE_SHA:-}" =~ ^[0-9a-f]{40}$ ]]; then source_stamp="$SOURCE_SHA"; fi

# iOS lays a one-time "slide to type" tip over the keyboard on a simulator's first text entry, and
# it can interrupt whichever test types first (a different test in each shard). Mark it shown and
# turn slide-to-type off on the test simulator before any test runs. Best effort: the catalog
# composer test still dismisses the tip if it appears.
sim_udid=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
for runtime, devices in json.load(sys.stdin)["devices"].items():
    if runtime.endswith("iOS-17-5"):
        for device in devices:
            if device["name"] == "iPhone 15":
                print(device["udid"])
                sys.exit()
' || true)
if [[ -n "$sim_udid" ]]; then
  xcrun simctl boot "$sim_udid" 2>/dev/null || true
  xcrun simctl bootstatus "$sim_udid" -b >/dev/null 2>&1 || true
  xcrun simctl spawn "$sim_udid" defaults write com.apple.keyboard.preferences DidShowContinuousPathIntroduction -bool true || true
  xcrun simctl spawn "$sim_udid" defaults write com.apple.keyboard.preferences KeyboardContinuousPathEnabled -bool false || true
fi

xcodebuild "${test_args[@]}" \
  -project tools/playground-ios/RemSettingsPlayground.xcodeproj \
  -scheme RemSettingsPlayground \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.5' \
  -parallel-testing-enabled NO \
  -test-timeouts-enabled YES \
  -maximum-test-execution-time-allowance 300 \
  -resultBundlePath "$RUNNER_TEMP/SettingsPlayground.xcresult" \
  REM_PLAYGROUND_SOURCE_SHA="$source_stamp" \
  CODE_SIGNING_ALLOWED=NO 2>&1 | tee "$RUNNER_TEMP/settings-ios-build.log"
