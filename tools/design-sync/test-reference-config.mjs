import assert from 'node:assert/strict';
import test from 'node:test';
import { assertPng, contractReferenceItems, mergeExportItems } from './reference-config.mjs';

test('collects contract screen references and structural waypoints', () => {
  const items = contractReferenceItems({
    demo: {
      references: { ready: { name: 'Demo-ready', node: '1:2' } },
      waypoints: { flow: { name: 'Demo-flow', node: '1:3' } },
    },
  });
  assert.deepEqual(items, [
    { name: 'Demo-ready', node: '1:2' },
    { name: 'Demo-flow', node: '1:3' },
  ]);
});

test('deduplicates an identical manifest and contract export', () => {
  assert.deepEqual(
    mergeExportItems(
      [{ name: 'Demo-ready', node: '1:2' }],
      [{ name: 'Demo-ready', node: '1:2' }],
    ),
    [{ name: 'Demo-ready', node: '1:2' }],
  );
});

test('rejects ambiguous export names and nodes', () => {
  assert.throws(
    () => mergeExportItems(
      [{ name: 'Demo-ready', node: '1:2' }],
      [{ name: 'Demo-ready', node: '1:3' }],
    ),
    /multiple Figma nodes/,
  );
  assert.throws(
    () => contractReferenceItems({
      demo: { references: { ready: { name: '../escape', node: '1:2' } } },
    }),
    /invalid export name/,
  );
  assert.throws(
    () => mergeExportItems([{ name: '../../escape', node: '1:2' }]),
    /invalid manifest export name/,
  );
  assert.throws(
    () => mergeExportItems(
      [{ name: 'Demo', node: '1:2' }],
      [{ name: 'demo', node: '1:3' }],
    ),
    /multiple Figma nodes/,
  );
});

test('rejects a non-PNG response before it becomes reference evidence', () => {
  assert.throws(() => assertPng(Buffer.from('forbidden'), 'Demo'), /not a PNG/);
  assert.doesNotThrow(() => assertPng(
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    'Demo',
  ));
});
