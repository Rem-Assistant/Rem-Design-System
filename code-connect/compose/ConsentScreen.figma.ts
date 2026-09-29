// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=777-248
// source=compose/RemDesignSystem/onboarding/ConsentStep.kt
// component=consentStep
import figma from 'figma'

export default {
  example: figma.code`
    consentStep(
      onAccept = onAccept,
      onOpenTerms = onOpenTerms,
      onOpenPrivacy = onOpenPrivacy,
    )
  `,
  imports: ['import com.rem.designsystem.onboarding.consentStep'],
  id: 'onboarding-consent-screen-compose',
}
