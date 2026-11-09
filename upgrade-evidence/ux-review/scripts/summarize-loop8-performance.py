from pathlib import Path
import json, statistics, hashlib

root=Path('/tmp/riserecode-ux-round8/performance')
metrics=['extractionMedianMs','extractionP95Ms','extractionP99Ms','extractionBytesPerCall','allocatedBytesPerFrame','cpuMsPerFrame','renderWorkMedianMs','renderWorkP95Ms','renderWorkP99Ms','harnessIntervalMedianMs','harnessIntervalP95Ms','harnessIntervalP99Ms','gcCollections','gcMillis','systemLoadAverage']
summary={'method':'median and min/max of three independent 30s trial statistics, not pooled frame percentiles','caps':{}}
for cap in (60,144):
    groups={}
    for name in ('baseline','candidate'):
        label=f'{name}{cap}'
        rows=[json.loads(line) for line in (root/f'{label}-trials.jsonl').read_text().splitlines()]
        assert len(rows)==12,(label,len(rows))
        assert all(row['effectiveLimit']==cap for row in rows)
        groups[name]={}
        for scene in ('idle','appearance','resources','preview'):
            values=[row for row in rows if row['scene']==scene]
            assert len(values)==3
            groups[name][scene]={key:{'median':statistics.median(row[key] for row in values),'min':min(row[key] for row in values),'max':max(row[key] for row in values)} for key in metrics}
            groups[name][scene]['frames']=sum(row['frames'] for row in values)
            groups[name][scene]['seconds']=sum(row['seconds'] for row in values)
    groups['changePercent']={scene:{key:100*(groups['candidate'][scene][key]['median']/groups['baseline'][scene][key]['median']-1) if groups['baseline'][scene][key]['median'] else None for key in metrics} for scene in ('idle','appearance','resources','preview')}
    summary['caps'][str(cap)]=groups
(root/'paired-summary-regenerated.json').write_text(json.dumps(summary,indent=2)+'\n')
lines=['# Loop 8 menu performance evidence','','Three 30s trials per scene and cap, after a five-second scene warmup. Values below are medians across the three trial statistics; ranges preserve trial variation. Baseline source14d047f; candidate source/class fingerprints are in candidate-source.json. These are sequential runs on a shared desktop, not a randomized controlled hardware study.','','| Cap | Scene | Extraction ms before → after | Extraction KiB before → after | Render-thread CPU ms/frame before → after |','|---:|---|---:|---:|---:|']
for cap,groups in summary['caps'].items():
    for scene in ('idle','appearance','resources','preview'):
        def pair(key,scale=1):
            return ' → '.join(f"{groups[k][scene][key]['median']/scale:.3f}" for k in ('baseline','candidate'))
        lines.append(f'| {cap} | {scene} | {pair("extractionMedianMs")} | {pair("extractionBytesPerCall",1024)} | {pair("cpuMsPerFrame")} |')
lines+=['','The preview workload differs: the baseline child froze its clock, whereas the candidate actually animates. Do not treat preview cost increases as a like-for-like regression. Appearance allocation is the targeted optimization; unrelated scene timing differences may reflect JIT/host variation.','','Environment: Java25.0.5, four JVM-visible processors, AMD Radeon RX9070/radeonsi/Mesa26.2.5/OpenGL,1280×960/GUI2, VSync off,240Hz monitor. Explicit test-only60/144 caps override the production Uncapped Themed Menus policy; effective caps are checked per trial. No world was loaded.','','The renderFrame elapsed metric includes update/extract/draw/present and limiter waiting, excludes Fabric barriers. Harness intervals include scheduling/barriers and cap. GUI extraction elapsed and render-thread CPU/allocated bytes are separate. This is not GPU timestamp, physical display latency, manual smoothness, live multiplayer or optional-renderer evidence. Frame percentiles are per-trial statistics; their median is not a pooled percentile. GC and system-load values are included in the JSON.','','The uncapped baseline/candidate runs are exploratory: the baseline predated the cap wrapper and the candidate Resources scene overlapped a brief parent-side JSON parse. Neither is used in the paired tables. The interrupted pilot-candidate directory is excluded entirely. Baseline compiled main class hashes were verified unchanged across all baseline modes while source edits were prepared separately. Candidate benchmark main source/class fingerprints were checked unchanged immediately after measurement. A later correctness-only change reads the exact native EditBox selection anchor instead of inferring it from repeated text; this affects EditBoxDraft.java and MenuEditBoxMixin.java, with their benchmark versions preserved under benchmark-source-before-selection-fix/. The final full regression build includes that correction. Appearance matching/rows, extraction, rendering and timing hooks are unchanged; measured costs belong to the recorded benchmark snapshot, not a byte-identical final build. The cosmetic probe is disabled in performance runs.','','JFR files are native recording artifacts; baseline-allocation-samples.json contains a large export. Allocation samples are statistical weighted attribution, not exact per-method totals. Allocation reductions in the paired table come from ThreadMXBean counters measured around extraction, not JFR sample weights.']
(root/'report-regenerated.md').write_text('\n'.join(lines)+'\n')
print('\n'.join(lines[:15]))
