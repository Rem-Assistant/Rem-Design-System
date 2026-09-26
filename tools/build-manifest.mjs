#!/usr/bin/env node
// Agent-facing manifest generator: agent/props.json + components/*.md front-matter + tokens.json
// -> manifest.json (one read gives every component's props, variants, rules, and metadata).
//   node tools/build-manifest.mjs           # write manifest.json
//   node tools/build-manifest.mjs --check    # CI drift guard: exit 1 if stale
//
// This is the "author once, render many" contract: the same .md front-matter `rules` and the same
// props.json drive the human doc pages, the agent manifest, and (via adherence) the lint rules.

import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const OUT = join(ROOT, "manifest.json");

const ORDER = [
  "Text", "Surface", "Card", "Pill", "ListRow", "ContainedIcon", "Button",
  "MessageBubble", "ComposerBar", "ContextualMessage", "ThinkingBlock", "TypingDots", "Toast", "ToolResultCard",
  "TaskEventRow", "SuggestedTaskRow", "ProposalCard", "DateNavigationHeader",
  "CalendarEventsCard", "RemindersCard",
  "AgendaView", "InboxView", "SettingsView", "ChatScreen", "ConversationView",
  "OnboardingSequencer",
];

const PROPS = JSON.parse(readFileSync(join(ROOT, "agent", "props.json"), "utf8"));
const TOK = JSON.parse(readFileSync(join(ROOT, "tokens", "tokens.json"), "utf8"));

function frontmatter(name) {
  const md = readFileSync(join(ROOT, "components", `${name}.md`), "utf8");
  const m = md.replace(/^<!--[\s\S]*?-->\s*/, "").match(/^---\n([\s\S]*?)\n---\n/);
  return m ? m[1] : "";
}
const scalar = (fm, k) => (fm.match(new RegExp(`^${k}:\\s*(.+)$`, "m")) || [])[1]?.trim();
function rules(fm) {
  const out = [];
  let inR = false, cur = null;
  for (const ln of fm.split("\n")) {
    if (/^rules:\s*$/.test(ln)) { inR = true; continue; }
    if (inR && /^\S/.test(ln)) break;
    if (!inR) continue;
    const id = ln.match(/^\s*-\s*id:\s*(.+)$/);
    if (id) { cur = { id: id[1].trim() }; out.push(cur); continue; }
    const kv = ln.match(/^\s*(do|dont|enforced_by):\s*(.+)$/);
    if (kv && cur) cur[kv[1]] = kv[2].trim().replace(/^"|"$/g, "").replace(/\\"/g, '"');
  }
  return out;
}
const list = (v) => (v ? v.replace(/[[\]]/g, "").split(",").map((s) => s.trim()).filter(Boolean) : undefined);

const components = {};
for (const name of ORDER) {
  const fm = frontmatter(name);
  const p = PROPS.components[name] || {};
  components[name] = {
    group: p.kind ? undefined : scalar(fm, "group"),
    kind: p.kind || scalar(fm, "kind"),
    mirrors: scalar(fm, "mirrors"),
    status: scalar(fm, "status") || "draft",
    composedOf: list(scalar(fm, "composed_of")),
    replaces: p.replaces,
    extends: p.extends,
    props: p.props || {},
    rules: rules(fm),
  };
  // drop undefined keys for a clean file
  for (const k of Object.keys(components[name])) if (components[name][k] === undefined) delete components[name][k];
}

const manifest = {
  $note: "Agent-facing contract for the Rem Design System. One read gives every component's props, variants, usage rules, and platform mirror. GENERATED — do not edit. Sources: agent/props.json + components/*.md front-matter + tokens/tokens.json.",
  $version: PROPS.$version || "0.1.0",
  tokens: "tokens/tokens.json",
  tokenVersion: TOK.$version,
  count: ORDER.length,
  components,
};

const json = JSON.stringify(manifest, null, 2) + "\n";
if (process.argv.includes("--check")) {
  let have = "";
  try { have = readFileSync(OUT, "utf8"); } catch {}
  if (have !== json) { console.error("DRIFT: manifest.json is stale. Run: node tools/build-manifest.mjs"); process.exit(1); }
  console.log("manifest: in sync ✓");
} else {
  writeFileSync(OUT, json);
  console.log(`wrote ${OUT} (${ORDER.length} components)`);
}
