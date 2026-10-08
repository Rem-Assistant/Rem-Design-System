import XCTest
@testable import RemDesignSystem

final class SettingsWalletFixtureTests: XCTestCase {
    func testConsentIsRequiredBeforeAnExternalBoundary() {
        var fixture = SettingsWalletFixture()
        fixture.connect()
        XCTAssertEqual(fixture.stage, .root)
        XCTAssertNil(fixture.provider)
        for provider in SettingsWalletProvider.allCases {
            fixture.open(provider)
            XCTAssertEqual(fixture.stage, .consent)
            XCTAssertEqual(fixture.provider, provider)
            fixture.connect()
            XCTAssertEqual(fixture.stage, .external)
            XCTAssertEqual(fixture.provider?.domain, provider == .link ? "app.link.com" : "shop.app")
            fixture.dismiss()
        }
    }

    func testCancelAndExternalCloseReturnToUnconnectedWallet() {
        for provider in SettingsWalletProvider.allCases {
            var fixture = SettingsWalletFixture()
            fixture.open(provider)
            fixture.dismiss()
            XCTAssertEqual(fixture, SettingsWalletFixture())
            fixture.open(provider)
            fixture.connect()
            fixture.dismiss()
            XCTAssertEqual(fixture, SettingsWalletFixture())
            fixture.open(provider)
            XCTAssertEqual(fixture.stage, .consent, "Reopening must ask again, never imply authentication")
        }
    }

    func testChoosingAnotherProviderNeverReusesConsentOrChangesProviderCopy() {
        var fixture = SettingsWalletFixture()
        fixture.open(.link)
        fixture.connect()
        fixture.open(.shopPay)
        XCTAssertEqual(fixture.stage, .consent)
        XCTAssertEqual(fixture.provider, .shopPay)
        XCTAssertEqual(fixture.provider?.consentBody, "Connect your Shop Pay wallet for purchases you ask Rem to make.")
        XCTAssertTrue(fixture.provider?.benefits.first?.body.contains("through Shop Pay.") == true)
        XCTAssertTrue(fixture.provider?.disclosure.contains("continue to Shop Pay") == true)
    }
}
