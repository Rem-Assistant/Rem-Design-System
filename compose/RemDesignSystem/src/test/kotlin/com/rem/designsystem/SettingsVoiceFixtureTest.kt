package com.rem.designsystem

import androidx.compose.runtime.saveable.SaverScope
import com.rem.designsystem.screens.VoiceChoice
import com.rem.designsystem.screens.VoiceConversationEntry
import com.rem.designsystem.screens.VoiceSettingsFixture
import com.rem.designsystem.screens.VoiceSettingsFixtureSaver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Local state and saveable-value tests; platform Back/layout still require runtime evidence. */
class SettingsVoiceFixtureTest {
    @Test fun defaultsAndFiveCanonicalChoices() {
        val fixture = VoiceSettingsFixture()
        assertEquals(VoiceChoice.Aria, fixture.selected)
        assertEquals(VoiceConversationEntry.VoiceSession, fixture.conversationEntry)
        assertEquals(listOf(0.50f, 0.75f, 0.50f), listOf(fixture.speed, fixture.consistency, fixture.likeness))
        assertNull(fixture.previewing)
        assertEquals(listOf("Aria (Warm)", "Sol (Bright)", "Rowan (Calm)", "Juniper (Expressive)", "Vale (Neutral)"),
            VoiceChoice.entries.map { it.label })
    }

    @Test fun previewAndSelectionAreIndependent() {
        var fixture = VoiceSettingsFixture().togglePreview(VoiceChoice.Aria).select(VoiceChoice.Sol)
        assertEquals(VoiceChoice.Sol, fixture.selected)
        assertEquals(VoiceChoice.Aria, fixture.previewing)
        fixture = fixture.togglePreview(VoiceChoice.Sol)
        assertEquals(VoiceChoice.Sol, fixture.previewing)
        fixture = fixture.togglePreview(VoiceChoice.Sol)
        assertNull(fixture.previewing)
        assertEquals(VoiceChoice.Sol, fixture.selected)
    }

    @Test fun leavingPreviewPreservesPreferencesAndAdjustedSourceValues() {
        val preferences = VoiceSettingsFixture(selected = VoiceChoice.Sol, conversationEntry = VoiceConversationEntry.Chat,
            speed = 0.75f, consistency = 0.50f, likeness = 0.75f)
        assertEquals(preferences, preferences.togglePreview(VoiceChoice.Rowan).stopPreview())
    }

    @Test fun recreationRoundTripRetainsSelectionAndIndependentControls() {
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any) = value is String || value is Float
        }
        val fixture = VoiceSettingsFixture(selected = VoiceChoice.Sol, conversationEntry = VoiceConversationEntry.Chat,
            speed = 0.75f, consistency = 0.50f, likeness = 0.75f, previewing = VoiceChoice.Aria)
        val saved = with(VoiceSettingsFixtureSaver) { scope.save(fixture) }
        assertNotNull(saved)
        assertEquals(fixture, VoiceSettingsFixtureSaver.restore(saved!!))
        val defaultSaved = with(VoiceSettingsFixtureSaver) { scope.save(VoiceSettingsFixture()) }
        assertEquals(VoiceSettingsFixture(), VoiceSettingsFixtureSaver.restore(defaultSaved!!))
    }
}
