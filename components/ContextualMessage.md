---
component: ContextualMessage
group: general
mirrors: RemContextualMessage (persistent in-context status card, glass material)
status: draft
rules:
  - id: persistent-vs-transient
    do: "Use ContextualMessage for a persistent, in-context state (connection problem, empty/loading, inline notice)."
    dont: "Use it for a brief confirmation — that's a Toast."
    enforced_by: "prose-only"
  - id: tone-vocabulary
    do: "Set `tone` (info/success/warning/error/neutral) — it picks the glyph and accent."
    dont: "Hand-pick a glyph/color that contradicts the tone."
    enforced_by: "no-restricted-syntax: ContextualMessage tone must be info|success|warning|error|neutral"
---

# ContextualMessage

## Overview
A persistent, in-context status card (glass material): a leading accent glyph, title, optional
subtitle, and an optional footer action row. For connection problems, empty/loading states, and
inline notices.

## When to use
- A state that persists until it changes: "Gateway unreachable", "Connecting…", an empty list, a
  loading region.

## When *not* to use
- **A brief confirmation** ("Task added") → `Toast`.
- **Tool-call output** → `ToolResultCard`.

## Anatomy
`[ accent glyph ] title / subtitle` on a glass surface, with an optional `actions` footer (e.g. a
`recovery` Button).

## Variants & states
| Prop | Values |
|---|---|
| `tone` | `info` · `success` · `warning` · `error` · `neutral` |
| `icon` / `iconColor` | override glyph / accent |
| `title` / `subtitle` / `actions` | content + footer |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use for persistent inline states. | Use for a transient confirmation (use Toast). |
| Let `tone` set glyph + accent. | Contradict the tone with a mismatched glyph. |
| Put recovery in `actions`. | Bury the action elsewhere. |

## Accessibility
- Convey status via glyph + text, not tone color alone.
- If it announces a state change (e.g. error), expose it to assistive tech as a status/alert region.

## Tokens used
- glass surface, `color.system.*` (tone accents), `radius.*`, `typography.*`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `tone` | 5 tones | `neutral`? | Semantic tone (glyph + accent) |
| `icon`/`iconColor` | `ReactNode`/`string` | tone default | Glyph / accent override |
| `title` | `ReactNode` | — | Title (required) |
| `subtitle` | `ReactNode` | — | Detail |
| `actions` | `ReactNode` | — | Footer action row |
