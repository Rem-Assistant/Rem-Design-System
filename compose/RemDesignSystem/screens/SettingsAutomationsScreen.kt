package com.rem.designsystem.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection

/**
 * Settings Automations `1833:5080`, reached from Agent settings → Automations. The "Built in"
 * section and its footer. The Daily Brief detail needs an automation runner the design system does
 * not have, so the row states that boundary instead of inventing behavior.
 * SwiftUI sibling: `Screens/SettingsAutomationsScreen.swift`.
 */
@Composable
fun SettingsAutomationsScreen(onBack: () -> Unit) {
    var boundary by rememberSaveable { mutableStateOf<String?>(null) }
    SettingsPageScaffold("Automations", "settingsAutomations", onBack,
        contentModifier = Modifier.semantics { contentDescription = PlaygroundMockData.hint }) {
        RemSection(header = SettingsAutomationsFixture.builtInHeader,
            footer = SettingsAutomationsFixture.builtInFooter, settingsHeader = true) {
            val automations = SettingsAutomationsFixture.builtIn
            automations.forEachIndexed { index, automation ->
                ListRow(
                    modifier = Modifier.testTag("automations.${automation.id}"),
                    showsDivider = index < automations.lastIndex,
                    onClick = { boundary = SettingsAutomationsFixture.boundary(automation) },
                    leading = {},
                    content = { ListRowLabel(automation.title, automation.subtitle) },
                    trailing = { DisclosureChevron() },
                )
            }
        }
    }
    SettingsPrototypeBoundary(boundary) { boundary = null }
}
