// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=101-18
// source=Sources/RemDesignSystem/Rows/ListRow.swift
// component=ListRow
import figma from 'figma'
const instance = figma.selectedInstance
const leading = instance.getBoolean('Show Leading') ? instance.getInstanceSwap('Leading Accessory') : null
const content = instance.getInstanceSwap('Content')
const trailing = instance.getInstanceSwap('Trailing Accessory')
const leadingCode = leading?.type === 'INSTANCE' ? leading.executeTemplate().example : undefined
const contentCode = content?.type === 'INSTANCE' ? content.executeTemplate().example : undefined
// Native NavigationLink owns the canonical disclosure; preserve other accessories dynamically.
const trailingCode = trailing?.type === 'INSTANCE' && trailing.name !== 'Chevron' ? trailing.executeTemplate().example : undefined
const divider = instance.getBoolean('Show Divider')
export default {
    example: figma.code`// Native List row content. The host supplies NavigationLink or Button behavior.
    ListRow(layout: .nativeList, leading: {
        ${leadingCode}
    }, content: {
        ${contentCode}
    }, trailing: {
        ${trailingCode}
    }).listRowSeparator(${divider ? '.visible' : '.hidden'})`,
    imports: ['import RemDesignSystem'],
    id: 'rem-list-row-native-swiftui', metadata: { nestable: true },
}
