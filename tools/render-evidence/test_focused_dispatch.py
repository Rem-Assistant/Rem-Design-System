"""Pins the focused native-journey dispatch: one scratch branch, validated selections, no release surface."""
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = (ROOT / '.github/workflows/screenshots.yml').read_text()
FOCUSED = "(github.event_name == 'workflow_dispatch' && github.ref_name == 'claude/build9-focused-journeys')"


def step(name):
    match = re.search(r"      - name: " + re.escape(name) + r"\n(.*?)(?=\n      - )", WORKFLOW, re.S)
    assert match, name
    return match.group(1)


def run_block(name):
    return re.search(r"run: \|\n(.*)", step(name), re.S).group(1)


class FocusedDispatchTests(unittest.TestCase):
    def test_dispatch_is_limited_to_the_scratch_branch(self):
        self.assertEqual(WORKFLOW.count(FOCUSED), 2)  # settings-ios and settings-android only
        self.assertEqual(WORKFLOW.count('claude/build9-focused-journeys'), 2)
        self.assertNotIn("github.ref_name == 'claude/build9-shell-integration'", WORKFLOW)
        self.assertNotIn('pull_request_target', WORKFLOW)
        self.assertNotIn('secrets.', WORKFLOW)
        self.assertIn('permissions:\n  contents: read\n', WORKFLOW)
        self.assertIn("group: screenshots-${{ github.ref }}-${{ inputs.platform || 'both' }}", WORKFLOW)

    def test_focused_inputs_reach_scripts_only_through_env(self):
        self.assertEqual(WORKFLOW.count('ANDROID_TEST: ${{ inputs.android_test }}'), 2)
        self.assertEqual(WORKFLOW.count('IOS_TEST: ${{ inputs.ios_test }}'), 1)
        for line in WORKFLOW.splitlines():
            if '${{ inputs.android_test }}' in line or '${{ inputs.ios_test }}' in line:
                self.assertRegex(line.strip(), r'^[A-Z_]+: \$\{\{ inputs\.(android|ios)_test \}\}$')

    def test_render_jobs_stay_out_of_focused_runs(self):
        self.assertIn("if: inputs.platform != 'android' && inputs.ios_test == '' && inputs.android_test == ''", WORKFLOW)
        self.assertIn("if: inputs.platform != 'ios' && inputs.ios_test == '' && inputs.android_test == ''", WORKFLOW)

    def validate(self, selection):
        with tempfile.NamedTemporaryFile('r') as env_file:
            proc = subprocess.run(['bash', '-c', run_block('Validate focused Android selection')],
                                  env={**os.environ, 'ANDROID_TEST': selection, 'GITHUB_ENV': env_file.name},
                                  capture_output=True, text=True)
            return proc.returncode, Path(env_file.name).read_text()

    def test_android_selection_validation(self):
        self.assertEqual(self.validate(''), (0, ''))
        code, env = self.validate('PlaygroundNavigationTest#shellDeveloperRoundTripKeepsConversationDraftAndScroll')
        self.assertEqual(code, 0)
        self.assertEqual(env, 'ANDROID_TEST_CLASSES=com.rem.designsystem.demo.PlaygroundNavigationTest'
                              '#shellDeveloperRoundTripKeepsConversationDraftAndScroll\n')
        code, env = self.validate('PlaygroundNavigationTest#a,SettingsPlaygroundTest#b')
        self.assertEqual((code, env), (0, 'ANDROID_TEST_CLASSES=com.rem.designsystem.demo.PlaygroundNavigationTest#a,'
                                          'com.rem.designsystem.demo.SettingsPlaygroundTest#b\n'))
        for bad in ['PlaygroundNavigationTest', 'com.rem.designsystem.demo.PlaygroundNavigationTest#a',
                    'PlaygroundNavigationTest#a;id', 'PlaygroundNavigationTest#$(id)', 'PlaygroundNavigationTest#a,',
                    'PlaygroundNavigationTest#a -Pfoo=1', 'Foo#a', 'PlaygroundNavigationTest#a\nB#c']:
            with self.subTest(bad=bad):
                self.assertEqual(self.validate(bad), (2, ''))

    def test_selected_android_methods_must_actually_run(self):
        script = run_block('Confirm focused Android tests ran')
        with tempfile.TemporaryDirectory() as work:
            results = Path(work, 'compose/demo/build/outputs/androidTest-results/connected')
            results.mkdir(parents=True)
            (results / 'TEST.xml').write_text('<testcase name="ran" classname="com.rem.designsystem.demo.PlaygroundNavigationTest" time="1">')
            def run(selection):
                return subprocess.run(['bash', '-c', script], cwd=work, env={**os.environ, 'ANDROID_TEST': selection},
                                      capture_output=True, text=True).returncode
            self.assertEqual(run('PlaygroundNavigationTest#ran'), 0)
            self.assertEqual(run('PlaygroundNavigationTest#ran,PlaygroundNavigationTest#missing'), 1)
            self.assertEqual(run('SettingsPlaygroundTest#ran'), 1)


if __name__ == '__main__':
    unittest.main()
