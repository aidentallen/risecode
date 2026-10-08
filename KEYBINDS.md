# Module keybindings — 0.7.2

Every module has the same keybind editor. Right-click its card, scroll to the
bottom of the settings, click the current key (initially `NONE`), then press the
new key. The active ClickGUI key is reserved and displays an explanation if
selected. Capture consumes keyboard input so assigning a key cannot accidentally
toggle the module, type in search, or close the GUI.

- **Toggle:** each press switches the module on or off. Repeats are ignored.
- **Hold:** pressing temporarily enables a disabled module; releasing disables
  it and runs its ordinary cleanup. An already manually enabled module stays on.
- **Reset:** clears the binding to `NONE`, keeping the selected activation mode.
- **Escape:** cancels capture. **Delete/Backspace:** clears the captured binding.

FreeLook defaults to **Left Alt + Hold** and works without first enabling it in
the GUI. It also supports Toggle activation. Clearing its key disables its old
hidden fallback; a reset binding cannot unexpectedly reactivate on Left Alt.
Other modules start unbound so no arbitrary keys conflict with gameplay controls.

The command format is `.bind <module> <key|NONE> [hold|toggle]`, for example
`.bind FreeCam F8 hold`. Omitting the mode keeps the module's current choice.

## Runtime and persistence

`KeybindManager` feeds raw `KeyPressEvent` input into `KeybindController`. The
controller owns temporary activations and releases them on key-up, missed release
callbacks, focus loss, open menus/overlays, death, disconnects, rebinds, activation
mode changes, profile loading and shutdown. It runs on the client thread through
the existing EventBus. Module lifecycle and subscription ownership stay intact.

Each module stores `keybind` and `bindMode` in `config/riserecode/config.json` and
named profiles. A temporary Hold activation is saved as disabled, so saving while
a key is pressed cannot make that module permanently enabled on the next launch.
Older configs default to Toggle, except FreeLook's previous `HoldKey` setting is
migrated into its shared Hold binding. Explicit `NONE` in the new format remains
unbound.

## Verification

`./gradlew keybindCheck` exercises 24 meaningful checks of independent keys,
press/repeat/release behavior, lifecycle cleanup, persistence and legacy migration.
`./gradlew runClientGameTest -PkeybindOnly` runs 31 native OpenGL checks covering
the actual ClickGUI editor, key capture, both activation modes, Reset, profile
loading, the command and FreeLook camera restoration. Full regression evidence
and screenshots are stored in `build/verification/keybinds/`.
