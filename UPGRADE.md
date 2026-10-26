# risecode 0.24.2

GUI rendering optimizations apply automatically, including with Performance mode Off.
Your font, blur, glow, shadows, animations and existing profiles keep their settings.
See [the GUI rendering investigation](docs/gui-rendering.md) for measured validation.


Client Sounds, Discord Activity and GUI Scale open inside the existing client GUI.
Footer row toggles and sound import/preview remain available. Existing page appearance
preferences and settings are retained.

Appearance section cards now bounce as a whole, including the surface and controls.
Tune the shared response under **Appearance → Button Feedback**. Set **Press Strength**
to zero to disable it; Reduced Motion also disables it. The same controls govern module cards.

Complex BlockOverlay contours use the union of Minecraft selection shapes. Anvils,
stairs and connected shapes retain their steps without component-box seams. Corner
marks apply to continuous union edges. Switching between different shapes retains
motion, including the shared entity highlight and placement-animation behavior.

Module-local Appearance shows only controls consumed by that module's output.
Some settings appear after selecting a relevant mode or a Theme-based color. Existing
inactive overrides stay in appearance profiles; no manual profile migration is needed.

# risecode 0.20.0

Container styling defaults to risecode. Choose **Appearance → Container Screens →
Container Style** or use the screen gear for **Inherit / Minecraft / risecode**.
Minecraft styling restores the native controls and surfaces; the gear stays available.
Existing explicit page opt-outs remain honored. Opening a container's gear preserves
its live menu and carried item; closing the editor returns to the same menu.

Profiles and Friends now open from the player/settings button at the bottom left of
ClickGUI. Profiles retains client and appearance Save/Load. Client Sounds and GUI Scale
moved into that popup; existing values and custom sound imports remain valid.
The sound master switch applies even to module-local sound choices. Click the chevron
beside Client Sounds or Discord Activity to open the corresponding setup panel.

Command Suggestions controls local-prefix risecode and Baritone completions only.
Minecraft/server slash commands keep their own suggestions. Menu Keybind edits the
existing Minecraft key mapping (Right Shift by default); Escape cancels capture.

Discord Activity is off by default. Its setup panel accepts a public Discord application
ID and reports connection status. Presence contains Minecraft and risecode only,
without server addresses, world names, player identities or module state.

**Background: None** removes the custom image/procedural backdrop, leaving the world
or native menu backdrop and normal interface surfaces. Particle controls and page
inheritance are independent of image opacity, tint, blur, darkness and motion. Legacy
per-page particle values migrate with their existing ownership and values.


Block Overlay has **See-through Outline**, off by default, and thickness up to 32 px.
Enable it to see every edge and the pop animation through the block. The fill keeps
its existing depth behavior. Module Appearance now changes rendered output only;
modules without supported visuals omit that section. Old overrides remain stored,
but unsupported fields no longer affect cards or other modules.

Appearance sections now match module cards. Native menu transitions animate only
buttons; the ClickGUI window keeps its own opening effect. Extensible numeric ranges
are larger, with fine slider travel around familiar values and exact numeric entry.
Existing defaults, percentages, opacity and world-coordinate bounds are preserved.

---

# risecode 0.16.1

Minecraft 26.2, Java 25, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.2.
The package, mod ID and configuration directory remain `riserecode`.

## 0.16.1 UI motion repairs

Profiles contains the Save/Load controls for client and appearance profiles.
Appearance opens directly into settings. Reset Theme and Undo Preset sit beside
presets; Import Logo, Import Image, Import Sound, Preview Sound, Reload Fonts and
Reset Layout sit in their related sections.

Sidebar hover fades, the moving selection highlight and page-switch movement are
restored. Minecraft menus animate their foreground over one stationary background,
without an additional vanilla dimming layer. Buttons use independent soft springs;
repeated presses preserve their motion and actions still execute immediately once.
Existing animation, sound and appearance settings remain valid. Reduced Motion
disables movement, scaling and spring feedback. Adjust the bounce in Appearance's
Button Feedback section.

## 0.16.0 appearance and maps

The sidebar arrow expands Friends, Profiles, Waypoints and Map settings inline.
Saved utility destinations expand automatically. Existing profiles, bindings,
friends, explored surface terrain and waypoints remain compatible.

Appearance controls the shared menu/container backdrop, background browser,
local PNG import, darkness, tint, blur and opacity. Image / Sound import accepts
local paths; imported assets are copied into the configuration directory. Sound
accepts OGG. Play previews the selected click sound. Width and Height resize the
client window independently of Interface Scale; the bottom-right handle also
resizes it. Layout restores the default dimensions and position. Reduced Motion
suppresses movement, scale and button springs. Global animation controls and
window overrides live in Appearance; each module's Pop In overrides save in its
module profile. The pause menu's risecode GUI button returns to that pause menu.

M opens the world map; B adds a waypoint. Rebind these in Minecraft Controls.
Choose Fullscreen or Panel in the map. Drag the panel title to move it and its
bottom-right handle to resize it. Wheel zoom stays centered on the cursor;
Shift+wheel chooses altitude. Auto uses roof/sky checks with hysteresis; Surface
and Cave force the layer type. The Y control selects a manual cave altitude or
returns to Auto. Minimap retains HUD-editor placement and adds Square / Circle.

Map recording defaults on, independently of overlay modules. Map settings can
turn it off. Terrain and destinations live under `config/riserecode/maps/`,
isolated by world/server and dimension and separate from module profiles.
Existing surface files remain readable. Cave layers add separate altitude stores.
Back up this directory when transferring an instance. Map backgrounds inherit
Appearance unless overridden; unexplored color and opacity are separate.

Every death records even with recording and overlays disabled. The newest death
is tracked; reaching it marks it reached rather than deleting it. Waypoints has
Recent / History pages, Hide reached and a confirmed bulk-clear action.

Zoom now stays armed when enabled. Its Hold / Toggle binding activates zoom;
each activation starts at the saved magnification. Wheel changes are temporary.
Opening a screen, losing focus, death, disconnect, disabling, rebinding or loading
a profile releases active zoom. FreeLook is now under Render with its original
identity, settings and bindings.

The new modules start off. Enable Minimap and place/scale it in the HUD editor.
Inventory Tools in AutoTool is opt-in; the default only selects hotbar tools.
FastBreak's default multiplier is 1×. Trajectory centerlines exclude random
spread and cannot predict another entity's future movement. BetterChat preserves
original signed messages and annotates repeated messages rather than deleting
signed entries.

Minecraftia remains an optional local TTF. Its visible capital height now matches
the shared text metrics; Font Scale still controls overall size. Integer physical
scales use crisp sampling; fractional scales use filtered coverage.

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

## Resources and Ambience in 0.14.0

**Resources** replaces Styles with the public Vanilla Tweaks resource-pack picker
for Minecraft 26.2. Search categories, click to select, right-click for details,
and use **Selected** to change top-first priority. Overlapping choices remain
selectable with warnings. **Apply** performs one resource reload; **Cancel** or
closing discards the draft. Failed reloads restore the previous resources and
ordinary pack list. Server packs remain above the generated Resources pack.

The first visit caches every public resource, icon, preview and discovered official
combination recipe. Wait for **Offline ready** to create new combinations without
network access. Pause/Resume, Stop and Retry retain validated downloads. Refresh
checks for source changes; each profile pins its cache revision. Copy
`config/riserecode/resources/26.2/` when moving profiles to another installation.
The WebP preview is cached in its original format and displayed using its official
icon on the stock JVM. Original Vanilla Tweaks pack contents and credits are retained.

**Ambience** is a normal Render module again, with the existing procedural sky,
water, colors and numeric settings, plus the standard module Hold/Toggle binding.
Active Iris shader packs own sky/water until disabled; module settings survive
that handoff. No resource selection automatically toggles Ambience.

Configuration schema 5 backs up old configurations as `config.pre-resources.json`
and old profiles as `<name>.json.pre-resources`. Former Styles atmosphere settings,
activation and binding move into Ambience. An explicit existing Ambience entry wins.
Old texture/style selections are cleared rather than replaced by unrelated tweaks.
Old `styles/` cache files move to a timestamped `styles-backup-*` directory and
remain recoverable. The old imported-sky renderer and Styles import browser are
removed; use Minecraft's normal resource-pack screen for other packs.

Resources cover publicly listed Java resource packs for 26.2. Hidden experimental
entries, data packs and crafting tweaks are not included. Some resources require
optional renderer features or conflict with Sodium/Iris/Vulkan; their original
notices remain visible. The picker and offline compiler use no embedded browser.

Credits:
Vanilla Tweaks: https://vanillatweaks.net/

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
warm-up. Built-in atmosphere stays at its disabled default for this comparison; the
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

## Agent removal in 0.13.1

The Agent page, provider connections, chat, AI tools and automated Agent tasks
have been removed. A previously selected AI/Agent tab opens Search instead.
F12 is available for normal module bindings again. The Baritone module and
`.b` commands remain available for direct navigation, mining and following.

Existing module profiles, Styles, appearance preferences and other configuration
are preserved. Old Agent records remain on disk but are no longer loaded.
Restart Minecraft after installing the updated JAR.

## 0.17.0

Keep the existing configuration directory. Old functional sidebar selections resolve to Gameplay or Visuals; no module IDs change. Theme profiles retain schema 1 and add an optional `Page Overrides` object. Old NoFall `VANILLA` values resolve to `AUTO`; existing `PACKET` selections remain Packet. Wider animation values require 0.17.0; restoring an older JAR may also require restoring the earlier theme/profile snapshot if those limits were used. Restart Minecraft after replacing the JAR.


## 0.17.1

The original category tabs and Utilities menu return; Appearance is one scrolling settings page again, including HUD/map styling. Saved Gameplay selections open Combat, and Visuals selections open Render. Other valid selections, modules, profiles and per-page customization remain intact. Appearance → Button Style offers Subtle (default for existing profiles) and Accent; the choice is saved independently in each theme profile. Restart Minecraft after replacing the JAR.
