// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2654-20860
// source=compose/RemDesignSystem/chat/MessageReactionBadge.kt
// component=MessageReactionBadge
import figma from 'figma'
// Reaction badge. The emoji is data (MessageReaction), not a Figma asset.
// Static example by design: the master's exact Figma property names could not be verified for this
// slice (the Code Connect context API needs a Dev seat), so no property is read and none is guessed.
// The call below is the real API; bind properties once their names are confirmed.
export default {
    example: figma.code`MessageReactionBadge(reaction = reaction)`,
    imports: ['import com.rem.designsystem.chat.MessageReactionBadge'],
    id: 'rem-message-reaction-badge-compose',
    metadata: { nestable: true },
}
