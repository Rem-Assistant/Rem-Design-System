// Shared helpers for the Figma REST API. Node 18+ (global fetch).
// Auth: set FIGMA_TOKEN (a personal access token with File content + Dev resources scope).
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
export const manifest = JSON.parse(readFileSync(join(HERE, 'manifest.json'), 'utf8'));

const TOKEN = process.env.FIGMA_TOKEN;
const API = 'https://api.figma.com/v1';

export function requireToken() {
  if (!TOKEN) {
    console.error('FIGMA_TOKEN is not set. Create a token at figma.com → Settings → Personal access tokens (scopes: File content read, Dev resources write).');
    process.exit(2);
  }
}

export async function figma(path, init = {}) {
  const res = await fetch(`${API}${path}`, {
    ...init,
    headers: { 'X-Figma-Token': TOKEN, 'Content-Type': 'application/json', ...(init.headers || {}) },
  });
  if (!res.ok) throw new Error(`Figma ${init.method || 'GET'} ${path} → ${res.status} ${await res.text()}`);
  return res.json();
}

// GitHub blob URL for a source path (or pass through an absolute http(s) url).
export function sourceUrl(source) {
  if (/^https?:\/\//.test(source)) return source;
  const { sourceRepo, sourceBranch } = manifest;
  return `https://github.com/${sourceRepo}/blob/${sourceBranch}/${source}`;
}
