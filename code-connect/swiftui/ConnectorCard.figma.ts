// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2567-2666
// source=Sources/RemDesignSystem/Chat/ConnectorCard.swift
// component=ConnectorCard
import figma from 'figma'
// Only the variant property is read: `State` and its four values are evidenced by the variant names
// (State=Authorize 2567:2639 / Connecting 2567:2646 / Added 2567:2653 / Error 2567:2658). Text property
// names could not be verified (the Code Connect context API needs a Dev seat), so provider, purpose
// and error copy stay host data. Unknown states are diagnostic, never a guessed state.
const instance = figma.selectedInstance
const state = instance.getEnum('State', {
    Authorize: '.authorize',
    Connecting: '.connecting',
    Added: '.added',
    Error: '.error(message: errorMessage)',
})
export default {
    example: state
        ? figma.code`ConnectorCard(ConnectorCardModel(provider: provider, subtitle: purpose, state: ${state}), onAuthorize: onAuthorize, onRetry: onRetry)`
        : figma.code`// Unsupported ConnectorCard State. Verify the Figma variant before implementation.`,
    imports: ['import RemDesignSystem'],
    id: 'rem-connector-card-swiftui',
    metadata: { nestable: true, props: { state } },
}
