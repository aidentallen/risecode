# Changelog

Recovered from retained source/JAR artifacts and development documentation.
Historical entries do not invent missing release dates or Git commit history.

## [0.9.2](docs/releases/v0.9.2.md) — Chunk fade and terrain animations

- Added independent NoRender Chunk Fade removal, without coupling it to atmospheric fog controls.
- Added disabled-by-default ChunkAnimations with Up/Down, distance, duration, and Ease-Out-Quart / Ease-In-Out-Cubic / Elastic-Out controls.
- Shared one first-drawable clock per chunk column; ordinary rebuilds do not replay movement, active flights capture their settings, and block entities follow terrain.
- Added native/Sodium GPU transforms, culling and face-mask corrections, Ambience water composition, Iris terrain/shadow support, and Sodium Extra shader-interface compatibility.
- Verified 663 core assertions, vanilla/Sodium OpenGL and Vulkan clients, Iris OpenGL with Complementary Unbound, and static screenshot comparisons.
- Repository publication adds an original-code MIT license, preserved third-party notices, public build dependency retrieval, and reconstructed historical release notes.

## [0.9.1](docs/releases/v0.9.1.md) — NoRender environment fog controls

- Added the disabled-by-default NoRender module under Render.
- Added All Fog and independent Overworld, Nether, End, other-dimension, water, lava, powder-snow, blindness, and darkness controls.
- Persisted controls through module configs/profiles and restored native fog on disable; Iris retains its shader-pack fog path.
- Preserves the original installed release JAR rather than the later build-folder rebuild of the same version.

## [0.9.0](docs/releases/v0.9.0.md) — Ambience, Appearance, and rendering upgrade

- Added procedural Ambience skies and animated water with native/Sodium adapters and Iris shader-pack handoff.
- Added optional official Baritone controls, command forwarding/completion, missing-dependency status, and safe task lifecycle.
- Added focused typed settings, branding/PNG logos, Appearance presets with Undo, Watermark/ArrayList HUD modules, and independent animation channels.
- Expanded native-menu theming, loading controls, title-screen interface access, and uncapped themed menus.
- Added shader-sampled gradients, rendering/layout caches, and blur/resource reuse. Historical performance evidence is documented in the upgrade guide.

## [0.8.0](docs/releases/v0.8.0.md) — Render, camera, HUD, and menu polish

- Added StorageESP, PlayerESP, BlockESP, Crosshair scaling, independent hand ViewModel transforms, and Hold-C Zoom.
- Added selected-tab colors, HUD borders, collapsible color pickers, and configurable dropped-item rotation/spin.
- Added vector sidebar icons, text/HUD caching, and independent Start/Multiplayer/Options theming.
- Recovered the original installed JAR from its backup and the preserved source-0.8.0 snapshot. Later overwritten build-folder copies are deliberately excluded.

## [0.7.2](docs/releases/v0.7.2.md) — Module keybind editor

- Added per-module inline key capture, Hold/Toggle activation, and Reset.
- Added exclusive capture, reserved GUI-key validation, repeat isolation, and cleanup on focus loss, menus, disconnects, and rebinding.
- Persisted bindings/modes in config and profiles; migrated FreeLook to the shared Hold binding.

## [0.7.1](docs/releases/v0.7.1.md) — Shared color controls and item outlines

- Added the shared HSV/alpha/hex color picker and independent gradients for module, interface, HUD, border, and shadow colors.
- Added Solid, Gradient, Rainbow, Astolfo, Sine, and Theme paint modes with direction, speed, and spacing controls.
- Corrected enchanted held-item outlines to an exterior-only silhouette mask with independent optional glint overlay.

## [0.7.0](docs/releases/v0.7.0.md) — Expanded HUD, camera, and visual modules

- Added concurrent frame animations, seven editable HUD elements, per-profile HUD layouts, and animated gradients.
- Added FreeCam, FreeLook, WeatherChanger, ItemPhysics, EnchantEffect, and BlockOverlay.
- Added the full lightmap FullBright mode with native restoration and retained a configurable gamma alternative.

## [0.6.1](docs/releases/v0.6.1.md) — Typography rendering fix

- Replaced normal-text SDF reconstruction with filled STB glyph coverage rasterized at 8x logical resolution.
- Preserved advances, kerning, baseline, and fractional positioning; shadows/glow keep their separate cached distance-field atlas.
- Corrected missing Minecraftia fallback so Inter uses smooth sampling rather than a requested pixel-font mode.

## [0.6.0](docs/releases/v0.6.0.md) — Live theme and typography editor

- Added a live Themes editor for fonts, HSV accents, glow, geometry, animation, panel/world blur, and HUD styling.
- Added independent theme/profile persistence and renderer-owned TrueType/bitmap font support.
- Added shader-backed shape, font, and blur rendering while retaining the sidebar layout.

## [0.5.0](docs/releases/v0.5.0.md) — Sidebar interface redesign

- Rebuilt the interface around the reference 4:3 window, quarter-width sidebar, teal navigation, and compact inline module settings.
- Added viewport-based layout density, draggable/clipped content, consistent sidebar pages, and compact right-aligned HUD rows.
- Retained lowercase risecode branding and existing engine/module controls.

## [0.4.0](docs/releases/v0.4.0.md) — Advanced combat engine

- Added shared target selection/filtering, friend policies, priority rotation ownership, attack scheduling, and weapon selection.
- Expanded KillAura with configurable timing, rotation, target sorting, visibility, switching, and pause controls.
- Preserved the existing settings, UI/HUD, profiles, packet hooks, and module subscription lifecycle.

## [0.3.0](docs/releases/v0.3.0.md) — Core engine and gameplay utilities

- Added Sprint, KillAura, Tracers, and local-state NoFall alongside FullBright.
- Added local command routing, friends, named profiles, validated configuration application, and GUI/profile refresh.
- Adapted raw keyboard, packet, and attack hooks into the EventBus; NoFall does not prevent server-side fall damage.

## [0.2.0](docs/releases/v0.2.0.md) — Animation clock correction

- Removed the 0.1-second elapsed-time clamp from ClickGUI and HUD animation updates.
- Animations advance using the actual frame interval, preserving elapsed time during long frames.
- Recovered by comparing the retained 0.1.0 and 0.2.0 source JARs; no separate contemporaneous changelog survives.

## [0.1.0](docs/releases/v0.1.0.md) — Earliest retained Fabric client

- Java 25 / Minecraft 26.2 client foundation with settings, synchronous EventBus, module lifecycle, and retargetable animations.
- Retained build includes the animated category/card ClickGUI, search, watermark/ArrayList HUD, and FullBright module.
- This is the earliest surviving binary/source pair; the original development history predates Git and cannot be reconstructed fully.

