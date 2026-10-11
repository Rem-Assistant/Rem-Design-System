// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2336-19583
// source=compose/RemDesignSystem/rows/AgendaSuggestionRow.kt
// component=AgendaSuggestionRow
import figma from 'figma'
const instance = figma.selectedInstance
// Verified variant property `action` (add 2336:19561 / move 2336:19572) + Title/Metadata text props
// per docs/playground/expansion-design/COVERAGE.md and playground-source-contracts.json.
const action = instance.getEnum('action', { add: 'SuggestionAccept.Add', move: 'SuggestionAccept.Move' })
const title = instance.getString('Title')
const metadata = instance.getString('Metadata')
export default {
    // Host owns the optimistic model: Add/Move accept, ✕ dismisses. No spinner/badge/error/Retry.
    example: figma.code`AgendaSuggestionRow(action = ${action}, title = "${title}", metadata = "${metadata}", onAccept = onAccept, onDismiss = onDismiss)`,
    imports: ['import com.rem.designsystem.rows.AgendaSuggestionRow', 'import com.rem.designsystem.rows.SuggestionAccept'],
    id: 'rem-agenda-suggestion-row-compose', metadata: { nestable: true, props: { action, title, metadata } },
}
