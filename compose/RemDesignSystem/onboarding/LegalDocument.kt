package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** One titled block of a legal document. The heading + body are supplied by the host; this module
 *  owns the *presentation*, not the legal copy (that is `LegalDocumentView`'s content in remclaw). */
data class LegalSection(val heading: String, val body: String)

/**
 * The **legal-document sheet** the consent step's Terms / Privacy rows open (page sheet on iOS, modal
 * sheet on Android) — the Compose sibling of the SwiftUI `LegalDocumentTemplate`, 1:1 with the shipping
 * `LegalDocumentView` (remclaw). Presentational: an inline nav title + a "Done" dismiss over a
 * scrollable body of [sections]. Per `docs/contracts/onboarding-consent.md`, the design system owns the
 * sheet chrome; the **legal copy body is owned by `LegalDocumentView`** and injected via [sections] —
 * the text here is representative structure for the paired render, not canonical legal copy.
 */
@Composable
fun LegalDocumentScreen(
    title: String,
    sections: List<LegalSection>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RemColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary),
    ) {
        // Inline nav bar: title leading, "Done" trailing (dismiss). Mirrors iOS's inline nav title.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RemSpacing.lg)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = RemTypography.title1.copy(fontWeight = FontWeight.SemiBold),
                color = colors.labelPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Done",
                style = RemTypography.body,
                color = colors.systemBlue,
                modifier = Modifier.clickableRole(onClick = onClose, label = "Done"),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.separator),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg, vertical = RemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
        ) {
            for (section in sections) {
                Column {
                    Text(text = section.heading, style = RemTypography.bodyBold, color = colors.labelPrimary)
                    Spacer(Modifier.height(RemSpacing.xs))
                    Text(text = section.body, style = RemTypography.body, color = colors.labelSecondary)
                }
            }
        }
    }
}

/** Representative Terms sections for previews/render evidence. Real copy is `LegalDocumentView`'s. */
internal val previewTermsSections = listOf(
    LegalSection(
        "1. Your account",
        "Rem accounts let you sign in, sync your data, and manage subscriptions across your devices. " +
            "You are responsible for keeping your sign-in credentials secure.",
    ),
    LegalSection(
        "2. Subscriptions",
        "Paid features renew automatically until cancelled. You can review or cancel a subscription " +
            "in Settings at any time; access continues through the end of the current period.",
    ),
    LegalSection(
        "3. Approved actions",
        "When you ask Rem to act on your behalf, it performs only the actions you approve through your " +
            "personal cloud gateway. You can revoke an approval at any time.",
    ),
)

/** Representative Privacy sections for previews/render evidence. Real copy is `LegalDocumentView`'s. */
internal val previewPrivacySections = listOf(
    LegalSection(
        "What we process",
        "Rem processes the messages, tasks, and connections you give it so it can answer you and act " +
            "on the things you ask. You can review or delete this data in Settings.",
    ),
    LegalSection(
        "Your gateway",
        "Requests route through your personal cloud gateway. Rem stores only what is needed to keep " +
            "your assistant working across sessions and devices.",
    ),
    LegalSection(
        "AI and voice providers",
        "To generate answers, relevant content may be sent to AI or voice providers under agreements " +
            "that limit their use to serving your request.",
    ),
)

@Preview(name = "Legal · terms (light)", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun LegalTermsPreview() {
    RemTheme {
        LegalDocumentScreen(title = "Terms of Service", sections = previewTermsSections, onClose = {})
    }
}

@Preview(name = "Legal · privacy (light)", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun LegalPrivacyPreview() {
    RemTheme {
        LegalDocumentScreen(title = "Privacy Policy", sections = previewPrivacySections, onClose = {})
    }
}
