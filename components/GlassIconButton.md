---
component: GlassIconButton
group: general
mirrors: RemGlassIconButton (SwiftUI `Buttons/RemGlassIconButton.swift`) / RemGlassIconButton (Compose `buttons/RemGlassIconButton.kt`)
status: draft
rules:
  - id: glass-over-content-only
    do: "Use a glass icon button for icon-only chrome that floats over content (a top bar's menu or back-to-chat control, jump to latest)."
    dont: "Use it for an action inside content or a form; use Button there, or ListRow for a tappable row."
    enforced_by: "prose-only"
  - id: glass-label-required
    do: "Give every glass icon button an accessibility label that names the action (\"Menu\", \"Back to chat\")."
    dont: "Leave the label empty or describe the glyph (\"Three lines\")."
    enforced_by: "API: contentDescription / accessibilityLabel is a required parameter"
  - id: glass-source-sibling
    do: "On Android, mark the content behind the controls with remGlassSource(state) and pass the same state to each button drawn over it."
    dont: "Put the button inside the glass source, or fake glass with a translucent fill or an alpha on the whole button."
    enforced_by: "prose-only"
  - id: glass-touch-target
    do: "Keep the default 44 size for chrome; a smaller circle still gets the 44 touch target."
    dont: "Shrink the hit area below 44 with an outer size or clip."
    enforced_by: "RemGlassIconButtonTest.touchTargetIsAtLeast44AtEverySize"
---

# GlassIconButton

## Overview
A round, icon-only button drawn in glass: iOS 26's system glass button (`.glassEffect(.regular.interactive())`) and
its Android twin. Member of the **Button** family (an icon-only affordance), not a new control. Public API:
`RemGlassIconButton` on both platforms. Chat's jump to latest (`ChatJumpToLatestButton`) is built on it on Android.

## When to use
- Chrome that floats over scrolling content: a top bar's leading menu (☰) or trailing back-to-chat (») control
  outside chat; a jump-to-latest control.
- One or two controls at the edges of a bar; the bar itself stays transparent so content scrolls under it.

## When *not* to use
- **An action in content, a sheet or a form** → `Button`.
- **A whole tappable row** → `ListRow`.
- **The Chat header's controls** → `ChatHeader` owns them (solid circles today; see *Open decisions*).

## Anatomy
- **Circle** — the glass surface: blurred, tinted backdrop (or the solid fallback), 0.5 `separator` hairline, soft shadow.
- **Glyph** — `labelPrimary`, centred: SF Symbol (17pt semibold in 44) on iOS; on Android an `ImageVector`, a
  `Painter`, or a registry `RemMaterialSymbol` (20dp in 44). Fixed size (does not grow with text size).
- **Touch target** — at least 44 × 44 at every circle size, the circle centred in it.

## Variants & states
| Prop | Values | Notes |
|---|---|---|
| `size` | 44 (default), any | Circle diameter; glyph scales with it (20/44 Android, 17/44 iOS). Touch target never below 44. |
| `glassState` (Android) | `RemGlassState?` (default null) | The backdrop to blur. Null draws the solid fallback. |
| enabled | `enabled` (Android) / `.disabled()` (iOS) | Disabled: glyph `labelTertiary`, not clickable, announced disabled; the surface does not change. |

States: rest, pressed (Android ripple inside the circle; iOS 26 interactive glass), disabled.

### Surface by platform and condition
| Condition | iOS / macOS | Android |
|---|---|---|
| iOS 26 / API 32+ with a glass source | System Liquid Glass | Blur (radius 20) of the source, `backgroundTertiary` at `opacity.glassTint` (0.6), hairline, shadow |
| Before iOS 26 | `.ultraThinMaterial` circle, hairline, shadow | — |
| Below API 32 | — | Solid `backgroundTertiary`, hairline, shadow (Haze's fallback tint) |
| No glass source | — (SwiftUI glass always samples what is behind) | Solid `backgroundTertiary`, hairline, shadow |
| Reduce Transparency / Increase Contrast | System material turns opaque / higher contrast | Android 14+ contrast level above standard → solid (Android has no reduce-transparency setting) |

The solid fallback keeps the same hairline and shadow, so the edge is visible on a matching background and the
`labelPrimary` glyph keeps full contrast against `backgroundTertiary`.

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use a glass icon button for icon-only chrome that floats over content (a top bar's menu or back-to-chat control, jump to latest). | Use it for an action inside content or a form; use Button there, or ListRow for a tappable row. |
| Give every glass icon button an accessibility label that names the action ("Menu", "Back to chat"). | Leave the label empty or describe the glyph ("Three lines"). |
| On Android, mark the content behind the controls with remGlassSource(state) and pass the same state to each button drawn over it. | Put the button inside the glass source, or fake glass with a translucent fill or an alpha on the whole button. |
| Keep the default 44 size for chrome; a smaller circle still gets the 44 touch target. | Shrink the hit area below 44 with an outer size or clip. |

## Accessibility
- **Contrast:** `labelPrimary` on the tinted or solid `backgroundTertiary`; the solid fallback is used whenever
  the system asks for more contrast.
- **Hit target:** ≥ 44 × 44 at every size (unit-tested on Android).
- **Scaling:** fixed icon-only chrome; iOS offers the Large Content Viewer.
- **Labels:** one Button node labelled by `contentDescription` / `accessibilityLabel`; the glyph is not announced.
  Disabled is announced as disabled.

## Tokens used
- `color.background.tertiary`, `color.background.primary` (Haze base), `color.separator`, `color.label.primary`,
  `color.label.tertiary` (disabled), `opacity.glassTint` (new, 0.6).
- Structural constants (Android `RemGlass`): blur radius 20, shadow elevation 4, hairline 0.5. Shared with the Chat
  edge backdrops; no blur / elevation tokens exist yet.

## Android usage
```kotlin
val glass = rememberRemGlassState()
Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().remGlassSource(glass)) { /* content scrolls under the bar */ }
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)) {
        RemGlassIconButton(Icons.Filled.Menu, "Menu", onMenu, glassState = glass)
        Spacer(Modifier.weight(1f))
        RemGlassIconButton(Icons.Filled.KeyboardDoubleArrowRight, "Back to chat", onBackToChat, glassState = glass)
    }
}
```
The buttons are siblings drawn over the source, never inside it. `RemGlassState` wraps Haze's state, so a consuming
app needs no Haze dependency of its own.

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `icon` / `painter` / `symbol` (Android); `systemName` / `image` (iOS) | glyph | — | The glyph. |
| `contentDescription` / `accessibilityLabel` | String | — | Required spoken label. |
| `onClick` / `action` | () -> Unit | — | Tap action. |
| `modifier` (Android) | Modifier | `Modifier` | Placement; applied to the touch target. |
| `enabled` (Android) | Boolean | true | iOS uses `.disabled(_:)`. |
| `size` | Dp / CGFloat | 44 | Circle diameter. |
| `glassState` (Android) | `RemGlassState?` | null | Backdrop from `rememberRemGlassState()` + `Modifier.remGlassSource(state)`. |

## Third-party
Android blur: [Haze](https://github.com/chrisbanes/haze) `dev.chrisbanes.haze:haze:1.1.1`, **Apache-2.0**, already a
dependency of the Compose library (see `compose/README.md`). It is an `implementation` dependency and does not
appear in the public API.

## Open decisions
- **Figma master and Code Connect.** Code-led; no Figma node yet. `RemGlassIconButton.figma.swift` is an archived
  placeholder (`node-id=TODO`) and there are no parserless templates until a master exists.
- **Chat header controls** stay solid (`backgroundPrimary` circle, 12dp shadow, no hairline) and unchanged here.
  Moving them to glass is a visual change for Samuel to approve.
- **iOS pre-26 fallback** uses `.ultraThinMaterial`; Chat's jump to latest (approved 2026-10-11) uses
  `.regularMaterial`. Align one way or the other.
