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
        /** Resolve a canonical slot id back to its slot (host callback boundary). */
        fun fromId(id: String): CheckinSlot? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One slot of the shipping cadence at the design-system boundary — a faithful projection of `Checkin`
 * (which slot, its brief time, whether it's on). The host builds these from the real service.
 */
data class CheckinCadence(val slot: CheckinSlot, val time: String?, val enabled: Boolean)

/**
 * Map the shipping cadence to [CheckinPeriodUiState] rows. Each row's [CheckinPeriodUiState.id] is the
 * canonical slot id (`morning | midday | night`), so the screen's `onToggle(id, on)` always reports
 * `night`, never the "Evening" display label. The tested mapping boundary the contract requires.
 */
fun checkinPeriods(cadence: List<CheckinCadence>): List<CheckinPeriodUiState> =
    cadence.map { entry ->
        CheckinPeriodUiState(
            id = entry.slot.id,
            title = entry.slot.displayTitle,
            time = entry.time,
            enabled = entry.enabled,
            icon = entry.slot.icon,
        )
    }

/** Default cadence loaded from `CheckinsService`: Morning on @ 8:00 AM, the others off. */
fun checkinDefaultCadence(morningOn: Boolean = true, middayOn: Boolean = false): List<CheckinCadence> = listOf(
    CheckinCadence(CheckinSlot.Morning, time = "8:00 AM", enabled = morningOn),
    CheckinCadence(CheckinSlot.Midday, time = "12:30 PM", enabled = middayOn),
    CheckinCadence(CheckinSlot.Night, time = "8:00 PM", enabled = false),
)
