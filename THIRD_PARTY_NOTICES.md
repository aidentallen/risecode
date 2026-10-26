# Third-party notices

The root [MIT license](LICENSE) covers original risecode code and documentation,
including the original code in the recovered historical releases. Third-party
files retain their own licenses; they are not relicensed by the root license.

| Component | Files / handling | License and provenance |
| --- | --- | --- |
| Sodium terrain shaders | `src/main/resources/assets/riserecode/shaders/core/{chunk_sodium.vsh,water_sodium.vsh,water_sodium.fsh}` and `shaders/include/sodium/` | [PolyForm Shield 1.0.0](src/main/resources/licenses/sodium-shaders-LICENSE.md), CaffeineMC contributors. [Upstream](https://github.com/CaffeineMC/sodium). Adaptations are described in [SHADER-NOTICES.txt](src/main/resources/licenses/SHADER-NOTICES.txt). The noncompetition restriction remains applicable to these files. |
| Google Sans | `font/google_sans_*.ttf` | SIL Open Font License 1.1; [license](src/main/resources/licenses/google-sans-OFL.txt) and [copyright notice](src/main/resources/licenses/google-sans-NOTICE.txt). |
| Montserrat | `font/montserrat_*.ttf` | [SIL Open Font License 1.1](src/main/resources/licenses/montserrat-OFL.txt). |
| Inter | `font/inter_*.ttf` | [SIL Open Font License 1.1](src/main/resources/licenses/inter-OFL.txt). |
| Roboto | `font/roboto_*.ttf` | [Apache License 2.0](src/main/resources/licenses/roboto-LICENSE.txt). |
| Menu photograph | `textures/gui/` background image | Jefferson Sees, [original photograph](https://unsplash.com/photos/a-night-time-view-of-a-lake-and-mountains-A8zGDWwiA1U), [Unsplash License](https://unsplash.com/license). [Credits](src/main/resources/assets/riserecode/textures/gui/CREDITS.txt). |
| Minecraft shader interfaces | `shaders/core/water_vanilla.*` | Minecraft is copyright Mojang AB; these adapters require Minecraft. [Shader notices](src/main/resources/licenses/SHADER-NOTICES.txt). No Minecraft executable is redistributed. |
| Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | Gradle contributors, Apache License 2.0; upstream notices in wrapper scripts. [Upstream license](https://github.com/gradle/gradle/blob/master/LICENSE). |

Font paths in this table are relative to
`src/main/resources/assets/riserecode/`. [FONT_SOURCES.md](FONT_SOURCES.md)
records exact versions, hashes, and additional provenance.

Baritone, Sodium, Iris, and glsl-transformer are separate compile-time
dependencies fetched from their upstream distributions. Their binaries are
excluded from Git and are not embedded in risecode's JAR. Baritone's optional
runtime API JAR is obtained from its [official release](https://github.com/cabaletta/baritone/releases/tag/v1.19.0).
Minecraftia assets remain local and are excluded from both the
repository and release artifacts.

Historical JARs are preserved byte-for-byte. Their accompanying release notices
and original embedded license files describe the applicable third-party terms;
adding the original-code MIT license does not replace those terms.

## Vanilla Tweaks resources and optional companions

Credits:
Vanilla Tweaks: https://vanillatweaks.net/

Resources downloads the public Minecraft 26.2 catalog, images and resource packs
directly from Vanilla Tweaks for the user's personal offline cache. Original
Vanilla Tweaks assets are not included in source control or release JARs and are
not relicensed by risecode's MIT license. The source packs and their notices remain
in the cache; generated personal packs include `credits.txt` and retain per-pack
credit/license text. See https://vanillatweaks.net/terms/ for Vanilla Tweaks' terms.
The Resources UI, cache and offline composition code are original risecode code.

Entity Model Features and Entity Texture Features are optional separate mods
by Traben-0, licensed LGPL-3.0-only. Their official distributions are used for
compatibility tests and may be installed alongside risecode; their code is not
incorporated into this release.

The previous Bauloo catalog integration and imported-sky renderer are removed in
0.14.0. Old personal imports/cache files are archived locally rather than shipped.

## Simple Map helpers

`map/MapPageLayout.java` and the pure density functions in `map/MapLodPolicy.java`
are adapted from [Simple Map at revision 564eb2ec950d5db80b4534c9d0d794ef050ee838](https://github.com/hungbbdzz/Simple-Map/tree/564eb2ec950d5db80b4534c9d0d794ef050ee838),
Copyright (c) 2026 Velorise, under the [MIT license retained from that revision](src/main/resources/licenses/simple-map-MIT.txt).
The Java package was changed, and memory-budget/cave-specific dependencies were
removed from the density helper. Viewport selection uses risecode's fixed cache
budget and persisted power-of-four overview levels. The original license is included in the release
JAR. These adaptations use the pinned revision's terms, rather than the current
main branch. Minecraft 26.2 recording, persistence, rendering, waypoints and UI
integration are implemented within risecode.

## Original interface additions

The scenic, space and abstract background images and pop, soft-pop and blob
interface tones added in 0.16.0 are original procedural artwork/audio, covered by
the project's MIT license. The existing lake image retains its Unsplash credit in
`assets/riserecode/textures/gui/CREDITS.txt`. Imported local images, sounds and
optional Minecraftia overrides stay in the user's configuration directory. Google Sans and Qraftys are bundled for offline use.

### Dual filtering reference and original UI sounds

The backend-neutral blur uses an original implementation of the dual-filtering technique described in Arm's [post-processing discussion](https://developer.arm.com/community/arm-community-blogs/b/mobile-graphics-and-gaming-blog/posts/post-processing-effects-on-mobile-optimization-and-alternatives). No external blur source is bundled. Tick, Tap, Mechanical, Wood and Glass are original synthesized audio, reproducible with `scripts/generate-ui-sounds.py`, distributed under the project license.

## Cosmetics assets and model loading

- Quaternius, **Cute Animated Monsters Pack**: CC0-1.0,
  https://quaternius.com/packs/cutemonsters.html. Models, PNG textures and source
  skeletal animations are adapted into self-contained GLB companions. The source
  preparation script pins the mirror commit and embeds original file hashes.
- Kenney, **Particle Pack**: CC0-1.0, https://kenney.nl/assets/particle-pack.
  Selected sprites are redistributed unchanged; the original license is included
  under `assets/riserecode/cosmetics/`.
- JglTF model/implementation libraries 2.0.4 by Marco Hutter: MIT,
  https://github.com/javagl/JglTF. Only the parser/model libraries are included.
- Jackson core/databind 2.22.4 and annotations 2.22: Apache-2.0,
  https://github.com/FasterXML/jackson. Their original notices and third-party
  license files are retained under `licenses/`.

Original risecode wearable geometry, cape patterns and cosmetic behavior use
the project's MIT license. Runtime imports remain user-supplied local assets.

### Qraftys Capitalized Font

Original bitmap by Qraftys, bundled with the redistribution permission confirmed for this release. Glyph artwork unchanged; see `licenses/qraftys-NOTICE.txt` for the pinned SHA-256. Explicit local overrides remain supported.

### AMD FidelityFX Super Resolution 1

AMD EASU and RCAS reference shaders, MIT license, pinned to `a21ffb8f6c13233ba336352bdff293894c706575` from https://github.com/GPUOpen-Effects/FidelityFX-FSR. Original headers and checksums in `shaders/include/fsr1`; integration wrappers by RiseRecode. See `licenses/FSR1-MIT.txt`.

## SeedCracker integration (0.28.0)

The structure finder patterns and reversal algorithms in
`src/main/java/dev/riserecode/seedcracker/` derive from
[SeedCrackerX 2.16.1](https://github.com/19MisterX98/SeedcrackerX/tree/b72a829f768967fa737a4f1f63b32e37b9619152),
commit `b72a829f768967fa737a4f1f63b32e37b9619152`. Copyright (c) 2020
KaptainWutax; SeedCrackerX is maintained by 19MisterX98 and contributors.
Distributed under MIT; complete notice: `licenses/seedcracker/SeedCrackerX-MIT.txt`.
Shipwreck recognition retains upstream's reference to skyrising's casual-mod.

SeedFinding libraries retain their upstream packages and pinned versions listed in
`dependencies/manifest.json` and upstream `gradle.properties`. Their MIT notices
are in `licenses/seedcracker/mc_*.txt`. LattiCG 1.07 is MIT, copyright (c) 2020
mjtb49, with its notice in `licenses/seedcracker/LattiCG.txt`. These notices are
included in source, the production JAR, sources JAR and release license bundle.

Risecode replaces upstream initialization, global biome override, automatic
cracking/submission, raw-world background finders, global executors, configuration
screen and storage. Modern structure matching is local to the engine and leaves
the game's biomes untouched. Unreliable legacy decorators and biome cracking are
unavailable. See `docs/seedcracker.md` for the integration boundaries.
