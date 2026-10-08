import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    "validate_figma_run", Path(__file__).with_name("validate_figma_run.py")
)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class FigmaWorkflowRunValidationTests(unittest.TestCase):
    # Captured shape and identifiers from the first live scoped run, 36499087426.
    head_sha = "917c775f608a6b531937560ab45dac1eadd80b83"
    base_sha = "10a2c7108f6d6aeaa8565c2be3abdd2564ee1f5b"
    repository = "Rem-Assistant/Rem-Design-System"

    def fixtures(self):
        run = {
            "id": 36499087426,
            "path": ".github/workflows/design-drift.yml",
            "event": "pull_request_target",
            "status": "completed",
            "conclusion": "success",
            "head_sha": self.head_sha,
            "head_branch": "agent-factory/issue-30",
            "head_repository": {"id": 1382765586, "full_name": self.repository},
            "pull_requests": [{
                "number": 31,
                "head": {
                    "sha": self.head_sha,
                    "ref": "agent-factory/issue-30",
                    "repo": {"id": 1382765586},
                },
                "base": {
                    "sha": self.base_sha,
                    "ref": "main",
                    "repo": {"id": 1382765586},
                },
            }],
        }
        artifact = {
            "name": f"figma-reference-evidence-31-{self.head_sha}",
            "workflow_run": {
                "id": run["id"],
                "head_sha": self.head_sha,
                "head_branch": run["head_branch"],
                "head_repository_id": 1382765586,
                "repository_id": 1382765586,
            },
        }
        return run, artifact

    def validate(self, run, artifact):
        return validator.validate(
            run, artifact, repository=self.repository, pr=31,
            head_sha=self.head_sha, base_sha=self.base_sha, base_ref="main",
        )

    def test_manual_settings_run_requires_opt_in_and_exact_trusted_base(self):
        run, item = self.fixtures()
        run.update(event="workflow_dispatch", head_sha=self.base_sha,
                   head_branch="codex/settings-integration", pull_requests=[])
        item["workflow_run"].update(head_sha=self.base_sha, head_branch=run["head_branch"])
        kwargs = dict(repository=self.repository, pr=31, head_sha=self.head_sha,
                      base_sha=self.base_sha, base_ref="codex/settings-integration")
        with self.assertRaises(ValueError):
            validator.validate(run, item, **kwargs)
        self.assertEqual(validator.validate(run, item, allow_settings_manual=True, **kwargs)["run_id"], run["id"])
        for field, value in (("head_sha", self.head_sha), ("head_branch", "main"), ("conclusion", "failure")):
            broken = dict(run); broken[field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                validator.validate(broken, item, allow_settings_manual=True, **kwargs)
        kwargs["base_ref"] = "main"
        with self.assertRaises(ValueError):
            validator.validate(run, item, allow_settings_manual=True, **kwargs)

    def test_accepts_real_pull_request_target_metadata_shape(self):
        run, artifact = self.fixtures()
        self.assertEqual(self.validate(run, artifact), {
            "run_id": 36499087426,
            "repository_id": 1382765586,
            "head_branch": "agent-factory/issue-30",
        })

    def test_rejects_base_sha_instead_of_exact_feature_sha(self):
        run, artifact = self.fixtures()
        run["head_sha"] = self.base_sha
        with self.assertRaisesRegex(ValueError, "exact feature head"):
            self.validate(run, artifact)

        run, artifact = self.fixtures()
        artifact["workflow_run"]["head_sha"] = self.base_sha
        with self.assertRaisesRegex(ValueError, "feature head"):
            self.validate(run, artifact)

    def test_rejects_missing_or_mismatched_pr_binding(self):
        mutations = (
            ("missing PR", lambda run: run.update(pull_requests=[])),
            ("head SHA", lambda run: run["pull_requests"][0]["head"].update(sha="c" * 40)),
            ("base SHA", lambda run: run["pull_requests"][0]["base"].update(sha="c" * 40)),
            ("base ref", lambda run: run["pull_requests"][0]["base"].update(ref="release")),
            ("head repository", lambda run: run["pull_requests"][0]["head"]["repo"].update(id=9)),
            ("base repository", lambda run: run["pull_requests"][0]["base"]["repo"].update(id=9)),
        )
        for label, mutate in mutations:
            with self.subTest(label=label):
                run, artifact = self.fixtures()
                mutate(run)
                with self.assertRaises(ValueError):
                    self.validate(run, artifact)

    def test_rejects_wrong_run_or_artifact_identity(self):
        run_mutations = (
            lambda run: run.update(event="pull_request"),
            lambda run: run.update(conclusion="failure"),
            lambda run: run["head_repository"].update(full_name="other/repo"),
        )
        for mutate in run_mutations:
            run, artifact = self.fixtures()
            mutate(run)
            with self.assertRaises(ValueError):
                self.validate(run, artifact)

        artifact_mutations = (
            lambda item: item.update(name="wrong"),
            lambda item: item["workflow_run"].update(id=1),
            lambda item: item["workflow_run"].update(head_branch="other"),
            lambda item: item["workflow_run"].update(repository_id=9),
        )
        for mutate in artifact_mutations:
            run, artifact = self.fixtures()
            mutate(artifact)
            with self.assertRaises(ValueError):
                self.validate(run, artifact)
