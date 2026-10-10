// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2566-2645
// source=compose/RemDesignSystem/chat/ActionReceipt.kt
// component=ActionReceipt
import figma from 'figma'
// Non-interactive outcome receipt. `Outcome` is read from the verified variant names
// (`Outcome=Confirmed` 2566:2641, `Outcome=Unconfirmed` 2566:2643). The contextual label lives on the
// nested Button and is host copy, so it stays a variable rather than a guessed property name.
const instance = figma.selectedInstance
const outcome = instance.getEnum('Outcome', {
    Confirmed: 'ActionReceiptOutcome.Confirmed', Unconfirmed: 'ActionReceiptOutcome.Unconfirmed',
})
export default {
    example: figma.code`ActionReceipt(${outcome}, label = label)`,
    imports: ['import com.rem.designsystem.chat.ActionReceipt', 'import com.rem.designsystem.chat.ActionReceiptOutcome'],
    id: 'rem-action-receipt-compose',
    metadata: { nestable: true, props: { outcome } },
}
