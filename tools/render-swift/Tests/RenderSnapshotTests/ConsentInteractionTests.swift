import XCTest
@testable import RemDesignSystem

final class ConsentInteractionTests: XCTestCase {
    func testLegalRowsInvokeTheirOpenCallbacks() {
        var opened: [String] = []
        let terms = OnboardingConsentTemplate.LegalItem(
            symbol: "doc.text",
            title: "Terms of Service",
            subtitle: "Terms summary",
            action: { opened.append("terms") }
        )
        let privacy = OnboardingConsentTemplate.LegalItem(
            symbol: "shield",
            title: "Privacy Policy",
            subtitle: "Privacy summary",
            action: { opened.append("privacy") }
        )

        terms.open()
        privacy.open()

        XCTAssertEqual(opened, ["terms", "privacy"])
    }

    func testLegalDocumentDoneInvokesDismissCallback() {
        var dismissCount = 0
        let sheet = LegalDocumentTemplate(
            title: "Terms of Service",
            sections: [],
            onClose: { dismissCount += 1 }
        )

        sheet.dismiss()

        XCTAssertEqual(dismissCount, 1)
    }
}
