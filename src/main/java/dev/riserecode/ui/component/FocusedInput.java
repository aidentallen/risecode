package dev.riserecode.ui.component;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** One keyboard owner across all custom settings contexts. */
public final class FocusedInput {
    private static TextField active;
    private FocusedInput() { }
    static void focus(TextField field) {
        if (active == field) { return; }
        if (active != null) { active.commit(); }
        active = field;
    }
    static void release(TextField field) { if (active == field) { active = null; } }
    public static boolean active() { return active != null; }
    public static boolean owns(TextField field) { return active == field; }
    public static void pointer(double x, double y) { if (active != null && !active.contains(x, y)) { active.commit(); } }
    public static void commit() { if (active != null) { active.commit(); } }
    public static void cancel() { if (active != null) { active.cancel(); } }
    public static boolean key(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) {
        if (active == null) { return false; }
        TextField field = active;
        if (event.isEscape()) { field.cancel(); }
        else if (event.isConfirmation()) { field.commit(); }
        else { field.keyPressed(event, clipboard, copy); }
        return true;
    }
    public static boolean character(CharacterEvent event) { if (active == null) { return false; } active.charTyped(event); return true; }
}
