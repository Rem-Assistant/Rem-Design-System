#!/usr/bin/env node
// #2 drift-check, comparison: perceptual-diff the SwiftUI snapshots against the Figma
// exports. Same basename in both dirs = a matched pair. Non-blank diffs write a *.diff.png
// and fail the process (→ CI red) so design/code divergence is caught automatically —
// e.g. the "leading badge gap" and "title clipping" bugs would have tripped this.
//
//   node tools/design-sync/compare.mjs <swiftuiDir> <figmaDir> [--threshold=0.1] [--maxDiffRatio=0.02]
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

let failed = 0, checked = 0, skipped = 0;
for (const f of readdirSync(figmaDir).filter((f) => f.endsWith('.png'))) {
  const name = basename(f, '.png');
  const swiftPath = join(swiftDir, f);
  if (!existsSync(swiftPath)) { console.warn(`⚠ no SwiftUI snapshot for ${name} — skipping`); skipped++; continue; }
  const a = PNG.sync.read(readFileSync(swiftPath));
  const b = loadResized(join(figmaDir, f), a.width, a.height);
  const diff = new PNG({ width: a.width, height: a.height });
  const px = pixelmatch(a.data, b.data, diff.data, a.width, a.height, { threshold });
  const ratio = px / (a.width * a.height);
  checked++;
  if (ratio > maxDiffRatio) {
    failed++;
    writeFileSync(join(outDir, `${name}.diff.png`), PNG.sync.write(diff));
    console.log(`✗ ${name}: ${(ratio * 100).toFixed(2)}% differ (> ${(maxDiffRatio*100)}%) → ${outDir}/${name}.diff.png`);
  } else {
    console.log(`✓ ${name}: ${(ratio * 100).toFixed(2)}% differ`);
  }
}
console.log(`\n${checked} checked · ${failed} drifted · ${skipped} unmatched`);
process.exit(failed > 0 ? 1 : 0);
