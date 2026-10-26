package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
@Mixin(ItemInHandRenderer.class)
public abstract class GlassHandMixin {
    @Unique private boolean risecode$main=true;
    @WrapMethod(method="submitArmWithItem")
    private void risecode$scope(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float attack,ItemStack stack,float height,PoseStack poses,SubmitNodeCollector collector,int light,Operation<Void> original){
        boolean before=risecode$main;risecode$main=hand==InteractionHand.MAIN_HAND;
        try{original.call(player,partial,pitch,hand,attack,stack,height,poses,collector,light);}finally{risecode$main=before;}
    }
    @WrapMethod(method="renderItem")
    private void risecode$item(LivingEntity entity,ItemStack stack,ItemDisplayContext display,PoseStack poses,SubmitNodeCollector collector,int light,Operation<Void> original){
        var s=GlassCapture.surface(GlassCapture.local(),Component.ITEM,risecode$main,Slot.NONE);
        original.call(entity,stack,display,poses,s==null?collector:new GlassCollector(collector,s),light);
    }
    @WrapMethod(method="renderMap")
    private void risecode$map(PoseStack poses,SubmitNodeCollector collector,int light,ItemStack stack,Operation<Void> original){
        var s=GlassCapture.surface(GlassCapture.local(),Component.ITEM,risecode$main,Slot.NONE);
        original.call(poses,s==null?collector:new GlassCollector(collector,s),light,stack);
    }
}
