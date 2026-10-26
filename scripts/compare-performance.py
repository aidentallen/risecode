#!/usr/bin/env python3
"""Compare direct render-thread counters from three alternating benchmark pairs."""
import argparse,json,statistics
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('directory',type=Path);a=p.parse_args()
records={}
for kind in ('baseline','candidate'):
 for run in range(1,4):
  path=a.directory/f'{kind}-{run}'/'frames.jsonl'
  for row in map(json.loads,path.read_text().splitlines()):
   assert row['warmupSeconds']>=10 and row['measurementSeconds']>=30,(path,row)
   assert row['renderFrames']>0 and row['throttleReason']=='NONE',(path,row)
   records.setdefault(row['scene'],{}).setdefault(kind,[]).append(row)
summary={}
print('| Scene | Allocation KiB/frame before / after | Reduction | Render median ms before / after | Render p95 ms before / after | CPU ms/frame before / after |')
print('| --- | ---: | ---: | ---: | ---: | ---: |')
for scene,kinds in records.items():
 for values in kinds.values(): assert len(values)==3,(scene,len(values))
 def m(kind,key):return statistics.median(r[key] for r in kinds[kind])
 keys=('renderAllocatedBytesPerFrame','renderMedianMs','renderP95Ms','renderCpuMsPerFrame')
 pairs=[(m('baseline',key),m('candidate',key)) for key in keys]
 reduction=(1-pairs[0][1]/pairs[0][0])*100
 print(f'| {scene} | {pairs[0][0]/1024:.1f} / {pairs[0][1]/1024:.1f} | {reduction:.1f}% | {pairs[1][0]:.3f} / {pairs[1][1]:.3f} | {pairs[2][0]:.3f} / {pairs[2][1]:.3f} | {pairs[3][0]:.3f} / {pairs[3][1]:.3f} |')
 summary[scene]={'medians':dict(zip(keys,pairs)),'allocationReductionPercent':reduction,'p95ChangePercent':(pairs[2][1]/pairs[2][0]-1)*100,'allocationTargetMet':reduction>=20 if scene in ('synthetic-24-players','common-stone-esp') else None,'runs':kinds}
(a.directory/'comparison.json').write_text(json.dumps(summary,indent=2)+'\n')
