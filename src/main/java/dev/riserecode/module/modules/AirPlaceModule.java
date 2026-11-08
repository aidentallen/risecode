package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.EspGeometry;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places the held block in mid air where you look, while the use button is held and nothing is targeted. */
public final class AirPlaceModule extends Module {
    private final DoubleSetting distance = register(DoubleSetting.extended("Distance", "How far in front of you the block appears", 4, 1.5, 6));
    private final DoubleSetting delay = register(DoubleSetting.extended("Delay", "Ticks between placements", 4, 0, 10));
    private final BooleanSetting outline = register(new BooleanSetting("Outline", "Frame the empty block that would be filled, like a normal block outline", true));
    private final ColorSetting outlineColor = register(new ColorSetting("Outline Color", "Frame color", 0xE6101216));
    private final DoubleSetting outlineWidth = register(DoubleSetting.extended("Outline Width", "Frame pixels", 2, 0.5, 5));
    private final Minecraft minecraft;
    private int cooldown;

    public AirPlaceModule(EventBus bus, Minecraft minecraft) {
        super("AirPlace", "Place blocks in mid air where you look.", Category.PLAYER, bus);
        this.minecraft = minecraft;
        outlineColor.setVisibleWhen(outline::getValue);
        outlineWidth.setVisibleWhen(outline::getValue);
    }

    @Override protected void onEnable() {
        cooldown = 0;
        listen(UpdateEvent.class, event -> {
            var player = minecraft.player;
            if (cooldown > 0) { cooldown--; }
            if (player == null || minecraft.gameMode == null || !minecraft.options.keyUse.isDown() || cooldown > 0) { return; }
            BlockPos pos = target();
            InteractionHand hand = hand();
            if (pos == null || hand == null) { return; }
            // Clicking an empty block makes the game replace that block itself, so no support block is needed.
            var click = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            if (minecraft.gameMode.useItemOn(player, hand, click).consumesAction()) { player.swing(hand); cooldown = (int) Math.round(delay.getValue()); }
        });
        listen(Render3DEvent.class, event -> {
            if (!outline.getValue()) { return; }
            BlockPos pos = target();
            if (pos != null) { EspGeometry.box(event, new AABB(pos), outlineColor, outlineWidth.getValue(), false); }
        });
    }

    private InteractionHand hand() {
        var player = minecraft.player;
        if (player == null) { return null; }
        return player.getMainHandItem().getItem() instanceof BlockItem ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().getItem() instanceof BlockItem ? InteractionHand.OFF_HAND : null;
    }

    /** The empty block in front of the crosshair that a click would fill, or null when something is targeted or nothing can be placed. */
    private BlockPos target() {
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gui.screen() != null || hand() == null) { return null; }
        var hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) { return null; }
        BlockPos pos = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(distance.getValue())));
        return minecraft.level.getBlockState(pos).canBeReplaced() ? pos : null;
    }
}
