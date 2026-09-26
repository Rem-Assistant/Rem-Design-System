# Task: ChatHistory template — sessions list

## Outcome
- **User outcome:** browse, search, and reopen past chat sessions.
- **Scope:** the ChatHistory screen template (composed from rows + states).
- **Mode:** Reproduce.
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #6`.

## Design authority
- **Product authority:** `Rem/Sources/Chat/ChatHistoryView.swift` (nav title "Chat Sessions",
  `.searchable("Search conversations")`, session rows title + lastMessagePreview + timestamp; swipe
  Delete/Rename; empty via `ContentUnavailableView`; loading skeleton).
- **Task artifact (already built):** Figma Chat Sessions `438:15`.
- **Design-system authority:** `ListRow` (session row), `Skeleton` (loading — see its packet),
  `ContextualMessage`/empty pattern, search field, tokens.

## Approved experience
- **Composition:** large title "Chat Sessions" → search → grouped session rows.
- **States:** list · **loading** (skeleton) · **empty** ("no conversations" / searching magnifier) ·
  **error** (`ContentUnavailableView` "Couldn't Load Conversations") · row swipe actions.
- **Interaction:** tap → open chat; search filters; swipe → delete/rename; paginate on scroll.

## System use
- **Reuse:** ListRow, Skeleton, ContextualMessage, search field, tokens.
- **Exact:** the four states + row shape from code. **Excluded:** session-store/pagination logic.

## Delivery contract
- **Acceptance:** template with all four states; row = title + preview + timestamp.
- **Evidence:** iOS + Android, light + dark, per state; Preview; Code Connect.

## Approval
- **Status:** approved for Build. **Open decisions:** none.
