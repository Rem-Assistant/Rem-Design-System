// Deterministic mapping identity check. Full template syntax/property validation remains
// npm run check-code-connect; this guard proves a template belongs to the component being linted.
export function parserlessMappingMatches(text, { source, component }) {
  const annotation = (name) => {
    const values = [...text.matchAll(new RegExp(`^// ${name}=(.+)$`, 'gm'))];
    return values.length === 1 ? values[0][1].trim() : null;
  };
  if (annotation('source') !== source || annotation('component') !== component) return false;
  let url;
  try { url = new URL(annotation('url')); } catch { return false; }
  if (url.protocol !== 'https:' || !['figma.com', 'www.figma.com'].includes(url.hostname) ||
      url.username || url.password || url.port ||
      !/^\/(design|file)\/[A-Za-z0-9]+(?:\/|$)/.test(url.pathname) ||
      !/^\d+[-:]\d+$/.test(url.searchParams.get('node-id') ?? '')) return false;
  const code = text.replace(/\/\*[\s\S]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
  return /import\s+figma\s+from\s+['"]figma['"]/.test(code) &&
    /export\s+default\s*\{/.test(code) &&
    /\bexample\s*:\s*[\s\S]*?figma\.code\s*`/.test(code);
}
