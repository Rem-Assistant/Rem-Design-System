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

export function verifyStructure(contract, pageDocument, flowDocument, prototypeDocument, inventoryDocument) {
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

  if (flowDocument.id !== contract.flow.id) {
    errors.push(`Flow root must be ${contract.flow.id}; received ${flowDocument.id}`);
  }

  const index = walk(flowDocument);
  const hierarchy = contract.flow.hierarchy;
  const rowsNodes = [...index.values()]
    .filter(({ node }) => node.name === hierarchy.rows && node.type === 'FRAME');
  if (rowsNodes.length !== 1) {
    errors.push(`Flow must contain exactly one ${hierarchy.rows}; received ${rowsNodes.length}`);
  }
  const rowNodes = (rowsNodes[0]?.node.children || [])
    .filter((node) => node.name === hierarchy.row && node.type === 'FRAME' && node.visible !== false);
  if (rowNodes.length !== hierarchy.rowCount) {
    errors.push(`${hierarchy.rows} must contain ${hierarchy.rowCount} visible ${hierarchy.row} rows; received ${rowNodes.length}`);
  }
  rowNodes.forEach((row, index) => {
    const spacing = hierarchy.rowSpacing[index];
    if (row.itemSpacing !== spacing) {
      errors.push(`${hierarchy.row} row ${index + 1} must use ${spacing} item spacing; received ${row.itemSpacing}`);
    }
    const visibleConnectors = (row.children || [])
      .filter((node) => node.visible !== false && node.type === 'VECTOR');
    const visiblePlaceholders = (row.children || [])
      .filter((node) => node.visible !== false && node.name === hierarchy.placeholder);
    const requiredConnectors = index === 0 ? Math.max(visiblePlaceholders.length - 1, 0) : 0;
    if (visibleConnectors.length !== requiredConnectors) {
      errors.push(`${hierarchy.row} row ${index + 1} must contain ${requiredConnectors} visible arrow vectors; received ${visibleConnectors.length}`);
    }
  });

  const screens = contract.flow.screens.map((expected) => {
    const match = index.get(expected.node);
    if (!match) {
      errors.push(`${expected.name} (${expected.node}) is missing from flow ${contract.flow.id}`);
      return { ...expected, status: 'missing' };
    }

    const [placeholder, row, rows, section, sections] = match.ancestors;
    const checks = [
      [placeholder, hierarchy.placeholder, 'direct parent'],
      [row, hierarchy.row, 'row'],
      [rows, hierarchy.rows, 'row collection'],
      [section, hierarchy.section, 'section'],
      [sections, hierarchy.sections, 'section collection'],
    ];
    for (const [node, requiredName, relationship] of checks) {
      if (!node || node.type !== 'FRAME' || node.name !== requiredName) {
        errors.push(`${expected.name} ${relationship} must be FRAME ${requiredName}`);
      }
    }
    const forbiddenChildren = (placeholder?.children || [])
      .filter(({ name }) => hierarchy.forbiddenPlaceholderChildren.includes(name));
    if (forbiddenChildren.length) {
      errors.push(`${expected.name} placeholder contains forbidden legacy children: ${forbiddenChildren.map(({ name }) => name).join(', ')}`);
    }
    const expectedRow = rowNodes[expected.row];
    if (!expectedRow || row?.id !== expectedRow.id) {
      errors.push(`${expected.name} must be in ${hierarchy.row} row ${expected.row + 1}`);
    }

    const conformant = checks.every(([node, requiredName]) =>
      node?.type === 'FRAME' && node.name === requiredName) &&
      row?.id === expectedRow?.id && forbiddenChildren.length === 0;

    return {
      ...expected,
      status: conformant ? 'conformant' : 'invalid',
      placeholder: placeholder ? { id: placeholder.id, type: placeholder.type, name: placeholder.name } : null,
      row: row ? { id: row.id, type: row.type, name: row.name, index: rowNodes.findIndex(({ id }) => id === row.id) } : null,
      ancestry: match.ancestors.slice(0, 5).map(({ id, type, name }) => ({ id, type, name })),
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
    const direct = match.node.type === 'FRAME' && match.node.name === expected.name && match.ancestors[0]?.id === expectedPrototypeRoot.id;
    if (!direct) errors.push(`${expected.name} must be a direct FRAME child of prototype ${expectedPrototypeRoot.id}`);
    const hasFlowStart = flowStartIds.has(expected.node);
    if (hasFlowStart !== expected.flowStart) {
      errors.push(`${expected.name} flow-start status must be ${expected.flowStart}; received ${hasFlowStart}`);
    }
    return { ...expected, status: direct && hasFlowStart === expected.flowStart ? 'conformant' : 'invalid' };
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
    const flowResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.flow.id)}`);
    const prototypeResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.flow.prototype.root.id)}`);
    const pageDocument = pageResponse.nodes?.[contract.page.id]?.document;
    const inventoryDocument = inventoryResponse.nodes?.[contract.inventory.id]?.document;
    const flowDocument = flowResponse.nodes?.[contract.flow.id]?.document;
    const prototypeDocument = prototypeResponse.nodes?.[contract.flow.prototype.root.id]?.document;
    if (!pageDocument || !inventoryDocument || !flowDocument || !prototypeDocument) throw new Error('Figma did not return the contracted page, inventory, flow, and prototype nodes');

    const result = verifyStructure(contract, pageDocument, flowDocument, prototypeDocument, inventoryDocument);
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
