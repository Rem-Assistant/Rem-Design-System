package com.rem.designsystem.primitives

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColorScheme
import com.rem.designsystem.tokens.RemOpacity
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/**
 * **Glass** — Rem's Android stand-in for iOS 26 Liquid Glass on small floating controls ([RemGlassIconButton][com.rem.designsystem.buttons.RemGlassIconButton],
 * Chat's jump to latest). A control drawn with glass blurs whatever a **glass source** draws behind it
 * ([Haze](https://github.com/chrisbanes/haze) 1.1.1, Apache-2.0: `RenderEffect` on API 32+), tinted with
 * `backgroundTertiary` at `RemOpacity.glassTint`, with a 0.5dp `separator` hairline and a soft shadow.
 *
 * It falls back to a **solid** `backgroundTertiary` fill (same hairline and shadow, so the edge stays visible and
 * the glyph keeps its contrast) when:
 * - there is no glass source (`glassState = null`), e.g. a flat top bar or a catalog row;
 * - the device is below API 32 (Haze draws its fallback tint, which is the same solid fill);
 * - the system asks for more contrast (Android 14+ contrast level above standard). Android has no
 *   reduce-transparency setting; this is the nearest system signal and matches the Chat edge backdrops.
 *
 * Usage: create one state per screen region, mark the scrolling content as the source, and pass the state to
 * the controls that float over it. The controls must **not** be inside the source (they are siblings drawn on
 * top), or Haze would blur them into themselves.
 *
 * ```
 * val glass = rememberRemGlassState()
 * Box {
 *     LazyColumn(Modifier.fillMaxSize().remGlassSource(glass)) { … }
 *     RemGlassIconButton(Icons.Filled.Menu, "Menu", onMenu, Modifier.align(Alignment.TopStart), glassState = glass)
 * }
 * ```
 *
 * Haze stays an implementation detail: consumers hold a [RemGlassState], never a `HazeState`, so they need no
 * Haze dependency of their own and the library can move Haze versions without breaking them.
 */
object RemGlass {
    /** Backdrop blur radius (API 32+). Same as the Chat edge backdrops. */
    val BlurRadius: Dp = 20.dp
    /** Shadow under a glass control: soft, so it lifts off content without reading as a Material FAB. */
    val ShadowElevation: Dp = 4.dp
    /** Hairline stroke around a glass control, `separator`. Keeps the edge visible on matching backgrounds. */
    val Hairline: Dp = 0.5.dp
    /** Alpha of the `backgroundTertiary` tint over the blur (token `opacity.glassTint`). */
    const val TINT_ALPHA: Float = RemOpacity.glassTint
}

/**
 * The backdrop a glass control blurs. Create it with [rememberRemGlassState], mark the content behind the controls
 * with [Modifier.remGlassSource], and pass it to each glass control drawn over that content.
 */
@Stable
class RemGlassState internal constructor() {
    internal val haze = HazeState()
}

/** One [RemGlassState] for a screen region, kept across recompositions. */
@Composable
fun rememberRemGlassState(): RemGlassState = remember { RemGlassState() }

/**
 * Marks this content as the backdrop for glass controls that use [state]. Apply it to the scrolling content (or
 * the whole background) behind the controls, not to an ancestor of the controls themselves.
 */
fun Modifier.remGlassSource(state: RemGlassState): Modifier = haze(state.haze)

/**
 * Whether glass should draw solid because the system asks for more contrast (Android 14+ contrast level above
 * standard). False where unavailable (below API 34, layoutlib).
 */
@Composable
internal fun rememberRemGlassSolidFallback(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Build.VERSION.SDK_INT >= 34 && runCatching {
            (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.contrast?.let { it > 0f } ?: false
        }.getOrDefault(false)
    }
}

/**
 * The glass surface for a control of [shape]: shadow, then the blurred and tinted backdrop from [haze] (solid
 * `backgroundTertiary` when [haze] is null or [solid]; the same solid fill below API 32), then the hairline, then a
 * clip so content and ripples stay inside the shape.
 */
internal fun Modifier.remGlassSurface(shape: Shape, colors: RemColorScheme, haze: HazeState?, solid: Boolean): Modifier =
    shadow(RemGlass.ShadowElevation, shape, clip = false)
        .then(
            if (haze == null || solid) Modifier.background(colors.backgroundTertiary, shape)
            else Modifier.clip(shape).hazeChild(
                haze,
                HazeStyle(
                    backgroundColor = colors.backgroundPrimary,
                    tint = HazeTint(colors.backgroundTertiary.copy(alpha = RemGlass.TINT_ALPHA)),
                    blurRadius = RemGlass.BlurRadius,
                    noiseFactor = 0f,
                    // Below API 32: the solid control.
                    fallbackTint = HazeTint(colors.backgroundTertiary),
                ),
            ),
        )
        .border(RemGlass.Hairline, colors.separator, shape)
        .clip(shape)
