package com.rem.designsystem.primitives

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import com.figma.code.connect.FigmaConnect
import com.figma.code.connect.FigmaProperty
import com.figma.code.connect.FigmaType
import com.rem.designsystem.tokens.RemLightColors

/**
 * Code Connect for `ContainedIcon` — the Compose twin of `ContainedIcon.figma.swift`. Binds the Figma
 * ContainedIcon variant set `614:8` (properties `Fill` × `Size`) to the real Kotlin types, co-located
 * so a rename surfaces drift immediately (`figma connect check`).
 *
 * Like the Swift `.figma.swift` files, this is **excluded from the library's Gradle build** (the app
 * never links `com.figma.code.connect`); the `figma connect` CLI reads it directly. Dormant-but-ready
 * — publishing is plan-gated (same as RemButton / the SwiftUI Code Connect).
 *
 * ⚠️ VERIFY ON THE ANDROID RUNNER: Compose Code Connect's DSL is newer than SwiftUI's — confirm the
 * exact `@FigmaProperty` enum-mapping signature against the pinned `com.figma.code.connect` version.
 * The SwiftUI `ContainedIcon.figma.swift` is the mature reference for the mapping intent below:
 *   Size:  "Small" → Small,  "Large" → Large
 *   Fill:  "Tinted" → Tint(brandBlue),  "Subtle" → Subtle
 * `RemLightColors.brandBlue` is used because brand blue is a fixed value token (identical in both
 * schemes), so it resolves without a `@Composable` scope here.
 */
@FigmaConnect("https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=614-8")
class ContainedIconDoc {
    @FigmaProperty(FigmaType.Enum, "Size")
    val size: ContainedIconSize = ContainedIconSize.Small

    @FigmaProperty(FigmaType.Enum, "Fill")
    val fill: ContainedIconFill = ContainedIconFill.Tint(RemLightColors.brandBlue)

    @Composable
    fun example() {
        // The swapped glyph is a Material ImageVector on Android (form diverges; intent shared).
        ContainedIcon(icon = Icons.Filled.Settings, fill = fill, size = size)
    }
}
