#!/usr/bin/env node
// GET-only source verification. All assertions come from the trusted base checkout.
import { createHash } from 'node:crypto';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { figma } from './lib.mjs';

export const SCOPES = ['agenda-suggestions', 'onboarding-voice'];
const equal = (a, b) => JSON.stringify(a) === JSON.stringify(b);
// Plugin API calls pages PAGE; the REST representation calls them CANVAS.
const restType = (type) => type === 'PAGE' ? 'CANVAS' : type;
function identity(actual, expected) {
  return actual && actual.id === expected.id && actual.name === expected.name &&
    actual.type === restType(expected.type);
}
function visibleNodes(node, result = []) {
  if (!node || node.visible === false) return result;
  result.push(node);
  for (const child of node.children || []) visibleNodes(child, result);
  return result;
}
function laneFor(contract, scope) {
  if (contract.schemaVersion !== 1 || !SCOPES.includes(scope)) throw new Error('Unsupported playground source scope/schema');
  const matches = contract.lanes.filter((lane) => lane.laneId === scope);
  if (matches.length !== 1 || matches[0].states.length !== 3) throw new Error('Exactly one lane with three canonical states required');
  return matches[0];
}

export function verifyPlaygroundStructure(contract, scope, nodes) {
  const lane = laneFor(contract, scope);
  const errors = [];
  const check = (ok, message) => { if (!ok) errors.push(message); };
  const node = (id) => nodes[id];
  const checkIdentity = (expected) => check(identity(node(expected.id), expected), `${expected.id} identity changed`);
  const path = (ids) => {
    for (let i = 1; i < ids.length; i++) check(
      (node(ids[i - 1])?.children || []).some((child) => child.id === ids[i]),
      `${ids[i - 1]} → ${ids[i]} direct ancestry changed`);
  };
  checkIdentity(lane.page); checkIdentity(lane.section);
  path([lane.page.id, lane.section.id]);
  const laneStart = errors.length;
  const canonicalScreens = [];
  for (const state of lane.states) {
    const start = errors.length;
    checkIdentity(state.root); checkIdentity(state.directParent);
    path(state.pageToRootPathIds);
    check(state.pageToRootPathIds[0] === lane.page.id &&
      state.pageToRootPathIds.at(-2) === state.directParent.id &&
      state.pageToRootPathIds.at(-1) === state.root.id, `${state.root.id} invalid source path`);
    const root = node(state.root.id);
    check(root?.visible !== false, `${state.root.id} hidden root`);
    check(equal((root?.children || []).map(({ id }) => id), state.requiredRootChildrenInOrder), `${state.root.id} root child order changed`);
    const visible = visibleNodes(root);
    const textScope = visible.find(({ id }) => id === state.textScopeNodeId);
    check(Boolean(textScope), `${state.textScopeNodeId} visible text scope missing`);
    const text = visibleNodes(textScope).filter(({ type }) => type === 'TEXT');
    let previous = -1;
    for (const expected of state.requiredVisibleTextInOrder) {
      const index = text.findIndex(({ id }) => id === expected.id);
      check(index > previous && text[index]?.characters === expected.text, `${expected.id} visible copy/order changed`);
      if (index >= 0) previous = index;
    }
    for (const forbidden of state.forbiddenVisibleText) check(!text.some(({ characters }) => characters === forbidden), `${state.root.id} forbidden visible copy: ${forbidden}`);
    for (const instance of state.requiredInstances) {
      checkIdentity(instance.node); checkIdentity(instance.mainComponent); checkIdentity(instance.mainComponentParent);
      const actual = node(instance.node.id);
      check(actual?.componentId === instance.mainComponent.id, `${instance.node.id} canonical component changed`);
      path(instance.rootToInstancePathIds);
      check(instance.rootToInstancePathIds[0] === state.root.id && instance.rootToInstancePathIds.at(-1) === instance.node.id,
        `${instance.node.id} invalid instance path`);
      path([instance.mainComponentParent.id, instance.mainComponent.id]);
      check(visible.some(({ id }) => id === instance.node.id), `${instance.node.id} canonical instance hidden`);
      for (const [key, expected] of Object.entries(instance.requiredProperties)) {
        const property = actual?.componentProperties?.[key];
        check(property?.type === expected.type && equal(property?.value, expected.value), `${instance.node.id} property ${key} changed`);
      }
    }
    for (const assertion of state.childOrderAssertions) check(assertion.mode === 'exact' &&
      equal((node(assertion.parentId)?.children || []).map(({ id }) => id), assertion.childIds), `${assertion.parentId} exact child order changed`);
    canonicalScreens.push({ id: state.root.id, name: state.root.name, status: start === errors.length && laneStart === 0 ? 'conformant' : 'nonconformant' });
  }
  const numeric = (actual, properties, label) => {
    for (const [key, value] of Object.entries(properties)) {
      // REST exposes local coordinates in relativeTransform when geometry=paths.
      const observed = key === 'y' ? actual?.relativeTransform?.[1]?.[2] : actual?.[key];
      check(typeof observed === 'number' && observed === value, `${label} ${key} differs from ${value}`);
    }
  };
  for (const slot of lane.canonicalSlotAssertions) {
    checkIdentity(slot.node); checkIdentity(slot.directParent);
    path(slot.pageToNodePathIds);
    check(slot.pageToNodePathIds[0] === lane.page.id && slot.pageToNodePathIds.at(-2) === slot.directParent.id &&
      slot.pageToNodePathIds.at(-1) === slot.node.id, `${slot.node.id} invalid slot path`);
    const actual = node(slot.node.id); const parent = node(slot.directParent.id);
    numeric(actual, slot.numericProperties, slot.node.id);
    numeric(parent, slot.parentNumericProperties, slot.directParent.id);
    const index = (parent?.children || []).findIndex(({ id }) => id === slot.node.id);
    check(index > 0 && identity(parent.children[index - 1], slot.immediatelyPrecededBy), `${slot.node.id} preceding AddSchedule changed`);
    check(identity(actual?.children?.[0], slot.firstChild.node), `${slot.node.id} first child changed`);
    numeric(node(slot.firstChild.node.id), slot.firstChild.numericProperties, slot.firstChild.node.id);
  }
  const structure = { canonicalScreens, screens: [], sourceAmendments: lane.limits };
  return { errors, structure, structureDigest: createHash('sha256').update(JSON.stringify(structure)).digest('hex') };
}

export async function createPlaygroundStructureReport(contract, scope, { fetchFigma = figma, head } = {}) {
  if (!/^[0-9a-f]{40}$/.test(head || '')) throw new Error('Full candidate head SHA required');
  const lane = laneFor(contract, scope);
  const report = { version: 1, fileKey: contract.fileKey, primaryContract: scope, head, capturedAt: new Date().toISOString() };
  try {
    const ids = new Set([lane.page.id, lane.section.id]);
    for (const state of lane.states) {
      state.pageToRootPathIds.forEach((id) => ids.add(id));
      for (const instance of state.requiredInstances) {
        instance.rootToInstancePathIds.forEach((id) => ids.add(id));
        ids.add(instance.mainComponent.id); ids.add(instance.mainComponentParent.id);
      }
    }
    for (const slot of lane.canonicalSlotAssertions) {
      slot.pageToNodePathIds.forEach((id) => ids.add(id)); ids.add(slot.firstChild.node.id);
    }
    const nodes = {};
    function index(node) { if (!node) return; nodes[node.id] = node; for (const child of node.children || []) index(child); }
    // Shallow ancestry/masters first, then complete bounded reference roots. No whole-file fetch.
    const list = [...ids];
    for (let i = 0; i < list.length; i += 30) {
      const response = await fetchFigma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(list.slice(i, i + 30).join(','))}&depth=2&geometry=paths`);
      for (const entry of Object.values(response.nodes || {})) {
        const visit = (node) => { if (!node) return; if (!nodes[node.id] || (node.children?.length || 0) >= (nodes[node.id].children?.length || 0)) nodes[node.id] = node; for (const child of node.children || []) visit(child); };
        visit(entry?.document);
      }
    }
    const response = await fetchFigma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(lane.states.map(({ root }) => root.id).join(','))}&geometry=paths`);
    for (const entry of Object.values(response.nodes || {})) index(entry?.document);
    const result = verifyPlaygroundStructure(contract, scope, nodes);
    return { ...report, ...result, status: result.errors.length ? 'failed' : 'completed' };
  } catch (error) {
    return { ...report, status: 'error', errors: [error.message], structure: null, structureDigest: null };
  }
}
async function main() {
  const options = Object.fromEntries(process.argv.slice(2).map((arg) => { const [key, ...value] = arg.replace(/^--/, '').split('='); return [key, value.join('=')]; }));
  const contract = JSON.parse(await readFile(new URL('./playground-source-contracts.json', import.meta.url), 'utf8'));
  const report = await createPlaygroundStructureReport(contract, options['primary-contract'], {
    head: options.head || process.env.GITHUB_SHA,
    fetchFigma: process.env.FIGMA_TOKEN ? figma : async () => { throw new Error('FIGMA_TOKEN is not set'); },
  });
  const output = resolve(options.output || 'artifacts/figma-structure-report.json');
  await mkdir(dirname(output), { recursive: true });
  await writeFile(output, JSON.stringify(report, null, 2) + '\n');
  if (report.status !== 'completed') throw new Error(report.errors.join('\n'));
  console.log(`Verified ${options['primary-contract']} source ${report.structureDigest}`);
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) await main();
