import SwiftUI

/// Header backdrop: the full Chat composition draws the transcript **under** the header, which sits on a
/// progressive blur instead of a solid fill, so messages scroll behind it (reference: iOS large-title bars
/// and the Claude iOS app). The backdrop is a material masked by a vertical gradient: fully opaque over the
/// status bar and the header, fading to clear just below the header's bottom edge. The header's own controls
/// keep their materials. With Reduce Transparency the backdrop is solid `backgroundPrimary` with a hairline.
/// The composition owns it; hosts wire nothing. Pure rules so both platforms agree. Twin of Compose
/// `ChatHeaderBackdrop`. Internal (no Code Connect mapping). Contract: `docs/contracts/chat.md` rule 11.
///
/// "Chrome height" below is the distance from the top of the transcript viewport (the screen top: the
/// transcript extends under the status bar) to the header's bottom edge, including the header's bottom gap.
enum ChatHeaderBackdrop {
    /// Height of the fade from full material to clear (pt).
    static let fadeHeight: CGFloat = 20
    /// How far the fade reaches past the header's bottom edge (pt). Equal to the transcript's top padding
    /// (`transcriptTopPadding`), so at rest at the top the first message sits wholly in the clear.
    static let fadeOverhang: CGFloat = 12
    /// The transcript content's own top padding inside the scroll view (`Spacing.md`).
    static let transcriptTopPadding: CGFloat = DesignTokens.Spacing.md
    /// Mask opacity over the status bar and header (start of the fade) and at the end of the fade.
    static let topOpacity: Double = 1
    static let bottomOpacity: Double = 0
    /// Reduce Transparency fallback: hairline under the solid `backgroundPrimary` chrome.
    static let hairline: CGFloat = 0.5

    /// The transcript's top content inset: exactly the chrome height, so the first message is never hidden
    /// under the header when scrolled to the top. Never negative.
    static func contentTopInset(chromeHeight: CGFloat) -> CGFloat {
        max(0, chromeHeight)
    }

    /// Chrome height from measured edges in one coordinate space: the header's bottom edge minus the
    /// transcript viewport's top edge.
    static func chromeHeight(headerBottom: CGFloat, viewportTop: CGFloat) -> CGFloat {
        max(0, headerBottom - viewportTop)
    }

    /// The material backdrop's total height: the chrome plus the fade's overhang below the header.
    static func backdropHeight(chromeHeight: CGFloat) -> CGFloat {
        contentTopInset(chromeHeight: chromeHeight) + fadeOverhang
    }

    /// Mask gradient stops (location 0…1 down the backdrop, opacity): opaque to the start of the fade,
    /// then clear at the bottom. The fade starts `fadeHeight - fadeOverhang` above the header's bottom edge.
    static func maskStops(chromeHeight: CGFloat) -> [(location: CGFloat, opacity: Double)] {
        let total = backdropHeight(chromeHeight: chromeHeight) // always ≥ fadeOverhang > 0
        let fadeStart = max(0, total - fadeHeight) / total
        return [(0, topOpacity), (fadeStart, topOpacity), (1, bottomOpacity)]
    }

    /// Minimum height of the transcript content inside the viewport, so a short conversation fills the
    /// visible area below the header (bottom-aligned) without becoming scrollable.
    static func transcriptMinHeight(viewportHeight: CGFloat, topInset: CGFloat) -> CGFloat {
        max(0, viewportHeight - max(0, topInset))
    }

    /// Where the first message's top edge sits, in viewport coordinates, when scrolled to the top.
    static func firstMessageTop(chromeHeight: CGFloat) -> CGFloat {
        contentTopInset(chromeHeight: chromeHeight) + transcriptTopPadding
    }

    /// Whether a row whose top edge is at `rowTop` (viewport coordinates) is fully clear of the backdrop.
    static func isClearOfBackdrop(rowTop: CGFloat, chromeHeight: CGFloat) -> Bool {
        rowTop >= backdropHeight(chromeHeight: chromeHeight)
    }

    /// Whether a row whose top edge is at `rowTop` is fully below the header (visible, possibly under the
    /// last part of the fade).
    static func isBelowHeader(rowTop: CGFloat, chromeHeight: CGFloat) -> Bool {
        rowTop >= contentTopInset(chromeHeight: chromeHeight)
    }
}

/// The header backdrop drawn behind the header and over the transcript: a gradient-masked material, or the
/// solid Reduce Transparency fallback. Not interactive and hidden from accessibility. Top-aligned; the
/// caller extends it under the status bar.
struct ChatHeaderBackdropView: View {
    let chromeHeight: CGFloat
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        Group {
            if reduceTransparency {
                DesignTokens.Color.backgroundPrimary
                    .frame(height: ChatHeaderBackdrop.contentTopInset(chromeHeight: chromeHeight))
                    .overlay(alignment: .bottom) {
                        DesignTokens.Color.separator.frame(height: ChatHeaderBackdrop.hairline)
                    }
            } else {
                Rectangle()
                    .fill(.regularMaterial)
                    .frame(height: ChatHeaderBackdrop.backdropHeight(chromeHeight: chromeHeight))
                    .mask {
                        LinearGradient(
                            stops: ChatHeaderBackdrop.maskStops(chromeHeight: chromeHeight).map {
                                Gradient.Stop(color: .black.opacity($0.opacity), location: $0.location)
                            },
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
