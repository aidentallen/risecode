package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.Client;
import dev.riserecode.module.modules.LowFireModule;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ScreenEffectRenderer.class)
public abstract class LowFireMixin {
    @WrapMethod(method = "submitFire")
    private static void riserecode$fire(PoseStack poses, SubmitNodeCollector collector, TextureAtlasSprite sprite, Operation<Void> original) {
        var module = Client.INSTANCE.isInitialized() ? Client.INSTANCE.getModuleManager().getByClass(LowFireModule.class).filter(LowFireModule::isEnabled) : java.util.Optional.<LowFireModule>empty();
        if (module.isEmpty()) { original.call(poses, collector, sprite); return; }
        poses.pushPose();
        try { poses.translate(0, -module.get().drop(), 0); original.call(poses, collector, sprite); }
        finally { poses.popPose(); }
    }
}
