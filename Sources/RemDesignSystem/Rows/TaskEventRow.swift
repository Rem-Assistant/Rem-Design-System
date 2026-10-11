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

    private let kind: Kind
    private let title: String
    private let leading: Leading
    private let pills: [String]
    private let showPills: Bool
    private let pending: Bool

    public init(
        kind: Kind,
        title: String,
        leading: Leading = .none,
        pills: [String] = [],
        showPills: Bool = true,
        pending: Bool = false
    ) {
        self.kind = kind
        self.title = title
        self.leading = leading
        self.pills = pills
        self.showPills = showPills
        self.pending = pending
    }

    // Leading column width so titles align across timed rows; matches Figma's ~64pt leading region.
    private let leadingColumnWidth: CGFloat = 52
    private let statusRingSize: CGFloat = 24
    private let eventBarWidth: CGFloat = 4

    public var body: some View {
        HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
            if case .none = leading {
                EmptyView()
            } else {
                leadingView
                    .frame(width: leadingColumnWidth, alignment: .leading)
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
            Text(label)
                .font(DesignTokens.Typography.footnote)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
        case .schedule:
            leadingGlyph("calendar.badge.plus")
        case .clock:
            leadingGlyph("clock")
        case .none:
            EmptyView()
        }
    }

    private func leadingGlyph(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 18, weight: .regular))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
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
    // draws a dashed ring — matching the authored Agenda Suggestions `Add` outcome.
    private var statusRing: some View {
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
    }
    .background(DesignTokens.Color.backgroundSecondary)
    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
    .padding()
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
