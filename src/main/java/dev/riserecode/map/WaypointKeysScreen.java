package dev.riserecode.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.RiseRecodeClient;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Every waypoint-related key in one place: click a key to rebind it, Backspace clears it, Esc cancels. */
public final class WaypointKeysScreen extends Screen {
    private static final int ROW = 28;
    private record Hit(double x, double y, double width, double height, Runnable action) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private KeyMapping listening;

    public WaypointKeysScreen(Screen parent) { super(Component.literal("Waypoint keybinds")); this.parent = parent; }

    private int left() { return width / 2 - Math.min(200, width / 2 - 10); }
    private int paneWidth() { return width - left() * 2; }

    private static String keyName(KeyMapping mapping) { return mapping.isUnbound() ? "None" : mapping.getTranslatedKeyMessage().getString(); }

    private void button(GuiGraphicsExtractor g, double x, double y, double w, double h, String label, boolean active, int mx, int my, Runnable action) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        RenderUtil.drawRoundedRect(g, x, y, w, h, Math.min(8, h / 2), active ? Theme.opacity(Theme.accent(), hover ? 0.7 : 0.5) : hover ? 0x40FFFFFF : 0x28FFFFFF);
        double scale = 0.85, textWidth = RenderUtil.textWidth(font, label, false) * scale;
        RenderUtil.text(g, font, label, x + (w - textWidth) / 2, y + (h - 9 * scale) / 2 + 0.5, scale, Theme.TEXT);
        hits.add(new Hit(x, y, w, h, action));
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xB30F1115);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        hits.clear();
        var keys = RiseRecodeClient.waypointKeys();
        int left = left(), w = paneWidth(), top = Math.max(12, (height - (keys.size() * ROW + 96)) / 2), panelHeight = keys.size() * ROW + 96;
        RenderUtil.drawSurfaceTransition(g, left, top, w, panelHeight, 12, 0, 1);
        RenderUtil.text(g, font, "Waypoint keybinds", left + 14, top + 12, 1.2, Theme.TEXT);
        RenderUtil.text(g, font, "Click a key to change it · Backspace clears · Escape cancels", left + 14, top + 30, 0.75, Theme.SECONDARY);
        for (int i = 0; i < keys.size(); i++) {
            var mapping = keys.get(i);
            double y = top + 46 + i * ROW;
            RenderUtil.text(g, font, Component.translatable(mapping.getName()).getString(), left + 16, y + 7, 0.95, Theme.TEXT);
            boolean waiting = listening == mapping;
            button(g, left + w - 160, y, 100, 22, waiting ? "Press a key..." : keyName(mapping), waiting, mouseX, mouseY, () -> listening = waiting ? null : mapping);
            button(g, left + w - 54, y, 40, 22, "Reset", false, mouseX, mouseY, () -> assign(mapping, mapping.getDefaultKey()));
        }
        double footY = top + 46 + keys.size() * ROW + 8;
        button(g, left + 14, footY, w - 28, 22, "Done", true, mouseX, mouseY, this::onClose);
    }

    private void assign(KeyMapping mapping, InputConstants.Key key) {
        mapping.setKey(key); KeyMapping.resetMapping(); minecraft.options.save();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listening != null) { listening = null; return true; }
        if (event.button() == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).contains(event.x(), event.y())) { hits.get(i).action().run(); return true; }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (listening != null) {
            if (event.key() != 256) { assign(listening, event.key() == 259 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(event.key())); }
            listening = null; return true;
        }
        return super.keyPressed(event);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
