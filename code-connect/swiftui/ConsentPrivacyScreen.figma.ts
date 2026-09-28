// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=777-392
// source=Sources/RemDesignSystem/Templates/LegalDocumentTemplate.swift
// component=LegalDocumentTemplate
import figma from 'figma'

export default {
  example: figma.code`
    LegalDocumentTemplate(
      title: "Privacy Policy",
      sections: sections,
      onClose: onClose
    )
  `,
  imports: ['import RemDesignSystem'],
  id: 'onboarding-consent-privacy-screen-swiftui',
}
