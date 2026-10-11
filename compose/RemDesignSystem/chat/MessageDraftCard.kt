package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The outgoing message a [MessageDraftCard] shows. Display data only: the host owns the real draft, its
 * account and its delivery. The payload stays identical across every card state.
 */
data class MessageDraft(
    val from: String,
    val to: String,
    val subject: String,
    val body: String,
    val title: String = "New Email",
)

/** Figma property `State` on **MessageDraftCard** (`2555:1550`). */
enum class MessageDraftCardState(val figmaName: String) {
    /** Awaiting the user's explicit Send Email or Discard (`458:69`). Nothing has been sent. */
    Review("Review"),
    /** The provider confirmed delivery of this exact message (`2555:1499`). Not inferred from time. */
    Sent("Sent"),
    /** The outcome is unknown (`2555:1517`). It never implies permission to send again. */
    Unconfirmed("Unconfirmed");

    /** The receipt that replaces the actions, or `null` while the draft is in review. */
    val receiptOutcome: ActionReceiptOutcome?
        get() = when (this) {
            Review -> null
            Sent -> ActionReceiptOutcome.Confirmed
            Unconfirmed -> ActionReceiptOutcome.Unconfirmed
        }

    /** The receipt's contextual label (`2555:1499` / `2555:1517`), or `null` while in review. */
    val receiptLabel: String?
        get() = when (this) {
            Review -> null
            Sent -> "Sent"
            Unconfirmed -> "Unconfirmed"
        }

    /** Only the review state offers Send Email and Discard. */
    val showsActions: Boolean get() = this == Review
}

/** Figma master width; the card fills narrower rows. */
val MessageDraftCardMaxWidth = 330.dp

/**
 * **MessageDraftCard** — Compose sibling of the SwiftUI `MessageDraftCard`: an outgoing email shown for
 * review — title, a From / To / Subject / Body payload, then either **Send Email** (Button `377:8`
 * `Rect · Blue`) and **Discard** (`Rect · Secondary`) or an [ActionReceipt] once the host reports the
 * outcome. Presentation only: [onSend] / [onDiscard] report intent; the host sends and moves the card to
 * [MessageDraftCardState.Sent] only on provider confirmation. The masters carry no leading icon.
 * Figma: **MessageDraftCard** (`2555:1550`; Review `458:69`, Sent `2555:1499`, Unconfirmed `2555:1517`).
 */
@Composable
fun MessageDraftCard(
    draft: MessageDraft,
    modifier: Modifier = Modifier,
    state: MessageDraftCardState = MessageDraftCardState.Review,
    sendLabel: String = "Send Email",
    discardLabel: String = "Discard",
    accessibilityPrefix: String = "messageDraft",
    onSend: () -> Unit = {},
    onDiscard: () -> Unit = {},
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .widthIn(max = MessageDraftCardMaxWidth)
            .fillMaxWidth()
            .background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.large))
            .padding(RemSpacing.md)
            .testTag(accessibilityPrefix),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(
            draft.title,
            style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.labelPrimary,
            modifier = Modifier.fillMaxWidth().semantics { heading() }.testTag("$accessibilityPrefix.title"),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.backgroundPrimary, RoundedCornerShape(RemRadius.medium)),
        ) {
            InlineField("From", draft.from, "$accessibilityPrefix.from")
            InlineField("To", draft.to, "$accessibilityPrefix.to")
            StackedField("Subject", draft.subject, "$accessibilityPrefix.subject")
            StackedField("Body", draft.body, "$accessibilityPrefix.body")
        }
        val outcome = state.receiptOutcome
        val receiptLabel = state.receiptLabel
        if (state.showsActions) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
                RemButton(
                    sendLabel, onClick = onSend, variant = RemButtonVariant.RectBlue,
                    modifier = Modifier.weight(1f).testTag("$accessibilityPrefix.send"),
                )
                RemButton(
                    discardLabel, onClick = onDiscard, variant = RemButtonVariant.RectSecondary,
                    modifier = Modifier.weight(1f).testTag("$accessibilityPrefix.discard"),
                )
            }
        } else if (outcome != null && receiptLabel != null) {
            ActionReceipt(outcome, receiptLabel, accessibilityPrefix = "$accessibilityPrefix.receipt")
        }
    }
}

@Composable
private fun InlineField(name: String, value: String, tag: String) {
    val colors = RemColors.current
    Row(
        Modifier.fillMaxWidth().padding(RemSpacing.sm).semantics(mergeDescendants = true) {}.testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(name, style = RemTypography.subheadline, color = colors.labelSecondary)
        Text(value, style = RemTypography.subheadline, color = colors.labelPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StackedField(name: String, value: String, tag: String) {
    val colors = RemColors.current
    Column(
        Modifier.fillMaxWidth().padding(RemSpacing.sm).semantics(mergeDescendants = true) {}.testTag(tag),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Text(name, style = RemTypography.subheadline, color = colors.labelSecondary)
        Text(value, style = RemTypography.subheadline, color = colors.labelPrimary)
    }
}

@Preview(name = "MessageDraftCard", showBackground = true, widthDp = 402)
@Composable
private fun MessageDraftCardPreview() {
    val draft = MessageDraft(
        from = "me@example.com", to = "alex@example.com", subject = "Re: Next steps",
        body = "Hi Alex,\n\nThanks for reaching out. I've put time on the calendar.\n\nBest",
    )
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            MessageDraftCardState.entries.forEach { MessageDraftCard(draft, state = it) }
        }
    }
}
