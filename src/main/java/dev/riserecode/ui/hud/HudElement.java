package dev.riserecode.ui.hud;

import dev.riserecode.module.modules.hud.HudModule;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;

public final class HudElement {
    private final String id, label;
    private final HudModule module;
    private final HudLayout layout;
    private final Animation opacity = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.HUD_ENTRANCE, 0, 0.22, MathUtil.Easing.OUT_QUART);
    private final Animation x = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.DRAGGING, 0, 0.07, MathUtil.Easing.OUT_QUART);
    private final Animation y = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.DRAGGING, 0, 0.07, MathUtil.Easing.OUT_QUART);
    private boolean positioned;
    public HudElement(String id, String label, HudModule module, double x, double y) {
        this.id = id; this.label = label; this.module = module; layout = new HudLayout(x, y);
    }
    public String id() { return id; }
    public String label() { return label; }
    public HudModule module() { return module; }
    public HudLayout layout() { return layout; }
    public boolean enabled() { return module == null || module.isEnabled(); }
    public double opacity() { opacity.channel(enabled() ? dev.riserecode.ui.theme.ThemeManager.Channel.HUD_ENTRANCE : dev.riserecode.ui.theme.ThemeManager.Channel.HUD_EXIT).animateTo(enabled() ? 1 : 0); return MathUtil.clamp01(opacity.getValue()); }
    public double[] position(double targetX, double targetY) {
        if (!positioned) { x.reset(targetX); y.reset(targetY); positioned = true; }
        x.animateTo(targetX); y.animateTo(targetY); return new double[] {x.getValue(), y.getValue()};
    }
}
