package com.rem.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.agents.AgentKind
import com.rem.designsystem.agents.AgentLook
import com.rem.designsystem.agents.AgentProfileAbout
import com.rem.designsystem.agents.AgentProfileDisplay
import com.rem.designsystem.agents.AgentProfileHeader
import com.rem.designsystem.agents.AgentService
import com.rem.designsystem.agents.AgentServiceGlyph
import com.rem.designsystem.agents.NewAgentDraft
import com.rem.designsystem.agents.NewAgentSetupScreen
import com.rem.designsystem.navigation.RemAccountPill
import com.rem.designsystem.navigation.RemSidebar
import com.rem.designsystem.navigation.RemSidebarAccount
import com.rem.designsystem.navigation.RemSidebarAgent
import com.rem.designsystem.navigation.RemSidebarContainer
import com.rem.designsystem.navigation.RemSidebarDisplay
import com.rem.designsystem.navigation.RemSidebarSelection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography
import org.junit.Rule
import org.junit.Test

/** Fixtures matching `SidebarRenderSnapshots.swift` so the paired renders show the same content. */
internal object SidebarFixtures {
    val sidebar = RemSidebarDisplay(
        mainAgent = RemSidebarAgent("rem", "Rem", "Daily Brief ready"),
        sideAgents = listOf(
            RemSidebarAgent("health", "Health", "Appointments & meds"),
            RemSidebarAgent("money", "Money", "Bills & budget", hasUnread = true),
            RemSidebarAgent("travel", "Travel planning: Lisbon in May", "Flights, stays"),
            RemSidebarAgent("jobs", "Job search: recruiter follow-ups and interview prep", "Applications & interviews"),
            RemSidebarAgent("home", "Home & errands", "Chores & repairs"),
            RemSidebarAgent("spanish", "Learning Spanish", "Daily practice"),
            RemSidebarAgent("gifts", "Gift ideas for Mom's birthday", "Gift ideas & reminders"),
        ),
        account = RemSidebarAccount("Samuel"),
        selection = RemSidebarSelection.Main,
    )

    val calendar = AgentService("gcal", "Google Calendar", "Connected", AgentServiceGlyph.Calendar)

    val healthProfile = AgentProfileDisplay(
        kind = AgentKind.Side, name = "Health", look = AgentLook.Default, laneSummary = "Appointments and refills",
        handles = "Appointments, prescriptions, insurance claims", services = listOf(calendar),
    )

    val remProfile = AgentProfileDisplay(
        kind = AgentKind.Main, name = "Rem", laneSummary = "Daily Brief, sees all your lanes",
        handles = "Daily Brief, sees all your lanes",
    )

    val filledDraft = NewAgentDraft(name = "Health", handles = "Appointments, prescriptions, insurance claims")
}

/**
 * Paparazzi evidence for the sidebar + New agent flow, keyed to pair with `SidebarRenderSnapshots.swift`.
 * Kept separate from `EvidenceSnapshots.kt` so these surfaces don't edit the shared gallery.
 */
class SidebarSnapshots {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private fun shot(name: String, content: @Composable () -> Unit) =
        paparazzi.snapshot(name = name, composable = content)

    @Test
    fun sidebarOpen() {
        shot("Sidebar-open-light") { RemTheme { container(isOpen = true) } }
        shot("Sidebar-open-dark") { RemTheme(darkTheme = true) { container(isOpen = true) } }
    }

    @Test
    fun sidebarClosed() = shot("Sidebar-closed-light") { RemTheme { container(isOpen = false) } }

    @Test
    fun sidebarEmpty() = shot("Sidebar-empty-light") {
        RemTheme {
            Box(Modifier.width(332.dp)) {
                RemSidebar(
                    display = RemSidebarDisplay(SidebarFixtures.sidebar.mainAgent, emptyList(), RemSidebarAccount("Samuel")),
                    onAction = {},
                )
            }
        }
    }

    @Test
    fun accountPill() = shot("AccountPill-light") {
        RemTheme {
            Box(Modifier.fillMaxSize().background(RemColors.current.backgroundSecondary), contentAlignment = Alignment.Center) {
                RemAccountPill(RemSidebarAccount("Samuel"), onClick = {})
            }
        }
    }

    @Test
    fun newAgentEmpty() = shot("NewAgent-empty-light") {
        RemTheme { NewAgentSetupScreen(NewAgentDraft(), onDraftChange = {}, onAction = {}, focusesNameOnAppear = false) }
    }

    @Test
    fun newAgentFilled() {
        shot("NewAgent-filled-light") {
            RemTheme { NewAgentSetupScreen(SidebarFixtures.filledDraft, onDraftChange = {}, onAction = {}, focusesNameOnAppear = false) }
        }
        shot("NewAgent-filled-dark") {
            RemTheme(darkTheme = true) {
                NewAgentSetupScreen(SidebarFixtures.filledDraft, onDraftChange = {}, onAction = {}, focusesNameOnAppear = false)
            }
        }
    }

    @Test
    fun agentProfileSide() = shot("AgentProfile-side-about-light") { RemTheme { profile(SidebarFixtures.healthProfile) } }

    @Test
    fun agentProfileMain() = shot("AgentProfile-main-about-light") { RemTheme { profile(SidebarFixtures.remProfile) } }

    @Composable
    private fun container(isOpen: Boolean) {
        RemSidebarContainer(
            isOpen = isOpen,
            onOpenChange = {},
            sidebar = { RemSidebar(SidebarFixtures.sidebar, onAction = {}) },
        ) { chatStandIn() }
    }

    // Header + About body as Agent activity will host them (tab control belongs to AgentActivityScreen).
    @Composable
    private fun profile(display: AgentProfileDisplay) {
        Column(
            Modifier
                .fillMaxSize()
                .background(RemColors.current.backgroundPrimary)
                .verticalScroll(rememberScrollState())
                .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
        ) {
            AgentProfileHeader(display)
            AgentProfileAbout(display, onAction = {})
        }
    }

    // Neutral stand-in for the pushed chat (the real chat chassis is not on main).
    @Composable
    private fun chatStandIn() {
        val colors = RemColors.current
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.backgroundPrimary)
                .padding(start = RemSpacing.lg, end = RemSpacing.lg, top = 60.dp),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(colors.backgroundSecondary))
            Text("Good morning, Samuel", style = RemTypography.title3Bold, color = colors.labelPrimary)
            Text(
                "Three things today: the venue replied about Saturday, two threads still need you, and your 2:00 PM moved to 3:00.",
                style = RemTypography.subheadline,
                color = colors.labelSecondary,
            )
        }
    }
}
