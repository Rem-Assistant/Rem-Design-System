import SwiftUI

/// Structured brief counts for the prose-less fallback. Mirrors the Compose `BriefCounts` (renamed here so it cannot collide with the app's `BriefCounts`).
public struct DailyBriefCounts: Equatable, Sendable {
    public var blocked: Int
    public var overdue: Int
    public var scheduledToday: Int
    public var done: Int
    public var total: Int

    public init(blocked: Int = 0, overdue: Int = 0, scheduledToday: Int = 0, done: Int = 0, total: Int = 0) {
        self.blocked = blocked
        self.overdue = overdue
        self.scheduledToday = scheduledToday
        self.done = done
        self.total = total
    }
}

/// **DailyBriefCard** — the brief entry point at the top of *today's* Agenda: a title + chevron, the
/// brief prose, and — only when a structured brief has no prose — a fallback row of count capsules
/// (blocked / overdue / done / today). An optional "Read latest brief" action sits **beside** the
/// navigation button, never nested inside it, so playback is independently tappable.
///
/// Design-system sibling of the Compose `DailyBriefCard` (`agentsurfaces/DailyBriefCard.kt`), with the
/// same inputs. Authority: the shipping app's `Rem/Sources/Components/DailyBriefCard.swift`, which binds
/// the app's `DailyBrief` model; this component takes plain values so it carries no app model.
/// Deliberately uncontained — no boxed card background, exactly as the shipping card. No Figma master
/// exists yet (`tasks/DailyBriefCard.md`).
public struct DailyBriefCard: View {
    private let title: String
    private let summary: String?
    private let counts: DailyBriefCounts
    private let onTap: (() -> Void)?
    private let onRead: (() -> Void)?
    private let isReading: Bool

    public init(
        title: String,
        summary: String? = nil,
        counts: DailyBriefCounts = DailyBriefCounts(),
        onTap: (() -> Void)? = nil,
        onRead: (() -> Void)? = nil,
        isReading: Bool = false
    ) {
        self.title = title
        self.summary = summary
        self.counts = counts
        self.onTap = onTap
        self.onRead = onRead
        self.isReading = isReading
    }

    /// Counts are a prose-less fallback: only when there is no summary and something is non-zero.
    private var showsCounts: Bool {
        summary == nil && (counts.total > 0 || counts.blocked > 0 || counts.overdue > 0 || counts.scheduledToday > 0)
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Button { onTap?() } label: {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                    HStack(spacing: DesignTokens.Spacing.sm) {
                        Text(title)
                            .font(DesignTokens.Typography.title3Bold)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                        Spacer(minLength: 0)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(DesignTokens.Color.labelTertiary)
                            .accessibilityHidden(true)
                    }
                    if let summary {
                        Text(summary)
                            .font(DesignTokens.Typography.subheadline)
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    if showsCounts {
                        BriefCapsuleFlow(spacing: 5, lineSpacing: 6) {
                            if counts.blocked > 0 {
                                BriefCapsule(symbol: "exclamationmark.triangle.fill", text: "\(counts.blocked) blocked", tint: DesignTokens.Color.systemRed)
                            }
                            if counts.overdue > 0 {
                                BriefCapsule(symbol: "clock.badge.exclamationmark", text: "\(counts.overdue) overdue", tint: DesignTokens.Color.systemOrange)
                            }
                            if counts.total > 0 {
                                BriefCapsule(symbol: nil, text: "\(counts.done) of \(counts.total) done", tint: DesignTokens.Color.systemGreen)
                            }
                            if counts.scheduledToday > 0 {
                                BriefCapsule(symbol: "calendar", text: "\(counts.scheduledToday) today", tint: DesignTokens.Color.brandBlue)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(onTap == nil)
            .accessibilityElement(children: .combine)

            // Read action — a sibling of the navigation button, never nested inside it.
            if let onRead {
                Button(action: onRead) {
                    Label(isReading ? "Stop reading" : "Read latest brief",
                          systemImage: isReading ? "stop.fill" : "speaker.wave.2")
                        .font(DesignTokens.Typography.subheadline.weight(.bold))
                        .foregroundStyle(DesignTokens.Color.brandBlue)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, DesignTokens.Spacing.xs)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// A calm count chip — the brief's blocked / overdue / done / today glyphs.
private struct BriefCapsule: View {
    let symbol: String?
    let text: String
    let tint: Color

    var body: some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            if let symbol {
                Image(systemName: symbol).font(.system(size: 11, weight: .semibold)).foregroundStyle(tint)
            } else {
                Circle().fill(tint).frame(width: 8, height: 8)
            }
            Text(text)
                .font(DesignTokens.Typography.caption1)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
        .padding(.horizontal, DesignTokens.Spacing.sm)
        .padding(.vertical, 6)
        .background(DesignTokens.Color.backgroundSecondary, in: Capsule())
    }
}

/// Wraps capsules onto new lines when the row is full (the Compose card's `FlowRow`).
private struct BriefCapsuleFlow: Layout {
    var spacing: CGFloat
    var lineSpacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(subviews, width: proposal.width ?? .infinity)
        let width = rows.map(\.width).max() ?? 0
        let height = rows.map(\.height).reduce(0, +) + lineSpacing * CGFloat(max(rows.count - 1, 0))
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in arrange(subviews, width: bounds.width) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(at: CGPoint(x: x, y: y + (row.height - size.height) / 2), proposal: .unspecified)
                x += size.width + spacing
            }
            y += row.height + lineSpacing
        }
    }

    private func arrange(_ subviews: Subviews, width: CGFloat) -> [(indices: [Int], width: CGFloat, height: CGFloat)] {
        var rows: [(indices: [Int], width: CGFloat, height: CGFloat)] = []
        var current: (indices: [Int], width: CGFloat, height: CGFloat) = ([], 0, 0)
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            let needed = current.indices.isEmpty ? size.width : current.width + spacing + size.width
            if needed > width, !current.indices.isEmpty {
                rows.append(current)
                current = ([index], size.width, size.height)
            } else {
                current = (current.indices + [index], needed, max(current.height, size.height))
            }
        }
        if !current.indices.isEmpty { rows.append(current) }
        return rows
    }
}

#if DEBUG
#Preview("DailyBriefCard — prose") {
    DailyBriefCard(
        title: "Your morning brief",
        summary: "Three things need you today: the investor update, a venue reply, and rehearsal timing.",
        onTap: {},
        onRead: {}
    )
    .padding()
}

#Preview("DailyBriefCard — counts") {
    DailyBriefCard(
        title: "Good morning",
        counts: DailyBriefCounts(blocked: 2, overdue: 3, scheduledToday: 5, done: 2, total: 7),
        onTap: {}
    )
    .padding()
}
#endif
