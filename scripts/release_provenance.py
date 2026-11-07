"""Unconditional source/artifact validation shared by release tooling."""
import hashlib
import json
import os
from pathlib import Path
import subprocess


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
