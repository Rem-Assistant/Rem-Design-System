import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fetchHeadData } from './fetch-head-data.mjs';

const manifest = JSON.parse(await readFile(new URL('./manifest.json', import.meta.url), 'utf8'));
const contract = JSON.parse(await readFile(new URL('./structure-contract.json', import.meta.url), 'utf8'));
const response = (value) => ({
  ok: true,
  status: 200,
  json: async () => ({ type: 'file', encoding: 'base64', content: Buffer.from(JSON.stringify(value)).toString('base64') }),
});

const temp = await mkdtemp(join(tmpdir(), 'rem-head-data-'));
try {
  const seen = [];
  await fetchHeadData({
    apiUrl: 'https://api.github.com',
    repo: 'Rem-Assistant/Rem-Design-System',
    sha: 'a'.repeat(40),
    token: 'test-token',
    destination: temp,
    expectedFileKey: manifest.figmaFileKey,
    fetchImpl: async (url, options) => {
      seen.push({ url, options });
      return response(url.includes('structure-contract.json') ? contract : manifest);
    },
  });
  assert.equal(seen.length, 2);
  assert.ok(seen.every(({ url }) => url.endsWith(`?ref=${'a'.repeat(40)}`)));
  assert.equal(JSON.parse(await readFile(join(temp, 'manifest.json'))).figmaFileKey, manifest.figmaFileKey);

  await assert.rejects(() => fetchHeadData({
    apiUrl: 'https://api.github.com', repo: 'Rem-Assistant/Rem-Design-System', sha: 'b'.repeat(40),
    token: 'test-token', destination: join(temp, 'mismatch'), expectedFileKey: manifest.figmaFileKey,
    fetchImpl: async (url) => response(url.includes('structure-contract.json')
      ? { ...contract, fileKey: 'unexpectedFileKey' } : manifest),
  }), /unexpected Figma file/);

  await assert.rejects(() => fetchHeadData({
    apiUrl: 'https://evil.invalid', repo: 'Rem-Assistant/Rem-Design-System', sha: 'c'.repeat(40),
    token: 'test-token', destination: join(temp, 'origin'), expectedFileKey: manifest.figmaFileKey,
    fetchImpl: async () => { throw new Error('must not fetch'); },
  }), /unexpected GitHub API origin/);
} finally {
  await rm(temp, { recursive: true, force: true });
}

console.log('Exact-head declarative data tests passed');
