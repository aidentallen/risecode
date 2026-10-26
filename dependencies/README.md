# Build dependencies

Run `python3 scripts/fetch-dependencies.py` from the project root before building.
It fetches the pinned Baritone API, Sodium, and Iris distributions, extracts
Iris's nested glsl-transformer dependency, includes official EMF/ETF test companions, and verifies every SHA-256 digest.
Already verified files are reused. These JARs are build/test dependencies and are never
included in the risecode mod JAR or the Git repository.

Sodium and Iris are optional at runtime. Baritone controls show an unavailable
state when the separate official Baritone API mod is absent.

Use -PwithEntityFeatures with runClientGameTest to include EMF/ETF in a disposable
test client. Their distributions and LGPL licenses remain intact; neither is
bundled into risecode.

Cosmetics uses the pinned JglTF 2.0.4 parser/model JARs and Jackson core/databind
2.22.4 plus annotations 2.22. These runtime libraries are included by Loom; the
renderer, assets, and animation controls remain risecode's implementation. The
manifest records exact URLs and SHA-256 digests.

The optional BetterBlockEntities adapter compiles against the checksum-pinned
`bbe-fabric-1.3.9+mc26.2-mod.jar` (not bundled). Native validation also covers the
installed 1.3.8-beta.1 API; the adapter leaves BBE settings and lid rendering intact.
