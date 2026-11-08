# Independent UX/UI review 2

Reviewed 2026-11-09, working tree based on `6d1afc0ad5e021f7506c641c88af965557f4fd72`. Source was changing during this review; fixes described as resolved were reread in current source. This reviewer changed no repository files, ran no Gradle tasks, installed/published nothing and did not delegate. Requested report written only to `/tmp/riserecode-ux-review-2.md`.

## Evidence and fixed rubric

Same categories and standard as review 1: 0 absent; 2 usually blocks the task; 4 partial implementation requiring significant discovery/workarounds; 6 usable for a familiar user with material gaps; 8 clear, consistent support for common tasks and edge cases; 10 excellent support demonstrated across ordinary users, screen sizes, inputs, and failure states.

Inspected current diff and source. Also inspected fresh native captures `build/run/clientGameTest/screenshots/0000_ux-combat.png`, `0003_ux-appearance-search.png`, and `0004_ux-resources-narrow.png`. `/tmp/riserecode-ux-native.log` at review time recorded `UX journey checks passed: 9` and `BUILD SUCCESSFUL in 33s`. This is evidence from the parent's execution, not execution by this reviewer. The inspected captures establish that these three states render and their pictured arrangements are legible at that captured size. They do not establish contrast compliance, animation smoothness, low-resolution behavior, keyboard-only completion, renderer compatibility or performance. Later source additions (separate keyboard disclosure, popup narration, new ChoiceScreen and additional guard tests) were inspected statically; the completed nine-check run does not verify them.

Equal-weight average: **6.5/10** (baseline 5.7). These are bounded usability scores, not visual certification.

| Category | /10 | Current assessment |
|---|---:|---|
| Navigation and orientation | 7 | Named Settings, sidebar focus targets and HUD caller return materially improve orientation. Utilities remain a secondary disclosure and HUD has no visible Done/Back. Nested option-screen lifetime has a late static fix requiring runtime verification. |
| Layout and responsiveness | 6 | Fresh narrow Resources capture confirms the search and category/action rows are separate. Removing height/576 scaling avoids unnecessary default miniaturization. Whole-window shrinking, tiny metadata and partial keyboard clipping remain; no Font Scale 2/long-German/small-native-window matrix. |
| Hierarchy and readability | 6 | Fresh captures show clear title/card/sidebar hierarchy and a clean Reduced Motion search result. Small secondary labels, compressed setting labels and abbreviated/truncated content persist. |
| Motion and transitions | 7 | Query typing no longer restarts every module entrance; Reduced Motion and shared animation scheduling remain good foundations. Composed motion and variable frame-rate behavior remain unmeasured. |
| Feedback and recovery | 7 | Dirty draft confirmation, waypoint creation as a draft, delete confirmation, clear-search and useful empty module-search text improve recovery. Error forms and blocked Apply still lack precise persistent explanations. |
| Keyboard and accessibility | 7 | Sidebar/Settings focus, separately focusable module disclosure, popup narration, map/list registration and HUD parent return close real gaps. Only fully visible controls register, no general auto-scroll-to-focus, and HUD element selection/movement remains pointer dependent. Narrator audio was unavailable in the native log, so speech output is unverified. |
| Discoverability and ordinary-user usability | 6 | Settings is named, module + and textual On/Off are visible, and normal enum activation opens inspectable alternatives. Main module body still activates gameplay without an explicit switch; technical settings lack explanations. |
| Consistency | 6 | Shared controls and labels improve consistency, but module body/disclosure, settings-popup toggle/details, cycling cosmetic categories and live-save versus Apply workflows retain different rules. Full-screen enum pickers add another navigation pattern and need lifecycle verification. |
| Settings workflow | 6 | Appearance scoped search and module-setting-name matching improve findability; Clear and Ctrl+F work with the scoped page. Client settings remain separate from search, zero appearance matches leave a blank body, and setting descriptions/units are still absent in primary controls. |
| Perceived performance and implementation economy | 7 | Cached filters/layouts, culling and central scheduling remain useful; eliminating entrance replay during typing improves stability. Appearance streams/row structures and cosmetics filtering still rebuild per frame. CPU/GPU/frame timing unmeasured. |

## Resolved or substantially improved since review 1

- U1: ModuleButton now has a visible plus/minus and textual On/Off; current source uses an independent keyboard disclosure focus target at ModuleButton.java:134–135. Its pointer disclosure is limited to the upper-right region at :202, preserving On/Off activation below it.
- U2: ClientSettingsPopup.java:59–62 names Settings and uses a gear. Sidebar.java:85–87 registers pages; popup :49 and :112–113 now narrate row values/instructions. The earlier missing-popup-narration concern is resolved statically.
- U3: ClickGui.java:84–98 guards dirty Resources/Cosmetics exits; page appearance was initially a bypass, but current `openPageAppearance()` also calls the guard. WaypointScreen.java:23 and WorldMapScreen.java:47 construct uncommitted waypoint values; Save inserts them in WaypointEditScreen.java:20. Delete has confirmation at WaypointScreen.java:41.
- U4: Map/list screens own navigation collection and route keyboard input; WaypointScreen.java:59 offers Page Up/Down, and WorldMapScreen.java:91 pan/zoom keys. HUD now returns to its caller at HudEditorScreen.java:164. Keyboard pan target zoom is clamped by draw at WorldMapScreen.java:58, so an initial unbounded-zoom suspicion is retracted.
- U5: ResourcesEditor.java:137–144 wraps narrow toolbar and adjusts viewport/input geometry. Confirmed visually in the fresh narrow capture.
- U6/U7/U9: ThemeEditor.java:67–78 adds scoped matching and temporarily reveals matching groups; EnumSelector.java:62–67 opens a searchable choice list; Panel.java:54 no longer restarts entrance motion on query changes.
- An intermediate ThemeEditor compile error was reported promptly and is fixed in current source at :229. It is not an outstanding finding.

## Outstanding findings

### R1 — Nested enum picker container lifetime: late fix inspected; runtime gate remains

**Initially reported static regression, now addressed in source:** Replacing PageAppearanceScreen with ChoiceScreen previously reached PageAppearanceScreen.removed and disposed its suspended container. Current EnumSelector.java:66 marks a page editor suspended for choice; PageAppearanceScreen.java:91 skips disposal while choosing; ChoiceScreen.java:38–41 forwards validity ticking, preserves pause behavior, resumes the parent on normal return, and disposes it when a picker is abandoned. PageAppearanceScreen.java:41 retains its editor across reinitialization, preserving scroll. These changes directly address the reported causes and the finding is not an outstanding source defect.

**Acceptance still needs runtime evidence:** Open a chest/inventory appearance editor, open and cancel/choose each enum, return to the same setting/scroll, and finish back in the same valid container without an unintended close packet. Also close the container remotely while the picker is open and confirm clean disposal. No completed native log for this exact late source was available at finalization; `/tmp/riserecode-ux-validation.log` was still in progress.

### P2 / R2 — Text scaling still reduces readability instead of reflowing

**Static and limited visual evidence:** Slider.java:72 and EnumSelector.java:47 shrink complete labels to fit. CosmeticsEditor.java:124 renders attribution at .52, ResourcesEditor.java:184 uses .54 for the details hint, and :201 uses .5 for attribution. Fresh Resources capture shows these secondary lines much smaller than action labels. ClickGui.java:226 still scales the entire logical window down when it does not fit. Adding Font Scale can therefore enlarge desired text and cause compensating per-label shrink rather than a readable line wrap.

**Fix:** Minimum body/secondary sizes, wrapping or two-line label/value layouts, full focused descriptions, and a comfortable density mode. Preserve working layout slots and test long localized labels.

**Acceptance:** Captures at minimum native window, logical 360×240, Font Scale .75/1/2 and German demonstrate readable complete controls with no collisions; pointer targets remain usable. No pixel contrast claim without measurement.

### P2 / R3 — Settings search needs complete coverage and an explicit empty state

**Static:** ThemeEditor.matches at :77 only matches group plus raw setting name. It excludes descriptions and translated names; client Sounds/Scale/Performance settings are not searched by the Appearance field (constructor :100 excludes Interface Sounds and Interface Scale). No matching groups renders a blank body in :167–202. Module search matches setting names but returns the module card without revealing/highlighting the matching setting.

**Fix:** Match display/localized names, descriptions and aliases; index client settings and jump to their page/control; show result count/breadcrumb and a stable no-results state with Clear. Reveal the actual matching module setting without enabling the module.

**Acceptance:** Search blur, reduced motion, scale and click volume in the advertised settings search; reach each actual control without knowing its section. A nonsense query clearly explains no results and recovery.

### P2 / R4 — Focus traversal does not reach content beyond the viewport automatically

**Static:** KeyboardNavigation.java:40–41 rejects any partly clipped control; :54 cycles only the current entries. Scrolling and focus registration are separate, so Tab wraps through what happens to be visible instead of continuing into the rest of a long form. Sidebar can have the same condition when many utility pages are shown. HUD element selection/drag remains in pointer handling; returning to its caller alone does not create a keyboard editing journey.

**Fix:** Keep a logical focus order for all eligible controls, reveal the next focused item through its scroll owner, and register HUD elements with select/move/nudge actions. Preserve native EditBox behavior.

**Acceptance:** Tab from the top to the bottom of a long Appearance/module/list page without manual scrolling; focus remains visible and can return backward. Select and move a HUD item with keyboard only. Page Up/Down is useful supplemental support but is not equivalent to continuous traversal.

### P2 / R5 — Recoverable failures still rely on short or broad messages

**Static:** ClickGui notice lifespan is still three seconds; Resources Apply remains disabled by cache readiness at ResourcesEditor.java:63 without an explanation attached to that action. WaypointEditScreen.java:20 responds to any invalid coordinate/dimension/color with the same broad error. TextField validation remains focus-dependent. Dirty editors now warn on exit but still lack a persistent dirty marker or live-save explanation.

**Fix:** Field-specific persistent errors, disabled-action reason, useful retry/empty/failure states, a dirty badge and explicit “Changes save automatically” in live editors. Keep short success toasts.

**Acceptance:** Invalid color identifies Color and valid format; invalid X identifies X; errors survive focus movement until corrected. A selected resource with unready cache explains how to enable Apply beside the action.

### P3 / R6 — Finish visible action semantics and guidance

Module headers now support safe inspection, but ordinary left-click across the body still activates gameplay while only a small On/Off word conveys state. Popup Sounds/Discord still use left click for toggling and an edge/right-click/Right key for details (ClientSettingsPopup.java:91–95,104,113). HUD still has no visible Done button. Cosmetic category and pose still cycle without showing alternatives (CosmeticsEditor.java:46,54). Map pan/zoom keys are implemented but not taught in its status line at WorldMapScreen.java:66.

**Fix:** Explicit switch target separate from a card's inspect action; consistent detail chevrons, visible HUD Done, named choice lists for cosmetic category/pose, and concise contextual keyboard hints.

**Acceptance:** First-time user can inspect module behavior, choose a cosmetic category, edit settings and return without needing right-click discovery or external instructions.

## Verification priorities

First verify R1's late fix with native container lifetime. Then run the expanded dirty-draft/On-Off/choice journey on final source, and use keyboard-only journeys rather than merely asserting registered labels. Capture no-match Appearance, German, Font Scale 2, small native window, resource error/paused/unready states, waypoint invalid fields and nested picker cancel/return. Static functional checks and screenshots do not establish animation smoothness, accessibility compliance or performance.

Memory consulted: MEMORY.md:48–49 and :59 only informed strict review scope and the evidence boundary; findings and category scores come from current inspected source and the noted captures.
