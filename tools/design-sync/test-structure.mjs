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
const rows = Array.from({ length: contract.flow.hierarchy.rowCount }, (_, row) => {
  const placeholders = contract.flow.screens.filter((entry) => entry.row === row).map(placeholder);
  const children = placeholders.flatMap((entry, index) => index === placeholders.length - 1
    ? [entry]
    : [entry, { id: `arrow-${row}-${index}`, type: 'VECTOR', name: `Vector ${index + 2}`, visible: true }]);
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
  name: '01A · Consent · Documentation',
  children: [{
    id: 'mobile-flow', type: 'FRAME', name: 'Mobile Flow', children: [{
      id: 'placeholder-sections', type: 'FRAME', name: 'Placeholder Sections', children: [{
        id: 'placeholder-section', type: 'FRAME', name: 'Placeholder Section', children: [{
          id: 'placeholder-rows', type: 'FRAME', name: 'Placeholder Rows', children: rows,
        }],
      }],
    }],
  }],
};
const prototype = {
  ...contract.flow.prototype.root,
  children: [
    { ...contract.flow.prototype.label, type: 'TEXT' },
    ...contract.flow.prototype.frames.map((entry) => ({ id: entry.node, type: 'FRAME', name: entry.name, children: [] })),
  ],
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

const passing = verifyStructure(contract, page, flow, prototype);
assert.equal(passing.ok, true);
assert.equal(passing.canonical.screens.every((entry) => entry.status === 'conformant'), true);
assert.match(passing.digest, /^[a-f0-9]{64}$/);

const looseFlow = structuredClone(flow);
const firstPlaceholder = looseFlow.children[0].children[0].children[0].children[0].children[0].children[0];
firstPlaceholder.children = [{ id: 'legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: firstPlaceholder.children }];
const failing = verifyStructure(contract, page, looseFlow, prototype);
assert.equal(failing.ok, false);
assert.match(failing.errors.join('\n'), /direct parent must be FRAME Mobile Placeholder/);

const leftoverSlotFlow = structuredClone(flow);
const leftoverPlaceholder = leftoverSlotFlow.children[0].children[0].children[0].children[0].children[0].children[0];
leftoverPlaceholder.children.push({ id: 'empty-legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: [] });
const leftoverSlot = verifyStructure(contract, page, leftoverSlotFlow, prototype);
assert.equal(leftoverSlot.ok, false);
assert.match(leftoverSlot.errors.join('\n'), /contains forbidden legacy children/);

const wrongSpacingFlow = structuredClone(flow);
wrongSpacingFlow.children[0].children[0].children[0].children[0].children[0].itemSpacing = 200;
const wrongSpacing = verifyStructure(contract, page, wrongSpacingFlow, prototype);
assert.equal(wrongSpacing.ok, false);
assert.match(wrongSpacing.errors.join('\n'), /row 1 must use 24 item spacing/);

const nestedPrototype = structuredClone(prototype);
nestedPrototype.children = [{ id: 'wrapper', type: 'FRAME', name: 'Wrapper', children: nestedPrototype.children }];
const invalidPrototype = verifyStructure(contract, page, flow, nestedPrototype);
assert.equal(invalidPrototype.ok, false);
assert.match(invalidPrototype.errors.join('\n'), /direct child of prototype/);

const extraTopLevel = structuredClone(page);
extraTopLevel.children.push({ id: 'extra:1', type: 'FRAME', name: 'Loose screen' });
const scattered = verifyStructure(contract, extraTopLevel, flow, prototype);
assert.equal(scattered.ok, false);
assert.match(scattered.errors.join('\n'), /Page top level/);

console.log('Figma structure verifier tests passed');
