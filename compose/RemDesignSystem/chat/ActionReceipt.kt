package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.buttons.RemButtonSize
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.buttons.remButtonTokens
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme

/** The outcome an [ActionReceipt] reports. Figma property `Outcome` on **ActionReceipt** (`2566:2645`). */
enum class ActionReceiptOutcome(val figmaName: String) {
    /** The provider confirmed the action (`2566:2641`). */
    Confirmed("Confirmed"),
    /** The outcome is unknown; implies neither success, failure nor permission to retry (`2566:2643`). */
    Unconfirmed("Unconfirmed"),
}

/** Success-tint alpha for the Confirmed fill (Figma `fill/success-tint`). */
const val ActionReceiptSuccessTintAlpha = 0.12f

/** Figma geometry of the nested Button (Regular): 48dp tall. */
val ActionReceiptHeight = 48.dp

/**
 * **ActionReceipt** — Compose sibling of the SwiftUI `ActionReceipt`: the non-interactive outcome that
 * replaces a chat card's actions once the host reports what happened (for example "Sent" under a
 * [MessageDraftCard]). A status, not a control: no click, no retry, no button role.
 *
 * The visual primitive is Button `377:8` at **Rect · Secondary / Disabled / Regular** (`909:5569`), so
 * shape, padding and Body/Bold come from [remButtonTokens] for [RemButtonVariant.RectSecondary].
 * Unconfirmed keeps the stock disabled colors; Confirmed applies the receipt-owned success tint
 * (`systemGreen` at 12%) with `labelPrimary`. Figma: **ActionReceipt** (`2566:2645`).
 */
@Composable
fun ActionReceipt(
    outcome: ActionReceiptOutcome,
    label: String,
    modifier: Modifier = Modifier,
    accessibilityPrefix: String = "actionReceipt",
) {
    val colors = RemColors.current
    val tokens = remButtonTokens(RemButtonVariant.RectSecondary, RemButtonSize.Regular)
    val confirmed = outcome == ActionReceiptOutcome.Confirmed
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ActionReceiptHeight)
            .background(
                if (confirmed) colors.systemGreen.copy(alpha = ActionReceiptSuccessTintAlpha) else tokens.backgroundDisabled,
                tokens.shape,
            )
            .padding(horizontal = tokens.horizontalPadding)
            .clearAndSetSemantics { contentDescription = label }
            .testTag("$accessibilityPrefix.${outcome.name.lowercase()}"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = tokens.font,
            color = if (confirmed) colors.labelPrimary else tokens.foregroundDisabled,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "ActionReceipt", showBackground = true, widthDp = 330)
@Composable
private fun ActionReceiptPreview() {
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundSecondary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            ActionReceipt(ActionReceiptOutcome.Confirmed, "Sent")
            ActionReceipt(ActionReceiptOutcome.Unconfirmed, "Unconfirmed")
        }
    }
}
