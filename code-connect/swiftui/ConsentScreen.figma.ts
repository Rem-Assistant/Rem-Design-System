// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=777-248
// source=Sources/RemDesignSystem/Templates/OnboardingConsentTemplate.swift
// component=OnboardingConsentTemplate
import figma from 'figma'

export default {
  example: figma.code`
    OnboardingConsentTemplate(
      message: "Rem uses your data to answer you and act on the things you ask. You can review or delete it anytime in Settings.",
      legalItems: legalItems,
      footnote: "By tapping “Accept and Continue,” you agree to our Terms of Service and Privacy Policy.",
      onPrimary: onAccept
    )
  `,
  imports: ['import RemDesignSystem'],
  id: 'onboarding-consent-screen-swiftui',
}
