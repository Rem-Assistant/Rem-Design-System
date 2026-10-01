import SwiftUI

/// **RunningTaskBanner** — the glass "Live-Activity"-style banner shown while an agent task is running
/// (e.g. *Browser · Signing in to my.dnb.com*). One glass capsule (`.ultraThinMaterial` fill, capsule
/// radius) laid out as: a leading thumbnail, a title + status stack, and a trailing **Stop** control.
/// A single `Tone` drives the status color: `.working` reads the status in `labelSecondary`;
/// `.attention` reads it in the brand accent to flag a task that needs the user (e.g.
/// "Needs you · Password rejected"). Like `VoiceBar` it is the DS *display* form — the affordances are
/// shown, not wired.
///
/// Figma canonical: **RunningTaskBanner** `432:39` (variant set, `Tone=` Working / Attention). Compose
/// sibling: `RunningTaskBanner` in `agentsurfaces/RunningTaskBanner.kt`.
///
/// GLASS: floats over content, so the background is `.ultraThinMaterial` (true glass on iOS/macOS).
/// Compose has no material blur, so the Compose sibling falls back to `backgroundSecondary` — the same
/// flat-grey substitution the other DS components use.
public struct RunningTaskBanner: View {
    /// Drives the status-line color. `.working` is a quiet in-progress line (`labelSecondary`);
    /// `.attention` flags a task that needs the user and reads in the brand accent (faithful to the
    /// Figma `Tone=Attention` variant, which uses `brand-blue`).
    public enum Tone: Equatable, Sendable {
        case working
        case attention
    }

    private let task: String
    private let status: String
    private let tone: Tone

    /// - Parameters:
    ///   - task: the task source / title, e.g. `Browser`.
    ///   - status: the live status line, e.g. `Signing in to my.dnb.com` or
    ///     `Needs you · Password rejected`.
    ///   - tone: `.working` (default) or `.attention`.
    public init(task: String, status: String, tone: Tone = .working) {
        self.task = task
        self.status = status
        self.tone = tone
    }

    private var statusColor: Color {
        switch tone {
        case .working: return DesignTokens.Color.labelSecondary
        case .attention: return DesignTokens.Color.brandBlueOnFill
        }
    }

    public var body: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            // Leading thumbnail placeholder (the live banner slots the task's preview here).
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium, style: .continuous)
                .fill(DesignTokens.Color.fillTertiary)
                .frame(width: 40, height: 40)

            VStack(alignment: .leading, spacing: 1) {
                Text(task)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .lineLimit(1)
                Text(status)
                    .font(.system(size: 13))
                    .foregroundStyle(statusColor)
                    .lineLimit(1)
            }

            Spacer(minLength: DesignTokens.Spacing.sm)

            // Trailing Stop control (display form — shown, not wired).
            Text("Stop")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.sm)
                .background(DesignTokens.Color.fillTertiary, in: Capsule())
        }
        .padding(.horizontal, DesignTokens.Spacing.sm)
        .padding(.vertical, DesignTokens.Spacing.sm)
        .frame(minHeight: 56)
        .background(.ultraThinMaterial,
                    in: Capsule(style: .continuous))
    }
}

#if DEBUG
#Preview("RunningTaskBanner — tones") {
    VStack(spacing: 16) {
        RunningTaskBanner(task: "Browser", status: "Signing in to my.dnb.com")
        RunningTaskBanner(task: "Browser",
                          status: "Needs you · Password rejected",
                          tone: .attention)
    }
    .padding(24)
    .frame(width: 418)
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
