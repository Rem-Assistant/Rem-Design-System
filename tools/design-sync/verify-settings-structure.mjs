#!/usr/bin/env node
// Read-only, trusted-base source verification for Settings. Intentionally
// independent of the Onboarding page's documentation/prototype requirements.
import { createHash } from 'node:crypto';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { figma } from './lib.mjs';

function visibleNodes(node, parent = null, found = []) {
  if (node.visible === false) return found;
  found.push({ node, parent });
  for (const child of node.children || []) visibleNodes(child, node.id, found);
  return found;
}
function visibleText(node) {
  return visibleNodes(node).filter(({ node }) => node.type === 'TEXT').map(({ node }) => node.characters);
}
export function verifySettingsStructure(contract, { page, section, screens }) {
  const errors = [];
  for (const [label, actual, expected] of [['page', page, contract.page], ['section', section, contract.section]]) {
    if (!actual || ['id', 'type', 'name'].some((key) => actual[key] !== expected[key])) errors.push(`Settings ${label} identity differs from the approved source`);
  }
  if (!(page?.children || []).some((node) => node.id === contract.section.id)) errors.push('Settings section is not a direct child of the approved page');
  const canonicalScreens = [];
  for (const expected of contract.screens) {
    const root = screens[expected.id];
    const start = errors.length;
    if (!root || ['id', 'type', 'name'].some((key) => root[key] !== expected[key]) || root.visible === false) errors.push(`${expected.id} canonical screen identity is missing or changed`);
    if (!(section?.children || []).some((node) => node.id === expected.id)) errors.push(`${expected.id} is not a direct child of the Settings section`);
    const nodes = root ? visibleNodes(root) : [];
    const byId = new Map(nodes.map((entry) => [entry.node.id, entry]));
    const families = new Set(expected.instances.map((node) => node.componentId));
    const actualOrder = nodes.filter(({ node }) => node.type === 'INSTANCE' && families.has(node.componentId)).map(({ node }) => node.id);
    if (JSON.stringify(actualOrder) !== JSON.stringify(expected.instances.map(({ id }) => id))) errors.push(`${expected.id} visible section/row/label order or canonical reuse changed`);
    for (const instance of expected.instances) {
      const actual = byId.get(instance.id);
      if (!actual || actual.node.type !== 'INSTANCE' || actual.node.componentId !== instance.componentId || actual.parent !== instance.parent) {
        errors.push(`${instance.id} canonical instance or parent changed`);
        continue;
      }
      if (instance.text && JSON.stringify(visibleText(actual.node)) !== JSON.stringify(instance.text)) errors.push(`${instance.id} visible copy changed`);
    }
    canonicalScreens.push({ id: expected.id, name: expected.name, status: errors.length === start ? 'conformant' : 'nonconformant' });
  }
  const structure = { canonicalScreens, screens: [], sourceAmendments: contract.amendments };
  return { errors, structure, structureDigest: createHash('sha256').update(JSON.stringify(structure)).digest('hex') };
}
export async function createSettingsStructureReport(contract, { fetchFigma = figma, head } = {}) {
  if (!/^[0-9a-f]{40}$/.test(head || '')) throw new Error('Full candidate head SHA required');
  const report = { version: contract.version, fileKey: contract.fileKey, head, capturedAt: new Date().toISOString() };
  try {
    const request = (ids, depth = '') => fetchFigma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(ids.join(','))}${depth ? `&depth=${depth}` : ''}`);
    const [pageData, sectionData, screensData] = await Promise.all([
      request([contract.page.id], 1), request([contract.section.id], 1), request(contract.screens.map(({ id }) => id)),
    ]);
    const result = verifySettingsStructure(contract, {
      page: pageData.nodes?.[contract.page.id]?.document,
      section: sectionData.nodes?.[contract.section.id]?.document,
      screens: Object.fromEntries(contract.screens.map(({ id }) => [id, screensData.nodes?.[id]?.document])),
    });
    return { ...report, ...result, status: result.errors.length ? 'failed' : 'completed' };
  } catch (error) {
    return { ...report, status: 'error', errors: [error.message], structure: null, structureDigest: null };
  }
}
async function main() {
  const options = Object.fromEntries(process.argv.slice(2).map((arg) => { const [key, ...value] = arg.replace(/^--/, '').split('='); return [key, value.join('=')]; }));
  // The contract is loaded only from this trusted checkout, never PR executable/data input.
  const contract = JSON.parse(await readFile(new URL('./settings-structure-contract.json', import.meta.url), 'utf8'));
  const output = resolve(options.output || 'artifacts/figma-structure-report.json');
  const report = await createSettingsStructureReport(contract, {
    head: options.head || process.env.GITHUB_SHA,
    fetchFigma: process.env.FIGMA_TOKEN ? figma : async () => { throw new Error('FIGMA_TOKEN is not set'); },
  });
  await mkdir(dirname(output), { recursive: true });
  await writeFile(output, JSON.stringify(report, null, 2) + '\n');
  if (report.status !== 'completed') throw new Error(report.errors.join('\n'));
  console.log(`Verified Settings source ${report.structureDigest}`);
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) await main();
