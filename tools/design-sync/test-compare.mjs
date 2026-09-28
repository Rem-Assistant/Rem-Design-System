import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { PNG } from 'pngjs';

function onePixelPng() {
  const png = new PNG({ width: 1, height: 1 });
  png.data.set([255, 255, 255, 255]);
  return PNG.sync.write(png);
}

const tempRoot = await mkdtemp(join(tmpdir(), 'rem-design-compare-'));
try {
  const swiftDir = join(tempRoot, 'swiftui');
  const figmaDir = join(tempRoot, 'figma');
  await mkdir(swiftDir);
  await mkdir(figmaDir);
  const png = onePixelPng();

  await writeFile(join(swiftDir, 'Matched.png'), png);
  await writeFile(join(figmaDir, 'Matched.png'), png);
  await writeFile(join(figmaDir, 'NeedsSwift.png'), png);
  await writeFile(join(swiftDir, 'NeedsFigma.png'), png);
  await writeFile(join(figmaDir, 'OptionalOnly.png'), png);

  const result = spawnSync(process.execPath, [
    fileURLToPath(new URL('./compare.mjs', import.meta.url)),
    swiftDir,
    figmaDir,
    '--require=Matched,NeedsSwift,NeedsFigma',
  ], { cwd: tempRoot, encoding: 'utf8' });

  assert.equal(result.status, 1);
  assert.match(`${result.stdout}\n${result.stderr}`, /HARD ERROR/);
  assert.match(result.stdout, /2 hard errors/);

  const report = JSON.parse(await readFile(join(tempRoot, 'artifacts/design-drift-report.json'), 'utf8'));
  assert.equal(report.status, 'error');
  assert.equal(report.checked, 1);
  assert.equal(report.skipped, 1);
  assert.deepEqual(report.missingRequired, ['NeedsSwift', 'NeedsFigma']);
  assert.equal(report.hardErrors.length, 2);
  assert.deepEqual(report.hardErrors.find(({ name }) => name === 'NeedsSwift').missing, ['swiftui']);
  assert.deepEqual(report.hardErrors.find(({ name }) => name === 'NeedsFigma').missing, ['figma']);
  for (const name of report.missingRequired) {
    const evidenceError = report.results.find((entry) => entry.name === name);
    assert.equal(evidenceError.status, 'error');
    assert.equal(evidenceError.passed, false);
    assert.equal(evidenceError.errorCode, 'missing-required-evidence');
    assert.equal(evidenceError.hardError, true);
  }
} finally {
  await rm(tempRoot, { recursive: true, force: true });
}

console.log('Design drift missing-evidence tests passed');
