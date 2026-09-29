package com.rem.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
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
 * this shipping source contains no sample legal prose. Render-only fixture copy lives under
 * `src/test`, outside the packaged Android library. The host owns the native modal container, scrim,
 * optional drag affordance, and device-status-bar layering; this composable owns the content chrome
 * rendered inside that container.
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
        // Inline nav bar: centered title + a 44dp Done affordance. The explicit tap target keeps the
        // dismiss action visibly and behaviorally equivalent to the SwiftUI page-sheet chrome.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = RemTypography.title1.copy(fontWeight = FontWeight.SemiBold),
                color = colors.labelPrimary,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = RemSpacing.sm)
                    .size(44.dp)
                    .clickableRole(onClick = onClose, label = "Done"),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Done", style = RemTypography.body, color = colors.systemBlue)
            }
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
