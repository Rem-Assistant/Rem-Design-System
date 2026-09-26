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

## File organization (auto-layout pass — 2026-09-24)

Founder feedback: pages felt disorderly (no auto-layout) and some screens were on the wrong page.
Fixed:
- **Cover** (`0:1`): cleared of stray components; added a cover card `452:2` (brand-blue, title + subtitle).
- **New Task or Event `404:5`** moved off Cover → **Task & Events** (`356:4`), lined up with Task detail
  `299:2` and Task activity `413:32`.
- **Accessories** PermissionStatusBadge `383:14`, StatusChevron `383:15`, Accessory/Value `389:5` moved off
  Cover → **Rows & Controls** (matching the Page column below — they were physically stranded on Cover).
- **Proposed · Agent surfaces** (`427:15`): rebuilt as an auto-layout catalog under root `449:56` —
  a **Components** wrap-grid (captioned cells) + a **Screens** row. No more overlaps.
- **Primitives** (`297:191`): tidied into an auto-layout column `451:2` (Button, Pill, ContainedIcon,
  RemFaceMark) + a labeled **Superseded** group `451:8` (orphaned Text/Surface/Card headers).

> _Residual drift (logged, not blocking):_ a few Page-column values below still name a home a component
> hasn't physically been moved to (e.g. ContainedIcon `110:54` reads "Rows & Controls" but lives on
> Primitives; Avatar's "ContainedIcon" page no longer exists). Reconcile the Page column to physical pages
> in a later housekeeping pass.

## Canonical components

| Component | Page | Node | SwiftUI source | Status |
|---|---|---|---|---|
| ListRow (3 slots: **Leading Accessory · Content · Trailing Accessory**) — swappable leading (ContainedIcon/Avatar), re-based to iOS 26 metrics + variables | Rows & Controls | `101:18` | `SharedSettingsView.swift` insetGrouped rows | ✓ canonical (kept — kit Row leading isn't swappable to ContainedIcon) |
| ListRowLabel (default Content: Title/Subtitle) | ListRow | `188:2` | the row's text block | ✓ canonical |
| Avatar (29×29 leading option) | ContainedIcon | `185:2` | circular photo/initials leading | ✓ canonical |
| SectionHeader | Section | `161:68` | `Section { } header: { Text }` | ✓ canonical |
| SectionFooter | ListRow | `161:70` | `Section { } footer: { Text }` | ✓ canonical |
| TaskEventRow (task/event) — Row descendant; props **Kind**, **Leading** (Time/Schedule/Clock), **Pills** (bool) | Tasks & Agenda | `46:21` | `TaskEventRowView.swift` (taskContent) | ✓ canonical |
| TaskRunStatusBadge — **Status** = Working / Needs review / Needs attention / Done (tint@12% + border + icon) | Tasks & Agenda | `417:60` | `TaskRunStatusBadge.swift` | ✓ canonical |
| TaskDeemphasisBadge — **Reason** = Blocked / Stale (icon + label, pill) | Tasks & Agenda | `419:41` | `TaskDeemphasisBadge.swift` | ✓ canonical |
| Overdue (pill) | Tasks & Agenda | `419:42` | `OverduePillView` (`TaskEventRowView`/`SharedTaskRow`) | ✓ canonical |
| Priority — **Level** = High / Low (tinted pill) | Tasks & Agenda | `419:48` | `SharedTaskRow` priority pill | ✓ canonical |
| SuggestedTaskRow (add/move) | SuggestedTaskRow | `48:25` | `SuggestedTaskRow.swift` | ✓ canonical |
| Button — **Type · Tier** model (founder-directed, no gradient): **Rect · Black** (login CTA) / **Rect · Blue** / **Rect · Secondary** (`.bordered`) / **Rect · Destructive** (`.borderedProminent .red`) / **Pill · Secondary** (Connect) / **Text · Accent** (settings CTA) / **Text · Destructive** (`.plain .destructive`). Full model = Type {Rectangular/Pill/Text} × Tier {Black/Secondary/Blue/Destructive}; the 7 built cover real usage — add other combos as needed. | Primitives | `377:8` | `RemSettingsCTAButtonStyle.swift` (`RemPrimaryActionButtonStyle`=Rect·Black, `RemSettingsCTAButtonStyle`=Text·Accent/Destructive, `RemRowConnectCTA`=Pill·Secondary) | ✓ canonical |
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
| ContextualMessage | ContextualMessage | `73:39` | boolean **`Actions`** + swappable **ButtonGroup** footer (`576:31`); true instances: pairing `577:2` (warning, Review/Reset), calendar `577:31` (info, Enable) | ✓ canonical — models `RemContextualMessage` icon + title/subtitle + `actions()` slot |
| ButtonGroup | ButtonGroup | `576:31` | HStack of pill buttons (fill/tertiary, label semibold); swapped into ContextualMessage's Actions slot | ✓ |
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
| GuidedFlow (coach-mark overlay) — dimmed scrim + spotlight + Step X/Y tooltip (Skip / Next) | Platform Controls | `442:113` | `GuidedFlow.swift` (spotlight coach-mark engine; replaces the deprecated onboarding screens) | ✓ canonical |
| OnboardingSequencer (shell) — ordered flow: back + progress + Continue/Skip over a shared step scaffold; hosts the reproduced **Sign-in** (`411:15`) + **Consent** (`410:16`) steps and exposes ordered slots for the #12 middle steps. **No deploy/provisioning slot.** | Onboarding | `411:15` / `410:16` (Flows page for the shell path) | `OnboardingFlow.swift` (deploy step dropped from the path) · Compose `compose/RemDesignSystem/onboarding/` (`OnboardingSequencer.kt`, `OnboardingScaffold.kt`, `SignInStep.kt`, `ConsentStep.kt`, `Onboarding.figma.kt`) | ✓ canonical (Compose siblings added — issue #11) |
| RemFaceMark (brand face; **Mode** = idle / thinking) | Primitives | `362:7` | `RemFaceMark.swift` (CustomFaceShape) | ✓ canonical |
| Navigation Bar | imported | `d29957…` | iOS 26 kit (Apple) | kit |
| Status Bar / Toolbar | imported | — | iOS 26 kit (Apple) | kit |

## Proposed (NOT-yet-shipped — Muse-informed agentic surfaces)

> These live on the **`Proposed · Agent surfaces`** page and are **design proposals**, not built from
> shipping code — never mix them with the canonical (as-built) components above. They graduate up when
> the code ships them. IA/placement (Agent hub, Wallet home, Settings→General) + deprecation debates
> remain **open** for the founder (see `EVOLUTION.md`).

| Proposed component | Node | Muse pattern |
|---|---|---|
| AgentStatusPill — **Tone** = Neutral / Attention · **Status** text prop (Working / Generating PDF / Reviewing guidance / Needs approval / Needs you) | `427:21` | glass status pill under the agent avatar |
| ActionCard — **State** = Active / Completed (agent-initiated input: icon tile + title + subtitle + CTA → "Added") | `428:37` | inline "Secure Store" action card |
| ApprovalGate — in-chat permission prompt: icon + heading + body + Details JSON block + **Allow (gradient/Commit) / Always allow / Deny** | `429:20` | in-chat approval card ("Allow Arlo to …?") |
| **Timeline (base component)** — reusable vertical timeline: status node (green ✓ / red ✗ / blue in-progress) on a **connecting rail** + title + right-aligned timestamp + secondary description; tail node has no connector. Founder-directed ("create your own timeline component"). **Used on the Activity screen `413:32`.** | `482:56` | our own timeline (evolves the exec-trace, adds the connecting rail) |
| Execution trace (pattern reference / modal variant) — "In progress" pill + X, MAIN/SUBAGENT step rows, "Working" footer. **Superseded by the `Timeline` base component `482:56`** as the product pattern; kept as the modal/standalone reference (avatar / in-chat). | `431:21` | agent step-timeline / "show your work" surface |
| RunningTaskBanner — **Tone** = Working / Attention (glass pill: thumbnail + task + status + Stop) | `432:39` | Live-Activity "Browser · Needs you" banner |
| Connector consent pre-screen (sheet) — logo tile + name + tagline + 3 icon-rows + legal + **gradient Connect** / Cancel | `434:21` | connector consent sheet ("Connect Notion?") |
| Accessory/MenuValue — value + up/down chevron pull-down (**Value** text prop: Ask / Allow / Deny), on canonical ListRow | `435:22` | per-capability permission menu (Browser perms) |
| ContentUnavailableView + CTA — empty state with an action slot (icon + title + subtitle + **Button/Standard-blue**) | `436:67` | "No info saved → Add login info" |
| Browser takeover (screen) — dark chrome (title/subtitle/X) + embedded browser + coachmark + **Take control (gradient) / Stop the task** | `437:68` | human-in-the-loop browser takeover |
| PollCard (in-chat) — question header + selectable option rows (title wraps + right-aligned status label: "On your list" / "New") + "None of these" row; container = proposal-card style (backgroundSecondary, radius 16, white option pills) | `458:56` | founder screenshot — in-chat priority/choice card. **Net-new (not in code** — closest is `RemProposalCardView`, a single Approve/Dismiss proposal; protocol has a non-UI `PollParams`). |
| MessageDraftCard (in-chat) — provider header (logo tile + name) + To/Subject/body fields + full-width **Send** (blue) | `458:69` | founder screenshot — agent-drafted message-send card. **Net-new (not in code** — reuse `RemRemoteLogoView` provider logos + proposal-card shell). Logo = neutral tile placeholder (brand-logo debt). |

## Screens (one generation — device-framed on the Screens page)

> **Navigation map** lives on the `Flows` page (`420:15`): a static screen-graph (labeled boxes by nav
> depth + SVG arrow connectors) — Launch→onboarding→Main hub→tabs, task + settings branches. Mirrors the
> code nav graph (`ContentView` routing). v1 static; live prototype wiring is a follow-up.
>
> **Settings-domain screens are consolidated on one `Settings` page** (Settings · Connectors · About ·
> Billing · Permissions + Sign Out / Delete Account states, laid out in a row) — the separate
> Connectors/About/Billing/Permissions pages were removed (founder: "group all the settings screens on
> the same page"). Onboarding screens live on the `Onboarding` page; task screens on `Task & Events`.

Built on the canonical components above, housed in `DeviceFrame/iPhone`, organized in labeled
Sections. Legacy standalone templates (old AgendaView/ChatScreen/InboxView/SettingsView) are being
**retired** in favor of these.

| Screen | Node | States |
|---|---|---|
| Settings | `130:44` | root — **profile row = ListRow** (Avatar leading + name/email), rest ListRow |
| Connectors | `133:192` | connected / not-connected rows |
| About | `134:242` | — |
| Billing & Usage | `341:862` | Free plan — **Plan row = ListRow + Value**, Usage progress rows custom (accepted), SectionHeaders — `BillingSettingsView.swift` |
| Voice (Settings) | `476:530` | Settings page — **kit nav bar** (`Toolbar - Top - iPhone`, back + "Voice"); grouped list: **Hear-this-voice** preview (fillTertiary disc + blue play.fill + name), **Spoken responses** (waveform ContainedIcon + "Voice"/name + chevron.up.chevron.down, opens chooser sheet), **Character & speed** (Speed/Consistency/Likeness sliders + min/max labels) — `SharedVoiceSettingsView.swift`. First screen using the kit nav bar via the swap recipe. |
| Permissions | `349:905` | 3 sections — **all rows = ListRow + StatusChevron trailing**, SectionHeader/Footer — `SettingsView.swift` DevicePermissionsView |
| General (Proposed) | `509:833` | Settings page — the **agent-runtime hub** reached via the Settings **Rem · Connected** row (`SharedRemGatewayHomeView` → `SharedGatewayDetailView`, today "Agent settings"), retitled **General** per founder: **Gateway** (Connection · Connected) · **Connectivity** (Paired Devices, Connectors, Cloud browser, Backup, Skills, Daily Check-in) · **Memory & Keys** (Memory, Models) · **Experience** (Voice). Rename the entry row + view title to "General" to wire. |
| DateNavigationHeader | `43:2` | `‹ - - - 📅 Today / date - - - ›` — chevrons + **three flanking dashes each side** + calendar glyph + relative/absolute date. Matches the app (fixed 2026-09-25). `SharedDateNavigationHeader`. |
| Agenda scenarios | `530:22` | Tasks & Agenda page — **Jump to Today** pill `530:34` (`arrow.uturn.backward`, floating capsule when off-today, `AgendaView.swift:109`). |
| Developer mode pill | `535:33` | Chat · Scenarios — dark pill + orange warning + "Developer mode" (shown when serving/dev build). |
| Finish connecting this device (pairing) | `535:36` | Chat · Scenarios — pairing-recovery card: person.badge.key + title + body + **Review** (orange) / **Reset** (orange tint). `runtimePairingRecoveryCard` / `ChatConnectionRecoveryCard` (pairingRequired). |
| Agenda | rebuilding | scheduled · empty · loading (jump-to-today, sort modes to add) |
| Chat | `71:533` | voice-active thread (iOS 26 Toolbar-Top nav, bound, real symbols) ✓ |
| Chat Sessions (as-built tab) | `438:15` | Large Title + search + session rows (name + preview + timestamp via ListRow + Value) — `ChatHistoryView`. The main/side-chat *redesign* is a separate Proposed item (founder-gated). |
| Task detail | `299:2` | root — iOS 26 nav (back+Task), title/date/meta, Last-activity card, notes, composer; bound + real symbols ✓ (page: **Task & Events**) |
| New Task or Event | `404:5` | create mode — Cancel/Save nav, **New Task / New Event** segmented picker, Title + dashed-circle status indicator, "Set date, time, repeat" card, notes — `TaskEventView` (isNewTask) |
| New Task or Event — kit Full sheet | `507:115` | Settings page — kit **Full-Screen sheet** (grabber · **X** · title · blue **↑**=save) wrapping the New Task form — the Inbox/Agenda "+" full-height sheet variant |
| Schedule Tasks — kit Full sheet | `514:272` | Agenda page — kit **Full-Screen sheet** (grabber · **X**=Cancel · "Schedule Tasks" · submit-arrow hidden): segmented **All/Inbox/Overdue** → inset-grouped task list with selection circles (`checkmark.circle.fill` / ring) → action bar **Add to Today** (clock) + **Plan** (calendar). Opened by the AgendaAddSchedule "Schedule" action. Backing view `TaskSelectorSheet` (`AgendaView.swift` L1030–1158). Bound colors + real symbols ✓ |
| Add to Chat sheet | `525:2` | **Chat · Scenarios** page — the composer **"+"** medium sheet: Camera/Photos/Files attach boxes + **Cloud browser** row + **Thinking** (Off/Low/Medium/High). `addToChatSheet` (`SharedRemChatView.swift` L4135). Real SF Symbols, bound colors ✓ |

### Chat · Scenarios (code-real chat states — `Chat · Scenarios` page `524:2`, in progress)
The scenario layer of chat (composer +/attachments, cloud-browser use, permission asks, prompt/stream states,
empty state) — see CLEANUP "Founder review 6" for the full gap list + build order.

| Component | Node | Notes |
|-----------|------|-------|
| BrowserLiveCard | `524:31` | In-chat cloud-browser card, variant set **State=Opening/Active/Ended** — 56×40 preview + "Rem's browser session" + status. `BrowserLiveView.swift:142`. ✓ |
| Composer (states) | `527:2` | Full composer `[+] · model · field · [Speak] [send]` in 3 states: Idle / Composing (+ attachments strip: Cloud-browser + image chip) / Sending (red `stop.fill` abort). `SharedRemChatView.composerBar` L3913. ✓ |
| Prompt & status states | `528:2` | Empty state (face + "Start a conversation" + starter prompts) · Interrupted→Retry (orange) · error banner (reconnecting) · quota banner (red). `emptyStateBody`/`interruptedTurnCard`/`errorBanner`/`quotaExceededBanner`. ✓ |
| Browser takeover (live) | `556:31` | `SharedBrowserLiveSheet` live state — grabber · Done/"Rem's browser"/End nav · address bar (lock + host/path + Live badge) · live surface (page + **RemoteCursor**) · "Rem is driving" + Take control. The field-level browser control. ✓ |
| Browser takeover (controlling) | `562:31` | `SharedBrowserLiveSheet` paired **field-control** state — same nav/address bar · surface with a **focused input** (blue outline, "sam@example.com") · control-bar **field editor** (fillTertiary field + return glyph) · "You have the controls" · full-width **Button/Rect·Black** "Give control back to Rem". The take-control CTA's landed state. ✓ |
| AssistantMarkdown — code + table | `566:31` | `AssistantMarkdownRenderer.swift` segment render — bold heading (### → bold) · body prose · **code block** (`swift` label chatChrome/labelTertiary + mono body, bg fill/tertiary @0.75, radius 8) · **GFM table** (From/Subject/Time, 1px `separator`-showthrough grid, header bg background/secondary semibold, cells h10 v7, radius 8). ✓ · **gap:** no SF Mono face in file → code renders proportional. |
| Device status card | `566:68` healthy · `566:78` stressed | `ToolResultCardView.DeviceStatusCard` — battery **SF Symbol** (battery.100percent `U+1006E8` green / battery.25percent `U+1006E9` red) + "Device Status" (subheadline semibold) · **status pills** (footnote text on color@0.12 Capsule: %, battery state, thermal `Serious` orange, `Low Power` yellow) · Storage line (footnote labelTertiary). Card bg fill/tertiary, radius 12(medium). ✓ |
| CalendarEventsCard | `570:31` (Cards page) | `CalendarCard.swift` — header (`calendar` `U+100249` red + "N events · Cal" semibold) + event rows: time column (start footnote semibold / end caption tertiary, or "All day"), 3×32 `system/red` bar, title (subheadline semibold) + calendar (footnote tertiary). `+N more` past 5. ✓ |
| RemindersCard | `570:59` (Cards page) | `RemindersCard.swift` — header (Apple Reminders logo *(orange tile placeholder — brand-asset debt)* + "N reminders") + rows: `circle`/`checkmark.circle.fill` (`U+100000`/`U+100063`, green if done) + title (strikethrough if done) + due·list (footnote tertiary) + `!`/`!!!` priority (orange/red). ✓ |
| DeviceInfoCard | `570:86` (Cards page) | `ToolResultCardView.DeviceInfoCard` — device **SF Symbol** (`iphone` `U+1007DC` / ipad / desktopcomputer, systemBlue, title2) + name (subheadline semibold) + "model · OS version" (footnote tertiary). ✓ |
| BottomToolbar (tab bar) | `552:33` | RemUI main tab bar: ☰ · mic FAB · +. ✓ |
| Result cards | `557:31` | In-chat tool-result cards: **ConfirmationCard** (Event Created/Reminder Set/Task Created — icon+title+subtitle, fillTertiary) + collapsed **ErrorResultCard** (warning + "Error" + chevron). `ConfirmationCard`/`ErrorResultCard`. ✓ |
| Run activity & connection | `558:31` | **ActionLifecycleCard** ("Working" spinner) + **ActionLifecycleDisclosure** ("Worked for 12s" + step timeline) + **ChatWakingSkeleton** (shimmer bubbles) + connection **RemContextualMessage** ("Can't reach your gateway" + Retry). ✓ |
| Add to Chat sheet | `525:2` | (also in Screens above) the composer "+" attachment/thinking sheet. ✓ |
| Privacy — "Privacy by design" | `410:16` | Onboarding page — hero (lock.shield.fill on brand tile), title/body, **Terms/Privacy rows in a grey grouped section**, **Button/Rect·Black CTA** "Accept and Continue" + legal footer — `AIDataSharingConsentView`. Reproduced as a sequencer step (issue #11): Compose `onboarding/ConsentStep.kt` (`consentStep(…)`), hosted by `OnboardingSequencer`. Copy is verbatim from `tasks/refs/onboarding/02-consent.png`. |
| Login — Sign in | `411:15` | Onboarding page — Rem logo + "Rem" + tagline, two **SignInButtons** (neutral/black filled): Continue with Google (placeholder G) / Apple (apple.logo U+F8FF), legal footer — `OnboardingFlow.signInContent`. Reproduced as a sequencer step (issue #11): Compose `onboarding/SignInStep.kt` (`signInStep(state: SignInState, …)` — returning/new/checking/error/recovery), hosted by `OnboardingSequencer`. Sign-in-with-Apple treatment per `tasks/refs/onboarding/01-sign-in.png`. |
| Sign Out — confirmation | `412:15` | Settings page — iOS action sheet: message + red "Sign Out" + separate "Cancel" — `SharedSettingsView` confirmationDialog |
| Delete Account — sheet | `412:24` | Settings page — `.medium` sheet: Cancel/title nav, warning copy, "type delete" confirm field, red destructive button — `SharedDeleteAccountSheet` |
| View history — Activity (task-scoped timeline) | `413:32` | Task & Events page — pushed screen: back+"Activity" nav → task header (title + subtitle + timestamp, **no modal pill/X**) → the **`Timeline` base component `482:56`** (status nodes on a connecting rail + title/time/description). Replaces the old one-comment-deep Rem/You activity list and the earlier rail-less exec-trace body. Entered from the task detail (and the agent avatar routes here). Backing view `TaskActivityHistoryView`. |
| Onboarding — Start Using Rem | `414:15` | ⚠ **UP FOR DEPRECATION** (founder) — post-setup activation being retired in favour of the `GuidedFlow` coach-mark overlay. `PostSetupActivationView` |
| Onboarding — Setting Up (deploying) | `415:15` | ⚠ **UP FOR DEPRECATION** (founder) — `OnboardingFlow.deployingContent`. Don't invest further. **Dropped from the `OnboardingSequencer` path (issue #11)** — the sequencer injects no deploy/provisioning slot; the deploy code itself is retired later with the runtime migration's *Product cleanup* (out of scope for #11). |
