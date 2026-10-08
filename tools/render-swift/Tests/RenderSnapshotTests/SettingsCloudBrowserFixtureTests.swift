import XCTest
@testable import RemDesignSystem

/// Fixture-state proof for the SwiftUI Cloud browser destination. These assert the scope/cancel/save
/// rules the authored masters require, without a simulator — the same contract the Compose
/// `SettingsCloudBrowserFixtureTest` proves on Android.
@MainActor
final class SettingsCloudBrowserFixtureTests: XCTestCase {

    private func id(_ domain: String, in model: CloudBrowserModel) -> UUID {
        model.sites.first { $0.domain == domain }!.id
    }

    func testDeriveDomainStripsSchemeAndPath() {
        XCTAssertEqual(CloudBrowserModel.deriveDomain(from: "https://example.com/path?q=1#x"), "example.com")
        XCTAssertEqual(CloudBrowserModel.deriveDomain(from: "HTTP://user@sub.example.co.uk:8080/"), "sub.example.co.uk")
        XCTAssertEqual(CloudBrowserModel.deriveDomain(from: "  github.com  "), "github.com")
    }

    func testDeriveDomainRejectsUnusableInput() {
        XCTAssertNil(CloudBrowserModel.deriveDomain(from: ""))
        XCTAssertNil(CloudBrowserModel.deriveDomain(from: "   "))
        XCTAssertNil(CloudBrowserModel.deriveDomain(from: "not a domain"))
        XCTAssertNil(CloudBrowserModel.deriveDomain(from: "localhost"))
        XCTAssertNil(CloudBrowserModel.deriveDomain(from: "https://"))
        XCTAssertNil(CloudBrowserModel.deriveDomain(from: "a..b"))
    }

    func testAddSiteValidatesDomainAndAppends() {
        let model = CloudBrowserModel()
        let before = model.sites.count
        XCTAssertFalse(CloudBrowserModel.canAddSite(urlDraft: ""))
        XCTAssertNil(model.addSite(urlDraft: "", permission: .ask))
        XCTAssertEqual(model.sites.count, before)

        XCTAssertTrue(CloudBrowserModel.canAddSite(urlDraft: "https://acme.dev"))
        let newID = model.addSite(urlDraft: "https://acme.dev/login", permission: .allow)
        XCTAssertNotNil(newID)
        let added = model.site(newID!)!
        XCTAssertEqual(added.domain, "acme.dev")
        XCTAssertEqual(added.permission, .allow)
        XCTAssertEqual(model.sites.count, before + 1)
        XCTAssertTrue(added.logins.isEmpty)
    }

    func testAddSiteWithUsernameCreatesOneOptionalLogin() {
        let model = CloudBrowserModel()
        let newID = model.addSite(urlDraft: "beta.io", permission: .ask, username: "me@beta.io", password: "pw")!
        XCTAssertEqual(model.site(newID)!.logins.count, 1)
        XCTAssertEqual(model.site(newID)!.logins.first!.username, "me@beta.io")
    }

    func testRowSubtitleMatchesAuthoredFixtures() {
        let model = CloudBrowserModel()
        XCTAssertEqual(model.sites[0].rowSubtitle, "Ask · 1 saved login")   // github.com
        XCTAssertEqual(model.sites[1].rowSubtitle, "Allow · Signed in")      // notion.so
        XCTAssertEqual(model.sites[2].rowSubtitle, "Ask · No saved login")   // linear.app
    }

    func testSetPermissionIsScopedToOneSite() {
        let model = CloudBrowserModel()
        model.setPermission(.allow, for: id("github.com", in: model))
        XCTAssertEqual(model.sites.first { $0.domain == "github.com" }!.permission, .allow)
        XCTAssertEqual(model.sites.first { $0.domain == "linear.app" }!.permission, .ask) // untouched
    }

    func testAddLoginRequiresBothFields() {
        let model = CloudBrowserModel()
        let linear = id("linear.app", in: model)
        XCTAssertFalse(CloudBrowserModel.canAddLogin(username: "", password: "pw"))
        XCTAssertFalse(CloudBrowserModel.canAddLogin(username: "u", password: ""))
        XCTAssertNil(model.addLogin(siteID: linear, username: "", password: "pw"))
        XCTAssertTrue(model.site(linear)!.logins.isEmpty)

        XCTAssertTrue(CloudBrowserModel.canAddLogin(username: "dev@linear.app", password: "pw"))
        XCTAssertNotNil(model.addLogin(siteID: linear, username: "dev@linear.app", password: "pw"))
        XCTAssertEqual(model.site(linear)!.logins.count, 1)
    }

    func testEditUpdatesOnlySelectedCredential() {
        let model = CloudBrowserModel()
        let github = id("github.com", in: model)
        let second = model.addLogin(siteID: github, username: "alt@github.com", password: "pw")!
        let first = model.site(github)!.logins.first!.id

        model.updateUsername(siteID: github, loginID: first, to: "changed@github.com")
        XCTAssertEqual(model.site(github)!.logins.first { $0.id == first }!.username, "changed@github.com")
        XCTAssertEqual(model.site(github)!.logins.first { $0.id == second }!.username, "alt@github.com")

        model.updatePassword(siteID: github, loginID: first, to: "new-secret")
        XCTAssertEqual(model.site(github)!.logins.first { $0.id == first }!.password, "new-secret")
        // Display stays masked regardless of value.
        XCTAssertEqual(model.site(github)!.logins.first { $0.id == first }!.maskedPassword, String(repeating: "•", count: 12))
    }

    func testRemoveLoginAffectsOnlyThatLoginAndPreservesCookies() {
        let model = CloudBrowserModel()
        let github = id("github.com", in: model)
        let extra = model.addLogin(siteID: github, username: "alt@github.com", password: "pw")!
        let cookiesBefore = model.site(github)!.cookieCount
        model.removeLogin(siteID: github, loginID: extra)
        XCTAssertEqual(model.site(github)!.logins.count, 1)
        XCTAssertEqual(model.site(github)!.logins.first!.username, "samuel@example.com")
        XCTAssertEqual(model.site(github)!.cookieCount, cookiesBefore)
        XCTAssertTrue(model.site(github)!.signedIn)
    }

    func testClearSiteDataZeroesCookiesButKeepsPasswords() {
        let model = CloudBrowserModel()
        let github = id("github.com", in: model)
        model.clearSiteData(siteID: github)
        let site = model.site(github)!
        XCTAssertEqual(site.cookieCount, 0)
        XCTAssertFalse(site.signedIn)
        XCTAssertEqual(site.logins.count, 1) // credential preserved
        XCTAssertEqual(model.sites.first { $0.domain == "notion.so" }!.cookieCount, 8) // others untouched
    }

    func testClearAllSiteDataKeepsEveryPassword() {
        let model = CloudBrowserModel()
        model.clearAllSiteData()
        XCTAssertTrue(model.sites.allSatisfy { $0.cookieCount == 0 && !$0.signedIn })
        XCTAssertEqual(model.sites.first { $0.domain == "github.com" }!.logins.count, 1)
    }
}
