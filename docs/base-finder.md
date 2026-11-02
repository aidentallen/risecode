# Base Finder and suspicious chunks

Enable **BaseFinder** in Render. It starts disabled, scans only terrain the server
has sent, and never requests chunks, probes blocks, or sends movement packets.
The module combines block outlines, chunk highlights, centroid tracers, and
frosted discovery cards. It uses the existing native geometry/blur pipelines;
no raw OpenGL calls or terrain/fluid replacements are introduced.

## Detection

- **Classic:** separately compares mature growth and enabled block-entity counts
  against Classic Threshold (default 8).
- **ANBS+:** original six-connected geode grouping plus 4×4×4 clue density and
  horizontal layout checks. Spatially scattered equal-count clues can be rejected.
  This is an original implementation inspired by the requested algorithm family,
  not imported Anubis or Amethyst Scanner source.
- **Activity Clues:** containers ×3, machinery ×2.5, redstone ×2, geode shape clues
  ×1.5, mature growth ×1. Sensitivity is the required score: **lower finds more**.
  Its default is 30. The displayed score is capped at 100, not a calibrated
  probability of finding a player base.

Containers include chests/trapped chests, barrels, shulkers, hoppers,
droppers/dispensers, furnaces/smokers/blast furnaces, and brewing stands.
Shulkers, chests and hoppers have independent filters. Machinery includes pistons,
sticky pistons and observers; adjacent hopper/dropper pairs with redstone logic
add a static clock-layout clue. This does not establish that a clock is running.
Growth includes mature wheat/carrots/potatoes/beetroots/nether wart, full amethyst
clusters, terminal sugar cane/kelp above at least two matching growth blocks, and
attached pumpkin/melon stems with adjacent fruit. Growth can be enabled separately
from geode analysis.

The false-positive filter recognizes local cobble + mossy cobble + spawner,
rail + web, reinforced deepslate/soul lantern + sculk, and trial spawner/vault +
tuff/copper combinations. It suppresses nearby cells, preserving independent
clues elsewhere in the chunk. It is a block-combination approximation, not access
to server structure metadata.

## Amethyst methods

- **Bypass:** reports received amethyst/budding blocks exposed to adjacent air or
  transparent cells, and exposed cells in validated geode groups. Isolated exposed
  calcite or basalt does not suffice. This method does not require Min Cells.
- **Light:** decodes both full chunk-with-light and incremental light packets.
  Below Y=0, a received block-light value of five with known zero skylight is a
  clue only when no in-chunk neighbor has stronger block light. Unknown skylight
  does not count as darkness. Sparse update masks retain prior layers and empty
  masks clear them. Light alone has a low display score, capped at 45.
- **ANBS+ Scan:** six-neighbor BFS groups amethyst, budding amethyst, calcite and
  smooth basalt. At least Min Cells (default 12) and an amethyst/budding member
  are required; basalt/calcite formations alone are rejected. Validated geodes
  can appear independently of the general activity threshold. Shell openings,
  artificial blocks next to budding amethyst, and sampled hollow interiors each
  contribute at most one geode shape clue. Natural geodes can also match.

All analysis uses received data. Paper/Purpur may replace real blocks or transmit
fake ones; a light value of five is not proof of an amethyst emitter, and no
client heuristic can guarantee recovery of data a server did not send. See
[Paper's anti-Xray documentation](https://docs.papermc.io/paper/anti-xray/).
Clusters and neighbor tests stop at chunk borders. A geode split across chunks
may fall below Min Cells in each part. No remote-server detection rate or
Paper/Purpur Engine 1/2 bypass guarantee has been measured.

## Visuals and cards

General settings, clue filters, amethyst controls, outlines, and discovery cards
have separate headings. Range is 1–16 chunks (default 8). Choose Bounding Box,
Surface Grid, Corners Only, or None; block outlines and tracers are independent.
Geometry is depth tested, so terrain can occlude discoveries. Chunk color follows
the highest-weight enabled clue type. Fill Alpha defaults to 0.035; use zero to
remove translucent faces. Geometry extraction caps at 32 nearby chunks and 256
block outlines, with 128 stored block targets per discovery.

Cards show an item icon, clue type, score, coordinates, live distance/direction,
container/machinery/redstone counts, mature amethyst/growth counts and geode/light
counts. Choose top-right or bottom-right, three glass tints, 1–6 visible cards,
and a timeout (default 30 seconds; zero retains cards while in range).
**P** pins/unpins the first visible card; **Delete** dismisses existing cards.
Both actions have configurable key choices, including None. Pinned cards still
leave on unload, world change, disable, or leaving range. Reduced Motion removes
the entry slide/fade. Blur follows existing theme and performance controls.

## Lifetime and resource limits

Vanilla accepts packets on the client thread; the scanner's independent palette
parsing, light decoding, growth analysis and heuristics run on one daemon worker.
There are at most 16 queued jobs, 32 decoded chunk states, and 256 tracked tickets
and discoveries. Overload drops work and invalidates its ticket, never executes
scanner analysis on the caller. Consecutive same-chunk updates share analysis.
Resident chunks are backfilled gradually from private section/light copies, at
most one chunk every two ticks with queue headroom. Evicted decoded states are
eligible for backfill; immutable discoveries remain until invalidated.

Workers never read live chunks, levels, settings or GUI objects. Ticket identity
checks reject stale results after unload/reload and world/setting changes.
Disable cancels the worker, clears discoveries and releases EventBus listeners.
Discoveries are session-only and are not written into user configuration.

## Verification

`./gradlew baseFinderCheck` covers deterministic threshold, topology, structure,
noise and light fixtures. `./gradlew runClientGameTest -PbaseFinderOnly
-PresourcesOffline` exercises real client hooks, palette decoding, incremental
changes, sparse light updates, native pin/clear input, geometry extraction,
settings invalidation, and overload/unload behavior in a disposable world.
Native renderer runs exercise startup and extraction; they do not establish
pixel-accurate occlusion, long-session memory stability, or an FPS improvement.
