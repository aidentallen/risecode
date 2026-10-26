package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.AmbienceRenderer;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import net.minecraft.client.Minecraft;

/** Rendering overrides are frame parameters; native chunk geometry stays intact. */
public final class AmbienceModule extends Module {
    public enum SkyMode { AURORA, NEBULA, SUNSET, CLOUDS, STORM }
    public enum Quality { FAST, BALANCED, HIGH }
    public final BooleanSetting sky = register(new BooleanSetting("Sky", "Procedural atmosphere", true));
    public final BooleanSetting water = register(new BooleanSetting("Water", "Animated surface lighting and sky reflections", true));
    public final EnumSetting<SkyMode> mode = register(new EnumSetting<>("Sky Mode", "Atmosphere style", SkyMode.AURORA));
    public final BooleanSetting theme = register(new BooleanSetting("Theme Colors", "Use Appearance accents", true));
    public final ColorSetting first = register(new ColorSetting("Sky Color", "Primary atmosphere color", 0xFF65E7B4));
    public final ColorSetting second = register(new ColorSetting("Sky Secondary", "Secondary atmosphere color", 0xFF9B70EE));
    public final DoubleSetting intensity = register(DoubleSetting.extended("Intensity", "Atmosphere brightness", 0.8, 0, 2));
    public final DoubleSetting speed = register(DoubleSetting.extended("Sky Speed", "Atmosphere animation speed", 0.2, 0, 3));
    public final DoubleSetting softness = register(DoubleSetting.extended("Softness", "Atmosphere edge softness", 0.6, 0.05, 1));
    public final BooleanSetting stars = register(new BooleanSetting("Stars", "Procedural stars", true));
    public final EnumSetting<Quality> quality = register(new EnumSetting<>("Quality", "Noise detail", Quality.BALANCED));
    public final ColorSetting shallow = register(new ColorSetting("Shallow Water", "Shallow surface tint", 0xFF47AEBB));
    public final ColorSetting deep = register(new ColorSetting("Deep Water", "Deep surface tint", 0xFF153657));
    public final DoubleSetting waves = register(DoubleSetting.extended("Wave Strength", "Swell and ripple normals", 0.25, 0, 1));
    public final DoubleSetting waterSpeed = register(DoubleSetting.extended("Water Speed", "Wave animation speed", 1, 0, 3));
    public final DoubleSetting transparency = register(DoubleSetting.extended("Transparency", "Native water transparency", 0.35, 0, 1));
    public final DoubleSetting reflection = register(DoubleSetting.extended("Reflection Strength", "Atmosphere reflected at grazing angles", 0.4, 0, 1));
    private Object level;
    public AmbienceModule(EventBus bus) {
        super("Ambience", "Original animated sky and water; yields to Iris shader packs", Category.RENDER, bus);
        for (var setting : java.util.List.of(mode, theme, intensity, speed, softness, stars, quality)) { setting.setVisibleWhen(sky::getValue); }
        first.setVisibleWhen(() -> sky.getValue() && !theme.getValue()); second.setVisibleWhen(first::isVisible);
        for (var setting : java.util.List.of(shallow, deep, waves, waterSpeed, transparency, reflection)) { setting.setVisibleWhen(water::getValue); }
    }
    public String status() { if(!dev.riserecode.render.RendererCapabilities.chunkAdapters){return "Renderer water adapter unavailable";} return AmbienceRenderer.shaderPackActive() ? "Iris shader pack active — Ambience suspended" : "Native geometry · shader animation"; }
    @Override protected void onEnable() { level = Minecraft.getInstance().level; AmbienceRenderer.bind(this); listen(UpdateEvent.class, event -> {
        if (level != Minecraft.getInstance().level) { level = Minecraft.getInstance().level; AmbienceRenderer.reset(); }
        AmbienceRenderer.updateHandoff();
    }); }
    @Override protected void onDisable() { AmbienceRenderer.unbind(this); level = null; }
}
