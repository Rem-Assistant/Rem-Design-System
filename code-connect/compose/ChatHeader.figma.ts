// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2054-19725
// source=compose/RemDesignSystem/chat/ChatHeader.kt
// component=ChatHeader
import figma from 'figma'
// Chat header: Rem face avatar with the agent identity pill and current activity.
// Static example by design: the master's exact Figma property names could not be verified for this
// slice (the Code Connect context API needs a Dev seat), so no property is read and none is guessed.
// The call below is the real API; bind properties once their names are confirmed.
export default {
    example: figma.code`ChatHeader(activity = activity, status = status)`,
    imports: ['import com.rem.designsystem.chat.ChatHeader'],
    id: 'rem-chat-header-compose',
    metadata: { nestable: true },
}
