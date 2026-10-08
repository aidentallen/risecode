package dev.riserecode.module.modules;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.mixin.GammaOptionAccessor;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

/** Substitutes a white client lightmap while vanilla keeps updating its own untouched texture. */
public final class FullBrightModule extends Module {
    public enum Mode { LIGHTMAP, GAMMA }
    private final Minecraft minecraft;
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Lighting implementation", Mode.LIGHTMAP));
    private final DoubleSetting brightness = register(new DoubleSetting("Brightness", "Gamma multiplier up to 1000", 1, 0, 1));
    private Double previousGamma;
    private GpuTexture texture;
    private GpuTextureView view;
    public FullBrightModule(EventBus bus, Minecraft minecraft) {
        super("FullBright", "Illuminates unlit areas using a client lightmap override.", Category.RENDER, bus);
        this.minecraft = minecraft; brightness.setVisibleWhen(() -> mode.getValue() == Mode.GAMMA);
    }
    public DoubleSetting getBrightness() { return brightness; }
    public EnumSetting<Mode> getMode() { return mode; }
    @Override protected void onEnable() { applyGamma(); listen(UpdateEvent.class, ignored -> applyGamma()); }
    private void applyGamma() {
        if (minecraft.options == null) { return; }
        if (previousGamma == null) { previousGamma = minecraft.options.gamma().get(); }
        rawGamma(mode.getValue() == Mode.GAMMA ? brightness.getValue() * 1000 : previousGamma);
    }
    private void rawGamma(double value) { ((GammaOptionAccessor) (Object) minecraft.options.gamma()).riserecode$setRawValue(value); }
    /** Called from the native lightmap texture binding, never during client bootstrap. */
    public GpuTextureView lightmap(GpuTextureView original) {
        if (!isEnabled() || mode.getValue() != Mode.LIGHTMAP || minecraft.level == null) { return original; }
        if (view == null) {
            texture = RenderSystem.getDevice().createTexture(() -> "risecode fullbright lightmap",
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.RGBA8_UNORM, 16, 16, 1, 1);
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, new Vector4f(1, 1, 1, 1));
            view = RenderSystem.getDevice().createTextureView(texture);
        }
        return view;
    }
    @Override protected void onDisable() {
        if (previousGamma != null && minecraft.options != null) { rawGamma(previousGamma); previousGamma = null; }
        if (view != null) { view.close(); view = null; }
        if (texture != null) { texture.close(); texture = null; }
    }
}
