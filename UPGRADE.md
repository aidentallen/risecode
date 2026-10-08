# risecode 0.9.2

Minecraft 26.2, Java 25, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2.
The package, mod ID and configuration directory remain `riserecode`.

## Controls

Right Shift and the title-screen **Interface** button open the interface.
The former Themes tab is **Appearance**; existing selected-tab values migrate.
Click a numeric label to type, or drag its lower slider track. Typed values
preview immediately. Invalid or incomplete input shows the accepted range and
keeps the last valid preview. Enter commits; Escape restores the value captured
when editing began. Ctrl A/C/X/V, selection, caret movement and Up/Down are
supported; Shift Up/Down uses a larger step. A focused field receives keyboard
input before interface shortcuts and module bindings. Searchable selectors,
key capture and right-click color expansion retain their existing controls.

Appearance contains seven visible preset previews and an **Undo** action.
Presets change palette, gradients, glow and background styling, preserving
typography, geometry, animation channels, branding and HUD layout. Named
appearance profiles remain in `config/riserecode/theme-profiles/`.

The Branding section edits display name, interface tagline and watermark text
independently. Enter a local PNG path in **Logo File**, then click **Logo** in the
Appearance toolbar to import it. Imports are validated and copied to
`config/riserecode/branding/<sha256>.png`. Size, placement, tint and visibility
update live. One texture is cached and checked for changes once per second.
The default name and watermark remain lowercase `risecode`.

NoRender is under Render and starts disabled. Its All Fog switch removes every
native fog environment. When All Fog is off, independent switches cover
Overworld, Nether, End, other dimensions, Water, Lava, Powder Snow, Blindness
and Darkness. The hook edits the per-frame FogData after native environment and
Sodium Extra distance setup; disabling the module releases its event subscription
and native fog returns immediately. Iris retains ownership of its shader-pack
fog path.

NoRender also has an independent **Chunk Fade** switch, enabled by default.
It removes native fog-colored terrain blending without changing atmospheric
fog choices. ChunkAnimations suppresses that fade while enabled, too.

## ChunkAnimations

Render → ChunkAnimations starts disabled and unbound. Up rises from below;
Down descends from above. Defaults are Up, 8 blocks, 0.5 seconds and
Ease-Out-Quart. Distance accepts 0–64 blocks, duration 0.05–3 seconds, and
the easing choices include Ease-In-Out-Cubic and Elastic-Out. Both numeric
controls support the existing focused typed editing and validation.

One clock is shared by all vertical sections in a newly received chunk,
starting at its first drawable frame. Existing terrain does not move when
enabled, and ordinary mesh rebuilds do not restart the animation. Active
flights retain their captured settings; edits apply to subsequent flights.
Block entities follow the terrain. Physics and collision keep their native
positions. Disable, unload, disconnect, dimension changes and resource reload
clear old animation state; profiles retain controls without stale flights.

Vanilla uses section transforms. Sodium 0.9.2 uses a reusable GPU offset table,
with matching culling and draw-command updates rather than mesh rebuilding.
Ambience water composes with the same offsets. Iris 1.11.4 terrain and shadow
programs receive the same frame offsets; Complementary Unbound r5.9.3 is the
verified pack. Shader packs retain their lighting, sky, water and fog.
Replacement Sodium shaders use Sodium Extra 0.9.4's own fog-shape transformer,
preserving matching shader interfaces on OpenGL and Vulkan.
Optional renderer adapters are compile-only dependencies, not bundled mods.

Chunk checks run with `./gradlew chunkAnimationCheck`; real-client checks use
`runClientGameTest -PchunkAnimationsOnly`, adding `-PinstalledStack`,
`-PwithShaderPack`, or `-PwithoutIris -PgraphicsBackend=VULKAN` as appropriate.
The disposable test client has a bounded heap and disables focus pausing;
these choices do not change the installed instance's options.

Animation channels independently control opening, navigation, hover, press,
toggles, expansion, scrolling, dragging, HUD entrance, exit and reordering.
Each exposes seconds and easing, with movement or scale where the transition
uses that effect. Global speed and Reduced Motion also apply. Running instances
retarget from their current rendered value when reversed or when policy changes.
Legacy global easing migrates into missing channel preferences.

Watermark and ArrayList are HUD modules with independent toggles and bindings.
Their existing element IDs and layouts remain intact. Both start enabled during
migration, with global HUD Visible and F1 still respected. The ArrayList excludes
its own containers and other HUD modules by default; both choices are settings.
The HUD editor also accepts typed scale and normalized X/Y positions.

## Ambience

Render → Ambience starts disabled, with Aurora, theme colors and Balanced
quality selected. Sky and Water are independent. Aurora, Nebula, Sunset, Clouds
and Storm are original procedural shaders with intensity, speed, softness,
stars, color and quality settings. Water shades native still and flowing fluid
surfaces with animated swell/ripple normals, optical-depth tint, transparency
and analytical reflections of the same sky. It retains the fluid meshes,
terrain lighting, fog, texture sampling, cutout and native transparency sorting.
Animation updates GPU parameters without rebuilding chunks.

Adapters use Minecraft 26.2 RenderPass/RenderPipeline on OpenGL and Vulkan,
including Sodium's packed terrain format. Sky rendering respects native reverse
depth and the actual camera pose. Dimensions without an overworld sky retain
their native sky. An active Iris shader pack owns sky and water; Ambience keeps
its saved settings, shows a suspended status and resumes when the pack is off.
Disable, disconnect, dimension changes and resource reload release or reset
owned resources and restore native pipelines.

## Baritone

Utility → Baritone uses the separate official **API Fabric 1.19.0** JAR:
<https://github.com/cabaletta/baritone/releases/tag/v1.19.0>.
Its SHA-256 matches the official release asset digest:
`eca6e2fdf43c6657fe9fea10a8f9a7572d78dc91ad389b998b808bd28fa5b5d1`.
The implementation is not embedded in risecode. Without the dependency, the
module shows an explicit unavailable status and the client still starts.

Choose Destination, Mine, Follow or Command, enter its controls and press Start.
Enabling alone starts nothing. Pause, Resume and Stop use the public API;
disable, disconnect and profile changes cancel work and clear forced input.
Coordinates and task controls are saved, while running tasks are never resumed
from configuration or profiles. `.b <command>` and `.baritone <command>` forward
locally; native chat Tab completion uses Baritone's public command registry and
argument completion.

## Menus and rendering

Start, Multiplayer, Options and Loading each have separate theme/background
switches. Native connecting, disconnected, world-selection/loading, resource
reload, settings and installed Sodium/Sodium Extra, Iris and Mod Menu controls
retain their input, focus, narration, progress, cancel and back actions.
Uncapped Themed Menus defaults on, overriding the software cap only while a
themed interface is active. VSync, minimized and AFK throttles stay native.
Turning it off or leaving a themed menu immediately restores native limits.
Initial startup uses the native resource overlay until custom shaders are ready.

Paint shapes use one quad with immutable endpoint/policy attributes; gradients
are sampled in the fragment shader at logical screen coordinates. Glyph runs
batch only adjacent compatible layers, flushing for native Unicode fallback.
Icon arcs, formatted values, module filters and labels are cached. Offscreen
controls skip extraction. Panel blur covers requested regions plus the entire
kernel margin, shares equal-radius passes, and reuses framebuffers at the
existing half resolution. Static menu backdrops remain cached; world effects
still update every frame. Existing quality settings remain available.

## Verification

The 0.9.2 release passes **663 core assertions**, including 85 chunk-animation
and 132 NoRender assertions. The new real-client chunk suite passes 21 assertions
each on vanilla OpenGL, vanilla Vulkan and the installed Sodium/Sodium Extra
Vulkan stack, plus 28 on the installed OpenGL stack with Iris and Complementary
Unbound r5.9.3. Vulkan uses the installed stack with Iris excluded: Iris 1.11.4
requires OpenGL. Tests cover first drawable starts, both directions, rebuilds,
Ambience composition, shader toggles, streamed chunks, resource reload, dimension
changes and disconnect cleanup. Core checks also cover captured settings,
all easing curves, unload/reload, defaults, persistence and profile restoration.

Fixed-progress screenshots compare completed flights with native terrain across
all four configurations. All 24 static stone/glass/chest region comparisons pass
the declared RMSE ≤ 4 and p95 channel difference ≤ 5 tolerances; the largest
observed RMSE is 2.69 on a 0–255 channel scale, and the chest crops match exactly.
Animated water, changing UI and sky are excluded from those numeric comparisons.
Logs, screenshots, comparison script/results and installation checks are retained
in `upgrade-evidence/chunk-0.9.2/`.

`scripts/verify.sh` builds the release and runs all core checks, including
`upgradeCheck`. The disposable real-client suite runs with:

```bash
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew runClientGameTest -PupgradeOnly
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew runClientGameTest -PupgradeOnly -PinstalledStack -PwithBaritone -PwithShaderPack
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew runClientGameTest -PupgradeOnly -PinstalledStack -PwithoutIris -PgraphicsBackend=VULKAN
```

All game-test worlds/configuration are isolated in `build/run/clientGameTest`.
Benchmark and deterministic screenshot evidence is kept in `upgrade-evidence/`.
The pristine 0.8.0 benchmark source is preserved in
`build/upgrade-evidence/source-0.8.0.tar.gz`.

The original 0.9.0 build passed **446 core assertions** across eleven check tasks.
Its real-client suite passed 40 input/render assertions on vanilla OpenGL,
54 on the installed OpenGL stack with Baritone and Complementary Unbound,
and 40 on the compatible Sodium Vulkan stack. Each run also passes the native
menu, background, cancellation/back, VSync and AFK-limit checks. Evidence is in
`logs/upgrade-build-final.log`, `logs/upgrade-gpu-*-final.log` and the
`upgrade-evidence/*-final/` screenshots. Narrator audio could not initialize in
the disposable environment because its native flite library is unavailable;
native narration code remains unchanged.

## Performance and visual evidence

The reproduction uses the same disposable world, stone/water scene, camera,
installed mod stack, 2048 × 1152 window, GUI scale 2 and warm-up for both versions.
The CPU is an Intel i3-10100F and the GPU is an AMD Radeon RX 9070 using Mesa
26.2.2. World blur, panel blur, shadows, glow and animated Sine gradients are
enabled at the existing quality settings; VSync is off and the software cap is
unlimited. Each measurement contains 600 consecutive full-frame intervals after
warm-up. New Ambience stays at its disabled default for this comparison; the
separate sky/water suite exercises all modes. No Baritone task or shader pack
runs during measurement.

| OpenGL state | Median frame time | p95 frame time | FPS from median |
| --- | ---: | ---: | ---: |
| 0.8.0 interface closed | 0.919 ms | 1.427 ms | 1088 |
| 0.8.0 interface open | 9.760 ms | 13.622 ms | 102 |
| 0.9.0 interface closed | 0.942 ms | 1.392 ms | 1061 |
| 0.9.0 interface open | 1.683 ms | 3.142 ms | 594 |

Opening adds **8.841 ms before and 0.741 ms after**, a **91.6% reduction** in
median overhead, exceeding the requested 50% target in this scene. These are
local reproduction results, not a general gameplay FPS guarantee. On the
compatible Vulkan stack the final open-interface median/p95 is 1.405/2.150 ms
(712 FPS from median). Raw measurements are in
`upgrade-evidence/benchmark-{baseline-full,after-full,after-vulkan}.jsonl` and
the calculated comparison is in `upgrade-evidence/performance.json`.

The fixed screenshot fixture includes all five paint modes, rounded corners,
borders, shadows, glowing glyphs and native Unicode fallback. Against 0.8.0,
foreground RGB RMSE is 1.677/255, with p95 channel difference 2/255 and maximum
17/255 at transitions; continuous sampling replaces the former gradient strips.
The final OpenGL/Vulkan fixture has foreground RMSE 0.046/255, p95 difference
zero and maximum difference 1/255. Shapes, text placement, clipping and font
resolution are retained. Screenshots and numerical results are in
`upgrade-evidence/{baseline-full,after-full,after-vulkan}/` and
`upgrade-evidence/fidelity-comparison.json`.

## External dependency limitations

The installed Iris 1.11.4 aborts during Vulkan renderer initialization when it
queries an OpenGL sampler limit without an OpenGL context. Vulkan validation
therefore omits Iris in the disposable stack; the installed instance is kept
intact and should use OpenGL while Iris is installed. The captured JVM callback
stack and system core metadata confirm the startup call path; no OOM occurred.

Official Baritone 1.19.0 leaves non-daemon cache workers waiting on its queue after
normal client cleanup, which trips Minecraft 26.2's post-main shutdown watchdog
in the task-exercising test instance. Functional API checks finish before this
dependency shutdown failure. The official JAR is preserved unmodified; risecode
does not access Baritone internals or forcibly exit the user's game.

## Installed release

The verified 0.9.1 release and official Baritone API Fabric 1.19.0 JAR are in
`/home/toast/.local/share/ModrinthApp/profiles/Fabric 26.2/mods/`.
The prior 0.8.0 JAR is backed up outside `mods/` in
`riserecode-backups/20261007-234653-upgrade-0.9.0/`, with a checksum manifest.
Exactly one riserecode and one Baritone JAR are present; all eleven unrelated
mods and twenty-two configuration files retain their pre-install checksums.
`release/manifest.json`, `release/SHA256SUMS` and
`upgrade-evidence/installation.json` record the artifacts and preservation checks.
No game process was stopped. Restart Minecraft to load both updated JARs.
