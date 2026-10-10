package com.rem.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.brand.RemFaceMark
import com.rem.designsystem.brand.RemFaceMarkMode
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.onboarding.CheckinStatus
import com.rem.designsystem.onboarding.LegalDocumentScreen
import com.rem.designsystem.onboarding.OnboardingCheckinScreen
import com.rem.designsystem.onboarding.OnboardingSequencer
import com.rem.designsystem.onboarding.checkinDefaultPeriods
import com.rem.designsystem.onboarding.OnboardingSignInScreen
import com.rem.designsystem.onboarding.SignInState
import com.rem.designsystem.onboarding.consentStep
import com.rem.designsystem.onboarding.rememberOnboardingSequencerState
import com.rem.designsystem.onboarding.signInStep
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.primitives.RemContentUnavailableView
import com.rem.designsystem.primitives.RemPill
import com.rem.designsystem.primitives.RemPillKind
import com.rem.designsystem.primitives.RemSlider
import com.rem.designsystem.agenda.DateNavigationHeader
import com.rem.designsystem.chat.ActionReceipt
import com.rem.designsystem.chat.ActionReceiptOutcome
import com.rem.designsystem.chat.AddToChatSheet
import com.rem.designsystem.chat.MessageDraft
import com.rem.designsystem.chat.MessageDraftCard
import com.rem.designsystem.chat.MessageDraftCardState
import com.rem.designsystem.chat.PollCard
import com.rem.designsystem.chat.PollOption
import com.rem.designsystem.chat.PollPurpose
import com.rem.designsystem.chat.ChatHeader
import com.rem.designsystem.chat.ChatHeaderStatus
import com.rem.designsystem.chat.ChatModelMenu
import com.rem.designsystem.chat.ChatModelOption
import com.rem.designsystem.chat.ChatModelProvider
import com.rem.designsystem.chat.ChatModelSelection
import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.chat.MessageBubble
import com.rem.designsystem.chat.MessageDelivery
import com.rem.designsystem.chat.MessageReaction
import com.rem.designsystem.chat.MessageReactionPicker
import com.rem.designsystem.chat.ThinkingLevel
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.chat.RemComposerBar
import com.rem.designsystem.chat.ComposerSendState
import com.rem.designsystem.screens.AgendaScreen
import com.rem.designsystem.screens.AgendaReferenceSuggestions
import com.rem.designsystem.screens.AgendaSuggestionsFixture
import com.rem.designsystem.screens.AgendaSuggestionsOverflowContent
import com.rem.designsystem.screens.AgendaSuggestionsPlayground
import com.rem.designsystem.screens.AddTaskField
import com.rem.designsystem.screens.InboxScreen
import com.rem.designsystem.screens.ChatMessageActionsDisplay
import com.rem.designsystem.screens.ChatMessageDisplay
import com.rem.designsystem.screens.ChatScreen
import com.rem.designsystem.screens.MessageActionSheet
import com.rem.designsystem.screens.TaskDetailScreen
import com.rem.designsystem.screens.SettingsScreen
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.agentsurfaces.AgentStatusPill
import com.rem.designsystem.agentsurfaces.AgentStatusTone
import com.rem.designsystem.agentsurfaces.RunningTaskBanner
import com.rem.designsystem.agentsurfaces.RunningTaskTone
import com.rem.designsystem.agentsurfaces.BrowserLiveCard
import com.rem.designsystem.agentsurfaces.BrowserLiveCardState
import com.rem.designsystem.agentsurfaces.ExecutionTrace
import com.rem.designsystem.agentsurfaces.ExecutionStep
import com.rem.designsystem.agentsurfaces.ExecutionTraceLane
import com.rem.designsystem.agentsurfaces.ExecutionStepStatus
import com.rem.designsystem.agentsurfaces.ExecutionTraceStatus
import com.rem.designsystem.screens.WalletScreen
import com.rem.designsystem.screens.walletReferenceProviders
import com.rem.designsystem.screens.SettingsReferenceContent
import com.rem.designsystem.agentsurfaces.DailyBriefCard
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.agentsurfaces.BriefCounts
import com.rem.designsystem.rows.SuggestionSection
import com.rem.designsystem.rows.TaskSuggestion
import com.rem.designsystem.rows.SuggestionAccept
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Text as M3Text
import com.rem.designsystem.chat.VoiceBar
import com.rem.designsystem.chat.VoiceBarState
import com.rem.designsystem.rows.TaskEventRow
import com.rem.designsystem.rows.TaskEventKind
import com.rem.designsystem.rows.TaskEventLeading
import com.rem.designsystem.onboarding.Connector
import com.rem.designsystem.onboarding.OnboardingConnectorsScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.rem.designsystem.chat.ConnectorCard
import com.rem.designsystem.chat.ConnectorCardModel
import com.rem.designsystem.chat.ConnectorCardState
import com.rem.designsystem.chat.LoginCard
import com.rem.designsystem.chat.LoginCardModel
import com.rem.designsystem.chat.LoginCardState
import com.rem.designsystem.chat.LoginForm
import com.rem.designsystem.chat.PermissionCard
import com.rem.designsystem.chat.PermissionCardModel
import com.rem.designsystem.chat.PermissionCardState
import com.rem.designsystem.chat.PermissionRequestDetails
import com.rem.designsystem.rows.ConnectorProvider
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot EVIDENCE (not a golden-diff gate): renders the Compose design-system components to PNGs
 * via Paparazzi — pure JVM/LayoutLib, no emulator — so the Android output is visible next to the
 * SwiftUI renders. Mirrors each component's own `@Preview`. Run with `recordPaparazziDebug`.
 */
class EvidenceSnapshots {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private fun shot(name: String, content: @Composable () -> Unit) =
        paparazzi.snapshot(name = name, composable = content)

    // Pairs with the SwiftUI `RemButton-light` / `RemButton-dark` gallery: every variant, then Disabled.
    @Test
    fun remButton() {
        shot("RemButton-light") { RemTheme { buttonGallery() } }
        shot("RemButton-dark") { RemTheme(darkTheme = true) { buttonGallery() } }
    }

    @Composable
    private fun buttonGallery() {
        Column(
            Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RemButtonVariant.entries.forEach { RemButton(it.figmaStyleName, onClick = {}, variant = it) }
            RemButton("Disabled", onClick = {}, enabled = false)
        }
    }

    @Test
    fun containedIcon() {
        shot("ContainedIcon-light") { RemTheme { iconRow() } }
        shot("ContainedIcon-dark") { RemTheme(darkTheme = true) { iconRow() } }
    }

    // Mirrors the iOS ContainedIcon gallery glyph-for-glyph — now via the Material Symbols glyph
    // overload at each registry FILL, so the side-by-side table is a true comparison: shield_lock
    // (≈ lock.shield.fill, FILL 1, tinted+large), description (≈ doc.text, FILL 0, subtle), shield
    // (≈ shield, FILL 0, subtle) — on the primary background so the "light" shot is white, not the
    // Paparazzi default (which otherwise made the subtle tile vanish on dark).
    @Composable
    private fun iconRow() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RemColors.current.backgroundPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                ContainedIcon(
                    symbol = RemMaterialSymbols.PrivacyLockShield,
                    fill = ContainedIconFill.Tint(RemColors.current.brandBlue),
                    size = ContainedIconSize.Large,
                )
                ContainedIcon(symbol = RemMaterialSymbols.TermsDocument, fill = ContainedIconFill.Subtle)
                ContainedIcon(symbol = RemMaterialSymbols.PrivacyPolicy, fill = ContainedIconFill.Subtle)
            }
        }
    }

    @Test
    fun remFaceMark() {
        shot("RemFaceMark-light") { RemTheme { faceMark() } }
        shot("RemFaceMark-dark") { RemTheme(darkTheme = true) { faceMark() } }
    }

    // The canonical Rem face mark — the same scalloped blob + eyes + smile as the SwiftUI
    // `RemFaceMark` (idle resting frame), brandBlue on the primary background.
    @Composable
    private fun faceMark() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RemColors.current.backgroundPrimary),
            contentAlignment = Alignment.Center,
        ) {
            RemFaceMark(mode = RemFaceMarkMode.Idle, tint = RemColors.current.brandBlue, size = 96.dp)
        }
    }

    // Sign-in renders its own centered screen (OnboardingSignInScreen) — NOT the sequencer scaffold's
    // bottom-pinned CTA bar — so the paired iOS⟷Android render diffs the same arrangement.
    @Test
    fun signInReturning() = shot("SignIn-returning-light") {
        RemTheme {
            OnboardingSignInScreen(
                state = SignInState.Returning(accountName = "Sam"),
                onContinue = {},
                onUseDifferentAccount = {},
            )
        }
    }

    @Test
    fun signInNew() = shot("SignIn-new-light") {
        RemTheme {
            OnboardingSignInScreen(state = SignInState.New, onContinue = {}, onUseDifferentAccount = {})
        }
    }

    @Test
    fun signInChecking() = shot("SignIn-checking-light") {
        RemTheme {
            OnboardingSignInScreen(state = SignInState.Checking, onContinue = {}, onUseDifferentAccount = {})
        }
    }

    @Test
    fun signInError() = shot("SignIn-error-dark") {
        RemTheme(darkTheme = true) {
            OnboardingSignInScreen(
                state = SignInState.Error("We couldn't sign you in. Check your connection and try again."),
                onContinue = {},
                onUseDifferentAccount = {},
            )
        }
    }

    @Test
    fun signInRecovery() = shot("SignIn-recovery-light") {
        RemTheme {
            OnboardingSignInScreen(
                state = SignInState.Recovery("Your session expired. Sign in again to pick up where you left off."),
                onContinue = {},
                onUseDifferentAccount = {},
            )
        }
    }

    // Check-in cadence states — the Compose siblings of the SwiftUI `Checkin-*` shots, one state per
    // snapshot so the paired table diffs each state directly.
    @Test
    fun checkinDefault() = shot("Checkin-default-light") {
        RemTheme {
            OnboardingCheckinScreen(
                status = CheckinStatus.Default,
                periods = checkinDefaultPeriods(morningOn = true, middayOn = false, nightOn = false),
                onToggle = { _, _ -> },
                onContinue = {},
            )
        }
    }

    @Test
    fun checkinEdited() = shot("Checkin-edited-light") {
        RemTheme {
            OnboardingCheckinScreen(
                status = CheckinStatus.Edited,
                periods = checkinDefaultPeriods(morningOn = true, middayOn = true, nightOn = false),
                onToggle = { _, _ -> },
                onContinue = {},
            )
        }
    }

    @Test
    fun checkinSaving() = shot("Checkin-saving-light") {
        RemTheme {
            OnboardingCheckinScreen(
                status = CheckinStatus.Saving,
                periods = checkinDefaultPeriods(morningOn = true, middayOn = true, nightOn = false),
                onToggle = { _, _ -> },
                onContinue = {},
            )
        }
    }

    @Test
    fun checkinSaved() = shot("Checkin-saved-light") {
        RemTheme {
            OnboardingCheckinScreen(
                status = CheckinStatus.Saved,
                periods = checkinDefaultPeriods(morningOn = true, middayOn = true, nightOn = false),
                onToggle = { _, _ -> },
                onContinue = {},
            )
        }
    }

    @Test
    fun checkinFailure() = shot("Checkin-failure-light") {
        RemTheme {
            OnboardingCheckinScreen(
                status = CheckinStatus.Failure("We couldn't save your check-in times. Check your connection and try again."),
                periods = checkinDefaultPeriods(morningOn = true, middayOn = true, nightOn = false),
                onToggle = { _, _ -> },
                onContinue = {},
            )
        }
    }

    @Test
    fun consent() = shot("Consent-default-light") { RemTheme { consentScreen() } }

    // Reusable legal content chrome. The host-owned modal container, scrim, and return behavior are
    // verified by the Figma prototype and interaction evidence rather than duplicated in this fixture.
    @Test
    fun consentTerms() = shot("Consent-terms-light") {
        RemTheme { LegalDocumentScreen(title = "Terms of Service", sections = previewTermsSections, onClose = {}) }
    }

    @Test
    fun consentPrivacy() = shot("Consent-privacy-light") {
        RemTheme { LegalDocumentScreen(title = "Privacy Policy", sections = previewPrivacySections, onClose = {}) }
    }

    @Test
    fun connectors() = shot("LegacyOnboardingConnectors-light") {
        RemTheme {
            OnboardingConnectorsScreen(
                connectors = listOf(
                    Connector(Icons.Filled.Email, Color(0xFFEA4335), "Gmail", "Connected", true) {},
                    Connector(Icons.Filled.DateRange, Color(0xFF1A73E8), "Google Calendar", "Not connected", false) {},
                    Connector(Icons.Filled.Notifications, Color(0xFF6B4FBB), "Slack", "Not connected", false) {},
                ),
                onContinue = {}, onSkip = {},
            )
        }
    }

    // Onboarding Voice now has its own interactive native journey (OnboardingVoice-* captures) that
    // reuses the shared VoiceControlsContent/VoiceChooserContent cores. The legacy static Paparazzi
    // render was retired so it cannot drift from, or collide with, those journey shots.

    @Test
    fun pill() {
        shot("Pill-light") { RemTheme { pillGallery() } }
        shot("Pill-dark") { RemTheme(darkTheme = true) { pillGallery() } }
    }

    @Composable
    private fun pillGallery() {
        Box(
            modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
                RemPill("3 tasks", kind = RemPillKind.List)
                RemPill("Standup", kind = RemPillKind.Dot(RemColors.current.systemBlue))
                RemPill("Personal")
            }
        }
    }

    @Test
    fun slider() {
        shot("Slider-light") { RemTheme { sliderGallery() } }
        shot("Slider-dark") { RemTheme(darkTheme = true) { sliderGallery() } }
    }

    @Composable
    private fun sliderGallery() {
        Column(
            modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RemSlider(value = 0.25f, onValueChange = {})
            RemSlider(value = 0.6f, onValueChange = {})
            RemSlider(value = 0.9f, onValueChange = {})
        }
    }

    @Test
    fun agendaEmpty() = shot("AgendaEmpty-light") {
        RemTheme {
            RemContentUnavailableView(
                icon = Icons.Filled.DateRange,
                title = "No agenda yet",
                message = "Create a new task or schedule existing ones",
                actionLabel = "Add New",
                onAction = {},
            )
        }
    }

    @Test
    fun dateNavigationHeader() = shot("DateNavigationHeader-light") {
        RemTheme {
            Box(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary),
                contentAlignment = Alignment.Center,
            ) {
                DateNavigationHeader(dateText = "Oct 1 2026", onPrevious = {}, onNext = {})
            }
        }
    }

    @Test
    fun messageBubble() = shot("MessageBubble-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MessageBubble("Can you tidy up my inbox before I start my day?", role = MessageRole.User)
                MessageBubble(
                    "Done — I archived 38 newsletters and snoozed 5 low-priority threads.\n\n" +
                        "Want me to draft replies to the two that still need you?",
                    role = MessageRole.Assistant,
                )
                MessageBubble("Yes, go ahead.", role = MessageRole.User, meta = "9:41 AM")
            }
        }
    }

    @Test
    fun taskEventRow() = shot("TaskEventRow-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp),
            ) {
                TaskEventRow(kind = TaskEventKind.Task, title = "Reply to Alex about the audition", leading = TaskEventLeading.Time("9:00"), pills = listOf("3 tasks"))
                TaskEventRow(kind = TaskEventKind.Event(RemColors.current.systemBlue), title = "Team standup", leading = TaskEventLeading.Time("10:30"), pills = listOf("Work"))
                TaskEventRow(kind = TaskEventKind.Task, title = "Unfiled inbox task", leading = TaskEventLeading.Schedule, showPills = false)
            }
        }
    }

    @Test
    fun voiceBar() = shot("VoiceBar-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VoiceBarState.values().forEach { VoiceBar(state = it) }
            }
        }
    }

    @Test
    fun composerBar() = shot("ComposerBar-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RemComposerBar()
                RemComposerBar(text = "Remind me to send the investor update tomorrow", state = ComposerSendState.Active, showAttachments = true)
                RemComposerBar(text = "Plan the rest of my day", state = ComposerSendState.Sending)
            }
        }
    }

    // Chat slice: 320dp screen = 288dp row (accepted narrow fixture 2659:21942), then the wide composer,
    // Add to Chat, header and reaction row. Paired with the SwiftUI chat renders in RenderSnapshots.swift.
    @Test
    fun chatMessageStatesNarrow() = shot("ChatMessageStates-narrow-light") {
        RemTheme {
            Column(
                modifier = Modifier.width(320.dp).background(RemColors.current.backgroundPrimary).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MessageBubble("Can you move the planning sync to Thursday?", role = MessageRole.User, reaction = MessageReaction.Heart)
                MessageBubble(
                    "Done \u2014 the planning sync is now Thursday at 10:00, and both attendees have the update.",
                    role = MessageRole.Assistant, reaction = MessageReaction.ThumbsUp,
                )
                MessageBubble("Thanks, that works.", role = MessageRole.User, delivery = MessageDelivery.Read("10:24"))
                MessageBubble("Please share the agenda with the group as well.", role = MessageRole.User, delivery = MessageDelivery.Failed, onRetry = {})
            }
        }
    }

    private val chatProviders = listOf(ChatModelProvider("provider-a", "Provider A", listOf(ChatModelOption("model-a1", "Model A1"))))

    @Test
    fun chatComposerAuto() = shot("ChatComposerAuto-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                RemComposerBar(
                    onTextChange = {}, onSend = {}, onAdd = {},
                    attachments = listOf(ComposerAttachment.CloudBrowser, ComposerAttachment("photo.0", "Photo 1", ComposerAttachment.Kind.Image)),
                    onRemoveAttachment = {},
                    modelMenu = { enabled -> ChatModelMenu(chatProviders, ChatModelSelection.Automatic, onSelect = {}, enabled = enabled) },
                )
                RemComposerBar(
                    text = "Plan the rest of my day", state = ComposerSendState.Sending, onTextChange = {}, onSend = {},
                    modelMenu = { enabled -> ChatModelMenu(chatProviders, ChatModelSelection.Model("model-a1"), onSelect = {}, enabled = enabled) },
                )
            }
        }
    }

    @Test
    fun addToChatSheet() = shot("AddToChatSheet-light") {
        RemTheme {
            AddToChatSheet(
                showsCamera = false, browserAvailable = true, thinking = ThinkingLevel.Medium, onThinkingChange = {},
                onPhotos = {}, onFiles = {}, onDone = {},
                modifier = Modifier.background(RemColors.current.backgroundPrimary),
            )
        }
    }

    @Test
    fun chatHeader() = shot("ChatHeader-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                ChatHeader(activity = "Connected")
                ChatHeader(activity = "Reading the shared notes", faceMode = RemFaceMarkMode.Thinking)
                ChatHeader(activity = "Needs you", status = ChatHeaderStatus.NeedsYou)
            }
        }
    }

    @Test
    fun messageReactionPicker() = shot("MessageReactionPicker-light") {
        RemTheme {
            Column(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp)) {
                MessageReactionPicker(selection = MessageReaction.Heart, onSelect = {})
            }
        }
    }

    /** Long-press sheet `2603:19498`: the assistant reference, then the own-message variant (no Report). */
    @Composable
    private fun MessageActionSheetShot(role: MessageRole) = RemTheme {
        Column(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary)) {
            MessageActionSheet(
                ChatMessageActionsDisplay(ChatMessageDisplay("m1", role, "Here’s a clearer introduction you can review.")),
                onAction = {},
            )
        }
    }

    @Test
    fun messageActionSheetAssistant() = shot("MessageActionSheet-assistant-light") { MessageActionSheetShot(MessageRole.Assistant) }

    @Test
    fun messageActionSheetOwn() = shot("MessageActionSheet-own-light") { MessageActionSheetShot(MessageRole.User) }

    // Chat cards (MessageDraftCard 2555:1550, PollCard 2559:1524, ActionReceipt 2566:2645), every state.
    // Paired with the SwiftUI `ChatCard*` renders in RenderSnapshots.swift.
    private val cardDraft = MessageDraft(
        from = "me@example.com", to = "alex@example.com", subject = "Re: Product Designer - Next Steps",
        body = "Hi Alex,\n\nThanks for reaching out — I’ve put time on the calendar. Looking forward to chatting.\n\nBest",
    )
    private val choiceOptions = listOf(PollOption("add", "Add Notion"), PollOption("later", "Not now"))
    private val suggestionOptions = listOf(
        PollOption("review", "Review the draft"),
        PollOption("calendar", "Check my calendar"),
        PollOption("remind", "Remind me later"),
    )

    @Test
    fun chatCardMessageDraft() = shot("ChatCardMessageDraft-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MessageDraftCardState.entries.forEach { MessageDraftCard(cardDraft, state = it) }
            }
        }
    }

    @Test
    fun chatCardPoll() = shot("ChatCardPoll-light") {
        val question = "Add the Notion connector so I can use your shared workspace?"
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PollCard(question, choiceOptions)
                PollCard(question, choiceOptions, selection = "add")
                // Three options: the A/B pattern extended to C (no verified C master in Figma).
                PollCard("What would you like to do next?", suggestionOptions, purpose = PollPurpose.Suggestion)
                PollCard("What would you like to do next?", suggestionOptions, purpose = PollPurpose.Suggestion, selection = "remind")
            }
        }
    }

    @Test
    fun chatCardActionReceipt() = shot("ChatCardActionReceipt-light") {
        RemTheme {
            Box(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary), contentAlignment = Alignment.Center) {
                Column(Modifier.width(306.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionReceipt(ActionReceiptOutcome.Confirmed, "Sent")
                    ActionReceipt(ActionReceiptOutcome.Unconfirmed, "Unconfirmed")
                }
            }
        }
    }

    @Test
    fun agendaScreen() = shot("AgendaScreen-light") {
        RemTheme {
            AgendaScreen(dateText = "Oct 1 2026", onPrevious = {}, onNext = {}) {
                TaskEventRow(kind = TaskEventKind.Task, title = "Reply to Alex about the audition", leading = TaskEventLeading.Time("9:00"), pills = listOf("3 tasks"))
                TaskEventRow(kind = TaskEventKind.Event(RemColors.current.systemBlue), title = "Team standup", leading = TaskEventLeading.Time("10:30"), pills = listOf("Work"))
                TaskEventRow(kind = TaskEventKind.Task, title = "Draft the investor update", leading = TaskEventLeading.Time("14:00"), pills = listOf("Fundraise"))
            }
        }
    }

    @Test
    fun agendaSuggestionsInline() = shot("AgendaSuggestions-inline-light") {
        RemTheme { AgendaSuggestionsPlayground(fixture = AgendaSuggestionsFixture.Loaded) }
    }

    @Test
    fun agendaSuggestionsOverflow() = shot("AgendaSuggestions-overflow-light") {
        RemTheme {
            Column(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary)) {
                AgendaSuggestionsOverflowContent(suggestions = AgendaReferenceSuggestions)
            }
        }
    }

    @Test
    fun agendaSuggestionsNone() = shot("AgendaSuggestions-none-light") {
        RemTheme { AgendaSuggestionsPlayground(fixture = AgendaSuggestionsFixture.None) }
    }

    @Test
    fun dailyBriefCard() = shot("DailyBriefCard-light") {
        RemTheme {
            Column(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp)) {
                DailyBriefCard(
                    title = "Daily brief",
                    summary = "Sent to damilola.ogunnaike@gmail.com at 9:37 am — “Hi Damilola, I’ll send " +
                        "you the notes from yesterday’s call before 2pm. Best, Larissa.” 1 task overdue needs attention.",
                    onTap = {}, onRead = {},
                )
            }
        }
    }

    @Test
    fun suggestionSection() = shot("SuggestionSection-light") {
        RemTheme {
            Column(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp)) {
                SuggestionSection(
                    suggestions = listOf(
                        TaskSuggestion("Set up the TestFlight pipeline using ASC CLI", "Samuel · Granola · 8h ago"),
                        TaskSuggestion("Pull the Claude/DSFlows branch and confirm the Compose UI renders", "Damilola · Granola · 8h ago"),
                        TaskSuggestion("Reply to the venue about the deposit", "‘Confirm Saturday’ · overdue 3d", accept = SuggestionAccept.Move),
                    ),
                    onAccept = {}, onDismiss = {}, onSeeMore = {},
                )
            }
        }
    }

    // The full Today screen assembled from DS components — the engineer's parity target.
    @Test
    fun agendaToday() = shot("AgendaToday-light") {
        RemTheme {
            AgendaScreen(dateText = "Oct 2 2026", onPrevious = {}, onNext = {}) {
                DailyBriefCard(
                    title = "Daily brief",
                    summary = "1 task overdue needs attention. Larissa will send yesterday’s call notes before 2pm.",
                    counts = BriefCounts(done = 0, total = 0),
                    onTap = {},
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                TaskEventRow(kind = TaskEventKind.Task, title = "Send yesterday’s call notes", pills = listOf("Overdue"))
                TaskEventRow(kind = TaskEventKind.Event(RemColors.current.systemBlue), title = "National Day", pills = listOf("Holidays in Nigeria"))
                AddTaskField(text = "", onTextChange = {}, onAdd = {}, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                SuggestionSection(
                    suggestions = listOf(
                        TaskSuggestion("Set up the TestFlight pipeline using ASC CLI", "Samuel · Granola · 8h ago"),
                        TaskSuggestion("Continue applying to other programs", "Larissa · Gmail · 1d ago"),
                    ),
                    onAccept = {}, onDismiss = {},
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    @Test
    fun inboxScreen() = shot("InboxScreen-light") {
        RemTheme {
            InboxScreen {
                TaskEventRow(kind = TaskEventKind.Task, title = "Follow up with the Freestyle team", leading = TaskEventLeading.None, showPills = false)
                TaskEventRow(kind = TaskEventKind.Task, title = "Review the Q4 roadmap draft", leading = TaskEventLeading.None, showPills = false)
                TaskEventRow(kind = TaskEventKind.Task, title = "Book the venue for the offsite", leading = TaskEventLeading.None, showPills = false)
            }
        }
    }

    @Test
    fun chatScreen() = shot("ChatScreen-light") {
        RemTheme {
            ChatScreen(composerState = ComposerSendState.Idle) {
                MessageBubble("Can you tidy up my inbox before I start my day?", role = MessageRole.User)
                MessageBubble(
                    "Done — I archived 38 newsletters and snoozed 5 low-priority threads. " +
                        "Want me to draft replies to the two that still need you?",
                    role = MessageRole.Assistant,
                )
                MessageBubble("Yes, go ahead.", role = MessageRole.User)
            }
        }
    }

    @Test
    fun taskDetailScreen() = shot("TaskDetailScreen-light") {
        RemTheme {
            TaskDetailScreen(title = "Draft the investor update", dateText = "Oct 1 2026", metaPills = listOf("Fundraise")) {
                M3Text("LAST ACTIVITY", style = RemTypography.caption1, color = RemColors.current.labelSecondary)
                M3Text(
                    "Rem drafted the investor update and pulled last quarter's metrics — want me to send it?",
                    style = RemTypography.body, color = RemColors.current.labelPrimary,
                )
                RemSection(header = "Notes") {
                    M3Text(
                        "Add your notes here",
                        style = RemTypography.body, color = RemColors.current.labelTertiary,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }
    }

    @Test
    fun settingsScreen() {
        shot("SettingsScreen-light") { RemTheme { SettingsScreen { SettingsReferenceContent() } } }
        shot("SettingsScreen-dark") { RemTheme(darkTheme = true) { SettingsScreen { SettingsReferenceContent() } } }
    }

    // MARK: - Wave 4 — agent surfaces (product bets) + Wallet proposal

    @Test
    fun agentStatusPill() = shot("AgentStatusPill-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AgentStatusPill("Working")
                AgentStatusPill("Needs you", tone = AgentStatusTone.Attention)
            }
        }
    }

    @Test
    fun agentStatusPillDark() = shot("AgentStatusPill-dark") {
        RemTheme(darkTheme = true) {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AgentStatusPill("Working")
                AgentStatusPill("Needs you", tone = AgentStatusTone.Attention)
            }
        }
    }

    @Test
    fun runningTaskBanner() = shot("RunningTaskBanner-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RunningTaskBanner(task = "Browser", status = "Signing in to my.dnb.com")
                RunningTaskBanner(task = "Browser", status = "Needs you · Password rejected", tone = RunningTaskTone.Attention)
            }
        }
    }

    @Test
    fun browserLiveCard() = shot("BrowserLiveCard-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BrowserLiveCard(BrowserLiveCardState.Opening)
                BrowserLiveCard(BrowserLiveCardState.Active)
                BrowserLiveCard(BrowserLiveCardState.Ended)
            }
        }
    }

    @Test
    fun executionTrace() = shot("ExecutionTrace-light") {
        RemTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(24.dp),
            ) {
                ExecutionTrace(
                    status = ExecutionTraceStatus.InProgress,
                    title = "Build RFE checklist",
                    subtitle = "Writing the RFE checklist PDF template",
                    timestamp = "10:49pm",
                    steps = listOf(
                        ExecutionStep(
                            label = "Launched H-1B RFE Checklist Tailoring Subagent",
                            detail = "Delegated the checklist prep to a subagent via artifact.send_input, covering a tailored checklist for USCIS.",
                            lane = ExecutionTraceLane.Main,
                            status = ExecutionStepStatus.Done,
                        ),
                        ExecutionStep(
                            label = "Prepared RFE Checklist Source Directories",
                            detail = "Staged the checklist sources; the concatenate step returned an incomplete JSON payload.",
                            lane = ExecutionTraceLane.Subagent("01"),
                            status = ExecutionStepStatus.Failed,
                        ),
                        ExecutionStep(
                            label = "Found USCIS RFE Official Results",
                            detail = "Web search targeting USCIS official guidance for H-1B Requests for Evidence.",
                            lane = ExecutionTraceLane.Subagent("01"),
                            status = ExecutionStepStatus.Done,
                        ),
                        ExecutionStep(
                            label = "Created index.html source file",
                            detail = "Wrote the RFE checklist HTML template used to render the PDF.",
                            lane = ExecutionTraceLane.Subagent("01"),
                            status = ExecutionStepStatus.Done,
                        ),
                    ),
                    footer = "Working",
                )
            }
        }
    }

    @Test
    fun walletScreen() {
        shot("WalletScreen-light") { RemTheme { WalletScreen(providers = walletReferenceProviders(), onBack = {}) } }
        shot("WalletScreen-dark") { RemTheme(darkTheme = true) { WalletScreen(providers = walletReferenceProviders(), onBack = {}) } }
    }

    // Chat cards — paired with the SwiftUI `ChatConnectorCard-*` / `ChatLoginCard-*` / `ChatLoginForm-*` /
    // `ChatPermissionCard-*` shots of the same names.
    @Composable
    private fun chatCardColumn(content: @Composable () -> Unit) {
        Column(
            Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }

    @Test
    fun chatConnectorCard() = shot("ChatConnectorCard-light") {
        RemTheme {
            chatCardColumn {
                listOf(ConnectorCardState.Authorize, ConnectorCardState.Connecting, ConnectorCardState.Added, ConnectorCardState.Error())
                    .forEach { state ->
                        ConnectorCard(
                            ConnectorCardModel(ConnectorProvider.Gmail, "Search, read, draft, and manage email.", state),
                            onAuthorize = {}, onRetry = {},
                        )
                    }
            }
        }
    }

    @Test
    fun chatLoginCard() = shot("ChatLoginCard-light") {
        RemTheme {
            chatCardColumn {
                LoginCard(LoginCardModel("GitHub login details", "github.com", LoginCardState.Entry), onAddLogin = {}, onOpenSaved = {})
                LoginCard(LoginCardModel("GitHub login details", "github.com", LoginCardState.Saved), onAddLogin = {}, onOpenSaved = {})
                LoginCard(LoginCardModel("GitHub login details", "github.com", LoginCardState.Entry), onAddLogin = {}, onOpenSaved = {},
                    showsChevron = false)
            }
        }
    }

    @Test
    fun chatLoginForm() = shot("ChatLoginForm-light") {
        RemTheme {
            chatCardColumn {
                LoginForm("github.com", "samuel@example.com", {}, "illustrative", {})
            }
        }
    }

    private fun permissionRequest(state: PermissionCardState) = PermissionCardModel(
        title = "Reminder permission", question = "Allow Rem to create this reminder?",
        summary = "One reminder in your Personal list.",
        details = PermissionRequestDetails("Send investor update", "Oct 10, 2026 · 9:00 AM UTC", "Reminders · Personal"),
        state = state, alwaysAllowScope = "create reminders in Personal only.",
    )

    /** One shot per State, each Expanded then Collapsed (together the six Figma variants). */
    @Test
    fun chatPermissionCard() {
        PermissionCardState.entries.forEach { state ->
            shot("ChatPermissionCard-${state.name.lowercase()}-light") {
                RemTheme {
                    chatCardColumn {
                        listOf(true, false).forEach { expanded ->
                            PermissionCard(permissionRequest(state), expanded, {}, onAllow = {}, onDeny = {},
                                onAlwaysAllow = {}, onReviewAgain = {})
                        }
                    }
                }
            }
        }
    }

    @Test
    fun chatPermissionCardNarrow() = shot("ChatPermissionCard-narrow-light") {
        RemTheme {
            Box(Modifier.fillMaxSize().background(RemColors.current.backgroundPrimary).padding(16.dp)) {
                PermissionCard(permissionRequest(PermissionCardState.Awaiting), true, {}, onAllow = {}, onDeny = {},
                    onAlwaysAllow = {}, modifier = Modifier.width(288.dp))
            }
        }
    }

    @Composable
    private fun consentScreen() {
        OnboardingSequencer(
            steps = listOf(
                signInStep(state = SignInState.Returning("Sam"), onContinue = {}, onUseDifferentAccount = {}),
                consentStep(onAccept = {}, onOpenTerms = {}, onOpenPrivacy = {}),
            ),
            state = rememberOnboardingSequencerState(stepCount = 2, initialIndex = 1),
        )
    }
}
