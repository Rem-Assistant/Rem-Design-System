package com.rem.designsystem.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.RemGlassState
import com.rem.designsystem.primitives.remGlassSource
import com.rem.designsystem.primitives.rememberRemGlassSolidFallback
import com.rem.designsystem.primitives.rememberRemGlassState
import com.rem.designsystem.tokens.RemColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Edge backdrops — twin of SwiftUI `ChatEdgeBackdrop`: the full Chat composition draws the transcript **under**
 * the header and under the composer dock (IME-docked included), so messages scroll behind both instead of
 * clipping at solid bands. Each edge is a backdrop blur of the transcript ([Haze](https://github.com/chrisbanes/haze),
 * API 32+, `RenderEffect` through a `GraphicsLayer`) tinted with `backgroundPrimary` at [BLUR_TINT_ALPHA], masked
 * by a vertical gradient: full at the screen edge (status bar + header; dock + navigation bar / IME), clear just
 * past the chrome's inner edge. Below API 32 Haze draws the same mask as a `backgroundPrimary` scrim at
 * [SCRIM_EDGE_ALPHA]. With the system's high-contrast setting (Android 14+ contrast level above standard) each
 * edge is solid `backgroundPrimary` with a hairline on its inner edge (`rememberRemGlassSolidFallback`, shared with
 * the glass controls); Android has no reduce-transparency setting. The transcript is a public `remGlassSource`, so
 * the edges and [ChatJumpToLatestButton] use the same backdrop machinery as `RemGlassIconButton`. The composition
 * owns it; hosts wire nothing. Internal (no Code Connect mapping). Contract:
 * `docs/contracts/chat.md` (Edge backdrops rule).
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
    /** Backdrop blur radius (API 32+). */
    const val BLUR_RADIUS = 20f
    /** `backgroundPrimary` tint over the blur (API 32+), so chrome stays legible over busy content. */
    const val BLUR_TINT_ALPHA = 0.7f

    // Top (header)

    /** The transcript's top content inset: exactly the chrome height. Never negative. */
    fun contentTopInset(chromeHeight: Float): Float = maxOf(0f, chromeHeight)

    /** The header scrim's total height: the chrome plus the fade's overhang below the header. */
    fun backdropHeight(chromeHeight: Float): Float = contentTopInset(chromeHeight) + FADE_OVERHANG

    /** Header mask stops (fraction 0…1 down the backdrop to opacity): 1 to the start of the fade, then 0. */
    fun maskStops(chromeHeight: Float): List<Pair<Float, Float>> {
        val total = backdropHeight(chromeHeight) // always ≥ FADE_OVERHANG > 0
        val fadeStart = maxOf(0f, total - FADE_HEIGHT) / total
        return listOf(0f to 1f, fadeStart to 1f, 1f to 0f)
    }

    /** Header scrim stops (below API 32): the mask at [SCRIM_EDGE_ALPHA]. */
    fun scrimStops(chromeHeight: Float): List<Pair<Float, Float>> =
        maskStops(chromeHeight).map { (at, opacity) -> at to scrimAlpha(opacity) }

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

    /** Dock mask stops (fraction 0…1 down the backdrop to opacity): 0 at the top, 1 from the end of the fade. */
    fun dockMaskStops(dockHeight: Float): List<Pair<Float, Float>> {
        val total = dockBackdropHeight(dockHeight) // always ≥ FADE_OVERHANG > 0
        val fadeEnd = minOf(total, FADE_HEIGHT) / total
        return listOf(0f to 0f, fadeEnd to 1f, 1f to 1f)
    }

    /** Dock scrim stops (below API 32): the mask at [SCRIM_EDGE_ALPHA]. */
    fun dockScrimStops(dockHeight: Float): List<Pair<Float, Float>> =
        dockMaskStops(dockHeight).map { (at, opacity) -> at to scrimAlpha(opacity) }

    private fun scrimAlpha(opacity: Float): Float = SCRIM_CLEAR_ALPHA + (SCRIM_EDGE_ALPHA - SCRIM_CLEAR_ALPHA) * opacity

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

/**
 * The transcript's glass source, for glass controls drawn over it ([ChatJumpToLatestButton]); null outside the
 * composition or when the solid (high-contrast) fallback is on.
 */
internal val LocalChatEdgeHaze = staticCompositionLocalOf<RemGlassState?> { null }

/**
 * Lays [header] (padded below the status bar) and [dock] (at the bottom; the dock applies its own navigation-bar
 * and IME padding) over [content], which fills the whole area, is the blur source, and receives the measured
 * insets. [overlay] (jump to latest) sits between the transcript and the chrome and may use [LocalChatEdgeHaze].
 * Each edge's backdrop is its own layer behind the chrome, sized to the chrome plus the fade's overhang. One
 * measure pass, so a single-frame render already has both insets (no post-layout state).
 */
@Composable
internal fun ChatEdgeOverlayLayout(
    modifier: Modifier,
    header: @Composable () -> Unit,
    dock: @Composable () -> Unit,
    overlay: @Composable BoxScope.(insets: ChatEdgeInsets) -> Unit,
    content: @Composable BoxScope.(insets: ChatEdgeInsets) -> Unit,
) {
    val solid = rememberRemGlassSolidFallback()
    val glass = rememberRemGlassState()
    val haze = glass.haze
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minHeight = 0)
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val headerPlaceables = subcompose("header") {
            Box(Modifier.fillMaxWidth().statusBarsPadding()) { header() }
        }.map { it.measure(loose) }
        val dockPlaceables = subcompose("dock") {
            Box(Modifier.fillMaxWidth()) { dock() }
        }.map { it.measure(loose) }
        val chromePx = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val dockPx = dockPlaceables.maxOfOrNull { it.height } ?: 0
        val chrome = chromePx / density
        val dockHeight = dockPx / density
        val insets = ChatEdgeInsets(
            top = ChatEdgeBackdrop.contentTopInset(chrome).dp,
            bottom = ChatEdgeBackdrop.contentBottomInset(dockHeight).dp,
        )
        // The solid fallback covers the chrome only; the blur / scrim adds the fade's overhang.
        val headerBackdropPx = if (solid) chromePx else ChatEdgeBackdrop.backdropHeight(chrome).dp.roundToPx()
        val dockBackdropPx = if (solid) dockPx else ChatEdgeBackdrop.dockBackdropHeight(dockHeight).dp.roundToPx()
        val headerBackdrop = subcompose("headerBackdrop") {
            ChatEdgeBackdropLayer(top = true, extent = chrome, solid = solid, haze = haze)
        }.map { it.measure(Constraints.fixed(width, headerBackdropPx)) }
        val dockBackdrop = subcompose("dockBackdrop") {
            ChatEdgeBackdropLayer(top = false, extent = dockHeight, solid = solid, haze = haze)
        }.map { it.measure(Constraints.fixed(width, dockBackdropPx)) }
        val bodyPlaceables = subcompose("body") {
            Box(Modifier.fillMaxSize().remGlassSource(glass)) { content(insets) }
        }.map { it.measure(constraints) }
        val overlayPlaceables = subcompose("overlay") {
            CompositionLocalProvider(LocalChatEdgeHaze provides if (solid) null else glass) {
                Box(Modifier.fillMaxSize()) { overlay(insets) }
            }
        }.map { it.measure(constraints) }
        layout(width, height) {
            bodyPlaceables.forEach { it.place(0, 0) }
            overlayPlaceables.forEach { it.place(0, 0) }
            headerBackdrop.forEach { it.place(0, 0) }
            headerPlaceables.forEach { it.place(0, 0) }
            dockBackdrop.forEach { it.place(0, height - dockBackdropPx) }
            dockPlaceables.forEach { it.place(0, height - dockPx) }
        }
    }
}

/**
 * One edge's backdrop, filling its layer: the Haze blur under a gradient mask (scrim fallback below API 32), or
 * solid `backgroundPrimary` with a hairline on the inner edge. [extent] is the chrome or dock height (dp).
 */
@Composable
private fun ChatEdgeBackdropLayer(top: Boolean, extent: Float, solid: Boolean, haze: HazeState) {
    val colors = RemColors.current
    if (solid) {
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(colors.backgroundPrimary)
                val hairline = ChatEdgeBackdrop.HAIRLINE.dp.toPx()
                drawRect(
                    colors.separator,
                    topLeft = Offset(0f, if (top) size.height - hairline else 0f),
                    size = Size(size.width, hairline),
                )
            },
        )
        return
    }
    val stops = if (top) ChatEdgeBackdrop.maskStops(extent) else ChatEdgeBackdrop.dockMaskStops(extent)
    val mask = Brush.verticalGradient(*stops.map { (at, opacity) -> at to Color.Black.copy(alpha = opacity) }.toTypedArray())
    val style = HazeStyle(
        backgroundColor = colors.backgroundPrimary,
        tint = HazeTint(colors.backgroundPrimary.copy(alpha = ChatEdgeBackdrop.BLUR_TINT_ALPHA)),
        blurRadius = ChatEdgeBackdrop.BLUR_RADIUS.dp,
        noiseFactor = 0f,
        // Below API 32: the scrim, drawn through the same mask (tint alpha × mask alpha).
        fallbackTint = HazeTint(colors.backgroundPrimary.copy(alpha = ChatEdgeBackdrop.SCRIM_EDGE_ALPHA)),
    )
    Box(Modifier.fillMaxSize().hazeChild(haze, style) { this.mask = mask })
}
