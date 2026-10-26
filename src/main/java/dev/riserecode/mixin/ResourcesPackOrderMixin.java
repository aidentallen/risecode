package dev.riserecode.mixin;

import dev.riserecode.resources.ResourcesPackSource;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackRepository.class)
public abstract class ResourcesPackOrderMixin {
    @Inject(method="rebuildSelected",at=@At("RETURN"),cancellable=true)
    private void riserecode$order(Collection<String> ids,CallbackInfoReturnable<List<Pack>> ci){
        var packs=new ArrayList<>(ci.getReturnValue());Pack resources=packs.stream().filter(p->p.getId().equals(ResourcesPackSource.ID)).findFirst().orElse(null);
        if(resources==null)return;packs.remove(resources);int index=packs.size();
        for(int i=0;i<packs.size();i++)if(packs.get(i).getPackSource()==PackSource.SERVER){index=i;break;}
        packs.add(index,resources);ci.setReturnValue(List.copyOf(packs));
    }
}
