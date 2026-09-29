import SwiftUI

/// Presentational template for the onboarding **"When should Rem check in?"** cadence step.
/// Pure: no scheduling, no persistence, no `CheckinsService` — the app wraps it, maps the shipping
/// `CheckinsService` / `Checkin` cadence to the ``Period`` list + a save ``Status``, and supplies the
/// toggle / continue / retry callbacks. The template only renders the state it is handed (the same
/// boundary that keeps sign-in "wired to real behaviour, no mock").
///
/// Reproduced from the founder reference frame `tasks/refs/onboarding/04-checkin.png` and the shipping
/// `CheckinsService` cadence model: a scaffolded onboarding step (hero → title → body → grouped list
/// of time-of-day rows) with a Body-owned **ActionArea** whose label + treatment track the save lifecycle
/// seen in the reference ("Saving…"). Composes design-system components: ``ContainedIcon`` (hero + row
/// leading), ``RemSection`` + ``ListRow`` (the cadence list), ``RemSwitch`` (per-row toggle).
///
/// The Compose sibling is `OnboardingCheckinScreen` (`compose/RemDesignSystem/onboarding/CheckinStep.kt`);
/// the two render the same states so the side-by-side evidence compares the same screen. Built to
/// `docs/contracts/onboarding-checkin.md`.
public struct OnboardingCheckinTemplate: View {
    /// One time-of-day cadence row (Morning / Midday / Evening). The formatted `time` is shown only
    /// while the row is on; `hour24` and `minute` seed the native iOS time picker. The host receives
    /// toggle and time edits through distinct persistence callbacks keyed by this period's canonical id.
    public struct Period: Identifiable {
        public let id: String
        public let symbol: String
        public let title: String
        public let time: String?
        public let hour24: Int
        public let minute: Int
        public let isOn: Bool
        public let onToggle: (Bool) -> Void
        public let onTimeChange: (Int, Int) -> Void

        public init(
            id: String,
            symbol: String,
            title: String,
            time: String?,
            hour24: Int = 8,
            minute: Int = 0,
            isOn: Bool,
            onToggle: @escaping (Bool) -> Void,
            onTimeChange: @escaping (Int, Int) -> Void = { _, _ in }
        ) {
            self.id = id
            self.symbol = symbol
            self.title = title
            self.time = time
            self.hour24 = hour24
            self.minute = minute
            self.isOn = isOn
            self.onToggle = onToggle
            self.onTimeChange = onTimeChange
        }

        /// The single toggle endpoint used by the production switch and interaction tests.
        func toggle(_ on: Bool) {
            onToggle(on)
        }

        func changeTime(hour24: Int, minute: Int) {
            onTimeChange(hour24, minute)
        }
    }

    /// The save lifecycle, driven by the host's real `CheckinsService`. Mirrors the Compose
    /// `CheckinStatus`. `default` (pristine, as-loaded) and `edited` (unsaved user change) share the
    /// "Continue" CTA; `saving` / `saved` / `failure` are the reference's persistence states.
    public enum Status: Equatable {
        /// The cadence exactly as loaded from `CheckinsService`; nothing changed yet.
        case `default`
        /// The user changed a toggle or time; the change is not yet persisted.
        case edited
        /// The change is being persisted. CTA shows the disabled "Saving…" spinner; rows lock.
        case saving
        /// The change persisted. CTA shows a brief "Saved" confirmation before the host advances.
        case saved
        /// Persistence failed and can be retried. A transient Toast sits above "Try again".
        case failure(message: String)
    }

    var status: Status
    var title: String
    var message: String
    var periods: [Period]
    var onPrimary: () -> Void
    var onRetry: () -> Void

    public init(
        status: Status,
        title: String = OnboardingCheckinTemplate.canonicalTitle,
        message: String = OnboardingCheckinTemplate.canonicalMessage,
        periods: [Period],
        onPrimary: @escaping () -> Void,
        onRetry: @escaping () -> Void = {}
    ) {
        self.status = status
        self.title = title
        self.message = message
        self.periods = periods
        self.onPrimary = onPrimary
        self.onRetry = onRetry
    }

    public var body: some View {
        // `Body` owns the screen inset and fills the available content region. Its content and
        // actions are one semantic VStack with space between; system navigation chrome stays with
        // the host outside this template.
        VStack(spacing: DesignTokens.Spacing.xl) {
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.lg) {
                    Spacer(minLength: DesignTokens.Spacing.xxl)
                    hero
                    cadenceCard
                }
                .frame(maxWidth: 560)
            }
            actionsRegion
        }
        .padding(DesignTokens.Spacing.xl)
        .background(DesignTokens.Color.backgroundPrimary.ignoresSafeArea())
    }

    private var hero: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            // Registry hero: `clock.badge.checkmark.fill` (pairs with Android `alarm_on`, FILL 1) —
            // a scheduled, confirmed check-in time. White glyph on the brand-blue squircle.
            ContainedIcon("clock.badge.checkmark.fill", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
            VStack(spacing: DesignTokens.Spacing.sm) {
                Text(title)
                    .font(DesignTokens.Typography.largeTitle.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var cadenceCard: some View {
        RemSection(rows: periods) { period in
            ListRow(
                period.title,
                emphasis: rowsInteractive ? .standard : .deemphasized,
                leading: { ContainedIcon(period.symbol, fill: .subtle) },
                trailing: { rowTrailing(period) }
            )
        }
    }

    /// Trailing accessory: the value pill (only while the row is on) sits directly left of the switch.
    @ViewBuilder private func rowTrailing(_ period: Period) -> some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            if period.isOn, let time = period.time {
                TimePickerValue(
                    text: time,
                    hour24: period.hour24,
                    minute: period.minute,
                    enabled: rowsInteractive,
                    onTimeChange: period.changeTime
                )
            }
            RemSwitch(isOn: period.isOn, enabled: rowsInteractive, onChange: period.toggle)
        }
    }

    /// Actions inside `Body` — a transient recoverable-failure toast above the primary CTA. There is
    /// no legal footnote on this step, so this uses the ActionArea composition with metadata hidden.
    private var actionsRegion: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            noticeRegion
            actionArea
        }
        .frame(maxWidth: .infinity)
        .frame(maxWidth: 560)
    }

    @ViewBuilder private var noticeRegion: some View {
        if case .failure(let message) = status {
            RemToast(variant: .error, message: message)
        }
    }

    private var actionArea: some View {
        primaryButton
    }

    /// The primary CTA — the shipping `SignInButton` treatment (`buttonBackground` fill, `medium`
    /// radius, inverted `bodyBold` label) with a leading spinner (`saving`) or checkmark (`saved`) so
    /// the paired render reads as the same button across every state on both platforms.
    private var primaryButton: some View {
        Button(action: primaryAction) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                switch status {
                case .saving:
                    ProgressView().tint(DesignTokens.Color.backgroundPrimary)
                case .saved:
                    Image(systemName: "checkmark").font(.system(size: 15, weight: .bold))
                default:
                    EmptyView()
                }
                Text(primaryLabel)
                    .font(DesignTokens.Typography.bodyBold)
            }
            .frame(maxWidth: .infinity)
        }
        .remButton(.rectBlack)
        .disabled(!primaryEnabled)
    }

    private func primaryAction() {
        switch status {
        case .failure: onRetry()
        default: onPrimary()
        }
    }

    private var primaryLabel: String {
        switch status {
        case .default, .edited: return "Continue"
        case .saving: return "Saving\u{2026}"
        case .saved: return "Saved"
        case .failure: return "Try again"
        }
    }

    /// The CTA is actionable when there is at least one selected time ("Start with one") and no save is
    /// in flight; `failure` re-enables it for the retry.
    private var primaryEnabled: Bool {
        switch status {
        case .default, .edited: return anyEnabled
        case .saving, .saved: return false
        case .failure: return true
        }
    }

    /// Rows lock while a save is in flight or has just completed, so the persisted set can't change
    /// out from under the request.
    private var rowsInteractive: Bool {
        switch status {
        case .saving, .saved: return false
        default: return true
        }
    }

    private var anyEnabled: Bool {
        periods.contains { $0.isOn }
    }

}

// MARK: - Canonical copy (shared by previews, snapshot evidence, and the Compose sibling)

public extension OnboardingCheckinTemplate {
    /// Title — verbatim from the reference frame. MUST NOT change without an authority change.
    static let canonicalTitle = "When should Rem check in?"
    /// Body — verbatim from the reference frame. MUST NOT change without an authority change.
    static let canonicalMessage =
        "At each time you pick, Rem writes you a brief on what came in. Start with one; add more anytime in Settings."
}

/// Compact time value that opens the platform's native time picker. Its closed appearance stays
/// aligned with the canonical row while the picker UI, typography, and input behavior remain iOS-owned.
private struct TimePickerValue: View {
    let text: String
    let hour24: Int
    let minute: Int
    let enabled: Bool
    let onTimeChange: (Int, Int) -> Void
    @State private var isPickerPresented = false

    var body: some View {
        Button { isPickerPresented = true } label: {
            Text(text)
                .font(DesignTokens.Typography.body)
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
                .padding(.horizontal, DesignTokens.Spacing.sm)
                .padding(.vertical, DesignTokens.Spacing.xs)
                .background(
                    RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.small, style: .continuous)
                        .fill(DesignTokens.Color.fillTertiary)
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel("Edit \(text)")
        .popover(isPresented: $isPickerPresented) {
            platformPicker
        }
    }

    @ViewBuilder private var platformPicker: some View {
        #if os(iOS)
        picker.datePickerStyle(.wheel)
        #else
        picker
        #endif
    }

    private var picker: some View {
        DatePicker(
            "Check-in time",
            selection: Binding(
                get: { pickerDate },
                set: { date in
                    let components = Calendar.current.dateComponents([.hour, .minute], from: date)
                    onTimeChange(components.hour ?? hour24, components.minute ?? minute)
                }
            ),
            displayedComponents: .hourAndMinute
        )
        .labelsHidden()
        .padding()
        .presentationCompactAdaptation(.sheet)
    }

    private var pickerDate: Date {
        Calendar.current.date(from: DateComponents(hour: hour24, minute: minute)) ?? Date()
    }
}

#if DEBUG
// Every preview explicitly supplies all three enabled flags to the one canonical three-row model;
// an off row remains present and only hides its time value (Evening keeps the canonical `night` id).
private func checkinPreviewPeriods(
    morningOn: Bool,
    middayOn: Bool,
    nightOn: Bool
) -> [OnboardingCheckinTemplate.Period] {
    OnboardingCheckinTemplate.periods(
        from: OnboardingCheckinTemplate.defaultCadence(
            morningOn: morningOn,
            middayOn: middayOn,
            nightOn: nightOn
        ),
        onToggle: { _, _ in }
    )
}

#Preview("Check-in · default") {
    OnboardingCheckinTemplate(
        status: .default,
        periods: checkinPreviewPeriods(morningOn: true, middayOn: false, nightOn: false),
        onPrimary: {}
    )
}
#Preview("Check-in · edited") {
    OnboardingCheckinTemplate(
        status: .edited,
        periods: checkinPreviewPeriods(morningOn: true, middayOn: true, nightOn: false),
        onPrimary: {}
    )
}
#Preview("Check-in · saving") {
    OnboardingCheckinTemplate(
        status: .saving,
        periods: checkinPreviewPeriods(morningOn: true, middayOn: true, nightOn: false),
        onPrimary: {}
    )
}
#Preview("Check-in · saved") {
    OnboardingCheckinTemplate(
        status: .saved,
        periods: checkinPreviewPeriods(morningOn: true, middayOn: true, nightOn: false),
        onPrimary: {}
    )
}
#Preview("Check-in · failure") {
    OnboardingCheckinTemplate(
        status: .failure(message: "We couldn't save your check-in times. Check your connection and try again."),
        periods: checkinPreviewPeriods(morningOn: true, middayOn: true, nightOn: false),
        onPrimary: {}
    )
}
#endif
