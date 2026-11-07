"""Run with normal Python, -O and PYTHONOPTIMIZE=1; never calls GitHub."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / 'publish-releases.py'
spec = importlib.util.spec_from_file_location('publish', SCRIPT)
publish = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publish)


class ReleaseGuards(unittest.TestCase):
    def test_invalid_release_states_never_publish(self):
        for failure in ('identity', 'checksum', 'missing', 'extra', 'notes', 'still_draft'):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                artifact = root / 'mod.jar'
                artifact.write_bytes(b'verified jar')
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

                with patch.object(publish, 'api', api), patch.object(publish, 'gh', gh), patch.object(publish, 'asset_matches', lambda *args: failure != 'checksum'), patch.object(publish, 'ROOT', root), patch('sys.argv', [str(SCRIPT), str(plan)]):
                    with self.assertRaises(RuntimeError): publish.main()
                if failure != 'still_draft': self.assertFalse(any(call[:2] == ('release', 'edit') for call in calls))
                self.assertFalse((root / 'build/publication/published-releases.json').exists())


if __name__ == '__main__': unittest.main()
