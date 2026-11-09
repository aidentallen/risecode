# Isolated native UX review

Launch `runClientGameTest -PuxInteractiveOnly -PuxInteractiveDir=/tmp/unique-review-dir -PresourcesOffline -PbundledFontsOnly`. This is a disposable singleplayer test instance, not the installed Modrinth client. The session stops after20minutes or a `finish` action. The test-only teardown checks that only the world owner is connected. The fixture uses Java/native rendering, VSync off, a60cap with themed-menu uncapping disabled, and an offline resource catalog. It is not a GPU/display-latency benchmark or human usability study.

Call `python3 scripts/ux-review-session.py --dir /tmp/unique-review-dir state` and view the returned screenshot with `view_image`. Coordinates are screenshot pixels, not GUI-scaled coordinates. Send one JSON command at a time; each returns a fresh screenshot/state. The script never opens the user's instance or sends messages to other people.

- `{"action":"move","x":400,"y":300}`: move without clicking, including hover inspection.
- `{"action":"click","x":500,"y":350,"button":0}`: native test mouse press/release.
- `{"action":"key","key":258}`: key press/release. GLFW codes:Tab258,Enter257,Escape256,Backspace259,arrows262–265,PageUp266,PageDown267,RightShift344. Modifier flags may be supplied explicitly:Shift1,Ctrl2,Alt4. Fabric's helper does not infer real device modifier state.
- `{"action":"type","text":"example"}`: character input into the focused control. Ctrl+A then Backspace clears an existing field.
- `{"action":"scroll","x":500,"y":500,"amount":-3}`: scroll down at a location (positive is up).
- `{"action":"drag","x":500,"y":400,"dx":100,"dy":0,"steps":10}`: held native test mouse movement, one tick per step.
- `{"action":"resize","width":1440,"height":1080,"guiScale":3}`: actual window/GUI resize.
- `{"action":"preset","name":"compact"}` / `normal`: test-fixture geometry/text presets, preserving current page/draft; not an ordinary-user action. Compact960×720/GUI3/font2/360×240 reference window; normal1280×960/GUI2/font1/428×320.
- `{"action":"wait","ticks":10}` / `snapshot`: wait/render then capture. Most commands default to3ticks; ticks is clamped1–40. Timings include harness scheduling and are not physical-input latency.
- `{"action":"finish"}`: capture the last state, then stop the isolated client/world.

No widget tree or solution path is exposed. Record first-use task attempts and concrete confusion separately from later source audit. Inputs are synthetic native callbacks; screenshots demonstrate pixels at sampled times, not continuous display smoothness. `commands/`, `frames/` and `observations.jsonl` preserve the trace outside Git.
