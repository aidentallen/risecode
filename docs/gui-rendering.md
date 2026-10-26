# GUI rendering investigation: 0.24.2

The reported change was roughly 250 FPS with the GUI closed and 120 FPS while it remained
open, equivalent to about 4.0 versus 8.3 ms per frame. The agreed goal is settled open-GUI
FPS within 10% of the same scene closed, preserving the current appearance.

## Implementation

Qraftys previously extracted a separate 64-by-64 padded quad for every glyph and repeated
that submission for shadows and glow. The same font geometry now lives in the existing
2048-entry, reload-invalidated layout cache. Contiguous glyphs share one immutable render
state, with per-glyph tints and explicit Unicode fallback boundaries. Original quads and UV
interpolation remain intact: physically cropping them changed nearest-sampling stem edges
at fractional scales. Ordering bounds account for the asymmetric atlas padding: the right
and bottom contain eight extra unused logical pixels. Only proven empty/zero-tail regions
are excluded from ordering bounds. Bitmap body and effect samplers now bind together in one material;
the shader selects the original nearest or linear sampling for each layer. This avoids
draw-call splits between overlapping text layers without resampling the atlas. A guarded fragment discard skips only padding outside the bitmap's
possible body ink; effect layers retain their original shader and geometry.

Unsupported glyphs retain Minecraft's font renderables and the original normalized Gaussian
stencil. Their prepared glyphs, immutable poses, material and clipping data are reused in a
1024-entry cache, cleared by font/resource reload. Container label adapters retain their
existing interception path. Each deferred state retains its own data; buffers are not reused
across an unfinished frame.

ClickGUI has an explicit ordered batching scope. Commands may pass earlier commands only
when their conservative visible bounds do not overlap. Compatible commands retain their
original order within a batch. Every command retains its own immutable clip. Bitmap text
carries the same framebuffer clip bounds in its vertices, allowing body and glow clips to
share one material; clipping happens after coverage evaluation to preserve edge derivatives.
Native text/items/pictures and other mods' native GUI submissions flush pending commands.
Outside the explicit ordered scope, scissor changes keep the previous flush behavior. The queue flushes at 1024 entries and is
cleared even if extraction fails. Nested icon/stencil batches stay grouped. Overlap dependencies
are calculated once per pass rather than repeatedly scanned for each material. Randomized
checks verify every overlapping pair and the original scheduler's exact output order. A
16-entry LRU reuses the index plan only when every pipeline identity, texture setup, clip
and visible bound is unchanged. It reads fresh frame commands on every hit, owns no GPU
resources, and clears on font/resource reload. Colors and animation payloads are not cached
by this plan.

Unchanged vector icons reuse immutable extraction commands in a bounded 128-entry cache.
The key includes the complete transform, clipping, color, rounding, opacity and resolved
paint. Animated paints and effect scopes continue live extraction; the cache owns no GPU
resources.

Window/header layout is reused until geometry, page, language, font generation, theme
revision or page animation changes. Settled collapsed cards skip hidden control measurement.
Repeated text elision has a 1024-entry cache keyed by font, scale, generation, text and width.
Animations, live labels, caret, hovering, input and the world continue updating normally.

Blur requests collect the actual transformed, clipped bounds of every requester. Filtering
works backwards through the complete downsample/upsample chain to include every source tap,
bilinear neighbour and outward-rounded pixel at odd dimensions. The source capture remains
complete and current. World blur retains full-frame filtering. Resolution, kernel, radius,
opacity and refresh rate are unchanged; targets remain pooled and close on resource reload
and shutdown.

Fully transparent paints bypass extraction and drawing. Blur pass uniforms are uploaded only
when their exact float bits change (size, radius, pass direction or opacity), and their cached
values are discarded with the GPU buffers. The scene capture and filtering still run every
requested frame.

These are implementation optimizations, active with Performance mode Off. No settings or
profile migration is introduced. Existing optional effect reductions remain independent.

## Reproduction and boundaries

Local evidence is under `.tooling/gui-render/`, excluded from Git. Baseline is the frozen
0.24.1 production JAR, SHA-256
`116898fdfa23e7f7a34fbecb70694c1101ba1c04d69f2f5f5b557f4720944d15`.
Both sides use production JARs with a separate test fixture; private font assets and test
classes do not enter the release.

The final runs use a frozen snapshot of 53 mod files (including disabled files), with manifest
SHA-256 `3a314ce3c3e7df7f8dd347e52206e72e3f094156fce3667fbe4b6d08315df523`.
A live profile update temporarily removed a required library during an earlier launch. That
failed launch and the earlier changing-stack measurements are diagnostics, excluded from the
final comparison. Tests read the frozen mods, shader pack and font, while installation checks
the current profile anew.

The disposable fixture uses the personal Qraftys theme (font scale 1.3, GUI blur radius 22,
glow radius 7, configured window dimensions), Complementary Unbound r5.9.3 with its 22 custom
options, and the installed mods except Baritone. It asserts the selected font, radius,
active shader pack, Performance Off, equal uncapped limits and no inactivity throttle.
Resolution is 2048×1152 with Minecraft GUI scale 3. The Fabric fixture uses its consistent
flat-world settings (seed 1, no generated structures or mob spawning), a 1536 MiB Java heap
and `ActiveProcessorCount=4`. These test JVM settings are unchanged on both sides and do not
change the installed game's JVM arguments. The player faces a fixed platform,
structure and water in a consistent disposable world at render distance 12. Gameplay modules
and Discord presence are disabled only inside the fixture. It never connects to the user's
server, edits the user's world, or stops their game.

Each main state receives 10 seconds of wall-clock warm-up and 30 seconds of measurement. A
final closed-GUI control gets five seconds of warm-up and ten seconds of measurement. The
stricter target check uses the faster of the before/after closed medians, exposing warm-up or
background-load drift. Frame
timings cover Minecraft's renderFrame call and exclude Fabric's barriers between calls.
Direct thread counters measure CPU and allocation; asynchronous timestamp queries measure
the elapsed GUI/blur GPU command interval without blocking for results. GUI intervals can
include command-stream idle time and are not a pure shader-instruction cost. Scrolling uses a fixed 50 ms input interval and reverses every three seconds, independent
of FPS. JFR runs are separate from the timing comparison.

Initial setup attempts are retained as diagnostics and excluded from final comparisons.
The original all-chunk-download wait could not complete at the requested distance; the
corrected fixture matches server view distance and waits for rendered sections. An init
script also initially copied configuration relative to its own directory. The destination
is now absolute, frozen fixture files have checksums, and appearance/shader assertions catch
that mismatch before measurement.

These are controlled native scenes, not proof of identical FPS on every multiplayer world,
shader, imported font or hardware configuration. Baritone is omitted from clean-exit tests
because its non-daemon workers previously triggered Minecraft's exit watchdog; its installed
JAR and settings remain untouched.

## Visual and correctness checks

The fixed-clock Qraftys and Inter reference scenes include fractional placement, clipping,
0.37-opacity bitmap/native Unicode, font reload and resource reload. The test normalizes the
version label and suppresses the independently fading native Saving world indicator during
pixel references. Vanilla and Sodium reference pairs also suppress native vignette brightness drift.
The Iris reference pair settles the separate native page-gear entrance, which otherwise
left three pixels at different one-step rounding values in the first Qraftys image.
Vulkan retains its normal vignette/menu behavior. These controls
are test-only and do not affect timing runs or the production JAR. These images
match 0.24.1 byte for byte in decoded RGBA8. Three adversarial blur scenes with disjoint
requesters, moved/scaled windows, clipped edges and a deterministic GPU checkerboard match
full-frame filtering byte for byte. Native input checks cover search invalidation, resized
hit testing, repeated opening/closing, odd-sized resize, Unicode and successful resource
reload. Earlier diagnostic screenshots caught fractional-scale bitmap interpolation changes;
those cropped-geometry variants are not the release candidate.

Headless `check build compileGametestJava` passes, including 53,043 UI assertions covering
overlap ordering, Gaussian zero-tail bounds, blur sampling support and existing input checks.
Existing runtime and performance suites also pass.

The production-JAR entry audit finds 42 changed entries relative to installed 0.24.1: GUI
classes, blur/render helpers, shaders, mixin registration and release metadata. Gameplay and
camera classes are unchanged. The release excludes all gametest classes and private font
assets.

## Final artifact and profile

The final 0.24.2 production JAR has SHA-256
`9acce993711cdebcc188c290a43cbb7e7233ca67c0aaf3e9a920be060f94f9fb`.
A separate 30-second Search JFR recording identified rebuilding the ordering graph in
164 of 1,757 render-thread samples before plan reuse (9.3%). After plan reuse, the graph
build is absent from sampled hotspots; `UiBatch.flush` appears in 18 of 1,719 samples
(1.0%). Remaining samples include shape/glyph vertex writing, Minecraft's vertex upload,
driver synchronization, theme lookup and the world/shader renderer. Sampling proportions
are diagnostic evidence, not exact time attribution or a live multiplayer FPS guarantee.

Three alternating baseline/candidate pairs for the preceding `8a10b316…` candidate measured
these medians of per-run medians, with appearance preserved:

| Scene | 0.24.1 frame time | Before plan reuse | Reduction |
| --- | ---: | ---: | ---: |
| Closed | 2.45 ms | 2.38 ms | 2.8% |
| Search | 7.82 ms | 3.21 ms | 58.9% |
| Expanded settings | 6.37 ms | 3.02 ms | 52.6% |
| Scrolling | 7.03 ms | 3.03 ms | 56.9% |

Search draw calls fell from about 927 to 18 per frame, CPU time from 7.19 to 2.43 ms,
and allocation from 1.72 MB to 0.30 MB per frame. Search p95 frame time fell from 11.62
to 5.18 ms. The exact personal scene retains a measurable open-GUI overhead: its Search
FPS was 63–74% of the faster before/after closed control, and expanded settings 74–79%.
**The agreed within-10% target did not pass.** Those repeated results apply to the
pre-plan-reuse JAR; final-artifact follow-up results are recorded separately below.

## Final-artifact follow-up

A fresh sequential 0.24.1/final-0.24.2 pair completed with the same frozen fixture.
The user's existing Modrinth Minecraft process was also active; no application was stopped
or changed. These are shared-load diagnostic results, not an isolated FPS acceptance test.

| Scene | Render-thread CPU per frame | Draws per frame | Allocation per frame |
| --- | ---: | ---: | ---: |
| Search | 7.81 → 2.54 ms | 927 → 18 | 1.84 → 0.31 MB |
| Expanded | 6.28 → 2.75 ms | 674 → 30 | 1.46 → 0.32 MB |
| Scrolling | 6.94 → 2.67 ms | 864 → 20 | 1.75 → 0.33 MB |

Search CPU time fell by 67.5% and allocation by 83.0%. Raw median frame times were
8.89 → 6.35 ms for Search, 7.53 → 5.49 ms for expanded settings, and 7.67 → 5.71 ms
for scrolling. The beginning closed control drifted from 4.86 to 6.47 ms while its CPU
time stayed 2.43 → 2.42 ms; the ending closed controls were 5.63 and 5.72 ms.
This drift, and a concurrently busy user client, prevent attributing the final open/closed
FPS ratio to this change. Although its raw Search ratio was 90.15%, it is explicitly
**not counted as a pass of the 10% goal**. Earlier three-pair results remain separately
reported above; the exact goal remains outstanding.

The final production JAR passes all four native configurations and all 44 exact decoded
RGBA8 comparisons. Headless checks pass, including 53,043 UI, 35 runtime and 304 performance
assertions plus the existing suites. The two refreshed reference pairs use the documented
test-only controls; release code and timing behavior are unchanged.

## Installation receipt

Installed 0.24.2 into the authorized Modrinth **Fabric 26.2** instance, with exactly one
`riserecode` JAR and matching build/installed SHA-256. The previous 0.24.1 JAR and a manifest
are backed up outside `mods/` under
`.riserecode-backups/gui-render-20261023T001008Z/`. All 52 unrelated mod files were verified
unchanged, including an independent post-install checksum pass. Saved settings and other
configuration passed the installer's preservation comparison. Six terrain tiles changed
concurrently; the installer did not write terrain/configuration files. The approved temporary
Bobby setting remains `enabled=false`, with its cache untouched.

The existing Minecraft process was left running. **Restart Minecraft to load 0.24.2.**
Installation does not establish the outstanding FPS target; it delivers the verified CPU,
allocation and submission improvements described above. The local detailed receipt is
`.tooling/gui-render/install-receipt.json`.
