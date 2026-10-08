# risecode 0.8.0

Minecraft 26.2, Java 25, Fabric. All new modules start disabled and use the existing
module keybind editor, Hold/Toggle modes, Reset button, config and profiles.

## Controls

- **Themes → Interface Colors → Selected Tab Color** defaults to pure white.
  This color controls the selected sidebar label/icon, independently of hover.
- The client Language sidebar page is removed. Minecraft's native Language
  option remains available under Options.
- **Themes → HUD → HUD Border** enables the interface border style on HUD
  backgrounds. In the HUD editor, each element has **Border: Inherit / On / Off**.
  Borders use the existing Border Color paint, thickness and opacity, and are
  omitted for a background set to None.
- Right-click any color picker's header/swatch to expand or collapse it. Left
  click also opens/closes it. This applies to module, theme and HUD editor colors.
- **ItemPhysics** adds RandomYaw, RotationX/Y/Z, Spin (Off/Airborne/Always),
  SpinAxis and SpinSpeed in degrees/second. Its default grounded pose and airborne
  spin are preserved. Transforms affect rendering only.

## New modules

| Module | Category | Controls |
| --- | --- | --- |
| StorageESP | Render | Box/Outline, Color, Width, Range, VisibleOnly, Minecarts |
| PlayerESP | Render | Box/Outline, Color/FriendColor, Width, Range, VisibleOnly, IncludeInvisible |
| BlockESP | Render | Box/Outline, Color, Width, Range, TargetLimit, VisibleOnly, searchable Blocks list |
| Crosshair | HUD | Native reticle/attack-indicator Scale, 0.25–4 |
| ViewModel | Render | Independent Left/Right XYZ position, XYZ rotation and uniform Scale |
| Zoom | Render | Hold C by default, Magnification, ScrollAdjust, ReduceSensitivity |

ESP defaults to through-wall rendering; VisibleOnly depth-tests against terrain.
Storage includes loaded Container block entities, ender chests, lecterns, chest
minecarts and hopper minecarts. Player ESP excludes the local player, dead and
spectator players, colors friends separately, and excludes invisible players
unless enabled. Block ESP starts with all vanilla ores and ancient debris.
Click **Blocks: … selected** to search registry identifiers and toggle selections.
Unavailable mod block identifiers remain in saved profiles.

Crosshair is a native centered reticle, not a draggable HUD text element. Scale 1
preserves its native size. ViewModel transforms physical left/right item poses,
including maps, without changing bare arms or world items. Zoom does not write
the user's FOV or sensitivity options. Scroll multiplies magnification by 1.15
per notch and is consumed before the hotbar handles it. The camera service keeps
the zoom-out animation alive after the module is disabled or the hold is released.

## Native menu themes

**Themes → Menu Screens** has separate Theme and Background switches for:
Start Screen, Multiplayer, and Options. Theme Off restores native rendering.
Background Off keeps themed controls over the native backdrop. Options styling
also covers its controls, language, resource packs, telemetry and credits pages.
Native button actions, sliders, server editing, input fields, focus, narration,
tooltips and navigation remain Minecraft's implementations.

The background is an appropriately credited Jefferson Sees night lake/mountain
photo under the Unsplash license; see
`src/main/resources/assets/riserecode/textures/gui/CREDITS.txt`. The bundled PNG
is a format conversion of the downloaded photo. Crop/tint happen at runtime.
Three cached half-resolution targets hold the photo and its separable Gaussian
blur. Resize, texture reload or blur-radius changes rebuild the cache; ordinary
frames reuse it. Background Darkness is applied immediately during composition.
These glass surfaces sample the photo's own blur, never a stale world framebuffer.

## Engine and performance

- Modules still own and clean up synchronous EventBus subscriptions.
- `LoadedBlockIndex` observes client chunk load/drop and block changes. It never
  requests chunks. Palette rejection skips unrelated sections; a tick processes
  at most two sections and checks a 1 ms scan deadline between sections. This is
  a budget, not a hard limit on one section's execution time. Block comparisons
  use resolved registry objects. The nearest snapshot is bounded to 1024 entries
  and rebuilds when data or the player's block position changes. The index does
  no target work with both block and storage ESP disabled.
- The three ESP modules share `SilhouetteOutlineRenderer`: independent union
  masks per paint/depth policy, original native model/atlas alpha, full-resolution
  edge fidelity, two-pass dilation and explicit rejection of mask interiors.
  Box mode uses an explicit through-wall or native depth-tested line pipeline.
- Visible masks copy terrain depth at their first actual native feature draw.
  Submit collection happens before terrain rendering, so copying during
  collection would read cleared depth and incorrectly show outlines through walls.
- Targets are pooled across color/width edits and composite passes use projected
  bounds as scissors. Enchant item outlines keep their adapter and the same
  exterior-only shader. Its world-item depth copy uses the corrected draw timing.
- HUD names/watermark metrics and ArrayList sorting are invalidated by membership,
  font metrics, resource reload or localization changes. Glyph runs cache original
  kerning and advances in a bounded 2048-entry LRU and share layouts across text,
  shadow and glow passes. HUD module content snapshots rebuild on live data or
  settings changes. Animated paints still sample every frame; hidden elements
  skip content extraction. The existing filled 8× font coverage/SDF shadow policy
  and animation transitions are preserved.
- Rendering/animation/index work stays on the game/render thread. No worker
  threads are introduced for GPU work.

## Persistence and verification

Theme colors, HUD Border and menu family switches are in `config/riserecode/theme.json`
and theme profiles. Module controls/keybind modes and Blocks arrays are in the
main config and module profiles. Per-element HUD Border is stored with each HUD
layout; old layouts default to Inherit. Existing unknown module/setting data and
unavailable block identifiers remain preserved.

Run `scripts/verify.sh --offline --no-daemon` for the release build and CPU checks.
Run `./gradlew runClientGameTest -PpolishOnly --offline --no-daemon` for the focused
native OpenGL fixture, or omit `-PpolishOnly` for all client regressions. Native
fixtures and their screenshots are excluded from release JARs. The verification
report and final screenshots are under `build/verification/polish/`.
