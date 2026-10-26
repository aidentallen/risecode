# Living Panorama

The start screen can show a seeded forest and lake using the active resource pack's loaded block models and materials. Enable **Living Panorama** in Appearance or the start screen's appearance gear. It starts disabled, preserving the selected background and older theme files.

The leaf button or **F1** enters Take a breath. F1, Escape, or the faint return leaf restores the menu. Drag exposed scenery to look around, click exposed water for ripples, and use Reset view to center the camera. The atmosphere button cycles through Misty Dawn, Sunlit Day, Golden Hour, Rainfall, Firefly Dusk, and Aurora Night. Keyboard navigation and narration use native buttons; hidden controls are inactive and cannot take focus.

Appearance and the start-screen gear share a Living Panorama section. Tree and undergrowth density use fixed candidate placements; the lake and principal trees stay in place. Wind, time of day, weather strength, and fog select Custom when edited. Presets leave density, camera sensitivity, interaction strength, quality, refresh, the optional day/night cycle, volume, and mute independent. Environmental audio starts at 15%, with Minecraft's ambient water/firefly-bush and weather rain categories. Menu music keeps its existing settings.

Low uses a 75% scene target and a simplified distant forest. Balanced uses native resolution, cached sunlight occlusion and sky reflections. Cinematic adds a half-resolution planar reflection refreshed at 15 Hz. The interface always uses native resolution. Scene refresh is independently selectable at 30, 60 (default), 120 Hz, or uncapped. Reduced Motion freezes procedural movement and disables scene particles; Minecraft and RiseCode particle reductions are honored. An unfocused window freezes scene rendering and stops owned audio.

## Implementation

`ui/panorama/ForestScene` describes deterministic voxels without creating a level or server. `PanoramaResources.capture` reads loaded model parts, atlas identities, UVs, tint sources and material layers on the client thread. Workers process immutable value snapshots, cull hidden faces and cache sunlight occlusion. Meshes are uploaded once in groups; shaders animate foliage with bounded gust/ripple uniforms. The renderer uses Blaze3D pipelines and explicit sky, opaque, cutout, water, translucent-material and particle passes. Its lighting is independent of world and Iris lighting. The pipeline follows [Fabric's rendering guidance](https://docs.fabricmc.net/develop/rendering/basic-concepts).

`LivingPanorama` owns the title/editor lifetime, generation cancellation, debounced density replacement, environment blending, pointer picking and immutable extracted frames. An existing scene remains available during a rebuild; the saved background remains available during initial preparation. Obsolete work checks its generation and is discarded after density changes or resource reloads. Departure, disabling, shutdown and resource reload release buffers, targets and owned sounds. Rendering failure restores the saved backdrop and retains the enabled preference; disabling/re-enabling or reloading retries preparation.

`PanoramaInteraction` separates a press from a drag at a five-GUI-pixel threshold. A gesture that ever crosses the threshold cannot also click water. Camera yaw and pitch, including parallax, stay inside ±60° / ±15°. `PanoramaSound` owns three quiet relative voices and stops only those voices.

## Verification

Reproduce the pure checks with:

```
python3 scripts/fetch-dependencies.py
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check build --offline --no-daemon
```

`PanoramaChecks` covers default/migrated appearance, preset and Custom persistence, independent preferences, deterministic density changes, principal trees, the lake opening, camera/gesture limits, bounded impulses, expiry and cancellation. `PanoramaIntegrationTest` is selected by `-PpanoramaOnly` in disposable clients and can run from the packaged production JAR with `scripts/panorama-packaged-tests.gradle`, which isolates the production JAR from main-source output. It covers atmospheres and blend completion, widget precedence, water and drag gestures, F1/Escape/return-leaf recovery, focus loss, mute, resource-pack model replacement, generation cancellation, injected rendering failure, quality levels, resource ownership, resize, fullscreen and repeated title/editor/options/world transitions.

For a packaged vanilla run:

```
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew runClientGameTest \
  -I scripts/panorama-packaged-tests.gradle -PpanoramaOnly -PbundledFontsOnly \
  -PclientTestDirectory=build/run/panorama-vanilla --offline --no-daemon
```

Add `-PrendererStack -PwithoutIris` for Sodium OpenGL; add `-PrendererStack -PwithShaderPack` for Iris; add `-PrendererStack -PwithoutIris -PgraphicsBackend=VULKAN` for native Vulkan. The shader-pack fixture reads the authorized profile's Complementary Unbound archive and copies it into the disposable run directory.

Timing instrumentation separates scene-update CPU, reused-frame CPU, allocations, renderer command submission, GPU timestamps, draw counts, startup and owned GPU bytes. Warm-up is excluded. GPU timestamp deltas are calibrated with `DeviceInfo.timestampPeriod()` on each backend; raw Vulkan ticks are not nanoseconds. GPU scene timestamps exclude native UI composition and button blur. Measurements are diagnostic samples on a shared host, rather than multiplayer FPS comparisons or long-session proof. Screenshots and motion captures live under the disposable run/evidence directories and are excluded from Git.

Final packaged renderer-matrix results and installation receipt are recorded in `docs/releases/v0.26.0.md` after verification.
