import XCTest

/// Launch arguments every journey in this target starts with (all four UI test classes). `--no-motion` stops
/// the design system's repeating animations (RemFaceMark's idle and thinking loops) and the skeleton shimmer, so
/// the app reaches idle between steps and each step does not wait on a never-ending animation (route-map
/// "Test seams"). Pass the journey's own arguments; this appends the shared ones.
enum JourneyLaunch {
    static let shared = ["--no-motion"]

    static func arguments(_ arguments: [String]) -> [String] { arguments + shared }
}

/// Root structure, the component catalog's loading demo, and the onboarding flow's step routing.
/// Everything is local fixture state — no account, service, audio, or persistence.
final class PlaygroundNavigationUITests: XCTestCase {
    let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launchArguments = JourneyLaunch.arguments(["--developer", "--settings-light"])
        app.launch()
    }

    /// On failure, print what was on screen (bounded) so it reaches the job log; the workflow repeats
    /// it at the end of a failed job, since attachments are exported only after a passing run.
    override func tearDownWithError() throws {
        if let run = testRun, run.failureCount > 0 {
            let tree = app.debugDescription
            print("[failure-diagnostic] \(name)\n\(tree.prefix(12_000))\n[/failure-diagnostic]")
        }
    }

    private func capture(_ name: String) {
        RunLoop.current.run(until: Date().addingTimeInterval(0.5)) // Let native navigation chrome settle.
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func reveal(_ element: XCUIElement) {
        if app.keyboards.count > 0 {
            revealAboveKeyboard(element)
            return
        }
        // A control can be hittable yet sit in the bottom home-indicator strip, where synthesized taps
        // are intermittently swallowed (run 38094402869: the composer's Add at y 808-852 of 852 never
        // opened its sheet). Scroll it clear of that strip as well.
        // Scroll with full-window swipes from mid-screen, as a person would: on the Chat page they start
        // on long-pressable bubbles, so a gesture composition that swallows that scroll fails here (run
        // 38112625726 saw it right after the model menu closed). A docked control that scrolling cannot
        // move stops after one unproductive swipe.
        let bottomGestureEdge = app.windows.firstMatch.frame.maxY - 34
        var lastMaxY: CGFloat?
        for _ in 0..<8 {
            let hittable = element.isHittable
            if hittable && element.frame.maxY <= bottomGestureEdge { break }
            if hittable, let lastMaxY, abs(lastMaxY - element.frame.maxY) < 1 { break }
            lastMaxY = hittable ? element.frame.maxY : nil
            app.swipeUp()
        }
        XCTAssertTrue(element.isHittable, "Expected reachable control: \(element.identifier)")
    }

    /// The part of the page's scroll view a person can see while typing: below the navigation bar and
    /// above the keyboard and its input-assistant bar (a full-window swipe would start over the
    /// keyboard and scroll nothing).
    private func visibleScrollRegion(_ scrollView: XCUIElement) -> CGRect {
        let frame = scrollView.frame
        let keyboard = app.keyboards.firstMatch
        var top = keyboard.exists ? keyboard.frame.minY : frame.maxY
        let assistant = app.otherElements["SystemInputAssistantView"]
        if assistant.exists { top = min(top, assistant.frame.minY) }
        var minY = frame.minY
        let bar = app.navigationBars.firstMatch
        if bar.exists { minY = max(minY, bar.frame.maxY) } // the scroll view extends under the bar
        return CGRect(x: frame.minX, y: minY, width: frame.width, height: max(0, min(frame.maxY, top) - minY))
    }

    /// With the keyboard up, drags inside the scroll view's blank leading margin and only within the
    /// visible region, until `element` sits fully inside that region and is hittable. Bounded: stops
    /// when a drag makes no progress.
    private func revealAboveKeyboard(_ element: XCUIElement) {
        // The page's scroll view is the one holding `element`: with the keyboard up, the input-assistant
        // bar's typing-predictions scroll view is also in the tree and can be the first match.
        let holds = element.identifier.isEmpty
            ? NSPredicate(format: "label == %@", element.label)
            : NSPredicate(format: "identifier == %@", element.identifier)
        let scrollView = app.scrollViews.containing(holds).firstMatch
        XCTAssertTrue(scrollView.exists, "The page exposes the scroll view holding \(element.identifier)")
        let origin = app.coordinate(withNormalizedOffset: .zero)
        var lastMinY: CGFloat?
        for _ in 0..<8 {
            let visible = visibleScrollRegion(scrollView)
            let target = element.frame
            // Success falls through to the shared final assertions (keyboard still up, fully visible).
            if element.exists && visible.contains(target) && element.isHittable { break }
            if let lastMinY, abs(lastMinY - target.minY) < 1 { break }
            lastMinY = target.minY
            let up = target.minY < visible.minY
            let x = scrollView.frame.minX + 8
            let near = visible.minY + visible.height * 0.3
            let far = visible.maxY - visible.height * 0.15
            let from = origin.withOffset(CGVector(dx: x, dy: up ? near : far))
            let to = origin.withOffset(CGVector(dx: x, dy: up ? far : near))
            from.press(forDuration: 0.05, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.3)
        }
        let visible = visibleScrollRegion(scrollView)
        XCTAssertGreaterThan(app.keyboards.count, 0, "The keyboard stays up while revealing \(element.identifier)")
        XCTAssertTrue(element.exists && visible.contains(element.frame) && element.isHittable,
                      "Expected \(element.identifier) fully visible above the keyboard (\(element.frame) in \(visible))")
    }

    /// Taps a text input and requires it to take keyboard focus before anything is typed, so a tap
    /// that does not focus fails here rather than inside typeText.
    private func focus(_ field: XCUIElement) {
        field.tap()
        dismissKeyboardIntroduction()
        // Accessibility snapshots of a busy hierarchy can take seconds, so poll a freshly resolved
        // element (by type + identifier) for a bounded 10s, then evaluate once more before failing.
        // Keyboard focus itself stays the requirement.
        let hasFocus = NSPredicate(format: "hasKeyboardFocus == true")
        let type = field.elementType, identifier = field.identifier
        let fresh = { self.app.descendants(matching: type).matching(identifier: identifier).firstMatch }
        let deadline = Date().addingTimeInterval(10)
        var isFocused = false
        while !isFocused && Date() < deadline {
            isFocused = hasFocus.evaluate(with: fresh())
            if !isFocused { RunLoop.current.run(until: Date().addingTimeInterval(0.25)) }
        }
        if !isFocused { isFocused = hasFocus.evaluate(with: fresh()) }
        let result: XCTWaiter.Result = isFocused ? .completed : .timedOut
        if result != .completed {
            // Evidence for a failure: what the tap hit and what, if anything, holds focus. Printed
            // (bounded) so it reaches the job log, since attachments are exported only after a pass.
            let tree = app.debugDescription
            let limit = 12_000
            print("""
            [focus-diagnostic] \(field.identifier) unfocused after tap; keyboards: \(app.keyboards.count); \
            frame: \(field.frame); hierarchy (\(min(tree.count, limit)) of \(tree.count) chars):
            \(tree.prefix(limit))
            [/focus-diagnostic]
            """)
            let hierarchy = XCTAttachment(string: tree)
            hierarchy.name = "\(field.identifier)-unfocused-hierarchy"
            hierarchy.lifetime = .keepAlways
            add(hierarchy)
        }
        XCTAssertEqual(result, .completed, "\(field.identifier) did not take keyboard focus on tap")
    }

    /// On a simulator's first text entry iOS lays a one-time "slide to type" tip over the keyboard
    /// (`UIContinuousPathIntroductionView`). It is OS onboarding, not app UI; the full run's screen
    /// dump showed it over a focused composer. Dismiss it with its own Continue when it appears.
    private func dismissKeyboardIntroduction() {
        let intro = app.otherElements["UIContinuousPathIntroductionView"]
        guard intro.waitForExistence(timeout: 2) else { return }
        intro.buttons["Continue"].tap()
        waitUntilGone(intro, "The keyboard tip closes")
    }

    private func tap(_ identifier: String) {
        let element = app.buttons[identifier]
        XCTAssertTrue(element.waitForExistence(timeout: 3), "Missing \(identifier)")
        reveal(element)
        element.tap()
    }

    /// Taps the first button whose combined label contains `text` (rows fold icons and subtitles in).
    private func tapButton(containing text: String) {
        let element = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
        XCTAssertTrue(element.waitForExistence(timeout: 3), "Missing button containing \(text)")
        reveal(element)
        element.tap()
    }

    /// Opens the catalog composer's Add to Chat sheet. Same dropped-tap guard as `openCatalogPage`:
    /// if the sheet has not appeared after 3s and Add is still hittable, tap it once more, and still
    /// require the sheet to open.
    private func openAddToChat() {
        tap("catalog.composerAdd")
        let photos = app.buttons["catalog.addToChat.photos"]
        let add = app.buttons["catalog.composerAdd"]
        if !photos.waitForExistence(timeout: 3) && add.exists && add.isHittable { add.tap() }
        XCTAssertTrue(photos.waitForExistence(timeout: 5), "Add opens the Add to Chat sheet")
    }

    private func waitUntilGone(_ element: XCUIElement, _ message: String) {
        let gone = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: element)
        XCTAssertEqual(XCTWaiter().wait(for: [gone], timeout: 3), .completed, message)
    }

    private func openOnboardingStep(_ identifier: String) {
        tap("openOnboarding")
        tap(identifier)
    }

    // MARK: Root

    func testRootOffersComponentsAndScreensOnly() {
        for id in ["openComponents", "openSettings", "openAgendaSuggestions", "openOnboarding"] {
            XCTAssertTrue(app.buttons[id].waitForExistence(timeout: 3), "Root is missing \(id)")
        }
        XCTAssertFalse(app.buttons["Shared controls"].exists, "Controls live in the component catalog")
        XCTAssertFalse(app.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] %@", "prototype")).firstMatch.exists,
                       "Root carries functional labels only")
        XCTAssertTrue(app.staticTexts["playground.build"].exists, "Build revision stays visible")
        capture("Playground-root-light")
    }

    // MARK: Components

    func testCatalogLoadingShowsSkeletonThenContentAndActionProgress() {
        app.terminate()
        app.launchArguments = JourneyLaunch.arguments(["--developer", "--settings-light", "--loading-hold"])
        app.launch()
        tap("openComponents")
        XCTAssertTrue(app.buttons["openControls"].waitForExistence(timeout: 3))
        capture("Playground-components-light")
        tap("openLoading")
        let skeleton = app.descendants(matching: .any)["loading.skeleton"].firstMatch
        let memory = app.staticTexts["Memory"]
        XCTAssertTrue(skeleton.waitForExistence(timeout: 2), "Content load starts on the skeleton")
        XCTAssertEqual(skeleton.label, "Loading content")
        XCTAssertFalse(memory.exists, "Held loading contains no loaded rows")
        tap("loading.completeFixture")
        XCTAssertTrue(memory.waitForExistence(timeout: 8), "Skeleton resolves to content")
        waitUntilGone(skeleton, "Skeleton is removed once content arrives")
        capture("Loading-content-light")
        // Reload holds the fixture in place, without navigation competing for the capture.
        // Every skeleton assertion has a deterministic window, including the initial push above.
        // Bracket the screenshot with state checks so mislabeled loaded-content proof fails.
        tap("loading.reload")
        XCTAssertTrue(skeleton.waitForExistence(timeout: 2), "Reload returns to the skeleton")
        waitUntilGone(memory, "Reload replaces the content with the skeleton")
        XCTAssertTrue(skeleton.exists && !memory.exists, "Capture starts on the skeleton only")
        capture("Loading-skeleton-light")
        XCTAssertTrue(skeleton.exists && !memory.exists,
                      "The load finished before Loading-skeleton-light was captured, so it may not show the skeleton")
        tap("loading.completeFixture")
        XCTAssertTrue(memory.waitForExistence(timeout: 8), "Reloaded skeleton resolves to content")
        waitUntilGone(skeleton, "Skeleton is removed once reloaded content arrives")
        tap("loading.refresh")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Refreshing")).firstMatch.waitForExistence(timeout: 2),
                      "Actions show inline progress")
        XCTAssertTrue(app.staticTexts["Memory"].exists, "Content stays visible during an action")
        capture("Loading-action-progress-light")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label == %@", "Refresh")).firstMatch.waitForExistence(timeout: 6),
                      "Action progress clears")
    }

    /// The test fixture must not replace the normal Playground's automatic completion path.
    func testCatalogLoadingCompletesAutomaticallyWithoutFixtureHold() {
        tap("openComponents")
        tap("openLoading")
        XCTAssertFalse(app.buttons["loading.completeFixture"].exists,
                       "Normal Playground use exposes no fixture completion action")
        XCTAssertTrue(app.staticTexts["Memory"].waitForExistence(timeout: 8))
        XCTAssertFalse(app.descendants(matching: .any)["loading.skeleton"].firstMatch.exists)
        tap("loading.reload")
        XCTAssertTrue(app.staticTexts["Memory"].waitForExistence(timeout: 8))
        XCTAssertTrue(app.buttons["loading.refresh"].isEnabled)
    }

    // MARK: Catalog pages

    private func openCatalogPage(_ identifier: String, title: String) {
        tap("openComponents")
        tap(identifier)
        let bar = app.navigationBars[title]
        // A synthesized row tap is occasionally dropped: the app goes idle at once, no push starts and
        // the catalog stays put (Chat, run 38092241592). Tap the still-reachable row once more; the page
        // must still open.
        if !bar.waitForExistence(timeout: 3) {
            let row = app.buttons[identifier]
            if row.exists && row.isHittable { row.tap() }
        }
        XCTAssertTrue(bar.waitForExistence(timeout: 5), "\(title) page opens")
    }

    private func slug(_ text: String) -> String {
        text.lowercased().map { $0.isLetter || $0.isNumber ? String($0) : "-" }.joined()
            .split(separator: "-").joined(separator: "-")
    }

    /// One overview capture: `name` names the file, `anchor` is text inside the component (never its
    /// group heading). `last` picks the final match when the page repeats the text (the trace's
    /// "Working" footer follows the "Working" status pill); `up` scrolls back toward the page top.
    private struct Shot {
        let name: String
        let anchor: String
        var last = false
        var up = false
    }

    /// Scrolls in short drags held at the end (no fling) until `element` is hittable, so it stops just
    /// inside the edge it entered from: the bottom when scrolling down, with its component above it,
    /// or the top when scrolling up, with its component below it. Drags start in the scroll view's
    /// leading margin, clear of the page's controls and of the scroll indicator.
    private func scroll(to element: XCUIElement, named anchor: String, up: Bool) {
        // Drag inside the page's scroll view, in its blank leading margin: the trailing edge is the
        // interactive scroll indicator on long pages, which absorbs a short drag without scrolling.
        // Form-based pages (Controls) expose a collection view rather than a scroll view.
        let scroller = app.scrollViews.firstMatch.exists ? app.scrollViews.firstMatch : app.collectionViews.firstMatch
        XCTAssertTrue(scroller.exists, "The page exposes its scroller")
        let dx = 8 / max(scroller.frame.width, 1)
        let from = scroller.coordinate(withNormalizedOffset: CGVector(dx: dx, dy: up ? 0.35 : 0.65))
        let to = scroller.coordinate(withNormalizedOffset: CGVector(dx: dx, dy: up ? 0.65 : 0.35))
        for _ in 0..<24 {
            if element.exists && element.isHittable { break }
            from.press(forDuration: 0.05, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.3)
        }
        XCTAssertTrue(element.exists && element.isHittable, "Expected reachable content: \(anchor)")
    }

    /// Unobscured overview captures: each page's top, then each component scrolled into view by its
    /// own content, before any interaction (so no keyboard or post-action scroll position). The
    /// execution trace is captured at its bottom, then its top, in case it outgrows the viewport.
    func testCatalogSectionCaptures() {
        tap("openComponents")
        let pages: [(id: String, title: String, slug: String, shots: [Shot])] = [
            ("openControls", "Controls", "controls", [
                Shot(name: "Buttons", anchor: "Pill \u{00B7} Secondary"),
                Shot(name: "Slider", anchor: "50%"),
                Shot(name: "Pills", anchor: "Personal"),
            ]),
            ("openRows", "Rows", "rows", [
                Shot(name: "Section", anchor: "Applies to this device."),
                Shot(name: "Connector row", anchor: "Gmail"),
                Shot(name: "Task and event rows", anchor: "Unfiled inbox task"),
            ]),
            ("openCatalogAgenda", "Agenda", "agenda", [
                Shot(name: "Suggestion rows", anchor: "Reply to the venue"),
                Shot(name: "Suggestion section", anchor: "See more"),
            ]),
            ("openChat", "Chat", "chat", [
                Shot(name: "Composer", anchor: "Auto"),
                Shot(name: "Voice bar", anchor: "Listening\u{2026}"),
            ]),
            ("openAgentCatalog", "Agent", "agent", [
                Shot(name: "Running task banner", anchor: "Needs you \u{00B7} Password rejected"),
                Shot(name: "Browser card", anchor: "Rem's browser session"),
                Shot(name: "Execution trace bottom", anchor: "Working", last: true),
                Shot(name: "Execution trace top", anchor: "IN PROGRESS", up: true),
                Shot(name: "Daily brief card", anchor: "Read latest brief"),
            ]),
            ("openBrand", "Brand & empty states", "brand", [
                Shot(name: "Provider marks", anchor: "Google"),
                Shot(name: "Empty state", anchor: "Add New"),
            ]),
        ]
        for page in pages {
            tap(page.id)
            XCTAssertTrue(app.navigationBars[page.title].waitForExistence(timeout: 3), "\(page.title) opens")
            capture("Catalog-\(page.slug)-top-light")
            for shot in page.shots {
                // Combined accessibility labels fold row text together, so match by containment.
                let matches = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", shot.anchor))
                let element = shot.last ? matches.element(boundBy: max(matches.count - 1, 0)) : matches.firstMatch
                scroll(to: element, named: shot.anchor, up: shot.up)
                capture("Catalog-\(page.slug)-\(slug(shot.name))-light")
            }
            app.navigationBars[page.title].buttons.element(boundBy: 0).tap()
            XCTAssertTrue(app.buttons[page.id].waitForExistence(timeout: 3), "Back returns to the catalog")
        }
    }

    func testCatalogListsEveryPage() {
        tap("openComponents")
        for id in ["openControls", "openRows", "openCatalogAgenda", "openChat", "openAgentCatalog", "openBrand", "openLoading"] {
            XCTAssertTrue(app.buttons[id].waitForExistence(timeout: 3), "Catalog is missing \(id)")
        }
    }

    func testCatalogControlsButtonsSliderAndPills() {
        openCatalogPage("openControls", title: "Controls")
        let blue = app.buttons["Rect · Blue"]
        reveal(blue)
        blue.tap()
        let last = app.staticTexts["controls.lastButton"]
        XCTAssertTrue(last.waitForExistence(timeout: 2))
        XCTAssertEqual(last.label, "Tapped Rect · Blue", "Only the tapped variant fires")
        let level = app.staticTexts["50%"]
        reveal(level)
        XCTAssertTrue(level.exists, "Slider value is shown")
        XCTAssertFalse(app.buttons["Disabled"].isEnabled, "Disabled variant is not interactive")
        capture("Catalog-controls-light")
    }

    func testCatalogRowsListAndConnectorStates() {
        openCatalogPage("openRows", title: "Rows")
        tap("catalog.listRow")
        // ListRow buttons fold their subtitle into the button label.
        let opened = XCTNSPredicateExpectation(predicate: NSPredicate(format: "label CONTAINS %@", "Opened"),
                                               object: app.buttons["catalog.listRow"])
        XCTAssertEqual(XCTWaiter().wait(for: [opened], timeout: 3), .completed, "List row action runs")
        tapButton(containing: "Connect Gmail")
        XCTAssertTrue(app.switches.firstMatch.waitForExistence(timeout: 5), "Connect moves through Connecting to Connected")
        capture("Catalog-rows-light")
    }

    func testCatalogAgendaDateAndSuggestions() {
        openCatalogPage("openCatalogAgenda", title: "Agenda")
        XCTAssertTrue(app.staticTexts["Oct 1 2026"].exists)
        app.buttons["Next day"].tap()
        XCTAssertTrue(app.staticTexts["Oct 2 2026"].waitForExistence(timeout: 2), "Next day advances the date")
        tap("catalog.suggestion.accept.add")
        XCTAssertTrue(app.staticTexts["Added"].waitForExistence(timeout: 2), "Accepting resolves the suggestion")
        capture("Catalog-agenda-light")
        tap("catalog.suggestion.restore")
        XCTAssertTrue(app.buttons["catalog.suggestion.accept.add"].waitForExistence(timeout: 2), "Restore brings it back")
        // Suggestion section: three inline, the fourth behind See more.
        let fourth = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "Book the venue for the offsite")).firstMatch
        XCTAssertFalse(fourth.exists, "Only three suggestions render inline")
        let seeMore = app.buttons["See more"]
        reveal(seeMore)
        seeMore.tap()
        XCTAssertTrue(fourth.waitForExistence(timeout: 2), "See more reveals the rest")
        tap("catalog.section.reset")
    }

    func testCatalogChatComposerSendsMessage() {
        openCatalogPage("openChat", title: "Chat")
        // A vertical-axis TextField is exposed as a text view.
        let field = app.textViews["catalog.composerField"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        reveal(field)
        focus(field)
        field.typeText("Plan my afternoon")
        tap("catalog.composerSend")
        let sentBubble = element("chat.sent.0.bubble")
        XCTAssertTrue(sentBubble.waitForExistence(timeout: 3), "Send adds a message bubble")
        XCTAssertTrue(sentBubble.label.contains("Plan my afternoon"))
        capture("Catalog-chat-light")
    }

    /// First element anywhere in the tree with this accessibility identifier.
    private func element(_ identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    /// A vertical drag that starts on a long-pressable bubble scrolls the page and opens nothing. Drags
    /// up first; if the page is already at its end, drags back down. Either must move the bubble.
    private func assertDragOnBubbleScrolls(_ candidates: [XCUIElement], _ context: String) {
        guard let bubble = candidates.first(where: { $0.exists && $0.isHittable }) else {
            XCTFail("\(context): no long-pressable bubble on screen to start the drag on")
            return
        }
        let before = bubble.frame.minY
        for dy: CGFloat in [-220, 220] {
            let start = bubble.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            start.press(forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 0, dy: dy)))
            if abs(bubble.frame.minY - before) > 20 { break }
        }
        XCTAssertGreaterThan(abs(bubble.frame.minY - before), 20,
                             "\(context): a vertical drag that starts on a bubble scrolls the page")
        XCTAssertFalse(app.buttons["chat.reactions.0"].exists, "\(context): the drag opens no reaction row")
    }

    /// Chat slice journey, all local fixture state: header activity, long-press reaction (outgoing
    /// upper-left), failed delivery and Try again, the Auto model menu, the Cloud browser chip and the
    /// Thinking level. No message, reaction, model choice or attachment leaves the page.
    func testCatalogChatReactionsDeliveryModelMenuAndAttachments() {
        openCatalogPage("openChat", title: "Chat")

        let identity = element("chat.header.identity")
        XCTAssertTrue(identity.waitForExistence(timeout: 3))
        XCTAssertTrue(identity.label.contains("Connected"), "Header shows the agent's current activity")

        let outgoing = element("chat.outgoing.bubble")
        reveal(outgoing)
        XCTAssertFalse(element("chat.outgoing.reaction").exists)
        outgoing.press(forDuration: 1.0)
        let heart = app.buttons["chat.reactions.2"]
        XCTAssertTrue(heart.waitForExistence(timeout: 3), "Long press opens the six-choice reaction row")
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "chat.reactions.")).count, 6)
        heart.tap()
        let reaction = element("chat.outgoing.reaction")
        XCTAssertTrue(reaction.waitForExistence(timeout: 3), "Chosen reaction shows on the message")
        // Outgoing reaction sits at the upper-left, overlapping toward the conversation centre.
        XCTAssertLessThan(reaction.frame.minX, outgoing.frame.minX)
        XCTAssertLessThan(reaction.frame.minY, outgoing.frame.minY)

        let failedBubble = element("chat.failed.bubble")
        let failure = element("chat.failed.failure")
        reveal(failure)
        let receipt = element("chat.failed.receipt")
        XCTAssertEqual(receipt.label, "Not delivered")
        // The failure control sits entirely outside the bubble on the right; the label is right-aligned to it.
        XCTAssertGreaterThanOrEqual(failure.frame.minX, failedBubble.frame.maxX)
        XCTAssertEqual(receipt.frame.maxX, failedBubble.frame.maxX, accuracy: 1)
        failure.tap()
        let tryAgain = app.buttons["Try again"]
        XCTAssertTrue(tryAgain.waitForExistence(timeout: 3), "Failure control opens the Try again menu")
        tryAgain.tap()
        XCTAssertTrue(element("chat.failed.receipt").label.contains("Delivered"), "Try again resolves the fixture failure")

        let menu = app.buttons["catalog.modelMenu"]
        reveal(menu)
        XCTAssertTrue(menu.label.contains("Auto"))
        menu.tap()
        XCTAssertTrue(app.buttons["Automatic"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["Manage Models"].exists)
        app.buttons["Provider A"].tap()
        let modelA2 = app.buttons["Model A2"]
        XCTAssertTrue(modelA2.waitForExistence(timeout: 3), "Provider submenu lists its models")
        modelA2.tap()
        XCTAssertTrue(menu.label.contains("Model A2"), "Trigger shows the selected model")
        menu.tap()
        app.buttons["Automatic"].tap()
        // The next tap must not land while the menu's dismissal layer is still up.
        waitUntilGone(app.buttons["Automatic"], "Choosing Automatic closes the model menu")
        XCTAssertTrue(menu.label.contains("Auto"), "Automatic returns the trigger to Auto")
        assertDragOnBubbleScrolls([failedBubble, outgoing], "Right after the model menu closes")

        openAddToChat()
        XCTAssertTrue(app.buttons["catalog.addToChat.files"].exists)
        tap("catalog.addToChat.cloudBrowser")
        let removeBrowser = app.buttons["catalog.removeAttachment.cloud-browser"]
        XCTAssertTrue(removeBrowser.waitForExistence(timeout: 3), "Cloud browser adds a removable chip and dismisses")
        removeBrowser.tap()
        waitUntilGone(removeBrowser, "Removing the chip clears it")

        openAddToChat()
        let thinking = app.buttons["catalog.addToChat.thinking"]
        XCTAssertTrue(thinking.waitForExistence(timeout: 3))
        XCTAssertTrue(thinking.label.contains("Medium"))
        thinking.tap()
        app.buttons["High"].tap()
        XCTAssertTrue(thinking.label.contains("High"), "Thinking level is chosen from four options")
        tap("catalog.addToChat.done")
        waitUntilGone(thinking, "Done dismisses Add to Chat")
        capture("Catalog-chat-journey-light")
    }

    func testCatalogAgentSurfaces() {
        openCatalogPage("openAgentCatalog", title: "Agent")
        XCTAssertTrue(app.staticTexts["Working"].firstMatch.exists)
        let ended = app.segmentedControls["catalog.browserState"].buttons["Ended"]
        reveal(ended)
        ended.tap()
        XCTAssertTrue(ended.isSelected, "Browser card state can be switched")
        let read = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Read latest brief")).firstMatch
        reveal(read)
        read.tap()
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Stop reading")).firstMatch.waitForExistence(timeout: 2),
                      "The brief's read action toggles")
        capture("Catalog-agent-light")
    }

    func testCatalogBrandAndEmptyState() {
        openCatalogPage("openBrand", title: "Brand & empty states")
        tapButton(containing: "Add New")
        XCTAssertTrue(app.buttons["catalog.emptyReset"].waitForExistence(timeout: 2), "Empty-state action shows content")
        capture("Catalog-brand-light")
        tap("catalog.emptyReset")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Add New")).firstMatch.waitForExistence(timeout: 2))
        XCTAssertTrue(app.descendants(matching: .any)["catalog.providerMarks"].firstMatch.exists, "Provider marks render")
    }

    // MARK: Onboarding

    /// Continue from Sign in walks every established step, in order, to the completion state.
    func testContinueAdvancesThroughEveryStepToCompletion() {
        openOnboardingStep("openOnboardingSignIn")
        capture("Onboarding-signIn-light")
        tapButton(containing: "Continue with Apple")
        XCTAssertTrue(app.buttons["Accept and Continue"].waitForExistence(timeout: 3), "Sign in → Consent")
        capture("Onboarding-consent-light")
        tap("Accept and Continue")
        XCTAssertTrue(app.staticTexts["Connectors"].waitForExistence(timeout: 3), "Consent → Connectors")
        capture("Onboarding-connectors-light")
        tap("Continue")
        XCTAssertTrue(app.staticTexts["When should Rem check in?"].waitForExistence(timeout: 3), "Connectors → Check-in")
        assertCheckInRowsFit()
        capture("Onboarding-checkin-light")
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5), "Check-in saves → Voice")
        tap("onboardingVoice.continue")
        let complete = app.staticTexts["onboarding.complete"]
        XCTAssertTrue(complete.waitForExistence(timeout: 3), "Voice → completion")
        XCTAssertEqual(complete.value as? String, "continue")
        capture("Onboarding-complete-light")
        tap("onboarding.done")
        XCTAssertTrue(app.buttons["openOnboardingSignIn"].waitForExistence(timeout: 3), "Done returns to the onboarding hub")
    }

    /// Connectors and Voice offer Skip, which moves the flow on like Continue. Check-in has no Skip
    /// (it asks for at least one time), so the flow passes it with Continue.
    func testSkipAdvancesFromConnectorsToCompletion() {
        openOnboardingStep("openOnboardingConnectors")
        tap("Skip")
        XCTAssertTrue(app.staticTexts["When should Rem check in?"].waitForExistence(timeout: 3), "Connectors Skip → Check-in")
        waitUntilGone(app.buttons["Skip"], "Check-in has no Skip")
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.skip"].waitForExistence(timeout: 5), "Check-in → Voice")
        tap("onboardingVoice.skip")
        let complete = app.staticTexts["onboarding.complete"]
        XCTAssertTrue(complete.waitForExistence(timeout: 3), "Voice Skip → completion")
        XCTAssertEqual(complete.value as? String, "skip")
    }

    /// Native Back walks the pushed steps in reverse.
    func testBackReturnsToPreviousStep() {
        openOnboardingStep("openOnboardingCheckIn")
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5))
        app.navigationBars.element(boundBy: 0).buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.staticTexts["When should Rem check in?"].waitForExistence(timeout: 3), "Back from Voice → Check-in")
    }

    /// Check-in: a switch adds a time, the native wheel changes one, and Continue saves before moving on.
    func testCheckInSwitchTimePickerAndSave() {
        openOnboardingStep("openOnboardingCheckIn")
        XCTAssertFalse(app.buttons["Edit 12:30 PM"].exists, "Midday starts off, without a time")
        app.switches["Midday"].tap()
        XCTAssertTrue(app.buttons["Edit 12:30 PM"].waitForExistence(timeout: 2), "Turning Midday on shows its time")
        tap("Edit 8:00 AM")
        let picker = app.datePickers.firstMatch
        XCTAssertTrue(picker.waitForExistence(timeout: 3), "The time opens the native picker")
        app.pickerWheels.element(boundBy: 0).adjust(toPickerWheelValue: "9")
        capture("Onboarding-checkin-picker-light")
        // Drag the sheet down from its top margin, clear of the wheels.
        let top = picker.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0)).withOffset(CGVector(dx: 0, dy: -10))
        top.press(forDuration: 0.1, thenDragTo: app.windows.firstMatch.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.98)))
        waitUntilGone(picker, "The picker sheet closes")
        XCTAssertTrue(app.buttons["Edit 9:00 AM"].waitForExistence(timeout: 2), "The chosen time shows in the row")
        assertCheckInRowsFit()
        capture("Onboarding-checkin-edited-light")
        // The one-second Saving state is not asserted here: its spinner keeps the app from idling, so
        // XCUITest may not observe it before Saved. The failure test pins the persistence states.
        app.buttons["Continue"].tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5), "Saved → Voice")
    }

    /// Check-in with the "Fails once" fixture: the first save fails with Try again, and retrying recovers.
    func testCheckInSaveFailureRecovers() {
        tap("openOnboarding")
        let failsOnce = app.segmentedControls["checkInSaveFixture"].buttons["Fails once"]
        failsOnce.tap()
        XCTAssertTrue(failsOnce.isSelected, "The save fixture switches to Fails once")
        tap("openOnboardingCheckIn")
        app.buttons["Continue"].tap()
        let retry = app.buttons["Try again"]
        XCTAssertTrue(retry.waitForExistence(timeout: 4), "A failed save offers Try again")
        XCTAssertTrue(app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "save your check-in times")).firstMatch.exists, "The failure toast explains it")
        assertCheckInRowsFit()
        capture("Onboarding-checkin-failure-light")
        retry.tap()
        XCTAssertTrue(app.buttons["onboardingVoice.continue"].waitForExistence(timeout: 5), "Retry saves → Voice")
    }

    /// Check-in at accessibility text size: every title stays on one line and every time stays on screen.
    func testCheckInRowsFitAtLargeText() {
        app.terminate()
        app.launchArguments = JourneyLaunch.arguments(["--developer", "--settings-light", "--settings-large-text"])
        app.launch()
        openOnboardingStep("openOnboardingCheckIn")
        XCTAssertTrue(app.staticTexts["When should Rem check in?"].waitForExistence(timeout: 3), "Check-in opens")
        assertCheckInRowsFit()
        revealCheckInControl(app.buttons["Edit 8:00 AM"])
        XCTAssertTrue(app.staticTexts["Morning"].isHittable, "The capture includes the Morning title")
        assertCheckInRowsFit()
        capture("Onboarding-checkin-large-text")
        let midday = app.switches["Midday"]
        revealCheckInControl(midday)
        midday.tap()
        let middayTime = app.buttons["Edit 12:30 PM"]
        XCTAssertTrue(middayTime.waitForExistence(timeout: 2), "Turning Midday on shows its time")
        assertCheckInRowsFit()
        revealCheckInControl(middayTime)
        capture("Onboarding-checkin-large-text-edited")
        for title in ["Morning", "Midday", "Evening"] {
            let control = app.switches[title]
            revealCheckInControl(control)
            XCTAssertTrue(control.isHittable, "The \(title) switch remains reachable")
        }
    }

    /// Check-in insets its scroll view by 24pt. Window-edge gestures fall outside that view, so drag
    /// within its blank leading margin and require the entire target inside the scroll viewport.
    private func revealCheckInControl(_ element: XCUIElement) {
        let scrollView = app.scrollViews.firstMatch
        XCTAssertTrue(scrollView.exists, "Check-in exposes its scrollable content")
        for _ in 0..<12 {
            let viewport = scrollView.frame
            if element.exists && !element.frame.isEmpty && viewport.contains(element.frame) && element.isHittable {
                return
            }
            let up = element.exists && element.frame.minY < viewport.minY
            let from = scrollView.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: up ? 0.35 : 0.75))
            let to = scrollView.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: up ? 0.75 : 0.35))
            from.press(forDuration: 0.05, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.3)
        }
        XCTAssertTrue(element.exists && !element.frame.isEmpty && scrollView.frame.contains(element.frame) && element.isHittable,
                      "Expected the complete Check-in control inside the scroll viewport: \(element.identifier)")
    }

    /// The Check-in row regression: each period title lays out on one line and each time is fully on
    /// screen. XCUITest exposes no line count, so a title is compared with a time value set in the same
    /// body style: one line is shorter than the time pill (one line + 8pt padding) × 1.4, two lines are
    /// not, for any line height of 19pt or more (default body is 22pt).
    private func assertCheckInRowsFit() {
        let times = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Edit "))
        XCTAssertTrue(times.firstMatch.waitForExistence(timeout: 2), "A Check-in time is shown")
        let reference = times.firstMatch.frame.height
        // iOS exposes a named SwiftUI Toggle wrapper and an anonymous native UISwitch child.
        // Count the named cadence controls; hidden fit candidates must not duplicate those labels.
        let cadenceSwitches = app.switches.matching(NSPredicate(format: "label IN %@",
            ["Morning", "Midday", "Evening"]))
        XCTAssertEqual(cadenceSwitches.count, 3, "Exactly three named cadence switches are exposed")
        for title in ["Morning", "Midday", "Evening"] {
            let text = app.staticTexts[title]
            XCTAssertTrue(text.exists, "The \(title) title is shown")
            XCTAssertGreaterThan(text.frame.width, 0)
            XCTAssertGreaterThan(text.frame.height, 0)
            XCTAssertTrue(text.frame.minX >= app.frame.minX && text.frame.maxX <= app.frame.maxX,
                          "The \(title) title is clipped horizontally")
            XCTAssertLessThan(text.frame.height, reference * 1.4,
                              "The \(title) title wraps: \(text.frame) against a one-line time of height \(reference)")
        }
        for time in times.allElementsBoundByIndex {
            XCTAssertTrue(time.frame.minX >= app.frame.minX && time.frame.maxX <= app.frame.maxX,
                          "\(time.label) is clipped horizontally: \(time.frame)")
        }
    }

    func testConsentLegalRowsOpenDocuments() {
        openOnboardingStep("openOnboardingConsent")
        tapButton(containing: "Terms of Service")
        let done = app.buttons["Done"]
        XCTAssertTrue(done.waitForExistence(timeout: 3), "Terms opens its document")
        capture("Onboarding-consent-terms-light")
        done.tap()
        waitUntilGone(done, "Done closes the document")
        XCTAssertTrue(app.buttons["Accept and Continue"].isHittable, "Closing returns to Consent")
    }

    func testConnectorRowTogglesLocalState() {
        openOnboardingStep("openOnboardingConnectors")
        // Gmail starts connected; Google Calendar and Slack offer Connect.
        let connect = app.buttons.matching(NSPredicate(format: "label == %@", "Connect"))
        XCTAssertTrue(connect.firstMatch.waitForExistence(timeout: 3))
        XCTAssertEqual(connect.count, 2)
        connect.firstMatch.tap()
        let oneLeft = expectation(for: NSPredicate(format: "count == 1"), evaluatedWith: connect)
        wait(for: [oneLeft], timeout: 3)
    }

    // MARK: Full-screen Chat, task reply and Inbox (Playground 7 candidate)
    //
    // Canonical compositions driven by the DS `ChatPlaygroundFixture` / `InboxPlaygroundFixture`.
    // Receipts appear only through the explicit fixture-host controls behind the header overflow.

    /// Picks a root fixture segment, revealing the picker first (root rows below the fold).
    private func chooseRootFixture(_ picker: String, _ segment: String) {
        let control = app.segmentedControls[picker]
        XCTAssertTrue(control.waitForExistence(timeout: 3), "Missing \(picker)")
        reveal(control)
        control.buttons[segment].tap()
    }

    private func openChatScreen(_ conversation: String = "Populated") {
        chooseRootFixture("chatFixturePicker", conversation)
        tap("openChatScreen")
        XCTAssertTrue(app.buttons["chat.header.back"].waitForExistence(timeout: 3), "The header owns Back")
    }

    private func sendFromChat(_ text: String) {
        let field = app.textViews["chat.composerField"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        focus(field)
        field.typeText(text)
        app.buttons["chat.composerSend"].tap()
    }

    private func hostControl(_ identifier: String) {
        app.buttons["chat.header.overflow"].tap()
        let control = app.buttons[identifier]
        XCTAssertTrue(control.waitForExistence(timeout: 3), "Missing fixture host control \(identifier)")
        control.tap()
    }

    /// WS1d: a left swipe is a temporary peek. After release every row returns to its resting position.
    /// The time itself is hidden from accessibility and spoken through the bubble instead.
    func testChatSwipeLeftPeeksTimesAndSnapsBack() {
        openChatScreen()
        let bubble = element("message.a1.bubble")
        XCTAssertTrue(bubble.waitForExistence(timeout: 3))
        XCTAssertEqual(bubble.value as? String, "Received at 10:21", "The time is spoken without the gesture")
        let rest = bubble.frame.minX
        bubble.swipeLeft()
        // Same check: back within 1pt of rest, within 3s. Each poll reads a fresh snapshot, which throws
        // (and is retried) when a lookup needs retries; the earlier block predicate read `bubble.frame`
        // directly and failed on a slow runner (run 38118821889: the rows were back at x 16 in the
        // failure dump, but the lookup never completed inside the window).
        let deadline = Date().addingTimeInterval(3)
        var settledX: CGFloat?
        repeat {
            // A reading that completes after the deadline does not count: the 3s window is unchanged.
            if let x = try? bubble.snapshot().frame.minX, Date() <= deadline, abs(x - rest) < 1 {
                settledX = x
                break
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        } while Date() < deadline
        XCTAssertNotNil(settledX, "Rows return to their resting position (minX \(rest)) after the swipe")
    }

    /// Jump to latest: hidden at the newest message, shown centred above the composer once the person has
    /// scrolled up, and a tap returns to the newest message and hides it again. The DS owns it.
    func testChatJumpToLatestAppearsWhenScrolledUpAndReturns() {
        openChatScreen("Long")
        // Developer covers the shell, whose Chat uses the same identifiers: only the hub's Chat is in the tree.
        XCTAssertEqual(app.textViews.matching(identifier: "chat.composerField").count, 1,
                       "Exactly one composer under Developer: the covered shell stays out of UI automation")
        let latest = element("message.a2.bubble")
        XCTAssertTrue(latest.waitForExistence(timeout: 3))
        XCTAssertTrue(latest.isHittable, "The conversation opens at the latest message")
        let jump = app.buttons["chat.jumpToLatest"]
        XCTAssertFalse(jump.exists, "Hidden at the latest message")

        // Scroll up with a slow drag in the leading gutter (a mid-screen swipe can start on a bubble).
        let content = element("chat.content")
        XCTAssertTrue(content.exists)
        for _ in 0..<4 where !jump.exists {
            let from = content.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: 0.25))
            let to = content.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: 0.85))
            from.press(forDuration: 0.05, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.3)
        }
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "Shown after scrolling up")
        XCTAssertEqual(jump.label, "Scroll to latest message")
        XCTAssertFalse(latest.isHittable, "Scrolled away from the latest message")
        let composer = app.textViews["chat.composerField"]
        XCTAssertLessThanOrEqual(jump.frame.maxY, composer.frame.minY, "Never covers the composer")
        XCTAssertEqual(jump.frame.midX, app.frame.midX, accuracy: 1, "Centred horizontally")
        capture("ChatScreen-jumpToLatest-light")

        jump.tap()
        waitUntilGone(jump, "Hidden again at the latest message")
        let back = expectation(for: NSPredicate(format: "isHittable == true"), evaluatedWith: latest)
        wait(for: [back], timeout: 3)
    }

    /// A vertical drag that starts on a transcript bubble scrolls the transcript and opens nothing: the
    /// `ChatTranscriptList` twin of the catalog's `assertDragOnBubbleScrolls` (b7a985c, a standalone bubble).
    /// Here the rows also carry the swipe-left time peek (`ChatTimestampReveal`) and sit in the full Chat
    /// composition. Run 38179871589's in-flight journey pressed 4pt beside an incoming bubble and four slow
    /// drags moved nothing; this isolates that start from the in-flight state (no send, no keyboard).
    func testChatTranscriptDragThatStartsOnABubbleScrolls() {
        openChatScreen("Long")
        XCTAssertTrue(element("message.a2.bubble").waitForExistence(timeout: 3))
        let bubble = element("message.a1.bubble")
        XCTAssertTrue(bubble.exists && bubble.isHittable, "An incoming bubble is on screen to start the drag on")
        let jump = app.buttons["chat.jumpToLatest"]
        XCTAssertFalse(jump.exists, "Opens at the latest message")
        let before = bubble.frame.minY
        let start = bubble.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 0, dy: 300)),
                    withVelocity: .slow, thenHoldForDuration: 0.3)
        XCTAssertGreaterThan(bubble.frame.minY - before, 20, "A vertical drag that starts on a bubble scrolls the transcript")
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "Scrolled up: jump to latest shows")
        XCTAssertFalse(app.buttons["message.a1.actions.report"].exists, "The drag opens no message actions")
    }

    /// The near-bubble strip: a vertical drag that starts 4pt outside an incoming bubble's leading edge (in the
    /// gutter, but within touch slop of the row) scrolls the transcript. Runs 38176095096 / 38179871589 pressed
    /// at (11.8, 213), 4.2pt left of `message.h16.bubble`, and nothing scrolled while the swipe-left time peek
    /// was a SwiftUI `DragGesture` on the rows; it is now a UIKit pan on the scroll view (`ChatTimestampRevealPan`).
    func testChatTranscriptDragBesideABubbleScrolls() {
        openChatScreen("Long")
        XCTAssertTrue(element("message.a2.bubble").waitForExistence(timeout: 3))
        let bubble = element("message.a1.bubble")
        XCTAssertTrue(bubble.exists && bubble.isHittable, "An incoming bubble is on screen to start the drag beside")
        let jump = app.buttons["chat.jumpToLatest"]
        XCTAssertFalse(jump.exists, "Opens at the latest message")
        let before = bubble.frame.minY
        let start = bubble.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0.5)).withOffset(CGVector(dx: -4, dy: 0))
        XCTAssertGreaterThan(start.screenPoint.x, 0, "The start point is on screen, in the leading gutter")
        start.press(forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 0, dy: 300)),
                    withVelocity: .slow, thenHoldForDuration: 0.3)
        XCTAssertGreaterThan(bubble.frame.minY - before, 20, "A vertical drag that starts beside a bubble scrolls the transcript")
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "Scrolled up: jump to latest shows")
        XCTAssertFalse(app.buttons["message.a1.actions.report"].exists, "The drag opens no message actions")
    }

    func testChatScreenDefaultHasOneHeaderAndLatestReceiptOnly() {
        openChatScreen()
        assertLatestClearsComposer("message.a2.bubble")
        XCTAssertTrue(app.buttons["chat.header.overflow"].exists, "The header owns overflow")
        XCTAssertEqual(app.navigationBars.count, 0, "No second navigation-title row above the header")
        XCTAssertEqual(element("chat.header.identity").exists, true)
        XCTAssertTrue(element("message.u2.receipt").waitForExistence(timeout: 3))
        XCTAssertTrue(element("message.u2.receipt").label.contains("Delivered"))
        XCTAssertFalse(element("message.u1.receipt").exists, "Older outgoing messages carry no receipt")
        XCTAssertFalse(app.buttons["chat.composerSend"].isEnabled, "Empty draft disables send")
        capture("ChatScreen-default-light")
    }

    func testChatScreenEmptyShowsStartersWithoutASecondFace() {
        openChatScreen("Empty")
        XCTAssertTrue(app.buttons["chat.starter.plan-day"].waitForExistence(timeout: 3))
        capture("ChatScreen-empty-light")
        app.buttons["chat.starter.plan-day"].tap()
        XCTAssertTrue(element("message.sent.1.bubble").waitForExistence(timeout: 3), "A starter sends")
        XCTAssertFalse(element("message.sent.1.receipt").exists, "No receipt without host acceptance")
    }

    func testChatScreenKeyboardOpenDocksComposerAboveKeyboard() {
        openChatScreen()
        let field = app.textViews["chat.composerField"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        focus(field)
        XCTAssertGreaterThan(app.keyboards.count, 0)
        let send = app.buttons["chat.composerSend"]
        XCTAssertLessThanOrEqual(send.frame.maxY, app.keyboards.firstMatch.frame.minY + 1, "Composer docks above the keyboard")
        XCTAssertTrue(app.buttons["chat.header.back"].isHittable, "Header stays visible with the keyboard open")
        capture("ChatScreen-keyboard-light")
    }

    /// Swipe down on the composer (`ChatKeyboardDismiss`, chat.md rule 11): a clear downward swipe that starts
    /// on the composer field hides the keyboard; after focusing again, an upward swipe leaves it up.
    func testChatComposerSwipeDownDismissesKeyboard() {
        openChatScreen()
        let field = app.textViews["chat.composerField"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        focus(field)
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 3), "Keyboard shown after focusing the composer")

        // A short press (well under `holdLimit`) then a quick downward drag that starts on the field.
        let start = field.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        start.press(forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 0, dy: 80)),
                    withVelocity: .fast, thenHoldForDuration: 0)
        waitUntilGone(app.keyboards.firstMatch, "A downward swipe on the composer hides the keyboard")
        XCTAssertFalse(NSPredicate(format: "hasKeyboardFocus == true").evaluate(with: field),
                       "The composer field is no longer focused")

        focus(field)
        XCTAssertTrue(app.keyboards.firstMatch.waitForExistence(timeout: 3), "Keyboard shown again")
        let again = field.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        again.press(forDuration: 0.05, thenDragTo: again.withOffset(CGVector(dx: 0, dy: -80)),
                    withVelocity: .fast, thenHoldForDuration: 0)
        RunLoop.current.run(until: Date().addingTimeInterval(1))
        XCTAssertGreaterThan(app.keyboards.count, 0, "An upward swipe leaves the keyboard up")
        XCTAssertTrue(NSPredicate(format: "hasKeyboardFocus == true").evaluate(with: field),
                      "The composer field stays focused after an upward swipe")
    }

    func testChatScreenSendStopAcceptAndRead() {
        openChatScreen()
        sendFromChat("Plan my afternoon")
        XCTAssertTrue(element("message.sent.1.bubble").waitForExistence(timeout: 3))
        XCTAssertFalse(element("message.sent.1.receipt").exists, "No receipt without host acceptance")
        let control = app.buttons["chat.composerSend"]
        XCTAssertEqual(control.label, "Stop", "In flight, the control is Stop")
        control.tap()
        XCTAssertEqual(app.buttons["chat.composerSend"].label, "Send", "Stop cancels back to Send")
        XCTAssertFalse(element("message.sent.1.receipt").exists, "Stop never fabricates a receipt")

        sendFromChat("Again")
        hostControl("chat.host.accept")
        let receipt = element("message.sent.2.receipt")
        XCTAssertTrue(receipt.waitForExistence(timeout: 3))
        XCTAssertTrue(receipt.label.contains("Delivered · 10:24"))
        hostControl("chat.host.read")
        XCTAssertTrue(element("message.sent.2.receipt").label.contains("Read · 10:24"))
        hostControl("chat.host.reply")
        XCTAssertEqual(app.buttons["chat.composerSend"].label, "Send")
        capture("ChatScreen-receipts-light")
    }

    func testChatScreenFailureRetry() {
        openChatScreen()
        sendFromChat("Share the agenda")
        hostControl("chat.host.fail")
        let failure = element("message.sent.1.failure")
        XCTAssertTrue(failure.waitForExistence(timeout: 3))
        XCTAssertTrue(element("message.sent.1.receipt").label.contains("Not delivered"))
        capture("ChatScreen-failed-light")
        failure.tap()
        let retry = app.buttons["Try again"]
        XCTAssertTrue(retry.waitForExistence(timeout: 3))
        retry.tap()
        XCTAssertTrue(element("chat.fixtureNote").waitForExistence(timeout: 3))
        XCTAssertFalse(element("message.sent.1.failure").exists)
        // The retried message's own turn completes: acceptance lands on it and the reply ends the turn.
        XCTAssertEqual(app.buttons["chat.composerSend"].label, "Stop", "Retry starts a turn")
        hostControl("chat.host.accept")
        let receipt = element("message.sent.1.receipt")
        XCTAssertTrue(receipt.waitForExistence(timeout: 3))
        XCTAssertTrue(receipt.label.contains("Delivered · 10:24"))
        hostControl("chat.host.reply")
        XCTAssertEqual(app.buttons["chat.composerSend"].label, "Send", "Reply complete ends the retried turn")
    }

    /// Long-press sheet `2603:19498`: Report only on assistant messages; Reply targets the reply accessory.
    func testChatScreenLongPressMessageActions() {
        openChatScreen()
        let assistant = element("message.a2.bubble")
        XCTAssertTrue(assistant.waitForExistence(timeout: 3))
        assistant.press(forDuration: 1.0)
        XCTAssertTrue(app.buttons["message.a2.actions.report"].waitForExistence(timeout: 3), "Report on assistant messages")
        XCTAssertEqual(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "message.a2.actions.reactions.")).count, 12)
        capture("ChatScreen-messageActions-light")
        app.buttons["message.a2.actions.reply"].tap()
        XCTAssertTrue(element("chat.replyContext.label").waitForExistence(timeout: 3), "Reply targets the reply accessory")

        let own = element("message.u2.bubble")
        own.press(forDuration: 1.0)
        XCTAssertTrue(app.buttons["message.u2.actions.copy"].waitForExistence(timeout: 3))
        XCTAssertFalse(app.buttons["message.u2.actions.report"].exists, "Never Report on the person's own message")
        XCTAssertFalse(app.buttons["message.u2.actions.markUnread"].exists)
        app.buttons["message.u2.actions.reactions.10"].tap()
        XCTAssertTrue(element("message.u2.reaction").waitForExistence(timeout: 3))
    }

    /// Photos and Files open the real system pickers, which UI tests cannot drive; this asserts the
    /// canonical options exist and no sample chip is injected. Picked-content mapping and attachment-only
    /// send are covered by `ChatComposerFixtureTests`.
    func testChatScreenAddToChatOffersSystemPickers() {
        openChatScreen()
        app.buttons["chat.composerAdd"].tap()
        XCTAssertTrue(app.buttons["chat.addToChat.photos"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.buttons["chat.addToChat.files"].exists)
        XCTAssertFalse(element("chat.attachment.photo.0").exists, "No sample photo chip is injected")
        tap("chat.addToChat.cloudBrowser")
        let removeBrowser = app.buttons["chat.removeAttachment.cloud-browser"]
        XCTAssertTrue(removeBrowser.waitForExistence(timeout: 3), "Cloud browser adds a removable chip and dismisses")
        removeBrowser.tap()
        waitUntilGone(removeBrowser, "Removing the chip clears it")
    }

    func testChatScreenBackExits() {
        openChatScreen()
        app.buttons["chat.header.back"].tap()
        XCTAssertTrue(app.buttons["openChatScreen"].waitForExistence(timeout: 3), "Back returns to the root")
    }

    /// Header identity → Agent activity (`2002:76914`): title, the Activity / Approvals segmented control,
    /// the same current state as the Chat header, the timeline, the labeled Approvals data gap, and Back
    /// to the unchanged conversation.
    func testChatHeaderOpensAgentActivityAndBackReturnsToChat() {
        openChatScreen()
        let header = element("chat.header.identity")
        XCTAssertTrue(header.label.contains("Connected"))
        header.tap()

        let bar = app.navigationBars["Agent activity"]
        XCTAssertTrue(bar.waitForExistence(timeout: 3), "The header identity opens Agent activity")
        let tabs = app.segmentedControls["agentActivity.tabs"]
        XCTAssertTrue(tabs.waitForExistence(timeout: 3))
        XCTAssertTrue(tabs.buttons["Activity"].isSelected, "Activity is selected first")
        XCTAssertTrue(tabs.buttons["Approvals"].exists)
        XCTAssertTrue(element("agentActivity.identity").label.contains("Connected"), "Same current state as the Chat header")
        XCTAssertTrue(element("agentActivity.event.reminder").exists)
        XCTAssertFalse(app.buttons["agentActivity.event.reminder"].exists, "Rows have no verified destination")
        capture("AgentActivity-light")

        tabs.buttons["Approvals"].tap()
        XCTAssertTrue(element("agentActivity.approvals.gap").waitForExistence(timeout: 3), "Approvals is a labeled data gap")
        XCTAssertFalse(element("agentActivity.event.reminder").exists, "No timeline rows posing as approvals")
        XCTAssertTrue(element("agentActivity.identity").label.contains("Connected"), "Switching sections keeps the current state")
        capture("AgentActivity-approvals-light")

        bar.buttons["Chat"].tap()
        XCTAssertTrue(app.buttons["chat.header.back"].waitForExistence(timeout: 3), "Back returns to Chat")
        XCTAssertTrue(element("message.u2.receipt").exists, "The conversation is unchanged")
        XCTAssertFalse(element("chat.fixtureNote").exists, "Activity opens a screen, not a note")
    }

    func testInboxStatesRouteIntoTaskReplyAndDismissAccessory() {
        chooseRootFixture("inboxFixturePicker", "Items")
        tap("openInbox")
        // The row is one button; its label folds in the title and the host-reported status.
        let venue = app.buttons["inbox.item.venue-booking"]
        XCTAssertTrue(venue.waitForExistence(timeout: 3))
        XCTAssertTrue(venue.label.contains("Needs approval"))
        XCTAssertTrue(app.buttons["inbox.item.calendar-holds"].label.contains("Status unknown"))
        let plain = app.buttons["inbox.item.plan-next-step"].label
        for label in ["Working", "Needs", "Status unknown", "Done", "Loading"] {
            XCTAssertFalse(plain.contains(label), "No state, no status")
        }
        capture("Inbox-states-light")

        app.buttons["inbox.item.venue-booking"].tap()
        let context = element("chat.replyContext.label")
        XCTAssertTrue(context.waitForExistence(timeout: 3))
        XCTAssertTrue(context.label.contains("Approve the venue booking"))
        XCTAssertTrue(element("chat.header.identity").label.contains("Needs approval"), "Task chat shows the same state")
        XCTAssertEqual(app.textViews["chat.composerField"].exists, true, "The same composer")
        capture("ChatScreen-taskReply-light")
        app.buttons["chat.replyContext.dismiss"].tap()
        waitUntilGone(element("chat.replyContext.label"), "Dismiss clears the reply target")
        XCTAssertTrue(app.textViews["chat.composerField"].exists, "The composer stays")
        app.buttons["chat.header.back"].tap()
        XCTAssertTrue(app.buttons["inbox.item.venue-booking"].waitForExistence(timeout: 3), "Back returns to the Inbox")
    }

    func testInboxEmpty() {
        chooseRootFixture("inboxFixturePicker", "Empty")
        tap("openInbox")
        XCTAssertTrue(element("inbox.empty").waitForExistence(timeout: 3))
    }

    // MARK: App shell (Build 9; `docs/playground/route-map.md`)
    //
    // setUp launches with `--developer`, straight into the Developer hub. These journeys close it and
    // drive the shell itself: Chat full screen, the sidebar behind the header, New agent, Settings from
    // the account pill. Twin of the Compose `shell…` journeys.

    /// Developer → Close: back on the shell's full-screen conversation (the root).
    private func returnToShell() {
        tap("developer.close")
        XCTAssertTrue(app.buttons["chat.header.menu"].waitForExistence(timeout: 5), "Close returns to the shell")
    }

    private func openSidebar() {
        app.buttons["chat.header.menu"].tap()
        XCTAssertTrue(element("sidebar.agent.main").waitForExistence(timeout: 3), "The header button opens the sidebar")
        XCTAssertTrue(element("sidebar.close").waitForExistence(timeout: 3))
    }

    /// Shell presentation changes animate and the shell's tree is large, so allow a snapshot-heavy 10s.
    private func waitGone(_ element: XCUIElement, _ message: String) {
        let gone = XCTNSPredicateExpectation(predicate: NSPredicate(format: "exists == false"), object: element)
        XCTAssertEqual(XCTWaiter().wait(for: [gone], timeout: 10), .completed, message)
    }

    /// sidebar-new-agent.md, push container accessibility: on iOS the closed sidebar is not rendered, so its
    /// controls are absent from the tree (not merely hidden behind the chat card). Waits out the close slide.
    private func assertClosedSidebarNotInTree(_ context: String) {
        waitGone(element("sidebar.account"), "\(context): the closed sidebar's account pill is not in the tree")
        waitGone(element("sidebar.newAgent"), "\(context): the closed sidebar's New agent is not in the tree")
    }

    private func chooseSidebarRow(_ identifier: String) {
        let row = element(identifier)
        XCTAssertTrue(row.waitForExistence(timeout: 3), "Missing \(identifier)")
        row.tap()
        waitGone(element("sidebar.close"), "Choosing an agent closes the sidebar")
    }

    private func assertChatShows(_ agent: String) {
        let identity = element("chat.header.identity")
        XCTAssertTrue(identity.waitForExistence(timeout: 5))
        XCTAssertTrue(identity.label.hasPrefix(agent), "Expected \(agent)'s conversation, got \(identity.label)")
    }

    /// chat.md rule 12: once layout settles, the latest message sits wholly above the composer dock (its
    /// accessibility frame ends above the composer field's). Polls up to 3s for the inset to settle.
    private func assertLatestClearsComposer(_ identifier: String) {
        let latest = element(identifier)
        let field = app.textViews["chat.composerField"]
        XCTAssertTrue(latest.waitForExistence(timeout: 3) && field.waitForExistence(timeout: 3))
        let deadline = Date().addingTimeInterval(3)
        while latest.frame.maxY > field.frame.minY && Date() < deadline {
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }
        XCTAssertLessThanOrEqual(latest.frame.maxY, field.frame.minY,
                                 "Latest message (\(latest.frame)) must clear the composer (\(field.frame)) at rest")
    }

    /// Root: Chat full screen with the sidebar behind the header button; choose an agent; the chat card
    /// closes the sidebar and keeps the selection.
    func testShellSidebarSelectsAnAgentAndClosesIt() {
        returnToShell()
        assertChatShows("Rem")
        assertLatestClearsComposer("message.a2.bubble")
        XCTAssertFalse(app.buttons["chat.header.back"].exists, "The root conversation has no Back")
        XCTAssertFalse(app.buttons["chat.header.overflow"].exists, "Fixture host controls live under Developer")
        XCTAssertEqual(app.buttons["chat.header.menu"].label, "Open sidebar")
        capture("Shell-chat-light")
        assertClosedSidebarNotInTree("Before the sidebar first opens")

        openSidebar()
        XCTAssertTrue(element("sidebar.section.side").exists)
        XCTAssertTrue(element("sidebar.newAgent").exists)
        XCTAssertTrue(element("sidebar.account").exists)
        capture("Shell-sidebar-light")
        chooseSidebarRow("sidebar.agent.health")
        assertClosedSidebarNotInTree("After choosing an agent closes the sidebar")
        assertChatShows("Health")
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label CONTAINS %@", "Start a conversation with Health")).firstMatch.exists)

        openSidebar()
        XCTAssertTrue(element("sidebar.newAgent").exists, "The reopened sidebar is rendered again")
        XCTAssertTrue(element("sidebar.account").exists, "The reopened sidebar is rendered again")
        element("sidebar.close").tap()
        waitGone(element("sidebar.close"), "The chat card closes the sidebar")
        assertClosedSidebarNotInTree("After the chat card closes the sidebar")
        assertChatShows("Health")
        openSidebar()
        chooseSidebarRow("sidebar.agent.main")
        assertChatShows("Rem")
    }

    /// New agent: Close cancels without change; Create adds the agent and opens its chat.
    func testShellNewAgentCancelsAndCreates() {
        returnToShell()
        openSidebar()
        element("sidebar.newAgent").tap()
        let name = app.textFields["newAgent.name"]
        XCTAssertTrue(name.waitForExistence(timeout: 3), "New agent opens its sheet")
        XCTAssertFalse(app.buttons["newAgent.create"].isEnabled, "Create waits for a name")
        capture("Shell-newAgent-light")
        app.buttons["newAgent.cancel"].tap()
        waitGone(name, "Close dismisses the sheet")
        assertChatShows("Rem")
        openSidebar()
        XCTAssertFalse(element("sidebar.agent.new.1").exists, "Cancel creates nothing")

        element("sidebar.newAgent").tap()
        XCTAssertTrue(name.waitForExistence(timeout: 3))
        focus(name)
        name.typeText("Garden")
        app.buttons["newAgent.color.green"].tap()
        app.buttons["newAgent.create"].tap()
        waitGone(name, "Create dismisses the sheet")
        assertChatShows("Garden")
        openSidebar()
        XCTAssertTrue(element("sidebar.agent.new.1").exists, "The new agent is listed")
    }

    /// Settings from the account pill, into a destination and back to the same agent and conversation.
    func testShellSettingsFromAccountReturnsToTheSelectedChat() {
        returnToShell()
        openSidebar()
        chooseSidebarRow("sidebar.agent.money")
        assertChatShows("Money")
        openSidebar()
        let account = element("sidebar.account")
        XCTAssertTrue(account.label.contains("Avery Diaz"), "The account pill names the Settings profile")
        account.tap()
        let settings = app.navigationBars["Settings"]
        XCTAssertTrue(settings.waitForExistence(timeout: 5), "The account pill opens Settings")
        XCTAssertTrue(app.buttons["settings.developer"].exists)
        capture("Shell-settings-light")

        tap("openAgent")
        XCTAssertTrue(app.navigationBars["Agent settings"].waitForExistence(timeout: 5))
        let devices = app.buttons["agentDestination.pairedDevices"]
        XCTAssertTrue(devices.waitForExistence(timeout: 5))
        reveal(devices)
        devices.tap()
        XCTAssertTrue(app.navigationBars["Paired devices"].waitForExistence(timeout: 5), "A destination is pushed")
        app.navigationBars["Paired devices"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.navigationBars["Agent settings"].waitForExistence(timeout: 5), "Back pops one level")
        app.navigationBars["Agent settings"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(settings.waitForExistence(timeout: 5))
        app.buttons["settings.done"].tap()
        waitGone(settings, "Done dismisses Settings")
        assertChatShows("Money")

        // A swipe down dismisses the sheet too.
        openSidebar()
        element("sidebar.account").tap()
        XCTAssertTrue(settings.waitForExistence(timeout: 5))
        settings.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            .press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.95)))
        waitGone(settings, "A swipe down dismisses Settings")
        assertChatShows("Money")
    }

    /// Chat in the shell: Add to Chat and message actions are sheets that dismiss; Activity is pushed and pops.
    func testShellChatSheetsDismissAndActivityPops() {
        returnToShell()
        app.buttons["chat.composerAdd"].tap()
        let done = app.buttons["chat.addToChat.done"]
        XCTAssertTrue(done.waitForExistence(timeout: 3), "Add opens the Add to Chat sheet")
        done.tap()
        waitGone(done, "Done dismisses Add to Chat")

        let assistant = element("message.a2.bubble")
        XCTAssertTrue(assistant.waitForExistence(timeout: 3))
        assistant.press(forDuration: 1.0)
        let copy = app.buttons["message.a2.actions.copy"]
        XCTAssertTrue(copy.waitForExistence(timeout: 3))
        copy.tap()
        waitGone(copy, "An action dismisses the sheet")
        assertChatShows("Rem")

        element("chat.header.identity").tap()
        let bar = app.navigationBars["Agent activity"]
        XCTAssertTrue(bar.waitForExistence(timeout: 3), "The header identity pushes Agent activity")
        bar.buttons["Chat"].tap()
        XCTAssertTrue(app.buttons["chat.header.menu"].waitForExistence(timeout: 3), "Back pops to the root conversation")
        assertChatShows("Rem")
    }

    /// The scroll position survives Settings: scrolled up in the Long conversation, jump to latest stays.
    func testShellKeepsScrollPositionAcrossSettings() {
        chooseRootFixture("chatFixturePicker", "Long")
        returnToShell()
        let latest = element("message.a2.bubble")
        XCTAssertTrue(latest.waitForExistence(timeout: 3))
        let jump = app.buttons["chat.jumpToLatest"]
        let content = element("chat.content")
        for _ in 0..<4 where !jump.exists {
            let from = content.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: 0.25))
            let to = content.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: 0.85))
            from.press(forDuration: 0.05, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.3)
        }
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "Scrolled up")
        let marker = element("message.h8.bubble")
        let before = marker.frame.minY

        openSidebar()
        element("sidebar.account").tap()
        let settings = app.navigationBars["Settings"]
        XCTAssertTrue(settings.waitForExistence(timeout: 5))
        app.buttons["settings.done"].tap()
        waitGone(settings, "Done dismisses Settings")
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "Still scrolled up after Settings")
        XCTAssertEqual(marker.frame.minY, before, accuracy: 1, "The conversation keeps its scroll position")
    }

    /// Developer stays separate: no catalog or fixture control in the shell's chat or sidebar; Developer
    /// opens from Settings only, and Close returns to the same conversation.
    func testShellDeveloperEntryStaysSeparate() {
        XCTAssertTrue(app.navigationBars["Developer"].waitForExistence(timeout: 3), "--developer opens the hub")
        XCTAssertTrue(app.staticTexts["playground.build"].exists)
        capture("Developer-hub-light")
        returnToShell()
        for id in ["openComponents", "openSettings", "chatFixturePicker", "playground.build"] {
            XCTAssertFalse(element(id).exists, "\(id) stays in the Developer hub")
        }
        openSidebar()
        for id in ["openComponents", "chatFixturePicker", "settings.developer"] {
            XCTAssertFalse(element(id).exists, "\(id) is not in the sidebar")
        }
        chooseSidebarRow("sidebar.agent.travel")
        openSidebar()
        element("sidebar.account").tap()
        let developer = app.buttons["settings.developer"]
        XCTAssertTrue(developer.waitForExistence(timeout: 5))
        XCTAssertEqual(developer.label, "Developer")
        developer.tap()
        XCTAssertTrue(app.buttons["openComponents"].waitForExistence(timeout: 5), "Developer opens the hub")
        XCTAssertFalse(app.navigationBars["Settings"].exists, "Settings closed behind the hub")
        returnToShell()
        assertChatShows("Travel planning: Lisbon in May")
    }

    /// Leaving the chat ends editing: Settings opens with the keyboard hidden, and the draft is still there.
    func testShellRouteChangeHidesKeyboardAndKeepsDraft() {
        returnToShell()
        let field = app.textViews["chat.composerField"]
        XCTAssertTrue(field.waitForExistence(timeout: 3))
        focus(field)
        field.typeText("Keep this draft")
        XCTAssertGreaterThan(app.keyboards.count, 0, "The keyboard is up while typing")
        openSidebar()
        waitGone(app.keyboards.firstMatch, "Opening the sidebar hides the keyboard")
        element("sidebar.account").tap()
        let settings = app.navigationBars["Settings"]
        XCTAssertTrue(settings.waitForExistence(timeout: 5))
        XCTAssertEqual(app.keyboards.count, 0, "Settings opens with the keyboard hidden")
        capture("Shell-settings-noKeyboard-light")
        app.buttons["settings.done"].tap()
        waitGone(settings, "Done dismisses Settings")
        XCTAssertEqual(app.keyboards.count, 0)
        XCTAssertEqual(field.value as? String, "Keep this draft", "The draft is kept")
    }

    /// Message bubbles wholly inside the transcript viewport, by identifier.
    private func visibleBubbles() -> Set<String> {
        let viewport = element("chat.content").frame
        let bubbles = app.descendants(matching: .any)
            .matching(NSPredicate(format: "identifier BEGINSWITH 'message.' AND identifier ENDSWITH '.bubble'"))
            .allElementsBoundByIndex
        return Set(bubbles.filter { $0.frame.minY >= viewport.minY && $0.frame.maxY <= viewport.maxY }.map(\.identifier))
    }

    /// Swipe down on the composer to end editing (chat.md rule 11). The drag starts in the composer pill's top
    /// padding, just above the field: still the composer (the pill, field included), but clear of the text
    /// view's own caret and selection gestures over typed text, which the earlier field-centre press met in
    /// run 38177680171 (the keyboard stayed up). A short press, then a quick 80pt drag down.
    private func swipeDownOnComposer(_ field: XCUIElement) {
        let start = field.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0)).withOffset(CGVector(dx: 0, dy: -6))
        start.press(forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 0, dy: 80)),
                    withVelocity: .fast, thenHoldForDuration: 0)
    }

    /// Up to four slow drags toward older messages in the transcript's leading gutter (a mid-screen drag can
    /// start on a bubble), stopping once `jump` (jump to latest) shows. Each drag starts at `clearGutterStart`,
    /// so the press is clear of the rows as well as mid-screen; a drag that starts on a bubble is its own
    /// journey (`testChatTranscriptDragThatStartsOnABubbleScrolls`).
    private func dragTranscriptUp(until jump: XCUIElement) {
        let content = element("chat.content")
        XCTAssertTrue(content.exists)
        for _ in 0..<4 where !jump.exists {
            let dy = clearGutterStart(in: content)
            let from = content.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: dy))
            let to = content.coordinate(withNormalizedOffset: CGVector(dx: 0.03, dy: dy + 0.6))
            from.press(forDuration: 0.05, thenDragTo: to, withVelocity: .slow, thenHoldForDuration: 0.3)
        }
    }

    /// The first normalized height from 0.25 to 0.35 at which the leading-gutter press point (x 0.03, about 12pt
    /// on a 393pt screen) is at least `margin` from every transcript text: bubbles, meta lines and timestamps.
    /// Incoming bubbles start at x 16, so a fixed 0.25 can land 4pt beside one: run 38179871589 pressed at
    /// (11.8, 213) beside `message.h16.bubble` (16, 206.7, 215 x 66), and four drags moved nothing. Falls back
    /// to 0.25 when no height in the range is clear or the snapshot fails.
    private func clearGutterStart(in content: XCUIElement) -> CGFloat {
        guard let snapshot = try? content.snapshot() else { return 0.25 }
        var texts: [CGRect] = []
        func collect(_ node: XCUIElementSnapshot) {
            if node.elementType == .staticText { texts.append(node.frame) }
            for child in node.children { collect(child) }
        }
        collect(snapshot)
        let frame = snapshot.frame
        let margin: CGFloat = 12
        for step in 0...10 {
            let dy = 0.25 + CGFloat(step) * 0.01
            let point = CGPoint(x: frame.minX + frame.width * 0.03, y: frame.minY + frame.height * dy)
            if !texts.contains(where: { $0.insetBy(dx: -margin, dy: -margin).contains(point) }) { return dy }
        }
        return 0.25
    }

    /// The shell's simulated reply has landed (accepted after 0.6s, complete 1.5s later) and the turn has ended.
    private func waitForShellReply(_ identifier: String) {
        XCTAssertTrue(element(identifier).waitForExistence(timeout: 10), "The shell's simulated reply completes")
        XCTAssertEqual(app.buttons["chat.composerSend"].label, "Send", "The reply ends the turn")
    }

    private func openDeveloperFromShell() {
        openSidebar()
        element("sidebar.account").tap()
        let developer = app.buttons["settings.developer"]
        XCTAssertTrue(developer.waitForExistence(timeout: 5))
        developer.tap()
        XCTAssertTrue(app.buttons["openComponents"].waitForExistence(timeout: 10), "Developer opens the hub")
        // The covered shell leaves the tree once the hub is up (route-map "Covered surfaces").
        waitGone(app.textViews["chat.composerField"], "The covered shell's composer is not in the tree under Developer")
        waitGone(app.buttons["chat.header.menu"], "The covered shell's header is not in the tree under Developer")
    }

    /// Developer round trip keeps the work: a sent message, a draft and the scroll position survive
    /// Settings → Developer → Close when no fixture changes; choosing a different Chat fixture starts afresh.
    func testShellDeveloperRoundTripKeepsConversationDraftAndScroll() {
        chooseRootFixture("chatFixturePicker", "Long")
        returnToShell()
        sendFromChat("Round trip message")
        XCTAssertTrue(element("message.sent.1.bubble").waitForExistence(timeout: 5))
        // The shell simulates the reply (route-map "Simulated reply"); let it land first, so the transcript is
        // settled before scrolling and the round trip compares the same content.
        waitForShellReply("message.reply.2.bubble")
        let field = app.textViews["chat.composerField"]
        focus(field)
        field.typeText("Keep this draft")
        // Scroll up with the keyboard down (as on Android). With it up, the drag toward older messages also
        // dismisses it, the viewport grows by the keyboard height and the remaining distance can fall within
        // the near-latest threshold; the visible set would also be taken with a different viewport than the
        // one the round trip returns to (route changes end editing). The composer swipe-down is rule 11.
        swipeDownOnComposer(field)
        waitUntilGone(app.keyboards.firstMatch, "The keyboard is down before scrolling up")
        let jump = app.buttons["chat.jumpToLatest"]
        dragTranscriptUp(until: jump)
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "Scrolled up")
        let before = visibleBubbles()
        XCTAssertFalse(before.isEmpty)
        XCTAssertFalse(before.contains("message.sent.1.bubble"), "Scrolled away from the latest message")
        XCTAssertFalse(before.contains("message.reply.2.bubble"), "Scrolled away from the latest message")

        openDeveloperFromShell()
        returnToShell()
        XCTAssertTrue(element("message.sent.1.bubble").exists, "The sent message is kept")
        XCTAssertTrue(element("message.reply.2.bubble").exists, "The reply is kept")
        XCTAssertEqual(field.value as? String, "Keep this draft", "The draft is kept")
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "Still scrolled up")
        XCTAssertEqual(visibleBubbles(), before, "The same messages are visible after the round trip")

        // An explicit fixture change in Developer starts that conversation afresh.
        openDeveloperFromShell()
        chooseRootFixture("chatFixturePicker", "Populated")
        returnToShell()
        XCTAssertFalse(element("message.sent.1.bubble").exists, "A new fixture starts afresh")
        XCTAssertNotEqual(field.value as? String, "Keep this draft")

        // A -> B -> A is fresh too (route-map Retention): work on Populated, Developer -> Long -> Close, then
        // Developer -> Populated -> Close. Only the live conversation key is kept.
        sendFromChat("Populated message")
        XCTAssertTrue(element("message.sent.1.bubble").waitForExistence(timeout: 5))
        focus(field)
        field.typeText("Populated draft")
        openDeveloperFromShell()
        chooseRootFixture("chatFixturePicker", "Long")
        returnToShell()
        XCTAssertFalse(element("message.sent.1.bubble").exists, "Long starts afresh")
        XCTAssertNotEqual(field.value as? String, "Populated draft")
        openDeveloperFromShell()
        chooseRootFixture("chatFixturePicker", "Populated")
        returnToShell()
        XCTAssertFalse(element("message.sent.1.bubble").exists, "Choosing Populated again starts afresh")
        XCTAssertNotEqual(field.value as? String, "Populated draft", "The earlier draft is not restored")

        // Switching agents replaces the conversation as well: Rem -> Health -> Rem starts Rem afresh.
        sendFromChat("Rem message")
        XCTAssertTrue(element("message.sent.1.bubble").waitForExistence(timeout: 5))
        focus(field)
        field.typeText("Rem draft")
        openSidebar()
        chooseSidebarRow("sidebar.agent.health")
        assertChatShows("Health")
        openSidebar()
        chooseSidebarRow("sidebar.agent.main")
        assertChatShows("Rem")
        XCTAssertFalse(element("message.sent.1.bubble").exists, "Rem starts afresh after an agent switch")
        XCTAssertNotEqual(field.value as? String, "Rem draft", "Rem's draft is not restored")
    }

    /// Regression: scrolling while a reply is in flight. Run 38176095096 sent in the shell and four slow drags
    /// did not move the transcript while Stop showed. `--hold-replies` keeps the simulated reply in flight (motion
    /// is off, as in every journey); the drags must move the transcript: jump to latest appears and the message
    /// just sent leaves view, while Stop is still shown. Twin of `shellScrollsWhileAReplyIsInFlight`.
    func testShellScrollsWhileAReplyIsInFlight() {
        app.terminate()
        app.launchArguments = JourneyLaunch.arguments(["--developer", "--settings-light", "--hold-replies"])
        app.launch()
        chooseRootFixture("chatFixturePicker", "Long")
        returnToShell()
        sendFromChat("Scroll while you reply")
        let sent = element("message.sent.1.bubble")
        XCTAssertTrue(sent.waitForExistence(timeout: 5))
        let control = app.buttons["chat.composerSend"]
        XCTAssertEqual(control.label, "Stop", "The reply is in flight")
        XCTAssertTrue(element("chat.header.identity").label.contains("Working on your request"))

        let field = app.textViews["chat.composerField"]
        swipeDownOnComposer(field)
        waitUntilGone(app.keyboards.firstMatch, "The keyboard is down before scrolling up")
        let jump = app.buttons["chat.jumpToLatest"]
        waitUntilGone(jump, "The transcript is at the message just sent")
        XCTAssertTrue(visibleBubbles().contains("message.sent.1.bubble"), "The message just sent is in view")

        dragTranscriptUp(until: jump)
        XCTAssertTrue(jump.waitForExistence(timeout: 3), "The transcript scrolls while the reply is in flight")
        XCTAssertFalse(sent.isHittable, "The message just sent has left view")
        XCTAssertFalse(visibleBubbles().contains("message.sent.1.bubble"), "The message just sent has left view")
        XCTAssertEqual(app.buttons["chat.composerSend"].label, "Stop", "The reply is still in flight")
        XCTAssertFalse(element("message.reply.2.bubble").exists, "--hold-replies holds the reply")
    }

    // MARK: Gesture probe (experiment branch only; never merged)
    // Same start and assertions as testChatTranscriptDragThatStartsOnABubbleScrolls; only how the drag is
    // synthesized changes. Each variant prints one PROBE line so one run compares them all.

    private func probeDragOnBubble(_ name: String, _ drag: (XCUICoordinate, XCUIElement) -> Void) {
        openChatScreen("Long")
        XCTAssertTrue(element("message.a2.bubble").waitForExistence(timeout: 3))
        let bubble = element("message.a1.bubble")
        XCTAssertTrue(bubble.exists && bubble.isHittable, "An incoming bubble is on screen to start the drag on")
        let before = bubble.frame.minY
        drag(bubble.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)), bubble)
        RunLoop.current.run(until: Date().addingTimeInterval(0.5))
        let moved = bubble.exists ? bubble.frame.minY - before : .infinity
        let jump = app.buttons["chat.jumpToLatest"].waitForExistence(timeout: 3)
        let actions = app.buttons["message.a1.actions.report"].exists
        print("PROBE \(name) moved=\(moved) jump=\(jump) actions=\(actions)")
        XCTAssertGreaterThan(moved, 20, "\(name): the drag scrolls the transcript")
        XCTAssertTrue(jump, "\(name): jump to latest shows")
        XCTAssertFalse(actions, "\(name): the drag opens no message actions")
    }

    private func down(_ c: XCUICoordinate, _ dy: CGFloat = 300) -> XCUICoordinate { c.withOffset(CGVector(dx: 0, dy: dy)) }

    func testProbeA_SlowThenHold() {  // the failing journey's drag, as a control
        probeDragOnBubble("A slow+hold") { c, _ in c.press(forDuration: 0.05, thenDragTo: down(c), withVelocity: .slow, thenHoldForDuration: 0.3) }
    }
    func testProbeB_SlowNoHold() {
        probeDragOnBubble("B slow") { c, _ in c.press(forDuration: 0.05, thenDragTo: down(c), withVelocity: .slow, thenHoldForDuration: 0) }
    }
    func testProbeC_DefaultVelocity() {
        probeDragOnBubble("C default") { c, _ in c.press(forDuration: 0.05, thenDragTo: down(c)) }
    }
    func testProbeD_Fast() {
        probeDragOnBubble("D fast") { c, _ in c.press(forDuration: 0.05, thenDragTo: down(c), withVelocity: .fast, thenHoldForDuration: 0) }
    }
    func testProbeE_Flick1500() {
        probeDragOnBubble("E 1500pt/s") { c, _ in c.press(forDuration: 0.01, thenDragTo: down(c), withVelocity: XCUIGestureVelocity(1500), thenHoldForDuration: 0) }
    }
    func testProbeF_ElementSwipeDown() {
        probeDragOnBubble("F bubble.swipeDown") { _, bubble in bubble.swipeDown() }
    }
    func testProbeG_ElementSwipeDownFast() {
        probeDragOnBubble("G bubble.swipeDown fast") { _, bubble in bubble.swipeDown(velocity: .fast) }
    }
    func testProbeH_LongerHoldBeforeDrag() {  // a 0.3s press first: does the long press claim the touch?
        probeDragOnBubble("H press0.3+fast") { c, _ in c.press(forDuration: 0.3, thenDragTo: down(c), withVelocity: .fast, thenHoldForDuration: 0) }
    }
}
