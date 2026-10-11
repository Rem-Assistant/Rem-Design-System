// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=161-68
// source=Sources/RemDesignSystem/Rows/RemSection.swift
// component=SectionHeader
import figma from 'figma'
const instance = figma.selectedInstance
const text = instance.getString('Header')
const action = instance.getBoolean('Show trailing action') ? instance.getSlot('Trailing action') : undefined
export default {
    example: figma.code`HStack { Text("${text}").font(.headline); Spacer(); ${action} }.foregroundStyle(DesignTokens.Color.labelSecondary)`,
    imports: ['import SwiftUI', 'import RemDesignSystem'],
    id: 'rem-section-header-swiftui', metadata: { nestable: true },
}
