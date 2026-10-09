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
        if fixture != "Success" {
            let choice = app.segmentedControls.buttons[fixture]
            reveal(choice)
            choice.tap()
        }
        let open = app.buttons["openSettings"]
        reveal(open)
        open.tap()
        XCTAssertTrue(app.buttons["openAgent"].waitForExistence(timeout: 3))
    }
    func testSettingsNavigationAndBack() {
        openSettings()
        capture("SettingsEntry-light")
        app.buttons["openAgent"].tap()
        waitForAgent()
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
        waitForAgent()
        capture("AgentSettings-dark")
    }
    func testLargeTextSettingsScrolls() {
        app.terminate()
        app.launchArguments = ["--settings-light", "--settings-large-text"]
        app.launch()
        openSettings()
        capture("SettingsEntry-large-text")
        app.buttons["openAgent"].tap()
        waitForAgent()
        capture("AgentSettings-large-text")
        for _ in 0..<5 where !app.staticTexts["Voice"].isHittable { app.swipeUp() }
        XCTAssertTrue(app.staticTexts["Voice"].isHittable)
        capture("AgentSettings-large-text-bottom")
    }
    func testAutomationsHasNoNavigationAction() {
        openSettings()
        app.buttons["openAgent"].tap()
        waitForAgent()
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
        waitForAgent()
    }
    func testCancelLoadingDoesNotNavigateLater() {
        openSettings("Slow")
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.buttons["cancelLoad"].waitForExistence(timeout: 2))
        let skeleton = app.descendants(matching: .any)["agentSettings.skeleton"]
        XCTAssertTrue(skeleton.exists, "Slow loads show the skeleton, not a bare spinner")
        XCTAssertEqual(skeleton.label, "Loading agent settings")
        capture("ios-loading")
        app.buttons["cancelLoad"].tap()
        // After the original load deadline, the destination must still be dismissed.
        RunLoop.current.run(until: Date().addingTimeInterval(10.3))
        XCTAssertFalse(app.descendants(matching: .any)["agentSettings"].firstMatch.exists)
        XCTAssertTrue(app.buttons["openAgent"].exists)
        capture("ios-cancelled-load")
    }
    /// Native SwiftUI Menus can expose a valid visible frame but no AX hit point.
    /// Do not use reveal() here: its upward scrolling moves the account row out of view.
    private func tapGmailMenu(_ identifier: String, toolbar: Bool = false) {
        let menu = app.descendants(matching: .any).matching(identifier: identifier).firstMatch
        let navigationBar = app.navigationBars["Gmail"]
        XCTAssertTrue(navigationBar.waitForExistence(timeout: 3))
        for _ in 0..<12 {
            let appBounds = app.frame
            let visibleBounds = toolbar ? appBounds : CGRect(
                x: appBounds.minX, y: navigationBar.frame.maxY,
                width: appBounds.width, height: max(0, appBounds.maxY - navigationBar.frame.maxY))
            if menu.exists {
                let frame = menu.frame
                if !frame.isEmpty && !frame.isInfinite && visibleBounds.contains(frame) {
                    if menu.isHittable {
                        menu.tap()
                    } else {
                        // Match XCTest's native Menu center-point workaround, only after
                        // checking this exact element lies fully inside the visible viewport.
                        menu.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
                    }
                    return
                }
                if !toolbar && frame.minY >= visibleBounds.maxY {
                    app.swipeUp()
                    continue
                }
            }
            if toolbar { break }
            app.swipeDown()
        }
        capture("Gmail-menu-unreachable")
        print(app.debugDescription)
        XCTFail("Expected visible Gmail menu: \(identifier)")
    }

    /// The menu overlay can make a visible navigation title fail AXScrollToVisible.
    /// Use its measured center for the same native outside tap, then prove dismissal.
    private func dismissGmailMenuOutside(visibleItem: XCUIElement) {
        XCTAssertTrue(visibleItem.waitForExistence(timeout: 3))
        let bar = app.navigationBars["Gmail"]
        let title = bar.staticTexts["Gmail"]
        guard title.exists else {
            XCTFail("Expected Gmail navigation title behind the open menu")
            return
        }
        let frame = title.frame
        guard !frame.isEmpty, !frame.isInfinite,
              app.frame.contains(frame), bar.frame.contains(frame) else {
            XCTFail("Expected a visible Gmail navigation title for the outside-menu tap")
            return
        }
        title.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let closed = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"),
                                               object: visibleItem)
        XCTAssertEqual(XCTWaiter.wait(for: [closed], timeout: 3), .completed,
                       "The outside tap must dismiss the native Gmail menu")
        XCTAssertTrue(bar.exists, "Dismissing the menu must retain Gmail detail")
    }

    func testConnectorsGmailMenusAndScopedPermissions() {
        openDestination("connectors", title: "Connectors")
        capture("Connectors-light")
        for provider in ["gmail", "googleCalendar", "notion", "slack", "googleDrive", "linear", "todoist"] {
            let row = app.buttons["connectors.provider.\(provider)"]
            reveal(row)
        }
        capture("Connectors-brand-rows-bottom-light")
        for _ in 0..<3 { app.swipeDown() }
        let notion = app.buttons["connectors.provider.notion"]
        reveal(notion)
        notion.tap()
        XCTAssertTrue(app.alerts["Prototype boundary"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Notion account details are not included in this prototype. No connection is changed."].exists)
        capture("Connectors-unsupported-provider-light")
        app.alerts.buttons["Done"].tap()
        app.buttons["connectors.provider.gmail"].tap()
        XCTAssertTrue(app.navigationBars["Gmail"].waitForExistence(timeout: 3))
        capture("Gmail-default-light")
        tapGmailMenu("gmail.accountMenu.avery")
        XCTAssertTrue(app.buttons["gmail.accountSettings.avery"].waitForExistence(timeout: 3))
        capture("Gmail-account-menu-light")
        app.buttons["gmail.accountSettings.avery"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["gmail.permission.avery.lowRisk"].isSelected)
        capture("Gmail-account-permissions-light")
        navigateBack(from: "Settings", to: "Gmail")
        tapGmailMenu("gmail.connectorMenu", toolbar: true)
        XCTAssertTrue(app.buttons["gmail.disconnectAll"].waitForExistence(timeout: 3))
        capture("Gmail-connector-menu-light")
        dismissGmailMenuOutside(visibleItem: app.buttons["gmail.disconnectAll"])
        let permissions = app.buttons["gmail.permissions"]
        reveal(permissions)
        permissions.tap()
        XCTAssertTrue(app.navigationBars["Permissions"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["gmail.permission.connector.lowRisk"].isSelected)
        capture("Gmail-connector-permissions-light")
        for policy in ["alwaysAsk", "lowRisk", "alwaysAllow", "neverAllow"] {
            let choice = app.buttons["gmail.permission.connector.\(policy)"]
            reveal(choice)
            choice.tap()
            XCTAssertTrue(choice.isSelected)
            for other in ["alwaysAsk", "lowRisk", "alwaysAllow", "neverAllow"] where other != policy {
                XCTAssertFalse(app.buttons["gmail.permission.connector.\(other)"].isSelected)
            }
            capture("Gmail-connector-policy-\(policy)-light")
        }
        navigateBack(from: "Permissions", to: "Gmail")
        for _ in 0..<2 { app.swipeDown() }
        tapGmailMenu("gmail.accountMenu.avery")
        XCTAssertTrue(app.buttons["gmail.accountSettings.avery"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["gmail.disconnectAccount.avery"].exists)
        capture("Gmail-account-menu-after-policy-light")
        app.buttons["gmail.accountSettings.avery"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["gmail.permission.avery.neverAllow"].isSelected, "Unset account policy inherits connector policy")
        capture("Gmail-account-permissions-inherited-light")
        for policy in ["alwaysAsk", "lowRisk", "alwaysAllow", "neverAllow"] {
            let choice = app.buttons["gmail.permission.avery.\(policy)"]
            reveal(choice)
            choice.tap()
            XCTAssertTrue(choice.isSelected)
            for other in ["alwaysAsk", "lowRisk", "alwaysAllow", "neverAllow"] where other != policy {
                XCTAssertFalse(app.buttons["gmail.permission.avery.\(other)"].isSelected)
            }
        }
        for _ in 0..<3 { app.swipeDown() }
        app.buttons["gmail.permission.avery.alwaysAsk"].tap()
        navigateBack(from: "Settings", to: "Gmail")
        reveal(permissions)
        permissions.tap()
        XCTAssertTrue(app.buttons["gmail.permission.connector.neverAllow"].isSelected, "Account override must not change connector policy")
        app.buttons["gmail.permission.connector.alwaysAllow"].tap()
        navigateBack(from: "Permissions", to: "Gmail")
        for _ in 0..<2 { app.swipeDown() }
        tapGmailMenu("gmail.accountMenu.avery")
        app.buttons["gmail.accountSettings.avery"].tap()
        XCTAssertTrue(app.buttons["gmail.permission.avery.alwaysAsk"].isSelected, "Connector changes must retain an explicit account override")
        capture("Gmail-account-override-retained-light")
        navigateBack(from: "Settings", to: "Gmail")
        navigateBack(from: "Gmail", to: "Connectors")
        navigateBack(from: "Connectors", to: "Agent settings")
    }

    func testGmailSingleAndAllDisconnectCancelThenConfirm() {
        for all in [false, true] {
            if all { app.terminate(); app.launch() }
            openDestination("connectors", title: "Connectors")
            app.buttons["connectors.provider.gmail"].tap()
            XCTAssertTrue(app.navigationBars["Gmail"].waitForExistence(timeout: 3))
            let menuID = all ? "gmail.connectorMenu" : "gmail.accountMenu.avery"
            let action = app.buttons[all ? "gmail.disconnectAll" : "gmail.disconnectAccount.avery"]
            tapGmailMenu(menuID, toolbar: all)
            XCTAssertTrue(action.waitForExistence(timeout: 3))
            action.tap()
            let confirm = app.buttons["gmail.confirmDisconnect"]
            XCTAssertTrue(confirm.waitForExistence(timeout: 3))
            let title = all ? "Disconnect all Gmail accounts?" : "Disconnect avery@example.com?"
            XCTAssertTrue(app.staticTexts[title].exists)
            capture(all ? "Gmail-disconnect-accounts-light" : "Gmail-disconnect-account-light")
            app.buttons["Cancel"].tap()
            XCTAssertTrue(app.navigationBars["Gmail"].exists)
            XCTAssertTrue(app.staticTexts["avery@example.com"].exists, "Cancel must retain membership")
            tapGmailMenu(menuID, toolbar: all)
            action.tap()
            XCTAssertTrue(confirm.waitForExistence(timeout: 3))
            confirm.tap()
            let returned = app.navigationBars["Connectors"].waitForExistence(timeout: 3)
            if !returned {
                capture("Gmail-disconnect-unexpected")
                print(app.debugDescription)
            }
            XCTAssertTrue(returned)
            XCTAssertTrue(app.buttons["Connect Gmail"].exists)
            XCTAssertTrue(app.buttons["connectors.provider.googleCalendar"].exists)
            XCTAssertTrue(app.buttons["connectors.provider.notion"].exists)
            XCTAssertTrue(app.buttons["connectors.provider.slack"].exists)
            capture(all ? "Gmail-disconnect-all-result-light" : "Gmail-disconnect-one-result-light")
        }
    }

    func testConnectorsGmailDarkAndLargeTextReachability() {
        for (arguments, suffix) in [(["--settings-dark"], "dark"), (["--settings-light", "--settings-large-text"], "large-text")] {
            app.terminate()
            app.launchArguments = arguments
            app.launch()
            openDestination("connectors", title: "Connectors")
            capture("Connectors-\(suffix)")
            reveal(app.buttons["connectors.provider.todoist"])
            capture("Connectors-bottom-\(suffix)")
            for _ in 0..<6 { app.swipeDown() }
            app.buttons["connectors.provider.gmail"].tap()
            XCTAssertTrue(app.navigationBars["Gmail"].waitForExistence(timeout: 3))
            capture("Gmail-default-\(suffix)")
            reveal(app.buttons["gmail.info.Report an issue"])
            capture("Gmail-information-\(suffix)")
            for _ in 0..<7 { app.swipeDown() }
            let permissions = app.buttons["gmail.permissions"]
            reveal(permissions)
            permissions.tap()
            XCTAssertTrue(app.navigationBars["Permissions"].waitForExistence(timeout: 3))
            capture("Gmail-connector-permissions-\(suffix)")
            reveal(app.buttons["gmail.permission.connector.neverAllow"])
            reveal(app.staticTexts["Controls the level of access Rem has across all Gmail accounts connected to this connector."])
            capture("Gmail-connector-permissions-bottom-\(suffix)")
            navigateBack(from: "Permissions", to: "Gmail")
            for _ in 0..<7 { app.swipeDown() }
            tapGmailMenu("gmail.accountMenu.avery")
            app.buttons["gmail.accountSettings.avery"].tap()
            XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 3))
            reveal(app.staticTexts["Controls what Rem can do with avery@example.com. Overrides the connector-wide setting for this account."])
            capture("Gmail-account-permissions-bottom-\(suffix)")
        }
    }

    func testControlsCancelRollbackAndSave() {
        app.buttons["openComponents"].tap()
        XCTAssertTrue(app.buttons["openControls"].waitForExistence(timeout: 3))
        app.buttons["openControls"].tap()
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
    private func waitForAgent() {
        let ready = app.descendants(matching: .any)["agentSettings"].firstMatch.waitForExistence(timeout: 5)
        if !ready {
            capture("AgentSettings-not-ready")
            let hierarchy = XCTAttachment(string: app.debugDescription)
            hierarchy.name = "AgentSettings-not-ready-hierarchy"
            hierarchy.lifetime = .keepAlways
            add(hierarchy)
            print(app.debugDescription)
        }
        XCTAssertTrue(ready, "Agent settings must finish loading")
    }
    private func openDestination(_ destination: String, title: String) {
        openSettings()
        app.buttons["openAgent"].tap()
        waitForAgent()
        let route = app.buttons["agentDestination.\(destination)"]
        reveal(route)
        route.tap()
        let arrived = app.navigationBars[title].waitForExistence(timeout: 5)
        if !arrived {
            capture("Destination-\(destination)-unexpected")
            print(app.debugDescription)
        }
        XCTAssertTrue(arrived, "Expected destination: \(title)")
    }
    private func reveal(_ element: XCUIElement) {
        for _ in 0..<7 where !element.isHittable {
            if app.scrollViews.firstMatch.exists { app.scrollViews.firstMatch.swipeUp() }
            else { app.swipeUp() }
        }
        XCTAssertTrue(element.isHittable, "Expected reachable control: \(element.identifier)")
    }
    private func revealFully(_ element: XCUIElement, above footer: XCUIElement? = nil) {
        for _ in 0..<12 {
            let bounds = app.frame
            let bar = app.navigationBars.firstMatch
            let top = bar.exists ? bar.frame.maxY : bounds.minY + 60
            // Wallet footer has 16pt opaque padding above Connect; keep another 8pt clear.
            let bottom = footer.map { $0.exists ? $0.frame.minY - 24 : bounds.maxY - 34 } ?? bounds.maxY - 34
            let viewport = CGRect(x: bounds.minX, y: top, width: bounds.width, height: max(0, bottom - top))
            if element.exists {
                let frame = element.frame
                if !frame.isEmpty && viewport.contains(frame) { return }
                let overflow = frame.maxY > bottom ? frame.maxY - bottom + 8 : frame.minY - top - 8
                // A tiny correction can remain below UIKit’s scroll gesture threshold.
                let distance = min(max(44, abs(overflow)), viewport.height * 0.4)
                let direction: CGFloat = overflow > 0 ? 1 : -1
                let origin = app.coordinate(withNormalizedOffset: .zero)
                let start = origin.withOffset(CGVector(dx: bounds.width / 2, dy: viewport.midY + direction * distance / 2))
                let end = origin.withOffset(CGVector(dx: bounds.width / 2, dy: viewport.midY - direction * distance / 2))
                start.press(forDuration: 0.05, thenDragTo: end, withVelocity: .slow, thenHoldForDuration: 0.2)
            } else {
                app.swipeUp()
            }
        }
        capture("Fully-visible-target-unexpected")
        XCTFail("Expected the complete capture target inside the visible content area: \(element.identifier)")
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

    private func setMemoryToggle(_ identifier: String, isOn: Bool) {
        let toggle = app.switches[identifier]
        XCTAssertTrue(toggle.exists)
        let expected = isOn ? "1" : "0"
        guard toggle.value as? String != expected else { return }
        // The labeled SwiftUI switch exposes a separate native switch child.
        // Target that real control rather than the containing row or an estimated point.
        let nativeSwitch = toggle.switches.firstMatch
        if nativeSwitch.exists {
            XCTAssertTrue(nativeSwitch.isHittable)
            nativeSwitch.tap()
        } else {
            toggle.tap()
        }
        let changed = XCTNSPredicateExpectation(predicate: NSPredicate(format: "value == %@", expected),
                                                object: toggle)
        XCTAssertEqual(XCTWaiter.wait(for: [changed], timeout: 3), .completed)
        assertToggle(identifier, isOn: isOn)
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
        setMemoryToggle("memory.toggle.searchAndReference", isOn: false)
        setMemoryToggle("memory.toggle.generateMemory", isOn: false)
        setMemoryToggle("memory.toggle.sensitiveTopics", isOn: true)
        capture("Memory-controls-changed-light")
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
        capture("Memory-composer-focused-light")
        send.tap()
        let feedback = app.staticTexts["memory.composerFeedback"]
        XCTAssertTrue(feedback.waitForExistence(timeout: 3))
        XCTAssertEqual(feedback.label, "Noted in this prototype session. Rem doesn’t reply or change memory here.")
        XCTAssertFalse(send.isEnabled)
        capture("Memory-composer-feedback-initial-light")
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
        let edit = app.buttons["cloudBrowser.savedLogin.edit\(field)"]
        reveal(edit)
        edit.tap()
    }
    private func openCloudSites() {
        openDestination("cloudBrowser", title: "Cloud browser")
        capture("CloudBrowser-light")
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

    func testCloudRootAddSiteSaveAndPermissionScopes() {
        openDestination("cloudBrowser", title: "Cloud browser")
        let permission = app.buttons["cloudBrowser.permissionMenu"]
        XCTAssertTrue(permission.waitForExistence(timeout: 3))
        XCTAssertTrue(permission.label.contains("Ask"))
        permission.tap()
        capture("CloudBrowser-default-permission-menu-light")
        app.buttons["Allow"].tap()
        XCTAssertTrue(permission.label.contains("Allow"))
        permission.tap()
        app.buttons["Ask"].tap()
        XCTAssertTrue(permission.label.contains("Ask"))

        // Exercise the root header entry, separately from the Sites toolbar entry.
        app.buttons["Add site"].tap()
        XCTAssertTrue(app.navigationBars["Add site"].waitForExistence(timeout: 3))
        let save = app.buttons["cloudBrowser.addSite.save"]
        XCTAssertFalse(save.isEnabled)
        let domain = cloudField("cloudBrowser.addSite.domainField")
        domain.tap()
        domain.typeText("HTTPS://ROOT-FIXTURE.EXAMPLE/PATH")
        permission.tap()
        app.buttons["Allow"].tap()
        let username = cloudField("cloudBrowser.addSite.username")
        username.tap()
        username.typeText("ROOT-FIXTURE@EXAMPLE.COM")
        let password = cloudField("cloudBrowser.addSite.password", secure: true)
        reveal(password)
        password.tap()
        password.typeText("ROOT-FIXTURE-ONLY")
        XCTAssertTrue(save.isEnabled)
        XCTAssertFalse(app.staticTexts["ROOT-FIXTURE-ONLY"].exists)
        capture("CloudBrowser-add-site-filled-light")
        save.tap()
        XCTAssertTrue(app.navigationBars["Cloud browser"].waitForExistence(timeout: 3))
        XCTAssertTrue(permission.label.contains("Ask"), "Per-site permission must not change the default")
        let allSites = app.buttons["cloudBrowser.seeAllSites"]
        reveal(allSites)
        allSites.tap()
        let added = app.buttons["cloudBrowser.site.root-fixture.example"]
        reveal(added)
        XCTAssertTrue(added.label.contains("Allow · 1 saved login"))
        added.tap()
        XCTAssertTrue(app.navigationBars["root-fixture.example"].waitForExistence(timeout: 3))
        XCTAssertTrue(permission.label.contains("Allow"))
        permission.tap()
        app.buttons["Ask"].tap()
        XCTAssertTrue(permission.label.contains("Ask"))
        capture("CloudBrowser-added-site-permission-light")
        app.buttons["cloudBrowser.siteDetail.login"].tap()
        XCTAssertTrue(app.staticTexts["ROOT-FIXTURE@EXAMPLE.COM"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
        navigateBack(from: "Saved login", to: "root-fixture.example")
        navigateBack(from: "root-fixture.example", to: "Sites")
        XCTAssertTrue(added.label.contains("Ask · 1 saved login"))
        XCTAssertTrue(app.buttons["cloudBrowser.site.notion.so"].label.contains("Allow · Signed in"))
        navigateBack(from: "Sites", to: "Cloud browser")
        XCTAssertTrue(permission.label.contains("Ask"))
    }

    func testCloudAddLoginEmptyFocusFilledSaveAndBackDiscard() {
        openCloudSites()
        app.buttons["cloudBrowser.site.linear.app"].tap()
        XCTAssertTrue(app.navigationBars["linear.app"].waitForExistence(timeout: 3))
        let add = app.buttons["cloudBrowser.siteDetail.addLogin"]
        reveal(add)
        add.tap()
        XCTAssertTrue(app.navigationBars["Add login"].waitForExistence(timeout: 3))
        let save = app.buttons["cloudBrowser.addLogin.save"]
        XCTAssertFalse(save.isEnabled)
        XCTAssertFalse(app.keyboards.firstMatch.exists)
        capture("CloudBrowser-add-login-empty-light")
        let username = cloudField("cloudBrowser.addLogin.username")
        username.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 3))
        username.typeText("DISCARDED-LOGIN@EXAMPLE.COM")
        XCTAssertFalse(save.isEnabled, "Username alone must not save a login")
        capture("CloudBrowser-add-login-focused-light")
        let password = cloudField("cloudBrowser.addLogin.password", secure: true)
        XCTAssertTrue(password.exists)
        password.tap()
        password.typeText("DISCARDED-LOGIN-ONLY")
        XCTAssertTrue(save.isEnabled)
        navigateBack(from: "Add login", to: "linear.app")
        XCTAssertFalse(app.buttons["cloudBrowser.siteDetail.login"].exists)
        add.tap()
        XCTAssertTrue(app.navigationBars["Add login"].waitForExistence(timeout: 3))
        XCTAssertFalse(save.isEnabled)
        XCTAssertFalse((cloudField("cloudBrowser.addLogin.username").value as? String ?? "").contains("DISCARDED-LOGIN"))
        cloudField("cloudBrowser.addLogin.username").tap()
        cloudField("cloudBrowser.addLogin.username").typeText("SAVED-LOGIN@EXAMPLE.COM")
        XCTAssertFalse(save.isEnabled, "Back must also discard the password draft")
        cloudField("cloudBrowser.addLogin.password", secure: true).tap()
        cloudField("cloudBrowser.addLogin.password", secure: true).typeText("SAVED-LOGIN-ONLY\n")
        XCTAssertTrue(save.isEnabled)
        XCTAssertFalse(app.staticTexts["SAVED-LOGIN-ONLY"].exists)
        let keyboardDismissed = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "exists == false"), object: app.keyboards.firstMatch)
        XCTAssertEqual(XCTWaiter.wait(for: [keyboardDismissed], timeout: 3), .completed)
        XCTAssertTrue(app.staticTexts["Username or email"].exists)
        XCTAssertTrue(app.staticTexts["Password"].exists)
        XCTAssertGreaterThan(cloudField("cloudBrowser.addLogin.password", secure: true).frame.height, 12,
                             "The filled secure input must remain visible after keyboard dismissal")
        capture("CloudBrowser-add-login-filled-light")
        save.tap()
        XCTAssertTrue(app.navigationBars["linear.app"].waitForExistence(timeout: 3))
        XCTAssertEqual(app.buttons.matching(identifier: "cloudBrowser.siteDetail.login").count, 1)
        app.buttons["cloudBrowser.siteDetail.login"].tap()
        XCTAssertTrue(app.staticTexts["SAVED-LOGIN@EXAMPLE.COM"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
        navigateBack(from: "Saved login", to: "linear.app")
        add.tap()
        XCTAssertTrue(app.navigationBars["Add login"].waitForExistence(timeout: 3))
        XCTAssertFalse(save.isEnabled, "Successful Save must leave the next draft empty")
        navigateBack(from: "Add login", to: "linear.app")
        XCTAssertEqual(app.buttons.matching(identifier: "cloudBrowser.siteDetail.login").count, 1)
    }

    func testCloudRemoveLoginCancelAndConfirmPreservesOtherCredential() {
        openCloudSites()
        app.buttons["cloudBrowser.site.github.com"].tap()
        XCTAssertTrue(app.navigationBars["github.com"].waitForExistence(timeout: 3))
        app.buttons["cloudBrowser.siteDetail.addLogin"].tap()
        XCTAssertTrue(app.navigationBars["Add login"].waitForExistence(timeout: 3))
        cloudField("cloudBrowser.addLogin.username").tap()
        cloudField("cloudBrowser.addLogin.username").typeText("RETAINED-LOGIN@EXAMPLE.COM")
        cloudField("cloudBrowser.addLogin.password", secure: true).tap()
        cloudField("cloudBrowser.addLogin.password", secure: true).typeText("RETAINED-LOGIN-ONLY")
        app.buttons["cloudBrowser.addLogin.save"].tap()
        XCTAssertTrue(app.navigationBars["github.com"].waitForExistence(timeout: 3))
        let logins = app.buttons.matching(identifier: "cloudBrowser.siteDetail.login")
        XCTAssertEqual(logins.count, 2)
        let original = logins.element(boundBy: 0)
        reveal(original)
        XCTAssertTrue(original.label.contains("samuel@example.com"))
        original.tap()
        XCTAssertTrue(app.navigationBars["Saved login"].waitForExistence(timeout: 3))
        let remove = app.buttons["cloudBrowser.savedLogin.remove"]
        reveal(remove)
        remove.tap()
        let confirm = app.buttons["cloudBrowser.savedLogin.confirmRemove"]
        XCTAssertTrue(confirm.waitForExistence(timeout: 3))
        capture("CloudBrowser-remove-login-confirmation-light")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(app.navigationBars["Saved login"].exists)
        XCTAssertTrue(app.staticTexts["samuel@example.com"].exists)
        XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
        remove.tap()
        XCTAssertTrue(confirm.waitForExistence(timeout: 3))
        confirm.tap()
        XCTAssertTrue(app.navigationBars["github.com"].waitForExistence(timeout: 3))
        XCTAssertEqual(logins.count, 1)
        XCTAssertTrue(logins.firstMatch.label.contains("RETAINED-LOGIN@EXAMPLE.COM"))
        let cookies = app.buttons["cloudBrowser.siteDetail.cookies"]
        reveal(cookies)
        XCTAssertTrue(cookies.label.contains("12 cookies · Signed in"))
        capture("CloudBrowser-login-removed-scoped-light")
        logins.firstMatch.tap()
        XCTAssertTrue(app.staticTexts["RETAINED-LOGIN@EXAMPLE.COM"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
        navigateBack(from: "Saved login", to: "github.com")
        navigateBack(from: "github.com", to: "Sites")
        app.buttons["cloudBrowser.site.notion.so"].tap()
        XCTAssertTrue(app.navigationBars["notion.so"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["cloudBrowser.siteDetail.cookies"].label.contains("8 cookies · Signed in"))
    }

    func testCloudDetailAndAllClearCancelConfirmKeepCredentials() {
        openDestination("cloudBrowser", title: "Cloud browser")
        app.buttons["cloudBrowser.site.github.com"].tap()
        XCTAssertTrue(app.navigationBars["github.com"].waitForExistence(timeout: 3))
        let cookies = app.buttons["cloudBrowser.siteDetail.cookies"]
        let clearSite = app.buttons["cloudBrowser.siteDetail.clearSiteData"]
        reveal(clearSite)
        clearSite.tap()
        let confirmSite = app.buttons["cloudBrowser.siteDetail.confirmClear"]
        XCTAssertTrue(confirmSite.waitForExistence(timeout: 3))
        capture("CloudBrowser-detail-clear-confirmation-light")
        app.buttons["Cancel"].tap()
        XCTAssertTrue(cookies.label.contains("12 cookies · Signed in"))
        XCTAssertEqual(app.buttons.matching(identifier: "cloudBrowser.siteDetail.login").count, 1)
        clearSite.tap()
        confirmSite.tap()
        XCTAssertTrue(cookies.label.contains("0 cookies · Signed out"))
        app.buttons["cloudBrowser.siteDetail.login"].tap()
        XCTAssertTrue(app.staticTexts["samuel@example.com"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
        navigateBack(from: "Saved login", to: "github.com")
        navigateBack(from: "github.com", to: "Cloud browser")

        // Add a credential on a second site so clear-all must preserve both scopes.
        app.buttons["cloudBrowser.site.notion.so"].tap()
        XCTAssertTrue(app.navigationBars["notion.so"].waitForExistence(timeout: 3))
        XCTAssertTrue(cookies.label.contains("8 cookies · Signed in"))
        app.buttons["cloudBrowser.siteDetail.addLogin"].tap()
        XCTAssertTrue(app.navigationBars["Add login"].waitForExistence(timeout: 3))
        cloudField("cloudBrowser.addLogin.username").tap()
        cloudField("cloudBrowser.addLogin.username").typeText("NOTION-FIXTURE@EXAMPLE.COM")
        cloudField("cloudBrowser.addLogin.password", secure: true).tap()
        cloudField("cloudBrowser.addLogin.password", secure: true).typeText("NOTION-FIXTURE-ONLY")
        app.buttons["cloudBrowser.addLogin.save"].tap()
        XCTAssertTrue(app.navigationBars["notion.so"].waitForExistence(timeout: 3))
        navigateBack(from: "notion.so", to: "Cloud browser")
        let clearAll = app.buttons["cloudBrowser.clearAllData"]
        reveal(clearAll)
        clearAll.tap()
        let confirmAll = app.buttons["cloudBrowser.confirmClearAll"]
        XCTAssertTrue(confirmAll.waitForExistence(timeout: 3))
        capture("CloudBrowser-clear-all-confirmation-light")
        app.buttons["Cancel"].tap()
        app.buttons["cloudBrowser.site.notion.so"].tap()
        XCTAssertTrue(app.navigationBars["notion.so"].waitForExistence(timeout: 3))
        XCTAssertTrue(cookies.label.contains("8 cookies · Signed in"))
        XCTAssertEqual(app.buttons.matching(identifier: "cloudBrowser.siteDetail.login").count, 1)
        navigateBack(from: "notion.so", to: "Cloud browser")
        let checkOtherSites = app.buttons["cloudBrowser.seeAllSites"]
        reveal(checkOtherSites)
        checkOtherSites.tap()
        let unclearedOpenAI = app.buttons["cloudBrowser.site.openai.com"]
        reveal(unclearedOpenAI)
        unclearedOpenAI.tap()
        XCTAssertTrue(app.navigationBars["openai.com"].waitForExistence(timeout: 3))
        XCTAssertTrue(cookies.label.contains("5 cookies · Signed in"), "Cancel must leave other sites untouched")
        navigateBack(from: "openai.com", to: "Sites")
        navigateBack(from: "Sites", to: "Cloud browser")
        reveal(clearAll)
        clearAll.tap()
        XCTAssertTrue(confirmAll.waitForExistence(timeout: 3))
        confirmAll.tap()
        for (domain, username) in [("github.com", "samuel@example.com"), ("notion.so", "NOTION-FIXTURE@EXAMPLE.COM")] {
            let site = app.buttons["cloudBrowser.site.\(domain)"]
            reveal(site)
            site.tap()
            XCTAssertTrue(app.navigationBars[domain].waitForExistence(timeout: 3))
            XCTAssertTrue(cookies.label.contains("0 cookies · Signed out"))
            app.buttons["cloudBrowser.siteDetail.login"].tap()
            XCTAssertTrue(app.staticTexts[username].waitForExistence(timeout: 3))
            XCTAssertTrue(app.staticTexts[String(repeating: "•", count: 12)].exists)
            capture("CloudBrowser-clear-all-retained-login-\(domain)-light")
            navigateBack(from: "Saved login", to: domain)
            navigateBack(from: domain, to: "Cloud browser")
        }
        let allSites = app.buttons["cloudBrowser.seeAllSites"]
        reveal(allSites)
        allSites.tap()
        let openAI = app.buttons["cloudBrowser.site.openai.com"]
        reveal(openAI)
        openAI.tap()
        XCTAssertTrue(app.navigationBars["openai.com"].waitForExistence(timeout: 3))
        XCTAssertTrue(cookies.label.contains("0 cookies · Signed out"))
        capture("CloudBrowser-clear-all-empty-site-light")
    }

    private func captureDestinationAppearance(arguments: [String], suffix: String) {
        for (route, title, name, finalControl) in [
            ("pairedDevices", "Paired devices", "PairedDevices", "pairedDevices.peer.mac-studio"),
            ("memory", "Memory", "Memory", "memory.summaryRow"),
            ("models", "Models", "Models", "models.addProviderKey"),
            ("cloudBrowser", "Cloud browser", "CloudBrowser", "cloudBrowser.seeAllSites")
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

    private func assertWalletSheetDismissed() {
        let hidden = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"),
                                               object: app.buttons["wallet.consent.connect"])
        XCTAssertEqual(XCTWaiter.wait(for: [hidden], timeout: 4), .completed)
        let rootReady = XCTNSPredicateExpectation(predicate: NSPredicate(format: "hittable == true"),
                                                  object: app.buttons["wallet.provider.link"])
        XCTAssertEqual(XCTWaiter.wait(for: [rootReady], timeout: 4), .completed)
    }
    func testWalletBothProvidersCancelBoundaryCloseAndSwipeDismiss() {
        openDestination("wallet", title: "Wallet")
        capture("Wallet-light")
        for (provider, domain) in [("link", "app.link.com"), ("shopPay", "shop.app")] {
            let row = app.buttons["wallet.provider.\(provider)"]
            row.tap()
            XCTAssertTrue(app.buttons["wallet.consent.connect"].waitForExistence(timeout: 3))
            capture("Wallet-\(provider)-consent-light")
            app.buttons["wallet.consent.cancel"].tap()
            assertWalletSheetDismissed()
            row.tap()
            XCTAssertTrue(app.buttons["wallet.consent.connect"].waitForExistence(timeout: 3))
            app.buttons["wallet.consent.connect"].tap()
            XCTAssertTrue(app.navigationBars[domain].waitForExistence(timeout: 3))
            XCTAssertTrue(app.buttons["wallet.external.close"].exists)
            XCTAssertEqual(app.webViews.count, 0, "Boundary must not load a provider website")
            XCTAssertEqual(app.textFields.count + app.secureTextFields.count, 0)
            capture("Wallet-\(provider)-boundary-light")
            app.buttons["wallet.external.close"].tap()
            XCTAssertTrue(row.waitForExistence(timeout: 3))
            let closed = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"),
                                                  object: app.buttons["wallet.external.close"])
            XCTAssertEqual(XCTWaiter.wait(for: [closed], timeout: 4), .completed)
            row.tap()
            XCTAssertTrue(app.buttons["wallet.consent.connect"].waitForExistence(timeout: 3))
            let sheet = app.descendants(matching: .any).matching(identifier: "wallet.consent.\(provider)").firstMatch
            XCTAssertTrue(sheet.exists)
            sheet.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.02))
                .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.95)))
            assertWalletSheetDismissed()
            // A dismissed flow must request consent again, never show a fabricated linked state.
            row.tap()
            XCTAssertTrue(app.buttons["wallet.consent.connect"].waitForExistence(timeout: 3))
            app.buttons["wallet.consent.cancel"].tap()
            assertWalletSheetDismissed()
        }
        navigateBack(from: "Wallet", to: "Agent settings")
    }

    private func voiceRevealAbove(_ element: XCUIElement) {
        for _ in 0..<8 where !element.isHittable { app.swipeDown() }
        XCTAssertTrue(element.isHittable)
    }
    private func voiceSliderPercent(_ slider: XCUIElement) -> Double {
        let text = slider.value as? String ?? ""
        let number = text.split(whereSeparator: { !$0.isNumber && $0 != "." }).compactMap { Double($0) }.first
        XCTAssertNotNil(number, "Slider should expose a numeric accessibility value")
        return number ?? -1
    }
    func testVoiceSelectionPreviewMenuAndSlidersSurviveChooserBack() {
        openDestination("voice", title: "Voice")
        capture("Voice-light")
        app.buttons["voice.conversationEntry"].tap()
        XCTAssertTrue(app.buttons["Voice session"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["Chat"].exists)
        capture("Voice-menu-light")
        app.buttons["Chat"].tap()
        XCTAssertTrue(app.navigationBars["Voice"].exists)
        let choose = app.buttons["voice.chooseVoice"]
        reveal(choose)
        choose.tap()
        XCTAssertTrue(app.navigationBars["Choose a voice"].waitForExistence(timeout: 3))
        let voices = ["aria", "sol", "rowan", "juniper", "vale"]
        for voice in voices { XCTAssertTrue(app.buttons["voice.select.\(voice)"].exists) }
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "voice.select.")).count, 5)
        XCTAssertTrue(app.buttons["voice.select.aria"].isSelected)
        capture("Voice-chooser-light")
        app.buttons["voice.preview.aria"].tap()
        XCTAssertTrue(app.buttons["voice.preview.aria"].label.contains("Pause Aria"))
        capture("Voice-chooser-preview-light")
        app.buttons["voice.preview.aria"].tap()
        app.buttons["voice.preview.rowan"].tap()
        XCTAssertTrue(app.buttons["voice.preview.rowan"].label.contains("Pause Rowan"))
        XCTAssertTrue(app.buttons["voice.select.aria"].isSelected)
        XCTAssertFalse(app.buttons["voice.select.rowan"].isSelected)
        capture("Voice-independent-preview-light")
        app.buttons["voice.select.sol"].tap()
        XCTAssertTrue(app.buttons["voice.select.sol"].isSelected)
        XCTAssertFalse(app.buttons["voice.select.aria"].isSelected)
        XCTAssertTrue(app.buttons["voice.preview.rowan"].label.contains("Pause Rowan"))
        XCTAssertTrue(app.navigationBars["Choose a voice"].exists)
        capture("Voice-independent-selection-light")
        app.buttons["voice.preview.rowan"].tap()
        capture("Voice-chooser-selected-light")
        navigateBack(from: "Choose a voice", to: "Voice")
        XCTAssertEqual(app.buttons["voice.previewSelected"].label, "Preview Sol")
        app.buttons["voice.previewSelected"].tap()
        XCTAssertTrue(app.buttons["voice.previewSelected"].label.contains("Pause Sol"))
        capture("Voice-preview-selected-light")
        app.buttons["voice.previewSelected"].tap()
        var adjusted: [String: Double] = [:]
        for (id, target) in [("speed", 0.75), ("consistency", 0.50), ("likeness", 0.75)] {
            let slider = app.sliders["voice.slider.\(id)"]
            reveal(slider)
            slider.adjust(toNormalizedSliderPosition: CGFloat(target))
            let actual = voiceSliderPercent(slider)
            XCTAssertEqual(actual, target * 100, accuracy: 6)
            adjusted[id] = actual
        }
        capture("Voice-sliders-adjusted-light")
        voiceRevealAbove(choose)
        choose.tap()
        XCTAssertTrue(app.navigationBars["Choose a voice"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["voice.select.sol"].isSelected)
        XCTAssertTrue(app.buttons["voice.preview.rowan"].label.contains("Preview Rowan"))
        navigateBack(from: "Choose a voice", to: "Voice")
        for id in ["speed", "consistency", "likeness"] {
            let slider = app.sliders["voice.slider.\(id)"]
            reveal(slider)
            XCTAssertEqual(voiceSliderPercent(slider), adjusted[id]!, accuracy: 0.1)
        }
        voiceRevealAbove(app.buttons["voice.conversationEntry"])
        XCTAssertTrue(app.buttons["voice.conversationEntry"].label.contains("Chat"))
        navigateBack(from: "Voice", to: "Agent settings")
    }

    private func captureWalletVoiceAppearance(arguments: [String], suffix: String) {
        app.terminate()
        app.launchArguments = arguments
        app.launch()
        openDestination("wallet", title: "Wallet")
        capture("Wallet-\(suffix)")
        for (provider, name) in [("link", "Link"), ("shopPay", "Shop Pay")] {
            app.buttons["wallet.provider.\(provider)"].tap()
            XCTAssertTrue(app.buttons["wallet.consent.connect"].waitForExistence(timeout: 3))
            capture("Wallet-\(provider)-consent-\(suffix)")
            if suffix == "large-text" {
                let benefit = app.descendants(matching: .any).matching(NSPredicate(
                    format: "label CONTAINS %@ AND label CONTAINS %@",
                    "You choose what Rem can do",
                    "Rem asks before actions that need review. Disconnect anytime in Settings."
                )).firstMatch
                revealFully(benefit, above: app.buttons["wallet.consent.connect"])
                capture("Wallet-\(provider)-consent-large-text-benefit")
            }
            let disclosure = app.staticTexts["Next, continue to \(name) to sign in and review access. Rem will exchange info with \(name); see its terms and privacy policy."]
            revealFully(disclosure, above: app.buttons["wallet.consent.connect"])
            XCTAssertTrue(app.buttons["wallet.consent.connect"].isHittable)
            if suffix == "large-text" { capture("Wallet-\(provider)-consent-large-text-footer") }
            app.buttons["wallet.consent.cancel"].tap()
            assertWalletSheetDismissed()
        }
        app.terminate()
        app.launch()
        openDestination("voice", title: "Voice")
        capture("Voice-\(suffix)")
        if suffix == "large-text" {
            revealFully(app.buttons["voice.chooseVoice"])
            capture("Voice-large-text-choice-row")
            reveal(app.sliders["voice.slider.speed"])
            capture("Voice-large-text-speed")
        }
        let footer = app.staticTexts.matching(NSPredicate(
            format: "label == %@",
            "Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice."
        )).firstMatch
        reveal(footer)
        capture("Voice-\(suffix)-footer")
        let choose = app.buttons["voice.chooseVoice"]
        voiceRevealAbove(choose)
        choose.tap()
        XCTAssertTrue(app.navigationBars["Choose a voice"].waitForExistence(timeout: 3))
        capture("Voice-chooser-\(suffix)")
        let chooserFooter = app.staticTexts["Your choice follows this agent across your devices. Tap a play button to hear a preview."]
        revealFully(chooserFooter)
        capture("Voice-chooser-\(suffix)-footer")
    }
    func testWalletVoiceDarkAppearance() {
        captureWalletVoiceAppearance(arguments: ["--settings-dark"], suffix: "dark")
    }
    func testWalletVoiceLargeTextReachability() {
        captureWalletVoiceAppearance(arguments: ["--settings-light", "--settings-large-text"], suffix: "large-text")
    }

}
