#!/usr/bin/env python3
"""Retrieve pinned public Compendium evidence; reject changed official content for explicit re-review."""
import argparse,gzip,hashlib
from pathlib import Path
from urllib.request import Request,urlopen
from urllib.parse import urlparse
from prepare_compendium import ROOT,SOURCES

def fetch(directory):
 directory=Path(directory);directory.mkdir(parents=True,exist_ok=True)
 for name,(url,expected) in SOURCES.items():
  dest=directory/name
  if dest.exists():
   if hashlib.sha256(dest.read_bytes()).hexdigest()!=expected:raise ValueError('Unexpected existing source; inspect rather than overwrite: '+name)
  elif name == '2024-adult-compendium-age-source.html':
   # Reproduce immutable r1/r2 evidence; never accept the live page under an old hash.
   snapshot=ROOT/'scripts/data/evidence'/('2024-adult-compendium-age-source.html.gz')
   with gzip.open(snapshot,'rb') as evidence:
    data=evidence.read(20*1024*1024+1)
   if len(data)>20*1024*1024 or hashlib.sha256(data).hexdigest()!=expected:
    raise ValueError('Bundled official age evidence failed the original source SHA256 check')
   dest.write_bytes(data)
  else:
   with urlopen(Request(url,headers={'User-Agent':'FitNutri academic source verification'}),timeout=45) as response:
    if urlparse(response.geturl()).hostname!='pacompendium.com':raise ValueError('Unexpected official source redirect')
    data=response.read(20*1024*1024+1)
   if len(data)>20*1024*1024 or hashlib.sha256(data).hexdigest()!=expected:raise ValueError('Official source/page changed; review actual evidence and create a new revision: '+name)
   if name.endswith('.pdf') and not data.startswith(b'%PDF-'):raise ValueError('Expected official PDF')
   dest.write_bytes(data)
  print(name+' SHA256 '+expected+' VERIFIED')
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source-dir',type=Path,default=ROOT/'.local/reference-sources');args=parser.parse_args();fetch(args.source_dir)
