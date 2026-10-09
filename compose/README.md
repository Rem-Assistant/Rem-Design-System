# Rem Compose design system

`RemDesignSystem` is the reusable Android library. `demo` is an installable test app that renders the library's screen and section components on a device or emulator.

Open this `compose` directory in Android Studio, select the `demo` run configuration, and run it on an Android 7.0+ device or emulator.

From a terminal with JDK 17 and the Android SDK configured:

```sh
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
adb shell am start -n com.rem.designsystem.demo/.MainActivity
```

The playground root has Components (Controls, Loading) and Screens (Settings, Agenda, Onboarding). Settings exposes **Success**, **Slow** (10 seconds), and **Error** local fixtures. Open Settings → Rem to inspect Agent settings; Back/Cancel returns to Settings and Retry recovers. **Shared controls** contains local notification/name editing. Other Settings rows, including Automations, are visual references only.

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
