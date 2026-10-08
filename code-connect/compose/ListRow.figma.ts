// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=101-18
// source=compose/RemDesignSystem/rows/ListRow.kt
// component=ListRow
import figma from 'figma'
const instance = figma.selectedInstance
const leading = instance.getBoolean('Show Leading') ? instance.getInstanceSwap('Leading Accessory') : null
const content = instance.getInstanceSwap('Content')
const trailing = instance.getInstanceSwap('Trailing Accessory')
const leadingCode = leading?.type === 'INSTANCE' ? leading.executeTemplate().example : undefined
const contentCode = content?.type === 'INSTANCE' ? content.executeTemplate().example : undefined
const trailingCode = trailing?.type === 'INSTANCE' ? trailing.executeTemplate().example : undefined
const divider = instance.getBoolean('Show Divider')
export default {
    example: figma.code`ListRow(showsDivider = ${divider}, leading = {
        ${leadingCode}
    }, content = {
        ${contentCode}
    }, trailing = {
        ${trailingCode}
    })`,
    imports: ['import com.rem.designsystem.rows.*'],
    id: 'rem-list-row-compose', metadata: { nestable: true },
}
