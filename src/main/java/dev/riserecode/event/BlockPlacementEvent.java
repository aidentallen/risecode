package dev.riserecode.event;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Successful local prediction; later server rejection still cancels its visual animation. */
public final class BlockPlacementEvent extends Event {
    public final ClientLevel level;
    public final BlockPos pos;
    public final BlockState state;
    public final Player player;
    public BlockPlacementEvent(ClientLevel level, BlockPos pos, BlockState state, Player player) {
        this.level=level; this.pos=pos.immutable(); this.state=state; this.player=player;
    }
}
