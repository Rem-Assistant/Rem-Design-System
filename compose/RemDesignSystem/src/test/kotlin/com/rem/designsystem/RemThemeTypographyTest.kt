package com.rem.designsystem

import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
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
        // labelLarge on the Rem ramp: menu items are body, as on iOS.
        assertEquals(RemTypography.body.fontSize, style!!.fontSize)
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
        var installed: androidx.compose.material3.Typography? = null
        render { installed = MaterialTheme.typography }
        assertSame(RemMaterialTypography, installed)
        with(RemMaterialTypography) {
            listOf(
                displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, labelLarge, labelMedium, labelSmall,
            ).forEachIndexed { index, slot -> assertEquals("slot $index", Inter, slot.fontFamily) }
        }
    }

    @Test
    fun outsideRemThemeMaterialStaysRoboto() {
        // Guard on the test itself: without RemTheme, Material's default is not Inter.
        var style: TextStyle? = null
        paparazzi.snapshot { DropdownMenuItem(text = { Text("Rename", onTextLayout = { style = it.layoutInput.style }) }, onClick = {}) }
        assertEquals(FontFamily.SansSerif, style!!.fontFamily)
    }

    /** Only Regular (400) and Bold (700) are bundled: 100 to 500 resolve to Regular, 600 to 900 to Bold. */
    @Test
    fun inBetweenWeightsResolveToTheNearestBundledFace() {
        val resolved = mutableMapOf<Int, Typeface>()
        render {
            val resolver = LocalFontFamilyResolver.current
            for (weight in listOf(300, 400, 500, 600, 700, 900)) {
                resolved[weight] = resolver.resolve(Inter, FontWeight(weight)).value as Typeface
            }
        }
        val regular = resolved.getValue(400)
        val bold = resolved.getValue(700)
        assertNotNull(regular)
        assertEquals(400, regular.weight)
        assertEquals(700, bold.weight)
        for (w in listOf(300, 500)) assertEquals("weight $w renders Regular", 400, resolved.getValue(w).weight)
        for (w in listOf(600, 900)) assertEquals("weight $w renders Bold", 700, resolved.getValue(w).weight)
    }
}
