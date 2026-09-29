// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=876-1121
// source=compose/RemDesignSystem/onboarding/CheckinStep.kt
// component=OnboardingCheckinScreen
import figma from 'figma'

export default {
  example: figma.code`
    OnboardingCheckinScreen(
      status = CheckinStatus.Default,
      periods = checkinDefaultPeriods(),
      onToggle = onToggle,
      onContinue = onContinue,
      onRetry = onRetry,
    )
  `,
  imports: [
    'import com.rem.designsystem.onboarding.CheckinStatus',
    'import com.rem.designsystem.onboarding.OnboardingCheckinScreen',
    'import com.rem.designsystem.onboarding.checkinDefaultPeriods',
  ],
  id: 'onboarding-checkin-screen-compose',
}
