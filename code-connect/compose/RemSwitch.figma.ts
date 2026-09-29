// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=110-50
// source=compose/RemDesignSystem/controls/RemSwitch.kt
// component=RemSwitch
import figma from 'figma'

const instance = figma.selectedInstance
const isOn = instance.getBoolean('On')

export default {
  example: figma.code`
    RemSwitch(checked = ${isOn ? 'true' : 'false'}, onCheckedChange = onCheckedChange)
  `,
  imports: ['import com.rem.designsystem.controls.RemSwitch'],
  id: 'rem-switch-compose',
}
