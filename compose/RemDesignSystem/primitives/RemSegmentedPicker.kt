package com.rem.designsystem.primitives

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** Which renderer [RemSegmentedPicker] uses. */
enum class RemSegmentedPickerStyle {
    /**
     * The iOS segmented control look (the parity alternate): a filled capsule track, a raised thumb that
     * slides to the selected segment, and no checkmark. Token-driven; see [RemSegmentedPickerTokens].
     */
    Ios,

    /**
     * Material 3 single-choice segmented buttons (outlined, with a checkmark) — the Android-native form
     * Rem's own sheets keep by default (`docs/design-reconciliation/2026-10-03-settings-onboarding.md`).
     */
    Material,
}

/**
 * Metrics and colours of the [RemSegmentedPickerStyle.Ios] renderer, all resolved from Rem tokens.
 * Track: `fillTertiary` (iOS `tertiarySystemFill`). Thumb: `backgroundTertiary` (white in light; the dark
 * value `#2C2C2E` is the nearest token to iOS's dark thumb, which has no Rem token). Labels: footnote
 * (13), semibold when selected, `labelPrimary`.
 */
object RemSegmentedPickerTokens {
    val Height = 32.dp
    val TrackInset = 2.dp
    val ThumbElevation = 3.dp
    const val ThumbShadowAlpha = 0.12f
    const val PressedAlpha = 0.5f
    const val DisabledAlpha = 0.4f
    val LabelPadding = RemSpacing.sm
}

/**
 * **RemSegmentedPicker** — a single-choice segmented picker, the Compose sibling of SwiftUI's
 * `Picker(...).pickerStyle(.segmented)`. With [style] = [RemSegmentedPickerStyle.Ios] (the default) it
 * reproduces the iOS segmented control: a filled capsule track, a raised thumb sliding to the selected
 * segment, no checkmark. [RemSegmentedPickerStyle.Material] renders Material 3 segmented buttons instead.
 *
 * Accessibility: the row is a selectable group; each segment is a [Role.Tab] carrying `selected`
 * semantics and its label. [tag] tags each segment (e.g. `agendaCreate.mode.Task`), so tests select and
 * assert segments the same way under either style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> RemSegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    tag: ((T) -> String)? = null,
    enabled: Boolean = true,
    style: RemSegmentedPickerStyle = RemSegmentedPickerStyle.Ios,
) {
    when (style) {
        RemSegmentedPickerStyle.Material -> SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    enabled = enabled,
                    modifier = if (tag != null) Modifier.testTag(tag(option)) else Modifier,
                    label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        RemSegmentedPickerStyle.Ios -> IosSegmentedPicker(options, selected, onSelect, label, modifier, tag, enabled)
    }
}

@Composable
private fun <T> IosSegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier,
    tag: ((T) -> String)?,
    enabled: Boolean,
) {
    if (options.isEmpty()) return
    val colors = RemColors.current
    val selectedIndex = options.indexOf(selected)
    // The thumb slides between segments; snapped on first composition (and so in previews / snapshots).
    val thumbPosition by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "RemSegmentedPicker.thumb",
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(RemSegmentedPickerTokens.Height)
            .alpha(if (enabled) 1f else RemSegmentedPickerTokens.DisabledAlpha)
            .clip(CircleShape)
            .background(colors.fillTertiary)
            .padding(RemSegmentedPickerTokens.TrackInset),
    ) {
        val segmentWidth = maxWidth / options.size
        if (selectedIndex >= 0) {
            Box(
                Modifier
                    .offset(x = segmentWidth * thumbPosition)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .shadow(
                        RemSegmentedPickerTokens.ThumbElevation,
                        CircleShape,
                        ambientColor = Color.Black.copy(alpha = RemSegmentedPickerTokens.ThumbShadowAlpha),
                        spotColor = Color.Black.copy(alpha = RemSegmentedPickerTokens.ThumbShadowAlpha),
                    )
                    .background(colors.backgroundTertiary, CircleShape),
            )
        }
        Row(Modifier.fillMaxSize().selectableGroup()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.Tab,
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(option) },
                        )
                        .then(if (tag != null) Modifier.testTag(tag(option)) else Modifier)
                        .padding(horizontal = RemSegmentedPickerTokens.LabelPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = RemTypography.footnote.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = colors.labelPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // iOS dims an unselected segment's label while it is pressed.
                        modifier = Modifier.alpha(if (pressed && !isSelected) RemSegmentedPickerTokens.PressedAlpha else 1f),
                    )
                }
            }
        }
    }
}

@Preview(name = "RemSegmentedPicker", showBackground = true, widthDp = 402)
@Composable
private fun RemSegmentedPickerPreview() {
    RemTheme {
        var mode by remember { mutableStateOf("New Task") }
        var filter by remember { mutableStateOf("All") }
        Column(
            Modifier.background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            RemSegmentedPicker(listOf("New Task", "New Event"), mode, { mode = it }, { it })
            RemSegmentedPicker(listOf("All", "Inbox", "Overdue"), filter, { filter = it }, { it })
            RemSegmentedPicker(listOf("All", "Inbox", "Overdue"), filter, { filter = it }, { it },
                style = RemSegmentedPickerStyle.Material)
        }
    }
}
