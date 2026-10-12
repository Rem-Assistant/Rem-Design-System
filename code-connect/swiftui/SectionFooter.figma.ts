// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=161-70
// source=Sources/RemDesignSystem/Rows/RemSection.swift
// component=SectionFooter
import figma from 'figma'
const instance = figma.selectedInstance
const text = instance.getString('Footer')
export default {
    example: figma.code`Text("${text}").font(.footnote).foregroundStyle(DesignTokens.Color.labelSecondary)`,
    imports: ['import SwiftUI', 'import RemDesignSystem'],
    id: 'rem-section-footer-swiftui', metadata: { nestable: true },
}
