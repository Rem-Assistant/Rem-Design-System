// Offline contract execution, not Figma live evaluation or native snippet compilation.
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import vm from 'node:vm';
import ts from 'typescript';

const root = new URL('../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');
const snapshot = JSON.parse(read('docs/contracts/settings-code-connect-snapshot.json'));
const render = value => Array.isArray(value) ? value.map(render).join('') : value ?? '';
const code = (strings, ...values) => strings.flatMap((text, i) => [text, values[i] ?? '']);

function run(platform, name, instance) {
  const output = ts.transpileModule(read(`code-connect/${platform}/${name}.figma.ts`), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText;
  const exports = {};
  vm.runInNewContext(output, {
    exports,
    require(name) {
      assert.equal(name, 'figma');
      return { __esModule: true, default: { selectedInstance: instance, code } };
    },
  }, { timeout: 1000 });
  return exports.default;
}

function instance(props, children = {}) {
  const get = (name, type) => {
    assert.ok(Object.hasOwn(props, name), `Unexpected property: ${name}`);
    if (type) assert.equal(typeof props[name], type, name);
    return props[name];
  };
  return {
    type: 'INSTANCE',
    getString: name => get(name, 'string'),
    getBoolean: name => get(name, 'boolean'),
    getEnum: (name, values) => values[get(name, 'string')],
    getInstanceSwap: name => get(name),
    findInstance: name => { assert.ok(children[name], `Unknown child: ${name}`); return children[name]; },
  };
}

test('saved Logo fixture is bound to the unmodified source snapshot', () => {
  assert.equal(createHash('sha256').update(read(snapshot.logo.source)).digest('hex'), snapshot.logo.sourceSha256);
  const assets = JSON.parse(read(snapshot.logo.source)).assets;
  for (const { nodeId, brand } of snapshot.logo.instances) {
    assert.ok(assets.some(a => a.main?.id === nodeId && a.main.name === `Brand=${brand}`));
  }
});

for (const platform of ['swiftui', 'compose']) {
  test(`${platform}: every saved Settings brand executes through ConnectorRow's nested template`, () => {
    const native = read(platform === 'swiftui' ? 'Sources/RemDesignSystem/Rows/ConnectorRow.swift' : 'compose/RemDesignSystem/rows/ConnectorRow.kt');
    for (const { brand } of snapshot.logo.instances) {
      const leading = instance({ Brand: brand });
      leading.executeTemplate = () => run(platform, 'ConnectorProviderMark', leading);
      const label = instance({ Title: brand, Subtitle: 'Available', 'Show Subtitle': true, 'Show title accessory': false, 'Title layout': 'Fill' });
      label.executeTemplate = () => run(platform, 'ListRowLabel', label);
      const row = instance({ 'Show Divider': true, 'Show Leading': true,
        'Leading Accessory': leading, Content: label, 'Trailing Accessory': instance({ Label: 'Connect' }) });
      const result = run(platform, 'ConnectorRow', instance({ State: 'Available', Accessory: 'Action' }, { ListRow: row }));
      const snippet = render(result.example);
      const mark = render(leading.executeTemplate().example);
      assert.match(mark, /^ConnectorProviderMark\(/);
      assert.ok(snippet.includes(mark), `${brand} lost its nested provider mark`);
      assert.ok(snippet.includes(`ListRowLabel("${brand}"`));
      assert.ok(snippet.includes('Connect'));
      assert.ok(!snippet.includes('[object Object]') && !snippet.includes('undefined'));
      const provider = leading.executeTemplate().metadata.props.provider.replace(/^\./, '');
      assert.match(native, new RegExp(`\\b${provider}\\b`), `${provider} must be a real native enum case`);
    }
    const unknown = render(run(platform, 'ConnectorProviderMark', instance({ Brand: 'Unsupported provider' })).example);
    assert.match(unknown, /Unmapped connector brand/);
    assert.ok(!unknown.includes('ConnectorProviderMark('));
  });

  test(`${platform}: saved Voice glyphs retain Style × Size and reject unverified raw glyphs`, () => {
    for (const symbol of snapshot.voiceGlyphs.symbols) {
      assert.ok(snapshot.voiceGlyphs.sourceExcerpt.includes(symbol.glyph));
      assert.ok(snapshot.voiceGlyphs.sourceExcerpt.includes(symbol.swift));
      for (const Style of ['Subtle', 'Tinted']) for (const Size of ['Small', 'Large']) {
        const result = run(platform, 'ContainedIcon', instance({ Symbol: symbol.glyph, Style, Size }));
        const snippet = render(result.example);
        assert.ok(snippet.includes(symbol[platform === 'swiftui' ? 'swift' : 'compose']));
        assert.equal(result.metadata.props.backgroundToken, Style === 'Subtle' ? 'backgroundSecondary' : 'systemBlue');
        assert.equal(result.metadata.props.foregroundToken, Style === 'Subtle' ? 'labelPrimary' : 'labelOnColor');
        assert.ok(snippet.includes(Size === 'Small' ? '15' : '30'));
        assert.ok(!snippet.includes('undefined'));
      }
    }
    const unknown = render(run(platform, 'ContainedIcon', instance({ Symbol: 'unverified glyph', Style: 'Subtle', Size: 'Small' })).example);
    assert.match(unknown, /Unmapped Figma SF Symbol glyph/);
    assert.ok(!unknown.includes('ContainedIcon('));
  });
}

test('default config selects parserless SwiftUI and manual publishing selects both platforms', () => {
  assert.deepEqual(JSON.parse(read('figma.config.json')), JSON.parse(read('figma.swiftui.config.json')));
  const workflow = read('.github/workflows/figma-publish.yml');
  assert.ok(!workflow.includes('figma connect check'));
  assert.ok(!workflow.includes('Sources/**/*.figma.swift'));
  assert.ok(workflow.includes('default: false'));
  for (const platform of ['swiftui', 'compose']) {
    assert.ok(workflow.includes(`figma connect publish --config figma.${platform}.config.json`));
  }
});
