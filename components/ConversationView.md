---
component: ConversationView
group: general
kind: screen-template
mirrors: Rem's conversation region
status: draft
composed_of: [MessageBubble, ThinkingBlock, ToolResultCard, ProposalCard, ComposerBar]
rules:
  - id: canonical-conversation-composition
    do: "Use this as the canonical conversation composition: user + assistant turns, a reasoning disclosure, a tool-result card, an inline proposal, and the composer."
    dont: "Improvise a different arrangement of chat elements."
    enforced_by: "prose-only (screen template)"
---

# ConversationView (screen template)

## Overview
The conversation region: user + assistant turns, a reasoning disclosure, a tool-call result card, an
inline proposal, and the composer. A reference layout of Rem's chat.

## When to use
- Embedding Rem's conversation inside another surface (no full nav bar) — the canonical composition.

## When *not* to use
- **The full chat screen with nav bar** → `ChatScreen`.

## Anatomy
`MessageBubble` (user/assistant) turns, interleaved with `ThinkingBlock`, `ToolResultCard`, and
`ProposalCard`, ending in `ComposerBar`.

## Composed of
[`MessageBubble`](MessageBubble.md) · [`ThinkingBlock`](ThinkingBlock.md) ·
[`ToolResultCard`](ToolResultCard.md) · [`ProposalCard`](ProposalCard.md) · [`ComposerBar`](ComposerBar.md)

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use the canonical element set + order. | Improvise a different arrangement. |
| Place proposals inline with turns. | Hoist proposals into a separate panel. |
| Reuse the child components. | Rebuild bespoke variants. |

## Accessibility
- Reading order follows the visual turn order; reasoning/tool disclosures expose expand state.

## Tokens used
- Inherited from its child components; padding via `spacing.*`.

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `width` | `number` | phone column | Fixed template width |
