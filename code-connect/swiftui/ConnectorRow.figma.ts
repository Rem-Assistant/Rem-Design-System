// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2213-9330
// source=Sources/RemDesignSystem/Rows/ConnectorRow.swift
// component=ConnectorRow
import figma from 'figma'
const instance = figma.selectedInstance
const state = instance.getEnum('State', { Available: '.available', Connecting: '.connecting', Connected: '.connected', Error: '.error' })
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
const control = accessory === 'action' ? figma.code`.action("${label}", onAction)`
    : accessory === 'progress' ? figma.code`.progress`
    : accessory === 'switch' ? figma.code`.toggle($isEnabled)` : figma.code`.disclosure`
export default {
    example: figma.code`// App owns actions and switch binding (Figma switch specimen: ${initialToggle}).
    // Native List: pass layout: .nativeList and wrap Disclosure in NavigationLink.
    ConnectorRow("${title}", state: ${state}, accessory: ${control}, showsDivider: ${divider}, leading: {
        ${leadingCode}
    }, content: {
        ${contentCode}
    })`,
    imports: ['import RemDesignSystem'],
    id: 'rem-connector-row-swiftui', metadata: { nestable: true },
}
