import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('provenance', Path(__file__).resolve().parents[1]/'release_provenance.py')
provenance = importlib.util.module_from_spec(spec)
spec.loader.exec_module(provenance)


class ProvenanceChecks(unittest.TestCase):
    def test_changed_source_artifact_and_logs_refused(self):
        for mismatch in ('none', 'source', 'dirty', 'artifact', 'log', 'task'):
            with self.subTest(mismatch=mismatch), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                output = root/'build/verification'
                output.mkdir(parents=True)
                artifact = root/'build/mod.jar'
                artifact.write_bytes(b'jar')
                identity = dict(commit='abc', tree='tree', dirty=False, fingerprint='source')
                tasks = []
                for name in ('dependencies', 'check-build'):
                    log = output/(name+'.log')
                    log.write_text('fresh output')
                    tasks.append(dict(name=name, exitCode=0, logSha256=hashlib.sha256(log.read_bytes()).hexdigest()))
                report = dict(source=dict(identity), tasks=tasks, artifacts={'build/mod.jar':hashlib.sha256(artifact.read_bytes()).hexdigest()})
                receipt = output/'project.json'
                receipt.write_text(json.dumps(report))
                if mismatch == 'source': identity['fingerprint'] = 'changed'
                if mismatch == 'dirty': identity['dirty'] = True
                if mismatch == 'artifact': artifact.write_bytes(b'changed')
                if mismatch == 'log': (output/'check-build.log').write_text('stale green log')
                if mismatch == 'task':
                    report['tasks'][0]['exitCode'] = 1
                    receipt.write_text(json.dumps(report))
                with patch.object(provenance, 'source_identity', lambda root: identity):
                    if mismatch == 'none': self.assertEqual(provenance.verified_source(root, artifact), identity)
                    else:
                        with self.assertRaises(RuntimeError): provenance.verified_source(root, artifact)


if __name__ == '__main__': unittest.main()
