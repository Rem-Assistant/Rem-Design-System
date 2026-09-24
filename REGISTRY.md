# Rem Design System — Component Registry

**Single source of truth.** Before creating anything in the Figma file
(`af4yDqCzp57jds9lkFiIaO`), find it here and **reuse the canonical component** — never
hand-build a duplicate. This mirrors the **Component Index** page in Figma.

## The rule (how we avoid drift)

1. **Check the registry first.** If a component exists, instance the canonical master. Do not
   re-draw it.
2. **One canonical per concept, on its own named page.** No second generation, no bespoke copy
   (the DateNav/Card drift we cleaned up came from breaking this).
3. **Name to match SwiftUI.** `Section` (not Card), `ContentUnavailableView` (not EmptyState),
   `SectionHeader`/`SectionFooter`, etc. The name is the code-connect.
4. **New concept?** Add the row here *and* on the Figma Component Index in the same change.

## Canonical components

| Component | Page | Node | SwiftUI source | Status |
|---|---|---|---|---|
| ListRow (3 slots: **Leading Accessory · Content · Trailing Accessory**) — swappable leading (ContainedIcon/Avatar), re-based to iOS 26 metrics + variables | Rows & Controls | `101:18` | `SharedSettingsView.swift` insetGrouped rows | ✓ canonical (kept — kit Row leading isn't swappable to ContainedIcon) |
| ListRowLabel (default Content: Title/Subtitle) | ListRow | `188:2` | the row's text block | ✓ canonical |
| Avatar (29×29 leading option) | ContainedIcon | `185:2` | circular photo/initials leading | ✓ canonical |
| SectionHeader | Section | `161:68` | `Section { } header: { Text }` | ✓ canonical |
| SectionFooter | ListRow | `161:70` | `Section { } footer: { Text }` | ✓ canonical |
| TaskEventRow (task/event) | TaskEventRow | `46:21` | `TaskEventRowView.swift` | ✓ canonical |
| SuggestedTaskRow (add/move) | SuggestedTaskRow | `48:25` | `SuggestedTaskRow.swift` | ✓ canonical |
| Button (accessory) | Button | `110:47` | `Button(.borderedProminent)` | ✓ canonical |
| Switch (accessory) | Switch | `110:50` | `Toggle().labelsHidden().tint(.green)` | ✓ canonical |
| Chevron (accessory) | Chevron | `110:52` | NavigationLink disclosure | ✓ canonical |
| ContainedIcon | ContainedIcon | `110:54` | `SettingsIcon` | ✓ canonical |
| Accessory/None | Controls | `157:43` | — (no accessory) | ✓ canonical |
| MessageBubble (user/assistant) | MessageBubble | `50:7` | `ChatMessageViews.swift` | ✓ canonical |
| RemComposerBar | RemComposerBar | `53:2` | `RemComposerBar.swift` (used by SharedRemChatView + TaskCommentsSection) | ✓ canonical |
| ConversationView | ConversationView | `71:35` | folds into Chat screen | consolidating |
| VoiceBar (MiniPlayerBar, 6 states) | Screens ⑤ | `160:884` | `MiniPlayerBar.swift` | ✓ canonical |
| TypingDots | TypingDots | `17:3` | `SharedChatTypingDots` | ✓ canonical |
| ThinkingBlock | ThinkingBlock | `63:20` | — | ✓ canonical |
| ToolResultCard | ToolResultCard | `62:2` | — | ✓ canonical |
| ContextualMessage | ContextualMessage | `73:39` | — | ✓ canonical |
| Toast | Toast | `72:24` | — | ✓ canonical |
| CalendarEventsCard | CalendarEventsCard | `67:2` | — | ✓ canonical |
| RemindersCard | RemindersCard | `67:481` | — | ✓ canonical |
| ProposalCard | ProposalCard | `54:55` | — | ✓ canonical |
| DateNavigationHeader | DateNavigationHeader | `43:2` | `SharedAgendaView.swift` | ✓ canonical |
| ContentUnavailableView | States | `108:51` | `ContentUnavailableView` | ✓ canonical |
| ErrorBanner | States | `108:56` | — | ✓ canonical |
| LoadingSkeleton | States | `108:62` | — | ✓ canonical |
| Text (type ramp) | Text | `65:26` | iOS 26 text styles | ✓ canonical |
| Surface / Card → **Section** | Surface / Card | `8:12` / `11:11` | grouped Section container | rename → Section |
| Pill | Pill | `64:14` | — | ✓ canonical |
| DeviceFrame/iPhone (bezel + Screen slot) | Screens | `128:46` | presentation-only | ✓ canonical |
| Navigation Bar | imported | `d29957…` | iOS 26 kit (Apple) | kit |
| Status Bar / Toolbar | imported | — | iOS 26 kit (Apple) | kit |

## Screens (one generation — device-framed on the Screens page)

Built on the canonical components above, housed in `DeviceFrame/iPhone`, organized in labeled
Sections. Legacy standalone templates (old AgendaView/ChatScreen/InboxView/SettingsView) are being
**retired** in favor of these.

| Screen | Node | States |
|---|---|---|
| Settings | `130:44` | root |
| Connectors | `133:192` | connected / not-connected rows |
| About | `134:242` | — |
| Agenda | rebuilding | scheduled · empty · loading (jump-to-today, sort modes to add) |
| Chat | `71:533` | voice-active thread (iOS 26 Toolbar-Top nav, bound, real symbols) ✓ |
| Task detail | `299:2` | root — iOS 26 nav (back+Task), title/date/meta, Last-activity card, notes, composer; bound + real symbols ✓ |
