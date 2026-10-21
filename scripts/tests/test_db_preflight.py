#!/usr/bin/env python3
"""Real PG16 fixtures only, in unique disposable databases inside our test container."""
import json
import os
import re
from pathlib import Path
import subprocess
import unittest
import uuid

ROOT = Path(__file__).resolve().parents[2]
def isolated_target(environment):
    container = environment.get('TEST_DB_DOCKER_CONTAINER', '')
    user = environment.get('TEST_DB_USERNAME', '')
    if not re.fullmatch(r'fitnutri-integration-[a-z0-9-]+', container):
        raise ValueError('Set explicit TEST_DB_DOCKER_CONTAINER to an owned fitnutri-integration-* test container')
    if user != 'fitnutri_test':
        raise ValueError('Set TEST_DB_USERNAME=fitnutri_test for the isolated fixture role')
    return container, user

def docker(*args, stdin=None, check=True):
    container, _ = isolated_target(os.environ)
    return subprocess.run(['docker','exec','-i',container,*args], input=stdin, text=True, capture_output=True, check=check)

class PreflightTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        _, cls.user = isolated_target(os.environ)
        # Pin the legacy fixture to reviewed V1; the admin connection database may be empty.
        cls.schema = (ROOT/'backend/src/main/resources/db/migration/V1__legacy_schema_baseline.sql').read_text()

    def setUp(self):
        self.database = 'fitnutri_preflight_' + uuid.uuid4().hex
        docker('createdb','-U',self.user,self.database)
        self.sql(self.schema)

    def tearDown(self):
        # Only the exact UUID-named database created by this test can be removed.
        if self.database.startswith('fitnutri_preflight_') and len(self.database)==51:
            docker('dropdb','-U',self.user,self.database)

    def sql(self, text):
        return docker('psql','-XAt','-v','ON_ERROR_STOP=1','-U',self.user,'-d',self.database,stdin=text).stdout

    def report(self):
        out = self.sql((ROOT/'scripts/db/preflight.sql').read_text())
        check = subprocess.run(['python3',str(ROOT/'scripts/db/check_preflight.py')],input=out,text=True,capture_output=True)
        reports = {v['kind']:v for line in out.splitlines() if line.startswith('{') for v in [json.loads(line)]}
        return check, reports

    def test_clean_inventory_is_read_only_and_complete(self):
        before = self.sql('SELECT count(*) FROM app_user; SELECT count(*) FROM food_reference;')
        check, reports = self.report()
        self.assertEqual(0,check.returncode,check.stdout)
        self.assertEqual(6,len(reports))
        self.assertEqual(before,self.sql('SELECT count(*) FROM app_user; SELECT count(*) FROM food_reference;'))
        columns=reports['columns']['data']
        self.assertTrue(any(c['table_name']=='food_reference' and c['column_name']=='carbsg' for c in columns))
        self.assertTrue(any(c['table_name']=='food_reference' and c['column_name']=='proteing' for c in columns))
        self.assertFalse(reports['counts']['flywayPresent'])

    def test_email_and_weight_collisions_are_reported_without_private_values(self):
        self.sql("""INSERT INTO app_user (created_at,display_name,email,enabled,password_hash,role) VALUES
          (now(),'Synthetic A','collision@example.test',true,'synthetic-only','USER'),
          (now(),'Synthetic B','COLLISION@example.test',true,'synthetic-only','USER');
          INSERT INTO weight_entry(created_at,entry_date,weight_kg,user_id) VALUES
          (now(),'2026-01-01',70,1),(now(),'2026-01-01',80,1);""")
        check,reports=self.report()
        self.assertEqual(3,check.returncode)
        self.assertEqual(1,reports['conflicts']['canonicalEmailGroups'])
        self.assertEqual(1,reports['conflicts']['duplicateWeightDates'])
        self.assertNotIn('collision@example.test',json.dumps(reports))
        self.assertNotIn('synthetic-only',json.dumps(reports))
        self.assertEqual('2',self.sql('SELECT count(*) FROM weight_entry;').strip())

    def test_invalid_numbers_and_unknown_reference_foreign_keys_block(self):
        self.sql("""INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role)
          VALUES(now(),'Synthetic','fixture@example.test',true,'synthetic-only','USER');
          INSERT INTO weight_entry(created_at,entry_date,weight_kg,user_id) VALUES(now(),'2026-01-01','NaN',1);
          CREATE TABLE synthetic_external_reference (food_key varchar(255) REFERENCES food_reference(public_food_key));""")
        check,reports=self.report()
        self.assertEqual(3,check.returncode)
        self.assertEqual(1,reports['conflicts']['invalidWeights'])
        self.assertEqual(1,reports['conflicts']['foodReferenceForeignKeys'])

    def test_incomplete_report_fails_closed(self):
        check = subprocess.run(['python3',str(ROOT/'scripts/db/check_preflight.py')],input='',text=True,capture_output=True)
        self.assertEqual(2,check.returncode)
        self.assertIn('incomplete',check.stdout)

    def test_shell_requires_explicit_database_identity(self):
        import os
        env={k:v for k,v in os.environ.items() if k not in ['PGDATABASE','PGUSER','PGHOST']}
        check=subprocess.run(['sh',str(ROOT/'scripts/db/preflight.sh')],env=env,text=True,capture_output=True)
        self.assertNotEqual(0,check.returncode)
        self.assertIn('PGDATABASE',check.stderr)

class IsolatedTargetTest(unittest.TestCase):
    def test_missing_or_shared_target_is_rejected_without_docker(self):
        for environment in [{}, {'TEST_DB_DOCKER_CONTAINER':'shared-production','TEST_DB_USERNAME':'fitnutri_test'}, {'TEST_DB_DOCKER_CONTAINER':'fitnutri-integration-owned','TEST_DB_USERNAME':'postgres'}]:
            with self.assertRaises(ValueError):
                isolated_target(environment)

    def test_explicit_owned_target_is_used_instead_of_workstation_default(self):
        self.assertEqual(('fitnutri-integration-handover-test','fitnutri_test'), isolated_target({'TEST_DB_DOCKER_CONTAINER':'fitnutri-integration-handover-test','TEST_DB_USERNAME':'fitnutri_test'}))

if __name__=='__main__':
    unittest.main(verbosity=2)
