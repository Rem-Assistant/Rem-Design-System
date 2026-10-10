// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2555-1550
// source=compose/RemDesignSystem/chat/MessageDraftCard.kt
// component=MessageDraftCard
import figma from 'figma'
// Outgoing-message review card. `State` is read from the verified variant names (Review 458:69,
// Sent 2555:1499, Unconfirmed 2555:1517). The From/To/Subject/Body payload is host data (the master's
// sample copy is private), so it is passed as `draft` and no text property name is guessed.
const instance = figma.selectedInstance
const state = instance.getEnum('State', {
    Review: 'MessageDraftCardState.Review', Sent: 'MessageDraftCardState.Sent', Unconfirmed: 'MessageDraftCardState.Unconfirmed',
})
export default {
    example: figma.code`MessageDraftCard(draft, state = ${state}, onSend = onSend, onDiscard = onDiscard)`,
    imports: ['import com.rem.designsystem.chat.MessageDraftCard', 'import com.rem.designsystem.chat.MessageDraftCardState'],
    id: 'rem-message-draft-card-compose',
    metadata: { nestable: false, props: { state } },
}
