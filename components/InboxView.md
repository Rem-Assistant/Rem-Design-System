---
component: InboxView
group: general
kind: screen-template
mirrors: The Inbox / tasks-list screen
status: draft
composed_of: [TaskEventRow, SuggestedTaskRow]
rules:
  - id: start-from-the-template
    do: "Start a tasks-list screen from this template: large title over grouped task rows and a suggestion."
    dont: "Rebuild the list layout or invent a different grouping affordance."
    enforced_by: "prose-only (screen template)"
---

# InboxView (screen template)

## Overview
The Inbox / tasks-list screen: a large title over grouped task rows and a suggestion. A reference
layout composed from `TaskEventRow` and `SuggestedTaskRow`.

## When to use
- The flat/grouped tasks list (no day paging) — as the starting point for that screen.

## When *not* to use
- **A day-scoped agenda with paging** → `AgendaView`.

## Anatomy
`Text largeTitle` (screen title) → grouped `TaskEventRow`s → `SuggestedTaskRow`(s).

## Composed of
[`TaskEventRow`](TaskEventRow.md) · [`SuggestedTaskRow`](SuggestedTaskRow.md)

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Start here; swap in real tasks. | Rebuild the list layout from scratch. |
| Use the large-title header. | Introduce a different title treatment. |
| Keep suggestions visually distinct. | Blend suggestions into committed rows. |

## Accessibility
- Large title is the screen heading; groups have accessible section labels.

## Tokens used
- `typography.largeTitle`, inherited child tokens, `spacing.*`.

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `width` | `number` | phone column | Fixed template width |
