// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2559-1524
// source=compose/RemDesignSystem/chat/PollCard.kt
// component=PollCard
import figma from 'figma'
// Choice/suggestion card. `Purpose` and `State` are read from the verified variant names
// (458:56, 2559:1504, 2559:1510, 2559:1516). Question and options are host data. The master only has
// A/B options; three or more options extend the same lettering in code (no verified C master).
const instance = figma.selectedInstance
const purpose = instance.getEnum('Purpose', { Choice: 'PollPurpose.Choice', Suggestion: 'PollPurpose.Suggestion' })
const state = instance.getEnum('State', { Awaiting: 'Awaiting', Answered: 'Answered' })
export default {
    example: state === 'Answered'
        ? figma.code`PollCard(question, options, purpose = ${purpose}, selection = selectedOptionId, onSelect = onSelect)`
        : figma.code`PollCard(question, options, purpose = ${purpose}, onSelect = onSelect)`,
    imports: ['import com.rem.designsystem.chat.PollCard', 'import com.rem.designsystem.chat.PollPurpose'],
    id: 'rem-poll-card-compose',
    metadata: { nestable: false, props: { purpose, state } },
}
