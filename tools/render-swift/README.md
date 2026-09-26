# render-swift — SwiftUI screenshot evidence

A tiny macOS executable that renders each shipped SwiftUI component to a PNG (light + dark) so the
built design system is **visible** — no simulator, no running app.

- Declared as the `RenderGallery` executable **target in the repo-root `Package.swift`** (depends on
  the `RemDesignSystem` library target by name), so it renders the *real* components. It is not part
  of the library product, so library consumers never build it.
- Uses SwiftUI `ImageRenderer`; dark mode is driven by both `\.colorScheme` and the current drawing
  `NSAppearance` (the tokens resolve to `NSColor` on macOS).

```bash
# from the repo root, on macOS:
swift run RenderGallery ./out        # writes ./out/<Component>-{light,dark}.png
```

Run in CI by `.github/workflows/screenshots.yml` (job `swiftui`), which uploads the PNGs and posts
them to the PR alongside the Compose (Android) renders.
