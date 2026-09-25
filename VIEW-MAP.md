# Rem app — full view map (for the code-first design-system agent)

_Companion to `HANDOFF.md`. Generated 2026-09-25 by scanning the RemClaw tree (not from chat memory),
so it includes views we never discussed. ~312 `View` structs total; this map categorizes them.
Paths are RemClaw-relative. Platform note: **`RemMac/Sources/UI/*` are thin platform wrappers** around
the `Shared/Views/**` surfaces — treat Shared as canonical, Mac as an injection shell._

## How to read this
- **Template** = a full surface (screen, tab root, or modal sheet) that composes components. In the
  design system these become **screen templates with state variants** (loading / empty / populated /
  error / etc.).
- **Component** = a reusable, presentational piece (row, card, badge, pill, bubble, composer…). These
  are the **design-system library**, each with variants + Code Connect.
- **Child view / Section** = internal composition used by exactly one template (sections, headers,
  sub-sheets). Not standalone library components; they travel with their template.
- ⚠️ = on the **gateway runtime-migration deprecation path** (`docs/rebuild/07-REM-RUNTIME-MIGRATION.md`).
  Don't invest design in these; the migration removes them. Still live in code today.
- 🧪 = fixture/debug/preview only — **ignore** (not shipped UI).

---

## 0. App shell / navigation
| View | File | Role |
|---|---|---|
| `RemApp` | `Rem/RemApp.swift` | iOS app entry, scenePhase reconnect |
| `ContentView` | `Rem/ContentView.swift` | iOS root: tab host (Agenda / Chat / Inbox / Settings), brief playback, add-menu |
| `RemMacApp` / `MacAppModel` / `MainWindow` / `MenuBarPopover` / `MacRouter` | `RemMac/Sources/App/*`, `RemMac/Sources/UI/MainWindow.swift`, `MenuBarPopover.swift` | Mac shell: menu-bar + main window, routing |

**Menus you flagged (to inventory as components):**
- **Add-button menu** — `ContentView.swift:991` and `:1040` (the `+` → New Task / Schedule / etc.); also `AgendaView.swift:519`.
- **Bottom-toolbar left menu** (☰) — Agenda toolbar; RemUI equivalent is `BottomToolbar` (line.3.horizontal · mic FAB · +). Chat has its own menus at `SharedRemChatView.swift:1012/4038/4050/4312`.

---

## 1. Onboarding
**Templates** (flow = `OnboardingFlow`, caller `ContentView:46`; steps: signIn → dataSharingConsent → deploying → postSetupActivation):
- `OnboardingFlow` `Rem/Sources/Onboarding/OnboardingFlow.swift` — states: **signIn / consent / deploying⚠️ / activation**
- `AIDataSharingConsentView` `…/AIDataSharingConsentView.swift` — "Privacy by design" consent
- `PostSetupActivationView` `…/PostSetupActivationView.swift`
- `ConversationalCaptureView` `…/ConversationalCaptureView.swift` — first-capture primer
- 🧪 `LaunchRecoveryCopyFixtureView`, `TaskCollaborationFixtureView`
- Component: `SignInButton` (Apple/Google), `EducationPageCard`, `FirstUseHintCard`, `Coachmark` (GuidedFlow overlay)
- **Note:** the `deploying` step + the RemUI "How Rem Works"/Permissions value-props are retired/deprecating; the future onboarding is Connect-apps → Set-up-voice (see HANDOFF).

## 2. Agenda / Tasks
**Templates:**
- `SharedAgendaView` `Shared/Views/Tasks/SharedAgendaView.swift` (+ iOS `Rem/Sources/Screens/AgendaView.swift`, Mac `RemMac/…/MacAgendaView.swift`) — **states:** empty ("No agenda yet" + suggestions) / populated / loading; **pairing ContextualMessage banner** at top; DateNav; DailyBriefCard
- `SharedInboxView` `Shared/Views/Tasks/SharedInboxView.swift` (+ iOS `Rem/Sources/Screens/InboxView.swift`) — **states:** empty / loading / list
- `TaskEventView` `Rem/Sources/Screens/TaskEventView.swift` (+ Mac `MacTaskDetailView`) — task detail; **states:** task vs event; overdue; running
- `TaskInspectorSheet` `Rem/Sources/Screens/TaskEventView.swift:961` — modal: date/alert/repeat/duration (task vs event variants)
- `TasksByListView` `Rem/Sources/Screens/TasksByListView.swift` — tasks filtered by list
- `MacTaskCreateView` `RemMac/…/MacTaskCreateView.swift` — new task/event
- `FocusSessionSetupView`, `FocusSessionTaskPickerSheet` `Rem/Sources/Screens/*` — focus sessions

**Components:**
- `SharedTaskRow` ⭐ (canonical Agenda/Inbox row: time indicator, event color-bar, circle/checkmark, title, pills) · `TaskEventRowView` · `SuggestedTaskRow` · `TaskActivityRow` · `DeviceCalendarEventRow`
- `DailyBriefCard` ⭐ `Rem/Sources/Components/DailyBriefCard.swift` — **brief entry point, stateful:** Read latest brief → Stop (playing) → Read again → Retry. **This is the founder's "entry point to the brief as a component with states."**
- `MiniPlayerBar` — brief playback bar
- Badges/pills: `TaskRunStatusBadge`, `TaskRuntimeBadge`, `TaskDeemphasisBadge`, `TaskStatusIndicator`, `OverduePillView`/`SharedOverduePillView`, `PillView`/`SharedPillView`, `ListBadgeView`
- `DateNavigationHeader` / `SharedDateNavigationHeader` (DateNav — iOS variant has the packed dashes), `InboxHeader`
- `TaskEventRowSkeleton` (loading)
- Menus: `CalendarChipMenu`, `TaskListChipMenu`
- Child/sections: `DateInfoSection`, `EditableAlertRepeatSection`, `EditableTaskDetailsSection`, `TaskCommentsSection`, `TaskCommentRow`, `TaskCommentComposer`, `SharedSuggestionSection`, `SuggestionOverflowSheet`, `DurationSegmentedPicker`, `CreateFolderSheet`, `CreateListSheet`, `CalendarSheet`, `CalendarSelectionSheet`, `TaskSelectorSheet`, `FocusSessionTaskPickerSheet`

## 3. Chat
**Templates:**
- `SharedRemChatView` `Shared/Views/Chat/SharedRemChatView.swift` (~5k lines) — **main chat screen; the state-richest surface:** empty ("Start a conversation" + starters) / streaming / interrupted→retry / error banner / quota banner / waking skeleton / connection-lost. iOS wrapper `Rem/Sources/Chat/RemChatView.swift`; Mac `MacChatWindow`.
- `ChatView` / `RemGatewayChatView` `Packages/RemKit/Sources/RemChatUI/ChatView.swift` — the RemChatUI package chat (cleaner, componentized)
- `ChatHistoryView` `Rem/Sources/Chat/ChatHistoryView.swift` (+ Mac `MacSessionsView`) — sessions list ("Chat Sessions", search, rows); states: loading skeleton / empty / list / error
- `SharedBrowserLiveSheet` / `SharedBrowserLiveView` `Shared/Views/Browser/SharedBrowserLiveView.swift` — browser takeover modal; **states: live / waking / failed / ended / controlling**
- `ChatSessionsSheet`, `ChatSheets` (RemChatUI)

**Components (this is the biggest component family — the founder's "tool calls in chat"):**
- Bubbles: `ChatMessageBubble`/`MessageBubble`, `ChatStreamingAssistantBubble`, `ChatPendingToolsBubble`, `ChatTypingIndicatorBubble`
- Composer: `RemChatComposer` / `RemComposerBar` (Astryx-style attachment composer)
- **Tool-result cards** (router `ToolResultCardView`; 16 `ParsedToolResult` cases — see REGISTRY): `ConfirmationCard`, `ErrorResultCard`, `FallbackResultCard`, `InlineToolResultCard`, `ToolResultCard`, `ToolCallCard`, `CalendarEventsCard`, `RemindersCard`, `DeviceStatusCard`, `DeviceInfoCard`. **(founder: restyle to tokens, do NOT deprecate yet)**
- Lifecycle/status: `ActionLifecycleCard` (Working timeline), `ChatConnectionRecoveryCard`, `ChatNoticeCard`, `ChatNoticeBanner`, `ThinkingIndicator`, `ChatWakingSkeleton`
- Markdown: `AssistantMarkdownView` (code blocks + GFM tables + inline images) `Shared/Views/Chat/AssistantMarkdownRenderer.swift`
- Proposal/poll: `RemProposalCardView`
- Browser: `BrowserLiveCard`, `BrowserLiveSurface`, `RemoteCursor`
- Child/sections: `SharedChatCollapsibleSection`, `SharedChatThinkingContent`, `SessionListLoadingSkeleton`
- 🧪 `AssistantMarkdownFixtureView`, `ChatDayDividerFixtureView`, `AssistantDiagnosticsFixtureView`, `AssistantCustomSchemeLinkFixtureView`, `ToolResultImageFixtureView`, `RemConversationDebugEntryView`

## 4. Settings
**Templates (grouped-list; Shared canonical, Mac = `MacFullSettingsView` + `PermissionsTab`):**
- `SharedSettingsView` ⭐ `Shared/Views/Settings/SharedSettingsView.swift` — root: Profile · **Agent runtime → Rem·Connected ⚠️** · Billing · Permissions · About · Feedback (Share/Send/Report) · Sign Out · Delete
- `SharedAboutView`, `SharedBYOKSettingsView`, `SharedModelsSettingsView`, `SharedVoiceSettingsView`, `SharedMemorySettingsView`, `SharedAutomationsSettingsView`, `SharedCloudBrowserSettingsView`, `SharedComposioConnectionsView`, `SharedWorkspaceFileDetailView`, `SharedTaskRuntimeSettingsView`, `BillingSettingsView`
- Modals: `SharedDeleteAccountSheet`/`DeleteAccountSheet`, `QuotaExceededSheet`, `BYOKAddKeySheet`, `BYOKEditKeySheet`, `ComposioConnectionSheet`, `AppleSubscriptionSheet`, `CalendarSelectionSheet`
- **Components:** `SettingsActionRowLabel`, `ConnectionStatusBadge`, `PermissionStatusBadge`, `PermissionRow`, `CalendarPermissionRow`, `SharedAboutVersionSection`, `SharedAboutLegalLinksSection`; skeletons `ConnectorListLoadingSkeleton`, `CloudBrowserSettingsSkeleton`, `AutomationInputRowSkeleton`, `AutomationOverviewRowSkeleton`, `VoiceSettingsOverviewLoadingSkeleton`; `VoiceSelectionIndicator`, `ElapsedTimeLabel`
- 🧪 `SharedSettingsFixtureView`

## 5. Skills / MCP
**Templates:** `SharedSkillsHomeView`, `SharedSkillBrowseView`, `SharedSkillsSettingsView`, `SharedMcpServersView` (`Shared/Views/Skills/*`)
**Child:** `SharedSkillDetailSheet`, `SharedMcpAddServerSheet`, `SharedSkillRequirementActionSheet`
**Components:** `SharedSkillRow`, `SharedMcpServerRow`, `SkillIconBadge`, `SkillRequirementRowView`

## 6. ⚠️ Gateway (DEPRECATING with runtime migration — do not design fresh)
All under `Shared/Views/Gateway/*` (+ Mac `GatewayChoiceView`): `SharedGatewayDetailView` (the "agent runtime hub" = Settings `509:833`), `SharedGatewayListView`, `SharedGatewayDeviceConnectionsView`, `SharedGatewayRecoveryDestinationView`, `SharedLinkedDevicesView`, `SharedNearbyGatewaysView`, `SharedPendingDevicesView`, `SharedSetupCodeEntryView`, `QRScannerView`, `CloudGatewayDeploySheet`, `SharedClawHubDetailSheet`.
Components here (`SharedGatewayRow`, `SharedDeviceRow`, `SharedPendingDeviceRow`, `SharedGatewayApprovalRequestCard`, `SharedGatewayChoiceCard`, `SharedClawHubResultRow`, `GatewayDisconnectedBanner`, `ConnectionStatusBadge`, `CloudGatewayElapsedTimeLabel`) live or die with the migration. **Only reconcile these after the migration's *Product cleanup* slice; today they still ship.**

## 7. Cross-cutting primitives (the base of the library)
`RemToast` (toast), `RemContextualMessage`/ContextualMessage (icon + title/subtitle + Actions slot), `PillView`/`SharedPillView`, `SignInButton`, `ListRow` (Figma canonical row + accessory swap), badges/indicators (above), skeletons (loading pattern), `DesignTokens.swift` (the token source → tokens.json).

## 8. 🧪 Ignore (fixtures / debug / experimental)
`SomeClawChatView`, `SomeClawSettingsView`, `SomeClawClient` (`Rem/Sources/SomeClaw/*` — experimental), all `*FixtureView`, `*DebugEntryView`, `LaunchRecoveryCopyFixtureView`, `SharedGatewayUpdateTargetsFixtureView`, `SuggestionSourceFixtureView`.

---

## Suggested design-system taxonomy (Fluent-style, for the agent)
```
Tokens            DesignTokens.swift → tokens.json (color/type/spacing/radius)
Primitives        Pill, Badge, Button (SignInButton), Toast, ListRow, Skeleton, TimeLabel, StatusIndicator
Components        Rows (SharedTaskRow…), Cards (DailyBriefCard, tool-result cards…), Bubbles, Composer,
                  ContextualMessage, DateNav, Markdown, Menus (add-menu, ☰), BottomToolbar
Patterns          Empty states, loading skeletons, notice banners, coachmarks
Templates         Onboarding, Agenda, Inbox, TaskDetail+Inspector, Chat, ChatHistory, BrowserTakeover,
                  Settings(+subscreens), Skills   — each with State variants
```
Each Template and Component gets explicit **State variants** (the founder: "screens can have states too")
and a **Code Connect** `.figma.swift` mapping. Start with `DailyBriefCard` (already stateful) as the
reference implementation.
