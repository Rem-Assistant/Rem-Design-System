---
component: ListRow
group: general
mirrors: SharedDeviceRow / SharedGatewayRow / SharedSkillRow (the settings/list row pattern)
status: draft
rules:
  - id: use-listrow-for-rows
    do: "Use ListRow for settings/list rows: leading icon, title + subtitle, trailing accessory."
    dont: "Hand-build a row from an HStack/flex with ad-hoc spacing."
    enforced_by: "prose-only"
  - id: group-in-card
    do: "Group rows in a `Card padding=\"none\"`; hide the last row's separator."
    dont: "Stack bare rows with manual dividers."
    enforced_by: "prose-only"
---

# ListRow

## Overview
The settings/list row shared across device, gateway, and skill rows: an optional leading icon, a
title + subtitle, and a trailing accessory.

## When to use
- Any settings or list row: a labeled item with an optional icon, detail, and trailing control.

## When *not* to use
- **A standalone primary action** → `Button`. **A badge** → `Pill`.

## Anatomy
`[ leading (ContainedIcon) ] title / subtitle … trailing (chevron | Pill | Button | value)`, with an
optional hairline `separator` beneath.

## Variants & states
| Prop | Values |
|---|---|
| `leading` | node (typically `ContainedIcon`) |
| `title` | node (required) |
| `subtitle` | node |
| `trailing` | node (chevron/Pill/Button/value) |
| `separator` | boolean |

States: default, and (via trailing content) navigable (chevron) / actionable (Button) / status (Pill).

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Use ListRow for settings/list rows. | Hand-build rows from raw flex + spacing. |
| Group in `Card padding="none"`. | Stack bare rows with manual dividers. |
| Hide the last row's `separator`. | Leave a dangling divider under the final row. |

## Accessibility
- The whole row should be one accessibility element when it navigates; expose the trailing control
  separately only when it's independently actionable.
- Trailing chevron is decorative — the title conveys the destination.

## Tokens used
- `spacing.*` (row insets), `color.separator` (hairline), `color.label.*` (title/subtitle)

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `leading` | `ReactNode` | — | Leading accessory (usually `ContainedIcon`) |
| `title` | `ReactNode` | — | Primary label (required) |
| `subtitle` | `ReactNode` | — | Secondary label |
| `trailing` | `ReactNode` | — | Trailing accessory |
| `separator` | `boolean` | — | Show hairline under the row |
