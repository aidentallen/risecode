# Changelog

Every documented version, newest first.

[Project home](README.md) · [Release index](docs/releases/README.md) · [User guide](docs/usage.md)

Historical entries were recovered from retained source/JAR artifacts and development
documentation. Their notes identify provenance and missing history; original dates
and development commits are not invented.

## [0.27.0](docs/releases/v0.27.0.md) — Glass

- Add opt-in Glass with independent body, native arms, armor and held-item targets, optional other players, hand/slot filters and retained material overrides.
- Add Crystal/Clear/Frosted/Violet presets, animated colors, frosting/refraction, view-dependent highlights, pixel edges, bloom, shimmer and texture/glint controls.
- Add a themed material editor and isolated, rotatable equipment preview using the gameplay shader.
- Add Blaze3D rendering for OpenGL/Vulkan, pinned Iris hand/final-pass support, native fallback, lifecycle cleanup and packaged renderer checks.

## [0.26.0](docs/releases/v0.26.0.md) — Living Panorama

Add opt-in Living Panorama with a resource-pack-aware Minecraft forest and lake, six blended
atmospheres, GPU foliage wind and cursor gusts, water ripples, discoveries, bounded look-around,
and Take a breath recovery through F1, Escape or the return leaf. Share persistent controls
between Appearance and the title gear; retain the prior background and existing menu music.
Own and release scene meshes, targets and environmental audio, debounce/cancel rebuilds,
reuse throttled scene textures, and provide Low, Balanced and Cinematic reflection settings.

## [0.25.0](docs/releases/v0.25.0.md) — Rendering, typography and world resolution

Use acknowledged placement ownership for BBE geometry, start timing after transfer, and
keep double chest halves on one transform. Guard all custom world submissions before shadow
passes and publish ESP geometry/submissions together. Bundle Qraftys with attribution,
retain Google Sans, and add draw-time Font Boldness plus right-click disclosure/details.
Promote Performance to the sidebar with independent world resolution, Bilinear and AMD
FSR1 EASU/RCAS. Compose world overlays before upscaling and native HUD/GUI blur; preserve
requested settings on compatibility fallbacks and release targets on resize/reload/disable.

## [0.24.2](docs/releases/v0.24.2.md) — GUI rendering

Batch Qraftys glyph runs and compatible GUI commands with overlap and native-draw barriers.
Cache prepared Unicode fallback glyphs and repeated layout/elision work, skip settled
collapsed control measurement and transparent text, and limit blur filtering to the complete
sampling footprint of requested regions. Preserve font interpolation, effect settings,
animation refresh and existing Performance modes.

## [0.24.1](docs/releases/v0.24.1.md) — Joining-camera FOV fix

Preserve native FOV when zoom is inactive and skip unready native camera values during zoom transitions. Add joining-camera and hold/release zoom regressions. The separate Bobby world-cache merge crash is documented with a reversible, user-approved workaround.

## [0.24.0](docs/releases/v0.24.0.md) — Performance modes and retained-work reductions

Add global Off, Conservative, Balanced, Strong and Custom performance controls in the settings popup. Cache HUD and nametag work, pack ESP geometry, batch silhouette draws and retire idle targets. Bound map, preview and cosmetic caches while protecting pending draws. Replace sampled allocation percentages with direct render-thread benchmark comparisons.

## [0.23.0](docs/releases/v0.23.0.md) — Settings panels and complex block contours

Fix the footer popup background, embed Client Sounds/Discord Activity/GUI Scale inside ClickGUI, and bounce entire Appearance cards using shared Button Feedback. Render complex block selection unions without internal seams or overlapping fill faces, and advance interrupted shape transitions before retargeting. Audit every module's Appearance controls by its real output while preserving saved overrides.

## [0.22.0](docs/releases/v0.22.0.md) — Hitboxes and shared target highlighting

Add Hitboxes with Only When Looking and Always modes, entity filters, range, and independent style/animation overrides with BlockOverlay inheritance by default. Share one aimed highlight across blocks and entities, retaining displayed geometry during switches, matching entrance pops, smoothly blending appearance and fading block fill. Reset immediately when looking into empty space. Preserve normal targeting, actual entity bounds, placement-following outlines and shader-compatible rendering.

## [0.21.0](docs/releases/v0.21.0.md) — Placement motion and shader lighting

Add native placement animations, configurable transforms and synchronized oriented block highlights. Keep entrance pops for target acquisition by default, with optional Pop On Switch. Rename NoFall to Clutch with a Ghost badge and migrate existing profiles and bindings. Add optional Iris shader night vision to FullBright.

Render moving blocks through native world and shadow passes, retain shader material IDs, and hand geometry back to terrain only after mesh upload. Keep emissive blocks static with shaders, skip fluids and multipart blocks, and infer optional remote placements conservatively.

## [0.20.0](docs/releases/v0.20.0.md) — Container themes and client settings

Restore complete container rendering and add Minecraft/risecode styles, with per-screen overrides. Separate particles from backgrounds and add None. Move Friends/Profiles into a bottom-left settings popup with sounds, client command suggestions, opt-in Discord activity setup, menu key capture and GUI scale.

Add explicit see-through block outlines and 32 px thickness. Restrict module Appearance to settings used by its output, match Appearance sections to module cards, animate menu buttons individually, and extend numeric ranges with precise slider adjustment and matching renderer limits.

Keep BlockOverlay fill and outlines visible with Iris shader packs, including see-through edges, custom thickness, translucent colors and corner gaps. Composite world overlays after shader processing while retaining scene depth testing.

## [0.19.0](docs/releases/v0.19.0.md) — Appearance and block highlights

Add smooth block-highlight switching, center-out pop animations and adjustable corner-only outlines. Add module-local appearance with Global inheritance, persistent collapsible settings sections, compact tab-sized themed buttons and held-press feedback. Reveal the resize handle on proximity and classify every NoFall mode as Blatant.

## [0.18.0](docs/releases/v0.18.0.md) — Cosmetics and legacy cape physics

Add the Cosmetics wardrobe with previews, profile-owned outfits, offline items, downloadable companions and PNG cape imports. Add optional legacy cape physics. Clamp glow scissors to the screen for Minecraft 26.2, remove duplicate profile controls from Appearance and repair header/control alignment while preserving the restored interface structure and Button Style setting.

## [0.17.1](docs/releases/v0.17.1.md) — Previous layout and quieter buttons

Restore the original module tabs, Utilities menu, and scrolling Appearance layout, including HUD/map styling in Appearance. Add a saved Button Style option with Subtle dark buttons by default and Accent as an alternative. Retain per-page gears and all 0.17.0 rendering, input, animation, background, sound, module and NoFall fixes.

## [0.17.0](docs/releases/v0.17.0.md) — Interface polish and controls

Interface polish: six-page navigation, focused Appearance sections, shared themed buttons and hover feedback, glow clipping repair, high-resolution branding, expanded animation controls and five click sounds. Adds independent per-page themes/backgrounds, procedural animation, particles and Dual Filtering scene blur. Repairs focused-search closing and exact Baritone coordinate editing. Adds behavior badges and functional packet/item-clutch NoFall modes. See [release notes](docs/releases/v0.17.0.md).

## [0.16.1](docs/releases/v0.16.1.md) — UI motion repairs

- Restore sidebar hover fades, moving selection highlights and page-switch movement with scrolling and keyboard navigation.
- Keep menu backgrounds stationary, remove redundant vanilla dimming and coordinate foreground scale/fade once.
- Give each button a continuous spring response, including native controls and supported Sodium buttons, while dispatching actions and sounds once.
- Move Appearance imports, previews, font and layout actions beside their settings; retain a compact theme toolbar and responsive galleries.

## [0.16.0](docs/releases/v0.16.0.md) — Shared appearance, cave maps and polish

- Share menu/container backgrounds, styled text, entrance animations and single-dispatch spring/click feedback; add background/sound imports and window/module overrides.
- Expand sidebar utilities inline; add resizable client windows, pause-menu access and compact Resources browsing.
- Restyle chat, tab and nametags; fix legacy formatting and clickable custom-font text while preserving native metadata/content.
- Separate Zoom enablement from Hold/Toggle activation, preserve temporary wheel changes, move FreeLook to Render and extend enchanted outlines to special models/tridents.
- Share detailed surface/cave terrain rendering, automatic altitude layers, fullscreen/panel modes, circle/square minimaps and separate map backgrounds.
- Retain existing surface exploration, bound map work and byte residency, and preserve paged reached/death history independently of recording and overlays.

## [0.15.0](docs/releases/v0.15.0.md) — Navigation, typography and world utilities

- Move Friends and Profiles into the gear Settings hub, preserving saved destinations, data and commands; keep Appearance and Resources in the sidebar.
- Animate every Resources view with shared scrolling controls and displayed-position hit detection.
- Normalize the local Minecraftia font to shared text metrics with scale-aware sampling and Unicode fallback.
- Add disabled-by-default Nametags, Trajectories, AutoTool, FastBreak, BetterChat, BetterTab, Waypoints and a graphical Minimap in the HUD editor.
- Add the world map, rebindable M/B shortcuts, shared waypoint editing and latest death markers per dimension.
- Persist only client-observed surface terrain with texture/biome/water/height colors, bounded recording/work/caches and resource-reload recoloring; keep map data outside profiles.
- Adapt the pinned MIT Simple Map page-layout and density helpers, retaining their license and attribution.

## [0.14.0](docs/releases/v0.14.0.md) — Resources and Ambience

- Replace the old Styles catalog/import browser with the native Vanilla Tweaks Resources picker for Minecraft 26.2.
- Cache all public resources, images and official combined assets; compose new selections offline with explicit priority, warnings and profile persistence.
- Restore Ambience as a Render module, migrating Sky/Water settings and bindings while preserving Iris handoff.
- Retain atomic cache writes, integrity checks, cancellable/resumable downloads and resource reload rollback; archive old personal Styles files.
- Keep downloaded third-party assets out of source/release JARs and include Vanilla Tweaks attribution.

## [0.13.1](docs/releases/v0.13.1.md) — Remove AI Agent

- Remove the Agent page, provider/chat services, AI tools, background workers and Agent automation hooks.
- Remove Agent-only build tasks, fixtures, tests and current feature documentation.
- Preserve direct Baritone controls, shared settings/text editing, existing profiles and Styles; map saved AI/Agent tabs to Search and restore F12 bindings.

## [0.13.0](docs/releases/v0.13.0.md) — Experimental controllers

- Add reviewed, server-confirmed container transfers, unlocked recipe batches, equipment and bounded loadout preparation; stop on stale/rejected snapshots without replay or drops.
- Add a bounded safe-food survival routine with authoritative hunger proof and immediate forced-use cleanup.
- Add version-pinned Iris pack selection/options and Sodium/Sodium Extra fields, verified persistence, native change revisions and targeted Undo.
- Keep idle work bounded and recognized reads local; cap pack metadata to reduce tokens and avoid repeated inventory serialization while waiting.
- Verify disposable survival-world operations, optional integrations, existing GUI/controller regressions and renderer/performance compatibility.

## [0.12.0](docs/releases/v0.12.0.md) — Agent

- Rebrand the native AI page to Agent; route recognized commands locally with zero model tokens and select only relevant remote tool schemas.
- Add bounded opt-in game observations, explicit vanilla options, version-gated Iris status/toggles, profile/binding tools and reviewed preferences/documentation.
- Add approved deterministic Baritone navigation, mining, follow, exploration and gather-and-return, with resource ownership, safety/watchdogs, manual pause and interrupted checkpoints.
- Add on-demand frame metrics and repeated one-candidate visual experiments with conservative retention and conditional restoration, including Stop during persistence.
- Add OpenRouter input/output price ceilings, lower unset budgets and a repeated-failure stop; keep credentials session/environment only.
- Verify offline/scripted/native/live-provider workflows and repeated frame/retention benchmarks. Keep experimental world controllers and arbitrary mod tuning unexposed; document pinned external API limitations.

## [0.11.5](docs/releases/v0.11.5.md) — Explicit OpenRouter routing

- Added native Compatible/OpenRouter gateway selection, bounded ordered provider slugs, supported-parameter filtering and requested data-collection policy.
- OpenRouter mode emits explicit no-fallback preferences for Chat Completions and Responses, restricted to official API bases or loopback gateways; Compatible mode emits no routing fields.
- Preserve the selected policy through probes and tool rounds; routing changes cannot inherit another policy's capability evidence.
- Migrate older connections as Compatible, preserve invalid stored policies, and keep credentials outside profiles/payloads.
- Added policy, migration, endpoint, HTTP, capability-isolation and native editing/mutation/Undo checks. Live upstream routing remains unverified.

## [0.11.4](docs/releases/v0.11.4.md) — Provider capability reports

- Split connection testing into bounded JSON text, harmless echo-tool and optional streaming probes; no game tools are offered or executed.
- Show per-connection observed, unknown, absent and failed formats separately from client declarations, including provider-reported token usage and unknown context/pricing.
- Retain at most eight session reports by endpoint/model/protocol/output parameter; invalidate them on credential changes and suppress late cancelled results.
- Add a native connection report view and visibly disabled Responses token-parameter control with an explanation.
- Add protocol, timeout, cancellation, retention, credential and native report/scroll/control fixtures.

## [0.11.3](docs/releases/v0.11.3.md) — Responses protocol

- Added explicit Chat Completions/Responses selection to native AI settings; old profiles keep Chat Completions.
- Added Responses JSON/SSE text, function calls, call-result continuation, actual usage and stateless encrypted reasoning round trips within an active request.
- Shared bounded HTTP transport, deadlines and cancellation between protocols; failed/truncated/conflicting responses cannot execute calls or trigger fallback.
- Added adversarial protocol, migration, HTTP agent and native selector/FullBright/Undo fixtures. Credentials and opaque reasoning remain outside saved history/audit/Undo.

## [0.11.2](docs/releases/v0.11.2.md) — AI streaming and retention performance

- Reuse unchanged Markdown paragraphs while streaming and wrap with actual glyph advances/kerning without allocating or caching every text prefix.
- Reuse immutable chat snapshots between updates; preserve Unicode boundaries when clipping displayed text.
- Release message layout caches when leaving/closing the AI panel, and reset scrolling when a conversation is cleared/restored.
- Added fixed-scene HTTP/SSE, tool-burst and long-chat benchmarks, cleared-object/worker probes, and native font/wrapping regression checks. Fixtures remain outside release JARs.

## [0.11.1](docs/releases/v0.11.1.md) — Restore AI conversation context

- Opt-in saved conversations now restore bounded provider context after restart, including redacted historical tool outcomes.
- Archives contain plain conversation and receipts, excluding executable calls, raw tool arguments, pending approvals and credentials; live tools still validate each new request.
- Migrate existing saved chat, preserve Undo/audit when retention is disabled, and clear both visible chat and model context together.
- Added restart, retention, migration, credential redaction and permission-isolation checks.

## [0.11.0](docs/releases/v0.11.0.md) — Native AI configuration assistant

- Added native AI conversation/settings, multiline Unicode editing, streaming compatible transport and connection/tool-format probes.
- Added generic real module discovery/inspection, shared setting codecs, stable capability IDs, state revisions, targeted transactions and conflict-aware Undo.
- Added locally enforced narrow configuration scopes, concrete gameplay/inferred-change previews, call deduplication and session/cancellation gates.
- Added session/environment credentials, opt-in chat persistence, bounded redacted audit/Undo retention, and F12 before focused input.
- Added deterministic HTTP/provider checks and Minecraft input/mutation/Undo fixtures; preserved the Styles baseline.

## [0.10.0](docs/releases/v0.10.0.md) — Styles

- Added an original Styles sidebar browser with local/cloud previews, category mixing, and one Apply action.
- Added resource-pack imports with native model dependencies, per-entity EMF/ETF styles, and compatibility notices.
- Integrated the former Ambience sky/water controls and migrated existing settings and bindings into client profiles.
- Added imported sky rendering, cached SHA-256 downloads, shared-resource conflict detection, and resource-reload rollback.
- EMF and ETF remain optional companion mods rather than bundled dependencies.

## [0.9.2](docs/releases/v0.9.2.md) — Chunk fade and terrain animations

- Added independent NoRender Chunk Fade removal, without coupling it to atmospheric fog controls.
- Added disabled-by-default ChunkAnimations with Up/Down, distance, duration, and Ease-Out-Quart / Ease-In-Out-Cubic / Elastic-Out controls.
- Shared one first-drawable clock per chunk column; ordinary rebuilds do not replay movement, active flights capture their settings, and block entities follow terrain.
- Added native/Sodium GPU transforms, culling and face-mask corrections, Ambience water composition, Iris terrain/shadow support, and Sodium Extra shader-interface compatibility.
- Verified 663 core assertions, vanilla/Sodium OpenGL and Vulkan clients, Iris OpenGL with Complementary Unbound, and static screenshot comparisons.
- Repository publication adds an original-code MIT license, preserved third-party notices, public build dependency retrieval, and reconstructed historical release notes.

## [0.9.1](docs/releases/v0.9.1.md) — NoRender environment fog controls

- Added the disabled-by-default NoRender module under Render.
- Added All Fog and independent Overworld, Nether, End, other-dimension, water, lava, powder-snow, blindness, and darkness controls.
- Persisted controls through module configs/profiles and restored native fog on disable; Iris retains its shader-pack fog path.
- Preserves the original installed release JAR rather than the later build-folder rebuild of the same version.

## [0.9.0](docs/releases/v0.9.0.md) — Ambience, Appearance, and rendering upgrade

- Added procedural Ambience skies and animated water with native/Sodium adapters and Iris shader-pack handoff.
- Added optional official Baritone controls, command forwarding/completion, missing-dependency status, and safe task lifecycle.
- Added focused typed settings, branding/PNG logos, Appearance presets with Undo, Watermark/ArrayList HUD modules, and independent animation channels.
- Expanded native-menu theming, loading controls, title-screen interface access, and uncapped themed menus.
- Added shader-sampled gradients, rendering/layout caches, and blur/resource reuse. Historical performance evidence is documented in the upgrade guide.

## [0.8.0](docs/releases/v0.8.0.md) — Render, camera, HUD, and menu polish

- Added StorageESP, PlayerESP, BlockESP, Crosshair scaling, independent hand ViewModel transforms, and Hold-C Zoom.
- Added selected-tab colors, HUD borders, collapsible color pickers, and configurable dropped-item rotation/spin.
- Added vector sidebar icons, text/HUD caching, and independent Start/Multiplayer/Options theming.
- Recovered the original installed JAR from its backup and the preserved source-0.8.0 snapshot. Later overwritten build-folder copies are deliberately excluded.

## [0.7.2](docs/releases/v0.7.2.md) — Module keybind editor

- Added per-module inline key capture, Hold/Toggle activation, and Reset.
- Added exclusive capture, reserved GUI-key validation, repeat isolation, and cleanup on focus loss, menus, disconnects, and rebinding.
- Persisted bindings/modes in config and profiles; migrated FreeLook to the shared Hold binding.

## [0.7.1](docs/releases/v0.7.1.md) — Shared color controls and item outlines

- Added the shared HSV/alpha/hex color picker and independent gradients for module, interface, HUD, border, and shadow colors.
- Added Solid, Gradient, Rainbow, Astolfo, Sine, and Theme paint modes with direction, speed, and spacing controls.
- Corrected enchanted held-item outlines to an exterior-only silhouette mask with independent optional glint overlay.

## [0.7.0](docs/releases/v0.7.0.md) — Expanded HUD, camera, and visual modules

- Added concurrent frame animations, seven editable HUD elements, per-profile HUD layouts, and animated gradients.
- Added FreeCam, FreeLook, WeatherChanger, ItemPhysics, EnchantEffect, and BlockOverlay.
- Added the full lightmap FullBright mode with native restoration and retained a configurable gamma alternative.

## [0.6.1](docs/releases/v0.6.1.md) — Typography rendering fix

- Replaced normal-text SDF reconstruction with filled STB glyph coverage rasterized at 8x logical resolution.
- Preserved advances, kerning, baseline, and fractional positioning; shadows/glow keep their separate cached distance-field atlas.
- Corrected missing Minecraftia fallback so Inter uses smooth sampling rather than a requested pixel-font mode.

## [0.6.0](docs/releases/v0.6.0.md) — Live theme and typography editor

- Added a live Themes editor for fonts, HSV accents, glow, geometry, animation, panel/world blur, and HUD styling.
- Added independent theme/profile persistence and renderer-owned TrueType/bitmap font support.
- Added shader-backed shape, font, and blur rendering while retaining the sidebar layout.

## [0.5.0](docs/releases/v0.5.0.md) — Sidebar interface redesign

- Rebuilt the interface around the reference 4:3 window, quarter-width sidebar, teal navigation, and compact inline module settings.
- Added viewport-based layout density, draggable/clipped content, consistent sidebar pages, and compact right-aligned HUD rows.
- Retained lowercase risecode branding and existing engine/module controls.

## [0.4.0](docs/releases/v0.4.0.md) — Advanced combat engine

- Added shared target selection/filtering, friend policies, priority rotation ownership, attack scheduling, and weapon selection.
- Expanded KillAura with configurable timing, rotation, target sorting, visibility, switching, and pause controls.
- Preserved the existing settings, UI/HUD, profiles, packet hooks, and module subscription lifecycle.

## [0.3.0](docs/releases/v0.3.0.md) — Core engine and gameplay utilities

- Added Sprint, KillAura, Tracers, and local-state NoFall alongside FullBright.
- Added local command routing, friends, named profiles, validated configuration application, and GUI/profile refresh.
- Adapted raw keyboard, packet, and attack hooks into the EventBus; NoFall does not prevent server-side fall damage.

## [0.2.0](docs/releases/v0.2.0.md) — Animation clock correction

- Removed the 0.1-second elapsed-time clamp from ClickGUI and HUD animation updates.
- Animations advance using the actual frame interval, preserving elapsed time during long frames.
- Recovered by comparing the retained 0.1.0 and 0.2.0 source JARs; no separate contemporaneous changelog survives.

## [0.1.0](docs/releases/v0.1.0.md) — Earliest retained Fabric client

- Java 25 / Minecraft 26.2 client foundation with settings, synchronous EventBus, module lifecycle, and retargetable animations.
- Retained build includes the animated category/card ClickGUI, search, watermark/ArrayList HUD, and FullBright module.
- This is the earliest surviving binary/source pair; the original development history predates Git and cannot be reconstructed fully.
