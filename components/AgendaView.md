---
component: AgendaView
group: general
kind: screen-template
mirrors: The Agenda screen
status: draft
composed_of: [DateNavigationHeader, TaskEventRow, SuggestedTaskRow]
rules:
  - id: start-from-the-template
    do: "Start a new Agenda-like screen from this template and swap in real data; keep the composition (day header → rows → suggestion)."
    dont: "Re-lay-out the agenda from scratch or reorder its regions arbitrarily."
    enforced_by: "prose-only (screen template — the on-rails starting point)"
---

# AgendaView (screen template)

## Overview
The Agenda screen: a day header over a list of task/event rows and a suggestion. A reference layout
composed from `DateNavigationHeader`, `TaskEventRow`, and `SuggestedTaskRow`.

## When to use
- As the **starting point** for the day-scoped agenda (or when an agent generates an agenda screen)
  — swap in real data, keep the structure.

## When *not* to use
- **A flat task list with no day paging** → `InboxView`.

## Anatomy
`DateNavigationHeader` (day paging) → a list of `TaskEventRow`s (scheduled + unscheduled) → one or
more `SuggestedTaskRow`s (proposals).

## Composed of
[`DateNavigationHeader`](DateNavigationHeader.md) · [`TaskEventRow`](TaskEventRow.md) ·
[`SuggestedTaskRow`](SuggestedTaskRow.md)

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Start here and swap in real data. | Re-lay-out the agenda from scratch. |
| Keep header → rows → suggestion order. | Scatter suggestions among committed rows. |
| Reuse the child components as-is. | Fork their internals for this screen. |

## Accessibility
- One scannable vertical list; the day header announces the current date.
- Suggestions are clearly distinct (dashed ring) from committed rows.

## Tokens used
- Inherited from its child components; page padding via `spacing.*`.

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `width` | `number` | phone column | Fixed template width |
