// Local constituent mapping validation. This does not claim a standalone Code Connect master,
// live publication, native rendering fidelity, or completion of the release evidence gate.
import { isDeepStrictEqual } from 'node:util';
import { parserlessMappingMatches } from './component-code-connect.mjs';

export const suggestionSectionSource = 'Sources/RemDesignSystem/Agenda/SuggestionSection.swift';
const composeSource = 'compose/RemDesignSystem/rows/SuggestionSection.kt';
const expectedConstituents = [
  { component: 'Section', nodeId: '1307:667', variant: { Style: 'Plain' }, variantNodeId: '1307:660',
    templates: { swiftui: 'code-connect/swiftui/Section.figma.ts', compose: 'code-connect/compose/Section.figma.ts' } },
  { component: 'AgendaSuggestionRow', nodeId: '2336:19583',
    templates: { swiftui: 'code-connect/swiftui/AgendaSuggestionRow.figma.ts', compose: 'code-connect/compose/AgendaSuggestionRow.figma.ts' } },
];
const identities = {
  Section: {
    swiftui: { source: 'Sources/RemDesignSystem/Rows/RemSection.swift', component: 'SwiftUI.Section' },
    compose: { source: 'compose/RemDesignSystem/rows/RemSection.kt', component: 'RemSection' },
  },
  AgendaSuggestionRow: {
    swiftui: { source: 'Sources/RemDesignSystem/Agenda/AgendaSuggestionRow.swift', component: 'AgendaSuggestionRow' },
    compose: { source: 'compose/RemDesignSystem/rows/AgendaSuggestionRow.kt', component: 'AgendaSuggestionRow' },
  },
};

// read is injected for negative fixtures: absent/misdirected constituents must fail closed.
export function suggestionSectionMappingErrors(read) {
  const errors = [];
  const stripComments = text => text.replace(/\/\*[\s\S]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
  const check = (condition, message) => { if (!condition) errors.push(message); };
  try {
    const contract = JSON.parse(read('code-connect/SuggestionSection.composition.json'));
    check(contract.schemaVersion === 1 && contract.kind === 'constituent-composition' &&
      contract.component === 'SuggestionSection' && contract.standaloneMaster === null,
      'SuggestionSection must declare its composition identity with no standalone master');
    check(contract.figmaFileKey === 'af4yDqCzp57jds9lkFiIaO', 'wrong Figma file');
    check(isDeepStrictEqual(contract.sources, { swiftui: suggestionSectionSource, compose: composeSource }), 'wrong native composition sources');
    check(isDeepStrictEqual(contract.constituents, expectedConstituents), 'canonical Section Plain and AgendaSuggestionRow constituent bindings changed');
    check(isDeepStrictEqual(contract.references, [
      { nodeId: '2049:10080', type: 'INSTANCE' }, { nodeId: '2336:19714', type: 'FRAME' },
    ]), 'instance/frame references must not become mapping targets');
    check(isDeepStrictEqual(contract.behavior, { header: 'Suggestions', defaultInlineLimit: 3,
      overflowAction: 'See more', orderingOwner: 'host', empty: 'hidden' }), 'composition behavior changed');
    for (const entry of expectedConstituents) for (const platform of ['swiftui', 'compose']) {
      const text = read(entry.templates[platform]);
      const identity = identities[entry.component][platform];
      check(parserlessMappingMatches(text, identity), `${platform} ${entry.component}: invalid source/component/template`);
      read(identity.source); // a matching annotation cannot stand in for a missing native source
      const urlLine = text.match(/^\/\/ url=(.+)$/m)?.[1];
      const url = new URL(urlLine);
      check(url.pathname.startsWith('/design/af4yDqCzp57jds9lkFiIaO/') &&
        url.searchParams.get('node-id')?.replace('-', ':') === entry.nodeId,
        `${platform} ${entry.component}: wrong canonical node`);
      const templateCode = stripComments(text);
      if (entry.component === 'Section') {
        check(/^\s*const rows\s*=\s*instance\.getSlot\('Rows'\)/m.test(templateCode) && /^\s*const style\s*=\s*instance\.getEnum\('Style',/m.test(templateCode) &&
          /'Plain':\s*'(?:\.plain|RemSectionStyle\.Plain)'/.test(templateCode), `${platform} Section: Plain/Rows mapping missing`);
      } else {
        check(/^\s*const action\s*=\s*instance\.getEnum\('action',/m.test(templateCode) && /add:/.test(templateCode) && /move:/.test(templateCode) &&
          /^\s*const title\s*=\s*instance\.getString\('Title'\)/m.test(templateCode) && /^\s*const metadata\s*=\s*instance\.getString\('Metadata'\)/m.test(templateCode),
          `${platform} AgendaSuggestionRow: action/Title/Metadata mapping missing`);
      }
    }
    const code = path => stripComments(read(path));
    const swift = code(suggestionSectionSource);
    const compose = code(composeSource);
    const adapter = code('compose/RemDesignSystem/rows/SuggestedTaskRow.kt');
    check(/AgendaSuggestionRow\(/.test(swift) && /ForEach\(inline\)/.test(swift), 'SwiftUI must compose canonical rows');
    check(/SuggestedTaskRow\(/.test(compose) && /inline\.forEach/.test(compose), 'Compose must use its canonical row adapter');
    for (const [arg, field] of [['action', 'accept'], ['title', 'title'], ['metadata', 'subtitle']]) {
      check(new RegExp(`${arg}\\s*=\\s*suggestion\\.${field}\\b`).test(adapter), `Compose adapter must forward ${arg}`);
    }
    check(/AgendaSuggestionRow\(/.test(adapter), 'Compose adapter must call canonical row');
    check(/suggestions\.prefix\(max\(0, inlineLimit\)\)/.test(swift) &&
      /suggestions\.take\(inlineLimit\.coerceAtLeast\(0\)\)/.test(compose), 'rows must respect the nonnegative inline bound');
    check(/onAccept\s*=\s*onAccept\b/.test(adapter) && /onDismiss\s*=\s*onDismiss\b/.test(adapter), 'adapter must forward both callbacks');
    check(/defaultInlineLimit\s*=\s*3\b/.test(swift) && /inlineLimit:\s*Int\s*=\s*3\b/.test(compose), 'inline bound must remain three');
    check(/!suggestions\.isEmpty/.test(swift) && /suggestions\.isEmpty\(\)\) return/.test(compose), 'empty composition must stay hidden');
    for (const [platform, text] of [['swiftui', swift], ['compose', compose]]) {
      check(text.includes('"Suggestions"') && text.includes('"See more"') && text.includes('overflow > 0'), `${platform}: header/overflow contract missing`);
      check(text.includes('onAccept(suggestion)') && text.includes('onDismiss(suggestion)'), `${platform}: callbacks must retain item identity`);
    }
  } catch (error) { errors.push(`unreadable composition dependency: ${error.message}`); }
  return errors;
}
