// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=377-8
// source=compose/RemDesignSystem/buttons/RemButton.kt
// component=RemButton
import figma from 'figma'

const instance = figma.selectedInstance
const variant = instance.getEnum('Style', {
  'Rect · Black': 'RemButtonVariant.RectBlack',
  'Rect · Blue': 'RemButtonVariant.RectBlue',
  'Rect · Secondary': 'RemButtonVariant.RectSecondary',
  'Rect · Destructive': 'RemButtonVariant.RectDestructive',
  'Text · Accent': 'RemButtonVariant.TextAccent',
  'Text · Destructive': 'RemButtonVariant.TextDestructive',
  'Pill · Secondary': 'RemButtonVariant.PillSecondary',
})

export default {
  example: figma.code`
    RemButton(text = "Continue", onClick = onClick, variant = ${variant})
  `,
  imports: [
    'import com.rem.designsystem.buttons.RemButton',
    'import com.rem.designsystem.buttons.RemButtonVariant',
  ],
  id: 'rem-button-compose',
}
