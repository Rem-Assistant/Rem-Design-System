# Settings foundation API

Issue #72, approved Figma sources 1964:86819 and 1827:50855; user amendments in settings-foundation.md.

- iOS SettingsEntryContent and AgentSettingsContent own native Lists/Sections. Do not wrap either in ScrollView. Use SettingsEntryContent(agentDestination: { AgentPreview(...) }) for native navigation. Legacy openAgent action initializer remains available. Both accept optional onShare; absent action stays a static reference.
- AgentSettingsDestination is a stable seven-case route contract (Swift lowerCamelCase, Kotlin PascalCase): pairedDevices, connectors, cloudBrowser, memory, models, wallet, voice. Automations deliberately has no route. Available route sets default empty. iOS uses NavigationLink(value:) with host navigationDestination registration; Compose uses openDestination callback. Supply availability only after a real local destination exists.
- SettingsRowLabel composes ListRow, explicit Subtle/settings-size ContainedIcon and ListRowLabel. Swift label is content only; navigation/actions belong to the host. Compose owns its click action and single optional disclosure, since it has no List navigation chrome. A missing action gets no disclosure. Root non-Agent references remain static.
- Swift ListRow content initializer adds layout: .nativeList; native containers then own insets, separators and disclosure. Default standalone layout and all earlier initializers are unchanged. RemSection remains the custom-scroll adapter. Compose retains RemSection and content-slot ListRow.
- ListRowLabel supports title, optional subtitle, fill/hug title layout and titleAccessory slot on both platforms. Text uses semantic typography and wraps with Dynamic Type/font scaling.
- ContainedIcon adds an optional glyph size override (Swift glyphPointSize, Compose glyphSize), preserving old defaults. Current Figma Small means29/radius7, code settings; Figma Large means64/radius18. Primitive glyphs are15/30, while approved Settings instances use17. Legacy code small remains38.
- Share has only an optional action callback in this foundation. Root integration owns the system activity sheet/Android Sharesheet and the equivalent payload contract.

## Verification status

Swift syntax parse passed locally. Local simulator/emulator/build work is intentionally deferred due to internal disk headroom below1GB. Hosted compilation, interaction tests and rendered review remain required; syntax parsing is not compilation proof.

Actual playground UI tests preserve success/back, error/retry/cancel, delayed cancellation, and name draft Save/Cancel. Added real-host light/dark/large-text captures (SettingsEntry-light, AgentSettings-light, corresponding -dark and -large-text), large-text scrolling, and Automations no-action checks. No legacy SettingsScreen capture is claimed as proof for these screens.

## Local Code Connect contract

Templates use verified component IDs and exact property names: ContainedIcon614:8 (Style Tinted/Subtle, Size Small/Large, Symbol TEXT), ListRow101:18 (booleans and three instance swaps), ListRowLabel1966:60442 (title/subtitle, visibility, fill/hug and title accessory slot), Section1307:667 (style, header/footer visibility and Rows slot), SectionHeader161:68 (Header, Show trailing action, Trailing action slot), SectionFooter161:70 (Footer).

Nested header/footer and row instances execute their own templates. Native and standalone Swift row/section templates are separate: native rows remove the canonical Chevron child, use layout nativeList and let the host NavigationLink supply disclosure; standalone rows preserve the swapped accessory. The owning native List applies the selected section style. Custom-scroll RemSection now also accepts header/footer view slots while preserving its string initializers; Compose has an equivalent additive overload.

Symbol TEXT values resolve through the12 identities verified using Figma's SF-symbol utility and the existing Android semantic registry. Unsupported glyphs produce a mapping-gap comment, never an invented fallback or a raw glyph passed to systemName. Tinted maps the component default systemBlue; per-instance color overrides are not exposed as a Figma property and require explicit review. Settings itself always passes Subtle. These templates are local and unpublished: Code Connect endpoints remain plan-blocked.

Local npm run check-code-connect passed (TypeScript, SwiftUI parser, Compose parser). Compilation, hosted UI execution, screenshots and visual approval remain pending; parser success does not prove snippet rendering or platform fidelity.
