import Foundation

// MARK: - New agent setup inputs (presentation-only)
//
// Figma frame **E · New agent — set up** (`2714:3538`), opened from the sidebar's **New agent** button.
// Like Grok's bot setup, the first thing the person does is name the lane: the Name field is the first
// focused input and **Create agent** stays disabled until it has a non-blank name. Look (glyph +
// colour), what the agent handles, and services are optional and can be changed later.
//
// The DS owns only the draft's presentation and validation. It never creates the agent: the host
// receives `.create(draft)` and owns the request, its pending/failed states and navigation to the new
// agent's first chat (`E2`, `2717:2725`). Contract: `docs/contracts/sidebar-new-agent.md`.
//
// Compose twin: `agents/NewAgentSetupModel.kt`.

/// The editable draft behind the setup form.
public struct NewAgentDraft: Equatable, Sendable {
    public var name: String
    /// "What should this agent handle?" — the outcome / lane description.
    public var handles: String
    public var look: AgentLook
    /// Services the host has already attached to this draft (from "Add a service").
    public var services: [AgentService]

    public init(name: String = "", handles: String = "", look: AgentLook = .default, services: [AgentService] = []) {
        self.name = name
        self.handles = handles
        self.look = look
        self.services = services
    }

    /// The name as it will be shown in the sidebar and chat header.
    public var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    /// The handles copy without surrounding whitespace; `nil` when left blank (it is optional).
    public var trimmedHandles: String? {
        let value = handles.trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }

    /// Naming the lane is the one required step.
    public var canCreate: Bool { !trimmedName.isEmpty }
}

/// What the person asked the setup screen to do. The host performs every one of these.
public enum NewAgentSetupAction: Equatable, Sendable {
    case cancel
    /// "Tap to change icon" — the host presents its icon chooser.
    case changeIcon
    case addService
    case openService(id: String)
    /// Emitted only when `draft.canCreate`; carries the trimmed draft.
    case create(NewAgentDraft)
}

/// Approved copy for the setup screen (Figma `2714:3538`, verbatim).
public enum NewAgentSetupCopy {
    public static let title = "New agent"
    public static let cancel = "Cancel"
    public static let changeIcon = "Tap to change icon"
    public static let nameHeader = "Name"
    public static let namePlaceholder = "e.g. Health"
    public static let nameFooter = "Shown in your sidebar and at the top of its chat."
    public static let handlesHeader = "What should this agent handle?"
    public static let handlesPlaceholder = "Appointments, prescriptions, insurance claims\u{2026}"
    public static let handlesFooter = "Describe the outcome you want. Rem routes anything in this lane here."
    public static let servicesFooter = "Optional. You can connect more later."
    public static let create = "Create agent"
}

public extension NewAgentDraft {
    /// The action Create should emit, or `nil` when the lane has no name yet. Trims what is sent so a
    /// stray space never becomes part of the agent's name.
    var createAction: NewAgentSetupAction? {
        guard canCreate else { return nil }
        var sent = self
        sent.name = trimmedName
        sent.handles = trimmedHandles ?? ""
        return .create(sent)
    }
}
