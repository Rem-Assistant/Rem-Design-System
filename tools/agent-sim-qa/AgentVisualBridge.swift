
// MARK: - Agent visual diagnostic bridge (appended by .github/workflows/agent-sim-qa.yml)
//
// Diagnostic only. The workflow appends this class to a disposable checkout's UI-test file after
// verifying that the app sources are pristine; app sources and the original tests are not edited.
// It is a local input executor: it reads typed commands from a runner-local mailbox directory
// (AGENT_BRIDGE_DIR, passed as TEST_RUNNER_AGENT_BRIDGE_DIR) and writes one result file per
// command. It opens no port and accepts no shell text. Screenshots are taken outside XCTest by the
// controller (`xcrun simctl io <udid> screenshot`) so a stalled XCTest input still yields frames.

final class AgentVisualBridgeUITests: XCTestCase {
    private let app = XCUIApplication()
    private var mailbox = URL(fileURLWithPath: "/nonexistent")
    private var issues: [String] = []

    /// XCTest reports input failures (for example a synthesized event that never completes) as
    /// issues rather than thrown errors; keep them so the command's result says it failed.
    override func record(_ issue: XCTIssue) {
        issues.append(String(issue.compactDescription.prefix(300)))
        super.record(issue)
    }

    func testAgentVisualBridge() throws {
        continueAfterFailure = true
        executionTimeAllowance = 3600 // capped by the controller's -maximum-test-execution-time-allowance
        guard let dir = ProcessInfo.processInfo.environment["AGENT_BRIDGE_DIR"], !dir.isEmpty else {
            throw XCTSkip("AGENT_BRIDGE_DIR is unset; this bridge only runs inside agent-sim-qa.")
        }
        mailbox = URL(fileURLWithPath: dir, isDirectory: true)
        // A named profile, never free arguments: "journey" is JourneyLaunch's set for Build 9's journeys
        // (Developer hub, light, no repeating motion so XCTest reaches idle); anything else is v1's.
        let profile = ProcessInfo.processInfo.environment["AGENT_LAUNCH_PROFILE"] ?? "legacy"
        app.launchArguments = profile == "journey" ? ["--developer", "--settings-light", "--no-motion"]
                                                   : ["--settings-light"]
        app.launch()
        let size = app.windows.firstMatch.frame.size
        write("ready.json", ["ok": true, "width": Double(size.width), "height": Double(size.height),
                             "at": Date().timeIntervalSince1970])
        var seq = 0
        while true {
            let command = mailbox.appendingPathComponent(String(format: "cmd-%03d.json", seq))
            guard let data = try? Data(contentsOf: command),
                  let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                Thread.sleep(forTimeInterval: 0.2)
                continue
            }
            let kind = object["kind"] as? String ?? ""
            let started = Date().timeIntervalSince1970
            write(String(format: "ack-%03d.json", seq), ["seq": seq, "kind": kind, "at": started])
            var result: [String: Any] = ["seq": seq, "kind": kind, "started_at": started]
            issues.removeAll()
            do {
                if kind == "end" {
                    result["ok"] = true
                    result["ended_at"] = Date().timeIntervalSince1970
                    write(String(format: "res-%03d.json", seq), result)
                    return
                }
                try execute(kind, object, into: &result)
                result["ok"] = issues.isEmpty
                if let issue = issues.first { result["error"] = issue }
            } catch {
                result["ok"] = false
                result["error"] = String(String(describing: error).prefix(300))
            }
            result["ended_at"] = Date().timeIntervalSince1970
            write(String(format: "res-%03d.json", seq), result)
            seq += 1
        }
    }

    private struct BridgeError: Error, CustomStringConvertible {
        let description: String
    }

    private func number(_ object: [String: Any], _ key: String) throws -> CGFloat {
        guard let value = object[key] as? Double, value.isFinite else { throw BridgeError(description: "missing \(key)") }
        return CGFloat(value)
    }

    /// An optional bounded number (the controller validates the same bounds before sending).
    private func bounded(_ object: [String: Any], _ key: String, _ fallback: Double, _ range: ClosedRange<Double>) throws -> Double {
        guard let raw = object[key] else { return fallback }
        guard let value = raw as? Double, value.isFinite, range.contains(value) else {
            throw BridgeError(description: "invalid \(key)")
        }
        return value
    }

    private func point(_ x: CGFloat, _ y: CGFloat) -> XCUICoordinate {
        app.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: x, dy: y))
    }

    private func execute(_ kind: String, _ object: [String: Any], into result: inout [String: Any]) throws {
        switch kind {
        case "observe":
            result["ui"] = try describeUI()
        case "tap_id":
            guard let id = object["id"] as? String, !id.isEmpty else { throw BridgeError(description: "missing id") }
            let element = app.descendants(matching: .any).matching(identifier: id).firstMatch
            guard element.waitForExistence(timeout: 3) else { throw BridgeError(description: "no element \(id)") }
            let frame = element.frame
            result["element_frame"] = [frame.minX, frame.minY, frame.width, frame.height].map { Double($0) }
            guard element.isHittable else { throw BridgeError(description: "element \(id) is not hittable") }
            element.tap()
        case "tap_point":
            let x = try number(object, "x"), y = try number(object, "y")
            point(x, y).tap()
        case "drag":
            let x = try number(object, "x"), y = try number(object, "y")
            let toX = try number(object, "to_x"), toY = try number(object, "to_y")
            // No timing fields: exactly v1's drag, so earlier scenarios keep their input.
            if object["speed"] == nil && object["press_s"] == nil && object["hold_s"] == nil {
                point(x, y).press(forDuration: 0.1, thenDragTo: point(toX, toY))
                return
            }
            let press = try bounded(object, "press_s", 0.1, 0.05...1.0)
            let hold = try bounded(object, "hold_s", 0, 0...2)
            let speeds: [String: XCUIGestureVelocity] = ["slow": .slow, "default": .default, "fast": .fast]
            guard let speed = speeds[object["speed"] as? String ?? "default"] else {
                throw BridgeError(description: "invalid speed")
            }
            point(x, y).press(forDuration: press, thenDragTo: point(toX, toY), withVelocity: speed,
                              thenHoldForDuration: hold)
        case "long_press":
            let x = try number(object, "x"), y = try number(object, "y")
            let duration = try bounded(object, "duration_s", 1.0, 0.5...2.0)
            point(x, y).press(forDuration: duration)
        default:
            throw BridgeError(description: "unknown command")
        }
    }

    /// A bounded, flat accessibility listing (type, identifier, label, frame in points).
    private func describeUI() throws -> String {
        let names: [UInt: String] = [1: "other", 4: "window", 5: "sheet", 7: "alert", 9: "button",
            19: "keyboard", 21: "navigationBar", 22: "tabBar", 24: "toolbar", 26: "table", 32: "collectionView",
            40: "switch", 42: "link", 43: "image", 46: "scrollView", 48: "staticText", 49: "textField",
            52: "textView", 53: "menu", 54: "menuItem", 75: "cell"]
        var lines: [String] = []
        func visit(_ node: XCUIElementSnapshot) {
            guard lines.count < 300 else { return }
            let type = names[node.elementType.rawValue] ?? "type\(node.elementType.rawValue)"
            if !node.identifier.isEmpty || !node.label.isEmpty || type != "other" {
                let f = node.frame
                let label = node.label.replacingOccurrences(of: "\n", with: " ").prefix(80)
                lines.append("\(type) id=\(node.identifier.isEmpty ? "-" : node.identifier) label=\"\(label)\" "
                             + "frame=\(Int(f.minX)),\(Int(f.minY)),\(Int(f.width)),\(Int(f.height))"
                             + (node.isEnabled ? "" : " disabled"))
            }
            for child in node.children { visit(child) }
        }
        visit(try app.snapshot())
        return lines.joined(separator: "\n")
    }

    private func write(_ name: String, _ object: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: object) else { return }
        try? data.write(to: mailbox.appendingPathComponent(name), options: .atomic)
    }
}
