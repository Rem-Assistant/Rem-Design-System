import SwiftUI

/// **AgentStatusPill** — the small glass status pill shown under the agent avatar while the agent is
/// working. A quiet capsule (`.ultraThinMaterial` glass fill, capsule radius) carrying a single
/// `status` string whose color is driven by one `Tone`: `.neutral` reads in `labelPrimary`;
/// `.attention` reads in the adaptive brand accent to pull the eye to a state that needs the user
/// (e.g. "Needs you", "Needs approval"). Like `VoiceBar` it is the DS *display* form — it renders a
/// state, it is not the live session control.
///
/// Figma canonical: **AgentStatusPill** `427:21` (variant set, `Tone=` Neutral / Attention). Compose
/// sibling: `AgentStatusPill` in `agentsurfaces/AgentStatusPill.kt`.
///
/// GLASS: the shipping surface floats over the avatar, so the background is `.ultraThinMaterial`
/// (true glass on iOS/macOS). Compose has no material blur, so the Compose sibling falls back to
/// `backgroundSecondary` — the same flat-grey substitution the other DS components use.
public struct AgentStatusPill: View {
    /// Drives the label color. `.neutral` is a plain status; `.attention` flags a state that needs the
    /// user and reads in the brand accent (faithful to the Figma `Tone=Attention` variant, which uses
    /// `brand-blue`, not a red/orange alarm color).
    public enum Tone: Equatable, Sendable {
        case neutral
        case attention
    }

    private let status: String
    private let tone: Tone

    /// - Parameters:
    ///   - status: the status text, e.g. `Working`, `Generating PDF`, `Reviewing guidance`,
    ///     `Needs approval`, `Needs you`.
    ///   - tone: `.neutral` (default) or `.attention`.
    public init(_ status: String, tone: Tone = .neutral) {
        self.status = status
        self.tone = tone
    }

    private var labelColor: Color {
        switch tone {
        case .neutral: return DesignTokens.Color.labelPrimary
        case .attention: return DesignTokens.Color.brandBlueOnFill
        }
    }

    public var body: some View {
        Text(status)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(labelColor)
            .lineLimit(1)
            .padding(.horizontal, DesignTokens.Spacing.md)
            .padding(.vertical, 6)
            // Glass over content, with a `backgroundSecondary` underlay so the capsule still reads on a
            // flat (e.g. white) backdrop — matches the Compose sibling (no material blur there) and the
            // Figma proposal's visible capsule.
            .background(.ultraThinMaterial, in: Capsule())
            .background(DesignTokens.Color.backgroundSecondary, in: Capsule())
    }
}

#if DEBUG
#Preview("AgentStatusPill — tones") {
    VStack(spacing: 16) {
        AgentStatusPill("Working")
        AgentStatusPill("Generating PDF")
        AgentStatusPill("Reviewing guidance")
        AgentStatusPill("Needs approval", tone: .attention)
        AgentStatusPill("Needs you", tone: .attention)
    }
    .padding(40)
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
