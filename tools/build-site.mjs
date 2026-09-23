#!/usr/bin/env node
// Static site generator for the Rem Design System docs.
// Renders SPEC + tokens + all component pages into a Carbon-styled static site under ./site/.
//   node tools/build-site.mjs
// No framework — just `marked` for Markdown. Deployed to GitHub Pages by .github/workflows/pages.yml.

import { readFileSync, writeFileSync, mkdirSync, readdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join, basename } from "node:path";
import { marked } from "marked";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const OUT = join(ROOT, "site");
mkdirSync(OUT, { recursive: true });

// ---- nav model (order mirrors components/README.md) -------------------------
const GROUPS = {
  Primitives: ["Text", "Surface", "Card", "Pill", "ListRow", "ContainedIcon", "Button"],
  "Chat & conversation": ["MessageBubble", "ComposerBar", "ContextualMessage", "ThinkingBlock", "TypingDots", "Toast", "ToolResultCard"],
  "Tasks & agenda": ["TaskEventRow", "SuggestedTaskRow", "ProposalCard", "DateNavigationHeader"],
  "Tool-result cards": ["CalendarEventsCard", "RemindersCard"],
  "Screen templates": ["AgendaView", "InboxView", "SettingsView", "ChatScreen", "ConversationView"],
};

// ---- helpers ----------------------------------------------------------------
const esc = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
function stripFrontmatter(md) {
  let s = md.replace(/^<!--[\s\S]*?-->\s*/, ""); // leading HTML comment (template)
  const fm = s.match(/^---\n([\s\S]*?)\n---\n/);
  return { body: fm ? s.slice(fm[0].length) : s, fm: fm ? fm[1] : "" };
}
function fmField(fm, key) {
  const m = fm.match(new RegExp(`^${key}:\\s*(.+)$`, "m"));
  return m ? m[1].trim() : null;
}
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

function nav(active) {
  const link = (out, label) => `<a href="${out}"${out === active ? ' class="on"' : ""}>${esc(label)}</a>`;
  let s = `<div class="navgroup"><div class="navhdr">Overview</div>${link("index.html", "Rem Design System")}${link("spec.html", "Specification")}</div>`;
  s += `<div class="navgroup"><div class="navhdr">Foundations</div>${link("tokens.html", "Design tokens")}${link("template.html", "Doc template")}${link("components.html", "Components")}</div>`;
  for (const [g, names] of Object.entries(GROUPS))
    s += `<div class="navgroup"><div class="navhdr">${esc(g)}</div>${names.map((n) => link(`${n}.html`, n)).join("")}</div>`;
  return s;
}

function page(out, title, contentHtml) {
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)} · Rem Design System</title>
<meta name="description" content="Rem Design System — ${esc(title)}">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;600;700&family=IBM+Plex+Mono:wght@400;600&display=swap" rel="stylesheet">
<style>${CSS}</style>
</head>
<body>
<header class="topbar"><a class="brand" href="index.html">Rem <span>Design System</span></a><span class="tag">iOS · macOS</span></header>
<div class="shell">
<aside class="side"><nav>${nav(out)}</nav></aside>
<main class="content"><article>${contentHtml}</article>
<footer class="foot">Rem Design System · generated from <code>tokens.json</code> + component docs · <a href="https://github.com/Rem-Assistant/Rem-Design-System">source</a></footer>
</main>
</div>
</body>
</html>`;
  writeFileSync(join(OUT, out), html);
}

const CSS = `
:root{--bg:#fff;--fg:#161616;--fg2:#525252;--line:#e0e0e0;--accent:#0f62fe;--side:#f4f4f4;--code:#f4f4f4;--th:#f4f4f4}
@media (prefers-color-scheme:dark){:root{--bg:#161616;--fg:#f4f4f4;--fg2:#a8a8a8;--line:#393939;--accent:#78a9ff;--side:#262626;--code:#262626;--th:#262626}}
*{box-sizing:border-box}
html,body{margin:0}
body{background:var(--bg);color:var(--fg);font-family:"IBM Plex Sans",system-ui,sans-serif;line-height:1.6;font-size:16px}
code,pre{font-family:"IBM Plex Mono",ui-monospace,monospace}
a{color:var(--accent);text-decoration:none}
a:hover{text-decoration:underline}
.topbar{position:sticky;top:0;z-index:5;display:flex;align-items:center;gap:16px;height:48px;padding:0 24px;background:var(--bg);border-bottom:1px solid var(--line)}
.brand{color:var(--fg);font-weight:600;letter-spacing:.01em}
.brand span{color:var(--fg2);font-weight:400}
.tag{margin-left:auto;color:var(--fg2);font-size:12px;font-family:"IBM Plex Mono",monospace}
.shell{display:flex;align-items:flex-start}
.side{position:sticky;top:48px;flex:0 0 264px;height:calc(100vh - 48px);overflow-y:auto;background:var(--side);border-right:1px solid var(--line);padding:20px 12px}
.navgroup{margin-bottom:18px}
.navhdr{font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:var(--fg2);padding:0 12px 6px}
.side a{display:block;color:var(--fg);padding:5px 12px;border-radius:4px;font-size:14px}
.side a:hover{background:rgba(127,127,127,.12);text-decoration:none}
.side a.on{background:var(--accent);color:#fff;font-weight:600}
.content{flex:1 1 auto;min-width:0;padding:40px 40px 80px;max-width:860px}
article h1{font-size:34px;font-weight:600;letter-spacing:-.01em;margin:.2em 0 .4em}
article h2{font-size:22px;font-weight:600;margin:1.8em 0 .5em;padding-top:.4em;border-top:1px solid var(--line)}
article h3{font-size:17px;font-weight:600;margin:1.4em 0 .4em}
article p,article li{color:var(--fg)}
blockquote{margin:1em 0;padding:.5em 1em;border-left:3px solid var(--accent);background:rgba(15,98,254,.06);color:var(--fg2)}
table{border-collapse:collapse;width:100%;margin:1em 0;font-size:14px}
th,td{border:1px solid var(--line);padding:8px 12px;text-align:left;vertical-align:top}
th{background:var(--th);font-weight:600}
code{background:var(--code);padding:.12em .4em;border-radius:4px;font-size:.9em}
pre{background:var(--code);padding:16px;border-radius:6px;overflow:auto;border:1px solid var(--line)}
pre code{background:none;padding:0}
.meta{display:flex;flex-wrap:wrap;gap:8px;margin:.6em 0 1.4em}
.chip{font-family:"IBM Plex Mono",monospace;font-size:12px;color:var(--fg2);background:var(--code);border:1px solid var(--line);border-radius:100px;padding:3px 10px}
.sw{display:inline-block;width:14px;height:14px;border-radius:3px;border:1px solid var(--line);vertical-align:-2px;margin-right:6px}
.foot{margin-top:60px;padding-top:20px;border-top:1px solid var(--line);color:var(--fg2);font-size:13px}
@media(max-width:800px){.shell{flex-direction:column}.side{position:static;flex-basis:auto;width:100%;height:auto;border-right:0;border-bottom:1px solid var(--line)}.content{padding:24px 16px 60px}}
`;

// ---- render markdown pages --------------------------------------------------
function renderMarkdownFile(relPath, out, fallbackTitle) {
  const raw = readFileSync(join(ROOT, relPath), "utf8");
  const { body, fm } = stripFrontmatter(raw);
  let meta = "";
  if (fm) {
    const chips = [];
    const mirrors = fmField(fm, "mirrors"); if (mirrors) chips.push(`mirrors: ${mirrors}`);
    const kind = fmField(fm, "kind"); if (kind) chips.push(kind);
    const status = fmField(fm, "status"); if (status) chips.push(`status: ${status}`);
    const composed = fmField(fm, "composed_of"); if (composed) chips.push(`composed of: ${composed.replace(/[\[\]]/g, "")}`);
    if (chips.length) meta = `<div class="meta">${chips.map((c) => `<span class="chip">${esc(c)}</span>`).join("")}</div>`;
  }
  let html = marked.parse(body);
  html = rewriteLinks(html);
  // insert meta after the first <h1>
  if (meta) html = html.replace(/(<\/h1>)/, `$1\n${meta}`);
  page(out, fallbackTitle, html);
}

// ---- tokens reference page (generated from tokens.json) ---------------------
function tokensPage() {
  const T = JSON.parse(readFileSync(join(ROOT, "tokens", "tokens.json"), "utf8"));
  const row = (cells) => `<tr>${cells.map((c) => `<td>${c}</td>`).join("")}</tr>`;
  const table = (head, rows) => `<table><thead><tr>${head.map((h) => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table>`;
  const num = (o) => Object.entries(o).filter(([k]) => !k.startsWith("$"));

  const spacing = table(["Token", "px"], num(T.spacing).map(([k, v]) => row([`space.${k}`, v])));
  const radius = table(["Token", "px"], num(T.radius).map(([k, v]) => row([`radius.${k}`, v])));
  const type = table(["Role", "Size", "Weight", "Text style (Dynamic Type)"],
    Object.entries(T.typography.roles).map(([k, r]) => row([k, r.size, r.weight, r.textStyle + (r.family === "mono" ? " · mono" : "")])));
  const sw = (hex) => hex && hex.startsWith("#") ? `<span class="sw" style="background:${hex}"></span>` : "";
  const colorRows = [];
  colorRows.push(row([`brand.blue`, `${sw(T.color.brand.blue.value)}${T.color.brand.blue.value}`, "value"]));
  colorRows.push(row([`brand.blueOnFill`, `${sw(T.color.brand.blueOnFill.light)}light ${T.color.brand.blueOnFill.light} / dark ${T.color.brand.blueOnFill.dark}`, "value"]));
  const ref = (name, tok) => {
    const w = typeof tok.web === "object" ? tok.web : {};
    colorRows.push(row([name, `${sw(w.light)}light ${w.light || "—"} / dark ${w.dark || "—"}`, "reference"]));
  };
  ref("background.primary", T.color.background.primary); ref("background.secondary", T.color.background.secondary); ref("background.tertiary", T.color.background.tertiary);
  ref("label.primary", T.color.label.primary); ref("label.secondary", T.color.label.secondary); ref("label.tertiary", T.color.label.tertiary);
  ref("separator", T.color.separator); ref("fill.tertiary", T.color.fill.tertiary);
  for (const [k, v] of num(T.color.system)) ref(`system.${k}`, v);
  const colors = table(["Token", "Value (web)", "Kind"], colorRows);

  const body = `<h1>Design tokens</h1>
<p>Generated from the single source, <code>tokens.json</code>. <strong>Value</strong> tokens are literals on every platform; <strong>reference</strong> tokens keep the adaptive Apple system color in Swift and use the approximate hex shown here on the web. See the <a href="spec.html">Specification</a> §2.</p>
<h2>Spacing</h2>${spacing}
<h2>Radius</h2>${radius}
<h2>Typography</h2><p>Roles map to Apple <code>Font.TextStyle</code> (Dynamic Type). Sizes equal Apple's defaults.</p>${type}
<h2>Color</h2>${colors}
<h2>Opacity</h2>${table(["Token", "Value"], [row(["opacity.deemphasized", T.opacity.deemphasized.value])])}`;
  page("tokens.html", "Design tokens", body);
}

// ---- build ------------------------------------------------------------------
renderMarkdownFile("README.md", "index.html", "Rem Design System");
renderMarkdownFile("SPEC.md", "spec.html", "Specification");
renderMarkdownFile("templates/COMPONENT.md", "template.html", "Doc template");
renderMarkdownFile("components/README.md", "components.html", "Components");
tokensPage();
let count = 5;
for (const names of Object.values(GROUPS))
  for (const n of names) { renderMarkdownFile(`components/${n}.md`, `${n}.html`, n); count++; }

console.log(`Built ${count} pages into ${OUT}`);
