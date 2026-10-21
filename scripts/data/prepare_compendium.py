#!/usr/bin/env python3
"""Prepare a representative single option per activity from pinned official source facts. No invented METs."""
import argparse,hashlib,json,re,subprocess
from decimal import Decimal
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
PDF_URL='https://pacompendium.com/wp-content/uploads/2025/02/1_2024-adult-compendium_1_2024.pdf'
AGE_URL='https://pacompendium.com/adult-compendium/'
SOURCES={
 '2024-adult-compendium.pdf':(PDF_URL,'ac30234b8f8f813837e282773cfcb3e0fe062334777c2f7b3213429c4fbc251c'),
 '2024-adult-compendium-age-source.html':(AGE_URL,'6b3c3f397de722904c8d5e0718290393b99842ff8d8a13202312a548cd136697')
}
# Deliberate representative selections. Specific source conditions remain visible, never averaged across METs.
SELECTIONS=[('01014','cycling','Cycling (general)'),('02048','elliptical','Elliptical (moderate effort)'),('02054','strength-training','Strength training (multiple exercises, 8–15 reps)'),('02071','rowing','Rowing (stationary, under 100 watts)'),('02175','yoga','Yoga (general)'),('02210','hiit','HIIT (moderate effort)'),('03070','dancing','Dancing (contemporary, general)'),('12020','jogging','Jogging (self-selected pace)'),('12030','running','Running (5.0–5.2 mph)'),('15030','badminton','Badminton (social, general)'),('17080','hiking','Hiking (cross country)'),('17131','stair-climbing','Stair climbing (general)'),('17160','walking','Walking (for pleasure)'),('17200','brisk-walking','Brisk walking (3.5–3.9 mph)'),('18310','swimming','Swimming (leisurely, not laps)')]
def select_activities(text,selections=SELECTIONS):
 rows=[]
 for code,key,display in selections:
  matches=re.findall(r'^.*?\b'+re.escape(code)+r'\s+([0-9.]+)\s+([^\r\n]+)',text,re.MULTILINE)
  if len(matches)!=1:raise ValueError('Missing or ambiguous reviewed source code: '+code)
  met=Decimal(matches[0][0]);description=' '.join(matches[0][1].split())
  if not met.is_finite() or met<=0 or met>Decimal('9999.99') or met.normalize().as_tuple().exponent< -2 or not description:raise ValueError('Invalid official MET/description: '+code)
  rows.append(dict(activityKey=key,displayName=display,sourceActivityCode=code,sourceDescription=description,metValue=float(met)))
 if len({r['activityKey'] for r in rows})!=len(rows) or len({r['sourceActivityCode'] for r in rows})!=len(rows):raise ValueError('Duplicate representative selection')
 return sorted(rows,key=lambda r:r['activityKey'])
def prepare(directory):
 directory=Path(directory)
 for name,(_,sha) in SOURCES.items():
  if hashlib.sha256((directory/name).read_bytes()).hexdigest()!=sha:raise ValueError('Official source SHA mismatch: '+name)
 if b'ages 19-59' not in (directory/'2024-adult-compendium-age-source.html').read_bytes():raise ValueError('Age source evidence is missing')
 text=subprocess.run(['pdftotext','-layout',str(directory/'2024-adult-compendium.pdf'),'-'],check=True,capture_output=True,text=True).stdout
 rows=select_activities(text);payload=(json.dumps(dict(format='fitnutri.activity.v1',activities=rows),ensure_ascii=False,indent=2)+'\n').encode()
 manifest=dict(format='fitnutri.activity-manifest.v1',datasetName='COMPENDIUM_ADULT',release='2024',revision='2025-02-pdf_representative-r1',sourceUrl=PDF_URL,minAge=19,maxAge=59,recordsFile='compendium-2024-activities-r1.json',recordsSha256=hashlib.sha256(payload).hexdigest(),expectedCount=len(rows),sourceFiles=[dict(filename=n,url=u,sha256=h) for n,(u,h) in SOURCES.items()],mapping=dict(code='Specific Activity Code',met='MET Value',description='Activity Description',ageSourceUrl=AGE_URL,ageEvidence='ages 19-59',reviewDate='2026-10-16',selectionPolicy='One explicitly named representative per common activity; preserve conditions/code/MET, no averaging, no old map or Demo coefficients.',selections=[dict(code=c,activityKey=k) for c,k,_ in SELECTIONS]),sourceNote='Source: 2024 Adult Compendium of Physical Activities, Herrmann et al. 2024. Official pacompendium.com source facts and age page reviewed 2026-10-16. Educational application; no endorsement. Standard population MET estimates, not individual measurements. One representative option per activity; source conditions remain visible; MET values unchanged. No source artwork copied.')
 return payload,(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source-dir',type=Path,default=ROOT/'.local/reference-sources');parser.add_argument('--write',action='store_true');args=parser.parse_args()
 payload,manifest=prepare(args.source_dir)
 for name,data in [('compendium-2024-activities-r1.json',payload),('manifests/compendium-2024-r1.json',manifest)]:
  dest=ROOT/'backend/src/main/resources/data'/name
  if args.write:dest.write_bytes(data)
  elif not dest.exists() or dest.read_bytes()!=data:raise SystemExit('Reviewed activity artifact drift: '+name)
 print('PASS:15 representative official activity rows, source SHA/age evidence/code/MET and generated artifacts verified')
