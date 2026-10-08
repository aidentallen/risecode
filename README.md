# risecode

A modular client-side Fabric utility mod for **Minecraft 26.2**, built with
**Java 25**. It combines an animated sidebar interface, editable HUD,
appearance profiles, visual effects, and gameplay utilities.

[Download the latest release](https://github.com/aidentallen/risecode/releases/latest)
· [Changelog](CHANGELOG.md) · [Usage and upgrade guide](UPGRADE.md)

The display name is lowercase `risecode`. The Fabric mod ID, Java packages,
configuration directory, and JAR basename remain `riserecode`.

## Features

- **Interface and Appearance:** searchable module cards, inline settings,
  focused keyboard editing, clipboard shortcuts, color/gradient pickers,
  appearance presets with Undo, branding/logo controls, fonts, blur, glow,
  and independent animation channels.
- **HUD:** Watermark, ArrayList, FPS, ping, coordinates, potion effects,
  memory, crosshair scaling, and an interactive layout editor. HUD layouts
  and module controls persist per profile.
- **Ambience:** Aurora, Nebula, Sunset, Clouds, and Storm skies, plus animated
  water shading and reflections. Active Iris packs retain their sky and water.
- **Terrain controls:** independent environment fog and chunk-fade removal;
  ChunkAnimations can smoothly move newly loaded terrain up or down, with
  adjustable distance, duration, and easing.
- **Render and camera utilities:** FullBright, Storage/Player/Block ESP,
  Tracers, FreeCam, FreeLook, Zoom, independent hand transforms, weather,
  item physics, enchanted-item outlines, and block overlays.
- **Gameplay and automation:** Sprint, KillAura, local NoFall controls,
  friends, commands, named profiles, and optional Baritone navigation,
  mining, and follow controls.
- **Menus:** themed start, multiplayer, options, connecting, disconnected,
  loading, and supported settings screens with native navigation.

## Installation

1. Use **Minecraft 26.2**, **Java 25**, **Fabric Loader 0.19.5 or newer**, and
   **Fabric API 0.161.0+26.2 or newer**.
2. Download `riserecode-0.9.2.jar` from [Releases](https://github.com/aidentallen/risecode/releases)
   and place it in your instance's `mods` directory.
3. Restart Minecraft. **Right Shift** opens the interface; the title screen
   also has an interface button.

Sodium, Sodium Extra, Iris, and Baritone are optional. To use Baritone controls,
install the separate official [Baritone API Fabric 1.19.0 JAR](https://github.com/cabaletta/baritone/releases/tag/v1.19.0).
Enabling the Baritone module does not automatically start a task, and saved
tasks do not resume on startup.

## Controls and configuration

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
settings, bindings, and HUD layouts. Appearance profiles retain visual
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
at `build/libs/riserecode-0.9.2.jar` and a separate sources JAR.

```bash
./gradlew runClient
./gradlew runClientGameTest -PchunkAnimationsOnly
./gradlew runClientGameTest -PchunkAnimationsOnly -PgraphicsBackend=VULKAN
```

Real-client tests require a graphical environment. `-PinstalledStack` and
`-PwithShaderPack` are local test-fixture options that reference the maintainer's
Modrinth instance; a fresh checkout can run the vanilla tests directly.

The 0.9.2 implementation passed **663 core assertions**, real-client checks on
vanilla/Sodium OpenGL and Vulkan, and Iris OpenGL with Complementary Unbound
r5.9.3. [UPGRADE.md](UPGRADE.md) documents the verification scope. Iris 1.11.4
requires OpenGL.

## Documentation and history

- [Upgrade guide](UPGRADE.md), [render/camera controls](POLISH.md),
  [keybindings](KEYBINDS.md), [colors and outlines](COLOR_OUTLINE_FIX.md).
- [Font sources and local overrides](FONT_SOURCES.md).
- [Version changelog](CHANGELOG.md) and [recovered release notes](docs/releases/).
- [Archived development guide](docs/DEVELOPMENT_HISTORY.md) and the retained
  `PHASE*.md` source snapshots.

Historical releases preserve the retained binaries and source artifacts.
Their notes identify recovered information and any missing history.

## License

Original risecode code and documentation use the [MIT License](LICENSE).
Third-party fonts, the menu photograph, Gradle wrapper, and adapted Sodium
shaders retain their own terms. In particular, the Sodium-derived shader
files remain under **PolyForm Shield 1.0.0**. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for scope and attribution.
