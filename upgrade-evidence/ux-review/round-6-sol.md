# Independent UX/UI review — round 6

Reviewed 2026-11-09 at **00f670212e10110b4df2d2245ca0c4a049f82c68**, branch **feat/ux-motion-review**. HEAD and branch match; checkout clean at inspection. Read AGENTS.md, AGENTS.local.md, all five preceding Sol UX reports and implementation-log.md. No checkout edits, Gradle/client executions, legacy test executions, installation, publication or agents. Only this external report was written. Explicit read-only scope supersedes standing release authorization.

## Evidence and rubric

Independently hashed all **800 source entries against Git objects at the requested checkpoint**, and all **43 evidence entries against preserved files** in `/tmp/riserecode-ux-round5/final/manifest.json`: **zero mismatches**. The latter comprise **42 PNGs and native-check-build.log**. `validatedSourceCommit` names this checkpoint; `baseCommit` identifies the preceding checkpoint. These receipts establish source/evidence correspondence, not release JAR provenance, installed state, or independent rerunning of the client.

The supplied combined log records `:check` at line287, `:build` at289, six native suite results at442–448 and BUILD SUCCESSFUL at454. The counts are **19+65+58+14+7+52 = 215 assertions**, not 215 user journeys. Conventional `:test` is SKIPPED at239; custom JavaExec checks and native game tests are separate tasks. The legacy UI assertion count is not a usability score. Sampling UiChecks shows model/component logic checks; these do not substitute for native visual, input or error-recovery inspection. No legacy tests were executed by this reviewer.

Read all six Ux*IntegrationTest sources. Journey covers safe disclosure/toggle, empty/scoped search, draft leave/cancel, choice cancel, HUD caller return and map/list Done. Layout covers numeric invalid drafts, actual deep-module Tab, font2 row bounds, client search, block paging and HUD selection/nudge. Auxiliary covers combined-query reveal/restoration, numeric replacement/cancellation, HUD synchronization, Tab into auxiliary forms, cosmetic pose draft retention and Selected blocks. Viewport and Nested actually call Input.resizeWindow(960,720), set GUI scale3 and check a **live320×240** viewport. Older choice/waypoint320×180 Screen.init fixtures remain geometry fixtures inside a larger framebuffer. Nested covers expanded colors at .75/1/2, visible Blue pointer dispatch, endpoint draft/cancel baselines, contextual channel identity, concise/exact numeric entry and bounded Resources traversal/focus/reorder/removal/failure behavior. Shift-modified keys and numerous edits/setup/actions use direct screen/component methods; ordinary Tab and several activation keys use physical input. These mixed journeys do not prove every complete keyboard/pointer workflow.

The seven motion assertions sample opening geometry, advancement, immediate reversal continuity, settling and particular Reduced Motion behavior. They do not measure frame pacing or exercise interaction throughout slow transitions.

Used view_image to inspect **27 supplied PNGs**: 0000,0001,0009,0016,0017,0018,0021,0022,0023,0024,0025,0026,0027,0028 and0029–0041. These include every specifically requested new scene. Other PNGs were hash-verified but not visually inspected in this review. Main/module structure is coherent; expanded color channel labels/fields are separated; 0032 has distinct Glass instructions/model/controls; 0033 dedicates compact cosmetic space to the form; 0034 shows reachable Appearance Blue. 0035 exposes the final catalog card; 0036/0037 show readable detail line separation and scrolling; 0038/0039/0040 show deep Selected/Tools/category focus; 0041 visibly shows a disabled Apply and “Cache incomplete · Retry”. Some surrounding rows/action edges are clipped in these captures. That alone is not proof an entire control is unreachable, nor does focus-label existence prove complete visual legibility.

**Screenshot-observed** means visible in supplied pixels; **static** means derived from current source; **supplied runtime assertion** means explicitly checked by the preserved run; **unverified** means insufficient evidence. No interaction was reproduced live by this reviewer. No audible narration (flite unavailable), universal contrast, frame pacing/FPS/allocations, live container lifetime, multiplayer or optional-renderer proof is claimed. HUD FPS text is not a benchmark.

Unchanged rubric: **0** absent; **2** usually blocks the task; **4** partial implementation requiring significant discovery/workarounds; **6** usable for a familiar user with material gaps; **8** clear, consistent support for common tasks and edge cases; **10** excellent support demonstrated across ordinary users, screen sizes, inputs and failure states. Fractional scores reflect partial attainment, not measurement precision.

## Original category scores

Equal-weight average: **7.6/10**. Functional improvement is substantial, but remaining workflow defects and visible small-window compromises prevent a blanket8. Motion/performance scores do not rise merely because functional assertions increased.

| Original category | /10 | Assessment |
|---|---:|---|
| Navigation and orientation | 8 | Contextual Resources Back, stable return identity, scoped search and caller returns support common tasks. Waypoint rebuilds still lose action identity; compact navigation depends on icons/tooltips. |
| Layout and responsiveness | 7.5 | Measured nested colors and real small-window evidence close material defects. HUD annotations still overlap and Glass preview labels abbreviate. Compact layouts trade substantial visible context for space. |
| Hierarchy and readability | 7.5 | Named numeric fields, concise idle values and measured Resource detail text improve reading. HUD label collisions, truncated preview instructions and abbreviated page/draft titles remain visible. |
| Motion and transitions | 7 | Sampled continuity and Reduced Motion evidence remain useful. Slow-transition input behavior, auxiliary composition and frame pacing remain insufficiently demonstrated. |
| Feedback and recovery | 7.5 | Endpoint errors/baselines, numeric replacement and dirty guards improve recovery. Compact Resources hides actionable Apply help from keyboard access; Friends removal has weak recovery. |
| Keyboard and accessibility | 7.5 | Expanded-channel and Resource logical traversal now have meaningful runtime evidence. Hidden cosmetic search and waypoint focus reset remain concrete gaps. Speech output is unverified. |
| Discoverability and ordinary-user usability | 8 | Safe module inspection, inspectable choices, Selected block management and search breadcrumbs are strong. Some preview actions still need tooltip/keyboard discovery. |
| Consistency | 8 | Shared metrics, contextual fields and logical scroll ownership now extend to nested controls and Resources. Waypoint identity/recovery and hidden-search behavior remain exceptions. |
| Settings workflow | 8 | Color error/cancel policy, precise edits and Resources detail/reorder/return make common workflows coherent. Compact cosmetic Ctrl+F and inaccessible Apply help need repair. |
| Perceived performance and implementation economy | 7 | Offscreen ActionButton/card/Selected paint skipping is a sensible implementation improvement. Logical traversal still visits the catalog; Appearance/cosmetics rebuild collections during extraction. No measured latency or allocation change is established. |

## Reassessment: keep resolved findings resolved

Paths below are relative to `src/main/java/dev/riserecode/` unless stated otherwise.

- **Round5 F1, nested color geometry: addressed.** `ui/component/setting/ColorPicker.java:70–75,135–136` derives the full layout and each child stride from SettingMetrics. Supplied assertions check child separation at .75/1/2 and reach final Blue; 0029–0034 confirm the pictured expanded states. The former310/22-unit geometry is gone. These scenes do not certify every intermediate animation/localization state.
- **F2, whole-parent pointer rejection: addressed in requested auxiliary owners.** `ui/hud/HudEditorScreen.java:124–128`, `ui/component/GlassEditorScreen.java:152–161`, `ui/component/CosmeticsEditor.java:179–182` gate the visible viewport/revealed section and dispatch into intersecting children, retaining capture routes. Nested assertions directly click Blue in HUD/Glass/cosmetics. Do not retain the old claim that no portion of an expanded picker can accept pointer input.
- **F3, Resources logical traversal/identity: addressed in the covered paths.** `ResourcesEditor.java:90–107,137–146,164–174` retains stable actions and logical scroll owners. Nested assertions reach the last catalog card, contextual Back/return, selected reorder/removal, final Tools/provider and final category. Bounds, full forward traversal of641 cards and every refresh/filter/resize permutation are not universally proven. No resurrection of the old viewport-only collection finding.
- **F4, color hex loss on blur: addressed.** ColorPicker :55–61 retains endpoint-specific fields/errors and color/mode cancellation baselines; :89–95 implements explicit replacement. The supplied checks cover invalid start/end switching, Escape and channel replacement. Cross-endpoint mode interactions beyond those cases remain unverified, not presumed broken.
- **F5, contextual child names: addressed.** ColorPicker :48–57 supplies owning color, endpoint and channel/property context. Supplied label assertion supports the end-Blue case. Actual spoken narration remains unverified.
- **F6, raw idle precision: addressed.** `setting/Slider.java:28–30,76,84` separates concise idle text from exact active edits. Nested assertions check0.123 versus0.123456789123 and unchanged precision; 0018/0025 show concise coordinates. Original long-tail finding is closed.
- **F7, Resource typography: substantially addressed.** ResourcesEditor :109,121–130,169–185 measures line/action geometry. 0036/0037 show separated font2 paragraphs; Selected action wrapping is measured. Retain the specific compact help issue below, not the old fixed12-unit paragraph-overlap claim.
- Earlier safe module inspection, named Settings, dirty-exit guards, waypoint draft/cancel/delete confirmation, scoped client/Appearance search, combined-query reveal/restoration, numeric invalid-draft retention/replacement, HUD position synchronization and collapsed-body exclusion remain addressed within source/supplied evidence. 0016/0017/0018 support those particular states. The older pointer-only HUD selection assertion remains retracted.
- ChoiceScreen/MenuTheme suppress transient picker appearance gears and retain parent ownership in source. Do not revive the earlier secondary-gear disposal finding. Real container remote-close/abandonment still requires runtime evidence.

## Prioritized actionable findings

No new P1 established. F1–F4 are P2; F5–F6 are P3. Source-derived paths below were not executed by this reviewer.

### F1 — P2: Compact cosmetic Ctrl+F focuses an invisible search field

**Static regression introduced by hiding browse chrome.** `ui/component/CosmeticsEditor.java:99–104` skips search/top controls while editing at height<220. Its `keyPressed` at:189 nevertheless unconditionally focuses/selects the search field for Ctrl+F. `ui/clickgui/ClickGui.java:550` consumes that handler before normal search routing. TextField :49 establishes FocusedInput ownership; subsequent typing edits the hidden field. No transition back to Browse occurs. 0033 confirms that this compact state has no visible search field.

**Impact:** A standard search shortcut appears to do nothing, takes input away from the visible editor and silently changes the later catalog query. The user cannot see a caret, query or results while typing. Compact editing is the exact viewport now being presented as supported.

**Action:** Make Ctrl+F reveal Browse/search while retaining the draft, or provide a visible scoped form search. Release hidden search/import owners when changing layout modes; do not allow an invisible field to keep keyboard ownership.

**Acceptance:** At actual320×240/font2, open compact cosmetic Edit, press Ctrl+F, type/paste/clear a query and Escape/return. Focus/query/results are visible, the outfit draft remains intact and the form can be resumed predictably. Repeat resize into compact mode with search initially focused. Existing52 assertions do not cover this shortcut.

### F2 — P2: Compact Resources removes the keyboard-accessible explanation for disabled Apply

**Static regression plus screenshot-observed state.** `ui/component/ResourcesEditor.java:121` chooses compactFooter when height<240 or width<260. :186 explains “Cache resources in Tools before Apply”, but :191–194 draws that explanation only in the noncompact footer or a pointer-hover tooltip. :49 disables Apply when cache is unready; `ui/component/ActionButton.java:45–46` registers only enabled buttons. The provider Tools action at ResourcesEditor :48 reports attribution, not cache gating. Thus keyboard focus cannot reach disabled Apply to learn why or retrieve the hover help. In0041, Apply is gray and “Cache incomplete · Retry” is plain status text; there is no visible Retry button there. Recovery exists through Tools, but that relationship is not explained in the compact keyboard route.

**Action:** Keep a short visible “Tools → Cache all/Retry before Apply” explanation, or a focusable status/help action that includes phase, failure and recovery. A disabled button may expose its reason to focus without becoming activatable. Preserve compact attribution access.

**Acceptance:** With a selected draft and failed/incomplete cache at320×240/font2, keyboard-only users can discover why Apply is unavailable and reach the appropriate retry/cache action from the status. Verify loading/paused/failure/ready states and full accessible strings; separately verify audio on a working narrator host. The current failure assertion checks the progress model, not comprehension or keyboard access to this explanation.

### F3 — P2: Waypoint mutations rebuild every action and strand keyboard focus

**Static remaining surface gap.** `map/WaypointScreen.java:32–42` clears rows and creates fresh ActionButtons for every point. Visible/Track mutations at:39–40 change the store; :47 rebuilds on its revision. `ui/component/KeyboardNavigation.java:38,68–72` matches focus by object identity, and restarts from the beginning when that identity disappears. There is no stable waypoint/action key or requestFocus after rebuild. A user tabbing to a late waypoint and toggling Visible/Track loses that action identity on the next render; subsequent Tab starts at the toolbar. Generic “Visible”, “Track” and “Delete” labels also omit the owning waypoint. The native journey only activates manager Done; it does not test a long waypoint list mutation.

**Action:** Retain actions by waypoint UUID+operation, refresh values dynamically and restore the same logical action after mutation. Removal should select a predictable neighbor. Include waypoint name in action accessibility labels.

**Acceptance:** With20+ waypoints, keyboard-reach the final point, toggle visibility and tracking, edit/return, cancel/confirm deletion and continue both directions. Focus stays on the intended action/neighbor, is visible and identifies the waypoint. Do not require repeated traversal from the toolbar after every change.

### F4 — P2: HUD editor annotations collide with the HUD they identify

**Screenshot-observed and static.** 0017/0018 and actual-small0025/0031 show overlapping Watermark/FPS/Coordinates/Potion Effects labels and content at the upper left. These are editor annotations, not evidence that the underlying saved HUD values themselves overlap. `ui/hud/HudEditorScreen.java:75–81` draws a full-size label for every box, with a fixed10-unit offset or a label below the box near the top edge. No font-dependent separation, collision placement or selected-only policy exists. At font2 the .8 label is about14.4 logical units tall, exceeding the10-unit offset. Dense default boxes therefore get multiple conflicting labels exactly where users must select/move them. The new context form is much better; it does not fix selection-stage clutter.

**Action:** Show the selected/hovered element name in a stable inspector/tooltip, or place collision-aware annotation chips outside box content. Derive offsets from actual font metrics and keep pointer selection aligned with actual element boxes.

**Acceptance:** At .75/1/2 and actual320×240, a default dense HUD plus top-edge and adjacent elements has readable names without crossing HUD values or each other. Pointer and keyboard selection clearly identify one intended element. Capture the full editor before and after selection, not only a focused context child.

### F5 — P3: Glass preview actions still require interpreting truncated names

**Screenshot-observed residual readability issue.** 0032 improves separation of hint, avatar and controls, but shows “Drag rotates · Whe...” and “Ro...”/“Zo...” buttons. `ui/component/GlassEditorScreen.java:108–122` allocates three equal narrow columns and elides names. Pointer hover supplies full labels; keyboard labels explain arrows/reset. Thus controls are operable and this is not the old overlap or missing-keyboard defect. Ordinary users still must discover which abbreviated action resets which property, while the permanent hint omits wheel zoom.

**Action:** At narrow/high-type sizes wrap controls into full-name rows, use recognizable icons with labels, or move complete instructions into a visible expandable Help region. Preserve the separated model geometry and accessible names.

**Acceptance:** In the0032 size, Rotation/Tilt/Zoom and reset-versus-adjust semantics are discoverable without first hovering every abbreviated button. A complete zoom instruction remains available visibly and via keyboard help. Verify model/hint/control bounds after reflow.

### F6 — P3: Friends still removes on whole-row activation without recovery

**Static, previously noted polish now retained as an actionable issue.** `ui/clickgui/ClickGui.java:410–411` makes the entire “name ×” row remove the friend, then rebuilds actions. Unlike nearby client-profile and waypoint deletion, there is no confirmation or Undo. Clicking a name to inspect it can remove it; the transient “Friend removed” notice offers no restoration route.

**Action:** Separate the named row from an explicit Remove action, with short Undo or a confirmation. Restore useful focus after removal and include the affected name in the result.

**Acceptance:** Pointer/keyboard inspection cannot remove accidentally; explicit removal is recoverable with original identity intact. With a long list, removal leaves focus on a predictable neighbor. No runtime reproduction or claim of irreversible profile/data loss is made.

## Motion, implementation economy and remaining verification

No new animation smoothness defect is established from stills. `KeyboardNavigation.java:51` still rejects registration below .95 opacity, and ColorPicker :153 delays pointer children until its expansion is nearly complete. These are concrete input-availability policies to evaluate with slow transitions and Reduced Motion, not proof of a frame-pacing regression. The existing seven assertions do not interact throughout slow page navigation, nested expansion or interrupted focus changes. Acceptance follow-up should record timed focus/activation attempts during these states and confirm clear, consistent availability; capture temporal evidence separately from performance measurements.

ActionButton :46–47 registers logical actions before skipping clipped paint; ResourcesEditor :154–155 skips invisible card painting after registration, and :169–170 avoids invisible Selected title/preview painting. Those changes address unnecessary extraction paths. They do not establish FPS/allocations or eliminate all catalog traversal. ThemeEditor :173,189 still constructs filtered/layout collections in extraction; CosmeticsEditor :124 filters definitions during render. No user-visible slowdown is inferred without measurement.

Coverage spans main ClickGUI/module/search/sidebar, Appearance/client settings/page appearance, shared slider/text/color/choice/block controls, Resources catalog/details/Selected/Tools/failure states, cosmetics browse/edit/pose/draft paths, HUD context/selection, Glass preview/form, map/waypoint management/editing, Profiles/Friends and sampled vanilla-screen/page-gear/native-button integration. Source sampling is broader than the supplied successful journeys. The compact icon rail has named tooltips and keyboard routes (`Sidebar.java:94–106,110–116`); absence of permanent text is an intentional compact tradeoff, not the earlier missing-Settings-name defect.

Not independently verified: live world-map exploration/waypoint mutation journeys, profile load failures and restore behavior, full cosmetic acquisition/import/failure/retry, complete catalog/filter/selection permutations, real containers and remote closure, localization beyond the supplied partial German scenes, contrast over arbitrary gameplay/backgrounds, narration, renderer compatibility, multiplayer and frame-time/allocation behavior. NativeButtonLayout broadly compacts noncontainer widgets; the sampled source and picker captures do not prove every vanilla/third-party menu remains usable. No finding claims otherwise.

Recommended next work: repair F1/F2 and waypoint focus identity first; reduce HUD annotation clutter; then finish Glass labels and Friends recovery. Preserve the215 assertions, add the specific missing journeys, and keep source checks, supplied runtime assertions, screenshot observations and unverified behavior separate.

Memory registry lines50–51 informed preservation/read-only evidence discipline only; all scores and findings use this checkpoint and supplied evidence.
