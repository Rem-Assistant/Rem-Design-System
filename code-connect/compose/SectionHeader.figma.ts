// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=161-68
// source=compose/RemDesignSystem/rows/RemSection.kt
// component=SectionHeader
import figma from 'figma'
const instance = figma.selectedInstance
const text = instance.getString('Header')
const action = instance.getBoolean('Show trailing action') ? instance.getSlot('Trailing action') : undefined
export default {
    example: figma.code`Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${text}", modifier = Modifier.weight(1f), style = RemTypography.bodyBold, color = RemColors.current.labelSecondary)
        ${action}
    }`,
    imports: ['import androidx.compose.foundation.layout.*', 'import androidx.compose.material3.Text', 'import androidx.compose.ui.Alignment', 'import androidx.compose.ui.Modifier', 'import com.rem.designsystem.tokens.*'],
    id: 'rem-section-header-compose', metadata: { nestable: true },
}
