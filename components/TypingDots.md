---
component: TypingDots
group: general
mirrors: SharedChatTypingDots (assistant typing indicator)
status: draft
rules:
  - id: indicator-only
    do: "Use it purely as the transient 'assistant is typing' indicator, typically inside a bubble/surface."
    dont: "Use it as a general-purpose loading spinner elsewhere."
    enforced_by: "prose-only"
---

# TypingDots

## Overview
Three softly pulsing dots — the assistant "typing" indicator.

## When to use
- While the assistant is composing a reply, in place of the message.

## When *not* to use
- **General loading** → a spinner in `ContextualMessage`. **A tool running** → `ToolResultCard`
  header state.

## Anatomy
Three dots that pulse in sequence; usually placed inside a secondary `Surface` (bubble).

## Variants & states
No variants — a single animated indicator.

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use as the typing indicator in a bubble. | Repurpose as a global spinner. |
| Remove it as soon as text streams in. | Leave it up alongside the reply. |

## Accessibility
- Purely decorative motion — expose "assistant is typing" as a status message for assistive tech;
  don't rely on the animation alone.
- Respect reduced-motion: the pulse should calm/stop when the user prefers reduced motion.

## Tokens used
- `color.label.tertiary` (dots), placed on a secondary `Surface`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `className`/`id`/`style` | — | — | Passthrough (no configuration) |
