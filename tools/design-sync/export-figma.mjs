#!/usr/bin/env node
// #2 drift-check, Figma side: render every manifest component to a PNG (the design
// "expected"). The SwiftUI side (render each component's preview to a PNG of the same
// name) runs in Xcode on a macOS CI runner — see README + snapshots/RemDesignSnapshots.swift.
// compare.mjs then perceptual-diffs the two folders and fails on drift.
//
//   FIGMA_TOKEN=... node tools/design-sync/export-figma.mjs [outDir=artifacts/figma]
//
import { mkdir, writeFile } from 'node:fs/promises';
import { manifest, figma, requireToken } from './lib.mjs';

requireToken();
const outDir = process.argv[2] || 'artifacts/figma';
await mkdir(outDir, { recursive: true });

const items = [...manifest.components, ...manifest.screens];
const ids = items.map((i) => i.node);
// Figma image export: batch the ids in one call.
const { images } = await figma(`/images/${manifest.figmaFileKey}?ids=${encodeURIComponent(ids.join(','))}&format=png&scale=2`);

let n = 0;
for (const item of items) {
  const url = images[item.node];
  if (!url) { console.warn(`no render for ${item.name} (${item.node})`); continue; }
  const bytes = Buffer.from(await (await fetch(url)).arrayBuffer());
  await writeFile(`${outDir}/${item.name}.png`, bytes);
  n++;
}
console.log(`Exported ${n} Figma PNGs → ${outDir}`);
