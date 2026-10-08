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
  // Explicit 0 values are present in the fixture, so no schema default is restored.
  assert.deepEqual(result.defaultsApplied, []);
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
test('numeric failures expose bounded presence, type and geometry diagnostics without accepting defaults', () => {
  const f = fixture();
  // NONE layout is not auto-layout, so an absent paddingBottom still fails closed with diagnostics.
  f.nodes['6:1'].layoutMode = 'NONE';
  delete f.nodes['6:1'].paddingBottom;
  f.nodes['6:2'].itemSpacing = null;
  delete f.nodes['6:4'].relativeTransform;
  const result = verify(f);
  const errors = result.errors;
  assert.deepEqual(result.defaultsApplied, []);
  const details = (prefix) => JSON.parse(errors.find((error) => error.startsWith(prefix)).split('; observed=')[1]);
  assert.deepEqual(details('6:1 paddingBottom'), {
    nodeId: '6:1', field: 'paddingBottom', hasOwnProperty: false,
    observedType: 'undefined', observedValue: 'undefined', nodeType: 'SLOT',
    layoutMode: 'NONE', nodeMissing: false,
  });
  assert.equal(details('6:2 itemSpacing').hasOwnProperty, true);
  assert.equal(details('6:2 itemSpacing').observedType, 'null');
  assert.equal(details('6:2 itemSpacing').observedValue, 'null');
  assert.equal(details('6:4 y').relativeTransformHasOwnProperty, false);
  assert.equal(details('6:4 y').relativeTransformRaw, 'undefined');
  f.nodes['6:1'].paddingBottom = 'x'.repeat(2000);
  const bounded = verify(f).errors.find((error) => error.startsWith('6:1 paddingBottom'));
  assert.equal(JSON.parse(bounded.split('; observed=')[1]).observedValue.length, 512);
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

// --- Issue #91: REST zero-default (paddingBottom / itemSpacing) transport normalization ---
// Make a zero-default field genuinely absent (as the REST transport delivers it) on a node with a
// valid auto-layout mode. Base fixture ids: 6:1 is the SLOT (paddingBottom), 6:2 is the parent
// FRAME (itemSpacing).
const absentFixture = (nodeId, field, layoutMode) => {
  const f = fixture();
  f.nodes[nodeId].layoutMode = layoutMode;
  delete f.nodes[nodeId][field];
  return f;
};
test('restores schema-default 0 for an absent paddingBottom/itemSpacing on each allowed FRAME/SLOT auto-layout mode', () => {
  for (const [nodeId, field, type] of [['6:1', 'paddingBottom', 'SLOT'], ['6:2', 'itemSpacing', 'FRAME']]) {
    for (const layoutMode of ['HORIZONTAL', 'VERTICAL']) {
      const result = verify(absentFixture(nodeId, field, layoutMode));
      assert.deepEqual(result.errors, [], `${nodeId}/${field}/${layoutMode}`);
      assert.equal(result.defaultsApplied.length, 1);
      assert.deepEqual(result.defaultsApplied[0], {
        nodeId, field, rawPresence: 'absent', hadOwnProperty: false, appliedValue: 0,
        nodeType: type, layoutMode, schemaBasis: result.defaultsApplied[0].schemaBasis,
      });
      assert.match(result.defaultsApplied[0].schemaBasis, /HasFramePropertiesTrait/);
      if (type === 'SLOT') assert.match(result.defaultsApplied[0].schemaBasis, /SlotNode/);
      else assert.doesNotMatch(result.defaultsApplied[0].schemaBasis, /SlotNode/);
    }
  }
});
test('normalization mutates neither the fetched node nor the contract', () => {
  const f = absentFixture('6:1', 'paddingBottom', 'VERTICAL');
  const nodeBefore = JSON.stringify(f.nodes['6:1']);
  const contractBefore = JSON.stringify(f.contract);
  const result = verify(f);
  assert.deepEqual(result.errors, []);
  assert.equal(Object.hasOwn(f.nodes['6:1'], 'paddingBottom'), false);
  assert.equal(JSON.stringify(f.nodes['6:1']), nodeBefore);
  assert.equal(JSON.stringify(f.contract), contractBefore);
});
test('explicit 0 is accepted without emitting a default-normalization audit entry', () => {
  const f = fixture();
  f.nodes['6:1'].layoutMode = 'VERTICAL'; f.nodes['6:1'].paddingBottom = 0;
  f.nodes['6:2'].layoutMode = 'VERTICAL'; f.nodes['6:2'].itemSpacing = 0;
  const result = verify(f);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.defaultsApplied, []);
});
test('fails closed and applies no default for absent node, wrong identity, disallowed/unknown type and non-auto-layout', () => {
  for (const mutate of [
    (n) => { delete n['6:1']; },                                                                   // absent node
    (n) => { n['6:1'].layoutMode = 'VERTICAL'; delete n['6:1'].paddingBottom; n['6:1'].id = '6:9'; },     // wrong id
    (n) => { n['6:1'].layoutMode = 'VERTICAL'; delete n['6:1'].paddingBottom; n['6:1'].name = 'Renamed'; }, // wrong name
    (n) => { n['6:1'].layoutMode = 'VERTICAL'; delete n['6:1'].paddingBottom; n['6:1'].type = 'COMPONENT'; }, // disallowed type
    (n) => { n['6:1'].layoutMode = 'VERTICAL'; delete n['6:1'].paddingBottom; n['6:1'].type = 'MYSTERY'; },   // unknown type
    (n) => { delete n['6:1'].paddingBottom; },                                                      // missing layoutMode
    (n) => { n['6:1'].layoutMode = 'NONE'; delete n['6:1'].paddingBottom; },                        // NONE layout
    (n) => { n['6:1'].layoutMode = 'GRID'; delete n['6:1'].paddingBottom; },                        // GRID layout
    (n) => { n['6:1'].layoutMode = 'DIAGONAL'; delete n['6:1'].paddingBottom; },                    // unknown layout
  ]) {
    const f = fixture(); mutate(f.nodes);
    const result = verify(f);
    assert.ok(result.errors.length, 'expected fail-closed error');
    assert.deepEqual(result.defaultsApplied, []);
  }
});
test('fails closed for explicit undefined/null/string/boolean/NaN/Infinity/-Infinity and nonzero where zero is required', () => {
  for (const value of [undefined, null, 'x', false, true, NaN, Infinity, -Infinity, 12]) {
    const f = fixture();
    f.nodes['6:1'].layoutMode = 'VERTICAL';
    f.nodes['6:1'].paddingBottom = value; // present own property → not absent → never normalized
    const result = verify(f);
    assert.ok(result.errors.some((e) => e.startsWith('6:1 paddingBottom')), `value ${String(value)}`);
    assert.deepEqual(result.defaultsApplied, []);
  }
});
test('does not normalize a zero-default field whose expectation is nonzero, nor a non-zero-default missing field', () => {
  const f1 = fixture();
  f1.contract.lanes[0].canonicalSlotAssertions[0].numericProperties.paddingBottom = 24; // nonzero expectation
  f1.nodes['6:1'].layoutMode = 'VERTICAL'; delete f1.nodes['6:1'].paddingBottom;
  const r1 = verify(f1);
  assert.ok(r1.errors.some((e) => e.startsWith('6:1 paddingBottom')));
  assert.deepEqual(r1.defaultsApplied, []);
  const f2 = fixture();
  f2.nodes['6:1'].layoutMode = 'VERTICAL'; delete f2.nodes['6:1'].paddingTop; // not a zero-default field
  const r2 = verify(f2);
  assert.ok(r2.errors.some((e) => e.startsWith('6:1 paddingTop')));
  assert.deepEqual(r2.defaultsApplied, []);
});
test('normalization never masks top-padding drift or missing/changed first-child coordinates', () => {
  for (const mutate of [
    (n) => { n['6:1'].paddingTop = 0; },                              // top padding 24 → 0 drift
    (n) => { delete n['6:4'].relativeTransform; },                   // missing first-child coordinate
    (n) => { n['6:4'].relativeTransform = [[1, 0, 0], [0, 1, 48]]; }, // changed first-child y
  ]) {
    const f = fixture(); mutate(f.nodes);
    const result = verify(f);
    assert.ok(result.errors.length);
    assert.deepEqual(result.defaultsApplied, []);
  }
});
const fetchFromFixture = (f) => async (url) => {
  const ids = decodeURIComponent(url.split('ids=')[1].split('&')[0]).split(',');
  const nodes = {};
  for (const id of ids) if (f.nodes[id]) nodes[id] = { document: f.nodes[id] };
  return { nodes };
};
test('createPlaygroundStructureReport surfaces and serializes the applied-default audit data', async () => {
  const f = absentFixture('6:1', 'paddingBottom', 'VERTICAL');
  const report = await createPlaygroundStructureReport(f.contract, 'agenda-suggestions', {
    head: 'b'.repeat(40), fetchFigma: fetchFromFixture(f),
  });
  assert.equal(report.status, 'completed');
  assert.deepEqual(report.errors, []);
  assert.equal(report.defaultsApplied.length, 1);
  const serialized = JSON.parse(JSON.stringify(report));
  assert.equal(serialized.defaultsApplied[0].nodeId, '6:1');
  assert.equal(serialized.defaultsApplied[0].field, 'paddingBottom');
  assert.equal(serialized.defaultsApplied[0].appliedValue, 0);
  assert.equal(serialized.defaultsApplied[0].nodeType, 'SLOT');
  assert.equal(serialized.defaultsApplied[0].layoutMode, 'VERTICAL');
  assert.match(serialized.defaultsApplied[0].schemaBasis, /SlotNode/);
});
