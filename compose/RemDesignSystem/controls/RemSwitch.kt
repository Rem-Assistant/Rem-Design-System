package com.rem.designsystem.controls

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rem.designsystem.tokens.RemColors

/**
 * The design system's on/off **Switch** — the Android sibling of `Controls/RemSwitch.swift`. **Thin**:
 * it wraps the native Material 3 [Switch] (inheriting its shape, motion, and accessibility) and pins
 * the checked track to the `systemGreen` token so it reads as the same control as the iOS switch. The
 * cross-platform contract (SPEC) is shared intent + tokens, native form. Figma canonical:
 * **Switch** (`110:50`).
 *
 * Pure and state-driven, matching the design-system boundary: it renders the [checked] value it is
 * handed and reports changes through [onCheckedChange]; it never owns the value. [enabled] mirrors the
 * host lifecycle (a row locks while its screen is saving) and, when false, drops the change callback so
 * the control is genuinely non-interactive.
 */
@Composable
fun RemSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = RemColors.current
    Switch(
        checked = checked,
        onCheckedChange = if (enabled) onCheckedChange else null,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = colors.systemGreen,
            checkedBorderColor = colors.systemGreen,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = colors.fillTertiary,
            uncheckedBorderColor = colors.fillTertiary,
            // Locked state (a row is non-interactive while the screen is saving/saved). Material's
            // default disabled colors wash the checked track out to a near-invisible gray; keep the
            // green readable at a dimmed alpha so the locked-on switch stays legible — matching the
            // native-iOS "dimmed but still green" `Toggle` disabled treatment.
            disabledCheckedThumbColor = Color.White,
            disabledCheckedTrackColor = colors.systemGreen.copy(alpha = 0.5f),
            disabledCheckedBorderColor = colors.systemGreen.copy(alpha = 0.5f),
            disabledUncheckedThumbColor = Color.White,
            disabledUncheckedTrackColor = colors.fillTertiary.copy(alpha = 0.5f),
            disabledUncheckedBorderColor = colors.fillTertiary.copy(alpha = 0.5f),
        ),
    )
}
