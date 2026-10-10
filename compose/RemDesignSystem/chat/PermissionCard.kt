package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonSize
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * Decision state of an inline action-permission request (Figma `State` on `2577:17461`). Twin of the
 * SwiftUI `PermissionCardState`. Rem's own action permission only — device/OS permissions are a separate
 * system handoff and are not modelled here.
 */
enum class PermissionCardState {
    Awaiting,
    /** Allowed once. "Always allow" is only a proposal; no persistent grant exists. */
    Allowed,
    Denied;

    /** Awaiting opens expanded; resolved states collapse to their concise receipt. */
    val defaultExpanded: Boolean get() = this == Awaiting

    /** One concise status for the header — never a compound such as "denied • not set". */
    val receipt: String
        get() = when (this) {
            Awaiting -> "Needs your approval"
            Allowed -> "Allowed once"
            Denied -> "Denied"
        }
}

/** The concrete requested action, preserved unchanged through every state (fixed absolute times). */
data class PermissionRequestDetails(val title: String, val schedule: String, val source: String)

/**
 * How risky the requested action is, as marked by the model (twin of SwiftUI `PermissionRisk`).
 * [Elevated] shows an "Elevated risk" label above the question; [Standard] shows nothing.
 */
enum class PermissionRisk { Standard, Elevated }

/** One raw request parameter in the "Full parameters" disclosure under the card, e.g. `to` / `subject`. */
data class PermissionParameter(val label: String, val value: String)

/** Display model for [PermissionCard] (twin of SwiftUI `PermissionCardModel`). */
data class PermissionCardModel(
    val title: String,
    val question: String,
    val summary: String,
    val details: PermissionRequestDetails,
    val state: PermissionCardState,
    /** The narrow scope an "Always allow" would propose; `null` hides Always allow entirely. */
    val alwaysAllowScope: String? = null,
    /** Model-marked risk; [PermissionRisk.Elevated] adds the "Elevated risk" label above the question. */
    val risk: PermissionRisk = PermissionRisk.Standard,
    /** Raw request parameters for the collapsed "Full parameters" disclosure; empty hides it. */
    val parameters: List<PermissionParameter> = emptyList(),
) {
    val subtitle: String get() = state.receipt
    val showsDecisions: Boolean get() = state == PermissionCardState.Awaiting
    val showsAlwaysAllow: Boolean get() = showsDecisions && alwaysAllowScope != null
    val showsReviewAgain: Boolean get() = state == PermissionCardState.Denied
    val alwaysAllowFootnote: String?
        get() = if (showsAlwaysAllow) "Proposed Always allow scope: $alwaysAllowScope" else null
    /** The risk label above the question; `null` (nothing shown) unless the model marked elevated risk. */
    val riskLabel: String? get() = if (risk == PermissionRisk.Elevated) "Elevated risk" else null
    /** "Full parameters" appears only when the request carries raw parameters. */
    val showsParameters: Boolean get() = parameters.isNotEmpty()
    /** The key request rows inside the card, as label / value pairs (same payload in every state). */
    val detailRows: List<PermissionParameter>
        get() = listOf(
            PermissionParameter("Action", details.title),
            PermissionParameter("When", details.schedule),
            PermissionParameter("Source", details.source),
        )
}

/**
 * **PermissionCard** — Compose sibling of the SwiftUI `PermissionCard`: the INLINE action-permission card
 * in the transcript. A ListRow disclosure header (title + receipt, chevron) above one Request body that
 * expands/collapses in place — never a sheet stacked on the card.
 *
 * The body reads top to bottom: an optional "Elevated risk" label (only for [PermissionRisk.Elevated]), the
 * question, the summary, then the key request rows as label / value pairs. Awaiting stacks its decisions
 * full width on the PollCard choice surface — Always allow (only with a scope), Deny, then Allow once as
 * the one filled primary at the bottom — with the Always-allow scope footnote under them. Allowed shows
 * the request; Denied adds a `Text · Accent` "Review again". When the request carries raw `parameters`, a
 * collapsed-by-default "Full parameters" disclosure sits under the card (outside its background) and lists
 * them monospaced; the card owns that expansion.
 *
 * The host owns [expanded] (seed it with `state.defaultExpanded`) and every decision. "Allowed" does not
 * imply the action ran. Figma `2577:17461` (the stacked decisions, risk label and Full parameters
 * disclosure are code-led and not yet in the master).
 */
@Composable
fun PermissionCard(
    model: PermissionCardModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier,
    onAlwaysAllow: (() -> Unit)? = null,
    onReviewAgain: (() -> Unit)? = null,
    accessibilityPrefix: String = "permissionCard",
) {
    val colors = RemColors.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
        Column(
            modifier = Modifier.chatCardSurface().testTag(accessibilityPrefix),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                    .semantics(mergeDescendants = true) { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                    .heightIn(min = 60.dp).padding(vertical = RemSpacing.md)
                    .testTag("$accessibilityPrefix.header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
            ) {
                Box(Modifier.weight(1f)) { ListRowLabel(model.title, model.subtitle) }
                DisclosureChevron(Modifier.rotate(if (expanded) 90f else 0f))
            }
            if (expanded) {
                Column(
                    Modifier.fillMaxWidth().testTag("$accessibilityPrefix.body"),
                    verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
                ) {
                    model.riskLabel?.let { risk ->
                        Row(
                            Modifier.semantics(mergeDescendants = true) {}.testTag("$accessibilityPrefix.risk"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
                        ) {
                            Icon(Icons.Outlined.GppMaybe, contentDescription = null, tint = colors.systemRed, modifier = Modifier.size(16.dp))
                            Text(risk, style = RemTypography.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.systemRed)
                        }
                    }
                    Text(model.question, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary,
                        modifier = Modifier.semantics { heading() })
                    Text(model.summary, style = RemTypography.subheadline, color = colors.labelSecondary)
                    Column(
                        Modifier.fillMaxWidth()
                            .background(colors.backgroundPrimary, RoundedCornerShape(RemRadius.small))
                            .padding(RemSpacing.md)
                            .semantics(mergeDescendants = true) {}
                            .testTag("$accessibilityPrefix.details"),
                        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
                    ) {
                        // The key request rows: secondary label over the primary value, one pair per row.
                        model.detailRows.forEach { row ->
                            Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
                                Text(row.label, style = RemTypography.footnote, color = colors.labelSecondary)
                                Text(row.value, style = RemTypography.subheadline, color = colors.labelPrimary)
                            }
                        }
                    }
                    if (model.showsDecisions) {
                        // Stacked full width, the standing grant first and the one filled primary (Allow once) last.
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                            if (model.showsAlwaysAllow && onAlwaysAllow != null) {
                                PermissionDecision("Always allow", onAlwaysAllow, "$accessibilityPrefix.alwaysAllow", ChatChoiceEmphasis.Standard)
                            }
                            PermissionDecision("Deny", onDeny, "$accessibilityPrefix.deny", ChatChoiceEmphasis.Standard)
                            PermissionDecision("Allow once", onAllow, "$accessibilityPrefix.allowOnce", ChatChoiceEmphasis.Primary)
                        }
                    }
                    val footnote = model.alwaysAllowFootnote
                    if (footnote != null && onAlwaysAllow != null) {
                        Text(footnote, style = RemTypography.footnote, color = colors.labelPrimary,
                            modifier = Modifier.testTag("$accessibilityPrefix.scope"))
                    }
                    if (model.showsReviewAgain && onReviewAgain != null) {
                        RemButton(
                            "Review again", onClick = onReviewAgain, variant = RemButtonVariant.TextAccent,
                            size = RemButtonSize.Regular,
                            // Hug the label at the start of the body, as authored, rather than filling the row.
                            modifier = Modifier.width(IntrinsicSize.Max).testTag("$accessibilityPrefix.reviewAgain"),
                        )
                    }
                }
            }
        }
        if (expanded && model.showsParameters) PermissionFullParameters(model.parameters, accessibilityPrefix)
    }
}

/**
 * One stacked decision on the shared PollCard choice surface ([chatChoiceSurface]): full width, label
 * centred and wrapping instead of shrinking; [ChatChoiceEmphasis.Primary] fills it brand blue.
 */
@Composable
private fun PermissionDecision(label: String, onClick: () -> Unit, tag: String, emphasis: ChatChoiceEmphasis) {
    val colors = RemColors.current
    Text(
        label,
        style = RemTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
        color = if (emphasis == ChatChoiceEmphasis.Primary) colors.labelOnColor else colors.labelPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .chatChoiceSurface(emphasis)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(RemSpacing.md)
            .testTag(tag),
    )
}

/**
 * The collapsed-by-default disclosure under the card: every raw parameter as a monospaced `label: value`
 * row, inset to the card's content edge. Presentation detail, so the card owns its expansion.
 */
@Composable
private fun PermissionFullParameters(parameters: List<PermissionParameter>, accessibilityPrefix: String) {
    val colors = RemColors.current
    var open by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = RemSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Button) { open = !open }
                .semantics(mergeDescendants = true) { stateDescription = if (open) "Expanded" else "Collapsed" }
                .testTag("$accessibilityPrefix.parameters"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RemSpacing.xs),
        ) {
            Text("Full parameters", style = RemTypography.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.labelSecondary)
            DisclosureChevron(Modifier.rotate(if (open) 90f else 0f))
        }
        if (open) {
            SelectionContainer(Modifier.testTag("$accessibilityPrefix.parameterList")) {
                Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
                    parameters.forEach { parameter ->
                        Text("${parameter.label}: ${parameter.value}",
                            style = RemTypography.footnote.copy(fontFamily = FontFamily.Monospace), color = colors.labelPrimary)
                    }
                }
            }
        }
    }
}

@Preview(name = "PermissionCard — states", showBackground = true, widthDp = 402, heightDp = 1400)
@Composable
private fun PermissionCardPreview() {
    RemTheme {
        Column(
            Modifier.background(RemColors.current.backgroundPrimary).verticalScroll(rememberScrollState()).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
        ) {
            PermissionCardState.entries.forEach { state ->
                val model = PermissionCardModel(
                    title = "Reminder permission",
                    question = "Allow Rem to create this reminder?",
                    summary = "One reminder in your Personal list.",
                    details = PermissionRequestDetails("Send investor update", "Oct 10, 2026 · 9:00 AM UTC", "Reminders · Personal"),
                    state = state,
                    alwaysAllowScope = "create reminders in Personal only.",
                )
                listOf(true, false).forEach { expanded ->
                    PermissionCard(model, expanded, {}, onAllow = {}, onDeny = {}, onAlwaysAllow = {}, onReviewAgain = {})
                }
            }
            PermissionCard(
                PermissionCardModel(
                    title = "Email permission",
                    question = "Allow Rem to send this email?",
                    summary = "Sends from your Gmail account. This can’t be undone.",
                    details = PermissionRequestDetails("Q3 investor update", "Sends immediately", "Gmail · samuel@example.com"),
                    state = PermissionCardState.Awaiting,
                    risk = PermissionRisk.Elevated,
                    parameters = listOf(PermissionParameter("to", "investors@example.com"), PermissionParameter("subject", "Q3 investor update")),
                ),
                expanded = true, onExpandedChange = {}, onAllow = {}, onDeny = {},
            )
        }
    }
}
