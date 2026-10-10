package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** One option a [PollCard] offers. [id] is host data reported back through `onSelect`. */
data class PollOption(val id: String, val label: String)

/**
 * Figma property `Purpose` on **PollCard** (`2559:1524`). Purpose does not change the visuals; it keeps
 * the meaning explicit: a Choice records a decision, a Suggestion starts only that bounded step.
 */
enum class PollPurpose(val figmaName: String) { Choice("Choice"), Suggestion("Suggestion") }

/** An option paired with its stable list-order marker (A, B, C…). */
data class PollLetteredOption(val marker: String, val option: PollOption)

/** Figma property `State` on **PollCard**, resolved from the host's selection. */
sealed interface PollCardState {
    val figmaName: String

    /** No recorded selection: every option is offered. */
    data object Awaiting : PollCardState { override val figmaName = "Awaiting" }

    /** The selected option, with its original marker preserved. */
    data class Answered(val selected: PollLetteredOption) : PollCardState { override val figmaName = "Answered" }
}

/** Pure display rules for [PollCard], shared with the SwiftUI `PollCardModel`. */
object PollCardModel {
    /**
     * The marker for the option at [index] in list order: A…Z, then AA, AB… (bijective base 26).
     * Markers are identifiers, not outcome states.
     */
    fun marker(index: Int): String {
        require(index >= 0) { "Poll option index must not be negative" }
        var n = index + 1
        val letters = StringBuilder()
        while (n > 0) {
            letters.append('A' + (n - 1) % 26)
            n = (n - 1) / 26
        }
        return letters.reverse().toString()
    }

    /** Every option with its list-order marker. */
    fun lettered(options: List<PollOption>): List<PollLetteredOption> =
        options.mapIndexed { index, option -> PollLetteredOption(marker(index), option) }

    /**
     * [PollCardState.Answered] only when [selection] names one of [options]; an unknown id stays
     * [PollCardState.Awaiting] rather than inventing a selected row.
     */
    fun state(options: List<PollOption>, selection: String?): PollCardState =
        lettered(options).firstOrNull { selection != null && it.option.id == selection }
            ?.let { PollCardState.Answered(it) }
            ?: PollCardState.Awaiting
}

/** Figma master width; the card fills narrower rows. */
val PollCardMaxWidth = 330.dp

/** Fixed marker column (Figma 20dp). */
val PollCardMarkerWidth = 20.dp

/**
 * **PollCard** — Compose sibling of the SwiftUI `PollCard`: a question with a small set of options that
 * keeps the user's answer visible. Awaiting lists every option as a clickable row; Answered shows only the
 * selected row with its original marker and an independent green check. Selecting records a choice; it is
 * not success of any external action and grants no standing permission.
 *
 * Options carry stable alphabetical markers in list order. The Figma master and its specimens only have
 * two options (A/B); **there is no verified three-option (C) master**. With three or more options the same
 * A/B row pattern is extended to C, D… as requested by the product owner. Presentation only: [selection]
 * comes from the host and [onSelect] reports the option id.
 *
 * Figma: **PollCard** (`2559:1524`; Choice/Suggestion × Awaiting/Answered: `458:56`, `2559:1504`,
 * `2559:1510`, `2559:1516`).
 */
@Composable
fun PollCard(
    question: String,
    options: List<PollOption>,
    modifier: Modifier = Modifier,
    purpose: PollPurpose = PollPurpose.Choice,
    selection: String? = null,
    accessibilityPrefix: String = "pollCard",
    onSelect: (String) -> Unit = {},
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .widthIn(max = PollCardMaxWidth)
            .fillMaxWidth()
            .background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.large))
            .padding(RemSpacing.md)
            .testTag(accessibilityPrefix),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(
            question,
            style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.labelPrimary,
            modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("$accessibilityPrefix.question"),
        )
        when (val state = PollCardModel.state(options, selection)) {
            PollCardState.Awaiting -> PollCardModel.lettered(options).forEach { item ->
                val hint = if (purpose == PollPurpose.Choice) "Records this choice" else "Starts this step"
                PollRow(
                    item, selected = false,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClickLabel = hint) { onSelect(item.option.id) }
                        .clearAndSetSemantics { contentDescription = "${item.marker}, ${item.option.label}" }
                        .testTag("$accessibilityPrefix.option.${item.marker}"),
                )
            }
            is PollCardState.Answered -> PollRow(
                state.selected, selected = true,
                modifier = Modifier
                    .clearAndSetSemantics {
                        contentDescription = "${state.selected.marker}, ${state.selected.option.label}"
                        selected = true
                    }
                    .testTag("$accessibilityPrefix.selected.${state.selected.marker}"),
            )
        }
    }
}

@Composable
private fun PollRow(item: PollLetteredOption, selected: Boolean, modifier: Modifier) {
    val colors = RemColors.current
    val shape = RoundedCornerShape(RemRadius.small)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.backgroundPrimary, shape)
            .then(modifier)
            .padding(RemSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(item.marker, style = RemTypography.subheadline, color = colors.labelPrimary,
            textAlign = TextAlign.Center, modifier = Modifier.width(PollCardMarkerWidth))
        Text(item.option.label, style = RemTypography.subheadline, color = colors.labelPrimary, modifier = Modifier.weight(1f))
        if (selected) Text("✓", style = RemTypography.subheadline, color = colors.systemGreen)
    }
}

@Preview(name = "PollCard", showBackground = true, widthDp = 402)
@Composable
private fun PollCardPreview() {
    val options = listOf(
        PollOption("review", "Review the draft"),
        PollOption("calendar", "Check my calendar"),
        PollOption("later", "Remind me later"),
    )
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            PollCard("What would you like to do next?", options, purpose = PollPurpose.Suggestion)
            PollCard("What would you like to do next?", options, purpose = PollPurpose.Suggestion, selection = "calendar")
        }
    }
}
