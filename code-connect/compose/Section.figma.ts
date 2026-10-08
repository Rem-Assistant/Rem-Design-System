// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=1307-667
// source=compose/RemDesignSystem/rows/RemSection.kt
// component=RemSection
import figma from 'figma'
const instance = figma.selectedInstance
const rows = instance.getSlot('Rows')
const header = instance.getBoolean('Show Header') ? instance.findInstance('SectionHeader') : null
const footer = instance.getBoolean('Show Footer') ? instance.findInstance('SectionFooter') : null
const headerCode = header?.type === 'INSTANCE' ? header.executeTemplate().example : undefined
const footerCode = footer?.type === 'INSTANCE' ? footer.executeTemplate().example : undefined
const style = instance.getEnum('Style', { 'Inset Grouped': 'RemSectionStyle.InsetGrouped', 'Plain': 'RemSectionStyle.Plain' })
export default {
    example: figma.code`RemSection(header = {
        ${headerCode}
    }, footer = {
        ${footerCode}
    }, style = ${style}) {
        ${rows}
    }`,
    imports: ['import com.rem.designsystem.rows.*'],
    id: 'rem-section-compose', metadata: { nestable: true },
}
