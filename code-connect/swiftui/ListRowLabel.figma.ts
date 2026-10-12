// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=1966-60442
// source=Sources/RemDesignSystem/Rows/ListRowLabel.swift
// component=ListRowLabel
import figma from 'figma'
const instance = figma.selectedInstance
const title = instance.getString('Title')
const subtitle = instance.getString('Subtitle')
const showSubtitle = instance.getBoolean('Show Subtitle')
const accessory = instance.getBoolean('Show title accessory') ? instance.getSlot('Title accessory') : undefined
const layout = instance.getEnum('Title layout', { 'Fill': '.fill', 'Hug': '.hug' })
export default {
    example: figma.code`ListRowLabel("${title}", subtitle: ${showSubtitle ? figma.code`"${subtitle}"` : 'nil'}, titleLayout: ${layout}) {
        ${accessory}
    }`,
    imports: ['import RemDesignSystem'],
    id: 'rem-list-row-label-swiftui', metadata: { nestable: true },
}
