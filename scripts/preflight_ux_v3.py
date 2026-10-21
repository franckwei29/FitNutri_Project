#!/usr/bin/env python3
"""Read-only v3 inventory. No migration, repair, secret/body output or inferred target."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
APPROVED_VERSION=13
TABLES=('app_user','user_profile','weight_entry','food_reference','exercise_type','food_record','exercise_record','submission_receipt','food_dish_record','workout_record','workout_movement_log','food_search_profile','food_search_alias','exercise_option','movement_catalogue','food_dataset','activity_dataset')

def validate_target(container,user,database):
    if not re.fullmatch(r'fitnutri-[A-Za-z0-9_-]+',container or '') or not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*',user or '') or not re.fullmatch(r'fitnutri_[A-Za-z0-9_]+',database or ''):
        raise ValueError('Explicit owned FitNutri container, role and database required; no shared/default fallback.')

def discover_sql():
    return "SELECT coalesce(jsonb_object_agg(table_name,columns),'{}'::jsonb) FROM (SELECT table_name,jsonb_agg(column_name ORDER BY ordinal_position) columns FROM information_schema.columns WHERE table_schema='public' AND table_name IN ("+','.join("'"+t+"'" for t in TABLES)+") GROUP BY table_name) t;"

def inventory_sql(tables):
    statements=["BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;", "SELECT jsonb_build_object('kind','identity','database',current_database(),'serverVersion',current_setting('server_version_num'),'transactionReadOnly',current_setting('transaction_read_only'));","SELECT jsonb_build_object('kind','columns','data',"+discover_sql()[7:discover_sql().index(" FROM (")]+")"+discover_sql()[discover_sql().index(" FROM ("):]]
    # Known schema names only; user values never enter SQL identifiers or expressions.
    for table in TABLES:
        if table not in tables:continue
        parts=["'kind','table'","'table','"+table+"'","'rows',count(*)"]
        if 'deleted_at' in tables[table]:parts.append("'deleted',count(*) FILTER(WHERE deleted_at IS NOT NULL)")
        if table in ('food_record','exercise_record'):
            energy='energy_kj' if table=='food_record' else 'estimated_kj'
            parts.extend(["'knownEnergyKj',sum("+energy+") FILTER(WHERE deleted_at IS NULL)","'unknownEnergy',count(*) FILTER(WHERE deleted_at IS NULL AND "+energy+" IS NULL)"])
            if table=='food_record':parts.append("'quantitySum',sum(quantity_grams)")
            else:parts.append("'minutesSum',sum(duration_minutes) FILTER(WHERE deleted_at IS NULL)")
            # Only original identity, dates and numeric snapshots; never meal names or health answers.
            snapshot="concat_ws('|',id,user_id,client_entry_id,recorded_date,version,deleted_at,"+energy+(",quantity_grams" if table=='food_record' else ",duration_minutes")+")"
            parts.append("'snapshotChecksum',md5(coalesce(string_agg("+snapshot+",';' ORDER BY id),''))")
        if table=='submission_receipt':parts.append("'identityChecksum',md5(coalesce(string_agg(concat_ws('|',id,user_id,submission_id,operation,request_sha256,result_json::text),';' ORDER BY id),''))")
        statements.append('SELECT jsonb_build_object('+','.join(parts)+') FROM '+table+';')
    statements.extend(["SELECT jsonb_build_object('kind','migrations','rows',coalesce(jsonb_agg(jsonb_build_object('version',version,'success',success,'checksum',checksum) ORDER BY installed_rank),'[]')) FROM flyway_schema_history;", "SELECT jsonb_build_object('kind','constraints','unvalidated',count(*) FILTER(WHERE NOT convalidated)) FROM pg_constraint WHERE connamespace='public'::regnamespace;", "SELECT jsonb_build_object('kind','indexes','invalid',count(*) FILTER(WHERE NOT i.indisvalid),'names',jsonb_agg(c.relname ORDER BY c.relname)) FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid JOIN pg_class t ON t.oid=i.indrelid WHERE t.relnamespace='public'::regnamespace;"])
    sources=[]
    for table in ('food_dataset','activity_dataset'):
        if table in tables:sources.append("SELECT name,release_name,revision,content_sha256,(SELECT count(*) FROM "+("food_reference" if table=="food_dataset" else "exercise_type")+" r WHERE r.dataset_id=d.id) AS record_count FROM "+table+" d WHERE verified")
    statements.append("SELECT jsonb_build_object('kind','sources','verified',coalesce(jsonb_agg(to_jsonb(s)),'[]')) FROM ("+' UNION ALL '.join(sources)+") s;")
    conflicts={
        'canonicalEmailCollisions':"SELECT min(id) id FROM app_user GROUP BY lower(btrim(email)) HAVING count(*)>1",
        'duplicateDatedWeights':"SELECT min(id) id FROM weight_entry GROUP BY user_id,entry_date HAVING count(*)>1",
        'invalidWeights':"SELECT id FROM weight_entry WHERE weight_kg::text IN ('NaN','Infinity','-Infinity') OR weight_kg<20 OR weight_kg>400 OR weight_kg::numeric<>round(weight_kg::numeric,2)",
        'invalidFoodQuantities':"SELECT id FROM food_record WHERE quantity_grams IS NULL OR quantity_grams<=0 OR quantity_grams>10000 OR quantity_grams::text IN ('NaN','Infinity','-Infinity')",
        'invalidMinutes':"SELECT id FROM exercise_record WHERE duration_minutes<=0 OR duration_minutes>1440 OR duration_minutes::numeric<>round(duration_minutes::numeric,2)",
    }
    present=[t for t in ('food_record','exercise_record','food_dish_record','workout_record') if t in tables]
    identities=' UNION ALL '.join('SELECT id,user_id,client_entry_id FROM '+t for t in present)
    conflicts['clientIdentityCollisions']='SELECT min(id) id FROM ('+identities+') x GROUP BY user_id,client_entry_id HAVING count(*)>1'
    if 'food_dish_record' in tables:
        conflicts['dishConsistency']="SELECT d.id FROM food_dish_record d WHERE (d.deleted_at IS NULL AND (SELECT count(*) FROM food_record f WHERE f.dish_id=d.id AND f.deleted_at IS NULL) NOT BETWEEN 1 AND 20) OR (d.deleted_at IS NOT NULL AND EXISTS(SELECT 1 FROM food_record f WHERE f.dish_id=d.id AND f.deleted_at IS NULL)) OR EXISTS(SELECT 1 FROM food_record f WHERE f.dish_id=d.id AND f.deleted_at IS NULL AND (f.user_id<>d.user_id OR f.recorded_date<>d.recorded_date OR f.meal_type<>d.meal_type OR f.quantity_grams<>round(f.ingredient_input_grams*d.portion_numerator/d.portion_denominator,12)))"
    if 'workout_record' in tables:
        conflicts['workoutConsistency']="SELECT w.id FROM workout_record w WHERE (w.deleted_at IS NULL AND ((SELECT count(*) FROM exercise_record e WHERE e.workout_id=w.id AND e.deleted_at IS NULL) NOT BETWEEN 1 AND 12 OR (SELECT sum(duration_minutes) FROM exercise_record e WHERE e.workout_id=w.id AND e.deleted_at IS NULL)>1440 OR (SELECT count(*) FROM workout_movement_log m JOIN exercise_record e ON e.id=m.exercise_record_id WHERE e.workout_id=w.id AND e.deleted_at IS NULL)>100)) OR (w.deleted_at IS NOT NULL AND EXISTS(SELECT 1 FROM exercise_record e WHERE e.workout_id=w.id AND e.deleted_at IS NULL)) OR EXISTS(SELECT 1 FROM exercise_record e WHERE e.workout_id=w.id AND e.deleted_at IS NULL AND (e.user_id<>w.user_id OR e.recorded_date<>w.recorded_date))"
    for kind,query in conflicts.items():statements.append("SELECT jsonb_build_object('kind','conflict','check','"+kind+"','count',(SELECT count(*) FROM ("+query+") q),'sampleIds',(SELECT coalesce(jsonb_agg(id),'[]') FROM ("+query+" LIMIT 50) q));")
    statements.append('COMMIT;');return '\n'.join(statements)

def run_sql(container,user,database,sql):
    validate_target(container,user,database)
    result=subprocess.run(['docker','exec','-i','--env','PGOPTIONS=-c default_transaction_read_only=on -c statement_timeout=10000 -c lock_timeout=3000',container,'psql','-XAt','-v','ON_ERROR_STOP=1','-U',user,'-d',database],input=sql,text=True,capture_output=True,timeout=20)
    if result.returncode:raise RuntimeError('Read-only database inventory failed; inspect the private database diagnostic locally.')
    return [json.loads(line) for line in result.stdout.splitlines() if line.startswith('{')]

def verify_sources(sources,source_dir):
    manifests=[json.loads(p.read_text()) for p in (ROOT/'backend/src/main/resources/data/manifests').glob('*.json')]
    issues=[]
    for source in sources:
        manifest=next((m for m in manifests if (m.get('datasetName'),m.get('release'),m.get('revision'),m.get('recordsSha256'))==(source['name'],source['release_name'],source['revision'],source['content_sha256'])),None)
        if manifest is None:issues.append('Unknown verified dataset revision');continue
        if source.get('record_count')!=manifest['expectedCount']:issues.append('Verified catalogue record count mismatch')
        if source_dir is None:issues.append('Pinned source bytes require explicit --source-dir');continue
        data=ROOT/'backend/src/main/resources/data'/manifest['recordsFile']
        if hashlib.sha256(data.read_bytes()).hexdigest()!=manifest['recordsSha256']:issues.append('Bundled catalogue checksum mismatch')
        for item in manifest['sourceFiles']:
            path=source_dir/item['filename']
            if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest()!=item['sha256']:issues.append('Pinned source checksum missing or mismatched: '+item['filename'])
    return issues

def evaluate(reports,database,tables,source_dir=None):
    by_kind={r['kind']:r for r in reports if r.get('kind') not in ('table','conflict')}
    issues=[]
    if not {'identity','columns','migrations','constraints','indexes','sources'}<=by_kind.keys():issues.append('Incomplete inventory')
    identity=by_kind.get('identity',{})
    if identity.get('database')!=database or identity.get('transactionReadOnly')!='on' or not str(identity.get('serverVersion','')).startswith('16'):issues.append('Database identity/read-only/PG16 mismatch')
    if by_kind.get('columns',{}).get('data')!=tables:issues.append('Schema changed during discovery')
    history=by_kind.get('migrations',{}).get('rows',[])
    versions=[str(r['version']) for r in history]
    if not history or any(not r['success'] for r in history) or len(set(versions))!=len(versions) or versions!=[str(i) for i in range(1,len(versions)+1)] or not 8<=len(versions)<=APPROVED_VERSION:issues.append('Unapproved or incomplete migration history')
    for r in reports:
        if r.get('kind')=='conflict' and r['count']:issues.append(r['check'])
    if by_kind.get('constraints',{}).get('unvalidated',0) or by_kind.get('indexes',{}).get('invalid',0):issues.append('Unvalidated constraints or invalid indexes')
    issues.extend(verify_sources(by_kind.get('sources',{}).get('verified',[]),source_dir))
    return {'status':'BLOCKED_DATA' if issues else 'PASS','readOnly':True,'database':database,'issues':issues,'reports':reports,'migrationApplied':False,'rollback':'Forward fix or separately verified compatible backup/new-database restore; never truncate/down-migrate.'}

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--container',default=os.getenv('TEST_DB_DOCKER_CONTAINER'));p.add_argument('--user',default=os.getenv('TEST_DB_USERNAME'));p.add_argument('--database',required=True);p.add_argument('--source-dir',type=Path)
    args=p.parse_args()
    try:
        validate_target(args.container,args.user,args.database)
        tables=run_sql(args.container,args.user,args.database,discover_sql())[0]
        required={'app_user','user_profile','weight_entry','food_reference','exercise_type','food_record','exercise_record','submission_receipt','food_dataset','activity_dataset'}
        if not required<=tables.keys() or not {'recorded_date','client_entry_id','deleted_at','energy_kj'}<=set(tables.get('food_record',[])):raise ValueError('Reviewed V8+ baseline required; legacy adoption is a separate procedure.')
        result=evaluate(run_sql(args.container,args.user,args.database,inventory_sql(tables)),args.database,tables,args.source_dir)
    except (ValueError,RuntimeError,KeyError,IndexError,OSError,subprocess.SubprocessError):result={'status':'BLOCKED_ENV','readOnly':True,'issues':['Explicit target, reviewed baseline or read-only inventory unavailable. No changes made.'],'migrationApplied':False}
    print(json.dumps(result,sort_keys=True));return 0 if result['status']=='PASS' else 2
if __name__=='__main__':sys.exit(main())
