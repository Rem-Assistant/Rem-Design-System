// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=614-8
// source=compose/RemDesignSystem/primitives/ContainedIcon.kt
// component=ContainedIcon
import figma from 'figma'
const instance = figma.selectedInstance
// Verified with figma.util.getSfSymbolCharacter on 2026-10-08; never pass glyph text as a systemName.
const symbols: Record<string, string> = {
    '\u{100175}': 'RemMaterialSymbols.Info',
    '\u{100370}': 'RemMaterialSymbols.Billing',
    '\u{10027c}': 'RemMaterialSymbols.Permissions',
    '\u{100202}': 'RemMaterialSymbols.Share',
    '\u{100b29}': 'RemMaterialSymbols.Devices',
    '\u{1004a1}': 'RemMaterialSymbols.Connectors',
    '\u{1001aa}': 'RemMaterialSymbols.Browser',
    '\u{100757}': 'RemMaterialSymbols.Automations',
    '\u{100bcf}': 'RemMaterialSymbols.Memory',
    '\u{100ae5}': 'RemMaterialSymbols.Models',
    '\u{1007fe}': 'RemMaterialSymbols.Wallet',
    '\u{10066b}': 'RemMaterialSymbols.Voice',
}
const symbol = symbols[instance.getString('Symbol')]
const style = instance.getEnum('Style', { 'Tinted': 'ContainedIconFill.Tint(RemColors.current.systemBlue)', 'Subtle': 'ContainedIconFill.Subtle' })
// Approved utility contract: primary semantic glyph on the gray Subtle container.
// Explicit Tinted variants retain their accent/status/brand treatment.
// Verified Figma Small614:4/Large614:6: label/primary2:6 on background/secondary2:4.
const backgroundToken = instance.getEnum('Style', { 'Tinted': 'systemBlue', 'Subtle': 'backgroundSecondary' })
const foregroundToken = instance.getEnum('Style', { 'Tinted': 'labelOnColor', 'Subtle': 'labelPrimary' })
const size = instance.getEnum('Size', { 'Small': 'ContainedIconSize.Settings', 'Large': 'ContainedIconSize.Large' })
// Figma primitive Small is 29/radius7/glyph15. Settings rows explicitly use glyph17.
const glyphSize = instance.getEnum('Size', { 'Small': 15, 'Large': 30 })
export default {
    example: symbol ? figma.code`ContainedIcon(${symbol}, fill = ${style}, size = ${size}, glyphSize = ${glyphSize}.dp)` : figma.code`// Unmapped Figma SF Symbol glyph. Verify its semantic registry identity before implementation.`,
    imports: ['import com.rem.designsystem.primitives.*', 'import com.rem.designsystem.icons.RemMaterialSymbols', 'import com.rem.designsystem.tokens.RemColors', 'import androidx.compose.ui.unit.dp'],
    id: 'rem-contained-icon-compose', metadata: { nestable: true, props: { symbol, style, size, foregroundToken, backgroundToken } },
}
