---
component: MessageBubble
group: general
mirrors: Rem chat message (user bubble / assistant prose)
status: draft
rules:
  - id: role-drives-treatment
    do: "Set `role` — `user` renders a right-aligned brand-blue bubble; `assistant` renders left-aligned plain prose (no bubble)."
    dont: "Wrap assistant text in a bubble, or right-align it."
    enforced_by: "no-restricted-syntax: MessageBubble role must be user|assistant"
---

# MessageBubble

## Overview
One chat message. The user's turn is a right-aligned brand-blue bubble; the assistant's turn is
left-aligned plain prose (no bubble) — matching Rem's chat.

## When to use
- Rendering a single conversational turn in the thread.

## When *not* to use
- **A tool-call result** → `ToolResultCard`. **Reasoning** → `ThinkingBlock`. **A typing indicator**
  → `TypingDots`.

## Anatomy
`user`: right-aligned rounded brand-blue capsule with inverted label. `assistant`: left-aligned prose
at body scale, no container.

## Variants & states
| Prop | Values |
|---|---|
| `role` | `user` · `assistant` |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Let `role` drive alignment + treatment. | Put assistant text in a bubble. |
| Keep assistant turns as plain prose. | Right-align the assistant. |

## Accessibility
- Role is conveyed visually *and* should be exposed to assistive tech (who is speaking), not by color
  alone.
- User bubble uses the brand fill with an inverted label — meets AA.

## Tokens used
- `color.brand.blue` (user fill), `color.label.*` (assistant prose), `radius.*`, `typography.body`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `role` | `user\|assistant` | — | Speaker (drives alignment + treatment) |
| `children` | `ReactNode` | — | Message content |
