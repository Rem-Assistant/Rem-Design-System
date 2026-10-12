package com.rem.designsystem

import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.rem.designsystem.tokens.Inter
import com.rem.designsystem.tokens.RemMaterialTypography
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Inside [RemTheme], text that names no Rem style is still Inter: a bare `Text`, Material 3 widgets that read
 * `MaterialTheme.typography` (DropdownMenuItem), and `TextField`. Styles are captured from the real text layout
 * (`onTextLayout`), i.e. what was measured and drawn, on the Paparazzi (layoutlib) host.
 */
class RemThemeTypographyTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private fun render(content: @Composable () -> Unit) = paparazzi.snapshot { RemTheme { Column { content() } } }

    @Test
    fun bareTextIsInterBody() {
        var style: TextStyle? = null
        render { Text("Plain text", onTextLayout = { style = it.layoutInput.style }) }
        assertEquals(Inter, style!!.fontFamily)
        assertEquals(RemTypography.body.fontSize, style!!.fontSize)
    }

    @Test
    fun dropdownMenuItemIsInter() {
        var style: TextStyle? = null
        render {
            DropdownMenuItem(text = { Text("Rename", onTextLayout = { style = it.layoutInput.style }) }, onClick = {})
        }
        assertEquals(Inter, style!!.fontFamily)
        // Material's own labelLarge metrics (14sp Medium): only the face changes.
        assertEquals(Typography().labelLarge.fontSize, style!!.fontSize)
        assertEquals(FontWeight.Medium, style!!.fontWeight)
    }

    @Test
    fun textFieldDefaultStyleIsInter() {
        var fieldStyle: TextStyle? = null
        render {
            fieldStyle = LocalTextStyle.current
            TextField(value = "Draft", onValueChange = {})
        }
        assertEquals(Inter, fieldStyle!!.fontFamily)
    }

    @Test
    fun everyMaterialSlotIsInter() {
        var installed: Typography? = null
        render { installed = MaterialTheme.typography }
        assertSame(RemMaterialTypography, installed)
        with(RemMaterialTypography) {
            listOf(
                displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall,
            ).zip(Typography().run {
                listOf(
                    displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
                    titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall,
                )
            }).forEachIndexed { index, (slot, material) ->
                assertEquals("slot $index is Inter", Inter, slot.fontFamily)
                assertEquals("slot $index keeps Material's metrics", material.copy(fontFamily = Inter), slot)
            }
        }
    }

    @Test
    fun outsideRemThemeMaterialStaysRoboto() {
        // Guard on the test itself: without RemTheme, Material's default is not Inter.
        var style: TextStyle? = null
        paparazzi.snapshot { DropdownMenuItem(text = { Text("Rename", onTextLayout = { style = it.layoutInput.style }) }, onClick = {}) }
        assertEquals(FontFamily.SansSerif, style!!.fontFamily)
    }

    /**
     * Regular (400), Medium (500), SemiBold (600) and Bold (700) are bundled: each resolves to its own face, and the
     * weights in between take the nearest one (100–300 Regular, 800–900 Bold). The typeface's weight is read from the
     * resolved font file (OS/2 usWeightClass), so a SemiBold request that fell back to Bold would fail here.
     */
    @Test
    fun weightsResolveToTheirOwnFaceOrTheNearestBundledOne() {
        val expected = mapOf(100 to 400, 300 to 400, 400 to 400, 500 to 500, 600 to 600, 700 to 700, 800 to 700, 900 to 700)
        val resolved = mutableMapOf<Int, Typeface>()
        render {
            val resolver = LocalFontFamilyResolver.current
            for (weight in expected.keys) resolved[weight] = resolver.resolve(Inter, FontWeight(weight)).value as Typeface
        }
        for ((weight, face) in expected) {
            val typeface = resolved.getValue(weight)
            assertEquals("weight $weight renders the $face face", face, typeface.weight)
        }
        assertTrue("Medium and SemiBold are distinct faces", resolved.getValue(500) !== resolved.getValue(600))
    }
}
