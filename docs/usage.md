# Using risecode

[Home](../README.md) · [Feature guides](README.md) · [Release history](releases/README.md)

## Interface and shortcuts

**Right Shift** opens the interface from gameplay. The title screen also has an
interface button. Rebind the interface key in Minecraft Controls.
Right-click a module card to open its settings.

Click a numeric value to type an exact number or drag its slider. Valid edits
preview immediately; **Enter** commits and **Escape** restores the starting
value. Text fields support selection and **Ctrl+A/C/X/V**. Up/Down adjusts
numeric values; Shift uses a larger step.

Module settings include a keybind editor with **Hold**, **Toggle**, and **Reset**.
Toggle switches state once per press. Hold temporarily enables a disabled module
until release; a manually enabled module remains enabled. Escape cancels key
capture, and Delete/Backspace clears the binding. The interface key is reserved.
Temporary holds release when focus, menus, bindings, or profiles change.

## Appearance and HUD

Open **Appearance** for fonts, colors, gradients, blur, glow, layout, animation,
branding, and backgrounds. Presets and **Undo** let you try a palette without
rebuilding the rest of your setup. **Button Style** offers Subtle and Accent.
Per-page gears control menu and container styling and background overrides.
Choose **None** for a transparent backdrop; particles have separate controls.

Color controls support HSV, alpha, hex input, independent endpoints, and animated
paint modes. Animation channels control opening, navigation, hover, press,
toggles, expansion, scrolling, dragging, and HUD transitions. Global speed and
Reduced Motion apply across the interface.

Use the HUD editor to arrange and scale elements. Watermark and ArrayList are
separate modules. Minecraft's **F1** still hides the HUD. Appearance profiles
store visual preferences independently of module profiles.

The bottom-left settings popup contains profile/friend controls and client
settings such as sounds, command suggestions, Discord Activity, the menu key,
and GUI scale. Discord Activity is opt-in and uses your public application ID.

Fonts, local overrides, and their licenses are described in
[font sources](../FONT_SOURCES.md).

## Maps, waypoints, and death history

**M** opens the world map; **B** adds a waypoint at your location. Both shortcuts
can be changed in Minecraft Controls. The map and waypoint manager work
independently of the Minimap and Waypoints overlay toggles.

Use Auto, Surface, or Cave mode, with manual altitude layers when needed. The
world map supports fullscreen and movable panel layouts, pan, zoom, and dimension
selection. The minimap uses the HUD editor for placement and scale, with circular
or square presentation.

Terrain recording uses only information received by the client and defaults on
while connected. **Map settings** can turn it off. Exploration persists per
world/server and dimension under `config/riserecode/maps/`, separately from
module profiles. Death history records independently of terrain recording and
overlay switches, retaining reached destinations and the latest death per dimension.

## Resources

Open **Resources** to browse the Vanilla Tweaks catalog. The first visit starts
the Minecraft 26.2 cache download. **Tools → Pause / Resume**, **Stop**, and
**Cache all** control downloads independently of your selection; completed files
survive interruption. Wait for **Offline ready** before making new selections
offline. Refresh checks for a newer catalog; startup does not contact the provider
automatically.

Click cards to select or deselect them and right-click for previews, descriptions,
and compatibility notices. In **Selected**, drag rows or use Up/Down; the top
resource wins overlapping files. **Apply** activates the selection once, and
**Cancel** discards the draft. Server resource packs retain priority.

Downloads remain in your personal cache and retain Vanilla Tweaks attribution.
Resource-specific mod requirements and renderer notices still apply.

## Render and camera controls

**Ambience** provides configurable sky and water effects. Active Iris shader
packs retain control of their sky and water. The old Styles browser was replaced
by Resources in 0.14.0; ordinary Minecraft resource-pack controls remain available.

**NoRender** has independent environment-fog controls and **Chunk Fade**.
Turn off All Fog to choose individual environments. Iris keeps its shader-pack
fog path. **ChunkAnimations** animates newly loaded terrain, with direction,
distance, duration, and easing controls. It starts disabled. Collision remains
at the native position, and ordinary mesh rebuilds do not replay the animation.

**BlockAnimations** animates supported placed blocks. **Block Overlay** can
follow the moving shape, wait for it to settle, or remain fixed. Outlines support
smooth switching, corner gaps, pop animations, and an explicit see-through option.
**Hitboxes** offers Only When Looking and Always modes, with independent filters
and optional BlockOverlay style inheritance.

Enabling **Zoom** arms its Hold/Toggle binding. Mouse-wheel changes last for that
activation; releasing zoom restores the normal camera. Menus, focus loss, and
binding/profile changes release active zoom. FreeLook and FreeCam have their own
module controls. **Clutch** is the current name for the former NoFall module;
the [0.21.0 notes](releases/v0.21.0.md) describe its migration.

## Glass, scenery, and cosmetics

- [Glass](glass.md) — target switches, material presets, inheritance, and previews.
- [Living Panorama](living-panorama.md) — title-screen scenery, atmospheres,
  interaction, and ambient sound.
- [Cosmetics](cosmetics.md) — wardrobe, companions, effects, and PNG cape imports.
- [Legacy cape physics](legacy-cape-physics.md) — motion controls and modern poses.

## Performance

The **Performance** sidebar has Off, Conservative, Balanced, Strong, and Custom
modes with individual controls. Off preserves the selected effects. World
resolution is independent, with Bilinear or AMD FSR1 upscaling and sharpness
controls; 100% bypasses scaling. The HUD and interface remain at native resolution.
Compatibility fallbacks retain requested settings.

See [GUI rendering](gui-rendering.md), [performance evidence](remediation.md), and
the [0.25.0 notes](releases/v0.25.0.md) for behavior and measurement limits.

## Baritone and gameplay utilities

Install the separate official [Baritone API Fabric 1.19.0
JAR](https://github.com/cabaletta/baritone/releases/tag/v1.19.0) for Baritone
controls. Without it, the module reports that the dependency is unavailable.
Choose a destination, mine, follow, or command task and press Start.
Enabling the module alone starts nothing. Pause, Resume, and Stop control the
task; disable, disconnect, and profile changes cancel work. Saved tasks never
resume automatically.

AutoTool uses hotbar tools by default. **Inventory Tools** opts into synchronized
inventory swaps. FastBreak defaults to 1× progress with no inter-block delay.
Trajectory previews show deterministic centerlines; random spread and future
entity movement can change the impact. BetterChat retains original signed
entries and annotates duplicate counts without discarding them.

## Local commands

Commands begin with `.` and are handled locally. Use `.help` for the full list.

```text
.toggle Sprint
.bind FreeCam F8 hold
.bind Sprint NONE
.friend add PlayerName
.profile save MySetup
.profile load MySetup
.hudedit
.b goto 100 64 200
```

## Configuration and upgrading

Configuration lives under `config/riserecode/` in your game directory. Module
profiles retain enabled states, settings, bindings, HUD layouts, and ordered
Resources selections. Appearance profiles retain visual preferences separately.
Explored map terrain stays outside profiles. Imported branding assets live under
`config/riserecode/branding/`; optional local font overrides use `fonts/`.

Replace the matching riserecode mod JAR when updating and restart Minecraft.
Keep your configuration and imported assets. Version-specific migrations and
compatibility notes are listed in the [release history](releases/README.md).
