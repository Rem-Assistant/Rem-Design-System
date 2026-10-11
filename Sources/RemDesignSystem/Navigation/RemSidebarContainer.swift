import SwiftUI

/// **RemSidebarContainer** — the Claude-style push: closed, the chat fills the screen
/// (`2702:2351`); open, the chat slides right as a rounded, shadowed card leaving
/// `RemSidebarLayout.visibleContentPeek` visible, and tapping that card closes the sidebar
/// (`2702:2479`).
///
/// `isOpen` is owned by the host (the chat header's sidebar button sets it). The container owns
/// only geometry and the tap-to-close affordance. Compose twin: `navigation/RemSidebarContainer.kt`.
public struct RemSidebarContainer<Sidebar: View, Content: View>: View {
    @Binding private var isOpen: Bool
    private let sidebar: () -> Sidebar
    private let content: () -> Content

    public init(
        isOpen: Binding<Bool>,
        @ViewBuilder sidebar: @escaping () -> Sidebar,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self._isOpen = isOpen
        self.sidebar = sidebar
        self.content = content
    }

    public var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            let sidebarWidth = RemSidebarLayout.sidebarWidth(containerWidth: width)
            let offset = RemSidebarLayout.contentOffset(isOpen: isOpen, containerWidth: width)
            let radius: CGFloat = isOpen ? RemSidebarLayout.contentCornerRadius : 0

            ZStack(alignment: .leading) {
                DesignTokens.Color.backgroundSecondary.ignoresSafeArea()

                sidebar()
                    .frame(width: sidebarWidth)
                    .frame(maxHeight: .infinity)
                    .accessibilityHidden(!isOpen)

                content()
                    .frame(width: width)
                    .frame(maxHeight: .infinity)
                    .allowsHitTesting(!isOpen)
                    .accessibilityHidden(isOpen)
                    .background(DesignTokens.Color.backgroundPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
                    .shadow(color: .black.opacity(isOpen ? 0.12 : 0), radius: 16, y: 8)
                    .overlay {
                        if isOpen {
                            Color.clear
                                .contentShape(Rectangle())
                                .onTapGesture { isOpen = false }
                                .accessibilityElement()
                                .accessibilityLabel("Close sidebar")
                                .accessibilityAddTraits(.isButton)
                                .accessibilityAction { isOpen = false }
                                .accessibilityIdentifier("sidebar.close")
                        }
                    }
                    .offset(x: offset)
            }
            .animation(.snappy(duration: 0.3), value: isOpen)
        }
    }
}
