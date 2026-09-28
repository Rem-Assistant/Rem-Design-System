import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { verifyStructure } from './verify-figma-structure.mjs';

const contract = JSON.parse(await readFile(new URL('./structure-contract.json', import.meta.url), 'utf8'));

const inventory = {
  id: contract.inventory.id,
  type: contract.inventory.type,
  name: contract.inventory.name,
  fills: [{ type: 'SOLID', visible: true, color: { r: 245 / 255, g: 245 / 255, b: 245 / 255 } }],
  children: [
    {
      ...contract.inventory.screens[0],
      children: [
        { ...contract.inventory.assets.remAppIcon, fills: [{ type: 'IMAGE', visible: true, imageRef: 'source-raster' }] },
        {
          ...contract.inventory.assets.googleGlyph,
          children: Array.from({ length: contract.inventory.assets.googleGlyph.minimumVectorCount }, (_, index) => ({ id: `google-vector-${index}`, type: 'VECTOR', name: 'Vector' })),
        },
      ],
    },
    {
      ...contract.inventory.screens[1],
      children: contract.inventory.canonicalInstances.map((entry) => ({ ...entry })),
    },
  ],
};

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

const passing = verifyStructure(contract, page, flow, prototype, inventory);
assert.equal(passing.ok, true);
assert.equal(passing.canonical.screens.every((entry) => entry.status === 'conformant'), true);
assert.match(passing.digest, /^[a-f0-9]{64}$/);

const looseFlow = structuredClone(flow);
const firstPlaceholder = looseFlow.children[0].children[0].children[0].children[0].children[0].children[0];
firstPlaceholder.children = [{ id: 'legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: firstPlaceholder.children }];
const failing = verifyStructure(contract, page, looseFlow, prototype, inventory);
assert.equal(failing.ok, false);
assert.match(failing.errors.join('\n'), /direct parent must be FRAME Mobile Placeholder/);

const leftoverSlotFlow = structuredClone(flow);
const leftoverPlaceholder = leftoverSlotFlow.children[0].children[0].children[0].children[0].children[0].children[0];
leftoverPlaceholder.children.push({ id: 'empty-legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: [] });
const leftoverSlot = verifyStructure(contract, page, leftoverSlotFlow, prototype, inventory);
assert.equal(leftoverSlot.ok, false);
assert.match(leftoverSlot.errors.join('\n'), /contains forbidden legacy children/);

const wrongSpacingFlow = structuredClone(flow);
wrongSpacingFlow.children[0].children[0].children[0].children[0].children[0].itemSpacing = 200;
const wrongSpacing = verifyStructure(contract, page, wrongSpacingFlow, prototype, inventory);
assert.equal(wrongSpacing.ok, false);
assert.match(wrongSpacing.errors.join('\n'), /row 1 must use 24 item spacing/);

const nestedPrototype = structuredClone(prototype);
nestedPrototype.children = [{ id: 'wrapper', type: 'FRAME', name: 'Wrapper', children: nestedPrototype.children }];
const invalidPrototype = verifyStructure(contract, page, flow, nestedPrototype, inventory);
assert.equal(invalidPrototype.ok, false);
assert.match(invalidPrototype.errors.join('\n'), /direct child of prototype/);

const extraTopLevel = structuredClone(page);
extraTopLevel.children.push({ id: 'extra:1', type: 'FRAME', name: 'Loose screen' });
const scattered = verifyStructure(contract, extraTopLevel, flow, prototype, inventory);
assert.equal(scattered.ok, false);
assert.match(scattered.errors.join('\n'), /Page top level/);

const whiteInventory = structuredClone(inventory);
whiteInventory.fills[0].color = { r: 1, g: 1, b: 1 };
const wrongInventoryFill = verifyStructure(contract, page, flow, prototype, whiteInventory);
assert.equal(wrongInventoryFill.ok, false);
assert.match(wrongInventoryFill.errors.join('\n'), /inventory fill must be #F5F5F5/);

const placeholderAssets = structuredClone(inventory);
placeholderAssets.children[0].children[0].fills = [{ type: 'SOLID', color: { r: 0, g: 0, b: 1 } }];
placeholderAssets.children[0].children[1].children = [];
placeholderAssets.children[0].children.push({ id: '538:40', type: 'TEXT', name: 'logo-glyph' });
const invalidAssets = verifyStructure(contract, page, flow, prototype, placeholderAssets);
assert.equal(invalidAssets.ok, false);
assert.match(invalidAssets.errors.join('\n'), /IMAGE fill from the source asset/);
assert.match(invalidAssets.errors.join('\n'), /at least 4 vector paths/);
assert.match(invalidAssets.errors.join('\n'), /forbidden placeholder nodes/);

const loosePrivacySection = structuredClone(inventory);
loosePrivacySection.children[1].children[0].type = 'FRAME';
delete loosePrivacySection.children[1].children[0].componentId;
const invalidPrivacySection = verifyStructure(contract, page, flow, prototype, loosePrivacySection);
assert.equal(invalidPrivacySection.ok, false);
assert.match(invalidPrivacySection.errors.join('\n'), /canonical component 741:311/);

console.log('Figma structure verifier tests passed');
