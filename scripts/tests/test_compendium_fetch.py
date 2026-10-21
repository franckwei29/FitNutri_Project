"""Verify reproducible age evidence without accepting changed source bytes."""
import gzip
import hashlib
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'data'))
import fetch_compendium_sources as fetcher

NAME='2024-adult-compendium-age-source.html'
class PinnedEvidenceTest(unittest.TestCase):
    def test_fresh_cache_uses_exact_pinned_snapshot_without_network(self):
        with tempfile.TemporaryDirectory() as target, patch.object(fetcher,'SOURCES',{NAME:fetcher.SOURCES[NAME]}), patch.object(fetcher,'urlopen',side_effect=AssertionError('Unexpected live page request')):
            fetcher.fetch(target)
            data=(Path(target)/NAME).read_bytes()
            self.assertEqual(fetcher.SOURCES[NAME][1],hashlib.sha256(data).hexdigest())
            self.assertIn(b'ages 19-59',data)
            fetcher.fetch(target)
    def test_corrupt_snapshot_is_rejected_without_writing_cache(self):
        with tempfile.TemporaryDirectory() as root, tempfile.TemporaryDirectory() as target:
            evidence=Path(root)/'scripts/data/evidence';evidence.mkdir(parents=True)
            (evidence/(NAME+'.gz')).write_bytes(gzip.compress(b'changed evidence'))
            with patch.object(fetcher,'ROOT',Path(root)),patch.object(fetcher,'SOURCES',{NAME:fetcher.SOURCES[NAME]}):
                with self.assertRaisesRegex(ValueError,'SHA256'):fetcher.fetch(target)
            self.assertFalse((Path(target)/NAME).exists())
    def test_unexpected_existing_cache_is_preserved_and_rejected(self):
        with tempfile.TemporaryDirectory() as target,patch.object(fetcher,'SOURCES',{NAME:fetcher.SOURCES[NAME]}):
            file=Path(target)/NAME;file.write_bytes(b'unknown')
            with self.assertRaisesRegex(ValueError,'Unexpected existing'):fetcher.fetch(target)
            self.assertEqual(b'unknown',file.read_bytes())
