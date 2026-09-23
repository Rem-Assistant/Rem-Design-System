---
component: Toast
group: general
mirrors: RemToast (compact transient notice)
status: draft
rules:
  - id: transient-nonactionable
    do: "Use Toast for a brief, non-blocking confirmation or notice ('Reconnected', 'Task added')."
    dont: "Put actions/buttons in a Toast, or use it for a persistent state (use ContextualMessage)."
    enforced_by: "no-restricted-syntax: Toast accepts variant + message only"
  - id: variant-vocabulary
    do: "Set `variant` (info/success/warning/error) — it sets the glyph and tint."
    dont: "Hand-tint the capsule."
    enforced_by: "no-restricted-syntax: Toast variant must be info|success|warning|error"
---

# Toast

## Overview
A compact, transient, non-blocking toast — a leading semantic glyph + message on a translucent
capsule. For brief confirmations ("Reconnected", "Task added").

## When to use
- A short, auto-dismissing confirmation or notice that doesn't need a response.

## When *not* to use
- **A persistent state or one needing an action** → `ContextualMessage`.
- **A tool result** → `ToolResultCard`.

## Anatomy
`[ semantic glyph ] message` on a translucent capsule. Non-actionable by design.

## Variants & states
| Prop | Values |
|---|---|
| `variant` | `info` · `success` · `warning` · `error` |
| `message` | the text (required) |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Keep it brief and non-actionable. | Put buttons/actions in a Toast. |
| Use `variant` for glyph + tint. | Hand-tint the capsule. |
| Auto-dismiss. | Use it for a state that must persist. |

## Accessibility
- Announce via a polite live region so assistive tech hears it without stealing focus.
- Don't put critical, action-required information in a transient toast.

## Tokens used
- glass capsule, `color.system.*` (variant tint), `typography.footnote`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `variant` | `info\|success\|warning\|error` | `info` | Semantic glyph + tint |
| `message` | `ReactNode` | — | The notice (required) |
