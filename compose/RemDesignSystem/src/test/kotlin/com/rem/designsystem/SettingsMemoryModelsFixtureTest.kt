package com.rem.designsystem

import com.rem.designsystem.screens.MemoryContent
import com.rem.designsystem.screens.MemoryControl
import com.rem.designsystem.screens.ModelProvider
import com.rem.designsystem.screens.ModelsContent
import com.rem.designsystem.screens.SavedProvidersSaver
import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure fixture-logic tests for the Memory and Models destinations — the Compose siblings of the
 * SwiftUI `SettingsMemoryModelsFixtureTests`. They prove the scope/cancel/save boundaries the source
 * contract calls out, without an emulator.
 */
class SettingsMemoryModelsFixtureTest {

    // Memory

    @Test fun memoryToggleDefaultsMatchSource() {
        assertEquals(true, MemoryContent.toggleDefaults[MemoryControl.SearchAndReference])
        assertEquals(true, MemoryContent.toggleDefaults[MemoryControl.GenerateMemory])
        assertEquals(false, MemoryContent.toggleDefaults[MemoryControl.SensitiveTopics])
    }

    @Test fun memoryTogglesAreIndependent() {
        // Flipping one control in a copy leaves the others untouched.
        val flipped = MemoryContent.toggleDefaults + (MemoryControl.SensitiveTopics to true)
        assertEquals(true, flipped[MemoryControl.SensitiveTopics])
        assertEquals(true, flipped[MemoryControl.SearchAndReference])
        assertEquals(true, flipped[MemoryControl.GenerateMemory])
    }

    @Test fun memorySummaryCopyIsExactAndComplete() {
        assertEquals("Updated just now · Generated from your conversations", MemoryContent.summaryMetadata)
        assertEquals("Ask or update memory", MemoryContent.composerPlaceholder)
        assertEquals(
            listOf("Overview", "How Rem should work", "Current focus"),
            MemoryContent.summarySections.map { it.heading },
        )
        assertTrue(MemoryContent.summarySections[0].body.startsWith("You prefer direct, practical help"))
    }

    @Test fun memoryComposerBoundaryIgnoresBlankAndNotesText() {
        assertNull(MemoryContent.composerFeedback(""))
        assertNull(MemoryContent.composerFeedback("   \n"))
        val feedback = MemoryContent.composerFeedback("remember I prefer morning meetings")
        assertNotNull(feedback)
        assertEquals("Noted in this prototype session. Rem doesn’t reply or change memory here.", feedback)
    }

    // Models

    @Test fun modelsProviderPickerHasExactlyFiveProvidersInSourceOrder() {
        assertEquals(
            listOf("Anthropic", "OpenAI", "Google", "Mistral", "OpenRouter"),
            ModelProvider.entries.map { it.displayName },
        )
    }

    @Test fun modelsSaveEnablementRequiresNonemptyDraft() {
        assertFalse(ModelsContent.canSave(""))
        assertFalse(ModelsContent.canSave("   "))
        assertTrue(ModelsContent.canSave("sk-illustrative-dummy"))
    }

    @Test fun savedProviderFlagsRoundTripThroughOnlySaveableProviderNames() {
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = value is String
        }
        val providers = ModelProvider.entries.toSet()
        val saved = with(SavedProvidersSaver) { scope.save(providers) }
        assertNotNull(saved)
        assertEquals(ModelProvider.entries.map { it.name }, saved)
        assertEquals(providers, SavedProvidersSaver.restore(saved!!))
    }

    @Test fun modelsSaveRecordsFlagForNewProviderWithoutStoringKey() {
        val initial = setOf(ModelProvider.Anthropic)
        val afterOpenAI = ModelsContent.recordSavedKey(initial, ModelProvider.OpenAI)
        assertEquals(setOf(ModelProvider.Anthropic, ModelProvider.OpenAI), afterOpenAI)
        // Re-saving an existing provider is idempotent; only a provider flag is tracked, never a key.
        assertEquals(initial, ModelsContent.recordSavedKey(initial, ModelProvider.Anthropic))
    }
}
