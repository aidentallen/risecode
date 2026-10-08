package dev.riserecode.keybind;

import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/** Client-thread activation state, separate from Minecraft input and GUI rendering. */
public final class KeybindController implements AutoCloseable {
    private final ModuleManager modules;
    private final Set<Integer> pressed = new HashSet<>();
    private final Map<Module, Integer> temporary = new HashMap<>();
    public KeybindController(ModuleManager modules) {
        this.modules = modules; modules.setBindingChangeListener(this::release);
    }
    public void press(int key, boolean allowed) {
        if (!allowed || key == Module.UNBOUND || !pressed.add(key)) { return; }
        for (Module module : modules.getModules()) {
            if (module.getKeybind() != key) { continue; }
            if (module.getBindMode() == Module.BindMode.TOGGLE) { modules.toggle(module); }
            else if (!module.isEnabled()) {
                // Record ownership before onEnable: a synchronous save must see the permanent state.
                temporary.put(module, key);
                if (!modules.setEnabled(module, true)) { temporary.remove(module); }
            }
        }
    }
    public void releaseKey(int key) {
        pressed.remove(key);
        for (Module module : Set.copyOf(temporary.keySet())) {
            if (temporary.get(module) == key) { release(module); }
        }
    }
    public void release(Module module) {
        if (temporary.remove(module) != null) { modules.setEnabled(module, false); }
    }
    public boolean isTemporarilyEnabled(Module module) { return temporary.containsKey(module); }
    public boolean persistentEnabled(Module module) { return module.isEnabled() && !isTemporarilyEnabled(module); }
    /** Polling also handles a lost release callback, screen/focus changes, disconnects and direct rebinds. */
    public void update(boolean allowed, IntPredicate keyDown) {
        for (Module module : Set.copyOf(temporary.keySet())) {
            int key = temporary.get(module);
            if (!allowed || !keyDown.test(key) || module.getKeybind() != key
                    || module.getBindMode() != Module.BindMode.HOLD || !module.isEnabled()) { release(module); }
        }
        if (!allowed) { pressed.clear(); }
    }
    @Override public void close() { temporary.keySet().stream().toList().forEach(this::release); pressed.clear(); }
}
