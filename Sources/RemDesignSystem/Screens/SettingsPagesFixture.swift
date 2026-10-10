import Foundation

// Presentation fixtures for the Settings pages reached from Settings and Agent settings:
// Automations `1833:5080`, Billing & Usage `1827:50803`, Permissions `1827:50821`,
// About `1827:50839` and Help & Support `2014:68798`. Copy is the exact source copy.
//
// Only presentation state is modelled. The design system has no automation runner, billing
// account, purchase flow, device-permission bridge, feedback service or shake detector, so every
// control that would need one explains that limitation instead of inventing success.

/// One authored automation row. Figma `1833:5079`.
public struct SettingsAutomation: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let subtitle: String
}

public enum SettingsAutomationsFixture {
    public static let builtInHeader = "Built in"
    public static let builtIn: [SettingsAutomation] = [
        .init(id: "dailyBrief", title: "Daily Brief",
              subtitle: "Plans your day and follows up at the times you choose."),
    ]
    public static let builtInFooter =
        "Daily Brief is a built-in automation. Open it to choose its schedule, instructions, inputs, and outputs."
    public static func boundary(for automation: SettingsAutomation) -> String {
        "\(automation.title)’s schedule, instructions, inputs, and outputs are not included in this prototype. No automation runs or changes."
    }
}

/// One usage allowance. Figma `1827:50788` / `1827:50796`.
public struct SettingsUsageMeter: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let used: Int
    public let limit: Int
    public init(id: String, title: String, used: Int, limit: Int) {
        self.id = id; self.title = title; self.used = used; self.limit = limit
    }
    /// "8 / 20 used". Exact source format.
    public var label: String { "\(used) / \(limit) used" }
    /// Progress in 0...1. A zero or negative limit reads as empty rather than dividing by zero.
    public var fraction: Double {
        guard limit > 0 else { return 0 }
        return min(max(Double(used) / Double(limit), 0), 1)
    }
}

public enum SettingsBillingFixture {
    public static let title = "Billing & Usage"
    public static let planHeader = "Current Plan"
    public static let planTitle = "Plan"
    public static let plan = "Free"
    public static let usageHeader = "Usage"
    public static let usage: [SettingsUsageMeter] = [
        .init(id: "today", title: "Today", used: 8, limit: 20),
        .init(id: "month", title: "This Month", used: 120, limit: 300),
    ]
    public static let upgradeTitle = "Upgrade to Pro"
    public static let upgradeBoundary =
        "Purchases are not included in this prototype. No payment is made and your plan does not change."
}

/// Authored permission status. Figma `PermissionStatusBadge` `383:2` / `383:11` plus Denied.
public enum SettingsPermissionStatus: String, CaseIterable, Sendable {
    case enabled, notSet, denied
    public var title: String {
        switch self {
        case .enabled: "Enabled"
        case .notSet: "Not Set"
        case .denied: "Denied"
        }
    }
}

public struct SettingsPermission: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let symbol: String
    public let status: SettingsPermissionStatus
}

public struct SettingsPermissionSection: Identifiable, Hashable, Sendable {
    public let id: String
    public let header: String?
    public let footer: String
    public let permissions: [SettingsPermission]
}

public enum SettingsPermissionsFixture {
    public static let sections: [SettingsPermissionSection] = [
        .init(id: "notifications", header: nil,
              footer: "Tap a row to grant access. If denied, you'll be taken to Settings.",
              permissions: [.init(id: "notifications", title: "Notifications", symbol: "bell.fill", status: .notSet)]),
        .init(id: "deviceData", header: "Device Data",
              footer: "Allow Rem to view and edit your calendars and reminders on your behalf.",
              permissions: [
                .init(id: "calendar", title: "Calendar", symbol: "calendar", status: .enabled),
                .init(id: "reminders", title: "Reminders", symbol: "list.bullet", status: .notSet),
              ]),
        .init(id: "mediaVoice", header: "Media & Voice",
              footer: "Needed for voice conversations and scanning QR codes.",
              permissions: [
                // Source names `microphone.fill`; `mic.fill` is the same glyph on the iOS 17 floor.
                .init(id: "microphone", title: "Microphone", symbol: "mic.fill", status: .enabled),
                .init(id: "speechRecognition", title: "Speech Recognition", symbol: "waveform", status: .notSet),
                .init(id: "camera", title: "Camera", symbol: "camera.fill", status: .denied),
              ]),
    ]
    /// The design system has no device-permission bridge or open-Settings pattern, so the row
    /// explains the boundary rather than prompting the OS or leaving the app.
    public static func boundary(for permission: SettingsPermission) -> String {
        "\(permission.title) access is requested by the shipping app through the system prompt. This prototype does not request access or open system Settings; the status shown is illustrative."
    }
}

/// One About legal row. Summaries reuse the onboarding legal rows; real copy belongs to the app.
public struct SettingsLegalDocument: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let summary: String
}

public enum SettingsAboutFixture {
    public static let appName = "Rem"
    public static let tagline = "Turn your thoughts into actions"
    public static let legalHeader = "LEGAL"
    public static let legal: [SettingsLegalDocument] = [
        .init(id: "terms", title: "Terms of Service",
              summary: "How Rem accounts, subscriptions, and approved actions work."),
        .init(id: "privacy", title: "Privacy Policy",
              summary: "What Rem, your gateway, and AI or voice providers process."),
    ]
    public static let versionTitle = "Version"
    /// The source value. Hosts pass their real bundle version instead.
    public static let sourceVersion = "1.4.0 (128)"
    /// "1.4.0 (128)". Missing or blank parts fall back to the available part, never to fake data.
    public static func versionLabel(short: String?, build: String?) -> String {
        let short = short?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let build = build?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        switch (short.isEmpty, build.isEmpty) {
        case (false, false): return "\(short) (\(build))"
        case (false, true): return short
        case (true, false): return "(\(build))"
        case (true, true): return "Unknown"
        }
    }
}

public enum SettingsHelpDestination: String, CaseIterable, Identifiable, Sendable {
    case sendFeedback, reportBug
    public var id: String { rawValue }
    public var title: String { self == .sendFeedback ? "Send Feedback" : "Report a Bug" }
    public var symbol: String { self == .sendFeedback ? "bubble.left" : "ant.circle" }
    public var boundary: String {
        switch self {
        case .sendFeedback: "The Send Feedback form is not included in this prototype. Nothing is sent."
        case .reportBug: "The bug report form is not included in this prototype. No report is sent."
        }
    }
}

/// Help & Support presentation state. The shake preference is local to this playground session.
public struct SettingsHelpFixture: Equatable, Sendable {
    public var shakeToReport: Bool
    public init(shakeToReport: Bool = true) { self.shakeToReport = shakeToReport }
    public static let shakeTitle = "Shake to report an issue"
    /// Source footer, followed by the honest limitation: shake detection is not implemented here.
    public static let shakeFooter =
        "Shake your phone to open the Send Feedback form. Shake detection is not included in this prototype."
}
