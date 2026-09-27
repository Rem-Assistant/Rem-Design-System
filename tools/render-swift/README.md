# render-swift — SwiftUI screenshot evidence (iOS Simulator)

Renders each shipped SwiftUI component/screen to a PNG (light + dark) so the built design system is
**visible** — on a real **iOS Simulator**, so the colors are true iOS (`.systemBackground` is white
on iOS, grey on macOS) and `ScrollView`/`List` content renders faithfully.

- `Tests/RenderSnapshotTests/RenderSnapshots.swift` is an XCTest that snapshots a real
  `UIHostingController` hierarchy with `host.view.layer.render(in:)` (a headless logic-test simulator
  rejects `drawHierarchy(afterScreenUpdates:)` with a "render server" error). Because an env var like
  `SNAPSHOT_OUT_DIR` does not cross into the simulator process, each PNG is emitted as an
  `XCTAttachment` and extracted from the `.xcresult` afterwards (via `xcparse`) — the test does not
  write to disk itself. It's declared as a test target in the repo-root `Package.swift` and depends on
  the `RemDesignSystem` library. The file is `#if canImport(UIKit)`-guarded, so `swift test` on
  macOS compiles it to an empty target.

```bash
# on macOS with Xcode, from the repo root — PNGs land in the .xcresult, not on disk:
xcodebuild test \
  -scheme RemDesignSystem \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -resultBundlePath "$PWD/RenderSnapshots.xcresult" \
  -only-testing:RenderSnapshotTests
# then: xcparse attachments RenderSnapshots.xcresult ./artifacts/swiftui
```

Run in CI by `.github/workflows/screenshots.yml` (job `swiftui`), which extracts the PNGs from the
`.xcresult` and posts them next to the Compose (Android) renders in a side-by-side table on the PR.
