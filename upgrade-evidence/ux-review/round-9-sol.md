# Independent read-only UX/UI review 9

Checkpoint: **48f296e48bde33c719359cc92f53a84b96416564**, branch **feat/ux-motion-review**. Reviewed 2026-11-09. HEAD matched; checkout initially clean. Read AGENTS.md and AGENTS.local.md. The explicit read-only assignment takes precedence over standing installation/publication authorization. No builds, clients, installations, publication, delegation or checkout edits were performed. This report and temporary screenshot contact sheets were written outside the checkout.

## Judgment and fixed rubric

**Equal-weight average: 7.90/10.** The five round 8 findings are addressed in their demonstrated scope. The candidate is a usable, substantially coherent interface, with meaningful compact-window, editing, recovery and temporal improvements. It has not demonstrated excellent support across ordinary users, input combinations, native/container surfaces and failure states. Remaining defects below are independent findings, not reasons to reopen the five resolved findings.

Original scale retained: **0** absent; **2** usually blocks tasks; **4** partial with substantial discovery/workarounds; **6** usable for familiar users with material gaps; **8** clear and consistent for common tasks and edge cases; **10** excellent support demonstrated across ordinary users, screen sizes, inputs and failure states. Scores reflect attained usability and bounded evidence, not iteration count, hash counts or assertion totals.

| Original category | /10 | Reason, limitations and concrete next work |
|---|---:|---|
| Navigation and orientation | 8 | Named Settings, scoped search, safe module inspection, retained child return and stable collection identities support ordinary routes. Compact sidebar still relies on icons plus hover/focus names; map guidance favors pointer knowledge. Add accessible map Help and verify exploration→waypoint creation→return in a world. |
| Layout and responsiveness | 8 | Actual 320×240/font2 evidence shows usable shared controls, complete preview controls and full-message paging. Native resize retains new path/name drafts. Very dense settings still require substantial scrolling; fixed child layouts and arbitrary long localized names are not broadly exercised. Validate representative intermediate widths/scales and translated labels, including page settings and native menus. |
| Hierarchy and readability | 8 | Visible field labels, explicit Replace/Save as, reserved status and measured long-token wrapping improve comprehension. Native pixel-style field text visibly differs from surrounding themed type (0052); page-specific errors still elide. Finish F1, then check contrast with bright/custom imagery and long labels without sacrificing caret accuracy. |
| Motion and transitions | 8 | Opening/reversal samples, keyboard-triggered completion, row settling and genuinely advancing preview age/model pose support predictable motion. Pause, zero speed and Reduced Motion have bounded assertions. No continuous physical-input or display/GPU smoothness evidence. Capture intermediate pointer/keyboard interaction in page settings, HUD/Glass and container children; retain deliberate input-intent completion. |
| Feedback and recovery | 8 | Persistent global messages, full cosmetic error access, protected duplicate saves, retained invalid drafts and explicit disk-versus-active restoration are strong. Page-specific import failures lack full visual access (F1); disk I/O failures and late async abandonment remain untested. Add the same recovery affordance to page settings and exercise failures without changing active values unexpectedly. |
| Keyboard and accessibility | 7.5 | Deep traversal, contextual numeric identities, compact preview Tab/Enter controls and explicit Load restored routes have useful supplied evidence. HUD Right Shift conflicts with coarse movement (F2), audible narration is unverified, and modified-key injection skips real modifier-down behavior. Fix F2 and test both physical modifier sides plus narration on a working host. |
| Discoverability and ordinary-user usability | 7.5 | Visible disclosure, Settings, empty-state recovery, Preview/acquire and profile decisions remove much guessing. World-map keyboard movement/zoom remain poorly taught (F3); resource cache/provider concepts and compact icon navigation still assume familiarity. Add map Help and observe first-time users locating settings, recovering Resources and saving/loading a preset. |
| Consistency | 8 | Shared metrics, contextual child labels, validation policy, logical focus and retained ownership are coherent. Native forms use different text treatment; page-specific feedback lacks the global full-details pattern. Unify feedback access first; compare native/theme field presentation and input behavior before changing typography. |
| Settings workflow | 8 | Combined setting search, disclosure restoration, invalid-edit preservation, profile copy/replace/restore/load separation and resize draft retention cover important workflows. Failed persistence, nested page settings and long end-to-end keyboard acquisition remain incomplete evidence. Add failed-save/restore and native-page import/recovery journeys, preserving disk/active-state distinctions. |
| Perceived performance and implementation economy | 8 | Appearance cache changes have bounded paired evidence: extraction allocation falls about 63–65%, with lower extraction and render-thread CPU medians in this workload. This is stronger than an unmeasured optimization claim. Shared-desktop trials, different preview workloads, remaining render-time collection construction and no world/GPU/display measurements limit generalization. Profile representative world-backed and compact scenes before further optimization. |

Scores sum to 79.0; average is 79.0/10. No ordinary-user study is supplied.

## Evidence audit and interpretation

Independently recomputed SHA-256 for **813 source entries** in `/tmp/riserecode-ux-round8/final/manifest.json` against Git objects at the requested checkpoint: **zero mismatches**. Independently checked **69 evidence entries** against preserved files: **zero mismatches**. These comprise 67 screenshots, native-check-build.log and interaction-and-preview.json. `validatedSourceCommit` matches this checkpoint; `baseCommit` is the preceding checkpoint, not a conflicting source claim.

The frozen log records `:check` at line 287, `:build` at 289, nine UX suite results at 442–451, and BUILD SUCCESSFUL at 457. It reports **384 assertions**, not 384 complete user journeys. Narrator initialization fails to load flite at 389–413. I did not rerun these checks. Source/evidence correspondence is not release-JAR provenance, installation verification or publication verification.

Visually inspected **all 67 final screenshots in five ordered contact sheets**, with 0052, 0057 and 0059 additionally opened at full resolution. Contact-sheet inspection is useful for broad layout/overlap review but not an exact pixel-level audit of every small label. Also inspected all **16 primary capped-performance screenshots** in a contact sheet. Pictured strengths include visible nested color rows, clean Resources footer/status, full compact preview controls, explicit profile form labels and final long-error suffix/reason. The 320×180 choice/waypoint captures are layout fixtures; they must not be represented as actual native window sizes. Actual small-window evidence is 960×720 at GUI3 → 320×240.

Classifications used below:

- **Pictured:** visible in the supplied screenshot; a still cannot prove animation, timing or interaction.
- **Static:** derived from current source; not live reproduced by this reviewer.
- **Supplied assertion:** an explicit assertion in source, recorded passing in the supplied run. Distinct from setup, direct method calls and full physical input.
- **Unverified:** execution evidence is missing or insufficient. This is not itself a defect.

## Nine native UX sources: checked actions versus setup

Read all nine `src/gametest/java/dev/riserecode/Ux*IntegrationTest.java` sources. “Test input” below means Fabric's input helper, not a human-operated device or measured physical-input latency.

| Suite | Assertions | Actual scope and boundary |
|---|---:|---|
| Journey | 19 | Disclosure versus activation, no-result/scoped search, dirty-leave cancellation, choice return/cancel and map/list Done. Most state setup, page changes, pointer dispatch and some key events are direct calls. |
| Layout | 65 | Numeric invalid drafts, font2 row geometry, test-input Tab to a deep module setting, scoped client search, block Page Down and HUD selection. HUD modified nudge is direct. Choice/waypoint 320×180 cases call Screen.init as fixtures. |
| Auxiliary | 58 | Combined module/setting search reveal and restoration without gameplay activation; numeric replacement; HUD synchronization, typed/wheel routing; test-input Tab through auxiliary forms; pose return/draft and Selected blocks. Mixes fixtures, direct clicks, modifier events and test input. |
| Viewport | 14 | Actual resize to 960×720/GUI3, live 320×240 Appearance/choice geometry and backward reveal of HUD owning row. Backward key is directly injected; not all surfaces are resized. |
| Motion | 7 | Sampled opening advancement, reversal continuity, settling, Reduced Motion window and sidebar geometry. No frame pacing or continuous human interaction. |
| Nested/Resources | 52 | Expanded channels at .75/1/2, visible Blue pointer routing, invalid endpoint drafts/baselines, exact versus concise numbers, Resource details/return/Selected/reorder/removal/Tools/category/failure. Backward traversal reaches the last catalog entry; it is not forward traversal of all 641 entries. Modified keys inject flags directly. |
| Recovery | 18 | Compact visible cosmetic search, hidden focus release/draft retention, 24-waypoint mutation/return/deletion, two UUID friends and Resource cache recovery. Tab/Enter/Escape use test input; confirmation Yes and modified keys use direct calls. Waypoint deletion asserts nonempty focus, not exact neighbor identity. |
| Completion | 38 | Actual compact preview/import, empty-state recovery, offline provider acquisition start/cancel/fail/retry/select/apply, message header/full text, duplicate save/cancel/copy/replace/JSON restore and HUD help. Native buttons are activated by direct onPress; provider is a separate deterministic fixture and PNG generated locally. This is not live networking or a full physical-input journey. |
| Refinement | 113 | Submitted avatar age/wing phase and model sampling advance; test-input Tab/Enter zoom/rotate/reset/pose/pause/resume; Reduced Motion and zero speed; path/name resize text/caret/selection/focus; repeated-text selection anchor; full long-path/error paging; in-progress import resize; restored disk versus active state, consumed backup and keyboard Load; opening/typing/expansion/reversal/navigation probes at .2/1s with Reduced Motion on/off. Many selections, values, focus requests, pointer probes, restore actions and Ctrl+F events are direct setup/calls. |

Refinement's companion vertices are sampled from submitted age through `poseHash`; this is stronger than a frozen screenshot, but not a hash of actual final rendered pixels. Probe flags in the test mixin expose submitted state. The assertion checks progression by more than eight ticks, not precise phase-to-wall-clock calibration. A full wide-pane→child phase-continuity comparison is not asserted. Pose selection, pause/resume, zero speed and deliberate Reduced Motion pause are explicitly checked.

Ordinary Tab/Enter/typing use test input in the new paths; Ctrl+A/Ctrl+F and other modified paths inject native modifier flags because the helper supplies zero modifiers. Thus “physical” wording in assertion strings should be read as this narrower test-input scope. A real modifier-down event is not covered. Timeline observations occur after waits/barriers: e.g. Tab is observed around 235–237 ms, not measured at the physical key timestamp. Pointer opening probes invoke mouseClicked with computed displayed geometry; they do not exercise the complete OS mouse event path. Rapid pages and interrupted toggle calls are programmatic. No precise interaction-latency claim follows.

## Reassessment of all five round 8 findings

Paths below are relative to `src/main/java/dev/riserecode/` unless stated otherwise.

1. **F1 frozen child preview: addressed.** `ui/component/CosmeticDetailsScreen.java:51–52` advances the shared clock while it owns rendering; `CosmeticsEditor.java:115–116,175–186` advances/submits in the parent route. Refinement records advancing submitted age/wing phase/model sample and intentional pause/resume/Reduced Motion/zero speed. 0055/0056 show separated poses. This closes the frozen-clock defect, not universal smoothness.
2. **F2 lost native child drafts: addressed.** `ui/menu/EditBoxDraft.java:6–12` preserves exact text/caret/native selection anchor; `CosmeticDetailsScreen.java:28,38,47` and `ui/menu/ProfileSaveScreen.java:19–26` restore state on resize/return. Refinement checks typed path/name, actual resize, useful focus, repeated-text anchor direction and in-progress import resize. 0052/0053 show new visible naming guidance. This closes the specific child draft loss.
3. **F3 inaccessible long tokens/cosmetic errors: addressed in those owners.** `ui/menu/ScreenText.java:14–30` measures grapheme-boundary wrapping and preserves paragraphs; `InfoScreen.java:20–26` pages cached lines; `CosmeticDetailsScreen.java:37,55` exposes full status; ProfileSaveScreen has full error details. Refinement verifies character preservation, measured line fit, keyboard final page and return draft. 0058–0060 support readable start/end/prose. F1 below concerns a separate page-specific feedback owner, not a reopened InfoScreen defect.
4. **F4 compact preview control parity: addressed.** `CosmeticDetailsScreen.java:36,40–49,63–64` supplies pose, zoom, rotation, reset, pause and scoped wheel zoom; text arrows remain editing arrows. 0057 and test-input navigation support reachability and view-only draft stability. Do not reassert absent compact keyboard zoom or pose.
5. **F5 ambiguous Restore previous: addressed.** `ui/clickgui/ClickGui.java:390–401` explicitly separates saved/active state, consumed previous revision and Load restored action. Refinement asserts active values stay B, backup is consumed and explicit keyboard Load applies A; Completion checks restored JSON. 0061/0062 show profile routes, not all notice prose. No implicit gameplay apply is introduced.

Retain older demonstrated fixes: named Settings; distinct activation/disclosure; combined and scoped search; retained/cancelled drafts; logical offscreen traversal; shared font-aware rows and nested color geometry/routing/context; endpoint validation and exact editing precision; Resources details/Selected/reorder/Tools/cache recovery; cosmetic empty-state and import/acquisition route; confirmed friend/waypoint removal and stable identities; clean footer/status; HUD help and compact Glass preview. The Friends instruction is corrected at ClickGui:464. Source ownership still protects ordinary nested choices and suppresses transient nested gears. Real container remote-close/abandonment remains unverified, not the former unconditional-disposal finding.

## Prioritized actionable findings

No new P1 established. No introduced regression from 14d047f to this checkpoint was conclusively established. F1/F2 are current static defects outside the five fixed paths; F3 is a discoverability gap. All need targeted acceptance evidence, not more assertion totals.

### F1 — P2: Page-specific Appearance still hides full import/error details

**Classification: static; no supplied screenshot/assertion for this failure path.** `ui/menu/PageAppearanceScreen.java:41–47` sends editor/import exceptions into its private `message`. At **:75** it draws only an ellipsized line. There is no full-message action, paging route or message-specific narration here; **:78–86** dispatch only Done/Reset/editor/input. Global ClickGUI notice and cosmetic child full-details fixes do not own this string.

**Ordinary-user impact:** importing a custom page background with a long filesystem path or provider/decoder error can hide the part identifying the failed file/cause. Users cannot recover the full text visually from this screen, even though the global interface now offers that capability.

**Correction:** retain a focusable status/details action in reserved space and open the shared full-text view while preserving the page editor, draft/focus and container ownership. Give concise success/failure context and complete retained details.

**Acceptance:** through a native page gear, type a long invalid PNG path, activate Import Image and reach full details with Tab/Enter and pointer. Read through the final cause at actual 320×240/font2, return with path/focus intact and correct the path. Repeat with the theme disabled for that page and a container-backed caller; do not invalidate/close a live container merely by opening details. No test was run here.

### F2 — P2: HUD Right Shift closes the editor before the advertised coarse nudge can occur

**Classification: static, not supplied runtime reproduction.** `ui/hud/HudEditorScreen.java:177–181` implements Ctrl+Shift+arrows as ten-pixel movement, but **:186** unconditionally treats key **344 (Right Shift)** as Close. Outside a focused text input, pressing Right Shift as a modifier therefore exits before the subsequent arrow can reach `nudge`. `:193` saves on removal. `KeyboardHandlerMixin.java:27–30` auxiliary handling does not consume this modifier as a navigation action; it can reach the screen handler.

**Ordinary-user impact:** a keyboard user using the right-hand Shift for the documented shortcut unexpectedly leaves the editor and saves instead of moving the selected HUD item. Left Shift may work; this is not a claim that all coarse nudges fail. The current direct `nudge(new KeyEvent(...,3))` checks skip the Shift-down event, exactly the missing distinction.

**Correction:** remove the unconditional modifier-key close, or use a configurable explicit close action that does not conflict with documented editing modifiers. Escape/Done already provide clear exit actions. Preserve modifier entry while a HUD element is selected.

**Acceptance:** select an element without a numeric field owning text; press/release real left and right Ctrl/Shift in both orders, then arrow. Both sides move ten pixels and remain on the editor. Ctrl+arrow moves one pixel; plain modifiers do nothing destructive; Escape/Done still save/return. Include the actual modifier-down event in the native test path; keep explicit flag injection for isolated nudge-unit behavior.

### F3 — P3: World-map keyboard controls exist without equivalent visible help

**Classification: static discoverability limitation.** `map/WorldMapScreen.java:63,66–67` teaches drag and Shift+wheel (or connection status); **:91** implements arrow pan and +/- zoom. The toolbar at **:35–39** has no Help action. A keyboard user is not told these operations. The supplied Journey checks only keyboard Done, not map exploration or discovery.

**Correction:** add a named, focusable Help route explaining pan, zoom, altitude, center, Add and waypoint management, including modifier choices and connected/offline behavior. Keep concise context on the map itself.

**Acceptance:** a first-time keyboard user can find Help, pan/zoom, add and validate a waypoint, return to the same view and close to the caller in actual small/font2 and normal layouts. Show only operations available in the current connected state. This finding concerns teaching existing support, not absent map keyboard support.

## Performance evidence: credible result and limits

Independently hash-checked all **62 entries** in `/tmp/riserecode-ux-round8/performance/evidence-manifest.json`: zero mismatches. Baseline's **664 main-source hashes** match Git at 14d047f. Of candidate's **665 main-source hashes**, exactly two differ from final checkpoint: `ui/menu/EditBoxDraft.java` and `mixin/MenuEditBoxMixin.java`. Both preserved benchmark versions match their candidate receipt hashes. This independently confirms the stated snapshot boundary; it does not collapse benchmark source and final regression source into one build.

Read environment JSON, primary before/after per-trial JSONL, source/class receipts, native logs, measurement notes, harness/timing mixins, aggregation script, report and paired summary. Recomputed every median/min/max metric in the paired summary from primary JSONL: no discrepancies. Four primary runs each contain 12 trials: three roughly 30-second trials per scene, each reporting the correct 60/144 effective cap. Native logs end successfully. JFR files have native FLR headers and are hash-verified; the exported allocation attribution explicitly uses statistical weights and overlapping ancestor attribution. I did not independently decode/reanalyze entire JFR event streams or reconstruct historical compiled classes from source. Class receipts are preserved fingerprints, not an independently re-created binary chain.

The strongest supported result is Appearance extraction allocation: **702.155→257.333 KiB/call at cap60** and **701.573→243.627 at cap144**, reductions about **63.35%/65.27%**. All three candidate per-trial allocation values lie below all three baseline values at each cap. Extraction elapsed medians decrease about **25.29%/29.89%**, and render-thread CPU/frame about **14.48%/14.39%**. This supports lower measured allocation and CPU cost for this warm menu workload, consistent with cached matching/rows at `ThemeEditor.java:65–106,177–193`. It does not measure a corresponding percentage gain in FPS or perceived smoothness.

Methodology limits and unsupported extensions:

- Runs are sequential on a shared desktop, in fixed scene order with common JVM/JIT history. Three repetitions are not independent randomized experiments. System-load averages do not isolate interference. Timing attribution/causality is less secure than the large allocation difference.
- Explicit test-only caps override production themed-menu uncapping. These are 60/144 cap cases on a 240Hz monitor, not 60/144Hz physical-display latency experiments. Java25/RX9070/OpenGL, 1280×960/GUI2, VSync off and no loaded world constrain scope.
- `renderWork*` includes limiter waiting/present, despite the potentially misleading “work” name. Harness intervals include scheduling/barriers. Neither is GPU work time. Counts above 16.7/33.3 ms do not directly establish missed scans or input latency; at 144 cap those thresholds also are not the 6.94ms frame budget.
- Preview baseline freezes while candidate animates, and the child controls/layout differ. Increased preview CPU/allocation is not a like-for-like regression. Even other scenes have evolving keyboard/focus/scroll state; screenshots show different sampled Appearance positions. The benchmark is representative automated activity, not identical replayed pixels.
- Appearance setup expands/reveals Primary Accent, then scrolling drives the scene. This does not show every extraction samples the expanded picker on screen. Resource activity alternates query and Tabs; it does not prove all 641 entries are traversed.
- Warmup is five seconds per scene and one second between trials; JFR remains enabled. No confidence intervals, thermal randomization or robust cold-state comparison is supplied. GC counters are whole-process window deltas, not UI-only attribution.
- Uncapped runs remain exploratory; pilot-candidate is excluded. Notes explicitly disclose exploratory parsing load. Neither belongs in the primary inference.
- Allocation reductions come from measured ThreadMXBean counters, not exact per-method totals inferred from JFR sample weights. No universal FPS, GPU timing, display latency, live multiplayer, optional-renderer, final byte-identical-build or installed-release claim is supported.

The human report correctly discloses the later selection-anchor fix and final-regression boundary. However, `performance/scripts/summarize-ux8-performance.py` still contains the older generated sentence that candidate hashes are verified against the completed build. Regenerating it would overwrite that clarification. Update the generator to emit the current boundary before reusing it; this is evidence-maintenance debt, not a production UX defect.

## Breadth, unresolved evidence and next checkpoint

This review covers source and supplied examples across ClickGUI/modules/sidebar/search; Appearance and page settings; shared sliders/text/colors/choices; Resources/catalog/details/Selected/Tools; cosmetic browse/edit/import/preview; HUD/Glass; block selection; map/waypoints; Profiles/Friends; and native menu/container adaptation. It does not infer full native coverage from custom child-screen captures.

The most consequential missing evidence is real container and native menu behavior. `ui/menu/NativeButtonLayout.java:17–24` alters broad native noncontainer button geometry. `PageAppearanceScreen.java:17,88–91` and `RetainedScreen.java:10–20` own nested/container lifecycle. Exercise options, multiplayer/server lists, ordinary confirmations, inventory/container gear→choice/details→return, server-side container close while nested, and abandonment without return. Existing guards are a reason not to resurrect the closed disposal defect, not proof that every lifecycle works.

Other explicit evidence gates: audible focus/error narration on a working host; physical modified-key journeys; actual networking/cache pause/loading/ready/fail permutations; failed save/load/restore I/O; async import/download completion after abandonment; complete waypoint invalid/save/edit/delete/return in a world; contrast/localization with arbitrary imagery; optional renderers and multiplayer. These are unverified conditions, not asserted failures. Native pixel-style input mismatch is pictured polish debt, not proof of incorrect caret geometry.

Next work with material value: fix page-specific full-error access and HUD Right Shift behavior; add map Help; exercise those journeys plus real container/native integration and audible narration. Preserve the five resolved findings and current regression coverage. Reuse measured Appearance caching, but optimize remaining render-time collections only after identifying a measured cost. No installation or publication has been performed or certified by this review.
