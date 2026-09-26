import SwiftUI
import AppKit
import RemDesignSystem

// Renders the design system's SwiftUI components to PNGs (light + dark) so the built output is
// visible as screenshot evidence — no simulator, no running app. Uses SwiftUI `ImageRenderer`
// on a macOS runner. Output directory is arg 1 (default: artifacts/swiftui).
//
// Each gallery mirrors the component's own `#Preview` so the evidence matches what a developer
// sees in Xcode canvas. Dark mode is driven two ways at once: `\.colorScheme` for SwiftUI's own
// semantic colors, and the current drawing NSAppearance for the AppKit-backed dynamic tokens
// (DesignTokens resolves to NSColor on macOS — see DesignTokens.generated.swift).

@MainActor
private struct Gallery {
    let name: String
    let width: CGFloat
    let height: CGFloat?   // nil = intrinsic height
    let view: AnyView
}

// The 7 Figma `Style` variants, by intent name (matches RemButton.figma.swift). A named type,
// not a tuple, because SwiftUI `ForEach` needs an `Identifiable`/`id:` and Swift has no key paths
// to tuple elements.
private struct ButtonVariantRow: Identifiable {
    let id: String
    let variant: RemButtonVariant
}

private let buttonVariants: [ButtonVariantRow] = [
    .init(id: "Rect · Black", variant: .rectBlack),
    .init(id: "Rect · Blue", variant: .rectBlue),
    .init(id: "Rect · Secondary", variant: .rectSecondary),
    .init(id: "Rect · Destructive", variant: .rectDestructive),
    .init(id: "Text · Accent", variant: .textAccent),
    .init(id: "Text · Destructive", variant: .textDestructive),
    .init(id: "Pill · Secondary", variant: .pillSecondary),
]

@MainActor
private func chevron() -> some View {
    Image(systemName: "chevron.right")
        .font(.system(size: 14, weight: .semibold))
        .foregroundStyle(DesignTokens.Color.labelTertiary)
}

// The onboarding consent SCREEN, composed WITHOUT the shipping template's `ScrollView`.
// SwiftUI's `ImageRenderer` cannot rasterize `ScrollView`/`List` content (it captures an empty
// viewport), so the screen is re-composed scroll-free from the SAME real design-system components
// (`ContainedIcon`, `ListRow`, `RemButton`) for the screenshot. Only the screen scaffold is
// re-created here; the atomic components are the shipped ones. Mirrors `OnboardingConsentTemplate`.
@MainActor
private func consentScreen() -> some View {
    let message = "Rem uses your data to answer requests and run approved actions through your personal cloud gateway. You can review or delete your account data in Settings."
    let footnote = "By tapping \u{201C}Accept and Continue,\u{201D} you agree to our Terms of Service and Privacy Policy."
    return VStack(spacing: DesignTokens.Spacing.lg) {
        Spacer().frame(height: DesignTokens.Spacing.xxl)
        VStack(spacing: DesignTokens.Spacing.md) {
            ContainedIcon("lock.shield.fill", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
            VStack(spacing: DesignTokens.Spacing.sm) {
                Text("Privacy by design")
                    .font(DesignTokens.Typography.title1.weight(.semibold))
                    .foregroundStyle(DesignTokens.Color.labelPrimary)
                Text(message)
                    .font(DesignTokens.Typography.body)
                    .foregroundStyle(DesignTokens.Color.labelSecondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        VStack(spacing: 0) {
            ListRow("Terms of Service",
                    subtitle: "How Rem accounts, subscriptions, and approved actions work.",
                    leading: { ContainedIcon("doc.text", fill: .subtle) },
                    trailing: { chevron() })
            Divider().padding(.leading, 60)
            ListRow("Privacy Policy",
                    subtitle: "What Rem, your gateway, and AI or voice providers process.",
                    leading: { ContainedIcon("shield", fill: .subtle) },
                    trailing: { chevron() })
        }
        .background(DesignTokens.Color.backgroundSecondary)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xlarge, style: .continuous))
        Spacer()
        VStack(spacing: DesignTokens.Spacing.md) {
            Button("Accept and Continue") {}.remPrimaryActionButton()
            Text(footnote)
                .font(DesignTokens.Typography.caption1)
                .foregroundStyle(DesignTokens.Color.labelSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
    .padding(DesignTokens.Spacing.lg)
}

@MainActor
private func galleries() -> [Gallery] {
    let buttons = VStack(spacing: 14) {
        ForEach(buttonVariants) { row in
            Button(row.id) {}.remButton(row.variant)
        }
        Button("Disabled") {}.remButton(.rectBlack).disabled(true)
    }

    let icons = HStack(spacing: 16) {
        ContainedIcon("lock.shield.fill", fill: .tint(DesignTokens.Color.brandBlue), size: .large)
        ContainedIcon("doc.text", fill: .subtle)
        ContainedIcon("shield", fill: .subtle)
    }

    let rows = VStack(spacing: 0) {
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

    return [
        Gallery(name: "RemButton", width: 300, height: nil, view: AnyView(buttons)),
        Gallery(name: "ContainedIcon", width: 260, height: nil, view: AnyView(icons)),
        Gallery(name: "ListRow", width: 380, height: nil, view: AnyView(rows)),
        Gallery(name: "Consent", width: 402, height: 874, view: AnyView(consentScreen())),
    ]
}

@main
enum RenderGallery {
    @MainActor
    static func main() {
        _ = NSApplication.shared  // some AppKit-backed rendering wants a shared app instance
        let outDir = CommandLine.arguments.dropFirst().first ?? "artifacts/swiftui"
        try? FileManager.default.createDirectory(atPath: outDir, withIntermediateDirectories: true)

        var failures = 0
        var successes = 0
        for gallery in galleries() {
            for dark in [false, true] {
                if render(gallery, dark: dark, outDir: outDir) { successes += 1 } else { failures += 1 }
            }
        }
        if failures > 0 {
            FileHandle.standardError.write(Data("render: \(failures) image(s) failed, \(successes) succeeded\n".utf8))
        }
        // Only fail the job if NOTHING rendered — a partial failure still uploads the images that
        // did render, rather than discarding the whole platform's output.
        if successes == 0 { exit(1) }
    }

    @MainActor
    private static func render(_ gallery: Gallery, dark: Bool, outDir: String) -> Bool {
        let content = gallery.view
            .frame(width: gallery.width, height: gallery.height)
            .padding(24)
            .background(DesignTokens.Color.backgroundPrimary)
            .environment(\.colorScheme, dark ? .dark : .light)

        let renderer = ImageRenderer(content: content)
        renderer.scale = 2

        var nsImage: NSImage?
        if let appearance = NSAppearance(named: dark ? .darkAqua : .aqua) {
            appearance.performAsCurrentDrawingAppearance { nsImage = renderer.nsImage }
        } else {
            nsImage = renderer.nsImage
        }

        let suffix = dark ? "dark" : "light"
        guard let image = nsImage,
              let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff),
              let png = rep.representation(using: .png, properties: [:]) else {
            FileHandle.standardError.write(Data("failed: \(gallery.name)-\(suffix)\n".utf8))
            return false
        }
        let path = "\(outDir)/\(gallery.name)-\(suffix).png"
        do {
            try png.write(to: URL(fileURLWithPath: path))
            print("wrote \(path)")
            return true
        } catch {
            FileHandle.standardError.write(Data("write failed: \(path): \(error)\n".utf8))
            return false
        }
    }
}
