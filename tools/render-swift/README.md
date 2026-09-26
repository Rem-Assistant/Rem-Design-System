# render-swift — SwiftUI screenshot evidence (iOS Simulator)

Renders each shipped SwiftUI component/screen to a PNG (light + dark) so the built design system is
**visible** — on a real **iOS Simulator**, so the colors are true iOS (`.systemBackground` is white
on iOS, grey on macOS) and `ScrollView`/`List` content renders faithfully.

- `Tests/RenderSnapshotTests/RenderSnapshots.swift` is an XCTest that snapshots a real
  `UIHostingController` hierarchy with `drawHierarchy(afterScreenUpdates:)` and writes PNGs to
  `SNAPSHOT_OUT_DIR`. It's declared as a test target in the repo-root `Package.swift` and depends on
  the `RemDesignSystem` library. The file is `#if canImport(UIKit)`-guarded, so `swift test` on
  macOS compiles it to an empty target.

```bash
# on macOS with Xcode, from the repo root:
SNAPSHOT_OUT_DIR="$PWD/artifacts/swiftui" xcodebuild test \
  -scheme RemDesignSystem \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -only-testing:RenderSnapshotTests
```

Run in CI by `.github/workflows/screenshots.yml` (job `swiftui`), which uploads the PNGs and posts
them next to the Compose (Android) renders in a side-by-side table on the PR.
