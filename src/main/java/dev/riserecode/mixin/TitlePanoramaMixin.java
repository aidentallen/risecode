package dev.riserecode.mixin;

import dev.riserecode.ui.panorama.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(TitleScreen.class)
public abstract class TitlePanoramaMixin extends Screen {
    protected TitlePanoramaMixin(Component title){super(title);}
    @Inject(method="init",at=@At("TAIL"))
    private void risecode$sceneControls(CallbackInfo ci){
        addRenderableWidget(new PanoramaControl(PanoramaControl.Action.BREATH,10,25));
        addRenderableWidget(new PanoramaControl(PanoramaControl.Action.ATMOSPHERE,40,111));
        addRenderableWidget(new PanoramaControl(PanoramaControl.Action.MUTE,156,49));
        addRenderableWidget(new PanoramaControl(PanoramaControl.Action.RESET,210,74));
        LivingPanorama.syncWidgets(this);
    }
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void risecode$scenicClick(MouseButtonEvent e,boolean twice,CallbackInfoReturnable<Boolean> ci){
        if(!LivingPanorama.scenic())return;
        for(var child:children())if(child instanceof PanoramaControl control&&control.returnControl()&&control.mouseClicked(e,twice)){ci.setReturnValue(true);return;}
        if(e.button()==0)LivingPanorama.press(e.x(),e.y());ci.setReturnValue(true);
    }
    @Inject(method="mouseClicked",at=@At("RETURN"),cancellable=true)
    private void risecode$backgroundPress(MouseButtonEvent e,boolean twice,CallbackInfoReturnable<Boolean> ci){
        if(!ci.getReturnValueZ()&&LivingPanorama.active()&&e.button()==0){
            for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.visible&&e.x()>=widget.getX()&&e.x()<widget.getX()+widget.getWidth()&&e.y()>=widget.getY()&&e.y()<widget.getY()+widget.getHeight())return;
            LivingPanorama.press(e.x(),e.y());ci.setReturnValue(true);
        }
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(LivingPanorama.active()&&LivingPanorama.interaction().pressed()){LivingPanorama.drag(e.x(),e.y());return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(e.button()==0&&LivingPanorama.release(e.x(),e.y()))return true;return super.mouseReleased(e);}
    @Override public void mouseMoved(double x,double y){LivingPanorama.pointer(x,y);if(!LivingPanorama.scenic())super.mouseMoved(x,y);}
    @Override public boolean keyPressed(KeyEvent e){
        if(LivingPanorama.active()&&e.key()==org.lwjgl.glfw.GLFW.GLFW_KEY_F1){LivingPanorama.toggleScenic();return true;}
        if(LivingPanorama.scenic()){if(e.isEscape())LivingPanorama.toggleScenic();return true;}
        return super.keyPressed(e);
    }
    @Override public boolean charTyped(CharacterEvent e){return LivingPanorama.scenic()||super.charTyped(e);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){return LivingPanorama.scenic()||super.mouseScrolled(x,y,horizontal,vertical);}
    @Inject(method="removed",at=@At("HEAD"))
    private void risecode$leaveScene(CallbackInfo ci){LivingPanorama.leaveTitle();}
}
