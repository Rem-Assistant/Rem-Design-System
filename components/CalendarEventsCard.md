---
component: CalendarEventsCard
group: toolresultcard
mirrors: CalendarEventsCard (a calendar tool-call result)
status: draft
rules:
  - id: preset-of-toolresultcard
    do: "Use CalendarEventsCard to render a calendar tool result — it's the ToolResultCard preset for events (inset material)."
    dont: "Hand-build an events list in a solid Card, or bypass the ToolResultCard header."
    enforced_by: "no-restricted-syntax: CalendarEventsCard accepts events, calendarName, collapsible, defaultExpanded"
---

# CalendarEventsCard

## Overview
A calendar tool-call result — a fill (inset) card listing events with time, a calendar color bar, and
title. A `ToolResultCard` preset.

## When to use
- Rendering the result of a calendar lookup inline in a conversation.

## When *not* to use
- **A single agenda row** → `TaskEventRow`. **A reminders result** → `RemindersCard`. **A generic
  tool result** → `ToolResultCard`.

## Anatomy
A `ToolResultCard` header ("N events · <calendarName>") over event rows: time · color bar · title.
Optionally `collapsible`.

## Variants & states
| Prop | Purpose |
|---|---|
| `events` | the event list (required) |
| `calendarName` | header suffix after the count |
| `collapsible` / `defaultExpanded` | collapse behavior |

States: expanded, collapsed, empty (no events).

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use it for calendar tool results. | Hand-build an events list in a solid Card. |
| Summarize count + calendar in the header. | Leave the header generic. |
| Collapse long lists. | Dump a long list fully expanded. |

## Accessibility
- Each event row reads as time + title + calendar; don't rely on the color bar alone for calendar.
- Header count/summary should stand alone for screen-reader users.

## Tokens used
- inherits `ToolResultCard` (inset surface), `color.system.*` (calendar colors), `typography.*`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `events` | `CalendarEvent[]` | — | Events (required) |
| `calendarName` | `string` | — | Header suffix |
| `collapsible` | `boolean` | `false` | Allow collapse |
| `defaultExpanded` | `boolean` | — | Initial state |
