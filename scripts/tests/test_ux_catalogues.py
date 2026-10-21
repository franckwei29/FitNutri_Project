import importlib.util
from pathlib import Path
import copy
import json
import unittest
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('ux_catalogues',ROOT/'scripts/validate_ux_catalogues.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class UxCatalogueTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.foods=json.loads((ROOT/'backend/src/main/resources/data/ausnut-2023-foods-r1.json').read_text())['foods']
        cls.document=json.loads((ROOT/'backend/src/main/resources/data/ux/food-search-metadata-v1.json').read_text())
    def test_reviewed_artifact_has_real_keys_and_required_concept_coverage(self):
        report=m.validate_food(self.document,self.foods)
        self.assertGreaterEqual(report['reviewed'],40)
        self.assertFalse(report['errors'])
        self.assertTrue({'rice','noodles','egg','chicken','beef','pork','fish','prawn','milk','milk-alternative','yoghurt','cheese','oats','bread','fruit','vegetable','tofu','legume','potato','oil','coffee'}<=set(report['concepts']))
    def test_orphan_reference_or_modified_evidence_is_rejected(self):
        doc=copy.deepcopy(self.document);doc['entries'][0]['publicFoodKey']='NOT-REAL'
        self.assertTrue(m.validate_food(doc,self.foods)['errors'])
        doc=copy.deepcopy(self.document);doc['entries'][0]['evidence']['text']='invented'
        self.assertTrue(m.validate_food(doc,self.foods)['errors'])
    def test_negated_added_fat_is_not_classified_as_added_fat(self):
        doc=copy.deepcopy(self.document)
        row=next(x for x in doc['entries'] if x['publicFoodKey']=='F003718')
        self.assertEqual('NO',row['facets']['addedFat'])
        row['facets']['addedFat']='YES'
        self.assertTrue(m.validate_food(doc,self.foods)['errors'])
    def test_unknown_attributes_and_nutrition_in_metadata_are_rejected(self):
        doc=copy.deepcopy(self.document);doc['entries'][0]['facets']['state']='UNKNOWN_GUESSED'
        self.assertTrue(m.validate_food(doc,self.foods)['errors'])
        doc=copy.deepcopy(self.document);doc['entries'][0]['energyKj100g']=0
        self.assertTrue(m.validate_food(doc,self.foods)['errors'])
    def test_unreviewed_duplicate_and_cross_concept_aliases_cannot_activate(self):
        for alter in ('unreviewed','duplicate','alias'):
            doc=copy.deepcopy(self.document)
            if alter=='unreviewed':doc['entries'][0]['reviewStatus']='DRAFT'
            elif alter=='duplicate':doc['entries'].append(copy.deepcopy(doc['entries'][0]))
            else:
                egg=next(x for x in doc['entries'] if x['conceptKey']=='egg')
                egg['aliases'].append({'locale':'en','value':'rice'})
            self.assertTrue(m.validate_food(doc,self.foods)['errors'],alter)
