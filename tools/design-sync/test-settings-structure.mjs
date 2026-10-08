import test from 'node:test';
import assert from 'node:assert/strict';
import { createSettingsStructureReport, verifySettingsStructure } from './verify-settings-structure.mjs';

const contract = {
  version: 1, fileKey: 'fixture', page: { id: '1:1', type: 'CANVAS', name: 'Settings New' },
  section: { id: '1:2', type: 'SECTION', name: 'Settings' }, amendments: ['Subtle explicitly approved.'],
  screens: [{ id: '1:3', type: 'COMPONENT', name: 'Settings entry', instances: [
    { id: '1:4', componentId: '741:311', parent: '1:3' },
    { id: '1:5', componentId: '101:18', parent: '1:4', text: ['Avery Diaz', 'avery@example.com'] },
    { id: '1:6', componentId: '188:2', parent: '1:5', text: ['Avery Diaz', 'avery@example.com'] },
  ] }],
};
function fixture() {
  return { page: { ...contract.page, children: [{ ...contract.section }] },
    section: { ...contract.section, children: [{ ...contract.screens[0] }] },
    screens: { '1:3': { ...contract.screens[0], children: [{ id: '1:4', type: 'INSTANCE', componentId: '741:311', children: [
      { id: '1:5', type: 'INSTANCE', componentId: '101:18', children: [
        { id: '1:6', type: 'INSTANCE', componentId: '188:2', children: [
          { id: '1:7', type: 'TEXT', characters: 'Avery Diaz' }, { id: '1:8', type: 'TEXT', characters: 'avery@example.com' },
        ] },
      ] },
    ] }] } },
  };
}
test('verifies visible canonical ancestry, order, copy, and explicit amendments', () => {
  const result = verifySettingsStructure(contract, fixture());
  assert.deepEqual(result.errors, []);
  assert.equal(result.structure.canonicalScreens[0].status, 'conformant');
  assert.deepEqual(result.structure.sourceAmendments, contract.amendments);
});
test('ignores hidden source alternatives and rejects visible extra canonical rows', () => {
  const data = fixture(); const children = data.screens['1:3'].children;
  children.push({ id: '1:9', type: 'INSTANCE', componentId: '101:18', visible: false });
  assert.deepEqual(verifySettingsStructure(contract, data).errors, []);
  children.at(-1).visible = true;
  assert.match(verifySettingsStructure(contract, data).errors.join(), /order/);
});
test('rejects moved roots, detached instances, and changed source copy', () => {
  const data = fixture(); data.section.children = [];
  data.screens['1:3'].children[0].type = 'FRAME';
  data.screens['1:3'].children[0].children[0].children[0].children[0].characters = 'Different';
  const errors = verifySettingsStructure(contract, data).errors.join();
  assert.match(errors, /direct child/); assert.match(errors, /canonical instance/); assert.match(errors, /copy changed/);
});
test('reports missing live nodes as failure and transport errors without proof', async () => {
  const head = 'a'.repeat(40);
  const missing = await createSettingsStructureReport(contract, { head, fetchFigma: async () => ({ nodes: {} }) });
  assert.equal(missing.status, 'failed');
  const error = await createSettingsStructureReport(contract, { head, fetchFigma: async () => { throw new Error('read denied'); } });
  assert.equal(error.status, 'error'); assert.equal(error.structure, null);
  await assert.rejects(createSettingsStructureReport(contract, { head: 'short' }), /SHA/);
});
