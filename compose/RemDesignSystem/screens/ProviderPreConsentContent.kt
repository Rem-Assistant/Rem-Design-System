package com.rem.designsystem.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTypography

/** Provider-specific copy for shared Pre-consent/Provider1898:54528. No Wallet or auth dependency. */
data class ProviderPreConsentPayload(
    val title: String,
    val purpose: String,
    val benefits: List<ProviderPreConsentBenefit>,
    val disclosure: String,
    val connectTitle: String = "Connect",
    val cancelTitle: String = "Cancel",
)
data class ProviderPreConsentBenefit(val id: String, val title: String, val body: String)

/** Shared structure with caller-owned mark/icon slots and actions. Caller presents a native modal;
 * Link, Shop Pay, Notion and other authored providers keep distinct copy without duplicating layout.
 * This component performs no authentication, navigation, persistence, or external calls. */
@Composable
fun ProviderPreConsentContent(
    payload: ProviderPreConsentPayload,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    actionAccessibilityPrefix: String = "providerConsent",
    providerMark: @Composable () -> Unit,
    benefitIcon: @Composable (ProviderPreConsentBenefit) -> Unit,
) {
    val colors = RemColors.current
    // All content, including the actions, can scroll at large font sizes or in a short viewport.
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp)
            .padding(bottom = RemSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RemSpacing.lg),
    ) {
        providerMark()
        Text(payload.title, style = RemTypography.title1.copy(fontWeight = FontWeight.Bold), color = colors.labelPrimary,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(payload.purpose, style = RemTypography.body, color = colors.labelSecondary, textAlign = TextAlign.Center)
        payload.benefits.forEach { benefit ->
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(RemSpacing.md), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clearAndSetSemantics {}) { benefitIcon(benefit) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RemSpacing.xs)) {
                    Text(benefit.title, style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.labelPrimary)
                    Text(benefit.body, style = RemTypography.subheadline, color = colors.labelSecondary)
                }
            }
        }
        Text(payload.disclosure, style = RemTypography.footnote, color = colors.labelSecondary, textAlign = TextAlign.Center)
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().testTag("$actionAccessibilityPrefix.connect"),
            shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = colors.brandBlue, contentColor = colors.labelOnColor),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp)) {
            Text(payload.connectTitle, style = RemTypography.bodyBold)
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().testTag("$actionAccessibilityPrefix.cancel")) {
            Text(payload.cancelTitle, style = RemTypography.body, color = colors.brandBlue)
        }
    }
}
