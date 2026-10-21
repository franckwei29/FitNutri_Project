import sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'data'))
from prepare_compendium import SELECTIONS
from prepare_compendium_ux import UX_SELECTIONS,select_reviewed_rows
class CompendiumUxPreparationTest(unittest.TestCase):
 def test_existing_reviewed_rows_are_retained_verbatim_and_new_codes_unique(self):
  self.assertEqual(SELECTIONS,UX_SELECTIONS[:len(SELECTIONS)])
  self.assertEqual(26,len(UX_SELECTIONS));self.assertEqual(len(UX_SELECTIONS),len({r[0] for r in UX_SELECTIONS}))
 def test_source_code_condition_and_met_are_not_generalised_or_averaged(self):
  row=select_reviewed_rows('Activity 99901 4.5 Synthetic, under 100 watts\n',[('99901','synthetic','Synthetic under 100 watts')])[0]
  self.assertEqual('Synthetic, under 100 watts',row['sourceDescription']);self.assertEqual(4.5,row['metValue'])
  for text in ['', 'Activity 99901 4.5 Synthetic\nActivity 99901 5.0 Other\n']:
   with self.assertRaises(ValueError):select_reviewed_rows(text,[('99901','synthetic','Synthetic')])
