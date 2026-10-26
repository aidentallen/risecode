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
    public enum Mode { LIGHTMAP, GAMMA, SHADER_NIGHT_VISION }
    private final Minecraft minecraft;
    private final EnumSetting<Mode> mode = register(new EnumSetting<>("Mode", "Lighting implementation", Mode.LIGHTMAP));
    private final DoubleSetting brightness = register(DoubleSetting.extended("Brightness", "Gamma multiplier up to 1000", 1, 0, 1));
    private final DoubleSetting shaderStrength = register(new DoubleSetting("Shader Strength", "Night vision strength supplied to the shader pack", 1, 0, 1));
    private double shaderAmount;
    private long shaderUpdated;
    private Double previousGamma;
    private GpuTexture texture;
    private GpuTextureView view;
    public FullBrightModule(EventBus bus, Minecraft minecraft) {
        super("FullBright", "Illuminates unlit areas using a client lightmap override.", Category.RENDER, bus);
        this.minecraft = minecraft; brightness.setVisibleWhen(() -> mode.getValue() == Mode.GAMMA); shaderStrength.setVisibleWhen(() -> mode.getValue() == Mode.SHADER_NIGHT_VISION);
    }
    public DoubleSetting getShaderStrength() { return shaderStrength; }
    public float shaderNightVision(float original) {
        long now=System.nanoTime(); double dt=shaderUpdated==0?0:Math.max(0,(now-shaderUpdated)*1e-9); shaderUpdated=now;
        double target=isEnabled()&&mode.getValue()==Mode.SHADER_NIGHT_VISION&&minecraft.level!=null?shaderStrength.getValue():0;
        if (target==0 || dev.riserecode.ui.theme.Theme.bool("Reduced Motion",false)) { shaderAmount=target; }
        else { shaderAmount += (target-shaderAmount)*(1-Math.exp(-dt/0.05)); }
        return Math.max(original,(float)Math.clamp(shaderAmount,0,1));
    }
    public String shaderStatus() { return !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")?"Iris required":dev.riserecode.render.ShaderWorldOverlay.active()?"Shader night vision · pack-dependent lighting":"No shader pack active"; }
    public DoubleSetting getBrightness() { return brightness; }
    public double persistentGamma(){return previousGamma==null?minecraft.options.gamma().get():previousGamma;}
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
        shaderAmount=0; shaderUpdated=0;
        if (previousGamma != null && minecraft.options != null) { rawGamma(previousGamma); previousGamma = null; }
        if (view != null) { view.close(); view = null; }
        if (texture != null) { texture.close(); texture = null; }
    }
}
