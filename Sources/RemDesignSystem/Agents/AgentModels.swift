import Foundation

// MARK: - Shared agent identity inputs (presentation-only)
//
// Figma page `Proposed · Navigation & Daily Brief` (`2693:1474`): the sidebar (`2702:2479`), the New
// agent setup (`2714:3538`) and the agent profile (`2718:3055` / `2718:3396`) all show the same agent
// identity. These values are what a host hands the design system; the DS never creates, persists or
// routes agents. Contract: `docs/contracts/sidebar-new-agent.md`.
//
// Compose twin: `agents/AgentModels.kt`. Tests: `AgentDisplayModelTests.swift` / `AgentDisplayModelTest.kt`.

/// Which agent this is. Exactly one **Main agent** (Rem) exists; every user-created agent is a
/// **Side agent** with its own lane.
public enum AgentKind: String, CaseIterable, Sendable {
    case main
    case side

    /// The approved section / status copy — "Main agent" / "Side agent".
    public var label: String {
        switch self {
        case .main: return "Main agent"
        case .side: return "Side agent"
        }
    }
}

/// The colour swatches offered by New agent setup, in the Figma order (`2714:3590`).
public enum AgentColor: String, CaseIterable, Identifiable, Sendable {
    case pink, blue, green, purple, indigo

    public var id: String { rawValue }

    /// VoiceOver name for the swatch.
    public var accessibilityName: String { rawValue.capitalized }
}

/// The glyph on a side agent's avatar. Only the glyph the approved frames show is registered; the
/// icon chooser behind "Tap to change icon" is not designed yet, so the host owns that picker.
/// Each case is a researched SF Symbol ↔ Material Symbol pair in `docs/contracts/icon-registry.md`.
public enum AgentGlyph: String, CaseIterable, Sendable {
    case heart

    /// The SF Symbol (FILL pinned by the icon registry).
    public var systemName: String {
        switch self {
        case .heart: return "heart.fill"
        }
    }
}

/// A side agent's look: glyph + colour. The Main agent always renders the Rem face instead.
public struct AgentLook: Equatable, Sendable {
    public var glyph: AgentGlyph
    public var color: AgentColor

    public init(glyph: AgentGlyph = .heart, color: AgentColor = .pink) {
        self.glyph = glyph
        self.color = color
    }

    /// The look a new agent starts with (the E frame's default selection).
    public static let `default` = AgentLook()
}

/// The glyph on a connected-service row's tinted tile.
public enum AgentServiceGlyph: String, CaseIterable, Sendable {
    /// A calendar connector, e.g. Google Calendar.
    case calendar
    /// Generic service / the "Add a service" row.
    case link

    public var systemName: String {
        switch self {
        case .calendar: return "calendar"
        case .link: return "link"
        }
    }
}

/// One service an agent may use (host-supplied name and status copy, e.g. "Connected").
public struct AgentService: Identifiable, Equatable, Sendable {
    public var id: String
    public var name: String
    /// Host copy for the service state, e.g. "Connected". `nil` hides the subtitle.
    public var status: String?
    public var glyph: AgentServiceGlyph

    public init(id: String, name: String, status: String? = nil, glyph: AgentServiceGlyph = .link) {
        self.id = id
        self.name = name
        self.status = status
        self.glyph = glyph
    }
}

/// Copy shared by every surface that lists an agent's services.
public enum AgentServicesCopy {
    public static let header = "Connected services"
    public static let addService = "Add a service"
}
