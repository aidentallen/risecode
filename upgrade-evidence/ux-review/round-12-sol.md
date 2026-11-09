# Independent UX/UI review 12 — RiseRecode

Checkpoint `a0e123e7577d23dce9468c1cd8abf42b83bf7834`, branch `feat/ux-motion-review`, 2026-11-09. Checkout `/home/toast/Work/riserecode`.

**Equal-weight average: 8.325/10 (8.33 rounded).** Common inspection, correction and cancellation workflows now have clear support. Review11's four findings are resolved within the scopes described below. Remaining material work concerns compact contextual help and advanced-field space, consistent draft wording, and first-use discovery. This is an independent agent usability pass plus source/evidence audit, not an ordinary-user study. No all-9/10 conclusion is justified.

## Method and integrity

The live pass came first. Before it, I read only `scripts/ux-review-session.md`; I did not consult production code, implementation log, old reports or memory. I used the specified already-running owner-only disposable client through `python3 scripts/ux-review-session.py --dir /tmp/riserecode-ux-round14/live-review12`, individually viewing every returned PNG with `view_image` and choosing coordinates from screenshots. No widget tree or source-derived solution path was used. State0 through finish56 span 626.3 seconds, about10m26s. The last bridge response is **phase finished**. Commands and frames remain under that directory. AGENTS.md and AGENTS.local.md were read only afterward. This explicit assignment supersedes their installation/publication instructions.

The checkout was clean on initial post-live audit and final check, at the requested branch and SHA. No source edits, builds, extra clients, downloads, installation, publication or delegation occurred. Only reports/contact sheets and authorized disposable session inputs were written. I did not access the installed Modrinth instance.

I recomputed all **826 sourceSha256 entries against both disk and Git HEAD**, and all **127 evidenceSha256 entries against final evidence**: zero mismatches. The manifest's validatedSourceCommit is the requested full SHA; baseCommit is2806bd8. This supports source/runtime correspondence; the build preceded the commit, so it does **not** establish release-JAR provenance. No installation/publication is represented as done.

The final supplied native-check-build.log records BUILD SUCCESSFUL and fourteen UX totals summing to **728 assertions**, not728 user journeys. I did not rerun them. I read all14 Ux*IntegrationTest sources and the Java/Python interactive bridge. I inspected all122 supplied PNGs broadly in eight sequential contact sheets `/tmp/ux12-sheet-0.jpg` through `-7.jpg`; these preserve ascending filename order,16 images per sheet except the last. Broad sheets support hierarchy/coverage judgments, not exact reading of every small label. Live frames were read individually. Contact-sheet generation first encountered missing Pillow and an unavailable ImageMagick font; an unlabelled ImageMagick montage succeeded without installing anything.

Evidence labels below: **live observed**, **pictured**, **static**, **supplied assertion**, **unverified**. Missing evidence alone is not a reproduced defect. Actual960×720/GUI3 is320×240 logical pixels. The320×180 `Screen.init` images remain logical fixtures, not small native-window runs. Preset commands are fixture setup, not successful user navigation. Inputs are synthetic native callbacks; explicit modifier flags and scheduling/default3tick waits are material boundaries. Screenshots do not establish continuous FPS/smoothness or physical latency.

## Scores using the original rubric

Anchors:0 absent;2 usually blocks tasks;4 partial with substantial discovery/workarounds;6 usable for familiar users with material gaps;8 clear and consistent common-task/edge-case support;10 excellent support demonstrated across ordinary users, sizes, inputs and failure states. Scores do not reward iteration, hashes or assertion counts.

| Original category | /10 | Reason and limit |
|---|---:|---|
| Navigation and orientation |8.50| Named normal sidebar, compact Pages fallback, caller returns and scoped search are coherent. Cosmetics required a Resources/Tools detour and Utilities discovery in my first-use pass. |
| Layout and responsiveness |8.50| Native reflow kept waypoint drafts and actions; compact cosmetic inspection has useful space. Advanced ID fields remain too narrow to review at a glance; large tooltips obscure settings. |
| Hierarchy and readability |8.50| Bright explanatory text and stable backing improve the sampled sky/grass and flat probes. Large type remains legible, with clear headings/state. Truncation and small normal-size metadata persist; no reader study or universal contrast claim. |
| Motion and transitions |8.00| Sampled transitions retained usable state; supplied reversal/reduced-motion tests and current timeline support bounded temporal behavior. No sustained animation/frame-pacing or physical-input measurement. |
| Feedback and recovery |8.75| Typed correction now clears the owning error; cancel/unchanged/draft states worked. Supplied local failure/retry/late-result checks are strong within their scope. A preview subtitle still contradicts In draft after selection. |
| Keyboard and accessibility |8.00| Visible focus, preview controls, rotation, Escape, text replacement and empty-search recovery worked. Live task was mixed-input, not a full keyboard-only journey. Audible narration remains unverified and original environment fails narrator initialization. |
| Discoverability and ordinary-user usability |8.00| Preview labels and named world/color choices make intent clearer. Utilities hides cosmetics from initial view; Client/Ghost and cache terminology still need learning. Agent success does not establish novice success. |
| Consistency |8.50| Shared choices, controls, retained full details and footer conventions recur. Immediate settings versus draft Apply workflows are generally signposted. Preview-only subtitle should follow actual draft state. |
| Settings workflow |8.50| Card-body inspection kept module Off, automatic Appearance saving is explicit, and named choices precede technical values. Compact advanced editing and contextual help need refinement. |
| Perceived performance and implementation economy |8.00| My sampled actions completed without bridge errors; cached text/tooltip layouts and shared geometry avoid unnecessary duplication. No current-world CPU/GPU/FPS improvement or sustained-load responsiveness was independently established. |

Sum83.25, divided equally by10 =8.325.

## Concise first-use task log

All sequence IDs are command/frame IDs in `live-review12`; PNG names are six-digit versions of these IDs. No bridge command returned an error.

| Task | Actual actions and outcome |
|---|---|
| Discovery |0 initial Search;1 Resources;2 Tools;3 Catalog Tools. I expected resources might include cosmetics; Tools exposed cache/provider maintenance.4/5 sidebar wheel attempts stopped at collapsed Utilities;6 expanded it;7 scrolled;8 Cosmetics. This is observed agent hesitation, not a human failure-rate estimate. |
| Inspect without selecting |8 fresh outfit: installed rows, disabled Apply, No unapplied changes.9 clicked Preview Solid; item-specific full preview showed Installed/Preview only.10 Tab focused Back;11 Shift+Tab focused View controls;12 Enter exposed controls;13 Right rotated the model;14 Escape returned. Outfit preview still had no cape, Apply stayed disabled and No unapplied changes remained. Keyboard was used for a meaningful part of preview/control/return, but entry/discovery used the pointer. No pose selection was attempted live; view rotation met the view-only part of the task. |
| Deliberate draft/discard |15 reopened Solid;16 Select in draft: header changed to In draft and action became Selected in draft.17 Escape: cape visible in outfit preview, Unapplied changes and enabled Apply.18 compact preset: In draft/footer/Apply/Cancel remained readable; inline avatar removed to make room.19 Cancel: Installed row, No unapplied changes, disabled Apply.20 normal preset confirmed empty outfit. Never pressed Apply. |
| Waypoint error recovery |21 Waypoints;22 Manage;23 Add;24 X;25 appended oops to0;26 Save rejected0oops with specific whole-number range.27 X;28 Ctrl+A;29 typed123: old warning disappeared immediately;30 Tab: still gone;31 actual resize960×720/GUI3:123 and Y focus survived, no stale warning. This resize retained font1; it was not the font2 compact preset. |
| Named/custom choices and cancel |32 Dimension chooser;33 Nether;34 Color chooser;35 Blue. Named current choices and checkmarks were readable.36 custom Hex;37 Ctrl+A;38 typedFF7700: swatch changed orange and caption changed to Colors.39 deliberately canceled: empty manager. No destination was saved or required removal. Custom dimension entry was discoverable as an adjacent advanced ID, but was not typed live. |
| Recognition/settings |40 Escape to client;41 Map settings recognized by name and displayed recording/layer/map links;42 normal preset;43 Appearance recognized and grouped sections shown with automatic-save footer;44 wheel returned sidebar to top;45 Search;46 Ambience card-body click expanded settings with Off unchanged. Client/Render location labels and explanatory text remained visible over sky/grass. I did not independently test the meaning of Ghost with an unfamiliar user. |
| Compact input/limited motion |47 compact preset with Ambience expanded. Description truncated; hover tooltip covered title/nearby settings.48 Tab visibly focused Pages, but mouse tooltip remained until49 pointer move away.50 wheel scrolled module settings.51 ten-step header drag12px right/down: no visible window movement; incomplete drag attempt, not evidence of a broken drag control because the chosen header target may not be draggable. |
| Keyboard recovery |52 Ctrl+F focused search;53 one long type command entered an unmatched query; empty state said No matching modules/Try fewer words or clear.54 Ctrl+A;55 Backspace recovered results and preserved Ambience Off/expanded. This tests a long string, not sustained physical keystroke cadence.56 finish. |

The optional missing-PNG/correct-fixture import was **not attempted**. No import recovery success from older reports is presented as my live result. HUD precision/Help and a successful drag across navigation/open-close were also not attempted. I prioritized completing inspection/discard and error-correction/cancel.

## Independent reassessment of review11

Production paths below are relative to `src/main/java/dev/riserecode/`.

| Review11 item | Current judgment and evidence |
|---|---|
| F1 stale typed waypoint validation | **Closed within demonstrated scope.** Live26–31 showed invalid submission, field-owned typed replacement, blur and actual resize with accurate cleared feedback. `map/WaypointEditScreen.java:49,55–68` uses responders plus the same validator on Save; validationOwner prevents unrelated-field edits clearing an X error. Followup suite additionally re-invalidates X, changes other fields, rejects invalid Save and saves corrected values. Those latter cases are supplied assertions, not my live tasks. |
| F2 inspection versus draft selection | **Closed within demonstrated scope.** Live9–14 inspect/rotate/return kept unchanged outfit;15–19 explicitly selected and discarded. `ui/component/CosmeticsEditor.java:180–200` opens an item-specific child; `CosmeticDetailsScreen.java:19–30,78` owns inspected identity and separates Select in draft; `CosmeticsEditor.java:208–220` uses a temporary presentation loadout. Installed/In draft/dirty/footer states were substantially clearer. Small subtitle inconsistency below does not reopen the old missing-inspected-item route. |
| F3 secondary readability | **Addressed for defaults and sampled conditions; universal closure is not claimed.** `ui/theme/Theme.java:19–20` now defines brighter secondary/muted defaults; `util/RenderUtil.java:19–55` adds stable backing with Readable Surfaces, default enabled at `ThemeManager.java:106`. Live0/43/46/49 improved explanatory readability over sky/grass. Original RGB probes and source support a bounded measured result below. Custom palettes remain user-controlled/stored unchanged; disabling readability backing changes the result. |
| F4 compact preview allocation | **Closed for installed-item inspection.** Supplied0109/0110 show a useful large avatar and pose difference at actual320×240/font2. `CosmeticDetailsScreen.java:27,83` starts installed inspection in previewView and allocates height-110 =130 logical pixels instead of acquisition's50. Keyboard pose/large-preview return and unchanged outfit are supplied assertions. My large-preview inspection was normal size; compact live18/19 assessed root draft/discard, not the child preview. Acquisition mode still has a small model but now has an explicit Large preview route; this does not negate the specific inspection improvement. |

Earlier demonstrated closures remain scoped closures: safe module disclosure separate from On/Off; scoped/combined search and disclosure restoration; numeric invalid-draft preservation; dirty-leave decisions; keyboard reveal of deep controls; expanded color geometry/endpoint identity; friend removal confirmation; profile replacement/previous-version recovery; full error text and local import failure recovery; HUD Help/precision callbacks; named compact navigation; page-reset confirmation; native waypoint resize context; retained native menu ownership. Current tests/source/captures support these, but I did not independently repeat every historical live task. No regression of those behaviors was established. The implementation log is a list of claims to cross-check, not proof by itself. Provisional1 is superseded; it is not the final checkpoint.

## Prioritized remaining findings

### F1 — P3: compact hover explanations obscure the settings they explain

**Classification: live observed + static; not an out-of-bounds defect.** Frames47/48 show the Ambience description tooltip covering its title and Sky/Water labels while Pages has keyboard focus.50 similarly covers much of the Intensity field/track. Moving the pointer away49 removed the tooltip. `ui/component/ModuleButton.java:187–190` truncates the inline description and requests a two-line explanatory tooltip. `ui/menu/TooltipPresentation.java:31–36` wraps up to80% of viewport width and42% height; bounds safety does not guarantee useful placement relative to the active task.

**Ordinary-user impact:** at large compact text, recovering truncated explanation hides the nearby controls needed to act on it; a resting pointer can compete with keyboard focus. This is extra effort, not blocked editing or lost data.

**Correction:** use a shorter compact hover summary plus explicit full-description action, or position task help away from the focused/active setting where space permits. Suppress stale pointer help after keyboard intent until deliberate pointer movement.

**Acceptance:** at960×720/GUI3/font2, hover a module description, Tab to a setting and adjust it. Its label/value/state and focus should stay visible, and full description remain reachable. Test pointer-to-keyboard changes as well as geometric bounds; retain F1 full details for long text.

### F2 — P3: selected preview still instructs “Preview only”

**Classification: live observed + static.** Frame16 correctly changes the header to In draft and button to Selected in draft, but the secondary line still says “Preview only · Select in draft, then Apply in outfit.” `ui/component/CosmeticDetailsScreen.java:79` chooses this text solely from previewView, independent of isDraftSelected; narration at32 also gives selection instructions regardless of selection.

**Ordinary-user impact:** immediately after intentional selection, two adjacent messages describe different commitment stages. The root Unapplied changes/Apply distinction worked, so this is a local contradiction rather than silent applying.

**Correction:** derive the subtitle from item/draft/applied state: after selection say “In draft · Return to outfit to Apply or Cancel.” Keep view-only reassurance before selection.

**Acceptance:** inspect fresh item, select, return, cancel, reopen; header, subtitle, action and narration agree at every step, at normal/compact sizes, without implying library removal on draft cancellation.

### F3 — P3: compact advanced waypoint values cannot be inspected at a glance

**Classification: live pictured + static; no value-loss claim.** Frames31/33/35 show the ID field exposing only a suffix of minecraft:overworld/the_nether at actual960×720/GUI3. `map/WaypointEditScreen.java:40–48` splits compact form into two columns and reserves82 logical pixels of the Dimension cell for the named chooser. Named choices are now readable and precede advanced entry, which materially mitigates this finding. Custom color entry worked.

**Ordinary-user impact:** users checking a custom dimension cannot compare the whole value with an instruction without caret scrolling. This affects advanced edits more than common named-world selection.

**Correction:** collapse advanced ID/hex inputs behind an explicit Custom action, or give an expanded editor/full-width row when focused. Keep named world/color choices primary and retain drafts/focus on return.

**Acceptance:** type and inspect a long namespaced custom dimension at320×240/font1 and2, resize and return from choices; full value is available through a clear interaction, and no unrelated error/draft/caret is lost. Preserve current successful named choices and custom hex behavior.

A separate **discovery opportunity**, not a proven general defect: cosmetics took1–8 via Resources/Tools and collapsed Utilities. Consider exposing a normal-width All pages/search route or linking Resources to Cosmetics. `Sidebar.java:64–71` currently shows named Pages only when compact; Utilities membership/order lives in ClickGui's sidebar setup. Validate with unfamiliar users asked to find an installed cosmetic without naming the destination. My detour is one agent observation, not evidence that most users fail.

## Preserved evidence, coverage and limits

| Area | Supported observations and limits |
|---|---|
| ClickGUI/modules/sidebar/search | Live safe Ambience inspection, descriptions/location/state, sidebar wheel/disclosure, compact focus, long-query no-result recovery. Journey/Layout/Auxiliary supply combined/scoped matching and deep reveal. Module-specific vocabulary/novice comprehension is not generally validated. |
| Appearance/page editors/shared controls | Live Appearance recognition/group hierarchy/auto-save footer. Current PageAppearanceScreen reset request42 and retained details/PNG path recovery; supplied native page gear, reset scope/cancel and repeated resize contexts. Shared numeric/color controls reserve font-aware geometry; pictures cover expanded channels. No independent live reset or page import. |
| Resources/cosmetics | Live Resources catalog/Tools and cosmetic inspection/draft/discard. ResourcesEditor:47–53,189 separates cache recovery from Apply/Cancel. Technical cache/provider terminology remains. AsyncRecovery uses deterministic worker latches/reflection to check missing-file retry, duplicate suppression and late results after newer intent/discard/leave. External asset availability/network and permission-denied native reproduction remain unverified. |
| HUD/Glass | Pictured HUD focused rows, collapsed/deep inspector, Help; Glass full preview/reset controls and expanded channels. Layout/Nested/NativeWorkflow supply precision/modifier and clipping-route assertions. No independent live HUD/Glass pass, arbitrary HUD arrangements or all renderer combinations. |
| Block picker | BlockPickerScreen:29–50 offers translated names/IDs, Selected, unavailable-ID retention, pagination and immediate toggles. Supplied0014/0022 and auxiliary tests support these scopes. Init35–44 preserves query but rebuilds search focus rather than capturing exact caret/action identity; resize continuity remains static followup/unverified live, not a newly reproduced failure. |
| Map/waypoints | Live editor invalid correction/resize/named/custom color/cancel. NativeWorkflow supplies owner-world pan/zoom/Add/edit/delete/cancel/return and toolbar/footer separation at compact/normal sizes. Simple grass terrain does not establish large-map performance or arbitrary world/dimension behavior. |
| Profiles/friends | ClickGui:388–423 separates saved previous-version restore from active load, duplicate save decisions, inspect versus confirmed friend removal and neighbor focus. Completion/Refinement/Recovery assertions cover scoped recovery/UUID preservation. Not independently exercised live. |
| Native/container adaptation | NativeScreenRetention:11–15 validates exact menu owner and closes an abandoned owned menu; RetainedScreen propagates child suspension/return. NativeWorkflow/Polish/Tooltip supply local ChestMenu carried-stack/server-close/abandonment assertions, including exact replacement screen and diamond conservation for reset abandonment. Programmatic menus and explicit callback flags are boundaries; remote races, modded menus and physical chest interaction are not certified. |

All fourteen suites were read: Journey19, Layout66, Auxiliary58, Viewport12, Motion7, Nested52, Recovery18, Completion38, Refinement113, NativeWorkflow171, Polish97, Tooltip38, AsyncRecovery21, ReviewFollowup18. Setup uses direct page selection, setters, component actions and reflection; assertions inspect internal state/geometry alongside synthetic input. Tests searching focus labels are instructed traversal, not fresh discovery. Followup uses native item-row/pose/return/select input but directly establishes baseline, finds item and sets field focus/selection. Its flat ReadabilityProbe does not exercise every real production label. Polish additionally covers named choices before advanced fields, custom values and draft/focus retention. These distinctions remain material despite the passing total.

Contrast: I read MeasureUxContrast.java/method and independently recomputed all20 regions from original RGB PNGs; values agree to declared precision. Secondary full-ink/local median ratios are **5.738–11.753**, muted **4.602–9.379**, across white/sky-blue/grass-green/dark backdrops at decorative Panel Opacity0 and custom white panel, font1/2, with Readable Surfaces enabled. This compares brightest full-ink pixels with local median background in declared regions. It is not per-pixel minimum contrast, universal WCAG certification, calibrated-display measurement or a reader study. Custom palette choices remain unchanged; measurements are for supplied defaults/probes.

The final interaction-and-preview.json is **fresh native harness observation for this run**. UxRefinementIntegrationTest:54 writes the reused round8/timing destination each full run; the frozen final copy is distinct evidence, not unchanged historical carry-forward. It samples animation age/phase and scheduled opening/input behavior, not physical-input latency, continuous final-pixel motion, GPU timing or current-world FPS improvement. The separately fingerprinted round8/performance archive remains an older snapshot; no current-binary performance improvement is inferred from it.

The original speech environment is used in the main launch; final log387–402 records narrator initialization failure for missing Flite. Audible narration is **unverified**. Optional signed Flite extraction and file-synthesis preparation are not an installation or in-game speech result. Keyboard focus/narration strings alone do not close accessibility validation.

Next material work: correct the contradictory selected-preview wording, reduce compact tooltip interference, and improve custom-field inspection while preserving the demonstrated draft/error ownership. Then run unfamiliar-user discovery and a working-narrator keyboard task pass. Any performance claim needs a fresh current-world measurement with CPU, allocation, GPU/frame timing and FPS separated. No source change or release action is part of this review.
