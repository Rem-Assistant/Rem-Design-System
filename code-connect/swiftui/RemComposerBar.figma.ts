// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2071-11555
// source=Sources/RemDesignSystem/Chat/RemComposerBar.swift
// component=RemComposerBar
import figma from 'figma'
// Composer with the Auto secondary-pill model trigger. Providers/models are runtime data.
// Static example by design: the master's exact Figma property names could not be verified for this
// slice (the Code Connect context API needs a Dev seat), so no property is read and none is guessed.
// The call below is the real API; bind properties once their names are confirmed.
export default {
    example: figma.code`RemComposerBar(text: $draft, state: state, attachments: attachments, modelMenu: ChatModelMenu(providers: providers, selection: selection, onSelect: onSelectModel, onManageModels: onManageModels), onAdd: onAdd, onRemoveAttachment: onRemoveAttachment, onSend: onSend)`,
    imports: ['import RemDesignSystem'],
    id: 'rem-composer-bar-swiftui',
    metadata: { nestable: true },
}
