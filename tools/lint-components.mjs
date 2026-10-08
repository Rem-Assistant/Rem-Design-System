#!/usr/bin/env node
// Component architecture contract lint — see docs/contracts/component-architecture.md
//
// Checks the deterministic parts of the contract across the SwiftUI component source set:
//   figma   — a matching parserless SwiftUI mapping OR an excluded, co-located archived native mapping
//   compose — a Compose twin <Name>.kt exists somewhere under compose/RemDesignSystem/
//   tokenset— cross-product components (TIER2 below) have a <Name>TokenSet.swift
//   hex     — no raw color literals in the view body (token-only)
//
// Baseline-ratcheted: tools/component-contract.baseline.json grandfathers today's known gaps so CI
// stays green; the lint FAILS only on violations NOT in the baseline. Run with --write-baseline to
// regenerate it (grandfathering the current state). The baseline is the backfill TODO.
//
// Usage: node tools/lint-components.mjs [--write-baseline]

import { readFileSync, writeFileSync, existsSync, readdirSync } from 'node:fs';
import { join, basename, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parserlessMappingMatches } from './component-code-connect.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const SW_ROOT = join(ROOT, 'Sources', 'RemDesignSystem');
const COMPOSE_ROOT = join(ROOT, 'compose', 'RemDesignSystem');
const BASELINE = join(ROOT, 'tools', 'component-contract.baseline.json');

// Component folders (compositions in Screens/ and Templates/ follow a different contract).
const COMPONENT_FOLDERS = ['Agenda', 'AgentSurfaces', 'Brand', 'Buttons', 'Chat', 'Primitives', 'Rows', 'Views'];
// Tier-2 cross-product components (≥4 variants on one axis, or ≥2 style axes) — must have a TokenSet.
const TIER2 = new Set(['RemButton', 'ContainedIcon', 'VoiceBar', 'TaskEventRow', 'ExecutionTrace']);

const writeMode = process.argv.includes('--write-baseline');

function walk(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = join(dir, e.name);
    if (e.isDirectory()) return e.name === 'src' ? [] : walk(p);
    return e.name.endsWith('.swift') || e.name.endsWith('.kt') ? [p] : [];
  });
}

// Component name from a view file: strip .swift and a trailing "Style" (RemButtonStyle -> RemButton).
function componentName(file) {
  return basename(file, '.swift').replace(/Style$/, '');
}

// All Compose component file basenames (without .kt), across every compose dir.
const composeNames = new Set(
  walk(COMPOSE_ROOT)
    .filter((p) => p.endsWith('.kt') && !p.includes('.figma.'))
    .map((p) => basename(p, '.kt'))
);

const pkg = readFileSync(join(ROOT, 'Package.swift'), 'utf8');

// The configured SwiftUI parserless template directory; Compose mappings cannot satisfy this guard.
function parserlessTemplates(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return parserlessTemplates(path);
    return entry.name.endsWith('.figma.ts') ? [readFileSync(path, 'utf8')] : [];
  });
}
const swiftTemplates = parserlessTemplates(join(ROOT, 'code-connect', 'swiftui'));


// Raw color literal — the token-only rule. Conservative, high-signal patterns only.
const HEX = /Color\(\s*red:|UIColor\(\s*red:|#[0-9A-Fa-f]{6}\b|Color\(0x[0-9A-Fa-f]{6}/;

const violations = [];
const add = (component, rule, detail) => violations.push({ key: `${component}:${rule}`, detail });

for (const folder of COMPONENT_FOLDERS) {
  const dir = join(SW_ROOT, folder);
  if (!existsSync(dir)) continue;
  for (const entry of readdirSync(dir)) {
    if (!entry.endsWith('.swift')) continue;
    if (entry.endsWith('TokenSet.swift') || entry.endsWith('.figma.swift') || entry.endsWith('.generated.swift')) continue;
    const file = join(dir, entry);
    const name = componentName(file);

    // Prefer the current parserless path. Exact source + identity + node URL must agree.
    // Archived native mappings still count, but any native file must stay excluded from SPM.
    const mapped = swiftTemplates.some((text) => parserlessMappingMatches(text, {
      source: `Sources/RemDesignSystem/${folder}/${entry}`, component: name,
    }));
    const figmaFile = join(dir, `${name}.figma.swift`);
    if (!mapped && !existsSync(figmaFile))
      add(name, 'figma', `missing matching parserless mapping or ${folder}/${name}.figma.swift`);
    if (existsSync(figmaFile) && !pkg.includes(`${folder}/${name}.figma.swift`))
      add(name, 'figma-exclude', `${name}.figma.swift not in Package.swift exclude:`);

    // compose twin anywhere under compose/RemDesignSystem/
    if (!composeNames.has(name)) add(name, 'compose', `no Compose twin ${name}.kt`);

    // cross-product -> TokenSet
    if (TIER2.has(name) && !existsSync(join(dir, `${name}TokenSet.swift`)))
      add(name, 'tokenset', `cross-product component missing ${name}TokenSet.swift`);

    // token-only (no raw hex in the body) — scan code only, not comments (DS docs often cite hex
    // values like "#3C3C43" in prose; those are not violations).
    const body = readFileSync(file, 'utf8');
    const codeOnly = body
      .replace(/\/\*[\s\S]*?\*\//g, '')                               // block comments
      .split('\n').filter((l) => !/^\s*(\/\/|\*)/.test(l)).join('\n'); // line / doc-comment / * lines
    if (HEX.test(codeOnly)) add(name, 'hex', `raw color literal in ${folder}/${entry}`);
  }
}

const currentKeys = violations.map((v) => v.key).sort();

if (writeMode) {
  writeFileSync(BASELINE, JSON.stringify({ grandfathered: currentKeys }, null, 2) + '\n');
  console.log(`Wrote baseline with ${currentKeys.length} grandfathered violation(s) -> ${BASELINE}`);
  process.exit(0);
}

const baseline = existsSync(BASELINE) ? new Set(JSON.parse(readFileSync(BASELINE, 'utf8')).grandfathered) : new Set();
const fresh = violations.filter((v) => !baseline.has(v.key));
const fixed = [...baseline].filter((k) => !currentKeys.includes(k));

if (fixed.length) {
  console.log(`✓ ${fixed.length} baseline item(s) now FIXED — remove from baseline:\n  ${fixed.join('\n  ')}\n`);
}
if (fresh.length) {
  console.error(`✗ Component contract: ${fresh.length} NEW violation(s) (not grandfathered):`);
  for (const v of fresh) console.error(`  • ${v.key} — ${v.detail}`);
  console.error(`\nFix them, or (only for a pre-existing gap) regenerate: node tools/lint-components.mjs --write-baseline`);
  console.error(`See docs/contracts/component-architecture.md`);
  process.exit(1);
}
console.log(`✓ Component contract: no new violations (${baseline.size} grandfathered, ${currentKeys.length} current).`);
