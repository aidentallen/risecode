package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class GlassAvatarMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
    private void risecode$owner(Avatar player,AvatarRenderState state,float partial,CallbackInfo ci){
        ((GlassCapture.State)state).risecode$glassOwner(GlassCapture.owner(player.getUUID(),player==Minecraft.getInstance().player,false));
    }
    @WrapMethod(method="renderHand")
    private void risecode$arm(PoseStack poses,SubmitNodeCollector collector,int light,Identifier texture,ModelPart arm,boolean sleeve,Operation<Void> original){
        var mc=Minecraft.getInstance();boolean right=arm==((AvatarRenderer<?>)(Object)this).getModel().rightArm;
        boolean main=mc.player==null||right==(mc.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT);
        var s=GlassCapture.surface(GlassCapture.local(),Component.ARM,main,Slot.NONE);
        original.call(poses,s==null?collector:new GlassCollector(collector,s),light,texture,arm,sleeve);
    }
}
