import XCTest
@testable import RemDesignSystem

final class CheckinInteractionTests: XCTestCase {
    private func period(_ id: String, isOn: Bool, onToggle: @escaping (Bool) -> Void) -> OnboardingCheckinTemplate.Period {
        OnboardingCheckinTemplate.Period(
            id: id, symbol: "sunrise", title: id.capitalized, time: "8:00 AM", isOn: isOn, onToggle: onToggle
        )
    }

    func testTogglingARowInvokesItsPersistenceEndpoint() {
        var changes: [(String, Bool)] = []
        let morning = period("morning", isOn: false) { changes.append(("morning", $0)) }
        let evening = period("evening", isOn: true) { changes.append(("evening", $0)) }

        morning.toggle(true)
        evening.toggle(false)

        XCTAssertEqual(changes.map { $0.0 }, ["morning", "evening"])
        XCTAssertEqual(changes.map { $0.1 }, [true, false])
    }

    func testFailureStatusCarriesTheRecoverableMessage() {
        let message = "We couldn't save your check-in times. Check your connection and try again."
        XCTAssertEqual(OnboardingCheckinTemplate.Status.failure(message: message),
                       OnboardingCheckinTemplate.Status.failure(message: message))
        XCTAssertNotEqual(OnboardingCheckinTemplate.Status.saving,
                          OnboardingCheckinTemplate.Status.saved)
    }
}
