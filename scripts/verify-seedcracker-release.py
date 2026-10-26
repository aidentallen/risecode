#!/usr/bin/env python3
"""Validate one packaged 0.28.0 artifact in disposable native clients, sequentially."""
from pathlib import Path
import hashlib,json,os,re,subprocess,time
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'.tooling/seedcracker'
OUT.mkdir(parents=True,exist_ok=True)
artifact=ROOT/'build/libs/riserecode-0.28.0.jar'
expected=hashlib.sha256(artifact.read_bytes()).hexdigest()
assert 'BUILD SUCCESSFUL' in (OUT/'final-check.log').read_text()
(OUT/'final-check-receipt.json').write_text(json.dumps({'exitCode':0,'sha256':expected},indent=2)+'\n')
env=dict(os.environ,GRADLE_USER_HOME=str(ROOT/'.tooling/gradle-home'))
for label,options in [('vanilla',['-PseedCrackerRecovery']),('sodium',['-PrendererStack','-PwithoutIris']),('iris',['-PrendererStack','-PwithShaderPack']),('vulkan',['-PrendererStack','-PwithoutIris','-PgraphicsBackend=VULKAN'])]:
    directory=ROOT/('build/run/seedcracker-final-'+label)
    start=time.time()
    command=[str(ROOT/'gradlew'),'runClientGameTest','-I',str(ROOT/'scripts/panorama-packaged-tests.gradle'),'-PseedCrackerOnly','-PbundledFontsOnly','-PclientTestDirectory='+str(directory),'--offline','--no-daemon']+options
    with (OUT/('final-'+label+'.log')).open('w') as log:
        code=subprocess.run(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT).returncode
    actual=hashlib.sha256(artifact.read_bytes()).hexdigest()
    assert actual==expected,'Release artifact changed during renderer matrix'
    log=(OUT/('final-'+label+'.log')).read_text()
    checks=re.search(r'SeedCracker native checks passed \((\d+) assertions\)',log)
    result={'label':label,'exitCode':code,'seconds':time.time()-start,'sha256':actual,'assertions':int(checks.group(1)) if checks else None,'runDirectory':str(directory),'fullNativePlacementRecovery':label=='vanilla','realGeneratedEndRecovery':label=='vanilla'}
    (OUT/('final-'+label+'-receipt.json')).write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result),flush=True)
    if code or not checks or 'BUILD SUCCESSFUL' not in log: raise SystemExit(code or 1)
print('SeedCracker packaged renderer matrix passed',flush=True)
