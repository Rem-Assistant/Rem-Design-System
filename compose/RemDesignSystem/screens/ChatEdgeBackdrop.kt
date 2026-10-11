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
import androidx.compose.runtime.Immutable
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
 * Edge backdrops — twin of SwiftUI `ChatEdgeBackdrop`: the full Chat composition draws the transcript **under**
 * the header and under the composer dock (IME-docked included), so messages scroll behind both instead of
 * clipping at solid bands. Each edge is a vertical gradient of `backgroundPrimary` from [SCRIM_EDGE_ALPHA] at the
 * screen edge (status bar + header; dock + navigation bar / IME) to clear just past the chrome's inner edge.
 * Compose has no backdrop (behind-content) blur: `Modifier.blur` / `RenderEffect` blur a node's *own* content.
 * With the system's high-contrast setting (Android 14+ contrast level above standard) each edge falls back to
 * solid `backgroundPrimary` with a hairline on its inner edge; Android has no reduce-transparency setting. The
 * composition owns it; hosts wire nothing. Internal (no Code Connect mapping). Contract: `docs/contracts/chat.md`
 * (Edge backdrops rule).
 *
 * "Chrome height" is the distance from the top of the transcript viewport (the window top) to the header's
 * bottom edge, including the header's bottom gap. "Dock height" is the distance from the dock's top edge to the
 * window bottom, including the navigation bar or the IME. All values dp.
 */
internal object ChatEdgeBackdrop {
    /** Height of each fade from full scrim to clear. */
    const val FADE_HEIGHT = 20f
    /** How far each fade reaches past the chrome's inner edge: the transcript's vertical padding (`RemSpacing.md`). */
    const val FADE_OVERHANG = 12f
    /** The transcript content's own vertical padding inside the scroll (`RemSpacing.md`). */
    const val TRANSCRIPT_PADDING = 12f
    /** Scrim alpha of `backgroundPrimary` at the screen edge (through to the start of the fade), and at its end. */
    const val SCRIM_EDGE_ALPHA = 0.85f
    const val SCRIM_CLEAR_ALPHA = 0f
    /** High-contrast fallback: hairline on the solid chrome's inner edge. */
    const val HAIRLINE = 0.5f

    // Top (header)

    /** The transcript's top content inset: exactly the chrome height. Never negative. */
    fun contentTopInset(chromeHeight: Float): Float = maxOf(0f, chromeHeight)

    /** The header scrim's total height: the chrome plus the fade's overhang below the header. */
    fun backdropHeight(chromeHeight: Float): Float = contentTopInset(chromeHeight) + FADE_OVERHANG

    /** Header gradient stops (fraction 0…1 down the scrim to alpha): full to the start of the fade, then clear. */
    fun scrimStops(chromeHeight: Float): List<Pair<Float, Float>> {
        val total = backdropHeight(chromeHeight) // always ≥ FADE_OVERHANG > 0
        val fadeStart = maxOf(0f, total - FADE_HEIGHT) / total
        return listOf(0f to SCRIM_EDGE_ALPHA, fadeStart to SCRIM_EDGE_ALPHA, 1f to SCRIM_CLEAR_ALPHA)
    }

    /** Where the first message's top edge sits, in viewport coordinates, when scrolled to the top. */
    fun firstMessageTop(chromeHeight: Float): Float = contentTopInset(chromeHeight) + TRANSCRIPT_PADDING

    /** Whether a row whose top edge is at [rowTop] is fully clear of the header scrim. */
    fun isClearOfBackdrop(rowTop: Float, chromeHeight: Float): Boolean = rowTop >= backdropHeight(chromeHeight)

    /** Whether a row whose top edge is at [rowTop] is fully below the header. */
    fun isBelowHeader(rowTop: Float, chromeHeight: Float): Boolean = rowTop >= contentTopInset(chromeHeight)

    // Bottom (composer dock)

    /** The transcript's bottom content inset: exactly the dock height. Never negative. */
    fun contentBottomInset(dockHeight: Float): Float = maxOf(0f, dockHeight)

    /** The dock scrim's total height: the dock plus the fade's overhang above it. */
    fun dockBackdropHeight(dockHeight: Float): Float = contentBottomInset(dockHeight) + FADE_OVERHANG

    /** Dock gradient stops (fraction 0…1 down the scrim to alpha): clear at the top, full from the end of the fade. */
    fun dockScrimStops(dockHeight: Float): List<Pair<Float, Float>> {
        val total = dockBackdropHeight(dockHeight) // always ≥ FADE_OVERHANG > 0
        val fadeEnd = minOf(total, FADE_HEIGHT) / total
        return listOf(0f to SCRIM_CLEAR_ALPHA, fadeEnd to SCRIM_EDGE_ALPHA, 1f to SCRIM_EDGE_ALPHA)
    }

    /** Where the latest message's bottom edge sits, in viewport coordinates, when scrolled to the bottom. */
    fun latestMessageBottom(viewportHeight: Float, dockHeight: Float): Float =
        viewportHeight - contentBottomInset(dockHeight) - TRANSCRIPT_PADDING

    /** Whether a row whose bottom edge is at [rowBottom] is fully clear of the dock scrim. */
    fun isClearOfDockBackdrop(rowBottom: Float, viewportHeight: Float, dockHeight: Float): Boolean =
        rowBottom <= viewportHeight - dockBackdropHeight(dockHeight)

    /** Gap from the viewport bottom to jump to latest: the dock plus [ChatJumpToLatest.BOTTOM_SPACING]. */
    fun jumpButtonBottomPadding(dockHeight: Float): Float = contentBottomInset(dockHeight) + ChatJumpToLatest.BOTTOM_SPACING

    /**
     * How far the transcript's bottom edge (its bottom inset included) sits below the visible area, from the
     * scroll's `maxValue - value`. Zero at rest at the latest message, so [ChatJumpToLatest.isVisible] hides there.
     */
    fun distanceFromLatest(maxScroll: Float, scroll: Float): Float = maxScroll - scroll

    /** Opening offset (dp) of the internal evidence seam [LocalChatEdgeBackdropEvidence]. */
    const val EVIDENCE_SCROLL = 96f
}

/** The transcript's measured insets (dp): status bar + header on top, dock + navigation bar / IME below. */
@Immutable
internal data class ChatEdgeInsets(val top: Dp, val bottom: Dp)

/** High contrast on (Android 14+ contrast level above standard). False where unavailable (layoutlib, < 34). */
@Composable
internal fun rememberChatEdgeSolidBackdrop(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Build.VERSION.SDK_INT >= 34 && runCatching {
            (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.contrast?.let { it > 0f } ?: false
        }.getOrDefault(false)
    }
}

/**
 * Lays [header] (on its scrim, padded below the status bar) and [dock] (on its scrim, at the bottom; the dock
 * applies its own navigation-bar and IME padding) over [content], which fills the whole area and receives the
 * measured insets. One measure pass, so a single-frame render already has both insets (no post-layout state).
 */
@Composable
internal fun ChatEdgeOverlayLayout(
    modifier: Modifier,
    header: @Composable () -> Unit,
    dock: @Composable () -> Unit,
    content: @Composable BoxScope.(insets: ChatEdgeInsets) -> Unit,
) {
    val colors = RemColors.current
    val solid = rememberChatEdgeSolidBackdrop()
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minHeight = 0)
        val headerPlaceables = subcompose("header") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val chrome = size.height / density
                        if (solid) {
                            drawRect(colors.backgroundPrimary, size = size)
                            val hairline = ChatEdgeBackdrop.HAIRLINE.dp.toPx()
                            drawRect(colors.separator, topLeft = Offset(0f, size.height - hairline), size = Size(size.width, hairline))
                        } else {
                            val total = ChatEdgeBackdrop.backdropHeight(chrome).dp.toPx()
                            val stops = ChatEdgeBackdrop.scrimStops(chrome)
                                .map { (at, alpha) -> at to colors.backgroundPrimary.copy(alpha = alpha) }
                                .toTypedArray()
                            // Drawn past the header's bounds by the overhang; drawing is not clipped.
                            drawRect(Brush.verticalGradient(*stops, startY = 0f, endY = total), size = Size(size.width, total))
                        }
                    }
                    .statusBarsPadding(),
            ) { header() }
        }.map { it.measure(loose) }
        val dockPlaceables = subcompose("dock") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val dockHeight = size.height / density
                        if (solid) {
                            drawRect(colors.backgroundPrimary, size = size)
                            drawRect(colors.separator, size = Size(size.width, ChatEdgeBackdrop.HAIRLINE.dp.toPx()))
                        } else {
                            val total = ChatEdgeBackdrop.dockBackdropHeight(dockHeight).dp.toPx()
                            val stops = ChatEdgeBackdrop.dockScrimStops(dockHeight)
                                .map { (at, alpha) -> at to colors.backgroundPrimary.copy(alpha = alpha) }
                                .toTypedArray()
                            // Starts the overhang above the dock's top edge; drawing is not clipped.
                            val top = size.height - total
                            drawRect(
                                Brush.verticalGradient(*stops, startY = top, endY = size.height),
                                topLeft = Offset(0f, top),
                                size = Size(size.width, total),
                            )
                        }
                    },
            ) { dock() }
        }.map { it.measure(loose) }
        val chromePx = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val dockPx = dockPlaceables.maxOfOrNull { it.height } ?: 0
        val insets = ChatEdgeInsets(
            top = ChatEdgeBackdrop.contentTopInset(chromePx / density).dp,
            bottom = ChatEdgeBackdrop.contentBottomInset(dockPx / density).dp,
        )
        val bodyPlaceables = subcompose("body") {
            Box(Modifier.fillMaxSize()) { content(insets) }
        }.map { it.measure(constraints) }
        val height = constraints.maxHeight
        layout(constraints.maxWidth, height) {
            bodyPlaceables.forEach { it.place(0, 0) }
            headerPlaceables.forEach { it.place(0, 0) }
            dockPlaceables.forEach { it.place(0, height - dockPx) }
        }
    }
}
