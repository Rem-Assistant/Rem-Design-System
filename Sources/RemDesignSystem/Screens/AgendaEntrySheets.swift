import SwiftUI

// MARK: - Add New · creation sheet

/// **AgendaCreationSheet** — Add New's destination: the authored creation form (Creation · Filled
/// drafts, choosers and outcomes `2390:28498`; the create menu's "New Task or Event" `2049:10336`).
/// Cancel / **New Task or Event** / Save, a New Task · New Event segmented picker, the title with its
/// native marker (task ring or event bar), the task-list or calendar chooser, the inline date/time,
/// duration, alert and repeat summaries, and notes. Save returns the draft to the host, which applies it
/// in memory; Cancel leaves the Agenda unchanged. The summaries are read-only here — their inspector
/// is separate work in the design. Compose twin: `AgendaCreationSheet` in `screens/AgendaEntrySheets.kt`.
public struct AgendaCreationSheet: View {
    @State private var draft: AgendaCreationDraft
    private let onCancel: () -> Void
    private let onSave: (AgendaCreationDraft) -> Void

    public init(
        draft: AgendaCreationDraft = AgendaCreationDraft(),
        onCancel: @escaping () -> Void = {},
        onSave: @escaping (AgendaCreationDraft) -> Void = { _ in }
    ) {
        _draft = State(initialValue: draft)
        self.onCancel = onCancel
        self.onSave = onSave
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    Picker("Kind", selection: $draft.mode) {
                        ForEach(AgendaCreationDraft.Mode.allCases, id: \.self) { Text($0.title).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                    .accessibilityIdentifier("agendaCreate.mode")
                    .padding(.bottom, DesignTokens.Spacing.sm)

                    HStack(spacing: DesignTokens.Spacing.sm) {
                        marker
                        TextField(draft.mode == .task ? "Task title" : "Event title", text: $draft.title)
                            .font(DesignTokens.Typography.title1Bold)
                            .foregroundStyle(DesignTokens.Color.labelPrimary)
                            .accessibilityIdentifier("agendaCreate.title")
                    }
                    chooser
                    summaries
                    TextField("Notes", text: $draft.notes, axis: .vertical)
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .lineLimit(3...)
                        .padding(.top, DesignTokens.Spacing.xs)
                        .accessibilityIdentifier("agendaCreate.notes")
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.top, DesignTokens.Spacing.sm)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .background(DesignTokens.Color.backgroundPrimary)
            .navigationTitle("New Task or Event")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                        .accessibilityIdentifier("agendaCreate.cancel")
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { onSave(draft) }
                        .disabled(!draft.canSave)
                        .accessibilityIdentifier("agendaCreate.save")
                }
            }
        }
    }

    /// Task: the status ring. Event: the calendar-colored bar.
    @ViewBuilder private var marker: some View {
        switch draft.mode {
        case .task:
            Image(systemName: "circle")
                .font(DesignTokens.Typography.title1)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .accessibilityHidden(true)
        case .event:
            RoundedRectangle(cornerRadius: 2)
                .fill(calendarColor(draft.calendar))
                .frame(width: 4, height: 30)
                .accessibilityHidden(true)
        }
    }

    /// The designed chooser: No List / Follow-ups / Work for tasks; Personal / Work for events.
    @ViewBuilder private var chooser: some View {
        Menu {
            switch draft.mode {
            case .task:
                Picker("Task list", selection: $draft.taskList) {
                    ForEach(AgendaCreationDraft.TaskList.allCases, id: \.self) { Text($0.title).tag($0) }
                }
            case .event:
                Picker("Calendar", selection: $draft.calendar) {
                    ForEach(AgendaCreationDraft.EventCalendar.allCases, id: \.self) { Text($0.title).tag($0) }
                }
            }
        } label: {
            HStack(spacing: DesignTokens.Spacing.xs) {
                switch draft.mode {
                case .task:
                    Image(systemName: "list.bullet")
                case .event:
                    Circle().fill(calendarColor(draft.calendar)).frame(width: 10, height: 10)
                }
                Text(draft.chooserLabel)
                Image(systemName: "chevron.down").font(DesignTokens.Typography.caption1)
            }
            .font(DesignTokens.Typography.subheadline)
            .foregroundStyle(draft.mode == .task && draft.taskList == .noList
                             ? DesignTokens.Color.labelSecondary : DesignTokens.Color.labelPrimary)
            .padding(.horizontal, DesignTokens.Spacing.sm)
            .padding(.vertical, DesignTokens.Spacing.xs)
            .background(Capsule().fill(DesignTokens.Color.fillTertiary))
        }
        .accessibilityIdentifier("agendaCreate.chooser")
    }

    /// The authored inline summaries: when, then duration · alert · repeat.
    private var summaries: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            summary("clock", draft.whenSummary)
            HStack(spacing: DesignTokens.Spacing.sm) {
                summary("alarm", draft.durationSummary)
                summary("bell", "No alert")
                summary("repeat", "No repeat")
            }
        }
        .padding(.vertical, DesignTokens.Spacing.xs)
    }

    private func summary(_ symbol: String, _ text: String) -> some View {
        HStack(spacing: DesignTokens.Spacing.xs) {
            Image(systemName: symbol).accessibilityHidden(true)
            Text(text)
        }
        .font(DesignTokens.Typography.body)
        .foregroundStyle(DesignTokens.Color.labelSecondary)
    }
}

/// Personal → system blue, Work → system orange (the saved-event bar colors in `2390:28498`).
func calendarColor(_ calendar: AgendaCreationDraft.EventCalendar) -> Color {
    calendar == .work ? DesignTokens.Color.systemOrange : DesignTokens.Color.systemBlue
}

// MARK: - Schedule · Schedule Tasks sheet

/// **AgendaScheduleSheet** — Schedule's destination: **Schedule Tasks** (`2295:13691`). All / Inbox /
/// Overdue filters over existing tasks (events are never listed); selection survives filter changes.
/// **Add to Today** (or Tomorrow / a date, for the viewed day) opens **Pick a Time** for that day; **Plan**
/// opens **Pick a Date**, then Next → **Pick a Time**. Pick a Time's Date row expands the calendar. Done
/// schedules every selected task together at the chosen slot (default 9:00 AM); the close control leaves
/// the Agenda unchanged. Native `DatePicker`s render the calendar and time wheel. Compose twin:
/// `AgendaScheduleSheet` in `screens/AgendaEntrySheets.kt`.
public struct AgendaScheduleSheet: View {
    private enum Step { case select, pickDate, pickTime }

    private let fixture: AgendaEntryFixture
    private let onCancel: () -> Void
    private let onDone: (AgendaScheduleRequest) -> Void

    @State private var filter: AgendaScheduleFilter = .all
    @State private var selected: Set<String> = []
    @State private var step: Step = .select
    @State private var day: AgendaDay
    @State private var time: AgendaTime = .scheduleDefault
    @State private var dateExpanded = false

    public init(
        fixture: AgendaEntryFixture,
        onCancel: @escaping () -> Void = {},
        onDone: @escaping (AgendaScheduleRequest) -> Void = { _ in }
    ) {
        self.fixture = fixture
        self.onCancel = onCancel
        self.onDone = onDone
        _day = State(initialValue: fixture.viewedDay)
    }

    public var body: some View {
        NavigationStack {
            Group {
                switch step {
                case .select: selectStep
                case .pickDate: pickDateStep
                case .pickTime: pickTimeStep
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(DesignTokens.Color.backgroundPrimary)
            .navigationTitle(title)
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: onCancel) { Image(systemName: "xmark") }
                        .accessibilityLabel("Close")
                        .accessibilityIdentifier("agendaSchedule.close")
                }
                ToolbarItem(placement: .confirmationAction) { confirmAction }
            }
        }
        .presentationDetents(step == .select ? [.medium, .large] : [.large])
    }

    /// Schedule Tasks has no trailing action; Pick a Date offers Next, Pick a Time offers Done.
    @ViewBuilder private var confirmAction: some View {
        switch step {
        case .select:
            EmptyView()
        case .pickDate:
            Button("Next") { step = .pickTime }
                .accessibilityIdentifier("agendaSchedule.next")
        case .pickTime:
            Button("Done") { onDone(request) }
                .accessibilityIdentifier("agendaSchedule.done")
        }
    }

    private var title: String {
        switch step {
        case .select: return "Schedule Tasks"
        case .pickDate: return "Pick a Date"
        case .pickTime: return "Pick a Time"
        }
    }

    /// The selected tasks, in list order, scheduled together.
    private var request: AgendaScheduleRequest {
        AgendaScheduleRequest(
            taskIDs: fixture.candidates(.all).map(\.id).filter(selected.contains),
            day: day,
            time: time
        )
    }

    // MARK: Schedule Tasks

    private var selectStep: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Picker("Filter", selection: $filter) {
                ForEach(AgendaScheduleFilter.allCases, id: \.self) { Text($0.title).tag($0) }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .accessibilityIdentifier("agendaSchedule.filter")

            let tasks = fixture.candidates(filter)
            ScrollView {
                if tasks.isEmpty {
                    Text(filter.emptyText)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .frame(maxWidth: .infinity, minHeight: 88)
                        .background(DesignTokens.Color.backgroundSecondary,
                                    in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium))
                        .accessibilityIdentifier("agendaSchedule.empty")
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(tasks.enumerated()), id: \.element.id) { index, task in
                            if index > 0 {
                                Divider().padding(.leading, DesignTokens.Spacing.lg)
                            }
                            taskRow(task)
                        }
                    }
                    .background(DesignTokens.Color.backgroundSecondary,
                                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium))
                }
            }

            HStack(spacing: DesignTokens.Spacing.md) {
                Button {
                    day = fixture.viewedDay
                    step = .pickTime
                } label: {
                    Label(fixture.viewedDay.addToLabel(today: fixture.today), systemImage: "clock")
                }
                .remButton(.rectBlue)
                .accessibilityIdentifier("agendaSchedule.addTo")

                Button {
                    step = .pickDate
                } label: {
                    Label("Plan", systemImage: "calendar")
                }
                .remButton(.rectSecondary)
                .accessibilityIdentifier("agendaSchedule.plan")
            }
            .disabled(selected.isEmpty)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.lg)
        .padding(.bottom, DesignTokens.Spacing.sm)
    }

    /// `ScheduleTaskRow` (`2302:11932` / `2302:11938`): title, metadata, selection glyph.
    private func taskRow(_ task: AgendaBacklogItem) -> some View {
        let isSelected = selected.contains(task.id)
        return Button {
            if isSelected { selected.remove(task.id) } else { selected.insert(task.id) }
        } label: {
            HStack(spacing: DesignTokens.Spacing.sm) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(task.title)
                        .font(DesignTokens.Typography.body)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                    Text(task.detail)
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
                Spacer(minLength: 0)
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(DesignTokens.Typography.title3)
                    .foregroundStyle(isSelected ? DesignTokens.Color.brandBlue : DesignTokens.Color.labelTertiary)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, 11)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityIdentifier("agendaSchedule.task.\(task.id)")
    }

    // MARK: Pick a Date / Pick a Time

    private var pickDateStep: some View {
        ScrollView {
            calendarPicker
                .padding(DesignTokens.Spacing.lg)
        }
    }

    private var pickTimeStep: some View {
        ScrollView {
            VStack(spacing: DesignTokens.Spacing.lg) {
                Button {
                    dateExpanded.toggle()
                } label: {
                    HStack {
                        Text("Date").foregroundStyle(DesignTokens.Color.labelPrimary)
                        Spacer()
                        Text(day.dateRowText(today: fixture.today))
                            .foregroundStyle(DesignTokens.Color.labelSecondary)
                    }
                    .font(DesignTokens.Typography.body)
                    .padding(.horizontal, DesignTokens.Spacing.lg)
                    .padding(.vertical, DesignTokens.Spacing.md)
                    .background(DesignTokens.Color.backgroundSecondary,
                                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("agendaSchedule.dateRow")

                if dateExpanded { calendarPicker }

                timePicker
            }
            .padding(DesignTokens.Spacing.lg)
        }
    }

    private var calendarPicker: some View {
        DatePicker("Date", selection: dateBinding, displayedComponents: .date)
            .datePickerStyle(.graphical)
            .labelsHidden()
            .accessibilityIdentifier("agendaSchedule.datePicker")
    }

    @ViewBuilder private var timePicker: some View {
        let picker = DatePicker("Time", selection: timeBinding, displayedComponents: .hourAndMinute)
            .labelsHidden()
            .accessibilityIdentifier("agendaSchedule.timePicker")
        #if os(iOS)
        picker.datePickerStyle(.wheel)
        #else
        picker
        #endif
    }

    private var dateBinding: Binding<Date> {
        Binding(
            get: { date(on: day, at: time) },
            set: { value in
                let parts = Calendar.current.dateComponents([.year, .month, .day], from: value)
                day = AgendaDay(year: parts.year ?? day.year, month: parts.month ?? day.month, day: parts.day ?? day.day)
            }
        )
    }

    private var timeBinding: Binding<Date> {
        Binding(
            get: { date(on: day, at: time) },
            set: { value in
                let parts = Calendar.current.dateComponents([.hour, .minute], from: value)
                time = AgendaTime(hour: parts.hour ?? time.hour, minute: parts.minute ?? time.minute)
            }
        )
    }

    private func date(on day: AgendaDay, at time: AgendaTime) -> Date {
        Calendar.current.date(from: DateComponents(year: day.year, month: day.month, day: day.day,
                                                   hour: time.hour, minute: time.minute)) ?? Date()
    }
}

#if DEBUG
#Preview("AgendaCreationSheet") {
    AgendaCreationSheet(draft: AgendaCreationDraft(title: "Prepare rehearsal notes", notes: "Bring the revised set list."))
}
#Preview("AgendaScheduleSheet") {
    AgendaScheduleSheet(fixture: AgendaEntryFixture())
}
#endif
