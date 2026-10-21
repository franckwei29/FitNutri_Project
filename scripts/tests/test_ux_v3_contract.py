"""Synthetic boundary checks for the target v3 contract; no live API claims."""
import copy
import json
from pathlib import Path
import unittest
from jsonschema import Draft202012Validator, FormatChecker
ROOT=Path(__file__).resolve().parents[2]
class UxV3ContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.spec=json.loads((ROOT/'docs/api/openapi-mvp.yaml').read_text())
    def valid(self,name,value):
        return Draft202012Validator({'$ref':'#/components/schemas/'+name,'components':self.spec['components']},format_checker=FormatChecker()).is_valid(value)
    def test_exact_fraction_and_percentage_are_different_valid_shapes(self):
        for value in [{'kind':'PERSONAL'},{'kind':'FRACTION','numerator':1,'denominator':3},{'kind':'PERCENT','percent':33.3},{'kind':'SERVINGS','total':4,'eaten':1}]:
            self.assertTrue(self.valid('PortionInputRequest',value),value)
        for value in [{'kind':'PERSONAL','percent':50},{'kind':'FRACTION','numerator':0,'denominator':3},{'kind':'PERCENT','percent':100.001},{'kind':'PERCENT','percent':'33.3'},{'kind':'SERVINGS','total':1.5,'eaten':1}]:
            self.assertFalse(self.valid('PortionInputRequest',value),value)
    def test_strict_kind_rejects_invented_energy_nested_dishes_and_numeric_ids(self):
        value={'kind':'SINGLE','clientEntryId':'11111111-1111-4111-8111-111111111111','referenceId':'101','quantityGrams':150,'mealType':'LUNCH'}
        self.assertTrue(self.valid('FoodEntryInputV2Request',value))
        for key,v in [('calories',100),('referenceId',101),('quantityGrams',0),('ingredients',[])]:
            changed=copy.deepcopy(value);changed[key]=v
            self.assertFalse(self.valid('FoodEntryInputV2Request',changed))
        self.assertFalse(self.valid('FoodIngredientV2Request',{'dishId':'1','inputGrams':100}))
    def test_optional_details_accept_null_and_zero_load_but_reject_unknown_or_invalid(self):
        self.assertTrue(self.valid('EquipmentDetailsV2Request',{}))
        self.assertTrue(self.valid('EquipmentDetailsV2Request',{'inclinePercent':-2,'speedKmh':None}))
        self.assertFalse(self.valid('EquipmentDetailsV2Request',{'met':5}))
        self.assertFalse(self.valid('EquipmentDetailsV2Request',{'speedKmh':'5'}))
        self.assertTrue(self.valid('MovementV2Request',{'catalogueId':'1','loadKg':0}))
        self.assertFalse(self.valid('MovementV2Request',{'catalogueId':'1','repetitions':1.5}))
    def test_all_planned_operations_are_session_protected_and_writes_require_csrf(self):
        ops=[o for p,methods in self.spec['paths'].items() if p.startswith('/v2/') or p=='/food-reference/smart-search' for o in methods.values()]
        self.assertEqual(20,len(ops))
        for op in ops:
            self.assertEqual([{'sessionCookie':[]}],op['security'])
            if 'requestBody' in op:self.assertTrue(any(p['name']=='X-CSRF-TOKEN' and p['required'] for p in op['parameters']))
    def test_legacy_hash_shapes_not_extended_with_v2_group_fields(self):
        s=self.spec['components']['schemas']
        self.assertEqual(['FOOD_BATCH','EXERCISE_CREATE'],s['SubmissionReceipt']['properties']['kind']['enum'])
        self.assertNotIn('entries',s['FoodBatchRequest']['properties'])
        self.assertEqual(50,s['FoodBatchRequest']['properties']['items']['maxItems'])
        self.assertFalse(s['SubmissionReceiptV2']['additionalProperties'])
