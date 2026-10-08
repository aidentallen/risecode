package dev.riserecode.event;

import net.minecraft.world.item.ItemDisplayContext;
import dev.riserecode.setting.ColorSetting;

/** Native item renderer asks enabled modules for an immutable visual policy. */
public final class EnchantmentRenderEvent extends Event {
    public record Style(boolean outline, int outlineColor, double intensity, boolean overlay, int overlayColor, double speed, double scale, double width, double glow, ColorSetting.Paint outlinePaint, ColorSetting.Paint overlayPaint) { }
    private final ItemDisplayContext context;
    private Style style;
    public EnchantmentRenderEvent(ItemDisplayContext context) { this.context = context; }
    public ItemDisplayContext context() { return context; }
    public Style style() { return style; }
    public void style(Style value) { style = value; }
}
