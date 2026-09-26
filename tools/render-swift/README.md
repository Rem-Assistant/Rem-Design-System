# render-swift — SwiftUI screenshot evidence

A tiny macOS executable that renders each shipped SwiftUI component to a PNG (light + dark) so the
built design system is **visible** — no simulator, no running app.

- Depends on the shippable `RemDesignSystem` package via a path dependency (`../..`), so it renders
  the *real* components, not copies.
- Uses SwiftUI `ImageRenderer`; dark mode is driven by both `\.colorScheme` and the current drawing
  `NSAppearance` (the tokens resolve to `NSColor` on macOS).

```bash
# from this directory, on macOS:
swift run RenderGallery ./out        # writes ./out/<Component>-{light,dark}.png
```

Run in CI by `.github/workflows/screenshots.yml` (job `swiftui`), which uploads the PNGs and posts
them to the PR alongside the Compose (Android) renders.
