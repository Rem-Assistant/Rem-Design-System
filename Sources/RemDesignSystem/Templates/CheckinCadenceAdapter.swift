import SwiftUI

/// The canonical Check-in **cadence identity** contract and the mapping boundary from the shipping
/// `CheckinsService` / `Checkin` model to the presentational ``OnboardingCheckinTemplate`` state.
///
/// The shipping app persists three time-of-day slots identified by a **canonical slot id**
/// (`morning | midday | night`). The founder reference frame labels the third slot **"Evening"**, but
/// its stored/sent identity stays `night` — the display string and the persisted id are deliberately
/// different. This adapter is the one place that reconciles them, so a caller can never accidentally
/// send `"evening"` (a display label) where `CheckinsService` expects `night` (the slot id). The same
/// mapping is mirrored in Compose (`CheckinSlot` / `checkinPeriods` in `CheckinCadenceAdapter.kt`), and
/// both are asserted by tests — the contract requires a *tested* boundary, not a prose "the host maps it".
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

    /// Resolve a raw shipping slot id (`morning | midday | night`) to a canonical ``CheckinSlot``.
    /// Returns `nil` for anything else — including the `"evening"` *display* label, which is never a
    /// valid slot id — so an invalid slot is dropped at the boundary instead of rendering a bogus row.
    public static func validating(id: String) -> CheckinSlot? {
        CheckinSlot(rawValue: id)
    }

    /// Format a shipping delivery time (24-hour `deliveryHour` / `deliveryMinute`) into the reference's
    /// brief-time label — "8:00 AM", "12:30 PM", "8:00 PM". Deterministic 12-hour clock (not a
    /// locale-dependent `DateFormatter`) so the value matches the reference exactly regardless of the
    /// runner locale. The formatting lives *inside* the adapter: the host never supplies a display string.
    public static func formatTime(deliveryHour: Int, deliveryMinute: Int) -> String {
        let hour24 = ((deliveryHour % 24) + 24) % 24
        let minute = ((deliveryMinute % 60) + 60) % 60
        let period = hour24 < 12 ? "AM" : "PM"
        var hour12 = hour24 % 12
        if hour12 == 0 { hour12 = 12 }
        return String(format: "%d:%02d %@", hour12, minute, period)
    }
}

/// The **raw shipping `Checkin` fields**, exactly as `CheckinsService` persists them, at the
/// design-system boundary. This is the adapter's *input*: the host hands the authoritative fields
/// straight through — it does **not** pre-format a time string or invent a display label. The adapter
/// validates the `slot` id, formats the hour/minute internally, and produces the template rows, so the
/// design system (not the host) owns the display mapping.
public struct Checkin: Equatable, Sendable {
    /// The canonical slot id as stored by `CheckinsService`: `morning | midday | night`. Any other
    /// value (including the `"evening"` display label) is rejected by the adapter.
    public let slot: String
    /// Whether this slot's brief is currently on.
    public let enabled: Bool
    /// The delivery time, hour in 24-hour form (`0...23`).
    public let deliveryHour: Int
    /// The delivery time, minute (`0...59`).
    public let deliveryMinute: Int
    /// The IANA timezone the delivery time is expressed in (carried for identity/persistence; the
    /// brief-time label is formatted from `deliveryHour`/`deliveryMinute` directly).
    public let timezone: String

    public init(slot: String, enabled: Bool, deliveryHour: Int, deliveryMinute: Int, timezone: String) {
        self.slot = slot
        self.enabled = enabled
        self.deliveryHour = deliveryHour
        self.deliveryMinute = deliveryMinute
        self.timezone = timezone
    }
}

public extension OnboardingCheckinTemplate {
    /// Map the **raw shipping `Checkin` fields** to the template's ``Period`` rows. The adapter:
    ///  - **validates** each `slot` against `morning | midday | night`, dropping anything else
    ///    (an unknown slot, or the `"evening"` display label used as an id);
    ///  - **formats** the `deliveryHour` / `deliveryMinute` into the brief-time label internally
    ///    (`8:00 AM`, `12:30 PM`) — the host never supplies a display string;
    ///  - carries the **canonical slot id** (`morning | midday | night`) as each row's `id` and
    ///    forwards toggles as a ``CheckinSlot`` — so persistence always sees `night`, never the
    ///    "Evening" display label.
    ///
    /// This is the tested mapping boundary the contract requires (`CheckinInteractionTests`).
    static func periods(
        from checkins: [Checkin],
        onToggle: @escaping (CheckinSlot, Bool) -> Void,
        onTimeChange: @escaping (CheckinSlot, Int, Int) -> Void = { _, _, _ in }
    ) -> [Period] {
        checkins.compactMap { checkin in
            guard let slot = CheckinSlot.validating(id: checkin.slot) else { return nil }
            return Period(
                id: slot.rawValue,
                symbol: slot.symbol,
                title: slot.displayTitle,
                time: CheckinSlot.formatTime(
                    deliveryHour: checkin.deliveryHour,
                    deliveryMinute: checkin.deliveryMinute
                ),
                hour24: checkin.deliveryHour,
                minute: checkin.deliveryMinute,
                isOn: checkin.enabled,
                onToggle: { onToggle(slot, $0) },
                onTimeChange: { hour, minute in onTimeChange(slot, hour, minute) }
            )
        }
    }

    /// The canonical three-row cadence as loaded from `CheckinsService`: Morning on @ 8:00 AM by
    /// default, the others off. Enabled flags change row state, never row membership. Returns the
    /// **raw `Checkin` fields** (not a pre-formatted value), so previews, snapshot evidence, and the
    /// host's initial state all exercise the same real adapter path — canonical slot ids in, formatted
    /// brief-time labels out.
    static func defaultCadence(
        morningOn: Bool = true,
        middayOn: Bool = false,
        nightOn: Bool = false
    ) -> [Checkin] {
        [
            Checkin(slot: "morning", enabled: morningOn, deliveryHour: 8, deliveryMinute: 0, timezone: "America/New_York"),
            Checkin(slot: "midday", enabled: middayOn, deliveryHour: 12, deliveryMinute: 30, timezone: "America/New_York"),
            Checkin(slot: "night", enabled: nightOn, deliveryHour: 20, deliveryMinute: 0, timezone: "America/New_York"),
        ]
    }
}
