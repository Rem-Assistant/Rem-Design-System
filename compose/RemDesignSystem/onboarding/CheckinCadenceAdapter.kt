package com.rem.designsystem.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Canonical Check-in cadence identity + the mapping boundary from the shipping `CheckinsService` /
 * `Checkin` model to the presentational [OnboardingCheckinScreen] state. Sibling of the SwiftUI
 * `CheckinSlot` / `OnboardingCheckinTemplate.periods(from:)`.
 *
 * The shipping app persists three time-of-day slots identified by a **canonical slot id**
 * (`morning | midday | night`). The founder reference frame labels the third slot **"Evening"**, but
 * its stored/sent identity stays `night`. This adapter is the single place that reconciles the display
 * label vs the slot id, so a toggle always persists `night`, never the "evening" label. Asserted by
 * `CheckinCadenceTest` — the contract requires a *tested* boundary, not a prose "the host maps it".
 */
enum class CheckinSlot(val id: String, val displayTitle: String, val icon: ImageVector) {
    Morning("morning", "Morning", Icons.Outlined.WbTwilight),
    Midday("midday", "Midday", Icons.Outlined.WbSunny),

    /** Displayed as "Evening" per the reference, stored/sent as the canonical `night` slot id. */
    Night("night", "Evening", Icons.Outlined.Bedtime);

    companion object {
        /**
         * Resolve a raw shipping slot id (`morning | midday | night`) back to its slot. Returns `null`
         * for anything else — including the `"evening"` *display* label, which is never a valid slot id
         * — so an invalid slot is dropped at the boundary instead of rendering a bogus row. This is the
         * host callback boundary too: a toggle reports [CheckinSlot.id], and `fromId` round-trips it.
         */
        fun fromId(id: String): CheckinSlot? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The **raw shipping `Checkin` fields**, exactly as `CheckinsService` persists them, at the
 * design-system boundary. This is the adapter's *input*: the host hands the authoritative fields
 * straight through — it does **not** pre-format a time string or invent a display label.
 */
data class Checkin(
    /** The canonical slot id: `morning | midday | night`. Anything else (incl. "evening") is rejected. */
    val slot: String,
    val enabled: Boolean,
    /** Delivery time, hour in 24-hour form (`0..23`). */
    val deliveryHour: Int,
    /** Delivery time, minute (`0..59`). */
    val deliveryMinute: Int,
    /** IANA timezone the delivery time is expressed in (carried for identity; not used for the label). */
    val timezone: String,
)

/**
 * Format a shipping delivery time (24-hour [hour] / [minute]) into the reference's brief-time label —
 * "8:00 AM", "12:30 PM", "8:00 PM". Deterministic 12-hour clock (not a locale-dependent formatter) so
 * the value matches the reference exactly regardless of the runner locale. The formatting lives
 * *inside* the adapter: the host never supplies a display string.
 */
internal fun formatCheckinTime(hour: Int, minute: Int): String {
    val hour24 = ((hour % 24) + 24) % 24
    val safeMinute = ((minute % 60) + 60) % 60
    val period = if (hour24 < 12) "AM" else "PM"
    var hour12 = hour24 % 12
    if (hour12 == 0) hour12 = 12
    return "%d:%02d %s".format(hour12, safeMinute, period)
}

/**
 * Map the **raw shipping `Checkin` fields** to [CheckinPeriodUiState] rows. The adapter validates each
 * `slot` against `morning | midday | night` (dropping anything else — an unknown slot, or the
 * `"evening"` display label used as an id), formats the `deliveryHour` / `deliveryMinute` into the
 * brief-time label internally, and carries the canonical slot id as [CheckinPeriodUiState.id] — so the
 * screen's `onToggle(id, on)` always reports `night`, never the "Evening" display label. The tested
 * mapping boundary the contract requires (`CheckinCadenceTest`).
 */
fun checkinPeriods(checkins: List<Checkin>): List<CheckinPeriodUiState> =
    checkins.mapNotNull { checkin ->
        val slot = CheckinSlot.fromId(checkin.slot) ?: return@mapNotNull null
        CheckinPeriodUiState(
            id = slot.id,
            title = slot.displayTitle,
            time = formatCheckinTime(checkin.deliveryHour, checkin.deliveryMinute),
            hour24 = checkin.deliveryHour,
            minute = checkin.deliveryMinute,
            enabled = checkin.enabled,
            icon = slot.icon,
        )
    }

/**
 * Canonical three-row cadence loaded from `CheckinsService`: Morning on @ 8:00 AM by default, the
 * others off. Enabled flags change row state, never row membership. Returns the **raw `Checkin`
 * fields** (not a pre-formatted value), so previews, snapshot evidence, and the host's initial state
 * all exercise the same real adapter path — canonical slot ids in, formatted labels out.
 */
fun checkinDefaultCadence(
    morningOn: Boolean = true,
    middayOn: Boolean = false,
    nightOn: Boolean = false,
): List<Checkin> = listOf(
    Checkin("morning", enabled = morningOn, deliveryHour = 8, deliveryMinute = 0, timezone = "America/New_York"),
    Checkin("midday", enabled = middayOn, deliveryHour = 12, deliveryMinute = 30, timezone = "America/New_York"),
    Checkin("night", enabled = nightOn, deliveryHour = 20, deliveryMinute = 0, timezone = "America/New_York"),
)
