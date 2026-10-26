package dev.riserecode.mixin;

import dev.riserecode.Client;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class BlockIndexMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void riserecode$changed(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> callback) {
        if (Client.INSTANCE.isInitialized()) {
            LevelChunk chunk=(LevelChunk)(Object)this;
            Client.INSTANCE.getBlockIndex().changed(chunk, pos, state);
            var mc=net.minecraft.client.Minecraft.getInstance();
            if(mc.isSameThread()&&chunk.getLevel()==mc.level&&mc.level!=null&&callback.getReturnValue()!=null){Client.INSTANCE.getEventBus().post(new dev.riserecode.event.TerrainChangedEvent(mc.level,pos,callback.getReturnValue(),state));}
        }
    }
}
