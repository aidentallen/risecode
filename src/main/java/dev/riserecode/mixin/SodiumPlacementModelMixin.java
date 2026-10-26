package dev.riserecode.mixin;
import dev.riserecode.render.placement.PlacementAnimations;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer",remap=false)
public abstract class SodiumPlacementModelMixin {
    @Inject(method="renderModel",at=@At("HEAD"),cancellable=true)
    private void riserecode$hide(BlockStateModel model,BlockState state,BlockPos pos,BlockPos origin,CallbackInfo ci){if(PlacementAnimations.hidden(pos,state)){
        if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock&&net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betterblockentities"))PlacementAnimations.bbeSuppressed();
        ci.cancel();
    }}
}
