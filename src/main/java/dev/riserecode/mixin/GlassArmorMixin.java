package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(HumanoidArmorLayer.class)
public abstract class GlassArmorMixin {
    @WrapMethod(method="renderArmorPiece")
    private void risecode$armor(PoseStack poses,SubmitNodeCollector collector,ItemStack stack,EquipmentSlot slot,int light,HumanoidRenderState state,Operation<Void> original){
        var s=GlassCapture.surface(state,Component.ARMOR,true,GlassCapture.slot(slot));
        original.call(poses,s==null?collector:new GlassCollector(collector,s),stack,slot,light,state);
    }
}
