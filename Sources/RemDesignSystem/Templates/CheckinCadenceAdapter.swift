import SwiftUI

/// The canonical Check-in **cadence identity** contract and the mapping boundary from the shipping
/// `CheckinsService` / `Checkin` model to the presentational ``OnboardingCheckinTemplate`` state.
///
/// The shipping app persists three time-of-day slots identified by a **canonical slot id**
/// (`morning | midday | night`). The founder reference frame labels the third slot **"Evening"**, but
/// its stored/sent identity stays `night` — the display string and the persisted id are deliberately
/// different. This adapter is the one place that reconciles them, so a caller can never accidentally
/// send `"evening"` (a display label) where `CheckinsService` expects `night` (the slot id). The same
/// mapping is mirrored in Compose (`CheckinSlot` / `checkinPeriods` in `CheckinStep.kt`), and both are
/// asserted by tests — the contract requires a *tested* boundary, not a prose "the host maps it".
public enum CheckinSlot: String, CaseIterable, Sendable {
    case morning
    case midday
    /// Displayed as "Evening" per the reference, stored/sent as the canonical `night` slot id.
    case night

    /// The user-facing row title. `night` intentionally reads "Evening" while its id stays `night`.
    public var displayTitle: String {
        switch self {
        case .morning: return "Morning"
        case .midday: return "Midday"
        case .night: return "Evening"
        }
    }

    /// The leading SF Symbol for the row (Android twin resolved via `docs/contracts/icon-registry.md`).
    public var symbol: String {
        switch self {
        case .morning: return "sunrise"
        case .midday: return "sun.max"
        case .night: return "moon.stars"
        }
    }
}

/// One slot of the shipping cadence at the design-system boundary: a faithful projection of the
/// `CheckinsService` / `Checkin` state (which slot, its brief time, whether it's on). The host builds
/// these from the real service; the adapter turns them into template rows and forwards toggles back as
/// a ``CheckinSlot`` so persistence always sees the canonical slot id.
public struct CheckinCadence: Equatable, Sendable {
    public let slot: CheckinSlot
    public let time: String?
    public let isOn: Bool

    public init(slot: CheckinSlot, time: String?, isOn: Bool) {
        self.slot = slot
        self.time = time
        self.isOn = isOn
    }
}

public extension OnboardingCheckinTemplate {
    /// Map the shipping cadence to the template's ``Period`` rows. Each row carries the **canonical
    /// slot id** (`morning | midday | night`) as its `id` and forwards toggles as a ``CheckinSlot`` —
    /// so persistence always sees `night`, never the "Evening" display label. This is the tested
    /// mapping boundary the contract requires.
    static func periods(
        from cadence: [CheckinCadence],
        onToggle: @escaping (CheckinSlot, Bool) -> Void
    ) -> [Period] {
        cadence.map { entry in
            Period(
                id: entry.slot.rawValue,
                symbol: entry.slot.symbol,
                title: entry.slot.displayTitle,
                time: entry.time,
                isOn: entry.isOn,
                onToggle: { onToggle(entry.slot, $0) }
            )
        }
    }

    /// The default cadence as loaded from `CheckinsService`: Morning on @ 8:00 AM, the others off.
    /// Shared by previews, snapshot evidence, and the host's initial state so all three exercise the
    /// same canonical identities.
    static func defaultCadence(morningOn: Bool = true, middayOn: Bool = false) -> [CheckinCadence] {
        [
            CheckinCadence(slot: .morning, time: "8:00 AM", isOn: morningOn),
            CheckinCadence(slot: .midday, time: "12:30 PM", isOn: middayOn),
            CheckinCadence(slot: .night, time: "8:00 PM", isOn: false),
        ]
    }
}
