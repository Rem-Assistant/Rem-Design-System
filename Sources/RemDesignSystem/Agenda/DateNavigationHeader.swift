import SwiftUI

/// **DateNavigationHeader** — the Agenda screen's day header: a *previous* affordance, the current
/// day (a `brandBlue` `calendar` glyph + relative `title` stacked over the full `dateText`), and a
/// *next* affordance. Each affordance is a **unit** — an outer 22-bold chevron paired with three
/// 10×4 dashes that point inward toward the label, mirroring the `unit` frames in Figma `43:2`.
///
/// The chevrons and the dashes share `labelSecondary`: the Figma binds both — and the date line — to
/// `label/secondary` (`#3C3C43` at 60%). Only the calendar glyph is `brandBlue`, and only "Today" is
/// `labelPrimary`. The `title` / `dateText` sizes (22-bold, 13-bold) and the 10×4 dash are the spec's
/// explicit values; everything else routes through `DesignTokens`.
///
/// Figma canonical: **DateNavigationHeader** (`43:2`). Compose sibling: `DateNavigationHeader` in
/// `agenda/DateNavigationHeader.kt`.
public struct DateNavigationHeader: View {
    private let title: String
    private let dateText: String
    private let onPrevious: () -> Void
    private let onNext: () -> Void
    private let onCalendarTap: (() -> Void)?

    public init(
        title: String = "Today",
        dateText: String,
        onPrevious: @escaping () -> Void,
        onNext: @escaping () -> Void,
        onCalendarTap: (() -> Void)? = nil
    ) {
        self.title = title
        self.dateText = dateText
        self.onPrevious = onPrevious
        self.onNext = onNext
        self.onCalendarTap = onCalendarTap
    }

    public var body: some View {
        HStack(spacing: 0) {
            navigationUnit(.previous)
            Spacer(minLength: DesignTokens.Spacing.sm)
            dayLabel
            Spacer(minLength: DesignTokens.Spacing.sm)
            navigationUnit(.next)
        }
        .padding(.vertical, DesignTokens.Spacing.sm)
    }

    // MARK: - Day label (center)

    private var dayLabel: some View {
        VStack(spacing: DesignTokens.Spacing.xs) {
            HStack(spacing: DesignTokens.Spacing.xs) {
                calendarIcon
                Text(title)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
            }
            Text(dateText)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
        .accessibilityElement(children: .combine)
    }

    /// The brand-blue `calendar` glyph. When `onCalendarTap` is supplied it becomes a button
    /// (e.g. to open a date picker); otherwise it is a plain decorative mark.
    @ViewBuilder
    private var calendarIcon: some View {
        let glyph = Image(systemName: "calendar")
            .font(.system(size: 22, weight: .bold))
            .foregroundStyle(DesignTokens.Color.brandBlue)
        if let onCalendarTap {
            Button(action: onCalendarTap) { glyph }
                .buttonStyle(.plain)
                .accessibilityLabel("Open calendar")
        } else {
            glyph
        }
    }

    // MARK: - Navigation units (leading / trailing)

    private enum Direction { case previous, next }

    /// A prev/next affordance: the outer chevron and the inward-pointing dashes form one tappable
    /// `unit` (Figma names the frame `unit`), giving a comfortably large target.
    private func navigationUnit(_ direction: Direction) -> some View {
        Button {
            switch direction {
            case .previous: onPrevious()
            case .next: onNext()
            }
        } label: {
            HStack(spacing: DesignTokens.Spacing.xs) {
                switch direction {
                case .previous:
                    chevron("chevron.left")
                    dashes
                case .next:
                    dashes
                    chevron("chevron.right")
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(direction == .previous ? "Previous day" : "Next day")
    }

    private func chevron(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 22, weight: .bold))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
    }

    /// Three 10×4 pill dashes in `labelSecondary` — the decorative prev/next hint beside each chevron.
    private var dashes: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            ForEach(0..<3, id: \.self) { _ in
                Capsule()
                    .fill(DesignTokens.Color.labelSecondary)
                    .frame(width: 10, height: 4)
            }
        }
        .accessibilityHidden(true)
    }
}

#Preview {
    VStack(spacing: DesignTokens.Spacing.xl) {
        DateNavigationHeader(dateText: "Aug 13 2026", onPrevious: {}, onNext: {})
        DateNavigationHeader(
            title: "Tomorrow",
            dateText: "Aug 14 2026",
            onPrevious: {},
            onNext: {},
            onCalendarTap: {}
        )
    }
    .padding(DesignTokens.Spacing.xl)
    .background(DesignTokens.Color.backgroundPrimary)
}
