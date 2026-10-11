import XCTest
@testable import RemDesignSystem

final class SettingsConnectorsFixtureTests: XCTestCase {
    private let second = GmailAccountFixture(id: "second", email: "second@example.com", role: "Secondary")
    func testConnectorPolicyInheritedExceptExplicitAccountOverride() {
        var fixture = GmailFixture(accounts: [.primary, second])
        fixture.setAccountPermission(.neverAllow, accountID: "avery")
        for policy in GmailPermission.allCases {
            fixture.setConnectorPermission(policy)
            XCTAssertEqual(fixture.connectorPermission, policy)
            XCTAssertEqual(fixture.permission(for: "avery"), .neverAllow)
            XCTAssertEqual(fixture.permission(for: "second"), policy)
        }
        for policy in GmailPermission.allCases {
            fixture.setAccountPermission(policy, accountID: "avery")
            XCTAssertEqual(fixture.permission(for: "avery"), policy)
            XCTAssertEqual(fixture.permission(for: "second"), .neverAllow)
        }
        fixture.setAccountPermission(.alwaysAllow, accountID: "missing")
        XCTAssertNil(fixture.accountOverrides["missing"])
    }
    func testSingleDisconnectPreservesOtherAccountAndConnectorPolicy() {
        var fixture = GmailFixture(accounts: [.primary, second])
        fixture.setConnectorPermission(.alwaysAsk)
        fixture.setAccountPermission(.neverAllow, accountID: "avery")
        fixture.setAccountPermission(.alwaysAllow, accountID: "second")
        fixture.disconnect(accountID: "avery")
        XCTAssertEqual(fixture.accounts, [second])
        XCTAssertEqual(fixture.connectorPermission, .alwaysAsk)
        XCTAssertNil(fixture.accountOverrides["avery"])
        XCTAssertEqual(fixture.permission(for: "second"), .alwaysAllow)
        fixture.disconnect(accountID: "missing")
        XCTAssertEqual(fixture.accounts, [second])
    }
    func testDisconnectAllRemovesEveryAccountAndOverrideButRetainsPolicy() {
        var fixture = GmailFixture(accounts: [.primary, second])
        fixture.setConnectorPermission(.neverAllow)
        fixture.setAccountPermission(.alwaysAllow, accountID: "second")
        fixture.disconnectAll()
        XCTAssertFalse(fixture.isConnected)
        XCTAssertTrue(fixture.accountOverrides.isEmpty)
        XCTAssertEqual(fixture.connectorPermission, .neverAllow)
    }
    func testDefaultFixtureIsOnlyDesignedAccount() {
        let fixture = GmailFixture()
        XCTAssertEqual(fixture.accounts.map(\.email), ["avery@example.com"])
        XCTAssertEqual(fixture.permission(for: "avery"), .lowRisk)
    }
}
