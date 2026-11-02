# KillAura

Enable **Combat → KillAura** to attack eligible nearby living entities. The module
starts disabled. This replaces the previous randomized MinCPS/MaxCPS implementation;
there is one registered KillAura module and no Meteor runtime dependency.

## Controls

- **AttackWhenHolding** defaults to Weapons. **Weapons** accepts space- or
  comma-separated families: `sword axe trident mace spear pickaxe shovel hoe`.
  The default is `sword axe trident`; a sword entry covers every material tier.
  All permits any held item, including an empty hand, and keeps the current slot
  unless shield breaking requires an axe. Unknown families match nothing.
- **AutoSwitch** chooses the strongest acceptable hotbar item, ranking damage,
  then attack speed and the current slot. **SwapBack** defaults on and restores
  the previous slot when combat pauses or ends. A manual slot change takes
  precedence for the rest of that selection session. Switches are visible.
- **ShieldMode** None attacks normally; Ignore excludes blocking players;
  Break requires an axe while a selected player blocks. With AutoSwitch enabled,
  Break looks for the strongest hotbar axe. Without an axe it waits.
- **OnlyOnClick** requires the attack key; **OnlyOnLook** uses the actual picked
  entity and still applies every other filter.
- **Rotate** keeps RiseCode's None/Snappy/Smooth visible camera styles.
  **RotationTiming** Always follows the primary target; OnHit aims when a delay
  is ready and releases after the attack window; None releases rotation control.
  All targets in a batch share the primary target's aim. Attacks wait for the
  rotation lease and an ordinary movement tick; another rotation owner can block them.
- **PauseBaritone** temporarily pauses an active optional Baritone task. It
  resumes only the pause it owns; task-changing commands entered through RiseCode while paused
  invalidate that ownership. Commands issued directly to Baritone outside RiseCode
  cannot be tracked by this adapter.

## Targets and range

**Targets** selects Players, Mobs, or Both. Mobs here includes living non-player
entities, including armor stands. **EntityTypes** optionally narrows this group
with IDs such as `minecraft:pig minecraft:zombie` (the `minecraft:` prefix may be
omitted). Empty permits the group; unknown IDs match nothing. Nonliving entities
such as crystals and boats are outside RiseCode's living-entity combat API.

**TargetMode** offers distance, health, angle, farthest distance, or highest health,
with stable distance/entity-ID tie breakers. **MaxTargets** caps each attack window
at one to five entities. Each distinct target gets a cancellable intent and one
ordinary vanilla game-mode attack. The whole batch reserves the shared attack
manager's tick so another managed attacker cannot enter the same tick.

**Range** measures from the player's feet to the nearest point on the target's
hitbox. **OnlyVisible** defaults on and checks a collision ray to the same body
point used for aim. With it off, **WallsRange** permits occluded targets within a
second hitbox-distance limit. Both settings remain bounded by the game's actual
held-item attack reach; this module does not extend reach.

Players in creative or spectator mode are excluded. Friend, invisible and team
filters remain configurable. Passive mob targeting is separate from
**IgnorePassive**, which excludes calm endermen, piglins, zombified piglins and
wolves. **PassiveAge** filters animals/villagers (default Adult); **HostileAge**
filters the hostile families with baby variants (default Both). Frogs and parrots
have no baby variant to filter. **IgnoreNamed** protects custom-named non-players;
**IgnoreTamed** defaults on and protects mobs owned by you.

## Timing, pauses and saved profiles

Vanilla cooldown is the default. **CustomDelay** instead uses **HitDelay**, rounded
down to whole ticks; zero permits one bounded batch per client tick. **SwitchDelay**
waits after both automatic and manual slot changes. **TpsSync** scales these delays
and the cooldown floor using the interval between server time packets. The estimate
is clamped to 1–20 TPS and resets on a world/player change. It is an estimate rather
than a remote performance or anticheat guarantee. **PauseOnLag** waits after three
seconds without a server time packet; before the first packet it does not pause.

Using an item or breaking a block pauses by default. Inventory screens pause by
default; ClickGUI always pauses. Death, spectator mode, disconnection and world
replacement release targets, rotation and owned slot/pathing control. Timing is
retained across short pauses to avoid bypassing a pending delay. Swing can be disabled.
Targets, friends, held slots, pause conditions and rotation ownership are checked
again immediately before each dispatch, including after AttackEvent listeners run.

Existing profile keys for range, targeting, visible rotation, friends, switching,
swing and screen/item pauses remain supported. MinCPS/MaxCPS are removed; old
values are ignored. New controls use their defaults when missing. Global settings,
other modules and appearance are retained. There is no CrystalAura-specific pause
because RiseCode does not have that module.

## Inspection and provenance

Behavior was inspected in Meteor Client's
[KillAura](https://github.com/MeteorDevelopment/meteor-client/blob/e3a2e77967629d216e127b5ceecf71fb15f9d2e5/src/main/java/meteordevelopment/meteorclient/systems/modules/combat/KillAura.java)
and [TargetUtils](https://github.com/MeteorDevelopment/meteor-client/blob/e3a2e77967629d216e127b5ceecf71fb15f9d2e5/src/main/java/meteordevelopment/meteorclient/utils/entity/TargetUtils.java)
at revision `e3a2e77967629d216e127b5ceecf71fb15f9d2e5` (Meteor Development).
Meteor's source is GPL-3.0. This is an original implementation using RiseCode's
existing EventBus, settings, visible RotationManager, cancellable AttackManager,
friend store and optional Baritone API. No Meteor source files or binaries are
bundled. It deliberately retains RiseCode's vanilla reach, visible rotation,
manual-slot ownership and GUI safeguards.
