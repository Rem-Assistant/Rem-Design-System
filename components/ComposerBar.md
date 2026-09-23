---
component: ComposerBar
group: general
mirrors: RemComposerBar (the chat composer shell)
status: draft
rules:
  - id: presentation-only-slots
    do: "Inject affordances through the slots (`leading`, `trailing`, `send`, `attachments`); the bar is presentation-only."
    dont: "Fork the composer to hardcode a specific button set."
    enforced_by: "no-restricted-syntax: ComposerBar accepts only its declared props"
---

# ComposerBar

## Overview
The Rem composer shell — a vertically growing text field in a rounded, translucent pill, with a
control row (leading · spacer · trailing · send) and an optional attachments strip. Presentation
only; affordances are injected as slots.

## When to use
- The message input at the bottom of a chat/conversation screen.

## When *not* to use
- **A one-line settings text field** → a plain field, not the composer.

## Anatomy
Optional `attachments` strip → growing text field → control row: `leading` · spacer · `trailing` ·
`send`. Grows to `maxRows`, then scrolls.

## Variants & states
| Prop | Purpose |
|---|---|
| `value` / `defaultValue` / `onChange` | controlled or uncontrolled text |
| `onSubmit` | fired on Enter (no Shift) or send |
| `maxRows` | growth cap before scroll |
| `attachments` / `leading` / `trailing` / `send` | slots |

States: empty (placeholder), typing (grows), with attachments/banner.

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Inject affordances via slots. | Fork the shell for a fixed button set. |
| Use `onSubmit` for send-on-Enter. | Re-implement Enter/Shift handling outside. |

## Accessibility
- The field needs an accessible label (via `placeholder` or an external label).
- Slot controls (send, mic, add) each need their own labels — the bar doesn't label them for you.

## Tokens used
- `color.pillBackground` / glass, `radius.*`, `spacing.*`, `typography.body`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `placeholder` | `string` | — | Field placeholder |
| `value`/`defaultValue` | `string` | — | Controlled / initial text |
| `onChange`/`onSubmit` | `(v)=>void` | — | Change / submit handlers |
| `maxRows` | `number` | — | Max rows before scroll |
| `attachments`/`leading`/`trailing`/`send` | `ReactNode` | — | Slots |
