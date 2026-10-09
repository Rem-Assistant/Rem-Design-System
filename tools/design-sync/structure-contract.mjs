const ROOT_NODE_ID = /^\d+:\d+$/;
const FIGMA_NODE_ID = /^(?:I)?\d+:\d+(?:;\d+:\d+)*$/;
const FILE_KEY = /^[A-Za-z0-9_-]{8,128}$/;

function object(value, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${label} must be an object`);
  }
  return value;
}

function string(value, label, { pattern, max = 500 } = {}) {
  if (typeof value !== 'string' || value.length === 0 || value.length > max || (pattern && !pattern.test(value))) {
    throw new Error(`${label} is invalid`);
  }
  return value;
}

function array(value, label, { min = 0, max = 100 } = {}) {
  if (!Array.isArray(value) || value.length < min || value.length > max) {
    throw new Error(`${label} must contain ${min}-${max} entries`);
  }
  return value;
}

function node(value, label, root = false) {
  return string(value, label, { pattern: root ? ROOT_NODE_ID : FIGMA_NODE_ID, max: 500 });
}

/**
 * Validate untrusted, declarative PR-head contract data before trusted code uses it to build
 * Figma API requests. This deliberately checks bounds and the fields consumed by the verifier;
 * the data is never imported or executed.
 */
export function validateStructureContract(contract, { expectedFileKey } = {}) {
  object(contract, 'structure contract');
  if (!Number.isInteger(contract.version) || contract.version < 1 || contract.version > 1_000_000) {
    throw new Error('structure contract version is invalid');
  }
  const fileKey = string(contract.fileKey, 'structure contract fileKey', { pattern: FILE_KEY, max: 128 });
  if (expectedFileKey !== undefined && fileKey !== expectedFileKey) {
    throw new Error('structure contract targets an unexpected Figma file');
  }

  const page = object(contract.page, 'page');
  node(page.id, 'page.id', true);
  string(page.name, 'page.name');
  array(page.topLevel, 'page.topLevel', { min: 1, max: 50 }).forEach((entry, index) => {
    object(entry, `page.topLevel[${index}]`);
    node(entry.id, `page.topLevel[${index}].id`, true);
    string(entry.type, `page.topLevel[${index}].type`, { max: 100 });
    string(entry.name, `page.topLevel[${index}].name`);
  });

  const inventory = object(contract.inventory, 'inventory');
  node(inventory.id, 'inventory.id', true);
  string(inventory.type, 'inventory.type', { max: 100 });
  string(inventory.name, 'inventory.name');
  string(inventory.fill, 'inventory.fill', { pattern: /^#[0-9A-F]{6}$/, max: 7 });
  array(inventory.screens, 'inventory.screens', { min: 1, max: 50 }).forEach((entry, index) => {
    object(entry, `inventory.screens[${index}]`);
    node(entry.id, `inventory.screens[${index}].id`, true);
    string(entry.type, `inventory.screens[${index}].type`, { max: 100 });
    string(entry.name, `inventory.screens[${index}].name`);
  });
  object(inventory.assets, 'inventory.assets');
  if (Object.keys(inventory.assets).length > 100) throw new Error('inventory.assets has too many entries');
  for (const [key, entry] of Object.entries(inventory.assets)) {
    object(entry, `inventory.assets.${key}`);
    node(entry.id, `inventory.assets.${key}.id`, true);
    string(entry.type, `inventory.assets.${key}.type`, { max: 100 });
    string(entry.name, `inventory.assets.${key}.name`);
  }
  array(inventory.canonicalInstances, 'inventory.canonicalInstances', { max: 100 }).forEach((entry, index) => {
    object(entry, `inventory.canonicalInstances[${index}]`);
    node(entry.id, `inventory.canonicalInstances[${index}].id`, true);
    node(entry.componentId, `inventory.canonicalInstances[${index}].componentId`, true);
    string(entry.type, `inventory.canonicalInstances[${index}].type`, { max: 100 });
    string(entry.name, `inventory.canonicalInstances[${index}].name`);
  });
  array(inventory.forbiddenNodeIds, 'inventory.forbiddenNodeIds', { max: 100 })
    .forEach((id, index) => node(id, `inventory.forbiddenNodeIds[${index}]`, true));

  const screenComponents = object(contract.screenComponents, 'screenComponents');
  node(screenComponents.id, 'screenComponents.id', true);
  string(screenComponents.type, 'screenComponents.type', { max: 100 });
  string(screenComponents.name, 'screenComponents.name');
  array(screenComponents.rootOrder, 'screenComponents.rootOrder', { min: 1, max: 20 })
    .forEach((entry, index) => string(entry, `screenComponents.rootOrder[${index}]`, { max: 100 }));
  array(screenComponents.hierarchyScreenIds, 'screenComponents.hierarchyScreenIds', { min: 1, max: 50 })
    .forEach((entry, index) => node(entry, `screenComponents.hierarchyScreenIds[${index}]`, true));
  const screenBody = object(screenComponents.body, 'screenComponents.body');
  if (!Number.isFinite(screenBody.padding) || screenBody.padding < 0 || screenBody.padding > 1_000) {
    throw new Error('screenComponents.body.padding is invalid');
  }
  string(screenBody.layoutSizingHorizontal, 'screenComponents.body.layoutSizingHorizontal', { max: 100 });
  string(screenBody.layoutSizingVertical, 'screenComponents.body.layoutSizingVertical', { max: 100 });
  string(screenBody.primaryAxisAlignItems, 'screenComponents.body.primaryAxisAlignItems', { max: 100 });
  array(screenBody.children, 'screenComponents.body.children', { min: 1, max: 20 })
    .forEach((entry, index) => string(entry, `screenComponents.body.children[${index}]`, { max: 100 }));
  if (screenBody.actionAreaOwnsOuterInset !== false) {
    throw new Error('screenComponents.body.actionAreaOwnsOuterInset must be false');
  }
  string(screenBody.actionAreaName, 'screenComponents.body.actionAreaName', { max: 100 });
  array(screenComponents.screens, 'screenComponents.screens', { min: 1, max: 50 }).forEach((entry, index) => {
    object(entry, `screenComponents.screens[${index}]`);
    node(entry.id, `screenComponents.screens[${index}].id`, true);
    string(entry.type, `screenComponents.screens[${index}].type`, { max: 100 });
    string(entry.name, `screenComponents.screens[${index}].name`);
    if (!Number.isFinite(entry.width) || !Number.isFinite(entry.height) || entry.width <= 0 || entry.height <= 0) {
      throw new Error(`screenComponents.screens[${index}] dimensions are invalid`);
    }
  });

  const quality = object(contract.componentQuality, 'componentQuality');
  array(quality.dimensions, 'componentQuality.dimensions', { max: 50 })
    .forEach((entry, index) => string(entry, `componentQuality.dimensions[${index}]`, { max: 100 }));
  array(quality.components, 'componentQuality.components', { min: 1, max: 50 }).forEach((entry, index) => {
    object(entry, `componentQuality.components[${index}]`);
    node(entry.id, `componentQuality.components[${index}].id`, true);
    string(entry.type, `componentQuality.components[${index}].type`, { max: 100 });
    string(entry.name, `componentQuality.components[${index}].name`);
    string(entry.category, `componentQuality.components[${index}].category`, { max: 100 });
    const checks = object(entry.checks, `componentQuality.components[${index}].checks`);
    array(checks.textStyleBindings ?? [], `componentQuality.components[${index}].checks.textStyleBindings`, { max: 100 })
      .forEach((binding, bindingIndex) => {
        object(binding, `textStyleBindings[${bindingIndex}]`);
        node(binding.id, `textStyleBindings[${bindingIndex}].id`, true);
        string(binding.name, `textStyleBindings[${bindingIndex}].name`);
      });
    array(checks.variableBindings ?? [], `componentQuality.components[${index}].checks.variableBindings`, { max: 100 })
      .forEach((binding, bindingIndex) => {
        object(binding, `variableBindings[${bindingIndex}]`);
        node(binding.id, `variableBindings[${bindingIndex}].id`, true);
        string(binding.name, `variableBindings[${bindingIndex}].name`);
      });
  });

  const flow = object(contract.flow, 'flow');
  node(flow.id, 'flow.id', true);
  node(flow.templateSource, 'flow.templateSource', true);
  node(flow.placeholderComponent, 'flow.placeholderComponent', true);
  const documentInstance = object(flow.documentInstance, 'flow.documentInstance');
  node(documentInstance.id, 'flow.documentInstance.id', true);
  node(documentInstance.componentId, 'flow.documentInstance.componentId', true);
  string(documentInstance.type, 'flow.documentInstance.type', { max: 100 });
  string(documentInstance.name, 'flow.documentInstance.name');
  const hierarchy = object(flow.hierarchy, 'flow.hierarchy');
  for (const key of ['sections', 'sectionsSlot', 'section', 'rows', 'rowsSlot', 'row', 'steps', 'placeholder', 'screenSlot', 'arrow']) {
    string(hierarchy[key], `flow.hierarchy.${key}`);
  }
  array(hierarchy.forbiddenPlaceholderChildren, 'flow.hierarchy.forbiddenPlaceholderChildren', { max: 50 })
    .forEach((entry, index) => string(entry, `flow.hierarchy.forbiddenPlaceholderChildren[${index}]`));
  array(hierarchy.stepSequence, 'flow.hierarchy.stepSequence', { min: 1, max: 100 })
    .forEach((entry, index) => string(entry, `flow.hierarchy.stepSequence[${index}]`));
  if (!Number.isFinite(hierarchy.stepSpacing) || hierarchy.stepSpacing < 0 || hierarchy.stepSpacing > 10_000) {
    throw new Error('flow.hierarchy.stepSpacing is invalid');
  }
  array(flow.screens, 'flow.screens', { min: 1, max: 50 }).forEach((entry, index) => {
    object(entry, `flow.screens[${index}]`);
    string(entry.name, `flow.screens[${index}].name`);
    node(entry.node, `flow.screens[${index}].node`);
    node(entry.componentId, `flow.screens[${index}].componentId`, true);
    node(entry.placeholder, `flow.screens[${index}].placeholder`);
    string(entry.role, `flow.screens[${index}].role`, { max: 100 });
    if (entry.navigationTarget !== undefined) node(entry.navigationTarget, `flow.screens[${index}].navigationTarget`);
  });
  const prototype = object(flow.prototype, 'flow.prototype');
  const prototypeRoot = object(prototype.root, 'flow.prototype.root');
  node(prototypeRoot.id, 'flow.prototype.root.id', true);
  string(prototypeRoot.type, 'flow.prototype.root.type', { max: 100 });
  string(prototypeRoot.name, 'flow.prototype.root.name');
  const prototypeLabel = object(prototype.label, 'flow.prototype.label');
  node(prototypeLabel.id, 'flow.prototype.label.id', true);
  string(prototypeLabel.name, 'flow.prototype.label.name');
  array(prototype.flowStartingPoints, 'flow.prototype.flowStartingPoints', { min: 1, max: 50 }).forEach((entry, index) => {
    object(entry, `flow.prototype.flowStartingPoints[${index}]`);
    node(entry.nodeId, `flow.prototype.flowStartingPoints[${index}].nodeId`, true);
    string(entry.name, `flow.prototype.flowStartingPoints[${index}].name`);
  });
  array(prototype.frames, 'flow.prototype.frames', { min: 1, max: 50 }).forEach((entry, index) => {
    object(entry, `flow.prototype.frames[${index}]`);
    string(entry.name, `flow.prototype.frames[${index}].name`);
    node(entry.node, `flow.prototype.frames[${index}].node`, true);
    node(entry.componentId, `flow.prototype.frames[${index}].componentId`, true);
    string(entry.type, `flow.prototype.frames[${index}].type`, { max: 100 });
    string(entry.trigger, `flow.prototype.frames[${index}].trigger`, { pattern: /^[A-Z][A-Z0-9_]*$/, max: 100 });
    array(entry.destinations, `flow.prototype.frames[${index}].destinations`, { max: 50 })
      .forEach((id, destinationIndex) => node(id, `flow.prototype.frames[${index}].destinations[${destinationIndex}]`, true));
    if (!Number.isInteger(entry.backCount) || entry.backCount < 0 || entry.backCount > 100) {
      throw new Error(`flow.prototype.frames[${index}].backCount is invalid`);
    }
  });

  return contract;
}

export function validateManifestData(manifest, { expectedFileKey } = {}) {
  object(manifest, 'manifest');
  const fileKey = string(manifest.figmaFileKey, 'manifest.figmaFileKey', { pattern: FILE_KEY, max: 128 });
  if (expectedFileKey !== undefined && fileKey !== expectedFileKey) {
    throw new Error('manifest targets an unexpected Figma file');
  }
  for (const field of ['components', 'screens']) {
    array(manifest[field], `manifest.${field}`, { max: 500 }).forEach((entry, index) => {
      object(entry, `manifest.${field}[${index}]`);
      string(entry.name, `manifest.${field}[${index}].name`);
      node(entry.node, `manifest.${field}[${index}].node`, true);
    });
  }
  return manifest;
}
