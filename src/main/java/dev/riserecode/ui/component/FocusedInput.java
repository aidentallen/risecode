package dev.riserecode.ui.component;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** One keyboard owner across all custom settings contexts. */
public final class FocusedInput {
    public interface Owner {
        boolean contains(double x,double y);
        void commit(); void cancel();
        boolean charTyped(CharacterEvent event);
        boolean keyPressed(KeyEvent event,Supplier<String> clipboard,Consumer<String> copy);
        default void confirmation(KeyEvent event) { commit(); }
    }
    private static Owner active;
    private static java.util.function.DoubleUnaryOperator pointerX=v->v,pointerY=v->v;
    public static void pointerMapping(java.util.function.DoubleUnaryOperator x,java.util.function.DoubleUnaryOperator y){if(active!=null){pointerX=x;pointerY=y;}}
    private FocusedInput() { }
    static void focus(Owner field) {
        if (active == field) { return; }
        if (active != null) { active.commit(); }
        active = field;pointerX=v->v;pointerY=v->v;
    }
    static void release(Owner field) { if (active == field) { active = null;pointerX=v->v;pointerY=v->v; } }
    public static boolean active() { return active != null; }
    public static boolean owns(Owner field) { return active == field; }
    public static void pointer(double x, double y) { if (active != null && !active.contains(pointerX.applyAsDouble(x), pointerY.applyAsDouble(y))) { active.commit(); } }
    public static void commit() { if (active != null) { active.commit(); } }
    public static void cancel() { if (active != null) { active.cancel(); } }
    public static boolean key(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) {
        if (active == null) { return false; }
        Owner field = active;
        if (event.isEscape()) { field.cancel(); }
        else if (event.isConfirmation()) { field.confirmation(event); }
        else { field.keyPressed(event, clipboard, copy); }
        return true;
    }
    public static boolean character(CharacterEvent event) { if (active == null) { return false; } active.charTyped(event); return true; }
}
