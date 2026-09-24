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
| TaskEventRow (task/event) — Row descendant; props **Kind**, **Leading** (Time/Schedule/Clock), **Pills** (bool) | Tasks & Agenda | `46:21` | `TaskEventRowView.swift` (taskContent) | ✓ canonical |
| SuggestedTaskRow (add/move) | SuggestedTaskRow | `48:25` | `SuggestedTaskRow.swift` | ✓ canonical |
| Button — **Style** = Primary (filled `.label`, login CTA) / **Primary Blue** (standard emphasis) / **Primary Gradient** (commit emphasis, blue→purple) / CTA (accent text) / Connect (capsule) / Destructive (red text). **Emphasis tier** (Neutral·black / Standard·blue / Commit·gradient) per the Muse audit — belongs in `RemButtonTokenSet`. | Primitives | `377:8` | `RemSettingsCTAButtonStyle.swift` — `RemPrimaryActionButtonStyle` (Primary), `RemSettingsCTAButtonStyle` (CTA/Destructive), `RemRowConnectCTA` (Connect); blue/gradient tiers are new (design-leads-code, see EVOLUTION.md) | ✓ canonical |
| PermissionStatusBadge — **Status** = Enabled / Denied / Limited / Not Set (dot + label) | Rows & Controls | `383:14` | `PermissionUtils.swift` `PermissionStatusBadge` | ✓ canonical |
| StatusChevron (trailing accessory: badge + chevron) | Rows & Controls | `383:15` | permission-row trailing (`SettingsView.swift`) | ✓ canonical |
| Switch (accessory) | Switch | `110:50` | `Toggle().labelsHidden().tint(.green)` | ✓ canonical |
| Chevron (accessory) | Chevron | `110:52` | NavigationLink disclosure | ✓ canonical |
| ContainedIcon (colored square + white **Symbol** glyph prop) | Rows & Controls | `110:54` | `SettingsIcon` | ✓ canonical |
| Accessory/None | Controls | `157:43` | — (no accessory) | ✓ canonical |
| Accessory/Value (right-aligned detail text; **Value** text prop) | Rows & Controls | `389:5` | title+value settings rows (e.g. Billing "Plan · Free") | ✓ canonical |
| Avatar (leading; 29 default, 44 in profile row) | ContainedIcon | `185:2` | `SharedSettingsView.swift` `profileRow` fallbackAvatar | ✓ canonical |
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
| Pill | Pill | `64:14` | — | ✓ canonical |
| DeviceFrame/iPhone (bezel + Screen slot + HomeIndicator) | Screens | `128:46` | presentation-only | ✓ canonical |
| HomeIndicator (bottom safe-area handle) | Platform Controls | `333:102` | system safe-area overlay | ✓ canonical |
| RemFaceMark (brand face; **Mode** = idle / thinking) | Primitives | `362:7` | `RemFaceMark.swift` (CustomFaceShape) | ✓ canonical |
| Navigation Bar | imported | `d29957…` | iOS 26 kit (Apple) | kit |
| Status Bar / Toolbar | imported | — | iOS 26 kit (Apple) | kit |

## Screens (one generation — device-framed on the Screens page)

Built on the canonical components above, housed in `DeviceFrame/iPhone`, organized in labeled
Sections. Legacy standalone templates (old AgendaView/ChatScreen/InboxView/SettingsView) are being
**retired** in favor of these.

| Screen | Node | States |
|---|---|---|
| Settings | `130:44` | root — **profile row = ListRow** (Avatar leading + name/email), rest ListRow |
| Connectors | `133:192` | connected / not-connected rows |
| About | `134:242` | — |
| Billing & Usage | `341:862` | Free plan — **Plan row = ListRow + Value**, Usage progress rows custom (accepted), SectionHeaders — `BillingSettingsView.swift` |
| Permissions | `349:905` | 3 sections — **all rows = ListRow + StatusChevron trailing**, SectionHeader/Footer — `SettingsView.swift` DevicePermissionsView |
| Agenda | rebuilding | scheduled · empty · loading (jump-to-today, sort modes to add) |
| Chat | `71:533` | voice-active thread (iOS 26 Toolbar-Top nav, bound, real symbols) ✓ |
| Task detail | `299:2` | root — iOS 26 nav (back+Task), title/date/meta, Last-activity card, notes, composer; bound + real symbols ✓ (page: **Task & Events**) |
| New Task or Event | `404:5` | create mode — Cancel/Save nav, **New Task / New Event** segmented picker, Title + dashed-circle status indicator, "Set date, time, repeat" card, notes — `TaskEventView` (isNewTask) |
| Privacy — "Privacy by design" | `410:16` | Onboarding page — hero (lock.shield.fill on brand tile), title/body, Terms/Privacy consent rows, **Button/Primary CTA** "Accept and Continue" + legal footer — `AIDataSharingConsentView` |
| Login — Sign in | `411:15` | Onboarding page — Rem logo + "Rem" + tagline, two **SignInButtons** (neutral/black filled): Continue with Google (placeholder G) / Apple (apple.logo U+F8FF), legal footer — `OnboardingFlow.signInContent` |
| Sign Out — confirmation | `412:15` | Settings page — iOS action sheet: message + red "Sign Out" + separate "Cancel" — `SharedSettingsView` confirmationDialog |
| Delete Account — sheet | `412:24` | Settings page — `.medium` sheet: Cancel/title nav, warning copy, "type delete" confirm field, red destructive button — `SharedDeleteAccountSheet` |
| View history — Activity | `413:32` | Task & Events page — back+"Activity" nav, alternating Rem/You activity rows (avatar + author + time + body, separators) — `TaskActivityHistoryView` (Rem avatar render debt logged) |
| Onboarding — Start Using Rem | `414:15` | Onboarding page — post-setup activation (page 1/3): message.badge.waveform.fill hero, "Start in chat" copy, pager dots, Button/Primary "Start Using Rem" — `PostSetupActivationView` |
| Onboarding — Setting Up (deploying) | `415:15` | Onboarding page — "Setting Up" + subtitle, green progress bar + timer, 4 phase rows (Creating done / Deploying active / Configuring·Finishing pending) — `OnboardingFlow.deployingContent`. ⚠ founder believed removed; still in code |
