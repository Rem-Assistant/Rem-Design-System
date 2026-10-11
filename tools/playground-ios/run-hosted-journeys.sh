#!/bin/bash
# Hosted-only native journey execution with a targeted main-thread hang diagnostic.
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true ]] || { echo 'Run this helper on GitHub Actions only.' >&2; exit 2; }
test_args=(test)
# Shard mode (IOS_SHARD_INDEX of IOS_SHARD_COUNT, zero-based) runs one partition of the inventory in
# ios_test_shards.py, checked against what Xcode discovers. It is exclusive with a focused IOS_TEST list.
shard_mode=false
if [[ -n "${IOS_SHARD_COUNT:-}" ]]; then
  [[ -z "${IOS_TEST:-}" ]] || { echo 'IOS_TEST and IOS_SHARD_COUNT are exclusive.' >&2; exit 2; }
  [[ "$IOS_SHARD_COUNT" =~ ^[1-9]$ && "${IOS_SHARD_INDEX:-}" =~ ^[0-9]$ ]] || { echo 'Invalid iOS shard.' >&2; exit 2; }
  (( IOS_SHARD_INDEX < IOS_SHARD_COUNT )) || { echo 'Invalid iOS shard.' >&2; exit 2; }
  shard_mode=true
fi
xcode_args=(
  -project tools/playground-ios/RemSettingsPlayground.xcodeproj
  -scheme RemSettingsPlayground
  -sdk iphonesimulator
  -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.5'
)
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
if [[ "$shard_mode" == true ]]; then
  # The partition is computed from the source inventory; Xcode's own discovery must match it exactly,
  # so a test the inventory misses (or a disabled one) fails the shard instead of silently not running.
  xcodebuild test "${xcode_args[@]}" -enumerate-tests -test-enumeration-style flat \
    -test-enumeration-format json -test-enumeration-output-path "$RUNNER_TEMP/ios-test-inventory.json" \
    REM_PLAYGROUND_SOURCE_SHA="$source_stamp" CODE_SIGNING_ALLOWED=NO 2>&1 | tee "$RUNNER_TEMP/settings-ios-enumerate.log"
  python3 tools/playground-ios/ios_test_shards.py check-enumeration "$RUNNER_TEMP/ios-test-inventory.json"
  IOS_TEST=$(python3 tools/playground-ios/ios_test_shards.py shard --count "$IOS_SHARD_COUNT" --index "$IOS_SHARD_INDEX")
  [[ -n "$IOS_TEST" ]] || { echo 'Empty iOS shard.' >&2; exit 2; }
  IFS=',' read -r -a selected_methods <<< "$IOS_TEST"
  for selected_method in "${selected_methods[@]}"; do
    test_args+=("-only-testing:RemSettingsPlaygroundUITests/$selected_method")
  done
fi

if [[ -n "$sim_udid" ]]; then
  xcrun simctl boot "$sim_udid" 2>/dev/null || true
  xcrun simctl bootstatus "$sim_udid" -b >/dev/null 2>&1 || true
  xcrun simctl spawn "$sim_udid" defaults write com.apple.keyboard.preferences DidShowContinuousPathIntroduction -bool true || true
  xcrun simctl spawn "$sim_udid" defaults write com.apple.keyboard.preferences KeyboardContinuousPathEnabled -bool false || true
fi

xcodebuild "${test_args[@]}" "${xcode_args[@]}" \
  -parallel-testing-enabled NO \
  -test-timeouts-enabled YES \
  -maximum-test-execution-time-allowance 300 \
  -resultBundlePath "$RUNNER_TEMP/SettingsPlayground.xcresult" \
  REM_PLAYGROUND_SOURCE_SHA="$source_stamp" \
  CODE_SIGNING_ALLOWED=NO 2>&1 | tee "$RUNNER_TEMP/settings-ios-build.log"

# A filter that matches nothing makes xcodebuild report TEST SUCCEEDED with 0 tests (run 38096566809:
# a bare method name was prefixed with the wrong class). Every selected method must actually start.
if [[ -n "${IOS_TEST:-}" ]]; then
  for selected_method in "${selected_methods[@]}"; do
    method="${selected_method##*/}"
    grep -Eq "Test Case '-\[[A-Za-z]+\.[A-Za-z]+UITests ${method}\]' started" "$RUNNER_TEMP/settings-ios-build.log" || {
      echo "Selected iOS test did not run: $selected_method (check its class name)." >&2
      exit 1
    }
  done
fi

# A shard must run every selected test to a pass or a failure: none missing, skipped or from another shard.
if [[ "$shard_mode" == true ]]; then
  python3 tools/playground-ios/ios_test_shards.py check-results "$RUNNER_TEMP/settings-ios-build.log" \
    --count "$IOS_SHARD_COUNT" --index "$IOS_SHARD_INDEX"
fi
