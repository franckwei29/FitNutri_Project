#!/usr/bin/env python3
"""Fail closed on incomplete/conflicted reports; reads stdin, writes no database."""
import json
import sys

reports = [json.loads(line) for line in sys.stdin if line.lstrip().startswith('{')]
by_kind = {r['kind']: r for r in reports}
required = {'identity', 'columns', 'constraints', 'indexes', 'counts', 'conflicts'}
missing = required - by_kind.keys()
if missing:
    print('BLOCKED: incomplete preflight sections: '+','.join(sorted(missing)))
    sys.exit(2)
conflicts = {k:v for k,v in by_kind['conflicts'].items() if k != 'kind' and v != 0}
if conflicts:
    print('BLOCKED: migration requires review of '+json.dumps(conflicts, sort_keys=True))
    sys.exit(3)
print('PASS: complete read-only schema inventory; no checked conflicts. Legacy timezone, backup and V1 schema comparison still required before adoption. No migration authorised by this report.')
