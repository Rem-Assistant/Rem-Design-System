import XCTest

final class SettingsPlaygroundUITests: XCTestCase {
    let app = XCUIApplication()
    override func setUpWithError() throws {
        continueAfterFailure = false
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
        capture("ios-settings-entry")
        app.buttons["openAgent"].tap()
        XCTAssertTrue(app.staticTexts["Capabilities"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Scheduled and triggered work"].exists)
        capture("ios-agent-settings")
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.buttons["openAgent"].waitForExistence(timeout: 3))
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
}
