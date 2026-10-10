// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=2577-17461
// source=compose/RemDesignSystem/chat/PermissionCard.kt
// component=PermissionCard
import figma from 'figma'
// `State` × `Disclosure` are evidenced by the variant names (e.g. State=Awaiting, Disclosure=Expanded
// 2002:70251). The request payload is host data: text property names could not be verified (the Code
// Connect context API needs a Dev seat). Disclosure seeds the host-owned expanded state.
// `risk` / `parameters` (elevated-risk label, Full parameters disclosure) are code-led with no Figma
// property yet; they default to Standard / empty and are left to the host.
const instance = figma.selectedInstance
const state = instance.getEnum('State', { Awaiting: 'Awaiting', Allowed: 'Allowed', Denied: 'Denied' })
const expanded = instance.getEnum('Disclosure', { Expanded: 'true', Collapsed: 'false' })
export default {
    example: state && expanded
        ? figma.code`// Host state: var expanded by rememberSaveable { mutableStateOf(${expanded}) }
PermissionCard(PermissionCardModel(title, question, summary, details, PermissionCardState.${state}, alwaysAllowScope), expanded = expanded, onExpandedChange = { expanded = it }, onAllow = onAllow, onDeny = onDeny, onAlwaysAllow = onAlwaysAllow, onReviewAgain = onReviewAgain)`
        : figma.code`// Unsupported PermissionCard State/Disclosure. Verify the Figma variant before implementation.`,
    imports: [
        'import com.rem.designsystem.chat.PermissionCard',
        'import com.rem.designsystem.chat.PermissionCardModel',
        'import com.rem.designsystem.chat.PermissionCardState',
    ],
    id: 'rem-permission-card-compose',
    metadata: { nestable: true, props: { state, expanded } },
}
