import subprocess
import tempfile
import unittest
from pathlib import Path

import check_pr_size as size

ROOT = Path(__file__).resolve().parents[2]


def repo(tmp):
    def git(*args):
        return subprocess.run(["git", "-C", tmp, *args], check=True, capture_output=True, text=True).stdout.strip()
    git("init", "-q")
    git("config", "user.email", "t@example.com")
    git("config", "user.name", "t")
    Path(tmp, "base.txt").write_text("base\n")
    git("add", "-A")
    git("commit", "-qm", "base")
    return git


class LimitTests(unittest.TestCase):
    def test_limits(self):
        self.assertEqual(size.problems(["f"] * 150, 750_000), [])
        self.assertEqual(len(size.problems(["f"] * 151, 1)), 1)
        self.assertEqual(len(size.problems(["f"], 750_001)), 1)
        self.assertEqual(len(size.problems(["f"] * 151, 750_001)), 2)

    def test_measures_against_the_merge_base_like_github(self):
        with tempfile.TemporaryDirectory() as tmp:
            git = repo(tmp)
            base = git("rev-parse", "HEAD")
            git("checkout", "-qb", "feature")
            for i in range(3):
                Path(tmp, f"f{i}.txt").write_text("x" * 100 + "\n")
            git("add", "-A")
            git("commit", "-qm", "feature")
            head = git("rev-parse", "HEAD")
            # A later commit on the base branch is not part of the PR.
            git("checkout", "-q", base)
            Path(tmp, "other.txt").write_text("other\n")
            git("add", "-A")
            git("commit", "-qm", "base moves")
            moved = git("rev-parse", "HEAD")
            cwd = Path.cwd()
            try:
                import os
                os.chdir(tmp)
                files, diff_bytes = size.measure(moved, head)
            finally:
                os.chdir(cwd)
            self.assertEqual(sorted(files), ["f0.txt", "f1.txt", "f2.txt"])
            self.assertGreater(diff_bytes, 300)

    def test_workflow_runs_the_check_on_every_pull_request(self):
        workflow = (ROOT / ".github/workflows/pr-size.yml").read_text()
        self.assertIn("pull_request:", workflow)
        self.assertNotIn("paths:", workflow)
        self.assertNotIn("pull_request_target", workflow)
        self.assertIn("permissions:\n  contents: read\n", workflow)
        self.assertIn("persist-credentials: false", workflow)
        self.assertIn('python3 tools/pr-size/check_pr_size.py "$BASE_SHA" "$HEAD_SHA"', workflow)


if __name__ == "__main__":
    unittest.main()
