# Independent UX/UI review 1

Baseline: `6d1afc0ad5e021f7506c641c88af965557f4fd72`, reviewed 2026-11-08. Checkout was clean when inspected. Read AGENTS.md and AGENTS.local.md; standing installation/publication instructions do not apply to this read-only review. No checkout files changed, no builds/installations/publication, no agent delegation.

## Evidence boundary

This is a **static interaction/layout review** of current source. I did not open Minecraft, exercise the UI, inspect fresh screenshots, measure contrast, run a usability study, or measure frame time. Scores describe implementation support and identifiable usability gaps; they do not certify rendered visual quality, animation smoothness, accessibility compliance, or performance. Historical performance text in docs/gui-rendering.md was seen but is not fresh execution evidence. Source line numbers below refer to the baseline above.

A preliminary suspicion that HUD context animations were not advanced was investigated and **retracted**: Animation.java:80 registers themed animations centrally; AnimationFrameMixin.java:20 drives AnimationManager each frame. Missing local update calls are not a demonstrated defect.

## Scoring rubric

Each category uses the same 0–10 scale: **0** absent; **2** usually blocks the task; **4** partial implementation requiring significant discovery/workarounds; **6** usable for a familiar user with material gaps; **8** clear, consistent support for common tasks and edge cases; **10** excellent support demonstrated across ordinary users, screen sizes, inputs, and failure states. Scores are evidence bounded, not a claim of user testing. Equal-weight average: **5.7/10**.

| Category | /10 | Meaning and baseline evidence |
|---|---:|---|
| Navigation and orientation | 6 | Category sidebar and Ctrl+Tab work, selected state is retained; settings utilities hide behind a player-name footer; HUD returns to gameplay instead of its caller. ClickGui.java:110–116,177–190,520–525; ClientSettingsPopup.java:58–61; HudEditorScreen.java:162. |
| Layout and responsiveness | 5 | Clipping, scrolling, fit-to-screen scaling and action flow exist, but the whole window shrinks; fixed-width subtoolbars can overlap at legal sizes. ClickGui.java:211–243; ResourcesEditor.java:136–139; ButtonMetrics.java:13–18. |
| Hierarchy and readability | 6 | Titles/descriptions/section headers establish hierarchy; tiny secondary scales, extensive ellipsis, and compressed option labels reduce comprehension. ModuleButton.java:138–157; CosmeticsEditor.java:123,130–131; EnumSelector.java:45–52. Pixel contrast unmeasured. |
| Motion and transitions | 7 | Central animation scheduling, independent channels, reversible closing, and Reduced Motion are strong foundations. Search typing replays entrance motion on every query and nested scale/fade effects need live tuning. AnimationManager.java:17–27; Animation.java:32–34,65–91; Panel.java:49–54; ClickGui.java:549–554. Smoothness unmeasured. |
| Feedback and recovery | 6 | Notices, loading/progress state, numeric validation and profile-delete confirmation exist. Three-second truncated notices, disabled actions without reasons, and direct waypoint deletion weaken recovery. ClickGui.java:324–339,386–400; ResourcesEditor.java:63,190–199; WaypointScreen.java:40. |
| Keyboard and accessibility | 5 | Shared focus ring/narration, sliders and text editing support real keyboard operation. Map/waypoint classes are excluded from auxiliary navigation, settings footer has no focus registration, and navigation registers only fully visible controls. KeyboardNavigation.java:18–20,36–41,48–62; ClientSettingsPopup.java:54–82. |
| Discoverability and ordinary-user usability | 4 | Search and descriptive module cards help, but left-click toggles/right-click settings are invisible, settings are disguised as account identity, and choice controls hide available alternatives. ModuleButton.java:156–157,190–197; ClientSettingsPopup.java:58–61; EnumSelector.java:55–62. |
| Consistency | 6 | Shared components, metrics, theme and motion channels cover many surfaces. Module headers, disclosure cards and cycling enums use different click semantics; save behavior varies by editor. ModuleButton.java:191–197; SettingsSection.java:42–52; ResourcesEditor.java:63–77; ThemeEditor.java:93–97. |
| Settings workflow | 5 | Live previews, groups, exact numeric entry, preset undo and resource ordering are useful. No settings search, shallow explanation of technical choices, and inconsistent dirty-state/commit semantics create avoidable work. Panel.java:58–60; ThemeEditor.java:99–116; ClickGui.java:183,530,562–565. |
| Perceived performance and implementation economy | 7 | Viewport culling, cached filters/layouts, ordered batches and centralized animation scheduling are sound foundations. Query changes replay all entrances, Appearance recalculates row structures each frame, and cosmetics filters definitions each frame. Panel.java:45,81–94; ClickGui.java:222–228; ThemeEditor.java:145–175; CosmeticsEditor.java:113. CPU/GPU/frame-time impact unmeasured. |

## Surface coverage

- **Main ClickGUI and module categories:** reviewed geometry, search filtering, card expansion/toggle, scroll, scaling, key handling and notices in ClickGui, Panel, Sidebar and ModuleButton.
- **Appearance and per-page appearance:** reviewed groups, section disclosures, presets/reset/undo, imports, control visibility, page editor layout and exit in ThemeEditor, SettingsSection, PageAppearanceScreen and ThemeManager.
- **Settings footer, Sounds, Discord, GUI Scale and Performance:** reviewed menu trigger, row semantics, keys and reuse of canonical settings in ClientSettingsPopup, ClientSettingsEditor and ClientSettingsScreen.
- **Resources:** reviewed search/category/selected/tools toolbar, card details, draft/apply/cancel, conflict feedback, progress, selected order and keyboard reorder in ResourcesEditor.
- **Cosmetics:** reviewed browse/catalog/category, preview rotation/zoom, pose, downloads/import path, editing and draft lifecycle in CosmeticsEditor.
- **HUD editor:** reviewed element selection, drag/snap, context controls, hint bar, scrolling and exit in HudEditorScreen.
- **Glass material editor:** reviewed preview/control split, section expansion, reset, keyboard preview axes and scroll in GlassEditorScreen. Its explicit gameplay-enabled versus preview-only status is a useful pattern.
- **Block picker:** reviewed live search, selected IDs, visible rows, keyboard registration and return in BlockPickerScreen.
- **World map, waypoint list and waypoint editor:** reviewed toolbar layout, map pan/zoom/altitude, add/edit/delete, validation, exit and keyboard limits in their three screen classes.
- **Profiles and Friends:** reviewed save/load/delete confirmation, action flow and direct friend removal in ClickGui.
- **Vanilla screens/title/container integration:** sampled MenuScreenMixin, MenuTheme, NativeButtonLayout, PageGear, MenuTransition and related keyboard hooks. Native gameplay screens retain their native widget model, while broad native-button compaction deserves compatibility/layout verification; I did not audit every vanilla or third-party screen.

## Prioritized actionable findings

### P1 / U1 — Make module activation and opening settings visibly different actions

**Static evidence:** ModuleButton.java:129–131 exposes enabled/expanded state to narration, but renderCard at :138–157 renders a classification badge, name and one-line description without an explicit switch or expansion chevron. :191–197 toggles gameplay on left-click and expands settings on right-click. SettingsSection.java:42–45 instead shows “Click to expand” and a chevron.

**User impact:** A newcomer trying to inspect a module can activate gameplay behavior accidentally. The current header gives no persistent visible instruction for opening settings or checking on/off state without relying on accent color.

**Fix:** Add a separate visible On/Off control and a Settings/chevron disclosure target. Keep right-click as a shortcut; make keyboard focus distinguish both controls and narrate each action. Preserve the useful classification badge but avoid letting it consume the state/affordance area.

**Acceptance:** From a clean profile, a first-time user can inspect settings without enabling the module; pointer and keyboard can independently toggle and disclose; enabled state remains obvious in a monochrome capture.

### P1 / U2 — Expose settings by name and make its footer keyboard reachable

**Static evidence:** ClientSettingsPopup.java:58–61 draws a player icon, username and ›. Actual content is Profiles, Friends, Client Sounds, Command Suggestions, Discord Activity, Menu Keybind, GUI Scale and Performance at :49–53. Its trigger and rows do not call KeyboardNavigation.control; :106–111 only handles keys after it is already open. Sidebar likewise does not register regular focus targets, although Ctrl+Tab provides page traversal.

**User impact:** Everyday settings are concealed behind an account-like control. Keyboard users cannot discover/open that control through normal Tab traversal.

**Fix:** Label the footer “Settings” with a gear; keep identity secondary. Register its trigger and popup rows with explicit accessible labels/current values; display the menu shortcut. Add a clearly marked advanced-details chevron for Sounds/Discord, whose left-click currently toggles while right-click or a narrow 16-unit edge opens details (:90–102).

**Acceptance:** Tab can open Settings and reach every row; all actions are available without right-click; a screenshot makes the location of GUI Scale and menu binding obvious.

### P1 / U3 — Protect drafts and destructive waypoint operations

**Static evidence:** ClickGui.java:183 discards cosmetics when changing pages, and :562–565 discards resources and cosmetics when closing/removed. CosmeticsEditor.java:58 has Apply/Cancel, but no dirty indicator or exit warning. WorldMapScreen.java:46 and WaypointScreen.java:22 add a waypoint before opening its editor; WaypointEditScreen.java:23 Cancel only returns, so creation has already occurred. WaypointScreen.java:40 directly removes a waypoint; unlike client-profile deletion at ClickGui.java:386–400, there is no confirmation/undo.

**User impact:** Users can lose a wardrobe/resource edit accidentally, and “Cancel” during waypoint creation does not cancel creation. A destructive click has no obvious recovery.

**Fix:** Track changed drafts and show “Unapplied changes”; confirm Apply/Discard/Stay only when leaving a dirty editor. Create new waypoints as uncommitted drafts and insert on Save. Add short undo for deletion; reserve a confirmation for bulk clear. Label live-save editors “Changes save automatically”.

**Acceptance:** Cancel new waypoint leaves the store unchanged; navigating away from a changed outfit/resources choice cannot silently lose edits; delete can be undone and retains all waypoint fields.

### P1 / U4 — Extend keyboard support to map management and restore a useful Back path

**Static evidence:** KeyboardNavigation.auxiliary at :19 explicitly accepts only class names beginning dev.riserecode.ui.; map screens live in dev.riserecode.map. WorldMapScreen.java:32–38,61 uses ActionButton instances but :88 handles only Escape; WaypointScreen.java:22–26,35–40,48–55 similarly has no native widgets or navigation routing. Native EditBoxes in WaypointEditScreen do provide native focus. Client.java:190 opens HUD without passing a parent; HudEditorScreen.java:162 exits to null.

**Fix:** Include map screens in a deliberate managed-screen contract, register/map focus, provide keyboard pan/zoom/add/edit/remove and Page Up/Down list traversal. Pass the originating screen into HUD editor and add a visible Done/Back action.

**Acceptance:** Without touching the mouse, open map, recenter, zoom, add/save/edit/delete a waypoint, return to caller; HUD Done returns to HUD settings. Preserve native EditBox traversal.

### P2 / U5 — Resolve real toolbar overlap before adding visual polish

**Static evidence and exact geometry:** Allowed Window Width minimum is 360 and Content Padding maximum 20 (ThemeManager.java:141,143). ClickGui.java:232 gives content width 360 − 107 − 40 = **213**. ResourcesEditor.java:137 enforces a minimum search width **40**, while category starts at content x + 213 − 70 − 74 − 42 = **x+27** (:138), creating a **13 logical-unit overlap**. This is a direct arithmetic consequence, not a screenshot observation. ClickGui fittedScale at :211–212 fits the whole logical window, which preserves rather than fixes internal collisions.

**Fix:** Measure toolbar contents and wrap to a second row below a minimum width; derive body top/height from toolbar height. Use a minimum readable text/control scale and scroll/reflow instead of continuously shrinking the complete UI. Check Cosmetics two-column preview at small window sizes and the six-column waypoint toolbar (WaypointScreen.java:48).

**Acceptance:** At 360×240 logical window, padding 20, Font Scale 0.75/1/2 and supported GUI scales, toolbar bounds do not intersect, active controls remain reachable, preview/list contents stay inside clips. Capture representative wide and narrow native windows.

### P2 / U6 — Search settings and provide useful explanations in context

**Static evidence:** Panel.java:58–60 searches module names/descriptions/category only. ThemeEditor groups every control but has no search field. ClickGui.java:530 sends Ctrl+F from Appearance to module Search; only Resources has a scoped search. ThemeManager has extensive technical controls such as refraction, gradients, typography, per-family/per-channel animation overrides (:90–197). Setting descriptions exist in models, but Slider/EnumSelector/ToggleSwitch primarily draw labels/values.

**Fix:** Give Appearance and client settings a scoped search with group breadcrumbs and clear/reset; add a global command-style search that can jump to setting controls. Surface concise descriptions/range/units on focus or hover. Keep common controls prominent and move animation channel overrides into Advanced. Choose a specific subsection and reveal it, following ThemeEditor.reveal (:138–141).

**Acceptance:** Searching “reduced motion”, “blur”, “scale”, or “click volume” reaches the control without knowing its group; focus shows the same explanation as hover; no ordinary action requires docs.

### P2 / U7 — Replace blind option cycling with an inspectable choice list

**Static evidence:** EnumSelector.java:41,55–62 cycles forward on activation/left click and backward on right click; rendered label only exposes the current option (:45–52). Cosmestic category uses the same cyclic pattern at CosmeticsEditor.java:54. Choices do not show the number or names of alternatives before changing them.

**Fix:** Use a dropdown/listbox for multi-choice settings with a visible chevron, named selected value and keyboard arrows/typeahead; use segmented controls for two or three short options. Keep cycling as an optional shortcut.

**Acceptance:** A user can inspect alternatives without applying an intermediate choice; Escape restores current selection; keyboard opens, navigates and confirms predictably.

### P2 / U8 — Make feedback persist where an error or blocked action needs attention

**Static evidence:** ClickGui.java:337–339 gives every notice three seconds; :328–332 truncates to at most 292 logical units and displays at 0.8 scale. TextField.java:78–80 puts a small validation line over the bottom of its own field and only while focused. ResourcesEditor.java:63 disables Apply until cache readiness without a direct reason at that control. WaypointEditScreen.java:20 uses one broad “Check coordinates, dimension and six-digit color” error for every failure.

**Fix:** Keep transient success toasts, but put validation beside the exact field, retain errors until corrected, and explain disabled Apply near the footer. Add stable loading/failed/empty states with a retry action. Distinguish harmless overlap from incompatible resource choices with named conflicts.

**Acceptance:** Invalid coordinate/color identifies the specific field; error text survives focus movement; users understand how to enable Apply; long status text can be read in full.

### P2 / U9 — Use motion to preserve orientation, reduce replay during typing

**Static evidence:** Panel.setQuery at :51–54 resets scroll and calls open on every changed string; open at :49 calls enter on every module button. ModuleButton.java:124–126 derives scale/fade from entry age. Rapid search therefore repeatedly restarts card entrance while typing. Main open scale, page shift/fade and card scale can compose (ClickGui.java:283–309,435–438).

**Fix:** Animate page/list entry once, preserve existing result identity during query changes, and fade only newly inserted results with short limits. Keep resize/drag/scroll positional consistency; ensure Reduced Motion bypasses decorative transitions. Tune the composed result live at 60/144 Hz and with slower animation settings.

**Acceptance:** Typing a ten-character query does not repeatedly scale/fade persistent matches or hide focus. Reduced Motion makes controls available immediately. Animation timings are tested in a native client, not inferred solely from easing code.

### P3 / U10 — Improve density, text scaling and affordance consistency as one design pass

**Static evidence:** Secondary labels can be .52–.62 scale (CosmeticsEditor.java:123; ModuleButton.java:139–141); EnumSelector and Slider shrink long labels to fit (:47 / :72); Panel scrollbar is 1.5 units with opacity .25 (:100–103), and no drag interaction exists (:106–125). NativeButtonLayout.java:20–24 changes actual native widget dimensions to shared 17-unit height; this changes target size across vanilla menus as well as appearance.

**Fix:** Establish minimum text sizes, full descriptions on focus/hover, consistent section/title/body spacing, explicit switches/chevrons and larger pointer targets around compact visuals. Offer Comfortable/Compact density and preserve native layout slots when broad compaction would harm reachability. Make scrollbar thumb draggable and offer Home/End/page movement.

**Acceptance:** Long English/German labels and Font Scale 2 remain readable without silent miniaturization; target rectangles and visual controls match; low-vision and keyboard checks cover every custom screen. Verify contrast over the actual photo/glass backgrounds instead of assuming palette constants are sufficient.

## Recommended implementation order and verification

1. U1/U2/U3/U4 establish safe, discoverable actions and navigation.
2. U5/U6/U8 establish responsive layout, findability and recoverable forms.
3. U7/U9/U10 standardize choices, motion and density.

Use targeted deterministic checks for arithmetic layout, draft lifecycles, and keyboard registration. Then capture and interact with native UI at multiple window sizes, GUI/font scales, long localized labels, empty/loading/failure states, and Reduced Motion. Exercise module inspection without activation, draft leave/apply/discard, profile delete cancellation, resource reorder/cache error, new waypoint Cancel, HUD return, and keyboard-only traversal. Compare CPU allocation/frame timing only with repeatable fresh runs and identify active renderer/world load. Do not treat historical receipts or functional tests as proof of visual polish or FPS.

## Memory consulted

A quick pass over /home/toast/.codex/memories/MEMORY.md:48–59 informed evidence boundaries and historical accessibility context. Current findings and scores above were established from the inspected source, not copied from earlier review scores.
