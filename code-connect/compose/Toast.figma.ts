// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=72-24
// source=compose/RemDesignSystem/feedback/RemToast.kt
// component=RemToast
import figma from 'figma'

const instance = figma.selectedInstance
const variant = instance.getEnum('Variant', {
  info: 'RemToastVariant.Info',
  success: 'RemToastVariant.Success',
  warning: 'RemToastVariant.Warning',
  error: 'RemToastVariant.Error',
})
const message = instance.getString('Message')

export default {
  example: figma.code`
    RemToast(message = ${message}, variant = ${variant})
  `,
  imports: [
    'import com.rem.designsystem.feedback.RemToast',
    'import com.rem.designsystem.feedback.RemToastVariant',
  ],
  id: 'rem-toast-compose',
}
