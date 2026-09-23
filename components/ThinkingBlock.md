---
component: ThinkingBlock
group: general
mirrors: SharedChatThinkingBlock (collapsible reasoning disclosure)
status: draft
rules:
  - id: quiet-and-collapsed
    do: "Keep it quiet and collapsed by default; use `live` for the still-thinking shimmer."
    dont: "Force reasoning open by default or style it as primary content."
    enforced_by: "prose-only"
---

# ThinkingBlock

## Overview
A collapsible reasoning disclosure — a quiet "thinking" header that expands to the model's reasoning
text. Collapsed by default.

## When to use
- Revealing the assistant's reasoning inline in a conversation, without competing with the answer.

## When *not* to use
- **The answer itself** → `MessageBubble`. **A tool result** → `ToolResultCard`.

## Anatomy
A quiet header (`title`, default "Thought for a moment") that toggles to reveal the reasoning text.
`live` adds a subtle shimmer while still thinking.

## Variants & states
| Prop | Values |
|---|---|
| `title` | header label |
| `defaultExpanded` | start open |
| `live` | shimmer while thinking |

States: collapsed (default), expanded, live.

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Keep it collapsed + quiet by default. | Force reasoning open above the answer. |
| Use `live` for the in-progress shimmer. | Leave a stale shimmer after completion. |

## Accessibility
- The header is a disclosure control — expose expanded/collapsed state to assistive tech.
- The `live` shimmer is decorative; also convey "thinking" via the label text.

## Tokens used
- `color.label.secondary` (quiet header), `typography.footnote/caption1`, `opacity.deemphasized`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `title` | `ReactNode` | "Thought for a moment" | Collapsed header |
| `children` | `ReactNode` | — | Reasoning text |
| `defaultExpanded` | `boolean` | `false` | Start expanded |
| `live` | `boolean` | `false` | Shimmer while thinking |
