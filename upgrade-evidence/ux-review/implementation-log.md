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
