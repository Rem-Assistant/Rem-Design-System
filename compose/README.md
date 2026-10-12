# Rem Compose design system

`RemDesignSystem` is the reusable Android library. `demo` is an installable test app that renders the library's screen and section components on a device or emulator.
The demo ships as the **Rem Playground** (`com.rem.designsystem.demo`, minSdk 24, compileSdk/targetSdk 36 as Play
packaging requires; the library still compiles against 34). Its launcher icon reuses the iOS Playground artwork
(`tools/playground-ios/App/Assets.xcassets/AppIcon.appiconset/AppIcon.png`): an adaptive icon (`#0C50FF` background,
white mark inside the 66dp safe zone) plus legacy square and round mipmaps for API 24–25.

Open this `compose` directory in Android Studio, select the `demo` run configuration, and run it on an Android 7.0+ device or emulator.

From a terminal with JDK 17 and the Android SDK configured:

```sh
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
adb shell am start -n com.rem.designsystem.demo/.MainActivity
```

The playground root has Components (a catalog: Controls, Rows, Agenda, Chat, Agent, Brand & empty states, Loading) and Screens (Settings, Agenda, Onboarding — whose Check-in step is the shared cadence screen with the platform time picker and a Succeeds/Fails once save fixture). Settings exposes **Success**, **Slow** (10 seconds), and **Error** local fixtures. Open Settings → Rem to inspect Agent settings; Back/Cancel returns to Settings and Retry recovers. **Shared controls** contains local notification/name editing. Other Settings rows, including Automations, are visual references only.

Run the instrumentation suite on a dedicated device:

```sh
./gradlew :demo:assembleDebug :demo:assembleDebugAndroidTest
adb -s YOUR_TEST_DEVICE install -r demo/build/outputs/apk/debug/demo-debug.apk
adb -s YOUR_TEST_DEVICE install -r demo/build/outputs/apk/androidTest/debug/demo-debug-androidTest.apk
adb -s YOUR_TEST_DEVICE shell am instrument -w -r \
  com.rem.designsystem.demo.test/androidx.test.runner.AndroidJUnitRunner
adb -s YOUR_TEST_DEVICE pull /sdcard/Android/data/com.rem.designsystem.demo/files ./screenshots
```

`./gradlew :RemDesignSystem:testDebugUnitTest` runs the existing library checks. The Settings instrumentation suite is separate from those snapshots. See `../docs/contracts/settings-playground.md` for the source frames and acceptance criteria.

## Third-party dependencies

| Dependency | Version | Licence | Used by |
|---|---|---|---|
| [Haze](https://github.com/chrisbanes/haze) (`dev.chrisbanes.haze:haze`) | 1.1.1 (pinned) | Apache-2.0 | `primitives/RemGlass.kt` (public glass source `rememberRemGlassState` / `Modifier.remGlassSource`), `buttons/RemGlassIconButton.kt` (glass icon button), `screens/ChatEdgeBackdrop.kt` (Chat header and composer-dock backdrop blur) and `screens/ChatJumpToLatestButton.kt` (glass jump to latest, built on `RemGlassIconButton`). Blur on API 32+, gradient scrim / solid fallback below. An `implementation` dependency: Haze types never appear in the public API (`RemGlassState` wraps `HazeState`), so consumers need no Haze dependency. |

Everything else is AndroidX / Jetpack Compose (Apache-2.0). Haze 1.1.1 is built against this toolchain (Kotlin
2.0.21, Compose 1.7, minSdk 21 ≤ 24, compileSdk 34); it lifts Compose foundation/ui from 1.7.3 to 1.7.5. Haze 1.3+
needs Kotlin 2.1 and 1.4+ needs core-ktx 1.15 (compileSdk 35), so moving past 1.1.x waits on a toolchain bump.
