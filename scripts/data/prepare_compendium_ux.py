#!/usr/bin/env python3
"""Add reviewed UX source selections as r2; never change the original r1 artifacts or official METs."""
import argparse,hashlib,json,subprocess
from pathlib import Path
from prepare_compendium import ROOT,SELECTIONS,prepare as prepare_r1,select_activities
UX_SELECTIONS=SELECTIONS+[
 ('01200','stationary-cycling-general','Stationary cycling (general)'),
 ('01270','spin-bike-class','Spin bike class'),
 ('02065','stair-treadmill','Stair treadmill ergometer (general)'),
 ('02080','ski-machine','Ski machine (general)'),
 ('02082','ski-ergometer-slow-moderate','Ski ergometer (slow to moderate double poling)'),
 ('15055','basketball','Basketball (general)'),
 ('15610','soccer-casual','Soccer (casual, general)'),
 ('15660','table-tennis','Table tennis / ping pong'),
 ('15675','tennis-moderate','Tennis (moderate effort)'),
 ('15710','volleyball','Volleyball'),
 ('17358','treadmill-brisk-walk','Treadmill walking (5.6–6.3 km/h, level)')]
def select_reviewed_rows(text,selections=UX_SELECTIONS):return select_activities(text,selections)
def prepare(directory):
 directory=Path(directory);_,old_manifest=prepare_r1(directory)
 text=subprocess.run(['pdftotext','-layout',str(directory/'2024-adult-compendium.pdf'),'-'],check=True,capture_output=True,text=True).stdout
 rows=select_reviewed_rows(text);payload=(json.dumps(dict(format='fitnutri.activity.v1',activities=rows),ensure_ascii=False,indent=2)+'\n').encode()
 manifest=json.loads(old_manifest);manifest.update(revision='2025-02-pdf_ux-r2',recordsFile='compendium-2024-activities-r2.json',recordsSha256=hashlib.sha256(payload).hexdigest(),expectedCount=len(rows))
 manifest['mapping'].update(reviewDate='2026-10-17',selectionPolicy='Retain every r1 selection verbatim; add individually checked official device/sport codes for UX. Exact source conditions and METs preserved; no interpolation or averaging.',selections=[dict(code=c,activityKey=k) for c,k,_ in UX_SELECTIONS])
 manifest['sourceNote']='Source: same SHA-pinned 2024 Adult Compendium PDF and adult age page as r1. UX additions checked against exact source codes/descriptions on 2026-10-17. Original r1 artifacts unchanged. Standard population estimates, not individual/device measurements; source applicability/conditions retained. Educational use; no endorsement or source artwork.'
 return payload,(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n').encode()
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source-dir',type=Path,default=ROOT/'.local/reference-sources');parser.add_argument('--write',action='store_true');args=parser.parse_args();payload,manifest=prepare(args.source_dir)
 for name,data in [('compendium-2024-activities-r2.json',payload),('manifests/compendium-2024-r2.json',manifest)]:
  dest=ROOT/'backend/src/main/resources/data'/name
  if args.write:dest.write_bytes(data)
  elif not dest.exists() or dest.read_bytes()!=data:raise SystemExit('Reviewed UX activity source drift: '+name)
 print('PASS:26 source-pinned official activities; r1 retained, r2 code/description/MET/age and bytes verified')
