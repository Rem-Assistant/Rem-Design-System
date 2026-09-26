# Task: TaskDetail template (+ TaskInspector sheet)

## Outcome
- **User outcome:** view/edit a task or event and its date/alert/repeat/duration.
- **Scope:** the TaskDetail screen template + the TaskInspector modal, composed from components.
- **Mode:** Reproduce.
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #6`.

## Design authority
- **Product authority:** `Rem/Sources/Screens/TaskEventView.swift` (detail) + `TaskInspectorSheet`
  (`:961`, `.sheet` + `.presentationDetents([.medium,.large])`).
- **Task artifact (already built):** Figma Task detail `299:2`, TaskInspectorSheet `581:61`.
- **Design-system authority:** kit sheet chrome, `ListRow`, `Pill`, `ContainedIcon`, `ComposerBar`
  ("Continue in chat"), tokens.

## Approved experience
- **Composition:** nav ("Task") → title → DateInfo (clock + date) → Duration/Alert/Repeat badges →
  Last Activity (agent) → Run now / View history → notes → "Continue in chat" composer. Inspector:
  grouped sections (Any Time toggle + Start Date; Alert; Repeat; Duration) + Clear/Done.
- **States:** task vs event; scheduled vs Any Time; running/overdue; completed (strikethrough).
- **Interaction:** tap date row → TaskInspector sheet; Done saves; run/complete/snooze.

## System use
- **Reuse:** kit sheet, ListRow, Pill, ComposerBar, tokens. **Exact:** the three inspector sections from code.
- **Excluded:** task store logic.

## Delivery contract
- **Acceptance:** template + inspector as composed layouts; inspector matches `taskSections`.
- **Evidence:** iOS + Android, light + dark; task + event; inspector open; Preview; Code Connect.

## Approval
- **Status:** approved for Build (Figma exists; reconcile to tokens/components). **Open decisions:** none.
