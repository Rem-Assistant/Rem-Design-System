import XCTest

final class SettingsPlaygroundUITests: XCTestCase {
    let app = XCUIApplication()
    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--settings-light"]
        app.launch()
    }
    private func capture(_ name: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(0.5)) // Allow native navigation chrome to finish its transition.
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
    private func openSettings(_ fixture: String = "Success") {
        app.segmentedControls.buttons[fixture].tap()
        app.buttons["openSettings"].tap()
        XCTAssertTrue(app.buttons["openAgent"].waitForExistence(timeout: 3))
    }
    func testSettingsNavigationAndBack() {
        openSettings()
        capture("SettingsEntry-light")
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.staticTexts["Capabilities"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Scheduled and triggered work"].exists)
        capture("AgentSettings-light")
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.buttons["openAgent"].waitForExistence(timeout: 3))
    }
    func testDarkSettingsFoundation() {
        app.terminate()
        app.launchArguments = ["--settings-dark"]
        app.launch()
        openSettings()
        capture("SettingsEntry-dark")
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.staticTexts["Capabilities"].waitForExistence(timeout: 5))
        capture("AgentSettings-dark")
    }
    func testLargeTextSettingsScrolls() {
        app.terminate()
        app.launchArguments = ["--settings-light", "--settings-large-text"]
        app.launch()
        openSettings()
        capture("SettingsEntry-large-text")
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.staticTexts["Capabilities"].waitForExistence(timeout: 5))
        capture("AgentSettings-large-text")
        for _ in 0..<5 where !app.staticTexts["Voice"].isHittable { app.swipeUp() }
        XCTAssertTrue(app.staticTexts["Voice"].isHittable)
        capture("AgentSettings-large-text-bottom")
    }
    func testAutomationsHasNoNavigationAction() {
        openSettings()
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.staticTexts["Capabilities"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["automationsUnavailable"].exists)
        let row = app.descendants(matching: .any)["automationsUnavailable"].firstMatch
        XCTAssertTrue(row.exists)
        row.tap()
        XCTAssertTrue(app.navigationBars["Agent settings"].exists)
    }
    func testErrorRetryAndCancel() {
        openSettings("Error")
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.buttons["retry"].waitForExistence(timeout: 5))
        capture("ios-load-error")
        app.buttons["cancelLoad"].tap()
        XCTAssertTrue(app.buttons["openAgent"].waitForExistence(timeout: 3))
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.buttons["retry"].waitForExistence(timeout: 5))
        app.buttons["retry"].tap()
        XCTAssertTrue(app.staticTexts["Capabilities"].waitForExistence(timeout: 5))
    }
    func testCancelLoadingDoesNotNavigateLater() {
        openSettings("Slow")
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.buttons["cancelLoad"].waitForExistence(timeout: 2))
        capture("ios-loading")
        app.buttons["cancelLoad"].tap()
        // After the original load deadline, the destination must still be dismissed.
        RunLoop.current.run(until: Date().addingTimeInterval(10.3))
        XCTAssertFalse(app.staticTexts["Capabilities"].exists)
        XCTAssertTrue(app.buttons["openAgent"].exists)
        capture("ios-cancelled-load")
    }
    func testControlsCancelRollbackAndSave() {
        app.buttons["Shared controls"].tap()
        app.buttons["editName"].tap()
        let field = app.textFields["nameField"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        field.tap()
        field.typeText(" Changed")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(app.staticTexts["Avery Diaz"].exists)
        app.buttons["editName"].tap()
        XCTAssertEqual(field.value as? String, "Avery Diaz")
        field.tap()
        field.typeText(" Saved")
        app.buttons["Save"].tap()
        XCTAssertTrue(app.staticTexts["Avery Diaz Saved"].exists)
        capture("ios-controls")
    }
    // These journeys run through the public playground routes. All entered text is
    // an intentionally non-secret prototype placeholder; no service is contacted.
    private func openDestination(_ destination: String, title: String) {
        openSettings()
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.staticTexts["Capabilities"].waitForExistence(timeout: 5))
        let route = app.buttons["agentDestination.\(destination)"]
        reveal(route)
        route.tap()
        XCTAssertTrue(app.navigationBars[title].waitForExistence(timeout: 5))
    }
    private func reveal(_ element: XCUIElement) {
        for _ in 0..<7 where !element.isHittable {
            if app.scrollViews.firstMatch.exists { app.scrollViews.firstMatch.swipeUp() }
            else { app.swipeUp() }
        }
        XCTAssertTrue(element.isHittable, "Expected reachable control: \(element.identifier)")
    }
    private func navigateBack(from title: String, to expectedTitle: String) {
        app.navigationBars[title].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars[expectedTitle].waitForExistence(timeout: 5))
    }
    private func assertToggle(_ identifier: String, isOn: Bool) {
        let toggle = app.switches[identifier]
        XCTAssertTrue(toggle.exists)
        XCTAssertEqual(toggle.value as? String, isOn ? "1" : "0")
    }

    func testPairedDevicesBoundaryCancelRemovalAndEmptyRefresh() {
        openDestination("pairedDevices", title: "Paired devices")
        let peer = app.buttons["pairedDevices.peer.mac-studio"]
        XCTAssertTrue(peer.waitForExistence(timeout: 3))
        capture("PairedDevices-light")
        app.buttons["pairedDevices.add"].tap()
        XCTAssertTrue(app.navigationBars["Pair a new device"].waitForExistence(timeout: 3))
        capture("PairedDevices-add-boundary-light")
        app.navigationBars["Pair a new device"].buttons["Done"].tap()
        XCTAssertTrue(peer.waitForExistence(timeout: 3))
        peer.tap()
        XCTAssertTrue(app.navigationBars["Mac Studio"].waitForExistence(timeout: 3))
        capture("PairedDevices-detail-light")
        navigateBack(from: "Mac Studio", to: "Paired devices")
        XCTAssertTrue(peer.exists)
        peer.tap()
        let remove = app.buttons["pairedDevices.removeAccess"]
        reveal(remove)
        remove.tap()
        XCTAssertTrue(app.buttons["pairedDevices.confirmRemove"].waitForExistence(timeout: 3))
        capture("PairedDevices-remove-confirmation-light")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(app.navigationBars["Mac Studio"].exists)
        XCTAssertFalse(app.buttons["pairedDevices.confirmRemove"].exists)
        remove.tap()
        app.buttons["pairedDevices.confirmRemove"].tap()
        XCTAssertTrue(app.staticTexts["No paired devices"].waitForExistence(timeout: 3))
        XCTAssertFalse(peer.exists)
        XCTAssertFalse(app.buttons["pairedDevices.add"].exists)
        app.buttons["pairedDevices.refresh"].tap()
        XCTAssertTrue(app.staticTexts["No paired devices"].exists)
        XCTAssertFalse(peer.exists)
        capture("PairedDevices-empty-light")
        navigateBack(from: "Paired devices", to: "Agent settings")
    }

    func testMemoryControlsSurviveSummaryAndComposerGivesLocalFeedback() {
        openDestination("memory", title: "Memory")
        assertToggle("memory.toggle.searchAndReference", isOn: true)
        assertToggle("memory.toggle.generateMemory", isOn: true)
        assertToggle("memory.toggle.sensitiveTopics", isOn: false)
        capture("Memory-light")
        app.switches["memory.toggle.searchAndReference"].tap()
        app.switches["memory.toggle.generateMemory"].tap()
        app.switches["memory.toggle.sensitiveTopics"].tap()
        let summary = app.buttons["memory.summaryRow"]
        reveal(summary)
        summary.tap()
        XCTAssertTrue(app.navigationBars["Memory summary"].waitForExistence(timeout: 3))
        capture("Memory-summary-light")
        let send = app.buttons["memory.composerSend"]
        XCTAssertFalse(send.isEnabled)
        let composer = app.descendants(matching: .any).matching(identifier: "memory.composerField").firstMatch
        XCTAssertTrue(composer.exists)
        composer.tap()
        composer.typeText("Use short answers in this prototype.")
        XCTAssertTrue(send.isEnabled)
        send.tap()
        let feedback = app.staticTexts["memory.composerFeedback"]
        XCTAssertTrue(feedback.waitForExistence(timeout: 3))
        XCTAssertEqual(feedback.label, "Noted in this prototype session. Rem doesn’t reply or change memory here.")
        XCTAssertFalse(send.isEnabled)
        reveal(feedback)
        capture("Memory-composer-feedback-light")
        navigateBack(from: "Memory summary", to: "Memory")
        assertToggle("memory.toggle.searchAndReference", isOn: false)
        assertToggle("memory.toggle.generateMemory", isOn: false)
        assertToggle("memory.toggle.sensitiveTopics", isOn: true)
        navigateBack(from: "Memory", to: "Agent settings")
    }

    func testModelsPickerMaskedDraftBackDiscardAndSingleSave() {
        openDestination("models", title: "Models")
        capture("Models-light")
        let addKey = app.buttons["models.addProviderKey"]
        reveal(addKey)
        addKey.tap()
        XCTAssertTrue(app.navigationBars["Add provider key"].waitForExistence(timeout: 3))
        let save = app.buttons["models.saveKey"]
        XCTAssertEqual(app.buttons.matching(identifier: "models.saveKey").count, 1)
        XCTAssertFalse(save.isEnabled)
        capture("Models-add-key-empty-light")
        app.buttons["models.providerPicker"].tap()
        let providers = ["Anthropic", "OpenAI", "Google", "Mistral", "OpenRouter"]
        for provider in providers {
            XCTAssertTrue(app.buttons["models.providerOption.\(provider)"].waitForExistence(timeout: 3))
        }
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "models.providerOption.")).count, 5)
        capture("Models-provider-picker-light")
        app.buttons["models.providerOption.OpenAI"].tap()
        XCTAssertTrue(app.buttons["models.providerPicker"].label.contains("OpenAI"))
        let key = app.secureTextFields["models.keyField"]
        XCTAssertTrue(key.exists, "Provider draft must use a secure field")
        key.tap()
        key.typeText("prototype-only-placeholder")
        XCTAssertTrue(save.isEnabled)
        XCTAssertFalse(app.staticTexts["prototype-only-placeholder"].exists)
        capture("Models-key-masked-light")
        navigateBack(from: "Add provider key", to: "Models")
        reveal(addKey)
        addKey.tap()
        XCTAssertTrue(save.waitForExistence(timeout: 3))
        XCTAssertFalse(save.isEnabled, "Back must discard the ephemeral draft")
        key.tap()
        key.typeText("another-prototype-placeholder")
        XCTAssertEqual(app.buttons.matching(identifier: "models.saveKey").count, 1)
        save.tap()
        XCTAssertTrue(app.navigationBars["Models"].waitForExistence(timeout: 3))
        reveal(addKey)
        addKey.tap()
        XCTAssertTrue(app.navigationBars["Add provider key"].waitForExistence(timeout: 3))
        XCTAssertFalse(save.isEnabled, "Saving must not retain the entered draft")
        navigateBack(from: "Add provider key", to: "Models")
        navigateBack(from: "Models", to: "Agent settings")
    }

    private func cloudField(_ identifier: String, secure: Bool = false) -> XCUIElement {
        let type: XCUIElement.ElementType = secure ? .secureTextField : .textField
        let direct = app.descendants(matching: type).matching(identifier: identifier).firstMatch
        if direct.exists { return direct }
        return app.descendants(matching: .any).matching(identifier: identifier).firstMatch
            .descendants(matching: type).firstMatch
    }
    private func editCloudLogin(_ field: String) {
        let row = app.descendants(matching: .any)
            .matching(identifier: "cloudBrowser.savedLogin.edit\(field)").firstMatch
        reveal(row)
        row.buttons.firstMatch.tap()
    }
    private func openCloudSites() {
        openDestination("cloudBrowser", title: "Cloud browser")
        let sites = app.buttons["cloudBrowser.seeAllSites"]
        reveal(sites)
        sites.tap()
        XCTAssertTrue(app.navigationBars["Sites"].waitForExistence(timeout: 3))
    }

    func testCloudAddSiteEmptyFocusAndBackDiscards() {
        openCloudSites()
        capture("CloudBrowser-sites-light")
        app.buttons["cloudBrowser.sites.addSite"].tap()
        XCTAssertTrue(app.navigationBars["Add site"].waitForExistence(timeout: 3))
        let save = app.buttons["cloudBrowser.addSite.save"]
        XCTAssertFalse(save.isEnabled)
        XCTAssertFalse(app.keyboards.firstMatch.exists)
        capture("CloudBrowser-add-site-empty-light")
        let domain = cloudField("cloudBrowser.addSite.domainField")
        domain.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 3))
        capture("CloudBrowser-add-site-focused-light")
        domain.typeText("discarded.example")
        XCTAssertTrue(save.isEnabled)
        navigateBack(from: "Add site", to: "Sites")
        XCTAssertFalse(app.buttons["cloudBrowser.site.discarded.example"].exists)
        for site in ["github.com", "notion.so", "linear.app", "openai.com"] {
            XCTAssertTrue(app.buttons["cloudBrowser.site.\(site)"].exists)
        }
        app.buttons["cloudBrowser.sites.addSite"].tap()
        XCTAssertTrue(app.navigationBars["Add site"].waitForExistence(timeout: 3))
        XCTAssertFalse(save.isEnabled)
        XCTAssertFalse((cloudField("cloudBrowser.addSite.domainField").value as? String ?? "").contains("discarded.example"))
        navigateBack(from: "Add site", to: "Sites")
        navigateBack(from: "Sites", to: "Cloud browser")
        navigateBack(from: "Cloud browser", to: "Agent settings")
    }

    func testCloudSavedLoginCancelSaveMaskAndCookieClearKeepsLogin() {
        openCloudSites()
        app.buttons["cloudBrowser.site.github.com"].tap()
        XCTAssertTrue(app.navigationBars["github.com"].waitForExistence(timeout: 3))
        capture("CloudBrowser-site-detail-light")
        app.buttons["cloudBrowser.siteDetail.login"].tap()
        XCTAssertTrue(app.navigationBars["Saved login"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["samuel@example.com"].exists)
        capture("CloudBrowser-saved-login-light")
        editCloudLogin("Username")
        let username = cloudField("cloudBrowser.savedLogin.usernameField")
        username.tap()
        username.typeText(".discarded")
        capture("CloudBrowser-login-edit-username-light")
        app.buttons["cloudBrowser.savedLogin.cancel"].tap()
        XCTAssertTrue(app.staticTexts["samuel@example.com"].exists)
        XCTAssertFalse(app.buttons["cloudBrowser.savedLogin.save"].exists)
        editCloudLogin("Username")
        let current = cloudField("cloudBrowser.savedLogin.usernameField")
        current.coordinate(withNormalizedOffset: CGVector(dx: 0.99, dy: 0.5)).tap()
        current.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: "samuel@example.com".count))
        current.typeText("prototype@example.com")
        app.buttons["cloudBrowser.savedLogin.save"].tap()
        XCTAssertTrue(app.staticTexts["prototype@example.com"].waitForExistence(timeout: 3))
        editCloudLogin("Password")
        let password = cloudField("cloudBrowser.savedLogin.passwordField", secure: true)
        XCTAssertTrue(password.exists, "Password editor must use a native secure field")
        XCTAssertFalse(app.buttons["cloudBrowser.savedLogin.save"].isEnabled)
        password.tap()
        password.typeText("prototype-password-placeholder")
        XCTAssertFalse(app.staticTexts["prototype-password-placeholder"].exists)
        capture("CloudBrowser-login-edit-password-masked-light")
        app.buttons["cloudBrowser.savedLogin.save"].tap()
        XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
        editCloudLogin("Password")
        XCTAssertFalse(app.buttons["cloudBrowser.savedLogin.save"].isEnabled)
        app.buttons["cloudBrowser.savedLogin.cancel"].tap()
        navigateBack(from: "Saved login", to: "github.com")
        let cookies = app.buttons["cloudBrowser.siteDetail.cookies"]
        reveal(cookies)
        cookies.tap()
        XCTAssertTrue(app.navigationBars["Cookies & sessions"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["12 illustrative cookies"].exists)
        capture("CloudBrowser-cookies-light")
        let clear = app.buttons["cloudBrowser.cookies.clearSiteData"]
        reveal(clear)
        clear.tap()
        XCTAssertTrue(app.buttons["cloudBrowser.cookies.confirmClear"].waitForExistence(timeout: 3))
        capture("CloudBrowser-cookies-confirmation-light")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(app.staticTexts["12 illustrative cookies"].exists)
        XCTAssertTrue(app.staticTexts["Signed in"].exists)
        clear.tap()
        app.buttons["cloudBrowser.cookies.confirmClear"].tap()
        XCTAssertTrue(app.staticTexts["0 illustrative cookies"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Signed out"].exists)
        capture("CloudBrowser-cookies-cleared-light")
        navigateBack(from: "Cookies & sessions", to: "github.com")
        app.buttons["cloudBrowser.siteDetail.login"].tap()
        XCTAssertTrue(app.staticTexts["prototype@example.com"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
    }

    private func captureDestinationAppearance(arguments: [String], suffix: String) {
        for (route, title, name, finalControl) in [
            ("pairedDevices", "Paired devices", "PairedDevices", "pairedDevices.peer.mac-studio"),
            ("memory", "Memory", "Memory", "memory.summaryRow"),
            ("models", "Models", "Models", "models.addProviderKey")
        ] {
            app.terminate()
            app.launchArguments = arguments
            app.launch()
            openDestination(route, title: title)
            capture("\(name)-\(suffix)")
            reveal(app.buttons[finalControl])
            if suffix == "large-text" { capture("\(name)-large-text-bottom") }
        }
    }
    func testDestinationsDarkAppearance() {
        captureDestinationAppearance(arguments: ["--settings-dark"], suffix: "dark")
    }
    func testDestinationsLargeTextReachability() {
        captureDestinationAppearance(arguments: ["--settings-light", "--settings-large-text"], suffix: "large-text")
    }

}
