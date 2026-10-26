# risecode — Technical Code Review

**Project:** `/home/toast/Work/riserecode`  
**Target:** Fabric / Minecraft 26.2 / Java 25  
**Scope:** Architecture, performance, rendering, mixins, concurrency, code quality  
**Review type:** Read-only review. No source files were modified. This document is the only added file.

---

## Table of Contents

- [📊 Executive Summary](#-executive-summary)
- [🏗️ Architectural & Structural Review](#️-architectural--structural-review)
- [⚡ Performance & Memory Analysis](#-performance--memory-analysis)
- [🎨 Rendering & Mixin Evaluation](#-rendering--mixin-evaluation)
- [🧵 Concurrency & Stability](#-concurrency--stability)
- [🚨 Critical Red Flags & Bugs](#-critical-red-flags--bugs)
- [🗺️ Actionable Roadmap](#️-actionable-roadmap)

---

## 📊 Executive Summary

This is a substantially stronger codebase than a typical Minecraft client. The architecture is deliberate: a typed event bus, explicit module lifecycle, immutable extraction snapshots, modern `RenderPipeline`/`RenderTarget` usage, conditional Sodium/Iris mixins, validated config decoding, and atomic file writes. There is **no `@Overwrite`**, no wildcard method targets, and no raw OpenGL state manipulation. Those are meaningful positives.

The main problems are not architectural collapse. They are **hot-path allocation, redundant per-frame scans, string-keyed theme resolution, broad mixin surface, and a few lifecycle/context bugs**.

| Area | Assessment | Main reason |
|---|---|---|
| Architecture | **Strong** | Clear composition root, event bus, module lifecycle, extraction/draw split |
| Rendering | **Good** | Modern pipelines, no raw GL, shader compatibility layer |
| Performance | **Needs work** | Allocation-heavy text/UI/world paths; O(n²) HUD updates |
| Concurrency | **Good but implicit** | Thread-safe registries, but no enforced event thread policy |
| Config/theme | **Functional but heavy** | Per-setting `Gson`, synchronous theme/profile writes, full appearance copies |
| Mixins | **Disciplined but broad** | No overwrites, but 22 `@Redirect`s and several `Screen`/`Hud` targets |
| Maintainability | **Mixed** | Good naming in places; dense one-liners and string-keyed services |

### Highest-impact findings

1. **Per-glyph UI allocations** — `UiMesh.quad()` allocates a matrix, bounds, intersection, and `Quad` for every glyph; `CustomTextRenderer` calls it per glyph.
2. **HUD O(n²) lookups** — `HudRenderer.listed()` calls `ModuleManager.getByClass()` for every module, twice per frame.
3. **Nametags per-frame raycasts and equipment state** — `NametagsModule` rebuilds labels, item render states, and calls `hasLineOfSight()` every frame.
4. **Chunk motion map churn** — `ChunkMotionTracker.frame()` rebuilds a `HashMap<Long,Float>` and boxes keys/values per section per frame.
5. **Persistence cost** — `SettingCodecs.encode()` creates `new Gson()` per setting per save; `ThemeManager.save()` writes a large theme document synchronously on the client thread.

---

## 🏗️ Architectural & Structural Review

### What is genuinely well designed

- **Composition root is clean.** `Client` wires managers and services; `RiseRecodeClient` is only the Fabric adapter. Feature modules do not scatter Fabric hooks throughout the codebase.
- **Event bus is fit for purpose.** `EventBus` is synchronous, typed, reflection-free, priority-ordered, and uses snapshot dispatch. Registry changes are thread-safe, dispatch is cached per event class, and listener failures are isolated (`EventBus.java:59-73`, `:109-123`).
- **Module lifecycle is better than most clients.** `Module.setEnabled()` has reentrancy protection, rollback on failed `onEnable`, subscription cleanup, and even `Error` handling with suppressed cleanup failures (`Module.java:138-169`). `listen()` scopes module appearance context for render events and releases subscriptions on disable (`Module.java:179-207`).
- **Extraction/draw separation is sound.** `Render3DEvent` collects immutable primitives; `WorldRenderer` consumes them later. The comment “no entity references survive into drawing” is a good invariant (`Render3DEvent.java:9`).
- **Rendering uses the modern API.** `RenderPipeline`, `RenderType`, `GuiGraphicsExtractor`, `SubmitNodeCollector`, `TextureTarget`, and `GpuBuffer` are used instead of legacy immediate-mode GL.
- **Compatibility is considered.** `RendererMixinPlugin` gates Sodium/Iris mixins; `@Pseudo` and `require = 0` are used for optional targets; `ConnectionMixin` wraps the deepest send boundary rather than every overload.
- **Config decoding is defensive.** `SettingCodecs` validates types and ranges; `ConfigManager` uses atomic writes and revisioned async saves; profiles are prevalidated before application.
- **Java 25 is used properly.** Records, switch expressions, pattern matching, `var`, and `-Xlint:all -Werror` are present. This is not a Java 8 codebase wearing a new version number.

### Structural concerns

1. **Hardcoded module registry and name-based identity**
   `ModuleManager.initDefaults()` manually constructs every module (`ModuleManager.java:50-92`). `Module.classification()` switches on `getName()` (`Module.java:75-83`), and `getByName()` contains a special case mapping `"NoFall"` to `"Clutch"` (`ModuleManager.java:107`). This makes modules harder to test in isolation and makes renames behavior-changing. A registry list or `ServiceLoader`/annotation processor would be more maintainable.

2. **String-keyed theme service locator**
   `Theme.number("...")`, `Theme.bool("...")`, and `Theme.paint("...")` resolve settings by string through `ThemeManager.effective()` (`Theme.java:38-56`, `ThemeManager.java:71-79`). Unknown names throw at runtime (`ThemeManager.java:264-267`). This is flexible, but it prevents compile-time checking and encourages repeated map/stream lookups in render loops.

3. **`ModuleAppearance` copies too much**
   Every module gets a full `ModuleAppearance` at startup via `migrateModuleAnimations()` (`Client.java:104`), and each appearance copies every applicable global control (`ModuleAppearance.java:71-79`). With dozens of modules and 100+ global controls, this creates thousands of `Setting` objects and a large theme document. A sparse override map would be much lighter.

4. **`ModuleAppearance.effective()` is linear per lookup**
   ```java
   public Setting<?> effective(String name) {
       return global.getValue() || controls.stream()
           .noneMatch(c -> c.setting().getName().equals(name) && supported(c))
           ? null : values.get(name);
   }
   ```
   (`ModuleAppearance.java:101`). This is called for every theme lookup inside a module context, including HUD and render paths. A precomputed `Map<String, Setting<?>>` is the obvious fix.

5. **`PageAppearance.context` is global mutable state**
   `MenuScreenMixin` sets `PageAppearance.context(...)` at `HEAD` and resets it at `TAIL` (`MenuScreenMixin.java:16-17`, `:39-40`). If rendering is cancelled or throws, the context leaks into subsequent rendering. `ModuleAppearance.with()` does this correctly with `try/finally` (`ModuleAppearance.java:17-18`); `PageAppearance` should use the same scoped pattern.

6. **`ThemeManager.add()` silently overwrites duplicate names**
   `settings.put(setting.getName(), setting)` has no duplicate check (`ThemeManager.java:201-204`). `Module.register()` does check duplicates, so the inconsistency is notable. Given the string-keyed design, a duplicate would silently replace a setting.

7. **`Client` is a god object with fragile shutdown ordering**
   `Client.shutdown()` saves config, closes many services, then calls `moduleManager.disableAll()` after `configManager.close()` (`Client.java:144-161`). Module cleanup can therefore mutate settings after the config writer is shut down, and a failure in any earlier close step skips the rest because there is no `try/finally`.

8. **Inconsistent persistence paths**
   `ConfigManager` has a dedicated writer, revision tracking, and atomic writes. `ThemeManager.save()` and `ProfileManager.saveProfile()` write synchronously on the calling thread. The same application has two very different persistence quality levels.

9. **Getter with side effects in the render path**
   `Module.popDuration()` calls `Theme.manager().module(this).migrate(this)` while rendering (`Module.java:42`). `ModuleAppearance.migrate()` can seed settings, change `global`, and call `changed()`, which schedules a theme save (`ModuleAppearance.java:122-127`). A getter called during layout should not mutate persistent state. Move migration to startup or module enable.

### Java and exception-handling notes

The code is generally modern and defensive, but readability is uneven. Several files are written as dense one-liners with multiple statements per line, especially `NoFallModule`, `ThemeManager`, `PageAppearance`, and `BlockAnimationsModule`. That style makes stack traces, reviews, and merge conflicts harder. A formatter and a modest line-length/readability rule would help more than additional abstraction.

Exception handling is mostly good: config/theme loads preserve unreadable files, save failures are logged, and event listener failures do not abort dispatch. The weak points are lifecycle exception safety (`Client.shutdown`) and render-path exceptions (`WorldRenderer` submissions, `Render3DEvent.targetHighlight`) where a single failure can abort a frame.

---

## ⚡ Performance & Memory Analysis

These are inspection-based findings, not profiler traces. The snippets below are **recommendations**, not applied changes.

### 1. UI text and quad extraction: the largest allocation source

**Current:** every call to `UiMesh.quad()` allocates a `Matrix3x2f`, a `ScreenRectangle`, a transformed bounds rectangle, potentially an intersection rectangle, and a `Quad` (`UiMesh.java:40-55`). `CustomTextRenderer.drawFace()` calls `UiMesh.quad()` **once per glyph** (`CustomTextRenderer.java:441-443`). A 100-character label therefore creates hundreds of short-lived objects per frame.

**Before**
```java
Matrix3x2f pose = new Matrix3x2f(graphics.pose());
ScreenRectangle bounds = new ScreenRectangle(...).transformMaxBounds(pose);
if (scissor != null) bounds = bounds.intersection(scissor);
UiBatch.submit(graphics, new Quad(..., pose, ..., bounds));
```

**After**
```java
// Reuse scratch objects per draw call, not per glyph.
Matrix3x2f pose = SCRATCH_POSE.set(graphics.pose());
ScreenRectangle bounds = SCRATCH_BOUNDS.set(...).transformMaxBounds(pose);
// For glyph runs, build one batched render state with a reusable vertex buffer
// instead of one Quad per glyph.
```

At minimum, hoist the pose copy and scissor/bounds calculation to the text-run level. The larger win is to batch a whole glyph run into one `GuiElementRenderState` with an indexed vertex buffer.

### 2. Theme lookups inside render loops

**Current:** `Theme.number()`/`Theme.bool()` go through `ThemeManager.effective()`, which checks module context, page context, and then the global map. In a module context, `ModuleAppearance.effective()` streams all controls (`ThemeManager.java:71-79`, `ModuleAppearance.java:101`). This happens per HUD row, per animation, per color sample, and per glyph when a paint is animated.

**Before**
```java
public Setting<?> effective(String name) {
    return global.getValue() || controls.stream()
        .noneMatch(c -> c.setting().getName().equals(name) && supported(c))
        ? null : values.get(name);
}
```

**After**
```java
private final Map<String, Setting<?>> supportedValues = new HashMap<>();

private void rebuildSupportedValues() {
    supportedValues.clear();
    for (var control : controls) {
        if (supported(control)) supportedValues.put(control.setting().getName(), control.setting());
    }
}

public Setting<?> effective(String name) {
    return global.getValue() ? null : supportedValues.get(name);
}
```

Also cache `PageAppearance.pages()` once instead of returning `Collections.unmodifiableMap(pages)` on every `plain()` check (`ThemeManager.java:70`, `PageAppearance.java:22-24`).

Two more contributors in this path:

- `Animation.selectedDuration()` calls `channels.channelNumber(...)`, which builds a string key like `"HUD Entrance Duration"` and performs map lookups **per active animation per frame** (`ThemeManager.java:211`, `Animation.java:103`).
- `CustomTextRenderer.width()` calls `Theme.number("Font Scale", 1)` for every measurement (`CustomTextRenderer.java:321-325`). Cache the font scale by theme revision.

### 3. HUD update is O(n²) per frame

**Current:** `HudRenderer.update()` loops all modules; inside the loop it calls `listed(module)`, and `listed()` calls `ModuleManager.getByClass(ArrayListModule.class)` — a stream over all modules (`HudRenderer.java:106-113`, `:125-134`, `ModuleManager.java:108-110`). It does this twice per module. With 40 modules that is roughly 3,200 type checks and 80 stream allocations per frame, plus `content()` allocation per HUD element.

**Before**
```java
private boolean listed(Module module) {
    var array = modules.getByClass(ArrayListModule.class).orElse(null);
    return container(module) ? array != null && array.containers.getValue() : ...
}

for (Module module : modules.getModules()) {
    if (module.isEnabled() && listed(module) && !entries.containsKey(module)) { ... }
}
```

**After**
```java
private final ArrayListModule arrayList; // constructor injection

private boolean listed(Module module) {
    return container(module)
        ? arrayList.containers.getValue()
        : !(module instanceof HudModule) || arrayList.hud.getValue();
}

for (Module module : modules.getModules()) {
    boolean listed = listed(module); // compute once
    ...
}
```

The second half of the HUD cost is `HudContent` generation. `extractElement()` calls `element.module().content()` every frame (`HudRenderer.java:169`), then walks the content list twice with streams (`:177`), and `drawLine()` recomputes `lineWidth()` after it was already measured (`:201-204`). Modules such as FPS, coordinates, memory, and potion effects therefore rebuild their line/span lists at frame rate.

**Recommended shape**

```java
private record CachedContent(long revision, List<HudContent.Line> lines,
                             List<LineLayout> layouts, double width, double height) { }

private CachedContent contentFor(HudModule module) {
    long revision = module.stateRevision() ^ module.enabledRevision();
    CachedContent cached = contentCache.get(module);
    if (cached == null || cached.revision() != revision) {
        // build lines once, measure each line once, store layouts
    }
    return cached;
}
```

This removes the per-frame `content()` allocation, the duplicate `lineWidth()` measurements, and the stream pipelines. It also makes HUD layout stable when only unrelated modules change.

### 4. Chunk motion: per-frame map rebuild and boxing

**Current:** `ChunkMotionTracker.frame()` creates a new `HashMap<Long, Float>`, inserts boxed `Long`/`Float` values for every moving section, then copies the whole map with `Map.copyOf()` (`ChunkMotionTracker.java:45-56`). `offset()` boxes the section key on every lookup (`:65`). `unloaded()` copies the entire offset map again (`:40-43`). `ChunkSectionMixin.getBoundingBox()` also creates a new `AABB` whenever an offset exists (`ChunkSectionMixin.java:18-22`).

**Before**
```java
var next = new HashMap<Long, Float>();
flights.entrySet().removeIf(entry -> {
    ...
    next.put(entry.getKey(), settings.initialOffset());
    ...
});
offsets = Map.copyOf(next);
```

**After**
```java
private final Long2FloatOpenHashMap scratch = new Long2FloatOpenHashMap();
private Long2FloatOpenHashMap offsets = new Long2FloatOpenHashMap();

public void frame(long now, Settings settings) {
    scratch.clear();
    flights.entrySet().removeIf(entry -> {
        ...
        scratch.put(entry.getKey(), value);
        ...
    });
    var previous = offsets;
    offsets = scratch;   // swap, then clear the old map next frame
    previous.clear();
}

public float offset(int x, int z) {
    return offsets.get(key(x, z));
}
```

Also cache the section `AABB` in a `@Unique` field and update it only when the offset changes, rather than allocating a new `AABB` per section per frame. `SodiumChunkMotionMixin` should use the same primitive lookup (`SodiumChunkMotionMixin.java:47`, `:64`).

### 5. `WorldRenderer`: streams, `Vec3` allocation, and per-quad arrays

**Current:** every frame `WorldRenderer.submit()` allocates `new boolean[] { true, false }` and calls `submitLines()` twice (`WorldRenderer.java:29`). Each pass filters lines and quads with streams and `toList()` (`:32-33`). The quad draw callback allocates a `Vec3[]` and `int[]` per quad (`:38`). The line path allocates a `Vec3` for each endpoint, a normalized direction, an `ArrayList`, and a `List.copyOf` (`:47-54`). For a few hundred lines this is thousands of objects per frame.

**Before**
```java
List<Render3DEvent.Line> snapshot = lines.stream()
    .filter(line -> line.visibleOnly() == visible).toList();
List<Render3DEvent.Quad> faces = quads.stream()
    .filter(face -> face.visibleOnly() == visible).toList();

Vec3[] points = {face.a(), face.b(), face.c(), face.d()};
int[] colors = {face.color(), face.colorB(), face.colorC(), face.colorD()};
```

**After**
```java
// One pass, no streams, no boolean array.
private final List<Line> visibleLines = new ArrayList<>();
private final List<Line> hiddenLines = new ArrayList<>();

private void partition() {
    visibleLines.clear();
    hiddenLines.clear();
    for (Line line : lines) {
        (line.visibleOnly() ? visibleLines : hiddenLines).add(line);
    }
    // same for quads
}
```

Better still, compute camera-relative endpoints during extraction and store primitive `double` arrays or a reusable geometry buffer. `Render3DEvent` can keep immutable snapshots for correctness, but `WorldRenderer` should not re-partition and re-copy them every frame.

### 6. `NametagsModule`: per-frame raycasts and equipment state

**Current:** for every visible player, every frame, `NametagsModule` builds a `Component`, formats health with `String.format`, allocates a new `ArrayList`, iterates `List.of(...)` equipment slots, copies every item stack, creates a new `ItemStackRenderState`, calls `updateForLiving()`, reads `getUUID()` multiple times, allocates a `List.copyOf`, creates a `Vec3`, and calls `mc.player.hasLineOfSight(p)` unless `through` is enabled (`NametagsModule.java:34-44`). This is one of the most expensive render paths in the client.

**Before**
```java
for (var p : mc.level.players()) {
    if (!camera.getCullFrustum().isVisible(...) ||
        (!through.getValue() && !mc.player.hasLineOfSight(p))) continue;

    var label = Component.empty();
    if (health.getValue()) label.append(String.format(Locale.ROOT, " %.0f♥", ...));
    ...
    List<ItemStackRenderState> items = new ArrayList<>();
    for (var slot : List.of(EquipmentSlot.MAINHAND, ...)) {
        var item = new ItemStackRenderState();
        mc.getItemModelResolver().updateForLiving(item, stack.copy(), ...);
        items.add(item);
    }
}
```

**After**
```java
UUID id = p.getUUID();
if (!through.getValue() && !lineOfSightVisible(p, now)) continue;

CachedLabel label = labelCache.computeIfAbsent(id, ignored -> new CachedLabel());
if (label.needsRefresh(now, p, mc)) {
    label.refresh(p, mc); // at 4–5 Hz, not per frame
}

List<ItemStackRenderState> items = equipmentCache
    .computeIfAbsent(id, ignored -> new ArrayList<>());
if (equipmentDirty(id, now)) refreshEquipment(p, items);
```

Throttle `hasLineOfSight()` to roughly 5–10 Hz and refresh equipment render states at tick rate. Reuse the `seen` set and avoid `String.format` in the render loop by caching formatted strings or using `Component.literal` with preformatted values. This alone should materially reduce frame-time spikes on populated servers.

### 7. `ColorSetting.Paint`: allocation on every sample and opacity change

**Current:** `paint()` creates a new record every call; `sample()` calls `paint()`; `opacity()` creates another record; `apply()` calls `paint()` twice (`ColorSetting.java:45-50`, `:70-75`). `Theme.paint(name)` calls `colorSetting(name).paint()`, and `RenderUtil.drawPaintRect()` immediately calls `paint.opacity(...)` (`RenderUtil.java:87-89`). Animated text calls `paint.sample(...)` per glyph (`CustomTextRenderer.java:439`).

**Before**
```java
public Paint paint() {
    return new Paint(getValue(), secondary, mode, direction, speed, spacing);
}

public int sample(double x, double y) {
    return paint().sample(x, y, ColorUtil.time());
}

public Paint opacity(double multiplier) {
    ...
    return new Paint(p, s, mode, direction, speed, spacing);
}
```

**After**
```java
private Paint cachedPaint;

private void invalidatePaint() { cachedPaint = null; }

public Paint paint() {
    Paint value = cachedPaint;
    if (value == null) cachedPaint = value = new Paint(...);
    return value;
}

public int sample(double x, double y) {
    return sampleComponents(getValue(), secondary, mode, direction,
            speed, spacing, x, y, ColorUtil.time());
}
```

For opacity, either pass the alpha multiplier into `sampleComponents()` or cache a small set of alpha-adjusted paints. Do not allocate a `Paint` per rectangle or per glyph.

### 8. `SettingCodecs.encode()` creates a `Gson` per setting

**Current:** `new Gson().toJsonTree(value)` runs for every non-color, non-enum setting (`SettingCodecs.java:13-17`). `ConfigManager.saveSnapshot()` walks every module and every setting (`ConfigManager.java:235-264`), so a save can construct hundreds of `Gson` instances. `ModuleAppearance.json()` and `PageAppearance.json()` do the same (`ModuleAppearance.java:113`, `PageAppearance.java:60`).

**Before**
```java
public static JsonElement encode(Setting<?> setting, Object value) {
    if (setting instanceof ColorSetting color) return color.toJson();
    if (value instanceof Enum<?> option) return new JsonPrimitive(option.name());
    return new Gson().toJsonTree(value);
}
```

**After**
```java
private static final Gson GSON = new Gson();

public static JsonElement encode(Setting<?> setting, Object value) {
    if (setting instanceof ColorSetting color) return color.toJson();
    if (value instanceof Enum<?> option) return new JsonPrimitive(option.name());
    return GSON.toJsonTree(value);
}
```

Better: pass the existing `ConfigManager.gson` or a shared `JsonCodec` instance into these methods so there is exactly one configured `Gson` in the application.

### 9. Theme and profile saves block the client thread

**Current:** `ThemeManager` subscribes to `UpdateEvent`; 400 ms after a change it calls `save()` synchronously (`ThemeManager.java:284-286`), which serializes the entire theme — including every module appearance — and writes it on the client thread (`:401-404`). `ProfileManager.saveProfile()` also writes synchronously (`ProfileManager.java:37-41`), and `ClickGui` invokes it directly from an action button (`ClickGui.java:333-341`).

**Before**
```java
if (dirtySince != 0 && now >= retryAfter && now - dirtySince >= 400_000_000L) {
    save();
}
```

**After**
```java
if (dirtySince != 0 && now >= retryAfter && now - dirtySince >= 400_000_000L) {
    saveAsync(); // snapshot on client thread, write on the config writer
}
```

`ThemeManager.write()` already uses a temporary file and atomic move, which is good. The remaining problem is *where* it runs. Reuse the `ConfigManager` writer or a dedicated single-thread executor, and guard completion with a revision so a newer edit cannot be cleared by an older write. Do the same for profile saves, or switch `ClickGui` to `ProfileManager.savePrepared()`.

### 10. Silhouette and item-outline targets are full resolution

**Current:** `ItemOutlineRenderer` allocates a full-window RGBA8 mask and a full-window RGBA8 horizontal dilation target, plus a depth copy (`ItemOutlineRenderer.java:92-98`, `:54`). Each pooled `SilhouetteMask` does the same (`SilhouetteMask.java:88-95`). At 4K, each RGBA8 target is about 33 MB; a mask/horizontal pair is about 66 MB, before depth. If the silhouette pool grows, VRAM use grows with it.

`SilhouetteOutlineRenderer.collector()` also allocates a `Key`, a dynamic `Proxy`, an `InvocationHandler` lambda, and performs `method.getName()` reflection for every entity, every frame (`SilhouetteOutlineRenderer.java:30-53`). `SilhouetteMask.request()` allocates a `Matrix4f` and eight `Vector4f`s per entity (`SilhouetteMask.java:104-109`).

**Before**
```java
mask = new TextureTarget("risecode item silhouette",
        main.width, main.height, true, GpuFormat.RGBA8_UNORM);
horizontal = new TextureTarget("risecode silhouette dilation",
        main.width, main.height, false, GpuFormat.RGBA8_UNORM);
```

**After**
```java
// Half-resolution mask is usually indistinguishable after dilation and compositing.
int scale = 2;
mask = new TextureTarget("risecode item silhouette",
        Math.max(1, main.width / scale), Math.max(1, main.height / scale),
        true, GpuFormat.RGBA8_UNORM);
horizontal = new TextureTarget("risecode silhouette dilation",
        Math.max(1, main.width / scale), Math.max(1, main.height / scale),
        false, GpuFormat.RGBA8_UNORM);
```

Alternatively, allocate a bounded target sized to the union of the requested screen-space bounds, capped at something like 1024×1024. Cache the proxy per `(channel, style, visible)` instead of creating one per entity, and reuse a scratch `Matrix4f`/`Vector4f` set.

### Other hot spots worth measuring

- `PlayerEspModule` creates a `Vec3`, an `AABB`, a `Style`, a `Paint`, and a submission lambda per player per frame (`PlayerEspModule.java:35-45`). Cache styles by color/width/visibility and avoid `bounds.move()`/`inflate()` allocations where possible.
- `TracersModule` samples the selected paint twice per player per frame (`TracersModule.java`). Cache the resolved paint and alpha; do not call `sample()` twice for the same endpoints.
- `FriendManager.isFriend(uuid, name)` normalizes the name on every call (`FriendManager.java:54-56`); nametags/ESP call it per player per frame. Cache normalized names or expose a UUID set.
- `TargetUtil.findTargets()` builds and sorts a full candidate list, including line-of-sight checks, on every target-selection tick (`TargetUtil.java:33-44`). For distance-only sorting, a single-pass minimum avoids the list and sort.
- `HitboxesModule` in `ALWAYS` mode allocates `HashSet`s, `AABB`s, `Request`s, and style lists per frame. Reuse collections and cache the style by theme revision.
- `BlockHighlightMotion.normalize()`/`transform()` use streams and create new `Part`/`AABB` lists per frame (`BlockHighlightMotion.java:62-68`). Use reusable arrays or a preallocated list.
- `Render3DEvent` validates every `Line` and every `Quad` in canonical constructors, and `Quad` allocates `List.of(a,b,c,d)` just to validate (`Render3DEvent.java:22-29`). Validate with four direct calls, or gate validation behind a debug flag.
- `RenderUtil.ellipsize()` repeatedly calls `textWidth()` and `substring()` in a loop (`RenderUtil.java:186-193`), which is O(n²) for long strings. Use binary search over code-point indices.
- `ResourcesPreviews` performs `NativeImage.read()` and texture upload on the render thread when a future completes. Decode on the worker; upload at most one or two textures per frame.

---

## 🎨 Rendering & Mixin Evaluation

### Strengths

The rendering architecture is modern and generally well-behaved:

- No raw `gl*` calls or manual matrix stacks outside the game's abstractions.
- Immutable extraction data; no live entity references in draw callbacks.
- Explicit `UiBatch` batching that flushes on pipeline, texture, or scissor changes (`UiBatch.java:23-37`).
- Dedicated `ShaderWorldOverlay` output target so custom lines/quads are not overwritten by Iris's final pass.
- Through-wall pipelines with depth state removed, rather than toggling global GL state.
- Conditional Sodium/Iris mixins through `RendererMixinPlugin`.
- `@Unique`, `@Shadow`, MixExtras `@WrapOperation`/`@WrapMethod`, and `@Pseudo` used appropriately.
- No `@Overwrite` and no wildcard method selectors.

That discipline matters. Many clients at this scale are a pile of `@Redirect` and `RenderSystem` state leaks.

### Mixin surface and compatibility

Counted from source: **116 mixins**, roughly **117 `@Inject`**, **22 `@Redirect`**, **6 `@ModifyVariable`**, and **21 `@WrapOperation`/`@WrapMethod`**.

The `@Inject` count is not inherently bad. The `@Redirect` count is the concern. `@Redirect` replaces the original invocation entirely, so it composes poorly with other mods that also redirect the same call. `@WrapOperation` is generally the better choice because it preserves the original call and lets multiple mods wrap it.

The clearest example is `MouseHandlerMixin`, which has six redirects around `Screen.mouseClicked`, `mouseReleased`, `mouseDragged`, `mouseScrolled`, and `mouseMoved` (`MouseHandlerMixin.java:17-31`). It reimplements pointer-coordinate mapping for every screen. That is a high-conflict design: any UI mod that also wraps those methods can produce double-transformed coordinates or skip one of the handlers. Prefer `@WrapOperation`, or use Fabric's screen events where they cover the same behavior.

Other broad targets worth auditing:

| Target | Mixin(s) | Risk |
|---|---|---|
| `Screen` | `MenuScreenMixin` | Affects every screen, including mod screens |
| `MouseHandler` | `MouseHandlerMixin` | Six redirects; input conflicts |
| `KeyboardHandler` | `KeyboardHandlerMixin` | Cancellable `keyPress` HEAD; text input ordering |
| `Hud` | `CrosshairMixin` | Pose lifetime across cancellable render |
| `ItemInHandRenderer` | `ViewModelMixin` | Pose lifetime and shared mutable field |
| `SectionRenderDispatcher` | `ChunkSectionMixin` | Per-section allocation and visibility overrides |
| `GameRenderer` | `AnimationFrameMixin` | Global frame clock; runs even before client init |

For each of these, add a short comment stating the target method contract and the expected cancellation behavior. Consider splitting mixin configs by domain (input, render, chunk, network) so optional compatibility plugins can enable/disable narrower groups.

### Pose and context lifetime bugs

These are the most important correctness issues in the mixin layer.

**`CrosshairMixin`** pushes a matrix at `HEAD` and pops at `RETURN` (`CrosshairMixin.java:13-19`). If another mixin cancels `extractCrosshair`, or if the method throws, the pop never runs and the pose stack is corrupted for the rest of the frame.

**`ViewModelMixin`** does the same for `renderItem` and `renderMap` (`ViewModelMixin.java:23-35`). It also stores `riserecode$mapHand` in a mutable field on the shared `ItemInHandRenderer` instance (`:21`). If rendering is reentrant or an exception occurs between `submitArmWithItem` and `renderMap`, the hand context can be stale.

**`MenuScreenMixin`** sets `PageAppearance.context(...)` at `HEAD` and resets it only at `TAIL` (`MenuScreenMixin.java:16-17`, `:39-40`). Cancellation or an exception leaves the page context active for subsequent rendering.

**Fix pattern:** use `@WrapMethod` and `try/finally` for the whole target method, or use MixExtras' `@WrapOperation` around the specific call. For example:

```java
@WrapMethod(method = "extractCrosshair")
private void riserecode$crosshair(GuiGraphicsExtractor g, DeltaTracker delta,
                                  Operation<Void> original) {
    double scale = Client.INSTANCE.isInitialized()
            ? Client.INSTANCE.getEventBus().post(new CrosshairEvent()).scale() : 1;
    g.pose().pushMatrix();
    try {
        g.pose().translate(g.guiWidth() / 2f, g.guiHeight() / 2f);
        g.pose().scale((float) scale, (float) scale);
        g.pose().translate(-g.guiWidth() / 2f, -g.guiHeight() / 2f);
        original.call(g, delta);
    } finally {
        g.pose().popMatrix();
    }
}
```

Use the same shape for `ViewModelMixin.renderItem`/`renderMap`, and wrap `MenuScreenMixin` with a scoped `PageAppearance` context.

### `KeyboardHandlerMixin` double post

`KeyboardHandlerMixin` posts a priority `KeyPressEvent` for the ClickGui/capture case, then falls through and posts the normal event as well (`KeyboardHandlerMixin.java:15-30`). In practice `KeybindManager` usually cancels the first event, so the duplicate is masked. But it is still a latent bug: any listener that consumes the priority event without cancelling, or any future change to `KeybindManager`, can cause the same key press to be handled twice. Restructure it as an explicit early return:

```java
if (priorityHandled) {
    callback.cancel();
    return;
}
// normal path posts exactly once
```

### `Render3DEvent.targetHighlight` throws inside the render path

`Render3DEvent.targetHighlight()` throws `IllegalStateException` if two providers request the aimed highlight in one extraction (`Render3DEvent.java:32-34`). Today `HitboxesModule` and `BlockOverlayModule` are usually mutually exclusive by hit type, but this is a fragile invariant. A future module, a compatibility mode, or a changed hit result can turn it into a frame crash. Prefer a priority/merge policy, or log and ignore the second request rather than throwing during rendering.

Similarly, `WorldRenderer.submit()` runs module-supplied submissions without a `try/catch` (`WorldRenderer.java:26-28`). One failing ESP/outline submission aborts the rest of the world overlay and propagates into the frame. Wrap each submission, log the failure, and continue.

### Render pipeline notes

- `ShaderWorldOverlay.active()` is called multiple times per frame and performs FabricLoader/Iris checks (`WorldRenderer.java:36`, `:56`). Cache the result on init and resource reload.
- `UiBatch` batching is correct: it flushes on pipeline, texture setup, or scissor changes and uses `List.copyOf` per batch (`UiBatch.java:32-37`). The remaining cost is one `Quad` per primitive/glyph, not the batching itself.
- `MenuTransition.capture()` wraps each state when opacity is below 1. That adds a wrapper object per primitive. If the wrapper only changes alpha, fold the alpha into the `Quad` color at extraction time.
- `AmbienceRenderer` uses static mutable state and reflection for Iris, but the classloading is guarded and the state is render-thread confined. Document that confinement; do not let UI/network code touch it.
- `ItemOutlineRenderer.TYPES` is a static `HashMap` keyed by atlas + held state (`ItemOutlineRenderer.java:45`). It is cleared on close but not on resource reload. Verify that cached `RenderType`s remain valid across pack reloads, or clear the map from the reload listener.
- `SilhouetteOutlineRenderer` uses `Proxy.newProxyInstance` and `method.getName()` dispatch for every entity (`SilhouetteOutlineRenderer.java:39-52`). This is both allocation-heavy and reflective. A small concrete delegating collector, or a prebuilt wrapper cached per batch, would be faster and easier to debug.

---

## 🧵 Concurrency & Stability

### What is already safe

- `EventBus` registry mutation is synchronized, dispatch uses a volatile snapshot, and registration state is volatile (`EventBus.java:26-28`, `:79-90`, `:132`).
- `FriendManager` publishes immutable `Map` snapshots through a volatile field (`FriendManager.java:18`).
- `ConfigManager` uses volatile dirty/writing/revision flags, a single writer thread, a write lock, and atomic JSON storage (`ConfigManager.java:68-73`, `:268-286`).
- `ResourcesManager` performs selection and application on the client thread and returns completion there (`ResourcesManager.java:58-108`).
- `ProfileManager`, `MapService`, and `DiscordActivity` use dedicated worker threads and marshal UI/game mutations back to the client thread.
- `Module` lifecycle and subscriptions are explicitly client-thread owned, and `ModuleAppearance.with()` restores context with `try/finally`.
- `WorldRenderer` publishes line/quad/submission snapshots through volatile fields (`WorldRenderer.java:16-18`).

This is a good baseline. The missing piece is an explicit, enforced thread policy.

### Risks

1. **The event bus runs callbacks on the posting thread, including Netty.**
   `PacketEvent.Send`/`Receive` are posted directly from `ConnectionMixin` (`ConnectionMixin.java:26`, `:40`). `NoFallModule` handles them with volatile fields and immutable packet replacement (`NoFallModule.java:23-25`, `:135-142`); `AutoToolModule` uses a volatile inventory flag (`AutoToolModule.java:26`); `BlockAnimationsModule` correctly marshals to `mc.execute()` and checks `enabledRevision()` (`BlockAnimationsModule.java:68-70`). That is good discipline, but it is convention, not enforcement. A new module can easily mutate `ClientLevel`, inventories, or UI state from the Netty thread.

   **Recommendation:** mark events with a thread policy, e.g. `ClientThreadEvent`/`AnyThreadEvent`, and assert in debug builds. For game-mutating packet listeners, enqueue to `Minecraft.execute()` and check `isEnabled()` plus the module's enabled revision.

2. **`PageAppearance.context` is not exception-safe.**
   As above, `MenuScreenMixin` relies on `TAIL` to reset it. A cancelled render or exception leaves a stale page context, which changes theme resolution for later draws. Use a scoped `try/finally` helper.

3. **Shutdown ordering is fragile.**
   `Client.shutdown()` closes `configManager` before `moduleManager.disableAll()` (`Client.java:154-155`). Module `onDisable()` methods can still call `requestSave()` or change settings, but the writer is already shut down. A failure in an earlier close step also skips the remaining cleanup and leaves `initialized` true. Wrap the whole shutdown in `try/finally`, disable modules first, save last, and make it idempotent.

4. **Async profile loads have no generation guard.**
   `ProfileManager.loadProfileAsync()` prepares on the IO thread, applies on the client thread, and completes `whenComplete` (`ProfileManager.java:50-52`). If the user starts two loads quickly, the older result can apply after the newer one. Add an `AtomicLong` request id and drop completions whose id is no longer current.

5. **Profile application disables and re-enables every module.**
   `ConfigManager.commit()` disables all modules, loads settings, then re-enables all of them (`ConfigManager.java:349-364`). That triggers every module's `onDisable`/`onEnable` side effects, potentially twice on rollback. Apply settings only to modules whose state actually changes, or apply while disabled and toggle only the changed set.

6. **Subscription close during dispatch is only partially safe.**
   `EventBus` checks `registration.active` before invoking, but a dispatch already in progress can still invoke a listener after `close()` if the check passed. On Netty, that can overlap with `onDisable()` cleanup. For stateful packet listeners, either defer cleanup to the next client tick or document that `onDisable()` must be safe against one in-flight callback.

7. **Synchronous theme/profile saves can hitch.**
   `ThemeManager.save()` and `ProfileManager.saveProfile()` run on the calling thread, including the render thread from ClickGui actions. Move them to the existing config writer and use revisioned snapshots.

8. **`AnimationManager.driving` is never reset.**
   Once `frame()` runs, `isDriving()` stays true, so themed animations ignore explicit deltas (`Animation.java:87-91`). That is intentional while the central frame clock runs, but if frame advancement stops, themed animations freeze. It is a minor robustness issue, not a crash.

### Recommended thread policy

Introduce a small contract at the event bus boundary:

- `ClientThreadEvent`: assert `Minecraft.getInstance().isSameThread()` in debug; in release, log once and enqueue.
- `AnyThreadEvent`: explicitly allowed on Netty; must use volatile/immutable state only.
- `RenderThreadEvent`: assert render thread; no game mutation.
- `PacketEvent`: `AnyThreadEvent`; game-mutating subscribers must marshal.

This would make the existing good patterns explicit and prevent future regressions.

---

## 🚨 Critical Red Flags & Bugs

| Severity | Issue | Evidence | Fix |
|---|---|---|---|
| **P0** | Per-glyph `UiMesh.quad()` allocation | `UiMesh.java:40-55`, `CustomTextRenderer.java:441-443` | Reuse scratch matrix/bounds; batch glyph runs into one render state |
| **P0** | Nametags raycast + equipment rebuild every frame | `NametagsModule.java:34-44` | Throttle LOS/equipment to 4–10 Hz; cache labels and item states |
| **P0** | `new Gson()` per setting per save | `SettingCodecs.java:13-17`, `ConfigManager.java:235-264` | Shared static/configured `Gson` |
| **P1** | HUD O(n²) `getByClass` per module | `HudRenderer.java:106-113`, `:125-134` | Inject `ArrayListModule`; compute `listed` once |
| **P1** | Chunk motion map rebuild and boxing | `ChunkMotionTracker.java:45-66` | Primitive map, reuse/swap, cache section `AABB` |
| **P1** | World overlay streams + `Vec3`/array churn | `WorldRenderer.java:29-54` | One-pass partition, precomputed relative geometry |
| **P1** | Silhouette dynamic proxy + full-res targets | `SilhouetteOutlineRenderer.java:30-53`, `SilhouetteMask.java:88-95` | Cache wrapper, half-res/bounded targets |
| **P1** | Theme/profile saves block client thread | `ThemeManager.java:284-286`, `:401-404`, `ProfileManager.java:37-41` | Async writer + revision guard |
| **P1** | `PageAppearance.context` leaks on cancel/throw | `MenuScreenMixin.java:16-17`, `:39-40` | `@WrapMethod` + `try/finally` |
| **P1** | Pose push/pop imbalance | `CrosshairMixin.java:13-19`, `ViewModelMixin.java:23-35` | Wrap target method with `try/finally` |
| **P1** | Broad `@Redirect` screen input | `MouseHandlerMixin.java:17-31` | `@WrapOperation` or Fabric screen events |
| **P2** | Config closed before modules disabled | `Client.java:144-161` | Disable modules first, save last, `try/finally` |
| **P2** | Double `KeyPressEvent` post | `KeyboardHandlerMixin.java:15-30` | Explicit early return after priority handling |
| **P2** | `targetHighlight` throws in render path | `Render3DEvent.java:32-34` | Merge/priority or log-and-ignore |
| **P2** | Duplicate theme setting names silently overwrite | `ThemeManager.java:201-204` | Reject duplicates at construction |
| **P2** | Async profile loads can apply out of order | `ProfileManager.java:50-52` | Request id / generation guard |
| **P2** | Profile apply disables/re-enables all modules | `ConfigManager.java:349-364` | Apply only changed modules |
| **P2** | Name-based module identity/classification | `Module.java:75-83`, `ModuleManager.java:107` | Stable IDs/enum metadata |
| **P2** | `popDuration()` mutates theme state during render | `Module.java:42`, `ModuleAppearance.java:122-127` | Move migration to startup/enable |
| **P3** | `ellipsize()` O(n²) measurement | `RenderUtil.java:186-193` | Binary search over code points |
| **P3** | `FriendManager.isFriend()` normalizes per call | `FriendManager.java:54-56` | Cache normalized names / UUID set |
| **P3** | `TargetUtil` full sort per selection | `TargetUtil.java:33-44` | Single-pass minimum for distance mode |

### The three I would fix first

1. **Persistence:** shared `Gson`, async theme/profile writes, and a sparse `ModuleAppearance`. This removes save-time stutter and a large amount of config/theme memory.
2. **HUD and text:** remove the O(n²) `getByClass` churn and batch glyph rendering. This affects every frame and every screen.
3. **Nametags and world overlays:** throttle raycasts/equipment and stop re-partitioning/re-allocating geometry every frame. This is where populated servers will hurt most.

---

## 🗺️ Actionable Roadmap

### Quick Wins (3–5 items)

1. **Kill per-setting `Gson` and cache theme lookups.**
   - Static/configured `Gson` in `SettingCodecs`, `ModuleAppearance`, `PageAppearance`.
   - Precompute `Map<String, Setting<?>>` in `ModuleAppearance`.
   - Store the `Collections.unmodifiableMap` wrapper once in `ThemeManager`.
   - **Impact:** high; **Effort:** small.

2. **Remove HUD per-frame module scans.**
   - Inject `ArrayListModule` into `HudRenderer`; delete `getByClass` from `listed()`/`update()`.
   - Compute `listed` once per module per frame.
   - Cache `HudContent` by `stateRevision`/`enabledRevision`.
   - **Impact:** high; **Effort:** small.

3. **Make lifecycle and context handling exception-safe.**
   - `Client.shutdown()`: disable modules → save config/theme → close services, all in `try/finally`.
   - `PageAppearance`: scoped `try/finally` helper.
   - `CrosshairMixin`/`ViewModelMixin`: `@WrapMethod` with `try/finally`.
   - `KeyboardHandlerMixin`: single post, explicit early return.
   - **Impact:** high; **Effort:** medium.

4. **Replace chunk-motion map churn.**
   - `Long2FloatOpenHashMap` or a reused primitive map.
   - Remove `Map.copyOf` per frame; remove map copy on unload.
   - Cache section `AABB` in `ChunkSectionMixin`.
   - **Impact:** medium–high; **Effort:** medium.

5. **Throttle nametags and cache labels/equipment.**
   - LOS at 5–10 Hz; equipment at tick rate.
   - Cache `Component` labels by player + state.
   - Reuse `seen`/list objects; avoid `String.format` in the render loop.
   - **Impact:** high on populated servers; **Effort:** medium.

### Long-Term Goals

- **Rendering:** batch glyph runs into indexed vertex buffers; use a reusable frame arena for UI primitives; move silhouette/item-outline masks to half-resolution or bounded targets; add a frame-time/allocation budget.
- **Architecture:** replace hardcoded module registration with a registry/ServiceLoader/annotation processor; introduce typed theme keys instead of strings; make `ModuleAppearance` sparse; split `Client` into lifecycle/services.
- **Concurrency:** formalize event thread policies with assertions; enqueue Netty packet events for game mutations; make lifecycle transitions transactional.
- **Mixins:** replace `@Redirect` with `@WrapOperation` where possible; split mixin configs by target domain; maintain a Sodium/Iris/ETF/EMF compatibility matrix; add optional gating for known conflicting mods.
- **Config/theme:** one persistence service for config, theme, and profiles; schema migrations; async writes with revision checks; backup/restore on corrupt input.
- **Quality:** add a formatter and a small style check; add JMH or JFR-based allocation tests for HUD/text/config paths; add headless tests for codecs/event bus and client gametests for mixin behavior.

### Suggested order of work

1. Persistence and theme lookup caches — low risk, immediate save-time and memory wins.
2. HUD `getByClass` removal and content caching — small diff, visible frame-time improvement.
3. Context/pose lifetime fixes and shutdown ordering — correctness first, before more refactoring.
4. Chunk motion and world overlay allocation reduction.
5. Nametags/equipment/LOS throttling.
6. Glyph batching and half-res outline targets — larger rendering refactors, best done with profiling.

The codebase does not need a rewrite. It needs targeted hot-path work and a stricter lifecycle/thread contract around an already solid foundation.
