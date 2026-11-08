# Independent UX/UI review — round 3

Reviewed 2026-11-09 at `b97307a8853764c2d70e59ed873fe17cd7337783`, branch `feat/ux-motion-review`. Checkout clean at inspection. Read AGENTS.md, AGENTS.local.md, both prior Sol reports and implementation-log.md. This review modified no checkout files, ran no Gradle/client process, installed/published nothing, and spawned no agents. Standing installation/publication instructions do not apply to this explicitly read-only assignment.

## Evidence and fixed rubric

Same categories and standards: 0 absent; 2 usually blocks the task; 4 partial implementation requiring significant discovery/workarounds; 6 usable for a familiar user with material gaps; 8 clear, consistent support for common tasks and edge cases; 10 excellent support demonstrated across ordinary users, screen sizes, inputs, and failure states.

Inspected all eight supplied native PNGs using view_image: combat, expanded module, empty module search, Appearance search, narrow Resources, actual choice list, Appearance no-results, German Font Scale 2. These are **observed rendered states** from the parent's run, not interaction performed by this reviewer. `/tmp/riserecode-ux-checkpoint-final.log:287–289,442,448` records check/build, 19 UX checks and BUILD SUCCESSFUL. Read the journey test to bound those checks: ordinary enum open/cancel and map Done are covered; container lifetime, automatic offscreen traversal and minimum-height Font Scale 2 are not. The German capture changes font scale and language at default window geometry; the narrow Resources capture changes width/padding, not window height. Neither is a minimum native viewport matrix.

**Static** below means a concrete source-derived failure path or limitation, not a fresh runtime reproduction. **Unverified** means evidence is insufficient to judge. No screenshot or passing assertion establishes animation smoothness, GPU/CPU performance, contrast compliance, audible narration, user-study results, or optional-renderer compatibility. A lightweight memory search returned no relevant UX entry; the conclusions use current files and captures.

## Scores

Equal-weight average: **6.7/10**. Scores retain material gaps rather than rewarding every implementation change with a point.

| Original category | /10 | Current assessment |
|---|---:|---|
| Navigation and orientation | 7 | Named Settings, separate disclosure, scoped search and visible HUD Done improve orientation. Picker's exposed secondary gear creates a remaining nested ownership risk. |
| Layout and responsiveness | 6 | Narrow Resources wraps successfully. German Font Scale 2 visibly clips Utilities and abbreviates Settings; module control heights remain fixed despite two-line enums and larger type. |
| Hierarchy and readability | 6 | Main card/title hierarchy is coherent. Expanded module is crowded even at default type size; tiny resource hints and clipped large-type navigation weaken ordinary comprehension. |
| Motion and transitions | 7 | Search no longer replays all card entrances; shared channels and Reduced Motion are useful. Several scale/fade/position layers still compose, with no temporal evidence. |
| Feedback and recovery | 7 | Dirty markers/guards, specific waypoint errors, no-results guidance and cache explanation improve recovery. Invalid numeric/string draft text can still disappear on blur or lose its visible error. |
| Keyboard and accessibility | 6 | Many visible controls are reachable, but Tab still wraps in the viewport, HUD cannot select/nudge an element, and block picker cannot traverse the full registry without mouse scrolling/search. Earlier 7 overstated journey completeness. |
| Discoverability and ordinary-user usability | 7 | Settings label, On/Off, +, option list and search recovery are substantial improvements. Click-anywhere module activation, technical choices and cycling cosmetic controls still require learning. |
| Consistency | 7 | Shared control language and explicit live/draft states help. Enum picker, block picker, cosmetics cycling and Settings toggle/details retain differing interaction rules. |
| Settings workflow | 7 | Appearance search now includes display names/descriptions and has an explicit empty state. Global module search still stops at a closed card; Sounds/Scale/client settings are outside that index. |
| Perceived performance and implementation economy | 7 | Cached module filtering, culling and centralized scheduling remain sound. Appearance rebuilds filtered rows/layout work during rendering; no measured performance result warrants a stronger score. |

## Improvements verified within their evidence limits

- Actual captures confirm clear module empty-search and Appearance no-results states. Appearance matching now includes description/display name (`ThemeEditor.java:76–78`), addressing that part of round 2 R3.
- Resource search/category/actions occupy separate rows at narrow width. Footer now explains cache gating and unapplied state (`ResourcesEditor.java:200–205`); previous missing-reason finding is substantially addressed.
- HUD now draws a visible Done (`HudEditorScreen.java:83`) and has pointer handling for it (:109). Caller return is covered by parent's test.
- Waypoint validation identifies Name/X/Y/Z/Dimension/Color and focuses the offending field (`WaypointEditScreen.java:20–35`). Previous broad-error finding is addressed; fitting/wrapping the error remains a concern.
- TextField draws error border/text regardless of focus (`TextField.java:78–81`). That renderer fix does not ensure owning controls preserve invalid text or keep rendering their input.
- Normal choice open/cancel preserves current value and returns to ClickGUI in the completed native test (`UxJourneyIntegrationTest.java:24–27`). The ordinary PageAppearance suspension/resumption path is present in source. Do not carry forward the old unconditional-disposal finding as if unchanged; F1 below is a different remaining route.

## Prioritized defects and acceptance checks

Paths below are relative to `src/main/java/dev/riserecode/` unless explicitly stated.

### F1 — P1: Secondary appearance gear can dispose the container behind an open choice picker

**Static, newly identified path.** `ui/menu/MenuTheme.java:74` permits PageGear on ChoiceScreen. `PageGear.java:13` opens a new PageAppearanceScreen with the picker as owner. Replacing the picker invokes `ChoiceScreen.java:40`; because `returning` is false, it resumes and calls `removed()` on the original PageAppearanceScreen. That original screen still owns `suspendedContainer`, so `PageAppearanceScreen.java:91` clears it and invokes the underlying container's `removed()`. The new editor treats ChoiceScreen as its direct parent; its validity tick (:89) only checks direct AbstractContainerScreen parents. Returning through the new editor can therefore lead back to a picker whose original container has already been disposed.

This is particularly important because the choice screenshot visibly exposes the upper-right gear. The 19 checks exercise ClickGUI picker cancel, not chest/inventory → appearance → picker → gear.

**Action:** Suppress appearance customization on transient pickers, or implement explicit suspension/ownership through the complete nested chain. Apply the same policy to other transient screens rather than relying only on class-name exclusions.

**Acceptance:** With a real container open, enter page appearance, open an enum, click its gear if supported, change/cancel/back through every layer, and return to the same valid container. Verify no unintended close packet/disposal, preserved setting/scroll and correct remote-close handling at every layer. Also test title-screen picker gear return. Native verification is still required.

### F2 — P2: Tab order still cannot continue into offscreen content

**Static, persists.** `ui/component/KeyboardNavigation.java:36–41` drops clipped controls; :52–55 wraps only over collected entries. `Panel.java:95–97` also culls entire offscreen cards. ThemeEditor has a targeted `pendingReveal` mechanism (:207), but generic focus traversal never calls it. Manual Page Down/Home/End are useful alternatives, not automatic traversal. Sidebar's reveal is tied to selected page (`Sidebar.java:57–59`), not the next focused item.

**Action:** Represent logical focus independently of rendered visibility, attach a reveal operation to each scroll owner, then move focus and scroll together. Preserve logical focus identity across filtering and opening/canceling pickers.

**Acceptance:** Using only Tab/Shift+Tab from the top of a long Appearance form, expanded Kill Aura, Friends/Profiles list and expanded Utilities, reach every enabled control and return backward without manual scrolling or focus loss. The focus ring must stay visible after smooth scrolling and resizing. Test partially clipped controls too.

### F3 — P2: Two-line enum rendering exceeds module row height; large fonts amplify collisions

**Observed crowding + static geometry defect.** Expanded-module PNG has nearly touching label/value sequences (Rotate/Rotation Timing and Targets/Entity Types). `ui/component/SettingControls.java:33` still allocates **15** units to EnumSelector. Its renderer now lays out **18 × Font Scale × .85** units (`setting/EnumSelector.java:50–53`): 15.3 at Font Scale 1 and **30.6** at Font Scale 2. `ModuleButton.java:109–110` simply returns the adapter's fixed height for non-color controls, so module layout reserves half the required height at font2. Booleans (15), sliders (24), and StringControl's label offset (10) also retain fixed module metrics. Appearance's font-dependent rows do not fix module rows.

**Action:** Give controls a font-aware preferred height used consistently for body measurement, drawing, clipping and hit testing. Include label/value line spacing, validation and group spacing. Avoid shrinking type back down to compensate for a user's readability preference.

**Acceptance:** Capture an unfiltered expanded module at Font Scale .75/1/2, long German labels, default and 360×240 logical geometry. No label/value/next-control intersection; click and focus rectangles align with what is drawn. Test stacked enums and strings rather than only Reduced Motion search.

### F4 — P2: Sidebar and auxiliary forms do not fully adapt to large type/minimum height

**Observed:** German font2 PNG visibly crops Utilities at the bottom of the sidebar and renders Settings as `Einstellu...`; most settings UI remains English, so this is a mixed-language capture, not complete localization coverage. Sidebar row height is fixed 20.75 (`Sidebar.java:18`); label scale calculation (:96) does not consistently reserve the user's enlarged text height. Host allocates a fixed 107-wide sidebar and 87-wide footer (`ClickGui.java:247–248`). Utilities uses its own fixed placement/text scale (:99–105). Whole-window fit still shrinks the entire UI (`ClickGui.java:229–230`).

**Static separate auxiliary issue:** `ChoiceScreen.java:29–34` fixes top to at least 28 and footer to top+178 with 20px height: bottom **226**, overflowing a 180px logical viewport. WaypointEditScreen similarly bottoms at **228** (:17–18), and its long error is a single unwrapped line (:38). Neither form scrolls/reflows. No supplied capture establishes behavior at this size.

**Action:** Reflow navigation/footer and derive line/row heights from actual rendered text. Use responsive available-height layouts/scrolling for native forms and wrap persistent errors. Define and test a supported minimum viewport explicitly.

**Acceptance:** At the supported minimum native size and high Minecraft GUI scale, choice Cancel/Next and waypoint Save/Cancel/error remain visible and reachable. At font2, all navigation labels or an intentional readable compact alternative are discoverable; no clipped Utilities. Repeat at width360/height240/padding20 and font2 together, not in separate tests.

### F5 — P2: Invalid edits still silently revert or lose visible validation when focus moves

**Static.** `setting/StringControl.java:17` retains invalid text only while focused; :25 overwrites it with saved setting value on blur, without clearing the old validation. A user can see valid reverted text with an obsolete error. `setting/Slider.java:51` replaces invalid text with the current value on commit; its input/error is rendered only while focused (:78–80), so error persistence in TextField does not make slider errors persist. NumericField commit explicitly resets value and clears validation (`NumericField.java:27`). `FocusedInput.java:29–30` and keyboard traversal commit on focus movement. Valid model preservation is good; silently replacing the draft does not explain recovery.

**Action:** Define a shared invalid-edit policy: retain draft/error until correction or explicit cancel, or announce an explicit revert. Render validation beside its control after blur, and clear errors when a value is genuinely corrected/reverted. Keep error text within a reserved layout area; TextField's .55 line at y+height−5 is not a general error layout.

**Acceptance:** Enter invalid/out-of-range text in slider, coordinate and validated string; then Tab/click elsewhere and return. User can identify exactly what happened and recover without retyping a lost draft. Valid reverted values must never retain stale errors. Escape restores the original with matching error state.

### F6 — P2: Search still finds containers rather than the user's intended setting

**Static, partially resolved since round 2.** `Panel.java:63–65` indexes raw setting names but not localized setting names/descriptions; returned cards remain at their previous expansion state. Appearance matching is better, but constructor `ThemeEditor.java:99–100` excludes Sounds/Interface Scale. Client settings are built separately (`ClientSettingsEditor.java:25–31`). Ctrl+F on those pages redirects to module Search (`ClickGui.java:555`). The no-results hint now explains the split, which helps recovery but does not make settings searchable.

**Action:** Provide a unified settings index or clearly scoped search on each client settings page. Search results should identify page/module/group, disclose and reveal the matched control, and preserve the module's enabled state. Include localized labels, descriptions and ordinary aliases.

**Acceptance:** Find click volume, GUI/interface scale, reduced motion and a deep module setting from the visible search affordance; reach the actual control without enabling its module or knowing its group. Repeat with German display names. Closing/canceling a choice returns to that result/focus.

### F7 — P2: Keyboard editing still stops at HUD element selection and full block-list browsing

**Static.** HUD element selection is inside pointer hit testing (`HudEditorScreen.java:118–124`); keys (:160–165) do not select or nudge elements. Visible Done fixes exit, not editing. BlockPicker registers visible rows but only handles Backspace locally (`setting/BlockPickerScreen.java:68–70`); registry scrolling is mouse-only (:62–63), with no PageScrollable implementation. Its search is an ad hoc always-active query rather than a field with caret, selection, paste, clear and stable empty-state feedback.

**Action:** Register HUD elements with selection and nudge controls, including coarse/fine steps. Reuse a real searchable list model for block picking with keyboard reveal and visible Done. Teach the available shortcuts in context.

**Acceptance:** Select, move and save a HUD element with keyboard only; browse past the first screen of blocks without narrowing the query or touching the mouse; paste/edit/clear a block query and recover from no matches.

### F8 — P3: Finish shared action semantics and contextual explanations

**Observed/static.** Combat capture has a plus and On/Off, but the whole module name/body still toggles gameplay; it looks like an inspectable card. Hover help explains this (`ModuleButton.java:165`) but a first-time pointer action can precede the tooltip. Settings Sounds/Discord mix row-toggle with edge/right-click details (`ClientSettingsPopup.java:92–104`). Cosmetics category and pose still cycle through unseen alternatives (`CosmeticsEditor.java:46,54`). Slider/enum explanations show only on hover (`Slider.java:74`, `EnumSelector.java:54`); their keyboard narration lacks the full description.

**Action:** Prefer an explicit switch target and card-body inspect behavior; standardize visible details chevrons and inspectable choice lists across cosmetic/settings surfaces. Show the same concise descriptions/ranges on focus as hover.

**Acceptance:** A newcomer can inspect module settings without changing gameplay, inspect cosmetic alternatives before choosing, and reach Sounds/Discord details without guessing a secondary click. A keyboard user receives the same explanation as a pointer user.

## Motion and performance assessment

No proven animation regression from the still captures. Search entrance replay is resolved in `Panel.java:56–59`. Composition remains a **static tuning/verification concern**, not a measured smoothness defect: host canvas/open scale and navigation fade/shift (`ClickGui.java:292–309`), panel navigation displacement (`Panel.java:93`), card entry multiplied by press scale (`ModuleButton.java:126–128`), and expansion/body/row position animations (:92–105,179) can run together. Focus registration is unavailable below .95 opacity (`KeyboardNavigation.java:36`), so slow transitions also delay keyboard access.

Record opening, switching, rapid filtering, expanding/collapsing, dragging and cancel/return with default/slow/elastic motion and Reduced Motion at 60/144 Hz. Check displacement, overshoot, keyboard availability, pointer alignment and interrupted reversals. Measure frame times/allocations separately if performance claims are wanted. Appearance still rebuilds filtered lists and rows each render (`ThemeEditor.java:162,172,188`); this is implementation cost, not proof of user-visible slowness.

## Coverage and next iteration

Reviewed current main/module/sidebar/search and shared control paths; Appearance/page appearance/client Sounds/Scale/Discord/Performance; Resources/cosmetics drafts, toolbar/footer and choice semantics; HUD; Glass editor scrolling/preview input; block picker; map/waypoint native form/return paths; Profiles/Friends action routing; sampled native screen/mixin/page-gear integration. This is broad source coverage, not successful end-to-end operation of every surface. No fresh captures were supplied for Glass/HUD editing, invalid forms, profiles/friends, resource failure states or actual containers.

Recommended implementation order: F1 ownership first; F3/F4 shared layout metrics next; F2/F7 complete logical keyboard journeys; F5 validation; F6 search; F8 semantic consistency. Keep the existing 19 checks, add actual journeys for those failure paths, and keep screenshots, functional assertions and temporal/performance evidence separate. Do not increase scores merely because a check exists: the capture/test must demonstrate the usability property being claimed.
