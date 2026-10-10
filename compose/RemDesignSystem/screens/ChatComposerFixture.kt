package com.rem.designsystem.screens

import com.rem.designsystem.chat.ComposerAttachment

// Chat composer fixture (presentation-only) — Compose twin of `Screens/ChatComposerFixture.swift`.
//
// The deterministic, in-memory host behind the Playground's canonical composer, mirroring the Settings
// fixture pattern. It drives `RemComposerBar(state, onAction)` exactly as an app adapter would, so the
// Playground and the app render the same component and rules. It is not an app adapter and claims
// nothing about runtime truth: nothing is sent, [sent] is local fixture copy, and no delivery or read
// state is produced here. Immutable: every operation returns the next fixture.

data class ChatComposerFixture(
    val state: ChatComposerState = ChatComposerState(),
    val scenario: Scenario = Scenario.Ready,
    /** Local fixture copy of what Send produced. Never transmitted. */
    val sent: List<String> = emptyList(),
    /** A fixture note explaining what the app would do for an action the Playground cannot perform. */
    val note: String? = null,
) {
    /** The host situations the Playground can switch between to review each presentation state. */
    enum class Scenario(val label: String) {
        Ready("Ready"),
        Sending("Sending"),
        Streaming("Streaming"),
        Unavailable("Unavailable"),
        NoVoice("No voice"),
    }

    /** Switch the host situation. The draft and attachments are kept. */
    fun select(scenario: Scenario): ChatComposerFixture = copy(
        scenario = scenario,
        state = state.copy(
            phase = when (scenario) {
                Scenario.Sending -> ComposerPhase.Sending
                Scenario.Streaming -> ComposerPhase.Streaming
                else -> ComposerPhase.Idle
            },
            availability = if (scenario == Scenario.Unavailable) ComposerAvailability.Disabled(UnavailableReason) else ComposerAvailability.Enabled,
            voiceAvailable = scenario != Scenario.NoVoice,
        ),
    )

    /** Apply one composer action the way a host would. [ChatComposerAction.Add] is the page's concern. */
    fun apply(action: ChatComposerAction): ChatComposerFixture = when (action) {
        is ChatComposerAction.DraftChanged -> copy(state = state.copy(draft = action.text))
        ChatComposerAction.Send -> if (!state.canSend) this else {
            val text = state.draft.trim()
            val content = state.attachments.filter { it.kind != ComposerAttachment.Kind.Capability }
            copy(
                sent = sent + (text.ifEmpty { content.joinToString(", ") { it.title } }),
                state = state.copy(draft = "", attachments = state.attachments.filter { it.kind == ComposerAttachment.Kind.Capability }),
                note = null,
            )
        }
        ChatComposerAction.Cancel -> if (!state.canCancel) this else select(Scenario.Ready).copy(note = CancelNote)
        ChatComposerAction.Speak -> if (!state.showsSpeak) this else copy(note = SpeakNote)
        ChatComposerAction.Add -> this
        is ChatComposerAction.RemoveAttachment -> copy(state = state.copy(attachments = state.attachments.filter { it.id != action.id }))
        is ChatComposerAction.FocusChanged -> copy(state = state.copy(isFocused = action.focused))
    }

    /** Adds an attachment once (by id), as Add to Chat does. */
    fun attach(attachment: ComposerAttachment): ChatComposerFixture =
        if (state.attachments.any { it.id == attachment.id }) this else copy(state = state.copy(attachments = state.attachments + attachment))

    /** Replaces every attachment of [kind] (a new photo pick replaces the previous one). */
    fun replaceAttachments(kind: ComposerAttachment.Kind, attachments: List<ComposerAttachment>): ChatComposerFixture =
        copy(state = state.copy(attachments = state.attachments.filter { it.kind != kind } + attachments))

    /**
     * Photos picked in the system photo picker: one image chip per pick ("Photo 1"…), replacing the
     * previous photo pick. Only the count is used; picked content stays on the device and is never read.
     */
    fun attachPickedPhotos(count: Int): ChatComposerFixture =
        if (count <= 0) this else replaceAttachments(ComposerAttachment.Kind.Image, (0 until count).map(::photoAttachment))

    /**
     * Documents picked in the system document picker, one file chip per name, each added once.
     * Only the display name is used; the file is never opened, read or uploaded.
     */
    fun attachPickedFiles(names: List<String>): ChatComposerFixture =
        names.fold(this) { fixture, name -> fixture.attach(fileAttachment(name)) }

    /** Shows a page-level note (e.g. Manage Models, Camera). */
    fun show(note: String?): ChatComposerFixture = copy(note = note)

    companion object {
        const val UnavailableReason = "Chat is unavailable in this fixture."
        const val CancelNote = "Stop asks the app to cancel the turn."
        const val SpeakNote = "Speak starts voice input in the app."

        /** The chip for the photo at [index] of a pick (`photo.0` → "Photo 1"). */
        fun photoAttachment(index: Int) = ComposerAttachment("photo.$index", "Photo ${index + 1}", ComposerAttachment.Kind.Image)

        /** The chip for a picked document, keyed by its display name. */
        fun fileAttachment(name: String) = ComposerAttachment("file.$name", name, ComposerAttachment.Kind.File)
    }
}
