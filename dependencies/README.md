# Build dependencies

Run `python3 scripts/fetch-dependencies.py` from the project root before building.
It fetches the pinned Baritone API, Sodium, and Iris distributions, extracts
Iris's nested glsl-transformer dependency, and verifies every SHA-256 digest.
Already verified files are reused. These JARs are compile-only and are never
included in the risecode mod JAR or the Git repository.

Sodium and Iris are optional at runtime. Baritone controls show an unavailable
state when the separate official Baritone API mod is absent.
