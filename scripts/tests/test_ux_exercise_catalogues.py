import sys,unittest,json,copy
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from validate_ux_catalogues import validate_exercise
ROOT=Path(__file__).resolve().parents[2]
class ExerciseUxCataloguesTest(unittest.TestCase):
 def setUp(self):
  data=ROOT/'backend/src/main/resources/data'
  self.options=json.loads((data/'ux/exercise-options-v1.json').read_text());self.movements=json.loads((data/'ux/movement-catalogue-v1.json').read_text());self.activities=json.loads((data/'compendium-2024-activities-r2.json').read_text())['activities']
 def validate(self):return validate_exercise(self.options,self.movements,self.activities)
 def test_complete_coverage_and_typed_ambiguous_aliases(self):
  result=self.validate();self.assertEqual([],result['errors']);self.assertGreaterEqual(result['options'],30);self.assertGreaterEqual(result['movements'],35);self.assertIn('划船',result['ambiguousAliases'])
 def test_generic_rower_never_becomes_an_under_100w_specific_measurement(self):
  row=next(x for x in self.options['entries'] if x['optionKey']=='rowing-machine');ref=next(x for x in self.activities if x['activityKey']=='rowing');row.update(mappingKind='SPECIFIC_REFERENCE',activityKey='rowing',sourceActivityCode=ref['sourceActivityCode'],sourceDescription=ref['sourceDescription']);self.assertTrue(self.validate()['errors'])
 def test_source_condition_mismatch_unreviewed_duplicates_and_met_copies_fail(self):
  original=copy.deepcopy(self.options)
  for mutate in [lambda r:r.update(sourceDescription='Invented condition'),lambda r:r.update(reviewStatus='DRAFT'),lambda r:r.update(metValue=99),lambda r:r.update(aliases=['bike','ＢＩＫＥ'])]:
   self.options=copy.deepcopy(original);row=next(x for x in self.options['entries'] if x['mappingKind']!='UNAVAILABLE');mutate(row);self.assertTrue(self.validate()['errors'])
  self.options=copy.deepcopy(original);self.options['entries'].append(copy.deepcopy(self.options['entries'][0]));self.assertTrue(self.validate()['errors'])
 def test_movements_have_no_met_or_time_and_require_reviewed_strength_default(self):
  original=copy.deepcopy(self.movements)
  for change in [dict(metValue=3.5),dict(durationMinutes=30),dict(defaultOptionKey='stationary-bike'),dict(loadBasis='TOTAL_UNKNOWN'),dict(allowedFields=['loadKg','calories'])]:
   self.movements=copy.deepcopy(original);self.movements['entries'][0].update(change);self.assertTrue(self.validate()['errors'])
