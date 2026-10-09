"""Protect discoverability of Chat rules, not their visual/runtime implementation."""
from pathlib import Path
import re
import tempfile
import unittest
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[2]
GUIDANCE = (
    '.claude/skills/design-system-delivery/SKILL.md',
    '.claude/skills/rem-design-system/SKILL.md',
    '.claude/skills/rem-design-system/references/component-track.md',
    '.claude/skills/rem-design-system/references/screen-track.md',
    '.claude/skills/rem-design-system/references/chat-review.md',
    'REGISTRY.md',
    'FILE-ORG.md',
    'docs/design-reconciliation/2026-10-09-chat-guidance-review.md',
)


def anchors(markdown):
    # GitHub heading fragments used by these guidance files (including duplicate headings).
    seen = {}
    result = set()
    for heading in re.findall(r'^#{1,6}\s+(.+?)\s*#*$', markdown, re.M):
        slug = re.sub(r'[^\w\- ]', '', heading.lower()).replace(' ', '-')
        count = seen.get(slug, 0)
        seen[slug] = count + 1
        result.add(f'{slug}-{count}' if count else slug)
    return result


def local_link_errors(source):
    errors = []
    for href in re.findall(r'(?<!!)\[[^\]]+\]\(([^)]+)\)', source.read_text()):
        parts = urlsplit(href)
        if parts.scheme or parts.netloc:
            continue
        target = source.parent / unquote(parts.path) if parts.path else source
        if not target.is_file():
            errors.append(f'{source}: missing file {href}')
        elif parts.fragment and target.suffix == '.md':
            if unquote(parts.fragment) not in anchors(target.read_text()):
                errors.append(f'{source}: missing heading {href}')
    return errors


class ChatGuidanceLinksTests(unittest.TestCase):
    def test_skill_and_review_routes_resolve(self):
        for relative in GUIDANCE:
            with self.subTest(path=relative):
                self.assertEqual(local_link_errors(ROOT / relative), [])

    def test_missing_file_or_heading_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / 'guide.md'
            (source.parent / 'target.md').write_text('# Stable heading\n')
            source.write_text('[valid](target.md#stable-heading)\n'
                              '[missing](absent.md)\n'
                              '[stale](target.md#old-heading)\n'
                              '[remote](https://example.com/reference)\n')
            errors = local_link_errors(source)
            self.assertEqual(len(errors), 2)
            self.assertIn('missing file', errors[0])
            self.assertIn('missing heading', errors[1])


if __name__ == '__main__':
    unittest.main()
