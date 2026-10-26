package dev.riserecode.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.riserecode.render.placement.PlacementAnimations;
import dev.riserecode.render.placement.PlacementBuildStamp;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(SectionCompiler.class)
public abstract class PlacementCompileMixin {
    @WrapMethod(method="compile")
    private SectionCompiler.Results riserecode$compile(SectionPos section,RenderSectionRegion region,VertexSorting sort,SectionBufferBuilderPack buffers,Operation<SectionCompiler.Results> original){
        long generation=PlacementAnimations.beginBuild();
        try{var result=original.call(section,region,sort,buffers);((PlacementBuildStamp)(Object)result).riserecode$placementStamp(generation);return result;}
        finally{PlacementAnimations.endBuild();}
    }
}
