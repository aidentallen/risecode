package dev.riserecode.event;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
/** Received terrain changed on the client thread, after vanilla applied it. */
public final class TerrainChangedEvent extends Event {
    private final ClientLevel level;private final BlockPos position;
    private final net.minecraft.world.level.block.state.BlockState previous,state;
    public TerrainChangedEvent(ClientLevel level,BlockPos position){this(level,position,level.getBlockState(position),level.getBlockState(position));}
    public TerrainChangedEvent(ClientLevel level,BlockPos position,net.minecraft.world.level.block.state.BlockState previous,net.minecraft.world.level.block.state.BlockState state){this.level=level;this.position=position.immutable();this.previous=previous;this.state=state;}
    public net.minecraft.world.level.block.state.BlockState previous(){return previous;}
    public net.minecraft.world.level.block.state.BlockState state(){return state;}
    public ClientLevel level(){return level;}public BlockPos position(){return position;}
}
