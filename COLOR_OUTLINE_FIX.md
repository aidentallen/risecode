# Colors and held-item outlines — 0.7.1

Every module color now opens the same full color picker. Right-click a module to
expand its settings, then click a color row. Themes uses the same control for
accents, interface backgrounds, surfaces, hover colors, text, borders, panel/text
shadows, watermark, ArrayList and HUD backgrounds. HUD Editor context panels also
have a per-element background picker.

The picker provides:

- Start and End colors, an HSV square, hue strip and independent endpoint opacity.
- Exact `RRGGBB` or `AARRGGBB` hex input, with clipboard/select-all support.
- Solid, Gradient, Rainbow, Astolfo, Sine and Theme modes. Click Mode to advance;
  right-click it to go backwards. Theme follows the global accent policy.
- Horizontal/vertical direction, animation speed and spacing. Speed zero makes
  the gradient stationary. Changes apply immediately and keep the existing UI
  animation and font systems.

FPS and memory retain their existing Static/Threshold/Gradient color policies.
Static uses the local picker, which can itself supply an animated gradient;
Threshold uses the corresponding customizable threshold color; Gradient follows
the theme. Tracers and BlockOverlay sample their colors per vertex, so their
configured gradients also affect world rendering.

## Outline correction

EnchantEffect writes the original held-item geometry and atlas alpha into an
independent transparent mask. Two separable dilation passes expand that silhouette;
the final compositor explicitly rejects every original mask pixel. It therefore
draws the item's exterior border without recoloring its faces or drawing internal
quad edges. This replaces the previous expanded-geometry effect.

Outline Width is measured in framebuffer pixels. Outline Intensity controls
opacity; Outline Glow is optional and defaults to zero. Overlay is the independent
glint customization and now defaults off. Existing saved choices are preserved.
For the requested outline-only appearance, keep Outline on and Overlay off.

Mask/temporary textures are reused until the framebuffer size changes. Held-item
postprocessing is scissored to the projected item bounds, including the border/glow
margin. There are no framebuffer readbacks or OS rendering threads in gameplay;
readbacks exist only in the test fixture. GPU buffers and textures close at shutdown.
The implementation uses Minecraft 26.2 Blaze3D render pipelines, consistent with
[Fabric's rendering guidance](https://docs.fabricmc.net/develop/rendering/basic-concepts).

## Config compatibility

Module and HUD background paint policies save/load with the existing configuration
and named profiles. Visual palette policies save in the existing separate theme
JSON and named theme profiles. Global gradient/animation policy keeps its existing
module-profile integration.

Each color stores `color`, `secondary`, `mode`, `direction`, `speed`, and `spacing`.
Legacy integer colors and six/eight-digit hex strings still load. Missing fields
use the declared default paint; reset restores both endpoints and all paint options.
Profile/theme validation completes before application to prevent partially loaded
invalid documents. Installation preserves the user's existing config files.

## Verification

Run with Java 25:

```bash
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew build runClientGameTest --offline --no-daemon
```

The CPU suite covers color/alpha interpolation, independent gradient axes and
clocks, immutable render snapshots, config/profile/theme round trips, old color
migration and strict invalid-profile handling, alongside the existing checks.
Native OpenGL tests compare baseline and outlined diamond axes and swords,
verify unchanged item interiors and a visible exterior border, exercise real
picker mouse/keyboard input and restore a complete color policy through profiles.
The full suite also exercises fonts, blur, combat integration, cameras, FullBright,
world highlights and HUD editing. Evidence is in `build/verification/color-outline`.

Verified release: 345 CPU assertions, 27 new native OpenGL assertions and 36
existing Phase 8 native assertions passed, along with the earlier integration
and font regressions. The final axe/sword comparisons found zero changed interior
pixels across 246,340 and 301,796 sampled interior pixels respectively. Version
0.7.1 was installed into the Fabric 26.2 instance after backing up 0.7.0; the
installation manifest verifies all 11 unrelated mod files, 7 config/font files
and the resource pack were unchanged.

![Whole-item outline](build/verification/color-outline/outline-silhouette-diamond_axe.png)
![Custom gradient outline](build/verification/color-outline/outline-custom-gradient.png)
![Shared custom color picker](build/verification/color-outline/module-custom-color-picker.png)
