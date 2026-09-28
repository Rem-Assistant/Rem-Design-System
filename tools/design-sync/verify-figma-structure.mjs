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
  const screens = contract.flow.screens.map((expected) => {
    const match = index.get(expected.node);
    if (!match) {
      errors.push(`${expected.name} (${expected.node}) is missing from flow ${contract.flow.id}`);
      return { ...expected, status: 'missing' };
    }

    const slot = match.ancestors[0];
    const placeholder = match.ancestors[1];
    if (!slot || slot.name !== contract.flow.screenSlotName) {
      errors.push(`${expected.name} must be a direct child of ${contract.flow.screenSlotName}`);
    }
    if (!placeholder || !placeholder.name.startsWith(contract.flow.placeholderNamePrefix)) {
      errors.push(`${expected.name} must be inside a ${contract.flow.placeholderNamePrefix}`);
    }

    return {
      ...expected,
      status: slot?.name === contract.flow.screenSlotName &&
        placeholder?.name.startsWith(contract.flow.placeholderNamePrefix) ? 'nested' : 'invalid',
      slot: slot ? { id: slot.id, type: slot.type, name: slot.name } : null,
      placeholder: placeholder ? { id: placeholder.id, type: placeholder.type, name: placeholder.name } : null,
    };
  });

  const canonical = { page: { id: pageDocument.id, name: pageDocument.name, topLevel: actualTop }, screens };
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
      version: 1,
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
