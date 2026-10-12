package com.rem.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.buttons.RemGlassIconButton
import com.rem.designsystem.buttons.RemGlassIconButtonDefaults
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.RemGlass
import com.rem.designsystem.screens.ChatJumpToLatest
import com.rem.designsystem.screens.ChatJumpToLatestButton
import com.rem.designsystem.tokens.RemOpacity
import com.rem.designsystem.tokens.RemTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * [RemGlassIconButton] behaviour: one Button node labelled by its content description, a 44dp touch target at any
 * visual size, a working click, and a real disabled state. Composed on the Paparazzi (layoutlib) host, then read
 * from the merged semantics tree — the same tree TalkBack and `createComposeRule` see.
 */
class RemGlassIconButtonTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    /** Composes [content] and returns its merged semantics nodes plus the density, read after layout. */
    private fun mergedNodes(content: @Composable () -> Unit): Pair<List<SemanticsNode>, Float> {
        var nodes: List<SemanticsNode> = emptyList()
        var density = 0f
        paparazzi.snapshot {
            val root = LocalView.current as RootForTest
            Box(
                Modifier.onGloballyPositioned {
                    nodes = root.semanticsOwner.getAllSemanticsNodes(mergingEnabled = true)
                    density = root.density.density
                },
            ) { RemTheme { content() } }
        }
        assertTrue("semantics were read after layout", nodes.isNotEmpty())
        return nodes to density
    }

    private fun List<SemanticsNode>.labelled(label: String): List<SemanticsNode> =
        filter { it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true }

    @Test
    fun isOneButtonLabelledByItsContentDescription() {
        val (nodes, _) = mergedNodes { RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}) }
        val buttons = nodes.labelled("Menu")
        assertEquals("exactly one node carries the label", 1, buttons.size)
        val button = buttons.single()
        assertEquals(listOf("Menu"), button.config[SemanticsProperties.ContentDescription])
        assertEquals(Role.Button, button.config.getOrNull(SemanticsProperties.Role))
        assertNotNull(button.config.getOrNull(SemanticsActions.OnClick))
        assertFalse("enabled by default", button.config.contains(SemanticsProperties.Disabled))
        assertNull("the glyph adds no text", button.config.getOrNull(SemanticsProperties.Text))
    }

    @Test
    fun symbolGlyphIsNotAnnouncedAsText() {
        val (nodes, _) = mergedNodes { RemGlassIconButton(RemMaterialSymbols.Close, "Close", onClick = {}) }
        val button = nodes.labelled("Close").single()
        assertNull("the Material Symbol glyph character is not read out", button.config.getOrNull(SemanticsProperties.Text))
        assertEquals(Role.Button, button.config.getOrNull(SemanticsProperties.Role))
    }

    @Test
    fun clickInvokesOnClick() {
        var clicks = 0
        val (nodes, _) = mergedNodes { RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = { clicks++ }) }
        nodes.labelled("Menu").single().config[SemanticsActions.OnClick].action!!.invoke()
        assertEquals(1, clicks)
    }

    @Test
    fun touchTargetIsAtLeast44AtEverySize() {
        for (size in listOf(24.dp, 32.dp, 44.dp, 56.dp)) {
            val (nodes, density) = mergedNodes { RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, size = size) }
            val bounds = nodes.labelled("Menu").single().size
            val expected = maxOf(size.value, 44f)
            assertEquals("width at $size", expected, bounds.width / density, 0.5f)
            assertEquals("height at $size", expected, bounds.height / density, 0.5f)
        }
    }

    @Test
    fun disabledIsAnnouncedDisabled() {
        // `clickable(enabled = false)`: pointer input ignores taps and the node is marked Disabled, which is what
        // keeps TalkBack from activating it (the OnClick action stays declared, as on every Compose clickable).
        val (nodes, _) = mergedNodes { RemGlassIconButton(Icons.Filled.Menu, "Menu", onClick = {}, enabled = false) }
        val button = nodes.labelled("Menu").single()
        assertTrue("announced disabled", button.config.contains(SemanticsProperties.Disabled))
        assertEquals(Role.Button, button.config.getOrNull(SemanticsProperties.Role))
        assertEquals("still labelled", listOf("Menu"), button.config[SemanticsProperties.ContentDescription])
    }

    @Test
    fun jumpToLatestKeepsItsLabelTagAndTarget() {
        val (nodes, density) = mergedNodes { ChatJumpToLatestButton(onClick = {}) }
        val button = nodes.labelled(ChatJumpToLatest.ACCESSIBILITY_LABEL).single()
        assertEquals(ChatJumpToLatest.TEST_TAG, button.config.getOrNull(SemanticsProperties.TestTag))
        assertEquals(Role.Button, button.config.getOrNull(SemanticsProperties.Role))
        assertEquals(ChatJumpToLatest.DIAMETER, button.size.width / density, 0.5f)
    }

    @Test
    fun geometryAndTintComeFromTheSharedGlass() {
        assertEquals(44f, RemGlassIconButtonDefaults.Size.value, 0f)
        assertEquals(44f, RemGlassIconButtonDefaults.MinTouchTarget.value, 0f)
        assertEquals(20f, RemGlassIconButtonDefaults.iconSize(44.dp).value, 0f)
        assertEquals(ChatJumpToLatest.GLYPH_SIZE, RemGlassIconButtonDefaults.iconSize(ChatJumpToLatest.DIAMETER.dp).value, 0f)
        assertEquals(0.6f, RemOpacity.glassTint, 0f)
        assertEquals(RemOpacity.glassTint, RemGlass.TINT_ALPHA, 0f)
        assertEquals(RemGlass.TINT_ALPHA, ChatJumpToLatest.GLASS_TINT_ALPHA, 0f)
        assertEquals(20f, RemGlass.BlurRadius.value, 0f)
        assertEquals(0.5f, RemGlass.Hairline.value, 0f)
    }
}
