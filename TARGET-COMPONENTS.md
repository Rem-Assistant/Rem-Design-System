# Target component roster — collapse map + gaps

_Bridges `VIEW-MAP.md` (the ~312 shipping SwiftUI views) onto the target roster in
[`components/`](components/README.md) + `SPEC.md`. Proves the roster absorbs the real app, and flags
what it's still missing. Cross-platform framing per SPEC: **shared = tokens + each component's intent;
form diverges to the native idiom** (SwiftUI/iOS, Compose/Material/Android). Mac is out (follows iPad
later). ⚠️ = deprecates with the runtime migration — do not add._

## How to read
For each **target component** (the destination), the **absorbs** column lists the shipping views that
collapse into it. If the roster is missing a home for a real family, it's in **§Gaps**.

## Primitives (7) — exist ✓
| Target | Absorbs (shipping views) |
|---|---|
| **Text** | every ad-hoc `Text(...).font(DesignTokens...)` call |
| **Surface** | base for the four material presets below |
| **Card** (solid) | `Card`, grouped `Section` panels, `RemProposalCardView` shell, `EducationPageCard` |
| **Pill** (the one badge) | `PillView`/`SharedPillView`, `OverduePillView`/`SharedOverduePillView`, `ListBadgeView`, `TaskRunStatusBadge`, `TaskRuntimeBadge`, `TaskDeemphasisBadge`, `ConnectionStatusBadge`, `PermissionStatusBadge`, `ProgressBadgeView`, `SkillIconBadge` |
| **ListRow** | `SharedTaskRow`(settings-context), `SettingsActionRowLabel`, `PermissionRow`, `CalendarPermissionRow`, `SharedDeviceRow`⚠️, `SharedGatewayRow`⚠️, `SharedPendingDeviceRow`⚠️, `SharedMcpServerRow`, `SharedSkillRow`, `SharedClawHubResultRow`⚠️, `SkillRequirementRowView`, `AttachmentRow` |
| **ContainedIcon** | the glyph-in-rounded-tile pattern used in Settings rows, brief, etc. |
| **Button** | `SignInButton` (variant: provider), the recovery/CTA buttons, `ButtonGroup` (Figma `576:31`) |

## Chat & conversation (7) — exist ✓
| Target | Absorbs |
|---|---|
| **MessageBubble** | `ChatMessageBubble`/`MessageBubble`, `ChatStreamingAssistantBubble`, `ChatPendingToolsBubble` |
| **ComposerBar** | `RemChatComposer`, `RemComposerBar`, `TaskCommentComposer` |
| **ContextualMessage** (glass, `tone`+`Actions`) | `ChatConnectionRecoveryCard`, `GatewayDisconnectedBanner`⚠️, `ChatNoticeCard`, `ChatNoticeBanner`, `FirstUseHintCard`, `CalendarPermissionRow`(request form), the pairing/calendar recovery cards (already Figma instances `577:2`/`577:31`), `ChatWakingSkeleton`(empty/loading tone) |
| **ThinkingBlock** | `SharedChatThinkingContent`, `SharedChatCollapsibleSection` |
| **TypingDots** | `ThinkingIndicator`, `ChatTypingIndicatorBubble` |
| **Toast** | `RemToast` |
| **ToolResultCard** (inset; base) | router `ToolResultCardView` + `InlineToolResultCard`/`FallbackResultCard`/`ToolCallCard`/`ToolResultCard`; **needs variants** below |

## Tasks & agenda (4) — exist ✓
| Target | Absorbs |
|---|---|
| **TaskEventRow** (Kind×Leading matrix) | `SharedTaskRow`(agenda/inbox), `TaskEventRowView`, `DeviceCalendarEventRow`, `TaskActivityRow` |
| **SuggestedTaskRow** | `SuggestedTaskRow` |
| **ProposalCard** | `RemProposalCardView` (in-conversation task-update proposal) |
| **DateNavigationHeader** | **`DateNavigationHeader`(iOS, packed dashes = canonical) + `SharedDateNavigationHeader`(Mac) → ONE** (resolves the "shared-but-not-shared" smell) |

## Tool-result variants (under ToolResultCard) — 2 exist, 4 to add
`CalendarEventsCard` ✓ · `RemindersCard` ✓ · **add: `ConfirmationCard`** (add/update/delete/create/notify — Figma `557:31`), **`ErrorResultCard`** (`557:31`), **`DeviceStatusCard`** (`566:68`), **`DeviceInfoCard`** (`570:86`). All 16 `ParsedToolResult` cases then map to a ToolResultCard variant. (Founder: **restyle to tokens, do NOT deprecate.**)

## Screen templates (5) — exist ✓, several to add
`AgendaView` ✓ · `InboxView` ✓ · `SettingsView` ✓ · `ChatScreen` ✓ · `ConversationView` ✓
→ **the iOS `AgendaView` is canonical**; the Mac `SharedAgendaView` folds in or is renamed (no separate Mac track).

---

## §Gaps — real families with no home in the roster yet (add these)
Ranked by value:

1. **DailyBriefCard** ⭐ (`Rem/Sources/Components/DailyBriefCard.swift`) — **the brief entry point the
   founder specifically wants as a stateful component.** States: `Read latest brief` → `Stop`(playing)
   → `Read again`(done) → `Retry`(failure). **Highest priority — it's the Code Connect reference
   implementation.** Not currently in the roster.
2. **Skeleton** (primitive) — one loading-shimmer primitive absorbing `SessionListLoadingSkeleton`,
   `ConnectorListLoadingSkeleton`, `CloudBrowserSettingsSkeleton`, `TaskEventRowSkeleton`,
   `AutomationInputRowSkeleton`, `AutomationOverviewRowSkeleton`, `VoiceSettingsOverviewLoadingSkeleton`,
   `ChatWakingSkeleton`. ~7 bespoke skeletons → one primitive with a shape prop.
3. **Menu** — the **add-button menu** (`ContentView.swift:991/1040`) and the **bottom-toolbar ☰**
   (Agenda). Founder flagged both as un-inventoried. Likely a `Menu` primitive + a `BottomBar`/`TabBar`
   shell component.
4. **PlayerBar** — `MiniPlayerBar` (brief voice playback: Stop / mute / Retry mini-bar). Pairs with
   DailyBriefCard's playing state.
5. **BrowserTakeover** (screen template + parts) — `SharedBrowserLiveSheet` (states live/waking/
   failed/ended/controlling), `BrowserLiveCard`, `BrowserLiveSurface`, `RemoteCursor`. Already built
   in Figma (`556:31`/`562:31`); needs a component/template home.
6. **Screen templates to add:** `OnboardingFlow` (signIn/consent/… with the consent flow the other
   agent rebuilt), `TaskDetail` + `TaskInspectorSheet` (Figma `581:61`), `ChatHistory` (sessions list),
   plus the **app shell** (tab bar + FAB) as a template.
7. **DurationSegmentedPicker** / inline pickers — if kept, a `SegmentedControl` primitive; else fold
   into TaskInspector.

## §Do-not-add (⚠️ deprecating with the runtime migration)
All `Shared/Views/Gateway/*` screens + their rows/cards/banners (`SharedGatewayDetailView`,
`SharedGatewayListView`, `SharedLinkedDevicesView`, `SharedNearbyGatewaysView`, `SharedPendingDevicesView`,
`SharedSetupCodeEntryView`, `QRScannerView`, `SharedGatewayApprovalRequestCard`, `SharedGatewayChoiceCard`,
`CloudGatewayDeploySheet`, `GatewayDisconnectedBanner`, gateway rows). Reconcile only after the
migration's *Product cleanup* slice lands.

## §Ignore (fixtures/experimental) — never components
`SomeClaw*`, all `*FixtureView`, `*DebugEntryView`.

---

## The number
**~312 shipping `View` structs → ~30 library components + ~9 screen templates.** The roster today has
**25 components + 5 templates**; closing §Gaps (DailyBriefCard, Skeleton, Menu/BottomBar, PlayerBar,
BrowserTakeover, 4 ToolResultCard variants) + the missing templates (Onboarding, TaskDetail+Inspector,
ChatHistory, app-shell) reaches the target. That's the destination — the collapses in
`VIEW-MAP.md §Consolidation` are the path to it.
