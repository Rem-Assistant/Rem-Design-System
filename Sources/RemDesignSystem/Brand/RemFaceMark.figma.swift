import Figma
import SwiftUI

// Code Connect for RemFaceMark — binds the Figma RemFaceMark component `362:7` (property `Mode` =
// idle / thinking) to the real Swift type (`RemFaceMark`, `RemFaceMark.Mode`). Co-located with the
// component so a rename here surfaces drift immediately, and `figma connect check` validates it
// against the node.
//
// EXCLUDED from the RemDesignSystem SPM target (see Package.swift), so the shipping library never
// links `github.com/figma/code-connect`. The `figma connect` CLI reads it directly. Publishing is
// plan-gated (Org/Enterprise + Dev/Full seat), so this stays dormant-but-ready — same as RemButton
// and ContainedIcon.
struct RemFaceMark_connection: FigmaConnect {
    let component = RemFaceMark.self
    let figmaNodeUrl =
        "https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=362-7"

    @FigmaEnum("Mode", mapping: [
        "Idle": RemFaceMark.Mode.idle,
        "Thinking": RemFaceMark.Mode.thinking,
    ])
    var mode: RemFaceMark.Mode = .idle

    var body: some View {
        RemFaceMark(mode: mode, tint: DesignTokens.Color.labelPrimary, size: 96)
    }
}
