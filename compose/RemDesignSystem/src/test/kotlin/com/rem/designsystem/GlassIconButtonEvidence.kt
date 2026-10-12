package com.rem.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.buttons.RemGlassIconButton
import com.rem.designsystem.buttons.RemGlassIconButtonDefaults
import com.rem.designsystem.buttons.RemGlassIconButtonLayout
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.RemGlassState
import com.rem.designsystem.primitives.remGlassSource
import com.rem.designsystem.primitives.rememberRemGlassState
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * Evidence for [RemGlassIconButton] (paired by name with the SwiftUI `GlassIconButtonRenderSnapshots.swift`):
 * - `RemGlassIconButton-glass-*`: a May-style top bar (menu ☰ leading, back to chat » trailing) floating over busy,
 *   scrolled content that is the glass source, so the circles blur and tint what is behind them;
 * - `RemGlassIconButton-states-*`: the solid fallbacks (no glass source; the high-contrast fallback forced), disabled,
 *   the three glyph overloads, and sizes (a 32 circle inside the 44 touch target, the 44 default, a 56 circle).
 */
internal class GlassIconButtonEvidence(private val paparazzi: Paparazzi) {
    private fun shot(name: String, content: @Composable () -> Unit) =
        paparazzi.snapshot(name = name, composable = content)

    fun glass() {
        shot("RemGlassIconButton-glass-light") { RemTheme { glassTopBar() } }
        shot("RemGlassIconButton-glass-dark") { RemTheme(darkTheme = true) { glassTopBar() } }
    }

    fun states() {
        shot("RemGlassIconButton-states-light") { RemTheme { stateGallery() } }
        shot("RemGlassIconButton-states-dark") { RemTheme(darkTheme = true) { stateGallery() } }
    }

    @Composable
    private fun glassTopBar() {
        val colors = RemColors.current
        val glass = rememberRemGlassState()
        Column(Modifier.fillMaxSize().background(colors.backgroundPrimary)) {
            caption("Glass: top bar over a glass source (content scrolled under the bar)")
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                BusyContent(Modifier.fillMaxSize().remGlassSource(glass))
                TopBar(glass)
            }
            caption("Same bar, no glass source: the solid fallback")
            Box(Modifier.fillMaxWidth().height(260.dp)) {
                BusyContent(Modifier.fillMaxSize())
                TopBar(glassState = null)
            }
        }
    }

    /** May's two round controls on a top bar outside chat: menu (☰) leading, back to chat (») trailing. */
    @Composable
    private fun TopBar(glassState: RemGlassState?) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, glassState = glassState)
            Spacer(Modifier.weight(1f))
            RemGlassIconButton(Icons.Filled.KeyboardDoubleArrowRight, "Back to chat", onClick = {}, glassState = glassState)
        }
    }

    /** Colourful, high-frequency content so a blur (or its absence) is obvious under the circles. */
    @Composable
    private fun BusyContent(modifier: Modifier) {
        val colors = RemColors.current
        val swatches = listOf(colors.systemBlue, colors.systemOrange, colors.systemGreen, colors.systemPink, colors.systemIndigo)
        Column(modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(6) { row ->
                Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(9) { col ->
                        Box(
                            Modifier.size(36.dp).background(swatches[(row + col) % swatches.size], RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text("${row * 9 + col}", style = RemTypography.caption1Bold, color = colors.labelOnColor) }
                    }
                }
            }
        }
    }

    @Composable
    private fun stateGallery() {
        val colors = RemColors.current
        Column(Modifier.fillMaxSize().background(colors.backgroundPrimary).padding(vertical = RemSpacing.lg)) {
            caption("Solid (no glass source): enabled, disabled")
            row {
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {})
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, enabled = false)
                RemGlassIconButton(Icons.Filled.KeyboardDoubleArrowRight, "Back to chat", onClick = {})
                RemGlassIconButton(Icons.Filled.KeyboardDoubleArrowRight, "Back to chat", onClick = {}, enabled = false)
            }
            caption("High-contrast fallback forced over a glass source (solid)")
            val glass = rememberRemGlassState()
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                Box(Modifier.fillMaxSize().remGlassSource(glass).background(colors.systemBlue))
                Row(Modifier.padding(horizontal = RemSpacing.lg, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(RemSpacing.lg)) {
                    forcedSolid(enabled = true)
                    forcedSolid(enabled = false)
                }
            }
            caption("Glyph overloads: ImageVector, Painter, registry Material Symbol")
            row {
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {})
                RemGlassIconButton(rememberVectorPainter(Icons.Filled.KeyboardDoubleArrowRight), "Back to chat", onClick = {})
                RemGlassIconButton(RemMaterialSymbols.Close, "Close", onClick = {})
                RemGlassIconButton(RemMaterialSymbols.Search, "Search", onClick = {})
            }
            caption("Sizes: 32 circle (44 touch target, outlined), 44 default, 56")
            row {
                Box(Modifier.size(RemGlassIconButtonDefaults.MinTouchTarget).background(colors.fillTertiary)) {
                    RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, size = 32.dp)
                }
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {})
                RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, size = 56.dp)
            }
        }
    }

    @Composable
    private fun forcedSolid(enabled: Boolean) {
        val glass = rememberRemGlassState()
        RemGlassIconButtonLayout("Menu", {}, Modifier, enabled, RemGlassIconButtonDefaults.Size, glass.haze, solid = true) { tint, iconSize ->
            Icon(Icons.Filled.Menu, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        }
    }

    @Composable
    private fun row(content: @Composable () -> Unit) {
        Row(
            Modifier.padding(horizontal = RemSpacing.lg, vertical = RemSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }

    @Composable
    private fun caption(text: String) {
        Text(
            text,
            style = RemTypography.caption1,
            color = RemColors.current.labelSecondary,
            modifier = Modifier.padding(horizontal = RemSpacing.lg, vertical = RemSpacing.xs).width(360.dp),
        )
    }
}
