package dev.riserecode.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.Client;
import dev.riserecode.cosmetics.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CosmeticCapeMixin {
    @Shadow @Final private HumanoidModel<AvatarRenderState> model;
    @Shadow private boolean hasLayer(ItemStack stack,EquipmentClientInfo.LayerType type){throw new AssertionError();}
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",at=@At("HEAD"),cancellable=true)
    private void risecode$cape(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch,CallbackInfo ci){
        var outfit=((CosmeticRenderState)state).risecode$outfit();if(outfit==null||!outfit.visible()){return;}var item=outfit.items().get(CosmeticCategory.CAPE);if(item==null){return;}
        var asset=Client.INSTANCE.getCosmetics().request(item.id());if(asset==null||asset.textures().isEmpty()){return;}ci.cancel();
        if(state.isInvisible||!state.showCape||hasLayer(state.chestEquipment,EquipmentClientInfo.LayerType.WINGS)){return;}
        poses.pushPose();try{if(hasLayer(state.chestEquipment,EquipmentClientInfo.LayerType.HUMANOID)){poses.translate(0,-.053125F,.06875F);}
            poses.translate(item.x(),-item.y(),item.z());float scale=(float)item.scale();poses.scale(scale,scale,scale);
            collector.submitModel(model,state,poses,RenderTypes.entityTranslucent(asset.textures().getFirst()),light,OverlayTexture.NO_OVERLAY,CosmeticsRenderer.color(item.primary(),item.opacity()),null,state.outlineColor,null);
        }finally{poses.popPose();}
    }
}
