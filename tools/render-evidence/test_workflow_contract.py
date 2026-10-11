from pathlib import Path
import unittest


class PublisherWorkflowContractTests(unittest.TestCase):
    @staticmethod
    def workflow():
        return (
            Path(__file__).resolve().parents[2]
            / ".github/workflows/publish-builder-delivery.yml"
        ).read_text(encoding="utf-8")

    def test_figma_artifact_downloads_into_validator_root(self):
        workflow = self.workflow()
        start = workflow.index("      - name: Download exact-head Figma reference evidence")
        following_step = workflow.index("\n      - name:", start + 1)
        step = workflow[start:following_step]

        self.assertIn("uses: actions/download-artifact@v4", step)
        self.assertIn("artifact-ids: ${{ steps.figma-run.outputs.artifact_id }}", step)
        self.assertIn("path: ${{ runner.temp }}/figma-evidence", step)
        self.assertIn("merge-multiple: true", step)

    def test_authenticated_structure_report_is_staged_only_after_validation(self):
        workflow = self.workflow()
        start = workflow.index("      - name: Validate and stage authenticated Figma references")
        following_step = workflow.index("\n      - name:", start + 1)
        step = workflow[start:following_step]
        validation = step.index("tools/render-evidence/artifact.py")
        staged_report = step.index(
            'cp "$FIGMA_ROOT/structure/report.json" "$EVIDENCE_ROOT/structure/report.json"'
        )
        self.assertLess(validation, staged_report)


class ScreenshotsBuild9RoutingContractTests(unittest.TestCase):
    """Pins the Build 9 same-repo routing clause on the native Settings playground jobs in screenshots.yml."""

    BUILD9_CLAUSE = (
        "(github.event.pull_request.base.ref == 'main'"
        " && github.event.pull_request.head.ref == 'claude/build9-shell-integration'"
        " && github.event.pull_request.head.repo.full_name == github.repository)"
    )

    @staticmethod
    def workflow():
        return (
            Path(__file__).resolve().parents[2]
            / ".github/workflows/screenshots.yml"
        ).read_text(encoding="utf-8")

    def job_condition(self, job):
        workflow = self.workflow()
        start = workflow.index(f"\n  {job}:\n")
        following_job = workflow.find("\n\n  ", start + 1)
        block = workflow[start:following_job if following_job != -1 else len(workflow)]
        lines = [line for line in block.splitlines() if line.startswith("    if: ")]
        self.assertEqual(len(lines), 1, f"{job} has exactly one job-level if")
        return lines[0][len("    if: "):]

    def test_settings_jobs_route_build9_only_from_the_same_repository(self):
        for job, excluded in (("settings-ios", "android"), ("settings-android", "ios")):
            with self.subTest(job=job):
                condition = self.job_condition(job)
                self.assertEqual(condition.count(self.BUILD9_CLAUSE), 1)
                # The clause sits inside the routing disjunction, which the platform guard then narrows.
                routing, _, guard = condition.rpartition(" && ")
                self.assertIn(self.BUILD9_CLAUSE, routing)
                self.assertEqual(guard, f"inputs.platform != '{excluded}'")
                self.assertTrue(routing.startswith("(") and routing.endswith(")"))

    def test_build9_route_runs_untrusted_code_without_credentials(self):
        workflow = self.workflow()
        triggers = workflow[workflow.index("\non:\n"):workflow.index("\njobs:\n")]
        self.assertIn("  pull_request:\n", triggers)
        self.assertNotIn("pull_request_target", workflow)
        self.assertNotIn("secrets.", workflow)
        self.assertIn("\npermissions:\n  contents: read\n", workflow)
        for job in ("settings-ios", "settings-android"):
            with self.subTest(job=job):
                start = self.workflow().index(f"\n  {job}:\n")
                checkout = workflow.index("uses: actions/checkout@", start)
                self.assertIn("persist-credentials: false", workflow[checkout:workflow.index("\n      - ", checkout)])


class ScreenshotsIOSDiagnosticsContractTests(unittest.TestCase):
    """A journeys step that reaches its limit still leaves its raw iOS evidence (build log and result bundle):
    those upload before the optional screenshot extraction, and every post-test step has its own bound."""

    @staticmethod
    def steps():
        workflow = (Path(__file__).resolve().parents[2] / ".github/workflows/screenshots.yml").read_text(encoding="utf-8")
        start = workflow.index("\n  settings-ios:\n")
        block = workflow[start:workflow.index("\n\n  settings-android:\n")]
        return block, block.split("\n      - ")[1:]

    def index_of(self, steps, marker):
        found = [i for i, step in enumerate(steps) if marker in step]
        self.assertEqual(len(found), 1, marker)
        return found[0]

    def test_raw_diagnostics_upload_before_screenshot_extraction(self):
        _, steps = self.steps()
        journeys = self.index_of(steps, "name: Run the actual playground journeys")
        build_log = self.index_of(steps, "name: settings-ios-build-log")
        package = self.index_of(steps, "name: Package iOS journey diagnostics")
        bundle = self.index_of(steps, "name: settings-ios-journeys\n")
        extract = self.index_of(steps, "name: Extract actual playground screenshots")
        screenshots = self.index_of(steps, "name: settings-swiftui-screenshots")
        self.assertLess(journeys, build_log)
        self.assertLess(build_log, package)
        self.assertLess(package, bundle)
        self.assertLess(bundle, extract)
        self.assertLess(extract, screenshots)
        for index in (build_log, package, bundle, extract, screenshots):
            self.assertIn("if: always()", steps[index])

    def test_limits_and_failure_semantics_are_unchanged(self):
        block, steps = self.steps()
        self.assertIn("\n    timeout-minutes: 45\n", block)
        self.assertIn("timeout-minutes: 40", steps[self.index_of(steps, "name: Run the actual playground journeys")])
        self.assertIn("timeout-minutes: 3", steps[self.index_of(steps, "name: Package iOS journey diagnostics")])
        self.assertIn("timeout-minutes: 5", steps[self.index_of(steps, "name: Extract actual playground screenshots")])
        self.assertNotIn("continue-on-error", block)
        for name in ("settings-ios-build-log", "settings-ios-journeys\n", "settings-swiftui-screenshots"):
            self.assertIn("if-no-files-found: error", steps[self.index_of(steps, f"name: {name}")])


if __name__ == "__main__":
    unittest.main()
