# SeedCracker

Minecraft 26.2 / Java 25 only. The SeedCrackerX 2.16.1 engine is built into
Risecode; do not install the standalone SeedCrackerX mod alongside it.

## Collect, then Start

Enable **World → SeedCracker**. Each enabled finder collects observations after
chunk application. **Rescan Loaded Chunks** queues resident chunks. No observation,
login, restore, toggle or database response starts cracking. **Start** takes an
immutable snapshot; data collected during an attempt belongs to the next manual
Start. **Stop** cancels workers and network requests while collection continues.
Disable stops collection, cracking, network work and outlines and clears active
session state. Saved jobs never resume. Workers default to
`max(1, min(4, availableProcessors − 1))`; changes apply at the next Start.

The controls show observation counts, regular/lifting bits, the current phase,
candidate counts and results. Structure seeds contain 48 generation bits and are
not full world seeds. Candidate world seeds are tested against the server's
64-bit SHA-256-derived seed hash. Only hash-verified world seeds are offered by
**Copy Seed**. A missing/zero hash cannot confirm a full world seed. A server can
hide or spoof its hash; custom generation can invalidate observations.

Collect at least **40 lifting bits and 32 regular bits**, preferably several
independent structure kinds. Alternatively, collect **32 regular bits from End
cities plus all ten central End pillars**. More data reduces false candidates.
Pillager outposts contribute lifting bits only, following upstream's restriction.
The bounded search stops at 250,000 structure candidates; collect more data and
Start again. Hash searches run when at most 1,000 structure candidates remain.
No result is a useful outcome: collect additional independent structures and
check the generation profile rather than assuming the first candidate is correct.

## Finders and outlines

Independent switches: buried treasure, desert pyramids, End cities, jungle
temples, ocean monuments, swamp huts, shipwrecks, pillager outposts, igloos,
trial chambers and End pillars. Unreliable modern-era dungeon, emerald,
desert-well, End-gateway, warped-fungus and biome cracking are unavailable.
Biome gates are confined to the engine's positive structure patterns; Risecode
does not replace Minecraft's biome source. Player-built lookalikes, modified or
obfuscated structures can yield incorrect observations.

Outlines: **Off**, **Visible** (native depth), **Through Walls**. They use
Risecode's existing world overlay renderer and color settings. Dimension travel
clears overlays and queued scans, cancels cracking, and retains compatible
observations. Unload drops pending scans/outlines. A scan queue holds at most
8,192 finder tasks; one task runs on the client thread each tick. The queue
reports overflow; use Rescan after it drains. Scan cost depends on the finder
and world height. Debug reports cumulative finder CPU time, not GPU/FPS claims.
Observation storage is capped at 4,096; clear old data if this limit is reached.

## Storage and generation

Archives live under `config/riserecode/seedcracker/`, keyed by a SHA-256 digest of
server address or local world path, seed hash, generation mode and **Generation
Profile**. The profile is a user label: change it for multi-world servers or
changed datapacks/salts. Remote servers do not expose their full generator
settings to clients; their vanilla compatibility cannot be inferred from version
alone. Flat/debug worlds and custom dimension IDs are unsupported. Older-server
translation is outside this release.

Data saves periodically and when leaving/disabling, using a temporary file and
atomic replacement. Fresh sessions retain older compatible saved observations
when writing archives. **Restore Saved Data** explicitly replaces active data
with the matching archive and never starts a job. **Clear Data** explicitly
clears the current archive as well as active observations/results. Invalid saves
are preserved and reported; restore failure leaves active data intact. No
runtime configuration, server identities or personal archives enter Git/releases.

## Local commands

With the default `.` prefix:

```
.seedcracker on
.seedcracker off
.seedcracker start
.seedcracker stop
.seedcracker rescan
.seedcracker clear
.seedcracker restore
.seedcracker copy
.seedcracker status
.seedcracker lookup
.seedcracker submit
.seedcracker set "Find Shipwrecks" false
.seedcracker set "Outlines" THROUGH_WALLS
.seedcracker set "Cracking Workers" 2
.seedcracker set "Generation Profile" second-world
```

All setting names also work with `set`; boolean values use `true`/`false`.
Commands are handled locally by the configured Risecode prefix.

## Optional database

Both toggles start off. **Database Lookup** permits the manual **Lookup** action,
which downloads the upstream public Google CSV. Seeds are confirmed only after
computing their hash and matching the current server hash; an address match or
the CSV's own hash column is not sufficient. No usable hash means no lookup.

**Allow Submission** permits manual **Submit Verified Seed**, never automatic
submission. Upstream eligibility is retained: one verified seed, more than ten
online players, a remote connection, and a licensed Minecraft account that passes
session authentication. The action sends **server address, dimension, seed,
version, username and anonymous flag** to the upstream Google Apps Script. It
also authenticates your Minecraft account to the `seedcrackerx` service ID.
Anonymous Submission sets upstream's anonymization flag; the service still
receives your username. This explanation appears beside the action. Requests
have connection/request timeouts; Stop, travel, disable or permission changes
cancel them and reject late callbacks. Automated database checks use a local
mock service; they do not submit real worlds.

## Provenance

[SeedCrackerX 2.16.1](https://github.com/19MisterX98/SeedcrackerX/tree/b72a829f768967fa737a4f1f63b32e37b9619152)
commit `b72a829f768967fa737a4f1f63b32e37b9619152` (MIT). SeedFinding and LattiCG
JARs are extracted from the checksum-verified official upstream release, avoiding
mutable Maven resolution. Full notices accompany all release artifacts.
