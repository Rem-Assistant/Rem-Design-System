# Native iOS Settings playground

Open `RemSettingsPlayground.xcodeproj` in Xcode, select **RemSettingsPlayground**, choose a dedicated iPhone simulator and Run. The Swift package is a local dependency; no backend or credentials are required. The committed project is runnable without XcodeGen. To regenerate after editing `project.yml`: `xcodegen generate --spec project.yml`.

From the repository root (replace the explicit simulator UUID):

```sh
xcodebuild -project tools/playground-ios/RemSettingsPlayground.xcodeproj \
  -scheme RemSettingsPlayground \
  -destination 'platform=iOS Simulator,id=YOUR_TEST_SIMULATOR_UUID' \
  -derivedDataPath /path/to/build-cache \
  -resultBundlePath /path/to/settings-tests.xcresult \
  -parallel-testing-enabled NO test
```

Tests exercise Settings → Rem → Back; simulated error → Cancel/Retry; cancellation before a 10-second slow load finishes; and name edit → Cancel/Save. Screenshot attachments are retained in the result bundle. Do not erase an existing personal device.

The gallery identifies scope: the Rem row is connected; other rows are visual references. See `docs/contracts/settings-playground.md` for the bounded design and behavior contract.
