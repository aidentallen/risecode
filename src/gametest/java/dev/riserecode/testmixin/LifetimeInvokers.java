package dev.riserecode.testmixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

public final class LifetimeInvokers {
    private LifetimeInvokers(){ }
    @Mixin(Hud.class)
    public interface Crosshair { @Invoker("extractCrosshair") void risecode$crosshair(GuiGraphicsExtractor graphics,DeltaTracker delta); }
    @Mixin(ItemInHandRenderer.class)
    public interface Map { @Invoker("renderMap") void risecode$map(PoseStack poses,SubmitNodeCollector collector,int light,ItemStack stack); }
}
