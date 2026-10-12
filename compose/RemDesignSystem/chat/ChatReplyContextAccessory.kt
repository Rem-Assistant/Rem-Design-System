package com.rem.designsystem.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.screens.ChatReplyContext
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** The dismiss control's touch target. */
val ChatReplyContextDismissTarget = 44.dp

/**
 * **ChatReplyContextAccessory** — Compose sibling of the SwiftUI `ChatReplyContextAccessory`: the task
 * reply context shown above the **same** [RemComposerBar] — "Replying to …", a summary, and a dismiss ×.
 * An accessory, not a composer variant: it owns no send, model, attachment or voice behaviour. Sizing is
 * content-driven (fills the dock, hugs its text) with a real 44dp dismiss target; the cancelled Figma
 * source-master size normalisation is not assumed. Figma: **Rem/Chat/Reply context** (`2682:22298`).
 */
@Composable
fun ChatReplyContextAccessory(
    context: ChatReplyContext,
    modifier: Modifier = Modifier,
    accessibilityPrefix: String = "replyContext",
    onDismiss: (() -> Unit)? = null,
) {
    val colors = RemColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ChatReplyContextDismissTarget)
            .background(colors.backgroundSecondary, RoundedCornerShape(RemRadius.medium))
            .padding(start = RemSpacing.md, end = if (onDismiss == null) RemSpacing.md else 0.dp)
            .padding(vertical = if (onDismiss == null) RemSpacing.sm else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.sm),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {}
                .testTag("$accessibilityPrefix.label"),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(context.title, style = RemTypography.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
            Text(context.summary, style = RemTypography.footnote, color = colors.labelSecondary, maxLines = 2)
        }
        if (onDismiss != null) {
            Box(
                modifier = Modifier
                    .size(ChatReplyContextDismissTarget)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .semantics { contentDescription = "Dismiss reply context" }
                    .testTag("$accessibilityPrefix.dismiss"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = colors.brandBlue, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview(name = "ChatReplyContextAccessory", showBackground = true, widthDp = 402)
@Composable
private fun ChatReplyContextAccessoryPreview() {
    RemTheme {
        Column(
            modifier = Modifier.background(RemColors.current.backgroundPrimary).padding(RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.sm),
        ) {
            ChatReplyContextAccessory(ChatReplyContext("target-1", "Replying to Rem", "Plan the next step"), onDismiss = {})
            RemComposerBar(placeholder = "Write your reply…")
        }
    }
}
