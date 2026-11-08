package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;

/** Sends a small hop to the server right before a hit on the ground, so the hit counts as falling. */
public final class CriticalsModule extends Module {
    private final Minecraft minecraft;
    public CriticalsModule(EventBus bus, Minecraft minecraft) { super("Criticals", "Turns ground hits into critical hits.", Category.COMBAT, bus); this.minecraft = minecraft; }
    /** Called right before the client attacks. */
    public void beforeAttack(Entity target) {
        var player = minecraft.player;
        var connection = minecraft.getConnection();
        if (player == null || connection == null || !player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable() || player.isPassenger()
                || player.getMainHandItem().is(Items.MACE) || player.isFallFlying()) { return; }
        double x = player.getX(), y = player.getY(), z = player.getZ();
        boolean wall = player.horizontalCollision;
        connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, wall));
        connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, wall));
    }
}
