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

        // The toggle/update payload preserves the canonical `night` slot id (never the "Evening" label).
        evening.toggle(true)
        XCTAssertEqual(toggled.map { $0.0 }, [.night])
        XCTAssertEqual(toggled.map { $0.0.rawValue }, ["night"])
        XCTAssertEqual(toggled.map { $0.1 }, [true])
    }

    func testEditingATimeInvokesTheCanonicalSlotUpdateEndpoint() {
        var changes: [(CheckinSlot, Int, Int)] = []
        let periods = OnboardingCheckinTemplate.periods(
            from: OnboardingCheckinTemplate.defaultCadence(),
            onToggle: { _, _ in },
            onTimeChange: { slot, hour, minute in changes.append((slot, hour, minute)) }
        )

        periods[1].changeTime(hour24: 13, minute: 45)

        XCTAssertEqual(changes.map { $0.0 }, [.midday])
        XCTAssertEqual(changes.map { $0.1 }, [13])
        XCTAssertEqual(changes.map { $0.2 }, [45])
    }

    func testAdapterMapsTheRawShippingCheckinFields() {
        // The adapter accepts the authoritative `Checkin` fields (slot / enabled / deliveryHour /
        // deliveryMinute / timezone) and formats the brief-time label itself — the host supplies no
        // display string. 8:00 AM (zero minute) and a non-zero minute both round-trip correctly.
        var toggled: [(CheckinSlot, Bool)] = []
        let periods = OnboardingCheckinTemplate.periods(
            from: [
                Checkin(slot: "morning", enabled: true, deliveryHour: 8, deliveryMinute: 0, timezone: "America/New_York"),
                Checkin(slot: "midday", enabled: false, deliveryHour: 12, deliveryMinute: 30, timezone: "America/New_York"),
                Checkin(slot: "night", enabled: false, deliveryHour: 21, deliveryMinute: 5, timezone: "America/New_York"),
            ],
            onToggle: { slot, on in toggled.append((slot, on)) }
        )

        XCTAssertEqual(periods.map { $0.id }, ["morning", "midday", "night"])
        XCTAssertEqual(periods[0].time, "8:00 AM")   // zero minute, single-digit hour, AM
        XCTAssertEqual(periods[1].time, "12:30 PM")  // non-zero minute, noon → 12 PM
        XCTAssertEqual(periods[2].time, "9:05 PM")   // non-zero minute is zero-padded; 21h → 9 PM
        XCTAssertEqual(periods.map { $0.hour24 }, [8, 12, 21])
        XCTAssertEqual(periods.map { $0.minute }, [0, 30, 5])
    }

    func testAdapterRejectsAnInvalidSlotIdSuchAsEvening() {
        // "evening" is the *display* label, never a valid slot id — the adapter drops it so a bogus
        // slot can never reach persistence or render a row.
        let periods = OnboardingCheckinTemplate.periods(
            from: [
                Checkin(slot: "evening", enabled: true, deliveryHour: 20, deliveryMinute: 0, timezone: "America/New_York"),
                Checkin(slot: "night", enabled: true, deliveryHour: 20, deliveryMinute: 0, timezone: "America/New_York"),
            ],
            onToggle: { _, _ in }
        )

        // Only the valid `night` row survives; the invalid `evening` id is rejected.
        XCTAssertEqual(periods.map { $0.id }, ["night"])
        XCTAssertEqual(periods.first?.title, "Evening")
        XCTAssertNil(CheckinSlot.validating(id: "evening"))
        XCTAssertEqual(CheckinSlot.validating(id: "night"), .night)
    }

    func testTimeFormatterHandlesTheClockEdges() {
        XCTAssertEqual(CheckinSlot.formatTime(deliveryHour: 0, deliveryMinute: 0), "12:00 AM")
        XCTAssertEqual(CheckinSlot.formatTime(deliveryHour: 8, deliveryMinute: 0), "8:00 AM")
        XCTAssertEqual(CheckinSlot.formatTime(deliveryHour: 12, deliveryMinute: 30), "12:30 PM")
        XCTAssertEqual(CheckinSlot.formatTime(deliveryHour: 20, deliveryMinute: 0), "8:00 PM")
        XCTAssertEqual(CheckinSlot.formatTime(deliveryHour: 9, deliveryMinute: 5), "9:05 AM")
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
