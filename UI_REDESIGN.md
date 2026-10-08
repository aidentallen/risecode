> Historical UI redesign snapshot. Phase 7 supersedes its font, theme persistence,
> and rendering sections; see [PHASE7.md](PHASE7.md) for the current implementation.

# risecode UI redesign

Primary reference: the in-game 6.130 interface in screenshots 2–4 supplied on
2026-10-05. Visible branding follows the requested lowercase **risecode** name.
The code and icons are original implementations; the project does not import
upstream client classes or copy their assets/source.

## Geometry and appearance

The canonical window is 428 × 320 units with a 107-unit sidebar, 10-unit outer
corner radius, 8-unit content padding, 5-unit card radius, and 8-unit card gaps.
The supplied full screenshots are **2560 × 1440**. Their approximately 1068 × 800
window provides the measured basis. The reference viewport maps to 576 logical
units of height, producing a 1070 × 800 window at that resolution and 856 × 640
at 2048 × 1152. This reference density is independent of Minecraft's GUI scale;
window and HUD proportions follow the viewport height. The fit guard also handles
narrow aspect ratios. Themes supports 85%, 100%, or 115% of reference scale.
World geometry stays clear around the window.

The background shades run from `#1E222C` to `#181F25`; the sidebar runs from
`#171F22` to `#141C1E`. Cards use `#111820`; primary text uses `#F0F2F3`, descriptions
use muted gray, and the default accent is `#59D5CB`. Selected navigation gets a
compact softly glowing pill; enabled modules use accent titles. Inline settings
follow the reference's flat arrangement rather than separate nested widget boxes.
The HUD uses snug dark rectangles and teal text, aligned to the right edge.

Typography uses static Roboto Regular/Medium from Google's original repository:
https://github.com/googlefonts/roboto-2. The bundled Apache 2.0 license accompanies
both font files. The exact original reference font/rendering assets were not supplied;
this is a measured recreation using licensed fonts and the client's actual module
catalog, with World/Utility/Profiles/Friends navigation tied to existing functionality.

## Interaction

- Right Shift opens or closes the screen; Escape closes it.
- Left click a module title/description area to toggle; right click to expand settings.
- Click a boolean row, drag a numeric slider, left/right click an enum, or click a color.
- Drag the logo area to move the window. Scroll the card viewport with the wheel.
- Search by name, description, or category. Ctrl+F focuses it; Tab/Shift+Tab cycles pages.
- Search/name input supports Unicode editing, selection, clipboard shortcuts, and a pulsing caret.
- Friends: type a player name and press Enter/Add; click a saved name to remove it.
- Profiles: type a name and press Enter/Save; click a saved name to load it.
- Themes: choose an accent, interface scale, and HUD visibility.
- Language: select English/German navigation labels. Engine identifiers remain unchanged.

All feature settings and profiles use the existing managers. UI preferences live
under `ui` in `config/riserecode/config.json` and survive restart. Profiles preserve
global friends, theme, language and window placement.

## Motion and rendering

Window entrance/exit fades and scales 0.94 → 1.0 using OutQuart (260 ms). Window
position follows a retargetable 120 ms OutQuart animation. Selection position/width,
list scrolling, settings height/row positions, value transitions, and scale changes
ease independently. Hover/active colors and theme color blending use InOutCubic.
Action buttons have a small press/release scale response. Changes can be reversed
mid-transition without jumping back to their initial state. Right Shift can reverse
a screen exit while it is still in progress.

HUD entries slide in for 320 ms and out for 220 ms. Reordering uses 240 ms OutQuart
Y movement; exiting entries are retained until their visibility reaches zero.
Accent blending shares a monotonic clock between HUD/screen rendering.

The 26.2 GUI extractor records texture/fill elements and their pose/scissor state.
Rounded shapes and shadows use cached antialiased nine-slice masks. The shaded window
is uploaded once. Two 1024 × 1024 glyph atlases contain supersampled Latin characters
and common UI punctuation; linear filtering preserves smooth coverage during motion.
Characters outside that atlas use Minecraft's regular font pipeline/fallback.
GPU textures are owned by Minecraft's TextureManager. There are no per-frame image
uploads, custom framebuffer captures, raw OpenGL calls, or additional mixins.

Scissors are nested: window content viewport → expanded card body. The extractor
intersects them after applying the current pose. Input reverses the same transforms,
and off-screen/hidden controls reject clicks. Captures release on mouse-up, navigation,
visibility changes, profile refresh, and screen removal.

## Validation

```bash
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew build runClientGameTest --offline --no-daemon
```

The test uses real OpenGL rendering, Minecraft mouse/keyboard input, an isolated
world, and test-only fixtures. It covers module toggles/restoration, settings,
conditional visibility, search, bounded scrolling, drag/resize, screen exit/reopening,
HUD sorting/exit, animated themes/scaling, friend/profile GUI actions, and the existing
command/network/targeting/rotation/combat integration checks. Test classes do not enter
the release JAR. Proof screenshots/logs are retained under `build/verification/ui-redesign`.
