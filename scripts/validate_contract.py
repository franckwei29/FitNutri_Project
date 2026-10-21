#!/usr/bin/env python3
"""Validate the complete target OpenAPI and all embedded synthetic examples.
Install scripts/contract-requirements.txt into an isolated virtual environment.
"""
import copy
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker
from openapi_spec_validator import validate

ROOT = Path(__file__).resolve().parents[1]

def main():
    spec = json.loads((ROOT / 'docs/api/openapi-mvp.yaml').read_text())
    validate(spec)
    schemas = spec['components']['schemas']
    count = 0
    def check(schema, value):
        nonlocal count
        Draft202012Validator({**schema, 'components': spec['components']}, format_checker=FormatChecker()).validate(value)
        count += 1
    for path, item in spec['paths'].items():
        for method, operation in item.items():
            for media in operation.get('requestBody', {}).get('content', {}).values():
                if 'example' in media:
                    check(media['schema'], media['example'])
            for response in operation['responses'].values():
                if '$ref' in response:
                    response = spec['components']['responses'][response['$ref'].rsplit('/', 1)[1]]
                for media in response.get('content', {}).values():
                    if 'example' in media:
                        check(media['schema'], media['example'])
            if method not in ['get', 'head']:
                assert any(p['name'] == 'X-CSRF-TOKEN' and p['required'] for p in operation['parameters']), path
                assert all(k in operation['responses'] for k in ['400', '401', '403', '409']), path
    examples = json.loads((ROOT / 'docs/api/examples/write-requests.json').read_text())
    for case in examples:
        check({'$ref': '#/components/schemas/' + case['schema']}, case['value'])
    for case in json.loads((ROOT / 'docs/api/examples/read-responses.json').read_text()):
        check({'$ref': '#/components/schemas/' + case['schema']}, case['value'])
    by_schema = {x['schema']: x['value'] for x in examples}
    negative = []
    a = copy.deepcopy(by_schema['RegisterRequest']); a['role'] = 'ADMIN'; negative.append(('RegisterRequest', a))
    a = copy.deepcopy(by_schema['ProfileSettingsRequest']); del a['goalType']; negative.append(('ProfileSettingsRequest', a))
    a = copy.deepcopy(by_schema['FoodBatchRequest']); a['items'] = []; negative.append(('FoodBatchRequest', a))
    for grams in [0, -1, 10001]:
        a = copy.deepcopy(by_schema['FoodBatchRequest']); a['items'][0]['quantityGrams'] = grams; negative.append(('FoodBatchRequest', a))
    a = copy.deepcopy(by_schema['FoodBatchRequest']); a['items'][0]['referenceId'] = 101; negative.append(('FoodBatchRequest', a))
    a = copy.deepcopy(by_schema['FoodBatchRequest']); a['items'][0]['calories'] = 999; negative.append(('FoodBatchRequest', a))
    a = copy.deepcopy(by_schema['ExerciseCreateRequest']); a['durationMinutes'] = 1441; negative.append(('ExerciseCreateRequest', a))
    a = copy.deepcopy(by_schema['QaRequest']); a['userId'] = '2'; negative.append(('QaRequest', a))
    a = copy.deepcopy(by_schema['WeightRequest']); a['recordDate'] = '2026-02-30'; negative.append(('WeightRequest', a))
    for name, value in negative:
        validator = Draft202012Validator({'$ref': '#/components/schemas/'+name, 'components': spec['components']}, format_checker=FormatChecker())
        assert not validator.is_valid(value), (name, value)
    operations = [o['operationId'] for p in spec['paths'].values() for o in p.values()]
    assert len(set(operations)) == len(operations)
    assert len(schemas) >= 50 and len(operations) == 64
    print(f'PASS: OpenAPI 3.1 valid; {len(operations)} unique operations, {len(schemas)} schemas; {count} valid examples; {len(negative)} invalid examples rejected. 0 failures, 0 skips.')

if __name__ == '__main__':
    main()
