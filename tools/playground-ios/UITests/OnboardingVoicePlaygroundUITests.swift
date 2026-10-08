import XCTest

/// Interactive journeys for the Onboarding New → Voice playground shell. These prove the behavior
/// that a static render cannot: default → chooser → preview → select Sol → Back reflecting Sol,
/// independent preview/selection, three persistent slider changes, preview stop, the distinct
/// Back / Continue / Skip host callbacks, and scroll + large-text reachability of the pinned actions.
/// Everything is local fixture state — no audio, account, service, or persistence.
final class OnboardingVoicePlaygroundUITests: XCTestCase {
    let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = ["--settings-light"]
        app.launch()
    }

    private func capture(_ name: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(0.5)) // Let native navigation chrome settle.
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func reveal(_ element: XCUIElement) {
        for _ in 0..<8 where !element.isHittable {
            if app.scrollViews.firstMatch.exists { app.scrollViews.firstMatch.swipeUp() }
            else { app.swipeUp() }
        }
        XCTAssertTrue(element.isHittable, "Expected reachable control: \(element.identifier)")
    }

    private func revealDown(_ element: XCUIElement) {
        for _ in 0..<8 where !element.isHittable { app.swipeDown() }
        XCTAssertTrue(element.isHittable, "Expected reachable control: \(element.identifier)")
    }

    private func openOnboardingVoice() {
        let open = app.buttons["openOnboardingVoice"]
        reveal(open)
        open.tap()
        XCTAssertTrue(app.staticTexts["Choose how Rem sounds"].waitForExistence(timeout: 3),
                      "Expected the onboarding Voice lockup title")
    }

    private func navigateBack(from title: String) {
        app.navigationBars[title].buttons.element(boundBy: 0).tap()
    }

    private func sliderPercent(_ slider: XCUIElement) -> Double {
        let text = slider.value as? String ?? ""
        let number = text.split(whereSeparator: { !$0.isNumber && $0 != "." }).compactMap { Double($0) }.first
        XCTAssertNotNil(number, "Slider should expose a numeric accessibility value")
        return number ?? -1
    }

    /// default → chooser → preview → independent selection → select Sol → Back reflecting Sol,
    /// then preview stop and three persistent slider changes preserved across chooser navigation.
    func testDefaultChooserPreviewSelectionBackAndSliders() {
        openOnboardingVoice()
        XCTAssertTrue(app.staticTexts["Preview a voice, choose the one Rem uses, then fine-tune its delivery."].exists)
        XCTAssertTrue(app.staticTexts["Aria (Warm)"].exists, "Default selection is Aria")
        capture("OnboardingVoice-light")

        let choose = app.buttons["voice.chooseVoice"]
        reveal(choose)
        choose.tap()
        XCTAssertTrue(app.navigationBars["Choose a voice"].waitForExistence(timeout: 3))
        let voices = ["aria", "sol", "rowan", "juniper", "vale"]
        for voice in voices { XCTAssertTrue(app.buttons["voice.select.\(voice)"].exists) }
        XCTAssertTrue(app.buttons["voice.select.aria"].isSelected)
        capture("OnboardingVoice-chooser-light")

        // Preview is a separate target from selection.
        app.buttons["voice.preview.aria"].tap()
        XCTAssertTrue(app.buttons["voice.preview.aria"].label.contains("Pause Aria"))
        capture("OnboardingVoice-chooser-preview-light")

        app.buttons["voice.preview.aria"].tap()
        app.buttons["voice.preview.rowan"].tap()
        XCTAssertTrue(app.buttons["voice.preview.rowan"].label.contains("Pause Rowan"))
        // Previewing Rowan must not change the selection (still Aria).
        XCTAssertTrue(app.buttons["voice.select.aria"].isSelected)
        XCTAssertFalse(app.buttons["voice.select.rowan"].isSelected)

        // Selecting Sol must not change the running Rowan preview (independent state).
        app.buttons["voice.select.sol"].tap()
        XCTAssertTrue(app.buttons["voice.select.sol"].isSelected)
        XCTAssertFalse(app.buttons["voice.select.aria"].isSelected)
        XCTAssertTrue(app.buttons["voice.preview.rowan"].label.contains("Pause Rowan"))

        // Native Back returns to the shell reflecting Sol with preview stopped.
        navigateBack(from: "Choose a voice")
        XCTAssertTrue(app.staticTexts["Choose how Rem sounds"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Sol (Bright)"].exists, "Shell must reflect the Sol selection")
        XCTAssertEqual(app.buttons["voice.previewSelected"].label, "Preview Sol", "Leaving the chooser stops preview")
        capture("OnboardingVoice-selected-light")

        // Preview can independently start and stop from the shell.
        app.buttons["voice.previewSelected"].tap()
        XCTAssertTrue(app.buttons["voice.previewSelected"].label.contains("Pause Sol"))
        app.buttons["voice.previewSelected"].tap()
        XCTAssertEqual(app.buttons["voice.previewSelected"].label, "Preview Sol")

        // Three persistent slider changes, represented through the native slider accessibility value.
        var adjusted: [String: Double] = [:]
        for (id, target) in [("speed", 0.75), ("consistency", 0.50), ("likeness", 0.75)] {
            let slider = app.sliders["voice.slider.\(id)"]
            reveal(slider)
            slider.adjust(toNormalizedSliderPosition: CGFloat(target))
            let actual = sliderPercent(slider)
            XCTAssertEqual(actual, target * 100, accuracy: 6)
            adjusted[id] = actual
        }
        capture("OnboardingVoice-sliders-adjusted-light")

        // Values persist across chooser navigation.
        let chooseAgain = app.buttons["voice.chooseVoice"]
        revealDown(chooseAgain)
        chooseAgain.tap()
        XCTAssertTrue(app.navigationBars["Choose a voice"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["voice.select.sol"].isSelected)
        navigateBack(from: "Choose a voice")
        XCTAssertTrue(app.staticTexts["Choose how Rem sounds"].waitForExistence(timeout: 3))
        for id in ["speed", "consistency", "likeness"] {
            let slider = app.sliders["voice.slider.\(id)"]
            reveal(slider)
            XCTAssertEqual(sliderPercent(slider), adjusted[id]!, accuracy: 0.1)
        }
    }

    func testContinueCallback() {
        openOnboardingVoice()
        let cont = app.buttons["onboardingVoice.continue"]
        XCTAssertTrue(cont.waitForExistence(timeout: 3))
        cont.tap()
        let outcome = app.staticTexts["onboardingVoice.outcome"]
        XCTAssertTrue(outcome.waitForExistence(timeout: 3))
        XCTAssertEqual(outcome.label, "Host callback: continue")
        XCTAssertFalse(app.buttons["onboardingVoice.skip"].exists, "Continue invokes only the Continue callback")
        capture("OnboardingVoice-continue-light")
    }

    func testSkipCallback() {
        openOnboardingVoice()
        let skip = app.buttons["onboardingVoice.skip"]
        XCTAssertTrue(skip.waitForExistence(timeout: 3))
        skip.tap()
        let outcome = app.staticTexts["onboardingVoice.outcome"]
        XCTAssertTrue(outcome.waitForExistence(timeout: 3))
        XCTAssertEqual(outcome.label, "Host callback: skip")
        XCTAssertFalse(app.buttons["onboardingVoice.continue"].exists, "Skip invokes only the Skip callback")
        capture("OnboardingVoice-skip-light")
    }

    func testOuterBackExitsToHost() {
        openOnboardingVoice()
        // The shell's own nav bar has no title; its back button is the first navigation-bar button.
        app.navigationBars.element(boundBy: 0).buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.buttons["openOnboardingVoice"].waitForExistence(timeout: 3),
                      "Outer Back exits the Voice shell to the playground host")
        XCTAssertFalse(app.staticTexts["Choose how Rem sounds"].exists)
        capture("OnboardingVoice-back-light")
    }

    func testDarkAppearance() {
        app.terminate()
        app.launchArguments = ["--settings-dark"]
        app.launch()
        openOnboardingVoice()
        capture("OnboardingVoice-dark")
        let choose = app.buttons["voice.chooseVoice"]
        reveal(choose)
        choose.tap()
        XCTAssertTrue(app.navigationBars["Choose a voice"].waitForExistence(timeout: 3))
        capture("OnboardingVoice-chooser-dark")
    }

    func testLargeTextReachability() {
        app.terminate()
        app.launchArguments = ["--settings-light", "--settings-large-text"]
        app.launch()
        openOnboardingVoice()
        capture("OnboardingVoice-large-text")
        // The pinned actions stay reachable even while the content scrolls at large Dynamic Type.
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].isHittable)
        XCTAssertTrue(app.buttons["onboardingVoice.skip"].isHittable)
        let footer = app.staticTexts.matching(NSPredicate(format: "label == %@",
            "Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice.")).firstMatch
        reveal(footer)
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].isHittable, "Continue remains reachable after scrolling content")
        capture("OnboardingVoice-large-text-bottom")
    }
}
