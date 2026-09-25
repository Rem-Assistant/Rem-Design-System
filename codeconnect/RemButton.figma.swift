// Code Connect — RemButton  (STUB — intentionally NOT in the SPM target, so it
// compiles only under the Figma Code Connect CLI, not `swift build`.)
//
// Figma node: Primitives `377:8`  (file af4yDqCzp57jds9lkFiIaO)
// Registry:  REGISTRY.md → "Button — Type · Tier model".
//
// To activate: add the Figma Code Connect Swift package + figma.config.json, then
// uncomment and publish with `figma connect publish`. Mapping intent below.
//
// import Figma
//
// struct RemButton_connection: FigmaConnect {
//     let component = RemButtonStyle.self
//     let figmaNodeUrl =
//         "https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO?node-id=377-8"
//
//     // Figma "Type" property  ↔  RemButtonStyle.Shape
//     @FigmaEnum("Type", mapping: [
//         "Rectangular": RemButtonStyle.Shape.rectangular,
//         "Pill":        RemButtonStyle.Shape.pill,
//         "Text":        RemButtonStyle.Shape.text,
//     ]) var shape: RemButtonStyle.Shape = .rectangular
//
//     // Figma "Tier" property  ↔  RemButtonStyle.Tier
//     @FigmaEnum("Tier", mapping: [
//         "Black":       RemButtonStyle.Tier.black,
//         "Blue":        RemButtonStyle.Tier.blue,
//         "Accent":      RemButtonStyle.Tier.accent,
//         "Destructive": RemButtonStyle.Tier.destructive,
//     ]) var tier: RemButtonStyle.Tier = .black
//
//     var body: some View {
//         Button("Label") {}
//             .buttonStyle(RemButtonStyle(shape: shape, tier: tier))
//     }
// }
