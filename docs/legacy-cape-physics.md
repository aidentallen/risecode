# Legacy cape physics

`Render → Cape Physics` is a client-only module for vanilla player capes. It starts
disabled and affects both local and remote players. `Motion Strength` ranges from
0 to 2 in 0.01 increments; 1 reproduces the reference amplitudes. The resting 6°
and the 25° sneak offset are retained at every strength. Player movement, camera
bob, cape ownership, textures and geometry are not changed.

The reference is Mojang's original 1.8.9 client, SHA-1
`3870888a6c3d349d3771a3e9d16c9bf5e076b908`, resolved from
[official version metadata](https://piston-meta.mojang.com/v1/packages/d546f1707a3f2b7d034eece5ea2e311eda875787/1.8.9.json).
`scripts/legacy-cape-reference/Oracle.java` drives its cape renderer (`bkp`) with
isolated player/graphics fixtures and records the actual rotation arguments.
The resulting numerical data is in `src/test/resources/dev/riserecode/cape/`.
Minecraft bytecode is not part of this project or its distributable.

For equivalent motion/pose traces, the implementation follows the legacy cloak
quarter-step history, per-axis teleport rule, horizontal walk-distance gating,
0.4 bob interpolation, float sine lookup and linear yaw interpolation. Forward
lean is nonnegative but has no upper cap; lateral sway has no modern ±20 cap.
Vertical movement is clamped to [-6, 32] before walk bob and sneak are added.
There is no default 0.93 scale factor or change to Minecraft's sprint speed.

In 26.2, `AvatarRenderer.extractCapeState(Avatar, AvatarRenderState, float)` is an
instance method. `capeFlap` contains vertical/bob motion, `capeLean` forward lean,
and `capeLean2` lateral sway; the 6° rest angle is applied in `PlayerCapeModel`.
Typed events connect extraction/model hooks to the module. The cape's local
rotation compensates for inherited body rotation while preserving modern body
translation and armor clearance.

The matching domain is standing/crouching player poses, including ordinary
walking, sprinting, airborne movement and riding. Swimming/crawling, swim
transitions, fall flying, spin attacks and other modern poses retain vanilla
behavior. Independent third-party cape renderers and mannequins are not modified.

Histories are per entity instance, initialized at its current position to avoid
an origin-to-spawn artifact when enabling the module. They are cleared on disable,
world changes/disconnect and entity removal; replacement entities start fresh.
Rendering samples immutable angle values without advancing the tick history.

Checks can run without JAR packaging:

```sh
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check compileGametestJava -x jar -x sourcesJar --offline --no-daemon
GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew runClientGameTest -PcapePhysicsOnly -x jar -x sourcesJar --offline --no-daemon
```

The focused native scenario uses an original checkerboard cape texture from the
test resources; it does not require an account cape or alter account entitlements.
