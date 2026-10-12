package com.rem.designsystem.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.rem.designsystem.icons.RemMaterialSymbol
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.RemGlassState
import com.rem.designsystem.primitives.rememberRemGlassSolidFallback
import com.rem.designsystem.primitives.remGlassSurface
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme
import dev.chrisbanes.haze.HazeState

/** Geometry for [RemGlassIconButton]. Twin of SwiftUI `RemGlassIconButton` defaults. */
object RemGlassIconButtonDefaults {
    /** Default circle diameter: the 44 minimum touch target. */
    val Size: Dp = 44.dp
    /** Touch target floor. A smaller [Size] keeps its visual circle but is still 44 to the finger. */
    val MinTouchTarget: Dp = 44.dp
    /** The glyph edge for a circle of [size]: 20 in the default 44 circle, in proportion otherwise. */
    fun iconSize(size: Dp): Dp = size * 20f / 44f
}

/**
 * **RemGlassIconButton** — a round, icon-only button drawn in glass: Android's twin of an iOS 26 system glass
 * button (SwiftUI `RemGlassIconButton`, `.glassEffect(.regular.interactive())`). Use it for chrome that floats over
 * content: a top bar's menu or back-to-chat control, a jump-to-latest affordance.
 *
 * With [glassState] (the same state the content behind it marks with
 * [remGlassSource][com.rem.designsystem.primitives.remGlassSource]) the circle blurs that content on API 32+.
 * Without one, below API 32, or when the system asks for more contrast, the circle is solid `backgroundTertiary`
 * with the same `separator` hairline and shadow, so the glyph keeps its contrast. See
 * [RemGlass][com.rem.designsystem.primitives.RemGlass].
 *
 * Always a 44dp touch target (a smaller [size] keeps its visual circle centred in the 44 target), role Button,
 * labelled by [contentDescription], ripple inside the circle. Disabled: not clickable, announced disabled, glyph in
 * `labelTertiary`; the surface stays the same.
 */
@Composable
fun RemGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = RemGlassIconButtonDefaults.Size,
    glassState: RemGlassState? = null,
) = RemGlassIconButtonLayout(contentDescription, onClick, modifier, enabled, size, glassState?.haze, rememberRemGlassSolidFallback()) { tint, iconSize ->
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
}

/** [RemGlassIconButton] with a [Painter] glyph (a product's own vector or raster). The painter is tinted. */
@Composable
fun RemGlassIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = RemGlassIconButtonDefaults.Size,
    glassState: RemGlassState? = null,
) = RemGlassIconButtonLayout(contentDescription, onClick, modifier, enabled, size, glassState?.haze, rememberRemGlassSolidFallback()) { tint, iconSize ->
    Icon(painter, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
}

/** [RemGlassIconButton] with a registry Material Symbol glyph (`docs/contracts/icon-registry.md`), at its pinned FILL. */
@Composable
fun RemGlassIconButton(
    symbol: RemMaterialSymbol,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = RemGlassIconButtonDefaults.Size,
    glassState: RemGlassState? = null,
) = RemGlassIconButtonLayout(contentDescription, onClick, modifier, enabled, size, glassState?.haze, rememberRemGlassSolidFallback()) { tint, iconSize ->
    // Fixed chrome: the glyph does not grow with font scale (like the vector overloads).
    val fontSize = with(LocalDensity.current) { iconSize.toSp() }
    Text(symbol.glyph, fontFamily = RemMaterialSymbols.family(symbol), fontSize = fontSize, color = tint)
}

/**
 * The shared layout. [haze] is the backdrop (null for the solid surface); [solid] forces the solid surface (the
 * high-contrast fallback, also used by evidence renders). Internal so Chat's jump to latest can pass its own
 * backdrop and tests can force the fallback.
 */
@Composable
internal fun RemGlassIconButtonLayout(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    size: Dp,
    haze: HazeState?,
    solid: Boolean,
    glyph: @Composable (tint: Color, iconSize: Dp) -> Unit,
) {
    val colors = RemColors.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(max(size, RemGlassIconButtonDefaults.MinTouchTarget))
            .clickable(
                interactionSource = interaction,
                // Centred in the circle and no larger than it, also when the touch target is bigger.
                indication = ripple(bounded = false, radius = size / 2),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .remGlassSurface(CircleShape, colors, haze, solid)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            glyph(if (enabled) colors.labelPrimary else colors.labelTertiary, RemGlassIconButtonDefaults.iconSize(size))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RemGlassIconButtonPreview() {
    Row {
        RemTheme {
            Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {})
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, enabled = false)
            }
        }
        RemTheme(darkTheme = true) {
            Row(
                Modifier.background(RemColors.current.backgroundPrimary).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {})
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, enabled = false)
            }
        }
    }
}
