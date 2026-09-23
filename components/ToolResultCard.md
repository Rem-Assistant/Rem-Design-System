---
component: ToolResultCard
group: general
mirrors: ToolResultCardView (wrapper for a tool-call result; inset material)
status: draft
rules:
  - id: inset-tool-output
    do: "Use ToolResultCard (inset material) to wrap any tool-call result in a conversation: icon + title header, result rows in the body."
    dont: "Render tool output as a solid Card or as loose text in the thread."
    enforced_by: "prose-only (material vocabulary: inset = quiet tool output)"
  - id: collapsible-for-long
    do: "Make long results `collapsible` and default them collapsed."
    dont: "Dump a long result fully expanded into the thread."
    enforced_by: "no-restricted-syntax: ToolResultCard accepts only its declared props"
---

# ToolResultCard

## Overview
The wrapper for a tool-call result in a conversation (inset material): a translucent fill card with an
icon + title header and a body of result rows. The base for `CalendarEventsCard` and `RemindersCard`.

## When to use
- Presenting the result of any tool call inline in the thread (search, calendar, reminders, etc.).

## When *not* to use
- **The assistant's prose answer** → `MessageBubble`. **Reasoning** → `ThinkingBlock`. **A persistent
  status** → `ContextualMessage`.

## Anatomy
`[ icon (tint) ] title` header (e.g. "3 events · Work") over a body of result rows. Optionally
`collapsible` to header-only.

## Variants & states
| Prop | Values |
|---|---|
| `icon` / `tint` | leading glyph + accent |
| `title` | header summary |
| `collapsible` / `defaultExpanded` | collapse behavior |

States: expanded, collapsed (header-only).

## Do's & don'ts
| ✅ Do | 🚫 Don't |
|---|---|
| Wrap tool output in the inset card. | Render it as a solid Card or loose text. |
| Summarize the result in `title`. | Leave the header generic ("Result"). |
| Make long results collapsible + collapsed. | Dump long output fully expanded. |

## Accessibility
- The header is a disclosure when `collapsible` — expose its state.
- The `title` should stand alone as a summary for screen-reader users who skip the body.

## Tokens used
- inset surface, `tint` from `color.system.*` / `brand.blue`, `radius.*`, `typography.*`

## API
| Prop | Type | Default | Description |
|---|---|---|---|
| `icon` | `ReactNode` | — | Leading glyph |
| `tint` | `string` | — | Glyph accent |
| `title` | `ReactNode` | — | Header summary (required) |
| `collapsible` | `boolean` | `false` | Allow collapse to header |
| `defaultExpanded` | `boolean` | — | Initial expanded state |
