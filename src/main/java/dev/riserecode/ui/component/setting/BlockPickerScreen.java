package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

/** Registry search with immediate selection and explicit preservation of unavailable IDs. */
public final class BlockPickerScreen extends Screen {
    private final Screen parent;
    private final BlockListSetting setting;
    private final Runnable changed;
    private final List<String> registry;
    private List<String> filtered;
    private String query = "";
    private int scroll;
    public BlockPickerScreen(Screen parent, BlockListSetting setting, Runnable changed) {
        super(Component.literal("Blocks")); this.parent = parent; this.setting = setting; this.changed = changed;
        var ids = new java.util.TreeSet<String>(); BuiltInRegistries.BLOCK.keySet().forEach(id -> ids.add(id.toString()));
        ids.addAll(setting.getValue()); registry = List.copyOf(ids); filtered = registry;
    }
    private void filter() { filtered = registry.stream().filter(id -> id.contains(query.toLowerCase(Locale.ROOT))).toList(); scroll = 0; }
    private int left() { return width / 2 - Math.min(180, width / 2 - 12); }
    private int paneWidth() { return width - left() * 2; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xB30F1115);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = left(), w = paneWidth();
        RenderUtil.drawSurfaceTransition(graphics, left, 12, w, height - 24, 12, 0, 1);
        RenderUtil.text(graphics, font, "Select blocks", left + 12, 24, 1.2, Theme.TEXT);
        RenderUtil.text(graphics, font, "Search: " + query + "_", left + 12, 44, 1, Theme.SECONDARY);
        RenderUtil.text(graphics, font, setting.getValue().size() + " selected · Escape to return", left + 12, height - 28, 0.85, Theme.SECONDARY);
        RenderUtil.scissorPush(graphics, left + 6, 64, w - 12, height - 104);
        try {
            int rows = Math.max(0, (height - 104) / 20);
            for (int i = 0; i < rows && i + scroll < filtered.size(); i++) {
                String id = filtered.get(i + scroll); boolean selected = setting.getValue().contains(id); int y = 64 + i * 20;
                if (mouseY >= y && mouseY < y + 20 && mouseX >= left && mouseX < left + w) { graphics.fill(left + 6, y, left + w - 6, y + 20, 0x18FFFFFF); }
                RenderUtil.text(graphics, font, (selected ? "● " : "○ ") + id, left + 12, y + 5, 0.9, selected ? Theme.accent() : Theme.TEXT);
            }
        } finally { RenderUtil.scissorPop(graphics); }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int rows = Math.max(0, (height - 104) / 20);
        if (event.button() == 0 && event.x() >= left() + 6 && event.x() < left() + paneWidth() - 6 && event.y() >= 64 && event.y() < 64 + rows * 20) {
            int index = (int) ((event.y() - 64) / 20) + scroll;
            if (index < filtered.size()) { setting.toggle(filtered.get(index)); changed.run(); } return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll = (int) Math.clamp(scroll - Math.signum(vertical) * 3, 0, Math.max(0, filtered.size() - Math.max(1, (height - 104) / 20))); return true;
    }
    @Override public boolean charTyped(CharacterEvent event) {
        if (query.length() < 128 && !Character.isISOControl(event.codepoint())) { query += new String(Character.toChars(event.codepoint())); filter(); } return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == 259 && !query.isEmpty()) { query = query.substring(0, query.offsetByCodePoints(query.length(), -1)); filter(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
