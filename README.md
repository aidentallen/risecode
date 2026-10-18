# risecode cosmetics catalog

Versioned prepared assets for the local risecode cosmetics wardrobe. No game
client code, accounts, credentials, player data, or release JARs are stored here.

`v1/index.json` lists 44 cosmetics: 26 bundled starter items and 18 downloadable
companions. Asset files have SHA-256 identities and are validated before loading.

Companion models, textures and skeletal animations: Quaternius,
https://quaternius.com/packs/cutemonsters.html, CC0-1.0. Converted from the pinned
glTF sources into self-contained GLB files; original hashes are embedded.

VFX sprites: Kenney, https://kenney.nl/assets/particle-pack, CC0-1.0. Original
license text is in `v1/KENNEY-LICENSE.txt`; the CC0 legal text is in `v1/CC0-1.0.txt`.

Original cape patterns and cosmetic definitions: risecode, MIT. See LICENSE-MIT.txt.
The reproducible preparation tool lives at `scripts/prepare-cosmetics.py` in the
application source checkout. Sources are pinned in source-manifest.json.
