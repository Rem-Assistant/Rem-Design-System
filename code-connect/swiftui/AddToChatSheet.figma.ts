// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2656-21214
// source=Sources/RemDesignSystem/Chat/AddToChatSheet.swift
// component=AddToChatSheet
import figma from 'figma'
// Add to Chat sheet with the Thinking menu (2660:128572). Hosts present the real pickers.
// Static example by design: the master's exact Figma property names could not be verified for this
// slice (the Code Connect context API needs a Dev seat), so no property is read and none is guessed.
// The call below is the real API; bind properties once their names are confirmed.
export default {
    example: figma.code`AddToChatSheet(showsCamera: showsCamera, browserAvailable: browserAvailable, thinking: $thinking, onCamera: onCamera, onPhotos: onPhotos, onFiles: onFiles, onCloudBrowser: onCloudBrowser, onDone: onDone)`,
    imports: ['import RemDesignSystem'],
    id: 'rem-add-to-chat-sheet-swiftui',
    metadata: { nestable: true },
}
