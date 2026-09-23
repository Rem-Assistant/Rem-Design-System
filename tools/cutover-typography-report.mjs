#!/usr/bin/env node
// READ-ONLY cutover helper for the app repo's Dynamic Type migration (SPEC item 2).
// Scans Swift sources for raw `Font.system(size:)` and maps each to the DesignTokens.Typography
// role it should become. Emits nothing but a report — it never edits code. Run it on the app
// checkout (default /home/user/RemClaw):
//
//   node tools/cutover-typography-report.mjs [path-to-app-repo]
//
// The point: turn "188 raw font sizes" into a confident, mechanical swap list, and flag the
// call sites that DON'T map cleanly (custom size/weight) for human judgement.

import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const APP = process.argv[2] || "/home/user/RemClaw";
const DIRS = ["Rem/Sources", "RemMac/Sources", "Shared"];

// size (+ weight/design) -> DesignTokens.Typography role
const ROLE = {
  "34": "largeTitle",
  "28": { regular: "title1", bold: "title1Bold" },
  "20": { regular: "title3", bold: "title3Bold" },
  "17": { regular: "body", bold: "bodyBold", mono: "chatCode" },
  "15": "subheadline",
  "13": "footnote",
  "12": { regular: "caption1", bold: "caption1Bold" },
};

function role(size, weight, mono) {
  const m = ROLE[size];
  if (!m) return null;
  if (typeof m === "string") return weight === "bold" ? null : m; // only regular has a role at this size
  if (mono && m.mono) return m.mono;
  if (weight === "bold" && m.bold) return m.bold;
  if ((!weight || weight === "regular") && m.regular) return m.regular;
  return null; // e.g. semibold/medium — no exact role
}

function* swiftFiles(dir) {
  let entries;
  try { entries = readdirSync(dir, { withFileTypes: true }); } catch { return; }
  for (const e of entries) {
    const p = join(dir, e.name);
    if (e.isDirectory()) yield* swiftFiles(p);
    else if (e.name.endsWith(".swift") && !e.name.includes("DesignTokens")) yield p;
  }
}

// Matches both `Font.system(size:…)` and the `.font(.system(size:…))` leading-dot shorthand.
const RX = /\bsystem\(size:\s*(\d+)(?:[^)]*?weight:\s*\.(\w+))?(?:[^)]*?design:\s*\.(\w+))?/g;
let mapped = 0, manual = 0;
const byRole = {}, manualExamples = [];

for (const dir of DIRS) {
  for (const file of swiftFiles(join(APP, dir))) {
    const src = readFileSync(file, "utf8");
    for (const m of src.matchAll(RX)) {
      const [, size, weight, design] = m;
      const r = role(size, weight, design === "monospaced");
      if (r) { mapped++; byRole[r] = (byRole[r] || 0) + 1; }
      else {
        manual++;
        if (manualExamples.length < 12)
          manualExamples.push(`${file.replace(APP + "/", "")}: size ${size}${weight ? " ." + weight : ""}${design ? " ." + design : ""}`);
      }
    }
  }
}

console.log(`\nDynamic Type migration report (app: ${APP})`);
console.log(`${"=".repeat(52)}`);
console.log(`Auto-mappable Font.system(size:) call sites: ${mapped}`);
console.log(`Needs manual judgement (no exact role):     ${manual}`);
console.log(`\nAuto-map by target token:`);
for (const [r, n] of Object.entries(byRole).sort((a, b) => b[1] - a[1]))
  console.log(`  DesignTokens.Typography.${r.padEnd(14)} ← ${n}`);
if (manualExamples.length) {
  console.log(`\nSample of manual cases (custom size/weight — decide per site):`);
  for (const e of manualExamples) console.log(`  ${e}`);
}
console.log(`\nNote: swap on a Mac + build; sizes already equal Apple defaults, so appearance holds.`);
