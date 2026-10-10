import Foundation

// MARK: - Chat composer fixture (presentation-only)
//
// The deterministic, in-memory host behind the Playground's canonical composer, mirroring the Settings
// fixture pattern (`SettingsCloudBrowserFixture.swift`). It drives `RemComposerBar(state:onAction:)`
// exactly as an app adapter would, so the Playground and the app render the **same** component and
// rules. It is **not** an app adapter and claims nothing about runtime truth: nothing is sent, the sent
// list is local fixture copy, and no delivery or read state is produced here.
//
// Compose twin: `screens/ChatComposerFixture.kt`. Tests: `ChatComposerFixtureTests.swift` /
// `ChatComposerFixtureTest.kt`.

public struct ChatComposerFixture: Equatable, Sendable {
    /// The host situations the Playground can switch between to review each presentation state.
    public enum Scenario: String, CaseIterable, Sendable {
        case ready = "Ready"
        case sending = "Sending"
        case streaming = "Streaming"
        case unavailable = "Unavailable"
        case noVoice = "No voice"
    }

    public static let unavailableReason = "Chat is unavailable in this fixture."
    public static let cancelNote = "Stop asks the app to cancel the turn."
    public static let speakNote = "Speak starts voice input in the app."

    public var state: ChatComposerState
    public private(set) var scenario: Scenario = .ready
    /// Local fixture copy of what Send produced. Never transmitted.
    public private(set) var sent: [String] = []
    /// A fixture note explaining what the app would do for an action the Playground cannot perform.
    public private(set) var note: String?

    public init(state: ChatComposerState = ChatComposerState()) {
        self.state = state
    }

    /// Switch the host situation. The draft and attachments are kept.
    public mutating func select(_ scenario: Scenario) {
        self.scenario = scenario
        state.phase = scenario == .sending ? .sending : scenario == .streaming ? .streaming : .idle
        state.availability = scenario == .unavailable ? .disabled(reason: Self.unavailableReason) : .enabled
        state.voiceAvailable = scenario != .noVoice
    }

    /// Apply one composer action the way a host would. `.add` is a presentation concern of the page
    /// (it opens Add to Chat) and is ignored here.
    public mutating func apply(_ action: ChatComposerAction) {
        switch action {
        case .draftChanged(let text):
            state.draft = text
        case .send:
            guard state.canSend else { return }
            let text = state.draft.trimmingCharacters(in: .whitespacesAndNewlines)
            let content = state.attachments.filter { $0.kind != .capability }
            sent.append(text.isEmpty ? content.map(\.title).joined(separator: ", ") : text)
            state.draft = ""
            state.attachments.removeAll { $0.kind != .capability }
            note = nil
        case .cancel:
            guard state.canCancel else { return }
            select(.ready)
            note = Self.cancelNote
        case .speak:
            guard state.showsSpeak else { return }
            note = Self.speakNote
        case .add:
            break
        case .removeAttachment(let id):
            state.attachments.removeAll { $0.id == id }
        case .focusChanged(let focused):
            state.isFocused = focused
        }
    }

    /// Adds an attachment once (by id), as Add to Chat does.
    public mutating func attach(_ attachment: ComposerAttachment) {
        if !state.attachments.contains(where: { $0.id == attachment.id }) { state.attachments.append(attachment) }
    }

    /// Replaces every attachment of `kind` (a new photo pick replaces the previous one).
    public mutating func replaceAttachments(of kind: ComposerAttachment.Kind, with attachments: [ComposerAttachment]) {
        state.attachments.removeAll { $0.kind == kind }
        state.attachments += attachments
    }

    /// Shows a page-level note (e.g. Manage Models, Camera).
    public mutating func show(note: String?) {
        self.note = note
    }
}
