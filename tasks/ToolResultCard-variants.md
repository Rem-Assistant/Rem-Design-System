# Task: ToolResultCard variants — cover all 16 tool results

## Outcome
- **User outcome:** every tool-call result in chat reads as one system, restyled to tokens (founder:
  they look out-of-system today — **restyle, do NOT deprecate**).
- **Scope:** add the missing variants to the existing `ToolResultCard` so all 16 `ParsedToolResult`
  cases map to a variant. Not a new base component.
- **Mode:** Extend.
- **Source concern:** `TARGET-COMPONENTS.md §Gaps #6` + `§Tool-result variants`.

## Design authority
- **Product authority:** `Shared/Views/Chat/ToolResultCards/ToolResultCardView.swift` (router, 16
  cases) + `CalendarCard.swift` / `RemindersCard.swift` / `ToolResultPayloads.swift`.
- **Design-system authority:** `ToolResultCard` (inset base), `Pill`, `ContainedIcon`, tokens.
- **Task artifact:** Figma already has `ConfirmationCard`/`ErrorResultCard` `557:31`, `DeviceStatusCard`
  `566:68`, `DeviceInfoCard` `570:86` — reconcile these into ToolResultCard variants.

## Approved experience
- **Variants to add:** `Confirmation` (add/update/delete/create/notify — icon+title+subtitle, tinted),
  `Error` (collapsible warning), `DeviceStatus` (battery SF Symbol + tinted status pills + storage),
  `DeviceInfo` (device SF Symbol + name + model·OS). Existing: `CalendarEvents`, `Reminders`.
- **States:** default · collapsed/expanded (long results default collapsed) · error.
- **Content:** titles/tints per the code router (e.g. calendar=red, reminders=orange, task=blue).

## System use
- **Reuse:** ToolResultCard base, Pill (status pills), ContainedIcon, tokens.
- **Exact:** the icon + tint + collapse rules per case (from the router).
- **Excluded:** deprecation — keep all callers (founder).

## Delivery contract
- **Acceptance:** all 16 cases map to a ToolResultCard variant; one base master; tokens (no raw hex);
  the pill-tint fix (bound variable + 0.12 opacity) applied.
- **Evidence:** iOS + Android, light + dark, one tile per variant; Preview; Code Connect to the router.

## Approval
- **Status:** approved for Build. **Open decisions:** the Gmail `MessageDraftCard` (`458:69`) is
  separate (net-new, no caller — see `tasks/README.md`); not part of this packet.
