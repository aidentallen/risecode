> Archived development guide. Version-specific descriptions and verification counts
> refer to their original deliveries. See the current [README](../README.md),
> [changelog](../CHANGELOG.md), and [upgrade guide](../UPGRADE.md) for 0.9.2.

# risecode

Original Java 25 client-side Fabric utility mod for Minecraft 26.2. The current
interface follows the in-game 6.130 references: one 4:3 window, a quarter-width
sidebar, teal navigation, compact module cards and inline settings, and a tight
right-aligned HUD. The visible name is **risecode**; the package, mod ID, config
path, and JAR basename remain `riserecode`.

Version 0.9.1 adds the NoRender fog controls. Version 0.9.0 adds original Ambience sky/water effects, optional official Baritone
controls, typed settings, Appearance presets and branding, independent HUD
modules, animation channels, expanded native menu theming and shader-based UI
rendering. [UPGRADE.md](../UPGRADE.md) documents usage and verification.

Render → NoRender starts disabled. **All Fog** removes every native fog pass;
turn it off to choose Overworld, Nether, End, other dimensions, Water, Lava,
Powder Snow, Blindness and Darkness independently. The settings persist with
the normal module config and profiles.

Version 0.8.0 adds Storage/Player/Block ESP with Box and exterior-only Outline
modes, native crosshair scaling, independent left/right View Model transforms,
and Hold C Zoom with scroll adjustment. It also adds selected-tab text colors,
HUD borders, collapsible color pickers, configurable dropped-item rotation/spin,
HUD/font caching, professional vector sidebar icons, and individually themed
Start, Multiplayer, and Options screens. [POLISH.md](../POLISH.md) documents the
controls, rendering design, persistence, and verification.

Version 0.7.2 adds an inline keybind editor to every module, Hold/Toggle activation,
and a Reset button. Bindings and activation modes persist in configs and profiles;
temporary holds release on key-up, focus loss, menus, disconnects and rebinding.
[KEYBINDS.md](../KEYBINDS.md) describes the controls and verification.

Version 0.7.1 added a shared HSV/alpha/hex color picker and independent gradients
for module colors, HUD colors/backgrounds, interface surfaces, borders and shadows.
Enchanted held items now use a whole-silhouette exterior outline, with a separate
optional glint overlay. [COLOR_OUTLINE_FIX.md](../COLOR_OUTLINE_FIX.md) describes the
controls, backward-compatible config format and OpenGL verification.

Phase 8 adds concurrent frame animations, a full lightmap FullBright override,
seven live/editable HUD elements, per-profile HUD layouts and animation policy,
animated gradients, free camera/freelook, weather overrides, grounded item
rendering, enchanted-item glow/glint and independent block fill/outline.
[PHASE8.md](../PHASE8.md) contains the full source delivery and OpenGL evidence.

Phase 7 adds a live Themes editor with typography, HSV accents, glow, geometry,
animation policy, localized GUI blur, whole-world blur and HUD styling. Themes
are saved independently of module profiles. The Phase 7 source snapshot is
[PHASE7.md](../PHASE7.md). The current typography correction is documented in
[FONT_FIX.md](../FONT_FIX.md).
[FONT_SOURCES.md](../FONT_SOURCES.md) lists font provenance and local asset installation.

Google Sans, Montserrat and Inter are bundled under their SIL Open Font Licenses.
Roboto remains available to Minecraft's Unicode fallback. Qraftys is installed as
a local resource pack and Minecraftia as a local personal-use font; their assets
are excluded from the distributable JAR. All five choices are available in this
development instance. The visible brand remains **risecode**.

Historical deliveries remain in [UI_REDESIGN.md](../UI_REDESIGN.md), [PHASE6.md](../PHASE6.md),
[PHASE5.md](../PHASE5.md), and [PHASE4.md](../PHASE4.md).

## Files

All packages are under `src/main/java/dev/riserecode`.

| File | Responsibility |
| --- | --- |
| `util/MathUtil.java` | Linear, OutQuart, InOutCubic, OutElastic, OutBounce, interpolation |
| `ui/animation/Animation.java`, `AnimationManager.java` | Independent state machines, once-per-frame scheduling, smooth reversal and cleanup |
| `event/Event.java` | Opt-in cancellation for input, packet, and attack hooks |
| `event/EventBus.java` | Typed synchronous listeners, priority, cached dispatch, subscription cleanup |
| `module/Module.java` | Metadata, keybind, toggle lifecycle, settings, owned listeners |
| `module/Category.java` | Feature categories |
| `setting/*.java` | Boolean, bounded double, enum, ARGB/gradient paint, `visibleWhen` predicates |
| `Client.java` | Singleton and ordered system initialization/cleanup |
| `RiseRecodeClient.java` | Fabric client entrypoint and event adapters |
| `event/*Event.java` | Tick, HUD/world extraction, keyboard, packet, and attack events |
| `module/ModuleManager.java` | Registry, lookup, toggles, default modules |
| `module/modules/FullBrightModule.java` | White lightmap substitution or extreme raw gamma, exact restoration |
| `module/modules/SprintModule.java` | Forward/omnidirectional sprint requests with vanilla eligibility guards |
| `module/modules/KillAuraModule.java` | Filtered/sorted targets, rotation leases, randomized timing, cooldown, optional weapon selection |
| `util/TargetUtil.java` | Entity policies, friends/passives/visibility filters, distance/health/angle sorting |
| `util/RotationManager.java` | Priority ownership, snap/smooth visible angles, expiry and restoration |
| `util/AttackManager.java` | Per-owner CPS timing, shared tick guard, native range/cooldown, attack intent and swing modes |
| `util/ItemUtil.java` | Data-driven hotbar weapon ranking and reversible selection |
| `module/modules/TracersModule.java` | Extracted player lines with separate friend colors and configurable width |
| `module/modules/NoFallModule.java` | Local fall-distance reset on tick or movement-packet send |
| `command/CommandManager.java` | Prefix routing, quoted arguments, aliases, local feedback |
| `command/commands/*.java` | Toggle, bind, friend, profile, and help commands |
| `friend/FriendManager.java` | Case-insensitive names, optional UUID identity, immutable snapshots |
| `config/ConfigManager.java` | Global Gson JSON persistence, friend/prefix storage, validated profile application |
| `config/ProfileManager.java` | Named atomic profile files and open-GUI refresh |
| `config/JsonStorage.java` | Shared UTF-8 JSON reads and atomic replacement |
| `keybind/KeybindManager.java`, `KeybindController.java` | Registered GUI key, exclusive key capture, Hold/Toggle dispatch and temporary activation cleanup |
| `ui/component/setting/KeybindControl.java` | Per-module key capture, Reset and activation mode buttons |
| `render/WorldRenderer.java` | Immutable camera-relative geometry submitted to vanilla's line pipeline |
| `ui/hud/HudRenderer.java` | Compact watermark and flush-right rows with smooth entrance, exit, and sorted Y transitions |
| `ui/clickgui/ClickGui.java` | Single draggable window, sidebar navigation, search, management pages, and animated input transforms |
| `ui/theme/Theme.java` | Shared live design tokens, animated accents, display labels and Unicode fallback styles |
| `ui/theme/ThemeManager.java`, `ThemeProfile.java` | Typed visual preferences, independent atomic JSON/profile persistence |
| `ui/font/CustomTextRenderer.java`, `BitmapPackFont.java` | STB TrueType and actual Qraftys bitmap glyphs, kerning, SDF shadows/glow |
| `ui/component/ThemeEditor.java` | Scrollable live visual controls, theme save/load/reset, font reload |
| `ui/render/UiMesh.java` | Immutable custom GUI quads submitted through native render pipelines |
| `util/BlurUtil.java` | Reusable half-resolution scene capture, separable Gaussian blur and rounded masks |
| `util/RenderUtil.java` | Shader distance-field shapes, borders, soft shadows, glow, gradients, text, nested scissors |
| `ui/component/*.java` | Bounds, outline icons, sidebar, Unicode input, clipped lists, module cards, actions |
| `ui/component/setting/*.java` | Toggle, bounded slider, enum selector, shared HSV/alpha/hex/gradient picker |
| `render/ItemOutlineRenderer.java` | Exact atlas-alpha silhouette mask and exterior-only GPU dilation |
| `mixin/KeyboardHandlerMixin.java` | Raw keyboard input adapter |
| `mixin/ChatInputMixin.java` | Local command interception before chat signing and packet creation |
| `mixin/ConnectionMixin.java` | Client connection send/receive cancellation and packet replacement |
| `mixin/GuiRendererMixin.java` | Finished-scene blur immediately before HUD/menu rendering |

## Build and run

Pinned versions: Minecraft 26.2, Fabric Loader 0.19.5, Fabric API
0.161.0+26.2, Loom 1.17.21, Gradle 9.6.0, Java 25. No preview features.
Minecraft is unobfuscated; this project uses `net.fabricmc.fabric-loom` without
Yarn or a mappings dependency. All code is client-only by mod metadata.

```bash
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
./gradlew build
./gradlew runClient
```

The installable mod is `build/libs/riserecode-0.9.1.jar`. Install it together with
Fabric API in a Minecraft 26.2 profile using Fabric Loader and Java 25.

Right Shift opens the GUI from gameplay or the title screen; Right Shift again
or Escape closes it with a short exit transition. Change the GUI binding in Minecraft's Controls menu.
Right-click any module card to expand its settings. At the bottom, click the
keybind button and press a key; choose **Toggle** or **Hold**. **Reset** clears the
binding. Escape cancels capture; Delete/Backspace clears it. Module binds only fire
in focused gameplay and ignore keyboard repeats. FreeLook defaults to Left Alt
with Hold; other modules start unbound. Commands also work: `.bind Sprint F6 hold`,
`.bind Sprint F6 toggle`, or `.bind Sprint NONE`. The GUI key is reserved.
The HUD control on the Appearance page toggles the overlay; F1
also hides it through Minecraft's native HUD visibility.

FullBright starts disabled with `Mode = LIGHTMAP`. Enabling it binds a cached
16×16 white GPU lightmap, illuminating caves and the Nether. Vanilla continues
updating its own texture, so disabling immediately restores that same texture.
`GAMMA` mode exposes `Brightness` (0.0–1.0) and sets raw gamma to
`Brightness × 1000`, bypassing only that option instance's slider validation.
The captured gamma and all owned GPU resources are restored/released on disable
and shutdown. Changing modes while enabled also restores the original gamma.

## Commands, friends, and profiles

Commands begin with `.` by default and execute locally. Prefix messages, including
unknown commands and malformed arguments, are consumed before chat signing and
never become outgoing chat packets. Names and aliases are case-insensitive; quoted
module names and escaped arguments are supported. Ordinary chat remains unchanged.

```text
.help
.t Sprint
.toggle Tracers
.bind Sprint F6
.bind Sprint NONE
.friend add PlayerName
.friend remove PlayerName
.friend list
.profile save PvP
.profile load PvP
.profile list
.hud
.hudedit
```

Friends are global and saved in `config/riserecode/config.json`, with the command
prefix and current configuration. Online players acquire their UUID from the
client player list; offline names are supported. Friend checks match either UUID
or case-insensitive name. KillAura excludes friends, and Tracers draws them in a
separate configurable color. The prefix can be changed through `CommandManager`
or the `commandPrefix` config field while Minecraft is closed.

Profiles live in `config/riserecode/profiles/<name>.json` and contain module
enabled states, keybinds, typed module settings, HUD layout and gradient/animation
policy. Fonts, colors, blur and HUD visibility belong to independent theme profiles. Names must begin
with a letter or digit and contain up to 32 letters, digits, hyphens, or underscores.
Loading validates values before changing state, disables modules before replacing
settings, and restores requested enabled states after all values are installed.
Activation failure attempts to restore the previous configuration. Missing or
invalid profiles report an error locally. Friends and the command prefix remain
global when switching profiles; an open GUI refreshes without discarding its layout.

All sixteen default modules start disabled. Sprint requests sprinting on forward
movement, or any movement with `OmniSprint`, while preserving vanilla eligibility
and movement rules. KillAura's selection, rotations, timing, and weapons are
described below. Tracers uses depth-tested lines, with no rendering
through solid terrain. Right click each module header to edit its settings.

**NoFall is a local state demonstration:** Minecraft 26.2 movement packets contain
position, rotation, on-ground, and horizontal-collision flags, but no fall-distance
field. `VANILLA` resets the client's fall distance on ticks; `PACKET` resets it at
movement-packet sends. Both leave outgoing movement fields unchanged and do not
prevent server-side fall damage.

## Advanced combat engine

This is original code built for the existing architecture. No Meteor classes,
source, or GPL implementation were imported; the repository has not been converted
to GPL. The utilities are shared services or stateless helpers, rather than logic
embedded exclusively in KillAura.

KillAura defaults: `Range=4`, `MinCPS=6`, `MaxCPS=10`, `Rotate=SMOOTH`,
`RotationSpeed=360` degrees/second, `TargetMode=DISTANCE`, `Targets=PLAYERS`,
`OnlyVisible=true`, `IgnoreFriends=true`, `IgnoreInvisible=true`, `AutoSwitch=false`,
`Swing=true`, and both pause settings enabled. `TargetPassive` additionally controls
non-Enemy mobs when `Targets` includes mobs. Existing saved values retain their
values; newly added settings use these defaults. Right click KillAura in the Combat
panel to edit them. RotationSpeed is visible only in SMOOTH mode.

Target queries exclude self, dead/removed entities, spectators, and teammates,
with optional friend, invisible, passive, and collision-ray filters. Health sorting
includes absorption; angle sorting measures angular distance from the view direction.
Equal scores use distance then entity ID for a stable result. The selection range
does not extend attack reach: dispatch calls the modern held-item attack-range
check with zero extra reach before using the ordinary vanilla attack path.

Rotation ownership uses client-thread leases. Higher priorities win, with acquisition
order breaking ties. Modules refresh requests at UpdateEvent priority 0; the manager
resolves/applies at -50; KillAura attacks at -75; config persistence runs at -100.
Unrefreshed requests expire at the following arbitration pass. NONE releases
control, SNAPPY faces the target immediately, and SMOOTH caps combined yaw/pitch
speed using elapsed time and the shortest yaw path. The camera visibly rotates;
vanilla sends those angles on its next player tick. KillAura waits for an existing
ownership grant and a preceding player tick, then requires both previous and current
angles within 2 degrees of its target. No hidden rotations or spoofed look packets
are sent. Closing the last active owner restores captured yaw, pitch, head, and body
angles. Player replacement discards stale captured state.

Attack sessions independently sample an interval after each dispatched attack;
readiness checks never resample it. Min/Max CPS sliders can cross; the effective
pair is ordered without changing their stored values. KillAura always respects
full vanilla cooldown, so actual attack frequency can be lower than requested CPS.
The shared manager permits at most one managed attack per tick and blocks recursive
dispatch from AttackEvent listeners. Swing helpers support none, local-only,
server-only, or both; KillAura's boolean selects both or none.

AutoSwitch ranks hotbar weapons by effective attack damage, then attack speed,
preserving the current slot on a tie. Scores replace main-hand attribute modifiers
in a copy and retain other equipment/effects; no live attributes are mutated.
Item tags and attack modifiers support swords, axes, tools, and custom weapons.
Selection is visible, resets attack charge on slot changes, and is synchronized
by vanilla game-mode handling. Cleanup restores the prior slot only while the
session still owns that selection. A manual hotbar change takes priority until
the selection session ends. Enchantment damage conditional on a target is not
part of this generic attribute ranking.

Target loss, item-use pause, GUI pause, disable, and invalid player state release
rotation/slot control and clear attack timing. ClickGUI always pauses, even when
PauseInInventory is disabled. The attack phase rechecks screen, player, friends,
and target validity after other selection-phase listeners have run. Every new
setting uses the existing generic config/profile persistence and UI components.

## Verification

Requires JDK 25. From this directory:

```bash
bash scripts/verify.sh
```

Builds the mod with all compiler warnings treated as errors and runs the
dependency-free `FoundationChecks`, `RuntimeChecks`, `UiChecks`, `EngineChecks`,
`CombatChecks`, `ThemeChecks`, `Phase8Checks`, `ColorPaintChecks`, `KeybindChecks`,
and `PolishChecks` harnesses through Gradle
JavaExec tasks. The standard JUnit task is disabled because those harnesses do
not use JUnit. Checks cover easing, animation, subscriptions, cancellation,
concurrent registry changes, lifecycle rollback, all four setting types, atomic
config saves, invalid input recovery, registry lookups, ARGB interpolation, slider
capture/clamping, enum wrapping, palette alpha preservation, command parsing and
routing, key parsing, friend identity, profile validation/rollback, and packet
cancellation/replacement, rotation geometry/delta time, CPS sampling/deadlines,
attribute operation order, visual profile validation/recovery, HSV math and global UI
animation continuity. Phase 8 adds independent animation scheduling and reversal,
gradient math, HUD validation, and selected-profile persistence. Color/keybind/polish
checks add paint persistence, activation modes, registry block lists, HUD borders,
and theme defaults (391 assertions across all ten harnesses).

```bash
./gradlew runClientGameTest
```

The separate `src/gametest` mod boots the real client, checks GUI opening/closing
through the keyboard mixin, real mouse toggles, slider dragging, setting visibility,
enum/color editing, scrolling, panel dragging, resize handling, gamma restoration,
HUD entry ordering/exit/reflow, and HUD/world events in an isolated test world.
Phase 5 additionally checks real chat interception without server-bound command
messages, keybind commands, forward/omnidirectional sprint flags, local NoFall
resets, send/receive cancellation and replacement, cancellation callbacks,
friend-aware target selection, a real vanilla attack on a server-spawned mob,
tracer geometry/rendering, and profile loading while the GUI is open.
Phase 6 adds deterministic live distance/health/angle selection, wall/passive/friend/
invisible filters, custom weapon attributes, hotbar restoration/manual precedence,
rotation ownership/handoff/expiry, vanilla movement rotation before attacks,
axe cooldown, recursive attack rejection, advanced settings profiles, item/death/
missing-player guards, and GUI opening between selection and attack.
It runs with OpenGL by default; use `-PgraphicsBackend=VULKAN` to test Vulkan.
Both require a display/graphics driver. The test exits
automatically. Its run directory and screenshots are in
`build/run/clientGameTest`; test classes are excluded from the installable JAR.
Phase 7 adds real font-picker and HSV input, glyph spacing, local Qraftys pack
compatibility, font/resource reload, theme profiles while the GUI is open, blur
target reuse/resize and HUD blur/none modes. Phase 7 evidence is retained in
`build/verification/phase7`; see PHASE7.md for hardware and rendering limits.
Phase 8 additionally verifies sealed-cave FullBright and restoration, gamma/shutdown
cleanup, camera input and restoration, native item/enchantment rendering, client-only
weather, simultaneous HUD animations, gradients with F1 active, real editor mouse
input and per-profile saving. The final OpenGL run passed 36 Phase 8 assertions;
42 screenshots from the complete regression run are retained in
`build/verification/phase8/opengl`. See PHASE8.md for the full source and evidence.

## Integration contract

- `RiseRecodeClient` invokes `Client.INSTANCE.init()`. It constructs the registry,
  friends, world renderer, shared rotation/attack services, default modules,
  theme manager, HUD, module config, key manager,
  profiles, and commands. Friends, setting values, and keys are loaded before
  restoring enabled states. The GUI is constructed at `CLIENT_STARTED`,
  after Minecraft's font is available; getters also support lazy construction.
- `END_CLIENT_TICK` posts `UpdateEvent`. `HudElementRegistry` posts
  `Render2DEvent` before chat using `GuiGraphicsExtractor`. World
  `LevelExtractionEvents.END_EXTRACTION` posts `Render3DEvent` for collecting
  render state. `LevelRenderEvents.COLLECT_SUBMITS` submits those immutable lines
  through `RenderTypes.lines()` and `submitCustomGeometry`; Minecraft owns the
  buffers and backend. The keyboard mixin posts press/release/repeat `KeyPressEvent`
  objects; cancelling one stops that original Minecraft input action.
- Config lives at `config/riserecode/config.json`, saves changes after a short
  debounce, and saves again before shutdown cleanup. Settings are loaded before
  enabling modules. Missing config creates defaults. Malformed JSON is backed up;
  invalid individual values are ignored. Saves preserve unknown fields, write a
  temporary file, and atomically replace the destination where supported.
- `ConnectionMixin` wraps the deepest send method and typed receive method once
  per packet, only for client connections. `PacketEvent.Send` and `.Receive`
  support cancellation and same-direction replacement. Cancelled sends notify
  any completion listener with a failed future on the channel event loop.
  Integrated-server connections are excluded. `AttackEvent` represents managed
  attacks dispatched through AttackManager, rather than every vanilla attack.
- Event callbacks run synchronously on the posting thread. Packet callbacks may
  run on a network thread; schedule Minecraft mutations onto the client thread.
  Module lifecycle and setting changes also belong to the client thread.
- Define settings with `register(...)` in module constructors or field
  initializers. Names must be unique within a module. Hidden settings retain
  their values; visibility only affects the UI. Numeric values outside their
  declared bounds are rejected without changing the previous value.
- Register feature handlers through `listen(...)` in `onEnable()`. They become
  active after initialization succeeds and are removed before `onDisable()`.
  `onDisable()` also rolls back failed initialization, so it should tolerate
  partial setup. Recursive enable/disable calls are rejected.
- Higher numeric event priorities run first; equal priorities retain insertion
  order. Cancelled events reach only listeners registered with
  `receiveCancelled = true`. Base event listeners also receive subclasses.
  Runtime listener failures are logged and remaining handlers continue; JVM
  errors propagate. A custom error reporter must return normally to continue.
- Animations take seconds, including duration and frame delta. Call `update`
  once per rendered frame, even at variable FPS; read `getValue()` for drawing.
  Requesting the current target again does not restart the clock. Retargeting
  preserves position, but does not preserve velocity. Elastic output may
  overshoot; clamp it explicitly for opacity/color channels.

```java
Animation panel = new Animation(0.0, 0.25, MathUtil.Easing.OUT_QUART);
panel.animateTo(1.0);
double openAmount = panel.update(deltaSeconds);

BooleanSetting advanced = register(new BooleanSetting("advanced", "Show extra controls", false));
DoubleSetting scale = register(new DoubleSetting("scale", "HUD scale", 1.0, 0.5, 2.0));
scale.setVisibleWhen(advanced::getValue);
```

## UI behavior and rendering

Use the sidebar to filter by category or open Search, Profiles, Friends, Appearance,
and Language. Search matches words across module identifiers, formatted names,
descriptions, and categories. Ctrl+F focuses Search; Tab/Shift+Tab cycles pages.
The input supports Unicode, selection, clipboard shortcuts, arrows, Home/End,
and delete/backspace. It scrolls long input to keep the caret visible.

Left click a module card header to toggle it; right click to expand inline settings.
Boolean rows have animated accent dots. Sliders capture the pointer until release,
including outside the viewport. Enums cycle forward on left click and backward on
right click. Color controls cycle the existing palette, preserving alpha. Hidden
settings retain their values and animate the surrounding layout when visibility changes.

Drag the logo region to move the window. Scroll module lists with the wheel.
Appearance exposes typography, colors/glow, geometry/layout, animations, background
effects and HUD settings in a clipped, scrollable two-column editor. Left/right
click cycles fonts, curves and backdrop modes; HSV squares and hue bars edit accents.
Save/Load stores the named visual profile, Reset restores defaults, and Fonts reloads
user-added assets. Every setting takes effect in the next frame, with transitions
where appropriate. Profiles continues to manage module settings independently.
Friends and Language retain their existing behavior.

`config/riserecode/theme.json` holds active visual preferences;
`theme-profiles/<name>.json` holds named themes. Visual names use 1–40 letters,
digits, hyphens or underscores. Invalid documents cannot partially apply settings;
an unreadable main theme is preserved before defaults are created. Window offset
and navigation language remain global `ui` preferences in module config. Old
accent, interface scale and HUD visibility migrate once on first Phase 7 startup.

Rendering uses Minecraft 26.2's `GuiGraphicsExtractor`, extracted custom GUI quads,
`GpuDevice`, `RenderPass` and native pipeline abstractions. Rounded surfaces, borders,
soft shadows and HSV controls use original fragment shaders. TrueType faces use
STB kerning and 8× filled-coverage atlases with fractional positions and pixel-footprint
filtering. A separate distance-field atlas supplies soft shadows/glow. Qraftys retains
its original bitmap metrics and lowercase small caps. Missing or invalid local assets
visibly fall back to smoothly sampled Inter; only a loaded pixel face uses pixel sampling. Unsupported
Unicode falls back per glyph rather than changing neighbouring Latin text.

GUI blur captures only the finished world/panorama before any HUD or screen draws.
Four depthless targets are reused at half framebuffer resolution. The Gaussian
kernel uses two passes with five bilinear fetches each; matching GUI/world radii
share an output. No CPU readback or blur feedback occurs. World blur is optional
and only runs while the GUI requests it. HUD Blur Box independently uses scene blur
while playing. GPU resources are released at shutdown and replaced only on resize.

The selected global curve (Ease-Out-Quart by default) and speed multiplier drive
opening/closing, drag settling, navigation, scrolling, expansion, hover/active
colors, accent transitions and HUD movement. Ordinary backend Animation instances
retain their own timing/curves. Retargeting preserves the rendered position;
changing curves mid-flight preserves continuity. Closing blocks further input and
completes its exit before restoring the previous screen. Disabled HUD entries
remain until their exit settles, and sorted rows animate to new Y positions.
Nested native scissors intersect module-list and card-settings viewports. Input
inverts the exact canvas/opening transforms used for extraction. Resize clamps the
window to the available area; window movement, page changes, visibility changes,
profile refresh and removal release pointer captures.

## Fabric target notes

[Fabric's 26.2 migration guidance](https://fabricmc.net/2026/06/15/262.html)
describes the OpenGL/Vulkan rendering transition and GUI/HUD API reorganization.
The HUD and GUI use Minecraft's rendering abstractions rather than raw OpenGL.
Four production mixins adapt keyboard input, outgoing chat, client connections
and the scene capture before GUI rendering. MixinExtras, used for the
connection wrappers, is supplied by the pinned Fabric Loader.

[Fabric's world-rendering documentation](https://docs.fabricmc.net/develop/rendering/world)
describes the extraction/submission split used by Tracers. Its extraction handler
collects immutable world positions, colors, and widths; the renderer converts them
to camera-relative coordinates and queues custom geometry through the native line
render type. Entities are not retained by the drawing callback.

[Fabric's 26.1 tooling guidance](https://fabricmc.net/2026/03/14/261.html)
established Java 25 and the unobfuscated `net.fabricmc.fabric-loom` plugin, with
standard dependency configurations and no Yarn mappings. Preserve that workflow
in this 26.2 build.

## Phase 8 controls and persistence

Open the **HUD** category with Right Shift. Toggle FPS, Ping, Coordinates,
PotionEffects and MemoryUsage, or press **Edit HUD** / type `.hud`. The editor
previews all seven registered elements with real data, including disabled
elements. Drag to move; right click selects scale, text alignment, background
and the element toggle. Grid size is configurable; **G** toggles grid snapping.
Screen and neighboring-element guides appear while dragging. Escape or Right
Shift saves and returns to gameplay. The editor pauses a local singleplayer
world and consumes GUI input.

HUD anchors are normalized screen coordinates and scale, alignment and background
are stored per element in `config.json` → `hudLayout`. Named module profiles
(schema 3) include that layout, snap/grid settings and `visualPolicy`: gradient
mode, speed, direction, spacing, animation speed and easing. Saving or loading a
named profile selects it; `config.json` remembers its name as `activeProfile`.
Closing the HUD editor saves the current layout to the main config and updates
the selected profile's HUD layout, HUD-module settings/toggles and visual policy.
Its saved combat/movement snapshot is preserved. Validation happens
before any module lifecycle changes; activation failures attempt rollback. Older
profiles remain readable. Fonts, colors, blur, ClickGUI position, friends and
command prefix keep their independent/global persistence.

Appearance → **Colors & Glow** exposes Static, Rainbow, Astolfo and Sine gradients.
Offsets follow the horizontal/vertical screen axis, including the real Qraftys
bitmap face. The gradient clock follows global animation speed. Each UI component
owns its own animation, and a render-thread frame scheduler advances all active
instances once; there are no animation worker threads.

Hold Left Alt to activate **FreeLook** from its disabled state, or assign another
key with `.bind FreeLook F6 hold`. Releasing restores the previous perspective.
Toggle mode keeps the camera active until the next press. Enable **FreeCam** to move the
view with WASD and Jump/Sneak; the real player stays under ordinary server physics.
Mouse turns affect only the camera while active. Disabling, disconnecting or
changing world clears the detached state.

**WeatherChanger** overrides client-side rain/thunder queries and native weather
attributes only. **ItemPhysics** changes dropped-item meshes, leaving movement,
pickup and server physics untouched. **EnchantEffect** renders exterior-only
whole-item outlines from an atlas-alpha mask and GPU dilation, plus a separate
tinted, scrolling glint through Blaze3D pipelines; `WorldItems`
adds outlines to frames/drops. **BlockOverlay** uses every AABB from the targeted
block's shape, with separately toggled fill and twelve-edge outlines.
