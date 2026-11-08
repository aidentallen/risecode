"""Unconditional source/artifact validation shared by release tooling."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile


def source_identity(root):
    def git(*args): return subprocess.check_output(['git', *args], cwd=root)
    paths = sorted(set(git('ls-files', '-z').split(b'\0')) | set(git('ls-files', '--others', '--exclude-standard', '-z').split(b'\0')))
    digest = hashlib.sha256()
    for raw in paths:
        if not raw: continue
        path = root / os.fsdecode(raw)
        digest.update(raw + b'\0')
        digest.update(path.read_bytes() if path.is_file() else b'<deleted>')
        digest.update(b'\0')
    return dict(commit=git('rev-parse', 'HEAD').decode().strip(), tree=git('rev-parse', 'HEAD^{tree}').decode().strip(),
                dirty=bool(git('status', '--porcelain')), fingerprint=digest.hexdigest())


def verified_source(root, artifact, receipt=None):
    receipt = receipt or root / 'build/verification/project.json'
    report = json.loads(receipt.read_text())
    if report.get('status') != 'passed': raise RuntimeError('Verification receipt did not pass')
    identity = source_identity(root)
    if identity['dirty'] or report['source'] != identity: raise RuntimeError('Release source differs from a clean verified commit')
    relative = str(artifact.relative_to(root))
    if report['artifacts'].get(relative) != hashlib.sha256(artifact.read_bytes()).hexdigest(): raise RuntimeError('Artifact differs from verified build')
    tasks = {task['name']: task for task in report['tasks']}
    for name in ('dependencies', 'check-build'):
        task = tasks.get(name)
        if task is None or task['exitCode'] != 0: raise RuntimeError('Missing successful verification task: '+name)
        log = receipt.parent / (name+'.log')
        if task['logSha256'] != hashlib.sha256(log.read_bytes()).hexdigest(): raise RuntimeError('Verification log changed: '+name)
    return identity


def license_inventory(root, artifact):
    """Require byte-identical checked-in attribution in each generated current JAR."""
    expected = {}
    for base in (root / 'licenses', root / 'src/main/resources/licenses'):
        if base.is_dir():
            for path in base.rglob('*'):
                if path.is_file(): expected['licenses/' + path.relative_to(base).as_posix()] = path.read_bytes()
    resources = root / 'src/main/resources'
    cosmetics = resources / 'assets/riserecode/cosmetics'
    if cosmetics.is_dir():
        for path in cosmetics.glob('*LICENSE*'): expected[path.relative_to(resources).as_posix()] = path.read_bytes()
    source = artifact.name.endswith('-sources.jar')
    for local, binary in [('LICENSE', 'licenses/risecode-MIT.txt'), ('THIRD_PARTY_NOTICES.md', 'licenses/THIRD_PARTY_NOTICES.md'), ('credits.txt', 'licenses/credits.txt')]:
        expected[local if source else binary] = (root/local).read_bytes()
    inventory = {}
    with zipfile.ZipFile(artifact) as archive:
        for name, content in sorted(expected.items()):
            try: actual = archive.read(name)
            except KeyError as missing: raise RuntimeError('Missing packaged attribution: ' + name) from missing
            if actual != content: raise RuntimeError('Changed packaged attribution: ' + name)
            inventory[name] = hashlib.sha256(actual).hexdigest()
    return inventory
