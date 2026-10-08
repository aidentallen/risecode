# Typography fix — 0.6.1

The October 6 screenshots showed broken/hairline Montserrat strokes and rigid
text when the missing Minecraftia option fell back to Inter.

## Changes

- Normal TrueType text now uses filled STB glyph bitmaps rasterized at 8× logical
  resolution. Four bilinear samples integrate each screen-pixel footprint, keeping
  diagonal strokes antialiased at fractional interface and font scales.
- Actual font advances, kerning, baseline and fractional positioning are retained.
  The normal pass uses glyph coverage directly instead of reconstructing fill
  from the old coarse signed-distance field.
- Soft shadows/glow retain their separate distance-field atlas and live controls.
  Both R8 atlases are cached per face; theme edits upload no glyph textures.
- Nearest/pixel sampling follows the loaded face. Missing or invalid Minecraftia
  uses smooth Inter; a requested pixel face cannot pixelate its fallback.
- HUD and ClickGUI share this renderer. Theme preferences and module behavior are
  unchanged. Bundled font files and their licenses are unchanged.

## Files

- `src/main/java/dev/riserecode/ui/font/CustomTextRenderer.java`
- `src/main/resources/assets/riserecode/shaders/core/font.fsh`
- `src/gametest/java/dev/riserecode/FontIntegrationTest.java`
- `src/gametest/java/dev/riserecode/ThemeIntegrationTest.java`
- `src/gametest/java/dev/riserecode/ClientIntegrationTest.java`
- `gradle.properties` (0.6.1), `README.md`, `FONT_SOURCES.md`

## Verification

Run with Java 25 and the cached project toolchain:

```bash
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew build runClientGameTest --offline --no-daemon
```

The native font regression uses Minecraft 26.2/OpenGL at 2560×1440 with interface
scale 1.11, reproducing the reported typography settings. It captures Google Sans,
Montserrat and Inter, font scale 1.37 with shadows disabled, a deliberately missing
Minecraftia file and its Inter fallback, and the in-game HUD. Only the disposable
test instance's optional font is temporarily hidden and restored.

The existing 273 backend/UI/theme checks and native keyboard/mouse, blur, font
picker and resource-reload tests remain required. Evidence belongs in
`build/verification/font-fix/`.

The installable file is `build/libs/riserecode-0.6.1.jar`. Minecraft must restart
when replacing a loaded mod JAR. Optional Qraftys/Minecraftia files remain local;
see [FONT_SOURCES.md](FONT_SOURCES.md) for their installation.

## Verified result and installation

`build runClientGameTest` passed on October 6, 2026: all 273 existing assertions,
the complete native client suite, the added font regression, and 30 screenshots.
The Montserrat and missing-Minecraftia images were inspected at native resolution.
The release contains all three bundled TrueType families and excludes optional
Minecraftia/Qraftys assets and test fixtures.

The verified JAR was installed into the Modrinth **Fabric 26.2** instance. Its old
0.6.0 JAR is backed up under `riserecode-backups/font-fix-20261006-200410/`. Checksums
confirm all 11 other mods and the user's theme JSON are unchanged. The local
Minecraftia font/readme and the requested Qraftys resource pack/original ZIP were
also copied into that instance so both optional choices can load. Restart Minecraft
to load the new mod. `build/verification/font-fix/installation.json` records the paths
and checksums.

Release SHA-256: `e20624b1f723653eae945bffaa91120d12081f45346c91dc01cdbab077b1ea5d`.
