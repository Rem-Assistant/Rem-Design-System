import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { verifyPlaygroundStructure, createPlaygroundStructureReport } from './verify-playground-structure.mjs';
import { contractReferenceItems } from './reference-config.mjs';

const identity = (id, type, name = id) => ({ id, type, name });
function fixture() {
  const page = identity('1:1', 'PAGE');
  const section = identity('1:2', 'SECTION');
  const master = identity('2:1', 'COMPONENT');
  const masterParent = identity('2:2', 'SECTION');
  const states = [3, 4, 5].map((n) => ({
    renderKey: `Fixture-${n}`, root: identity(`1:${n}`, 'FRAME'), directParent: section,
    pageToRootPathIds: ['1:1', '1:2', `1:${n}`], requiredRootChildrenInOrder: [`${n}:1`],
    textScopeNodeId: `1:${n}`, requiredVisibleTextInOrder: [{ id: `${n}:2`, text: 'Choose' }, { id: `${n}:3`, text: 'Continue' }],
    forbiddenVisibleText: ['Conversation entry'], childOrderAssertions: [],
    requiredInstances: [{ node: identity(`${n}:1`, 'INSTANCE'), mainComponent: master,
      mainComponentParent: masterParent, rootToInstancePathIds: [`1:${n}`, `${n}:1`],
      requiredProperties: { 'Show conversation entry#1:0': { type: 'BOOLEAN', value: false } } }],
  }));
  const slot = { node: identity('6:1', 'SLOT'), directParent: identity('6:2', 'FRAME'),
    pageToNodePathIds: ['1:1', '6:2', '6:1'], numericProperties: { paddingTop: 24, paddingBottom: 0 },
    parentNumericProperties: { itemSpacing: 0 }, immediatelyPrecededBy: identity('6:3', 'INSTANCE'),
    firstChild: { node: identity('6:4', 'INSTANCE'), numericProperties: { y: 24 } } };
  const contract = { schemaVersion: 1, fileKey: 'fixture', lanes: [{ laneId: 'agenda-suggestions', page, section, states,
    canonicalSlotAssertions: [slot], limits: [] }] };
  const nodes = {};
  for (const state of states) {
    const n = state.root.id.split(':')[1];
    const text = [2, 3].map((k) => ({ ...identity(`${n}:${k}`, 'TEXT'), characters: k === 2 ? 'Choose' : 'Continue' }));
    const instance = { ...state.requiredInstances[0].node, componentId: master.id,
      componentProperties: { 'Show conversation entry#1:0': { type: 'BOOLEAN', value: false } }, children: text };
    nodes[state.root.id] = { ...state.root, children: [instance] }; nodes[instance.id] = instance;
    for (const t of text) nodes[t.id] = t;
  }
  nodes['1:2'] = { ...section, children: states.map(({ root }) => nodes[root.id]) };
  nodes['2:1'] = master; nodes['2:2'] = { ...masterParent, children: [master] };
  nodes['6:4'] = { ...slot.firstChild.node, relativeTransform: [[1, 0, 0], [0, 1, 24]] };
  nodes['6:1'] = { ...slot.node, ...slot.numericProperties, children: [nodes['6:4']] };
  nodes['6:2'] = { ...slot.directParent, itemSpacing: 0, children: [slot.immediatelyPrecededBy, nodes['6:1']] };
  nodes['1:1'] = { ...page, type: 'CANVAS', children: [nodes['1:2'], nodes['6:2']] };
  return { contract, nodes };
}
const verify = ({ contract, nodes }) => verifyPlaygroundStructure(contract, 'agenda-suggestions', nodes);
test('verifies canonical roots, API page type, reuse, copy, properties and independent slot spacing', () => {
  const result = verify(fixture()); assert.deepEqual(result.errors, []);
  assert.equal(result.structure.canonicalScreens.length, 3);
});
test('rejects wrong page identity, moved sections and root replacement', () => {
  for (const mutate of [
    (n) => { n['1:1'].name = 'Other page'; },
    (n) => { n['1:1'].children = []; },
    (n) => { n['1:3'].type = 'COMPONENT'; },
    (n) => { n['1:3'].children = []; },
  ]) { const f = fixture(); mutate(f.nodes); assert.ok(verify(f).errors.length); }
});
test('rejects changed or hidden source copy, wrong text order and forbidden copy', () => {
  for (const mutate of [
    (n) => { n['3:2'].characters = 'Invented'; },
    (n) => { n['3:1'].visible = false; },
    (n) => { n['3:1'].children.reverse(); },
    (n) => { n['3:1'].children.push({ id: '9:1', type: 'TEXT', characters: 'Conversation entry' }); },
  ]) { const f = fixture(); mutate(f.nodes); assert.ok(verify(f).errors.length); }
});
test('rejects detached core, changed master parent, and enabled conversation entry', () => {
  for (const mutate of [
    (n) => { n['3:1'].componentId = '9:9'; },
    (n) => { n['2:2'].children = []; },
    (n) => { n['3:1'].componentProperties['Show conversation entry#1:0'].value = true; },
  ]) { const f = fixture(); mutate(f.nodes); assert.ok(verify(f).errors.length); }
});
test('rejects slot spacing drift, wrong preceding action and absent local coordinate proof', () => {
  for (const mutate of [
    (n) => { n['6:1'].paddingTop = 0; },
    (n) => { n['6:2'].itemSpacing = 24; },
    (n) => { n['6:2'].children.reverse(); },
    (n) => { delete n['6:4'].relativeTransform; },
  ]) { const f = fixture(); mutate(f.nodes); assert.ok(verify(f).errors.length); }
});
test('rejects additional or reordered children in explicit exact order assertions', () => {
  const f = fixture(); f.contract.lanes[0].states[0].childOrderAssertions = [{ parentId: '3:1', mode: 'exact', childIds: ['3:2', '3:3'] }];
  assert.deepEqual(verify(f).errors, []);
  f.nodes['3:1'].children.push(identity('9:9', 'FRAME'));
  assert.match(verify(f).errors.join(), /exact child order/);
});
test('GET-only reports fail closed on missing nodes, transport errors and unsupported scopes', async () => {
  const { contract } = fixture(); const calls = [];
  const report = await createPlaygroundStructureReport(contract, 'agenda-suggestions', { head: 'a'.repeat(40), fetchFigma: async (url, init) => {
    calls.push(url); assert.equal(init, undefined); return { nodes: {} };
  } });
  assert.equal(report.status, 'failed'); assert.ok(calls.every((url) => url.startsWith('/files/fixture/nodes?ids=')));
  const denied = await createPlaygroundStructureReport(contract, 'agenda-suggestions', { head: 'a'.repeat(40), fetchFigma: async () => { throw new Error('Denied'); } });
  assert.equal(denied.status, 'error'); assert.equal(denied.structure, null);
  await assert.rejects(createPlaygroundStructureReport(contract, 'settings-foundation', { head: 'a'.repeat(40) }), /scope/);
  await assert.rejects(createPlaygroundStructureReport(contract, 'agenda-suggestions', { head: 'short' }), /SHA/);
});
test('trusted delivery references match all six exact source roots and exempt only authored missing documentation/prototype', () => {
  const source = JSON.parse(readFileSync(new URL('./playground-source-contracts.json', import.meta.url)));
  const delivery = JSON.parse(readFileSync(new URL('../render-evidence/contracts.json', import.meta.url)));
  assert.equal(source.lanes.length, 2);
  for (const lane of source.lanes) {
    assert.deepEqual(contractReferenceItems(delivery, lane.laneId), lane.states.map(({ renderKey, root }) => ({ name: renderKey, node: root.id })));
    assert.equal(delivery[lane.laneId].requirePrototype, false);
    assert.ok(Object.values(delivery[lane.laneId].references).every((r) => r.requireDocumentation === false));
  }
});
