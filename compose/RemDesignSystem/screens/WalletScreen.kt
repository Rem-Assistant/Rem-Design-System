package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * **WalletScreen** — Compose sibling of the SwiftUI [WalletScreen], and a **net-new product proposal**
 * (no Figma master / shipping source yet). Same large-title template as [InboxScreen] / [SettingsScreen]:
 * a large "Wallet" title over a scrolling body supplied by the host through [content]; chrome comes from
 * the OS. The balance hero, usage summary, and transactions section are composed from canonical
 * components ([RemSection], [ListRow], [ContainedIcon], the iOS-matched primary [Button]) in the preview.
 *
 * Product intent (Rem thesis + Muse-informed payments): agent work costs credits, so Wallet shows a
 * balance/credits hero + top-up CTA, a period usage summary, and recent activity. Every concrete choice
 * (dual dollar+credits unit, a monthly budget meter, the transaction taxonomy, a saved card) is a
 * PROPOSAL for the founder to correct — see the `// PROPOSAL:` notes in the preview.
 */
@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    title: String = "Wallet",
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.backgroundPrimary)) {
        Text(
            text = title,
            style = RemTypography.largeTitle.copy(fontWeight = FontWeight.Bold),
            color = colors.labelPrimary,
            modifier = Modifier
                .padding(horizontal = RemSpacing.lg)
                .padding(top = RemSpacing.md, bottom = RemSpacing.sm),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RemSpacing.lg)
                .padding(bottom = RemSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
            content = content,
        )
    }
}

/** Canonical inset row separator (60dp = xxl + md leading inset), matching [SettingsScreen]'s RowDivider
 *  and the Swift `RemSection` separator. */
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

/** The **balance hero** — a prominent card in the shared card language (backgroundSecondary + xlarge
 *  radius, the same surface [RemSection] uses) with balance, credits, and the primary Add funds CTA.
 *  Neutral card, not a saturated brand fill, to stay on-system. */
@Composable
private fun WalletBalanceCard(
    balance: String,
    credits: String,
    onAddFunds: () -> Unit = {},
) {
    val colors = RemColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.xlarge))
            .background(colors.backgroundSecondary)
            .padding(RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RemSpacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RemSpacing.md),
            ) {
                ContainedIcon(
                    icon = Icons.Filled.AccountBalanceWallet,
                    fill = ContainedIconFill.Tint(colors.brandBlue),
                )
                Text("Available balance", style = RemTypography.footnote, color = colors.labelSecondary)
            }
            Text(
                text = balance,
                style = RemTypography.title1.copy(fontWeight = FontWeight.Bold),
                color = colors.labelPrimary,
            )
            Text(text = credits, style = RemTypography.subheadline, color = colors.labelSecondary)
        }
        // Match the iOS primary action button (RemButtonStyle.primary = rounded-rect at `medium` radius),
        // not Material's default pill — identical to ConnectorsScreen's Continue.
        Button(
            onClick = onAddFunds,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RemRadius.medium),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.buttonBackground,
                contentColor = colors.backgroundPrimary,
            ),
        ) { Text("Add funds", style = RemTypography.bodyBold) }
    }
}

/** The **usage summary** — card with this period's spend, a slim budget meter, and a caption.
 *  PROPOSAL: a monthly budget cap; drop the meter if Wallet stays a pure balance ledger. */
@Composable
private fun WalletUsageCard(spent: String, caption: String, fraction: Float) {
    val colors = RemColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RemRadius.xlarge))
            .background(colors.backgroundSecondary)
            .padding(RemSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(RemSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Spent this period", style = RemTypography.subheadline, color = colors.labelSecondary)
            Box(Modifier.weight(1f))
            Text(
                text = spent,
                style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.labelPrimary,
            )
        }
        UsageMeter(fraction = fraction)
        Text(caption, style = RemTypography.caption1, color = colors.labelSecondary)
    }
}

/** Slim capsule meter: fillTertiary track + brandBlue fill. No canonical Meter component yet, so it
 *  lives with the proposal. */
@Composable
private fun UsageMeter(fraction: Float) {
    val colors = RemColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(colors.fillTertiary),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.brandBlue),
        )
    }
}

/** Trailing amount label: a credit is systemGreen with `+`; a debit is labelPrimary. */
@Composable
private fun TransactionAmount(amount: String, isCredit: Boolean) {
    val colors = RemColors.current
    Text(
        text = amount,
        style = RemTypography.body.copy(fontWeight = FontWeight.SemiBold),
        color = if (isCredit) colors.systemGreen else colors.labelPrimary,
    )
}

/** A recent-activity entry. PROPOSAL: taxonomy = agent-spend vs. credit (top-up / plan grant). */
private data class WalletTransaction(
    val title: String,
    val date: String,
    val amount: String,
    val isCredit: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
)

/** The Wallet proposal body (hero + usage + activity + payment). Extracted as `internal` so the
 *  Paparazzi evidence harness renders the *real* composition (it can still call the file-private
 *  building blocks above, which stay out of the public API). */
@Composable
internal fun ColumnScope.WalletProposalContent() {
    val colors = RemColors.current
    val transactions = listOf(
        WalletTransaction("Agent run · Inbox triage", "Today, 9:24 AM", "-$0.42", false, Icons.Filled.Bolt, colors.systemBlue),
        WalletTransaction("Agent run · Draft email reply", "Today, 8:10 AM", "-$0.18", false, Icons.Filled.Bolt, colors.systemBlue),
        WalletTransaction("Top-up", "Yesterday", "+$20.00", true, Icons.Filled.AddCircle, colors.systemGreen),
        WalletTransaction("Agent run · Calendar summary", "Sep 28", "-$0.31", false, Icons.Filled.Bolt, colors.systemBlue),
        WalletTransaction("Monthly plan credit", "Sep 25", "+$5.00", true, Icons.Filled.Autorenew, colors.systemGreen),
    )

    // Balance hero — PROPOSAL: dual dollar + credits unit.
    WalletBalanceCard(balance = "$42.75", credits = "≈ 1,710 credits remaining")

    // Usage — PROPOSAL: $50 monthly budget; 36% used.
    WalletUsageCard(spent = "$18.20", caption = "$18.20 of $50.00 monthly budget", fraction = 18.20f / 50.00f)

    // Recent activity — multi-row RemSection, so it carries inset separators between rows.
    RemSection(header = "Recent activity") {
        transactions.forEachIndexed { index, tx ->
            ListRow(
                title = tx.title,
                subtitle = tx.date,
                leading = { ContainedIcon(icon = tx.icon, fill = ContainedIconFill.Tint(tx.tint)) },
                trailing = { TransactionAmount(tx.amount, tx.isCredit) },
            )
            if (index != transactions.lastIndex) RowDivider()
        }
    }

    // Payment method — PROPOSAL (optional): single-row section, no divider. Drop if top-up
    // always routes through the OS payment sheet with no stored method.
    RemSection(header = "Payment") {
        ListRow(
            title = "Visa ending 4242",
            subtitle = "Expires 08/27",
            onClick = {},
            leading = { ContainedIcon(icon = Icons.Filled.CreditCard, fill = ContainedIconFill.Tint(colors.systemIndigo)) },
            trailing = { DisclosureChevron() },
        )
    }
}

@Preview(name = "WalletScreen", showBackground = true, widthDp = 402, heightDp = 1000)
@Composable
private fun WalletScreenPreview() {
    RemTheme {
        WalletScreen { WalletProposalContent() }
    }
}
