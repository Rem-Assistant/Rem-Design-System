// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=868-210
// source=Sources/RemDesignSystem/Controls/RemSwitch.swift
// component=RemSwitch
import figma from 'figma'

const instance = figma.selectedInstance
const isOn = instance.getBoolean('State')

export default {
  example: figma.code`
    RemSwitch(isOn: ${isOn ? 'true' : 'false'}, onChange: onChange)
  `,
  imports: ['import RemDesignSystem'],
  id: 'rem-switch-swiftui',
}
