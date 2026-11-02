package dev.riserecode.mixin;

import dev.riserecode.render.HotbarPrivacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the drawn hotbar and selected item name extents while the HUD extracts. */
@Mixin(Hud.class)
public abstract class HudHotbarPrivacyMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private int toolHighlightTimer;
    @Shadow private net.minecraft.world.item.ItemStack lastToolHighlight;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void riserecode$beginFrame(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo callback) {
        HotbarPrivacy.beginFrame();
    }

    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    private void riserecode$captureHotbar(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo callback) {
        HotbarPrivacy.captureHotbar(graphics);
    }

    @Inject(method = "extractSelectedItemName", at = @At("TAIL"))
    private void riserecode$captureItemName(GuiGraphicsExtractor graphics, CallbackInfo callback) {
        // Same conditions and layout math as the vanilla extraction, so the blur
        // only covers frames where the name actually draws.
        if (!HotbarPrivacy.blurActive() || toolHighlightTimer <= 0 || lastToolHighlight.isEmpty()) return;
        if (!(minecraft.getCameraEntity() instanceof Player)) return;
        int width = ((Hud) (Object) this).getFont().width(lastToolHighlight.getHoverName());
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 59;
        if (!minecraft.gameMode.canHurtPlayer()) y += 14;
        HotbarPrivacy.captureItemName(graphics, x, y, width);
    }
}
