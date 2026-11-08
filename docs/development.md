# Development

[Home](../README.md) · [Documentation](README.md) · [Release history](releases/README.md) · [Verification](verification.md)

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

### Persistence and shutdown regression coverage

Seed archive coalescing compares the complete immutable saved payload (observations,
pillars, structure/world results, identity, preservation policy and clear intent).
A result-only change therefore stays eligible for a write even when observations
are unchanged; an older completion cannot acknowledge a newer payload. Native
fixtures inject completed manual/lookup results and hold an earlier write behind
an actual worker barrier. These tests validate persistence, not cracking accuracy
or remote lookup availability.

Resources owns Apply completion from admission, including queued preparation.
Close completes that result before waiting for client callbacks, cancels tracked
library work and prevents late commit. A native fixture occupies both work lanes,
queues Apply and closes it. Local object reads have stream-enforced caps matched
to their type: resource catalogs 4 MiB, icons 8 MiB, previews 32 MiB, archives
128 MiB, extracted entries 64 MiB; cosmetics metadata 1 MiB and assets 16 MiB.
Repair acquisition is capped too; a growing-file fixture checks the stream bound.
Glass Page Up/Down uses its settings scroller directly; preview arrows remain
independent. Native keyboard checks exercise expanded material controls at two
window sizes.

### Bounded runtime review matrix

The additional review fixtures are separate from the conventional JUnit task:

| Fixture flag | Coverage | Evidence limit |
| :--- | :--- | :--- |
| `-PreviewGpuOnly` | Actual native texture/view/upload-buffer failure boundaries for Face, bitmap and branding; successful retry/eviction; LWJGL tracked font allocation baseline | Native backend object closure and tracked allocations, not driver-global VRAM accounting; the bitmap is generated by the fixture |
| `-PreviewCollectionOnly` | Actual collection admission/yield, positive treasure/shipwreck layouts, mutation, archive/reopen and per-call CPU/wall/allocation | Fixed synthetic layouts; not universal structure recall or remote multiplayer |
| `-PreviewResourcesOnly` | Keyboard browse/order/apply/fail/recover at 640×400 and larger fonts with Reduced Motion; ten real Apply/fail/recover/prune cycles | First two cycles establish explicit tolerances; heap samples use explicit GC and include engine/test state |
| `-PreviewFrameOnly` | Three current-build scenes, each with 3s warmup and 10s measurement; frame wall/CPU/allocation and GUI/blur GPU timestamps | No A/B improvement claim, full-frame GPU time, soak or multiplayer claim; default appearance, no personal assets |
| `-PreviewOverlayOnly` | Widths 1/4/32 at 100%/75% resolution; opaque/glass/water/particles; five reloads and disconnect ownership | Fixed camera and pinned shader; shadow invocation is distinct from exhaustive shadow-image validation |
| `-PreviewBbeOnly -PrendererStack -PwithoutIris -PwithBbe` | One pinned BBE chest cached-mesh/native-renderer ownership handoff | One representative placement; unknown renderer versions remain unsupported |

`-PreviewResourcesOnly` uses the same declared offline-cache fixture as
`-PresourcesOnly`. GPU failure tests temporarily instrument a real backend only
within the test operation; they do not ship instrumentation or failure switches
in the production JAR. Normal presentation encoders are preserved.

Cosmetics owns every admitted work/import result through close. A decoded import
must pass the closed/token check under the same commit monitor as close; queued
results terminate without needing a client callback. Local path/digest/stream
checks protect malformed inputs and pre-existing traversal/symlinks. They are not
an OS sandbox against another process concurrently replacing files under the same
user account; cache roots are trusted user-owned directories.

The overlay oracle distinguishes native transparency tint from Iris's separate
post-world overlay. Water uses local channel contrast plus a no-line negative
control, rather than requiring unblended RGB through water. The scene simulation
is frozen while checking geometry so flowing water cannot move the camera or
change the occluder. World targets close at actual level clear; a network
Disconnect callback alone can precede the last old-world frame.

Archive tasks omit empty directories as well as normalizing timestamps and order.
An untracked empty directory left by an asset removal must not make a working
checkout's JAR differ from a fresh source archive build.
