"""Guard completeness and dependency safety of the user-provided task trace."""
import re
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
class UxTraceTest(unittest.TestCase):
    def test_exact_task_set_and_acyclic_dependencies(self):
        spec=(ROOT/'docs/implementation/FitNutri_UX_Refinement_Execution_Plan_v3.md').read_text()
        cards=re.findall(r'^### (UX-[A-Z]\d+)｜[^\n]+\n(.*?)(?=^### |^# 21\.)',spec,re.M|re.S)
        graph={task:re.findall(r'UX-[A-Z]\d+',re.search(r'\*\*Dependencies:\*\* (.+)',body).group(1)) for task,body in cards}
        self.assertEqual(52,len(graph))
        ledger=(ROOT/'docs/implementation/ux-v3-tasks.md').read_text()
        self.assertEqual(set(graph),set(re.findall(r'^\| (UX-[A-Z]\d+) \|',ledger,re.M)))
        visited=set()
        def visit(task,stack):
            self.assertNotIn(task,stack)
            if task in visited:return
            for dep in graph[task]:
                self.assertIn(dep,graph);visit(dep,stack|{task})
            visited.add(task)
        for task in graph:visit(task,set())
    def test_all_scenarios_and_backend_changes_are_traced(self):
        matrix=(ROOT/'docs/verification/ux-v3-test-matrix.md').read_text()
        self.assertEqual({f'TUX-{i:03d}' for i in range(1,130)},set(re.findall(r'^\| (TUX-\d+) \|',matrix,re.M)))
        changes=(ROOT/'docs/implementation/ux-v3-backend-changes.md').read_text()
        self.assertEqual({f'UX-BC{i:02d}' for i in range(1,15)},set(re.findall(r'^\| (UX-BC\d+) \|',changes,re.M)))
    def test_current_scenario_evidence_has_no_duplicate_ids_or_broken_local_links(self):
        path=ROOT/'docs/verification/ux-v3-test-matrix.md'
        matrix=path.read_text()
        identifiers=re.findall(r'^\| (TUX-\d+) \|',matrix,re.M)
        self.assertEqual(129,len(identifiers))
        for target in re.findall(r'\]\(([^)]+)\)',matrix):
            self.assertTrue((path.parent/target).is_file(),target)
