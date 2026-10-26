package dev.riserecode.mixin;

import dev.riserecode.resources.ResourcesPackSource;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Minecraft.class)
public abstract class ResourcesRepositoryMixin {
    @ModifyArgs(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"))
    private void riserecode$resourcesSource(Args args){
        RepositorySource[] sources=args.get(0);RepositorySource[] result=java.util.Arrays.copyOf(sources,sources.length+2);result[sources.length]=ResourcesPackSource.INSTANCE;result[sources.length+1]=dev.riserecode.ui.sound.UiSounds.INSTANCE;args.set(0,result);
    }
}
