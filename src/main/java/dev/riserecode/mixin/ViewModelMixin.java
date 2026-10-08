package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.Client;
import dev.riserecode.event.HeldItemTransformEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class ViewModelMixin {
    @Unique private ItemDisplayContext riserecode$mapHand=ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    @Unique private void riserecode$transform(PoseStack poses,ItemDisplayContext type) { poses.pushPose();if(Client.INSTANCE.isInitialized()) { Client.INSTANCE.getEventBus().post(new HeldItemTransformEvent(poses,type)); } }
    @Inject(method="renderItem",at=@At("HEAD"))
    private void riserecode$begin(LivingEntity entity,ItemStack stack,ItemDisplayContext type,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo callback) { riserecode$transform(poses,type); }
    @Inject(method="renderItem",at=@At("RETURN"))
    private void riserecode$end(LivingEntity entity,ItemStack stack,ItemDisplayContext type,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo callback) { poses.popPose(); }
    @Inject(method="submitArmWithItem",at=@At("HEAD"))
    private void riserecode$hand(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float attack,ItemStack stack,float height,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo callback) {
        var arm=hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        riserecode$mapHand=arm==HumanoidArm.LEFT?ItemDisplayContext.FIRST_PERSON_LEFT_HAND:ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
    @Inject(method="renderMap",at=@At("HEAD"))
    private void riserecode$mapBegin(PoseStack poses,SubmitNodeCollector collector,int light,ItemStack stack,CallbackInfo callback) { riserecode$transform(poses,riserecode$mapHand); }
    @Inject(method="renderMap",at=@At("RETURN"))
    private void riserecode$mapEnd(PoseStack poses,SubmitNodeCollector collector,int light,ItemStack stack,CallbackInfo callback) { poses.popPose(); }
}
