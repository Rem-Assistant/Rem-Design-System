// Local constituent mapping validation. This does not claim a standalone Code Connect master,
// live publication, native rendering fidelity, or completion of the release evidence gate.
import { isDeepStrictEqual } from 'node:util';
import { parserlessMappingMatches } from './component-code-connect.mjs';

export const suggestionSectionSource = 'Sources/RemDesignSystem/Agenda/SuggestionSection.swift';
const composeSource = 'compose/RemDesignSystem/rows/SuggestionSection.kt';
// The "Suggestions region" frame 2336:19714 is a SectionHeader, standalone rows and a Text · Accent
// Button ("See more · Plain" is the instance's layer name). No Section/rows surface wraps the rows.
const expectedConstituents = [
  { component: 'SectionHeader', nodeId: '161:68',
    templates: { swiftui: 'code-connect/swiftui/SectionHeader.figma.ts', compose: 'code-connect/compose/SectionHeader.figma.ts' } },
  { component: 'AgendaSuggestionRow', nodeId: '2336:19583',
    templates: { swiftui: 'code-connect/swiftui/AgendaSuggestionRow.figma.ts', compose: 'code-connect/compose/AgendaSuggestionRow.figma.ts' } },
  { component: 'Button', nodeId: '377:8', variant: { Style: 'Text · Accent' }, variantNodeId: '377:4',
    templates: { swiftui: 'Sources/RemDesignSystem/Buttons/RemButton.figma.swift', compose: 'code-connect/compose/RemButton.figma.ts' } },
];
const identities = {
  SectionHeader: {
    swiftui: { source: 'Sources/RemDesignSystem/Rows/RemSection.swift', component: 'SectionHeader' },
    compose: { source: 'compose/RemDesignSystem/rows/RemSection.kt', component: 'SectionHeader' },
  },
  Button: {
    swiftui: { source: 'Sources/RemDesignSystem/Buttons/RemButtonStyle.swift', component: 'RemButtonStyle' },
    compose: { source: 'compose/RemDesignSystem/buttons/RemButton.kt', component: 'RemButton' },
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
    check(isDeepStrictEqual(contract.constituents, expectedConstituents), 'canonical SectionHeader, AgendaSuggestionRow and Text · Accent Button constituent bindings changed');
    check(isDeepStrictEqual(contract.references, [
      { nodeId: '2049:10080', type: 'INSTANCE' }, { nodeId: '2336:19714', type: 'FRAME' },
    ]), 'instance/frame references must not become mapping targets');
    check(isDeepStrictEqual(contract.behavior, { header: 'Suggestions', defaultInlineLimit: 3,
      overflowAction: 'See more', orderingOwner: 'host', empty: 'hidden' }), 'composition behavior changed');
    for (const entry of expectedConstituents) for (const platform of ['swiftui', 'compose']) {
      const text = read(entry.templates[platform]);
      const identity = identities[entry.component][platform];
      read(identity.source); // a matching annotation cannot stand in for a missing native source
      if (entry.component === 'Button' && platform === 'swiftui') {
        // SwiftUI's Button mapping is the co-located Swift Code Connect file, not a parserless template.
        const swiftCode = stripComments(text);
        check(/let component = RemButtonStyle\.self/.test(swiftCode) &&
          /"https:\/\/www\.figma\.com\/design\/af4yDqCzp57jds9lkFiIaO\/[^"?]*\?node-id=377-8"/.test(swiftCode) &&
          /"Text · Accent":\s*RemButtonVariant\.textAccent/.test(swiftCode), 'swiftui Button: Text · Accent mapping missing');
        continue;
      }
      check(parserlessMappingMatches(text, identity), `${platform} ${entry.component}: invalid source/component/template`);
      const urlLine = text.match(/^\/\/ url=(.+)$/m)?.[1];
      const url = new URL(urlLine);
      check(url.pathname.startsWith('/design/af4yDqCzp57jds9lkFiIaO/') &&
        url.searchParams.get('node-id')?.replace('-', ':') === entry.nodeId,
        `${platform} ${entry.component}: wrong canonical node`);
      const templateCode = stripComments(text);
      if (entry.component === 'SectionHeader') {
        check(/^\s*const text\s*=\s*instance\.getString\('Header'\)/m.test(templateCode), `${platform} SectionHeader: Header mapping missing`);
      } else if (entry.component === 'Button') {
        check(/^\s*const variant\s*=\s*instance\.getEnum\('Style',/m.test(templateCode) &&
          /'Text · Accent':\s*'RemButtonVariant\.TextAccent'/.test(templateCode), `${platform} Button: Text · Accent mapping missing`);
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
    check(/RemButtonStyle\(\.textAccent\)/.test(swift) && /RemButtonVariant\.TextAccent/.test(compose),
      'See more must be the canonical Text · Accent Button');
    check(!/RemSection\(|Section\s*\{/.test(swift) && !/RemSection\(/.test(compose), 'suggestion rows stay standalone, with no Section wrapper');
  } catch (error) { errors.push(`unreadable composition dependency: ${error.message}`); }
  return errors;
}
