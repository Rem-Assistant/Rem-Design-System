#!/bin/bash
# Prepare a private, exact-SHA package. Does not install, upload, or create signing assets.
set -euo pipefail
[[ $# == 4 ]] || { echo 'Usage: build-local.sh ios|android APPROVED_SHA NEW_OUTPUT_DIR NATIVE_TESTS_JSON' >&2; exit 2; }
PLATFORM=$1
SHA=$2
OUT=$3
TESTS=$4
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
[[ "$OUT" == /* && ! -e "$OUT" ]] || { echo 'Output must be a new absolute directory' >&2; exit 2; }
python3 "$ROOT/tools/playground-delivery/delivery.py" preflight --sha "$SHA"
python3 "$ROOT/tools/playground-delivery/delivery.py" check-tests --sha "$SHA" --platform "$PLATFORM" --tests "$TESTS"
python3 - "$(dirname "$OUT")" <<'PY'
import shutil,sys
for path, reserve in [(sys.argv[1], 5*1024**3), ('/', 2*1024**3)]:
    if shutil.disk_usage(path).free < reserve:
        raise SystemExit('Insufficient disk space; no build started: '+path)
PY
case "$PLATFORM" in
  ios)
    : "${REM_DEVELOPMENT_TEAM:?Set an existing authorized Apple development team}"
    # Deliberately no -allowProvisioningUpdates or API-key arguments.
    security find-identity -v -p codesigning | grep -q 'Apple Development:' || { echo 'No existing development signing identity'; exit 1; }
    ;;
  android)
    java -version 2>&1 | grep -q 'version "17\.' || { echo 'An existing JDK 17 is required'; exit 1; }
    : "${ANDROID_HOME:?Set the existing Android SDK directory}"
    test -x "$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer" || { echo 'Existing apkanalyzer required'; exit 1; }
    test -x "$ANDROID_HOME/build-tools/34.0.0/apksigner" || { echo 'Existing Android build-tools 34 required'; exit 1; }
    test -d "$ANDROID_HOME/platforms/android-34" || { echo 'Existing Android SDK platform 34 required'; exit 1; }
    [[ -z "${ANDROID_PREFS_ROOT:-}" ]] || { echo 'Unset ANDROID_PREFS_ROOT to avoid ambiguous signing key location'; exit 1; }
    # Stop before Gradle can create a new persistent debug key.
    test -f "${ANDROID_USER_HOME:-$HOME/.android}/debug.keystore" || { echo 'Existing debug signing key required; no key generated'; exit 1; }
    ;;
  *) echo 'Expected ios or android' >&2; exit 2;;
esac
mkdir "$OUT"
mkdir "$OUT/tmp"
export TMPDIR="$OUT/tmp"
cd "$ROOT"
if [[ "$PLATFORM" == ios ]]; then
  xcodebuild -version > "$OUT/toolchain.txt"
  xcodebuild -project tools/playground-ios/RemSettingsPlayground.xcodeproj \
    -scheme RemSettingsPlayground -configuration Debug -sdk iphoneos \
    -destination 'generic/platform=iOS' -derivedDataPath "$OUT/DerivedData" \
    -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=YES CODE_SIGN_STYLE=Automatic \
    DEVELOPMENT_TEAM="$REM_DEVELOPMENT_TEAM" CODE_SIGN_IDENTITY='Apple Development' \
    REM_PLAYGROUND_SOURCE_SHA="$SHA" build > "$OUT/build.log" 2>&1
  APP="$OUT/DerivedData/Build/Products/Debug-iphoneos/RemSettingsPlayground.app"
  [[ "$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$APP/Info.plist")" == com.rem.playground.settings ]]
  [[ "$(/usr/libexec/PlistBuddy -c 'Print RemPlaygroundSourceSHA' "$APP/Info.plist")" == "$SHA" ]]
  codesign --verify --deep --strict "$APP"
  python3 "$ROOT/tools/playground-delivery/delivery.py" inspect --sha "$SHA" --platform ios --artifact "$APP" --output "$OUT/binary-metadata.json"
  BINARY="$OUT/rem-playground-$SHA.app.zip"
  ditto -c -k --keepParent "$APP" "$BINARY"
else
  # Reuse an explicitly selected external cache, or keep downloads in this output.
  export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$OUT/gradle-cache}"
  cd compose
  ./gradlew --version > "$OUT/toolchain.txt"
  java -version >> "$OUT/toolchain.txt" 2>&1
  # Verify the checked-in wrapper; never silently substitute installed Gradle 8.9.
  grep -q '^Gradle 8.14.4$' "$OUT/toolchain.txt"
  ./gradlew :demo:clean :demo:assembleDebug -PplaygroundSourceSha="$SHA" --no-daemon > "$OUT/build.log" 2>&1
  BINARY="$OUT/rem-playground-$SHA.apk"
  cp demo/build/outputs/apk/debug/demo-debug.apk "$BINARY"
  "$ANDROID_HOME/build-tools/34.0.0/apksigner" verify "$BINARY"
  python3 "$ROOT/tools/playground-delivery/delivery.py" inspect --sha "$SHA" --platform android --artifact "$BINARY" --output "$OUT/binary-metadata.json"
fi
python3 "$ROOT/tools/playground-delivery/delivery.py" manifest --sha "$SHA" --platform "$PLATFORM" \
  --metadata "$OUT/binary-metadata.json" --tests "$TESTS" --binary "$BINARY" --toolchain "$OUT/toolchain.txt" --output "$OUT/manifest.json"
python3 "$ROOT/tools/playground-delivery/delivery.py" verify --sha "$SHA" --platform "$PLATFORM" \
  --manifest "$OUT/manifest.json" --binary "$BINARY"
echo "Private local package: $BINARY; physical-device launch remains unverified."
