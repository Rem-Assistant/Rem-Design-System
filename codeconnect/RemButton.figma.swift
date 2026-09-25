// Code Connect — RemButton  (STUB — intentionally NOT in the SPM target, so it
// compiles only under the Figma Code Connect CLI, not `swift build`.)
//
// Figma node: Button `377:8`  (file af4yDqCzp57jds9lkFiIaO)
// The Figma component has ONE variant property `Style` with 7 values; the code now
// mirrors it 1:1 via `RemButtonVariant`, so this binds cleanly (no axis mismatch).
//
// To activate: add the Figma Code Connect Swift package + figma.config.json, then
// uncomment and publish with `figma connect publish`.
//
// import Figma
//
// struct RemButton_connection: FigmaConnect {
//     let component = RemButtonStyle.self
//     let figmaNodeUrl =
//         "https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=377-8"
//
//     // Figma `Style` property  ↔  RemButtonVariant  (one-to-one)
//     @FigmaEnum("Style", mapping: [
//         "Rect · Black":       RemButtonVariant.rectBlack,
//         "Rect · Blue":        RemButtonVariant.rectBlue,
//         "Rect · Secondary":   RemButtonVariant.rectSecondary,
//         "Rect · Destructive": RemButtonVariant.rectDestructive,
//         "Text · Accent":      RemButtonVariant.textAccent,
//         "Text · Destructive": RemButtonVariant.textDestructive,
//         "Pill · Secondary":   RemButtonVariant.pillSecondary,
//     ]) var variant: RemButtonVariant = .rectBlack
//
//     var body: some View {
//         Button("Label") {}
//             .remButton(variant)
//     }
// }
