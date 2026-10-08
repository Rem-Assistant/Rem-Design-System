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
// Figma REST omits a zero-valued auto-layout padding/spacing property: the file-node schema
// (HasFramePropertiesTrait) marks paddingBottom and itemSpacing optional with numeric default 0
// for auto-layout frames. Restore that documented default only for these exact fields.
const ZERO_DEFAULT_FIELDS = new Set(['paddingBottom', 'itemSpacing']);
const NORMALIZABLE_TYPES = new Set(['FRAME', 'SLOT']);
const AUTO_LAYOUT_MODES = new Set(['HORIZONTAL', 'VERTICAL']);
function zeroDefaultSchemaBasis(type) {
  const base = 'Figma REST file-node schema (HasFramePropertiesTrait): paddingBottom and itemSpacing are optional with numeric default 0 for auto-layout frames, so a 0 value is omitted from the REST response.';
  return type === 'SLOT'
    ? `${base} SlotNode is documented as a child auto-layout frame listed as supporting paddingBottom and itemSpacing (Plugin API node-properties); the independent exact-node Plugin read (2026-10-08) confirmed each property exists and equals 0. The REST spec does not separately enumerate SLOT.`
    : base;
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
  const defaultsApplied = [];
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
  const numeric = (actual, properties, label, expected) => {
    for (const [key, value] of Object.entries(properties)) {
      // REST exposes local coordinates in relativeTransform when geometry=paths.
      const observed = key === 'y' ? actual?.relativeTransform?.[1]?.[2] : actual?.[key];
      // The REST transport drops a zero-valued paddingBottom/itemSpacing. Restore the schema
      // default 0 only when the field is genuinely absent (no own property) on an existing,
      // identity-matched FRAME/SLOT whose explicit layoutMode is auto-layout, and only when the
      // trusted assertion itself expects zero. Everything else still fails closed below. This
      // never writes to the node or hides a broken identity/ancestry — those are checked apart.
      const absent = actual != null && !Object.hasOwn(actual, key);
      if (value === 0 && ZERO_DEFAULT_FIELDS.has(key) && absent && expected &&
        identity(actual, expected) && NORMALIZABLE_TYPES.has(actual.type) &&
        AUTO_LAYOUT_MODES.has(actual.layoutMode)) {
        defaultsApplied.push({
          nodeId: actual.id,
          field: key,
          rawPresence: 'absent',
          hadOwnProperty: false,
          appliedValue: 0,
          nodeType: actual.type,
          layoutMode: actual.layoutMode,
          schemaBasis: zeroDefaultSchemaBasis(actual.type),
        });
        continue;
      }
      if (typeof observed !== 'number' || observed !== value) {
        // Only selected public geometry fields are logged; never the whole REST response.
        // Bound unexpected values so malformed source data cannot flood diagnostics.
        const raw = (input) => JSON.stringify(input)?.slice(0, 512) ?? 'undefined';
        const details = {
          nodeId: actual?.id ?? label,
          field: key,
          hasOwnProperty: actual != null && Object.hasOwn(actual, key),
          observedType: observed === null ? 'null' : typeof observed,
          observedValue: raw(observed),
          nodeType: actual?.type ?? null,
          layoutMode: actual?.layoutMode ?? null,
          nodeMissing: actual == null,
          ...(key === 'y' ? {
            relativeTransformHasOwnProperty: actual != null && Object.hasOwn(actual, 'relativeTransform'),
            relativeTransformRaw: raw(actual?.relativeTransform),
          } : {}),
        };
        check(false, `${label} ${key} differs from ${value}; observed=${JSON.stringify(details)}`);
      }
    }
  };
  for (const slot of lane.canonicalSlotAssertions) {
    checkIdentity(slot.node); checkIdentity(slot.directParent);
    path(slot.pageToNodePathIds);
    check(slot.pageToNodePathIds[0] === lane.page.id && slot.pageToNodePathIds.at(-2) === slot.directParent.id &&
      slot.pageToNodePathIds.at(-1) === slot.node.id, `${slot.node.id} invalid slot path`);
    const actual = node(slot.node.id); const parent = node(slot.directParent.id);
    numeric(actual, slot.numericProperties, slot.node.id, slot.node);
    numeric(parent, slot.parentNumericProperties, slot.directParent.id, slot.directParent);
    const index = (parent?.children || []).findIndex(({ id }) => id === slot.node.id);
    check(index > 0 && identity(parent.children[index - 1], slot.immediatelyPrecededBy), `${slot.node.id} preceding AddSchedule changed`);
    check(identity(actual?.children?.[0], slot.firstChild.node), `${slot.node.id} first child changed`);
    numeric(node(slot.firstChild.node.id), slot.firstChild.numericProperties, slot.firstChild.node.id, slot.firstChild.node);
  }
  const structure = { canonicalScreens, screens: [], sourceAmendments: lane.limits };
  return { errors, structure, defaultsApplied, structureDigest: createHash('sha256').update(JSON.stringify(structure)).digest('hex') };
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
    return { ...report, status: 'error', errors: [error.message], structure: null, structureDigest: null, defaultsApplied: [] };
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
