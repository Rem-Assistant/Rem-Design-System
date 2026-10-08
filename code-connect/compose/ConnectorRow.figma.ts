// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2213-9330
// source=compose/RemDesignSystem/rows/ConnectorRow.kt
// component=ConnectorRow
import figma from 'figma'
const instance = figma.selectedInstance
const state = instance.getEnum('State', { Available: 'Available', Connecting: 'Connecting', Connected: 'Connected', Error: 'Error' })
const accessory = instance.getEnum('Accessory', { Action: 'action', Progress: 'progress', Disclosure: 'disclosure', Switch: 'switch' })
const row = instance.findInstance('ListRow')
const divider = row.type === 'INSTANCE' ? row.getBoolean('Show Divider') : false
const leading = row.type === 'INSTANCE' && row.getBoolean('Show Leading') ? row.getInstanceSwap('Leading Accessory') : null
const content = row.type === 'INSTANCE' ? row.getInstanceSwap('Content') : null
const trailing = row.type === 'INSTANCE' ? row.getInstanceSwap('Trailing Accessory') : null
const leadingCode = leading?.type === 'INSTANCE' ? leading.executeTemplate().example : undefined
const contentCode = content?.type === 'INSTANCE' ? content.executeTemplate().example : undefined
const title = content?.type === 'INSTANCE' ? content.getString('Title') : ''
const label = accessory === 'action' && trailing?.type === 'INSTANCE' ? trailing.getString('Label') : ''
const initialToggle = accessory === 'switch' && trailing?.type === 'INSTANCE'
    ? trailing.getEnum('State', { On: true, Off: false }) : undefined
const control = accessory === 'action' ? figma.code`ConnectorRowAccessory.Action("${label}", onAction)`
    : accessory === 'progress' ? figma.code`ConnectorRowAccessory.Progress`
    : accessory === 'switch' ? figma.code`ConnectorRowAccessory.Toggle(isEnabled, onEnabledChange)`
    : figma.code`ConnectorRowAccessory.Disclosure`
export default {
    example: figma.code`// App owns actions and switch value (Figma switch specimen: ${initialToggle}).
    ConnectorRow("${title}", state = ConnectorRowState.${state}, accessory = ${control},
        showsDivider = ${divider}, onClick = ${accessory === 'disclosure' ? 'onOpen' : 'null'}, leading = {
            ${leadingCode}
        }, content = {
            ${contentCode}
        })`,
    imports: ['import com.rem.designsystem.rows.*'],
    id: 'rem-connector-row-compose', metadata: { nestable: true },
}
