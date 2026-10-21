import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'data'))
from prepare_compendium import select_activities
class CompendiumPreparationTest(unittest.TestCase):
 def test_source_met_and_conditions_are_preserved(self):
  row=select_activities('Walking 99901 3.5 Synthetic example, general\n',[('99901','synthetic','Synthetic example')])[0]
  self.assertEqual(3.5,row['metValue']);self.assertEqual('Synthetic example, general',row['sourceDescription']);self.assertEqual('99901',row['sourceActivityCode'])
 def test_missing_and_ambiguous_codes_fail(self):
  for text in ['', 'X 99901 3.5 Synthetic\nX 99901 4.0 Different\n']:
   with self.assertRaises(ValueError):select_activities(text,[('99901','test','Test')])
 def test_zero_and_excess_precision_mets_fail(self):
  for met in ['0','3.555','10000']:
   with self.assertRaises(ValueError):select_activities('X 99901 '+met+' Synthetic\n',[('99901','test','Test')])
 def test_duplicate_selection_cannot_average_or_ambiguously_map(self):
  with self.assertRaises(ValueError):select_activities('X 99901 3.5 Synthetic\n',[('99901','test','Test'),('99901','other','Other')])
