import SwiftUI

/// Semantic tone for ``RemToast``. The tone owns its icon and tint so callers cannot hand-style a
/// one-off feedback capsule. Figma: `Toast` (`72:24`), property `Variant`.
public enum RemToastVariant: String, CaseIterable, Sendable {
    case info
    case success
    case warning
    case error

    fileprivate var symbol: String {
        switch self {
        case .info: "info.circle.fill"
        case .success: "checkmark.circle.fill"
        case .warning: "exclamationmark.triangle.fill"
        case .error: "xmark.circle.fill"
        }
    }

    fileprivate var tint: Color {
        switch self {
        case .info: DesignTokens.Color.systemBlue
        case .success: DesignTokens.Color.systemGreen
        case .warning: DesignTokens.Color.systemOrange
        case .error: DesignTokens.Color.systemRed
        }
    }
}

/// A brief, non-actionable status message that dismisses itself after four seconds by default.
///
/// Use a toast for transient feedback such as a saved result or a recoverable request failure. Do
/// not put buttons inside it or use it for information that must remain visible. The message is
/// announced as one accessibility element without moving focus.
public struct RemToast: View {
    public let variant: RemToastVariant
    public let message: String
    public let duration: Duration
    public let onDismiss: () -> Void

    @State private var isVisible = true

    public init(
        variant: RemToastVariant = .info,
        message: String,
        duration: Duration = .seconds(4),
        onDismiss: @escaping () -> Void = {}
    ) {
        self.variant = variant
        self.message = message
        self.duration = duration
        self.onDismiss = onDismiss
    }

    public var body: some View {
        Group {
            if isVisible {
                HStack(spacing: DesignTokens.Spacing.sm) {
                    Image(systemName: variant.symbol)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(variant.tint)
                    Text(message)
                        .font(DesignTokens.Typography.footnote)
                        .foregroundStyle(DesignTokens.Color.labelPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, DesignTokens.Spacing.md)
                .padding(.vertical, DesignTokens.Spacing.sm)
                .background(DesignTokens.Color.pillBackground, in: Capsule())
                .accessibilityElement(children: .combine)
                .accessibilityLabel(message)
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
        .task(id: message) {
            isVisible = true
            do {
                try await Task.sleep(for: duration)
            } catch {
                return
            }
            withAnimation(.easeOut(duration: 0.2)) { isVisible = false }
            onDismiss()
        }
    }
}

#if DEBUG
#Preview("Toast") {
    VStack(alignment: .leading, spacing: 12) {
        ForEach(RemToastVariant.allCases, id: \.self) { variant in
            RemToast(variant: variant, message: "Brief feedback message", duration: .seconds(60))
        }
    }
    .padding()
}
#endif
