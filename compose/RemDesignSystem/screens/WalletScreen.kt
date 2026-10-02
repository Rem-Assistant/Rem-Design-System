package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **WalletScreen** — Compose sibling of the SwiftUI [WalletScreen]. A **payment-methods** screen (NOT a
 * balance/credits ledger): reached from Settings → Wallet, it lets the user securely save payment
 * methods for the agent to use when making purchases. A centered hero (neutral wallet tile + title +
 * subtitle) over a grouped list of payment providers, each a logo tile + name + trailing action
 * (**Add** when linkable, **Coming soon** when not).
 *
 * Authority: the reference Wallet screen (Settings → Wallet; "Securely save payment methods for
 * {agent} to use when making purchases for you"; Link by Stripe / Shop Pay). Token-only values. Brand
 * provider glyphs are neutral placeholders until real logo assets land (logo debt).
 */
sealed interface WalletProviderAction {
    /** The provider can be linked now. */
    data class Add(val onAdd: () -> Unit) : WalletProviderAction
    /** The provider is announced but not yet available. */
    data object ComingSoon : WalletProviderAction
}

data class PaymentProvider(
    val name: String,
    val tileColor: Color,
    val glyph: ImageVector,
    val action: WalletProviderAction,
)

@Composable
fun WalletScreen(
    providers: List<PaymentProvider>,
    modifier: Modifier = Modifier,
    agentName: String = "Rem",
    onBack: (() -> Unit)? = null,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        if (onBack != null) {
            Box(modifier = Modifier.padding(start = RemSpacing.lg, top = RemSpacing.md)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.backgroundSecondary)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = colors.labelPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(RemSpacing.xl))
            ContainedIcon(
                icon = Icons.Filled.AccountBalanceWallet,
                fill = ContainedIconFill.Subtle,
                size = ContainedIconSize.Large,
            )
            Spacer(Modifier.height(RemSpacing.lg))
            Text(
                text = "Wallet",
                style = RemTypography.title1.copy(fontWeight = FontWeight.Bold),
                color = colors.labelPrimary,
            )
            Spacer(Modifier.height(RemSpacing.sm))
            Text(
                text = "Securely save payment methods for $agentName to use when making purchases for you.",
                style = RemTypography.subheadline,
                color = colors.labelSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(RemSpacing.xl))
            RemSection {
                providers.forEachIndexed { index, provider ->
                    ProviderRow(provider)
                    if (index != providers.lastIndex) RowDivider()
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(provider: PaymentProvider) {
    val colors = RemColors.current
    val comingSoon = provider.action is WalletProviderAction.ComingSoon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RemSpacing.md, vertical = RemSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(RemRadius.small))
                .background(provider.tileColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(provider.glyph, contentDescription = null, tint = colors.labelOnColor, modifier = Modifier.size(18.dp))
        }
        Text(
            text = provider.name,
            style = RemTypography.body,
            color = if (comingSoon) colors.labelSecondary else colors.labelPrimary,
            modifier = Modifier.weight(1f),
        )
        when (val action = provider.action) {
            is WalletProviderAction.Add -> Text(
                text = "Add",
                style = RemTypography.body.copy(fontWeight = FontWeight.Bold),
                color = colors.brandBlue,
                modifier = Modifier.clickable(onClick = action.onAdd).padding(RemSpacing.xs),
            )
            WalletProviderAction.ComingSoon -> Text(
                text = "Coming soon",
                style = RemTypography.body,
                color = colors.labelTertiary,
            )
        }
    }
}

/** Canonical inset row separator (60dp = xxl + md leading inset), matching [SettingsScreen]. */
@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .padding(start = RemSpacing.xxl + RemSpacing.md)
            .fillMaxWidth()
            .height(1.dp)
            .background(RemColors.current.separator),
    )
}

/** The two reference providers. Tile colors approximate the brands; glyphs are placeholders until
 *  real logo assets land (logo debt). Built inside a composable so it can read [RemColors]. */
@Composable
fun walletReferenceProviders(onAddStripe: () -> Unit = {}): List<PaymentProvider> = listOf(
    PaymentProvider(
        name = "Link by Stripe",
        tileColor = Color(0xFF00D66F),
        glyph = Icons.Filled.Link,
        action = WalletProviderAction.Add(onAddStripe),
    ),
    PaymentProvider(
        name = "Shop Pay",
        tileColor = Color(0xFF5A31F4),
        glyph = Icons.Filled.ShoppingBag,
        action = WalletProviderAction.ComingSoon,
    ),
)

@Preview(name = "WalletScreen", showBackground = true, widthDp = 402, heightDp = 860)
@Composable
private fun WalletScreenPreview() {
    RemTheme {
        WalletScreen(providers = walletReferenceProviders(), onBack = {})
    }
}
