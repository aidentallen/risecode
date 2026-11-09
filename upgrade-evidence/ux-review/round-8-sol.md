# Independent UX/UI review — round 8

Reviewed 2026-11-09 at **14d047f56ea3e3bd39bd77d0ee4ead8222ecf6ea**, branch **feat/ux-motion-review**. HEAD and branch matched; checkout clean before and after inspection. Read AGENTS.md, AGENTS.local.md, all seven preceding Sol reports and implementation-log.md. This reviewer made no checkout edits, ran no builds/client/tests, installed/published nothing and used no agents. Only this external report was written. The explicit read-only assignment supersedes standing installation/publication authorization.

## Evidence and unchanged rubric

Independently SHA-256 checked **807 source entries against Git objects at the requested checkpoint** and **56 evidence entries against preserved files** in `/tmp/riserecode-ux-round7/final/manifest.json`: **zero mismatches**. Evidence comprises **55 PNGs plus native-check-build.log**. `validatedSourceCommit` matches the requested SHA; `baseCommit` names the preceding checkpoint. This is source/runtime correspondence, not release artifact provenance, installed state or independent execution.

The supplied log records `:check` at287, `:build` at289, suite results at442–450 and BUILD SUCCESSFUL at456. Counts are **19+65+58+14+7+52+18+38 = 271 assertions**, not271 journeys. Narrator initialization reports missing flite at389–417. Neither that run nor this review establishes audible output.

Read all eight `src/gametest/java/dev/riserecode/Ux*IntegrationTest.java` sources:

| Suite | Assertions | Demonstrated scope and boundary |
|---|---:|---|
| Journey | 19 | Separate disclosure/toggle, empty/scoped search, dirty-leave cancellation, choice cancellation, HUD return and map/list Done. Many actions invoke methods directly. |
| Layout | 65 | Invalid numeric drafts, large-type row geometry, physical Tab into a deep module setting, client search, block paging and HUD selection/direct nudge. 320×180 choice/waypoint `Screen.init` cases are fixtures rather than native resizes. |
| Auxiliary | 58 | Combined-query reveal/restoration without enabling gameplay, numeric replacement, HUD position synchronization/character/wheel routing, physical Tab into auxiliary forms, pose-choice draft retention and Selected blocks. Mixed physical input/direct calls. |
| Viewport | 14 | Actual960×720 native resize, GUIscale3/live320×240, font2 Appearance/choices and backward owning-row focus reveal in HUD. A bounded scene set. |
| Motion | 7 | Sampled opening advancement, immediate reversal continuity, settling and particular Reduced Motion geometry. No continuous input or frame-time measurement. |
| Nested/Resources | 52 | Expanded colors at .75/1/2, visible Blue pointer routing, endpoint invalid drafts/baselines, contextual names, concise/exact numbers, Resources detail/return/reorder/remove/Tools/category/failure. Backward jump to last catalog card is not forward traversal of all641 entries. |
| Recovery | 18 | Actual small/font2 cosmetic search ownership/draft preservation;24 waypoints; two UUID friends; Resource failure help into Cache all. Physical Tab/Enter/Escape plus direct modified keys/setup/confirmation. Waypoint post-delete focus only asserts a nonempty label. |
| Completion | 38 | Actual compact global-editor details, invalid/valid PNG import, zero-result recovery, separate manager/editor/local-provider acquisition start/cancel/fail/retry/select/apply, reserved-header hit/full message, duplicate save cancellation bytes/Save as/Replace/Restore JSON for both profile types, appearance case normalization, HUD H/help. Native Button.onPress(KeyEvent), custom activation and setup bypass portions of physical navigation. |

Completion uses a generated64×32 PNG and deterministic offline catalog/provider fixture. Its acquisition is useful functional evidence, **not live networking or complete physical-input proof**. The production default constructor/provider remains unchanged. The actual global editor supplies details/import coverage; the separate fixture supplies acquisition coverage. The import revision guard is present, but abandonment races are not directly exercised. No child-screen resize draft-preservation or animated-avatar time progression assertion exists. HUD help checks full strings/opening; the pictured help fits, so it does not test multi-page reading.

Used **view_image** on **22 PNGs**:0009,0022,0026–0028,0031–0035,0037,0044–0054. Other files were hash-verified, not visually inspected. In particular0045 now has a clean Resources footer despite prior friend feedback;0046 explains cosmetic no matches and recovery;0047 shows avatar, acquisition/import/return;0048 and0049 show short failure states;0050 shows readable full prose;0051 shows clear Resources actions;0052/0053 show explicit profile decisions;0054 shows complete HUD help. Earlier motion poses and expanded-color/auxiliary views were inspected separately from functional claims.

**Screenshot-observed** means pictured pixels; **static** means a source-derived path, not live reproduction; **supplied assertion** means explicitly checked by the preserved run; **unverified** means insufficient execution evidence. No measured frame-pacing/FPS/allocations, universal localization/contrast, audible narration, real container lifetime, multiplayer or optional-renderer matrix is claimed. The HUD FPS counter is not a benchmark. A lightweight memory-registry search found no relevant UX entry; findings use this checkpoint and supplied evidence.

Fixed rubric: **0** absent; **2** usually blocks the task; **4** partial implementation requiring significant discovery/workarounds; **6** usable for a familiar user with material gaps; **8** clear, consistent support for common tasks and edge cases; **10** excellent support demonstrated across ordinary users, screen sizes, inputs and failure states. Fractional ratings express partial attainment, not measurement precision.

## Original ten categories

Equal-weight average **7.75/10** (rounded **7.8**). Recovery and compact task completion improve; motion/performance confidence does not rise with the assertion count. Remaining new-child limitations prevent treating compact preview as complete parity.

| Original category | /10 | Assessment |
|---|---:|---|
| Navigation and orientation | 8 | Retained child return, named Settings, scoped/breadcrumb search and stable Resource/waypoint identity support common routes. Cosmetic child lacks pose and complete preview control parity; recovery semantics still need clearer wording. |
| Layout and responsiveness | 7.5 | Reserved message space removes real overlap; compact acquisition and readable help now fit the actual small viewport. New child forms reset text on reinitialization; long tokens/status text remain insufficiently bounded. |
| Hierarchy and readability | 8 | Complete HUD help, explicit empty states and named profile decisions improve reading. Native text fields and themed labels differ visibly; some titles/metadata still abbreviate and long errors lose content. |
| Motion and transitions | 7 | Main opening continuity/Reduced Motion have bounded evidence. New full-window avatar freezes its preview clock statically; slow-transition input and frame pacing remain unverified. |
| Feedback and recovery | 8 | Explicit replacement, previous-document recovery, persistent full message access, draft guards and field validation are substantial. Cosmetic status loses long details; Restore previous does not explain its disk-only effect. |
| Keyboard and accessibility | 8 | Logical traversal, owning labels, HUD H/help and Resource failure recovery have meaningful evidence. Cosmetic child has wheel-only zoom, and audio/full physical workflows remain unverified. |
| Discoverability and ordinary-user usability | 8 | Preview/acquire, Clear search/Show catalog, explicit Replace and named Help remove important guessing. PNG/name fields need visible context and preview controls still differ by size. |
| Consistency | 8 | Shared metrics, retained ownership, validation policy and named actions are coherent across principal surfaces. New native forms do not preserve text like the waypoint editor; small/large cosmetic preview capabilities differ. |
| Settings workflow | 8 | Common search/edit/draft/apply and profile replacement/recovery now have useful supplied coverage. Child reinitialization and disk-versus-active restoration remain material edge cases. |
| Perceived performance and implementation economy | 7 | Central scheduling, cached module filtering and offscreen paint skipping remain sensible. Appearance/cosmetics still construct render-time collections. No measured cost or improvement establishes perceived smoothness. |

## Reassessment: retain the closed findings

Paths below are relative to `/home/toast/Work/riserecode/src/main/java/dev/riserecode/`.

- **Round7 F1, missing compact acquisition/import: addressed as a route.** `ui/component/CosmeticsEditor.java:152,157` exposes Preview/acquire; `ui/component/CosmeticDetailsScreen.java:21–41` provides avatar, rotation, acquisition, selection, import and Back.0047–0049 and Completion support the bounded import/acquisition states. Do not repeat the claim that compact users have no download/import route. Preview animation/parity below are distinct remaining gaps.
- **Round7 F2, notice covering/clicking through footer: addressed.** `ui/clickgui/ClickGui.java:53,264,356–363` uses a reserved header control and full retained InfoScreen.0045/0051 show unobstructed status/Apply/Cancel; Completion checks header bounds, pointer hit and full text. The former timed footer overlay is gone. A lone ! conveys less immediate result detail than text, but that does not resurrect overlap.
- **Round7 F3, silent duplicate replacement: addressed.** ClickGui:383–409 detects duplicates and exposes named Replace/Save as/Cancel plus Restore previous. `ui/menu/ProfileSaveScreen.java:17–22` enforces new-only Save as. `config/ProfileManager.java:90–97` and `ui/theme/ThemeManager.java:424–438` preserve one previous document; appearance existence/save/restore share lowercase paths. Completion checks cancellation bytes, separate save, backup and restored JSON equality for both. This is one previous revision, not unlimited history or live-state rollback.
- **Round7 F4, blank cosmetic search: addressed.** CosmeticsEditor:129–136 supplies context and keyboard-scroll actions.0046 visibly names Companions/Installed with Clear search/Show catalog; Completion checks clear plus draft retention. Loading/network permutations remain separate.
- **Round7 F5, elided HUD shortcut set: addressed.** `ui/hud/HudEditorScreen.java:88–93` has Help and short stable guidance; H opens complete text.0054 is readable and includes fine/coarse movement, grid, inspector paging and save/return. This is not a claim that all hints must fit the bottom line.
- **Round6 fixes remain closed in their tested scope:** visible compact Ctrl+F/released hidden fields, focusable Resource Apply reason, UUID waypoint action identity and return, cleaner HUD annotations, full compact Glass preview/reset labels and explicit confirmed Friends removal.0044/0045 and0031–0034 support those pictured states; Recovery provides bounded interaction evidence. Friends' footer hint is stale at ClickGui:457 (“Select a friend to remove them”); update it to describe inspection and explicit Remove, without undoing the safer behavior.
- **Earlier fixes remain supported:** measured nested color geometry/visible pointer routing, endpoint invalid draft and cancellation baselines, contextual channels, concise idle precision, Resource logical traversal/details/Selected reorder/return, safe module inspection versus activation, combined-query reveal/restoration, named Settings, scoped client/Appearance search, HUD synchronization/collapsed-body exclusion, dirty exits, uncommitted waypoint creation, waypoint confirmation, translated/Selected block management and transient picker gear suppression. Do not recycle the older pointer-only HUD selection or secondary-gear disposal claims. Real container closure/abandonment is still an execution gap, not a presumed defect.

## Prioritized actionable findings

No new P1 established. F1–F3 are **P2**; F4–F5 are **P3**. None was reproduced through a live client by this reviewer.

### F1 — P2: Full-window cosmetic preview never advances its animation clock

**Static regression in the new compact route.** `ui/component/CosmeticsEditor.java:104` advances `previewTime` only in the parent's render method. `:163–174` builds the avatar from that clock: ageInTicks, walking phase and animation state at:167. `CosmeticDetailsScreen.java:32–37` calls renderAvatar directly without advancing time; its tick is inherited from `ui/menu/RetainedScreen.java:16`, which only ticks a PageAppearance parent. The retained ClickGUI is not simultaneously rendering behind this replacement screen. `cosmetics/CosmeticsRenderer.java:32–56` derives wing/companion/aura/particle/trail motion and companion model sampling from the resulting age. Thus those time-dependent preview effects freeze while this child remains open, even though rotation/zoom can still change.

**Impact:** A normal user checking an animated cosmetic sees a still pose in the newly supplied compact preview, so Animation speed/Motion strength and walking behavior cannot be assessed there.0047 proves an avatar is drawn, not that it animates. This is a source-level time-progression defect, separate from unmeasured frame pacing.

**Action:** Advance a bounded preview clock through a shared method from whichever screen currently owns the preview; avoid double advancement in the wide pane. Define deliberate preview pause/Reduced Motion behavior and retain phase on return.

**Acceptance:** At actual320×240/font2 with an animated companion and wings, sample avatar age/model poses at separated times while the child remains active; show expected progression for nonzero speed and stable intentional pause/zero speed. Compare wide pane/child, walk/stand and return; add temporal evidence rather than a still-only presence assertion.

### F2 — P2: Child-screen reinitialization discards typed import/name drafts

**Static.** `ui/component/CosmeticDetailsScreen.java:21–29` recreates an empty path EditBox on every init. `ui/menu/ProfileSaveScreen.java:17–20` recreates its name field and resets it to original on every init. Neither snapshots the old text/caret. Native resize/reinitialization therefore loses a partly typed PNG path or new Save as name; profile errors can remain while the attempted name has been replaced. By comparison `map/WaypointEditScreen.java:15–17` explicitly carries field values across initialization.

**Impact:** Resizing to inspect a file path or recover a cramped form unexpectedly erases user work. In Save as the replacement is the existing name, so the next Save as hits the duplicate-name error. This does not overwrite a profile: the duplicate check still protects it.

**Action:** Preserve field text and reasonable caret/selection/focus across init, plus current error context. Restore original only on first open. Give the name field a visible “New profile name” label and allowed-format guidance; its accessible name alone does not provide that visible context in0052/0053.

**Acceptance:** Physically type a different name/path, resize960×720 to another supported size and back, then complete/cancel. Exact text remains, focus is useful, validation refers to that text, and cancellation leaves bytes/draft unchanged. Cover both profile kinds and import-in-progress resizing separately. Current Completion resizes before opening these children, so it does not test this path.

### F3 — P2: Full-message recovery still fails for long tokens and long cosmetic errors

**Static, bounded by short supplied captures.** `ui/menu/ScreenText.java:13` wraps only at whitespace and never splits a word wider than the line. A long absolute PNG path, URL or identifier remains overwide. `ui/menu/InfoScreen.java:19–24` pages vertically only, so the offscreen suffix is unreachable. `CosmeticDetailsScreen.java:31,37` can include an arbitrarily long import exception/path but draws only the first two wrapped lines, with no details action or scroll. `ProfileSaveScreen.java:22` likewise caps errors at two lines.0048 is a short `/missing/cape.png`;0050 is ordinary space-separated prose. Neither validates full real-path/error recovery.

**Impact:** The part identifying the failing file or actual provider error can be outside the viewport or omitted. Users who cannot hear narration have no complete visual recovery route despite the new full-text design.

**Action:** Wrap overlong tokens using measured character/grapheme boundaries while retaining their exact content. Offer a focusable full-status/details view from the cosmetic child, or scroll the complete status. Preserve explicit paragraph/newline structure where supplied; do not merely shrink text until it fits.

**Acceptance:** At320×240/font2 show a long no-space path/URL and a multi-sentence error exceeding two lines. Pointer and keyboard can read every character and final recovery instruction, page to the end, and return with path/draft retained. Test ordinary prose too; existing full-message behavior should remain readable.

### F4 — P3: Compact preview lacks keyboard zoom, pose selection and reset parity

**Static; controls pictured in0047–0049.** `CosmeticDetailsScreen.java:25–26,40–41` provides rotation buttons/Left/Right and unconditional wheel zoom, but no focusable zoom/reset or pose picker. Its instructions say only “Arrows rotate · Wheel zooms”. The wide pane at `CosmeticsEditor.java:178–184` has keyboard rotation/zoom adjustment/reset targets plus the pose choice. Compact rendering suppresses that pane at:110–114; consequently compact users cannot select walking/crouching/flying here, and keyboard-only users cannot zoom. The wheel handler also zooms even over the import field/buttons, rather than being scoped to the avatar.

**Impact/action:** Preview is now reachable but still varies in useful capability by window size. Add named Zoom in/out/reset and an inspectable pose choice, with a reset orientation action; scope wheel zoom to the preview or explain the deliberate global behavior. Keep text editing arrows intact.

**Acceptance:** Without a mouse choose each pose, rotate, zoom in/out, reset, and return to the same outfit/query. Verify pointer wheel over the path versus avatar, and that view-only adjustments never dirty/apply the outfit. Include actual physical Tab/activation rather than calling every button directly.

### F5 — P3: Restore previous changes the saved document without explaining that the active setup stays unchanged

**Static semantic/recovery gap, not failed disk recovery.** `config/ProfileManager.java:97` and `ui/theme/ThemeManager.java:438` write the previous document to disk and delete the backup; neither applies it to the active client/theme. ClickGui:387,393 reports “Previous client profile restored”/“Previous appearance restored” without telling the user to load it. Load is a separate action at:386,392. Completion verifies restored JSON equality but does not assert that active settings match it. With appearance autosave and an active profile name retained after Replace, users can reasonably expect an immediate visible undo and see no change.

**Action:** State the contract explicitly: “Saved preset restored; load [name] to use it”, with a named Load restored action, or provide a deliberate restore-and-load choice. Explain that the recovery is one previous saved document and is consumed, without implying unlimited undo. Do not silently apply gameplay settings merely to fix the wording.

**Acceptance:** Save A, replace with B, restore A for both profile kinds. Assert disk contents, active values and backup availability separately; feedback tells the user exactly which changed. Complete the intended Load route with keyboard and verify active values then match A. Keep existing JSON/cancel checks.

## Motion and performance: concrete next evidence

The early/reversed/settled0026–0028 captures support distinct poses and the Motion suite's sampled continuity, not smoothness. Early faint content is intentional transition evidence. `ui/component/KeyboardNavigation.java:52` excludes opacity<.95 while ClickGUI pointer availability starts earlier (`ui/clickgui/ClickGui.java:474`). ColorPicker also delays expanded-child input (`ui/component/setting/ColorPicker.java:153`). Treat this as an input-availability policy needing timed verification, not a reproduced FPS regression.

A bounded follow-up should record default and1s opening/page/expansion durations, interrupted close/reopen, rapid page changes, typing during navigation, a focused deep slider through collapse/reopen, and Reduced Motion in HUD/Glass/cosmetic children. At100ms intervals attempt Tab/Enter and a known pointer target; log focused identity, accepted action time, displayed pose and return focus. Confirm no unintended background action, stranded focus or unexplained dead period. Separately exercise F1's animated child for several seconds; functional acquisition passes do not cover visual motion.

For smoothness, hold world/view, renderer, resolution, GUI scale and settings constant; warm assets, then capture three30s runs each of baseline and checkpoint for idle menu, long Appearance scroll/expanded colors,641-entry Resources filtering/traversal and animated cosmetic preview. Record frame-time distributions (median/p95/p99, frames over16.7/33.3ms at the applicable target), CPU extraction timing and allocation bytes/rate with GC events. Distinguish cold acquisition from warm rendering and CPU from GPU time; report hardware, refresh/FPS cap and other load. Use60/144Hz where available, reporting unavailable targets honestly. No benchmark execution is requested or performed in this review.

`ui/component/ThemeEditor.java:173,189` still filters/builds row collections during extraction; `CosmeticsEditor.java:129` filters definitions per render. These are concrete potential costs, not proof of a visible slowdown. Existing culling/stable action work remains creditable. Optimize only after identifying measured hot paths; more assertions do not resolve frame pacing.

## Surface coverage and remaining gates

Source review spans main ClickGUI/modules/sidebar/search, Appearance and per-page appearance, client Sounds/Discord/Scale/Performance, shared text/slider/color/choice/block controls, Resources catalog/details/Selected/Tools/status, cosmetic browse/edit/draft/acquisition/import, HUD, Glass, Profiles/Friends, world map/waypoint list/editor and sampled native theme/button/gear integration. Supplied captures sample those surfaces; they do not constitute successful live use of every surface.

Compact rail names remain available through hover/focus; icon-only rendering is an intentional compromise, not the original inaccessible Settings defect. Expanded color screenshots show separated child fields and deep focus; clipped surrounding content alone is not proof of an unreachable focused control. New full child screens have generous layouts at320×240, but native pixel-style EditBox text versus themed surrounding text is visibly inconsistent in0048/0052; assess any typography change with caret/selection geometry intact. The name/path fields need visible identity during entry, not just an initial hint. Update the stale Friends hint noted above.

World map has pan/zoom keys but still teaches pointer controls more clearly (`map/WorldMapScreen.java:66–67,91`). A complete small/font2 exploration→add→invalid/save→edit/delete→caller-return journey remains useful. `ui/menu/NativeButtonLayout.java:17–24` affects broad noncontainer widget geometry; picker captures do not prove every vanilla/multiplayer/options/third-party menu. PageAppearance/RetainedScreen preserve parent ownership and validity checks in source; real container remote-close and abandoned-child disposal need runtime cases rather than inference.

Still unverified: audible narration and live error announcements; long localized text/complete contrast over arbitrary imagery; real network acquisition and full physical-input acquisition/import workflows; async abandonment races; profile failed-save/restore I/O recovery; all Resource cache loading/paused/ready/failure combinations; real containers; live multiplayer; optional renderers; and measured frame pacing/allocations. These are explicitly bounded confidence limits, not invented defects or release provenance.

Recommended next work: fix the frozen child preview and retain form text across resize, then complete long-error access. Finish compact preview parity and precise restore wording. Keep the five round7 findings closed within their demonstrated scope and retain the271 functional assertions while adding these targeted journeys and temporal measurements.
