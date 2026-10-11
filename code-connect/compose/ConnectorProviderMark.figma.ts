// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=1328-440
// source=compose/RemDesignSystem/rows/ConnectorRow.kt
// component=ConnectorProviderMark
import figma from 'figma'
const instance = figma.selectedInstance
// Seven Settings-used Brand values from the saved source packet; the full variant domain
// awaits fresh inspection. Unknown values remain diagnostic, never a guessed provider.
const provider = instance.getEnum('Brand', {
    'Gmail': 'Gmail',
    'Google Calendar': 'GoogleCalendar',
    'Notion': 'Notion',
    'Slack': 'Slack',
    'Google Drive': 'GoogleDrive',
    'Linear': 'Linear',
    'Todoist': 'Todoist',
})
export default {
    example: provider ? figma.code`ConnectorProviderMark(ConnectorProvider.${provider})`
        : figma.code`// Unmapped connector brand. Verify the source variant and its original asset before implementation.`,
    imports: ['import com.rem.designsystem.rows.ConnectorProvider', 'import com.rem.designsystem.rows.ConnectorProviderMark'],
    id: 'rem-connector-provider-mark-compose',
    metadata: { nestable: true, props: { provider } },
}
