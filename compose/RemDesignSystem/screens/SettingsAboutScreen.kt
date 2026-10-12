package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rem.designsystem.brand.RemAppIcon
import com.rem.designsystem.onboarding.LegalDocumentScreen
import com.rem.designsystem.onboarding.LegalSection
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

/**
 * Settings About `1827:50839`, reached from Settings → About. The app-icon hero (the canonical
 * [RemAppIcon] asset at 80dp, radius 18), the LEGAL section and the Version row. Terms and Privacy
 * open the shared [LegalDocumentScreen] with the same summaries the onboarding legal rows use; real
 * legal copy belongs to the shipping app. Hosts pass their real build version; the default is the
 * source value. SwiftUI sibling: `Screens/SettingsAboutScreen.swift`.
 */
@Composable
fun SettingsAboutScreen(onBack: () -> Unit, version: String = SettingsAboutFixture.sourceVersion) {
    val colors = RemColors.current
    var documentId by rememberSaveable { mutableStateOf<String?>(null) }
    SettingsPageScaffold("About", "settingsAbout", onBack) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("about.hero").semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RemAppIcon(size = 80.dp, cornerRadius = 18.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(SettingsAboutFixture.appName, style = RemTypography.title1Bold, color = colors.labelPrimary,
                    modifier = Modifier.semantics { heading() })
                Text(SettingsAboutFixture.tagline, style = RemTypography.body, color = colors.labelSecondary,
                    textAlign = TextAlign.Center)
            }
        }
        RemSection(header = SettingsAboutFixture.legalHeader, settingsHeader = true) {
            val legal = SettingsAboutFixture.legal
            legal.forEachIndexed { index, item ->
                ListRow(
                    modifier = Modifier.testTag("about.legal.${item.id}"),
                    showsDivider = index < legal.lastIndex,
                    onClick = { documentId = item.id },
                    leading = {},
                    content = { ListRowLabel(item.title) },
                    trailing = { DisclosureChevron() },
                )
            }
        }
        RemSection {
            ListRow(
                modifier = Modifier.testTag("about.version").semantics(mergeDescendants = true) {},
                leading = {},
                content = { ListRowLabel(SettingsAboutFixture.versionTitle) },
                trailing = {
                    // Source Callout/Regular 16.
                    Text(version, style = RemTypography.body.copy(fontSize = 16.sp), color = colors.labelSecondary)
                },
            )
        }
    }
    SettingsAboutFixture.legal.firstOrNull { it.id == documentId }?.let { item ->
        Dialog(onDismissRequest = { documentId = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(colors.backgroundPrimary)) {
                LegalDocumentScreen(title = item.title, sections = listOf(LegalSection(item.title, item.summary)),
                    onClose = { documentId = null })
            }
        }
    }
}
