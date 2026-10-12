import XCTest
import RemDesignSystem

/// The app-parity time column's pure contract: RemClaw `TaskEventRowView.swift` `formatHour` (two-digit
/// 12-hour, 0 → 12), `formatMinutes` and `formatAMPM`, and its 64pt column. Paired with the Compose
/// `TaskEventClockFaceTest.kt`, which asserts the same cases.
final class TaskEventClockFaceTests: XCTestCase {
    private func face(_ label: String) -> TaskEventClockFace? { TaskEventClockFace(label: label) }

    private func expect(_ label: String, _ hour: String, _ minutes: String, _ meridiem: String,
                        file: StaticString = #filePath, line: UInt = #line) {
        guard let face = face(label) else { return XCTFail("'\(label)' should parse", file: file, line: line) }
        XCTAssertEqual([face.hour, face.minutes, face.meridiem], [hour, minutes, meridiem], label, file: file, line: line)
    }

    func testColumnIsTheAppsSixtyFourPoints() {
        XCTAssertEqual(TaskEventClockFace.appColumnWidth, 64)
    }

    func testTwentyFourHourLabelsBecomeTheAppsTwelveHourFace() {
        expect("9:00", "09", "00", "AM")
        expect("14:30", "02", "30", "PM")
        expect("0:05", "12", "05", "AM")
        expect("12:00", "12", "00", "PM")
        expect("23:59", "11", "59", "PM")
    }

    func testMeridiemLabelsKeepTheirHalfOfTheDay() {
        expect("8:00 AM", "08", "00", "AM")
        expect("7:00 pm", "07", "00", "PM")
        expect("12:15 AM", "12", "15", "AM")
        expect("12:15PM", "12", "15", "PM")
        // iOS formatters put a narrow no-break space before AM/PM.
        expect("5:00\u{202F}PM", "05", "00", "PM")
    }

    func testUnreadableLabelsFallBackToTheCompactLabel() {
        for label in ["Oct 14", "", "9", "24:00", "9:60", "13:00 PM", "0:30 AM", "9:5", "+9:00", "9:00 noon"] {
            XCTAssertNil(face(label), "'\(label)' is not a clock time")
        }
    }

    func testSpokenTimeIsOnePhrase() {
        XCTAssertEqual(face("9:00")?.spoken, "9:00 AM")
        XCTAssertEqual(face("14:30")?.spoken, "2:30 PM")
    }

    func testEveryScheduleFilterHasAnEmptyState() {
        for filter in AgendaScheduleFilter.allCases {
            XCTAssertFalse(filter.emptyMessage.isEmpty, "\(filter)")
            XCTAssertFalse(filter.emptySymbol.isEmpty, "\(filter)")
        }
        XCTAssertEqual(AgendaScheduleFilter.inbox.emptyMessage, "Tasks you capture without a date land here.")
    }
}
