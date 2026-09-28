#!/usr/bin/env node
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateManifestData, validateStructureContract } from './structure-contract.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
const FILES = ['tools/design-sync/manifest.json', 'tools/design-sync/structure-contract.json'];
const MAX_BLOB_BYTES = 256 * 1024;

function safeRepo(value) {
  if (!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(value ?? '')) throw new Error('invalid head repository');
  return value;
}

export async function fetchHeadData({ apiUrl, repo, sha, token, destination, expectedFileKey, fetchImpl = fetch }) {
  if (!/^https:\/\/api\.github\.com$/.test(apiUrl ?? '')) throw new Error('unexpected GitHub API origin');
  safeRepo(repo);
  if (!/^[0-9a-f]{40}$/.test(sha ?? '')) throw new Error('full lowercase head SHA required');
  if (!token) throw new Error('GitHub token is required');

  const decoded = new Map();
  for (const path of FILES) {
    const encodedPath = path.split('/').map(encodeURIComponent).join('/');
    const response = await fetchImpl(`${apiUrl}/repos/${repo}/contents/${encodedPath}?ref=${sha}`, {
      headers: {
        Accept: 'application/vnd.github+json',
        Authorization: `Bearer ${token}`,
        'X-GitHub-Api-Version': '2022-11-28',
      },
    });
    if (!response.ok) throw new Error(`failed to fetch ${path} at exact head: HTTP ${response.status}`);
    const payload = await response.json();
    if (payload?.type !== 'file' || payload?.encoding !== 'base64' || typeof payload.content !== 'string') {
      throw new Error(`${path} is not a GitHub file blob`);
    }
    const bytes = Buffer.from(payload.content.replace(/\s/g, ''), 'base64');
    if (bytes.length === 0 || bytes.length > MAX_BLOB_BYTES) throw new Error(`${path} has an invalid size`);
    let data;
    try { data = JSON.parse(bytes.toString('utf8')); } catch { throw new Error(`${path} is not valid JSON`); }
    decoded.set(path, { bytes, data });
  }

  const manifest = validateManifestData(decoded.get(FILES[0]).data, { expectedFileKey });
  const contract = validateStructureContract(decoded.get(FILES[1]).data, { expectedFileKey });
  if (manifest.figmaFileKey !== contract.fileKey) throw new Error('head manifest and structure contract target different Figma files');

  await mkdir(destination, { recursive: true });
  for (const path of FILES) {
    await writeFile(join(destination, path.split('/').at(-1)), decoded.get(path).bytes, { flag: 'wx' });
  }
  return { manifest, contract };
}

async function main() {
  const options = Object.fromEntries(process.argv.slice(2).map((argument) => {
    const [key, ...rest] = argument.replace(/^--/, '').split('=');
    return [key, rest.join('=')];
  }));
  const trustedManifest = JSON.parse(await readFile(resolve(HERE, 'manifest.json'), 'utf8'));
  await fetchHeadData({
    apiUrl: options['api-url'] || process.env.GITHUB_API_URL,
    repo: options.repo,
    sha: options.sha,
    token: process.env.GITHUB_TOKEN,
    destination: resolve(options.destination || 'artifacts/head-data'),
    expectedFileKey: trustedManifest.figmaFileKey,
  });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) await main();
