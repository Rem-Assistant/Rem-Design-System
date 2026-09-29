from pathlib import Path
import unittest


class PublisherWorkflowContractTests(unittest.TestCase):
    def test_figma_artifact_downloads_into_validator_root(self):
        workflow = (
            Path(__file__).resolve().parents[2]
            / ".github/workflows/publish-builder-delivery.yml"
        ).read_text(encoding="utf-8")
        start = workflow.index("      - name: Download exact-head Figma reference evidence")
        following_step = workflow.index("\n      - name:", start + 1)
        step = workflow[start:following_step]

        self.assertIn("uses: actions/download-artifact@v4", step)
        self.assertIn("artifact-ids: ${{ steps.figma-run.outputs.artifact_id }}", step)
        self.assertIn("path: ${{ runner.temp }}/figma-evidence", step)
        self.assertIn("merge-multiple: true", step)


if __name__ == "__main__":
    unittest.main()
