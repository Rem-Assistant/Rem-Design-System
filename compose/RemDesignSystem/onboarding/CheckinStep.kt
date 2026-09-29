package com.rem.designsystem.onboarding

import android.app.TimePickerDialog
import android.view.ContextThemeWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.rem.designsystem.R
import com.rem.designsystem.controls.RemSwitch
import com.rem.designsystem.primitives.ContainedIcon
import com.rem.designsystem.primitives.ContainedIconFill
import com.rem.designsystem.primitives.ContainedIconSize
import com.rem.designsystem.rows.RemSection
import com.rem.designsystem.rows.ListRow
import com.rem.designsystem.rows.ListRowEmphasis
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemRadius
import com.rem.designsystem.tokens.RemSpacing
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/**
 * The save lifecycle for the Check-in cadence step, driven by the host's real `CheckinsService`. The
 * Compose sibling of the SwiftUI `OnboardingCheckinTemplate.Status`. `Default` (pristine, as-loaded)
 * and `Edited` (unsaved user change) share the "Continue" CTA; `Saving` / `Saved` / `Failure` are the
 * reference frame's persistence states.
 */
sealed interface CheckinStatus {
    /** The cadence exactly as loaded from `CheckinsService`; nothing changed yet. */
    data object Default : CheckinStatus

    /** The user changed a toggle or time; the change is not yet persisted. */
    data object Edited : CheckinStatus

    /** The change is being persisted. CTA shows the disabled "Saving…" spinner; rows lock. */
    data object Saving : CheckinStatus

    /** The change persisted. CTA shows a brief "Saved" confirmation before the host advances. */
    data object Saved : CheckinStatus

    /** Persistence failed and can be retried. A transient error toast sits above "Try again". */
    data class Failure(val message: String) : CheckinStatus
}

/** The bottom-CTA label for a save state. Pure so it is tested without composing the screen. */
internal fun CheckinStatus.primaryLabel(): String = when (this) {
    CheckinStatus.Default, CheckinStatus.Edited -> "Continue"
    CheckinStatus.Saving -> "Saving…"
    CheckinStatus.Saved -> "Saved"
    is CheckinStatus.Failure -> "Try again"
}

/**
 * Whether the primary CTA is actionable. In the actionable states it needs at least one selected time
 * ("Start with one"); a save-in-flight / just-saved state locks it; a failure re-enables the retry.
 */
internal fun CheckinStatus.isPrimaryEnabled(anyEnabled: Boolean): Boolean = when (this) {
    CheckinStatus.Default, CheckinStatus.Edited -> anyEnabled
    CheckinStatus.Saving, CheckinStatus.Saved -> false
    is CheckinStatus.Failure -> true
}

/** Rows lock while a save is in flight or just completed, so the persisted set can't change under it. */
internal fun CheckinStatus.rowsInteractive(): Boolean = when (this) {
    CheckinStatus.Saving, CheckinStatus.Saved -> false
    else -> true
}

/**
 * One time-of-day cadence row. [time] is the formatted brief time, shown as a value pill only while
 * the row is on. [icon] is the leading Material glyph (the Android half of the icon registry row —
 * outline vector, matching the iOS SF Symbol per `docs/contracts/icon-registry.md`).
 */
data class CheckinPeriodUiState(
    val id: String,
    val title: String,
    val time: String?,
    val hour24: Int,
    val minute: Int,
    val enabled: Boolean,
    val icon: ImageVector,
)

/**
 * The **onboarding Check-in cadence screen** — "When should Rem check in?" (Compose sibling of the
 * SwiftUI `OnboardingCheckinTemplate`), built to `docs/contracts/onboarding-checkin.md`. Authority:
 * the founder reference frame `tasks/refs/onboarding/04-checkin.png` + the shipping `CheckinsService`
 * / `Checkin` cadence model.
 *
 * A scaffolded onboarding step (hero → title → body → grouped time-of-day list) with a Body-owned
 * ActionArea whose label + treatment track the save lifecycle. Presentational and state-driven — no
 * scheduling, no persistence. The host maps `CheckinsService` to [status] + [periods] and wires real
 * behaviour through the callbacks: [onToggle] persists a row's on/off, [onContinue] advances after a
 * successful save, [onRetry] re-attempts after a failure.
 *
 * All copy is verbatim from the reference and MUST NOT change without an authority change:
 *  - title:    "When should Rem check in?"
 *  - subtitle: "At each time you pick, Rem writes you a brief on what came in. Start with one; add
 *               more anytime in Settings."
 */
@Composable
fun OnboardingCheckinScreen(
    status: CheckinStatus,
    periods: List<CheckinPeriodUiState>,
    onToggle: (String, Boolean) -> Unit,
    onTimeChange: (String, Int, Int) -> Unit = { _, _, _ -> },
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    title: String = CHECKIN_TITLE,
    subtitle: String = CHECKIN_SUBTITLE,
    progress: OnboardingProgress? = null,
    onBack: (() -> Unit)? = null,
) {
    val anyEnabled = periods.any { it.enabled }
    val rowsInteractive = status.rowsInteractive()
    val primary = when (status) {
        CheckinStatus.Default, CheckinStatus.Edited ->
            OnboardingAction(label = status.primaryLabel(), onClick = onContinue, enabled = status.isPrimaryEnabled(anyEnabled))
        CheckinStatus.Saving ->
            OnboardingAction(label = status.primaryLabel(), onClick = {}, enabled = false, loading = true)
        CheckinStatus.Saved ->
            OnboardingAction(label = status.primaryLabel(), onClick = {}, enabled = false, leadingIcon = Icons.Filled.Check)
        is CheckinStatus.Failure ->
            OnboardingAction(label = status.primaryLabel(), onClick = onRetry, enabled = true)
    }
    val toast = (status as? CheckinStatus.Failure)?.message

    OnboardingScaffold(
        modifier = modifier,
        primary = primary,
        bottomToast = toast,
        // Registry hero: `alarm_on` (pairs with the iOS `clock.badge.checkmark.fill`, FILL 1) — a
        // scheduled, confirmed check-in time — on the brand-blue squircle.
        hero = OnboardingHero(icon = Icons.Filled.AlarmOn, contentDescription = "Check-in schedule"),
        title = title,
        subtitle = subtitle,
        background = OnboardingBackground.Primary,
        progress = progress,
        onBack = onBack,
    ) {
        // Canonical grouped Section: backgroundSecondary + xlarge radius, no outer stroke.
        RemSection(modifier = Modifier.fillMaxWidth()) {
            periods.forEachIndexed { index, period ->
                CheckinPeriodRow(
                    period = period,
                    interactive = rowsInteractive,
                    onToggle = onToggle,
                    onTimeChange = onTimeChange,
                    showSeparator = index < periods.lastIndex,
                )
            }
        }
    }
}

/**
 * A single cadence configuration of canonical [ListRow]. The enabled time is an editable value: it
 * opens Android's native [TimePickerDialog] and reports the canonical slot id with the chosen time.
 */
@Composable
private fun CheckinPeriodRow(
    period: CheckinPeriodUiState,
    interactive: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onTimeChange: (String, Int, Int) -> Unit,
    showSeparator: Boolean,
    modifier: Modifier = Modifier,
) {
    ListRow(
        title = period.title,
        enabled = interactive,
        emphasis = if (interactive) ListRowEmphasis.Standard else ListRowEmphasis.Deemphasized,
        showSeparator = showSeparator,
        modifier = modifier,
        leading = {
            ContainedIcon(
                icon = period.icon,
                fill = ContainedIconFill.Subtle,
                size = ContainedIconSize.Small,
                contentDescription = null,
            )
        },
        trailing = {
            val time = period.time
            if (period.enabled && time != null) {
                CheckinTimePickerValue(
                    text = time,
                    hour24 = period.hour24,
                    minute = period.minute,
                    enabled = interactive,
                    onTimeChange = { hour, minute -> onTimeChange(period.id, hour, minute) },
                )
                Spacer(Modifier.width(RemSpacing.sm))
            }
            RemSwitch(
                checked = period.enabled,
                onCheckedChange = { onToggle(period.id, it) },
                enabled = interactive,
            )
        },
    )
}

/**
 * The selected brief time. It preserves the compact value treatment in the row and opens the
 * platform time picker when tapped; the host owns persistence through [onTimeChange].
 */
@Composable
private fun CheckinTimePickerValue(
    text: String,
    hour24: Int,
    minute: Int,
    enabled: Boolean,
    onTimeChange: (Int, Int) -> Unit,
) {
    val colors = RemColors.current
    val context = LocalContext.current
    val dialog = remember(context, hour24, minute, onTimeChange) {
        TimePickerDialog(
            ContextThemeWrapper(context, R.style.RemTimePickerDialogTheme),
            { _, hour, selectedMinute -> onTimeChange(hour, selectedMinute) },
            hour24,
            minute,
            false,
        )
    }
    Text(
        text = text,
        style = RemTypography.body,
        color = colors.labelPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(RemRadius.small))
            .background(colors.fillTertiary)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Edit $text") { dialog.show() }
            .padding(horizontal = RemSpacing.sm, vertical = RemSpacing.xs),
        maxLines = 1,
    )
}

/**
 * The Check-in **step** for the [OnboardingSequencer] — a thin wrapper that renders
 * [OnboardingCheckinScreen] with the sequencer's back affordance + progress. It advances through
 * [onContinue] only after the host reports a successful save.
 */
fun checkinStep(
    status: CheckinStatus,
    periods: List<CheckinPeriodUiState>,
    onToggle: (String, Boolean) -> Unit,
    onTimeChange: (String, Int, Int) -> Unit = { _, _, _ -> },
    onContinue: () -> Unit,
    onRetry: () -> Unit = {},
    id: String = "checkin",
): OnboardingStep = OnboardingStep(id = id) { scope ->
    OnboardingCheckinScreen(
        status = status,
        periods = periods,
        onToggle = onToggle,
        onTimeChange = onTimeChange,
        onContinue = onContinue,
        onRetry = onRetry,
        progress = scope.progress,
        onBack = scope.onBack,
    )
}

/**
 * Canonical Check-in cadence periods (Morning on @ 8:00 AM by default), shared by previews + evidence.
 * Built through the real [checkinPeriods] adapter, so the rows carry the canonical slot ids (the
 * Evening row's id is `night`, never the display label).
 */
fun checkinDefaultPeriods(morningOn: Boolean = true, middayOn: Boolean = false): List<CheckinPeriodUiState> =
    checkinPeriods(checkinDefaultCadence(morningOn = morningOn, middayOn = middayOn))

internal const val CHECKIN_TITLE = "When should Rem check in?"
internal const val CHECKIN_SUBTITLE =
    "At each time you pick, Rem writes you a brief on what came in. Start with one; add more anytime in Settings."

@Preview(name = "Check-in · default", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun CheckinDefaultPreview() {
    RemTheme {
        OnboardingCheckinScreen(
            status = CheckinStatus.Default,
            periods = checkinDefaultPeriods(),
            onToggle = { _, _ -> },
            onContinue = {},
        )
    }
}

@Preview(name = "Check-in · saving", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun CheckinSavingPreview() {
    RemTheme {
        OnboardingCheckinScreen(
            status = CheckinStatus.Saving,
            periods = checkinDefaultPeriods(middayOn = true),
            onToggle = { _, _ -> },
            onContinue = {},
        )
    }
}

@Preview(name = "Check-in · failure", showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun CheckinFailurePreview() {
    RemTheme {
        OnboardingCheckinScreen(
            status = CheckinStatus.Failure("We couldn't save your check-in times. Check your connection and try again."),
            periods = checkinDefaultPeriods(middayOn = true),
            onToggle = { _, _ -> },
            onContinue = {},
        )
    }
}
