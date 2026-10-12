import SwiftUI

/// **RemSidebarContainer** — the Claude-style push: closed, the chat fills the screen
/// (`2702:2351`); open, the chat slides right as a rounded, shadowed card leaving
/// `RemSidebarLayout.visibleContentPeek` visible, and tapping that card closes the sidebar
/// (`2702:2479`).
///
/// `isOpen` is owned by the host (the chat header's sidebar button sets it). The container owns
/// only geometry and the tap-to-close affordance. Compose twin: `navigation/RemSidebarContainer.kt`.
///
/// **Closed means not rendered.** `.accessibilityHidden(true)` did not remove the off-screen
/// sidebar's controls from the XCUITest tree (runs 38177680171, 38176095096), so a screen reader
/// could reach controls behind the chat. The sidebar is therefore inserted synchronously when
/// `isOpen` becomes true (before the slide starts) and removed when the close slide has
/// logically completed; under Reduce Motion there is no slide and it is removed at once. A reopen
/// before the close completes keeps it (each close carries a generation; a stale completion, or
/// one that finds the sidebar open again, does nothing). While closing it is already hidden from
/// accessibility and takes no input. Closing discards the sidebar's own view state (its list's
/// scroll position resets to the top); everything the host owns (agents, selection) is unaffected.
public struct RemSidebarContainer<Sidebar: View, Content: View>: View {
    @Binding private var isOpen: Bool
    private let sidebar: () -> Sidebar
    private let content: () -> Content

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    /// Whether the sidebar subtree is in the hierarchy at all.
    @State private var showsSidebar: Bool
    /// The geometry's open state; follows `isOpen` inside the container's own animation.
    @State private var presentedOpen: Bool
    /// Bumped on every open and close, so only the latest close's completion removes the sidebar.
    @State private var transitionGeneration = 0

    private static var slide: Animation { .snappy(duration: 0.3) }

    public init(
        isOpen: Binding<Bool>,
        @ViewBuilder sidebar: @escaping () -> Sidebar,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self._isOpen = isOpen
        self.sidebar = sidebar
        self.content = content
        self._showsSidebar = State(initialValue: isOpen.wrappedValue)
        self._presentedOpen = State(initialValue: isOpen.wrappedValue)
    }

    public var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            let sidebarWidth = RemSidebarLayout.sidebarWidth(containerWidth: width)
            let offset = RemSidebarLayout.contentOffset(isOpen: presentedOpen, containerWidth: width)
            let radius: CGFloat = presentedOpen ? RemSidebarLayout.contentCornerRadius : 0

            ZStack(alignment: .leading) {
                DesignTokens.Color.backgroundSecondary.ignoresSafeArea()

                if showsSidebar {
                    sidebar()
                        .frame(width: sidebarWidth)
                        .frame(maxHeight: .infinity)
                        .allowsHitTesting(isOpen)
                        .accessibilityHidden(!isOpen)
                }

                content()
                    .frame(width: width)
                    .frame(maxHeight: .infinity)
                    .allowsHitTesting(!isOpen)
                    .accessibilityHidden(isOpen)
                    .background(DesignTokens.Color.backgroundPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
                    .shadow(color: .black.opacity(presentedOpen ? 0.12 : 0), radius: 16, y: 8)
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
        }
        .onChange(of: isOpen) { _, open in
            self.transition(toOpen: open)
        }
    }

    private func transition(toOpen open: Bool) {
        transitionGeneration += 1
        let generation = transitionGeneration
        if open {
            // Insert first, without animation, so the sidebar is already behind the card as it slides.
            showsSidebar = true
            if reduceMotion {
                presentedOpen = true
            } else {
                withAnimation(Self.slide) { presentedOpen = true }
            }
            return
        }
        if reduceMotion {
            presentedOpen = false
            showsSidebar = false
            return
        }
        withAnimation(Self.slide, completionCriteria: .logicallyComplete) {
            presentedOpen = false
        } completion: {
            guard generation == self.transitionGeneration, !self.isOpen else { return }
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { self.showsSidebar = false }
        }
    }
}
