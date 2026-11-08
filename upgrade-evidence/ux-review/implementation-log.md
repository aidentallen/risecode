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
