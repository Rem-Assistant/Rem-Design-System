// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=741-311
// source=Sources/RemDesignSystem/Rows/RemSection.swift
// component=RemSection
import figma from 'figma'

const instance = figma.selectedInstance
const rows = instance.getSlot('Rows')
const showHeader = instance.getBoolean('Show Header')
const showFooter = instance.getBoolean('Show Footer')

export default {
  example: figma.code`
    RemSection(
      header: ${showHeader ? '"Section Header"' : 'nil'},
      footer: ${showFooter ? '"Explanatory footer text."' : 'nil'}
    ) {
      ${rows}
    }
  `,
  imports: ['import RemDesignSystem'],
  id: 'rem-section-swiftui',
  metadata: { nestable: true },
}
