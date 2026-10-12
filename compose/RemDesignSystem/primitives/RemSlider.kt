package com.rem.designsystem.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme

/**
 * **RemSlider** — Compose sibling of the SwiftUI [RemSlider]. A thin wrapper over the platform-native
 * Material [Slider], tinted to brandBlue. Native controls are wrapped, never hand-drawn. The Figma
 * counterpart wraps the forked iOS 26 / Material 3 kit sliders (plain: no symbols/ticks).
 */
@Composable
fun RemSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val colors = RemColors.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = colors.brandBlue,
            activeTrackColor = colors.brandBlue,
            inactiveTrackColor = colors.fillTertiary,
        ),
    )
}

@Preview(name = "RemSlider", showBackground = true, widthDp = 320)
@Composable
private fun RemSliderPreview() {
    RemTheme {
        var speed by remember { mutableFloatStateOf(0.45f) }
        var consistency by remember { mutableFloatStateOf(0.7f) }
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RemSlider(value = speed, onValueChange = { speed = it })
            RemSlider(value = consistency, onValueChange = { consistency = it })
        }
    }
}
