---
component: Surface
group: general
mirrors: The card/panel shell used across Rem surfaces
status: draft
rules:
  - id: material-vocabulary
    do: "Use one of the three materials by meaning: solid = a thing you act on, inset = quiet tool output, glass = floating/translucent status."
    dont: "Invent a new fill treatment or mix material semantics."
    enforced_by: "no-restricted-syntax: Surface material must be solid|inset|glass"
  - id: token-radius-padding
    do: "Use the `radius` / `padding` tokens (default `medium` / `md`)."
    dont: "Hardcode a corner radius or padding value."
    enforced_by: "no-restricted-syntax: Surface radius/padding must be a token; ds_no_hardcoded_corner_radius / ds_no_magic_padding (Swift)"
---

# Surface

## Overview
The card/panel shell — a background layer with a radius and optional hairline border. The base for
cards, sheets, and grouped rows.

## When to use
- As the low-level container when you need explicit control of background/radius/border.
- To build a new kind of panel from the shared material language.

## When *not* to use
- **Standard grouped content** → use `Card` (a Surface preset), not a raw Surface.
- **A badge** → `Pill`. **A row** → `ListRow`.

## Anatomy
A background layer (`material` *or* `background`) + `radius` + optional `bordered` hairline, wrapping
`padding` of content.

## Variants & states
| Prop | Values | Default |
|---|---|---|
| `material` | `solid` · `inset` · `glass` | — (takes precedence over `background`) |
| `background` | `primary` · `secondary` · `tertiary` | — |
| `padding` | `xs`…`xxxl` · `none` | `md` |
| `radius` | `none` · `small` · `medium` · `large` · `xlarge` | `medium` |
| `bordered` | boolean | false |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use the material by meaning (solid/inset/glass). | Invent a new fill treatment. |
| Use `radius`/`padding` tokens. | Hardcode a radius or padding number. |
| Reach for `Card` for standard grouped content. | Rebuild Card's preset by hand each time. |

## Accessibility
- Material fills keep AA text contrast when paired with the standard label colors.
- `glass` (translucent) must still clear contrast over its likely backdrops — verify on a busy scene.

## Tokens used
- `color.background.*`, `color.separator` (border), `radius.*`, `spacing.*`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `material` | `solid\|inset\|glass` | — | Fill treatment (wins over `background`) |
| `background` | `primary\|secondary\|tertiary` | — | Raw background layer |
| `padding` | spacing token \| `none` | `md` | Inner padding |
| `radius` | radius token | `medium` | Corner radius |
| `bordered` | `boolean` | `false` | Hairline separator border |
