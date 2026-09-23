---
component: Text
group: general
mirrors: SwiftUI Text + DesignTokens.Typography / DesignTokens.Color.label*
status: draft
rules:
  - id: use-variant-not-manual-font
    do: "Set the type role with `variant`; it maps 1:1 to a DesignTokens.Typography role."
    dont: "Apply a raw font size/weight to Text or wrap it to restyle."
    enforced_by: "no-restricted-syntax: Text variant must be one of the 12 roles (web); ds_no_raw_font_size (Swift)"
  - id: use-color-roles
    do: "Pick a label role with `color` (primary/secondary/tertiary) or brand/brandOnFill."
    dont: "Pass a raw hex/CSS color to Text."
    enforced_by: "no-restricted-syntax: Text color must be primary|secondary|tertiary|brand|brandOnFill"
  - id: brand-on-fill-for-tinted-on-gray
    do: "Use `color=\"brandOnFill\"` for brand-tinted text sitting on a gray fill (it clears AA in dark mode)."
    dont: "Use plain `brand` on a gray pill — it fails contrast in dark mode."
    enforced_by: "prose-only"
---

# Text

## Overview
One component for every typographic role in Rem, so headings, body, captions, and chat text stay on
the same scale across iOS, macOS, and web. Mirrors SwiftUI `Text` styled with `DesignTokens.Typography`.

## When to use
- Any text in the UI — always, so type roles stay consistent and Dynamic Type-aware.

## When *not* to use
- **A tappable label** → that's a `Button`, not styled `Text`.
- **A status label** → that's a `Pill`.

## Anatomy
A single text node rendered at a type `variant` and a label `color`. Block (`<p>`) for prose, inline
(`<span>`) when composed inside a line.

## Variants & states
| Prop | Values |
|---|---|
| `variant` | `largeTitle` · `title1` · `title1Bold` · `title3` · `title3Bold` · `body` · `bodyBold` · `subheadline` · `footnote` · `caption1` · `caption1Bold` · `chatCode` |
| `color` | `primary` · `secondary` · `tertiary` · `brand` · `brandOnFill` |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Set the role with `variant`. | Apply a raw font size/weight. |
| Choose a label role with `color`. | Pass a raw hex color. |
| Use `brandOnFill` for brand text on gray. | Use `brand` on a gray fill (fails dark-mode AA). |

## Accessibility
- **Dynamic Type:** roles map to Apple `Font.TextStyle`, so text scales with the user's size setting
  (default sizes equal Apple's, so the base look is unchanged).
- **Contrast:** `primary/secondary/tertiary` are the system label colors (AA by construction);
  `brandOnFill` is the AA-safe brand variant on gray fills.

## Tokens used
- `typography.roles.*`, `color.label.*`, `color.brand.blue` / `blueOnFill`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `variant` | 12 type roles | `body` | Typographic role |
| `color` | `primary`…`brandOnFill` | `primary` | Label color role |
| `as` | HTML tag | `p`/`span` | Element to render |
| `children` | `ReactNode` | — | Text content |
