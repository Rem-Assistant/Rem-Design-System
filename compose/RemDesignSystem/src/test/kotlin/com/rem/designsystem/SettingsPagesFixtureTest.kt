package com.rem.designsystem

import com.rem.designsystem.screens.AgentSettingsDestination
import com.rem.designsystem.screens.SettingsAboutFixture
import com.rem.designsystem.screens.SettingsAutomationsFixture
import com.rem.designsystem.screens.SettingsBillingFixture
import com.rem.designsystem.screens.SettingsEntryDestination
import com.rem.designsystem.screens.SettingsHelpDestination
import com.rem.designsystem.screens.SettingsHelpFixture
import com.rem.designsystem.screens.SettingsPermissionStatus
import com.rem.designsystem.screens.SettingsPermissionsFixture
import com.rem.designsystem.screens.SettingsUsageMeter
import org.junit.Assert.*
import org.junit.Test

class SettingsPagesFixtureTest {
    @Test fun everyPageIsRoutableFromSettingsOrAgentSettings() {
        assertEquals(listOf("Billing", "Permissions", "About", "HelpSupport"), SettingsEntryDestination.entries.map { it.name })
        assertTrue(AgentSettingsDestination.Automations in AgentSettingsDestination.entries)
        assertEquals(8, AgentSettingsDestination.entries.size)
    }

    @Test fun automationsUsesSourceCopyAndExplainsTheMissingRunner() {
        assertEquals("Built in", SettingsAutomationsFixture.builtInHeader)
        assertEquals(listOf("Daily Brief"), SettingsAutomationsFixture.builtIn.map { it.title })
        assertEquals("Plans your day and follows up at the times you choose.", SettingsAutomationsFixture.builtIn.first().subtitle)
        val boundary = SettingsAutomationsFixture.boundary(SettingsAutomationsFixture.builtIn.first())
        assertTrue(boundary.contains("not included in this prototype"))
        assertTrue(boundary.contains("No automation runs"))
    }

    @Test fun billingMetersMatchSourceProgressAndNeverOverflow() {
        val usage = SettingsBillingFixture.usage
        assertEquals(listOf("8 / 20 used", "120 / 300 used"), usage.map { it.label })
        // Source tracks fill 135 of 338 points for both meters.
        usage.forEach { assertEquals(0.4f, it.fraction, 0.0001f) }
        assertEquals(0f, SettingsUsageMeter("x", "x", 5, 0).fraction, 0f)
        assertEquals(1f, SettingsUsageMeter("x", "x", 50, 20).fraction, 0f)
        assertEquals(0f, SettingsUsageMeter("x", "x", -1, 20).fraction, 0f)
        assertEquals("Free", SettingsBillingFixture.plan)
        assertEquals("Upgrade to Pro", SettingsBillingFixture.upgradeTitle)
        assertTrue(SettingsBillingFixture.upgradeBoundary.contains("No payment is made"))
    }

    @Test fun permissionsKeepSourceOrderStatusesAndHonestBoundary() {
        val sections = SettingsPermissionsFixture.sections
        assertEquals(listOf(null, "Device Data", "Media & Voice"), sections.map { it.header })
        val permissions = sections.flatMap { it.permissions }
        assertEquals(listOf("Notifications", "Calendar", "Reminders", "Microphone", "Speech Recognition", "Camera"), permissions.map { it.title })
        assertEquals(
            listOf(SettingsPermissionStatus.NotSet, SettingsPermissionStatus.Enabled, SettingsPermissionStatus.NotSet,
                SettingsPermissionStatus.Enabled, SettingsPermissionStatus.NotSet, SettingsPermissionStatus.Denied),
            permissions.map { it.status })
        assertEquals(listOf("Enabled", "Not Set", "Denied"), SettingsPermissionStatus.entries.map { it.title })
        permissions.forEach {
            val message = SettingsPermissionsFixture.boundary(it)
            assertTrue(message.startsWith(it.title))
            assertTrue(message.contains("does not request access or open system Settings"))
        }
    }

    @Test fun aboutVersionLabelUsesRealPartsWithoutInventingValues() {
        assertEquals(SettingsAboutFixture.sourceVersion, SettingsAboutFixture.versionLabel("1.4.0", "128"))
        assertEquals("2.0", SettingsAboutFixture.versionLabel(" 2.0 ", null))
        assertEquals("(7)", SettingsAboutFixture.versionLabel(null, "7"))
        assertEquals("Unknown", SettingsAboutFixture.versionLabel("", " "))
        assertEquals(listOf("Terms of Service", "Privacy Policy"), SettingsAboutFixture.legal.map { it.title })
        assertEquals("LEGAL", SettingsAboutFixture.legalHeader)
    }

    @Test fun helpKeepsSeparateDestinationsAndALocalShakePreference() {
        assertEquals(listOf("Send Feedback", "Report a Bug"), SettingsHelpDestination.entries.map { it.title })
        SettingsHelpDestination.entries.forEach { assertTrue(it.boundary.contains("not included in this prototype")) }
        assertTrue("Source default is On", SettingsHelpFixture.shakeDefault)
        assertTrue(SettingsHelpFixture.shakeFooter.startsWith("Shake your phone to open the Send Feedback form."))
        assertTrue(SettingsHelpFixture.shakeFooter.contains("Shake detection is not included"))
    }
}
