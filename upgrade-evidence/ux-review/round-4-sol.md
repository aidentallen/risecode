# Independent UX/UI review — round 4

Checkpoint **4b812e5076377fc864816ecbf7ed4c3c1cce194a**, `feat/ux-motion-review`, reviewed 2026-11-09. HEAD and branch matched the assignment; checkout was clean before and after inspection. Read AGENTS.md, AGENTS.local.md, round-{1,2,3}-sol.md and implementation-log.md. Wrote only this external report. No checkout changes, Gradle, client launch, installation, publication or agents. Explicit read-only scope supersedes standing release instructions.

## Evidence and rubric

Same ten categories and standard: **0** absent; **2** usually blocks the task; **4** partial implementation requiring significant discovery/workarounds; **6** usable for a familiar user with material gaps; **8** clear, consistent support for common tasks and edge cases; **10** excellent support demonstrated across ordinary users, screen sizes, inputs, and failure states.

Inspected all **19 PNGs** under `/tmp/riserecode-ux-round3/screenshots/` with view_image. Verified all **20 manifest SHA-256 entries** (19 PNGs plus log): no mismatches; manifest names the requested commit. `validation.log:287` records check, :442–444 records 19 journey +65 layout/keyboard +7 temporal assertions, :450 records BUILD SUCCESSFUL. Read all three native UX test sources. These are preserved results from the parent's execution, not tests run by this reviewer.

**Screenshot-observed** means a pictured state, **static** means source-derived behavior, **runtime assertion** means the supplied test checks it, and **unverified** means no adequate execution evidence. The 65 layout checks include individual widget/row assertions; they are not 65 distinct user journeys. The deep-module test actually presses Tab; block paging invokes keyPressed directly; HUD selects through actual Tab/Enter then calls nudge directly. Motion tests establish sampled geometry, interruption continuity and selected Reduced Motion behavior, not frame pacing.

Choice/waypoint 320×180 fixtures initialize a Screen at those logical dimensions inside a larger native framebuffer. They establish bounded widget geometry, not native viewport resizing or settled visual readability: especially choice capture 0012 is taken during faint entrance motion. Font2 minimum module capture 0009 does combine configured 360×240 logical window, padding20, German and Font Scale2. No audible narration, contrast compliance, FPS, allocations, frame pacing, real multiplayer/container lifetime or all-renderer proof is inferred. The log reports unavailable flite and an X11 cursor warning. The FPS number pictured in the HUD is not a performance benchmark.

A quick memory registry search found no relevant UX entry; no memory-derived facts are used.

## Category scores

Equal-weight average **7.0/10**. Equal scores reflect different remaining gaps; fixes do not mechanically earn points.

| Fixed category | /10 | Assessment |
|---|---:|---|
| Navigation and orientation | 7 | Named Settings, safe inspection, scoped forms and caller returns work coherently. Auxiliary context/focus, picker orientation and hidden utility navigation still need finishing. |
| Layout and responsiveness | 7 | Module font-aware heights and narrow Resources are materially better; minimum logical capture has separate readable rows. HUD/Glass/cosmetics retain old geometry and actual small native viewport coverage is absent. |
| Hierarchy and readability | 7 | Expanded module no longer crowds stacked enum lines. Minimum content width still produces extensive abbreviation; tiny metadata, generic input narration and fixed auxiliary typography remain. |
| Motion and transitions | 7 | Removing module entrance scale/fade simplifies composition; sampled opening/reversal/settling evidence is useful. Keyboard gating and nested contexts are incompletely exercised; smoothness remains unverified. |
| Feedback and recovery | 7 | Draft guards, specific waypoint errors, persistent invalid text and cache explanation help. Slider recovery can leave stale invalid text over a valid new value. |
| Keyboard and accessibility | 7 | Actual deep module Tab traversal, HUD selection/nudge and native block input close substantial gaps. Auxiliary scroll contexts and focus labels remain incomplete. |
| Discoverability and ordinary-user usability | 7 | Card body now inspects, On/Off toggles, and HUD shortcuts are taught. Settings detail edges, cosmetic cycling and technical block IDs still impose discovery costs. |
| Consistency | 7 | Main modules and Appearance share metrics and logical reveal. HUD/Glass/cosmetics use older metrics and viewport focus; choice and multi-select patterns still differ. |
| Settings workflow | 7 | Client scoped search and module setting match reveal improve task completion. Mixed module/setting queries do not reliably reveal the intended setting; HUD position controls can become stale. |
| Perceived performance and implementation economy | 7 | Cached module filtering, pointer-mode culling and shared animation scheduling are sensible. Keyboard traversal renders offscreen controls and Appearance/cosmetics rebuild render-time collections; no measured performance defect or improvement is established. |

## Reassessment of round 3

- **F1 secondary gear disposal route: addressed statically.** `ui/menu/MenuTheme.java:74–75` excludes TransientScreen, implemented by ChoiceScreen and BlockPickerScreen. Choice capture 0005 no longer has the gear. Layout test explicitly asserts choice gear suppression. Do not report the same route as outstanding. Real chest/inventory ownership, remote close and abandoned-picker behavior remain unverified rather than presumed broken.
- **F2 offscreen Tab: substantially addressed in the main forms.** `KeyboardNavigation.java:23–27,43–45,61–62`, Panel scroll-region registration and ThemeEditor reveal retain logical controls and reveal focus. The native deep-module test presses Tab up to the last visible slider, asserts nonzero scroll, and captures its ring in 0010. That is meaningful runtime evidence. It does not prove every scroll region, backward traversal, or fresh Shift+Tab.
- **F3 module heights: addressed for ordinary controls.** `SettingMetrics.java:9–15` is used by `ModuleButton.java:114–115` and ThemeEditor. Captures 0001/0008/0009 show separated lines, and the test checks visible model row bounds at font2. Shared metrics have not reached every editor (R1).
- **F4 sidebar/short auxiliary forms: substantially improved.** Sidebar width/spacing and branding are font-aware. 0007/0009 show readable Settings and separated branding. Lower navigation now scrolls rather than all fitting at once. Small-form tests assert vertical widget bounds; native resizing, error states and font2 auxiliary forms remain unverified.
- **F5 invalid text loss on blur: addressed in the inspected string/slider/coordinate paths.** The tests explicitly check slider/coordinate invalid text and errors after blur plus cancellation clearing errors. StringControl preserves invalid text while validation exists. Remaining cross-input recovery is R4.
- **F6 client search: addressed for the new scoped pages.** 0011 shows volume leading to Click Volume; test keeps Ctrl+F on Sounds. Module matching now includes display names/descriptions, expands and highlights setting matches. This is not a unified search across all client pages, and mixed queries remain incomplete (R3).
- **F7 HUD claim correction:** prior source already registered HUD elements for Tab/Enter (`HudEditorScreen.java:71–73`). The earlier pointer-only selection claim was overbroad and is retracted. New Ctrl+arrow nudging plus native selection evidence addresses the editing gap. Block picker now uses native EditBox, paginated rows and visible Done; 0014 shows page2/100. Full auxiliary context traversal is still R2.
- **F8 main module semantics: addressed.** Body/plus inspect and the explicit state target toggles (`ModuleButton.java:209–214`), verified in the native test. Cosmetic cycling and popup toggle/details remain different (R7). Slider/enum keyboard labels now include descriptions.
- Motion: removed module entry scale/fade is confirmed by source, not just the log. No proven frame-pacing regression is claimed from stills. 0016–0018 and the seven temporal assertions support the particular opening/reversal/settling path.

## Prioritized actionable findings

All source paths below are relative to `src/main/java/dev/riserecode/`. These remaining defects are **P2** unless marked P3. No new P1 was established.

### R1 — Apply font-aware row metrics to auxiliary editors too

**Static, with one screenshot symptom.** HUD still allocates 28 per non-color row (`ui/hud/HudEditorScreen.java:177`, rendering :101–103). Glass uses 23 for enums/booleans, subtracting5 from rendered height (`ui/component/GlassEditorScreen.java:66,89–93`). Cosmetics uses 23 for enum rows plus5 spacing (`CosmeticsEditor.java:109–112`). EnumSelector renders two lines (`setting/EnumSelector.java:50–53`), totaling 30.6 logical text-height units at font2 before spacing. Thus Glass's 23-unit stride and HUD/cosmetics'28-unit strides can still intersect the next row. Main-module tests do not cover these owners.

HUD also renders controls regardless of section expansion: panel height uses bodyHeight, but the loop at :101–104 is unconditional. Capture0015 visibly shows the beginning of “Scale:1.00” at the collapsed context panel's bottom. This should be suppressed when collapsed, not treated as evidence that the full context is usable.

**Action:** Use SettingMetrics consistently in total measurement, row placement, scissor, hit test and reveal. Draw/activate only the section's revealed body. Reserve readable validation space.

**Acceptance:** At font .75/1/2, open HUD Layout & Style, Glass material and cosmetic Edit; capture stacked enums/sliders and expanded colors. No text/next-row intersections, clipped labels inside nominally visible controls or leaked body when collapsed. Pointer and keyboard bounds match the drawing.

### R2 — Finish logical Tab reveal in HUD, Glass and cosmetic scroll regions

**Static.** HUD context clipping (`HudEditorScreen.java:99–105`), Glass material clipping (`GlassEditorScreen.java:79–99`) and cosmetics list/settings clipping (`CosmeticsEditor.java:105–127`) do not push a logical scroll region. `KeyboardNavigation.java:43–44` therefore still rejects clipped controls there. Cosmetics additionally culls offscreen rows at :112,116. Tab cannot continue through these complete forms merely because main Panel and ThemeEditor now can. HUD PageDown fallback targets 75% screen width (`mixin/KeyboardHandlerMixin.java:30–34`), while its context is right-aligned, so that fallback can miss the panel entirely.

**Action:** Give each owner a stable logical focus list and reveal callback, including backward entry and partially clipped controls. Keep focus identity on picker return. Do not require manual pointer scrolling as the keyboard workaround.

**Acceptance:** With no mouse or manual scroll, Tab/Shift+Tab through the whole HUD style context, long Glass form, cosmetic catalog and cosmetic edit. Every enabled control is reachable with a visible ring, including after choice return and a size change. Retain the existing deep-module journey; add these separate journeys.

### R3 — Search filtering and matched-setting reveal disagree for ordinary combined queries

**Static.** `Panel.java:63–68` matches each query word against the combined module/description/settings haystack. `ModuleButton.java:88–90` requires every word to match a single setting. Searching **“kill aura rotation speed”** therefore returns Kill Aura, but no setting contains all those words, so a previously collapsed card stays collapsed and no Rotation Speed is revealed. The user has already specified both module and setting. The supplied tests do not exercise module matched-setting reveal at all.

**Action:** Return a structured match identifying module terms and setting terms, then reveal the appropriate control/group. Support specialized module editors explicitly when their settings have no ordinary control adapter. Preserve enabled state. Give a result breadcrumb and deliberate restoration when clearing.

**Acceptance:** From collapsed Search results, “kill aura rotation speed” and localized equivalents open/highlight/reveal Rotation Speed without enabling Kill Aura; a bare setting query does likewise. Test deep settings, hidden/dependent settings and specialized editors. Canceling a choice returns to that result and focus.

### R4 — A valid slider track edit can leave an invalid draft/error covering its new value

**Static, residual recovery defect.** `setting/Slider.java:78–80` renders input whenever validation is nonempty. Track editing calls setFromMouse (:106–111), updating the saved model but never clearing validation or replacing the invalid draft. Its keyboard adjustment callback (:61–64) has the same omission. Enter “bad”, blur, then drag the track: a valid new value is saved while the visible field still says “bad” with an error. This is different from the repaired blur-loss issue. Current tests stop after blur/cancel.

**Action:** Define track/arrow adjustment as an explicit replacement of the invalid draft, synchronize text and clear validation; alternatively require an explicit resolve/cancel before changing the model. Expose the setting label while showing its invalid input.

**Acceptance:** Invalid text → blur → pointer track drag or focused slider arrow adjustment yields one coherent valid model/text/error state. Escape after the replacement cannot silently restore an unrelated older value. Cover both numeric entry and track recovery.

### R5 — HUD position sliders become stale after nudging or dragging

**Static; affects the newly added nudge workflow.** `HudEditorScreen.java:50–53` constructs Position X/Y as independent DoubleSetting copies at selection time. Nudge (:160–164) and drag (:132–137) change the element layout directly without updating those copies. Opening Layout & Style afterward shows old coordinates; adjusting one of those stale sliders jumps from the old coordinate. Selecting the element again rebuilds the controls, but that is an undisclosed workaround.

**Action:** Bind position controls to current layout or synchronize them after every move, preserving an active valid/invalid text draft deliberately. Avoid maintaining contradictory sources of position truth.

**Acceptance:** Select → nudge both axes → open style → inspect X/Y; values match actual layout. Adjusting by one step continues from the new position. Repeat after dragging and after changing selection. Test both axes and coarse nudges.

### R6 — Text-field keyboard labels and editing geometry lose setting context

**Static.** TextField narration uses its placeholder as identity (`ui/component/TextField.java:52`). Sliders use “Value” (`setting/Slider.java:20`), NumericField uses “Block coordinate” (:14), and StringControl uses only the description (:16). When slider editing/invalid text overlays its label (:78–80), the visible setting identity also disappears. In a long keyboard form, “Value, text, bad, error...” does not say which setting is being edited. Audible narration remains unverified, but the supplied label strings are unambiguous evidence of missing context.

At font2, slider pointer editing still uses a fixed y+12 label target (:117), whereas drawn label text is about15.8 units high. A click on the bottom of the visible label starts a track edit instead. This is a font-aware hit-target omission despite the improved row allocation.

**Action:** Separate accessible name from placeholder; include setting name, units/range/description and current error. Keep the label visible during editing. Derive label/edit hit bounds from actual font metrics.

**Acceptance:** The collected keyboard label names Rotation Speed/X/etc. during edit and invalid states. At font2, clicks across the complete label enter editing without changing the value. Listen to narration on a machine with working speech before claiming audible success.

### R7 — P3: Finish inspectable choices and explicit details affordances

**Static.** Cosmetics category and pose still cycle unseen alternatives (`CosmeticsEditor.java:46,54`). Sounds/Discord popup rows mix toggle with a narrow16-unit details edge/right-click (`ClientSettingsPopup.java:92–104`); keyboard Right opens details (:113), but pointer discovery still depends on interpreting a value tail. Main module safe inspection now sets a better standard.

Block picker0014 shows only technical IDs, with 100 pages in this registry. `BlockPickerScreen.java:39–44` provides counts/pages and search, but no selected-only view, ordinary translated block names or icons. Managing an existing selection spread across the registry requires remembering IDs or searching repeatedly. This is a usability limitation, not failure of paging.

**Action:** Use inspectable cosmetic choice lists, separate popup switch/details targets and explicit chevrons. Show localized block names with IDs secondary, plus a selected-only filter. Preserve unknown stored IDs and immediate-save semantics clearly.

**Acceptance:** A newcomer can inspect cosmetic alternatives before applying one, open Sounds/Discord settings without guessing a secondary action, and review/remove all selected blocks without hunting through the full registry.

## Coverage limits and next acceptance pass

Reviewed main module/search/sidebar, Appearance/client forms, shared validation/focus/metrics, Resources and cosmetics, HUD, Glass, choice/block pickers, waypoint edit and sampled map/list/native gear routing. Retained earlier verified dirty guards, waypoint draft/cancel and deletion confirmation assessments; no fresh full map creation/edit/delete, resource failure/retry, Profiles/Friends traversal, real container or cosmetic acquisition journey is claimed.

Implement R1/R2 together, then R4/R5/R6, then R3; R7 is polish after correctness. Keep the current91 assertions but add targeted journeys rather than more bounds-only counts. Capture settled auxiliary font2 forms, invalid slider recovery, position synchronization and mixed-query reveal. Resize an actual native client window/GUI scale for the supported minimum viewport separately from Screen.init fixtures. Preserve container lifecycle and renderer checks as explicit unverified gates until executed. Motion verification should next cover focus availability during slow navigation/expansion, keyboard interaction while reversing, and Reduced Motion in auxiliary contexts; record time-series/video for temporal judgment and measure frame times separately if a performance claim is needed.
