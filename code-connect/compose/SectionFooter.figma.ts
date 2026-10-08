// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=161-70
// source=compose/RemDesignSystem/rows/RemSection.kt
// component=SectionFooter
import figma from 'figma'
const instance = figma.selectedInstance
const text = instance.getString('Footer')
export default {
    example: figma.code`Text("${text}", style = RemTypography.footnote, color = RemColors.current.labelSecondary)`,
    imports: ['import androidx.compose.foundation.layout.*', 'import androidx.compose.material3.Text', 'import androidx.compose.ui.Alignment', 'import androidx.compose.ui.Modifier', 'import com.rem.designsystem.tokens.*'],
    id: 'rem-section-footer-compose', metadata: { nestable: true },
}
