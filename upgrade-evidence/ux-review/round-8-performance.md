# Loop 8 menu performance evidence

Three 30s trials per scene and cap, after a five-second scene warmup. Values below are medians across the three trial statistics; ranges preserve trial variation. Baseline source14d047f; candidate source/class fingerprints are in candidate-source.json. These are sequential runs on a shared desktop, not a randomized controlled hardware study.

| Cap | Scene | Extraction ms before → after | Extraction KiB before → after | Render-thread CPU ms/frame before → after |
|---:|---|---:|---:|---:|
| 60 | idle | 0.199 → 0.242 | 99.799 → 100.556 | 1.307 → 1.401 |
| 60 | appearance | 0.812 → 0.607 | 702.155 → 257.333 | 2.025 → 1.732 |
| 60 | resources | 0.422 → 0.464 | 294.369 → 286.294 | 1.530 → 1.601 |
| 60 | preview | 0.171 → 0.198 | 80.041 → 92.844 | 1.617 → 1.729 |
| 144 | idle | 0.191 → 0.173 | 98.602 → 100.049 | 1.245 → 1.241 |
| 144 | appearance | 0.817 → 0.573 | 701.573 → 243.627 | 1.925 → 1.648 |
| 144 | resources | 0.439 → 0.421 | 282.227 → 286.624 | 1.551 → 1.519 |
| 144 | preview | 0.141 → 0.162 | 77.320 → 94.706 | 1.476 → 1.515 |

The preview workload differs: the baseline child froze its clock, whereas the candidate actually animates. Do not treat preview cost increases as a like-for-like regression. Appearance allocation is the targeted optimization; unrelated scene timing differences may reflect JIT/host variation.

Environment: Java25.0.5, four JVM-visible processors, AMD Radeon RX9070/radeonsi/Mesa26.2.5/OpenGL,1280×960/GUI2, VSync off,240Hz monitor. Explicit test-only60/144 caps override the production Uncapped Themed Menus policy; effective caps are checked per trial. No world was loaded.

The renderFrame elapsed metric includes update/extract/draw/present and limiter waiting, excludes Fabric barriers. Harness intervals include scheduling/barriers and cap. GUI extraction elapsed and render-thread CPU/allocated bytes are separate. This is not GPU timestamp, physical display latency, manual smoothness, live multiplayer or optional-renderer evidence. Frame percentiles are per-trial statistics; their median is not a pooled percentile. GC and system-load values are included in the JSON.

The uncapped baseline/candidate runs are exploratory: the baseline predated the cap wrapper and the candidate Resources scene overlapped a brief parent-side JSON parse. Neither is used in the paired tables. The interrupted pilot-candidate directory is excluded entirely. Baseline compiled main class hashes were verified unchanged across all baseline modes while source edits were prepared separately. Candidate benchmark main source/class fingerprints were checked unchanged immediately after measurement. A later correctness-only change reads the exact native EditBox selection anchor instead of inferring it from repeated text; this affects EditBoxDraft.java and MenuEditBoxMixin.java, with their benchmark versions preserved under benchmark-source-before-selection-fix/. The final full regression build includes that correction. Appearance matching/rows, extraction, rendering and timing hooks are unchanged; measured costs belong to the recorded benchmark snapshot, not a byte-identical final build. The cosmetic probe is disabled in performance runs.

JFR files are native recording artifacts; baseline-allocation-samples.json contains a large export. Allocation samples are statistical weighted attribution, not exact per-method totals. Allocation reductions in the paired table come from ThreadMXBean counters measured around extraction, not JFR sample weights.
