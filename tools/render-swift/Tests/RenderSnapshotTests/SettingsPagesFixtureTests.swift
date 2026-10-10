import XCTest
@testable import RemDesignSystem

final class SettingsPagesFixtureTests: XCTestCase {
    func testEveryPageIsRoutableFromSettingsOrAgentSettings() {
        XCTAssertEqual(Set(SettingsEntryDestination.allCases.map(\.rawValue)),
                       ["agentSettings", "billing", "permissions", "about", "helpSupport"])
        XCTAssertTrue(AgentSettingsDestination.allCases.contains(.automations))
        XCTAssertEqual(AgentSettingsDestination.allCases.count, 8)
    }

    func testAutomationsUsesSourceCopyAndExplainsTheMissingRunner() {
        XCTAssertEqual(SettingsAutomationsFixture.builtInHeader, "Built in")
        XCTAssertEqual(SettingsAutomationsFixture.builtIn.map(\.title), ["Daily Brief"])
        XCTAssertEqual(SettingsAutomationsFixture.builtIn.first?.subtitle,
                       "Plans your day and follows up at the times you choose.")
        let boundary = SettingsAutomationsFixture.boundary(for: SettingsAutomationsFixture.builtIn[0])
        XCTAssertTrue(boundary.contains("not included in this prototype"))
        XCTAssertTrue(boundary.contains("No automation runs"))
    }

    func testBillingMetersMatchSourceProgressAndNeverOverflow() {
        let usage = SettingsBillingFixture.usage
        XCTAssertEqual(usage.map(\.label), ["8 / 20 used", "120 / 300 used"])
        // Source tracks fill 135 of 338 points for both meters.
        for meter in usage { XCTAssertEqual(meter.fraction, 0.4, accuracy: 0.0001) }
        XCTAssertEqual(SettingsUsageMeter(id: "x", title: "x", used: 5, limit: 0).fraction, 0)
        XCTAssertEqual(SettingsUsageMeter(id: "x", title: "x", used: 50, limit: 20).fraction, 1)
        XCTAssertEqual(SettingsUsageMeter(id: "x", title: "x", used: -1, limit: 20).fraction, 0)
        XCTAssertEqual(SettingsBillingFixture.plan, "Free")
        XCTAssertEqual(SettingsBillingFixture.upgradeTitle, "Upgrade to Pro")
        XCTAssertTrue(SettingsBillingFixture.upgradeBoundary.contains("No payment is made"))
    }

    func testPermissionsKeepSourceOrderStatusesAndHonestBoundary() {
        let sections = SettingsPermissionsFixture.sections
        XCTAssertEqual(sections.map(\.header), [nil, "Device Data", "Media & Voice"])
        XCTAssertEqual(sections.flatMap(\.permissions).map(\.title),
                       ["Notifications", "Calendar", "Reminders", "Microphone", "Speech Recognition", "Camera"])
        XCTAssertEqual(sections.flatMap(\.permissions).map(\.status),
                       [.notSet, .enabled, .notSet, .enabled, .notSet, .denied])
        XCTAssertEqual(SettingsPermissionStatus.allCases.map(\.title), ["Enabled", "Not Set", "Denied"])
        for permission in sections.flatMap(\.permissions) {
            let message = SettingsPermissionsFixture.boundary(for: permission)
            XCTAssertTrue(message.hasPrefix(permission.title))
            XCTAssertTrue(message.contains("does not request access or open system Settings"))
        }
    }

    func testAboutVersionLabelUsesRealPartsWithoutInventingValues() {
        XCTAssertEqual(SettingsAboutFixture.versionLabel(short: "1.4.0", build: "128"), SettingsAboutFixture.sourceVersion)
        XCTAssertEqual(SettingsAboutFixture.versionLabel(short: " 2.0 ", build: nil), "2.0")
        XCTAssertEqual(SettingsAboutFixture.versionLabel(short: nil, build: "7"), "(7)")
        XCTAssertEqual(SettingsAboutFixture.versionLabel(short: "", build: " "), "Unknown")
        XCTAssertEqual(SettingsAboutFixture.legal.map(\.title), ["Terms of Service", "Privacy Policy"])
        XCTAssertEqual(SettingsAboutFixture.legalHeader, "LEGAL")
    }

    func testHelpKeepsSeparateDestinationsAndALocalShakePreference() {
        XCTAssertEqual(SettingsHelpDestination.allCases.map(\.title), ["Send Feedback", "Report a Bug"])
        for destination in SettingsHelpDestination.allCases {
            XCTAssertTrue(destination.boundary.contains("not included in this prototype"))
        }
        var fixture = SettingsHelpFixture()
        XCTAssertTrue(fixture.shakeToReport, "Source default is On")
        fixture.shakeToReport = false
        XCTAssertNotEqual(fixture, SettingsHelpFixture())
        XCTAssertTrue(SettingsHelpFixture.shakeFooter.hasPrefix("Shake your phone to open the Send Feedback form."))
        XCTAssertTrue(SettingsHelpFixture.shakeFooter.contains("Shake detection is not included"))
    }
}
