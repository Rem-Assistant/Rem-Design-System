package com.rem.designsystem.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme

/**
 * The **Rem face mark** — the Compose sibling of `Sources/RemDesignSystem/Brand/RemFaceMark.swift`,
 * and the Android half of the ONE canonical brand mark. It draws the SAME scalloped "sun/star" blob
 * outline (from the SVG-derived `CustomFaceShape`), the two rounded-rect eyes, and the gentle smile —
 * so both platforms render the real Rem identity instead of a generic system "face" glyph
 * (`Icons.Filled.Face` was the wrong stand-in this replaces).
 *
 * Source of truth for the bezier path + geometry ratios: `Shared/Views/RemFaceMark.swift` in
 * `rem-assistant/remclaw`. Every proportion here (pen weight, eye size/gap/offset, mouth inset/dip)
 * mirrors that file so the two marks are the same shape. Per the SPEC cross-platform contract the
 * *form* is native (a Compose [Canvas] path, not a SwiftUI `Shape`); the *intent + geometry* are shared.
 *
 * The mark is presentational and static (Paparazzi renders a single frame); [mode] selects which
 * resting composition is drawn:
 *  - [RemFaceMarkMode.Idle] — the happy resting face: outline + eyes + smile.
 *  - [RemFaceMarkMode.Thinking] — the heavier self-drawing outline only (features hidden), matching
 *    the app's "Rem is thinking" signature at its resting (fully drawn) frame.
 *
 * Figma canonical: RemFaceMark `362:7` (property `Mode` = idle / thinking). Code Connect: `RemFaceMark.figma.kt`.
 */
enum class RemFaceMarkMode { Idle, Thinking }

@Composable
fun RemFaceMark(
    modifier: Modifier = Modifier,
    mode: RemFaceMarkMode = RemFaceMarkMode.Idle,
    tint: Color? = null,
    size: Dp = 96.dp,
) {
    // Default ink = labelPrimary (resolved here so the default doesn't need a @Composable expression).
    val ink = tint ?: RemColors.current.labelPrimary
    val thinking = mode == RemFaceMarkMode.Thinking

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val edge = minOf(w, h)

        // Derived geometry — identical ratios to the SwiftUI mark (all proportional to `edge`).
        val penWeight = edge * 0.06f
        val outlineWidth = if (thinking) edge * 0.075f else penWeight

        // Outline (the scalloped blob). `.thinking` uses the heavier stroke, features hidden.
        drawPath(
            path = remFacePath(w, h),
            color = ink,
            style = Stroke(width = outlineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        if (thinking) return@Canvas

        // Eyes — two short vertical rounded bars, centered, nudged up.
        val eyeWidth = penWeight
        val eyeHeight = edge * 0.19f
        val eyeGap = edge * 0.15f
        val eyeCorner = eyeWidth / 2f
        val eyesCenterY = h / 2f - edge * 0.06f
        val eyeDx = eyeGap / 2f + eyeWidth / 2f
        for (cx in listOf(w / 2f - eyeDx, w / 2f + eyeDx)) {
            drawRoundRect(
                color = ink,
                topLeft = Offset(cx - eyeWidth / 2f, eyesCenterY - eyeHeight / 2f),
                size = Size(eyeWidth, eyeHeight),
                cornerRadius = CornerRadius(eyeCorner, eyeCorner),
            )
        }

        // Smile — a gentle upward-opening quadratic arc, narrower than its frame, sat below the eyes.
        val mouthWidth = edge * 0.30f
        val mouthHeight = edge * 0.11f
        val mouthCenterY = h / 2f + edge * 0.20f
        val rectLeft = w / 2f - mouthWidth / 2f
        val rectTop = mouthCenterY - mouthHeight / 2f
        val inset = mouthWidth * 0.14f
        val dip = mouthHeight * 0.6f
        val smile = Path().apply {
            moveTo(rectLeft + inset, rectTop)
            quadraticBezierTo(
                rectLeft + mouthWidth / 2f, rectTop + 2f * dip,
                rectLeft + mouthWidth - inset, rectTop,
            )
        }
        drawPath(
            path = smile,
            color = ink,
            style = Stroke(width = penWeight, cap = StrokeCap.Round),
        )
    }
}

/**
 * The scalloped blob outline as a Compose [Path]. Ports the SVG-derived `CustomFaceShape` verbatim:
 * the control points live on a 204×200 viewport and are scaled to the canvas (sx = w/204, sy = h/200),
 * exactly as the SwiftUI `Shape` does (`rect.width / 204`, `rect.height / 200`).
 */
private fun remFacePath(width: Float, height: Float): Path {
    val sx = width / 204f
    val sy = height / 200f
    return Path().apply {
        moveTo(101.813f * sx, 0f)
        cubicTo(110.46f * sx, 0f, 117.579f * sx, 2.738f * sy, 123.171f * sx, 8.214f * sy)
        cubicTo(128.694f * sx, 13.569f * sy, 133.531f * sx, 20.575f * sy, 137.677f * sx, 29.236f * sy)
        cubicTo(147.093f * sx, 27.003f * sy, 155.574f * sx, 26.367f * sy, 163.119f * sx, 27.324f * sy)
        cubicTo(170.843f * sx, 28.304f * sy, 177.444f * sx, 32.224f * sy, 182.920f * sx, 39.083f * sy)
        cubicTo(188.281f * sx, 45.828f * sy, 190.587f * sx, 53.091f * sy, 189.838f * sx, 60.873f * sy)
        cubicTo(189.099f * sx, 68.542f * sy, 186.570f * sx, 76.688f * sy, 182.249f * sx, 85.308f * sy)
        cubicTo(189.781f * sx, 91.162f * sy, 195.539f * sx, 97.371f * sy, 199.522f * sx, 103.934f * sy)
        cubicTo(203.615f * sx, 110.621f * sy, 204.710f * sx, 118.173f * sy, 202.808f * sx, 126.589f * sy)
        cubicTo(200.848f * sx, 135.063f * sy, 196.553f * sx, 141.432f * sy, 189.924f * sx, 145.698f * sy)
        cubicTo(183.570f * sx, 149.767f * sy, 175.760f * sx, 152.840f * sy, 166.496f * sx, 154.915f * sy)
        cubicTo(166.542f * sx, 164.573f * sy, 165.272f * sx, 172.944f * sy, 162.687f * sx, 180.026f * sy)
        cubicTo(160.035f * sx, 187.347f * sy, 154.789f * sx, 192.881f * sy, 146.950f * sx, 196.628f * sy)
        cubicTo(142.338f * sx, 198.876f * sy, 137.726f * sx, 200f * sy, 133.115f * sx, 200f * sy)
        cubicTo(127.811f * sx, 200f * sy, 122.537f * sx, 198.674f * sy, 117.291f * sx, 196.022f * sy)
        cubicTo(112.188f * sx, 193.471f * sy, 107.028f * sx, 190.054f * sy, 101.813f * sx, 185.774f * sy)
        cubicTo(96.542f * sx, 190.055f * sy, 91.353f * sx, 193.471f * sy, 86.249f * sx, 196.022f * sy)
        cubicTo(81.061f * sx, 198.674f * sy, 75.815f * sx, 200f * sy, 70.512f * sx, 200f * sy)
        cubicTo(65.843f * sx, 200f * sy, 61.260f * sx, 198.905f * sy, 56.764f * sx, 196.714f * sy)
        cubicTo(48.866f * sx, 192.967f * sy, 43.592f * sx, 187.433f * sy, 40.940f * sx, 180.112f * sy)
        cubicTo(38.354f * sx, 172.974f * sy, 37.085f * sx, 164.575f * sy, 37.130f * sx, 154.915f * sy)
        cubicTo(27.810f * sx, 152.838f * sy, 19.971f * sx, 149.737f * sy, 13.616f * sx, 145.612f * sy)
        cubicTo(7.045f * sx, 141.346f * sy, 2.808f * sx, 135.005f * sy, 0.905f * sx, 126.589f * sy)
        cubicTo(-0.997f * sx, 118.173f * sy, 0.098f * sx, 110.621f * sy, 4.191f * sx, 103.934f * sy)
        cubicTo(8.174f * sx, 97.371f * sy, 13.906f * sx, 91.162f * sy, 21.383f * sx, 85.308f * sy)
        cubicTo(17.066f * sx, 76.680f * sy, 14.533f * sx, 68.507f * sy, 13.789f * sx, 60.787f * sy)
        cubicTo(13.040f * sx, 53.005f * sy, 15.374f * sx, 45.742f * sy, 20.793f * sx, 38.997f * sy)
        cubicTo(26.154f * sx, 32.195f * sy, 32.726f * sx, 28.304f * sy, 40.508f * sx, 27.324f * sy)
        cubicTo(48.099f * sx, 26.312f * sy, 56.594f * sx, 26.947f * sy, 65.995f * sx, 29.226f * sy)
        cubicTo(70.130f * sx, 20.499f * sy, 74.921f * sx, 13.494f * sy, 80.369f * sx, 8.214f * sy)
        cubicTo(86.019f * sx, 2.738f * sy, 93.167f * sx, 0f, 101.813f * sx, 0f)
        close()
    }
}

@Preview(showBackground = true)
@Composable
private fun RemFaceMarkPreview() {
    RemTheme {
        RemFaceMark(mode = RemFaceMarkMode.Idle, tint = RemColors.current.brandBlue, size = 96.dp)
    }
}
