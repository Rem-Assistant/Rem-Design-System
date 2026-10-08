package com.rem.designsystem.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

// Models destination for the bounded Settings playground — Figma masters `Screen/Models`
// (`1833:5109`) and `Screen/Add provider key · Working states` (`1956:8163`) with the recovered
// provider menu (`1870:54593`). Code-only prototype: the Auto toggle, the Anthropic availability
// switch, and the per-provider saved-key flag are independent in-memory fixture values. No provider is
// contacted, no key is validated, and no credential is stored — Save records only a dummy saved-key
// flag and never retains the key draft. Owns its scaffold/title and a nested root↔addKey stack.

/** The five API-key providers from the recovered picker menu (`1870:54593`), in source order. */
enum class ModelProvider(val displayName: String) {
    Anthropic("Anthropic"),
    OpenAI("OpenAI"),
    Google("Google"),
    Mistral("Mistral"),
    OpenRouter("OpenRouter"),
}

/** Deterministic Models fixture: exact source copy, defaults, and the non-emptiness save rule. */
object ModelsContent {
    const val autoFooter = "Rem chooses a managed model for each question based on the task, availability, and cost."
    const val keyFooter = "Enter an API key from your provider to use its models."

    /** Save is enabled only for a nonempty key draft. Validation beyond non-emptiness is a source gap. */
    fun canSave(keyDraft: String): Boolean = keyDraft.isNotBlank()

    /**
     * Records a dummy saved-key flag — the key value is intentionally NOT a parameter, so nothing
     * about the draft is retained. Returns the new saved-provider set.
     */
    fun recordSavedKey(saved: Set<ModelProvider>, provider: ModelProvider): Set<ModelProvider> = saved + provider
}

private enum class ModelsRoute { Root, AddKey }

/**
 * Models destination entry point. Owns its scaffold/title and a nested root↔addKey stack. [onBack]
 * leaves the destination (outer navigation is owned by the host).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModelsScreen(onBack: () -> Unit) {
    val colors = RemColors.current
    var route by rememberSaveable { mutableStateOf(ModelsRoute.Root) }

    // Session fixture state, owned here so Back from Add provider key discards the draft but a saved
    // flag persists for the session.
    var autoManagedModel by rememberSaveable { mutableStateOf(true) }
    var savedProviders by remember { mutableStateOf(setOf(ModelProvider.Anthropic)) }
    val availableProviders = remember { mutableSetOf<ModelProvider>().toMutableStateList() }

    val title = if (route == ModelsRoute.Root) "Models" else "Add provider key"
    val leave = { if (route == ModelsRoute.AddKey) route = ModelsRoute.Root else onBack() }

    Scaffold(
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = RemTypography.bodyBold) },
                navigationIcon = {
                    IconButton(onClick = leave, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (route) {
                ModelsRoute.Root -> ModelsRootContent(
                    autoManagedModel = autoManagedModel,
                    onAutoChange = { autoManagedModel = it },
                    anthropicSaved = ModelProvider.Anthropic in savedProviders,
                    anthropicAvailable = ModelProvider.Anthropic in availableProviders,
                    onAnthropicAvailableChange = {
                        if (it) availableProviders.add(ModelProvider.Anthropic) else availableProviders.remove(ModelProvider.Anthropic)
                    },
                    openAddKey = { route = ModelsRoute.AddKey },
                )
                ModelsRoute.AddKey -> AddProviderKeyContent(
                    onSave = { provider ->
                        savedProviders = ModelsContent.recordSavedKey(savedProviders, provider)
                        route = ModelsRoute.Root
                    },
                )
            }
        }
    }
}

@Composable
private fun ModelsRootContent(
    autoManagedModel: Boolean,
    onAutoChange: (Boolean) -> Unit,
    anthropicSaved: Boolean,
    anthropicAvailable: Boolean,
    onAnthropicAvailableChange: (Boolean) -> Unit,
    openAddKey: () -> Unit,
) {
    val colors = RemColors.current
    Column(
        Modifier
            .testTag("settingsModels")
            .semantics { contentDescription = PlaygroundMockData.hint }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        RemSection(header = "Rem", footer = ModelsContent.autoFooter, settingsHeader = true) {
            ListRow(
                leading = {},
                content = { ListRowLabel("Auto", "Rem’s managed model") },
                trailing = {
                    Switch(
                        checked = autoManagedModel,
                        onCheckedChange = onAutoChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.systemGreen),
                        modifier = Modifier.testTag("models.toggle.auto").semantics { contentDescription = "Auto" },
                    )
                },
            )
        }
        RemSection(header = "API keys", settingsHeader = true) {
            // Navigable content (opens the key editor) paired with its disclosure chevron; the
            // availability switch keeps the trailing slot — one interaction per region.
            ListRow(
                showsDivider = true,
                onClick = openAddKey,
                modifier = Modifier.testTag("models.providerRow.anthropic"),
                leading = {},
                content = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { ListRowLabel("Anthropic", if (anthropicSaved) "Saved" else null) }
                        DisclosureChevron()
                    }
                },
                trailing = {
                    Switch(
                        checked = anthropicAvailable,
                        onCheckedChange = onAnthropicAvailableChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.systemGreen),
                        modifier = Modifier.testTag("models.toggle.anthropicAvailable").semantics { contentDescription = "Anthropic availability" },
                    )
                },
            )
            ListRow(
                onClick = openAddKey,
                modifier = Modifier.testTag("models.addProviderKey"),
                leading = {},
                content = { ListRowLabel("Add provider key") },
                trailing = { DisclosureChevron() },
            )
        }
    }
}

@Composable
private fun AddProviderKeyContent(onSave: (ModelProvider) -> Unit) {
    val colors = RemColors.current
    var provider by rememberSaveable { mutableStateOf(ModelProvider.Anthropic) }
    var keyDraft by rememberSaveable { mutableStateOf("") }
    var pickerExpanded by remember { mutableStateOf(false) }
    val canSave = ModelsContent.canSave(keyDraft)

    Column(Modifier.testTag("modelsAddKey").fillMaxSize().verticalScroll(rememberScrollState())) {
        // Single Save action for the Add provider key screen (the native top-bar back discards).
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onSave(provider) }, enabled = canSave, modifier = Modifier.testTag("models.saveKey")) {
                Text("Save")
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            RemSection(header = "API key", footer = ModelsContent.keyFooter, settingsHeader = true) {
                ListRow(
                    showsDivider = true,
                    leading = {},
                    content = { Text("Provider", style = RemTypography.body, color = colors.labelPrimary) },
                    trailing = {
                        Box {
                            Row(
                                Modifier.testTag("models.providerPicker").semantics { contentDescription = "Provider" },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    provider.displayName,
                                    style = RemTypography.body,
                                    color = colors.labelSecondary,
                                    modifier = Modifier.semantics { contentDescription = "Provider ${provider.displayName}" },
                                )
                                IconButton(onClick = { pickerExpanded = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.UnfoldMore, contentDescription = "Choose provider", tint = colors.labelSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(expanded = pickerExpanded, onDismissRequest = { pickerExpanded = false }) {
                                ModelProvider.entries.forEach { candidate ->
                                    DropdownMenuItem(
                                        text = { Text(candidate.displayName) },
                                        onClick = { provider = candidate; pickerExpanded = false },
                                        trailingIcon = { if (candidate == provider) Icon(Icons.Filled.Check, contentDescription = null) },
                                        modifier = Modifier.testTag("models.providerOption.${candidate.displayName}"),
                                    )
                                }
                            }
                        }
                    },
                )
                ListRow(
                    leading = {},
                    content = {
                        Box {
                            if (keyDraft.isEmpty()) {
                                Text("API key", style = RemTypography.body, color = colors.labelTertiary)
                            }
                            BasicTextField(
                                value = keyDraft,
                                onValueChange = { keyDraft = it },
                                singleLine = true,
                                textStyle = RemTypography.body.copy(color = colors.labelPrimary),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.brandBlue),
                                modifier = Modifier.fillMaxWidth().testTag("models.keyField"),
                            )
                        }
                    },
                    trailing = {},
                )
            }
        }
    }
}
