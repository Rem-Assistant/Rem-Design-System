import importlib.util
import plistlib
import sys
import zipfile
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("delivery", Path(__file__).with_name("delivery.py"))
d = importlib.util.module_from_spec(spec)
spec.loader.exec_module(d)


class NativeEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.sha = "a" * 40
        self.record = {"source_sha": self.sha, "platform": "ios", "features": d.FEATURES,
                       "status": "passed", "unfiltered": True, "passed_methods": ["Suite/testOne"], "passed_count": 1}
        self.record["execution"] = {"provider": "github_actions", "workflow_sha": self.sha, "run_id": "123", "run_attempt": "1"}
        self.expected = patch.object(d, "expected_tests", return_value={"Suite/testOne"})
        self.expected.start()
        self.addCleanup(self.expected.stop)

    def test_exact_native_evidence_passes(self):
        d.validate_tests(self.record, self.sha, "ios")

    def test_stale_source_rejected(self):
        with self.assertRaisesRegex(ValueError, "Stale"):
            d.validate_tests(self.record, "b" * 40, "ios")

    def test_skipped_job_is_not_native_success(self):
        self.record["status"] = "skipped"
        with self.assertRaises(ValueError):
            d.validate_tests(self.record, self.sha, "ios")

    def test_filtered_success_rejected(self):
        self.record["unfiltered"] = False
        with self.assertRaises(ValueError):
            d.validate_tests(self.record, self.sha, "ios")

    def test_missing_extra_duplicate_and_wrong_count_rejected(self):
        for change in ({"passed_methods": []}, {"passed_methods": ["Suite/testOther"]},
                       {"passed_methods": ["Suite/testOne", "Suite/testOne"]}, {"passed_count": 2}):
            with self.subTest(change=change), self.assertRaises(ValueError):
                d.validate_tests({**self.record, **change}, self.sha, "ios")

    def test_missing_or_stale_execution_rejected(self):
        for execution in ({}, {"provider": "github_actions", "workflow_sha": "b" * 40, "run_id": "123", "run_attempt": "1"}):
            with self.subTest(execution=execution), self.assertRaises(ValueError):
                d.validate_tests({**self.record, "execution": execution}, self.sha, "ios")

    def test_wrong_scope_rejected(self):
        self.record["features"] = ["settings", "agenda"]
        with self.assertRaises(ValueError):
            d.validate_tests(self.record, self.sha, "ios")

    def test_ios_passes_only_complete_method_output(self):
        log = "Test Case '-[RemSettingsPlaygroundUITests.Suite testOne]' passed (0.1 seconds).\n** TEST SUCCEEDED **"
        self.assertEqual(d.parse_ios(log), {"Suite/testOne"})
        for bad in (log.replace("passed", "skipped"), log.replace("passed", "failed"), "** TEST SUCCEEDED **", log + log, log.replace("** TEST SUCCEEDED **", "")):
            with self.subTest(log=bad), self.assertRaises(ValueError):
                d.parse_ios(bad)

    def test_android_skipped_failed_and_empty_reports_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            report = Path(tmp) / "TEST-Suite.xml"
            for child in ("<skipped/>", "<failure/>", "<error/>"):
                report.write_text(f'<testsuite><testcase classname="pkg.Suite" name="testOne">{child}</testcase></testsuite>')
                with self.subTest(child=child), self.assertRaises(ValueError):
                    d.parse_android([report])
            with self.assertRaises(ValueError):
                d.parse_android([])

    def test_android_method_identity_and_duplicates(self):
        with tempfile.TemporaryDirectory() as tmp:
            report = Path(tmp) / "TEST-Suite.xml"
            report.write_text('<testsuite><testcase classname="pkg.Suite" name="testOne"/></testsuite>')
            self.assertEqual(d.parse_android([report]), {"Suite/testOne"})
            with self.assertRaises(ValueError):
                d.parse_android([report, report])

    def test_manifest_rejects_changed_binary_and_wrong_source(self):
        with tempfile.TemporaryDirectory() as tmp:
            binary = Path(tmp) / "app.apk"
            binary.write_bytes(b"verified-build")
            record = {"source_sha": self.sha, "build_sha": self.sha, "platform": "ios", "application_id": d.IDS["ios"], "features": d.FEATURES,
                      "native_tests": self.record, "binary_sha256": d.digest(binary)}
            d.validate_manifest(record, self.sha, "ios", binary)
            with self.assertRaises(ValueError):
                d.validate_manifest(record, "b" * 40, "ios", binary)
            binary.write_bytes(b"stale-build")
            with self.assertRaisesRegex(ValueError, "hash mismatch"):
                d.validate_manifest(record, self.sha, "ios", binary)

    def test_embedded_app_stamp_and_production_identity_are_checked(self):
        with tempfile.TemporaryDirectory() as tmp:
            app = Path(tmp)
            info = {"CFBundleIdentifier": d.IDS["ios"], "CFBundleShortVersionString": "0.1.0", "CFBundleVersion": "1", "RemPlaygroundSourceSHA": self.sha}
            for change in ({}, {"RemPlaygroundSourceSHA": "b" * 40}, {"CFBundleIdentifier": "com.remapp.rem"}):
                (app / "Info.plist").write_bytes(plistlib.dumps({**info, **change}))
                if not change:
                    self.assertEqual(d.inspect_artifact("ios", app, self.sha)["source_sha"], self.sha)
                else:
                    with self.assertRaises(ValueError):
                        d.inspect_artifact("ios", app, self.sha)

    def test_android_stamp_uses_string_prefix_for_numeric_hashes(self):
        xml = '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.rem.designsystem.demo" android:versionName="1.0" android:versionCode="1"><application><meta-data android:name="RemPlaygroundSourceSHA" android:value="git:' + '0' * 40 + '" /></application></manifest>'
        with patch.dict(d.os.environ, {"ANDROID_HOME": "/fake/sdk"}), patch.object(d.subprocess, "check_output", return_value=xml):
            self.assertEqual(d.inspect_artifact("android", "app.apk", "0" * 40)["source_sha"], "0" * 40)

    def test_installer_rejects_archive_traversal_and_other_apps(self):
        sys.modules["delivery"] = d
        spec = importlib.util.spec_from_file_location("installer", Path(__file__).with_name("install-ios.py"))
        installer = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(installer)
        with tempfile.TemporaryDirectory() as tmp:
            for name in ("../outside", "/absolute", "ProductionRem.app/Info.plist"):
                binary = Path(tmp) / "app.zip"
                with zipfile.ZipFile(binary, "w") as archive:
                    archive.writestr(name, "bad")
                with self.subTest(name=name), self.assertRaises(ValueError):
                    installer.safe_archive(binary)

    def test_old_record_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "record.json"
            d.write_json(path, self.record)
            with self.assertRaises(FileExistsError):
                d.write_json(path, {})

    def test_binary_mutation_changes_digest(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "app.apk"
            path.write_bytes(b"first-build")
            original = d.digest(path)
            path.write_bytes(b"second-build")
            self.assertNotEqual(original, d.digest(path))


if __name__ == "__main__":
    unittest.main()
