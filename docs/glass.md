# Glass

Glass is an opt-in Render module. Right-click its card and choose **Customize / Preview**.
The editor previews the material even when gameplay Glass is disabled. Drag the player to
rotate, scroll over it to zoom, and scroll the settings panel to reach more controls.

The default Crystal material uses a cool-white tint, 35% frosting, 80% glow and animated
shimmer. Clear, Frosted and Violet presets change material settings without changing
who or what is affected. Editing a material value selects Custom. Color pickers support
the normal solid, gradient, rainbow, Astolfo, sine and theme modes. Numeric labels accept
exact typed values.

Choose your third-person body, native first-person arms/sleeves, armor, and first- or
third-person items independently. Other players require a separate master toggle,
initially off, with their own body/armor/item selections. Main hand and offhand refer to
logical hands, including left-handed players. Helmet, chestplate, leggings and boots
have independent filters; chest equipment includes native elytra geometry, and native
head equipment uses the helmet filter. Glass does not invent first-person armor geometry.

Shared material values apply to every selected component. Body, Armor and Items can
override them. Body also controls first-person arms. Turning an override off restores
Shared while retaining the override's saved values. All values use normal configuration
and profile serialization. Reset Glass restores all Glass settings and selections.

Frosted blurs the scene through surfaces; Refraction distorts it. Highlights depend on
surface orientation. Edges have independent color, intensity and pixel thickness, while
Glow controls bloom intensity and spread. Original texture blends native skin, armor,
trim, map and item details. Enchantment glint can be kept or suppressed; EnchantEffect
outlines remain independent of that choice. ViewModel transforms and PlayerESP continue
to use their normal native geometry paths.

## Rendering and lifecycle

`GlassExtractEvent` supplies immutable owner/material/selection snapshots through the
module's owned EventBus subscription. Avatar render state carries the owner and policy;
regular item submissions carry the resolved owner, component, view and material. Model
and custom geometry submissions carry the snapshot in their replacement render type.
Selection is captured before deferred execution. Labels, flames, shadows, capes, pets
and unrelated entities keep their native paths.

`GlassCollector` adapts native submissions without changing animation or model geometry.
Models, native arm model parts, special item models, baked item quads, armor layers,
trims and map surfaces retain their UVs and alpha cutouts. Native foil submissions are
forwarded or suppressed separately. Invisible outline-only surfaces stay native, and
vanilla glowing outlines retain their outline texture.

The Blaze3D pipelines use a copied scene color/depth, per-draw normal/coverage target,
view-dependent Fresnel/specular lighting, depth-checked refraction, scene blur, surface
edge detection and two-pass bloom. Glass follows native translucent sorting. Each
surface draw snapshots the scene again so previously rendered glass contributes to
later surfaces; body, armor and item overrides never collapse into a shared union mask.
These are screen-space approximations: off-screen scene information and physically
traced multi-layer transmission are unavailable. Native quad sorting has the usual
limitations for intersecting translucent geometry within one draw.

World and hand paths keep separate targets and native projections. World surfaces test
scene depth; hand surfaces draw with the hand projection and bloom finishes before
screen effects. Iris 1.11.4+mc26.2 has a pinned adapter: its solid/translucent hand paths
capture native draws once, and retain their projection and frame-owned staged buffers
until final world composition. Glass replays world draws after Iris's final world pass
and hand draws after the hand-depth clear. Native shadow submissions remain untouched.
Unrecognized Iris versions retain native surfaces and show a short status message.

The preview uses the same Glass shader with its own target, scene/depth copies, normal
coverage and bloom resources. Its player state is extracted into a new render state;
the live entity, equipment and camera are never modified. Outside a world it uses the
account skin lookup and explicit local sample equipment components, without binding or
mutating Minecraft's uninitialized item registries. The backdrop is rendered inside the
preview target, so frosting/refraction sample that pattern rather than the live world.

Targets are lazy and reused at stable dimensions. Disable releases gameplay resources;
preview closure releases preview resources. Resize, reload, disconnect and shutdown
release the corresponding resources. Pipelines are precompiled and targets reserved
before native surface replacement. Failure restores native rendering and reports a
concise status; re-enabling or reloading retries initialization. Disabled gameplay Glass
has no Glass draws, scene copies or bloom passes; opening its explicit preview can still
render the preview.

## Verification

`glassCheck` covers every combination of all fifteen target switches. Native packaged
checks cover setting/profile round trips, retained overrides, presets/reset, isolated
preview rotation/zoom, native arms and items, mixed materials, armor/trim/elytra,
enchanted/plain gear, bows/tridents/shields/maps/transparent item textures, swing/use,
crouch/water scenes, renderer failure recovery, scaling, resize/reload and enable/disable
cycles. Screenshots and local renderer receipts are retained under `.tooling/glass/` and
`build/run/glass-final-*/`. See [0.27.0 release verification](releases/v0.27.0.md) for the
actual validated matrix, costs and limitations.

This follows Fabric's [26.2 rendering guidance](https://docs.fabricmc.net/develop/rendering/basic-concepts):
GPU work uses Blaze3D instead of raw OpenGL, and deferred drawing consumes captured state.
