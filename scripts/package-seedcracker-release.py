#!/usr/bin/env python3
"""Package the staged, verified 0.28.0 source and distributables; never publishes."""
from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'release/0.28.0'
OUT.mkdir(parents=True,exist_ok=True)
artifact=ROOT/'build/libs/riserecode-0.28.0.jar'
expected=hashlib.sha256(artifact.read_bytes()).hexdigest()
for label in ['vanilla','sodium','iris','vulkan']:
    receipt=json.loads((ROOT/'.tooling/seedcracker'/('final-'+label+'-receipt.json')).read_text())
    assert receipt['exitCode']==0 and receipt['sha256']==expected,(label,receipt)
assert 'BUILD SUCCESSFUL' in (ROOT/'.tooling/seedcracker/final-check.log').read_text()
for name in ['riserecode-0.28.0.jar','riserecode-0.28.0-sources.jar']:
    shutil.copy2(ROOT/'build/libs'/name,OUT/name)
tree=subprocess.check_output(['git','write-tree'],cwd=ROOT,text=True).strip()
subprocess.run(['git','archive','--format=zip','--prefix=risecode-0.28.0/','--output='+str(OUT/'riserecode-0.28.0-source.zip'),tree],cwd=ROOT,check=True)
with zipfile.ZipFile(artifact) as source,zipfile.ZipFile(OUT/'riserecode-0.28.0-licenses.zip','w',zipfile.ZIP_DEFLATED) as output:
    for name in sorted(source.namelist()):
        if name.endswith('/') or not (name.startswith('licenses/') or name.endswith('CREDITS.txt') or name.endswith('-LICENSE.txt')):continue
        info=zipfile.ZipInfo(name,(2026,10,26,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
        output.writestr(info,source.read(name))
assets=[OUT/name for name in ['riserecode-0.28.0.jar','riserecode-0.28.0-sources.jar','riserecode-0.28.0-source.zip','riserecode-0.28.0-licenses.zip']]
(OUT/'SHA256SUMS').write_text(''.join(hashlib.sha256(path.read_bytes()).hexdigest()+'  '+path.name+'\n' for path in assets))
(ROOT/'.tooling/seedcracker/package-receipt.json').write_text(json.dumps({'sourceTree':tree,'releaseSha256':expected,'assets':{path.name:hashlib.sha256(path.read_bytes()).hexdigest() for path in assets+[OUT/'SHA256SUMS']}},indent=2)+'\n')
print('Packaged verified 0.28.0 source tree '+tree+' in '+str(OUT))
