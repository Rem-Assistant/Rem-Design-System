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
- **`compose/RemDesignSystem/primitives/ContainedIcon.kt`** — the worked exemplar. Mirrors the Swift
  `TokenSet + thin component` shape: a `ContainedIconTokens(fill, size)` resolves every value from
  `RemTokens`, and the composable is thin. This is the pattern every extracted Compose component
  follows (the Kotlin twin of the `RemButton`/`ContainedIcon` reference).

## Status / verify
Generator output is verified here (Node). **Compose compilation is not** — this repo has no Android
toolchain; that's the Agent Factory Android runner's job (the reference-vs-evidence split). A follow-up
wires a Gradle module (`com.rem.designsystem`) that consumes the generated `RemTokens.kt` and adds
Paparazzi/screenshot tests so `Verify` can attach the Android light+dark evidence a task's DoD requires.

## Drift guard
`node tools/generate-tokens.mjs --check` now covers `RemTokens.kt` alongside Swift + CSS, so the
existing `tokens.yml` CI fails if the Kotlin tokens drift from `tokens.json`.
