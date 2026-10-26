package dev.riserecode.mixin;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="com.mojang.blaze3d.font.TrueTypeGlyphProvider$Glyph$1")
public abstract class TrueTypeGlyphUploadMixin {
    @Redirect(method="upload",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/CommandEncoder;writeToTexture(Lcom/mojang/blaze3d/textures/GpuTexture;Lcom/mojang/blaze3d/platform/NativeImage;IIII)V"))
    private void risecode$upload(CommandEncoder encoder,GpuTexture texture,NativeImage source,int mip,int layer,int x,int y){
        if(texture.getFormat()!=GpuFormat.RGBA8_UNORM){encoder.writeToTexture(texture,source,mip,layer,x,y);return;}
        try(NativeImage rgba=new NativeImage(source.getWidth(),source.getHeight(),false)){
            for(int row=0;row<source.getHeight();row++){for(int column=0;column<source.getWidth();column++){
                rgba.setPixel(column,row,Byte.toUnsignedInt(source.getLuminanceOrAlpha(column,row))<<24|0xFFFFFF);
            }}
            encoder.writeToTexture(texture,rgba,mip,layer,x,y);
        }
    }
}
