import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { createStructureReport, structureNodeRequests } from './verify-figma-structure.mjs';

const contract = JSON.parse(await readFile(new URL('./structure-contract.json', import.meta.url), 'utf8'));
const expectedIds = structureNodeRequests(contract).flatMap(({ nodeIds }) => nodeIds);
const expectedUnavailableIds = [...new Set(expectedIds)];

const incomplete = await createStructureReport(contract, {
  fetchFigma: async () => ({ nodes: {} }),
  head: 'test-head',
  capturedAt: '2026-09-28T00:00:00.000Z',
});
assert.equal(incomplete.status, 'incomplete');
assert.deepEqual(incomplete.diagnostics.failedNodeIds, []);
assert.deepEqual(incomplete.diagnostics.missingNodeIds, expectedUnavailableIds);
assert.deepEqual(incomplete.diagnostics.unavailableNodeIds, expectedUnavailableIds);
assert.equal(incomplete.diagnostics.malformedResponses.length, structureNodeRequests(contract).length);
assert.equal(incomplete.structure, null);

let requestIndex = 0;
const failed = await createStructureReport(contract, {
  fetchFigma: () => {
    if (requestIndex++ === 0) throw new Error('network unavailable');
    return Promise.resolve({ nodes: {} });
  },
  head: 'test-head',
  capturedAt: '2026-09-28T00:00:00.000Z',
});
assert.equal(failed.status, 'error');
assert.deepEqual(failed.diagnostics.failedNodeIds, [contract.page.id]);
assert.ok(failed.diagnostics.missingNodeIds.includes(contract.flow.id));
assert.ok(failed.errors.some((error) => error.includes(`nodes ${contract.page.id}`) && error.includes('network unavailable')));

const tempRoot = await mkdtemp(join(tmpdir(), 'rem-figma-structure-'));
try {
  const reportPath = join(tempRoot, 'figma-structure-report.json');
  const cli = spawnSync(process.execPath, [fileURLToPath(new URL('./verify-figma-structure.mjs', import.meta.url)), reportPath], {
    cwd: tempRoot,
    encoding: 'utf8',
    env: Object.fromEntries(Object.entries(process.env).filter(([key]) => key !== 'FIGMA_TOKEN')),
  });
  assert.notEqual(cli.status, 0);
  const emitted = JSON.parse(await readFile(reportPath, 'utf8'));
  assert.equal(emitted.status, 'error');
  assert.deepEqual(emitted.diagnostics.failedNodeIds, expectedUnavailableIds);
  assert.deepEqual(emitted.diagnostics.missingNodeIds, []);
  assert.ok(emitted.errors.every((error) => error.includes('FIGMA_TOKEN is not set')));
} finally {
  await rm(tempRoot, { recursive: true, force: true });
}

console.log('Figma structure fetch diagnostics tests passed');
