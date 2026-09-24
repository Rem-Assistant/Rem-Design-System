#!/usr/bin/env node
// Attach a "View source" Dev Resource link to every canonical Figma component,
// pointing at its SwiftUI source on GitHub. This is the lightweight, Code-Connect-free
// design↔code mapping: it shows up in Figma Dev Mode as a link on the component.
//
// Why REST (not the plugin): addDevResourceAsync only exists in Dev-Mode plugins;
// the REST endpoint works headlessly in CI.
//
//   FIGMA_TOKEN=... node tools/design-sync/dev-resources.mjs
//
import { manifest, figma, sourceUrl, requireToken } from './lib.mjs';

requireToken();
const file_key = manifest.figmaFileKey;
const entries = [...manifest.components, ...manifest.screens];

// Figma caps dev_resources POST at 50 per call.
const dev_resources = entries.map((e) => ({
  name: `Source: ${e.source.split('/').pop()}`,
  url: sourceUrl(e.source),
  file_key,
  node_id: e.node,
}));

for (let i = 0; i < dev_resources.length; i += 50) {
  const batch = dev_resources.slice(i, i + 50);
  const out = await figma('/dev_resources', { method: 'POST', body: JSON.stringify({ dev_resources: batch }) });
  const errs = (out.errors || []).length;
  console.log(`Posted ${batch.length - errs}/${batch.length} dev resources` + (errs ? ` (${errs} errors: ${JSON.stringify(out.errors)})` : ''));
}
console.log('Done. Open the file in Dev Mode → each component now links to its source.');
