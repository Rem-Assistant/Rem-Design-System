import SwiftUI

// MARK: - Fixture model

/// One live Agenda suggestion. `id` is stable so optimistic add/move/dismiss preserve identity.
public struct AgendaSuggestionItem: Identifiable, Equatable, Sendable {
    public let id: String
    public let action: AgendaSuggestionRow.Action
    public let title: String
    public let metadata: String
    public init(id: String, action: AgendaSuggestionRow.Action, title: String, metadata: String) {
        self.id = id; self.action = action; self.title = title; self.metadata = metadata
    }

    /// The four authored suggestions, in order. Three render inline; the fourth lives in overflow.
    public static let referenceSuggestions: [AgendaSuggestionItem] = [
        AgendaSuggestionItem(id: "prep", action: .add,
                             title: "Prep for tonight’s rehearsal",
                             metadata: "Your calendar has rehearsal at 6:00 PM."),
        AgendaSuggestionItem(id: "move", action: .move,
                             title: "Move rehearsal check-in to 3:00 PM",
                             metadata: "Your calendar has a conflict at 8:00 AM."),
        AgendaSuggestionItem(id: "review", action: .add,
                             title: "Review venue notes",
                             metadata: "Have the details ready for tonight."),
        AgendaSuggestionItem(id: "setlist", action: .add,
                             title: "Bring the updated set list",
                             metadata: "Keep the latest songs ready for rehearsal."),
    ]
}

/// A single Agenda row (task or event). `sortMinutes` orders the day; `pending` draws the dashed ring
/// used for a just-accepted suggestion.
public struct AgendaRowItem: Identifiable, Equatable, Sendable {
    public enum Kind: Equatable, Sendable { case task; case event }
    public let id: String
    public var kind: Kind
    public var title: String
    public var timeLabel: String
    public var sortMinutes: Int
    public var pills: [String]
    public var pending: Bool
    public init(id: String, kind: Kind, title: String, timeLabel: String, sortMinutes: Int,
                pills: [String] = [], pending: Bool = false) {
        self.id = id; self.kind = kind; self.title = title; self.timeLabel = timeLabel
        self.sortMinutes = sortMinutes; self.pills = pills; self.pending = pending
    }
}

/// How a suggestion resolves when accepted. These are the **authored example fixtures** — a 5:00 PM
/// created task and a 3:00 PM rehearsal move — not app-wide defaults.
enum AgendaSuggestionResolution {
    /// Insert a new (pending) task into the day.
    case add(AgendaRowItem)
    /// Reschedule an existing row by id to a new time; if that row is absent (e.g. an empty day),
    /// insert `fallback` instead so the action still has a deterministic outcome.
    case move(targetId: String, timeLabel: String, sortMinutes: Int, fallback: AgendaRowItem)
}

/// The authored fixtures the playground presents.
public enum AgendaSuggestionsFixture: String, CaseIterable, Hashable, Sendable {
    case loaded        // a populated day + four suggestions (three inline, one in overflow)
    case empty         // an empty day + four suggestions (proves empty → populated)
    case none          // a populated day with no suggestions (the Suggestions slot is absent)
    case restoration   // loaded, but the first removal is optimistically restored (a simulated sync failure)

    public var title: String {
        switch self {
        case .loaded: return "Loaded"
        case .empty: return "Empty day"
        case .none: return "No suggestions"
        case .restoration: return "Restoration"
        }
    }
}

// MARK: - State

/// Owns the deterministic local Agenda Suggestions fixture and its optimistic model. No network, no
/// fetch, no invented spinner / success badge / error card / Retry.
final class AgendaSuggestionsModel: ObservableObject {
    @Published private(set) var rows: [AgendaRowItem]
    @Published private(set) var suggestions: [AgendaSuggestionItem]
    @Published var overflowOpen: Bool = false

    private var resolutions: [String: AgendaSuggestionResolution]
    private var restorationArmed: Bool

    init(fixture: AgendaSuggestionsFixture) {
        let seed = AgendaSuggestionsModel.seed(for: fixture)
        self.rows = seed.rows
        self.suggestions = seed.suggestions
        self.resolutions = seed.resolutions
        self.restorationArmed = (fixture == .restoration)
    }

    /// At most three inline; all live rows appear in overflow.
    var inlineSuggestions: ArraySlice<AgendaSuggestionItem> { suggestions.prefix(3) }
    var hasOverflow: Bool { suggestions.count > 3 }

    func accept(_ suggestion: AgendaSuggestionItem) {
        guard let index = suggestions.firstIndex(where: { $0.id == suggestion.id }) else { return }
        if let resolution = resolutions[suggestion.id] { apply(resolution) }
        removeSuggestion(at: index, original: suggestion)
    }

    func dismiss(_ suggestion: AgendaSuggestionItem) {
        guard let index = suggestions.firstIndex(where: { $0.id == suggestion.id }) else { return }
        removeSuggestion(at: index, original: suggestion)
    }

    private func removeSuggestion(at index: Int, original: AgendaSuggestionItem) {
        suggestions.remove(at: index)
        // The deterministic restoration fixture puts the first removed row back, modelling a sync that
        // failed after the optimistic update. No network is involved.
        if restorationArmed {
            restorationArmed = false
            let insertAt = min(index, suggestions.count)
            Task { @MainActor [weak self] in
                try? await Task.sleep(nanoseconds: 600_000_000)
                guard let self else { return }
                guard !self.suggestions.contains(where: { $0.id == original.id }) else { return }
                self.suggestions.insert(original, at: min(insertAt, self.suggestions.count))
            }
        }
        // Last removal hides the whole Suggestions slot (and its 24pt gap) and closes overflow.
        if suggestions.isEmpty { overflowOpen = false }
    }

    private func apply(_ resolution: AgendaSuggestionResolution) {
        switch resolution {
        case .add(let item):
            insert(item)
        case .move(let targetId, let timeLabel, let sortMinutes, let fallback):
            if let i = rows.firstIndex(where: { $0.id == targetId }) {
                rows[i].timeLabel = timeLabel
                rows[i].sortMinutes = sortMinutes
                rows.sort { $0.sortMinutes < $1.sortMinutes }
            } else {
                insert(fallback)
            }
        }
    }

    private func insert(_ item: AgendaRowItem) {
        guard !rows.contains(where: { $0.id == item.id }) else { return }
        rows.append(item)
        rows.sort { $0.sortMinutes < $1.sortMinutes }
    }

    // MARK: Seed data

    private struct Seed {
        var rows: [AgendaRowItem]
        var suggestions: [AgendaSuggestionItem]
        var resolutions: [String: AgendaSuggestionResolution]
    }

    private static func seed(for fixture: AgendaSuggestionsFixture) -> Seed {
        // The populated day shared by `loaded`, `none` and `restoration`.
        let loadedRows: [AgendaRowItem] = [
            AgendaRowItem(id: "reply", kind: .task, title: "Reply to the venue",
                          timeLabel: "8:00 AM", sortMinutes: 8 * 60, pills: ["Follow-ups"]),
            AgendaRowItem(id: "confirm", kind: .task, title: "Confirm rehearsal time",
                          timeLabel: "8:00 AM", sortMinutes: 8 * 60, pills: ["Follow-ups"]),
            AgendaRowItem(id: "coffee", kind: .event, title: "Coffee chat with a mentor",
                          timeLabel: "7:00 PM", sortMinutes: 19 * 60, pills: ["Personal"]),
        ]

        // The four authored suggestions, in order. Three render inline; the fourth lives in overflow.
        let suggestions = AgendaSuggestionItem.referenceSuggestions

        // Authored outcomes. `prep` creates the 5:00 PM task; `move` reschedules the existing rehearsal
        // confirmation to 3:00 PM. The other two Add outcomes carry their own fixture-local evening task.
        let resolutions: [String: AgendaSuggestionResolution] = [
            "prep": .add(AgendaRowItem(id: "prep-task", kind: .task,
                                       title: "Prep for tonight’s rehearsal",
                                       timeLabel: "5:00 PM", sortMinutes: 17 * 60, pending: true)),
            "move": .move(targetId: "confirm", timeLabel: "3:00 PM", sortMinutes: 15 * 60,
                          fallback: AgendaRowItem(id: "move-task", kind: .task,
                                                  title: "Rehearsal check-in",
                                                  timeLabel: "3:00 PM", sortMinutes: 15 * 60, pending: true)),
            "review": .add(AgendaRowItem(id: "review-task", kind: .task,
                                         title: "Review venue notes",
                                         timeLabel: "6:30 PM", sortMinutes: 18 * 60 + 30, pending: true)),
            "setlist": .add(AgendaRowItem(id: "setlist-task", kind: .task,
                                          title: "Bring the updated set list",
                                          timeLabel: "6:00 PM", sortMinutes: 18 * 60, pending: true)),
        ]

        switch fixture {
        case .loaded, .restoration:
            return Seed(rows: loadedRows, suggestions: suggestions, resolutions: resolutions)
        case .none:
            return Seed(rows: loadedRows, suggestions: [], resolutions: resolutions)
        case .empty:
            return Seed(rows: [], suggestions: suggestions, resolutions: resolutions)
        }
    }
}

// MARK: - Suggestions section (inline)

/// The inline Suggestions slot: the "Suggestions" header (canonical `SectionHeader 161:68` — 17pt
/// semibold, labelSecondary), up to three rows, then a "See more" plain action when there is overflow.
/// The region owns a 24pt top gap so removing it (last suggestion gone) removes the gap too.
struct AgendaSuggestionsSection: View {
    let suggestions: ArraySlice<AgendaSuggestionItem>
    let showSeeMore: Bool
    let onAccept: (AgendaSuggestionItem) -> Void
    let onDismiss: (AgendaSuggestionItem) -> Void
    let onSeeMore: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
            Text("Suggestions")
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.bottom, 6)
                .accessibilityAddTraits(.isHeader)
            ForEach(Array(suggestions)) { suggestion in
                AgendaSuggestionRow(
                    action: suggestion.action,
                    title: suggestion.title,
                    metadata: suggestion.metadata,
                    acceptIdentifier: "agendaSuggestion.inline.accept.\(suggestion.id)",
                    dismissIdentifier: "agendaSuggestion.inline.dismiss.\(suggestion.id)",
                    onAccept: { onAccept(suggestion) },
                    onDismiss: { onDismiss(suggestion) }
                )
            }
            if showSeeMore {
                Button(action: onSeeMore) {
                    Text("See more")
                        .font(DesignTokens.Typography.body.weight(.semibold))
                        .foregroundStyle(DesignTokens.Color.brandBlue)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 6)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("agendaSuggestions.seeMore")
            }
        }
        .padding(.top, DesignTokens.Spacing.xl)
    }
}

// MARK: - Overflow sheet

/// The overflow presentation: a native sheet with the authored **Suggestions / Done** header, a divider,
/// and a scrollable list of **all** live suggestions. No fixed detents, drag-indicator override or extra
/// navigation hierarchy are introduced.
public struct AgendaSuggestionsOverflowSheet: View {
    private let suggestions: [AgendaSuggestionItem]
    private let onAccept: (AgendaSuggestionItem) -> Void
    private let onDismiss: (AgendaSuggestionItem) -> Void
    private let onDone: () -> Void

    public init(
        suggestions: [AgendaSuggestionItem],
        onAccept: @escaping (AgendaSuggestionItem) -> Void = { _ in },
        onDismiss: @escaping (AgendaSuggestionItem) -> Void = { _ in },
        onDone: @escaping () -> Void = {}
    ) {
        self.suggestions = suggestions
        self.onAccept = onAccept
        self.onDismiss = onDismiss
        self.onDone = onDone
    }

    public var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Suggestions")
                    .font(DesignTokens.Typography.title3Bold)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Spacer()
                Button(action: onDone) {
                    Text("Done")
                        .font(DesignTokens.Typography.body.weight(.semibold))
                        .foregroundStyle(DesignTokens.Color.brandBlue)
                }
                .accessibilityIdentifier("agendaSuggestions.done")
            }
            .padding(DesignTokens.Spacing.lg)
            Divider()
            ScrollView {
                VStack(spacing: DesignTokens.Spacing.sm) {
                    ForEach(suggestions) { suggestion in
                        AgendaSuggestionRow(
                            action: suggestion.action,
                            title: suggestion.title,
                            metadata: suggestion.metadata,
                            acceptIdentifier: "agendaSuggestion.sheet.accept.\(suggestion.id)",
                            dismissIdentifier: "agendaSuggestion.sheet.dismiss.\(suggestion.id)",
                            onAccept: { onAccept(suggestion) },
                            onDismiss: { onDismiss(suggestion) }
                        )
                    }
                }
                .padding(DesignTokens.Spacing.lg)
            }
        }
        .background(DesignTokens.Color.backgroundPrimary)
        .accessibilityIdentifier("agendaSuggestions.overflow")
    }
}

// MARK: - Playground host view

/// **AgendaSuggestionsPlaygroundView** — the bounded Agenda New *Suggestions* journey. Composes the
/// reused `DateNavigationHeader`, the day's `TaskEventRow`s (or the empty `RemContentUnavailableView`),
/// the Add New / Schedule bar, and the Suggestions slot which follows the bar with exactly 24pt spacing
/// while present. Shell affordances outside this slice (date paging, sort, Add New, Schedule) call host
/// callbacks; this view does not invent their destinations.
public struct AgendaSuggestionsPlaygroundView: View {
    @StateObject private var model: AgendaSuggestionsModel
    private let onAddNew: () -> Void
    private let onSchedule: () -> Void
    private let onSort: () -> Void

    public init(
        fixture: AgendaSuggestionsFixture = .loaded,
        onAddNew: @escaping () -> Void = {},
        onSchedule: @escaping () -> Void = {},
        onSort: @escaping () -> Void = {}
    ) {
        _model = StateObject(wrappedValue: AgendaSuggestionsModel(fixture: fixture))
        self.onAddNew = onAddNew
        self.onSchedule = onSchedule
        self.onSort = onSort
    }

    public var body: some View {
        VStack(spacing: 0) {
            DateNavigationHeader(dateText: "Aug 13 2026", onPrevious: {}, onNext: {})
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.sm)
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if model.rows.isEmpty {
                        RemContentUnavailableView(
                            symbol: "calendar.badge.plus",
                            title: "No agenda yet",
                            message: "Create a new task or schedule existing ones"
                        )
                        .frame(maxWidth: .infinity, minHeight: 360)
                    } else {
                        sortTrigger
                        ForEach(model.rows) { row in
                            TaskEventRow(
                                kind: row.kind == .task ? .task : .event(DesignTokens.Color.systemBlue),
                                title: row.title,
                                leading: .time(row.timeLabel),
                                pills: row.pills,
                                pending: row.pending
                            )
                            .accessibilityIdentifier("agenda.row.\(row.id)")
                        }
                    }
                    addScheduleBar
                    if !model.suggestions.isEmpty {
                        AgendaSuggestionsSection(
                            suggestions: model.inlineSuggestions,
                            showSeeMore: model.hasOverflow,
                            onAccept: { model.accept($0) },
                            onDismiss: { model.dismiss($0) },
                            onSeeMore: { model.overflowOpen = true }
                        )
                    }
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.top, DesignTokens.Spacing.sm)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignTokens.Color.backgroundPrimary)
        .sheet(isPresented: $model.overflowOpen) {
            AgendaSuggestionsOverflowSheet(
                suggestions: model.suggestions,
                onAccept: { model.accept($0) },
                onDismiss: { model.dismiss($0) },
                onDone: { model.overflowOpen = false }
            )
        }
    }

    private var sortTrigger: some View {
        Button(action: onSort) {
            HStack(spacing: 6) {
                Image(systemName: "arrow.up.arrow.down")
                Text("Sort by: Time")
                Image(systemName: "chevron.down").font(.system(size: 12, weight: .semibold))
            }
            .font(DesignTokens.Typography.body.weight(.semibold))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.vertical, DesignTokens.Spacing.sm)
        .accessibilityIdentifier("agenda.sortTrigger")
    }

    private var addScheduleBar: some View {
        HStack(spacing: DesignTokens.Spacing.md) {
            Button(action: onAddNew) {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                    Text("Add New")
                }
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("agenda.addNew")
            Rectangle()
                .fill(DesignTokens.Color.separator)
                .frame(width: 1, height: 20)
            Button(action: onSchedule) {
                HStack(spacing: 6) {
                    Image(systemName: "calendar.badge.clock")
                    Text("Schedule")
                    Text("3")
                        .font(DesignTokens.Typography.caption1Bold)
                        .foregroundStyle(DesignTokens.Color.backgroundPrimary)
                        .padding(.horizontal, 6).padding(.vertical, 2)
                        .background(Capsule().fill(DesignTokens.Color.labelTertiary))
                }
                .font(DesignTokens.Typography.body.weight(.semibold))
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("agenda.schedule")
        }
        .padding(.top, DesignTokens.Spacing.sm)
    }
}

#if DEBUG
#Preview("AgendaSuggestions — loaded") {
    AgendaSuggestionsPlaygroundView(fixture: .loaded)
}
#Preview("AgendaSuggestions — empty") {
    AgendaSuggestionsPlaygroundView(fixture: .empty)
}
#endif
