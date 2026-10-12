import SwiftUI

/// **TaskEventRow** — the Agenda / Inbox row for a single task or event. A *specialization* of the base
/// 3-slot row (`ListRow`): **Leading** (time / schedule / clock) · **Content** (task status ring **or**
/// event bar, the title, and a Pills row) · **trailing** (the list-level chevron, which is **not baked** —
/// it comes from the host's `NavigationLink`, exactly like `ListRow`).
///
/// The Pills row reuses the canonical `RemPill` primitive — never a bespoke badge. The pill *kind* is
/// derived from the row's `Kind` so the design rule can't be violated per call site: **tasks → `.list`**
/// (a `list.bullet` badge), **events → `.dot(color)`** (the calendar color as an 8pt dot). Hide the row
/// entirely (`showPills: false`) for unfiled rows such as Inbox.
///
/// Figma canonical: **TaskEventRow** component set (`46:21`), variants `Kind = task | event` ×
/// `Leading = Time | None`. Per the component's authoritative description, `Leading` also carries the
/// `schedule` (calendar.badge.plus — Inbox add) and `clock` glyph modes; `None` maps to `.none` here.
/// Verified against the set: the event accent bar (`46:19`) is `labelSecondary`; the task status frame
/// interior (`46:9`) is `backgroundPrimary`. Compose sibling: `TaskEventRow` in `rows/TaskEventRow.kt`.
public struct TaskEventRow: View {
    /// Task vs event. The event case carries the **calendar color** that tints its Pills-row dots; the
    /// vertical accent bar itself is always the neutral `labelSecondary` (per Figma `46:19`).
    public enum Kind: Equatable {
        case task
        case event(Color)
    }

    /// The leading accessory. `time` is a small time label (e.g. `"8:00 AM"`); `schedule` is the
    /// `calendar.badge.plus` glyph (Inbox add); `clock` is the `clock` glyph; `none` reserves no leading
    /// column at all (the Figma `Leading=None` variant — content shifts fully leading).
    public enum Leading: Equatable {
        case time(String)
        case schedule
        case clock
        case none
    }

    /// How the leading column is drawn. `.compact` is the design-system default and is unchanged.
    /// `.app` reproduces the shipped Rem app's time column (RemClaw `Rem/Sources/Components/
    /// TaskEventRowView.swift`) for products that want parity with it:
    /// - **Width** 64pt: `TimeIndicatorWidthCalculator` (L6–14), applied at L67–70.
    /// - **Alignment**: the column is leading-aligned (L69) and the row centres vertically (L66, `alignment:
    ///   .center`); the column-to-content gap is `Spacing.sm` (L66). The `schedule` / `clock` glyphs centre
    ///   in the column (L110–118).
    /// - **Format and type**: a `.time` label is drawn as the app's stacked clock — the two-digit 12-hour
    ///   hour in `.title2`, then minutes over AM/PM in `.caption`, 2pt apart (`timeDisplay`, L125–137;
    ///   `formatHour` / `formatMinutes` / `formatAMPM`, L358–372) — in the primary label colour (the app
    ///   sets no foreground, L128–134). The label is parsed by `TaskEventClockFace`; a label it cannot read
    ///   (e.g. "Oct 14") falls back to the compact footnote.
    /// - **Glyphs**: `clock` at `.title3` (L110–113); `calendar.badge.plus` at 22pt semibold (L115–118).
    public enum LeadingStyle: Equatable, Sendable {
        case compact
        case app
    }

    private let kind: Kind
    private let title: String
    private let leading: Leading
    private let pills: [String]
    private let showPills: Bool
    private let pending: Bool
    private let completed: Bool
    private let leadingStyle: LeadingStyle

    /// - Parameters:
    ///   - completed: draws the task status as completed (a filled check circle) instead of the ring —
    ///     the shipped app's `checkmark.circle.fill` (RemClaw `TaskStatusIndicator.swift` L24, L64–65).
    ///     Takes precedence over `pending`; ignored for events. Default `false` (unchanged).
    ///   - leadingStyle: `.compact` (default, unchanged) or `.app` for parity with the shipped app's
    ///     time column; see `LeadingStyle`.
    public init(
        kind: Kind,
        title: String,
        leading: Leading = .none,
        pills: [String] = [],
        showPills: Bool = true,
        pending: Bool = false,
        completed: Bool = false,
        leadingStyle: LeadingStyle = .compact
    ) {
        self.kind = kind
        self.title = title
        self.leading = leading
        self.pills = pills
        self.showPills = showPills
        self.pending = pending
        self.completed = completed
        self.leadingStyle = leadingStyle
    }

    // Compact leading column width so titles align across timed rows (the DS default).
    private let leadingColumnWidth: CGFloat = 52
    private let statusRingSize: CGFloat = 24
    private let eventBarWidth: CGFloat = 4

    public var body: some View {
        HStack(
            alignment: leadingStyle == .app ? .center : .top,
            spacing: leadingStyle == .app ? DesignTokens.Spacing.sm : DesignTokens.Spacing.md
        ) {
            if case .none = leading {
                EmptyView()
            } else {
                leadingView
                    .frame(
                        width: leadingStyle == .app ? TaskEventClockFace.appColumnWidth : leadingColumnWidth,
                        alignment: .leading
                    )
            }
            content
            Spacer(minLength: 0)
        }
        .padding(.horizontal, DesignTokens.Spacing.md)
        .padding(.vertical, DesignTokens.Spacing.sm)
        .contentShape(Rectangle())
    }

    @ViewBuilder
    private var leadingView: some View {
        switch leading {
        case .time(let label):
            if leadingStyle == .app, let face = TaskEventClockFace(label: label) {
                appClock(face)
            } else {
                Text(label)
                    .font(DesignTokens.Typography.footnote)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
            }
        case .schedule:
            if leadingStyle == .app {
                // RemClaw TaskEventRowView.swift L115–118: 22pt semibold, centred in the column.
                appGlyph("calendar.badge.plus", font: .system(size: 22, weight: .semibold))
            } else {
                leadingGlyph("calendar.badge.plus")
            }
        case .clock:
            if leadingStyle == .app {
                // RemClaw TaskEventRowView.swift L110–113: `.title3`, centred in the column.
                appGlyph("clock", font: DesignTokens.Typography.title3)
            } else {
                leadingGlyph("clock")
            }
        case .none:
            EmptyView()
        }
    }

    private func leadingGlyph(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 18, weight: .regular))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
    }

    private func appGlyph(_ systemName: String, font: Font) -> some View {
        Image(systemName: systemName)
            .font(font)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .frame(maxWidth: .infinity, alignment: .center)
    }

    /// The shipped app's stacked clock (RemClaw `TaskEventRowView.swift` `timeDisplay`, L125–137): the
    /// hour in `.title2`, minutes over AM/PM in `.caption`, 2pt apart, primary label colour.
    private func appClock(_ face: TaskEventClockFace) -> some View {
        HStack(spacing: 2) {
            Text(face.hour)
                .font(DesignTokens.Typography.title2Bold.weight(.regular))
            VStack(spacing: 0) {
                Text(face.minutes)
                Text(face.meridiem)
            }
            .font(DesignTokens.Typography.caption1)
        }
        .foregroundStyle(DesignTokens.Color.labelPrimary)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(face.spoken)
    }

    @ViewBuilder
    private var content: some View {
        switch kind {
        case .task:
            // Status ring beside the title; Pills row below, aligned to the ring's leading edge.
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                HStack(alignment: .center, spacing: DesignTokens.Spacing.sm) {
                    statusRing
                    titleText
                }
                pillsRow
            }
        case .event:
            // Neutral accent bar spanning title + pills; calendar color rides the Pills-row dots.
            HStack(alignment: .center, spacing: DesignTokens.Spacing.md) {
                RoundedRectangle(cornerRadius: eventBarWidth / 2, style: .continuous)
                    .fill(DesignTokens.Color.labelSecondary)
                    .frame(width: eventBarWidth)
                    .frame(maxHeight: .infinity)
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    titleText
                    pillsRow
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var titleText: some View {
        Text(title)
            .font(DesignTokens.Typography.body.weight(.semibold))
            .foregroundStyle(DesignTokens.Color.labelPrimary)
            .fixedSize(horizontal: false, vertical: true)
    }

    // The status ring. A **pending** row (a just-accepted suggestion that has not yet "committed")
    // draws a dashed ring — matching the authored Agenda Suggestions `Add` outcome. A **completed** task
    // draws the shipped app's filled check circle instead (RemClaw `TaskStatusIndicator.swift` L64–65:
    // `checkmark.circle.fill`, primary label colour per L73–75).
    @ViewBuilder
    private var statusRing: some View {
        if completed {
            Image(systemName: "checkmark.circle.fill")
                .resizable()
                .scaledToFit()
                .foregroundStyle(DesignTokens.Color.labelPrimary)
                .frame(width: statusRingSize, height: statusRingSize)
                .accessibilityLabel("Completed")
        } else {
            ring
        }
    }

    private var ring: some View {
        Circle()
            .strokeBorder(
                DesignTokens.Color.labelSecondary,
                style: pending
                    ? StrokeStyle(lineWidth: 1.5, dash: [3, 2])
                    : StrokeStyle(lineWidth: 1.5)
            )
            .frame(width: statusRingSize, height: statusRingSize)
    }

    @ViewBuilder
    private var pillsRow: some View {
        if showPills && !pills.isEmpty {
            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(pills, id: \.self) { pill in
                    RemPill(pill, kind: pillKind)
                }
            }
        }
    }

    private var pillKind: RemPill.Kind {
        switch kind {
        case .task: return .list
        case .event(let color): return .dot(color)
        }
    }
}

/// The shipped Rem app's stacked time face for a `TaskEventRow` `.time` label, used by
/// `TaskEventRow.LeadingStyle.app`. Mirrors RemClaw `Rem/Sources/Components/TaskEventRowView.swift`:
/// `formatHour` (L358–362, two-digit 12-hour, 0 → 12), `formatMinutes` (L364–367) and `formatAMPM`
/// (L369–372). Reads `H:mm` / `HH:mm` (24-hour, e.g. "14:00") and `h:mm AM` / `h:mm PM` (any case, optional
/// space, e.g. "8:00 AM"); anything else (e.g. "Oct 14") is `nil` and the row keeps the compact label.
/// Compose twin: `TaskEventClockFace` in `rows/TaskEventRow.kt`; both are unit-tested with the same cases.
public struct TaskEventClockFace: Equatable, Sendable {
    /// RemClaw `TaskEventRowView.swift` `TimeIndicatorWidthCalculator` (L6–14): the fixed 64pt column.
    public static let appColumnWidth: CGFloat = 64

    public let hour: String
    public let minutes: String
    public let meridiem: String

    public init?(label: String) {
        let trimmed = label.trimmingCharacters(in: .whitespaces)
        let parts = trimmed.split(separator: ":", maxSplits: 1, omittingEmptySubsequences: false)
        guard parts.count == 2, (1...2).contains(parts[0].count), parts[0].allSatisfy({ $0.isASCII && $0.isNumber }),
              let rawHour = Int(parts[0]) else { return nil }
        let rest = parts[1]
        let minuteText = rest.prefix(2)
        guard minuteText.count == 2, minuteText.allSatisfy({ $0.isASCII && $0.isNumber }),
              let minute = Int(minuteText), minute < 60 else { return nil }
        let suffix = rest.dropFirst(2).trimmingCharacters(in: .whitespaces).uppercased()
        let hour24: Int
        switch suffix {
        case "":
            guard rawHour < 24 else { return nil }
            hour24 = rawHour
        case "AM", "PM":
            guard (1...12).contains(rawHour) else { return nil }
            hour24 = (rawHour % 12) + (suffix == "PM" ? 12 : 0)
        default:
            return nil
        }
        self.init(hour24: hour24, minute: minute)
    }

    /// From a 24-hour clock time (`hour24` 0–23, `minute` 0–59).
    public init(hour24: Int, minute: Int) {
        let display = hour24 == 0 ? 12 : (hour24 > 12 ? hour24 - 12 : hour24)
        hour = String(format: "%02d", display)
        minutes = String(format: "%02d", minute)
        meridiem = hour24 < 12 ? "AM" : "PM"
    }

    /// VoiceOver reads the time once, naturally ("9:00 AM"), not as three fragments.
    public var spoken: String { "\(Int(hour) ?? 0):\(minutes) \(meridiem)" }
}

#if DEBUG
#Preview("TaskEventRow") {
    VStack(spacing: 0) {
        // Task with a list pill + a leading time.
        TaskEventRow(
            kind: .task,
            title: "Reply to the venue",
            leading: .time("8:00 AM"),
            pills: ["Follow-ups"]
        )
        Divider().padding(.leading, DesignTokens.Spacing.md)
        // Event with a calendar-color dot pill + a leading time.
        TaskEventRow(
            kind: .event(DesignTokens.Color.systemBlue),
            title: "Coffee chat with a mentor",
            leading: .time("7:00 PM"),
            pills: ["Personal"]
        )
        Divider().padding(.leading, DesignTokens.Spacing.md)
        // Inbox row: schedule leading, Pills row hidden (unfiled).
        TaskEventRow(
            kind: .task,
            title: "Draft the offsite agenda",
            leading: .schedule,
            pills: ["Work"],
            showPills: false
        )
        Divider().padding(.leading, DesignTokens.Spacing.md)
        // App parity: the shipped app's 64pt stacked time column and a completed task.
        TaskEventRow(
            kind: .task,
            title: "Send the venue deposit",
            leading: .time("9:00"),
            pills: ["Follow-ups"],
            completed: true,
            leadingStyle: .app
        )
    }
    .background(DesignTokens.Color.backgroundSecondary)
    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
    .padding()
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
