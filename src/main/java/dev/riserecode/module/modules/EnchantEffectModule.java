package dev.riserecode.module.modules;

import dev.riserecode.event.EnchantmentRenderEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.world.item.ItemDisplayContext;

public final class EnchantEffectModule extends Module {
    private final BooleanSetting outline = register(new BooleanSetting("Outline", "Whole-item screen-space silhouette outline", true));
    private final ColorSetting outlineColor = register(new ColorSetting("OutlineColor", "Outline color", 0xFF59D5CB));
    private final DoubleSetting intensity = register(new DoubleSetting("OutlineIntensity", "Outline opacity", 1.0, 0, 1));
    private final DoubleSetting width = register(new DoubleSetting("OutlineWidth", "Outline thickness in screen pixels", 3, 0.5, 20));
    private final DoubleSetting glow = register(new DoubleSetting("OutlineGlow", "Optional outer glow in screen pixels", 0, 0, 12));
    private final BooleanSetting scene = register(new BooleanSetting("WorldItems", "Include frames and dropped items", false));
    private final BooleanSetting overlay = register(new BooleanSetting("Overlay", "Customize the enchantment glint independently", false));
    private final ColorSetting overlayColor = register(new ColorSetting("OverlayColor", "Glint color", 0xB0D946EF));
    private final DoubleSetting speed = register(new DoubleSetting("GlintSpeed", "Glint motion multiplier", 1, 0, 5));
    private final DoubleSetting scale = register(new DoubleSetting("GlintScale", "Glint texture scale", 1, 0.1, 4));
    public EnchantEffectModule(EventBus bus) {
        super("EnchantEffect", "Whole-item silhouette outlines and optional enchantment glint.", Category.RENDER, bus);
        outlineColor.setVisibleWhen(outline::getValue); intensity.setVisibleWhen(outline::getValue); width.setVisibleWhen(outline::getValue); glow.setVisibleWhen(outline::getValue); scene.setVisibleWhen(outline::getValue);
        overlayColor.setVisibleWhen(overlay::getValue); speed.setVisibleWhen(overlay::getValue); scale.setVisibleWhen(overlay::getValue);
    }
    @Override protected void onEnable() {
        listen(EnchantmentRenderEvent.class, event -> {
            boolean world = event.context() == ItemDisplayContext.GROUND || event.context() == ItemDisplayContext.FIXED;
            event.style(new EnchantmentRenderEvent.Style(outline.getValue() && (event.context().firstPerson() || scene.getValue() && world),
                    outlineColor.getValue(), intensity.getValue(), overlay.getValue(), overlayColor.getValue(), speed.getValue(), scale.getValue(), width.getValue(), glow.getValue(), outlineColor.paint(), overlayColor.paint()));
        });
    }
}
