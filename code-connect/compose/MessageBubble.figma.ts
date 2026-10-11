// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2000-3605
// source=compose/RemDesignSystem/chat/MessageBubble.kt
// component=MessageBubble
import figma from 'figma'
// Outgoing master (Rem/Chat/Outgoing). Incoming uses role .assistant on the same component.
// Static example by design: the master's exact Figma property names could not be verified for this
// slice (the Code Connect context API needs a Dev seat), so no property is read and none is guessed.
// The call below is the real API; bind properties once their names are confirmed.
export default {
    example: figma.code`MessageBubble(text = text, role = MessageRole.User, delivery = delivery, reaction = reaction, onRetry = onRetry, onLongPress = onLongPress)`,
    imports: ['import com.rem.designsystem.chat.MessageBubble', 'import com.rem.designsystem.chat.MessageDelivery', 'import com.rem.designsystem.chat.MessageRole'],
    id: 'rem-message-bubble-compose',
    metadata: { nestable: true },
}
