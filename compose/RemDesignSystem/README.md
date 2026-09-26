# RemDesignSystem — Compose (Android)

The Android sibling of the SwiftUI `Sources/RemDesignSystem/` package. Same token contract, native
form (Material containers, `ImageVector` glyphs — not iOS chrome / SF Symbols). Per `SPEC.md`: the
design system delivers component **code on both platforms**; tokens + component *intent* are shared,
form diverges.

## Layout
- **`tokens/generated/RemTokens.kt`** — GENERATED from `tokens.json` by `tools/generate-tokens.mjs`
  (same generator as Swift/CSS). `RemColorScheme` (light + dark) via `RemTheme` + `LocalRemColors`;
  `RemSpacing`/`RemRadius` (`Dp`), `RemTypography` (`TextStyle`), `RemOpacity`. Reference tokens
  resolve to the light/dark hex approximation and switch on `isSystemInDarkTheme()` (Compose has no
  Apple system palette) — exactly the CSS target's behavior. **Do not edit; run the generator.**
- **`compose/RemDesignSystem/primitives/ContainedIcon*.kt`** — the worked exemplar, in the same
  **3-file Fluent shape** as the Swift reference (`ContainedIcon.swift` / `…TokenSet.swift` /
  `….figma.swift`):
  - `ContainedIconTokenSet.kt` — the `TokenSet` + the public `Fill`/`Size` axes (1:1 with the Figma
    properties). Resolves every value from `RemTokens`. No `Style` file — a ContainedIcon is a
    decorative view, not a control (TokenSet + View for a view; TokenSet + Style for a control).
  - `ContainedIcon.kt` — the **thin** composable (reads the token set) + a `@Preview`.
  - `ContainedIcon.figma.kt` — Code Connect, **excluded from the Gradle build** (the app never links
    `com.figma.code.connect`); dormant-but-ready. Its exact DSL is flagged to verify on the Android
    runner (Compose Code Connect is newer than SwiftUI's).
  This is the pattern every extracted Compose component follows.

## Status / verify
Generator output is verified here (Node). **Compose compilation is not** — this repo has no Android
toolchain; that's the Agent Factory Android runner's job (the reference-vs-evidence split). A follow-up
wires a Gradle module (`com.rem.designsystem`) that consumes the generated `RemTokens.kt` and adds
Paparazzi/screenshot tests so `Verify` can attach the Android light+dark evidence a task's DoD requires.

## Drift guard
`node tools/generate-tokens.mjs --check` now covers `RemTokens.kt` alongside Swift + CSS, so the
existing `tokens.yml` CI fails if the Kotlin tokens drift from `tokens.json`.
