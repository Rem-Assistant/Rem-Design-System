import SwiftUI
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

/// Moving highlight for skeleton placeholders. Ported from the shipping app's `ShimmerModifier`
/// (`RemClaw/Shared/Views/DesignTokens.swift`), with one addition: when Reduce Motion is on the
/// placeholder stays static instead of sweeping.
public struct RemShimmerModifier: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var phase: CGFloat = -1

    public init() {}

    public func body(content: Content) -> some View {
        content
            .overlay {
                if !reduceMotion {
                    GeometryReader { proxy in
                        LinearGradient(
                            colors: [.clear, .white.opacity(0.3), .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                        .frame(width: proxy.size.width)
                        .offset(x: phase * proxy.size.width)
                    }
                    .mask(content)
                    .allowsHitTesting(false)
                }
            }
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) { phase = 1 }
            }
    }
}

public extension View {
    /// Applies the shared skeleton shimmer (static under Reduce Motion).
    func remShimmering() -> some View { modifier(RemShimmerModifier()) }
}

/// One rounded placeholder bar, in the skeleton fill used by the shipping app's row skeletons.
public struct RemSkeletonBlock: View {
    private let width: CGFloat?
    private let height: CGFloat
    private let cornerRadius: CGFloat

    public init(width: CGFloat? = nil, height: CGFloat = 14, cornerRadius: CGFloat = 4) {
        self.width = width
        self.height = height
        self.cornerRadius = cornerRadius
    }

    public var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(DesignTokens.Color.labelSecondary.opacity(0.2))
            .frame(width: width, height: height)
            .frame(maxWidth: width == nil ? .infinity : nil, alignment: .leading)
    }
}

/// Grouped-list skeleton: an icon tile plus a title bar per row, inside one rounded section.
/// Exposed to assistive technologies as a single element carrying `label`, so VoiceOver reads the
/// loading state once rather than every placeholder bar.
public struct RemSkeletonList: View {
    private let rows: Int
    private let label: String

    public init(rows: Int = 4, label: String) {
        self.rows = rows
        self.label = label
    }

    public var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<rows, id: \.self) { index in
                HStack(spacing: DesignTokens.Spacing.md) {
                    RemSkeletonBlock(width: 30, height: 30, cornerRadius: 7)
                    RemSkeletonBlock(width: index.isMultiple(of: 2) ? 160 : 120)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .frame(minHeight: 52)
                if index < rows - 1 {
                    Divider().padding(.leading, DesignTokens.Spacing.lg + 30 + DesignTokens.Spacing.md)
                }
            }
        }
        .background(DesignTokens.Color.backgroundSecondary, in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.medium))
        .remShimmering()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(.updatesFrequently)
    }
}

/// Posts a polite VoiceOver/assistive announcement (for example, "Agent settings loaded") when a
/// loading state resolves without moving focus.
@MainActor
public func remAnnounce(_ message: String) {
    #if canImport(UIKit)
    UIAccessibility.post(notification: .announcement, argument: message)
    #elseif canImport(AppKit)
    NSAccessibility.post(
        element: NSApplication.shared,
        notification: .announcementRequested,
        userInfo: [.announcement: message, .priority: NSAccessibilityPriorityLevel.medium.rawValue]
    )
    #endif
}

#Preview("Skeleton list") {
    RemSkeletonList(label: "Loading")
        .padding()
        .background(DesignTokens.Color.backgroundPrimary)
}
