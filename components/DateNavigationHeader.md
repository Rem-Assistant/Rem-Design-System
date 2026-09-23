---
component: DateNavigationHeader
group: general
mirrors: SharedDateNavigationHeader (the Agenda day header)
status: draft
rules:
  - id: relative-plus-full-date
    do: "Show the relative `label` (Today/Tomorrow/weekday) over the full `date`; hide the Today jump when `isToday`."
    dont: "Show only a raw date with no relative label, or keep a redundant Today jump on today."
    enforced_by: "no-restricted-syntax: DateNavigationHeader accepts only its declared props"
---

# DateNavigationHeader

## Overview
The Agenda day header: ‹ prev, the centered relative date + full date, next ›, and an optional Today
jump.

## When to use
- The header of a day-scoped view (Agenda) where the user pages between days.

## When *not* to use
- **A screen title** → a `Text` `largeTitle`. **A settings header** → the `SettingsView` template.

## Anatomy
`‹ prev   [ label / full date ]   next ›`, with an inline "Today" jump when not already on today.

## Variants & states
| Prop | Purpose |
|---|---|
| `label` / `date` | relative label + full date |
| `isToday` | hides the Today jump |
| `onPrev` / `onNext` / `onToday` | navigation |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Pair a relative `label` with the full `date`. | Show a bare date with no relative label. |
| Hide Today when `isToday`. | Keep a redundant Today jump on today. |

## Accessibility
- Prev/next/Today are buttons with clear labels ("Previous day", "Next day", "Jump to today").
- Announce the new date when the user pages.

## Tokens used
- `typography.title3`/`footnote`, `color.label.*`, `color.brand.blue` (controls), `spacing.*`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `label` | `ReactNode` | — | Relative label |
| `date` | `ReactNode` | — | Full date line |
| `isToday` | `boolean` | — | Hide the Today jump |
| `onPrev`/`onNext`/`onToday` | `()=>void` | — | Navigation |
