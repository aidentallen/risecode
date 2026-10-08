package dev.riserecode.ui.component;

import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Clipped, smoothly scrolling module column. Window movement belongs to the screen host. */
public final class Panel extends UIComponent {
    private final Category category;
    private final ModuleManager manager;
    private final Runnable changed;
    private final List<ModuleButton> buttons = new ArrayList<>();
    private final Animation opening = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION, 0, 0.2, MathUtil.Easing.OUT_QUART);
    private final Animation scroll = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.18, MathUtil.Easing.OUT_QUART);
    private long registryRevision = -1;
    private List<ModuleButton> filtered = List.of();
    private boolean filterDirty = true;
    private String query = "";
    private double maxScroll;
    private ModuleButton captured;

    /** A null category represents the global Search page. */
    public Panel(Category category, ModuleManager manager, Runnable changed) {
        this.category = category; this.manager = manager; this.changed = changed; syncModules();
    }
    public Category getCategory() { return category; }
    public List<ModuleButton> getModuleButtons() { syncModules(); return List.copyOf(buttons); }
    public List<ModuleButton> getVisibleButtons() { if (filterDirty) { filtered = buttons.stream().filter(this::matches).toList(); filterDirty = false; } return filtered; }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public boolean isOpen() { return !opening.isRunning() && opening.getValue() == 1; }
    public void open() { mouseReleased(0, 0, 0); opening.reset(0); opening.animateTo(1); }
    public void refresh() { mouseReleased(0, 0, 0); syncModules(); }
    public void setQuery(String value) {
        String next = value.strip().toLowerCase(Locale.ROOT);
        if (next.equals(query)) { return; }
        query = next; filterDirty = true; mouseReleased(0, 0, 0); scroll.reset(0); open();
    }
    private boolean matches(ModuleButton button) {
        String haystack = (button.getModule().getName() + " " + Theme.displayName(button.getModule().getName()) + " "
                + button.getModule().getDescription() + " " + button.getModule().getCategory().getDisplayName()).toLowerCase(Locale.ROOT);
        for (String word : query.split("\\s+")) { if (!haystack.contains(word)) { return false; } }
        return true;
    }
    private void syncModules() {
        if (registryRevision == manager.registryRevision()) { return; }
        registryRevision = manager.registryRevision(); filterDirty = true;
        List<Module> registered = category == null ? manager.getModules() : manager.getByCategory(category);
        buttons.removeIf(button -> !registered.contains(button.getModule()));
        for (Module module : registered) {
            if (buttons.stream().noneMatch(button -> button.getModule() == module)) {
                ModuleButton button = new ModuleButton(module, manager, changed); button.setShowCategory(category == null); buttons.add(button);
            }
        }
        buttons.sort(Comparator.comparing(button -> button.getModule().getName(), String.CASE_INSENSITIVE_ORDER));
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        syncModules();
        double amount = opening.update(seconds);
        List<ModuleButton> visible = getVisibleButtons();
        double content = 0;
        for (ModuleButton button : visible) { button.update(seconds); content += button.getPreferredHeight() + Theme.number("Content Padding", 8); }
        maxScroll = Math.max(0, content - Theme.number("Content Padding", 8) - height);
        if (scroll.getValue() > maxScroll) { scroll.reset(maxScroll); }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget()))); scroll.update(seconds);
        RenderUtil.scissorPush(graphics, x, y, width, height);
        try {
            double next = y - scroll.getValue() + (1 - amount) * 5 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.NAVIGATION);
            for (ModuleButton button : visible) {
                button.setBounds(x, next, width, button.getPreferredHeight());
                if (next + button.getPreferredHeight() >= y && next < y + height) { button.render(graphics, font, contains(mouseX, mouseY) ? mouseX : -1e6,
                        contains(mouseX, mouseY) ? mouseY : -1e6, seconds, opacity * amount); }
                next += button.getPreferredHeight() + Theme.number("Content Padding", 8);
            }
            if (visible.isEmpty()) {
                RenderUtil.text(graphics, font, Theme.label(query.isEmpty() ? "No modules in this category" : "No matching modules"),
                        x + 7, y + 9, 0.85, Theme.opacity(Theme.SECONDARY, opacity * amount));
            }
        } finally { RenderUtil.scissorPop(graphics); }
        if (maxScroll > 0) {
            double thumb = Math.max(14, height * height / (height + maxScroll));
            RenderUtil.drawRoundedRect(graphics, x + width - 2, y + (height - thumb) * scroll.getValue() / maxScroll,
                    1.5, thumb, 0.75, Theme.opacity(Theme.accent(), opacity * 0.25));
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        getVisibleButtons().forEach(child -> child.pointerFocus(mouseX, mouseY));
        if (!contains(mouseX, mouseY)) { return false; }
        for (ModuleButton child : getVisibleButtons()) {
            if (child.mouseClicked(mouseX, mouseY, button)) { captured = child; return true; }
        }
        return true;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) { return captured != null && captured.mouseDragged(mouseX, mouseY, button); }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(mouseX, mouseY, button); captured = null; return consumed;
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!contains(mouseX, mouseY)) { return false; }
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - amount * 24))); return true;
    }
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) { return getVisibleButtons().stream().anyMatch(button -> button.charTyped(event)); }
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event, java.util.function.Supplier<String> clipboard, java.util.function.Consumer<String> copy) { return getVisibleButtons().stream().anyMatch(button -> button.keyPressed(event, clipboard, copy)); }

}
