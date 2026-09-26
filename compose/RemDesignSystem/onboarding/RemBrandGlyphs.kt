package com.rem.designsystem.onboarding

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Provider marks for the sign-in treatment. On iOS the Sign-in-with-Apple button uses the
 * `apple.logo` SF Symbol; Compose has no SF Symbol palette, so the mark is a vector here (form
 * diverges, treatment is reproduced). The Apple silhouette is a faithful monochrome mark so the
 * "Continue with/as …" button reproduces the reference treatment. It is tinted at the call site to
 * the inverted label color, so it reads white on the black button.
 *
 * The multicolor Google "G" is a vendor brand asset (not redistributable as a path here) — the host
 * passes the real asset via `signInStep(googleMark = …)`. Until then the Google button renders with no
 * leading mark, matching the repo's documented brand-asset-debt convention (neutral placeholder).
 */
object RemBrandGlyphs {
    /** Apple logo, monochrome, on a 24×24 viewport — tint at the call site. */
    val AppleLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppleLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(APPLE_LOGO_PATH).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }
}

// Recognizable Apple logo silhouette (24 viewport). Monochrome; recolored per button via `tint`.
private const val APPLE_LOGO_PATH =
    "M17.05 12.04c-.03-2.65 2.16-3.92 2.26-3.98-1.23-1.8-3.15-2.05-3.83-2.08-1.63-.16-3.18.96-4.01.96" +
        "-.83 0-2.1-.94-3.46-.91-1.78.03-3.42 1.03-4.34 2.62-1.85 3.21-.47 7.95 1.33 10.55.88 1.27 " +
        "1.93 2.7 3.31 2.65 1.33-.05 1.83-.86 3.44-.86 1.6 0 2.05.86 3.46.83 1.43-.03 2.33-1.3 " +
        "3.2-2.58.99-1.48 1.4-2.91 1.42-2.99-.03-.01-2.73-1.05-2.76-4.16zM14.63 4.64c.73-.89 " +
        "1.22-2.12 1.09-3.35-1.05.04-2.32.7-3.08 1.58-.68.78-1.27 2.03-1.11 3.23 1.17.09 2.37-.6 " +
        "3.1-1.46z"
