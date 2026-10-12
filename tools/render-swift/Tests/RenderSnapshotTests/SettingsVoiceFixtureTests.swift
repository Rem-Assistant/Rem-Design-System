import XCTest
@testable import RemDesignSystem

/// Pure local state tests. Native Back, control layout, and accessibility need runtime evidence.
final class SettingsVoiceFixtureTests: XCTestCase {
    func testDefaultsAndFiveCanonicalChoices() {
        let fixture = VoiceSettingsFixture()
        XCTAssertEqual(fixture.selected, .aria)
        XCTAssertEqual(fixture.conversationEntry, .voiceSession)
        XCTAssertEqual([fixture.speed, fixture.consistency, fixture.likeness], [0.50, 0.75, 0.50])
        XCTAssertNil(fixture.previewing)
        XCTAssertEqual(VoiceChoice.allCases.map(\.label),
                       ["Aria (Warm)", "Sol (Bright)", "Rowan (Calm)", "Juniper (Expressive)", "Vale (Neutral)"])
    }

    func testPreviewAndSelectionAreIndependent() {
        var fixture = VoiceSettingsFixture()
        fixture.togglePreview(.aria)
        fixture.select(.sol)
        XCTAssertEqual(fixture.selected, .sol)
        XCTAssertEqual(fixture.previewing, .aria)
        fixture.togglePreview(.sol)
        XCTAssertEqual(fixture.previewing, .sol)
        fixture.togglePreview(.sol)
        XCTAssertNil(fixture.previewing)
        XCTAssertEqual(fixture.selected, .sol)
    }

    func testLeavingPreviewPreservesPreferencesAndAdjustedSourceValues() {
        var fixture = VoiceSettingsFixture()
        fixture.select(.sol)
        fixture.conversationEntry = .chat
        fixture.speed = 0.75
        fixture.consistency = 0.50
        fixture.likeness = 0.75
        fixture.togglePreview(.rowan)
        fixture.stopPreview()
        XCTAssertNil(fixture.previewing)
        XCTAssertEqual(fixture.selected, .sol)
        XCTAssertEqual(fixture.conversationEntry, .chat)
        XCTAssertEqual([fixture.speed, fixture.consistency, fixture.likeness], [0.75, 0.50, 0.75])
    }
}
