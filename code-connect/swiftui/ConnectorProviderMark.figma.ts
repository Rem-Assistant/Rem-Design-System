// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=1328-440
// source=Sources/RemDesignSystem/Rows/ConnectorRow.swift
// component=ConnectorProviderMark
import figma from 'figma'
const instance = figma.selectedInstance
// Seven Settings-used Brand values from the saved source packet; the full variant domain
// awaits fresh inspection. Unknown values remain diagnostic, never a guessed provider.
const provider = instance.getEnum('Brand', {
    'Gmail': '.gmail',
    'Google Calendar': '.googleCalendar',
    'Notion': '.notion',
    'Slack': '.slack',
    'Google Drive': '.googleDrive',
    'Linear': '.linear',
    'Todoist': '.todoist',
})
export default {
    example: provider ? figma.code`ConnectorProviderMark(${provider})`
        : figma.code`// Unmapped connector brand. Verify the source variant and its original asset before implementation.`,
    imports: ['import RemDesignSystem'],
    id: 'rem-connector-provider-mark-swiftui',
    metadata: { nestable: true, props: { provider } },
}
