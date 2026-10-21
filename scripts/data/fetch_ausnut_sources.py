#!/usr/bin/env python3
"""Fetch pinned public source files into the ignored local cache; never replace an unexpected existing file."""
import argparse
import hashlib
from pathlib import Path
from urllib.request import Request,urlopen
from urllib.parse import urlparse
from prepare_ausnut import ROOT,SOURCES

def fetch(directory):
    directory=Path(directory);directory.mkdir(parents=True,exist_ok=True)
    for name,(url,expected) in SOURCES.items():
        destination=directory/name
        if destination.exists():
            if hashlib.sha256(destination.read_bytes()).hexdigest()!=expected:raise ValueError('Unexpected existing source file; inspect it rather than overwrite: '+name)
        else:
            with urlopen(Request(url,headers={'User-Agent':'FitNutri academic source verification'}),timeout=45) as response:
                if urlparse(response.geturl()).hostname!='www.foodstandards.gov.au':raise ValueError('Unexpected official download redirect')
                content=response.read(20*1024*1024+1)
            if len(content)>20*1024*1024 or not content.startswith(b'PK') or hashlib.sha256(content).hexdigest()!=expected:raise ValueError('Official source changed; inspect and create a new manifest revision: '+name)
            destination.write_bytes(content)
        print(name+' SHA256 '+expected+' VERIFIED')
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source-dir',type=Path,default=ROOT/'.local/reference-sources');args=parser.parse_args();fetch(args.source_dir)
