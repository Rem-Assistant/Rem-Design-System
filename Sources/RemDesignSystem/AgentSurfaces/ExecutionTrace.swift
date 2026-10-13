import SwiftUI

/// **ExecutionTrace** — the agent "show your work" surface. A compact step-timeline that narrates what
/// the agent did: a header (status pill · title · subtitle · timestamp · close), a divider, then a
/// vertical list of step rows grouped into lanes (**MAIN** / **SUBAGENT**), and a "Working" footer
/// while the run is in progress.
///
/// Each step row is `status-icon · (title + detail) · chevron`: a green `checkmark.circle.fill` for a
/// completed step, a red `xmark.circle.fill` for a failed step, and a brand-blue hollow `circle` for the
/// step in flight. A lane header (`MAIN`, `SUBAGENT 01`) is emitted whenever the lane changes between
/// consecutive steps, so the list reads as grouped sections without the caller hand-placing headers.
///
/// Figma canonical: **ExecutionTrace** `431:21`, built on the base **Timeline** component `482:56`.
/// Source lineage: the agent trace surface. Compose sibling: `ExecutionTrace` in
/// `agentsurfaces/ExecutionTrace.kt`.
///
/// Packaging note: the Figma specimen is a full-bleed white screen; here it is packaged as a
/// `backgroundSecondary` rounded surface (`radius.large`) so it drops into a chat transcript as a
/// distinct card, per the DS card language. The header status pill is a filled *tone* capsule (not the
/// quiet `RemPill`), matching the specimen's solid "In progress" pill.
public struct ExecutionTrace: View {
    /// The run-level status shown in the header pill and gating the "Working" footer.
    public enum Status: Equatable {
        case inProgress
        case completed
        case failed
    }

    /// Which lane a step belongs to. The lane drives the grouping header. `subagent` carries an optional
    /// ordinal (e.g. `"01"`) so the header can read "SUBAGENT 01".
    public enum Lane: Equatable {
        case main
        case subagent(String? = nil)
    }

    /// A single step's outcome, driving its leading icon.
    public enum StepStatus: Equatable {
        /// In flight — brand-blue hollow circle.
        case active
        /// Completed — green filled checkmark.
        case done
        /// Failed — red filled cross.
        case failed
    }

    /// One row in the timeline.
    public struct Step: Identifiable {
        public let id = UUID()
        public let label: String
        public let detail: String?
        public let lane: Lane
        public let status: StepStatus

        public init(
            label: String,
            detail: String? = nil,
            lane: Lane = .main,
            status: StepStatus = .done
        ) {
            self.label = label
            self.detail = detail
            self.lane = lane
            self.status = status
        }
    }

    private let status: Status
    private let title: String
    private let subtitle: String?
    private let timestamp: String?
    private let steps: [Step]
    private let footer: String
    private let onClose: (() -> Void)?

    /// - Parameters:
    ///   - status: run-level status — drives the header pill and whether the footer shows.
    ///   - title: the run title (e.g. "Build RFE checklist").
    ///   - subtitle: optional supporting line beneath the title.
    ///   - timestamp: optional time label (e.g. "10:49pm").
    ///   - steps: the ordered step rows; lane headers are derived from lane changes.
    ///   - footer: the in-progress footer label (default "Working"); shown only while `.inProgress`.
    ///   - onClose: optional close handler. The `xmark` is always drawn; it becomes tappable when set.
    public init(
        status: Status = .inProgress,
        title: String,
        subtitle: String? = nil,
        timestamp: String? = nil,
        steps: [Step],
        footer: String = "Working",
        onClose: (() -> Void)? = nil
    ) {
        self.status = status
        self.title = title
        self.subtitle = subtitle
        self.timestamp = timestamp
        self.steps = steps
        self.footer = footer
        self.onClose = onClose
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            Divider()
                .overlay(DesignTokens.Color.separator)
            body(for: steps)
        }
        .background(
            DesignTokens.Color.backgroundSecondary,
            in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.large, style: .continuous)
        )
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            HStack(alignment: .center) {
                statusPill
                Spacer(minLength: DesignTokens.Spacing.sm)
                closeButton
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(DesignTokens.Typography.title3Bold)
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                if let subtitle {
                    Text(subtitle)
                        .font(DesignTokens.Typography.subheadline)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                }
                if let timestamp {
                    Text(timestamp)
                        .font(DesignTokens.Typography.caption1)
                        .foregroundStyle(DesignTokens.Color.labelTertiary)
                }
            }
        }
        .padding(DesignTokens.Spacing.lg)
    }

    private var statusPill: some View {
        Text(statusLabel.uppercased())
            .font(DesignTokens.Typography.caption1Bold)
            .foregroundStyle(DesignTokens.Color.labelOnColor)
            .padding(.horizontal, DesignTokens.Spacing.md)
            .padding(.vertical, 6)
            .background(statusTone, in: Capsule())
    }

    @ViewBuilder
    private var closeButton: some View {
        let glyph = Image(systemName: "xmark")
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(DesignTokens.Color.labelSecondary)
        if let onClose {
            Button(action: onClose) { glyph }
                .buttonStyle(.plain)
        } else {
            glyph
        }
    }

    private var statusLabel: String {
        switch status {
        case .inProgress: return "In progress"
        case .completed:  return "Completed"
        case .failed:     return "Failed"
        }
    }

    private var statusTone: Color {
        switch status {
        case .inProgress: return DesignTokens.Color.labelPrimary
        case .completed:  return DesignTokens.Color.systemGreen
        case .failed:     return DesignTokens.Color.systemRed
        }
    }

    // MARK: - Body (lane-grouped step list)

    @ViewBuilder
    private func body(for steps: [Step]) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            ForEach(Array(steps.enumerated()), id: \.element.id) { index, step in
                if index == 0 || steps[index - 1].lane != step.lane {
                    laneHeader(step.lane)
                        .padding(.top, index == 0 ? 0 : DesignTokens.Spacing.sm)
                }
                stepRow(step)
            }
            if status == .inProgress {
                workingFooter
                    .padding(.top, DesignTokens.Spacing.xs)
            }
        }
        .padding(DesignTokens.Spacing.lg)
    }

    private func laneHeader(_ lane: Lane) -> some View {
        Text(laneTitle(lane))
            .font(DesignTokens.Typography.caption1)
            .tracking(0.5)
            .foregroundStyle(DesignTokens.Color.labelTertiary)
    }

    private func laneTitle(_ lane: Lane) -> String {
        switch lane {
        case .main:
            return "MAIN"
        case .subagent(let ordinal):
            return ordinal.map { "SUBAGENT \($0)" } ?? "SUBAGENT"
        }
    }

    private func stepRow(_ step: Step) -> some View {
        HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
            stepIcon(step.status)
            VStack(alignment: .leading, spacing: 2) {
                Text(step.label)
                    .font(DesignTokens.Typography.subheadline.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                if let detail = step.detail {
                    Text(detail)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            Spacer(minLength: DesignTokens.Spacing.sm)
            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(DesignTokens.Color.labelTertiary)
                .padding(.top, 2)
        }
    }

    @ViewBuilder
    private func stepIcon(_ status: StepStatus) -> some View {
        let icon: (String, Color) = {
            switch status {
            case .active: return ("circle", DesignTokens.Color.brandBlue)
            case .done:   return ("checkmark.circle.fill", DesignTokens.Color.systemGreen)
            case .failed: return ("xmark.circle.fill", DesignTokens.Color.systemRed)
            }
        }()
        Image(systemName: icon.0)
            .font(.system(size: 22, weight: .regular))
            .foregroundStyle(icon.1)
            .frame(width: 24, height: 24)
    }

    private var workingFooter: some View {
        HStack(spacing: DesignTokens.Spacing.sm) {
            Image(systemName: "arrow.triangle.2.circlepath")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(DesignTokens.Color.brandBlue)
            Text(footer)
                .font(DesignTokens.Typography.subheadline)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
        }
    }
}

#if DEBUG
#Preview("ExecutionTrace") {
    ExecutionTrace(
        status: .inProgress,
        title: "Build RFE checklist",
        subtitle: "Writing the RFE checklist PDF template",
        timestamp: "10:49pm",
        steps: [
            .init(
                label: "Launched H-1B RFE Checklist Tailoring Subagent",
                detail: "Delegated the checklist prep to a subagent via artifact.send_input, covering a tailored checklist for USCIS.",
                lane: .main,
                status: .done
            ),
            .init(
                label: "Prepared RFE Checklist Source Directories",
                detail: "Staged the checklist sources; the concatenate step returned an incomplete JSON payload.",
                lane: .subagent("01"),
                status: .failed
            ),
            .init(
                label: "Found USCIS RFE Official Results",
                detail: "Web search targeting USCIS official guidance for H-1B Requests for Evidence.",
                lane: .subagent("01"),
                status: .done
            ),
            .init(
                label: "Created index.html source file",
                detail: "Wrote the RFE checklist HTML template used to render the PDF.",
                lane: .subagent("01"),
                status: .done
            )
        ]
    )
    .padding(24)
    .frame(width: 402)
    .background(DesignTokens.Color.backgroundPrimary)
}
#endif
