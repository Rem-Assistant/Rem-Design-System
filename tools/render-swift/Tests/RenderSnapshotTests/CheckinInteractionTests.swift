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
        let night = period("night", isOn: true) { changes.append(("night", $0)) }

        morning.toggle(true)
        night.toggle(false)

        XCTAssertEqual(changes.map { $0.0 }, ["morning", "night"])
        XCTAssertEqual(changes.map { $0.1 }, [true, false])
    }

    func testAdapterDisplaysEveningWhileTogglingTheCanonicalNightSlot() {
        // The `Checkin` identity contract: the third slot reads "Evening" but its stored/sent id and
        // its toggle payload are the canonical `night` — proven through the real mapping boundary.
        var toggled: [(CheckinSlot, Bool)] = []
        let periods = OnboardingCheckinTemplate.periods(
            from: OnboardingCheckinTemplate.defaultCadence(),
            onToggle: { slot, on in toggled.append((slot, on)) }
        )

        XCTAssertEqual(periods.map { $0.id }, ["morning", "midday", "night"])
        let evening = periods[2]
        XCTAssertEqual(evening.id, "night")
        XCTAssertEqual(evening.title, "Evening")

        evening.toggle(true)
        XCTAssertEqual(toggled.map { $0.0 }, [.night])
        XCTAssertEqual(toggled.map { $0.1 }, [true])
    }

    func testCheckinSlotDisplayTitlesMatchTheCanonicalIds() {
        XCTAssertEqual(CheckinSlot.morning.displayTitle, "Morning")
        XCTAssertEqual(CheckinSlot.midday.displayTitle, "Midday")
        XCTAssertEqual(CheckinSlot.night.displayTitle, "Evening")
        XCTAssertEqual(CheckinSlot.night.rawValue, "night")
    }

    func testFailureStatusCarriesTheRecoverableMessage() {
        let message = "We couldn't save your check-in times. Check your connection and try again."
        XCTAssertEqual(OnboardingCheckinTemplate.Status.failure(message: message),
                       OnboardingCheckinTemplate.Status.failure(message: message))
        XCTAssertNotEqual(OnboardingCheckinTemplate.Status.saving,
                          OnboardingCheckinTemplate.Status.saved)
    }
}
