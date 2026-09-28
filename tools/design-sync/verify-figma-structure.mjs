#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { figma, requireToken } from './lib.mjs';

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

function actionsFor(reaction) {
  if (Array.isArray(reaction?.actions)) return reaction.actions;
  return reaction?.action ? [reaction.action] : [];
}

function reactionInventory(root) {
  return [...walk(root).values()].flatMap(({ node }) =>
    (node.reactions || []).flatMap(actionsFor));
}

export function verifyStructure(contract, pageDocument, flowDocument, prototypeDocument, inventoryDocument, screenComponentsDocument) {
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
    const conforms = Boolean(match && match.type === expected.type && match.name === expected.name &&
      match.width === expected.width && match.height === expected.height);
    if (!conforms) errors.push(`Canonical screen must be ${JSON.stringify(expected)}`);
    return { ...expected, status: conforms ? 'conformant' : 'invalid' };
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

  const screens = contract.flow.screens.map((expected) => {
    const match = index.get(expected.node);
    if (!match) {
      errors.push(`${expected.name} (${expected.node}) is missing from flow ${contract.flow.id}`);
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
      [sectionsSlot, 'SLOT', hierarchy.sections, 'sections slot'],
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
    if (placeholder?.id !== expected.placeholder || placeholder?.componentId !== contract.flow.placeholderComponent) {
      errors.push(`${expected.name} must use Mobile Placeholder ${expected.placeholder} from ${contract.flow.placeholderComponent}`);
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
      placeholder?.id === expected.placeholder && placeholder?.componentId === contract.flow.placeholderComponent &&
      forbiddenChildren.length === 0;

    return {
      ...expected,
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
  const flowStartIds = new Set((pageDocument.flowStartingPoints || []).map(({ nodeId }) => nodeId));
  const prototypeLabel = prototypeIndex.get(contract.flow.prototype.label.id);
  if (!prototypeLabel || prototypeLabel.node.name !== contract.flow.prototype.label.name || prototypeLabel.ancestors[0]?.id !== expectedPrototypeRoot.id) {
    errors.push(`${contract.flow.prototype.label.name} must be a direct child of prototype ${expectedPrototypeRoot.id}`);
  }
  const prototype = contract.flow.prototype.frames.map((expected) => {
    const match = prototypeIndex.get(expected.node);
    if (!match) {
      errors.push(`${expected.name} (${expected.node}) is missing from prototype ${expectedPrototypeRoot.id}`);
      return { ...expected, status: 'missing' };
    }
    const direct = match.node.type === expected.type && match.node.name === expected.name &&
      match.node.componentId === expected.componentId && match.ancestors[0]?.id === expectedPrototypeRoot.id;
    if (!direct) errors.push(`${expected.name} must be a direct ${expected.type} child of prototype ${expectedPrototypeRoot.id} from ${expected.componentId}`);
    const hasFlowStart = flowStartIds.has(expected.node);
    if (hasFlowStart !== expected.flowStart) {
      errors.push(`${expected.name} flow-start status must be ${expected.flowStart}; received ${hasFlowStart}`);
    }
    const actions = reactionInventory(match.node);
    const destinations = [...new Set(actions.filter(({ type }) => type === 'NODE').map(({ destinationId }) => destinationId))].sort();
    const expectedDestinations = [...expected.destinations].sort();
    if (JSON.stringify(destinations) !== JSON.stringify(expectedDestinations)) {
      errors.push(`${expected.name} destinations must be ${JSON.stringify(expectedDestinations)}; received ${JSON.stringify(destinations)}`);
    }
    const backCount = actions.filter(({ type }) => type === 'BACK').length;
    if (backCount !== expected.backCount) errors.push(`${expected.name} must contain ${expected.backCount} Back actions; received ${backCount}`);
    const actionConforms = JSON.stringify(destinations) === JSON.stringify(expectedDestinations) && backCount === expected.backCount;
    return { ...expected, status: direct && hasFlowStart === expected.flowStart && actionConforms ? 'conformant' : 'invalid' };
  });

  const canonical = {
    page: {
      id: pageDocument.id,
      name: pageDocument.name,
      topLevel: actualTop,
      flowStartingPoints: pageDocument.flowStartingPoints || [],
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
    documentInstance: expectedDocument,
    stepSequence: actualSequence,
    prototypeRoot: expectedPrototypeRoot,
    screens,
    prototype,
  };
  const digest = createHash('sha256').update(JSON.stringify(canonical)).digest('hex');
  return { ok: errors.length === 0, errors, digest, canonical };
}

async function main() {
  requireToken();
  const output = resolve(process.argv[2] || 'artifacts/figma-structure-report.json');
  const contract = JSON.parse(await readFile(resolve(HERE, 'structure-contract.json'), 'utf8'));
  await mkdir(dirname(output), { recursive: true });

  let report;
  try {
    const pageResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.page.id)}&depth=1`);
    const inventoryResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.inventory.id)}`);
    const screenComponentsResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.screenComponents.id)}`);
    const flowResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.flow.id)}`);
    const prototypeResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.flow.prototype.root.id)}`);
    const pageDocument = pageResponse.nodes?.[contract.page.id]?.document;
    const inventoryDocument = inventoryResponse.nodes?.[contract.inventory.id]?.document;
    const screenComponentsDocument = screenComponentsResponse.nodes?.[contract.screenComponents.id]?.document;
    const flowDocument = flowResponse.nodes?.[contract.flow.id]?.document;
    const prototypeDocument = prototypeResponse.nodes?.[contract.flow.prototype.root.id]?.document;
    if (!pageDocument || !inventoryDocument || !screenComponentsDocument || !flowDocument || !prototypeDocument) throw new Error('Figma did not return the contracted page, inventory, canonical screens, flow, and prototype nodes');

    const result = verifyStructure(contract, pageDocument, flowDocument, prototypeDocument, inventoryDocument, screenComponentsDocument);
    report = {
      version: contract.version,
      status: result.ok ? 'completed' : 'failed',
      head: process.env.GITHUB_SHA || null,
      capturedAt: new Date().toISOString(),
      fileKey: contract.fileKey,
      page: contract.page,
      flow: { id: contract.flow.id, templateSource: contract.flow.templateSource },
      prototype: contract.flow.prototype.root,
      structureDigest: result.digest,
      errors: result.errors,
      structure: result.canonical,
    };
    await writeFile(output, `${JSON.stringify(report, null, 2)}\n`);
    if (!result.ok) throw new Error(result.errors.join('\n'));
  } catch (error) {
    if (!report) {
      report = {
        version: 1,
        status: 'error',
        head: process.env.GITHUB_SHA || null,
        capturedAt: new Date().toISOString(),
        fileKey: contract.fileKey,
        errors: [error instanceof Error ? error.message : String(error)],
      };
      await writeFile(output, `${JSON.stringify(report, null, 2)}\n`);
    }
    throw error;
  }

  console.log(`Verified Figma structure ${report.structureDigest} → ${output}`);
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await main();
}
