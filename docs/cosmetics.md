# Cosmetics wardrobe

Open **Right Shift → Utilities → Cosmetics**. All cosmetics start unequipped and
only affect your character on your client. No cosmetics account is required.

Choose a category, search, and select an item to preview a draft outfit. Drag the
player to rotate and scroll over the preview to zoom. The pose button cycles
standing, walking, crouching and flying. **Edit** opens the selected item's
applicable color, opacity, scale, offset, animation and density controls. Numeric
values can be typed. **Reset item** restores the design's defaults. **Remove**
clears that slot. **Visible** controls the complete outfit.

**Apply** equips the draft. **Cancel**, leaving the page or closing the interface
discards unapplied changes. Cosmetics save and load with ordinary client profiles;
profile controls remain exclusively in **Profiles**. Older profiles without a
cosmetics field preserve the current outfit. Missing assets keep their references
and settings, but do not render until restored.

The Installed view includes 26 starter items: four capes, three wing styles, four
head accessories, three tails, three companions, three trails, three auras and
three particle patterns. Select **Catalog** for 18 additional companions. **Refresh**
checks the project catalog explicitly; startup makes no catalog requests. Select
an unavailable item and press **Download**. Downloading never equips it: select
it after completion to preview, then Apply. Downloads can be cancelled and verified
completed files remain usable offline.

PNG cape imports use a standard 64×32 cape layout or an integer-scaled equivalent,
up to 2048×1024. Enter a local path and choose **Import PNG cape**. Imports are
copied into content-addressed storage. Profiles reference imported IDs, not the
original absolute path. A profile used on another installation needs its assets
imported or downloaded there too.

Custom capes follow vanilla cape motion, or the existing **Render → Cape Physics**
module when enabled. Vanilla cape visibility, armor clearance and elytra rules are
preserved. Decorative wings fold with elytra. Other wearables follow the player
pose; companions animate and bob independently. Cosmetics respect invisibility and
spectator state and owner effects are hidden in first person. Motion and trail
histories reset on disconnect and teleportation.

## Asset sources and preparation

Companion models/textures/animations are CC0 assets from Quaternius's Cute Animated
Monsters Pack: https://quaternius.com/packs/cutemonsters.html. Selected VFX sprites
come from Kenney's CC0 Particle Pack: https://kenney.nl/assets/particle-pack.
Simpler wearable geometry and cape patterns are original MIT-licensed risecode
assets. Credits and license copies ship with the client and prepared catalog.

`python3 scripts/prepare-cosmetics.py` requires the Pillow version pinned in
`scripts/cosmetics-requirements.txt` and network access. It
fetches pinned sources, converts embedded glTF to self-contained GLB, and renders
mesh thumbnails. Bundled assets are checked into source; optional catalog assets
are prepared under ignored `build/cosmetics-catalog/v1/`. The catalog is served
from the `cosmetics-catalog` branch of the existing GitHub repository.

Only prepared GLB 2.0 triangle meshes with embedded PNGs and supported skeletal /
transform animation are accepted. URI references, excessive complexity and
malformed files are rejected. Asset decoding and I/O run on a worker; GPU upload
and outfit commits run on the client thread. Deferred draws retain immutable
geometry/outfit snapshots. The runtime model parser is JglTF without its viewers.

## Verification without packaging

```sh
python3 scripts/fetch-dependencies.py
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check cosmeticsCatalogVerify compileGametestJava -x jar -x sourcesJar --offline --no-daemon
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew runClientGameTest -PcosmeticsOnly -x jar -x sourcesJar --offline --no-daemon
```

Add `-PrendererStack` for Sodium/Iris OpenGL. For Sodium Vulkan add
`-PrendererStack -PwithoutIris -PgraphicsBackend=VULKAN`.

Live catalog integration can be checked explicitly with `-PcosmeticsOnline`;
the default native scenario remains offline.
