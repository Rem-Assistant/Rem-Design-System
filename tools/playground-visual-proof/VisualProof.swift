import XCTest

// Diagnostic-only XCTest transport; appended to the test target in a disposable checkout.
// No application sources or original tests are modified. Decisions come from PNG inspection.
final class VisualProof: XCTestCase {
    let endpoint = "__ENDPOINT__"
    func request(_ path: String, body: Data? = nil) throws -> Data {
        let done = expectation(description: path)
        var result: Data?
        var failure: Error?
        var request = URLRequest(url: URL(string: endpoint + path)!)
        request.timeoutInterval = 330
        if let body { request.httpMethod = "POST"; request.httpBody = body }
        URLSession.shared.dataTask(with: request) { data, response, error in
            result = data; failure = error
            if (response as? HTTPURLResponse)?.statusCode != 200 {
                failure = NSError(domain: "VisualProofHTTP", code: 1)
            }
            done.fulfill()
        }.resume()
        wait(for: [done], timeout: 335)
        if let failure { throw failure }
        return result ?? Data()
    }
    func testVisualInputBridge() throws {
        continueAfterFailure = false
        executionTimeAllowance = 900
        let app = XCUIApplication()
        app.launchArguments = ["--settings-light"]
        app.launch()
        for frame in 0..<12 {
            let shot = XCUIScreen.main.screenshot()
            let attachment = XCTAttachment(screenshot: shot)
            attachment.name = "visual-frame-\(frame)"
            attachment.lifetime = .keepAlways
            add(attachment)
            _ = try request("/frame/\(frame)", body: shot.pngRepresentation)
            let data = try request("/action/\(frame)")
            let action = try JSONSerialization.jsonObject(with: data) as! [String: Any]
            let kind = action["action"] as! String
            if kind == "finish" { return }
            func point(_ x: String, _ y: String) -> XCUICoordinate {
                app.coordinate(withNormalizedOffset: CGVector(dx: action[x] as! Double, dy: action[y] as! Double))
            }
            if kind == "tap" {
                point("x", "y").tap()
            } else if kind == "drag" {
                point("x", "y").press(forDuration: 0.05, thenDragTo: point("to_x", "to_y"), withVelocity: .slow, thenHoldForDuration: 0.3)
            } else { XCTFail("Unknown bounded input action"); return }
            RunLoop.current.run(until: Date().addingTimeInterval(0.5))
        }
        XCTFail("Visual proof exceeded action budget")
    }
}
