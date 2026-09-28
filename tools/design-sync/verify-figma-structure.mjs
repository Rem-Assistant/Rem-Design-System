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

export function verifyStructure(contract, pageDocument, flowDocument) {
  const errors = [];
  const expectedTop = contract.page.topLevel;
  const actualTop = (pageDocument.children || []).map(({ id, type, name }) => ({ id, type, name }));
  if (JSON.stringify(actualTop) !== JSON.stringify(expectedTop)) {
    errors.push(`Page top level must be ${JSON.stringify(expectedTop)}; received ${JSON.stringify(actualTop)}`);
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

  const flowStartIds = new Set((pageDocument.flowStartingPoints || []).map(({ nodeId }) => nodeId));
  const prototypeLabel = index.get(contract.flow.prototype.label.id);
  if (!prototypeLabel || prototypeLabel.node.name !== contract.flow.prototype.label.name || prototypeLabel.ancestors[0]?.id !== flowDocument.id) {
    errors.push(`${contract.flow.prototype.label.name} must be a direct child of flow ${contract.flow.id}`);
  }
  const prototype = contract.flow.prototype.frames.map((expected) => {
    const match = index.get(expected.node);
    if (!match) {
      errors.push(`${expected.name} (${expected.node}) is missing from flow ${contract.flow.id}`);
      return { ...expected, status: 'missing' };
    }
    const direct = match.node.type === 'FRAME' && match.node.name === expected.name && match.ancestors[0]?.id === flowDocument.id;
    if (!direct) errors.push(`${expected.name} must be a direct FRAME child of flow ${contract.flow.id}`);
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
    const flowResponse = await figma(`/files/${contract.fileKey}/nodes?ids=${encodeURIComponent(contract.flow.id)}`);
    const pageDocument = pageResponse.nodes?.[contract.page.id]?.document;
    const flowDocument = flowResponse.nodes?.[contract.flow.id]?.document;
    if (!pageDocument || !flowDocument) throw new Error('Figma did not return the contracted page and flow nodes');

    const result = verifyStructure(contract, pageDocument, flowDocument);
    report = {
      version: contract.version,
      status: result.ok ? 'completed' : 'failed',
      head: process.env.GITHUB_SHA || null,
      capturedAt: new Date().toISOString(),
      fileKey: contract.fileKey,
      page: contract.page,
      flow: { id: contract.flow.id, templateSource: contract.flow.templateSource },
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
