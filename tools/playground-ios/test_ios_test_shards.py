import json
import tempfile
import unittest
from pathlib import Path

import ios_test_shards as shards

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = (ROOT / ".github/workflows/screenshots.yml").read_text(encoding="utf-8")


def log(*events, executed=None):
    lines = [f"Test Case '-[RemSettingsPlaygroundUITests.{t.split('/')[0]} {t.split('/')[1]}]' {state}." for t, state in events]
    if executed is not None:
        lines += ["Test Suite 'Selected tests' failed at 2026-10-11 21:52:26.912.",
                  f"\t Executed {executed} tests, with 0 failures (0 unexpected) in 1.0 (1.0) seconds"]
    return "\n".join(lines) + "\n"


class InventoryTests(unittest.TestCase):
    def test_inventory_matches_every_xctest_method_in_the_sources(self):
        tests = shards.inventory()
        self.assertEqual(len(tests), len(set(tests)))
        for path in sorted((ROOT / "tools/playground-ios/UITests").glob("*.swift")):
            source = path.read_text(encoding="utf-8")
            declared = source.count("\n    func test")
            self.assertEqual(declared, sum(1 for t in tests if t.startswith(path.stem + "/")), path.name)

    def test_inventory_ignores_helpers_and_nested_or_private_members(self):
        with tempfile.TemporaryDirectory() as root:
            Path(root, "AUITests.swift").write_text(
                "final class AUITests: XCTestCase {\n"
                "    func testOne() {}\n"
                "    private func testHelper() {}\n"
                "    func testTwo() throws {}\n"
                "    func testWithArg(_ x: Int) {}\n"
                "        func testNested() {}\n"
                "}\n", encoding="utf-8")
            Path(root, "Helpers.swift").write_text("func testFree() {}\n", encoding="utf-8")
            self.assertEqual(shards.inventory(Path(root)), ["AUITests/testOne", "AUITests/testTwo"])


class PartitionTests(unittest.TestCase):
    def test_partitions_are_disjoint_exhaustive_and_balanced(self):
        tests = shards.inventory()
        for count in range(1, 6):
            parts = [shards.shard(tests, count, i) for i in range(count)]
            union = [t for part in parts for t in part]
            self.assertEqual(sorted(union), tests, count)
            self.assertEqual(len(union), len(set(union)), count)
            self.assertLessEqual(max(map(len, parts)) - min(map(len, parts)), 1, count)

    def test_invalid_shard_is_refused(self):
        for count, index in ((0, 0), (3, 3), (3, -1)):
            with self.assertRaises(SystemExit):
                shards.shard(["A/testA"], count, index)


class DeliveryPlannerParityTests(unittest.TestCase):
    """tools/playground-delivery/delivery.py shards the same suite for native candidates; the two planners
    must name the same tests and the same partitions, or one of them silently drops tests."""

    def test_same_inventory_and_partitions_as_the_delivery_planner(self):
        import importlib.util
        spec = importlib.util.spec_from_file_location("delivery", ROOT / "tools/playground-delivery/delivery.py")
        delivery = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(delivery)
        tests = shards.inventory()
        self.assertEqual(set(tests), delivery.expected_tests("ios"))
        for count in range(1, 6):
            for index in range(count):
                self.assertEqual(set(shards.shard(tests, count, index)), delivery.shard_tests("ios", index, count))


class EnumerationTests(unittest.TestCase):
    TESTS = ["AUITests/testOne", "BUITests/testTwo"]

    def test_matching_enumeration_passes_in_grouped_and_flat_shapes(self):
        grouped = {"values": [{"enabledTests": [{"identifier": "RemSettingsPlaygroundUITests/AUITests/testOne"},
                                                {"identifier": "RemSettingsPlaygroundUITests/BUITests/testTwo()"}],
                               "disabledTests": []}]}
        flat = ["RemSettingsPlaygroundUITests/AUITests/testOne", "RemSettingsPlaygroundUITests/BUITests/testTwo"]
        self.assertEqual(shards.check_enumeration(grouped, self.TESTS), [])
        self.assertEqual(shards.check_enumeration(flat, self.TESTS), [])

    def test_missing_extra_disabled_or_empty_enumeration_fails(self):
        extra = ["RemSettingsPlaygroundUITests/AUITests/testOne", "RemSettingsPlaygroundUITests/BUITests/testTwo",
                 "RemSettingsPlaygroundUITests/CUITests/testThree"]
        self.assertTrue(shards.check_enumeration(extra, self.TESTS))
        self.assertTrue(shards.check_enumeration(extra[:1], self.TESTS))
        disabled = {"enabledTests": extra[:2], "disabledTests": ["RemSettingsPlaygroundUITests/AUITests/testOne"]}
        self.assertTrue(shards.check_enumeration(disabled, self.TESTS))
        self.assertTrue(shards.check_enumeration({}, self.TESTS))


class ResultTests(unittest.TestCase):
    SELECTED = ["AUITests/testOne", "AUITests/testTwo"]

    def test_every_selected_test_ran_to_a_result(self):
        ok = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"),
                 ("AUITests/testTwo", "started"), ("AUITests/testTwo", "failed"), executed=2)
        self.assertEqual(shards.check_results(ok, self.SELECTED), [])

    def test_missing_skipped_extra_or_miscounted_tests_fail(self):
        missing = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"), executed=1)
        skipped = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"),
                      ("AUITests/testTwo", "started"), ("AUITests/testTwo", "skipped"), executed=2)
        extra = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"),
                    ("AUITests/testTwo", "started"), ("AUITests/testTwo", "passed"),
                    ("BUITests/testThree", "started"), ("BUITests/testThree", "passed"), executed=3)
        unfinished = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"), ("AUITests/testTwo", "started"))
        uncounted = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"),
                        ("AUITests/testTwo", "started"), ("AUITests/testTwo", "passed"))
        miscounted = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"),
                         ("AUITests/testTwo", "started"), ("AUITests/testTwo", "passed"), executed=5)
        for name, text in (("missing", missing), ("skipped", skipped), ("extra", extra),
                           ("unfinished", unfinished), ("miscounted", miscounted), ("uncounted", uncounted)):
            with self.subTest(name):
                self.assertTrue(shards.check_results(text, self.SELECTED))

    def test_aggregate_requires_each_inventory_test_exactly_once(self):
        tests = ["AUITests/testOne", "AUITests/testTwo", "BUITests/testThree"]
        one = log(("AUITests/testOne", "started"), ("AUITests/testOne", "passed"))
        two = log(("AUITests/testTwo", "started"), ("AUITests/testTwo", "failed"),
                  ("BUITests/testThree", "started"), ("BUITests/testThree", "passed"))
        self.assertEqual(shards.check_aggregate([one, two], tests), [])
        self.assertTrue(shards.check_aggregate([one], tests))
        self.assertTrue(shards.check_aggregate([one, one, two], tests))
        self.assertTrue(shards.check_aggregate([one, two], tests[:2]))


class WorkflowTests(unittest.TestCase):
    def test_full_runs_shard_three_ways_and_focused_runs_do_not(self):
        self.assertIn("""shard: ${{ fromJSON(inputs.ios_test != '' && '["focused"]' || '["0", "1", "2"]') }}""", WORKFLOW)
        self.assertIn("fail-fast: false", WORKFLOW)
        self.assertIn("IOS_SHARD_COUNT: ${{ matrix.shard != 'focused' && '3' || '' }}", WORKFLOW)
        self.assertIn("IOS_SHARD_INDEX: ${{ matrix.shard != 'focused' && matrix.shard || '' }}", WORKFLOW)
        self.assertIn("check-aggregate --count 3 shard-logs/*/settings-ios-build.log", WORKFLOW)
        self.assertIn("if: always() && needs.settings-ios.result != 'skipped' && inputs.ios_test == ''", WORKFLOW)

    def test_each_shard_keeps_its_own_diagnostics(self):
        for name in ("settings-ios-build-log", "settings-ios-journeys", "settings-swiftui-screenshots"):
            self.assertIn(f"name: {name}-${{{{ matrix.shard }}}}", WORKFLOW)
        self.assertIn("cp dl/settings-swiftui-screenshots*/*.png out/swiftui/", WORKFLOW)

    def test_hosted_script_checks_enumeration_and_results_in_shard_mode(self):
        script = (ROOT / "tools/playground-ios/run-hosted-journeys.sh").read_text(encoding="utf-8")
        enumerate_at = script.index("-enumerate-tests")
        self.assertLess(enumerate_at, script.index("ios_test_shards.py check-enumeration"))
        self.assertLess(script.index("ios_test_shards.py check-enumeration"), script.index('xcodebuild "${test_args[@]}"'))
        self.assertLess(script.index('xcodebuild "${test_args[@]}"'), script.index("ios_test_shards.py check-results"))
        self.assertIn("IOS_TEST and IOS_SHARD_COUNT are exclusive.", script)


if __name__ == "__main__":
    unittest.main()
