#!/usr/bin/env python3
"""Run fresh checks and bind their exit status/log/artifacts to exact source bytes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
from release_provenance import source_identity as identify_source

ROOT = Path(__file__).resolve().parent.parent


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_identity():
    return identify_source(ROOT)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline', action='store_true', help='Use an existing Gradle dependency cache')
    parser.add_argument('--package', action='store_true', help='Package only a clean, freshly checked committed source tree')
    args = parser.parse_args()
    before = source_identity()
    if args.package and before['dirty']: raise SystemExit('Commit source changes before packaging an immutable release')
    output = ROOT / 'build/verification'
    output.mkdir(parents=True, exist_ok=True)
    receipt = output / 'project.json'
    receipt.unlink(missing_ok=True)
    env = dict(os.environ, GRADLE_USER_HOME=str(ROOT / '.tooling/gradle-home'))
    tasks = []
    for name, command in [('dependencies', ['python3', 'scripts/fetch-dependencies.py']),
                          ('check-build', ['./gradlew', 'check', 'build', '--rerun-tasks', '--no-daemon', *(['--offline'] if args.offline else [])])]:
        start = time.monotonic()
        log = output / (name + '.log')
        with log.open('w') as stream:
            result = subprocess.run(command, cwd=ROOT, env=env, stdout=stream, stderr=subprocess.STDOUT)
        text = log.read_text()
        tasks.append(dict(name=name, command=command, exitCode=result.returncode, seconds=time.monotonic()-start,
                          logSha256=sha(log), suites=[dict(name=m[0].strip(), assertions=int(m[1])) for m in re.findall(r'([^\n]+?) passed \((\d+) assertions\)', text)]))
        if result.returncode: raise SystemExit(f'{name} failed: {log}')
    after = source_identity()
    if before != after: raise SystemExit('Source changed during verification; receipt refused')
    properties = dict(line.split('=', 1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line and not line.lstrip().startswith('#'))
    basename = properties['archives_base_name'] + '-' + properties['mod_version']
    built = [ROOT/'build/libs'/(basename+suffix) for suffix in ('.jar', '-sources.jar')]
    if any(not path.is_file() for path in built): raise SystemExit('Current release JAR or sources JAR missing')
    artifacts = {str(path.relative_to(ROOT)): sha(path) for path in built}
    if not artifacts: raise SystemExit('No release artifacts built')
    report = dict(format=1, source=before, dependenciesSha256=sha(ROOT/'dependencies/manifest.json'),
                  java=subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT, text=True).strip(),
                  tasks=tasks, artifacts=artifacts, nativeRuntime='not executed by this command')
    if args.package:
        target = output / 'source.zip'
        subprocess.run(['git', 'archive', '--format=zip', '--prefix=risecode/', '--output='+str(target), before['commit']], cwd=ROOT, check=True)
        report['sourceArchiveSha256'] = sha(target)
    receipt.write_text(json.dumps(report, indent=2)+'\n')
    print(receipt)


if __name__ == '__main__': main()
