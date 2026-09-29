import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { verifyStructure } from './verify-figma-structure.mjs';

const contract = JSON.parse(await readFile(new URL('./structure-contract.json', import.meta.url), 'utf8'));

const inventory = {
  id: contract.inventory.id,
  type: contract.inventory.type,
  name: contract.inventory.name,
  fills: [{ type: 'SOLID', visible: true, color: { r: 245 / 255, g: 245 / 255, b: 245 / 255 } }],
  children: contract.inventory.screens.map((screen, index) => ({
    ...screen,
    width: 402,
    height: 874,
    children: index === 0
      ? [
          { ...contract.inventory.assets.remAppIcon, fills: [{ type: 'IMAGE', visible: true, imageRef: 'source-raster' }] },
          {
            ...contract.inventory.assets.googleGlyph,
            children: Array.from({ length: contract.inventory.assets.googleGlyph.minimumVectorCount }, (_, vectorIndex) => ({ id: `google-vector-${vectorIndex}`, type: 'VECTOR', name: 'Vector' })),
          },
        ]
      : index === 1
        ? contract.inventory.canonicalInstances.map((entry) => ({ ...entry }))
        : [],
  })),
};

const screenComponents = {
  id: contract.screenComponents.id,
  type: contract.screenComponents.type,
  name: contract.screenComponents.name,
  children: contract.screenComponents.screens.map(({ width, height, ...entry }) => ({
    ...entry,
    absoluteBoundingBox: { x: 0, y: 0, width, height },
    children: [],
  })),
};

const expectedComponentQuality = contract.componentQuality.components[0];
const componentQuality = {
  id: expectedComponentQuality.id,
  type: expectedComponentQuality.type,
  name: expectedComponentQuality.name,
  children: expectedComponentQuality.checks.textStyleBindings.map((entry) => ({
    ...entry,
    type: 'TEXT',
    styles: { text: `style-${entry.id}` },
    children: [],
  })),
};

const screenInstance = (entry) => ({
  id: entry.node,
  type: 'INSTANCE',
  name: contract.screenComponents.screens.find(({ id }) => id === entry.componentId).name,
  componentId: entry.componentId,
  children: [],
});
const placeholder = (entry) => ({
  id: entry.placeholder,
  type: 'INSTANCE',
  name: contract.flow.hierarchy.placeholder,
  componentId: contract.flow.placeholderComponent,
  children: [{
    id: `screen-slot-${entry.node}`,
    type: 'SLOT',
    name: contract.flow.hierarchy.screenSlot,
    children: [screenInstance(entry)],
  }],
});
const stepChildren = contract.flow.screens.flatMap((entry, index) => index === contract.flow.screens.length - 1
  ? [placeholder(entry)]
  : [placeholder(entry), { id: `arrow-${index}`, type: 'INSTANCE', name: contract.flow.hierarchy.arrow, componentId: '769:176', children: [] }]);
const steps = {
  id: 'steps-slot',
  type: 'SLOT',
  name: contract.flow.hierarchy.steps,
  itemSpacing: contract.flow.hierarchy.stepSpacing,
  children: stepChildren,
};
const flow = {
  id: contract.flow.id,
  type: 'SECTION',
  name: '01A · Consent · Documentation',
  children: [{
    ...contract.flow.documentInstance,
    children: [{
      id: 'mobile-flow', type: 'INSTANCE', name: 'Mobile Flow', children: [{
        id: 'placeholder-sections', type: 'INSTANCE', name: contract.flow.hierarchy.sections, children: [{
          id: 'sections-slot', type: 'SLOT', name: contract.flow.hierarchy.sectionsSlot, children: [{
            id: 'placeholder-section', type: 'INSTANCE', name: contract.flow.hierarchy.section, children: [{
              id: 'section-rows-slot', type: 'SLOT', name: contract.flow.hierarchy.rowsSlot, children: [{
                id: 'placeholder-rows', type: 'INSTANCE', name: contract.flow.hierarchy.rows, children: [{
                  id: 'row-slot', type: 'SLOT', name: contract.flow.hierarchy.rowsSlot, children: [{
                    id: 'placeholder-flow', type: 'INSTANCE', name: contract.flow.hierarchy.row, children: [steps],
                  }],
                }],
              }],
            }],
          }],
        }],
      }],
    }],
  }],
};

const prototypeNode = (entry) => {
  const actions = [
    ...entry.destinations.map((destinationId) => ({ type: 'NODE', destinationId, navigation: 'NAVIGATE', transition: null })),
    ...Array.from({ length: entry.backCount }, () => ({ type: 'BACK' })),
  ];
  return {
    id: entry.node,
    type: entry.type,
    name: entry.name,
    componentId: entry.componentId,
    children: actions.map((action, index) => ({
      id: `${entry.node}-action-${index}`,
      type: 'FRAME',
      name: action.type === 'BACK' ? (index === 0 ? 'Scrim' : 'Done') : 'ListRow',
      interactions: [{ trigger: { type: entry.trigger }, actions: [action] }],
    })),
  };
};
const prototype = {
  ...contract.flow.prototype.root,
  children: [
    { ...contract.flow.prototype.label, type: 'TEXT' },
    ...contract.flow.prototype.frames.map(prototypeNode),
  ],
};
const page = {
  id: contract.page.id,
  type: 'CANVAS',
  name: contract.page.name,
  children: contract.page.topLevel,
  flowStartingPoints: structuredClone(contract.flow.prototype.flowStartingPoints),
};

const componentQualityDocuments = (componentValue = componentQuality) => ({ [expectedComponentQuality.id]: componentValue });
const verify = (pageValue = page, flowValue = flow, prototypeValue = prototype, inventoryValue = inventory, screenValue = screenComponents, componentValue = componentQuality) =>
  verifyStructure(contract, pageValue, flowValue, prototypeValue, inventoryValue, screenValue, componentQualityDocuments(componentValue));
const findNode = (root, predicate) => {
  if (predicate(root)) return root;
  for (const child of root.children || []) {
    const match = findNode(child, predicate);
    if (match) return match;
  }
  return null;
};

const passing = verify();
assert.equal(passing.ok, true, passing.errors.join('\n'));
assert.equal(passing.canonical.screens.every((entry) => entry.status === 'conformant'), true);
assert.equal(passing.canonical.canonicalScreens.every((entry) => entry.status === 'conformant'), true);
assert.deepEqual(passing.canonical.prototype.flowStartingPoints, contract.flow.prototype.flowStartingPoints);
assert.deepEqual(passing.canonical.prototype.frames.map(({ destinations, backCount }) => ({ destinations, backCount })), [
  { destinations: ['781:637', '781:690'], backCount: 0 },
  { destinations: [], backCount: 2 },
  { destinations: [], backCount: 2 },
]);
assert.match(passing.digest, /^[a-f0-9]{64}$/);

const wrongFlowStart = structuredClone(page);
wrongFlowStart.flowStartingPoints[0].nodeId = '781:637';
const invalidFlowStart = verify(wrongFlowStart);
assert.equal(invalidFlowStart.ok, false);
assert.match(invalidFlowStart.errors.join('\n'), /flow starting points/);

const wrongDestination = structuredClone(prototype);
findNode(wrongDestination, ({ id }) => id === '781:596-action-0')
  .interactions[0].actions[0].destinationId = '781:690';
const invalidDestination = verify(page, flow, wrongDestination);
assert.equal(invalidDestination.ok, false);
assert.match(invalidDestination.errors.join('\n'), /navigation destinations/);
assert.deepEqual(invalidDestination.canonical.prototype.frames[0].destinations, ['781:690', '781:690']);

const wrongBackTrigger = structuredClone(prototype);
findNode(wrongBackTrigger, ({ id }) => id === '781:637-action-0')
  .interactions[0].trigger.type = 'ON_HOVER';
const invalidBackTrigger = verify(page, flow, wrongBackTrigger);
assert.equal(invalidBackTrigger.ok, false);
assert.match(invalidBackTrigger.errors.join('\n'), /interactions must use ON_CLICK/);

const missingBack = structuredClone(prototype);
findNode(missingBack, ({ id }) => id === '781:690').children.pop();
const invalidBackCount = verify(page, flow, missingBack);
assert.equal(invalidBackCount.ok, false);
assert.match(invalidBackCount.errors.join('\n'), /back action count must be 2; received 1/);

const looseFlow = structuredClone(flow);
const firstScreenSlot = findNode(looseFlow, ({ name }) => name === contract.flow.hierarchy.screenSlot);
firstScreenSlot.children = [{ id: 'legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: firstScreenSlot.children }];
const loose = verify(page, looseFlow);
assert.equal(loose.ok, false);
assert.match(loose.errors.join('\n'), /screen slot must be SLOT Screen/);

const substitutedFlowScreen = structuredClone(flow);
const exactFirstScreen = findNode(substitutedFlowScreen, ({ id }) => id === contract.flow.screens[0].node);
exactFirstScreen.id = 'replacement-with-same-component';
const invalidSubstitutedFlowScreen = verify(page, substitutedFlowScreen);
assert.equal(invalidSubstitutedFlowScreen.ok, false);
assert.ok(
  invalidSubstitutedFlowScreen.errors.some((error) =>
    error.includes(`exact contracted node ${contract.flow.screens[0].node} is missing`)),
);

const invalidWaypointRoleContract = structuredClone(contract);
const waypoint = invalidWaypointRoleContract.flow.screens.find(({ role }) => role === 'branch-return-navigation-waypoint');
waypoint.evidence = true;
const invalidWaypointRole = verifyStructure(
  invalidWaypointRoleContract,
  page,
  flow,
  prototype,
  inventory,
  screenComponents,
  componentQualityDocuments(),
);
assert.equal(invalidWaypointRole.ok, false);
assert.match(invalidWaypointRole.errors.join('\n'), /branch-return navigation waypoint must not be evidence/);

const invalidWaypointTargetContract = structuredClone(contract);
invalidWaypointTargetContract.flow.screens.find(
  ({ role }) => role === 'branch-return-navigation-waypoint',
).navigationTarget = invalidWaypointTargetContract.flow.screens[0].node;
const invalidWaypointTarget = verifyStructure(
  invalidWaypointTargetContract,
  page,
  flow,
  prototype,
  inventory,
  screenComponents,
  componentQualityDocuments(),
);
assert.equal(invalidWaypointTarget.ok, false);
assert.match(invalidWaypointTarget.errors.join('\n'), /branch-return navigation waypoint must target the next contracted screen/);

const leftoverFlow = structuredClone(flow);
const firstPlaceholder = findNode(leftoverFlow, ({ id }) => id === contract.flow.screens[0].placeholder);
firstPlaceholder.children.push({ id: 'empty-legacy-slot', type: 'FRAME', name: 'Device / Screen slot', children: [] });
const leftover = verify(page, leftoverFlow);
assert.equal(leftover.ok, false);
assert.match(leftover.errors.join('\n'), /forbidden legacy children/);

const wrongSpacingFlow = structuredClone(flow);
findNode(wrongSpacingFlow, ({ name, type }) => name === contract.flow.hierarchy.steps && type === 'SLOT').itemSpacing = 200;
const wrongSpacing = verify(page, wrongSpacingFlow);
assert.equal(wrongSpacing.ok, false);
assert.match(wrongSpacing.errors.join('\n'), /Steps must use 24 item spacing/);

const nestedPrototype = structuredClone(prototype);
nestedPrototype.children = [{ id: 'wrapper', type: 'FRAME', name: 'Wrapper', children: nestedPrototype.children }];
const invalidPrototype = verify(page, flow, nestedPrototype);
assert.equal(invalidPrototype.ok, false);
assert.match(invalidPrototype.errors.join('\n'), /direct INSTANCE child of prototype/);

const wrongPrototypeSource = structuredClone(prototype);
wrongPrototypeSource.children.find(({ id }) => id === '781:596').componentId = 'wrong:component';
const invalidPrototypeSource = verify(page, flow, wrongPrototypeSource);
assert.equal(invalidPrototypeSource.ok, false);
assert.match(invalidPrototypeSource.errors.join('\n'), /direct INSTANCE child of prototype/);

const extraTopLevel = structuredClone(page);
extraTopLevel.children.push({ id: 'extra:1', type: 'FRAME', name: 'Loose screen' });
const scattered = verify(extraTopLevel);
assert.equal(scattered.ok, false);
assert.match(scattered.errors.join('\n'), /Page top level/);

const wrongScreenSize = structuredClone(screenComponents);
wrongScreenSize.children[0].absoluteBoundingBox.width = 428;
const invalidScreen = verify(page, flow, prototype, inventory, wrongScreenSize);
assert.equal(invalidScreen.ok, false);
assert.match(invalidScreen.errors.join('\n'), /Canonical screen must be/);

const whiteInventory = structuredClone(inventory);
whiteInventory.fills[0].color = { r: 1, g: 1, b: 1 };
const wrongInventoryFill = verify(page, flow, prototype, whiteInventory);
assert.equal(wrongInventoryFill.ok, false);
assert.match(wrongInventoryFill.errors.join('\n'), /inventory fill must be #F5F5F5/);

const placeholderAssets = structuredClone(inventory);
placeholderAssets.children[0].children[0].fills = [{ type: 'SOLID', color: { r: 0, g: 0, b: 1 } }];
placeholderAssets.children[0].children[1].children = [];
placeholderAssets.children[0].children.push({ id: '538:40', type: 'TEXT', name: 'logo-glyph' });
const invalidAssets = verify(page, flow, prototype, placeholderAssets);
assert.equal(invalidAssets.ok, false);
assert.match(invalidAssets.errors.join('\n'), /IMAGE fill from the source asset/);
assert.match(invalidAssets.errors.join('\n'), /at least 4 vector paths/);
assert.match(invalidAssets.errors.join('\n'), /forbidden placeholder nodes/);

const loosePrivacySection = structuredClone(inventory);
loosePrivacySection.children[1].children[0].type = 'FRAME';
delete loosePrivacySection.children[1].children[0].componentId;
const invalidPrivacySection = verify(page, flow, prototype, loosePrivacySection);
assert.equal(invalidPrivacySection.ok, false);
assert.match(invalidPrivacySection.errors.join('\n'), /canonical component 741:311/);

const unboundComponentText = structuredClone(componentQuality);
delete unboundComponentText.children[0].styles.text;
const invalidComponentStyle = verify(page, flow, prototype, inventory, screenComponents, unboundComponentText);
assert.equal(invalidComponentStyle.ok, false);
assert.match(invalidComponentStyle.errors.join('\n'), /must bind a local text style/);

console.log('Figma structure verifier tests passed');
