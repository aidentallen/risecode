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
Minecraftia and Qraftys assets remain local and are excluded from both the
repository and release artifacts.

Historical JARs are preserved byte-for-byte. Their accompanying release notices
and original embedded license files describe the applicable third-party terms;
adding the original-code MIT license does not replace those terms.
