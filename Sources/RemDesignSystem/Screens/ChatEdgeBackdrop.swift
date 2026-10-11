import SwiftUI

/// Edge backdrops: the full Chat composition draws the transcript **under** the header and under the composer
/// dock (and the keyboard-docked composer), so messages scroll behind both instead of clipping at solid bands
/// (reference: iOS bars and the Claude iOS app). Each edge is a material masked by a vertical gradient:
/// fully opaque at the screen edge (status bar + header; dock + home indicator), fading to clear just past the
/// chrome's inner edge. The header's controls and the composer pill keep their own materials. With Reduce
/// Transparency each edge is solid `backgroundPrimary` with a hairline on its inner edge. The composition owns
/// it; hosts wire nothing. Pure rules so both platforms agree. Twin of Compose `ChatEdgeBackdrop`. Internal (no
/// Code Connect mapping). Contract: `docs/contracts/chat.md` (Edge backdrops rule).
///
/// "Chrome height" is the distance from the top of the transcript viewport (the screen top: the transcript
/// extends under the status bar) to the header's bottom edge, including the header's bottom gap. "Dock height"
/// is the distance from the dock's top edge to the bottom of the transcript viewport (the screen bottom, or
/// the top of the keyboard), including the home indicator when the keyboard is down.
enum ChatEdgeBackdrop {
    /// Height of each fade from full material to clear (pt).
    static let fadeHeight: CGFloat = 20
    /// How far each fade reaches past the chrome's inner edge (pt). Equal to the transcript's vertical padding
    /// (`transcriptPadding`), so at rest the first message (top) and the latest message (bottom) sit wholly in
    /// the clear.
    static let fadeOverhang: CGFloat = 12
    /// The transcript content's own vertical padding inside the scroll view (`Spacing.md`).
    static let transcriptPadding: CGFloat = DesignTokens.Spacing.md
    /// Mask opacity at the screen edge (through to the start of the fade) and at the end of the fade.
    static let edgeOpacity: Double = 1
    static let clearOpacity: Double = 0
    /// Reduce Transparency fallback: hairline on the solid chrome's inner edge.
    static let hairline: CGFloat = 0.5

    // MARK: Top (header)

    /// Chrome height from measured edges in one coordinate space: the header's bottom edge minus the
    /// transcript viewport's top edge.
    static func chromeHeight(headerBottom: CGFloat, viewportTop: CGFloat) -> CGFloat {
        max(0, headerBottom - viewportTop)
    }

    /// The transcript's top content inset: exactly the chrome height, so the first message is never hidden
    /// under the header when scrolled to the top. Never negative.
    static func contentTopInset(chromeHeight: CGFloat) -> CGFloat {
        max(0, chromeHeight)
    }

    /// The header backdrop's total height: the chrome plus the fade's overhang below the header.
    static func backdropHeight(chromeHeight: CGFloat) -> CGFloat {
        contentTopInset(chromeHeight: chromeHeight) + fadeOverhang
    }

    /// Header mask stops (location 0…1 down the backdrop, opacity): opaque to the start of the fade, then clear
    /// at the bottom. The fade starts `fadeHeight - fadeOverhang` above the header's bottom edge.
    static func maskStops(chromeHeight: CGFloat) -> [(location: CGFloat, opacity: Double)] {
        let total = backdropHeight(chromeHeight: chromeHeight) // always ≥ fadeOverhang > 0
        let fadeStart = max(0, total - fadeHeight) / total
        return [(0, edgeOpacity), (fadeStart, edgeOpacity), (1, clearOpacity)]
    }

    /// Where the first message's top edge sits, in viewport coordinates, when scrolled to the top.
    static func firstMessageTop(chromeHeight: CGFloat) -> CGFloat {
        contentTopInset(chromeHeight: chromeHeight) + transcriptPadding
    }

    /// Whether a row whose top edge is at `rowTop` (viewport coordinates) is fully clear of the header backdrop.
    static func isClearOfBackdrop(rowTop: CGFloat, chromeHeight: CGFloat) -> Bool {
        rowTop >= backdropHeight(chromeHeight: chromeHeight)
    }

    /// Whether a row whose top edge is at `rowTop` is fully below the header.
    static func isBelowHeader(rowTop: CGFloat, chromeHeight: CGFloat) -> Bool {
        rowTop >= contentTopInset(chromeHeight: chromeHeight)
    }

    // MARK: Bottom (composer dock)

    /// Dock height from measured edges in one coordinate space: the transcript viewport's bottom edge minus the
    /// dock's top edge.
    static func dockHeight(viewportBottom: CGFloat, dockTop: CGFloat) -> CGFloat {
        max(0, viewportBottom - dockTop)
    }

    /// The transcript's bottom content inset: exactly the dock height, so the latest message is never hidden
    /// under the composer when scrolled to the bottom. Never negative.
    static func contentBottomInset(dockHeight: CGFloat) -> CGFloat {
        max(0, dockHeight)
    }

    /// The dock backdrop's total height: the dock plus the fade's overhang above it.
    static func dockBackdropHeight(dockHeight: CGFloat) -> CGFloat {
        contentBottomInset(dockHeight: dockHeight) + fadeOverhang
    }

    /// Dock mask stops (location 0…1 down the backdrop, opacity): clear at the top (the overhang above the
    /// dock), opaque from the end of the fade down to the screen edge. The fade ends `fadeHeight - fadeOverhang`
    /// below the dock's top edge, so it is clear about 20pt above the composer pill.
    static func dockMaskStops(dockHeight: CGFloat) -> [(location: CGFloat, opacity: Double)] {
        let total = dockBackdropHeight(dockHeight: dockHeight) // always ≥ fadeOverhang > 0
        let fadeEnd = min(total, fadeHeight) / total
        return [(0, clearOpacity), (fadeEnd, edgeOpacity), (1, edgeOpacity)]
    }

    /// Where the latest message's bottom edge sits, in viewport coordinates, when scrolled to the bottom.
    static func latestMessageBottom(viewportHeight: CGFloat, dockHeight: CGFloat) -> CGFloat {
        viewportHeight - contentBottomInset(dockHeight: dockHeight) - transcriptPadding
    }

    /// Whether a row whose bottom edge is at `rowBottom` (viewport coordinates) is fully clear of the dock
    /// backdrop.
    static func isClearOfDockBackdrop(rowBottom: CGFloat, viewportHeight: CGFloat, dockHeight: CGFloat) -> Bool {
        rowBottom <= viewportHeight - dockBackdropHeight(dockHeight: dockHeight)
    }

    /// Gap from the viewport bottom to the jump-to-latest button: the dock plus the button's own spacing, so it
    /// keeps `ChatJumpToLatest.bottomSpacing` above the composer chrome.
    static func jumpButtonBottomPadding(dockHeight: CGFloat) -> CGFloat {
        contentBottomInset(dockHeight: dockHeight) + ChatJumpToLatest.bottomSpacing
    }

    // MARK: Both edges

    /// Minimum height of the transcript content inside the viewport, so a short conversation fills the visible
    /// area between the header and the dock (bottom-aligned) without becoming scrollable.
    static func transcriptMinHeight(viewportHeight: CGFloat, topInset: CGFloat, bottomInset: CGFloat = 0) -> CGFloat {
        max(0, viewportHeight - max(0, topInset) - max(0, bottomInset))
    }

    /// How far the transcript's bottom edge (its bottom inset included) sits below the visible area, from the
    /// content's bottom edge in the scroll view's own space. Zero at rest at the latest message, so
    /// `ChatJumpToLatest.isVisible` hides there.
    static func distanceFromLatest(contentBottom: CGFloat, viewportHeight: CGFloat) -> CGFloat {
        contentBottom - viewportHeight
    }
}

/// Which edge a backdrop sits on.
enum ChatEdge {
    case top, bottom
}

/// An edge backdrop drawn over the transcript: a gradient-masked material, or the solid Reduce Transparency
/// fallback with a hairline on its inner edge. `height` is the chrome (top) or dock (bottom) height. Not
/// interactive and hidden from accessibility. The caller extends it under the status bar / home indicator.
struct ChatEdgeBackdropView: View {
    let edge: ChatEdge
    let height: CGFloat
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        Group {
            if reduceTransparency {
                DesignTokens.Color.backgroundPrimary
                    .frame(height: max(0, height))
                    .overlay(alignment: edge == .top ? .bottom : .top) {
                        DesignTokens.Color.separator.frame(height: ChatEdgeBackdrop.hairline)
                    }
            } else {
                let total = edge == .top
                    ? ChatEdgeBackdrop.backdropHeight(chromeHeight: height)
                    : ChatEdgeBackdrop.dockBackdropHeight(dockHeight: height)
                let stops = edge == .top
                    ? ChatEdgeBackdrop.maskStops(chromeHeight: height)
                    : ChatEdgeBackdrop.dockMaskStops(dockHeight: height)
                Rectangle()
                    .fill(.regularMaterial)
                    .frame(height: total)
                    .mask {
                        LinearGradient(
                            stops: stops.map { Gradient.Stop(color: .black.opacity($0.opacity), location: $0.location) },
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: edge == .top ? .top : .bottom)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
