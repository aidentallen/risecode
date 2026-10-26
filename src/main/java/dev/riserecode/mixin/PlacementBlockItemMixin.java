package dev.riserecode.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.Client;
import dev.riserecode.event.BlockPlacementEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BlockItem.class)
public abstract class PlacementBlockItemMixin {
    @WrapMethod(method="place")
    private InteractionResult riserecode$placed(BlockPlaceContext context,Operation<InteractionResult> original){
        var pos=context.getClickedPos().immutable();var before=context.getLevel().getBlockState(pos);
        var result=original.call(context);
        if(result.consumesAction()&&before.canBeReplaced()&&context.getLevel() instanceof ClientLevel level&&Client.INSTANCE.isInitialized()){
            var state=level.getBlockState(pos);if(state.is(((BlockItem)(Object)this).getBlock()))Client.INSTANCE.getEventBus().post(new BlockPlacementEvent(level,pos,state,context.getPlayer()));
        }
        return result;
    }
}
