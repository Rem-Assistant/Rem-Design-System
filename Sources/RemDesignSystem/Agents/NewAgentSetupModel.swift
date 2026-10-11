import Foundation

// MARK: - New agent setup inputs (presentation-only)
//
// The **New agent** sheet, opened from the sidebar's **New agent** button. Samuel (2026-10-11): "just
// a name and some color is fine." The person names the agent (the first, focused field) and picks one
// colour from the fixed `AgentColor` palette; a large avatar previews the colour live. **Create agent**
// stays disabled until the trimmed name is non-blank. Nothing else is asked here — what the agent
// handles, services and its icon are not part of setup.
//
// The DS owns only the draft's presentation and validation. It never creates the agent: the host
// receives `.create(draft)` and owns the request, its pending/failed states and navigation to the new
// agent's first chat. Contract: `docs/contracts/sidebar-new-agent.md`.
//
// Compose twin: `agents/NewAgentSetupModel.kt`.

/// The editable draft behind the setup sheet: a name and a colour.
public struct NewAgentDraft: Equatable, Sendable {
    public var name: String
    public var color: AgentColor

    public init(name: String = "", color: AgentColor = AgentLook.default.color) {
        self.name = name
        self.color = color
    }

    /// The name as it will be shown in the sidebar and chat header.
    public var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }

    /// Naming the agent is the one required step.
    public var canCreate: Bool { !trimmedName.isEmpty }

    /// The look the new agent gets: the default glyph on the chosen colour (no glyph chooser).
    public var look: AgentLook { AgentLook(glyph: AgentLook.default.glyph, color: color) }
}

/// What the person asked the setup sheet to do. The host performs every one of these.
public enum NewAgentSetupAction: Equatable, Sendable {
    /// The top-leading close (X) button — dismiss without creating.
    case cancel
    /// Emitted only when `draft.canCreate`; carries the draft with its name trimmed.
    case create(NewAgentDraft)
}

/// Approved copy for the setup sheet (recorded in `docs/contracts/sidebar-new-agent.md`).
public enum NewAgentSetupCopy {
    public static let title = "New agent"
    /// Accessibility label of the X button.
    public static let close = "Close"
    /// Accessibility label of the name field (the placeholder carries the visible prompt).
    public static let nameLabel = "Name"
    public static let namePlaceholder = "Agent name"
    /// Accessibility label of the swatch group.
    public static let colorsLabel = "Color"
    public static let create = "Create agent"
    /// VoiceOver hint on Create while it is disabled.
    public static let createHint = "Name your agent to continue."
}

public extension NewAgentDraft {
    /// The action Create should emit, or `nil` when the agent has no name yet. Trims what is sent so a
    /// stray space never becomes part of the agent's name.
    var createAction: NewAgentSetupAction? {
        guard canCreate else { return nil }
        var sent = self
        sent.name = trimmedName
        return .create(sent)
    }
}

/// Metrics shared with the Compose twin (`NewAgentSetupLayout` in `NewAgentSetupModel.kt`).
public enum NewAgentSetupLayout {
    /// The live avatar preview.
    public static let avatarSize: CGFloat = 120
    /// Each swatch's touch target (and the selected ring's outer diameter).
    public static let swatchTarget: CGFloat = 44
    /// The coloured disc inside a swatch; the gap to the ring shows the selection.
    public static let swatchDisc: CGFloat = 34
    /// The palette wraps into rows of this many swatches (11 colours → 6 + 5).
    public static let swatchesPerRow = 6
    /// Minimum height of the name pill; it grows with Dynamic Type.
    public static let nameFieldMinHeight: CGFloat = 56

    /// The palette split into rows, in `AgentColor.allCases` order.
    public static var swatchRows: [[AgentColor]] {
        stride(from: 0, to: AgentColor.allCases.count, by: swatchesPerRow).map {
            Array(AgentColor.allCases[$0..<min($0 + swatchesPerRow, AgentColor.allCases.count)])
        }
    }
}
