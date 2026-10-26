# risecode

### 0.27.0 Glass

Opt-in Glass materials for your body, native first-person arms, armor and held items,
plus optional other players. Includes frosted refraction, luminous edges, bloom, shimmer,
retained material overrides and a rotatable player preview. See [Glass](docs/glass.md)
and [release verification](docs/releases/v0.27.0.md).

### 0.26.0 Living Panorama

An optional forest-and-lake title scene follows the active resource pack, with six atmospheres,
wind, cursor gusts, water ripples, quiet ambience and bounded camera movement. Use the start
screen gear or Appearance to enable it; the leaf or F1 enters Take a breath. The saved backdrop
is retained, and older themes remain unchanged. See the [guide](docs/living-panorama.md)
and [release verification](docs/releases/v0.26.0.md).

### 0.25.0 rendering, typography and world resolution

Animate single and double chests through native and BetterBlockEntities paths, keep PlayerESP
submissions out of shader shadow passes, and add cycle/spatial rainbow block outlines.
Google Sans and Qraftys work offline; Font Boldness adds stroke without changing layout.
Appearance disclosures and footer details support right-click. Performance now has a sidebar
tab with independent 25–100% world resolution, Bilinear/FSR1 and sharpness controls.
The HUD and GUI stay native, and 100% bypasses scaling. See [0.25.0 notes](docs/releases/v0.25.0.md).

### 0.24.2 GUI rendering

Batch bitmap text and compatible GUI commands while preserving overlapping paint order.
Reuse layout and native fallback glyph preparation, and filter only the requested blur
regions without changing effect settings. These improvements also apply with Performance
mode Off. See [rendering details](docs/gui-rendering.md).

### 0.24.1 camera initialization fix

Zoom leaves the native camera FOV unchanged while inactive and tolerates the zero FOV
reported before the first camera tick. See [0.24.1 notes](docs/releases/v0.24.1.md).

### 0.24.0 performance controls

The footer settings popup now includes Performance: Off, Conservative, Balanced, Strong
and Custom modes with individual reductions. Off preserves selected effects. Presets
leave module settings, appearance profiles, equipped cosmetics and explored map data intact.
See [0.24.0 changes](docs/releases/v0.24.0.md) and [measurement details](docs/remediation.md).

### 0.23.0 settings panels and complex outlines

Footer settings for Client Sounds, Discord Activity and GUI Scale now stay inside
the client window. The footer popup has one continuous background, and Appearance
cards bounce as complete panels using the shared Button Feedback controls. Complex
block outlines follow merged selection contours, with smooth transitions between
different shapes and entity boxes. Module Appearance controls follow the output
each module actually renders. See [0.23.0 changes](docs/releases/v0.23.0.md).

### 0.22.0 entity hitboxes and shared target motion

**Render → Hitboxes** offers **Only When Looking** and **Always** modes. Aimed
hitboxes share BlockOverlay's switching and center-out pop animations, including
transitions between blocks and entities. Style and animation inherit from
BlockOverlay by default; disable either inheritance toggle for individual controls.
See [0.22.0 changes](docs/releases/v0.22.0.md).

### 0.21.0 placement animation and shader lighting

**Render → BlockAnimations** animates your placements with Swing, Pop, Slide or a
custom transform. Block Overlay can follow the moving model, hide until it settles,
or stay fixed. Entrance pops now run when acquiring a target; **Pop On Switch**
restores a pop for every target change. **FullBright → Shader Night Vision** supplies
night vision to Iris shader packs without placing light blocks or changing the world.
NoFall is now **Player → Clutch**, with a Ghost badge and existing profiles preserved.
See [0.21.0 changes and compatibility](docs/releases/v0.21.0.md).

### 0.20.0 container themes and client settings

Choose Minecraft or risecode container styling globally or from each screen's gear.
Background **None** keeps the world/panorama visible, and particles have independent
rendering and per-page inheritance. The bottom-left player menu opens Profiles,
Friends, Client Sounds, Command Suggestions, Discord Activity setup, Menu Keybind
and GUI Scale. Discord remains off by default and requires your public application ID.
Block Overlay adds a **See-through Outline** toggle, including during pop animations,
and thickness up to 32 pixels. Module Appearance now exposes only settings used by
its rendered output. Appearance sections match module cards, and native menus pop
individual buttons while keeping the screen stationary. Numeric controls have
extended slider ranges with precise typing. See [0.20.0 changes](docs/releases/v0.20.0.md).

### 0.19.0 appearance and block highlight update

**Render → Block Overlay** now supports smooth switching, pop animations and
corner-only outlines. Modules with supported visual output have a collapsible
**Appearance** section: leave **Use Global Appearance** enabled to inherit shared
styling, or disable it to customize that output. Settings sections remember their open state;
themed buttons share compact tab dimensions and stay pressed while held.
[0.19.0 changes](docs/releases/v0.19.0.md).

### 0.18.0 cosmetics update

Open **Utilities → Cosmetics** for the wardrobe, live outfit preview, capes,
wings, headwear, companions and effects. See the [wardrobe guide](docs/cosmetics.md)
and [0.18.0 changes](docs/releases/v0.18.0.md).

Use **Right Shift** for the client interface. The original module tabs and **Utilities** menu are restored, and Appearance is a single scrolling settings page again. **Appearance → Button Style** selects **Subtle** dark buttons (default) or colorful **Accent** buttons. Corner gears still control each page's theme and background, including animated scenes and particles. All 0.17.0 fixes remain.

[0.17.1 changes](docs/releases/v0.17.1.md) · [0.17.0 fixes and NoFall requirements](docs/releases/v0.17.0.md).

A modular client-side Fabric utility mod for **Minecraft 26.2**, built with
**Java 25**. It combines an animated sidebar interface, editable HUD,
appearance profiles, visual effects, and gameplay utilities.

[Download the latest release](https://github.com/aidentallen/risecode/releases/latest)
· [Changelog](CHANGELOG.md) · [Usage guide](docs/usage.md)

The display name is lowercase `risecode`. The Fabric mod ID, Java packages,
configuration directory, and JAR basename remain `riserecode`.

## Features

- **Cosmetics:** `Utilities → Cosmetics` offers capes, animated wings, headwear,
  tails, companions and effects with a live wardrobe preview. Includes 26 offline
  items, an optional online catalog, PNG cape imports and profile-owned outfits.
  Cosmetics are local to your character. [Wardrobe guide](docs/cosmetics.md).

- **Legacy capes:** optional `Render → Cape Physics` restores the verified 1.8.9
  cape response for local and remote players, with adjustable motion strength and
  vanilla handling of modern poses. [Behavior and reference tests](docs/legacy-cape-physics.md).

- **Interface and Appearance:** searchable module cards, inline settings,
  focused keyboard editing, clipboard shortcuts, color/gradient pickers,
  appearance presets with Undo, branding/logo controls, fonts, blur, glow,
  independent animation channels, spring button feedback, configurable sounds,
  a background browser, and resizable windows.
- **Maps and destinations:** a detailed surface/cave minimap, fullscreen or movable panel map with smooth pan
  and cursor-centered zoom, dimension selection, and shared waypoint editing.
  Explored terrain persists per world/server and dimension. Recording uses only
  terrain received by the client. Auto / Surface / Cave modes support manual altitude
  layers; square and circular minimaps share the terrain renderer.
- **New utilities:** Nametags, Trajectories, AutoTool, FastBreak, BetterChat,
  BetterTab and Waypoints, with standard settings and Hold/Toggle bindings.
  All eight new modules, including Minimap, start disabled.
- **HUD:** Watermark, ArrayList, FPS, ping, coordinates, potion effects,
  memory, crosshair scaling, and an interactive layout editor. HUD layouts
  and module controls persist per profile.
- **Resources:** the complete publicly offered Vanilla Tweaks resource-pack picker
  for Minecraft 26.2, with categories, search, previews, overlap warnings and
  top-first priority. Cache every resource and supported combination once, then
  create new selections offline. Apply and Cancel keep browsing separate from
  active resources; selections save in existing client profiles.
- **Ambience:** a separate Render module with Aurora, Nebula, Sunset, Clouds,
  Storm and water effects, editable parameters and ordinary Hold/Toggle bindings.
  Active Iris shader packs retain control of sky and water.
- **Terrain controls:** independent environment fog and chunk-fade removal;
  ChunkAnimations can smoothly move newly loaded terrain up or down, with
  adjustable distance, duration, and easing.
- **Render and camera utilities:** FullBright, Storage/Player/Block ESP,
  Tracers, FreeCam, FreeLook, Zoom, independent hand transforms, weather,
  item physics, enchanted-item outlines, and block overlays.
- **Gameplay and automation:** Sprint, KillAura, Clutch fall controls,
  friends, commands, named profiles, and optional Baritone navigation,
  mining, and follow controls.
- **Menus:** themed start, multiplayer, options, connecting, disconnected,
  loading, and supported settings screens with native navigation.

## Installation

1. Use **Minecraft 26.2**, **Java 25**, **Fabric Loader 0.19.5 or newer**, and
   **Fabric API 0.161.0+26.2 or newer**.
2. Use the built `riserecode-0.16.1.jar`, or its matching asset from
   [Releases](https://github.com/aidentallen/risecode/releases) when available, and place it in your instance's `mods` directory.
3. Restart Minecraft. **Right Shift** opens the interface; the title screen
   also has an interface button.

Sodium, Sodium Extra, Iris, and Baritone are optional. To use Baritone controls,
install the separate official [Baritone API Fabric 1.19.0 JAR](https://github.com/cabaletta/baritone/releases/tag/v1.19.0).
Enabling the Baritone module does not automatically start a task, and saved
tasks do not resume on startup.

For custom entity models/textures, install the optional official
[EMF](https://modrinth.com/mod/entity-model-features) and
[ETF](https://modrinth.com/mod/entitytexturefeatures) Fabric 26.2 mods. EMF requires ETF.

## Controls and configuration

The small sidebar arrow expands **Friends / Profiles / Waypoints / Map settings**.
Existing friends, profiles and commands remain available. Appearance and Resources
stay in the sidebar. **M** opens the world map; **B** adds a waypoint at the player.
Both shortcuts can be changed in Minecraft Controls. The map and waypoint manager
work independently of the Minimap and Waypoints overlay toggles.

Terrain recording defaults on while connected. **Map settings** can
turn it off. Map data lives in `config/riserecode/maps/`, separately from module
profiles. World identities are hashed; dimensions have separate terrain stores.
Deaths record independently of map recording and overlay toggles. The latest death
is tracked; reached deaths remain in paged history, with hide-reached and clear controls.
Minimap placement and scale use the existing HUD editor.

AutoTool uses hotbar tools by default. **Inventory Tools** opts into ordinary
synchronized inventory swaps; restoration checks the expected items before
changing slots. FastBreak defaults to no inter-block delay with **1×** progress.
Trajectory previews show deterministic centerlines; random launch spread and
future movement of other entities can change the actual impact. BetterChat keeps
original signed entries and their formatting; duplicate counts annotate messages
without discarding originals.

Open **Resources** to browse Vanilla Tweaks. The first visit starts the full
26.2 cache download. The **Tools** menu contains **Pause / Resume**, **Stop**, and **Cache all** to control the
cache independently of your selection; completed downloads survive interruption.
Wait for **Offline ready** before applying new choices offline. **Refresh** checks
for an updated catalog; startup does not contact Vanilla Tweaks automatically.

Click cards to select or deselect, and right-click for previews, descriptions and
compatibility notices. In **Selected**, drag rows or use **Up / Down** (or
Ctrl+Up/Down on the focused row). The top resource wins overlapping files.
**Apply** changes the active pack once; **Cancel** discards the draft. Server
resource packs retain priority. Vanilla Tweaks resources that depend on a mod or
conflict with a renderer retain their original compatibility notices.

**Render → Ambience** contains the former Sky/Water controls. Its sky effects
replace resource-pack sky visuals while enabled; Iris shader packs suspend its
sky/water overrides. The old Styles catalog and custom import browser are removed;
Minecraft's ordinary resource-pack controls remain available.

Enabling **Zoom** arms its binding. Hold or toggle the key to zoom; wheel changes
last only for that activation. Screens, focus loss and binding/profile changes
release active zoom. The saved magnification starts each activation.

Right-click a module card to expand settings. Click a numeric value to type;
Enter commits, Escape cancels, and valid values preview immediately. Each module
has a Hold/Toggle keybind editor with Reset.

In **Render → NoRender**, `All Fog` removes native environment fog. Turn it off
to choose individual environments. `Chunk Fade` controls terrain loading fade
independently. **ChunkAnimations** starts disabled; its defaults are Up,
8 blocks, 0.5 seconds, and Ease-Out-Quart.

Commands start with `.` and execute locally:

```text
.help
.toggle Sprint
.bind FreeCam F8 hold
.friend add PlayerName
.profile save PvP
.profile load PvP
.hudedit
.b goto 100 64 200
```

Configuration is stored under `config/riserecode/`. Module profiles retain
settings, bindings, HUD layouts, and ordered Resources selections. Appearance profiles retain visual
preferences independently. Imported branding PNGs live under
`config/riserecode/branding/`. Watermark and ArrayList are enabled by default;
most utility/render modules start disabled.

## Build and verify

Requires Java 25 and Python 3. The checked-in Gradle wrapper pins Gradle 9.6.0;
Minecraft, Fabric API, Loader, and Loom versions are pinned in `gradle.properties`.

```bash
git clone https://github.com/aidentallen/risecode.git
cd risecode
python3 scripts/fetch-dependencies.py
./gradlew check build
```

The dependency script verifies pinned SHA-256 hashes and extracts Iris's
nested glsl-transformer library. Optional dependency JARs are compile-only;
they are not bundled in risecode. The build produces the installable JAR
at `build/libs/riserecode-0.27.0.jar` and a separate sources JAR.

```bash
./gradlew runClient
./gradlew runClientGameTest -PchunkAnimationsOnly
./gradlew runClientGameTest -PchunkAnimationsOnly -PgraphicsBackend=VULKAN
```

Real-client tests require a graphical environment. `-PinstalledStack` and
`-PwithShaderPack` are local test-fixture options that reference the maintainer's
Modrinth instance; a fresh checkout can run the vanilla tests directly.

The 0.14.0 implementation passed **711 core assertions**, plus 37 Resources
assertions on Iris/OpenGL with Complementary Unbound r5.9.3 and 35 on Sodium
Vulkan with EMF/ETF. Full-cache offline rebuilding made zero provider requests;
eight mixed selections matched official generated archives. See the
[release notes](docs/releases/v0.14.0.md) for scope. Iris 1.11.4 requires OpenGL.

## Documentation and history

- [Usage guide](docs/usage.md), [development guide](docs/development.md),
  and [rendering details](docs/gui-rendering.md).
- [Font sources and local overrides](FONT_SOURCES.md).
- [Version changelog](CHANGELOG.md) and [recovered release notes](docs/releases/README.md).

Historical releases preserve the retained binaries and source artifacts.
Their notes identify recovered information and any missing history.

## Credits

Credits:
Vanilla Tweaks: https://vanillatweaks.net/

Vanilla Tweaks assets are downloaded directly into the personal cache and are
not bundled in source or release JARs. Their [terms](https://vanillatweaks.net/terms/)
and original authors' rights apply.

## License

Original risecode code and documentation use the [MIT License](LICENSE).
Third-party fonts, the menu photograph, Gradle wrapper, and adapted Sodium
shaders retain their own terms. In particular, the Sodium-derived shader
files remain under **PolyForm Shield 1.0.0**. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for scope and attribution.
