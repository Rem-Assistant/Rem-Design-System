import SwiftUI

/// **VoiceBar** (a.k.a. *MiniPlayerBar*) — the bottom-anchored voice bar shown in Chat while a voice
/// session is live. A single rounded bar (`backgroundSecondary` fill, `radius.large`) that reads its
/// whole layout from one `VoiceBarState`: a leading control circle, a centered status stack
/// (eyebrow · title · subtitle/timer), and a trailing control. It is deliberately *not* six separate
/// widgets — the same bar transforms in place across states.
///
/// Figma canonical: **VoiceBar** `160:884` (variant set, `State=` Connecting/Listening/Speaking/
/// Muted/Reading/Closing). Source lineage: `MiniPlayerBar.swift`. Compose sibling: `VoiceBar` in
/// `chat/VoiceBar.kt`.
///
/// This is the DS *display* form — it renders a state, not the interactive session controls. The app's
/// `MiniPlayerBar` wires the taps; here the affordances are shown, not wired.
public enum VoiceBarState: String, Equatable, Sendable, CaseIterable {
    /// Session is starting but not yet live — collapses to animated thinking-dots, no controls.
    case connecting
    /// Live, capturing the user — neutral mic + red hang-up + running timer.
    case listening
    /// The assistant is talking back — same chrome as listening, `Speaking…` title.
    case speaking
    /// Mic is muted — red `mic.slash.fill` leading + red hang-up.
    case muted
    /// Reading an authored brief aloud — `stop.fill` trailing instead of hang-up.
    case reading
    /// Idle and auto-closing — brand-accent draining top line + "Keep open" CTA.
    case closing
}

public struct VoiceBar: View {
    private let state: VoiceBarState
    private let eyebrow: String?
    private let title: String?
    private let subtitle: String?

    /// - Parameters:
    ///   - state: which of the six voice states to render.
    ///   - eyebrow: overrides the uppercase eyebrow (default per state, e.g. `VOICE CHAT`).
    ///   - title: overrides the status title (default per state, e.g. `Listening…`).
    ///   - subtitle: overrides the subtitle / timer line (default per state, e.g. `0:03`).
    public init(
        _ state: VoiceBarState,
        eyebrow: String? = nil,
        title: String? = nil,
        subtitle: String? = nil
    ) {
        self.state = state
        self.eyebrow = eyebrow
        self.title = title
        self.subtitle = subtitle
    }

    // Circle-control chrome, mirrored from MiniPlayerBar (32pt circle, 12pt semibold glyph).
    private let circleDiameter: CGFloat = 32
    private let glyphPointSize: CGFloat = 12

    public var body: some View {
        let appearance = Appearance(state: state)
        ZStack(alignment: .top) {
            HStack(spacing: DesignTokens.Spacing.md) {
                leading(appearance)
                Spacer(minLength: DesignTokens.Spacing.sm)
                center(appearance)
                Spacer(minLength: DesignTokens.Spacing.sm)
                trailing(appearance)
            }
            .padding(.horizontal, DesignTokens.Spacing.md)
            .frame(minHeight: 58)

            if appearance.showsClosingLine {
                closingLine
            }
        }
        .background(DesignTokens.Color.backgroundSecondary,
                    in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.large, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.large, style: .continuous))
    }

    // MARK: - Leading control (mic / mic.slash / none)

    @ViewBuilder
    private func leading(_ appearance: Appearance) -> some View {
        if state == .connecting {
            typingDots
                .frame(width: circleDiameter, height: circleDiameter)
        } else if let glyph = appearance.leadingGlyph {
            circle(glyph, tint: appearance.leadingTint)
        } else {
            Color.clear.frame(width: circleDiameter, height: circleDiameter)
        }
    }

    // MARK: - Centered status stack

    @ViewBuilder
    private func center(_ appearance: Appearance) -> some View {
        if state == .connecting {
            EmptyView()
        } else {
            VStack(alignment: appearance.centerAligned == .leading ? .leading : .center, spacing: 1) {
                Text((eyebrow ?? appearance.eyebrow).uppercased())
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .lineLimit(1)
                Text(title ?? appearance.title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .lineLimit(1)
                Text(subtitle ?? appearance.subtitle)
                    .font(appearance.subtitleMonospaced
                          ? .system(size: 12, design: .monospaced)
                          : .system(size: 12))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: appearance.centerAligned == .leading ? .leading : .center)
        }
    }

    // MARK: - Trailing control (hang-up / stop / Keep-open / none)

    @ViewBuilder
    private func trailing(_ appearance: Appearance) -> some View {
        switch appearance.trailing {
        case .hangUp:
            circle("phone.down.fill", tint: DesignTokens.Color.systemRed)
        case .stop:
            circle("stop.fill", tint: DesignTokens.Color.systemRed)
        case .keepOpen:
            Text("Keep open")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(DesignTokens.Color.labelOnColor)
                .padding(.horizontal, 18)
                .padding(.vertical, 9)
                .background(DesignTokens.Color.brandBlue, in: Capsule())
        case .none:
            Color.clear.frame(width: circleDiameter, height: circleDiameter)
        }
    }

    // MARK: - Pieces

    private func circle(_ symbol: String, tint: Color) -> some View {
        Image(systemName: symbol)
            .font(.system(size: glyphPointSize, weight: .semibold))
            .foregroundStyle(tint)
            .frame(width: circleDiameter, height: circleDiameter)
            .background(tint.opacity(0.2), in: Circle())
    }

    /// Three quiet dots standing in for the live "connecting" thinking animation.
    private var typingDots: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { _ in
                Circle()
                    .fill(DesignTokens.Color.labelSecondary)
                    .frame(width: 6, height: 6)
            }
        }
    }

    /// The auto-close countdown: a brand-accent line draining along the top edge (shown ~55% here).
    private var closingLine: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Rectangle().fill(DesignTokens.Color.brandBlue.opacity(0.18))
                Rectangle().fill(DesignTokens.Color.brandBlue)
                    .frame(width: geo.size.width * 0.55)
            }
        }
        .frame(height: 3)
    }
}

// MARK: - Per-state appearance

private extension VoiceBar {
    enum Trailing { case hangUp, stop, keepOpen, none }
    enum StatusAlign { case center, leading }

    struct Appearance {
        let eyebrow: String
        let title: String
        let subtitle: String
        let subtitleMonospaced: Bool
        let leadingGlyph: String?
        let leadingTint: Color
        let trailing: Trailing
        let centerAligned: StatusAlign
        let showsClosingLine: Bool

        init(state: VoiceBarState) {
            switch state {
            case .connecting:
                eyebrow = ""; title = ""; subtitle = ""; subtitleMonospaced = false
                leadingGlyph = nil; leadingTint = .clear
                trailing = .none; centerAligned = .center; showsClosingLine = false
            case .listening:
                eyebrow = "Voice chat"; title = "Listening…"; subtitle = "0:03"; subtitleMonospaced = true
                leadingGlyph = "mic.fill"; leadingTint = DesignTokens.Color.labelSecondary
                trailing = .hangUp; centerAligned = .center; showsClosingLine = false
            case .speaking:
                eyebrow = "Voice chat"; title = "Speaking…"; subtitle = "0:12"; subtitleMonospaced = true
                leadingGlyph = "mic.fill"; leadingTint = DesignTokens.Color.labelSecondary
                trailing = .hangUp; centerAligned = .center; showsClosingLine = false
            case .muted:
                eyebrow = "Voice chat"; title = "Muted"; subtitle = "0:24"; subtitleMonospaced = true
                leadingGlyph = "mic.slash.fill"; leadingTint = DesignTokens.Color.systemRed
                trailing = .hangUp; centerAligned = .center; showsClosingLine = false
            case .reading:
                eyebrow = "Latest brief"; title = "Reading latest brief"
                subtitle = "Continue listening, then reply"; subtitleMonospaced = false
                leadingGlyph = "mic.fill"; leadingTint = DesignTokens.Color.labelSecondary
                trailing = .stop; centerAligned = .center; showsClosingLine = false
            case .closing:
                eyebrow = "Voice chat"; title = "Closing voice chat"
                subtitle = "Paused — no recent activity"; subtitleMonospaced = false
                leadingGlyph = nil; leadingTint = .clear
                trailing = .keepOpen; centerAligned = .leading; showsClosingLine = true
            }
        }
    }
}

#if DEBUG
#Preview("VoiceBar — all states") {
    VStack(spacing: 12) {
        ForEach(VoiceBarState.allCases, id: \.self) { state in
            VoiceBar(state)
        }
    }
    .padding(24)
    .frame(width: 418)
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
