package com.rem.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.rem.designsystem.onboarding.LegalDocumentScreen
import com.rem.designsystem.onboarding.OnboardingSequencer
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
import com.rem.designsystem.chat.MessageBubble
import com.rem.designsystem.chat.MessageRole
import com.rem.designsystem.chat.RemComposerBar
import com.rem.designsystem.chat.ComposerSendState
import com.rem.designsystem.screens.AgendaScreen
import com.rem.designsystem.screens.AddTaskField
import com.rem.designsystem.screens.InboxScreen
import com.rem.designsystem.screens.ChatScreen
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
import com.rem.designsystem.onboarding.OnboardingVoiceScreen
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

    @Test
    fun voice() = shot("LegacyOnboardingVoice-light") {
        RemTheme {
            OnboardingVoiceScreen(
                voiceName = "Aria",
                selectedVoice = "Aria (Warm)",
                onHearVoice = {}, onSelectVoice = {},
                speed = 0.45f, onSpeedChange = {},
                consistency = 0.7f, onConsistencyChange = {},
                likeness = 0.6f, onLikenessChange = {},
            )
        }
    }

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
