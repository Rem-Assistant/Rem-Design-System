package com.rem.designsystem.screens

// Presentation fixtures for the Settings pages reached from Settings and Agent settings:
// Automations `1833:5080`, Billing & Usage `1827:50803`, Permissions `1827:50821`,
// About `1827:50839` and Help & Support `2014:68798`. Copy is the exact source copy, shared 1:1
// with the SwiftUI `SettingsPagesFixture.swift`.
//
// Only presentation state is modelled. The design system has no automation runner, billing
// account, purchase flow, device-permission bridge, feedback service or shake detector, so every
// control that would need one explains that limitation instead of inventing success.

/** One authored automation row. Figma `1833:5079`. */
data class SettingsAutomation(val id: String, val title: String, val subtitle: String)

object SettingsAutomationsFixture {
    const val builtInHeader = "Built in"
    val builtIn: List<SettingsAutomation> = listOf(
        SettingsAutomation("dailyBrief", "Daily Brief", "Plans your day and follows up at the times you choose."),
    )
    const val builtInFooter =
        "Daily Brief is a built-in automation. Open it to choose its schedule, instructions, inputs, and outputs."
    fun boundary(automation: SettingsAutomation): String =
        "${automation.title}’s schedule, instructions, inputs, and outputs are not included in this prototype. No automation runs or changes."
}

/** One usage allowance. Figma `1827:50788` / `1827:50796`. */
data class SettingsUsageMeter(val id: String, val title: String, val used: Int, val limit: Int) {
    /** "8 / 20 used". Exact source format. */
    val label: String get() = "$used / $limit used"
    /** Progress in 0..1. A zero or negative limit reads as empty rather than dividing by zero. */
    val fraction: Float get() = if (limit <= 0) 0f else (used.toFloat() / limit).coerceIn(0f, 1f)
}

object SettingsBillingFixture {
    const val title = "Billing & Usage"
    const val planHeader = "Current Plan"
    const val planTitle = "Plan"
    const val plan = "Free"
    const val usageHeader = "Usage"
    val usage: List<SettingsUsageMeter> = listOf(
        SettingsUsageMeter("today", "Today", 8, 20),
        SettingsUsageMeter("month", "This Month", 120, 300),
    )
    const val upgradeTitle = "Upgrade to Pro"
    const val upgradeBoundary =
        "Purchases are not included in this prototype. No payment is made and your plan does not change."
}

/** Authored permission status. Figma `PermissionStatusBadge` `383:2` / `383:11` plus Denied. */
enum class SettingsPermissionStatus(val title: String) { Enabled("Enabled"), NotSet("Not Set"), Denied("Denied") }

enum class SettingsPermissionGlyph { Notifications, Calendar, Reminders, Microphone, SpeechRecognition, Camera }

data class SettingsPermission(val id: String, val title: String, val glyph: SettingsPermissionGlyph, val status: SettingsPermissionStatus)

data class SettingsPermissionSection(val id: String, val header: String?, val footer: String, val permissions: List<SettingsPermission>)

object SettingsPermissionsFixture {
    val sections: List<SettingsPermissionSection> = listOf(
        SettingsPermissionSection("notifications", null,
            "Tap a row to grant access. If denied, you'll be taken to Settings.",
            listOf(SettingsPermission("notifications", "Notifications", SettingsPermissionGlyph.Notifications, SettingsPermissionStatus.NotSet))),
        SettingsPermissionSection("deviceData", "Device Data",
            "Allow Rem to view and edit your calendars and reminders on your behalf.",
            listOf(
                SettingsPermission("calendar", "Calendar", SettingsPermissionGlyph.Calendar, SettingsPermissionStatus.Enabled),
                SettingsPermission("reminders", "Reminders", SettingsPermissionGlyph.Reminders, SettingsPermissionStatus.NotSet),
            )),
        SettingsPermissionSection("mediaVoice", "Media & Voice",
            "Needed for voice conversations and scanning QR codes.",
            listOf(
                SettingsPermission("microphone", "Microphone", SettingsPermissionGlyph.Microphone, SettingsPermissionStatus.Enabled),
                SettingsPermission("speechRecognition", "Speech Recognition", SettingsPermissionGlyph.SpeechRecognition, SettingsPermissionStatus.NotSet),
                SettingsPermission("camera", "Camera", SettingsPermissionGlyph.Camera, SettingsPermissionStatus.Denied),
            )),
    )

    /**
     * The design system has no device-permission bridge or open-Settings pattern, so the row explains
     * the boundary rather than prompting the OS or leaving the app.
     */
    fun boundary(permission: SettingsPermission): String =
        "${permission.title} access is requested by the shipping app through the system prompt. This prototype does not request access or open system Settings; the status shown is illustrative."
}

/** One About legal row. Summaries reuse the onboarding legal rows; real copy belongs to the app. */
data class SettingsLegalDocument(val id: String, val title: String, val summary: String)

object SettingsAboutFixture {
    const val appName = "Rem"
    const val tagline = "Turn your thoughts into actions"
    const val legalHeader = "LEGAL"
    val legal: List<SettingsLegalDocument> = listOf(
        SettingsLegalDocument("terms", "Terms of Service", "How Rem accounts, subscriptions, and approved actions work."),
        SettingsLegalDocument("privacy", "Privacy Policy", "What Rem, your gateway, and AI or voice providers process."),
    )
    const val versionTitle = "Version"
    /** The source value. Hosts pass their real build version instead. */
    const val sourceVersion = "1.4.0 (128)"

    /** "1.4.0 (128)". Missing or blank parts fall back to the available part, never to fake data. */
    fun versionLabel(short: String?, build: String?): String {
        val s = short?.trim().orEmpty()
        val b = build?.trim().orEmpty()
        return when {
            s.isNotEmpty() && b.isNotEmpty() -> "$s ($b)"
            s.isNotEmpty() -> s
            b.isNotEmpty() -> "($b)"
            else -> "Unknown"
        }
    }
}

enum class SettingsHelpDestination(val id: String, val title: String, val boundary: String) {
    SendFeedback("sendFeedback", "Send Feedback", "The Send Feedback form is not included in this prototype. Nothing is sent."),
    ReportBug("reportBug", "Report a Bug", "The bug report form is not included in this prototype. No report is sent."),
}

object SettingsHelpFixture {
    const val shakeTitle = "Shake to report an issue"
    /** Source default: on. The preference is local to the playground session. */
    const val shakeDefault = true
    /** Source footer, followed by the honest limitation: shake detection is not implemented here. */
    const val shakeFooter =
        "Shake your phone to open the Send Feedback form. Shake detection is not included in this prototype."
}
