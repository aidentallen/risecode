# Verification evidence

[Development](development.md) · [Release history](releases/README.md)

Evidence is scoped to a source commit and an artifact digest. A clean build is
not a runtime certification. The verifier's custom Gradle check tasks report their
executed outcomes; the conventional JUnit `test` task may be skipped.

## Whole-codebase remediation checkpoint 025632c

Implementation source: `025632c13e1b0831c053133ea5419e05a434443f`. This local checkpoint has not
been installed or published. Later documentation edits do not change the artifact.

| Evidence | Established result | Boundary |
| :--- | :--- | :--- |
| Fresh check/build/package | `round8-project.json`, SHA-256 `630c5e4a9547c09539579353b52c18bbc32f77016afb05e4da7efc630e2667b9`; clean source and 36 custom suite summaries | Custom tasks run; conventional JUnit task may be skipped |
| Release JAR | `eae296742bd3f8eef6f78a195b9896b68e0947a9793140f0bdff8fc531ac4c26` | Identifies this implementation, not earlier runtime artifacts |
| Sources JAR | `a87e48992bbe8598ba26c8c7f1f2a8649aff38f78746f9b1aa0ce91962c7be57` | Includes current source and attribution |
| Keyboard visual/state case | 33 assertions; `round8-native-keyboard.log`, SHA-256 `ddde2aff4802f71cab15efa1c16a26a91b80f9094bb542b800dfad0832e38de2` | Solid/gradient header pixels and active direction text, profiles/HUD/Glass; no human speech comprehension claim |
| Resource parser memory boundary | Exact 128 MiB decoded archive succeeds, one byte over rejects; 512 MiB heap and Linux process RSS gate below 1.5 GiB | Highly compressed synthetic archive; parser JVM only, excludes game/GPU/concurrent composition |

Complete receipts and native manifests live in the ignored evidence directory
and must accompany a release as assets. The current keyboard case does not
re-certify every earlier rendering case on this changed binary; each later matrix
records its own source/artifact/commands/log/image digests.

## Whole-codebase review checkpoint d36a768

This reviewed checkpoint is local and has not been installed or published.
Source commit: `d36a768d516c98cedd36a47757aaf760e94b504f`.
Release JAR SHA-256:
`01f1d93ddfa5f17dce1b3375166ba35abf9390be2cf22c7923c9ec8c283abcd6`.

| Evidence | Established result | Boundary |
| :--- | :--- | :--- |
| Fresh verifier | 35 custom suite summaries; clean source, 898 archived tracked files, 23 pinned dependencies and artifact/log hashes verified | Not native execution by the verifier |
| Two clean builds | Identical binary and sources JAR hashes | Same host/toolchain/cache; no independent-host claim |
| Final keyboard fixture | 18 assertions, working narrator engine, common/color/profile/friend/auxiliary controls | No human speech comprehension, complete canvas or Resources recovery claim |
| Final race fixture | 25 assertions, saturated actual map queues, failed clear writes, stale candidates, packet identities | Not every shutdown interleaving or remote multiplayer schedule |
| Final overlays | Vanilla3, Iris4 and Vulkan3 pixel assertions; fixed camera, red/opaque-occluded/through-wall lines | Glass/water/particles/reload captures lack a comprehensive image oracle; widths/shadows/scaling/BBE/armor/held remain separate |
| Final seed fixtures | Sodium865, Iris866, Vulkan865 assertions | Targeted native collection/render paths, not all gameplay |
| Dense finder sample | Four synthetic samples, 2,048 chests, maximum client-call wall duration 940,699 ns and thread allocation 148,384 bytes | Not frame/GPU timing, positive structure recall, soak or multiplayer performance |

All eight native cases used the unchanged JAR above. Iris used pinned
Complementary Unbound r5.9.3 and retained missing custom line shader override
warnings. Native engine speech dispatch was exercised; no human-audible result is
claimed. Generic allocation rollback regressions exercise production ownership
helpers, rather than injected GPU-device failure or live-handle accounting.
Qraftys artwork, provider and downloaded pack are absent from the release JAR,
sources JAR and full source archive; current notices claim no redistribution
permission. Optional selection-driven acquisition retains its source, limits,
digest verification, Inter fallback and explicit retry.

## Reproducing and preserving evidence

Run `python3 scripts/verify-project.py --offline --package` on a clean committed
tree after fetching dependencies. Local receipts/logs/archive are generated in
`build/verification/`, excluded from Git. Native fixtures and their flags are
listed in [Development](development.md). Record each command, exit status, source
commit, unchanged artifact digest and log/image digests when making a native
claim. Keep native, image, client CPU/allocation, frame/GPU and remote multiplayer
results separate. Attach complete receipts and inventories to the corresponding
release when publishing; a local-only report is not a published asset.

Later review remediations must receive their own source/artifact-bound evidence;
the historical checkpoint above does not certify changed code.
