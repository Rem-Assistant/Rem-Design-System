package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rem.designsystem.buttons.RemButton
import com.rem.designsystem.buttons.RemButtonVariant
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

/**
 * Settings Billing & Usage `1827:50803`, reached from Settings → Billing & Usage. Current plan, two
 * usage meters and the Rect · Blue "Upgrade to Pro" RemButton. Plan and usage are illustrative
 * fixture values. There is no billing account or purchase flow, so Upgrade states that boundary;
 * no payment or plan change is simulated. SwiftUI sibling: `Screens/SettingsBillingScreen.swift`.
 */
@Composable
fun SettingsBillingScreen(onBack: () -> Unit) {
    val colors = RemColors.current
    var boundary by rememberSaveable { mutableStateOf<String?>(null) }
    SettingsPageScaffold(SettingsBillingFixture.title, "settingsBilling", onBack,
        contentModifier = Modifier.semantics { contentDescription = PlaygroundMockData.hint }) {
        RemSection(header = SettingsBillingFixture.planHeader, settingsHeader = true) {
            ListRow(
                modifier = Modifier.testTag("billing.plan").semantics(mergeDescendants = true) {},
                leading = {},
                content = { ListRowLabel(SettingsBillingFixture.planTitle) },
                trailing = {
                    // Source Callout/Regular 16.
                    Text(SettingsBillingFixture.plan, style = RemTypography.body.copy(fontSize = 16.sp),
                        color = colors.labelSecondary)
                },
            )
        }
        RemSection(header = SettingsBillingFixture.usageHeader, settingsHeader = true) {
            SettingsBillingFixture.usage.forEach { meter -> BillingUsageMeterRow(meter) }
        }
        RemButton(
            text = SettingsBillingFixture.upgradeTitle,
            onClick = { boundary = SettingsBillingFixture.upgradeBoundary },
            modifier = Modifier.testTag("billing.upgrade"),
            variant = RemButtonVariant.RectBlue,
        )
    }
    SettingsPrototypeBoundary(boundary) { boundary = null }
}

/** Usage meter `1827:50788`: title, footnote count and a 4dp brand-blue progress track. */
@Composable
private fun BillingUsageMeterRow(meter: SettingsUsageMeter) {
    val colors = RemColors.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp)
            .testTag("billing.usage.${meter.id}")
            .semantics(mergeDescendants = true) {
                stateDescription = meter.label
                progressBarRangeInfo = ProgressBarRangeInfo(meter.fraction, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(meter.title, style = RemTypography.body, color = colors.labelPrimary)
            // Source: 8dp gap, 10dp spacer, 8dp gap between title and count.
            Spacer(Modifier.width(26.dp))
            Text(meter.label, style = RemTypography.footnote, color = colors.labelSecondary)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.fillTertiary)) {
            Box(Modifier.fillMaxWidth(meter.fraction).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(colors.brandBlue))
        }
    }
}
