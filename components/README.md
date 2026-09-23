# Components

All 25 Rem components, each documented from the [template](../templates/COMPONENT.md): overview,
when-to-use / when-not, anatomy, variants & states, do's & don'ts (also enforced as rules), a11y,
tokens, and API. Each page's `rules` front-matter is the single source for both the human do's/don'ts
and the machine adherence rule (see [`../SPEC.md`](../SPEC.md) §4).

## Primitives
- [Text](Text.md) — every typographic role
- [Surface](Surface.md) — the card/panel shell (solid / inset / glass)
- [Card](Card.md) — the standard grouped-content surface preset
- [Pill](Pill.md) — the one badge in the system
- [ListRow](ListRow.md) — the settings/list row
- [ContainedIcon](ContainedIcon.md) — glyph in a rounded colored container
- [Button](Button.md) — the tappable action

## Chat & conversation
- [MessageBubble](MessageBubble.md) — one chat turn (user bubble / assistant prose)
- [ComposerBar](ComposerBar.md) — the composer shell
- [ContextualMessage](ContextualMessage.md) — persistent in-context status (glass)
- [ThinkingBlock](ThinkingBlock.md) — collapsible reasoning
- [TypingDots](TypingDots.md) — typing indicator
- [Toast](Toast.md) — transient, non-blocking notice
- [ToolResultCard](ToolResultCard.md) — tool-call result wrapper (inset)

## Tasks & agenda
- [TaskEventRow](TaskEventRow.md) — the primary agenda task/event row
- [SuggestedTaskRow](SuggestedTaskRow.md) — a proposed (not-yet-real) task
- [ProposalCard](ProposalCard.md) — an in-conversation task-update proposal
- [DateNavigationHeader](DateNavigationHeader.md) — the agenda day header

## Tool-result cards
- [CalendarEventsCard](CalendarEventsCard.md) — calendar tool result
- [RemindersCard](RemindersCard.md) — reminders tool result

## Screen templates
Composed layouts — the on-rails starting points for whole screens.
- [AgendaView](AgendaView.md) — day header → rows → suggestion
- [InboxView](InboxView.md) — large title → grouped rows → suggestion
- [SettingsView](SettingsView.md) — grouped rows with icons + destructive action
- [ChatScreen](ChatScreen.md) — nav → thread → pinned composer
- [ConversationView](ConversationView.md) — the canonical conversation composition
