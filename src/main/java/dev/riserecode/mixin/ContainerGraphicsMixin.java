package dev.riserecode.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.ui.menu.ContainerTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class ContainerGraphicsMixin {
    @Unique private boolean risecode$drawingLabel;
    @Unique private boolean risecode$drawingProgress;
    @Inject(method="blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",at=@At("HEAD"),cancellable=true)
    private void risecode$panel(RenderPipeline pipeline,Identifier texture,int x,int y,float u,float v,int w,int h,int sw,int sh,int tw,int th,int color,CallbackInfo ci){
        if(ContainerTheme.texture((GuiGraphicsExtractor)(Object)this,texture,x,y,w,h,color)){ci.cancel();}
    }
    @Inject(method="blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",at=@At("HEAD"),cancellable=true)
    private void risecode$sprite(RenderPipeline pipeline,Identifier texture,int x,int y,int w,int h,int color,CallbackInfo ci){
        if(ContainerTheme.sprite((GuiGraphicsExtractor)(Object)this,texture,x,y,w,h,color)){ci.cancel();}
        else if(!risecode$drawingProgress&&ContainerTheme.progress(texture)){
            risecode$drawingProgress=true;try{((GuiGraphicsExtractor)(Object)this).blitSprite(pipeline,texture,x,y,w,h,Theme.opacity(Theme.accent(),(color>>>24)/255.0));}finally{risecode$drawingProgress=false;}ci.cancel();
        }
    }
    @Inject(method="blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIIII)V",at=@At("HEAD"),cancellable=true)
    private void risecode$progress(RenderPipeline pipeline,Identifier texture,int sw,int sh,int tx,int ty,int x,int y,int w,int h,int color,CallbackInfo ci){
        if(!risecode$drawingProgress&&ContainerTheme.progress(texture)){
            risecode$drawingProgress=true;try{((GuiGraphicsExtractor)(Object)this).blitSprite(pipeline,texture,sw,sh,tx,ty,x,y,w,h,Theme.opacity(Theme.accent(),(color>>>24)/255.0));}finally{risecode$drawingProgress=false;}ci.cancel();
        }
    }
    @Inject(method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void risecode$label(Font font,Component text,int x,int y,int color,boolean shadow,CallbackInfo ci){
        if(!risecode$drawingLabel && ContainerTheme.active()){
            risecode$drawingLabel=true;
            try{dev.riserecode.ui.font.StyledText.of(text).draw((GuiGraphicsExtractor)(Object)this,font,x,y,1,labelColor(color));}finally{risecode$drawingLabel=false;}
            ci.cancel();
        }
    }
    @Inject(method="text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void risecode$string(Font font,String text,int x,int y,int color,boolean shadow,CallbackInfo ci){
        if(!risecode$drawingLabel && text!=null && !text.matches("[0-9]+") && ContainerTheme.active()){
            risecode$drawingLabel=true;
            try{RenderUtil.text((GuiGraphicsExtractor)(Object)this,font,text,x,y,1,labelColor(color));}finally{risecode$drawingLabel=false;}
            ci.cancel();
        }
    }
    @Unique private static int labelColor(int color){return (color&0xFFFFFF)==0x404040?Theme.opacity(Theme.TEXT,(color>>>24)/255.0):color;}
}
