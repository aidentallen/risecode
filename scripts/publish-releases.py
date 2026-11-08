#!/usr/bin/env python3
"""Publish a prepared release plan using authenticated gh, verifying asset hashes."""
from pathlib import Path
import hashlib
import json
import subprocess
import sys
import argparse
import io
import zipfile
from release_provenance import verified_source

ROOT = Path(__file__).resolve().parent.parent
REPOSITORY = 'aidentallen/risecode'

def gh(*arguments, binary=False):
    result = subprocess.run(['gh', *arguments], check=True, capture_output=True)
    return result.stdout if binary else result.stdout.decode()

def api(path):
    return json.loads(gh('api', path))

def asset_matches(remote, path):
    expected = hashlib.sha256(path.read_bytes()).hexdigest()
    if remote['size'] != path.stat().st_size: return False
    if remote.get('digest'):
        return remote['digest'] == 'sha256:' + expected
    data = gh('api', '-H', 'Accept: application/octet-stream',
              'repos/'+REPOSITORY+'/releases/assets/'+str(remote['id']), binary=True)
    return hashlib.sha256(data).hexdigest() == expected

def audit_assets(paths):
    def archive(data, depth=0):
        if depth > 3: raise RuntimeError('Release archive nesting limit')
        with zipfile.ZipFile(io.BytesIO(data)) as zipped:
            entries = zipped.infolist()
            if len(entries) > 20000 or sum(entry.file_size for entry in entries) > 256*1024*1024:
                raise RuntimeError('Release archive inspection limit')
            for entry in entries:
                name = entry.filename.lower()
                if name.endswith(('qraftys.png', 'qraftys.zip', '/font/qraftys.json')):
                    raise RuntimeError('Restricted Qraftys artwork/provider in release asset: '+entry.filename)
                if entry.is_dir(): continue
                if name.endswith(('.jar', '.zip')): archive(zipped.read(entry), depth+1)
                elif name.endswith('.png') and entry.file_size <= 1048576:
                    if hashlib.sha256(zipped.read(entry)).hexdigest() == 'b3aa3de3f4a8ecc6f1b71c05f03765b5202178b915df478869b0d74717b7e0df':
                        raise RuntimeError('Restricted Qraftys bitmap in release asset')
    for path in paths:
        if path.suffix.lower() in ('.jar', '.zip'): archive(path.read_bytes())


def prepare_plan(document, historical=False):
    if historical:
        if not isinstance(document, list): raise RuntimeError('Historical migration requires a legacy release list')
        releases = document
    else:
        if not isinstance(document, dict) or document.get('format') != 1: raise RuntimeError('Current releases require a format=1 provenance plan')
        releases = document['releases']
    for release in releases:
        paths = [Path(value) for value in release['assets']]
        if not paths or len({path.name for path in paths}) != len(paths): raise RuntimeError('Missing or duplicate release assets')
        Path(release['notes']).read_text()  # All local inputs must exist before remote mutation.
        hashes = {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in paths}
        audit_assets(paths)
        if not historical:
            artifact = Path(release['artifact'])
            receipt = Path(release['verificationReceipt'])
            identity = verified_source(ROOT, artifact, receipt)
            if release['sourceCommit'] != identity['commit']: raise RuntimeError('Prepared source commit differs from verified source')
            if hashes != release['assetSha256']: raise RuntimeError('Prepared asset hashes differ')
            report = json.loads(receipt.read_text())
            expected = set(report['artifacts'].values()) | {report['sourceArchiveSha256']}
            if not expected.issubset(set(hashes.values())): raise RuntimeError('Release must include verified binary, sources and source archive')
    return releases


def verify_tag(tag, commit=None):
    reference = api('repos/'+REPOSITORY+'/git/ref/tags/'+tag)
    if commit is None: return
    target = reference['object']
    for _ in range(4):
        if target['type'] == 'commit':
            if target['sha'] != commit: raise RuntimeError('Remote release tag differs from verified source: '+tag)
            return
        if target['type'] != 'tag': break
        target = api('repos/'+REPOSITORY+'/git/tags/'+target['sha'])['object']
    raise RuntimeError('Remote tag does not resolve to a verified commit: '+tag)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('--historical-migration', action='store_true', help='Explicit legacy migration; restricted artwork remains forbidden')
    args = parser.parse_args()
    plan = prepare_plan(json.loads(args.plan.read_text()), args.historical_migration)
    # Validate every source tag before creating or uploading anything.
    for release in plan: verify_tag(release['tag'], None if args.historical_migration else release['sourceCommit'])
    (ROOT/'build/publication').mkdir(parents=True, exist_ok=True)
    if not (api('user')['login'] == 'aidentallen'):
        raise RuntimeError('Sign in as the repository owner first')
    reports = []
    for release in plan:
        tag = release['tag']; endpoint = 'repos/'+REPOSITORY+'/releases/tags/'+tag
        # Release creation must reference an already-pushed, recovered source tag.
        verify_tag(tag, None if args.historical_migration else release['sourceCommit'])
        try:
            remote = api(endpoint)
        except subprocess.CalledProcessError as error:
            if b'404' not in error.stderr: raise
            gh('release', 'create', tag, '--repo', REPOSITORY, '--verify-tag',
               '--title', release['title'], '--notes-file', release['notes'], '--draft', '--latest=false')
            remote = api(endpoint)
        existing = {asset['name']: asset for asset in remote['assets']}
        for value in release['assets']:
            path = Path(value)
            if path.name in existing:
                if not (asset_matches(existing[path.name], path)):
                    raise RuntimeError('Existing asset differs: '+tag+'/'+path.name)
            else:
                if not (remote['draft']):
                    raise RuntimeError('Missing asset on already published release: '+tag)
                gh('release', 'upload', tag, str(path), '--repo', REPOSITORY)
                print('Uploaded', tag, path.name, flush=True)
        remote = api(endpoint); existing = {asset['name']: asset for asset in remote['assets']}
        if not (set(existing) == {Path(p).name for p in release['assets']}):
            raise RuntimeError('Unexpected release assets: '+tag)
        for value in release['assets']:
            path = Path(value)
            if not (asset_matches(existing[path.name], path)):
                raise RuntimeError('Remote checksum mismatch: '+tag+'/'+path.name)
        if not (remote['body'].strip() == Path(release['notes']).read_text().strip()):
            raise RuntimeError('Release notes differ: '+tag)
        if remote['draft']:
            gh('release', 'edit', tag, '--repo', REPOSITORY, '--draft=false',
               '--latest='+('true' if release['version']=='0.9.2' else 'false'))
        published = api(endpoint)
        if not (not published['draft']):
            raise RuntimeError('Release verification failed')
        reports.append({'tag':tag,'url':published['html_url'],'assets':[
            {'name':a['name'],'bytes':a['size'],'digest':a.get('digest')} for a in published['assets']]})
        print('Published and verified:', published['html_url'], flush=True)
    (ROOT/'build/publication/published-releases.json').write_text(json.dumps(reports,indent=2)+'\n')

if __name__ == '__main__': main()
