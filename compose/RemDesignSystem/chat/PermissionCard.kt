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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.buttons.RemButtonSize
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

/** Display model for [PermissionCard] (twin of SwiftUI `PermissionCardModel`). */
data class PermissionCardModel(
    val title: String,
    val question: String,
    val summary: String,
    val details: PermissionRequestDetails,
    val state: PermissionCardState,
    /** The narrow scope an "Always allow" would propose; `null` hides Always allow entirely. */
    val alwaysAllowScope: String? = null,
) {
    val subtitle: String get() = state.receipt
    val showsDecisions: Boolean get() = state == PermissionCardState.Awaiting
    val showsAlwaysAllow: Boolean get() = showsDecisions && alwaysAllowScope != null
    val showsReviewAgain: Boolean get() = state == PermissionCardState.Denied
    val alwaysAllowFootnote: String?
        get() = if (showsAlwaysAllow) "Proposed Always allow scope: $alwaysAllowScope" else null
}

/**
 * **PermissionCard** — Compose sibling of the SwiftUI `PermissionCard`: the INLINE action-permission card
 * in the transcript. A ListRow disclosure header (title + receipt, chevron) above one Request body that
 * expands/collapses in place — never a sheet stacked on the card. Awaiting shows three horizontal
 * `Pill · Secondary` decisions with wrapping labels; Allowed shows the request; Denied adds a
 * `Text · Accent` "Review again". The host owns [expanded] (seed it with `state.defaultExpanded`) and every
 * decision. "Allowed" does not imply the action ran.
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
    Column(
        modifier = modifier.chatCardSurface().testTag(accessibilityPrefix),
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
                Text(model.question, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
                Text(model.summary, style = RemTypography.subheadline, color = colors.labelSecondary)
                Column(
                    Modifier.fillMaxWidth()
                        .background(colors.backgroundPrimary, RoundedCornerShape(RemRadius.small))
                        .padding(RemSpacing.md)
                        .semantics(mergeDescendants = true) {}
                        .testTag("$accessibilityPrefix.details"),
                    verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
                ) {
                    Text(model.details.title, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
                    Text(model.details.schedule, style = RemTypography.subheadline, color = colors.labelPrimary)
                    Text(model.details.source, style = RemTypography.footnote, color = colors.labelSecondary)
                }
                if (model.showsDecisions) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                        PermissionDecision("Allow once", onAllow, "$accessibilityPrefix.allowOnce", Modifier.weight(1f))
                        if (model.showsAlwaysAllow && onAlwaysAllow != null) {
                            PermissionDecision("Always allow", onAlwaysAllow, "$accessibilityPrefix.alwaysAllow", Modifier.weight(1f))
                        }
                        PermissionDecision("Deny", onDeny, "$accessibilityPrefix.deny", Modifier.weight(1f))
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
}

/**
 * One of the three horizontal decisions: a canonical `Pill · Secondary` [RemButton] (ButtonGroup `773:17`
 * horizontal) sharing the width, whose label wraps instead of shrinking, at the authored 64dp height.
 */
@Composable
private fun PermissionDecision(label: String, onClick: () -> Unit, tag: String, modifier: Modifier) {
    RemButton(label, onClick = onClick, variant = RemButtonVariant.PillSecondary,
        modifier = modifier.heightIn(min = 64.dp).testTag(tag))
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
        }
    }
}
