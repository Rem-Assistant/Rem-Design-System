---
component: ContainedIcon
group: general
mirrors: The settings-row icon (rounded colored container + white glyph)
status: draft
rules:
  - id: token-container-color
    do: "Set the container `color` from a token var (brand blue by default, or a system color)."
    dont: "Pass a raw hex color to the container."
    enforced_by: "prose-only (color is a free string; keep it a token var by convention)"
  - id: standard-sizes
    do: "Use the default 28px in rows; 44px for a larger emphasis icon."
    dont: "Sprinkle arbitrary sizes."
    enforced_by: "prose-only"
---

# ContainedIcon

## Overview
A glyph in a rounded, colored container — the leading icon for settings/list rows. The glyph renders
white and centered on a token-colored fill.

## Native semantic fill contract

SwiftUI and Compose `Subtle` use the gray semantic `backgroundSecondary` container with a **primary**
semantic `labelPrimary` glyph. This follows light/dark appearance without hardcoded black or white.
Settings utility icons select Subtle explicitly. Explicit tinted brand, status and accent treatments
retain their selected color and on-color glyph; they are not converted to Subtle by this rule.
The native token sets own this treatment, and parserless mappings record the foreground token.
The web reference API documented below is separate from the native fill/size APIs.

## When to use
- The `leading` accessory of a `ListRow`.
- Any place needing a small, categorized icon chip (brand blue default; system colors for category).

## When *not* to use
- **A bare glyph with no container** → use the raw icon.
- **A tappable icon button** → wrap the action in `Button` semantics.

## Anatomy
A rounded square (default 28px) filled with `color`, containing a centered white glyph.

## Variants & states
| Prop | Values | Default |
|---|---|---|
| `color` | any CSS color / token var | brand blue |
| `size` | px | 28 (44 for emphasis) |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Color the container from a token var. | Pass a raw hex color. |
| Use 28 in rows, 44 for emphasis. | Use arbitrary sizes. |
| Keep the glyph white/centered. | Recolor the glyph per-instance. |

## Accessibility
- The container color is decorative — don't use color alone to distinguish items; the adjacent
  `title` carries meaning.
- Ensure the white glyph clears contrast on light token fills (e.g. yellow) — prefer stronger fills.

## Tokens used
- `color.brand.blue` (default), `color.system.*` (category), `radius.*` (container)

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `color` | `string` (token var) | brand blue | Container fill |
| `size` | `number` | `28` | Container edge length (px) |
| `children` | `ReactNode` | — | The glyph (white, centered) |
