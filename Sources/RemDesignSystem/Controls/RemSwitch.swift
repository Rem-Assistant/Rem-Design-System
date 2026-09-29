import SwiftUI

/// The design system's on/off **Switch** — the canonical toggle used by grouped settings rows and
/// the onboarding Check-in cadence step. Thin and token-driven: it wraps the native SwiftUI `Toggle`
/// (so it inherits the platform's switch shape, motion, and accessibility) and pins the on-tint to
/// `Color.systemGreen`, the system switch color. Figma canonical: **RemSwitch** set (`868:210`);
/// the legacy iOS-on variant remains `110:50`.
///
/// Pure and state-driven, matching the design-system boundary (`RemButton` / `ContainedIcon`): it
/// renders the `isOn` it is handed and reports changes through `onChange`; it never owns the value.
/// `enabled` mirrors the host lifecycle (e.g. a row is non-interactive while its screen is saving).
public struct RemSwitch: View {
    private let isOn: Bool
    private let enabled: Bool
    private let onChange: (Bool) -> Void

    public init(isOn: Bool, enabled: Bool = true, onChange: @escaping (Bool) -> Void) {
        self.isOn = isOn
        self.enabled = enabled
        self.onChange = onChange
    }

    public var body: some View {
        Toggle("", isOn: Binding(get: { isOn }, set: { onChange($0) }))
            .labelsHidden()
            .tint(DesignTokens.Color.systemGreen)
            .disabled(!enabled)
    }
}

#if DEBUG
#Preview("RemSwitch") {
    VStack(alignment: .leading, spacing: 16) {
        RemSwitch(isOn: true, onChange: { _ in })
        RemSwitch(isOn: false, onChange: { _ in })
        RemSwitch(isOn: true, enabled: false, onChange: { _ in })
    }
    .padding()
}
#endif
