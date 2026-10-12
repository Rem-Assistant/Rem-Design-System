// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2577-17461
// source=Sources/RemDesignSystem/Chat/PermissionCard.swift
// component=PermissionCard
import figma from 'figma'
// `State` × `Disclosure` are evidenced by the variant names (e.g. State=Awaiting, Disclosure=Expanded
// 2002:70251). The request payload is host data: text property names could not be verified (the Code
// Connect context API needs a Dev seat). Disclosure seeds the host-owned expanded binding.
// `risk` / `parameters` (elevated-risk label, Full parameters disclosure) are code-led with no Figma
// property yet; they default to standard / empty and are left to the host.
const instance = figma.selectedInstance
const state = instance.getEnum('State', { Awaiting: '.awaiting', Allowed: '.allowed', Denied: '.denied' })
const expanded = instance.getEnum('Disclosure', { Expanded: 'true', Collapsed: 'false' })
export default {
    example: state && expanded
        ? figma.code`// Host state: @State private var isExpanded = ${expanded}
PermissionCard(PermissionCardModel(title: title, question: question, summary: summary, details: details, alwaysAllowScope: alwaysAllowScope, state: ${state}), isExpanded: $isExpanded, onAllow: onAllow, onAlwaysAllow: onAlwaysAllow, onDeny: onDeny, onReviewAgain: onReviewAgain)`
        : figma.code`// Unsupported PermissionCard State/Disclosure. Verify the Figma variant before implementation.`,
    imports: ['import RemDesignSystem'],
    id: 'rem-permission-card-swiftui',
    metadata: { nestable: true, props: { state, expanded } },
}
