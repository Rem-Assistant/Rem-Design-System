// Reusable build primitives for the Figma Plugin API (the `figma` global; what `use_figma` runs).
// Inject the bodies of these into a use_figma call, or keep them here as the canonical reference so
// every generation binds variables / renders real glyphs / lays out auto-layout the same way.
//
// Colour variables: look them up once by name into a map. Local "Color" collection holds the
// iOS-semantic tokens (background/primary, label/secondary, system/red, brand/blue, …).

export async function loadColorVars() {
  const cols = await figma.variables.getLocalVariableCollectionsAsync();
  const coll = cols.find((c) => c.name === 'Color');
  const map = {};
  for (const id of coll.variableIds) { const v = await figma.variables.getVariableByIdAsync(id); map[v.name] = v; }
  return map; // e.g. V['label/secondary']
}

// Bind a node's first fill to a colour variable WITHOUT losing its opacity
// (setBoundVariableForPaint resets opacity to 1 — the #1 gotcha).
export function bindFill(node, variable, opacity) {
  const f = (node.fills && node.fills !== figma.mixed ? node.fills.map((p) => ({ ...p })) : [{ type: 'SOLID', color: { r: 0, g: 0, b: 0 } }]);
  const orig = opacity ?? f[0].opacity ?? 1;
  f[0] = figma.variables.setBoundVariableForPaint(f[0], 'color', variable);
  f[0].opacity = orig;
  node.fills = f;
  return node;
}

// Text bound to a colour variable. style: 'Regular'|'Semibold'|'Bold'. Load the font first.
export async function text(str, { style = 'Regular', size = 17, color, align } = {}) {
  await figma.loadFontAsync({ family: 'SF Pro', style });
  const t = figma.createText();
  t.fontName = { family: 'SF Pro', style };
  t.characters = str; t.fontSize = size;
  if (align) t.textAlignHorizontal = align;
  if (color) bindFill(t, color);
  return t;
}

// A real SF Symbol as a glyph (SF Pro + the verified PUA codepoint from sf-symbols-map.md).
export async function symbol(codepointHex, { size = 17, style = 'Semibold', color } = {}) {
  return text(String.fromCodePoint(parseInt(codepointHex, 16)), { style, size, color });
}

// Auto-layout helpers. counter=hug by default; set fillWidth after appending to a parent.
export function stack(dir, { gap = 0, pad = 0, alignCross, alignMain } = {}) {
  const f = figma.createFrame();
  f.fills = [];
  f.layoutMode = dir === 'h' ? 'HORIZONTAL' : 'VERTICAL';
  f.itemSpacing = gap;
  f.primaryAxisSizingMode = 'AUTO';
  f.counterAxisSizingMode = 'AUTO';
  if (typeof pad === 'number') { f.paddingLeft = f.paddingRight = f.paddingTop = f.paddingBottom = pad; }
  if (alignCross) f.counterAxisAlignItems = alignCross;
  if (alignMain) f.primaryAxisAlignItems = alignMain;
  return f;
}
export const hstack = (o) => stack('h', o);
export const vstack = (o) => stack('v', o);

// After resize() (which LOCKS the axis to FIXED), call this to restore hug on an auto-layout frame.
export function rehug(frame, axis = 'both') {
  if (axis !== 'counter') frame.primaryAxisSizingMode = 'AUTO';
  if (axis !== 'primary') frame.counterAxisSizingMode = 'AUTO';
}
