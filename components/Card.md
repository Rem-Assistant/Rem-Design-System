---
component: Card
group: general
mirrors: A Surface preset (secondary background, medium radius, hairline border)
status: draft
rules:
  - id: card-for-grouped-content
    do: "Use Card as the standard container for grouped content; set `padding=\"none\"` and nest ListRows for a grouped list."
    dont: "Rebuild a bordered secondary panel by hand, or nest a Card in a Card."
    enforced_by: "prose-only"
  - id: material-and-radius-tokens
    do: "Keep the material vocabulary (solid/inset/glass) and token radius (default medium)."
    dont: "Invent a fill or hardcode a radius."
    enforced_by: "no-restricted-syntax: Card material/radius/padding/background must be tokens"
---

# Card

## Overview
A `Surface` preset — secondary background, medium radius, hairline border — the default container for
grouped content. Same prop surface as `Surface`.

## When to use
- Grouped content: a titled panel, a settings group, a list of `ListRow`s (`padding="none"`).
- Any standard "boxed" region — reach for Card before a raw Surface.

## When *not* to use
- **A floating status message** → `ContextualMessage` (glass) or `Toast`.
- **Quiet tool output** → `ToolResultCard` (inset).
- **A low-level custom panel** → drop to `Surface`.

## Anatomy
A bordered secondary Surface at `medium` radius wrapping `padding` of content. For grouped lists:
`Card padding="none"` + a stack of `ListRow`s (last one `separator={false}`).

## Variants & states
Same as `Surface`: `material` (`solid`/`inset`/`glass`), `background`, `padding` (default `md`),
`radius` (default `medium`), `bordered`.

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use Card for standard grouped content. | Hand-roll a bordered secondary panel. |
| `padding="none"` + `ListRow`s for a grouped list. | Add ad-hoc padding around each row. |
| Keep the material set + token radius. | Nest a Card inside a Card. |

## Accessibility
- Inherits Surface's contrast guarantees; ensure nested content uses label-color tokens.

## Tokens used
- `color.background.secondary`, `color.separator`, `radius.medium`, `spacing.*`

## API
Identical to [`Surface`](Surface.md): `material`, `background`, `padding`, `radius`, `bordered`, `as`,
`children`, `className`, `id`, `style`.
