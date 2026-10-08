// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=614-8
// source=Sources/RemDesignSystem/Primitives/ContainedIcon.swift
// component=ContainedIcon
import figma from 'figma'
const instance = figma.selectedInstance
// Verified with figma.util.getSfSymbolCharacter on 2026-10-08; never pass glyph text as a systemName.
const symbols: Record<string, string> = {
    '\u{100175}': 'info.circle.fill',
    '\u{100370}': 'creditcard.fill',
    '\u{10027c}': 'hand.raised.fill',
    '\u{100202}': 'square.and.arrow.up',
    '\u{100b29}': 'macbook.and.iphone',
    '\u{1004a1}': 'link.circle.fill',
    '\u{1001aa}': 'globe',
    '\u{100757}': 'bell.badge.fill',
    '\u{100bcf}': 'brain.head.profile',
    '\u{100ae5}': 'cpu',
    '\u{1007fe}': 'wallet.pass',
    '\u{10066b}': 'waveform',
}
const symbol = symbols[instance.getString('Symbol')]
const style = instance.getEnum('Style', { 'Tinted': '.tint(DesignTokens.Color.systemBlue)', 'Subtle': '.subtle' })
// Approved utility contract: primary semantic glyph on the gray Subtle container.
// Explicit Tinted variants retain their accent/status/brand treatment.
// Verified Figma Small614:4/Large614:6: label/primary2:6 on background/secondary2:4.
const backgroundToken = instance.getEnum('Style', { 'Tinted': 'systemBlue', 'Subtle': 'backgroundSecondary' })
const foregroundToken = instance.getEnum('Style', { 'Tinted': 'labelOnColor', 'Subtle': 'labelPrimary' })
const size = instance.getEnum('Size', { 'Small': '.settings', 'Large': '.large' })
// Figma primitive Small is 29/radius7/glyph15. Settings rows explicitly use glyph17.
const glyphSize = instance.getEnum('Size', { 'Small': 15, 'Large': 30 })
export default {
    example: symbol ? figma.code`ContainedIcon("${symbol}", fill: ${style}, size: ${size}, glyphWeight: .regular, glyphPointSize: ${glyphSize})` : figma.code`// Unmapped Figma SF Symbol glyph. Verify its semantic registry identity before implementation.`,
    imports: ['import RemDesignSystem'],
    id: 'rem-contained-icon-swiftui', metadata: { nestable: true, props: { symbol, style, size, foregroundToken, backgroundToken } },
}
