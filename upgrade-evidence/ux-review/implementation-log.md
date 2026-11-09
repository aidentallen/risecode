# UX implementation checkpoints

Branch: feat/ux-motion-review. Original source: 6d1afc0ad5e021f7506c641c88af965557f4fd72.
Independent Sol reports use a fixed rubric. Reports describe source at inspection time; they are not universal visual or performance certification.

## Loop 1

- Named Settings footer with gear, keyboard trigger and narrated menu selection/value/instructions.
- Sidebar pages and Utilities disclosure join keyboard traversal; page appearance gear gets an accessible name.
- Visible module disclosure and textual On/Off state; inspection does not enable the module. Separate keyboard targets for toggle and disclosure. Right-click compatibility remains.
- ActionButton keyboard activation now shares press motion and respects its configured sound behavior.
- Search supports module setting names, result counts, Clear and recovery guidance. Persistent matches no longer replay entrance animations while typing.
- Appearance gains scoped search with matching sections revealed and previous expansion state restored when clearing. Match names, display names, groups and descriptions; explicit no-results guidance.
- Resource and cosmetic drafts require an explicit discard choice before page navigation, closing, or opening page appearance. Escape/Keep editing retain the draft.
- Resource toolbar wraps at narrow widths; render, pointer and dropdown transforms share adjusted geometry.
- Searchable native choice list shows alternatives without changing the value on open/cancel. Page appearance preserves editor position and suspended-container ownership across nested choices.
- Interface scale follows the requested size subject to viewport fit, removing the additional height/576 shrink.
- HUD editor returns to its caller; waypoint creation is uncommitted until Save; deletion requires confirmation.
- Map/waypoint screens own keyboard navigation and map pan/zoom keys.
- Feedback width follows the content viewport.

Baseline scores: navigation 6; layout 5; readability 6; motion 7; feedback 6; keyboard/accessibility 5; discoverability 4; consistency 6; settings workflow 5; performance architecture 7.
Review 2: navigation 7; layout 6; readability 6; motion 7; feedback 7; keyboard/accessibility 7; discoverability 6; consistency 6; settings workflow 6; performance architecture 7.

## Loop 2 implementation following review 2

- Fixed both draft warning bypass via page appearance and Escape return from the confirmation screen.
- Fixed On/Off pointer activation sharing the disclosure region.
- Fixed nested option picker ownership over container page appearance; returning preserves editor instance/scroll.
- Slider labels keep a readable size and expose full labels/descriptions in tooltips; choice labels and values use two lines rather than shrinking the entire line.
- Module descriptions use full secondary opacity and offer full-text hover help.
- Validation borders/messages persist when focus moves; full validation text is available on hover.
- Waypoint errors identify the invalid field and expected format/range, focus that field and request narration.
- Resource footer explains cache gating and unapplied state, retaining provider attribution on a separate line.
- Main draft pages show unapplied status; Appearance identifies automatic saving.
- Added visible HUD Done control.
- Module scrollbars are more visible and draggable; Home/End reach list boundaries.

## Verification

Dependencies verified with python3 scripts/fetch-dependencies.py. Full Gradle check/build and native UX journey run in an isolated development client, not the user's Modrinth profile. Logs live outside Git under /tmp/riserecode-ux-*.log. Native captures are generated under build/run/clientGameTest/screenshots and remain outside Git.

First native run: 9 checks passed. Expanded run: 14 checks passed, including dirty draft cancellation and separate state activation. Test development exposed and corrected a headless enum-cycle expectation and Escape-confirmation behavior; earlier failing runs are not passes.

Final checkpoint run: **19 native checks passed**, full Gradle check/build passed (`/tmp/riserecode-ux-checkpoint-final.log`, BUILD SUCCESSFUL). It adds actual option-control opening/cancel, keyboard-only Done on both map screens, Appearance no-results and German Font Scale 2 captures. These additions passed on this checkpoint source.

No measured FPS, frame latency, allocation improvement, complete contrast compliance, audible narrator success, universal screen-size coverage or full renderer matrix is claimed. Native logs report unavailable flite narration and an X11 standard-cursor warning. Automated logical assertions and selected screenshots are bounded evidence; complete logical keyboard traversal through offscreen content still needs work.

This is an in-progress feature checkpoint. Installation, release publication and final review remain pending completion of the UX work.

## Loop 3 implementation following review 3

Review 3 at b97307a rated navigation 7, layout 6, readability 6, motion 7, feedback 7, keyboard/accessibility 6, discoverability 7, consistency 7, settings workflow 7, and performance/economy 7 (average 6.7). These precede the changes below; a new independent rating is pending.

- Introduced an explicit transient-screen policy: choice/block pickers and confirmation screens do not expose a nested appearance gear. This removes the reported secondary-gear container disposal route.
- Added shared font-aware setting metrics, used by module measurement/drawing/clipping/hit testing and Appearance. Enums reserve both lines; sliders/string/coordinate controls reserve editing and validation space.
- Made sidebar width, row spacing, branding/version separation and header height respond to Font Scale. Selection/background geometry uses the same variable sidebar dimensions. Captures exposed and prompted fixes for branding overlap and selection-pill displacement.
- Choice pickers paginate according to available height and width. Waypoint forms reflow into two columns at short heights, keep Save/Cancel visible, and wrap field errors.
- Keyboard focus entries in scroll regions retain logical controls beyond the clip, with an owner callback that reveals the focused control. Applied to modules, Appearance/client forms, sidebar Utilities, Friends/Profiles and waypoint lists. Offscreen module/Appearance extraction remains culled during ordinary pointer use and collects logical controls during keyboard traversal.
- Replaced the block picker ad hoc query/viewport-only rows with a native searchable, paginated multi-select list, native text editing/paste, Previous/Next/Done and Page Up/Down.
- Preserved invalid slider/string/coordinate drafts across focus changes; explicit Cancel restores the saved value and clears matching validation. Sliders continue to display invalid draft/error after blur.
- Added HUD Ctrl+arrow nudging with Shift for coarse steps. Existing shared HUD Tab registration supplies selection; the native journey verifies Tab/Enter selection and subsequent nudging. This corrects the review's overbroad statement that HUD selection was pointer-only; incomplete keyboard editing/nudging was the remaining gap.
- Module card bodies now inspect settings. The explicit On/Off target changes gameplay state. Keyboard inspection and state toggling have separate targets.
- Client Sounds/Scale/Discord/Performance pages now have scoped search and scoped Ctrl+F, rather than redirecting to module search.
- Module search includes setting display names/descriptions, discloses settings matches without enabling the module, highlights the matched control and reveals it after expansion settles.
- Keyboard descriptions for sliders/choices include the same setting explanation as pointer tooltips; slider narration includes its range.
- Removed module entrance scale/fade that compounded with the host entrance. Window/page motion, press springs, hover, expansion and scrolling remain. Enum value transitions retain a small bounded displacement/fade.

Final verification: dependency hashes verified; full Gradle check/build passed; native journey 19 checks, layout/keyboard 65 checks, and temporal motion 7 checks passed (**91 native assertions total**). Final log: `/tmp/ux-loop3-complete.log`, BUILD SUCCESSFUL. The layout suite traverses to a deep module setting with real Tab input and no manual scroll, checks all visible font2 module row bounds, and exercises invalid drafts, client search, small form fixtures, block paging and HUD keyboard editing. Temporal checks cover intermediate opening geometry, interrupted close/open continuity, settling and Reduced Motion. This does not establish smooth frame pacing, FPS, allocation improvement, audible narration or an optional-renderer matrix.

Small auxiliary form checks use Screen.init(320,180) fixtures inside the native client; they do not resize the native framebuffer. Large-font module captures combine actual configured logical Window Width 360, Window Height 240, padding 20, German and Font Scale 2. The development client remains isolated from Modrinth. No installation/publication yet: this remains an in-progress feature branch.

## Loop 4 implementation following review 4

Review 4 at 4b812e5 rated every original category 7/10. These ratings precede the implementation below; reassessment is pending.

- HUD, Glass and cosmetic editing now use SettingMetrics for row measurement and placement. Their scroll regions expose logical controls beyond the viewport and reveal focused controls. Collapsed HUD bodies are not drawn or registered as active controls.
- HUD position controls synchronize after dragging, nudging and external layout changes. A subsequent slider adjustment continues from the displayed element position instead of a selection-time copy. HUD Page Up/Down uses its context directly. Mouse wheel and character overrides are retained; dedicated native checks cover them.
- Slider track and primary-arrow adjustments replace stale invalid drafts with the new valid value and clear the matching error. Cancel after a replacement uses that replacement as its baseline; arrows within an active text edit retain normal Escape-to-edit-baseline behavior.
- Numeric sliders now keep their label visible above a named value field, with a separate reserved validation line and track. Label hit bounds follow font metrics. String/coordinate editors use the same label/field/error geometry. Text field accessible names are separate from placeholders and include setting context/range. Keyboard focus reveals the owning row, preserving the visible label during backward traversal into a text field.
- Combined module/setting queries separate module identity words from setting terms. Matching ordinary controls disclose, highlight and scroll into view without enabling the module. A breadcrumb identifies the matching setting; hidden/custom-editor matches are identified explicitly. Clearing search restores pre-search disclosure state.
- Cosmetic categories and preview poses offer inspectable choice lists. Choice-screen ownership is centralized, preserving the parent client draft and focus context on return. This also protects cosmetic enum choices. Abandoned children release the parent's retained resources.
- Sounds and Discord menu rows consistently open their named detail pages; their enable switches remain on those pages.
- Block selection shows translated block names, secondary IDs/tooltips and a Selected-only filter. Unknown stored IDs remain visible/removable. Native search fields have visible search hints.
- Large-type auxiliary titles and cosmetic metadata/preview hints have adjusted spacing. Empty search placeholders elide within their fields. HUD Done has its own backing and avoids the page-appearance gear. The context background keeps underlying HUD labels from obscuring form controls. Narrow Appearance uses a compact bounded autosave footer.

### Loop 4 verification

Dependencies verified again. Full Gradle check/build passed on final source (`/tmp/ux-loop4-hints-final.log`). Frozen final native run (`/tmp/ux-loop4-frozen.log`) passed 19 journey +65 layout/keyboard +58 auxiliary +14 actual-viewport +7 temporal assertions: **163 total**, not 163 separate user journeys.

The additional auxiliary journeys cover combined-query reveal/restoration and unchanged gameplay state; invalid text recovery through track/arrows; named numeric fields and full large-type label clicks; direct HUD character/wheel routing; position synchronization and continued adjustment; collapsed context exclusion; actual Tab traversal into HUD/Glass/cosmetic style controls; cosmetic pose-choice draft retention; and selected-block management.

Actual native resizing uses the isolated test client's Input.resizeWindow(960,720), GUI scale3 and a checked 320×240 live GUI viewport. It captures the font2 client, settled choices and backward text-focus reveal inside the small HUD context. This supersedes the former lack of native resizing coverage for these particular scenes; it does not prove every screen/size combination.

Preserved evidence: `/tmp/riserecode-ux-round4/final/`, with 29 PNGs, both validation logs, source-file SHA-256 values and an evidence manifest. The manifest is checked against the final checkpoint's source after commit. No frame-pacing, FPS, allocation improvement, audible narration, real container/multiplayer or optional-renderer matrix is claimed. Existing flite/cursor host warnings remain documented in the logs. No Modrinth installation or publication yet: this remains an in-progress feature checkpoint.

## Loop 5 implementation following review 5

Review 5 at 06cfb27 rated navigation 8, layout 7, hierarchy/readability 7, motion 7, feedback/recovery 7, keyboard/accessibility 7, discoverability 8, consistency 7, settings workflow 8, and perceived performance/economy 7 (average 7.3). These are the preceding checkpoint's ratings; the following implementation awaits independent reassessment.

- Rebuilt expanded ColorPicker measurement around shared setting metrics. Endpoint choices, HSV/alpha, labelled hex/error, mode/direction and all six nested sliders have separate measured positions; the expanded height follows that complete layout. The same geometry drives rendering, clipping and pointer targets.
- Color endpoints retain separate hex fields, invalid drafts, validation and saved color/mode baselines across blur and endpoint switches. Escape restores the matching baseline. Explicit channel/HSV replacements resolve the matching draft/error and become the new cancellation baseline. Mode and direction offer inspectable choice lists as well as arrow cycling.
- Nested hex/channel accessible names identify the owning color and start/end endpoint; speed/spacing identify the owner and animation property. Keyboard reveal includes the hex label and reserved error area.
- HUD, Glass and cosmetic pointer dispatch gates the pointer against the visible viewport/revealed section instead of requiring an entire large control to fit. Ongoing drags keep their capture/release route beyond the clip.
- Idle sliders use concise step/range-aware numbers. Explicit editing reveals the full stored value; an unchanged commit retains exact model precision. Invalid text continues to take priority over idle formatting.
- Resources catalog, categories, Tools and Selected views have logical keyboard scroll owners. Stable resource/category/action identities survive rebuilding, reorder and details return. Opening details focuses contextual Back; leaving restores the opener and scroll; removing a selection focuses its neighbor. Clear and unavailable selections keep explicit management routes.
- Resources uses font-aware detail line heights, measured Selected action wrapping and row heights, readable card typography, adaptive compact cards and footer layout. Short Tools views actually scroll. Details reclaim search/filter space; Ctrl+F returns to visible catalog search. The provider/status/Apply explanation remains available through the footer, full hover text and Tools provider action.
- Resource cards retain shared press feedback on pointer and keyboard activation. Invisible cards avoid preview extraction during logical collection; invisible Selected rows avoid title/preview painting; shared ActionButton extraction registers logical actions before skipping paint outside the clip. These are implementation changes, not measured FPS/allocation claims.
- At narrow reference widths with large type, the main sidebar becomes a compact icon rail. Full page names remain in tooltips and keyboard labels; Settings remains explicitly named through those routes. This preserves room for actual content.
- Compact cosmetic editing uses the full content width, hides browse/preview chrome until Browse, names the edited item in its section header, and excludes hidden browse pointer targets. Glass separates preview instructions, the avatar and three reset/keyboard-adjustment controls rather than laying instructions over the avatar.

### Loop 5 verification

Full Gradle check/build and the frozen native client run passed: 19 journey +65 layout/keyboard +58 auxiliary +14 actual-viewport +7 temporal +52 nested-control/Resources assertions = **215 assertions**, not 215 separate journeys. Final command: `GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check build runClientGameTest -PuxJourneyOnly -PresourcesOffline -PbundledFontsOnly --offline --no-daemon`. Log: `/tmp/ux-loop5-frozen.log` (BUILD SUCCESSFUL).

The new suite actually resizes the native window to960×720, checks320×240 GUI size, and traverses expanded HUD colors through final Blue at font .75/1/2. It checks measured nonintersection and visible channel pointer dispatch, invalid start/end drafts and Escape/replacement baselines, named channel context, concise HUD coordinates versus exact editing, expanded Glass/cosmetic pointer dispatch, and an Appearance color capture. Resources uses the641-entry offline catalog: backward Tab reaches the last card, details return restores identity, Selected detail/reorder/removal retains or repairs focus, Tools/category traversal reaches deep actions, and forced offline refresh failure offers Retry. Tests combine physical key input with direct native screen/component method calls; they do not constitute a complete manual end-user study.

Evidence was copied automatically on successful completion to `/tmp/riserecode-ux-round5/final/`:42 PNGs, the combined validation log, and a source/evidence SHA-256 manifest. Visual inspection covered large-type expanded HUD/Glass/cosmetic controls, the compact rail, reclaimed detail space, Selected and Tools views. Remaining clipped surrounding content and abbreviated labels are visible in captures; these are not a claim of universally flawless layouts. The manifest identifies the matching committed source after checkpointing; the build preceded commit, so this is source/runtime evidence rather than release JAR provenance.

No measured frame-pacing/FPS/allocation improvement, audible narrator success, comprehensive contrast certification, real container lifetime, multiplayer or optional-renderer matrix is claimed. The host still reports unavailable flite narration and the X11 standard-cursor warning. No Modrinth installation or publication yet: this remains an in-progress feature checkpoint.

## Loop 6 implementation following review 6

Review 6 at00f6702 rated navigation8, layout7.5, hierarchy/readability7.5, motion7, feedback/recovery7.5, keyboard/accessibility7.5, discoverability8, consistency8, settings workflow8 and perceived performance/economy7 (average7.6). These precede this implementation and await independent reassessment.

- Cosmetic Ctrl+F returns to visible Browse/search while preserving the outfit draft. Entering compact editing releases hidden search ownership; compact layouts release hidden import ownership and exclude hidden import pointer targets. The visible Edit action resumes editing. The new journey exposed a negative-height avatar viewport in compact Browse; short layouts now devote their width to the catalog/form, and avatar extraction also guards nonpositive bounds. Larger layouts retain the preview/import pane.
- Resources status is now a focusable, clickable help action in both footer layouts. Its accessible description includes phase, Apply gating, cache/retry/pause/resume recovery and actual failures. Activation opens Tools, expands its section and focuses Cache all/Resume/Pause. Attribution remains in the footer and Tools.
- Waypoint actions retain identity by UUID and operation across store revisions, with dynamic current waypoint values and owning-name accessibility labels. Edit/confirmation return restores action focus; confirmed deletion selects a neighboring waypoint or Done. Toolbar buttons wrap and list geometry follows that measured toolbar and font-aware row heights.
- HUD box annotations no longer draw competing names over HUD content. Box borders remain aligned to actual elements; hovered names use tooltips, selected names appear in the inspector and stable bottom instructions. Existing keyboard selection remains available.
- Narrow Glass uses explicit Settings/Preview modes, each with the full pane width. Preview controls use full Reset Rotation/Tilt/Zoom labels and separate Drag to rotate / Wheel to zoom instructions. Larger windows retain simultaneous settings/preview. This avoids shrinking the avatar into the space left by a tall stack of controls. Keyboard descriptions retain arrow-adjust and activation/reset semantics.
- Friends separates name inspection from explicit named Remove actions. Removal requires confirmation; cancel preserves the original stored UUID/name. Confirmation returns focus to the neighboring friend; cancellation returns to its Remove action. Cached actions survive screen reinitialization.

### Loop 6 verification

Final frozen Gradle check/build and native run passed:19 journey +65 layout/keyboard +58 auxiliary +14 actual-viewport +7 temporal +52 nested/Resources +18 recovery = **233 assertions**, not233 user journeys. Command: `GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check build runClientGameTest -PuxJourneyOnly -PresourcesOffline -PbundledFontsOnly --offline --no-daemon`. Log:`/tmp/ux-loop6-frozen.log`, BUILD SUCCESSFUL. The new18 recovery assertions also passed separately before the frozen combined run.

The recovery suite uses a live960×720 window/GUI scale3=320×240 and font2. It checks visible cosmetic Ctrl+F, query/draft retention and hidden-owner release;24-waypoint late-row visibility/tracking mutation, backward traversal, editor return and Escape/confirmed deletion; two named friends with UUIDs through inspection, Escape cancellation and confirmed removal/neighbor focus; and a selected Resource draft with a forced offline refresh failure, full focusable Apply reason and keyboard activation into Cache all. Setup/actions combine physical keys and direct native screen/component methods. Tests restore the isolated test store/friends after completion. This is bounded runtime coverage, not a manual user study or all loading/paused/cache permutations.

Automatically preserved evidence:`/tmp/riserecode-ux-round6/final/`,46 PNGs plus the combined log, source/evidence SHA-256 receipt and matching committed source identifier. Captures include clean HUD annotations across existing scenes, compact cosmetic search/form, full-width Glass settings/preview, late-waypoint mutation and Resource keyboard recovery. The receipt is source/runtime correspondence; build preceded commit and is not release artifact provenance. Existing main/nested coverage remains; the new suite supplements it.

No frame-pacing/FPS/allocation improvement, audible narrator success, universal contrast/localization, real-container lifetime, multiplayer or optional-renderer matrix is claimed. Existing flite/cursor host warnings remain in the log. Short cosmetic layouts currently dedicate space to browsing/editing and omit the avatar/import preview pane; larger layouts retain it. No Modrinth installation/publication yet: the feature remains in progress.

## Loop 7 implementation following review 7

Review7 at7b07c46 rated navigation8, layout7.5, hierarchy/readability8, motion7, feedback/recovery7.5, keyboard/accessibility8, discoverability8, consistency8, settings workflow8, performance/economy7 (average7.7). Those ratings precede this implementation.

- Compact cosmetics has an explicit Preview/acquire route into a full-window child workflow. Avatar, rotation/zoom, Download/Cancel/retry, Select in draft, local PNG import, status and return no longer depend on the omitted compact side pane. Parent draft/query ownership remains retained; larger layouts retain their pane. Import completion checks the editor revision so a discarded/replaced draft is not reintroduced by late I/O.
- Added a retained-child base for bounded client workflows and themed, measured child-screen text. Native child actions and text inputs remain accessible. Full message/help narration strings are supplied; audible speech is not established on this host.
- Replaced timed footer-overlay notices with a permanently reserved, focusable header message control. It opens wrapped/pageable full text, preserves named feedback across navigation and consumes its own pointer target. It does not paint or click through to footer controls. Removed the obsolete feedback animation/timer.
- Duplicate client and appearance profile saves now open explicit Save as / named Replace / Cancel decisions. Save as accepts only a new name. Existing profile writes preserve one previous document; Profiles offers Restore previous for each recoverable preset. Appearance existence/recovery uses the same case-normalized paths as saving. New versus replaced feedback is named. A failed backup/save reports failure; cancellation leaves stored bytes/active identity untouched.
- Cosmetic zero-result states identify category and Installed/Catalog context, with logical-scroll keyboard actions to Clear search or Show catalog. Recovery preserves outfit draft and selection.
- HUD has a named Help action and H shortcut exposing complete selection, fine/coarse nudge, grid, inspector scrolling and save/exit instructions in a readable pageable screen. Bottom guidance keeps a short stable selected name/help hint instead of eliding the whole shortcut set.
- Cosmetic managers accept an injected acquisition provider for deterministic offline workflows without production networking; the ordinary constructor retains its existing provider. New tests create their own PNG/catalog fixture outside Git.

### Loop 7 verification

Final frozen full-source Gradle check/build and native run passed:19 journey +65 layout/keyboard +58 auxiliary +14 viewport +7 temporal +52 nested/Resources +18 recovery +38 completion = **271 assertions**, not271 separate journeys. Command: `GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check build runClientGameTest -PuxJourneyOnly -PresourcesOffline -PbundledFontsOnly --offline --no-daemon`. Log:`/tmp/ux-loop7-frozen.log`, BUILD SUCCESSFUL. The38 completion checks also passed independently; an earlier combined run passed before final narration/import-lifetime changes, and the complete run was repeated afterward for this receipt.

The new completion suite uses the actual960×720/GUIscale3=320×240 window/font2. It covers compact zero-results recovery and retained draft; the actual compact details route; invalid and valid64×32 PNG import; a separate cosmetic-manager/local-provider fixture through start, cancel, explicit offline failure, retry, checked cached asset, draft selection and apply; named reserved message-control geometry, native pointer hit/full text and clear footer; duplicate client/appearance save cancellation preserving bytes, Save as, explicit Replace and Restore previous content, including case-normalized appearance names; and complete HUD Help. Native buttons are invoked with native KeyEvent arguments; setup, Ctrl/Shift paths, some actions and assertions use direct native methods. This is bounded functional evidence, not complete physical-input/manual user testing. The acquisition fixture is not live GitHub/network proof. The late-import revision guard and narration text are present in the validated source; the suite does not directly race every abandonment case or establish audible speech.

Successful-run capture copied55 PNGs plus the combined log and source/evidence SHA-256 manifest to `/tmp/riserecode-ux-round7/final/`. Visual inspection covered compact empty/details/import/error, reserved header messages/clear footer, explicit profile replacement and complete themed HUD help; the earlier scene set is retained. Source hashes are checked against the committed checkpoint. The build precedes commit, so this is source/runtime correspondence rather than release JAR provenance.

No measured frame-pacing/FPS/allocation improvement, audible narration, universal localization/contrast, real container lifetime, multiplayer or optional-renderer matrix is claimed. Existing flite/cursor host warnings remain. No Modrinth installation/publication yet: the feature remains in progress.


## Loop 8 implementation following review 8

Review 8 at14d047f rated navigation8, layout7.5, hierarchy/readability8, motion7, feedback/recovery8, keyboard/accessibility8, discoverability8, consistency8, settings workflow8 and performance/economy7 (average7.75, displayed7.8). These scores precede the changes below; they are not ratings of this implementation.

- The cosmetic preview clock now advances through a shared bounded method in whichever screen owns the avatar. The full-window child no longer freezes animated wings/companions. Explicit Pause/Resume preserves phase; Reduced Motion deliberately pauses avatar animation. The clock does not advance twice in wide layouts.
- Compact cosmetic preview has separate Acquire/import and View controls modes, with native focusable rotation, zoom, Reset view, an inspectable pose choice and Pause/Resume. Arrows adjust view and R resets when a text field does not own input. Wheel zoom is scoped to the avatar. View-only operations leave the outfit draft unchanged; reset uses the selected category's useful default orientation.
- Native cape-path and profile-name drafts survive real window resize/reinitialization, preserving text, caret/selection and useful field focus. A final audit caught ambiguity when selected text repeats on both sides of the caret: the existing native EditBox mixin now exposes the exact selection anchor rather than guessing it from text. A dedicated native resize check covers the ambiguous rightward selection. Visible path/name/format guidance remains during entry. Profile validation survives resizing and clears when the attempted value changes. An import already in progress preserves its status/path through resize and retains the existing editor-revision guard on asynchronous completion.
- Cosmetic status and profile save errors offer focusable full-text details. Shared wrapping now splits long tokens at measured grapheme boundaries, retains every character and explicit paragraph break, and caches layouts by text/width/font/scale/generation. Keyboard paging reaches final error characters. Nested retained help/pose workflows preserve their caller's ownership rather than disposing the suspended parent.
- Restore previous explicitly says it restores the saved preset, leaves active settings unchanged and consumes one previous saved version. A named Load restored client/appearance action deliberately applies that recovered document. Saving that preset again removes stale recovery-action state. Friends guidance now describes inspection and explicit removal accurately.
- Keyboard/text intent completes decorative window/page/disclosure/row-position geometry, keeping logical controls and displayed rows aligned during long transitions. Pointer use releases keyboard mode and retains pointer animation. Navigation collected before the next rendered frame queues the entrance action; closing still blocks background actions. This resolves a real row-position overlap found while extending the timed keyboard checks.
- A native allocation profile identified repeated Appearance matching/group/row construction as a measured hotspot. Appearance now splits query terms once, caches translated search metadata, checks dynamic visibility before rebuilding matched groups, reuses row structures by filtered-list identity/width/padding/font and avoids repeated stream row-height reductions. Dependent-setting visibility and multiword queries remain live. Performance results and limitations are recorded separately below.

### Loop 8 targeted verification

The standalone native refinement run passed111 assertions before the final repeated-text anchor correction (`/tmp/ux-loop8-refinement6.log`). It uses an actual960×720 window/GUIscale3=320×240/font2. Checks observe actual submitted cosmetic age, wing time and companion model vertices; physical Tab/Enter covers pose/zoom/reset/pause; Reduced Motion and zero-speed behavior are explicit. Other cases exercise physical text entry and native resize, caret/selection, an import held pending through resize, exact long-token wrapping/paging, both preset types through duplicate error/Save as/Replace/disk restore/explicit Load, default.2s and slow1s transitions with Reduced Motion on/off,100ms pointer probes, early Tab and typing, deep slider editing/collapse/reopen, interrupted close/reopen and rapid page changes. These are111 assertions, not111 independent user journeys.

The Fabric test input helper constructs key events with zero modifier bits even when holding modifiers. Modified shortcuts therefore inject a native KeyEvent with explicit modifier flags; subsequent characters and ordinary Tab/Enter are physical test input. Some setup/actions use component methods. Timeline observation timestamps include test barriers/scheduling and are not measured physical-input latency. The first nominal100ms pointer probe in a1s opening can occur while the scaled control is still below its interaction threshold; the timeline records that rather than asserting universal immediate interaction. Actual animation-state progression is established; still captures alone do not establish continuous smoothness.

### Loop 8 final verification and performance

Final Gradle check/build and the frozen combined native run passed:19 journey +65 layout/keyboard +58 auxiliary +14 actual viewport +7 temporal +52 nested/Resources +18 recovery +38 completion +113 refinement/timed interaction = **384 native assertions**. The final113 include the repeated-text selection-anchor resize case. Command:`GRADLE_USER_HOME="$PWD/.tooling/gradle-home" ./gradlew check build runClientGameTest -PuxJourneyOnly -PresourcesOffline -PbundledFontsOnly --offline --no-daemon`. Log:`/tmp/ux-loop8-frozen.log`, BUILD SUCCESSFUL. Source/evidence capture immediately afterward preserved67 PNGs, the combined log, observed interaction timeline and SHA-256 manifest in `/tmp/riserecode-ux-round8/final/`. These are assertions, not384 distinct user journeys.

Paired menu measurements are documented in [round-8-performance.md](round-8-performance.md) with raw trials, environment, JFR, source/class receipts and an interactive comparison under `/tmp/riserecode-ux-round8/performance/`. At explicit60/144 test-only caps, the medians across three30s Appearance trials show63.4%/65.3% lower extraction allocation and25.3%/29.9% lower extraction elapsed; render-thread CPU/frame is14.5%/14.4% lower. Idle/Resources timings vary in both directions; no general whole-client improvement is inferred. The preview baseline was frozen and the candidate animates, so its workload differs. The uncapped exploratory runs and interrupted pilot are excluded from the primary paired comparison. Baseline source hashes were checked against14d047f; candidate source/class hashes were checked unchanged immediately after measurement. The later correctness-only native selection-anchor fix affects two production files, whose exact benchmark versions are preserved separately. Final runtime source hashes match the completed combined build; benchmark and final-regression snapshots are explicitly distinct.

Visual inspection covered the new compact preview controls and animation-pose pair, complete long-error first/last pages and paragraph/URL wrapping, named saved-versus-active profile recovery, and slow-transition Appearance search at large type. Pictured readability is bounded to those states; title/field typography and abbreviated metadata still deserve independent scrutiny. Native selected pose/animation state and exact text behavior are checked separately from still images. The baseline JFR attribution export was summarized only after timed runs; it records statistical sample weights with overlapping ancestor methods, not exact per-method allocation totals.

This evidence does not establish GPU time/display latency, universal FPS or continuous manual smoothness, audible narration, universal contrast/localization, real-container lifetime, multiplayer or optional-renderer compatibility. Known host flite and X11 standard-cursor warnings remain. The build precedes checkpoint commit, so the receipt establishes source/runtime correspondence rather than release JAR provenance. No release installation or publication has occurred; this remains an in-progress feature checkpoint awaiting independent review.
