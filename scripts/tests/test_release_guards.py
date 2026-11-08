"""Run with normal Python, -O and PYTHONOPTIMIZE=1; never calls GitHub."""
import importlib.util
import sys
import zipfile
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / 'publish-releases.py'
sys.path.insert(0,str(SCRIPT.parent))
spec = importlib.util.spec_from_file_location('publish', SCRIPT)
publish = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publish)


class ReleaseGuards(unittest.TestCase):
    def test_incomplete_receipt_cannot_authorize_publication(self):
        from release_provenance import verified_source
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            receipt = root / 'project.json'
            for status in (None, 'running', 'failed', 'invalidated'):
                receipt.write_text(json.dumps(dict(status=status)))
                with self.subTest(status=status), self.assertRaisesRegex(RuntimeError, 'did not pass'):
                    verified_source(root, root / 'mod.jar', receipt)

    def test_invalid_release_states_never_publish(self):
        for failure in ('identity', 'checksum', 'missing', 'extra', 'notes', 'still_draft'):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                artifact = root / 'mod.jar'
                with zipfile.ZipFile(artifact,'w') as jar: jar.writestr('fabric.mod.json','{}')
                notes = root / 'notes.md'
                notes.write_text('Expected notes')
                plan = root / 'plan.json'
                plan.write_text(json.dumps([dict(tag='v1', title='v1', notes=str(notes), assets=[str(artifact)], version='1')]))
                remote = dict(draft=failure not in ('missing',), body='Wrong' if failure == 'notes' else 'Expected notes', assets=[dict(name='mod.jar', size=12, id=1)], html_url='https://example.invalid/release')
                if failure == 'missing': remote['assets'] = []
                if failure == 'extra': remote['assets'].append(dict(name='extra', size=0, id=2))
                calls = []

                def api(endpoint):
                    if endpoint == 'user': return dict(login='wrong' if failure == 'identity' else 'aidentallen')
                    if '/git/ref/' in endpoint: return {}
                    return remote

                def gh(*args, **kwargs):
                    calls.append(args)
                    if args[:2] == ('release', 'edit') and failure != 'still_draft': remote['draft'] = False
                    return ''

                with patch.object(publish, 'api', api), patch.object(publish, 'gh', gh), patch.object(publish, 'asset_matches', lambda *args: failure != 'checksum'), patch.object(publish, 'ROOT', root), patch('sys.argv', [str(SCRIPT), str(plan), '--historical-migration']):
                    with self.assertRaises(RuntimeError): publish.main()
                if failure != 'still_draft': self.assertFalse(any(call[:2] == ('release', 'edit') for call in calls))
                self.assertFalse((root / 'build/publication/published-releases.json').exists())

    def test_current_plan_requires_exact_provenance_and_rejects_restricted_art(self):
        import hashlib
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary)
            jar=root/'mod.jar'
            with zipfile.ZipFile(jar,'w') as archive: archive.writestr('fabric.mod.json','{}')
            notes=root/'notes.md';notes.write_text('notes')
            receipt=root/'project.json'
            digest=hashlib.sha256(jar.read_bytes()).hexdigest()
            receipt.write_text(json.dumps(dict(artifacts={'mod.jar':digest},sourceArchiveSha256=digest)))
            release=dict(tag='v1',assets=[str(jar)],notes=str(notes),artifact=str(jar),verificationReceipt=str(receipt),sourceCommit='verified',assetSha256={'mod.jar':digest})
            document=dict(format=1,releases=[release])
            with patch.object(publish,'verified_source',lambda *args:dict(commit='verified')):
                self.assertEqual(publish.prepare_plan(document),[release])
                release['sourceCommit']='changed'
                with self.assertRaisesRegex(RuntimeError,'commit'): publish.prepare_plan(document)
                release['sourceCommit']='verified';release['assetSha256']={'mod.jar':'wrong'}
                with self.assertRaisesRegex(RuntimeError,'hashes'): publish.prepare_plan(document)
            with patch.object(publish,'api',lambda endpoint:dict(object=dict(type='commit',sha='wrong'))):
                with self.assertRaisesRegex(RuntimeError,'tag differs'): publish.verify_tag('v1','verified')
            with zipfile.ZipFile(jar,'w') as archive: archive.writestr('assets/riserecode/textures/font/qraftys.png',b'forbidden')
            with self.assertRaisesRegex(RuntimeError,'Restricted'):publish.audit_assets([jar])
            with self.assertRaisesRegex(RuntimeError,'format=1'):publish.prepare_plan([release])


if __name__ == '__main__': unittest.main()
