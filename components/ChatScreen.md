---
component: ChatScreen
group: general
kind: screen-template
mirrors: The full-screen chat
status: draft
composed_of: [MessageBubble, ToolResultCard, ThinkingBlock, TypingDots, ComposerBar]
rules:
  - id: thread-plus-pinned-composer
    do: "Keep the structure: nav bar → scrolling thread (turns, tool results, reasoning, typing) → ComposerBar pinned at the bottom."
    dont: "Float the composer, or rebuild the thread with bespoke bubbles."
    enforced_by: "prose-only (screen template)"
---

# ChatScreen (screen template)

## Overview
The full-screen chat: a nav bar, a scrolling thread (turns, a tool-call result, a reasoning
disclosure, the typing indicator), and the composer pinned at the bottom — at phone size.

## When to use
- The primary chat surface — as the starting point (or an agent-generated chat screen).

## When *not* to use
- **A denser conversation-only region** (no nav bar) → `ConversationView`.

## Anatomy
Nav bar → scrolling thread of `MessageBubble`s, `ToolResultCard`s, `ThinkingBlock`s, and `TypingDots`
→ `ComposerBar` pinned at the bottom.

## Composed of
[`MessageBubble`](MessageBubble.md) · [`ToolResultCard`](ToolResultCard.md) ·
[`ThinkingBlock`](ThinkingBlock.md) · [`TypingDots`](TypingDots.md) · [`ComposerBar`](ComposerBar.md)

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Pin the composer at the bottom. | Float or center the composer. |
| Reuse the thread components. | Rebuild bespoke chat bubbles. |
| Keep nav → thread → composer order. | Reorder the regions. |

## Accessibility
- Thread is a scrollable log; new turns are announced; focus moves sensibly between composer and thread.
- Composer stays reachable above the keyboard.

## Tokens used
- Inherited from its child components; safe-area/padding via `spacing.*`.

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `width` | `number` | phone | Fixed template width |
| `height` | `number` | phone | Fixed template height |
