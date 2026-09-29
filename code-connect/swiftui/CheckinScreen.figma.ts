// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=876-1121
// source=Sources/RemDesignSystem/Templates/OnboardingCheckinTemplate.swift
// component=OnboardingCheckinTemplate
import figma from 'figma'

export default {
  example: figma.code`
    OnboardingCheckinTemplate(
      status: .default,
      periods: OnboardingCheckinTemplate.periods(
        from: OnboardingCheckinTemplate.defaultCadence(),
        onToggle: onToggle
      ),
      onPrimary: onContinue,
      onRetry: onRetry
    )
  `,
  imports: ['import RemDesignSystem'],
  id: 'onboarding-checkin-screen-swiftui',
}
