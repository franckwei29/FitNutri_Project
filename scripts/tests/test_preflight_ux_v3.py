"""Read-only inventory boundaries; real PostgreSQL execution is GroupConstraintsIT."""
import importlib.util
from pathlib import Path
import re
import unittest
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('ux_preflight',ROOT/'scripts/preflight_ux_v3.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class UxPreflightTest(unittest.TestCase):
    def reports(self):
        tables={'food_record':['recorded_date']}
        return tables,[{'kind':'identity','database':'fitnutri_it_test','serverVersion':'160015','transactionReadOnly':'on'},{'kind':'columns','data':tables},{'kind':'migrations','rows':[{'version':str(i),'success':True} for i in range(1,14)]},{'kind':'constraints','unvalidated':0},{'kind':'indexes','invalid':0},{'kind':'sources','verified':[]}]
    def test_requires_explicit_safe_target_and_does_not_accept_option_or_sql_injection(self):
        m.validate_target('fitnutri-integration-20261016','fitnutri_test','fitnutri_it_test')
        for c,u,d in [(None,'role','fitnutri_it_test'),('shared-prod','role','fitnutri_it_test'),('fitnutri-test','-bad','fitnutri_it_test'),('fitnutri-test','role',"fitnutri_it_x';DROP")]:
            with self.assertRaises(ValueError):m.validate_target(c,u,d)
    def test_generated_sql_contains_only_read_only_inventory_and_no_private_fields(self):
        sql=m.inventory_sql({t:[] for t in m.TABLES})
        self.assertIn('BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;',sql)
        self.assertNotRegex(sql,r'(?i)\b(INSERT|UPDATE|DELETE|ALTER|CREATE|DROP|TRUNCATE|REPAIR)\b')
        self.assertNotIn('password_hash',sql);self.assertNotIn('health_safety_answer',sql);self.assertNotIn('food_name',sql)
        self.assertIn('snapshotChecksum',sql);self.assertIn('sampleIds',sql)
    def test_clean_complete_report_is_read_only_and_not_migration_authority(self):
        tables,reports=self.reports();result=m.evaluate(reports,'fitnutri_it_test',tables)
        self.assertEqual('PASS',result['status']);self.assertTrue(result['readOnly']);self.assertFalse(result['migrationApplied'])
    def test_conflicts_unknown_history_and_changed_schema_fail_closed(self):
        tables,reports=self.reports();reports.append({'kind':'conflict','check':'dishConsistency','count':1,'sampleIds':[7]})
        self.assertIn('dishConsistency',m.evaluate(reports,'fitnutri_it_test',tables)['issues'])
        tables,reports=self.reports();reports[2]['rows'].append({'version':'99','success':True})
        self.assertNotEqual('PASS',m.evaluate(reports,'fitnutri_it_test',tables)['status'])
        tables,reports=self.reports();self.assertNotEqual('PASS',m.evaluate(reports,'fitnutri_it_test',{})['status'])
    def test_incomplete_wrong_identity_and_writable_session_fail_closed(self):
        tables,reports=self.reports();self.assertNotEqual('PASS',m.evaluate([], 'fitnutri_it_test',tables)['status'])
        reports[0]['transactionReadOnly']='off';self.assertNotEqual('PASS',m.evaluate(reports,'fitnutri_it_test',tables)['status'])
        tables,reports=self.reports();self.assertNotEqual('PASS',m.evaluate(reports,'fitnutri_it_other',tables)['status'])
    def test_verified_sources_require_actual_pinned_bytes_and_known_revision(self):
        self.assertTrue(m.verify_sources([{'name':'unknown','release_name':'x','revision':'x','content_sha256':'a'*64}],None))
        import json
        source=json.loads((ROOT/'backend/src/main/resources/data/manifests/ausnut-2023-r1.json').read_text())
        record={'name':source['datasetName'],'release_name':source['release'],'revision':source['revision'],'content_sha256':source['recordsSha256'],'record_count':source['expectedCount']}
        self.assertEqual(['Pinned source bytes require explicit --source-dir'],m.verify_sources([record],None))
