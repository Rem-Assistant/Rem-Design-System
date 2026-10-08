import XCTest
@testable import RemDesignSystem

// Pure fixture-logic tests for the Memory and Models destinations. These are not UIKit-guarded, so
// `swift test` runs them on this runner (no simulator). They prove the scope/cancel/save boundaries
// the source contract calls out: independent toggles, the deterministic Memory send boundary, the
// five-provider picker, and the Models save flow that records only a dummy flag — never the key draft.
final class SettingsMemoryModelsFixtureTests: XCTestCase {

    // MARK: Memory

    func testMemoryToggleDefaultsMatchSource() {
        let fixture = MemoryFixture()
        XCTAssertTrue(fixture.isOn(.searchAndReference))
        XCTAssertTrue(fixture.isOn(.generateMemory))
        XCTAssertFalse(fixture.isOn(.sensitiveTopics))
    }

    func testMemoryTogglesAreIndependent() {
        var fixture = MemoryFixture()
        fixture.set(.sensitiveTopics, true)
        // Flipping one control leaves the others exactly as they were.
        XCTAssertTrue(fixture.isOn(.sensitiveTopics))
        XCTAssertTrue(fixture.isOn(.searchAndReference))
        XCTAssertTrue(fixture.isOn(.generateMemory))

        fixture.set(.searchAndReference, false)
        XCTAssertFalse(fixture.isOn(.searchAndReference))
        XCTAssertTrue(fixture.isOn(.generateMemory))
        XCTAssertTrue(fixture.isOn(.sensitiveTopics))
    }

    func testMemorySummaryCopyIsExactAndComplete() {
        XCTAssertEqual(MemoryFixture.summaryMetadata, "Updated just now · Generated from your conversations")
        XCTAssertEqual(MemoryFixture.composerPlaceholder, "Ask or update memory")
        XCTAssertEqual(MemoryFixture.summarySections.map(\.heading),
                       ["Overview", "How Rem should work", "Current focus"])
        XCTAssertTrue(MemoryFixture.summarySections[0].body.hasPrefix("You prefer direct, practical help"))
    }

    func testMemoryComposerBoundaryIgnoresBlankAndNotesText() {
        XCTAssertNil(MemoryFixture.composerFeedback(for: ""))
        XCTAssertNil(MemoryFixture.composerFeedback(for: "   \n"))
        let feedback = MemoryFixture.composerFeedback(for: "remember I prefer morning meetings")
        XCTAssertNotNil(feedback)
        // Deterministic fixture feedback — not a generated reply.
        XCTAssertEqual(feedback, "Noted in this prototype session. Rem doesn’t reply or change memory here.")
    }

    // MARK: Models

    func testModelsProviderPickerHasExactlyFiveProvidersInSourceOrder() {
        XCTAssertEqual(ModelProvider.allCases.map(\.rawValue),
                       ["Anthropic", "OpenAI", "Google", "Mistral", "OpenRouter"])
    }

    func testModelsDefaultsMatchSource() {
        let fixture = ModelsFixture()
        XCTAssertTrue(fixture.autoManagedModel)
        XCTAssertTrue(fixture.isSaved(.anthropic))      // subtitle "Saved"
        XCTAssertFalse(fixture.isAvailable(.anthropic)) // availability switch off
    }

    func testModelsAutoAndProviderFlagsAreIndependent() {
        var fixture = ModelsFixture()
        fixture.autoManagedModel = false
        fixture.setAvailable(.anthropic, true)
        XCTAssertFalse(fixture.autoManagedModel)
        XCTAssertTrue(fixture.isAvailable(.anthropic))
        // Toggling availability never clears the saved-key flag, and Auto is unaffected.
        XCTAssertTrue(fixture.isSaved(.anthropic))
    }

    func testModelsSaveEnablementRequiresNonemptyDraft() {
        XCTAssertFalse(ModelsFixture.canSave(keyDraft: ""))
        XCTAssertFalse(ModelsFixture.canSave(keyDraft: "   "))
        XCTAssertTrue(ModelsFixture.canSave(keyDraft: "sk-illustrative-dummy"))
    }

    func testModelsSaveRecordsFlagForNewProviderWithoutStoringKey() {
        var fixture = ModelsFixture()
        XCTAssertFalse(fixture.isSaved(.openAI))
        fixture.recordSavedKey(for: .openAI)
        XCTAssertTrue(fixture.isSaved(.openAI))
        // The existing saved provider is untouched; only a provider flag is tracked, never a key value.
        XCTAssertTrue(fixture.isSaved(.anthropic))
        XCTAssertEqual(fixture.savedProviders, [.anthropic, .openAI])
    }
}
