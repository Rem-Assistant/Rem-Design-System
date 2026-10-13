import SwiftUI

/// **BrowserLiveCard** — the in-chat cloud-browser card. A compact, horizontal card that shows a
/// small preview thumbnail of Rem's live browser session, the session title, and a one-line status
/// that changes with the session's lifecycle. It is the chat-transcript entry point to the cloud
/// browser (watch / take over / review).
///
/// The whole card reads its layout from a single `State`, exactly like `VoiceBar` reads `VoiceBarState`:
/// one `backgroundSecondary` rounded card (`radius.large`) with a leading **56×40** thumbnail and a
/// trailing title + status stack. The thumbnail transforms in place across states — a loading globe
/// while **opening**, a live mini-browser (brand-blue chrome bar) while **active**, and a greyed
/// mini-browser once **ended** — rather than three separate widgets.
///
/// Figma canonical: **BrowserLiveCard** `524:31` (variant set, `State=` Opening / Active / Ended).
/// Source lineage: `BrowserLiveView.swift`. Compose sibling: `BrowserLiveCard` in
/// `agentsurfaces/BrowserLiveCard.kt`.
///
/// This is the DS *display* form — it renders a state, not the interactive session controls. The app
/// wires the tap-to-watch / tap-to-review affordances; here the status copy names them.
public enum BrowserLiveCardState: String, Equatable, Sendable, CaseIterable {
    /// The session is spinning up — thumbnail shows a loading globe, status reads "Opening browser…".
    case opening
    /// The session is live — thumbnail shows a brand-blue mini-browser, status invites watch / takeover.
    case active
    /// The session has finished — thumbnail greys out, status invites review.
    case ended
}

public struct BrowserLiveCard: View {
    private let state: BrowserLiveCardState
    private let title: String
    private let status: String?

    /// - Parameters:
    ///   - state: which lifecycle state to render (`.opening` / `.active` / `.ended`).
    ///   - title: the session label. Defaults to the Figma copy, "Rem's browser session".
    ///   - status: overrides the status line (default per state, e.g. "Session active · tap to watch
    ///     or take over").
    public init(
        _ state: BrowserLiveCardState,
        title: String = "Rem's browser session",
        status: String? = nil
    ) {
        self.state = state
        self.title = title
        self.status = status
    }

    // Thumbnail metrics, mirrored from the Figma specimen (56×40 preview, 10pt radius).
    private let thumbWidth: CGFloat = 56
    private let thumbHeight: CGFloat = 40
    private let thumbRadius: CGFloat = 10

    public var body: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            thumbnail
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(DesignTokens.Typography.subheadline.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .lineLimit(1)
                Text(status ?? defaultStatus)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(DesignTokens.Spacing.sm)
        .background(
            DesignTokens.Color.backgroundSecondary,
            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.large, style: .continuous)
        )
    }

    // MARK: - Thumbnail (per-state preview)

    @ViewBuilder
    private var thumbnail: some View {
        ZStack {
            RoundedRectangle(cornerRadius: thumbRadius, style: .continuous)
                .fill(DesignTokens.Color.fillTertiary)
            switch state {
            case .opening:
                Image(systemName: "globe")
                    .font(.system(size: 20, weight: .regular))
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            case .active:
                miniBrowser(chrome: DesignTokens.Color.brandBlue)
            case .ended:
                miniBrowser(chrome: DesignTokens.Color.labelTertiary)
            }
        }
        .frame(width: thumbWidth, height: thumbHeight)
        .clipShape(RoundedRectangle(cornerRadius: thumbRadius, style: .continuous))
    }

    /// A tiny browser window: a chrome bar across the top + two short content lines.
    private func miniBrowser(chrome: Color) -> some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(chrome)
                .frame(height: 12)
            VStack(alignment: .leading, spacing: 4) {
                RoundedRectangle(cornerRadius: 2, style: .continuous)
                    .fill(DesignTokens.Color.labelTertiary)
                    .frame(width: 28, height: 5)
                RoundedRectangle(cornerRadius: 2, style: .continuous)
                    .fill(DesignTokens.Color.labelTertiary)
                    .frame(width: 18, height: 5)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(.horizontal, 8)
            .padding(.top, 6)
        }
    }

    private var defaultStatus: String {
        switch state {
        case .opening: return "Opening browser…"
        case .active:  return "Session active · tap to watch or take over"
        case .ended:   return "Session ended · tap to review"
        }
    }
}

#if DEBUG
#Preview("BrowserLiveCard — all states") {
    VStack(spacing: 12) {
        ForEach(BrowserLiveCardState.allCases, id: \.self) { state in
            BrowserLiveCard(state)
        }
    }
    .padding(24)
    .frame(width: 362)
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
