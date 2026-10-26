# Development

[Home](../README.md) · [Documentation](README.md) · [Release history](releases/README.md)

## Build

Use **JDK 25** and **Python 3**. The checked-in Gradle wrapper uses Gradle 9.6.0;
Minecraft, Loader, Fabric API, and Loom versions are pinned in
[`gradle.properties`](../gradle.properties). Minecraft 26.2 uses unobfuscated
names; the project does not add Yarn mappings.

```bash
python3 scripts/fetch-dependencies.py
./gradlew check build
```

The dependency script verifies pinned SHA-256 hashes and reuses verified files.
The installable mod is `build/libs/riserecode-0.27.0.jar`; the sources JAR is
separate. After the required artifacts have been cached, an offline build can use:

```bash
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check build --offline --no-daemon
```

## Run and verify

```bash
./gradlew runClient
./gradlew runClientGameTest -PchunkAnimationsOnly
./gradlew runClientGameTest -PchunkAnimationsOnly -PgraphicsBackend=VULKAN
```

`check` runs the dependency-free policy/runtime suites registered in
[`build.gradle`](../build.gradle). Native client tests require a graphical
environment. Fixture options such as `-PinstalledStack` and `-PwithShaderPack`
refer to local installations; a fresh checkout can run the vanilla fixture.
Inspect the build script before using machine-specific fixture options.

Feature-specific release notes distinguish policy checks, disposable native
clients, renderer combinations, and performance evidence. CPU, allocation, GPU,
and frame-rate measurements are separate; historical receipts are not results
from a new checkout.

## Project layout

| Path | Purpose |
| :--- | :--- |
| `src/main/java/dev/riserecode/` | Client systems, modules, UI, renderers, and mixin adapters. |
| `src/main/resources/` | Mod metadata, shaders, fonts, cosmetics, sounds, and licenses. |
| `src/test/` | Policy, persistence, layout, and other non-client checks. |
| `src/gametest/` | Native client fixtures and integration checks. |
| `dependencies/manifest.json` | Pinned dependency URLs and checksums. |
| `docs/releases/` | Complete version notes and release manifest. |
| `scripts/` | Dependency retrieval, asset preparation, and verification helpers. |

Modules own their EventBus subscriptions and release them on disable. Mixins
adapt native game hooks into the EventBus. Preserve native terrain/fluid geometry,
transparency order, and optional renderer compatibility when changing rendering.

## Dependencies and attribution

Sodium, Iris, Baritone, BetterBlockEntities, and the optional EMF/ETF fixtures are
separate mods, not bundled implementations. JglTF and Jackson runtime libraries
are included by Loom for cosmetics. See [dependency handling](../dependencies/README.md)
and [third-party notices](../THIRD_PARTY_NOTICES.md).

Keep runtime configuration, personal imports, downloaded resource packs, build
caches, and generated release binaries outside Git. Retain all third-party
licenses and font provenance. Historical source evidence is linked to immutable
Git commits from the [release index](releases/README.md).
