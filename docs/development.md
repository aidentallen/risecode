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

### Publication contract

A current publication plan is a JSON object with `format: 1` and a `releases`
array. Each release supplies `tag`, `title`, `version`, `notes`, `assets`,
`artifact`, `verificationReceipt`, `sourceCommit` and `assetSha256` (asset base
name to SHA-256). Run the fresh verifier with `--package` on a clean commit
first. The publisher requires a passed receipt for the exact clean source,
verified binary/source/archive hashes, and a remote tag resolving to that same
commit. It validates every plan entry before release mutations. Incomplete,
failed or invalidated receipts cannot authorize publication.

Legacy list-shaped plans require explicit `--historical-migration`. That mode
is for restoring already-audited historical releases and does not establish
current build provenance. Both modes inspect nested archives and reject Qraftys
artwork, its provider and downloaded pack. Historical assets containing that
artwork must not be republished. Existing remote history is a separate audit.
Gradle archive timestamps and entry order are normalized; repeat-build hash
comparisons still need to accompany any reproducibility claim.

### Review regression fixtures

`reviewPersistenceCheck` covers retained map writes under executor/completion
queue saturation, a transient write failure, reload terminal outcomes, allocation
rollback, candidate budgets and unsupported-renderer motion policy. The native
`-PreviewRaceOnly` fixture exercises failed/rejected empty SeedCracker clears,
stale chest candidates, dense sliced/unsliced comparison, saturated actual
MapService transitions/close and packet attempt/connection identity. These have
different coverage: a helper regression is not a full world-transition or device
failure test.

`-PkeyboardReviewOnly -PrequireNarrator` requires a working native narrator and
uses Minecraft's keyboard input hook. It covers common module controls, color
endpoints/modes/direction, profile save/load, friend add/remove and auxiliary Glass/block-picker navigation. Speech
is dispatched to the native engine; human audible comprehension is not asserted.
The auxiliary collector covers project screens, with shared controls, gallery
and resource cards, HUD selections and preview rotation/zoom. Complete Resources recovery flows, every canvas gesture, small-window/large-font
behavior and clipped-content scrolling still need independent acceptance tests.

`-PreviewOverlayOnly` provides fixed-camera pixel assertions for visible,
opaque-occluded and through-wall lines, plus screenshots of glass, water,
particles and reload. The pinned Iris/Complementary combination logs missing
custom line shader overrides while the targeted color/depth assertions pass:
Risecode's dedicated overlay path keeps those lines outside the pack shader.
The warnings remain visible. This evidence does not establish every shader,
line width, shadow pass, armor/held-item surface, scaling or BBE ownership case.
Record the exact unchanged JAR and log/image hashes for a final release matrix.
