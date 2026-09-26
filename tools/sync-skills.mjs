#!/usr/bin/env node
// Rem Design System — process-skill sync.
// Single source: ../skills.json  ->  vendored skill dirs under .claude/skills/.
//
//   node sync-skills.mjs --from <src>          # copy the pinned skills into .claude/skills/
//   node sync-skills.mjs --from <src> --check  # exit 1 if vendored copies drift (CI drift guard)
//
// The tokens.json/generate-tokens.mjs of skill vendoring (see skills.json "$doc"). This repo is the
// Agent Factory harness; the Factory reads skill dirs off disk from the checkout, so the skills must
// live here as committed files. One copy in .claude/skills/ serves both the Factory and Claude Code.
//
// <src> is a local checkout of the source repo (skills.json .source.repo). Its git HEAD must equal
// .source.pin, so this container's sibling checkout and the CI actions/checkout land byte-identical
// trees — the script itself does no network I/O (this container's git proxy is scoped to the Rem
// repos; CI checks the source out at the pin and passes --from). Pass --allow-unpinned only for a
// throwaway local experiment against an unpinned tree.

import { readFileSync, writeFileSync, readdirSync, statSync, mkdirSync, rmSync, chmodSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, join, relative } from "node:path";

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, "..");
const MANIFEST = join(ROOT, "skills.json");
const M = JSON.parse(readFileSync(MANIFEST, "utf8"));

const check = process.argv.includes("--check");
const allowUnpinned = process.argv.includes("--allow-unpinned");
const fromIdx = process.argv.indexOf("--from");
const from = fromIdx !== -1 ? process.argv[fromIdx + 1] : null;

function die(msg) { console.error(msg); process.exit(2); }

if (!from) {
  die(`sync-skills: pass --from <path to a checkout of ${M.source.repo} at ${M.source.pin}>\n` +
      `  local:  node tools/sync-skills.mjs --from ../agent-skills\n` +
      `  CI:     actions/checkout the source at the pin, then --from that path`);
}

// The pin is the contract: refuse a source tree that is not the exact commit skills.json names,
// otherwise "in sync" would mean nothing. --allow-unpinned escapes this for local experiments only.
if (!allowUnpinned) {
  let head;
  try { head = execFileSync("git", ["-C", from, "rev-parse", "HEAD"], { encoding: "utf8" }).trim(); }
  catch { die(`sync-skills: --from ${from} is not a git checkout; cannot verify it is at ${M.source.pin} (use --allow-unpinned to skip)`); }
  if (head !== M.source.pin) {
    die(`sync-skills: --from ${from} is at ${head}, but skills.json pins ${M.source.pin}\n` +
        `  check out the pinned commit, or bump skills.json .source.pin deliberately`);
  }
}

// Recursively list files under dir as {rel, mode(exec bit), bytes}. Deterministic order.
function walk(dir) {
  const out = [];
  const recurse = (d) => {
    for (const ent of readdirSync(d, { withFileTypes: true }).sort((a, b) => a.name < b.name ? -1 : 1)) {
      const p = join(d, ent.name);
      if (ent.isDirectory()) recurse(p);
      else if (ent.isFile()) out.push({ rel: relative(dir, p), exec: (statSync(p).mode & 0o111) !== 0, bytes: readFileSync(p) });
    }
  };
  recurse(dir);
  return out;
}

let drift = false;
for (const skill of M.skills) {
  const src = join(from, skill);
  const dest = join(ROOT, M.dest, skill);
  let srcFiles;
  try { srcFiles = walk(src); } catch { die(`sync-skills: source skill dir missing: ${src}`); }
  if (!srcFiles.some((f) => f.rel === "SKILL.md")) die(`sync-skills: ${src} has no SKILL.md — not a skill dir`);

  if (check) {
    let destFiles = [];
    try { destFiles = walk(dest); } catch { console.error(`DRIFT: ${M.dest}/${skill} is missing`); drift = true; continue; }
    const srcByRel = new Map(srcFiles.map((f) => [f.rel, f]));
    const destByRel = new Map(destFiles.map((f) => [f.rel, f]));
    for (const rel of srcByRel.keys()) if (!destByRel.has(rel)) { console.error(`DRIFT: ${M.dest}/${skill}/${rel} is missing`); drift = true; }
    for (const rel of destByRel.keys()) if (!srcByRel.has(rel)) { console.error(`DRIFT: ${M.dest}/${skill}/${rel} is not in the source`); drift = true; }
    for (const [rel, s] of srcByRel) {
      const d = destByRel.get(rel);
      if (!d) continue;
      if (!s.bytes.equals(d.bytes)) { console.error(`DRIFT: ${M.dest}/${skill}/${rel} differs from source`); drift = true; }
      else if (s.exec !== d.exec) { console.error(`DRIFT: ${M.dest}/${skill}/${rel} exec bit differs from source`); drift = true; }
    }
  } else {
    rmSync(dest, { recursive: true, force: true });
    for (const f of srcFiles) {
      const target = join(dest, f.rel);
      mkdirSync(dirname(target), { recursive: true });
      writeFileSync(target, f.bytes);
      chmodSync(target, f.exec ? 0o755 : 0o644);
    }
    console.log(`synced ${M.dest}/${skill} (${srcFiles.length} files) from ${M.source.pin.slice(0, 8)}`);
  }
}

if (check) {
  if (drift) { console.error("Run: node tools/sync-skills.mjs --from <source checkout at the pin>"); process.exit(1); }
  console.log("skills: vendored copies are in sync ✓");
}
