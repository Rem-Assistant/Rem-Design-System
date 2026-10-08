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
