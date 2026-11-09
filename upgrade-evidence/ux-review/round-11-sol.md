# Independent UX/UI review 11 — RiseRecode

Checkpoint: `2806bd864e87bdd0ba4116abd159e9d66a05e450`, branch `feat/ux-motion-review`, reviewed 2026-11-09 in `/home/toast/Work/riserecode`.

**Equal-weight average: 8.10/10.** The common routes exercised here are usable, and round 10's three specific problems are addressed within the demonstrated scope. The clearest remaining work is consistent validation feedback, separating cosmetic inspection from draft selection, and strengthening secondary-text readability. No P1 was established. No introduced regression was conclusively established.

This is an **agent usability pass, not a human study**. Successful agent actions do not establish discoverability across ordinary users. Scores reflect the original rubric, not iteration counts, hashes or assertion totals.

Original rubric: **0 absent; 2 usually blocks tasks; 4 partial with substantial discovery/workarounds; 6 usable for familiar users with material gaps; 8 clear and consistent support for common tasks and edge cases; 10 excellent support demonstrated across ordinary users, screen sizes, inputs and failure states.**

| Original category | /10 | Reasons, limits and concrete next work |
|---|---:|---|
| Navigation and orientation | 8.50 | Named Pages, Current, selected checkmarks and contextual Back make compact destinations findable without icon knowledge. Search found Full Bright. Cosmetic Preview can nevertheless open without an item and refers to Browse ambiguously. Make that route contextual and separate inspection from selection. |
| Layout and responsiveness | 8.25 | Actual compact UI stayed operable; waypoint text and focus survived the two-column to one-column resize. Supplied native Options/choice/tooltip cases broaden coverage. Compact cosmetic preview is small relative to its controls; long dirty-state titles are truncated. Improve preview allocation and show draft state in a dedicated short status. Arbitrary sizes/translations remain unverified. |
| Hierarchy and readability | 7.75 | Clear titles, pill buttons, recessed inputs and teal selection form a coherent system. Secondary labels are noticeably subdued over grass; pixel input text differs from surrounding smooth typography. Increase secondary contrast and backdrop consistency before adding decoration. No measured universal contrast-compliance claim. |
| Motion and transitions | 8.00 | Sampled actions returned predictably and did not visibly strand this reviewer. Supplied sampled reversal, early input and Reduced Motion assertions support those specific states. Sustained wheel/drag/typing under slow transitions was not independently exercised. Validate continuous interaction and frame pacing separately. |
| Feedback and recovery | 8.25 | Reset explains scope before commitment; missing PNG cause, full details, correction/retry and explicit import draft result worked live. Typed waypoint correction leaves a stale red error. Unify field-owned validation and preserve unrelated errors. Delayed completion behavior is supplied assertion evidence, not my live reproduction. |
| Keyboard and accessibility | 8.00 | HUD Help teaches precision keys; native Ctrl+arrow movement was attempted and reversed. Tests cover logical reveal, modifiers and retained openers. My live pass was predominantly pointer driven; audible narration and OS-device behavior remain unverified. Exercise narration on a working host and complete keyboard-only first-use tasks. |
| Discoverability and ordinary-user usability | 7.75 | Named compact routes, inspectable choices and local-file error wording help. The installed-cosmetic inspection task remained incomplete; Preview/acquire with no selection and technical waypoint IDs/hex need more guidance. Put named Dimension/Color choices before advanced raw values and add a view-only item preview. Human task observation is still needed. |
| Consistency | 8.25 | Shared choices, details/return ownership and metrics unify many owners. Named chooser correction clears its own validation while typed correction does not. Cosmetics row activation also conflates selection in a draft with choosing an item to inspect. Align these behaviors across owners. |
| Settings workflow | 8.25 | Safe module disclosure, auto-save explanation, reset confirmation, unapplied cosmetic state and deliberate Cancel support common workflows. Profiles have explicit replacement and saved-versus-active recovery in supplied evidence. Improve field correction and contextual preview entry; failed persistence remains outside my live coverage. |
| Perceived performance and implementation economy | 8.00 | This session had no observed dead end or visible frozen workflow. Shared measured layouts, bounded tooltip cache and historical Appearance cost evidence support economical implementation within their scope. No fresh performance improvement is demonstrated for this checkpoint. Profile representative current world-backed menus before claiming one. |

Scores sum to **81.00**, divided by ten = **8.10**. A rating of 10 is not justified by this evidence.

## Order, authorization and integrity

Phase 1 preceded production source, previous-review, implementation-log and memory lookup. Only `scripts/ux-review-session.md` was read first. I then operated the already-running disposable client through the specified bridge, viewed fresh screenshots to choose coordinates, and sent one JSON action at a time. No widget tree or supplied solution path was used. The initial attempt incorrectly supplied a `send` subcommand; argparse rejected it before any runtime action. Retrying with the documented JSON-as-command syntax worked. This harness usage error is not a product failure.

State 0 to finished state 56 spans approximately **346 seconds** of the bridge capture clock, comfortably inside the requested live phase. Every returned phase was ready until **56: finished**. The trace is `/tmp/riserecode-ux-round13/live-review11-attempt2/{commands,frames,observations.jsonl}`. No client was launched by this reviewer. Initial fixture was 1280×960/GUI2/font1, 60 cap, VSync off, no menu uncapping. Compact preset was a test geometry action, not task success. Input uses Fabric helpers/KeyboardHandler callbacks, not physical OS input. Turnaround includes harness scheduling and is unsuitable for physical latency measurement.

After finish, I read AGENTS.md and AGENTS.local.md. This explicit review scope supersedes standing install/publication instructions. Checkout HEAD/branch matched the request; `git status --porcelain` was empty before and after inspection. No checkout files were modified, builds/tests/additional clients run, installation/publication performed or delegation used. Only the report and contact sheets were created under `/tmp`.

Recomputed the authoritative final manifest: **824/824 source/tooling SHA-256 entries match both checkout bytes and Git objects at the requested checkpoint; 111/111 evidence entries match supplied files**. Its validatedSourceCommit matches this checkpoint; baseCommit is the earlier 6b7a626 snapshot. Build precedes commit: this establishes source/runtime correspondence, not release-JAR provenance. No installation/publication is established.

## Compact first-use log

All sequence numbers below identify both command and returned screenshot; frame files use six digits.

| Task | Actual actions and evidence | Outcome and hesitation |
|---|---|---|
| 1 FullBright | 1 typed FullBright into initially focused search; 2 clicked card body; 3 clicked Mode; 4 Escape. | Found Full Bright while Off, inspected Lightmap/Gamma/Shader Night Vision with Lightmap checked, returned without enabling. The selected mode's technical renderer terms have no visible explanation in this chooser. Did not change/restore a general display setting; that optional part is uncompleted. |
| 2 Appearance/reset | 5 clicked named Appearance; 6 expanded Glass Material; 7 clicked gear; 8 Reset page; 9 Keep page; 10 Escape. | Scope explicitly says Appearance returns to shared appearance and clears page background/color/style overrides while leaving other pages/shared settings unchanged. Cancel returned to Page appearance; Escape returned to expanded Glass Material. No reset was committed. |
| 3 Compact navigation | 11 compact preset; 12 Pages; 13 typed Resources; 14 chose Resources; 15 Pages; 16 typed Cosmetics; 17 chose Cosmetics; 32 Pages; 33 typed Map; 34 chose Map settings. | Appearance remained visible after preset, though its main header was a truncated search placeholder rather than a full page name. Pages said Current: Appearance, then Current: Resources/Cosmetics on subsequent openings. Names and search avoided guessing icons. I did not test no-result recovery live; supplied tests cover it. |
| 4 Installed cosmetic inspection | 18 clicked Preview/acquire without choosing Solid or Gradient first. | Opened a generic avatar with “Choose an item in Browse…” and disabled Download/Select. **Original installed-item inspection was incomplete.** I did not backfill it from code. Later import supplied a visible cape preview; that is a different task. |
| 6 Missing import, recovery and preview | 19 focused path; 20 typed missing.png; 21 Import PNG; 22 Status details; 23 Escape; 24 focused path; 25 Ctrl+A; 26 typed authorized cape.png; 27 Import PNG; 28 Pose; 29 Crouching; 30 Back to outfit; 31 Cancel. | Explicit File not found with complete path in details. Escape retained path. Correction succeeded: Installed, imported cape, and “selected in draft” were visible. Pose chooser had readable named choices and current checkmark. Crouching changed preview. Back showed unapplied changes; Cancel removed dirty title and disabled Edit. Never pressed Apply. Import installed a local asset in the disposable library; Cancel discarded outfit draft, not the imported library asset. No external download initiated. |
| 5 Waypoint | 35 Waypoints; 36 Manage waypoints; 37 Add; 38 X; 39 typed oops after existing 0; 40 Save; 41 X; 42 Ctrl+A; 43 typed 12; 44 actual resize to1280×960/GUI2; 45 Cancel; 46 Escape. | Save rejected 0oops with “X: enter a whole number…” and did not leave editor. Corrected 12/caret/focus survived resize and column change. **Old red error remained**, even after correction and resize. Cancel deliberately returned to empty manager; no destination was saved or needed deletion. Raw Dimension ID and Color hex were visually prominent; Choose buttons existed but were not explored live. |
| 7 HUD | 47 Pages; 48 HUD; 49 Edit HUD; 50 Help; 51 Escape; 52 clicked FPS; 53 Ctrl+Right; 54 Ctrl+Left; 55 Escape. | Help explicitly taught Ctrl+arrows one pixel, Ctrl+Shift ten, grid and save/return. FPS selected; sampled border moved slightly and reversed. No exact internal pixel measurement was made independently. Returned to HUD page. |
| Finish | 56 finish | Bridge reported finished. |

Live typing, opening/closing, chooser selection, error correction and resize were exercised. Wheel scrolling and dragging were **not** independently attempted; do not infer their success from this log. No universal smoothness/FPS claim follows from these screenshots.

## Evidence audit and coverage

Inspected all **109 final PNGs** in seven contact sheets `/tmp/ux11-sheet-0.jpg` through `-6.jpg`, plus the live frames shown individually during operation. Contact sheets support broad hierarchy/layout inspection, not pixel-perfect reading of every label. Supplied stills are **pictured** evidence, not interaction or timing proof. Actual small native viewport is960×720/GUI3 =320×240;320×180 Screen.init captures remain logical fixtures.

The supplied final log records check/build and BUILD SUCCESSFUL at line759. Its thirteen suite totals sum to **710 assertions**, not710 end-to-end user journeys. I read the thirteen Ux*IntegrationTest sources and test-only interactive bridge, including setup, helper dispatch and assertions. Evidence classifications used here are **live observed**, **static**, **pictured**, **supplied assertion**, and **unverified**. The reviewer did not rerun the supplied tests.

| Suite | Assertions | Supported scope and boundary |
|---|---:|---|
| Journey |19| Disclosure versus toggle, scoped/empty search, dirty-leave and caller returns. Direct component/screen methods are used. |
| Layout |66| Numeric draft recovery, font-aware rows, deep Tab reveal, block paging/HUD selection.320×180 children use init fixtures; nudge is direct. |
| Auxiliary |58| Combined search/disclosure restore, replacement numeric baseline, HUD synchronization, shared auxiliary geometry, pose and selected-block view. Mixed direct/state setup and input. |
| Viewport |12| Actual960×720/GUI3 fit, small choices and backward HUD reveal. Does not resize every form. |
| Motion |7| Sampled advance, reversal continuity, settling and Reduced Motion. No continuous frame-pacing measurement. |
| Nested |52| Color endpoint validation/stride/routing, exact numeric edit, Resources last-card/Selected/reorder/Tools/category/error routes. Backward jump to last card is not traversal of every catalog entry. |
| Recovery |18| Compact cosmetic search ownership, waypoint mutation focus, confirmed friend removal/UUID retention, Resource cache-help route. Some confirmations use direct onPress; delete-neighbor check is narrower than exact identity. |
| Completion |38| Local provider cancellation/failure/retry/select/apply, local PNG import, full messages, profile replacement/recovery, HUD Help. Direct onPress and deterministic local assets do not prove network or physical input. |
| Refinement |113| Submitted preview time/model sampling, view controls, native draft/caret/selection resize, long-error paging and saved-versus-active profile recovery. Submitted state is not final-pixel animation proof. Historical interaction timeline is carried forward unchanged, not fresh timing evidence for this binary. |
| NativeWorkflow |171| Native Options/Multiplayer, page errors/choices, entrance/modifiers, world-backed map creation/correction/deletion/return, local ChestMenu retention. Programmatic menu provider, reflection and explicit callback flags remain. |
| Polish |97| Reset decision/scope, named compact navigation/search/dirty decisions, exact waypoint/choice resize context, named dimensions/colors. Also exact replacement Pause identity and diamond conservation for the reset-confirmation abandonment path. This strengthens the specific evidence gap noted in review10; it does not certify every container race. |
| Tooltip |38| Actual compact font.75/1/2 bounds, styled wrapping, bounded preview/F1/full details, scaled module hover and native menu retention. Mixed/nontext identity check does not establish arbitrary image fit. |
| AsyncRecovery |21| Direct missing-file/empty-path, pending/duplicate suppression, correctable retry, delayed result after item/path change/discard/leave and nested status return. Worker latches/reflection/direct root action are explicit. Permission-denied classification is static, not a native reproduced permission test. |

The bridge source creates an owner-only disposable world, disables modules, sets fixture geometry/cap and exits on finish or20minutes. It exposes screenshot/native input actions, not a widget solution API. No installed Modrinth state was accessed.

The before manifest identifies **6b7a626** and parent reproduction scope. I recomputed **668 declared production/tooling hashes against that Git commit and all5 evidence hashes**, with no mismatches. The input described665 production/resource hashes; the manifest declares668 entries in its productionSha256 map, so this report uses the actual declared count rather than treating that discrepancy as a UI defect. `before/native-red.log:164` records the absent-cause NullPointerException, `:194` the visible-error assertion failure, and `:245` BUILD FAILED. Exact test/runner wiring is preserved there. This is genuine **parent red reproduction evidence**, not my pre-fix observation. The post-fix result is supported separately by current source, final supplied assertions and my retained-route live import.

## Reassessment of round10 and loops10–13

Paths below are relative to `src/main/java/dev/riserecode/`.

1. **Round10 F1 reset recovery: closed within scope.** `ui/menu/PageAppearanceScreen.java:22,42` now uses requestReset and a retained confirmation before resetPage. Live7–10 demonstrated readable scope and safe cancellation. `UxPolishIntegrationTest.java:55–63` checks exact cancellation data, confirmed inheritance/style and unchanged other/shared settings, then details/return. My live pass did not commit a reset or test failed disk persistence.
2. **Round10 F2 waypoint resize context: closed within scope.** `map/WaypointEditScreen.java:25–35` captures native drafts, focused field/actions and restores them across rebuilding. Live43–44 retained corrected X and caret/focus across actual column change. Polish verifies repeated-substring selection anchors, Save/Cancel focus and subsequent typing isolation. Stale validation below is a separate feedback issue, not reopened context loss.
3. **Round10 F3 compact icon discovery: closed within scope.** `ui/component/Sidebar.java:36–37,66–70` provides named Pages; `ui/menu/PageNavigationScreen.java:25–59` supplies Current, named/searchable destinations, selected mark, Clear and contextual Back. Live12–17/32–34 found the requested destinations. No unfamiliar-human success rate is implied.
4. **Loop11 tooltips/full details/native retention: supported.** `ui/menu/TooltipPresentation.java:29–45` measures wrapped text, bounds height, adds F1 hint, caches a presentation and checks recent screen ownership. Native anchor conversion and shared retention are present. Supplied0096–0105 and Tooltip assertions support sampled sizes/routes. Live0's Auto Tool tooltip was contained. I did not independently exercise F1 or a container.
5. **Loop12 named waypoint choices/shared enums: supported.** `WaypointEditScreen.java:17–29,43` offers named dimensions/colors and contextual tooltips; `ui/menu/ChoiceScreen.java:30–54` retains query/caret/selection and focused option across repagination. Supplied0090/0092 show named dimensions. My Mode and Pose choices had clear selected marks and Cancel. Raw technical fields still dominate the waypoint form; this remains an opportunity, not a claim that named choices are missing.
6. **Loop13 import recovery: supported within local tested cases.** `ui/component/CosmeticsEditor.java:118–143` unwraps only known future wrappers, handles direct file/permission errors, marshals to client thread, shares pending state and guards auto-selection using revision/draft/item/category/ownership. `CosmeticDetailsScreen.java:28–30,53–65` captures retained draft/opener, stops auto-selection after departure and disables duplicate import. Full status stays reachable. My21–27 observed missing-path cause, full details, return and successful correction; delayed completion is supplied assertion evidence. No live network acquisition or permissions failure was reproduced independently.

Retain demonstrated earlier closures: safe card-body inspection; named Settings; scoped/combined search and disclosure restoration; numeric invalid-draft preservation; protected Resources/cosmetic dirty navigation; logical keyboard reveal; expanded color metrics/routing and endpoint identity; compact search release; friend confirmation; full Glass preview controls; full messages; profile replace/previous-version recovery; HUD Help and modifier handling; map Help and measured toolbar/footer separation. Current source/tests/captures support these scoped resolutions. Do not reopen them merely because broader human, renderer or timing evidence is absent.

## Prioritized current findings

### F1 — P2: Typed waypoint correction leaves contradictory stale error

**Evidence: live observed + static.** Frames40–44 show X:whole-number error after0oops, then corrected12 with the same red error still displayed, including after native resize. `map/WaypointEditScreen.java:41–43` creates inputs without a validation responder; `:49–64` stores error only during Save; `:72` keeps rendering it. `:29` clears a matching error for named choices, giving those corrections a different policy.

**Impact:** a user who has corrected the field cannot tell whether the new input is accepted, especially when the red warning persists through a layout change. Save may still succeed; this is not a demonstrated blocked save or lost draft.

**Correction:** track the field owning the current error. On edits, revalidate that field or replace its old error with a neutral “Changed; save to validate” state. Do not clear unrelated-field errors. Keep the draft/caret and reserve the existing error area.

**Acceptance:** submit invalid X, replace with12 using typing, blur and resize; the old invalid-X warning is gone or accurately describes the current state before another Save. An unrelated invalid X remains visible when Dimension/Color changes. Re-invalidating X restores the correct error and Save still commits only fully valid forms.

### F2 — P2 design: Cosmetic preview entry does not establish an item to inspect

**Evidence: live observed + static.** Frames17–18 show installed Solid/Gradient rows, then generic Preview/acquire opening with no selected item and “Choose an item in Browse…”; Download/Select are disabled. Task4 remained incomplete. `ui/component/CosmeticsEditor.java:67,180–183,194` keeps a general preview action enabled while row actions call select directly. `ui/component/CosmeticDetailsScreen.java:62,67` displays disabled acquisition/selection and the generic instruction when definition is null.

**Impact:** first-time users can enter a preview that cannot inspect the intended item. The instruction uses Browse although the current return action says Back to outfit and the main view was already an installed list. Choosing a list item is also an outfit-draft selection, making inspection harder to distinguish from changing the draft. I did not establish that any outfit was silently applied; explicit Apply separation worked in my import task.

**Correction:** provide an item-specific Preview action that only changes the inspected item; keep “Select in draft” explicit. Keep import accessible separately when no item is inspected. At minimum, label the empty route “Import a cape” and provide “Back to installed items,” with inline guidance beside Preview explaining selection requirements.

**Acceptance:** from a fresh empty outfit, an unfamiliar agent/user can preview Solid, change view/pose and return with the original draft intact; selected item, installed status and draft state are visibly distinct. Import remains available without a selected item and never applies the outfit. Assess with ordinary-user tasks rather than only setter assertions.

### F3 — P2 design: Secondary text recedes into the live-world backdrop

**Evidence: live pictured judgment + static styling, not measured compliance failure.** Frames0/5/17/30 show muted descriptions, sidebar names and creator/status text against sky/grass through the translucent surface. They are materially less readable than primary titles, despite explaining interaction/state. `ui/theme/Theme.java:19–20` defines SECONDARY#7F868E and MUTED#525C65; `ui/component/ModuleButton.java:184,187–188` applies secondary styling/small scale, and `ui/component/CosmeticsEditor.java:187,197` does likewise for creator/license and status. Sidebar normal text uses SECONDARY at `Sidebar.java:117`.

**Impact:** explanations and status require more effort precisely when a user is learning controls. Decorative transparency changes the background while text styling stays subdued. Primary controls remained readable; no color-vision or human-reader evaluation was performed.

**Correction:** raise secondary text luminance and use a more stable scrim behind explanatory/status text. Separate disabled text from ordinary explanatory text. Keep hierarchy through spacing/weight, rather than relying mainly on low contrast.

**Acceptance:** inspect normal and compact fonts over bright sky, grass, dark terrain and custom page backgrounds; descriptions, selected location, status and focus remain readable. Measure rendered contrast on representative backgrounds and verify with readers, while preserving clear primary/secondary/disabled distinctions.

### F4 — P3 design: Compact acquisition preview has little visual inspection space

**Evidence: live pictured + static allocation.** Frames27/29 show a small avatar surrounded by large acquisition controls; pose change is visible but cape detail is difficult to inspect. `CosmeticDetailsScreen.java:72` subtracts190 logical pixels in acquisition mode from a240-high viewport, leaving50 for the avatar area; `CosmeticsEditor.java:215` further fits size by height/2.5. View controls reclaim some space and zoom exists, so this is not absence of a preview feature.

**Impact:** the main purpose of cosmetic inspection gets less space than supporting controls. A task can technically succeed while visual comparison remains poor.

**Correction:** make preview/view mode prominent when an installed item is opened, collapse import inputs until requested, and allocate more height to the model. Retain explicit draft/apply state and accessible import recovery.

**Acceptance:** at960×720/GUI3/font2, inspect cape/pose differences without clipping or accidental selection; view-only controls retain the outfit baseline, and import/error routes remain reachable by keyboard.

## Coverage beyond the live tasks and remaining gates

- **ClickGUI/modules/sidebar/search:** live safe Full Bright inspection and compact named navigation; static combined matching and scoped search; supplied empty/reveal/keyboard tests. No claim that every module's vocabulary is understandable to novices.
- **Appearance/page editors/shared controls:** live grouping/gear/reset cancel; source confirmation/choice ownership and supplied color/numeric/deep reveal. The large gap in full-screen choices is visually sparse but keeps common choices separated; not itself a blocked task.
- **Resources/cosmetics:** live Resources recognition and cosmetics import/cancel; static ResourcesEditor:47–54,73,188–197 separates cache readiness, draft Apply and recovery Tools. “Cache” and provider names remain technical. Supplied failures use offline/local providers, not external-network certification.
- **HUD/Glass:** live Help/select/precision attempt/return; supplied shared color metrics, modifier combinations, full compact Glass preview reset controls. HUD overlap/focus in arbitrary custom layouts remains unverified. No fresh live Glass edit was attempted.
- **Block picker:** static `ui/component/setting/BlockPickerScreen.java:29–50` has translated names plus IDs, Selected view, unavailable-ID retention/removal, paging and immediate setting changes. Supplied screenshots0014/0022 and tests support those routes. Unlike the improved ChoiceScreen, it rebuilds native search focus on init and does not capture full native editing context; resize continuity here is an unverified follow-up, not my reproduced finding.
- **Map/waypoints:** live validation/resize/cancel; supplied world-backed map Help/pan/zoom/create/edit/delete/return and toolbar containment. World terrain was simple; arbitrary panel resizing, dimension names and large explored maps are not comprehensively demonstrated.
- **Profiles/friends:** static ClickGui:392–423 retains explicit saved/active restoration text, Load restored, separate inspect/remove and confirmation. Supplied duplicate-save/replace/cancel/restore/load and friend UUID/neighbor tests support scoped closures. Not exercised independently live.
- **Native/container adaptation:** current shared NativeScreenRetention/RetainedScreen retain the menu, validate ownership, close abandoned owners without replacing the new screen and propagate child lifecycle. Supplied local ChestMenu tests include carried-stack identity, reset decisions, server close and exact replacement/conservation. Other menu subclasses, remote races and physical clicked-chest interaction remain unverified.

Historical Appearance performance evidence in `/tmp/riserecode-ux-round8/performance/` and `upgrade-evidence/ux-review/round-8-performance.md` belongs to its separately fingerprinted48f296e-lineage menu snapshot. Capped60/144 trials support bounded extraction/allocation/render-thread CPU differences there. Render elapsed includes limiter waiting, harness intervals include scheduling, and baseline preview froze while candidate animated. Uncapped/pilot results are excluded from primary inference. No fresh improvement, GPU time, physical display latency, universal FPS, renderer matrix or live multiplayer performance claim is made for2806bd8. Host flite initialization failure leaves **audible narration unverified**.

The next material work is F1–F3, followed by compact preview allocation and a working-narrator/keyboard-only task pass. Use fresh source fingerprints for any new performance claim. Preserve the specific demonstrated closures and continue distinguishing agent tasks, supplied assertions and human usability validation.
