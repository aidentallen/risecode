package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;

/** Visible controls register in draw order, using transformed bounds and the active clip. */
public final class KeyboardNavigation {
    public interface PageScrollable { void scrollPage(int direction); }
    public interface ManagedScreen { KeyboardNavigation riserecode$keyboardNavigation(); }
    public static KeyboardNavigation auxiliary(net.minecraft.client.gui.screens.Screen screen) {
        if (screen == null || !screen.getClass().getName().startsWith("dev.riserecode.ui.") || screen instanceof dev.riserecode.ui.clickgui.ClickGui) { return null; }
        return screen instanceof ManagedScreen managed ? managed.riserecode$keyboardNavigation() : null;
    }
    private record Entry(Object id, Supplier<String> label, Runnable activate, IntConsumer adjust, Runnable reveal) { }
    private record ScrollRegion(java.util.function.Consumer<UIComponent.Bounds> reveal) { }
    private static final java.util.Deque<ScrollRegion> regions=new java.util.ArrayDeque<>();
    public static void pushScrollRegion(java.util.function.Consumer<UIComponent.Bounds> reveal){regions.push(new ScrollRegion(reveal));}
    public static void popScrollRegion(){regions.pop();}
    public static boolean collectingOffscreen(){return collecting!=null&&collecting.focused!=null;}
    public void pointerUsed(){focused=null;}
    private static KeyboardNavigation collecting;
    private final List<Entry> entries = new ArrayList<>();
    private Object focused;
    public void begin() { entries.clear(); regions.clear(); collecting = this; }
    public void end() { collecting = null; regions.clear(); }
    public String focusedLabel() {
        return entries.stream().filter(entry -> entry.id() == focused).findFirst().map(entry -> entry.label().get()).orElse("");
    }
    public List<String> labels() { return entries.stream().map(entry -> entry.label().get()).toList(); }
    public void clear() { entries.clear(); focused = null; }
    public static void control(GuiGraphicsExtractor graphics, Object id, UIComponent.Bounds bounds, double opacity,
            Supplier<String> label, Runnable activate, IntConsumer adjust) {
        var navigation = collecting;
        if (navigation == null || opacity < .95 || bounds.width() <= 0 || bounds.height() <= 0) { return; }
        var rectangle = new ScreenRectangle((int)Math.floor(bounds.x()), (int)Math.floor(bounds.y()),
                (int)Math.ceil(bounds.width()), (int)Math.ceil(bounds.height())).transformMaxBounds(graphics.pose());
        var clip = graphics.scissorStack.peek();
        if (regions.isEmpty() && clip != null && (rectangle.left() < clip.left() || rectangle.right() > clip.right()
                || rectangle.top() < clip.top() || rectangle.bottom() > clip.bottom())) { return; }
        var region=regions.peek();
        navigation.entries.add(new Entry(id,label,activate,adjust,region==null?()->{}:()->region.reveal().accept(bounds)));
        if (navigation.focused == id) {
            RenderUtil.drawRoundedBorder(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 3, Theme.accent(), 1);
        }
    }
    private void announce(Entry entry) { Minecraft.getInstance().getNarrator().saySystemNow(entry.label().get()); }
    public boolean key(KeyEvent event) {
        if (entries.isEmpty()) { return false; }
        int index = -1;
        for (int i = 0; i < entries.size(); i++) { if (entries.get(i).id() == focused) { index = i; break; } }
        if (event.key() == 258 && !event.hasControlDown()) {
            FocusedInput.commit();
            index = Math.floorMod(index < 0 ? (event.hasShiftDown() ? entries.size()-1 : 0) : index + (event.hasShiftDown() ? -1 : 1), entries.size());
            focused = entries.get(index).id(); entries.get(index).reveal().run(); announce(entries.get(index)); return true;
        }
        if (index < 0 || FocusedInput.active()) { return false; }
        Entry entry = entries.get(index);
        if (event.isConfirmation() || event.key() == 32) { entry.activate().run(); announce(entry); return true; }
        if (entry.adjust() != null && (event.key() >= 262 && event.key() <= 265)) {
            entry.adjust().accept(event.key() == 262 || event.key() == 265 ? 1 : -1); announce(entry); return true;
        }
        return false;
    }
}
