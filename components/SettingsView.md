---
component: SettingsView
group: general
kind: screen-template
mirrors: The Settings screen
status: draft
composed_of: [Card, ListRow, ContainedIcon, Button]
rules:
  - id: grouped-rows-in-cards
    do: "Group settings into `Card padding=\"none\"` blocks of `ListRow`s with `ContainedIcon` leadings; put destructive actions in a `Button` with role=\"destructive\"."
    dont: "Lay out settings as ad-hoc stacks or hand-styled rows."
    enforced_by: "prose-only (screen template)"
---

# SettingsView (screen template)

## Overview
The Settings screen: grouped rows with contained icons, values, and a destructive action. A reference
layout composed from `Card`, `ListRow`, `ContainedIcon`, and `Button`.

## When to use
- Any settings/config screen — as the starting point for grouping and row density.

## When *not* to use
- **A data list (tasks/events)** → `InboxView` / `AgendaView`.

## Anatomy
Stacked `Card padding="none"` groups, each a set of `ListRow`s (leading `ContainedIcon`, trailing
value/chevron/Pill), with a destructive `Button` (`role="destructive"`) at the end.

## Composed of
[`Card`](Card.md) · [`ListRow`](ListRow.md) · [`ContainedIcon`](ContainedIcon.md) · [`Button`](Button.md)

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Group rows in `Card padding="none"`. | Lay out settings as ad-hoc stacks. |
| Use `ContainedIcon` leadings. | Mix icon styles per row. |
| Destructive action = `role="destructive"`. | Hand-tint a red button. |

## Accessibility
- Grouped sections have labels; destructive actions are clearly distinguished and confirmed.
- On macOS, match the Native reference row density (see the app repo's CLAUDE.md principle 6).

## Tokens used
- Inherited from `Card`/`ListRow`/`ContainedIcon`/`Button`; group spacing via `spacing.*`.

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `width` | `number` | phone column | Fixed template width |
