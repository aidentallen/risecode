# Independent UX/UI review — round 5

Reviewed 2026-11-09 at **06cfb27ed7afeb1ddd77b86be11ef26fe57ff7bb**, branch **feat/ux-motion-review**. HEAD/branch matched; checkout was clean. Read AGENTS.md, AGENTS.local.md, round-{1,2,3,4}-sol.md and implementation-log.md. This reviewer ran no Gradle/client, installed/published nothing, changed no checkout files and used no agents. Only this external report was written. Explicit read-only scope supersedes standing installation/publication authorization.

## Evidence and fixed rubric

Unchanged rubric: **0** absent; **2** usually blocks the task; **4** partial implementation requiring significant discovery/workarounds; **6** usable for a familiar user with material gaps; **8** clear, consistent support for common tasks and edge cases; **10** excellent support demonstrated across ordinary users, screen sizes, inputs, and failure states. Scores are candid implementation/evidence assessments, not accessibility certification or user-study results.

Verified all **799 source/resource/test hashes** and **31 evidence hashes** (29 PNGs + two logs) in `/tmp/riserecode-ux-round4/final/manifest.json`: zero mismatches. `validatedSourceCommit` matches this checkpoint. This is a source/runtime receipt, **not release JAR provenance**; build preceded commit. `native.log:162–167` records 19 journey + 65 layout/keyboard + 58 auxiliary + 14 actual-viewport + 7 temporal assertions = **163 assertions, not 163 distinct journeys**. `check-build-viewport.log:287,289,442,448` records check/build, standalone viewport pass and BUILD SUCCESSFUL. These are preserved parent executions, not this reviewer's runs.

Read all five Ux*IntegrationTest sources. Actual viewport test calls `Input.resizeWindow(960,720)`, GUI scale 3, and asserts a live **320×240** screen. It captures font2 Appearance, settled choices and backward HUD text-focus reveal. The older 320×180 `Screen.init` fixtures remain separate. Auxiliary focus helper actually presses Tab into HUD/Glass/cosmetic controls; track/arrow recovery, HUD nudge, character/wheel and label clicks also use direct native method calls. These are useful bounded assertions, not complete physical-input journeys. No expanded color-picker editing journey is present: reaching `BackgroundColor, color` reaches its collapsed header.

Inspected these **12 relevant PNGs with view_image**: 0004, 0013, 0016–0025. Screenshot observations below concern those pictured states only. 0013 is an early fixture capture with faint buttons; 0024 is a settled actual-small-window choice capture. 0016 visibly reveals Rotation Speed with its breadcrumb; 0017 has no collapsed-body leak; 0018/0019/0020 show separated ordinary HUD/Glass/cosmetic rows and deep focus; 0022 shows Stone plus a retained unknown ID in Selected; 0025 shows the complete focused Position X row in the actual small viewport.

**Static** means source-derived, **screenshot-observed** means visible in supplied pixels, **runtime assertion** means explicitly checked by supplied tests, **unverified** means no adequate execution evidence. No audible narration (flite unavailable), contrast compliance, frame pacing/FPS/allocations, real container lifetime, multiplayer or optional-renderer matrix is claimed. Motion assertions cover particular opening/reversal/settling and Reduced Motion paths, not universal temporal quality. The pictured FPS counter is not a benchmark.

A quick memory registry search found no relevant UX entry; no memory-derived facts informed findings or scores.

## Category scores

Equal-weight average **7.3/10**. Improvements earn credit where the actual interaction is supported; untouched embedded controls and resource traversal keep several categories below 8.

| Original category | /10 | Current assessment |
|---|---:|---|
| Navigation and orientation | 8 | Named Settings, safe module inspection, caller returns and scoped/breadcrumb search form a coherent common workflow. Resource subviews still need logical focus continuity. |
| Layout and responsiveness | 7 | Ordinary auxiliary rows and actual minimum viewport evidence improve confidence. Expanded color internals and whole-control pointer clipping remain material gaps. |
| Hierarchy and readability | 7 | Persistent named fields and separated ordinary rows help. Expanded color sliders retain obsolete geometry; raw floating point strings and narrow abbreviation reduce clarity. |
| Motion and transitions | 7 | Reversal/settling and selected Reduced Motion behavior have bounded temporal evidence. Slow navigation/expansion input availability and frame pacing remain unverified. |
| Feedback and recovery | 7 | Numeric recovery, draft guards, waypoint-specific errors and Apply reasons improve ordinary tasks. Color hex validation still silently disappears on commit/blur. |
| Keyboard and accessibility | 7 | Actual auxiliary Tab and backward owning-row reveal are substantial progress. Resources retains viewport-only collection; color subfields remain poorly contextualized. Audio is unverified. |
| Discoverability and ordinary-user usability | 8 | Inspectable cosmetics, consistent Sounds/Discord detail pages, translated block names and Selected-only review address concrete discovery costs. Resource details retain a small right-click hint. |
| Consistency | 7 | Shared ordinary-control metrics and ownership improve consistency. Color subcontrols and resource traversal still follow older rules. |
| Settings workflow | 8 | Combined query reveal/restoration, coherent slider recovery and synchronized HUD positions make common settings tasks substantially better. Expanded color editing remains an exception requiring repair. |
| Perceived performance and implementation economy | 7 | Central scheduling/cached filtering remain sensible. Per-render filtering and offscreen extraction during keyboard traversal are still costs, with no measured latency/allocation result to justify stronger claims. |

## Reassessment of previous findings

Paths here and below are relative to `src/main/java/dev/riserecode/` unless stated otherwise.

- **Round 4 R1: ordinary auxiliary metrics and collapsed HUD body resolved.** HUD uses SettingMetrics at `ui/hud/HudEditorScreen.java:114,196`; Glass at `ui/component/GlassEditorScreen.java:66,93`; cosmetics at `ui/component/CosmeticsEditor.java:113–118`. HUD body rendering is conditional at :111 and body keyboard registration disappears when collapsed. Screenshot0017 and the auxiliary assertion support that fix. Do not recycle the leaked Scale label or old ordinary enum stride. Expanded ColorPicker internals are a distinct missed nested owner (F1).
- **R2: logical auxiliary reveal substantially resolved.** HUD :112, Glass :80, cosmetics :116,126 install scroll owners. Tests actually Tab to deep controls and actual-small HUD backward focus checks a whole owning row. Screenshot0025 confirms that particular row. This does not prove every child/expanded state; F2/F3 identify specific remaining paths rather than repeating the repaired general claim.
- **R3: combined search resolved for the tested ordinary case.** `ui/component/ModuleButton.java:91–107` separates module identity words, identifies a target and renders a breadcrumb; `Panel.java:61–63` restores prior disclosure on clearing. Auxiliary test checks Kill Aura + Rotation Speed reveal, unchanged enabled state and restoration; screenshot0016 supports it. Hidden/custom-editor matches are explicitly identified. Localized/dependent/custom-editor end-to-end journeys remain unverified, not assumed broken.
- **R4: invalid slider track/arrow recovery resolved.** `setting/Slider.java:31–34,110–124` replaces draft/baseline/error coherently. Auxiliary assertions check both replacement routes and Cancel baseline. No stale-invalid-slider finding is carried forward.
- **R5: HUD position copy staleness resolved in source.** HUD :149,176,194 synchronizes sliders after moves and each render at :95; Slider :31 synchronizes model/draft. Tests cover both axes after coarse nudge and continued X adjustment. Actual pointer drag and concurrent active-draft/external change policy are not comprehensively exercised.
- **R6: ordinary numeric/string identity and geometry resolved.** Slider :49 and :79–85 provide named text context and separate label/value/error/track, :120 uses font-aware label hit bounds. TextField :59 uses owning-row focus bounds. Supplied label-click and backward-focus assertions support those paths. Color internals remain separate residual scope (F1/F4/F5).
- **R7: resolved in the requested scope.** Cosmetics :47,50,58 use inspectable choices; ChoiceScreen centrally retains ClickGUI draft ownership (:26–29,46–47); popup Sounds/Discord open details (`ClientSettingsPopup.java:92,94`). BlockPicker :31–47 provides translated names, secondary IDs, Selected-only and unknown IDs. 0021/0022 show those changes; cosmetic pose cancellation preserves the draft in assertions. Full acquisition/category/choose/apply journeys remain unverified.
- Earlier **safe module inspection, dirty exit guards, new-waypoint draft cancellation, waypoint delete confirmation, named Settings, client scoped search, Appearance no-results, narrow Resource toolbar, transient gear suppression and HUD Done/caller return** remain resolved within prior/source evidence. No revival of the original container-disposal defect: `ui/menu/MenuTheme.java:74–75` suppresses transient picker gears. Real container lifetime still needs execution evidence.
- Earlier claim that HUD selection was pointer-only remains retracted. Native keyboard selection is present and tested.

## Prioritized findings

No new P1 is established. F1–F4 are **P2**; F5–F7 are **P3**. Findings are static unless a screenshot is explicitly identified. None was dynamically reproduced by this reviewer.

### F1 — Expanded color picker still allocates the old compact slider geometry

**Regression from the new shared Slider geometry.** `ui/component/setting/ColorPicker.java:25,66,133–136` fixes total expanded height at310 and gives Speed/Spacing/RGBA sliders **22 units**, with 22/23-unit strides. Slider now draws label + a separate value field + error allocation + track (`setting/Slider.java:79–88`). `SettingMetrics.java:9–11,19` requires **56.22 units at font1** and **74.44 at font2**. At font1 the Speed field starts y+182.92 and ends y+202.92 while Spacing's label starts y+194; it also draws below its own 22-unit bounds. The track at local y+16 falls into label/value geometry. RGBA rows likewise collide, and later fields extend past the picker's fixed scissor bottom. Fixing only the parent row height cannot fix these nested offsets.

**Impact:** Ordinary color customization across Appearance, modules, HUD, cosmetics and Glass can show overlapping labels/fields and misleading click targets. Keyboard context bounds also incorrectly claim each nested slider is only22 high. Current tests/captures show collapsed color headers, so their passes do not refute this path.

**Action:** Lay out color children through measured preferred heights, derive total expansion height from that layout, and use the same child geometry for rendering, clipping, pointer routing and focus reveal. Revisit endpoint/mode/hex label spacing at font2 as part of this nested layout.

**Acceptance:** At font .75/1/2 expand a color and edit Speed, Spacing, Alpha, Red, Green and Blue by pointer and keyboard. No child intersections or clipped fields; track edits work independently of text editing; final Blue row is reachable. Capture settled expanded states, not only headers, in Appearance and at least one auxiliary editor.

### F2 — Auxiliary editors reject clicks on every partially clipped control

`ui/hud/HudEditorScreen.java:127`, `ui/component/GlassEditorScreen.java:150`, and `ui/component/CosmeticsEditor.java:180` require the **entire component bounds** to fit the viewport before calling mouseClicked. A fully expanded ColorPicker is310 units high. At actual320×240 font2, HUD body viewport is approximately109 units (HUD :96–103); Glass material panel is116 (Glass :72–76). No scroll offset can make a310-unit control fully fit. Cosmetic Edit has the same issue whenever its body is shorter than310, as in0020. After expansion the visible color header, HSV surface and hex input can all stop accepting clicks, including the collapse header. Partially visible ordinary sliders are also inert despite showing clickable content.

**Action:** Gate pointer coordinates against the viewport and actual revealed section clip, then dispatch to intersecting controls; allow the visible portion of a large component to operate. Maintain capture/release beyond the clip for an ongoing drag. Do not require a complete component to fit.

**Acceptance:** In actual320×240/font2, expand HUD/Glass/cosmetic colors, scroll through them, change a visible channel/hex and collapse using the visible header. A visible partial slider label/track responds only within the clip. Hidden/retracted section content never responds. Repeat once F1 increases the correct expanded height.

### F3 — Resources still cannot Tab through its complete catalog/Selected/category views

`ui/component/ResourcesEditor.java:157–190` has no logical scroll owner. Categories are rendered only when intersecting the viewport (:168); catalog cards are culled (:175); Selected rows are culled (:216). `KeyboardNavigation.java:46–47` rejects clipped controls without a scroll region and :59–62 wraps within collected entries. Tab therefore cycles around the currently visible resource controls instead of revealing the next catalog/selected/category item. PageDown/wheel are manual alternatives, not continuous logical traversal. Tools sets maxScroll=0 (:160) even if its expanded104-unit body exceeds the short available region.

Additionally, Selected action instances are recreated by rebuild (:127–131), including after reorder; focus identity needs deliberate retention across that rebuild. This report does not claim a runtime reorder failure; the source does not provide stable action identity there.

**Action:** Apply the logical scroll-owner pattern to each Resources subview, retain stable entry/action IDs across selection/reorder, and give short Tools bodies real scrolling/reflow. Register logical controls beyond the clip while keyboard traversal is active.

**Acceptance:** With a long catalog and Selected list, Tab and Shift+Tab reach last items without manual scrolling. Open details, return, reorder/remove a selected item and continue from a predictable visible neighbor. At minimum viewport all Tools actions and cache/Apply recovery remain keyboard reachable. Preserve draft/order semantics.

### F4 — Invalid color hex silently disappears on focus commit

`ui/component/setting/ColorPicker.java:55–57` installs a hex onCommit that unconditionally replaces the draft with current saved hex and clears validation. `FocusedInput.java:23,29–30` commits on another field/pointer focus and KeyboardNavigation :60 commits on Tab. Thus enter an invalid hex, then Tab/click elsewhere: the attempted value and error disappear without an explicit cancel or explanation. Partial input can already have previewed a different valid color before the final invalid suffix. This is a remaining color-specific recovery gap, not the repaired Slider/String/Numeric blur bug. Hex remains generically named AARRGGBB (:29).

**Action:** Preserve invalid hex/error until corrected or explicitly canceled; retain the last valid model; make Escape restore the edit baseline. Give hex a contextual accessible name and reserved persistent error geometry. Resolve valid channel/HSV replacement of invalid hex coherently, following Slider's replacement policy.

**Acceptance:** Invalid hex → Tab/click away → return retains draft/error and identifies the owning color; Escape restores baseline with no stale error. A deliberate channel/HSV replacement clears invalid text and sets an unambiguous new Cancel baseline. Check both start/end endpoints.

### F5 — Color subfield names still lose owning-setting context

ColorPicker constructs child Slider settings as Speed, Spacing, Alpha, Red, Green, Blue (`setting/ColorPicker.java:29–35,52–53`). Their new generic Slider accessibility uses those names, so a form with many expanded colors narrates identical “Red, text…”/“Speed, slider…” entries without Background Color/Primary Color or start/end endpoint. Header/endpoint labels include setting context (:86–87,101–103); nested numeric and hex fields do not. Audible speech is unverified, but the supplied strings establish the identity gap.

**Action/acceptance:** Supply accessible names distinct from visible short labels, including owning color, endpoint and channel/animation property. Collect labels for two expanded colors and prove each child has unique useful context during valid/invalid editing; verify audio separately on a working narrator host.

### F6 — Idle numeric fields expose raw floating point tails instead of readable values

Screenshot0018 and actual-small0025 visibly display **0.0157421875** and **0.02336111111111111** for normalized HUD positions. Slider still computes a formatted cachedValue at `setting/Slider.java:73–74`, but no longer renders it: :82 writes Double.toString into the always-visible field, including after synchronizeExternal (:31–32). A tiny nudge can therefore dominate the form with irrelevant decimal precision; narrow fields scroll toward the caret and can hide the leading value.

**Action:** Use a concise, step/range-aware idle display while retaining exact precision on explicit edit and preserving the underlying value. Consider explaining normalized coordinates or offering pixel coordinates in HUD.

**Acceptance:** Drag/nudge HUD and inspect fields at minimum width/font2: meaningful values fit without long binary tails; activating precise edit reveals the full value and committing without changes does not quantize it.

### F7 — Resource typography remains fixed despite font scaling

Resource detail lines use fixed12-unit stride (`ui/component/ResourcesEditor.java:233`) although .72×font2 text is about12.96 units high before spacing; maxScroll also assumes12 (:147,234). Selected rows use fixed46 stride/42 high and17-high action slots (:210,214–223). Catalog metadata is .54 scale (:184), visibly tiny in0004. This is a separate untouched surface from the resolved ordinary module/auxiliary controls. No supplied font2 expanded Resource detail/Selected capture establishes legibility.

**Action:** Measure detail line heights, selected row/action heights and footer layout from current font metrics; use a readable secondary scale and full-name focus/hover access. Keep the narrow toolbar wrapping fix.

**Acceptance:** At font2 and minimum native viewport, long resource description/conflict text has visible line separation, scroll reaches the last line, Selected actions fit their labels, and cache/Apply explanations remain readable. Include loading/failure/paused/ready states; do not equate bounds-only assertions with legibility.

## Coverage and next implementation order

Reviewed main ClickGUI/sidebar/search/module controls, Appearance/page appearance and native gear policy, shared text/slider/color/choice/block controls, client Sounds/Discord/Scale/Performance routing, Resources, cosmetics, HUD, Glass, Profiles/Friends actions, and map/waypoint manager/editor navigation/layout. Broad source coverage is not successful live operation of every surface.

Additional lower-confidence/remaining verification areas: WaypointScreen still uses fixed54-unit rows and a six-column toolbar (`map/WaypointScreen.java:47–54`); long font2 labels and many-waypoint native captures are missing. World map teaches pointer controls more clearly than keyboard pan/zoom. Friends removes on row activation (`ClickGui.java:409–410`) with no undo; accidental removal recovery remains weaker than waypoint/profile deletion. These are ordinary usability polish opportunities; no new destructive data-loss severity is inferred. Cosmetic no-result/acquisition failure and import-path flows, profile loading/failure, native theme overlays over real gameplay imagery and all-editor localization/contrast deserve dedicated captures.

Implement **F1 + F2 first**, then **F3/F4**, then contextual names/number presentation/resource typography. Keep the resolved findings resolved. Extend journeys through expanded nested controls and resource subviews, with settled actual-small-window captures. Retain existing163 assertions but report additional tests by actual covered behavior, not inflated journey counts.

Motion follow-up should exercise actual keyboard/pointer interaction during slow navigation/expansion/reversal and Reduced Motion in auxiliary editors; opacity<.95 currently excludes focus registration (`KeyboardNavigation.java:42`). This is a concrete source policy worth checking, not a newly reproduced smoothness defect. Use time-series/video for temporal judgment and separate frame-time/allocation measurements for performance claims. Real chest/container remote close, abandoned child ownership, optional renderer paths and audible narration remain explicit unverified gates.
