package dev.riserecode.ui.hud;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render2DEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer implements AutoCloseable {
    /** Read-only presentation state, useful to future HUD editors. */
    public record EntryState(Module module, double visibility, double y) { }
    private static final class Entry {
        private final Module module;
        private final Animation y;
        private Animation slide = new Animation(0, 0.38, MathUtil.Easing.OUT_ELASTIC);
        private boolean active;
        private int width;

        private Entry(Module module) {
            this.module = module;
            y = new Animation(8, 0.24, MathUtil.Easing.OUT_QUART);
        }

        private void update(boolean enabled, double deltaSeconds) {
            if (enabled != active) {
                active = enabled;
                slide = new Animation(slide.getValue(), enabled ? 0.38 : 0.22,
                        enabled ? MathUtil.Easing.OUT_ELASTIC : MathUtil.Easing.OUT_QUART);
                slide.animateTo(enabled ? 1 : 0);
            }
            slide.update(deltaSeconds);
        }
    }

    private final Minecraft minecraft;
    private final ModuleManager modules;
    private final String watermark;
    private final EventBus.Subscription subscription;
    private final Map<Module, Entry> entries = new LinkedHashMap<>();
    private final Animation visibility = new Animation(1, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
    private boolean enabled = true;

    public HudRenderer(EventBus eventBus, Minecraft minecraft, ModuleManager modules, String watermark) {
        this.minecraft = minecraft; this.modules = modules; this.watermark = watermark;
        subscription = eventBus.subscribe(Render2DEvent.class, this::render);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<EntryState> getEntries() {
        return entries.values().stream().map(entry -> new EntryState(entry.module, entry.slide.getValue(), entry.y.getValue())).toList();
    }

    private void render(Render2DEvent event) {
        double seconds = Math.min(0.1, event.getDeltaSeconds());
        visibility.animateTo(enabled ? 1 : 0);
        double opacity = MathUtil.clamp01(visibility.update(seconds));
        for (Module module : modules.getModules()) {
            if (module.isEnabled()) { entries.computeIfAbsent(module, Entry::new); }
        }
        for (Entry entry : entries.values()) { entry.update(entry.module.isEnabled(), seconds); }
        entries.values().removeIf(entry -> !entry.active && !entry.slide.isRunning() && entry.slide.getValue() == 0);
        if (minecraft.font == null) { return; }
        var sorted = new ArrayList<>(entries.values());
        for (Entry entry : sorted) { entry.width = RenderUtil.textWidth(minecraft.font, entry.module.getName(), false); }
        sorted.sort(Comparator.comparingInt((Entry entry) -> entry.width).reversed().thenComparing(entry -> entry.module.getName()));
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).y.animateTo(8 + i * 24);
            sorted.get(i).y.update(seconds);
        }
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || opacity <= 0) { return; }
        GuiGraphicsExtractor graphics = event.getGraphics();
        int watermarkWidth = RenderUtil.textWidth(minecraft.font, watermark, true) + 24;
        RenderUtil.drawDropShadow(graphics, 8, 8, watermarkWidth, 28, 8, Theme.opacity(Theme.SHADOW, opacity));
        RenderUtil.drawRoundedRect(graphics, 8, 8, watermarkWidth, 28, 8, Theme.opacity(Theme.PANEL, opacity));
        RenderUtil.drawRoundedBorder(graphics, 8, 8, watermarkWidth, 28, 8, Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawText(graphics, minecraft.font, watermark, 20, 16, Theme.opacity(Theme.TEXT, opacity), true, false);
        RenderUtil.drawGradientRect(graphics, 20, 32, watermarkWidth - 24, 1,
                Theme.opacity(Theme.BLUE, opacity), Theme.opacity(Theme.FUCHSIA, opacity));
        for (int i = 0; i < sorted.size(); i++) {
            Entry entry = sorted.get(i);
            double amount = entry.slide.getValue();
            double alpha = opacity * MathUtil.clamp01(amount);
            double boxWidth = entry.width + 24;
            double x = graphics.guiWidth() - boxWidth - 8 + (1 - amount) * (boxWidth + 24);
            double y = entry.y.getValue();
            int accent = Theme.lerp(Theme.BLUE, Theme.FUCHSIA, sorted.size() <= 1 ? 0 : (double) i / (sorted.size() - 1));
            RenderUtil.drawDropShadow(graphics, x, y, boxWidth, 21, 6, Theme.opacity(Theme.SHADOW, alpha * 0.6));
            RenderUtil.drawRoundedRect(graphics, x, y, boxWidth, 21, 6, Theme.opacity(Theme.PANEL, alpha));
            RenderUtil.drawRoundedBorder(graphics, x, y, boxWidth, 21, 6, Theme.opacity(Theme.BORDER, alpha));
            RenderUtil.drawRoundedRect(graphics, x + boxWidth - 3, y + 5, 2, 11, 1, Theme.opacity(accent, alpha));
            RenderUtil.drawStringWithShadow(graphics, minecraft.font, entry.module.getName(), x + 10, y + 6,
                    Theme.opacity(Theme.TEXT, alpha));
        }
    }

    @Override
    public void close() { subscription.close(); entries.clear(); }
}
