# Independent UX/UI review — round 7

Reviewed 2026-11-09 at **7b07c46bc2b9cc387ca8328ea0affee82baff40e**, branch **feat/ux-motion-review**. HEAD and branch match; checkout clean. Read AGENTS.md, AGENTS.local.md, round-{1,2,3,4,5,6}-sol.md and implementation-log.md. No checkout edits, builds, client execution, installation, publication, legacy test execution or agents. Only this external report was written. The explicit read-only assignment supersedes standing release authorization.

## Evidence and fixed rubric

Independently SHA-256 verified **801 source entries against Git objects at this checkpoint**, and **47 evidence entries against preserved files** under `/tmp/riserecode-ux-round6/final/`: **zero mismatches**. Evidence comprises **46 PNGs plus native-check-build.log**. `validatedSourceCommit` matches the requested full SHA. This establishes source/runtime receipt correspondence; it does **not** establish release JAR provenance, installation, independent execution, or universal correctness.

The supplied combined log records `:check` at line287, `:build` at289, seven suite results at442–449 and BUILD SUCCESSFUL at455. The counts are **19+65+58+14+7+52+18 = 233 assertions**, not233 journeys. Conventional `:test` is SKIPPED at239; custom checks and client gametests are separate. Sampled legacy UiChecks covers component semantics, header arithmetic, Unicode input and renderer helpers. Those are useful checks, not end-user usability/visual certification. No legacy tests were run by this reviewer.

Read **all seven Ux*IntegrationTest sources**:

| Suite | Supplied assertions | Actual scope and limits |
|---|---:|---|
| Journey | 19 | Disclosure versus toggle, scoped/empty search, dirty-exit cancel, ordinary choice cancel, HUD caller return and map/list Done. Many actions call screen/component methods directly. |
| Layout | 65 | Invalid numeric drafts, row geometry, actual deep-module Tab, client search, block paging, HUD Tab/Enter selection and direct nudge. Choice/waypoint320×180 `Screen.init` checks are fixtures, not native framebuffer resizing. |
| Auxiliary | 58 | Combined-query reveal/restoration and unchanged enabled state, numeric replacement, HUD synchronization/character/wheel routing, actual Tab into auxiliary forms, pose-choice draft retention and Selected blocks. Mixed physical input and direct actions. |
| Viewport | 14 | Actual960×720 resize, GUI scale3/live320×240, font2, choice bounds and backward owning-row reveal in HUD. Not every screen/size combination. |
| Motion | 7 | Sampled opening advancement, immediate reversal continuity, settling and particular Reduced Motion geometry. No timed input attempts during slow transitions or frame-pacing measurement. |
| Nested/Resources | 52 | Expanded colors at .75/1/2, Blue pointer dispatch through clipped parents, endpoint draft/baseline recovery, contextual labels, concise versus exact numeric values, Resources deep traversal/details/reorder/removal/Tools/category/failure. Many setup/edits are direct methods; backward jump to the final catalog card is not forward traversal of all641 entries. |
| Recovery | 18 | Actual small viewport/font2; visible cosmetic search and draft retention;24 waypoints; two UUID friends; forced Resource failure-help activation into Cache all. Uses physical Tab/Enter/Escape plus direct Ctrl+F/Shift+Tab, screen setup, editor close and confirmation-button invocation. |

Recovery's waypoint-removal focus assertion only requires a nonempty label, while Friends explicitly requires JourneyFriendB. Its friend-cancel check uses `isFriend(UUID,name)`; source inspection supports actual identity preservation, but that single predicate is not a byte-for-byte friend-record comparison. It does exercise actual Escape. The suite does not import/download a cosmetic, start the focused cache action to completion, or establish all loading/paused/retry permutations. Glass preview is shown by direct `showPreview(true)`, not a complete user-driven mode-toggle/adjust/reset journey.

Inspected **23 PNGs using view_image**:0009,0016,0017,0025–0034,0036–0045. This includes every specifically requested scene. Other PNGs were hash-verified, not visually inspected. 0017/0025/0029–0031 show cleaner HUD identification and separated color controls. 0032/0033 show separate full-width Glass settings/preview with complete reset labels and both pointer instructions. 0034/0043 show compact cosmetic form versus visible search. 0036–0042 show Resources catalog/details/Selected/Tools/category/failure;0044 shows late waypoint tracking focus and wrapped toolbar. **0045 visibly retains a friend-removal notice over Resources attribution/Apply/Cancel.** A passing focus assertion does not make this recovery frame visually unobstructed.

Evidence labels throughout: **screenshot-observed** = pictured pixels; **static** = source-derived path; **supplied runtime assertion** = checked by the preserved run; **unverified** = insufficient evidence. Nothing was reproduced live by this reviewer. No measured frame-pacing/FPS/allocations, audible narration (flite absent), universal contrast/localization, live container lifetime, multiplayer or optional-renderer matrix is claimed. The HUD FPS counter is not a benchmark. Narrator and cursor host warnings remain in the supplied log.

Unchanged rubric: **0** absent; **2** usually blocks the task; **4** partial implementation requiring significant discovery/workarounds; **6** usable for a familiar user with material gaps; **8** clear, consistent support for common tasks and edge cases; **10** excellent support demonstrated across ordinary users, screen sizes, inputs and failure states. Fractional scores indicate partial attainment, not measured precision.

## Original category scores

Equal-weight average: **7.7/10**. The recent recovery/focus/readability fixes earn credit. Compact feature loss, overlay interference and profile overwrite recovery keep this below uniformly strong edge-case support. More assertions alone do not raise motion/performance scores.

| Original category | /10 | Assessment |
|---|---:|---|
| Navigation and orientation | 8 | Scoped search, caller returns and stable waypoint/resource identities support common routes. Compact cosmetics lacks a route to acquisition/preview; rail labels depend on hover/focus. |
| Layout and responsiveness | 7.5 | Glass modes, HUD label cleanup and wrapped waypoint toolbar improve the actual small viewport. Cosmetics removes functional actions with its preview; notices overlap footer actions. |
| Hierarchy and readability | 8 | Named numeric fields, concise values, separated color rows and complete Glass instructions materially improve reading. Large-type HUD guidance is still elided, and contextual titles can be truncated. |
| Motion and transitions | 7 | Reversal/settling and selected Reduced Motion support are established within supplied assertions. Slow-transition input availability and frame pacing remain insufficiently demonstrated. |
| Feedback and recovery | 7.5 | Focusable Resource failure/Apply help, safe Friends removal, persistent validation and draft guards are substantial. Notices obscure actions and duplicate-name profile save lacks an explicit recovery contract. |
| Keyboard and accessibility | 8 | Logical traversal, contextual nested names, stable waypoint mutations/return and failure-help activation have meaningful evidence. Compact cosmetic acquisition is absent for all inputs; speech output remains unverified. |
| Discoverability and ordinary-user usability | 8 | Safe module inspection, named Settings, inspectable choices, explicit Remove and full Glass labels are clear improvements. Blank cosmetic results and hidden compact acquisition still require guessing. |
| Consistency | 8 | Shared metrics, invalid-edit policy and contextual action identities extend across the principal forms. Cosmetic compact modes and profile replacement still diverge from the safer recovery patterns. |
| Settings workflow | 8 | Scoped/breadcrumb search, precise editing and retained draft/focus support ordinary work. Compact cosmetics cannot complete acquisition/preview, and saved-profile replacement needs protection. |
| Perceived performance and implementation economy | 7 | Central scheduling, cached module filters and offscreen paint skipping are sensible. Cosmetics/Appearance still allocate filtered/layout collections during extraction; no measured latency/allocation result establishes user impact. |

## Reassessment of resolved findings

All source paths below are relative to `/home/toast/Work/riserecode/src/main/java/dev/riserecode/` unless explicitly stated.

- **Round6 F1, invisible cosmetic Ctrl+F: addressed.** `ui/component/CosmeticsEditor.java:100–110,190` exits editing before focusing visible search and releases hidden search/import owners in short layouts. `:179` excludes hidden import pointer targets. Recovery checks typing Bee, visible search state, retained draft and hidden-owner release;0043 supports that visible state. Do not repeat the invisible-search finding. `:159` guards nonpositive avatar bounds; short layouts skip avatar extraction. No outstanding crash is established in the covered scene. Feature parity is a separate issue below.
- **Round6 F2, keyboard-inaccessible Resource Apply explanation: addressed.** `ResourcesEditor.java:48,193` registers a focusable status action with failure, Apply reason and recovery text; activation expands Tools and requests Cache all/Resume/Pause focus. Recovery checks the named reason/retry and activation;0045 confirms Cache all focus. The old plain-text-only footer claim is closed. Visual overlay interference remains distinct.
- **Round6 F3, waypoint mutation identity: addressed in covered routes.** `map/WaypointScreen.java:21–34,47–49` caches actions by UUID, resolves current values dynamically and labels operations with names; `:28,34` restores editor/confirmation focus and selects a deletion neighbor. `:56–64` measures wrapped toolbar/body and logical reveal. Recovery covers the final of24 rows, visibility/tracking, backward edit return, Escape and confirmed delete;0044 supports the mutation state. Not every history/filter/external-removal permutation is proven.
- **Round6 F4, colliding HUD annotations: addressed.** `ui/hud/HudEditorScreen.java:75–80,88–92,108` replaces per-box labels with hover help, inspector and bottom selected-name guidance. The requested HUD scenes no longer show the former annotation collisions. Underlying HUD values, panel overlay and top-right ArrayList are different content; no universal HUD layout claim follows.
- **Round6 F5, abbreviated Glass controls: addressed in the captured compact mode.** `ui/component/GlassEditorScreen.java:80–81,109–134` allocates full panes and full reset names with distinct instructions. 0032/0033 support the new arrangement. Pointer drag/wheel and keyboard adjustment/reset exist at:119–122,161–177, but complete mode-switch interaction was not newly proven by the screenshot.
- **Round6 F6, accidental Friends removal: addressed.** `ui/clickgui/ClickGui.java:411–423` separates inspection/removal, confirms deletion and restores a neighbor/canceled Remove target. Recovery checks inspection, actual Escape, retained identity predicate, confirmed removal and named neighbor. Do not repeat whole-row accidental removal as outstanding.
- **Earlier nested-color, visible-child pointer, endpoint draft/context and idle precision findings remain addressed.** `setting/ColorPicker.java:50–75` retains measured child layout, endpoint fields/baselines and owning names; supplied Nested assertions cover representative paths. The Blue screenshots support those states, not every animation/localization permutation.
- **Earlier Resources traversal/details/reorder/return and measured typography remain addressed** within `ResourcesEditor.java:83–115,180–188` and supplied tests/captures. Ordinary surrounding clipping during scrolling is not proof the focused control is unreachable.
- Safe module body inspection/explicit toggle (`ModuleButton.java:225–230`), combined-query reveal/restoration (`ModuleButton.java:91–107`, `Panel.java:58–63`), scoped client/Appearance search, HUD position synchronization, invalid slider replacement, new-waypoint draft cancellation, waypoint delete confirmation, translated/Selected block picking and dirty-exit guards remain supported by source/supplied evidence. Do not recycle superseded findings.
- Transient picker gear suppression remains explicit (`ui/menu/MenuTheme.java:74–75`); the former secondary-gear disposal route is closed in source. Real chest/container remote closure and abandoned-child lifetime still need runtime evidence. The earlier pointer-only HUD selection claim remains retracted.

## Prioritized actionable findings

No new P1 established. F1–F3 are **P2**; F4–F5 are **P3**. These are concrete remaining gaps, not claims of fresh live reproduction.

### F1 — P2: Compact cosmetics removes acquisition actions together with its preview

**Static; compact pane absence screenshot-observed.** `ui/component/CosmeticsEditor.java:106–110` sets zero preview bounds and skips `preview()` whenever content height<220, in Browse as well as Edit. But **Download/Cancel download** is created only inside `preview()` at:168–170, and **PNG path/Import PNG cape** only at:171–172. `select()` at:80–82 equips/requests only definitions already available; it does not download an unavailable catalog entry. Thus the short layout does more than omit a model: it removes the route to acquire an uninstalled item or import a local cape.0043 shows full-width Browse with no alternative pane toggle. Existing tests choose an already-available item, so they cannot detect this gap.

**Impact:** A small-window user can see catalog entries marked Download but cannot perform the named action there. The user must know to resize/change scale to recover. Local import and outfit preview similarly disappear without an explanation or alternate route.

**Action:** Separate acquisition/import from avatar geometry. Provide an explicit full-width Preview/Import/Details mode or a compact action area; expose Download/Cancel for the selected unavailable definition independently of the preview renderer. Retain the nonpositive viewport guard and draft ownership fixes.

**Acceptance:** At actual320×240/font2, use pointer and keyboard to browse an unavailable item, start/cancel/retry acquisition, select/apply it and import a PNG cape. A visible mode can inspect the draft avatar and return without losing draft/query/focus. Include a no-network failure state. Larger layouts retain their existing pane. A zero-width-preview assertion alone is insufficient.

### F2 — P2: Success notices obscure active footer controls across page changes

**Screenshot-observed plus static input mismatch.**0045 shows “Removed friend: JourneyFriendA” laid over Resources attribution and Apply/Cancel. `ui/clickgui/ClickGui.java:355–364` paints notices after page content at `windowY+windowHeight-30`, width up to the whole content area, with no reserved layout. `:368–370` retains them for three seconds; the notice survives the Friends→Resources route in this evidence. Resources draws footer buttons at `ResourcesEditor.java:192–196`. ClickGUI pointer dispatch at:480–504 has no notice hit region, so the overlay does not protect covered page controls from clicks.

**Impact:** A prior success masks the current task's controls/recovery context. A user clicking the message can hit the covered control instead; this is particularly confusing in a failure-recovery flow. This is a temporary visual/input defect, not evidence Resource help is keyboard-inaccessible again.

**Action:** Reserve a notice region or anchor it clear of active controls, reflow/wrap long text, and define dismissal/hit behavior. Prevent a notice from silently covering another page's task actions; keep useful named success feedback.

**Acceptance:** Repeat friend removal→Resources failure→status activation at320×240/font2 while the notice remains visible. Apply/Cancel, status help and focused cache action are unobstructed; clicking the notice cannot activate an unseen button. Verify long success/error strings, rapid notices and slow feedback transitions. Capture both active-notice and settled states.

### F3 — P2: Saving a duplicate profile name silently replaces the stored preset

**Static, ordinary workflow/recovery gap.** `ui/clickgui/ClickGui.java:391–404` immediately calls client/appearance `saveProfile(field.getValue())` and reports generic saved feedback, without checking existence or offering Replace/Save as/Cancel. `config/ProfileManager.java:90–93` writes the snapshot over the same name. Its `backupLegacyDocument` at:71–73 only creates a one-time pre-resources backup for legacy documents; it is not a current-profile revision history. `config/JsonStorage.java:35–42` uses atomic REPLACE_EXISTING. Appearance follows the same replacement policy at `ui/theme/ThemeManager.java:430–433,457–463`. Atomic replacement protects against failed partial writes, not accidental replacement of a valued preset.

**Impact:** Reusing a familiar name while saving a new setup destroys that stored version without explaining replacement or providing an in-product restore action. This is inconsistent with the deliberate confirmation/retained backup for client-profile deletion. No claim is made about unrelated external backups or freshly reproduced loss.

**Action:** Detect existing names and offer an explicit named Replace/Save as/Cancel choice, or provide versioned recovery with an obvious Undo/Restore action. Preserve cancellation and active profile identity; distinguish saved-new versus replaced feedback.

**Acceptance:** Save A, change settings, save A again in both client and appearance workflows. Cancel leaves stored A unchanged; Save as preserves A; explicit Replace is named and recoverable under the selected policy. Check case normalization for appearance names and failure feedback. Add a native workflow assertion; existing233 UX assertions do not cover duplicate-profile save.

### F4 — P3: Cosmetic empty search has no explicit explanation or recovery

**Static.** `ui/component/CosmeticsEditor.java:125–140` filters by category, Installed/Catalog and name, then only loops the results. Zero results draws no no-match/not-installed state or Clear/switch-category action. The footer at:145–146 still reports library status, which can say the offline collection is ready despite a blank list. Module/Appearance/choice/block searches elsewhere explain empty results.

**Impact:** Users cannot distinguish an overly narrow query, the wrong category/Installed filter or missing catalog content from a failed load. This is especially opaque with the preview pane omitted in compact Browse.0043 establishes the visible search geometry, not the zero-results state.

**Action:** Show an explicit empty-result message tied to query/category/filter, with clear search and useful Installed/Catalog/category recovery. Preserve draft and selection while recovering.

**Acceptance:** At narrow and wide sizes, nonsense query, empty category and unavailable-only Installed cases show an accurate state. Keyboard can clear/switch filters and recover results without losing the draft. Separate loading/failure states from legitimate zero matches.

### F5 — P3: Large-type HUD guidance hides the movement/save shortcuts

**Screenshot-observed and static.**0025/0031 end the bottom instructions at “Ctrl+arrow…”, while0029/0030 can show the whole line. `ui/hud/HudEditorScreen.java:88–92` concatenates selected name, selection, nudge, coarse step, grid and Escape/save into one elided line. The new stable selected-name location is good, but important keyboard workflow guidance is discarded at the actual small/font2 viewport. The inspector/Done retains exit and accessible element labels retain selection; this is a discoverability limitation, not missing keyboard functionality.

**Action:** Wrap guidance, show context-sensitive short instructions, or expose a focusable Help action with the complete shortcuts. Keep the selected name stable and avoid reintroducing box annotations or covering the inspector.

**Acceptance:** At320×240/font2 with a long selected name, a user can discover fine/coarse nudge, grid and save/exit without external docs. Full help is available to pointer and keyboard. Capture selected and unselected states and verify the help does not overlap controls.

## Motion, economy and broader surface limits

The early/reversed/settled0026–0028 captures support distinct pictured poses; early faint content is intentional transition evidence, not proof of permanent low contrast. The seven temporal assertions do not establish smoothness. `KeyboardNavigation.java:52` excludes opacity<.95, while ClickGUI pointer input becomes available at opening>=.5 (`ClickGui.java:482`). ColorPicker also gates expanded-child input until late expansion. These policies can produce different input availability during user-configured slow motion; they require timed interaction evidence before being classified as a reproduced regression. Next acceptance should attempt Tab/activation/pointer edits during opening, page navigation, expansion and interrupted reversal, with default/slow settings and Reduced Motion. Record focus continuity and availability separately from frame times.

Appearance still builds filtered lists/layout rows in extraction (`ThemeEditor.java:173,189`), and cosmetics filters definitions during rendering (`CosmeticsEditor.java:125`). Resource card/Selected paint skipping and stable actions reduce avoidable work, but catalog iteration and logical focus collection still exist. No visible slowdown or allocation/FPS improvement is inferred from source or the HUD counter.

Coverage sampled main ClickGUI/module/search/sidebar; Appearance/client Sounds/Discord/Scale/Performance and page appearance; shared text/slider/color/choice/block controls; Resources catalog/details/Selected/Tools/status; cosmetics browse/edit/pose/acquisition routes; HUD/Glass; Profiles/Friends; world map/waypoint manager/editor; native theme/button/gear integration. The compact rail has full hover/focus page names (`Sidebar.java:94–116`), so icons alone are not a revival of the original inaccessible Settings issue. World map retains pan/zoom keys but its status teaches Shift+wheel more clearly (`map/WorldMapScreen.java:66–67,91`); comprehensive small-font2 map operations still need evidence. NativeButtonLayout broadly compacts noncontainer widgets (`ui/menu/NativeButtonLayout.java:17–24`); selected picker captures do not prove all vanilla/third-party menus.

Still unverified: full resource loading/paused/cache-completion/failure permutations; compact cosmetic import/preview/acquisition; profile overwrite/load-failure recovery; long-list/filter/history/external-mutation combinations; live exploration and multiplayer; real container remote close/abandoned-child lifetime; optional renderers; audible narration; complete localization/contrast across arbitrary imagery; and measured performance. Partial German scenes are not universal localization coverage.

Prioritize compact cosmetic acquisition and safe overlay placement, then explicit profile replacement recovery. Add targeted journeys for these gaps while retaining existing checks and honest assertion counts. The recent six fixes should remain closed in their demonstrated scope.
