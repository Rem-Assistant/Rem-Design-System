// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=741-311
// source=compose/RemDesignSystem/rows/RemSection.kt
// component=RemSection
import figma from 'figma'

const instance = figma.selectedInstance
const rows = instance.getSlot('Rows')
const showHeader = instance.getBoolean('Show Header')
const showFooter = instance.getBoolean('Show Footer')

export default {
  example: figma.code`
    RemSection(
      header = ${showHeader ? '"Section Header"' : 'null'},
      footer = ${showFooter ? '"Explanatory footer text."' : 'null'},
    ) {
      ${rows}
    }
  `,
  imports: ['import com.rem.designsystem.rows.RemSection'],
  id: 'rem-section-compose',
  metadata: { nestable: true },
}
