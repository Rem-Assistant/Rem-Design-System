// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2000-3605
// source=Sources/RemDesignSystem/Chat/MessageBubble.swift
// component=MessageBubble
import figma from 'figma'
// Outgoing master (Rem/Chat/Outgoing). Incoming uses role .assistant on the same component.
// Static example by design: the master's exact Figma property names could not be verified for this
// slice (the Code Connect context API needs a Dev seat), so no property is read and none is guessed.
// The call below is the real API; bind properties once their names are confirmed.
export default {
    example: figma.code`MessageBubble(text, role: .user, delivery: delivery, reaction: reaction, onRetry: onRetry, onLongPress: onLongPress)`,
    imports: ['import RemDesignSystem'],
    id: 'rem-message-bubble-swiftui',
    metadata: { nestable: true },
}
