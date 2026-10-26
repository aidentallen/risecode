# Font sources and installation

The menu and HUD share the same live font choice. Google Sans, Montserrat and Inter
and Qraftys are bundled; Minecraftia remains a local optional font.

| Choice | Source | Asset handling |
| --- | --- | --- |
| Google Sans (default) | [Google Fonts official v14.000 release](https://github.com/googlefonts/googlesans/releases/tag/v14.000) | Unmodified static Regular/Medium; SIL OFL 1.1, embedded copyright notice and upstream license included. |
| Qraftys | [The exact CAPITALIZED FONT 2.1 file supplied by the user](https://www.curseforge.com/minecraft/texture-packs/qraftys-capitalized-font/files/5605880) | Original bitmap and namespaced font JSON bundled with the redistribution permission confirmed for 0.25.0; lowercase small-cap artwork and advances unchanged. |
| Minecraftia | [Andrew Tyler's author listing](https://www.dafont.com/minecraftia.font) | Local personal-use font; nearest sampling at its native 12 px em. Commercial apps need the author's license. No Minecraftia asset is bundled. |
| Montserrat | [Official v7.222](https://github.com/JulietaUla/Montserrat/tree/v7.222) | Unmodified static Regular/Medium; SIL OFL 1.1 included. |
| Inter | [Official project](https://github.com/rsms/inter) | Existing unmodified static Regular/Medium, embedded version 4.001; SIL OFL 1.1 included. |
| Roboto (Unicode fallback) | [Google Fonts Roboto project](https://github.com/googlefonts/roboto-2) | Existing Regular/Medium version 2.001047; Apache 2.0 included. |

The Google Sans release used here declares OFL licensing in its own font metadata.
Its native copyright notice is retained in `licenses/google-sans-NOTICE.txt`, alongside
the upstream `google-sans-OFL.txt`. This is the current open Google Sans release.

## Bundled TTF paths

Place static fonts in `src/main/resources/assets/riserecode/font/`:

```text
google_sans_regular.ttf
google_sans_medium.ttf
montserrat_regular.ttf
montserrat_medium.ttf
inter_regular.ttf
inter_medium.ttf
```

Each file is loaded directly with LWJGL STB; no Minecraft font JSON is required for
these renderer-owned TTF faces. Native Roboto JSON remains for Unicode fallback.
Fonts upload on first use; kerning/advances are measured from their actual tables.
Each TrueType face uses a filled 8× coverage atlas for normal text and a separate
distance-field atlas for shadows/glow. Pixel-footprint filtering keeps fractional
interface scales antialiased. Changing Font Scale, shadows or glow never rebakes
the atlases. Missing pixel fonts use smooth Inter sampling rather than inheriting
the requested pixel font’s sampling mode. See [FONT_FIX.md](FONT_FIX.md).

## Local overrides and optional fonts

In the running game's directory, use `config/riserecode/fonts/`:

```text
google_sans.ttf               # regular override
# or google_sans_regular.ttf and google_sans_medium.ttf
montserrat.ttf               # same naming rules
inter.ttf                    # same naming rules
minecraftia.ttf              # Minecraftia-Regular.ttf renamed
qraftys.zip                  # optional alternative to a ZIP in resourcepacks/
```

Choose the face in Right Shift → Themes → Typography. After adding or replacing a
file, click **Fonts** in the Themes toolbar. Missing/invalid assets show a fallback
notice and use Inter. Regular is reused if no medium variant exists. Local files
are limited to 16 MiB and their SFNT headers/table bounds are checked before STB.

Minecraftia is currently installed at `run/config/riserecode/fonts/minecraftia.ttf`
for local personal-use development, with the author's readme alongside it. Its
[author listing](https://www.dafont.com/minecraftia.font) specifies a separate
commercial-app license. Keep third-party local assets out of the source/JAR until
the intended distribution license permits their inclusion.

## Qraftys resource pack

Installed development pack:

```text
run/resourcepacks/qraftys-capitalized-font.zip
```

The exact supplied 2.1 download targets Minecraft 1.21 and uses pack format 34.
The local installation adapts **only** pack.mcmeta to 26.2's verified resource
format 88.0. `ascii.png` and `pack.png` remain byte-for-byte identical. The original
ZIP is retained under `run/config/riserecode/fonts/originals/`.

To install that downloaded ZIP in another game directory:

```bash
python scripts/install-qraftys.py /path/to/qraftys.zip /path/to/game-directory
```

The Appearance font renderer reads its bundled 128×128 bitmap, or an explicit local override,
preserving small-cap lowercase cells. It creates an original distance-field atlas
for shadows/glow and uses nearest sampling for normal text. Selecting Qraftys affects
risecode immediately without a global resource reload. Enable the pack in Minecraft's
Resource Packs menu too if you want its font applied to vanilla interfaces.

Qraftys is [listed as All Rights Reserved](https://www.curseforge.com/minecraft/texture-packs/qraftys-capitalized-font/files/5605880).
The user confirmed redistribution permission for RiseRecode 0.25.0; attribution and the exact original bitmap checksum are embedded in `licenses/qraftys-NOTICE.txt`. An
[official 26.2 pack release](https://www.curseforge.com/minecraft/texture-packs/qraftys-capitalized-font/files/8741759)
also exists; this workspace retains the exact glyph version requested by the user.

## Bundled font SHA-256

| File | SHA-256 |
| --- | --- |
| `google_sans_medium.ttf` | `654567c75cc0eac0567ed0e5e6f8c1ea88f2ffd397a10889e88857bd9c2e0e23` |
| `google_sans_regular.ttf` | `dd5ac86cf90395d0003b83075b4e92fbca7c7157869db743670e39bc4d30f05b` |
| `inter_medium.ttf` | `97ad806f526e41546d46365bb3a393145f75b7b1568913db74549ad8b8dba872` |
| `inter_regular.ttf` | `40d692fce188e4471e2b3cba937be967878f631ad3ebbbdcd587687c7ebe0c82` |
| `montserrat_medium.ttf` | `7ce96811837174f00c087b73332aed3f04a19069248ab46213d9ea05ff879cbc` |
| `montserrat_regular.ttf` | `6eabd0fb8e3066b6155bac338ef5a6c35b9963ab3f39be7c67edc4a99d870699` |
| `roboto_medium.ttf` | `2879a5ecb7fbfa13a7fc3e2cdd7fecbf73aa45e91b541dfdfa2c442eed0aac21` |
| `roboto_regular.ttf` | `56a45233d29f11b4dfb86d248e921939d115778f87325e7ae8cc108383d6664d` |

| `textures/font/qraftys.png` | `b3aa3de3f4a8ecc6f1b71c05f03765b5202178b915df478869b0d74717b7e0df` |

Font Boldness (0–100, default 0) adds up to 0.75 logical pixels of stroke at draw time. Advances, wrapping and atlases remain unchanged; body, shadow and glow draws capture the stroke in their extracted state.
