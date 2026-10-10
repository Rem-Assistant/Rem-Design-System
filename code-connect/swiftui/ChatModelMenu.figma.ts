// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2656-128164
// source=Sources/RemDesignSystem/Chat/ChatModelMenu.swift
// component=ChatModelMenu
import figma from 'figma'
// Model menu: Automatic, provider submenus (2656:128245), Manage Models. Never a hardcoded catalog.
// Static example by design: the master's exact Figma property names could not be verified for this
// slice (the Code Connect context API needs a Dev seat), so no property is read and none is guessed.
// The call below is the real API; bind properties once their names are confirmed.
export default {
    example: figma.code`ChatModelMenu(providers: providers, selection: selection, onSelect: onSelect, onManageModels: onManageModels)`,
    imports: ['import RemDesignSystem'],
    id: 'rem-chat-model-menu-swiftui',
    metadata: { nestable: true },
}
