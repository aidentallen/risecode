package dev.riserecode.mixin;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Keep our native TrueType glyph uploads in the backend-neutral RGBA atlas path. */
@Mixin(GlyphStitcher.class)
public abstract class FontGlyphStitcherMixin {
    @Shadow @Final private Identifier texturePrefix;
    @ModifyVariable(method="stitch",at=@At("HEAD"),argsOnly=true)
    private GlyphBitmap risecode$rgba(GlyphBitmap glyph){
        if(!texturePrefix.getNamespace().equals("riserecode")||!glyph.getClass().getName().equals("com.mojang.blaze3d.font.TrueTypeGlyphProvider$Glyph$1")){return glyph;}
        return new GlyphBitmap(){
            public int getPixelWidth(){return glyph.getPixelWidth();}
            public int getPixelHeight(){return glyph.getPixelHeight();}
            public float getOversample(){return glyph.getOversample();}
            public float getBearingLeft(){return glyph.getBearingLeft();}
            public float getBearingTop(){return glyph.getBearingTop();}
            public boolean isColored(){return true;}
            public void upload(int x,int y,GpuTexture texture){glyph.upload(x,y,texture);}
        };
    }
}
