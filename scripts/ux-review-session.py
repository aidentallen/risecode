#!/usr/bin/env python3
"""Send a synthetic native input action to the disposable UX review client; no personal instance access."""
import argparse,json,os,time
from pathlib import Path

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dir',type=Path,default=Path('/tmp/riserecode-ux-round10/live'))
parser.add_argument('command',help='state, or a JSON action object (see scripts/ux-review-session.md)')
args=parser.parse_args()
state_path=args.dir/'state.json'
if args.command=='state':
    print(state_path.read_text());raise SystemExit(0)
state=json.loads(state_path.read_text())
if state['phase']=='finished':raise SystemExit('Review client has finished')
command=json.loads(args.command)
sequence=state['sequence']+1
file=args.dir/'commands'/f'{sequence:06}.json'
pending=file.with_suffix('.pending')
with pending.open('x') as stream:json.dump(command,stream)
os.link(pending,file)
deadline=time.monotonic()+45
while time.monotonic()<deadline:
    updated=json.loads(state_path.read_text())
    if updated['sequence']>=sequence:
        print(json.dumps(updated,indent=2));break
    time.sleep(.1)
else:raise SystemExit('Client did not answer within45s; inspect its log, do not enqueue more commands')
