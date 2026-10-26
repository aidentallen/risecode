# Movement spikes and 2026-10-22 crash investigation

The installed 0.24.0 multiplayer session crashed at 01:28:49–50 local time. Evidence was
preserved in `.tooling/movement-crash/` before further launches. The crash report identifies
`NullPointerException: Cannot assign field dirty because targetRegion is null`, in
Bobby 5.2.15+mc26.2 `Worlds.processMerge`, line 666, on the render thread. The fatal stack
contains Bobby's world-cache update path; it does not identify a RiseRecode exception as
the fatal cause.

The log contains 69 `Merging world` events, including long chains on the render thread at
01:24:11, 01:25:00, 01:26:31 and 01:28:49. Dynamic multi-world support was enabled in Bobby.
Upstream code looks up `targetRegion` again after asynchronous copying/synchronization and
then writes `targetRegion.dirty` without a null check at that stage:
[installed-version source](https://github.com/Johni0702/bobby/blob/v5.2.15%2Bmc26.2/src/main/java/de/johni0702/minecraft/bobby/Worlds.java#L649-L666).
This confirms the failing operation. It does not prove why the region became absent.

Those merge chains, cache reloads and background storage churn are suspects for movement
spikes. There was no frame-time/CPU/GC recording from the failing gameplay session, so
this log cannot conclusively attribute each hitch. The crash report showed a 4 GiB maximum
heap and about 13.2 GiB system swap in use. Swap occupancy alone does not demonstrate active
paging during a freeze. No out-of-memory exception appears in the fatal stack, and the
kernel journal search for the session found no OOM kill or GPU reset. Do not change the
collector, heap size or unrelated mod settings based solely on these observations.

## User-approved workaround

Only `config/bobby.conf` changed from `enabled=true` to `enabled=false`, with a verified
backup under the profile's `.riserecode-backups/movement-crash-20261021T233335Z/bobby.conf`.
All Bobby cache files remain intact. Bobby's cached distant chunks stop displaying while
it is disabled; Distant Horizons remains enabled. Restart or relaunch the game to apply it.
The local receipt is `.tooling/movement-crash/bobby-receipt.json`. To undo later, restore
that backed-up setting after a suitable upstream fix has been verified.

The latest upstream release checked was 5.2.16 for Minecraft 26.3; it lists only the 26.3
update. The installed 5.2.15 is the newest 26.2 release in the checked release list, so no
cross-version replacement was installed:
[upstream releases](https://github.com/Johni0702/bobby/releases).

## Separate RiseRecode camera fix: 0.24.1

Two `Listener failed for FovEvent / Invalid FOV` errors occurred during joining at 01:18:19
and 01:18:20. Vanilla's new camera starts with zero-valued FOV interpolation fields before
its first tick. RiseRecode rewrote that native result through a positive-value setter even
when zoom was inactive. The setter threw and EventBus logged the failure. This is a
confirmed RiseRecode bug, separate from the fatal Bobby exception; two join-time errors do
not explain all later movement spikes.

0.24.1 leaves native FOV exactly unchanged while zoom is inactive. Active zoom tolerates an
unready zero/non-finite native value, and positive magnification results clamp before
conversion to float to prevent underflow. Existing valid hold/release zoom and its
one-degree minimum remain. Headless regressions cover native zero, negative zero,
sub-degree values, normal values, non-finite boundaries, active zoom and extreme scaling.
Native checks use a real camera listener with captured EventBus failures, then exercise
the actual zoom key, magnification and release path in a disposable world.

The 0.24.0 renderer and synthetic-player benchmark evidence in
[remediation.md](remediation.md) covers its stated fixtures. It did not establish performance
for this complete personal mod stack, world-cache history, shader configuration or live
multiplayer session. Movement-spike resolution remains to be verified in real gameplay
after the workaround and camera fix; it is not inferred from the earlier allocation gains.

## Verification boundaries and test teardown

The production artifact comparison against 0.24.0 found exactly three changed entries:
`FovEvent.class`, `CameraEffectsManager.class`, and version metadata. `check build
compileGametestJava` passed; the runtime suite now has 35 assertions, including 13 FOV
boundary regressions, alongside the existing 304 performance assertions and other suites.

The first disposable installed-stack camera probe blocked in Fabric GameTest's shutdown
phase barrier at `IntegratedServer.halt -> executeBlocking`. A test-only adapter now skips
only that empty non-owner cleanup after the fixture verifies that every player is the
singleplayer owner. Normal server halt and world saving still execute. The adapter is
absent from the production JAR.

With the installed mod set, the camera/zoom suite then passed all 16 assertions with no
`Invalid FOV` listener error. After the game main thread exited, its shutdown watchdog
fired because Baritone's two non-daemon worker threads remained alive. That diagnostic
is separate from the user's Bobby multiplayer crash and is retained locally as
`test-shutdown-baritone-crash.txt`; the run is not counted as a clean full-stack exit.
A further disposable run omitted only Baritone and passed all 16 assertions with a clean
process exit (`BUILD SUCCESSFUL in 44s`). It loaded Iris and the remaining installed mods.
The later GUI investigation found that the init script copied settings relative to its
own directory: Complementary was not active and the approved Bobby setting was not applied
in those camera fixtures. These runs establish camera behavior with those mods loaded;
they do not establish shader-pack or Bobby-workaround coverage. The corrected GUI fixture
uses absolute destinations and asserts the active shader pack before running.
This validates the camera regression and zoom lifecycle, not long-session multiplayer
performance. The user's Baritone mod and configuration are unchanged.

## Installed 0.24.1 artifact

The verified production JAR is installed in the Fabric 26.2 profile as
`mods/riserecode-0.24.1.jar`, SHA-256
`116898fdfa23e7f7a34fbecb70694c1101ba1c04d69f2f5f5b557f4720944d15`.
Exactly one JAR has Fabric metadata ID `riserecode`. The previous 0.24.0 JAR was backed up
outside `mods/` at `.riserecode-backups/camera-fov-20261021T234230Z/riserecode-0.24.0.jar`,
with its checksum verified before replacement.

Installation preserved all 48 unrelated mod files and 24,877 configuration/map files by
checksum, including the already-approved Bobby setting. No concurrent terrain changes
occurred during these inventories. The installer wrote no settings or terrain and did
not stop the user's game. The complete receipt is
`.tooling/movement-crash/install-receipt.json`. **Restart Minecraft to load 0.24.1 and
apply the Bobby workaround.**
