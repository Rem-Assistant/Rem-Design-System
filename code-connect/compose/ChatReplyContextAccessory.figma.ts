// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2682-22298
// source=compose/RemDesignSystem/chat/ChatReplyContextAccessory.kt
// component=ChatReplyContextAccessory
import figma from 'figma'
// Task-reply context above the same composer. The two public text properties are documented in the
// 2026-10-10 handoff (`Replying to#2682:0`, `Reply summary#2682:1`); the reply target id is host data.
const instance = figma.selectedInstance
const quote = (value: string) => JSON.stringify(value).replace(/\$/g, '\\$')
const title = quote(instance.getString('Replying to'))
const summary = quote(instance.getString('Reply summary'))
export default {
    example: figma.code`ChatReplyContextAccessory(ChatReplyContext(targetId, ${title}, ${summary}), onDismiss = onDismiss)`,
    imports: ['import com.rem.designsystem.chat.ChatReplyContextAccessory', 'import com.rem.designsystem.screens.ChatReplyContext'],
    id: 'rem-chat-reply-context-compose',
    metadata: { nestable: true },
}
