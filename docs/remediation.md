# Persistence and rendering remediation

Target remains Minecraft 26.2 / Java 25. This change preserves configuration schemas,
numeric UI ranges and enabled selections. The pre-existing working tree was preserved.

## Earlier persistence and rendering implementation

- Config workers exclusively own deep JSON snapshots. Save bookkeeping and save/profile-load
  success and failure completion run on the client executor. Missing/corrupt/unsupported
  profiles complete `false` on that executor. Revision checks preserve edits made in flight.
- Chunk animation policy accepts the extended setting boundaries already exposed by the UI.
- GUI preview textures remain pinned through their extraction and draw. Discard retires them
  until the next extraction; unique upload IDs protect same-frame discard/reopen. Shutdown
  releases the remaining textures. Held item, map, crosshair and screen context hooks restore
  their previous state with `try/finally`, including cancellation and renderer exceptions.
- BlockESP stores one 4096-bit mask per matching section, plus bounded nearest results.
  Scanning, nearest selection/heap draining, result materialization and container discovery
  are incremental. They share a 1 ms tick deadline and have explicit work caps; individual
  engine calls, allocation/GC and OS scheduling cannot be preempted by that deadline.
  A query in progress finishes for its captured position before a newer movement query runs.
  Chunk unloads invalidate pending queries; dimension changes discard all index state.
- Cosmetic primitives and GLB indices/material metadata are cached. Companion skinning runs
  once per animation tick and walking state; trails publish one immutable tick snapshot.
  Nametag text, equipment render states and line-of-sight checks refresh at tick cadence.
  HUD ArrayList and supported appearance references are cached. Contiguous atlas glyphs
  share one immutable extraction state with one pose/clip/bounds calculation per run.
- Silhouette collection uses a concrete, compiler-checked adapter. Optional chunk renderer
  methods, fields and injection call sites are checked from class bytes. Packed terrain
  shaders are enabled only for the verified Sodium 0.9.2+mc26.2 and Iris 1.11.4+mc26.2
  versions; unknown layouts retain native terrain/water behavior and keep user choices stored. Iris shader patches
  require all translation/region/fade anchors (including Iris's native fade-disabled branch)
  and reject partial, ambiguous or duplicate transformations before enabling motion.
  Nametag panels use the existing protected world-overlay target so Iris's final pass
  does not discard their backgrounds; the native depth and sort policy is retained.

## Earlier verification method (superseded for performance claims)

The new headless checks gate the real configuration writer, exercise client-executor
success/failure paths, scan dense common-block selections, enforce query work caps, move
queries, unload chunks, cancel dimension-stale work, retire/reopen textures, and reject
missing/duplicate shader anchors. Existing checks remain part of `./gradlew check build`.

The disposable native harness adds cancellation/throwing renderer probes and a nested
screen-context probe, five JFR scenes, five enable/disable + resource-reload + resize
cycles, screenshots, heap observations and global AMD VRAM observations.

Scene conditions: 2048x1152, render distance 2, VSync off, AFK limiter disabled in the
fixture, at least eight seconds and 600 frames per scene. `render*Ms` measures the native
`Minecraft.renderFrame` update/extract/draw/present interval. The separate cadence
statistics include Fabric test synchronization barriers and are diagnostic only. The populated-player scene uses
24 synthetic `RemotePlayer` instances with equipment and silhouette outlines; it does not
establish live multiplayer/network performance. JFR `profile` allocation samples are weighted
estimates, not exact object counts. Global device VRAM includes the desktop and other apps;
owned silhouette target counts are tracked separately. One run per configuration is a
smoke/performance comparison, not a statistically controlled hardware benchmark.

Raw logs, frame distributions, JFR recordings, screenshots, source baseline and the
before/after patch remain in ignored `upgrade-evidence/remediation/` and `.tooling/remediation/`.
The `initial-fixture/`, `afk-invalid/` and `cadence-only/` captures are retained for audit only and must not
be used for performance conclusions.

Commands:

```sh
python3 scripts/fetch-dependencies.py
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check build --offline --no-daemon
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew runClientGameTest \
  -PremediationOnly -PchunkAnimationsOnly -PresourcesOffline \
  -PremediationEvidence=upgrade-evidence/remediation/vanilla --offline --no-daemon
# Sodium: add -PrendererStack -PwithoutIris
# Sodium/Iris: add -PrendererStack -PwithShaderPack
# Sodium Vulkan: add -PrendererStack -PwithoutIris -PgraphicsBackend=VULKAN
java scripts/analyze-remediation.java upgrade-evidence/remediation/vanilla/*.jfr
```

## Corrected performance measurement (0.24.0)

The earlier allocation percentages and before/after table have been withdrawn. They used
short, single-run JFR weighted allocation samples. Those weights are useful for locating
allocation sites but do not establish exact render-thread bytes per frame.

The corrected fixture warms each of six scenes for ten seconds, then records thirty seconds.
Three alternating baseline/candidate repetitions use the preserved 0.23.0 production JAR
and the candidate production JAR with the same test instrumentation. Both use mode Off.
`ThreadMXBean` counters directly measure bytes allocated and CPU nanoseconds on the render
thread within `Minecraft.renderFrame`; Fabric barriers between calls are excluded. JFR
captures allocation sites, execution samples and GC events separately from startup.

Scenes cover idle HUD, 24 synthetic equipped players, common-stone ESP, cosmetics, resource
scrolling and movement with map recording. Synthetic players are local `RemotePlayer`
fixtures, not connected multiplayer clients. The target is at least 20% fewer allocated
bytes/frame in the synthetic-player and common-block ESP scenes. Any repeated p95 rendering
regression above 5% requires investigation before installation.

Raw evidence is in ignored `upgrade-evidence/lightweight/`; source and JAR preservation,
logs and installation receipts are in `.tooling/lightweight/`. Exploratory captures that
ran alongside compilation are excluded from comparisons. Use
`python3 scripts/compare-performance.py upgrade-evidence/lightweight/controlled`
to regenerate the comparison after all six controlled runs finish.

## Validated direct-counter results (0.24.0)

These are the medians of three alternating baseline/candidate repetitions on this machine,
with **Performance Off**, using vanilla OpenGL at 2048×1152 and render distance 2. Every scene used a ten-second warmup and thirty-second measurement.
The source and 0.23.0 baseline JAR were preserved before implementation. Baseline and
candidate use the same measurement fixture; candidate-only control and lifecycle checks run
separately. Startup, early diagnostic runs with unequal fixture histories, and the
`diagnostic-before-control-isolation` captures are excluded.

| Scene | Allocation KiB/frame before / after | Reduction | Render median ms before / after | Render p95 ms before / after | CPU ms/frame before / after |
| --- | ---: | ---: | ---: | ---: | ---: |
| idle-hud | 79.7 / 71.9 | 9.8% | 0.401 / 0.372 | 0.722 / 0.691 | 0.408 / 0.380 |
| synthetic-24-players | 2001.9 / 1346.5 | 32.7% | 2.968 / 2.673 | 4.658 / 4.335 | 3.061 / 2.769 |
| common-stone-esp | 1180.7 / 880.8 | 25.4% | 1.078 / 0.937 | 1.779 / 1.543 | 1.105 / 0.963 |
| cosmetics | 384.6 / 372.4 | 3.2% | 0.995 / 0.902 | 1.672 / 1.532 | 0.953 / 0.866 |
| resource-grid-scrolling | 605.9 / 596.3 | 1.6% | 1.425 / 1.355 | 2.184 / 2.075 | 1.380 / 1.299 |
| movement-map-recording | 141.2 / 134.2 | 4.9% | 0.640 / 0.617 | 1.094 / 1.048 | 0.597 / 0.575 |

Both required allocation targets passed: **32.7%** less per frame with 24 synthetic players,
and **25.4%** less with common-block ESP. This does not measure live multiplayer. CPU values
are direct render-thread CPU time per frame, rather than whole-process utilization or FPS.
Median render p95 improved in every scene. Across paired repetitions, the movement scene
had one +6.1% p95 result, followed by -4.6% and -14.6%; its baseline p95 range was
1.070–1.227 ms and candidate range 1.043–1.135 ms. The distributions overlap, CPU/frame was
lower in all three pairs, and the >5% regression did not reproduce. Cosmetics had one +0.2%
p95 pair; every other paired scene result improved. No reproducible >5% regression was found.

JFR allocation samples identify sites, not exact byte totals: baseline ESP samples point to
per-edge `Vec3`, line records, shape iteration and line submission; candidate samples shift
to packed array construction and partitioning. Player-scene execution samples cover label
rendering and silhouette submission; cached ordered text and batched silhouettes reduce
that work. Steady-state allocation remains in native terrain hooks, glyph meshes and
cosmetic animation arrays. The analyzer uses each recording's start boundary, excludes the
first weighted allocation sample on each thread, and reports execution samples and GC
pauses. Weighted sample percentages are not used for acceptance.

GC MXBean observations during each thirty-second measurement are separate from allocation
and CPU counters. Median young-collection count / collector milliseconds:

| Scene | Baseline | Candidate |
| --- | ---: | ---: |
| Idle HUD | 25 / 61 | 21 / 59 |
| Synthetic players | 83 / 151 | 56 / 116 |
| Common-block ESP | 137 / 177 | 78 / 111 |
| Cosmetics | 49 / 78 | 34 / 60 |
| Resource scrolling | 54 / 82 | 38 / 67 |
| Movement/map | 28 / 54 | 19 / 42 |

Concurrent-collector time is not added to pause time. End-of-scene heap occupancy varies
with collection timing and does not establish retained heap savings; long-session post-GC
observations and owned resource payloads are reported separately below. The JFR site and
execution report is `.tooling/lightweight/jfr-analysis.txt`, and direct counters, individual
repetitions and comparisons are in `upgrade-evidence/lightweight/controlled/`.

## 0.24.0 controls and resource ownership

The footer settings popup opens Performance, with Off, Conservative, Balanced, Strong and
Custom. Independent edits copy the effective preset to Custom and preserve its last values
across preset switches. Preferences are additive global `ui.performance` data. Reductions
never rewrite profiles, module selections, equipped cosmetics, map recording choices,
imported geometry, capes, outline resolution or target limits. The immutable policy is
published on the client and captured once per extraction.

Class-based module lookups preserve first-match behavior. HUD metrics cache content,
settings/appearance revisions, font generation and scale while animation/color sampling
continues. Nametag visibility bookkeeping is reused; immutable equipment snapshots rebuild
on changed equipment, resources or appearance, with dynamic items still refreshed each tick.
Packed immutable ESP snapshots retain lazily materialized line/quad inspection order and
partition depth behavior once. Silhouette quads batch by render type, pipeline keys are
typed, and targets retire at a frame boundary after forty unused client ticks.

Map, preview and cosmetic caches track entries, decoded CPU payload, native images and
owned GPU texture payload separately. Extraction pins retain retired resources until their
pending draw is safe; unique texture identifiers prevent stale completion/reupload aliasing.
Active cosmetics and visible previews form a reported working-set floor. Unused assets are
evicted first. Background completion queues apply backpressure and shutdown drains pending
acknowledgements; terrain, waypoint and death-marker persistence remain enabled according
to existing choices. Independent normal/reduced budgets are listed in
[the release notes](releases/v0.24.0.md#independent-work-and-cache-budgets).

## 0.24.0 packaged native validation

`check build compileGametestJava` passed, including **304 new performance assertions** and
all existing headless suites. Native validation used the frozen production JAR, with fault
probes and synthetic fixtures confined to the separate test mod. Controls were exercised
in every mode, including transformed popup hit testing, keyboard focus/navigation, resizing,
resource reloads, preset/profile preservation, dynamic equipment changes, stale async asset
completion, extraction pinning, cache-floor overages, chunk unload and dimension roundtrips.
The oversized preview-floor probe changes accounting weights only, without allocating
oversized native images or GPU textures.

| Runtime | Performance controls | Lifecycle/scene assertions | Terrain assertions | Reload/resize/enable cycles |
| --- | ---: | ---: | ---: | ---: |
| Vanilla OpenGL | 52 | 61 | 21 | 20 |
| Sodium 0.9.2 / Iris 1.11.4 OpenGL + Complementary Unbound r5.9.3 | 52 | 62 | 28 | 20 |
| Sodium 0.9.2 Vulkan | 52 | 61 | 21 | 20 |
| Sodium 0.9.2 OpenGL, plus 30-minute soak | 52 | 121 | 21 | 20 |

Each cycle rendered an actual silhouette and asserted its framebuffer dimensions before
disabling the modules. Every sampled pool held one target. Separate idle checks then
confirmed release after forty unused client ticks. Cycle heap readings, taken twenty ticks
after `System.gc()`, include intervening allocation and must not be read as retained heap:

| Runtime | Cycle heap MiB min–max | First / last MiB |
| --- | ---: | ---: |
| Vanilla OpenGL | 366.2–424.4 | 368.4 / 424.4 |
| Sodium 0.9.2 / Iris 1.11.4 OpenGL + Complementary Unbound r5.9.3 | 414.5–454.3 | 422.2 / 433.9 |
| Sodium 0.9.2 Vulkan | 355.4–446.6 | 355.4 / 440.6 |
| Sodium 0.9.2 OpenGL, plus 30-minute soak | 360.2–431.4 | 360.2 / 419.0 |

The primary long session ran Sodium OpenGL for thirty wall-clock minutes after the twenty
cycles. It kept moving map recording, a bee companion and a visible outlined player active,
periodically scrolled resources, and rotated Off, Conservative, Balanced, Strong, Custom,
then Off. Thirty immediate post-GC samples recorded **270.6–274.0 MiB** (first 272.4, last 270.8; first-five median 272.6, last-five 270.8).
This finite window establishes the observed range, not a general retained-heap reduction.
All map pending/worker-queue and cosmetic in-flight bound assertions passed (at most sixteen).
Sampled map completion queue range was [0, 1].

Owned payload ranges during that session are separate from heap and whole-device VRAM:

| Owner | Decoded CPU MiB | Owned native-image MiB | Owned GPU texture MiB |
| --- | ---: | ---: | ---: |
| Map | 1.367–1.395 | 0.912–0.992 | 0.912–0.992 |
| Cosmetics | 0.471–0.471 | 1.000–1.000 | 1.000–1.000 |
| Resource previews | 0.000–0.000 | 0.000–1.483 | 0.000–1.483 |

Silhouette target GPU payload range was 16.479–16.479 MiB while active;
idle release is covered separately. These counters describe resources owned by the client,
not backend/driver overhead or global VRAM. Native image backing and GPU storage are
separate allocations; decoded CPU model/tile payload is not a JVM object-size estimate.
All sampled map GPU, cosmetic entry/byte and preview overages were zero.

The first two Vulkan attempts stalled during Fabric test-context disconnect after returning
from the dimension roundtrip. Thread dumps show the render thread in
`IntegratedServer.halt -> executeBlocking`, while server/test threads waited on Fabric's
phase barriers. Only those disposable test clients were stopped. The test fixture now lets
chunk rendering and ten ticks settle, then allows the next server tick before context
close; all assertions remain. The final Vulkan and Sodium runs passed with this test-only
synchronization change. The frozen production JAR was unchanged. Diagnostic logs and thread
dumps remain under `.tooling/lightweight/` and `diagnostic-vulkan-teardown*` evidence folders.

Representative screenshots were checked for controls, equipment, panels, silhouettes,
cosmetics and terrain on all four runtimes. Fake-auth Realms warnings and the X11 cursor
warning at exit were nonfatal. The tested Iris pack emitted existing custom-pipeline
warnings. Other packs, hardware and live multiplayer remain separate validation gaps.

## Verified 0.24.0 installation (superseded by 0.24.1)

The following records the 0.24.0 installation and its benchmark artifact. The profile now
uses [0.24.1](movement-crash.md#installed-0241-artifact), which fixes a separate camera error.

Installed production release: `riserecode-0.24.0.jar`, SHA-256
`a20bea2dc90bc1d1883e319f8995781ce3b5f9ba439fcfb568a866e3cbcbe491`.
All ten controlled/native run receipts identify that same frozen candidate checksum.
The Fabric 26.2 profile contains exactly one mod with metadata ID `riserecode`.
The previous 0.23.0 JAR was checksum-verified and backed up outside `mods/` at
`.riserecode-backups/lightweight-20261021T231613Z/riserecode-0.23.0.jar` inside that profile.

Installation preserved all **48 unrelated mod files** and
**24,707 configuration/map files** by checksum. No concurrent terrain
changes occurred during the final inventories. The installer wrote no configuration or
terrain data and did not stop the user's game. A restart was required to load 0.24.0.
The complete local installation and backup receipt is
`.tooling/lightweight/install-receipt.json`.

## Historical 0.23.0 native validation on this machine

Final `check build compileGametestJava` passed, including 26 gated persistence assertions,
479 packed-index/cache/shader assertions and 89 chunk-policy assertions. Existing suites
also passed. The release contains production classes only; the fault probes are test fixtures.

| Runtime | Lifetime/scene checks | Real terrain checks | Reload/resize cycles |
| --- | ---: | ---: | ---: |
| Vanilla OpenGL | 24 | 21 | 5 |
| Sodium OpenGL 0.9.2+mc26.2 | 24 | 21 | 5 |
| Sodium/Iris OpenGL 1.11.4+mc26.2, Complementary Unbound r5.9.3 | 25 | 28 | 5 |
| Sodium Vulkan 0.9.2+mc26.2 | 24 | 21 | 5 |

The final Iris and Sodium lifetime/scene checks and terrain checks ran separately. An earlier combined
run completed the lifetime checks but its following terrain fixture tried to fill an unloaded
chunk. The fixture now explicitly loads its disposable server chunk, teleports and waits for chunk data before placing its blocks, then
waits for the client block update and compiled mesh. Assertions and original coordinates
are retained. The temporary server load ticket is released after placement. Other exploratory failures remain in the raw logs.

Across the five cycles, post-GC heap observations were 317–343 MB (vanilla), 438–451 MB
(Iris), 329–354 MB (Sodium), and 323–355 MB (Vulkan), with no monotonic heap rise. The owned silhouette pool
remained one target at every observation and matched each resized window. Global device
VRAM varied with the desktop: vanilla 13.37–13.52 GB, Iris 14.53–14.62 GB, Vulkan
13.67–13.68 GB; Sodium 13.51–14.28 GB, leveling near 14.24 GB after the first reload. These short observations show bounded owned targets and no sustained
heap growth during the tested cycles; they do not prove long-session or per-process VRAM
stability. Iris emitted nonfatal unmapped custom-pipeline warnings; screenshots were checked
for visible panels, equipment, silhouettes, terrain and UI. Other shader packs and live
multiplayer remain untested.

JFR captures have environment, system-property and process events removed. Use
`java scripts/analyze-remediation.java --json <recordings...>` for weighted totals by recording;
`allocations.jsonl` contains the comparable baseline/vanilla totals. The capture itself also
records frame distributions in `frames.jsonl` and cycle observations in `cycles.jsonl`.

## Historical 0.23.0 installed artifact

Installed release: `riserecode-0.23.0.jar`, SHA-256
`c40cf1cfcc4d6b7aef94ded37f72c25dfb695113c00e6375db6df940a1f52bc3`.
The matching previous JAR is backed up outside `mods/`; exactly one `riserecode` mod is
installed. The final installation preserved all 46 unrelated mod files and all 23,712
configuration/map files by checksum. An earlier attempt rolled back when active map
recording changed tiles during inventory; the final attempt observed no tile changes.
The installer never writes configuration or terrain data and did not stop the user's game.
Minecraft must restart to load the replacement. The local receipt and backup path are in
`.tooling/remediation/install-receipt.json`.
