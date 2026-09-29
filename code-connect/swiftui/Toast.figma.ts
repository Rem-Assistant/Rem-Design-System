// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=72-24
// source=Sources/RemDesignSystem/Feedback/RemToast.swift
// component=RemToast
import figma from 'figma'

const instance = figma.selectedInstance
const variant = instance.getEnum('Variant', {
  info: '.info',
  success: '.success',
  warning: '.warning',
  error: '.error',
})
const message = instance.getString('Message')

export default {
  example: figma.code`
    RemToast(variant: ${variant}, message: ${message})
  `,
  imports: ['import RemDesignSystem'],
  id: 'rem-toast-swiftui',
}
