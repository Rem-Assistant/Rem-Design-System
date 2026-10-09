// Offline template behavior and rejection fixtures; not live Figma or native visual evidence.
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { suggestionSectionMappingErrors } from './suggestion-section-mapping.mjs';
import { parserlessMappingMatches } from './component-code-connect.mjs';

const root = new URL('../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');
const contractPath = 'code-connect/SuggestionSection.composition.json';
const contract = JSON.parse(read(contractPath));
const baseline = JSON.parse(read('tools/component-contract.baseline.json')).grandfathered;
const code = (strings, ...values) => strings.flatMap((text, i) => [text, values[i] ?? '']).join('');
function daily(platform, playback) {
  const props = { Playback: playback, Headline: 'A "quoted" $headline', Summary: 'Line one\nLine two' };
  const context = { figma: { code, selectedInstance: {
    getEnum(name, values) {
      assert.equal(name, 'Playback');
      assert.deepEqual(Object.keys(values).sort(), ['Finished', 'Reading', 'Ready', 'Retry']);
      return values[props[name]];
    },
    getString(name) { assert.ok(name === 'Headline' || name === 'Summary'); return props[name]; },
  } }, result: undefined };
  // Templates use only one type annotation. Full TypeScript/parser validation is separate.
  const source = read(`code-connect/${platform}/DailyBriefCard.figma.ts`)
    .replace("import figma from 'figma'", '')
    .replace('(value: string)', '(value)')
    .replace('export default', 'result =');
  vm.runInNewContext(source, context, { timeout: 1000 });
  return context.result;
}

for (const platform of ['swiftui', 'compose']) {
  test(`${platform}: DailyBriefCard binds exact canonical identity and real input names`, () => {
    const text = read(`code-connect/${platform}/DailyBriefCard.figma.ts`);
    const source = platform === 'swiftui' ? 'Sources/RemDesignSystem/AgentSurfaces/DailyBriefCard.swift' : 'compose/RemDesignSystem/agentsurfaces/DailyBriefCard.kt';
    assert.ok(parserlessMappingMatches(text, { source, component: 'DailyBriefCard' }));
    assert.match(text, /node-id=2190-12237\n/);
    const native = read(source);
    for (const arg of ['title', 'summary', 'onTap', 'onRead', 'isReading']) assert.ok(native.includes(`${arg}:`), arg);
  });
  test(`${platform}: Ready/Reading map to the boolean and preserve escaped text`, () => {
    for (const state of ['Ready', 'Reading']) {
      const result = daily(platform, state);
      assert.equal(result.metadata.props.supported, true);
      assert.match(result.example, /^DailyBriefCard\(/);
      assert.match(result.example, new RegExp(`isReading[:= ]+${state === 'Reading'}`));
      assert.ok(result.example.includes('A \\"quoted\\"'));
      assert.ok(result.example.includes(platform === 'compose' ? '\\$headline' : '$headline'));
      assert.ok(!result.example.includes('undefined'));
      assert.ok(!result.example.includes('counts:') && !result.example.includes('playback:'));
    }
  });
  test(`${platform}: Finished/Retry and unknown states never emit a fabricated Ready call`, () => {
    for (const state of ['Finished', 'Retry', 'future-state']) {
      const result = daily(platform, state);
      assert.equal(result.metadata.props.supported, false);
      assert.match(result.example, /Unsupported DailyBriefCard Playback/);
      assert.ok(!result.example.includes('DailyBriefCard('));
      assert.ok(!result.example.includes('isReading: false') && !result.example.includes('isReading = false'));
    }
  });
}

test('exact checked composition closes traceability without a standalone master or baseline', () => {
  assert.deepEqual(suggestionSectionMappingErrors(read), []);
  assert.ok(!baseline.includes('DailyBriefCard:figma'));
  assert.ok(!baseline.includes('SuggestionSection:figma'));
  assert.equal(contract.standaloneMaster, null);
});

test('composition rejects false master, frame/instance targets, swapped sources and missing constituents', () => {
  const changes = [
    c => { c.standaloneMaster = '2336:19714'; },
    c => { c.sources.swiftui = c.sources.compose; },
    c => { c.figmaFileKey = 'other'; },
    c => { c.constituents[0].nodeId = '2049:10080'; },
    c => { c.constituents[0].variant.Style = 'Inset Grouped'; },
    c => { c.constituents[1].nodeId = '2336:19714'; },
    c => { c.constituents[1].templates.swiftui = c.constituents[1].templates.compose; },
    c => { c.constituents.pop(); },
    c => { c.behavior.defaultInlineLimit = 99; },
  ];
  for (const change of changes) {
    const modified = structuredClone(contract); change(modified);
    assert.ok(suggestionSectionMappingErrors(path => path === contractPath ? JSON.stringify(modified) : read(path)).length > 0);
  }
});

test('composition rejects missing native/template dependencies and wrong node/source/property bindings', () => {
  const dependencies = [contractPath, ...contract.constituents.flatMap(x => Object.values(x.templates)),
    ...Object.values(contract.sources), 'compose/RemDesignSystem/rows/SuggestedTaskRow.kt',
    'Sources/RemDesignSystem/Agenda/AgendaSuggestionRow.swift'];
  for (const missing of dependencies) {
    assert.ok(suggestionSectionMappingErrors(path => {
      if (path === missing) throw new Error(`missing ${path}`);
      return read(path);
    }).length > 0, missing);
  }
  for (const platform of ['swiftui', 'compose']) for (const component of ['Section', 'AgendaSuggestionRow']) {
    const target = `code-connect/${platform}/${component}.figma.ts`;
    for (const [pattern, replacement] of [
      [/node-id=\d+-\d+/, 'node-id=2336-19714'],
      [/\/\/ source=.*/, '// source=wrong/source.swift'],
      [component === 'Section' ? /getSlot\('Rows'\)/ : /getString\('Metadata'\)/, "getString('Unknown')"],
    ]) {
      assert.ok(suggestionSectionMappingErrors(path => path === target ? read(path).replace(pattern, replacement) : read(path)).length > 0);
    }
  }
});

test('composition rejects broken row forwarding, item identity, and loss of empty/bounded behavior', () => {
  for (const [target, before, after] of [
    ['compose/RemDesignSystem/rows/SuggestedTaskRow.kt', 'metadata = suggestion.subtitle', 'metadata = "constant"'],
    [contract.sources.swiftui, 'AgendaSuggestionRow(', 'OtherRow('],
    [contract.sources.compose, 'SuggestedTaskRow(', 'OtherRow('],
    [contract.sources.swiftui, 'onAccept(suggestion)', 'onAccept(other)'],
    [contract.sources.compose, 'onDismiss(suggestion)', 'onDismiss(other)'],
    [contract.sources.swiftui, 'defaultInlineLimit = 3', 'defaultInlineLimit = 30'],
    [contract.sources.compose, 'if (suggestions.isEmpty()) return', ''],
    [contract.sources.compose, 'suggestions.take(inlineLimit.coerceAtLeast(0))', 'suggestions'],
    [contract.sources.swiftui, 'suggestions.prefix(max(0, inlineLimit))', 'suggestions'],
  ]) {
    assert.ok(suggestionSectionMappingErrors(path => path === target ? read(path).replace(before, after) : read(path)).length > 0, before);
  }
});


test('constituent template comments cannot impersonate executable property bindings', () => {
  const target = 'code-connect/compose/Section.figma.ts';
  const modified = read(target).replace("instance.getSlot('Rows')", "undefined // instance.getSlot('Rows')");
  assert.ok(suggestionSectionMappingErrors(path => path === target ? modified : read(path)).length > 0);
  const commentOnly = modified.replace("undefined // instance.getSlot('Rows')", "undefined\n// instance.getSlot('Rows')");
  assert.ok(suggestionSectionMappingErrors(path => path === target ? commentOnly : read(path)).length > 0);
});

for (const platform of ['swiftui', 'compose']) {
  const execute = (name, instance) => {
    const source = read(`code-connect/${platform}/${name}.figma.ts`)
      .replace("import figma from 'figma'", '').replace('export default', 'result =');
    const context = { figma: { code, selectedInstance: instance }, result: undefined };
    vm.runInNewContext(source, context, { timeout: 1000 });
    return context.result.example;
  };
  test(`${platform}: constituent rows preserve dynamic add/move and Title/Metadata`, () => {
    for (const action of ['add', 'move']) {
      const snippet = execute('AgendaSuggestionRow', {
        getEnum(name, values) { assert.equal(name, 'action'); return values[action]; },
        getString(name) { assert.ok(['Title', 'Metadata'].includes(name)); return `dynamic-${name}`; },
      });
      assert.ok(snippet.includes('dynamic-Title') && snippet.includes('dynamic-Metadata'));
      assert.ok(snippet.includes(platform === 'swiftui' ? `.${action}` : `SuggestionAccept.${action === 'add' ? 'Add' : 'Move'}`));
      assert.ok(snippet.includes('onAccept') && snippet.includes('onDismiss'));
    }
  });
  test(`${platform}: canonical Section keeps dynamic Rows and Plain style`, () => {
    const snippet = execute('Section', {
      getSlot(name) { assert.equal(name, 'Rows'); return 'dynamic-row-snippet'; },
      getBoolean(name) { assert.ok(['Show Header', 'Show Footer'].includes(name)); return false; },
      getEnum(name, values) { assert.equal(name, 'Style'); return values.Plain; },
    });
    assert.ok(snippet.includes('dynamic-row-snippet'));
    assert.ok(snippet.includes(platform === 'swiftui' ? '.plain' : 'RemSectionStyle.Plain'));
  });
}
