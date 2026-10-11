package com.rem.designsystem.screens

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors

/**
 * Header backdrop — twin of SwiftUI `ChatHeaderBackdrop`: the full Chat composition draws the transcript
 * **under** the header, so messages scroll behind it instead of clipping at a solid header. Compose has no
 * backdrop (behind-content) blur: `Modifier.blur` / `RenderEffect` blur a node's *own* content, so a true
 * backdrop blur would mean drawing the transcript twice. The backdrop is therefore a **scrim only**: a
 * vertical gradient of `backgroundPrimary` from [SCRIM_TOP_ALPHA] over the status bar and header to clear just
 * below the header. With the system's high-contrast setting (Android 14+ contrast level above standard) it
 * falls back to solid `backgroundPrimary` with a hairline; Android has no reduce-transparency setting. The
 * composition owns it; hosts wire nothing. Internal (no Code Connect mapping). Contract:
 * `docs/contracts/chat.md` rule 11.
 *
 * "Chrome height" is the distance from the top of the transcript viewport (the window top: the transcript
 * extends under the status bar) to the header's bottom edge, including the header's bottom gap. All values dp.
 */
internal object ChatHeaderBackdrop {
    /** Height of the fade from full scrim to clear. */
    const val FADE_HEIGHT = 20f
    /** How far the fade reaches past the header's bottom edge: the transcript's top padding (`RemSpacing.md`). */
    const val FADE_OVERHANG = 12f
    /** The transcript content's own top padding inside the scroll (`RemSpacing.md`). */
    const val TRANSCRIPT_TOP_PADDING = 12f
    /** Scrim alpha of `backgroundPrimary` over the status bar and header (start of the fade), and at its end. */
    const val SCRIM_TOP_ALPHA = 0.95f
    const val SCRIM_BOTTOM_ALPHA = 0f
    /** High-contrast fallback: hairline under the solid `backgroundPrimary` chrome. */
    const val HAIRLINE = 0.5f

    /** The transcript's top content inset: exactly the chrome height. Never negative. */
    fun contentTopInset(chromeHeight: Float): Float = maxOf(0f, chromeHeight)

    /** The scrim's total height: the chrome plus the fade's overhang below the header. */
    fun backdropHeight(chromeHeight: Float): Float = contentTopInset(chromeHeight) + FADE_OVERHANG

    /** Gradient stops (fraction 0…1 down the scrim to alpha): full to the start of the fade, then clear. */
    fun scrimStops(chromeHeight: Float): List<Pair<Float, Float>> {
        val total = backdropHeight(chromeHeight) // always ≥ FADE_OVERHANG > 0
        val fadeStart = maxOf(0f, total - FADE_HEIGHT) / total
        return listOf(0f to SCRIM_TOP_ALPHA, fadeStart to SCRIM_TOP_ALPHA, 1f to SCRIM_BOTTOM_ALPHA)
    }

    /** Where the first message's top edge sits, in viewport coordinates, when scrolled to the top. */
    fun firstMessageTop(chromeHeight: Float): Float = contentTopInset(chromeHeight) + TRANSCRIPT_TOP_PADDING

    /** Whether a row whose top edge is at [rowTop] is fully clear of the scrim. */
    fun isClearOfBackdrop(rowTop: Float, chromeHeight: Float): Boolean = rowTop >= backdropHeight(chromeHeight)

    /** Whether a row whose top edge is at [rowTop] is fully below the header. */
    fun isBelowHeader(rowTop: Float, chromeHeight: Float): Boolean = rowTop >= contentTopInset(chromeHeight)

    /** Opening offset (dp) of the internal evidence seam [LocalChatHeaderBackdropEvidence]. */
    const val EVIDENCE_SCROLL = 96f
}

/** High contrast on (Android 14+ contrast level above standard). False where unavailable (layoutlib, < 34). */
@Composable
internal fun rememberChatHeaderSolidBackdrop(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Build.VERSION.SDK_INT >= 34 && runCatching {
            (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.contrast?.let { it > 0f } ?: false
        }.getOrDefault(false)
    }
}

/**
 * Lays [header] (on its backdrop, padded below the status bar) over [content], which fills the whole area
 * and receives the measured chrome height as its top inset. One measure pass, so a single-frame render
 * already has the inset (no post-layout state).
 */
@Composable
internal fun ChatHeaderOverlayLayout(
    modifier: Modifier,
    header: @Composable () -> Unit,
    content: @Composable BoxScope.(topInset: Dp) -> Unit,
) {
    val colors = RemColors.current
    val solid = rememberChatHeaderSolidBackdrop()
    SubcomposeLayout(modifier) { constraints ->
        val headerPlaceables = subcompose("header") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val chrome = size.height / density
                        if (solid) {
                            drawRect(colors.backgroundPrimary, size = size)
                            val hairline = ChatHeaderBackdrop.HAIRLINE.dp.toPx()
                            drawRect(colors.separator, topLeft = Offset(0f, size.height - hairline), size = Size(size.width, hairline))
                        } else {
                            val total = ChatHeaderBackdrop.backdropHeight(chrome).dp.toPx()
                            val stops = ChatHeaderBackdrop.scrimStops(chrome)
                                .map { (at, alpha) -> at to colors.backgroundPrimary.copy(alpha = alpha) }
                                .toTypedArray()
                            // Drawn past the header's bounds by the overhang; drawing is not clipped.
                            drawRect(
                                Brush.verticalGradient(*stops, startY = 0f, endY = total),
                                size = Size(size.width, total),
                            )
                        }
                    }
                    .statusBarsPadding(),
            ) { header() }
        }.map { it.measure(constraints.copy(minHeight = 0)) }
        val chromePx = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val inset = ChatHeaderBackdrop.contentTopInset(chromePx / density).dp
        val bodyPlaceables = subcompose("body") {
            Box(Modifier.fillMaxSize()) { content(inset) }
        }.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            bodyPlaceables.forEach { it.place(0, 0) }
            headerPlaceables.forEach { it.place(0, 0) }
        }
    }
}
