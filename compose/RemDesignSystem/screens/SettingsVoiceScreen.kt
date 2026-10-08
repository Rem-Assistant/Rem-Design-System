package com.rem.designsystem.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rem.designsystem.icons.RemMaterialSymbols
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.primitives.RemSlider
import com.rem.designsystem.rows.DisclosureChevron
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowLabel
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTypography

/** Shared masters 2217:1571 and 2217:1901. Fixture identities, not provider voice IDs/assets. */
enum class VoiceChoice(val id: String, val title: String, val character: String) {
    Aria("aria", "Aria", "Warm"), Sol("sol", "Sol", "Bright"), Rowan("rowan", "Rowan", "Calm"),
    Juniper("juniper", "Juniper", "Expressive"), Vale("vale", "Vale", "Neutral");
    val label: String get() = "$title ($character)"
}

enum class VoiceConversationEntry(val title: String) { VoiceSession("Voice session"), Chat("Chat") }

/** Source percentages normalized to 0…1; no invented provider speed units or service state. */
data class VoiceSettingsFixture(
    val selected: VoiceChoice = VoiceChoice.Aria,
    val conversationEntry: VoiceConversationEntry = VoiceConversationEntry.VoiceSession,
    val speed: Float = 0.50f,
    val consistency: Float = 0.75f,
    val likeness: Float = 0.50f,
    val previewing: VoiceChoice? = null,
) {
    fun select(voice: VoiceChoice) = copy(selected = voice)
    fun togglePreview(voice: VoiceChoice) = copy(previewing = if (previewing == voice) null else voice)
    fun stopPreview() = copy(previewing = null)

    companion object {
        const val conversationFooter = "Choose whether the center action opens a voice session or a new chat."
        const val spokenFooter = "Choose how Rem sounds when reading a response or talking with you."
        const val characterFooter = "Speed applies to the next thing Rem says. Consistency trades expressive range for a steadier delivery, and likeness controls how closely Rem holds to the chosen voice."
        const val chooserIntro = "Preview a voice, then choose the one Rem should use."
        const val chooserFooter = "Your choice follows this agent across your devices. Tap a play button to hear a preview."
        const val previewHint = "Demonstrates preview controls only. No audio is played in this playground."
    }
}

internal val VoiceSettingsFixtureSaver = listSaver<VoiceSettingsFixture, Any>(
    save = { listOf(it.selected.id, it.conversationEntry.name, it.speed, it.consistency, it.likeness, it.previewing?.id.orEmpty()) },
    restore = { values ->
        VoiceSettingsFixture(
            selected = VoiceChoice.entries.first { it.id == values[0] },
            conversationEntry = VoiceConversationEntry.valueOf(values[1] as String),
            speed = values[2] as Float, consistency = values[3] as Float, likeness = values[4] as Float,
            previewing = VoiceChoice.entries.firstOrNull { it.id == values[5] },
        )
    },
)

/** Native Settings shell. Only local fixture controls; no microphone, audio, TTS, sync or service. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsVoiceScreen(onBack: () -> Unit) {
    val colors = RemColors.current
    var fixture by rememberSaveable(stateSaver = VoiceSettingsFixtureSaver) { mutableStateOf(VoiceSettingsFixture()) }
    var chooser by rememberSaveable { mutableStateOf(false) }
    val controlsScroll = rememberScrollState()
    val chooserScroll = rememberScrollState()
    val leave = {
        fixture = fixture.stopPreview()
        if (chooser) chooser = false else onBack()
    }
    BackHandler(onBack = leave)
    Scaffold(
        containerColor = colors.backgroundPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (chooser) "Choose a voice" else "Voice", style = RemTypography.bodyBold) },
                navigationIcon = {
                    IconButton(onClick = leave, modifier = Modifier.testTag("back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.backgroundPrimary),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(if (chooser) chooserScroll else controlsScroll)
                .padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp)
                .testTag(if (chooser) "voiceChooser" else "settingsVoice"),
        ) {
            if (chooser) {
                VoiceChooserContent(fixture, onSelect = { fixture = fixture.select(it) },
                    onPreview = { fixture = fixture.togglePreview(it) })
            } else {
                VoiceControlsContent(fixture, onFixtureChange = { fixture = it }, showConversationEntry = true,
                    onPreview = { fixture = fixture.togglePreview(it) },
                    onChooseVoice = { fixture = fixture.stopPreview(); chooser = true })
            }
        }
    }
}

/** Controlled shared core. The shell owns scrolling, insets, navigation and context-specific state. */
@Composable
fun VoiceControlsContent(
    fixture: VoiceSettingsFixture,
    onFixtureChange: (VoiceSettingsFixture) -> Unit,
    onPreview: (VoiceChoice) -> Unit,
    onChooseVoice: () -> Unit,
    showConversationEntry: Boolean = true,
) {
    val colors = RemColors.current
    val previewingSelected = fixture.previewing == fixture.selected
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        RemSection {
            ListRow(leading = {}, content = {
                ListRowLabel("Hear this voice", fixture.selected.title + if (previewingSelected) " · Playing" else "")
            }, trailing = {
                Button(
                    onClick = { onPreview(fixture.selected) },
                    modifier = Modifier.size(48.dp).testTag("voice.previewSelected").semantics {
                        contentDescription = (if (previewingSelected) "Pause ${fixture.selected.title} preview" else "Preview ${fixture.selected.title}") + ". " + VoiceSettingsFixture.previewHint
                    },
                    contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandBlue, contentColor = colors.labelOnColor),
                ) {
                    Icon(if (previewingSelected) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                }
            })
        }
        if (showConversationEntry) {
            RemSection(header = "Conversation entry", footer = VoiceSettingsFixture.conversationFooter, settingsHeader = true) {
                ConversationEntryRow(fixture.conversationEntry) { entry ->
                    onFixtureChange(fixture.copy(conversationEntry = entry))
                }
            }
        }
        RemSection(header = "Spoken responses", footer = VoiceSettingsFixture.spokenFooter, settingsHeader = true) {
            ListRow(modifier = Modifier.testTag("voice.chooseVoice"), onClick = onChooseVoice,
                leading = { ContainedIcon(RemMaterialSymbols.Voice, fill = ContainedIconFill.Subtle, size = ContainedIconSize.Settings) },
                content = { ListRowLabel("Voice", fixture.selected.label) }, trailing = { DisclosureChevron() })
        }
        RemSection(header = "Character & speed", footer = VoiceSettingsFixture.characterFooter, settingsHeader = true) {
            VoiceSliderRow("Speed", "Slower", "Faster", "speed", fixture.speed, { onFixtureChange(fixture.copy(speed = it)) }, true)
            VoiceSliderRow("Consistency", "Creative", "Consistent", "consistency", fixture.consistency, { onFixtureChange(fixture.copy(consistency = it)) }, true)
            VoiceSliderRow("Likeness", "Flexible", "Faithful", "likeness", fixture.likeness, { onFixtureChange(fixture.copy(likeness = it)) }, false)
        }
    }
}

/** Keep the value control from starving its label on narrow screens or with large text. */
@Composable
private fun ConversationEntryRow(value: VoiceConversationEntry, onChange: (VoiceConversationEntry) -> Unit) {
    val colors = RemColors.current
    val fontScale = LocalDensity.current.fontScale
    var expanded by remember { mutableStateOf(false) }
    val menu: @Composable () -> Unit = {
        Box {
            TextButton(onClick = { expanded = true }, modifier = Modifier.testTag("voice.conversationEntry"),
                contentPadding = PaddingValues(0.dp)) {
                Text(value.title, style = RemTypography.body, color = colors.labelSecondary)
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = colors.labelSecondary,
                    modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                VoiceConversationEntry.entries.forEach { entry ->
                    DropdownMenuItem(text = { Text(entry.title) },
                        onClick = { onChange(entry); expanded = false },
                        modifier = Modifier.testTag("voice.entry.${entry.name}"))
                }
            }
        }
    }
    BoxWithConstraints {
        val stackValue = maxWidth < (380 * fontScale).dp
        ListRow(leading = {
            ContainedIcon(Icons.AutoMirrored.Filled.VolumeMute, fill = ContainedIconFill.Subtle,
                size = ContainedIconSize.Settings)
        }, content = {
            ListRowLabel("Center button starts")
            if (stackValue) menu()
        }, trailing = { if (!stackValue) menu() })
    }
}

@Composable
private fun VoiceSliderRow(title: String, minimum: String, maximum: String, id: String,
                           value: Float, onChange: (Float) -> Unit, divider: Boolean) {
    val colors = RemColors.current
    ListRow(showsDivider = divider, leading = {}, content = {
        Text(title, style = RemTypography.bodyBold, color = colors.labelPrimary)
        RemSlider(value, onChange, modifier = Modifier.testTag("voice.slider.$id").semantics { contentDescription = title })
        Row(Modifier.fillMaxWidth()) {
            Text(minimum, style = RemTypography.caption1, color = colors.labelSecondary)
            Spacer(Modifier.weight(1f))
            Text(maximum, style = RemTypography.caption1, color = colors.labelSecondary)
        }
    }, trailing = {})
}

/** Separate preview and selection targets. Selection stays in the chooser until Back. */
@Composable
fun VoiceChooserContent(fixture: VoiceSettingsFixture, onSelect: (VoiceChoice) -> Unit, onPreview: (VoiceChoice) -> Unit) {
    val colors = RemColors.current
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(VoiceSettingsFixture.chooserIntro, style = RemTypography.subheadline, color = colors.labelSecondary)
        RemSection {
            VoiceChoice.entries.forEachIndexed { index, voice ->
                val selected = fixture.selected == voice
                val previewing = fixture.previewing == voice
                ListRow(showsDivider = index < VoiceChoice.entries.lastIndex,
                    leading = {
                        IconButton(onClick = { onPreview(voice) }, modifier = Modifier.testTag("voice.preview.${voice.id}").semantics {
                            contentDescription = (if (previewing) "Pause ${voice.title} preview" else "Preview ${voice.title}") + ". " + VoiceSettingsFixture.previewHint
                        }) {
                            ContainedIcon(if (previewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                fill = ContainedIconFill.Subtle, size = ContainedIconSize.Settings)
                        }
                    }, content = {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(voice) })
                                .testTag("voice.select.${voice.id}")
                                .semantics { stateDescription = if (selected) "Selected" else "Not selected" },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) { ListRowLabel(voice.title, voice.character) }
                            Icon(if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null, tint = if (selected) colors.brandBlue else colors.labelSecondary,
                                modifier = Modifier.size(width = 28.dp, height = 24.dp))
                        }
                    }, trailing = {})
            }
        }
        Text(VoiceSettingsFixture.chooserFooter, style = RemTypography.subheadline, color = colors.labelSecondary)
    }
}
