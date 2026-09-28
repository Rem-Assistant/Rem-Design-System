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

    /**
     * The multicolor **Google "G"** provider mark on a 16×16 viewport — the real vendor asset,
     * transferred verbatim (the four path segments + brand colors) from the shipping app's
     * `google-icon` SVG (`rem-assistant/remclaw` `Rem/Assets.xcassets/google-icon.imageset`). Each
     * segment carries its own `SolidColor`, so it must be rendered **untinted** (`Icon` with
     * `Color.Unspecified`, or `Image`) to keep its four colors. This is the SwiftUI [RemGoogleGlyph]'s
     * Compose sibling — the earlier `null` default left the Google button with no mark ("Google icon
     * not transferring"); it is now the canonical default.
     */
    val GoogleG: ImageVector by lazy {
        ImageVector.Builder(
            name = "GoogleG",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(PathParser().parsePathString(GOOGLE_BLUE).toNodes(), fill = SolidColor(Color(0xFF4285F4)))
            addPath(PathParser().parsePathString(GOOGLE_GREEN).toNodes(), fill = SolidColor(Color(0xFF34A853)))
            addPath(PathParser().parsePathString(GOOGLE_YELLOW).toNodes(), fill = SolidColor(Color(0xFFFBBC05)))
            addPath(PathParser().parsePathString(GOOGLE_RED).toNodes(), fill = SolidColor(Color(0xFFEB4335)))
        }.build()
    }
}

// Google "G" path segments (16 viewport), verbatim from the vendor SVG. The near-zero `-4.57764e-05`
// origin coordinates in the red segment are written as `0` here — visually identical and free of any
// scientific-notation parsing ambiguity.
private const val GOOGLE_BLUE =
    "M15.8094 8.14968C15.8094 7.49416 15.7562 7.01581 15.6411 6.51975H8.15576V9.47841H12.5495C12.4609 " +
        "10.2137 11.9826 11.321 10.9195 12.065L10.9046 12.1641L13.2714 13.9976L13.4353 14.0139C14.9412 " +
        "12.6231 15.8094 10.5769 15.8094 8.14968Z"
private const val GOOGLE_GREEN =
    "M8.15558 15.945C10.3081 15.945 12.1152 15.2363 13.4352 14.0138L10.9194 12.065C10.2461 12.5345 " +
        "9.34258 12.8622 8.15558 12.8622C6.04731 12.8622 4.25794 11.4715 3.62007 9.54923L3.52658 " +
        "9.55717L1.06563 11.4617L1.03345 11.5512C2.34447 14.1555 5.03742 15.945 8.15558 15.945Z"
private const val GOOGLE_YELLOW =
    "M3.62036 9.54927C3.45205 9.05321 3.35465 8.52167 3.35465 7.97248C3.35465 7.42323 3.45205 6.89175 " +
        "3.6115 6.39569L3.60704 6.29004L1.11526 4.35489L1.03373 4.39367C0.493395 5.4744 0.18335 6.68802 " +
        "0.18335 7.97248C0.18335 9.25694 0.493395 10.4705 1.03373 11.5512L3.62036 9.54927Z"
private const val GOOGLE_RED =
    "M8.15558 3.08264C9.65262 3.08264 10.6625 3.7293 11.2383 4.26969L13.4883 2.07281C12.1064 0.788351 " +
        "10.3081 0 8.15558 0C5.03742 0 2.34447 1.78933 1.03345 4.39366L3.61122 6.39568C4.25794 4.47342 " +
        "6.04731 3.08264 8.15558 3.08264Z"

// Recognizable Apple logo silhouette (24 viewport). Monochrome; recolored per button via `tint`.
private const val APPLE_LOGO_PATH =
    "M17.05 12.04c-.03-2.65 2.16-3.92 2.26-3.98-1.23-1.8-3.15-2.05-3.83-2.08-1.63-.16-3.18.96-4.01.96" +
        "-.83 0-2.1-.94-3.46-.91-1.78.03-3.42 1.03-4.34 2.62-1.85 3.21-.47 7.95 1.33 10.55.88 1.27 " +
        "1.93 2.7 3.31 2.65 1.33-.05 1.83-.86 3.44-.86 1.6 0 2.05.86 3.46.83 1.43-.03 2.33-1.3 " +
        "3.2-2.58.99-1.48 1.4-2.91 1.42-2.99-.03-.01-2.73-1.05-2.76-4.16zM14.63 4.64c.73-.89 " +
        "1.22-2.12 1.09-3.35-1.05.04-2.32.7-3.08 1.58-.68.78-1.27 2.03-1.11 3.23 1.17.09 2.37-.6 " +
        "3.1-1.46z"
