#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

// Faithful iOS screenshot evidence. Runs on an iOS Simulator (via `xcodebuild test`) so the tokens
// resolve to real iOS UIColor semantics — `.systemBackground` is WHITE on iOS (it is grey on macOS,
// which is why the earlier macOS `ImageRenderer` renders looked inverted). Snapshots a real
// `UIHostingController` view hierarchy with `drawHierarchy(afterScreenUpdates:)`, which captures
// `ScrollView`/`List` content that `ImageRenderer` cannot — so the ACTUAL `OnboardingConsentTemplate`
// renders faithfully (no scroll-free re-composition). PNGs go to SNAPSHOT_OUT_DIR for the evidence
// pipeline.
@MainActor
final class RenderSnapshots: XCTestCase {

    private var outDir: URL {
        let base = ProcessInfo.processInfo.environment["SNAPSHOT_OUT_DIR"]
            ?? FileManager.default.currentDirectoryPath + "/artifacts/swiftui"
        let url = URL(fileURLWithPath: base)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    func testRenderAll() throws {
        for dark in [false, true] {
            let suffix = dark ? "dark" : "light"
            try render("RemButton-\(suffix)", width: 300, height: nil, dark: dark) { buttonGallery }
            try render("ContainedIcon-\(suffix)", width: 260, height: nil, dark: dark) { iconRow }
            try render("ListRow-\(suffix)", width: 380, height: nil, dark: dark) { listRowCard }
            try render("Consent-\(suffix)", width: 393, height: 852, dark: dark) { consentScreen }
        }
    }

    // MARK: - Galleries (mirror each component's #Preview)

    private func chevron() -> some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(DesignTokens.Color.labelTertiary)
    }

    @ViewBuilder private var buttonGallery: some View {
        let variants: [(String, RemButtonVariant)] = [
            ("Rect · Black", .rectBlack), ("Rect · Blue", .rectBlue),
            ("Rect · Secondary", .rectSecondary), ("Rect · Destructive", .rectDestructive),
            ("Text · Accent", .textAccent), ("Text · Destructive", .textDestructive),
            ("Pill · Secondary", .pillSecondary),
        ]
        VStack(spacing: 14) {
            ForEach(variants.indices, id: \.self) { i in
                Button(variants[i].0) {}.remButton(variants[i].1)
            }
            Button("Disabled") {}.remButton(.rectBlack).disabled(true)
        }
        .padding(24)
    }

    @ViewBuilder private var iconRow: some View {
        HStack(spacing: 16) {
            ContainedIcon("lock.shield.fill", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
            ContainedIcon("doc.text", fill: .subtle)
            ContainedIcon("shield", fill: .subtle)
        }
        .padding(24)
    }

    @ViewBuilder private var listRowCard: some View {
        VStack(spacing: 0) {
            ListRow("Terms of Service",
                    subtitle: "How Rem accounts, subscriptions, and approved actions work.",
                    action: {},
                    leading: { ContainedIcon("doc.text", fill: .subtle) },
                    trailing: { chevron() })
            Divider().padding(.leading, 60)
            ListRow("Privacy Policy",
                    subtitle: "What Rem, your gateway, and AI or voice providers process.",
                    action: {},
                    leading: { ContainedIcon("shield", fill: .subtle) },
                    trailing: { chevron() })
        }
        .background(DesignTokens.Color.backgroundSecondary)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
        .padding(24)
    }

    // The REAL onboarding consent template (with its ScrollView) — rendered faithfully now.
    private var consentScreen: some View {
        OnboardingConsentTemplate(
            message: "Rem uses your data to answer requests and run approved actions through your personal cloud gateway. You can review or delete your account data in Settings.",
            legalItems: [
                .init(symbol: "doc.text", title: "Terms of Service",
                      subtitle: "How Rem accounts, subscriptions, and approved actions work.", action: {}),
                .init(symbol: "shield", title: "Privacy Policy",
                      subtitle: "What Rem, your gateway, and AI or voice providers process.", action: {}),
            ],
            footnote: "By tapping \u{201C}Accept and Continue,\u{201D} you agree to our Terms of Service and Privacy Policy.",
            onPrimary: {}
        )
    }

    // MARK: - iOS hosting-controller snapshot

    private func render(_ name: String, width: CGFloat, height: CGFloat?, dark: Bool,
                        @ViewBuilder _ content: () -> some View) throws {
        let root = content()
            .frame(width: width)
            .environment(\.colorScheme, dark ? .dark : .light)

        let host = UIHostingController(rootView: AnyView(root))
        host.overrideUserInterfaceStyle = dark ? .dark : .light

        let fitHeight = height
            ?? host.sizeThatFits(in: CGSize(width: width, height: .greatestFiniteMagnitude)).height
        let size = CGSize(width: width, height: fitHeight)

        let window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.overrideUserInterfaceStyle = dark ? .dark : .light
        window.rootViewController = host
        window.makeKeyAndVisible()
        host.view.frame = CGRect(origin: .zero, size: size)
        host.view.setNeedsLayout()
        host.view.layoutIfNeeded()
        // Give SwiftUI a run-loop tick to commit its first render pass before capturing.
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 2
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let image = renderer.image { _ in
            host.view.drawHierarchy(in: CGRect(origin: .zero, size: size), afterScreenUpdates: true)
        }
        guard let png = image.pngData() else { return XCTFail("could not encode \(name)") }
        try png.write(to: outDir.appendingPathComponent("\(name).png"))
        print("wrote \(name).png (\(Int(size.width))x\(Int(size.height)))")
    }
}
#endif
