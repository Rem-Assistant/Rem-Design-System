// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=1307-667
// source=Sources/RemDesignSystem/Rows/RemSection.swift
// component=RemSection
import figma from 'figma'
const instance = figma.selectedInstance
const rows = instance.getSlot('Rows')
const header = instance.getBoolean('Show Header') ? instance.findInstance('SectionHeader') : null
const footer = instance.getBoolean('Show Footer') ? instance.findInstance('SectionFooter') : null
const headerCode = header?.type === 'INSTANCE' ? header.executeTemplate().example : undefined
const footerCode = footer?.type === 'INSTANCE' ? footer.executeTemplate().example : undefined
const style = instance.getEnum('Style', { 'Inset Grouped': '.insetGrouped', 'Plain': '.plain' })
export default {
    example: figma.code`// Custom ScrollView context; do not place this card inside native List.
    RemSection(style: ${style}, header: {
        ${headerCode}
    }, footer: {
        ${footerCode}
    }) {
        ${rows}
    }`,
    imports: ['import RemDesignSystem'],
    id: 'rem-section-custom-scroll-swiftui', metadata: { nestable: true },
}
