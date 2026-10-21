#!/usr/bin/env python3
"""Validate reviewed UX names/aliases against pinned official identities; never generate nutrients/MET."""
from pathlib import Path
import json
import re
import sys
import unicodedata
ROOT=Path(__file__).resolve().parents[1]
FACETS={'state':{'RAW','COOKED'},'part':{'BREAST','THIGH'},'cookingMethod':{'FRIED','BOILED'},'addedFat':{'YES','NO'},'dairyFat':{'REGULAR','REDUCED','SKIM'},'type':{'WHITE','BROWN'}}
def normalise(value):return re.sub(r'[\W_]+',' ',unicodedata.normalize('NFKC',value).lower()).strip()
def validate_food(document,foods):
    errors=[];known={x['publicFoodKey']:x for x in foods};keys=set();aliases={};short_names={};concepts=set();reviewed=0
    if set(document)!={'format','metadataRevision','dataset','entries'} or document.get('format')!='fitnutri.food-search-metadata.v1' or not isinstance(document.get('metadataRevision'),str) or not document.get('metadataRevision'):errors.append('Invalid document envelope')
    manifest=json.loads((ROOT/'backend/src/main/resources/data/manifests/ausnut-2023-r1.json').read_text())
    expected={'name':manifest['datasetName'],'release':manifest['release'],'revision':manifest['revision'],'recordsSha256':manifest['recordsSha256']}
    if document.get('dataset')!=expected:errors.append('Dataset revision must match pinned catalogue')
    entries=document.get('entries',[])
    if not isinstance(entries,list) or not entries:return {'errors':['No reviewed entries'],'reviewed':0,'concepts':[]}
    for index,row in enumerate(entries):
        prefix=str(index)+': '
        if not isinstance(row,dict) or set(row)!={'publicFoodKey','shortNameEn','shortNameZh','conceptKey','aliases','facets','commonRank','reviewStatus','evidence'}:errors.append(prefix+'Unexpected/missing fields');continue
        key=row['publicFoodKey'];source=known.get(key)
        if key in keys:errors.append(prefix+'Duplicate source identity')
        keys.add(key)
        if not source:errors.append(prefix+'Unknown source key');continue
        if row['reviewStatus']!='REVIEWED':errors.append(prefix+'Unreviewed entry cannot activate')
        else:reviewed+=1
        if row['evidence']!={'field':'officialName','text':source['officialName']}:errors.append(prefix+'Evidence does not match immutable official name')
        if not isinstance(row['shortNameEn'],str) or not 1<=len(row['shortNameEn'].strip())<=100:errors.append(prefix+'Invalid short name')
        if row['shortNameZh'] is not None and (not isinstance(row['shortNameZh'],str) or not 1<=len(row['shortNameZh'].strip())<=100):errors.append(prefix+'Invalid Chinese display name')
        if not isinstance(row['conceptKey'],str) or not re.fullmatch('[a-z][a-z-]{0,99}',row['conceptKey']):errors.append(prefix+'Invalid concept');continue
        if type(row['commonRank']) is not int or not 0<=row['commonRank']<=100000:errors.append(prefix+'Invalid product rank')
        concepts.add(row['conceptKey']);name=source['officialName'].lower();facets=row['facets']
        if not isinstance(facets,dict):errors.append(prefix+'Invalid facets');continue
        for facet,value in facets.items():
            if facet not in FACETS or value not in FACETS[facet]:errors.append(prefix+'Unknown attribute');continue
            supported=True
            if facet=='state':supported=bool(re.search(r'\b(raw|uncooked)\b' if value=='RAW' else r'\b(cooked|fried|boiled|baked|roasted|grilled|steamed)\b',name))
            if facet=='part':supported=value.lower() in name
            if facet=='cookingMethod':supported=bool(re.search(r'\b'+value.lower()+r'\b',name))
            if facet=='addedFat':
                negative=bool(re.search(r'\bno (?:fat added|added fat)\b',name))
                supported=negative if value=='NO' else bool(re.search(r'\badded fat\b',name)) and not negative
            if facet=='dairyFat':supported={'REGULAR':'regular fat','REDUCED':'reduced fat','SKIM':'skim'}[value] in name
            if facet=='type':supported=bool(re.search(r'\b'+value.lower()+r'\b',name))
            if not supported:errors.append(prefix+'Unsupported/negated '+facet)
        title=normalise(row['shortNameEn']);meaning=(row['conceptKey'],json.dumps(facets,sort_keys=True))
        if title in short_names and short_names[title]!=meaning:errors.append(prefix+'Conflicting short name')
        short_names[title]=meaning
        seen=set()
        if not isinstance(row['aliases'],list) or not row['aliases']:errors.append(prefix+'Missing aliases');continue
        for alias in row['aliases']:
            if not isinstance(alias,dict) or set(alias)!={'locale','value'} or alias['locale'] not in ('en','zh') or not isinstance(alias['value'],str):errors.append(prefix+'Invalid alias');continue
            value=normalise(alias['value']);identity=(alias['locale'],value)
            if not 1<=len(value)<=80 or identity in seen:errors.append(prefix+'Empty/duplicate alias')
            seen.add(identity)
            if identity in aliases and aliases[identity]!=row['conceptKey']:errors.append(prefix+'Alias targets unrelated concepts')
            aliases[identity]=row['conceptKey']
    return {'errors':errors,'reviewed':reviewed,'concepts':sorted(concepts)}
EXERCISE_OPTION_KEYS=set('treadmill-walk treadmill-brisk-walk treadmill-run treadmill-incline-walk rowing-machine elliptical stair-climber stepper stationary-bike recumbent-bike spin-bike ski-machine skierg vertical-climber basketball soccer badminton tennis table-tennis volleyball walking jogging running hiking cycling swimming yoga dancing hiit strength-general'.split())
MOVEMENT_KEYS=set('bench-press incline-bench-press chest-press pec-deck cable-fly lat-pulldown seated-cable-row assisted-pullup barbell-row single-arm-dumbbell-row back-extension shoulder-press-machine dumbbell-shoulder-press lateral-raise reverse-fly squat smith-squat hack-squat leg-press leg-extension seated-leg-curl lying-leg-curl hip-thrust hip-abduction hip-adduction calf-raise deadlift biceps-curl triceps-extension cable-pushdown assisted-dip ab-crunch-machine torso-rotation leg-raise plank'.split())
UNAVAILABLE_OPTIONS=set('treadmill-run treadmill-incline-walk rowing-machine stepper skierg vertical-climber running swimming'.split())
def validate_exercise(options,movements,activities):
    errors=[];source={r['activityKey']:r for r in activities};seen_options={};seen_movements=set();aliases={}
    manifest=json.loads((ROOT/'backend/src/main/resources/data/manifests/compendium-2024-r2.json').read_text())
    expected={'name':manifest['datasetName'],'release':manifest['release'],'revision':manifest['revision'],'recordsSha256':manifest['recordsSha256']}
    if set(options)!={'format','revision','dataset','entries'} or options.get('format')!='fitnutri.exercise-options.v1' or options.get('dataset')!=expected or not options.get('revision'):errors.append('Invalid reviewed option envelope/source revision')
    if set(movements)!={'format','revision','defaultOptionRevision','entries'} or movements.get('format')!='fitnutri.movement-catalogue.v1' or movements.get('defaultOptionRevision')!=options.get('revision') or not movements.get('revision'):errors.append('Invalid reviewed movement/default option revision')
    def key(value):return isinstance(value,str) and re.fullmatch('[a-z][a-z0-9-]{0,99}',value)
    def strings(values,allowed=None):return isinstance(values,list) and all(isinstance(v,str) and 1<=len(v.strip())<=80 and normalise(v) and (allowed is None or v in allowed) for v in values) and len({normalise(v) for v in values})==len(values)
    def title(value):return isinstance(value,str) and 1<=len(value.strip())<=100
    for row in options.get('entries',[]):
        if not isinstance(row,dict) or set(row)!={'optionKey','displayName','category','periodKind','aliases','allowedDetails','mappingKind','activityKey','sourceActivityCode','sourceDescription','mappingReason','reviewStatus'}:errors.append('Unknown/missing option fields (no MET allowed)');continue
        identity=row['optionKey']
        if not key(identity) or identity in seen_options:errors.append('Invalid/duplicate option key');continue
        seen_options[identity]=row
        if not title(row['displayName']) or row['reviewStatus']!='REVIEWED' or not isinstance(row['mappingReason'],str) or not row['mappingReason'].strip():errors.append(identity+': unreviewed/invalid label or reason')
        if row['category'] not in ('CARDIO','STRENGTH','SPORTS','OUTDOOR_OTHER') or row['periodKind'] not in ('ACTIVITY','STRENGTH') or (row['category']=='STRENGTH')!=(row['periodKind']=='STRENGTH'):errors.append(identity+': invalid category/period')
        if not strings(row['aliases']) or not row['aliases'] or not strings(row['allowedDetails'],{'speedKmh','inclinePercent','distanceMetres','machineLevel'}):errors.append(identity+': invalid aliases/details')
        else:
            for alias in row['aliases']:aliases.setdefault(normalise(alias),[]).append('ACTIVITY_OPTION:'+identity)
        if row['mappingKind']=='UNAVAILABLE':
            if any(row[k] is not None for k in ('activityKey','sourceActivityCode','sourceDescription')):errors.append(identity+': unavailable mapping has a reference')
        elif row['mappingKind'] in ('SPECIFIC_REFERENCE','GENERAL_ACTIVITY_ESTIMATE'):
            official=source.get(row['activityKey'])
            if not official or row['sourceActivityCode']!=official['sourceActivityCode'] or row['sourceDescription']!=official['sourceDescription']:errors.append(identity+': immutable source condition/code mismatch')
        else:errors.append(identity+': unknown mapping kind')
        if identity in UNAVAILABLE_OPTIONS and row['mappingKind']!='UNAVAILABLE':errors.append(identity+': generic conditions are unknown; cannot activate a conditional reference')
    for row in movements.get('entries',[]):
        if not isinstance(row,dict) or set(row)!={'movementKey','displayName','aliases','bodyRegion','equipmentType','loadBasis','allowedFields','defaultOptionKey','reviewStatus'}:errors.append('Unknown/missing movement fields (no time/MET allowed)');continue
        identity=row['movementKey']
        if not key(identity) or identity in seen_movements:errors.append('Invalid/duplicate movement key');continue
        seen_movements.add(identity);default=seen_options.get(row['defaultOptionKey'])
        if not title(row['displayName']) or row['reviewStatus']!='REVIEWED' or not default or default['periodKind']!='STRENGTH' or default['reviewStatus']!='REVIEWED':errors.append(identity+': invalid reviewed strength default')
        if row['bodyRegion'] not in ('CHEST','BACK','SHOULDERS','LEGS_GLUTES','ARMS','CORE') or row['equipmentType'] not in ('BARBELL','DUMBBELL','KETTLEBELL','SMITH_MACHINE','MACHINE','CABLE','BODYWEIGHT','ASSISTED_MACHINE') or row['loadBasis'] not in ('TOTAL_BARBELL','PER_DUMBBELL','MACHINE_DISPLAY','ADDED_LOAD','ASSISTANCE'):errors.append(identity+': invalid body/equipment/load basis')
        if not strings(row['aliases']) or not row['aliases'] or not strings(row['allowedFields'],{'setCount','repetitions','loadKg'}):errors.append(identity+': invalid aliases/optional fields')
        else:
            for alias in row['aliases']:aliases.setdefault(normalise(alias),[]).append('STRENGTH_MOVEMENT:'+identity)
    if not EXERCISE_OPTION_KEYS<=set(seen_options):errors.append('Missing required product options')
    if not MOVEMENT_KEYS<=seen_movements:errors.append('Missing required movements')
    return {'errors':errors,'options':len(seen_options),'movements':len(seen_movements),'ambiguousAliases':{a:sorted(v) for a,v in aliases.items() if len(v)>1}}
def main():
    resources=ROOT/'backend/src/main/resources/data'
    doc=json.loads((resources/'ux/food-search-metadata-v1.json').read_text());foods=json.loads((resources/'ausnut-2023-foods-r1.json').read_text())['foods']
    report=validate_food(doc,foods);exercise=validate_exercise(json.loads((resources/'ux/exercise-options-v1.json').read_text()),json.loads((resources/'ux/movement-catalogue-v1.json').read_text()),json.loads((resources/'compendium-2024-activities-r2.json').read_text())['activities']);report['exercise']=exercise;print(json.dumps(report,ensure_ascii=False,sort_keys=True));return 1 if report['errors'] or exercise['errors'] else 0
if __name__=='__main__':sys.exit(main())
