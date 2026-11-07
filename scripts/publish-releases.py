#!/usr/bin/env python3
"""Publish a prepared release plan using authenticated gh, verifying asset hashes."""
from pathlib import Path
import hashlib
import json
import subprocess
import sys

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

def main():
    if len(sys.argv) != 2:
        raise SystemExit('Usage: python3 scripts/publish-releases.py /path/to/prepared-plan.json')
    plan = json.loads(Path(sys.argv[1]).read_text())
    if not (api('user')['login'] == 'aidentallen'):
        raise RuntimeError('Sign in as the repository owner first')
    reports = []
    for release in plan:
        tag = release['tag']; endpoint = 'repos/'+REPOSITORY+'/releases/tags/'+tag
        # Release creation must reference an already-pushed, recovered source tag.
        api('repos/'+REPOSITORY+'/git/ref/tags/'+tag)
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
