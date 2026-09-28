// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=777-325
// source=compose/RemDesignSystem/onboarding/LegalDocument.kt
// component=LegalDocumentScreen
import figma from 'figma'

export default {
  example: figma.code`
    LegalDocumentScreen(
      title = "Terms of Service",
      sections = sections,
      onClose = onClose,
    )
  `,
  imports: ['import com.rem.designsystem.onboarding.LegalDocumentScreen'],
  id: 'onboarding-consent-terms-screen-compose',
}
