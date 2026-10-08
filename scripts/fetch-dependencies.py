#!/usr/bin/env python3
"""Fetch verified, pinned optional API dependencies without bundling them."""
from pathlib import Path
import hashlib
import json
import os
import tempfile
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent.parent
DESTINATION = ROOT / 'dependencies'
MANIFEST = DESTINATION / 'manifest.json'

def digest(data):
    return hashlib.sha256(data).hexdigest()

def install(entry, data):
    if digest(data) != entry['sha256']:
        raise RuntimeError('Dependency checksum mismatch: ' + entry['file'])
    with tempfile.NamedTemporaryFile(dir=DESTINATION, delete=False) as output:
        temporary = Path(output.name)
        output.write(data)
    try:
        os.replace(temporary, DESTINATION / entry['file'])
    finally:
        temporary.unlink(missing_ok=True)

def main():
    DESTINATION.mkdir(exist_ok=True)
    for entry in json.loads(MANIFEST.read_text())['dependencies']:
        target = DESTINATION / entry['file']
        if target.is_file() and digest(target.read_bytes()) == entry['sha256']:
            print('Verified:', entry['file'])
            continue
        if 'url' in entry:
            request = urllib.request.Request(entry['url'], headers={'User-Agent': 'risecode-build/0.9.2'})
            with urllib.request.urlopen(request, timeout=120) as response:
                data = response.read()
        else:
            with zipfile.ZipFile(DESTINATION / entry['archive']) as archive:
                data = archive.read(entry['member'])
        install(entry, data)
        print('Fetched and verified:', entry['file'])

if __name__ == '__main__':
    main()
