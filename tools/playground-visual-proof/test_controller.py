import unittest
from controller import validate

class CommandPolicyTests(unittest.TestCase):
    def test_rejects_stale_frames_and_command_injection(self):
        good=dict(frame=2,action='tap',x=.4,y=.5,observation='Visible Add button')
        self.assertEqual(validate(good,2),good)
        for bad in [dict(good,frame=1),dict(good,shell='echo unsafe'),dict(good,action='shell'),dict(good,x=float('nan')),dict(good,y=2),dict(good,x=True)]:
            with self.assertRaises(ValueError): validate(bad,2)
    def test_finish_requires_an_explicit_observed_outcome(self):
        good=dict(frame=3,action='finish',observation='Photos and Files visible',outcome='opened')
        self.assertEqual(validate(good,3),good)
        with self.assertRaises(ValueError): validate(dict(good,outcome='passed'),3)

if __name__=='__main__': unittest.main()
