---
component: ProposalCard
group: general
mirrors: RemProposalCardView (a task-update proposal in a conversation)
status: draft
rules:
  - id: pending-actionable-resolved-terminal
    do: "Pending shows the change + Approve/Dismiss; resolved states (succeeded/failed/dismissed/stale) show a terminal, non-actionable summary."
    dont: "Leave Approve/Dismiss live on a resolved proposal."
    enforced_by: "no-restricted-syntax: ProposalCard state must be pending|succeeded|failed|dismissed|stale"
  - id: stale-is-stop-nagging
    do: "Use `stale` to mean the proposal is no longer relevant — a quiet terminal state, not an error."
    dont: "Delete/hide it or render `stale` like `failed`."
    enforced_by: "prose-only (product: stale = stop-nagging, not delete)"
---

# ProposalCard

## Overview
One task-update proposal in a conversation. Pending shows the proposed change with Approve / Dismiss;
resolved states show a terminal, non-actionable summary.

## When to use
- Rem proposing a concrete task change inline in chat (mark done, archive, reschedule…).

## When *not* to use
- **An agenda suggestion** → `SuggestedTaskRow`. **A tool result** → `ToolResultCard`.

## Anatomy
`title` → `actionSentence` (the proposed change) → optional quiet `explanation`. Pending: Approve /
Dismiss footer (spinner when `busy`, orange `error` notice). Resolved: terminal summary.

## Variants & states
| Prop | Values |
|---|---|
| `state` | `pending` · `succeeded` · `failed` · `dismissed` · `stale` |
| `busy` | disable actions + spinner |
| `error` | inline orange notice |
| `terminalText` | override resolved summary |

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Keep Approve/Dismiss only while `pending`. | Leave actions live after resolution. |
| Treat `stale` as a quiet terminal state. | Render `stale` like `failed`, or delete it. |
| Show `explanation` (the WHY) quietly. | Bury or omit the rationale. |

## Accessibility
- Announce state transitions (succeeded/failed) to assistive tech.
- `busy` should disable and announce progress; don't let a double-tap double-approve.

## Tokens used
- solid card, `color.brand.blue` (approve), `color.system.orange` (error), `typography.*`,
  `opacity.deemphasized` (stale)

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `title` | `ReactNode` | — | Task the proposal is about |
| `actionSentence` | `ReactNode` | — | The proposed change |
| `explanation` | `ReactNode` | — | Quiet rationale |
| `state` | 5 states | `pending` | Lifecycle |
| `onApprove`/`onDismiss` | `()=>void` | — | Pending actions |
| `busy` | `boolean` | — | Disable + spinner |
| `error` | `ReactNode` | — | Inline error |
| `terminalText` | `ReactNode` | per-state | Resolved summary override |
