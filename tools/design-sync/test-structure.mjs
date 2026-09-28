import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { verifyStructure } from './verify-figma-structure.mjs';

const contract = JSON.parse(await readFile(new URL('./structure-contract.json', import.meta.url), 'utf8'));

const screen = (entry) => ({ id: entry.node, type: 'FRAME', name: entry.name });
const placeholder = (entry) => ({
  id: `placeholder-${entry.node}`,
  type: 'FRAME',
  name: 'Mobile Placeholder',
  children: [screen(entry)],
});
const rows = [0, 1].map((row) => {
  const placeholders = contract.flow.screens.filter((entry) => entry.row === row).map(placeholder);
  const children = row === 0
    ? placeholders.flatMap((entry, index) => index === placeholders.length - 1
      ? [entry]
      : [entry, { id: `arrow-${index}`, type: 'VECTOR', name: `Vector ${index + 2}`, visible: true }])
    : placeholders;
  return {
    id: `row-${row}`,
    type: 'FRAME',
    name: 'Placeholder Flows',
    itemSpacing: contract.flow.hierarchy.rowSpacing[row],
    children,
  };
});
const flow = {
  id: contract.flow.id,
  type: 'SECTION',
  name: '01 · Consent flow',
  children: [{
    id: 'mobile-flow', type: 'FRAME', name: 'Mobile Flow', children: [{
      id: 'placeholder-sections', type: 'FRAME', name: 'Placeholder Sections', children: [{
        id: 'placeholder-section', type: 'FRAME', name: 'Placeholder Section', children: [{
          id: 'placeholder-rows', type: 'FRAME', name: 'Placeholder Rows', children: rows,
        }],
      }],
    }],
  },
  { ...contract.flow.prototype.label, type: 'TEXT' },
  ...contract.flow.prototype.frames.map((entry) => ({ id: entry.node, type: 'FRAME', name: entry.name, children: [] }))],
};
const page = {
  id: contract.page.id,
  type: 'CANVAS',
  name: contract.page.name,
  children: contract.page.topLevel,
  flowStartingPoints: contract.flow.prototype.frames
    .filter((entry) => entry.flowStart)
    .map((entry) => ({ nodeId: entry.node, name: 'Consent flow' })),
};

const passing = verifyStructure(contract, page, flow);
assert.equal(passing.ok, true);
assert.equal(passing.canonical.screens.every((entry) => entry.status === 'conformant'), true);
assert.match(passing.digest, /^[a-f0-9]{64}$/);

const looseFlow = structuredClone(flow);
const firstPlaceholder = looseFlow.children[0].children[0].children[0].children[0].children[0].children[0];
firstPlaceholder.children = [{ id: 'legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: firstPlaceholder.children }];
const failing = verifyStructure(contract, page, looseFlow);
assert.equal(failing.ok, false);
assert.match(failing.errors.join('\n'), /direct parent must be FRAME Mobile Placeholder/);

const leftoverSlotFlow = structuredClone(flow);
const leftoverPlaceholder = leftoverSlotFlow.children[0].children[0].children[0].children[0].children[0].children[0];
leftoverPlaceholder.children.push({ id: 'empty-legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: [] });
const leftoverSlot = verifyStructure(contract, page, leftoverSlotFlow);
assert.equal(leftoverSlot.ok, false);
assert.match(leftoverSlot.errors.join('\n'), /contains forbidden legacy children/);

const wrongRowFlow = structuredClone(flow);
const flowRows = wrongRowFlow.children[0].children[0].children[0].children[0].children;
flowRows[1].children.push(flowRows[0].children.shift());
const wrongRow = verifyStructure(contract, page, wrongRowFlow);
assert.equal(wrongRow.ok, false);
assert.match(wrongRow.errors.join('\n'), /must be in Placeholder Flows row 1/);

const wrongSpacingFlow = structuredClone(flow);
wrongSpacingFlow.children[0].children[0].children[0].children[0].children[1].itemSpacing = 24;
const wrongSpacing = verifyStructure(contract, page, wrongSpacingFlow);
assert.equal(wrongSpacing.ok, false);
assert.match(wrongSpacing.errors.join('\n'), /row 2 must use 200 item spacing/);

const extraTopLevel = structuredClone(page);
extraTopLevel.children.push({ id: 'extra:1', type: 'FRAME', name: 'Loose screen' });
const scattered = verifyStructure(contract, extraTopLevel, flow);
assert.equal(scattered.ok, false);
assert.match(scattered.errors.join('\n'), /Page top level/);

console.log('Figma structure verifier tests passed');
