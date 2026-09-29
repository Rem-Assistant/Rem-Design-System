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


if __name__ == "__main__":
    unittest.main()
