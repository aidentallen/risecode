package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.Client;
import dev.riserecode.event.HeldItemTransformEvent;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class ViewModelMixin {
    @Unique private ItemDisplayContext riserecode$mapHand=ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    @Unique private void riserecode$transform(PoseStack poses,ItemDisplayContext type) { if(Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new HeldItemTransformEvent(poses,type)); } }
    @WrapMethod(method="renderItem")
    private void riserecode$item(LivingEntity entity,ItemStack stack,ItemDisplayContext type,PoseStack poses,SubmitNodeCollector collector,int light,Operation<Void> original) {
        poses.pushPose();
        try { riserecode$transform(poses,type); original.call(entity,stack,type,poses,collector,light); }
        finally { poses.popPose(); }
    }
    @Inject(method="submitArmWithItem",at=@At("HEAD"))
    private void riserecode$hand(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float attack,ItemStack stack,float height,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo callback) {
        var arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        riserecode$mapHand=arm==HumanoidArm.LEFT?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
    @WrapMethod(method="renderMap")
    private void riserecode$map(PoseStack poses,SubmitNodeCollector collector,int light,ItemStack stack,Operation<Void> original) {
        poses.pushPose();
        try { riserecode$transform(poses,riserecode$mapHand); original.call(poses,collector,light,stack); }
        finally { poses.popPose(); }
    }
}
