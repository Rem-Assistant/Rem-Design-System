package com.rem.designsystem.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

/**
 * Shared constants and chrome for the Settings prototype destination screens (Memory, Models, and
 * the Automations, Billing & Usage, Permissions, About and Help & Support pages).
 */
object PlaygroundMockData {
    /**
     * The playground-wide mock-data explanation, surfaced once per destination (as a container
     * content description) instead of as extra implementation text inside the designed rows.
     */
    const val hint = "Illustrative prototype data. No accounts, keys, services, or persistence are used."
}

/**
 * A single-level Settings page: centered title, Back (top bar and system), and the canonical
 * 16dp-inset scrolling column with 22dp section gaps. [tag] names the scrolling content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPageScaffold(
    title: String,
    tag: String,
    onBack: () -> Unit,
    contentModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RemColors.current
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = RemTypography.bodyBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().testTag(tag).then(contentModifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            content = content,
        )
    }
}

/**
 * Explains a control whose behavior needs a service the design system does not have. The row stays
 * interactive and honest: tapping it states the limitation instead of doing nothing.
 */
@Composable
internal fun SettingsPrototypeBoundary(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prototype boundary") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        modifier = Modifier.testTag("settings.boundary"),
    )
}
