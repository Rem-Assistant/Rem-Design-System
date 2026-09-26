package com.rem.designsystem.brand

import androidx.compose.runtime.Composable
import com.figma.code.connect.FigmaConnect
import com.figma.code.connect.FigmaProperty
import com.figma.code.connect.FigmaType

/**
 * Code Connect for `RemFaceMark` — the Compose twin of `RemFaceMark.figma.swift`. Binds the Figma
 * RemFaceMark component `362:7` (property `Mode` = idle / thinking) to the real Kotlin type, co-located
 * so a rename surfaces drift immediately (`figma connect check`).
 *
 * Like the Swift `.figma.swift` files, this is **excluded from the library's Gradle build** (the app
 * never links `com.figma.code.connect`; the `gatherDesignSystemSources` task drops `**/*.figma.kt`);
 * the `figma connect` CLI reads it directly. Dormant-but-ready — publishing is plan-gated (same as
 * RemButton / ContainedIcon / the SwiftUI Code Connect).
 *
 * ⚠️ VERIFY ON THE ANDROID RUNNER: confirm the exact `@FigmaProperty` enum-mapping signature against
 * the pinned `com.figma.code.connect` version. The SwiftUI `RemFaceMark.figma.swift` is the mature
 * reference for the mapping intent:  Mode: "Idle" → Idle, "Thinking" → Thinking.
 */
@FigmaConnect("https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=362-7")
class RemFaceMarkDoc {
    @FigmaProperty(FigmaType.Enum, "Mode")
    val mode: RemFaceMarkMode = RemFaceMarkMode.Idle

    @Composable
    fun example() {
        RemFaceMark(mode = mode)
    }
}
