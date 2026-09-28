const SAFE_NAME = /^[A-Za-z0-9][A-Za-z0-9._ -]*$/;
const FIGMA_NODE = /^\d+:\d+$/;
const PNG_SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

export function assertPng(bytes, name) {
  if (bytes.length < PNG_SIGNATURE.length || !bytes.subarray(0, PNG_SIGNATURE.length).equals(PNG_SIGNATURE)) {
    throw new Error(`Figma export is not a PNG: ${name}`);
  }
}

export function contractReferenceItems(contracts) {
  if (!contracts || typeof contracts !== 'object' || Array.isArray(contracts)) {
    throw new Error('contracts must be an object');
  }
  const items = [];
  const names = new Map();
  const nodes = new Map();
  for (const [contractName, contract] of Object.entries(contracts)) {
    for (const field of ['references', 'waypoints']) {
      const configured = contract?.[field] ?? {};
      if (!configured || typeof configured !== 'object' || Array.isArray(configured)) {
        throw new Error(`${contractName}.${field} must be an object`);
      }
      for (const [key, item] of Object.entries(configured)) {
        const name = item?.name;
        const node = item?.node;
        if (!SAFE_NAME.test(name ?? '')) {
          throw new Error(`invalid export name for ${contractName}.${field}.${key}`);
        }
        if (!FIGMA_NODE.test(node ?? '')) {
          throw new Error(`invalid Figma node for ${contractName}.${field}.${key}`);
        }
        const normalizedName = name.toLowerCase();
        const priorNode = names.get(normalizedName);
        if (priorNode && priorNode !== node) {
          throw new Error(`export name ${name} maps to multiple Figma nodes`);
        }
        const priorName = nodes.get(node);
        if (priorName && priorName !== name) {
          throw new Error(`Figma node ${node} maps to multiple export names`);
        }
        if (!priorNode) {
          items.push({ name, node });
          names.set(normalizedName, node);
          nodes.set(node, name);
        }
      }
    }
  }
  return items;
}

export function mergeExportItems(...groups) {
  const merged = [];
  const names = new Map();
  const nodes = new Map();
  for (const item of groups.flat()) {
    if (!SAFE_NAME.test(item?.name ?? '')) {
      throw new Error('invalid manifest export name');
    }
    if (!FIGMA_NODE.test(item?.node ?? '')) {
      throw new Error(`invalid Figma node for ${item.name}`);
    }
    const normalizedName = item.name.toLowerCase();
    const priorNode = names.get(normalizedName);
    const priorName = nodes.get(item.node);
    if (priorNode && priorNode !== item.node) {
      throw new Error(`export name ${item.name} maps to multiple Figma nodes`);
    }
    if (priorName && priorName !== item.name) {
      throw new Error(`Figma node ${item.node} maps to multiple export names`);
    }
    if (!priorNode) {
      merged.push(item);
      names.set(normalizedName, item.node);
      nodes.set(item.node, item.name);
    }
  }
  return merged;
}
