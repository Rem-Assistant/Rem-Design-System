// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=428-37
// source=Sources/RemDesignSystem/Chat/LoginCard.swift
// component=LoginCard
import figma from 'figma'
// `State` (Entry 428:20 / Saved 428:29) is evidenced by the variant names; text property names could
// not be verified (the Code Connect context API needs a Dev seat), so title and site stay host data.
// The native Add login form (1956:8162) is LoginForm, presented by the host from onAddLogin.
const instance = figma.selectedInstance
const state = instance.getEnum('State', { Entry: '.entry', Saved: '.saved' })
export default {
    example: state
        ? figma.code`LoginCard(LoginCardModel(title: title, site: site, state: ${state}), onAddLogin: onAddLogin, onOpenSaved: onOpenSaved)`
        : figma.code`// Unsupported LoginCard State. Verify the Figma variant before implementation.`,
    imports: ['import RemDesignSystem'],
    id: 'rem-login-card-swiftui',
    metadata: { nestable: true, props: { state } },
}
