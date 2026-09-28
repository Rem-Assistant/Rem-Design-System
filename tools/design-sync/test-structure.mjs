import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { verifyStructure } from './verify-figma-structure.mjs';

const contract = JSON.parse(await readFile(new URL('./structure-contract.json', import.meta.url), 'utf8'));

const screen = (entry) => ({ id: entry.node, type: 'FRAME', name: entry.name });
const placeholder = (entry) => ({
  id: `placeholder-${entry.node}`,
  type: 'FRAME',
  name: 'Mobile Placeholder',
  children: [{ id: `slot-${entry.node}`, type: 'FRAME', name: 'Device / Screen slot', children: [screen(entry)] }],
});
const flow = {
  id: contract.flow.id,
  type: 'SECTION',
  name: '01 · Consent flow',
  children: contract.flow.screens.map(placeholder),
};
const page = {
  id: contract.page.id,
  type: 'CANVAS',
  name: contract.page.name,
  children: contract.page.topLevel,
};

const passing = verifyStructure(contract, page, flow);
assert.equal(passing.ok, true);
assert.equal(passing.canonical.screens.every((entry) => entry.status === 'nested'), true);
assert.match(passing.digest, /^[a-f0-9]{64}$/);

const looseFlow = structuredClone(flow);
looseFlow.children[0] = screen(contract.flow.screens[0]);
const failing = verifyStructure(contract, page, looseFlow);
assert.equal(failing.ok, false);
assert.match(failing.errors.join('\n'), /direct child of Device \/ Screen slot/);

const extraTopLevel = structuredClone(page);
extraTopLevel.children.push({ id: 'extra:1', type: 'FRAME', name: 'Loose screen' });
const scattered = verifyStructure(contract, extraTopLevel, flow);
assert.equal(scattered.ok, false);
assert.match(scattered.errors.join('\n'), /Page top level/);

console.log('Figma structure verifier tests passed');
