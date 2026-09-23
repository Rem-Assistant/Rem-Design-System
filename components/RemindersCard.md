---
component: RemindersCard
group: toolresultcard
mirrors: RemindersCard (a reminders tool-call result)
status: draft
rules:
  - id: preset-of-toolresultcard
    do: "Use RemindersCard to render a reminders tool result — the ToolResultCard preset for reminders (inset material)."
    dont: "Hand-build a reminders list in a solid Card, or bypass the ToolResultCard header."
    enforced_by: "no-restricted-syntax: RemindersCard accepts reminders, collapsible, defaultExpanded"
---

# RemindersCard

## Overview
A reminders tool-call result — a fill (inset) card listing reminders with status, due date, and
priority. A `ToolResultCard` preset.

## When to use
- Rendering the result of a reminders lookup inline in a conversation.

## When *not* to use
- **A calendar result** → `CalendarEventsCard`. **A single agenda row** → `TaskEventRow`. **A generic
  tool result** → `ToolResultCard`.

## Anatomy
A `ToolResultCard` header ("N reminders") over reminder rows: status · title · due · priority.
Optionally `collapsible`.

## Variants & states
| Prop | Purpose |
|---|---|
| `reminders` | the reminder list (required) |
| `collapsible` / `defaultExpanded` | collapse behavior |

States: expanded, collapsed, empty; per-row done / due / priority.

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use it for reminders tool results. | Hand-build a reminders list in a solid Card. |
| Show status/due/priority per row. | Drop the status affordance. |
| Collapse long lists. | Dump a long list fully expanded. |

## Accessibility
- Convey done/priority with icon + text, not color alone.
- Header count should stand alone for screen-reader users.

## Tokens used
- inherits `ToolResultCard` (inset surface), `color.system.*` (priority/status), `typography.*`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `reminders` | `Reminder[]` | — | Reminders (required) |
| `collapsible` | `boolean` | `false` | Allow collapse |
| `defaultExpanded` | `boolean` | — | Initial state |
