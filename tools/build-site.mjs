#!/usr/bin/env node
// Static site generator for the Rem Design System docs — Carbon-styled, with tabbed component pages.
//   node tools/build-site.mjs   → ./site
// Component pages split into Carbon-style tabs: Usage · Style · Code · Accessibility.
// No framework — `marked` for Markdown. Deployed to GitHub Pages by .github/workflows/pages.yml.

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join, basename } from "node:path";
import { marked } from "marked";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const OUT = join(ROOT, "site");
mkdirSync(OUT, { recursive: true });

const GROUPS = {
  Primitives: ["Text", "Surface", "Card", "Pill", "ListRow", "ContainedIcon", "Button"],
  "Chat & conversation": ["MessageBubble", "ComposerBar", "ContextualMessage", "ThinkingBlock", "TypingDots", "Toast", "ToolResultCard"],
  "Tasks & agenda": ["TaskEventRow", "SuggestedTaskRow", "ProposalCard", "DateNavigationHeader"],
  "Tool-result cards": ["CalendarEventsCard", "RemindersCard"],
  "Screen templates": ["AgendaView", "InboxView", "SettingsView", "ChatScreen", "ConversationView"],
};
const COMPONENT_NAMES = new Set(Object.values(GROUPS).flat());

// Live Figma preview: the Rem Design System file (built from tokens.json → Figma variables) and the
// frame node-id per component. Embeds render once the file is shared "Anyone with the link · view".
const FIGMA_FILE = "af4yDqCzp57jds9lkFiIaO";
const FIGMA_SLUG = "Rem-Design-System";
// Corrected (app-traced, SF Pro) frames are pointed at their new node-ids as the sweep lands;
// the rest keep their v1 frame until corrected.
const FIGMA_NODES = {
  Text: "13-27", Surface: "8-12", Card: "11-11", Pill: "64-14", ListRow: "14-3", ContainedIcon: "12-19", Button: "6-11",
  MessageBubble: "50-7", ComposerBar: "53-2", ContextualMessage: "21-40", ThinkingBlock: "63-20", TypingDots: "17-3", Toast: "18-25", ToolResultCard: "62-2",
  TaskEventRow: "46-21", SuggestedTaskRow: "48-25", ProposalCard: "54-55", DateNavigationHeader: "43-2",
  CalendarEventsCard: "29-3", RemindersCard: "29-29",
  AgendaView: "49-67", InboxView: "37-3", SettingsView: "39-3", ChatScreen: "33-3", ConversationView: "32-3",
};
function figmaPreview(name) {
  const node = FIGMA_NODES[name];
  if (!node) return "";
  const embed = `https://embed.figma.com/design/${FIGMA_FILE}/${FIGMA_SLUG}?node-id=${node}&embed-host=rem-docs&footer=false&theme=system`;
  const open = `https://www.figma.com/design/${FIGMA_FILE}/${FIGMA_SLUG}?node-id=${node}`;
  return `<div class="preview"><div class="preview-hd"><span class="preview-lbl">Live preview</span><a class="preview-open" href="${open}" target="_blank" rel="noopener">Open in Figma ↗</a></div>`
    + `<div class="figwrap"><iframe class="figframe" title="${esc(name)} — Figma" src="${embed}" allowfullscreen loading="lazy"></iframe>`
    + `<div class="fsignote">Bound to the token variables generated from <code>tokens.json</code>. If the frame is blank, the Figma file isn't shared yet — <a href="${open}" target="_blank" rel="noopener">open it directly ↗</a>.</div></div></div>`;
}

const esc = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
function stripFrontmatter(md) {
  let s = md.replace(/^<!--[\s\S]*?-->\s*/, "");
  const fm = s.match(/^---\n([\s\S]*?)\n---\n/);
  return { body: fm ? s.slice(fm[0].length) : s, fm: fm ? fm[1] : "" };
}
const fmField = (fm, key) => (fm.match(new RegExp(`^${key}:\\s*(.+)$`, "m")) || [])[1]?.trim() || null;

function rewriteLinks(html) {
  return html.replace(/href="([^"]+)"/g, (m, href) => {
    if (/^https?:|^#|^mailto:/.test(href)) return m;
    const b = basename(href);
    if (href.endsWith("tokens.json")) return 'href="tokens.html"';
    if (b === "SPEC.md") return 'href="spec.html"';
    if (b === "COMPONENT.md") return 'href="template.html"';
    if (b === "README.md") return `href="${href.includes("components") ? "components.html" : "index.html"}"`;
    if (b.endsWith(".md")) return `href="${b.replace(/\.md$/, ".html")}"`;
    return m;
  });
}
// The docs are the cross-platform contract, so show neutral prop types, not the React mirror's.
function neutralizeTypes(html) {
  return html
    .replace(/React\.ReactNode|ReactNode/g, "content")
    .replace(/default_2\.CSSProperties|CSSProperties/g, "style")
    .replace(/\((?:v)?\)\s*=(?:&gt;|>)\s*void/g, "action")
    .replace(/\bboolean\b/g, "bool");
}

function nav(active) {
  const link = (out, label) => `<a href="${out}"${out === active ? ' class="on"' : ""}>${esc(label)}</a>`;
  let s = `<div class="navgroup"><div class="navhdr">Overview</div>${link("index.html", "Rem Design System")}${link("spec.html", "Specification")}</div>`;
  s += `<div class="navgroup"><div class="navhdr">Foundations</div>${link("tokens.html", "Design tokens")}${link("template.html", "Doc template")}${link("components.html", "All components")}</div>`;
  for (const [g, names] of Object.entries(GROUPS))
    s += `<div class="navgroup"><div class="navhdr">${esc(g)}</div>${names.map((n) => link(`${n}.html`, n)).join("")}</div>`;
  return s;
}

function shell(out, title, main) {
  return `<!DOCTYPE html><html lang="en"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)} · Rem Design System</title>
<meta name="description" content="Rem Design System — ${esc(title)}">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@300;400;600&family=IBM+Plex+Mono:wght@400;600&display=swap" rel="stylesheet">
<style>${CSS}</style></head><body>
<header class="uishell"><a class="brand" href="index.html"><b>Rem</b> Design System</a><span class="ns">iOS · macOS</span></header>
<div class="shell"><aside class="side"><nav>${nav(out)}</nav></aside>${main}</div>
<script>${TABJS}</script></body></html>`;
}
const write = (out, title, main) => writeFileSync(join(OUT, out), shell(out, title, main));

const FOOT = `<footer class="foot">Rem Design System · generated from <code>tokens.json</code> + component docs · <a href="https://github.com/Rem-Assistant/Rem-Design-System">source</a></footer>`;

// Plain single-column page (SPEC, tokens, indexes).
function simplePage(relPathOrHtml, out, title, isRaw = false) {
  let inner;
  if (isRaw) inner = relPathOrHtml;
  else {
    const { body } = stripFrontmatter(readFileSync(join(ROOT, relPathOrHtml), "utf8"));
    inner = neutralizeTypes(rewriteLinks(marked.parse(body)));
  }
  write(out, title, `<main class="content"><article class="prose">${inner}</article>${FOOT}</main>`);
}

// Component page: header + Carbon-style tabs (Usage / Style / Code / Accessibility).
const TAB_MAP = [
  ["Usage", ["Overview", "When", "Anatomy", "Variants", "Do", "Composed"]],
  ["Style", ["Tokens"]],
  ["Code", ["API"]],
  ["Accessibility", ["Accessibility"]],
];
function componentPage(relPath, out, name) {
  const { body, fm } = stripFrontmatter(readFileSync(join(ROOT, relPath), "utf8"));
  let html = neutralizeTypes(rewriteLinks(marked.parse(body)));
  const cut = html.indexOf("<h2");
  let head = cut >= 0 ? html.slice(0, cut) : html;
  const rest = cut >= 0 ? html.slice(cut) : "";
  // strip the rendered <h1> from head — the page header renders it separately
  const h1 = (head.match(/<h1[^>]*>(.*?)<\/h1>/) || [])[1] || esc(name);
  head = head.replace(/<h1[^>]*>.*?<\/h1>/, "");

  const chips = [];
  for (const [k, label] of [["mirrors", "mirrors"], ["kind", null], ["status", "status"], ["composed_of", "composed of"]]) {
    const v = fmField(fm, k);
    if (v) chips.push(label ? `${label}: ${v.replace(/[[\]]/g, "")}` : v);
  }
  const meta = chips.length ? `<div class="meta">${chips.map((c) => `<span class="chip">${esc(c)}</span>`).join("")}</div>` : "";

  const sections = rest.split(/(?=<h2)/).filter((s) => s.trim());
  const buckets = { Usage: [], Style: [], Code: [], Accessibility: [] };
  for (const sec of sections) {
    const plain = ((sec.match(/<h2[^>]*>(.*?)<\/h2>/) || [])[1] || "").replace(/<[^>]+>/g, "");
    const hit = TAB_MAP.find(([, keys]) => keys.some((k) => plain.includes(k)));
    buckets[hit ? hit[0] : "Usage"].push(sec);
  }
  const active = "Usage";
  const tabsAvail = TAB_MAP.map(([t]) => t).filter((t) => buckets[t].length);
  const tablist = tabsAvail.map((t) =>
    `<button class="tab${t === active ? " on" : ""}" role="tab" aria-selected="${t === active}" data-tab="${t}">${t}</button>`).join("");
  const panels = tabsAvail.map((t) =>
    `<section class="panel${t === active ? " on" : ""}" role="tabpanel" data-panel="${t}"><div class="prose">${buckets[t].join("\n")}</div></section>`).join("");

  const main = `<main class="content">
<div class="phead"><div class="crumb">Components</div><h1>${h1}</h1>${meta}${head}${figmaPreview(name)}</div>
<div class="tabs" role="tablist">${tablist}</div>
${panels}
${FOOT}</main>`;
  write(out, name, main);
}

// tokens reference page
function tokensPage() {
  const T = JSON.parse(readFileSync(join(ROOT, "tokens", "tokens.json"), "utf8"));
  const num = (o) => Object.entries(o).filter(([k]) => !k.startsWith("$"));
  const row = (c) => `<tr>${c.map((x) => `<td>${x}</td>`).join("")}</tr>`;
  const table = (h, rows) => `<table><thead><tr>${h.map((x) => `<th>${x}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table>`;
  const sw = (hex) => (hex && hex.startsWith("#") ? `<span class="sw" style="background:${hex}"></span>` : "");
  const spacing = table(["Token", "px"], num(T.spacing).map(([k, v]) => row([`space.${k}`, v])));
  const radius = table(["Token", "px"], num(T.radius).map(([k, v]) => row([`radius.${k}`, v])));
  const type = table(["Role", "Size", "Weight", "Dynamic Type style"],
    Object.entries(T.typography.roles).map(([k, r]) => row([k, r.size, r.weight, r.textStyle + (r.family === "mono" ? " · mono" : "")])));
  const cr = [];
  cr.push(row(["brand.blue", `${sw(T.color.brand.blue.value)}${T.color.brand.blue.value}`, "value"]));
  cr.push(row(["brand.blueOnFill", `${sw(T.color.brand.blueOnFill.light)}light ${T.color.brand.blueOnFill.light} / dark ${T.color.brand.blueOnFill.dark}`, "value"]));
  const ref = (n, t) => { const w = typeof t.web === "object" ? t.web : {}; cr.push(row([n, `${sw(w.light)}light ${w.light || "—"} / dark ${w.dark || "—"}`, "reference"])); };
  ref("background.primary", T.color.background.primary); ref("background.secondary", T.color.background.secondary); ref("background.tertiary", T.color.background.tertiary);
  ref("label.primary", T.color.label.primary); ref("label.secondary", T.color.label.secondary); ref("label.tertiary", T.color.label.tertiary);
  ref("separator", T.color.separator); ref("fill.tertiary", T.color.fill.tertiary);
  for (const [k, v] of num(T.color.system)) ref(`system.${k}`, v);
  const colors = table(["Token", "Value (web)", "Kind"], cr);
  const inner = `<h1>Design tokens</h1>
<p>Generated from the single source, <code>tokens.json</code>. <strong>Value</strong> tokens are literals on every platform; <strong>reference</strong> tokens keep the adaptive Apple system color in Swift and use the approximate hex shown here on the web. See the <a href="spec.html">Specification</a> §2.</p>
<h2>Spacing</h2>${spacing}<h2>Radius</h2>${radius}
<h2>Typography</h2><p>Roles map to Apple <code>Font.TextStyle</code> (Dynamic Type); sizes equal Apple's defaults.</p>${type}
<h2>Color</h2>${colors}
<h2>Opacity</h2>${table(["Token", "Value"], [row(["opacity.deemphasized", T.opacity.deemphasized.value])])}`;
  simplePage(inner, "tokens.html", "Design tokens", true);
}

const CSS = `
:root{--bg:#fff;--fg:#161616;--fg2:#525252;--fg3:#6f6f6f;--line:#e0e0e0;--accent:#0f62fe;--side:#fff;--sidehov:#e8e8e8;--code:#f4f4f4;--th:#f4f4f4;--hd:#161616}
@media (prefers-color-scheme:dark){:root{--bg:#161616;--fg:#f4f4f4;--fg2:#c6c6c6;--fg3:#a8a8a8;--line:#393939;--accent:#78a9ff;--side:#262626;--sidehov:#333;--code:#262626;--th:#262626;--hd:#000}}
*{box-sizing:border-box}html,body{margin:0}
body{background:var(--bg);color:var(--fg);font-family:"IBM Plex Sans",system-ui,sans-serif;font-size:16px;line-height:1.5;-webkit-font-smoothing:antialiased}
code,pre{font-family:"IBM Plex Mono",ui-monospace,monospace}
a{color:var(--accent);text-decoration:none}a:hover{text-decoration:underline}
.uishell{position:sticky;top:0;z-index:10;display:flex;align-items:center;gap:16px;height:48px;padding:0 16px;background:var(--hd);color:#f4f4f4;border-bottom:1px solid #393939}
.uishell .brand{color:#f4f4f4;font-weight:400;font-size:14px;letter-spacing:.01em}.uishell .brand b{font-weight:600}
.uishell .ns{margin-left:auto;color:#a8a8a8;font:400 12px "IBM Plex Mono",monospace}
.shell{display:flex;align-items:flex-start}
.side{position:sticky;top:48px;flex:0 0 256px;height:calc(100vh - 48px);overflow-y:auto;background:var(--side);border-right:1px solid var(--line);padding:16px 0}
.navgroup{margin-bottom:16px}
.navhdr{font-size:12px;letter-spacing:.02em;color:var(--fg3);padding:8px 16px 4px;font-weight:600}
.side a{display:block;color:var(--fg);padding:7px 16px;font-size:14px;border-left:3px solid transparent}
.side a:hover{background:var(--sidehov);text-decoration:none}
.side a.on{border-left-color:var(--accent);background:var(--sidehov);font-weight:600}
.content{flex:1 1 auto;min-width:0;padding:0 0 80px;max-width:960px}
.phead{padding:48px 48px 0}
.crumb{color:var(--fg2);font-size:14px;margin-bottom:8px}
.phead h1{font-size:42px;font-weight:300;line-height:1.1;letter-spacing:-.01em;margin:0 0 12px}
.meta{display:flex;flex-wrap:wrap;gap:8px;margin:0 0 16px}
.chip{font:400 12px "IBM Plex Mono",monospace;color:var(--fg2);background:var(--code);border:1px solid var(--line);border-radius:100px;padding:3px 10px}
.phead>p{color:var(--fg2);font-size:18px;max-width:640px;font-weight:300}
.tabs{display:flex;gap:0;padding:0 48px;border-bottom:1px solid var(--line);position:sticky;top:48px;background:var(--bg);z-index:5}
.tab{appearance:none;background:none;border:0;border-bottom:2px solid transparent;color:var(--fg2);font:400 15px "IBM Plex Sans",sans-serif;padding:14px 16px;margin-bottom:-1px;cursor:pointer}
.tab:hover{color:var(--fg);background:var(--code)}
.tab.on{color:var(--fg);border-bottom-color:var(--accent);font-weight:600}
.panel{display:none;padding:8px 48px 0}.panel.on{display:block}
.prose{max-width:672px}
.prose h2{font-size:24px;font-weight:400;margin:2em 0 .5em}
.prose h3{font-size:16px;font-weight:600;margin:1.6em 0 .4em}
.prose p,.prose li{color:var(--fg);font-size:16px}
.prose h2:first-child{margin-top:1.2em}
blockquote{margin:1.2em 0;padding:.5em 1em;border-left:3px solid var(--accent);background:color-mix(in srgb,var(--accent) 8%,transparent);color:var(--fg2)}
table{border-collapse:collapse;width:100%;margin:1.2em 0;font-size:14px}
th,td{border:1px solid var(--line);padding:9px 14px;text-align:left;vertical-align:top}
th{background:var(--th);font-weight:600}
code{background:var(--code);padding:.1em .4em;border-radius:3px;font-size:.88em}
pre{background:var(--code);padding:16px;border-radius:0;overflow:auto;border:1px solid var(--line)}pre code{background:none;padding:0}
.sw{display:inline-block;width:14px;height:14px;border-radius:2px;border:1px solid var(--line);vertical-align:-2px;margin-right:6px}
.content.prose,article.prose{padding:48px}article.prose h1{font-size:42px;font-weight:300;margin:.1em 0 .5em}
.preview{margin:24px 0 8px;max-width:720px}
.preview-hd{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px}
.preview-lbl{font:600 12px "IBM Plex Mono",monospace;letter-spacing:.04em;text-transform:uppercase;color:var(--fg3)}
.preview-open{font-size:13px}
.figwrap{border:1px solid var(--line);border-radius:6px;overflow:hidden;background:var(--code)}
.figframe{display:block;width:100%;height:460px;border:0}
.fsignote{border-top:1px solid var(--line);padding:8px 12px;font-size:12px;color:var(--fg2);background:var(--bg)}
.foot{margin:64px 48px 0;padding:20px 0;border-top:1px solid var(--line);color:var(--fg2);font-size:13px;max-width:672px}
@media(max-width:820px){.shell{flex-direction:column}.side{position:static;flex-basis:auto;width:100%;height:auto;border-right:0;border-bottom:1px solid var(--line)}.phead,.tabs,.panel{padding-left:16px;padding-right:16px}article.prose{padding:24px 16px}.phead h1,article.prose h1{font-size:32px}.tabs{position:static}}
`;
const TABJS = `document.addEventListener('click',function(e){var t=e.target.closest('.tab');if(!t)return;var main=t.closest('.content');main.querySelectorAll('.tab').forEach(function(x){var on=x===t;x.classList.toggle('on',on);x.setAttribute('aria-selected',on)});main.querySelectorAll('.panel').forEach(function(p){p.classList.toggle('on',p.dataset.panel===t.dataset.tab)})});`;

// ---- build ------------------------------------------------------------------
simplePage("README.md", "index.html", "Rem Design System");
simplePage("SPEC.md", "spec.html", "Specification");
simplePage("templates/COMPONENT.md", "template.html", "Doc template");
simplePage("components/README.md", "components.html", "Components");
tokensPage();
let count = 5;
for (const names of Object.values(GROUPS)) for (const n of names) { componentPage(`components/${n}.md`, `${n}.html`, n); count++; }
console.log(`Built ${count} pages into ${OUT}`);
