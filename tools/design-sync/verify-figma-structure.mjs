#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { figma } from './lib.mjs';
import { validateStructureContract } from './structure-contract.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));

function walk(node, ancestors = [], found = new Map()) {
  found.set(node.id, { node, ancestors });
  for (const child of node.children || []) walk(child, [node, ...ancestors], found);
  return found;
}

function colorToHex(color) {
  if (!color) return null;
  const channel = (value) => Math.round(value * 255).toString(16).padStart(2, '0').toUpperCase();
  return `#${channel(color.r)}${channel(color.g)}${channel(color.b)}`;
}

function observedInteractions(root) {
  const navigations = [];
  const backActions = [];
  const unsupportedActions = [];
  for (const { node } of walk(root).values()) {
    if (node.interactions === undefined) continue;
    if (!Array.isArray(node.interactions)) {
      unsupportedActions.push({ sourceNode: node.id, reason: 'interactions is not an array' });
      continue;
    }
    for (const interaction of node.interactions) {
      const trigger = interaction?.trigger?.type || null;
      if (!Array.isArray(interaction?.actions) || interaction.actions.length === 0) {
        unsupportedActions.push({ sourceNode: node.id, trigger, reason: 'interaction has no actions' });
        continue;
      }
      for (const action of interaction.actions) {
        const observed = { sourceNode: node.id, sourceName: node.name || '', trigger };
        if (action?.type === 'NODE' && action.navigation === 'NAVIGATE' && typeof action.destinationId === 'string') {
          navigations.push({ ...observed, destinationId: action.destinationId });
        } else if (action?.type === 'BACK') {
          backActions.push(observed);
        } else {
          unsupportedActions.push({ ...observed, actionType: action?.type || null, navigation: action?.navigation || null });
        }
      }
    }
  }
  const bySource = (left, right) => JSON.stringify(left).localeCompare(JSON.stringify(right));
  return {
    navigations: navigations.sort(bySource),
    backActions: backActions.sort(bySource),
    unsupportedActions: unsupportedActions.sort(bySource),
  };
}

export function verifyStructure(contract, pageDocument, flowDocument, prototypeDocument, inventoryDocument, screenComponentsDocument, componentQualityDocuments) {
  const errors = [];
  const expectedTop = contract.page.topLevel;
  const actualTop = (pageDocument.children || []).map(({ id, type, name }) => ({ id, type, name }));
  if (JSON.stringify(actualTop) !== JSON.stringify(expectedTop)) {
    errors.push(`Page top level must be ${JSON.stringify(expectedTop)}; received ${JSON.stringify(actualTop)}`);
  }

  const expectedInventory = contract.inventory;
  const inventory = inventoryDocument || { id: 'missing-inventory', children: [] };
  if (inventory.id !== expectedInventory.id || inventory.type !== expectedInventory.type || inventory.name !== expectedInventory.name) {
    errors.push(`Canonical inventory must be ${JSON.stringify({ id: expectedInventory.id, type: expectedInventory.type, name: expectedInventory.name })}`);
  }
  const inventoryFill = (inventory.fills || []).find(({ type, visible }) => type === 'SOLID' && visible !== false);
  const actualInventoryFill = colorToHex(inventoryFill?.color);
  if (actualInventoryFill !== expectedInventory.fill) {
    errors.push(`Canonical inventory fill must be ${expectedInventory.fill}; received ${actualInventoryFill}`);
  }
  const inventoryIndex = walk(inventory);
  const actualInventoryScreens = (inventory.children || []).map(({ id, type, name }) => ({ id, type, name }));
  if (JSON.stringify(actualInventoryScreens) !== JSON.stringify(expectedInventory.screens)) {
    errors.push(`Canonical inventory screens must be ${JSON.stringify(expectedInventory.screens)}; received ${JSON.stringify(actualInventoryScreens)}`);
  }
  for (const expected of Object.values(expectedInventory.assets)) {
    const match = inventoryIndex.get(expected.id)?.node;
    if (!match || match.type !== expected.type || match.name !== expected.name) {
      errors.push(`Canonical inventory asset must be ${JSON.stringify({ id: expected.id, type: expected.type, name: expected.name })}`);
      continue;
    }
    if (expected.imageFill && !(match.fills || []).some(({ type, visible }) => type === 'IMAGE' && visible !== false)) {
      errors.push(`${expected.name} must use an IMAGE fill from the source asset`);
    }
    if (expected.minimumVectorCount) {
      const vectorCount = [...walk(match).values()].filter(({ node }) => node.type === 'VECTOR').length;
      if (vectorCount < expected.minimumVectorCount) {
        errors.push(`${expected.name} must contain at least ${expected.minimumVectorCount} vector paths; received ${vectorCount}`);
      }
    }
  }
  for (const expected of expectedInventory.canonicalInstances) {
    const match = inventoryIndex.get(expected.id)?.node;
    if (!match || match.type !== expected.type || match.name !== expected.name || match.componentId !== expected.componentId) {
      errors.push(`${expected.name} must be INSTANCE ${expected.id} of canonical component ${expected.componentId}`);
    }
  }
  const forbiddenInventoryNodes = expectedInventory.forbiddenNodeIds.filter((id) => inventoryIndex.has(id));
  if (forbiddenInventoryNodes.length) {
    errors.push(`Canonical inventory contains forbidden placeholder nodes: ${forbiddenInventoryNodes.join(', ')}`);
  }

  const expectedScreenRoot = contract.screenComponents;
  const screenRoot = screenComponentsDocument || { id: 'missing-screen-components', children: [] };
  if (screenRoot.id !== expectedScreenRoot.id || screenRoot.type !== expectedScreenRoot.type || screenRoot.name !== expectedScreenRoot.name) {
    errors.push(`Canonical consent screen root must be ${JSON.stringify({ id: expectedScreenRoot.id, type: expectedScreenRoot.type, name: expectedScreenRoot.name })}`);
  }
  const screenRootIndex = walk(screenRoot);
  const canonicalScreens = expectedScreenRoot.screens.map((expected) => {
    const match = screenRootIndex.get(expected.id)?.node;
    const bounds = match?.absoluteBoundingBox;
    const width = match?.width ?? bounds?.width;
    const height = match?.height ?? bounds?.height;
    const conforms = Boolean(match && match.type === expected.type && match.name === expected.name &&
      width === expected.width && height === expected.height);
    if (!conforms) errors.push(`Canonical screen must be ${JSON.stringify(expected)}`);
    return { ...expected, status: conforms ? 'conformant' : 'invalid' };
  });

  const componentQuality = contract.componentQuality.components.map((expectedComponent) => {
    const component = componentQualityDocuments?.[expectedComponent.id] || { id: `missing-${expectedComponent.id}`, children: [] };
    if (component.id !== expectedComponent.id || component.type !== expectedComponent.type || component.name !== expectedComponent.name) {
      errors.push(`Component quality root must be ${JSON.stringify({ id: expectedComponent.id, type: expectedComponent.type, name: expectedComponent.name })}`);
    }
    const componentIndex = walk(component);
    const textStyleBindings = (expectedComponent.checks?.textStyleBindings || []).map((expected) => {
      const match = componentIndex.get(expected.id)?.node;
      const styleId = match?.styles?.text || null;
      const conforms = Boolean(match && match.type === 'TEXT' && match.name === expected.name && styleId);
      if (!conforms) errors.push(`${expectedComponent.name} text ${expected.id} (${expected.name}) must bind a local text style`);
      return { ...expected, styleId, status: conforms ? 'conformant' : 'invalid' };
    });
    const checks = [...textStyleBindings];
    const passed = checks.filter(({ status }) => status === 'conformant').length;
    return {
      id: component.id,
      name: component.name,
      category: expectedComponent.category,
      verifiedDimensions: textStyleBindings.length ? ['typography'] : [],
      score: { passed, total: checks.length },
      textStyleBindings,
    };
  });

  if (flowDocument.id !== contract.flow.id) {
    errors.push(`Flow root must be ${contract.flow.id}; received ${flowDocument.id}`);
  }

  const index = walk(flowDocument);
  const hierarchy = contract.flow.hierarchy;
  const expectedDocument = contract.flow.documentInstance;
  const documentMatch = index.get(expectedDocument.id);
  const documentConforms = Boolean(documentMatch && documentMatch.node.type === expectedDocument.type &&
    documentMatch.node.name === expectedDocument.name && documentMatch.node.componentId === expectedDocument.componentId &&
    documentMatch.ancestors[0]?.id === contract.flow.id);
  if (!documentConforms) errors.push(`Flow must contain direct canonical documentation instance ${JSON.stringify(expectedDocument)}`);

  const stepSlots = [...index.values()].filter(({ node }) => node.type === 'SLOT' && node.name === hierarchy.steps);
  if (stepSlots.length !== 1) errors.push(`Flow must contain exactly one ${hierarchy.steps} slot; received ${stepSlots.length}`);
  const steps = stepSlots[0]?.node;
  const visibleSteps = (steps?.children || []).filter(({ visible }) => visible !== false);
  const actualSequence = visibleSteps.map(({ name }) => name);
  if (JSON.stringify(actualSequence) !== JSON.stringify(hierarchy.stepSequence)) {
    errors.push(`${hierarchy.steps} must be ${JSON.stringify(hierarchy.stepSequence)}; received ${JSON.stringify(actualSequence)}`);
  }
  if (steps?.itemSpacing !== hierarchy.stepSpacing) {
    errors.push(`${hierarchy.steps} must use ${hierarchy.stepSpacing} item spacing; received ${steps?.itemSpacing}`);
  }

  const expectedPlaceholderSequence = contract.flow.screens.map(({ placeholder }) => placeholder);
  const actualPlaceholderSequence = visibleSteps
    .filter(({ type, name }) => type === 'INSTANCE' && name === hierarchy.placeholder)
    .map(({ id }) => id);
  if (JSON.stringify(actualPlaceholderSequence) !== JSON.stringify(expectedPlaceholderSequence)) {
    errors.push(`${hierarchy.steps} placeholder ids must be ${JSON.stringify(expectedPlaceholderSequence)}; received ${JSON.stringify(actualPlaceholderSequence)}`);
  }

  const allowedScreenRoles = new Set(['evidence-state', 'branch-return-navigation-waypoint']);
  for (const [screenIndex, expected] of contract.flow.screens.entries()) {
    if (!allowedScreenRoles.has(expected.role)) {
      errors.push(`${expected.name} must declare a supported flow role`);
      continue;
    }
    if (expected.role === 'evidence-state' && expected.evidence !== true) {
      errors.push(`${expected.name} evidence-state must be marked as evidence`);
    }
    if (expected.role === 'branch-return-navigation-waypoint') {
      if (expected.evidence !== false) {
        errors.push(`${expected.name} branch-return navigation waypoint must not be evidence`);
      }
      const nextScreen = contract.flow.screens[screenIndex + 1];
      if (!expected.navigationTarget || expected.navigationTarget !== nextScreen?.node) {
        errors.push(`${expected.name} branch-return navigation waypoint must target the next contracted screen`);
      }
    }
  }

  const screens = contract.flow.screens.map((expected) => {
    const match = index.get(expected.node);
    if (!match) {
      errors.push(`${expected.name} exact contracted node ${expected.node} is missing from flow ${contract.flow.id}`);
      return { ...expected, status: 'missing' };
    }

    const [screenSlot, placeholder, stepSlot, row, rowSlot, rows, rowsSlot, section, sectionsSlot, sections, mobileFlow, document] = match.ancestors;
    const checks = [
      [screenSlot, 'SLOT', hierarchy.screenSlot, 'screen slot'],
      [placeholder, 'INSTANCE', hierarchy.placeholder, 'placeholder'],
      [stepSlot, 'SLOT', hierarchy.steps, 'steps slot'],
      [row, 'INSTANCE', hierarchy.row, 'row'],
      [rowSlot, 'SLOT', hierarchy.rowsSlot, 'row slot'],
      [rows, 'INSTANCE', hierarchy.rows, 'row collection'],
      [rowsSlot, 'SLOT', hierarchy.rowsSlot, 'section rows slot'],
      [section, 'INSTANCE', hierarchy.section, 'section'],
      [sectionsSlot, 'SLOT', hierarchy.sectionsSlot, 'sections slot'],
      [sections, 'INSTANCE', hierarchy.sections, 'section collection'],
      [mobileFlow, 'INSTANCE', 'Mobile Flow', 'mobile flow'],
      [document, 'INSTANCE', expectedDocument.name, 'documentation shell'],
    ];
    for (const [node, requiredType, requiredName, relationship] of checks) {
      if (!node || node.type !== requiredType || node.name !== requiredName) {
        errors.push(`${expected.name} ${relationship} must be ${requiredType} ${requiredName}`);
      }
    }
    if (match.node.type !== 'INSTANCE' || match.node.componentId !== expected.componentId) {
      errors.push(`${expected.name} must be INSTANCE ${expected.node} of canonical screen ${expected.componentId}`);
    }
    if (placeholder?.componentId !== contract.flow.placeholderComponent) {
      errors.push(`${expected.name} must use Mobile Placeholder component ${contract.flow.placeholderComponent}`);
    }
    if ((screenSlot?.children || []).filter(({ visible }) => visible !== false).length !== 1) {
      errors.push(`${expected.name} Screen slot must contain exactly one visible canonical screen instance`);
    }
    const forbiddenChildren = (placeholder?.children || [])
      .filter(({ name }) => hierarchy.forbiddenPlaceholderChildren.includes(name));
    if (forbiddenChildren.length) {
      errors.push(`${expected.name} placeholder contains forbidden legacy children: ${forbiddenChildren.map(({ name }) => name).join(', ')}`);
    }
    const conformant = checks.every(([node, requiredType, requiredName]) =>
      node?.type === requiredType && node.name === requiredName) &&
      match.node.type === 'INSTANCE' && match.node.componentId === expected.componentId &&
      placeholder?.componentId === contract.flow.placeholderComponent &&
      forbiddenChildren.length === 0;

    return {
      ...expected,
      resolvedNode: match.node.id,
      status: conformant ? 'conformant' : 'invalid',
      placeholder: placeholder ? { id: placeholder.id, type: placeholder.type, name: placeholder.name, componentId: placeholder.componentId } : null,
      ancestry: match.ancestors.slice(0, 12).map(({ id, type, name }) => ({ id, type, name })),
    };
  });

  const expectedPrototypeRoot = contract.flow.prototype.root;
  if (!prototypeDocument || prototypeDocument.id !== expectedPrototypeRoot.id || prototypeDocument.type !== expectedPrototypeRoot.type || prototypeDocument.name !== expectedPrototypeRoot.name) {
    errors.push(`Prototype root must be ${JSON.stringify(expectedPrototypeRoot)}`);
  }
  const prototypeIndex = walk(prototypeDocument || { id: 'missing-prototype', children: [] });
  const prototypeLabel = prototypeIndex.get(contract.flow.prototype.label.id);
  if (!prototypeLabel || prototypeLabel.node.name !== contract.flow.prototype.label.name || prototypeLabel.ancestors[0]?.id !== expectedPrototypeRoot.id) {
    errors.push(`${contract.flow.prototype.label.name} must be a direct child of prototype ${expectedPrototypeRoot.id}`);
  }
  const actualFlowStartingPoints = (pageDocument.flowStartingPoints || [])
    .map(({ nodeId, name }) => ({ nodeId, name }))
    .sort((left, right) => JSON.stringify(left).localeCompare(JSON.stringify(right)));
  const expectedFlowStartingPoints = [...contract.flow.prototype.flowStartingPoints]
    .sort((left, right) => JSON.stringify(left).localeCompare(JSON.stringify(right)));
  if (actualFlowStartingPoints.length && JSON.stringify(actualFlowStartingPoints) !== JSON.stringify(expectedFlowStartingPoints)) {
    errors.push(`Prototype flow starting points must be ${JSON.stringify(expectedFlowStartingPoints)}; received ${JSON.stringify(actualFlowStartingPoints)}`);
  }
  const prototypeFrames = contract.flow.prototype.frames.map((expected) => {
    const match = prototypeIndex.get(expected.node);
    if (!match) {
      errors.push(`${expected.name} (${expected.node}) is missing from prototype ${expectedPrototypeRoot.id}`);
      return { node: null, name: null, type: null, componentId: null, status: 'missing', destinations: [], backCount: 0, navigations: [], backActions: [] };
    }
    const direct = match.node.type === expected.type && match.node.name === expected.name &&
      match.node.componentId === expected.componentId && match.ancestors[0]?.id === expectedPrototypeRoot.id;
    if (!direct) errors.push(`${expected.name} must be a direct ${expected.type} child of prototype ${expectedPrototypeRoot.id} from ${expected.componentId}`);
    const observed = observedInteractions(match.node);
    const destinations = observed.navigations.map(({ destinationId }) => destinationId).sort();
    const expectedDestinations = [...expected.destinations].sort();
    const triggersConform = [...observed.navigations, ...observed.backActions]
      .every(({ trigger }) => trigger === expected.trigger);
    if (JSON.stringify(destinations) !== JSON.stringify(expectedDestinations)) {
      errors.push(`${expected.name} navigation destinations must be ${JSON.stringify(expectedDestinations)}; received ${JSON.stringify(destinations)}`);
    }
    if (observed.backActions.length !== expected.backCount) {
      errors.push(`${expected.name} back action count must be ${expected.backCount}; received ${observed.backActions.length}`);
    }
    if (!triggersConform) {
      errors.push(`${expected.name} interactions must use ${expected.trigger}`);
    }
    if (observed.unsupportedActions.length) {
      errors.push(`${expected.name} contains unsupported prototype actions: ${JSON.stringify(observed.unsupportedActions)}`);
    }
    const interactionsConform = JSON.stringify(destinations) === JSON.stringify(expectedDestinations) &&
      observed.backActions.length === expected.backCount && triggersConform && observed.unsupportedActions.length === 0;
    return {
      node: match.node.id,
      name: match.node.name,
      type: match.node.type,
      componentId: match.node.componentId || null,
      status: direct && interactionsConform ? 'conformant' : 'invalid',
      destinations,
      backCount: observed.backActions.length,
      navigations: observed.navigations,
      backActions: observed.backActions,
    };
  });
  const destinationNodes = new Set(prototypeFrames.flatMap(({ destinations }) => destinations));
  const inferredFlowStartNodes = prototypeFrames
    .filter(({ node, status }) => node && status === 'conformant' && !destinationNodes.has(node))
    .map(({ node }) => node)
    .sort();
  const expectedFlowStartNodes = expectedFlowStartingPoints.map(({ nodeId }) => nodeId).sort();
  if (!actualFlowStartingPoints.length && JSON.stringify(inferredFlowStartNodes) !== JSON.stringify(expectedFlowStartNodes)) {
    errors.push(`Prototype interaction graph roots must be ${JSON.stringify(expectedFlowStartNodes)}; received ${JSON.stringify(inferredFlowStartNodes)}`);
  }
  const flowStartVerification = actualFlowStartingPoints.length ? 'figma-rest' : 'interaction-graph';
  const verifiedFlowStartingPoints = actualFlowStartingPoints.length
    ? actualFlowStartingPoints
    : expectedFlowStartingPoints.filter(({ nodeId }) => inferredFlowStartNodes.includes(nodeId));

  const canonical = {
    page: {
      id: pageDocument.id,
      name: pageDocument.name,
      topLevel: actualTop,
      flowStartingPoints: actualFlowStartingPoints,
    },
    inventory: {
      id: inventory.id,
      name: inventory.name,
      fill: actualInventoryFill,
      screens: actualInventoryScreens,
      assets: Object.values(expectedInventory.assets).map(({ id, name }) => ({ id, name, status: inventoryIndex.has(id) ? 'present' : 'missing' })),
      canonicalInstances: expectedInventory.canonicalInstances.map(({ id, name }) => ({ id, name, status: inventoryIndex.has(id) ? 'present' : 'missing' })),
    },
    canonicalScreens,
    componentQuality: {
      dimensions: contract.componentQuality.dimensions,
      components: componentQuality,
    },
    documentInstance: expectedDocument,
    stepSequence: actualSequence,
    prototypeRoot: expectedPrototypeRoot,
    screens,
    prototype: {
      flowStartingPoints: actualFlowStartingPoints,
      verifiedFlowStartingPoints,
      flowStartVerification,
      frames: prototypeFrames,
    },
  };
  const digest = createHash('sha256').update(JSON.stringify(canonical)).digest('hex');
  return { ok: errors.length === 0, errors, digest, canonical };
}

export function structureNodeRequests(contract) {
  const componentQualityIds = contract.componentQuality.components.map(({ id }) => id);
  return [
    {
      label: 'page',
      nodeIds: [contract.page.id],
      responseShape: 'file',
      path: `/files/${contract.fileKey}?depth=2`,
    },
    { label: 'inventory', nodeIds: [contract.inventory.id], path: `/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.inventory.id)}` },
    { label: 'screenComponents', nodeIds: [contract.screenComponents.id], path: `/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.screenComponents.id)}` },
    { label: 'componentQuality', nodeIds: componentQualityIds, path: `/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(componentQualityIds.join(','))}` },
    { label: 'flow', nodeIds: [contract.flow.id], path: `/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.flow.id)}` },
    { label: 'prototype', nodeIds: [contract.flow.prototype.root.id], path: `/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.flow.prototype.root.id)}` },
  ];
}

export async function createStructureReport(contract, {
  fetchFigma = figma,
  head = process.env.GITHUB_SHA || null,
  capturedAt = new Date().toISOString(),
} = {}) {
  const requests = structureNodeRequests(contract);
  const settled = await Promise.allSettled(
    requests.map(({ path }) => Promise.resolve().then(() => fetchFigma(path))),
  );
  const documents = new Map();
  const failedNodeIds = new Set();
  const missingNodeIds = new Set();
  const malformedResponses = [];
  const requestDiagnostics = [];
  const errors = [];

  for (const [index, outcome] of settled.entries()) {
    const request = requests[index];
    if (outcome.status === 'rejected') {
      const reason = outcome.reason instanceof Error ? outcome.reason.message : String(outcome.reason);
      request.nodeIds.forEach((id) => failedNodeIds.add(id));
      requestDiagnostics.push({ label: request.label, nodeIds: request.nodeIds, status: 'error', error: reason });
      errors.push(`Figma fetch failed for ${request.label} nodes ${request.nodeIds.join(', ')}: ${reason}`);
      continue;
    }

    if (request.responseShape === 'file') {
      const pages = outcome.value?.document?.children;
      if (!Array.isArray(pages)) {
        request.nodeIds.forEach((id) => missingNodeIds.add(id));
        const diagnostic = { label: request.label, nodeIds: request.nodeIds, reason: 'response.document.children is missing or malformed' };
        malformedResponses.push(diagnostic);
        requestDiagnostics.push({ label: request.label, nodeIds: request.nodeIds, status: 'malformed' });
        errors.push(`Malformed Figma response for ${request.label} nodes ${request.nodeIds.join(', ')}: ${diagnostic.reason}`);
        continue;
      }

      const missingForRequest = [];
      for (const id of request.nodeIds) {
        const document = pages.find((page) => page?.id === id);
        if (!document || typeof document !== 'object' || Array.isArray(document)) {
          missingNodeIds.add(id);
          missingForRequest.push(id);
        } else {
          documents.set(id, document);
        }
      }
      if (missingForRequest.length) {
        const diagnostic = { label: request.label, nodeIds: missingForRequest, reason: 'contracted page document is missing or malformed' };
        malformedResponses.push(diagnostic);
        requestDiagnostics.push({ label: request.label, nodeIds: request.nodeIds, status: 'incomplete', missingNodeIds: missingForRequest });
        errors.push(`Figma response omitted contracted ${request.label} nodes: ${missingForRequest.join(', ')}`);
      } else {
        requestDiagnostics.push({ label: request.label, nodeIds: request.nodeIds, status: 'fetched' });
      }
      continue;
    }

    const nodes = outcome.value?.nodes;
    if (!nodes || typeof nodes !== 'object' || Array.isArray(nodes)) {
      request.nodeIds.forEach((id) => missingNodeIds.add(id));
      const diagnostic = { label: request.label, nodeIds: request.nodeIds, reason: 'response.nodes is missing or malformed' };
      malformedResponses.push(diagnostic);
      requestDiagnostics.push({ label: request.label, nodeIds: request.nodeIds, status: 'malformed' });
      errors.push(`Malformed Figma response for ${request.label} nodes ${request.nodeIds.join(', ')}: ${diagnostic.reason}`);
      continue;
    }

    const missingForRequest = [];
    for (const id of request.nodeIds) {
      const document = nodes[id]?.document;
      if (!document || typeof document !== 'object' || Array.isArray(document)) {
        missingNodeIds.add(id);
        missingForRequest.push(id);
      } else {
        documents.set(id, document);
      }
    }
    if (missingForRequest.length) {
      const diagnostic = { label: request.label, nodeIds: missingForRequest, reason: 'contracted node document is missing or malformed' };
      malformedResponses.push(diagnostic);
      requestDiagnostics.push({ label: request.label, nodeIds: request.nodeIds, status: 'incomplete', missingNodeIds: missingForRequest });
      errors.push(`Figma response omitted contracted ${request.label} nodes: ${missingForRequest.join(', ')}`);
    } else {
      requestDiagnostics.push({ label: request.label, nodeIds: request.nodeIds, status: 'fetched' });
    }
  }

  const failed = [...failedNodeIds];
  const missing = [...missingNodeIds];
  const unavailable = [...new Set([...failed, ...missing])];
  const diagnostics = {
    requestedNodeIds: requests.flatMap(({ nodeIds }) => nodeIds),
    failedNodeIds: failed,
    missingNodeIds: missing,
    unavailableNodeIds: unavailable,
    malformedResponses,
    requests: requestDiagnostics,
  };
  const reportBase = {
    version: contract.version,
    head,
    capturedAt,
    fileKey: contract.fileKey,
    page: contract.page,
    flow: { id: contract.flow.id, templateSource: contract.flow.templateSource },
    prototype: contract.flow.prototype.root,
  };

  if (unavailable.length) {
    return {
      ...reportBase,
      status: failed.length ? 'error' : 'incomplete',
      errors,
      diagnostics,
      structureDigest: null,
      structure: null,
    };
  }

  try {
    const componentQualityDocuments = Object.fromEntries(
      contract.componentQuality.components.map(({ id }) => [id, documents.get(id)]),
    );
    const result = verifyStructure(
      contract,
      documents.get(contract.page.id),
      documents.get(contract.flow.id),
      documents.get(contract.flow.prototype.root.id),
      documents.get(contract.inventory.id),
      documents.get(contract.screenComponents.id),
      componentQualityDocuments,
    );
    return {
      ...reportBase,
      status: result.ok ? 'completed' : 'failed',
      structureDigest: result.digest,
      errors: result.errors,
      diagnostics,
      structure: result.canonical,
    };
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    return {
      ...reportBase,
      status: 'error',
      structureDigest: null,
      errors: [`Figma structure verification failed: ${message}`],
      diagnostics: { ...diagnostics, verificationError: message },
      structure: null,
    };
  }
}

async function main() {
  const positional = process.argv.slice(2).filter((argument) => !argument.startsWith('--'));
  const options = Object.fromEntries(process.argv.slice(2).filter((argument) => argument.startsWith('--')).map((argument) => {
    const [key, ...rest] = argument.slice(2).split('=');
    return [key, rest.join('=')];
  }));
  const output = resolve(options.output || positional[0] || 'artifacts/figma-structure-report.json');
  const contractPath = resolve(options.contract || resolve(HERE, 'structure-contract.json'));
  const contract = validateStructureContract(
    JSON.parse(await readFile(contractPath, 'utf8')),
    { expectedFileKey: options['expected-file-key'] || undefined },
  );
  await mkdir(dirname(output), { recursive: true });
  const fetchFigma = process.env.FIGMA_TOKEN
    ? figma
    : async () => { throw new Error('FIGMA_TOKEN is not set'); };
  const report = await createStructureReport(contract, {
    fetchFigma,
    head: options.head || process.env.GITHUB_SHA || null,
  });
  await writeFile(output, `${JSON.stringify(report, null, 2)}\n`);
  if (report.status !== 'completed') throw new Error(report.errors.join('\n'));
  console.log(`Verified Figma structure ${report.structureDigest} → ${output}`);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await main();
}
