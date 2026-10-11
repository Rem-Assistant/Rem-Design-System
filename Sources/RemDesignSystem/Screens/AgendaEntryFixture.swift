import Foundation

// MARK: - Agenda entry routing · fixture model
//
// Presentation-only model behind the Playground Agenda's two entries in `AgendaAddSchedule` (`2189:11919`):
// **Add New** → task/event creation (Creation section `2390:28498`, reached like the create menu's
// "New Task or Event" item `2049:10336`) and **Schedule** → existing-task selection (Schedule Tasks
// `2295:13691`, section `2295:13690`). The fixture host applies results in memory; nothing is
// persisted, synced or sent. Compose twin: `screens/AgendaEntryFixture.kt`.

/// A calendar day, independent of time zone. Day arithmetic is plain civil-calendar math so both
/// platforms produce identical labels (Compose cannot use `java.time` at minSdk 24).
public struct AgendaDay: Hashable, Comparable, Sendable {
    public let year: Int
    public let month: Int
    public let day: Int

    public init(year: Int, month: Int, day: Int) {
        self.year = year; self.month = month; self.day = day
    }

    /// The authored fixture date (Schedule Tasks: "Sample fixture: August 13, 2026"); the Playground
    /// Agenda views this day and treats it as today.
    public static let fixtureToday = AgendaDay(year: 2026, month: 8, day: 13)

    /// Days since 1970-01-01 (proleptic Gregorian).
    public var epochDay: Int {
        let y = month <= 2 ? year - 1 : year
        let era = (y >= 0 ? y : y - 399) / 400
        let yoe = y - era * 400
        let mp = (month + 9) % 12
        let doy = (153 * mp + 2) / 5 + day - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    public init(epochDay: Int) {
        let z = epochDay + 719_468
        let era = (z >= 0 ? z : z - 146_096) / 146_097
        let doe = z - era * 146_097
        let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        self.init(year: yoe + era * 400 + (m <= 2 ? 1 : 0), month: m, day: d)
    }

    public func adding(days: Int) -> AgendaDay { AgendaDay(epochDay: epochDay + days) }

    /// 0 = Sunday … 6 = Saturday.
    public var weekday: Int { ((epochDay % 7) + 7 + 4) % 7 }

    public static func < (lhs: AgendaDay, rhs: AgendaDay) -> Bool { lhs.epochDay < rhs.epochDay }

    static let monthNames = ["January", "February", "March", "April", "May", "June", "July",
                             "August", "September", "October", "November", "December"]
    static let weekdayNames = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]

    /// "Aug 13 2026" — the `DateNavigationHeader` date line.
    public var headerText: String { "\(Self.monthNames[month - 1].prefix(3)) \(day) \(year)" }

    /// The Pick a Time **Date** row value: "Today, August 13", "Tomorrow, August 14", or
    /// "Saturday, August 15".
    public func dateRowText(today: AgendaDay) -> String {
        let prefix: String
        switch epochDay - today.epochDay {
        case 0: prefix = "Today"
        case 1: prefix = "Tomorrow"
        default: prefix = Self.weekdayNames[weekday]
        }
        return "\(prefix), \(Self.monthNames[month - 1]) \(day)"
    }

    /// The Schedule Tasks primary action for the viewed day: "Add to Today", "Add to Tomorrow" or
    /// "Add to Aug 15".
    public func addToLabel(today: AgendaDay) -> String {
        switch epochDay - today.epochDay {
        case 0: return "Add to Today"
        case 1: return "Add to Tomorrow"
        default: return "Add to \(Self.monthNames[month - 1].prefix(3)) \(day)"
        }
    }
}

/// A time of day on the fixture's 12-hour clock.
public struct AgendaTime: Hashable, Sendable {
    public let hour: Int
    public let minute: Int
    public init(hour: Int, minute: Int) { self.hour = hour; self.minute = minute }

    /// Schedule Tasks default time ("Default time 09:00").
    public static let scheduleDefault = AgendaTime(hour: 9, minute: 0)

    public var sortMinutes: Int { hour * 60 + minute }

    /// "9:00 AM" — the `TaskEventRow` time label used by the Agenda fixtures.
    public var label: String {
        let h12 = hour % 12 == 0 ? 12 : hour % 12
        let mm = minute < 10 ? "0\(minute)" : "\(minute)"
        return "\(h12):\(mm) \(hour < 12 ? "AM" : "PM")"
    }
}

// MARK: Scheduling

/// Schedule Tasks filters (`ScheduleFilters` `2301:11644`). Changing a filter never clears selection.
public enum AgendaScheduleFilter: String, CaseIterable, Hashable, Sendable {
    case all, inbox, overdue

    public var title: String {
        switch self {
        case .all: return "All"
        case .inbox: return "Inbox"
        case .overdue: return "Overdue"
        }
    }

    /// Empty-filter copy (State=Empty filter: "No tasks in Inbox").
    public var emptyText: String {
        switch self {
        case .all: return "No tasks to schedule"
        case .inbox: return "No tasks in Inbox"
        case .overdue: return "No overdue tasks"
        }
    }
}

/// An existing, unscheduled backlog item. Only tasks are schedule candidates; events are excluded.
public struct AgendaBacklogItem: Identifiable, Equatable, Sendable {
    public enum Bucket: Equatable, Sendable { case inbox, overdue, undated }
    public let id: String
    public let kind: AgendaRowItem.Kind
    public let title: String
    /// The row's metadata line ("Inbox", "Overdue · 2d", "No date").
    public let detail: String
    public let bucket: Bucket
    /// Pills the task keeps once it lands on the Agenda.
    public let pills: [String]

    public init(id: String, kind: AgendaRowItem.Kind = .task, title: String, detail: String,
                bucket: Bucket, pills: [String] = []) {
        self.id = id; self.kind = kind; self.title = title; self.detail = detail
        self.bucket = bucket; self.pills = pills
    }

    public func matches(_ filter: AgendaScheduleFilter) -> Bool {
        switch filter {
        case .all: return true
        case .inbox: return bucket == .inbox
        case .overdue: return bucket == .overdue
        }
    }
}

/// What Schedule Tasks hands back on Done: the selected task IDs (scheduled together) and one slot.
public struct AgendaScheduleRequest: Equatable, Sendable {
    public let taskIDs: [String]
    public let day: AgendaDay
    public let time: AgendaTime
    public init(taskIDs: [String], day: AgendaDay, time: AgendaTime) {
        self.taskIDs = taskIDs; self.day = day; self.time = time
    }
}

// MARK: Creation

/// The creation draft (`2390:28498`): New Task / New Event, title, chooser value and notes. Date, time,
/// duration, alert and repeat are the authored inline summaries — their inspector is separate work.
public struct AgendaCreationDraft: Equatable, Sendable {
    public enum Mode: String, CaseIterable, Hashable, Sendable {
        case task, event
        public var title: String { self == .task ? "New Task" : "New Event" }
    }

    /// The designed task-list chooser (`CreationChooserMenu/Task list`).
    public enum TaskList: String, CaseIterable, Hashable, Sendable {
        case noList, followUps, work
        public var title: String {
            switch self {
            case .noList: return "No List"
            case .followUps: return "Follow-ups"
            case .work: return "Work"
            }
        }
    }

    /// The calendar chooser (`CreationChooserMenu/Calendar`).
    public enum EventCalendar: String, CaseIterable, Hashable, Sendable {
        case personal, work
        public var title: String { self == .personal ? "Personal" : "Work" }
    }

    public var mode: Mode
    public var title: String
    public var notes: String
    public var taskList: TaskList
    public var calendar: EventCalendar

    public init(mode: Mode = .task, title: String = "", notes: String = "",
                taskList: TaskList = .noList, calendar: EventCalendar = .personal) {
        self.mode = mode; self.title = title; self.notes = notes
        self.taskList = taskList; self.calendar = calendar
    }

    /// Save is enabled only for a non-blank title.
    public var canSave: Bool { !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }

    /// Authored slots: a task lands at 5:00 PM (30 min); an event spans 6:00–7:00 PM (1 hr).
    public var start: AgendaTime { mode == .task ? AgendaTime(hour: 17, minute: 0) : AgendaTime(hour: 18, minute: 0) }

    /// "Today, 5:00 PM" / "Today, 6:00–7:00 PM".
    public var whenSummary: String { mode == .task ? "Today, 5:00 PM" : "Today, 6:00–7:00 PM" }
    public var durationSummary: String { mode == .task ? "30 min" : "1 hr" }

    /// The chooser's current label ("Select task list" until a list is picked).
    public var chooserLabel: String {
        switch mode {
        case .task: return taskList == .noList ? "Select task list" : taskList.title
        case .event: return calendar.title
        }
    }
}

// MARK: Fixture

/// The in-memory fixture that applies creation and scheduling results to the viewed day.
public struct AgendaEntryFixture: Equatable, Sendable {
    public let today: AgendaDay
    public let viewedDay: AgendaDay
    public private(set) var backlog: [AgendaBacklogItem]
    private var createdCount = 0

    public init(today: AgendaDay = .fixtureToday, viewedDay: AgendaDay = .fixtureToday,
                backlog: [AgendaBacklogItem] = AgendaEntryFixture.referenceBacklog) {
        self.today = today; self.viewedDay = viewedDay; self.backlog = backlog
    }

    /// The authored Schedule Tasks list, plus one undated event that must never appear there.
    public static let referenceBacklog: [AgendaBacklogItem] = [
        AgendaBacklogItem(id: "draft", title: "Draft project update", detail: "Inbox", bucket: .inbox, pills: ["Inbox"]),
        AgendaBacklogItem(id: "specs", title: "Review design specs", detail: "Overdue · 2d", bucket: .overdue),
        AgendaBacklogItem(id: "dentist", title: "Book dentist appointment", detail: "No date", bucket: .undated),
        AgendaBacklogItem(id: "walkthrough", kind: .event, title: "Venue walkthrough", detail: "No date", bucket: .undated),
    ]

    /// Schedule candidates for a filter, in backlog order. Events are excluded.
    public func candidates(_ filter: AgendaScheduleFilter) -> [AgendaBacklogItem] {
        backlog.filter { $0.kind == .task && $0.matches(filter) }
    }

    /// The Schedule badge count: every task still waiting to be scheduled.
    public var scheduleCount: Int { candidates(.all).count }

    /// Applies Done: the selected tasks leave the backlog together; those scheduled on the viewed day
    /// are returned as Agenda rows at the chosen time (other days are off-screen). Unknown IDs and events
    /// are ignored.
    public mutating func schedule(_ request: AgendaScheduleRequest) -> [AgendaRowItem] {
        let chosen = backlog.filter { $0.kind == .task && request.taskIDs.contains($0.id) }
        guard !chosen.isEmpty else { return [] }
        let ids = Set(chosen.map(\.id))
        backlog.removeAll { ids.contains($0.id) }
        guard request.day == viewedDay else { return [] }
        return chosen.map {
            AgendaRowItem(id: "scheduled-\($0.id)", kind: .task, title: $0.title,
                          timeLabel: request.time.label, sortMinutes: request.time.sortMinutes, pills: $0.pills)
        }
    }

    /// Applies Save: a valid draft becomes a new Agenda row on the viewed day; a blank one is ignored.
    /// Tasks keep the chosen list (none for No List) and show the pending ring; events keep the calendar.
    public mutating func create(_ draft: AgendaCreationDraft) -> AgendaRowItem? {
        guard draft.canSave else { return nil }
        createdCount += 1
        let title = draft.title.trimmingCharacters(in: .whitespacesAndNewlines)
        let start = draft.start
        switch draft.mode {
        case .task:
            return AgendaRowItem(id: "created-task-\(createdCount)", kind: .task, title: title,
                                 timeLabel: start.label, sortMinutes: start.sortMinutes,
                                 pills: draft.taskList == .noList ? [] : [draft.taskList.title], pending: true)
        case .event:
            return AgendaRowItem(id: "created-event-\(createdCount)", kind: .event, title: title,
                                 timeLabel: start.label, sortMinutes: start.sortMinutes,
                                 pills: [draft.calendar.title], calendar: draft.calendar)
        }
    }
}
