---
component: TaskEventRow
group: general
mirrors: TaskEventRowView (the primary agenda task/event row)
status: draft
rules:
  - id: one-row-tasks-and-events
    do: "Use TaskEventRow for both tasks and calendar events in the agenda; `time` fills the fixed 64px leading slot, `done` renders the task status circle."
    dont: "Build separate bespoke rows for tasks vs events."
    enforced_by: "no-restricted-syntax: TaskEventRow accepts only its declared props"
  - id: badges-via-props
    do: "Express calendar / list / duration / overdue through the provided props."
    dont: "Hand-roll Pills inside the row for those badges."
    enforced_by: "prose-only"
  - id: deemphasis-once
    do: "For a receded (done/blocked/stale) row, apply the deemphasized opacity ONCE."
    dont: "Stack dimming (two applications multiply to ~0.30 and read as broken)."
    enforced_by: "prose-only (DesignTokens.Opacity.deemphasized)"
---

# TaskEventRow

## Overview
The primary agenda row (for tasks *and* calendar events): a fixed-width time indicator, a status
circle, the title, and a row of badges (calendar, list, duration, overdue).

## When to use
- Any task or event line in the Agenda / a day list.

## When *not* to use
- **A settings/config row** → `ListRow`. **A proposed (not-yet-real) task** → `SuggestedTaskRow`.

## Anatomy
`[ time (64px) | status circle ] title  [ calendar · list · duration · overdue badges ]`, with an
optional `eventColor` bar down the left for events.

## Variants & states
| Prop | Purpose |
|---|---|
| `time` | scheduled time (omit for unscheduled task) |
| `done` | task completion (renders status circle; ignored for events) |
| `eventColor` | left color bar (events) |
| `calendar` / `list` / `duration` / `overdue` | badges |

States: scheduled/unscheduled, done (receded), overdue.

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use one row type for tasks + events. | Build bespoke task vs event rows. |
| Set badges via props. | Hand-roll Pills in the row. |
| Deemphasize a done/blocked row once. | Stack dimming (reads as broken). |

## Accessibility
- Convey `done` / `overdue` with the status circle + text, not color alone.
- The receded opacity (0.55) must still clear text contrast — it marks "quiet", not "disabled".

## Tokens used
- `spacing.*`, `color.system.*` (badges/event color), `color.label.*`, `opacity.deemphasized`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `time` | `ReactNode` | — | Time in the 64px slot |
| `title` | `ReactNode` | — | Row title (required) |
| `done` | `boolean` | — | Task completion (status circle) |
| `eventColor` | `string` | — | Left color bar (events) |
| `calendar` | `{name,color}` | — | Calendar badge |
| `list` | `string` | — | List badge |
| `duration` | `string` | — | Duration/range pill |
| `overdue` | `boolean` | — | Overdue pill |
