---
component: SuggestedTaskRow
group: general
mirrors: SuggestedTaskRow (a proposed task in the Agenda)
status: draft
rules:
  - id: always-show-why
    do: "Always give the `subtitle` — the attribution / WHY line (what the suggestion is based on)."
    dont: "Show a bare suggested title with no reason."
    enforced_by: "prose-only (product: proposals must explain themselves)"
  - id: not-yet-real
    do: "Keep the dashed-ring 'not yet real' treatment; offer accept (`onAccept`) and dismiss (`onDismiss`)."
    dont: "Style a suggestion like a committed TaskEventRow."
    enforced_by: "no-restricted-syntax: SuggestedTaskRow action must be add|move"
---

# SuggestedTaskRow

## Overview
A proposed task in the Agenda. Mirrors `TaskEventRow`, but the time slot becomes an accept CTA, the
chevron becomes a dismiss ✕, and a dashed ring marks it "not yet real".

## When to use
- A Rem-proposed task the user can accept or dismiss (add a new task, or move an overdue one).

## When *not* to use
- **A committed task/event** → `TaskEventRow`. **A conversational task-update proposal** →
  `ProposalCard`.

## Anatomy
`[ accept CTA (+ add / ↱ move) ] title / subtitle(WHY)  [ ✕ dismiss ]`, inside a dashed "not yet
real" ring.

## Variants & states
| Prop | Values |
|---|---|
| `action` | `add` (+) · `move` (↱) |
| `title` / `subtitle` | task + WHY line |
| `onAccept` / `onDismiss` | handlers |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Always show the WHY in `subtitle`. | Suggest with no reason. |
| Keep the dashed "not yet real" look. | Style it like a committed row. |
| Offer accept + dismiss. | Auto-commit a suggestion. |

## Accessibility
- The dashed ring is decorative — the label must state it's a suggestion.
- Accept and dismiss are distinct controls with their own labels.

## Tokens used
- dashed ring (`color.separator`), `color.brand.blue` (accept), `spacing.*`, `typography.*`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `action` | `add\|move` | `add` | Left CTA kind |
| `title` | `ReactNode` | — | Suggested task |
| `subtitle` | `ReactNode` | — | Attribution / WHY |
| `onAccept` / `onDismiss` | `()=>void` | — | Accept / dismiss |
