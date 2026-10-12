// Keep composition rejection fixtures in the existing dependency-free contract test entrypoint.
import './test-playground-mappings.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { parserlessMappingMatches } from './component-code-connect.mjs';

const identity = { source: 'Sources/RemDesignSystem/Rows/ListRowLabel.swift', component: 'ListRowLabel' };
const template = `// url=https://www.figma.com/design/af4yDqCzp57jds9lkFiIaO/Rem?node-id=1966-60442
// source=${identity.source}
// component=${identity.component}
import figma from 'figma'
export default { example: figma.code\`ListRowLabel("Title")\`, id: 'row-label' }
`;

test('accepts a real parserless mapping tied to exact source, component and Figma node', () => {
  assert.equal(parserlessMappingMatches(template, identity), true);
  assert.equal(parserlessMappingMatches(template.replace('/design/', '/file/').replace('1966-60442', '1966:60442'), identity), true);
});
test('another component or platform cannot satisfy the SwiftUI mapping requirement', () => {
  for (const replacement of [
    ['ListRowLabel.swift', 'ListRow.swift'],
    [`source=${identity.source}`, 'source=compose/RemDesignSystem/rows/ListRowLabel.kt'],
    ['component=ListRowLabel', 'component=OtherLabel'],
    [`source=${identity.source}`, `source=../${identity.source}`],
  ]) assert.equal(parserlessMappingMatches(template.replace(...replacement), identity), false);
});
test('rejects missing, duplicate or invalid source/node annotations and comment-only stubs', () => {
  for (const bad of [
    template.replace('?node-id=1966-60442', ''),
    template.replace('1966-60442', 'TODO'),
    template.replace('www.figma.com', 'www.figma.com.example.org'),
    template.replace('https:', 'http:'),
    `${template}// source=${identity.source}\n`,
    template.replace("import figma from 'figma'", '// import figma from \'figma\''),
    template.replace('export default', '// export default'),
    template.replace('figma.code`', 'other`'),
  ]) assert.equal(parserlessMappingMatches(bad, identity), false);
});
