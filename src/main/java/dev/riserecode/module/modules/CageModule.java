package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.util.ItemUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Right click a player or mob while holding obsidian to wall them in: floor, both sides at feet and head height, and a ceiling. */
public final class CageModule extends Module {
    private final DoubleSetting perTick = register(DoubleSetting.extended("Blocks Per Tick", "How many blocks are placed each tick", 3, 1, 10));
    private final BooleanSetting floor = register(new BooleanSetting("Floor", "Also place a block under the player", true));
    private final BooleanSetting ceiling = register(new BooleanSetting("Ceiling", "Also place a block above the player", true));
    private final BooleanSetting mobs = register(new BooleanSetting("Mobs", "Also wall in mobs such as zombies, handy for testing", true));
    private final BooleanSetting airPlace = register(new BooleanSetting("Air Place", "Place blocks that have no neighbor to click on", true));
    private final Minecraft minecraft;
    private final List<BlockPos> pending = new ArrayList<>();
    private ItemUtil.HotbarSelection selection;
    private boolean wasDown;
    private int ticks;

    public CageModule(EventBus bus, Minecraft minecraft) {
        super("Cage", "Right click a player with obsidian to wall them in.", Category.COMBAT, bus);
        this.minecraft = minecraft;
    }

    @Override protected void onEnable() { listen(UpdateEvent.class, this::update); }
    @Override protected void onDisable() { finish(); wasDown = false; }

    private void update(UpdateEvent event) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null || minecraft.gui.screen() != null) { finish(); return; }
        boolean down = minecraft.options.keyUse.isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (pressed && pending.isEmpty() && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity victim && victim != player && (victim instanceof Player || mobs.getValue())
                && player.getMainHandItem().is(Items.OBSIDIAN)) {
            start(victim.blockPosition());
        }
        if (pending.isEmpty()) { return; }
        if (++ticks > 60 || obsidianSlot(player) < 0) { finish(); return; }
        if (selection == null) { selection = new ItemUtil.HotbarSelection(player); }
        if (selection.wasManuallyOverridden()) { finish(); return; }
        selection.select(obsidianSlot(player));
        int budget = (int) Math.round(perTick.getValue());
        var reach = player.blockInteractionRange();
        for (var iterator = pending.iterator(); iterator.hasNext() && budget > 0; ) {
            BlockPos pos = iterator.next();
            if (!minecraft.level.getBlockState(pos).canBeReplaced()) { iterator.remove(); continue; }
            if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > reach * reach) { continue; }
            BlockHitResult click = clickFor(pos);
            if (click == null) { continue; }
            if (minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, click).consumesAction()) { player.swing(InteractionHand.MAIN_HAND); budget--; }
        }
        if (pending.isEmpty()) { finish(); }
    }

    private void start(BlockPos feet) {
        pending.clear(); ticks = 0;
        if (floor.getValue()) { pending.add(feet.below()); }
        for (int up = 0; up < 2; up++) { for (Direction side : Direction.Plane.HORIZONTAL) { pending.add(feet.above(up).relative(side)); } }
        if (ceiling.getValue()) { pending.add(feet.above(2)); }
    }

    /** A real neighbor to click when there is one, otherwise the empty position itself. */
    private BlockHitResult clickFor(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!minecraft.level.getBlockState(neighbor).canBeReplaced()) {
                Direction face = direction.getOpposite();
                Vec3 point = Vec3.atCenterOf(neighbor).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5);
                return new BlockHitResult(point, face, neighbor, false);
            }
        }
        return airPlace.getValue() ? new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false) : null;
    }

    private static int obsidianSlot(net.minecraft.client.player.LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) { if (player.getInventory().getItem(slot).is(Items.OBSIDIAN)) { return slot; } }
        return -1;
    }

    private void finish() {
        pending.clear(); ticks = 0;
        if (selection != null) { if (selection.getPlayer() == minecraft.player) { selection.close(); } else { selection.abandon(); } selection = null; }
    }
}
