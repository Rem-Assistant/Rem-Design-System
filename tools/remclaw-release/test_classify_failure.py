import io
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

import classify_failure as cf

FIXTURES = Path(__file__).resolve().parent / 'fixtures'
# The only shape public output may take: fixed tokens and integers.
PUBLIC = re.compile(r'classification=(%s)( [a-z]+=\d+)+' % '|'.join(cf.CODES))


VOCABULARY = {c.lower() for c, _ in cf.CATEGORIES} | {'classification', 'lines'} | set(cf.CODES)


def assert_public(case, out):
    """Every token of the output is fixed vocabulary or an integer; nothing else can carry log text."""
    case.assertRegex(out.strip(), r'\A' + PUBLIC.pattern + r'\Z')
    tokens = out.strip().replace('=', ' ').split(' ')
    case.assertTrue(all(t in VOCABULARY or t.isdigit() for t in tokens), out)


class ClassifierTests(unittest.TestCase):
    def run_cli(self, *args):
        p = subprocess.run([sys.executable, str(Path(cf.__file__)), *map(str, args)], capture_output=True, text=True)
        self.assertEqual(p.returncode, 0)
        self.assertEqual(p.stderr, '')
        return p.stdout

    def test_each_fixture_maps_to_its_fixed_code(self):
        expected = {'compile': 'COMPILE', 'provisioning': 'PROVISIONING', 'signing': 'SIGNING',
                    'dependency': 'DEPENDENCY', 'upload': 'UPLOAD', 'auth': 'AUTH', 'unknown': 'UNKNOWN'}
        for name, code in expected.items():
            with self.subTest(name=name):
                classification, counts, total = cf.classify_files([FIXTURES / f'{name}.log'])
                self.assertEqual(classification, code)
                self.assertGreater(total, 0)
                if code != 'UNKNOWN':
                    self.assertGreater(counts[code], 0)

    def test_counts_are_disjoint_and_compile_errors_counted(self):
        classification, counts, total = cf.classify_files([FIXTURES / 'compile.log'])
        self.assertEqual(counts['COMPILE'], 4)  # two diagnostics, SwiftCompile failure, ARCHIVE FAILED
        self.assertLessEqual(sum(counts.values()), total)

    def test_priority_prefers_root_cause_over_downstream_symptoms(self):
        lines = ['** ARCHIVE FAILED **', 'xcodebuild: error: Could not resolve package dependencies:']
        self.assertEqual(cf.classify_lines(lines)[0], 'DEPENDENCY')
        self.assertEqual(cf.classify_lines(['** EXPORT FAILED **', 'requires a provisioning profile'])[0], 'PROVISIONING')

    def test_no_fixture_text_leaks_through_cli_output(self):
        for path in sorted(FIXTURES.glob('*.log')):
            with self.subTest(path=path.name):
                out = self.run_cli(path)
                assert_public(self, out)
                self.assertNotIn('MARKER', out)
                self.assertNotIn(path.name, out)
                self.assertNotIn(str(FIXTURES), out)

    def test_all_fixtures_together_and_record_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            record = Path(tmp) / 'failure_class'
            out = self.run_cli('--record', record, *sorted(FIXTURES.glob('*.log')))
            assert_public(self, out)
            self.assertEqual(record.read_text(), 'DEPENDENCY\n')
            self.assertNotIn('MARKER', out)

    def test_missing_unreadable_or_binary_input_is_unknown_without_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            secret_name = Path(tmp) / 'PRIVATE_PATH_MARKER.log'
            out = self.run_cli(secret_name)
            self.assertTrue(out.startswith('classification=UNKNOWN '))
            self.assertNotIn('PRIVATE_PATH_MARKER', out)
            binary = Path(tmp) / 'b.log'
            binary.write_bytes(b'\xff\xfe\x00SOURCE_LEAK_MARKER\x00\x80** ARCHIVE FAILED **\n')
            out = self.run_cli(binary)
            self.assertTrue(out.startswith('classification=COMPILE '))
            self.assertNotIn('SOURCE_LEAK_MARKER', out)
        self.assertTrue(self.run_cli().startswith('classification=UNKNOWN'))

    def test_internal_error_prints_only_fixed_code(self):
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            line = cf.render('UNKNOWN', {c: 0 for c, _ in cf.CATEGORIES}, 0)
        self.assertRegex(line, PUBLIC)
        with self.assertRaises(AssertionError):
            cf.render('privateBackendToken_SOURCE_LEAK_MARKER', {}, 0)

    def test_render_rejects_non_integer_counts(self):
        counts = {c: 0 for c, _ in cf.CATEGORIES}
        counts['COMPILE'] = 'SOURCE_LEAK_MARKER'
        with self.assertRaises(ValueError):
            cf.render('COMPILE', counts, 1)


if __name__ == '__main__':
    unittest.main()
