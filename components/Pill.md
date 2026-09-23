---
component: Pill
group: general
mirrors: SharedPillView / the single badge in the system
status: draft
rules:
  - id: one-badge
    do: "Use Pill for every badge — status and category alike."
    dont: "Hand-roll badge markup or introduce a second badge component."
    enforced_by: "prose-only (system rule from README)"
  - id: tone-for-status
    do: "Tint status with `tone` (neutral/blue/green/red/orange)."
    dont: "Pass a raw color; use the tone that carries the meaning."
    enforced_by: "no-restricted-syntax: Pill tone must be neutral|blue|green|red|orange"
  - id: dot-or-icon-for-category
    do: "Add a leading `dot` (e.g. a calendar color) or `icon` for category badges."
    dont: "Encode category by inventing new tones."
    enforced_by: "prose-only"
---

# Pill

## Overview
A small rounded label — **the one badge in the system**. Neutral by default; tinted tones cover status
(running, done, overdue…); a `dot` or `icon` covers category/list badges.

## When to use
- Status badges (running/done/failed/overdue).
- Category/list badges (a leading color dot or glyph).

## When *not* to use
- **A tappable action** → `Button`. **A row accessory that navigates** → chevron in `ListRow` trailing.

## Anatomy
`[ optional dot | icon ] label`, in a rounded tinted (or neutral gray) container.

## Variants & states
| Prop | Values |
|---|---|
| `tone` | `neutral` · `blue` · `green` · `red` · `orange` |
| `dot` | leading color string |
| `icon` | leading inline SVG |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use Pill for every badge. | Hand-roll badge markup / add a 2nd badge. |
| Tint status with `tone`. | Pass a raw color. |
| Use `dot`/`icon` for category. | Invent new tones to mean categories. |

## Accessibility
- Don't rely on tone alone to convey status — the label text carries the meaning; tone reinforces it.
- Tones derive from system colors and meet AA against the pill fill.

## Tokens used
- `color.pillBackground` (neutral), `color.system.*` (tones)

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `tone` | `neutral\|blue\|green\|red\|orange` | `neutral` | Color tint |
| `dot` | `string` | — | Leading color dot |
| `icon` | `ReactNode` | — | Leading glyph |
| `children` | `ReactNode` | — | Label |
