#!/usr/bin/env node
// Scaffold a contract-compliant component — see docs/contracts/component-architecture.md
//
// Usage: node tools/scaffold-component.mjs <Folder>/<Name> [--cross-product]
//   e.g. node tools/scaffold-component.mjs Primitives/RemBadge
//        node tools/scaffold-component.mjs Rows/StatRow --cross-product
//
// Writes the SwiftUI view (+ <Name>TokenSet.swift when --cross-product), the <Name>.figma.swift
// Code Connect stub, and the Compose twin, then prints the manual wiring lines to add. Never
// overwrites an existing file.

import { writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const arg = process.argv[2];
const crossProduct = process.argv.includes('--cross-product');
if (!arg || !arg.includes('/')) {
  console.error('Usage: node tools/scaffold-component.mjs <Folder>/<Name> [--cross-product]');
  process.exit(1);
}
const [folder, name] = arg.split('/');
const composeFolder = folder.toLowerCase();

function write(path, contents) {
  const full = join(ROOT, path);
  if (existsSync(full)) { console.log(`• skip (exists): ${path}`); return; }
  mkdirSync(dirname(full), { recursive: true });
  writeFileSync(full, contents);
  console.log(`• wrote: ${path}`);
}

const swiftView = `import SwiftUI

/// **${name}** — <one-line purpose>. Token-only values (DesignTokens); renders a state, not wired controls.
/// Figma: <node-id>. Compose twin: \`${composeFolder}/${name}.kt\`.
public struct ${name}: View {
    public init() {}
    public var body: some View {
        ${crossProduct ? `let tokens = ${name}TokenSet()\n        return ` : ''}Text("${name}")
            .font(DesignTokens.Typography.body)
            .foregroundStyle(DesignTokens.Color.labelPrimary)
    }
}

#if DEBUG
#Preview("${name}") { ${name}() }
#endif
`;

const swiftTokenSet = `import SwiftUI

/// Resolves every styleable value for ${name} from \`DesignTokens\` — the only place its per-variant /
/// per-state values live (the Fluent \`ButtonTokenSet\` pattern; see RemButtonTokenSet). The view holds
/// no values, only selection + layout.
struct ${name}TokenSet {
    let foreground: Color = DesignTokens.Color.labelPrimary
    let background: Color = DesignTokens.Color.backgroundSecondary
    // Add the axes (variant/size/state) and resolve each value here.
}
`;

const swiftFigma = `import Figma
import SwiftUI

// Code Connect for ${name} — binds the Figma node to the real Swift type. Excluded from the SPM
// target (add to Package.swift exclude:). Validated by \`figma connect check\` in CI.
struct ${name}_connection: FigmaConnect {
    let component = ${name}.self
    let figmaNodeUrl = "https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem-Design-System?node-id=TODO"

    var body: some View {
        ${name}()
    }
}
`;

const composeTwin = `package com.rem.designsystem.${composeFolder}

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rem.designsystem.tokens.RemColors
import com.rem.designsystem.tokens.RemTheme
import com.rem.designsystem.tokens.RemTypography

/** ${name} — Compose twin of Sources/RemDesignSystem/${folder}/${name}.swift. Token-only values. */
@Composable
fun ${name}(modifier: Modifier = Modifier) {
    Text("${name}", style = RemTypography.body, color = RemColors.current.labelPrimary, modifier = modifier)
}
`;

write(`Sources/RemDesignSystem/${folder}/${name}.swift`, swiftView);
if (crossProduct) write(`Sources/RemDesignSystem/${folder}/${name}TokenSet.swift`, swiftTokenSet);
write(`Sources/RemDesignSystem/${folder}/${name}.figma.swift`, swiftFigma);
write(`compose/RemDesignSystem/${composeFolder}/${name}.kt`, composeTwin);

console.log(`\nNow wire it up (the lint checks these):`);
console.log(`  1. Package.swift   → add "${folder}/${name}.figma.swift" to the RemDesignSystem target exclude:`);
console.log(`  2. build.gradle.kts → ensure from("${composeFolder}") is in gatherDesignSystemSources (if a new folder)`);
console.log(`  3. REGISTRY.md     → add a row mapping the Figma node ↔ code`);
console.log(`  4. Render harness  → RenderSnapshots.swift + EvidenceSnapshots.kt entries`);
if (crossProduct) console.log(`  5. Add ${name} to the TIER2 set in tools/lint-components.mjs`);
console.log(`  • Fill the figmaNodeUrl node-id in ${name}.figma.swift`);
console.log(`  • Verify: node tools/lint-components.mjs`);
