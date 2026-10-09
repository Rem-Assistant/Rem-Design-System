import json
from pathlib import Path
import tempfile
import unittest

import delivery


class EngineeringRoutingTests(unittest.TestCase):
    def test_narrow_engineering_delivery_needs_no_figma_and_carries_exact_head(self):
        with tempfile.TemporaryDirectory() as root:
            args=(Path(root),'a'*40,'success',['docs/agent-factory/routing-smoke.md'],{})
            status,body,media=delivery.prepare(*args,primary_contract='factory-routing',
                run_url='https://github.com/owner/repo/actions/runs/123')
            self.assertEqual(status,'ready'); self.assertEqual(media,[])
            self.assertIn('a'*40,body); self.assertIn('not-applicable',body)
            self.assertIn('Figma and product-design acceptance',body)
            for paths in ([],['Sources/UI.swift'],['docs/agent-factory/routing-smoke.md','tools/render-evidence/delivery.py']):
                with self.subTest(paths=paths), self.assertRaises(ValueError):
                    delivery.prepare(Path(root),'a'*40,'success',paths,{},'factory-routing',
                                     'https://github.com/owner/repo/actions/runs/123')
            with self.assertRaises(ValueError):
                delivery.prepare(*args,primary_contract='factory-routing')
            status,_,_=delivery.prepare(Path(root),'a'*40,'failure',args[3],{},'factory-routing',
                                        'https://github.com/owner/repo/actions/runs/123')
            self.assertEqual(status,'failed')

    def test_render_workflow_uses_head_not_merge_ref_and_checks_actual_checkout(self):
        root=Path(__file__).resolve().parents[2]
        workflow=(root/'.github/workflows/screenshots.yml').read_text()
        checkouts = workflow.count('uses: actions/checkout@v4')
        self.assertGreater(checkouts,0)
        self.assertEqual(workflow.count('ref: ${{ github.event.pull_request.head.sha || github.sha }}'),checkouts)
        self.assertEqual(workflow.count('persist-credentials: false'),checkouts)
        self.assertIn("if actual_head != os.environ['SHA']:",workflow)
        self.assertIn('"sha": actual_head',workflow)
        publisher=(root/'.github/workflows/publish-builder-delivery.yml').read_text()
        self.assertEqual(publisher.count("steps.validate.outputs.engineering != 'true'"),3)
        self.assertIn('test "$route_source" !=',publisher)
        self.assertIn('test "$(jq -r .figma.enabled consumer-delivery/.agent-factory/config.json)" = false',publisher)
        config=json.loads((root/'.agent-factory/config.json').read_text())
        self.assertFalse(config['figma']['enabled'])
