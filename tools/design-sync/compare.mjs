#!/usr/bin/env node
// #2 drift-check, comparison: perceptual-diff the SwiftUI snapshots against the Figma
// exports. Same basename in both dirs = a matched pair. Non-blank diffs write a *.diff.png
// and fail the process (→ CI red) so design/code divergence is caught automatically —
// e.g. the "leading badge gap" and "title clipping" bugs would have tripped this.
//
//   node tools/design-sync/compare.mjs <swiftuiDir> <figmaDir> [--threshold=0.1]
//     [--maxDiffRatio=0.02] [--require=NameA,NameB] [--exclusive-prefix=Family-]
//
// Deps: pixelmatch, pngjs  (npm i -D pixelmatch pngjs)
import { readdirSync, readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { join, basename } from 'node:path';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';

const [swiftDir, figmaDir] = process.argv.slice(2).filter((a) => !a.startsWith('--'));
const opt = Object.fromEntries(process.argv.slice(2).filter((a) => a.startsWith('--')).map((a) => a.slice(2).split('=')));
const threshold = Number(opt.threshold ?? 0.1);        // per-pixel color tolerance
const maxDiffRatio = Number(opt.maxDiffRatio ?? 0.02); // allow 2% differing pixels (font hinting etc.)
const required = (opt.require ?? '').split(',').map((name) => name.trim()).filter(Boolean);
const requiredSet = new Set(required);
const exclusivePrefixes = (opt['exclusive-prefix'] ?? '').split(',').map((prefix) => prefix.trim()).filter(Boolean);
if (exclusivePrefixes.length > 0 && required.length === 0) {
  console.error('--exclusive-prefix requires an explicit --require set');
  process.exit(2);
}
if (!swiftDir || !figmaDir) { console.error('usage: compare.mjs <swiftuiDir> <figmaDir>'); process.exit(2); }

const outDir = 'artifacts/diff';
mkdirSync(outDir, { recursive: true });

function loadResized(path, w, h) {
  const png = PNG.sync.read(readFileSync(path));
  if (png.width === w && png.height === h) return png;
  // nearest-neighbour resize so mismatched export scales still compare
  const out = new PNG({ width: w, height: h });
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const sx = Math.floor((x / w) * png.width), sy = Math.floor((y / h) * png.height);
    const si = (png.width * sy + sx) << 2, di = (w * y + x) << 2;
    out.data[di] = png.data[si]; out.data[di+1] = png.data[si+1]; out.data[di+2] = png.data[si+2]; out.data[di+3] = png.data[si+3];
  }
  return out;
}

let failed = 0, checked = 0, skipped = 0, errored = 0;
const results = [];
let figmaNames = new Set();
let swiftNames = new Set();
// A fatal (usually a corrupt/unreadable PNG or a missing dir) must NOT skip the report: the
// workflow gates the summary/artifact upload on the report existing, so a crash here would
// silently erase the evidence trail for a screen-delivery gate. We therefore ALWAYS emit the
// report (see the finally-style write below), tagging the run `error` so CI reads a missing
// comparison as unambiguous rather than "clean".
let fatal = null;
let figmaFiles = [];
let swiftFiles = [];
try {
  figmaFiles = readdirSync(figmaDir).filter((f) => f.endsWith('.png'));
} catch (err) {
  fatal = `Figma evidence directory: ${err?.message ?? err}`;
  console.error(`✗ fatal: ${fatal}`);
}
try {
  swiftFiles = readdirSync(swiftDir).filter((f) => f.endsWith('.png'));
} catch (err) {
  const swiftFatal = `SwiftUI evidence directory: ${err?.message ?? err}`;
  fatal = fatal ? `${fatal}; ${swiftFatal}` : swiftFatal;
  console.error(`✗ fatal: ${swiftFatal}`);
}
figmaNames = new Set(figmaFiles.map((f) => basename(f, '.png')));
swiftNames = new Set(swiftFiles.map((f) => basename(f, '.png')));

if (fatal === null) {
  try {
    for (const f of figmaFiles) {
      const name = basename(f, '.png');
      const swiftPath = join(swiftDir, f);
      if (!existsSync(swiftPath)) {
        if (!requiredSet.has(name)) {
          console.warn(`⚠ no SwiftUI snapshot for optional ${name} — skipping`);
          skipped++;
        }
        continue;
      }
      try {
        const a = PNG.sync.read(readFileSync(swiftPath));
        const b = loadResized(join(figmaDir, f), a.width, a.height);
        const diff = new PNG({ width: a.width, height: a.height });
        const px = pixelmatch(a.data, b.data, diff.data, a.width, a.height, { threshold });
        const ratio = px / (a.width * a.height);
        checked++;
        const passed = ratio <= maxDiffRatio;
        results.push({ name, diffRatio: ratio, passed });
        if (ratio > maxDiffRatio) {
          failed++;
          writeFileSync(join(outDir, `${name}.diff.png`), PNG.sync.write(diff));
          console.log(`✗ ${name}: ${(ratio * 100).toFixed(2)}% differ (> ${(maxDiffRatio*100)}%) → ${outDir}/${name}.diff.png`);
        } else {
          console.log(`✓ ${name}: ${(ratio * 100).toFixed(2)}% differ`);
        }
      } catch (err) {
        // One bad pair is a comparison failure for that state, not a reason to lose the whole report.
        errored++;
        results.push({ name, diffRatio: null, passed: false, error: String(err?.message ?? err) });
        console.error(`✗ ${name}: comparison error — ${err?.message ?? err}`);
      }
    }
  } catch (err) {
    fatal = String(err?.message ?? err);
    console.error(`✗ fatal: ${fatal}`);
  }
}
const compared = new Set(results.filter((result) => result.diffRatio !== null).map((result) => result.name));
const missingRequired = required.filter((name) => !compared.has(name));
const hardErrors = missingRequired.map((name) => {
  const missing = [];
  if (!figmaNames.has(name)) missing.push('figma');
  if (!swiftNames.has(name)) missing.push('swiftui');
  if (missing.length === 0) missing.push('comparable-pair');
  const message = `Required evidence ${name} is missing ${missing.join(' + ')}`;
  const existing = results.find((result) => result.name === name);
  if (existing) {
    Object.assign(existing, {
      status: 'error',
      passed: false,
      errorCode: 'missing-required-evidence',
      missing,
      hardError: true,
    });
  } else {
    results.push({
      name,
      status: 'error',
      diffRatio: null,
      passed: false,
      errorCode: 'missing-required-evidence',
      missing,
      hardError: true,
      error: message,
    });
  }
  console.error(`✗ HARD ERROR: ${message}`);
  return { code: 'missing-required-evidence', name, missing, message };
});
const unexpectedExclusive = [...new Set([...figmaNames, ...swiftNames])]
  .filter((name) => exclusivePrefixes.some((prefix) => name.startsWith(prefix)) && !requiredSet.has(name))
  .sort();
for (const name of unexpectedExclusive) {
  const present = [];
  if (figmaNames.has(name)) present.push('figma');
  if (swiftNames.has(name)) present.push('swiftui');
  const message = `Unexpected exclusive-family evidence ${name} is present in ${present.join(' + ')}`;
  hardErrors.push({ code: 'unexpected-exclusive-evidence', name, present, message });
  results.push({
    name,
    status: 'error',
    diffRatio: null,
    passed: false,
    errorCode: 'unexpected-exclusive-evidence',
    present,
    hardError: true,
    error: message,
  });
  console.error(`✗ HARD ERROR: ${message}`);
}

const report = {
  version: 1,
  status: fatal !== null || hardErrors.length > 0 ? 'error' : 'completed',
  head: process.env.GITHUB_SHA ?? null,
  threshold,
  maxDiffRatio,
  required,
  missingRequired,
  exclusivePrefixes,
  unexpectedExclusive,
  hardErrors,
  checked,
  failed,
  skipped,
  errored,
  fatal,
  results: results.sort((a, b) => a.name.localeCompare(b.name)),
};
writeFileSync('artifacts/design-drift-report.json', `${JSON.stringify(report, null, 2)}\n`);

console.log(`\n${checked} checked · ${failed} drifted · ${skipped} optional unmatched · ${errored} comparison errors · ${hardErrors.length} hard errors`);
if (checked === 0) console.error('✗ No registered Figma/code pairs were compared.');
process.exit(fatal !== null || failed > 0 || errored > 0 || checked === 0 || hardErrors.length > 0 ? 1 : 0);
