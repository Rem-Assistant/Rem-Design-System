#if canImport(UIKit)
import XCTest
import SwiftUI
import UIKit
import RemDesignSystem

/// iOS render evidence for `RemGlassIconButton`, paired by name with the Compose `GlassIconButtonEvidence.kt` shots:
/// `RemGlassIconButton-glass-*` (a May-style top bar, menu leading and back to chat trailing, over busy content) and
/// `RemGlassIconButton-states-*` (enabled / disabled, both initialisers, sizes). On a pre-26 SDK the circle is the
/// `.ultraThinMaterial` fallback; `layer.render(in:)` may flatten material blur, so judge the glass itself on device.
@MainActor
final class GlassIconButtonRenderSnapshots: XCTestCase {

    func testRenderGlassIconButton() {
        for dark in [false, true] {
            let scheme = dark ? "dark" : "light"
            render("RemGlassIconButton-glass-\(scheme)", width: 402, height: nil, dark: dark) { glassTopBar }
            render("RemGlassIconButton-states-\(scheme)", width: 402, height: nil, dark: dark) { states }
        }
    }

    @ViewBuilder private var glassTopBar: some View {
        VStack(alignment: .leading, spacing: 0) {
            caption("Glass: top bar over content scrolled under the bar")
            ZStack(alignment: .top) {
                busyContent
                HStack {
                    RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu") {}
                    Spacer()
                    RemGlassIconButton(systemName: "chevron.right.2", accessibilityLabel: "Back to chat") {}
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.sm)
            }
            .frame(height: 260, alignment: .top)
            .clipped()
        }
        .background(DesignTokens.Color.backgroundPrimary)
    }

    /// Colourful, high-frequency content so the glass (or its absence) is obvious under the circles. Same grid as Compose.
    private var busyContent: some View {
        let swatches = [DesignTokens.Color.systemBlue, DesignTokens.Color.systemOrange, DesignTokens.Color.systemGreen,
                        DesignTokens.Color.systemPink, DesignTokens.Color.systemIndigo]
        return VStack(alignment: .leading, spacing: 6) {
            ForEach(0..<6, id: \.self) { row in
                HStack(spacing: 6) {
                    ForEach(0..<9, id: \.self) { col in
                        RoundedRectangle(cornerRadius: 8)
                            .fill(swatches[(row + col) % swatches.count])
                            .frame(width: 36, height: 36)
                            .overlay(Text("\(row * 9 + col)").font(DesignTokens.Typography.caption1Bold)
                                .foregroundStyle(DesignTokens.Color.labelOnColor))
                    }
                }
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 20)
    }

    @ViewBuilder private var states: some View {
        VStack(alignment: .leading, spacing: 0) {
            caption("Enabled, disabled")
            row {
                RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu") {}
                RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu") {}.disabled(true)
                RemGlassIconButton(systemName: "chevron.right.2", accessibilityLabel: "Back to chat") {}
                RemGlassIconButton(systemName: "chevron.right.2", accessibilityLabel: "Back to chat") {}.disabled(true)
            }
            caption("Initialisers: SF Symbol, Image")
            row {
                RemGlassIconButton(systemName: "xmark", accessibilityLabel: "Close") {}
                RemGlassIconButton(image: Image(systemName: "magnifyingglass"), accessibilityLabel: "Search") {}
            }
            caption("Sizes: 32 circle (44 touch target, outlined), 44 default, 56")
            row {
                RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu", size: 32) {}
                    .background(DesignTokens.Color.fillTertiary)
                RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu") {}
                RemGlassIconButton(systemName: "line.3.horizontal", accessibilityLabel: "Menu", size: 56) {}
            }
        }
        .padding(.vertical, DesignTokens.Spacing.lg)
        .background(DesignTokens.Color.backgroundPrimary)
    }

    private func row(@ViewBuilder _ content: () -> some View) -> some View {
        HStack(spacing: DesignTokens.Spacing.lg) { content() }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.sm)
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .font(DesignTokens.Typography.caption1)
            .foregroundStyle(DesignTokens.Color.labelSecondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.xs)
    }

    // MARK: - iOS hosting-controller snapshot (mirrors RenderSnapshots.render)

    private func render(_ name: String, width: CGFloat, height: CGFloat?, dark: Bool,
                        @ViewBuilder _ content: () -> some View) {
        let root = content()
            .frame(width: width)
            .environment(\.colorScheme, dark ? .dark : .light)

        let host = UIHostingController(rootView: AnyView(root))
        host.overrideUserInterfaceStyle = dark ? .dark : .light
        if height == nil {
            host.safeAreaRegions = []
        }

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
        RunLoop.current.run(until: Date().addingTimeInterval(0.15))

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 2
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let image = renderer.image { ctx in
            host.view.layer.render(in: ctx.cgContext)
        }

        let attachment = XCTAttachment(image: image)
        attachment.name = "\(name).png"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
#endif
