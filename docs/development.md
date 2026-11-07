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
The installable mod is `build/libs/riserecode-<mod_version>.jar (use `mod_version` from `gradle.properties`)`; the sources JAR is
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

## Review hardening and verification

Run `python3 scripts/verify-project.py` for a fresh dependency check and full
`check build`. The ignored `build/verification/project.json` records the source
commit, source byte fingerprint, dirty state, Java version, dependency manifest,
commands, actual exit codes, suite assertion counts extracted from fresh output,
log digests and artifact digests. It explicitly does not establish native runtime
compatibility. `--package` requires a clean committed tree and archives that same
commit. Source changes during verification invalidate the receipt. Historical
0.28.0 packaging tools now require this source/artifact-bound receipt as well.

The ClickGUI uses Tab/Shift+Tab for visible control traversal and Ctrl+Tab for
pages. Enter/Space activates a focused control, arrows adjust supported values,
and Page Up/Page Down scrolls. Text editors retain their editing shortcuts.
Narrator announcements obey Minecraft's narrator setting. Common control families
participate; custom canvas interactions and every auxiliary screen still need
separate keyboard-only native acceptance testing.

Optional Sodium, Iris and Better Block Entities internal adapters apply only to
the exact versions pinned in `dependencies/manifest.json`; other versions skip
those adapters with a log warning. This is a defensive startup policy, not proof
of visual compatibility. Preserve separate final-artifact renderer, visual,
multiplayer and performance evidence.

Generated Resources packs retain the active and previous successful generation.
Before the next apply, unused UUID generation directories are reclaimed without
following symlinks. Source archives and their attribution stay intact. A failed
prune is logged and does not invalidate the active selection.
