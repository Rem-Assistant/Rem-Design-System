package com.rem.designsystem

import com.rem.designsystem.chat.ComposerAttachment
import com.rem.designsystem.screens.ChatComposerAction
import com.rem.designsystem.screens.ChatComposerFixture
import com.rem.designsystem.screens.ChatComposerFixture.Scenario
import com.rem.designsystem.screens.ChatComposerState
import com.rem.designsystem.screens.ComposerAvailability
import com.rem.designsystem.screens.ComposerPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Presentation-only Playground host for the canonical composer. Paired with `ChatComposerFixtureTests.swift`. */
class ChatComposerFixtureTest {
    private val photo = ComposerAttachment("photo.0", "Photo 1", ComposerAttachment.Kind.Image)

    @Test fun typingThenSendRecordsTrimmedTextAndClearsDraft() {
        val fixture = ChatComposerFixture().apply(ChatComposerAction.DraftChanged("  Plan my afternoon ")).apply(ChatComposerAction.Send)
        assertEquals(listOf("Plan my afternoon"), fixture.sent)
        assertEquals("", fixture.state.draft)
    }

    @Test fun sendIsIgnoredWhenTheRuleSaysNo() {
        val fixture = ChatComposerFixture().apply(ChatComposerAction.Send).attach(ComposerAttachment.CloudBrowser).apply(ChatComposerAction.Send)
        assertEquals(emptyList<String>(), fixture.sent)
    }

    @Test fun attachmentOnlySendKeepsCapabilityChip() {
        val fixture = ChatComposerFixture().attach(photo).attach(ComposerAttachment.CloudBrowser).apply(ChatComposerAction.Send)
        assertEquals(listOf("Photo 1"), fixture.sent)
        assertEquals(listOf(ComposerAttachment.CloudBrowser), fixture.state.attachments)
    }

    @Test fun attachIsIdempotentAndRemoveDropsById() {
        val twice = ChatComposerFixture().attach(ComposerAttachment.CloudBrowser).attach(ComposerAttachment.CloudBrowser)
        assertEquals(1, twice.state.attachments.size)
        assertTrue(twice.apply(ChatComposerAction.RemoveAttachment(ComposerAttachment.CloudBrowser.id)).state.attachments.isEmpty())
    }

    @Test fun pickedPhotosBecomeImageChipsAndANewPickReplacesThem() {
        val two = ChatComposerFixture().attach(ComposerAttachment.CloudBrowser).attachPickedPhotos(2)
        assertEquals(listOf("cloud-browser", "photo.0", "photo.1"), two.state.attachments.map { it.id })
        assertEquals("Photo 2", two.state.attachments.last().title)
        val one = two.attachPickedPhotos(1)
        assertEquals(listOf(ComposerAttachment.CloudBrowser, ChatComposerFixture.photoAttachment(0)), one.state.attachments)
        assertEquals("An empty pick (cancel) changes nothing", one, one.attachPickedPhotos(0))
    }

    @Test fun pickedFilesBecomeNamedFileChipsOnceAndAreRemovable() {
        val fixture = ChatComposerFixture().attachPickedPhotos(1)
            .attachPickedFiles(listOf("receipt.png", "map.heic"))
            .attachPickedFiles(listOf("receipt.png"))
        assertEquals(listOf("photo.0", "file.receipt.png", "file.map.heic"), fixture.state.attachments.map { it.id })
        assertEquals(listOf("receipt.png", "map.heic"), fixture.state.attachments.filter { it.kind == ComposerAttachment.Kind.File }.map { it.title })
        val removed = fixture.apply(ChatComposerAction.RemoveAttachment("file.receipt.png"))
        assertEquals(listOf("photo.0", "file.map.heic"), removed.state.attachments.map { it.id })
        assertTrue("Picked content alone can be sent", removed.state.canSend)
    }

    @Test fun scenariosDriveHostStates() {
        val base = ChatComposerFixture(state = ChatComposerState(draft = "Hello"))
        val unavailable = base.select(Scenario.Unavailable)
        assertEquals(ComposerAvailability.Disabled(ChatComposerFixture.UnavailableReason), unavailable.state.availability)
        assertFalse(unavailable.state.canSend)
        val noVoice = unavailable.select(Scenario.NoVoice)
        assertFalse(noVoice.state.showsSpeak)
        assertTrue(noVoice.state.canSend)
        val streaming = noVoice.select(Scenario.Streaming)
        assertEquals(ChatComposerAction.Cancel, streaming.state.primaryAction)
        assertEquals("Scenarios keep the draft", "Hello", streaming.state.draft)
    }

    @Test fun stopCancelsBackToReadyAndNeverSends() {
        val sending = ChatComposerFixture(state = ChatComposerState(draft = "Hello")).select(Scenario.Sending).apply(ChatComposerAction.Send)
        assertEquals(emptyList<String>(), sending.sent)
        val cancelled = sending.apply(ChatComposerAction.Cancel)
        assertEquals(Scenario.Ready, cancelled.scenario)
        assertEquals(ComposerPhase.Idle, cancelled.state.phase)
        assertEquals(ChatComposerFixture.CancelNote, cancelled.note)
    }

    @Test fun speakOnlyWhenOfferedAndFocusIsRecorded() {
        val silent = ChatComposerFixture().select(Scenario.NoVoice).apply(ChatComposerAction.Speak)
        assertNull(silent.note)
        val spoken = silent.select(Scenario.Ready).apply(ChatComposerAction.Speak)
        assertEquals(ChatComposerFixture.SpeakNote, spoken.note)
        assertTrue(spoken.apply(ChatComposerAction.FocusChanged(true)).state.isFocused)
    }
}
