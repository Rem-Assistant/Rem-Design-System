import Foundation

// MARK: - Shared agent identity inputs (presentation-only)
//
// Figma page `Proposed · Navigation & Daily Brief` (`2693:1474`): the sidebar (`2702:2479`), the New
// agent setup (`2714:3538`, superseded by the name + colour sheet) and the agent profile (`2718:3055` / `2718:3396`) all show the same agent
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

/// The fixed agent colour palette offered by New agent setup — eleven Apple system colours in
/// spectrum order, each bound to a `DesignTokens.Color.system*` token (`AgentColor.swatch`). The
/// chosen colour is the agent's identity colour (avatar now; its chat theme later). Compose twin
/// `AgentColor` lists the same cases in the same order, bound to the same `RemColors.system*` tokens.
public enum AgentColor: String, CaseIterable, Identifiable, Sendable {
    case red, orange, yellow, green, mint, teal, cyan, blue, indigo, purple, pink

    public var id: String { rawValue }

    /// VoiceOver name for the swatch, e.g. "Blue".
    public var accessibilityName: String { rawValue.capitalized }
}

/// The glyph on a side agent's avatar. New agent setup offers no glyph chooser (name + colour only),
/// so every new agent starts with the default glyph.
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

    /// The look a new agent starts with: the default glyph on the default colour (pink).
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
