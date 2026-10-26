package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.menu.MenuTransition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MenuScreenMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="extractRenderStateWithTooltipAndSubtitles")
    private void risecode$frame(GuiGraphicsExtractor graphics,int mx,int my,float delta,Operation<Void> original) {
        String previous = dev.riserecode.ui.theme.PageAppearance.context();
        dev.riserecode.ui.theme.PageAppearance.context(MenuTheme.pageId((Screen)(Object)this));
        try { MenuTheme.beginScreen((Screen)(Object)this); original.call(graphics,mx,my,delta); }
        finally { dev.riserecode.ui.theme.PageAppearance.context(previous); }
    }

    @WrapOperation(method="extractRenderStateWithTooltipAndSubtitles",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void risecode$background(Screen screen,GuiGraphicsExtractor graphics,int mx,int my,float delta,Operation<Void> original) {
        if(dev.riserecode.ui.panorama.LivingPanorama.active()&&!dev.riserecode.ui.panorama.LivingPanorama.settled()&&dev.riserecode.ui.theme.Theme.manager().choice("Background",dev.riserecode.ui.theme.ThemeManager.Background.class)==dev.riserecode.ui.theme.ThemeManager.Background.NONE){
            original.call(screen,graphics,mx,my,delta);MenuTheme.drawBackdrop(graphics);return;
        }
        if (MenuTheme.photo(screen)||dev.riserecode.ui.panorama.LivingPanorama.active()) { MenuTheme.drawBackdrop(graphics); }
        // Container subclasses draw their panel and player preview here too.
        original.call(screen,graphics,mx,my,delta);
        if(!(screen instanceof net.minecraft.client.gui.screens.ChatScreen)&&!dev.riserecode.ui.panorama.LivingPanorama.active()){MenuTheme.drawParticles(graphics);}
    }
    @WrapOperation(method="extractRenderStateWithTooltipAndSubtitles",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void risecode$foreground(Screen screen,GuiGraphicsExtractor graphics,int mx,int my,float delta,Operation<Void> original) {
        MenuTransition scope=MenuTransition.begin(graphics,screen);
        try {
            int px=(int)MenuTheme.pointerX(screen,mx),py=(int)MenuTheme.pointerY(screen,my);
            boolean panorama=screen instanceof net.minecraft.client.gui.screens.TitleScreen&&dev.riserecode.ui.panorama.LivingPanorama.active();
            if(!panorama||dev.riserecode.ui.panorama.LivingPanorama.menuOpacity()>0){
                var fade=dev.riserecode.ui.menu.MenuTransition.widget(graphics,0,0,1,panorama?dev.riserecode.ui.panorama.LivingPanorama.menuOpacity():1);
                try{original.call(screen,graphics,px,py,delta);}finally{fade.close();}
            }else{screen.children().stream().filter(dev.riserecode.ui.panorama.PanoramaControl.class::isInstance).map(dev.riserecode.ui.panorama.PanoramaControl.class::cast).filter(dev.riserecode.ui.panorama.PanoramaControl::returnControl).forEach(control->control.extractRenderState(graphics,px,py,delta));}
            screen.children().stream().filter(child->!dev.riserecode.ui.panorama.LivingPanorama.scenic()).filter(dev.riserecode.ui.menu.PageGear.class::isInstance).map(dev.riserecode.ui.menu.PageGear.class::cast).forEach(gear->gear.extractRenderState(graphics,px,py,delta));
        }
        finally { scope.close(); }
    }
    @Inject(method="added",at=@At("HEAD"))
    private void risecode$opened(CallbackInfo ci) { dev.riserecode.ui.animation.ButtonFeedback.cancelAll();MenuTheme.opened((Screen)(Object)this); }

    @Inject(method="init(II)V",at=@At("TAIL"))
    private void risecode$gear(int width,int height,CallbackInfo ci){
        Screen screen=(Screen)(Object)this;
        if(dev.riserecode.Client.INSTANCE.isInitialized()&&MenuTheme.gearSupported(screen)&&screen.children().stream().noneMatch(dev.riserecode.ui.menu.PageGear.class::isInstance)){
            ((ScreenWidgetsAccessor)this).risecode$add(new dev.riserecode.ui.menu.PageGear(screen));
        }
    }
    // Subclasses and mod screens sometimes invoke background helpers from their foreground pass.
    @Inject(method={"extractTransparentBackground","extractBlurredBackground","extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"},at=@At("HEAD"),cancellable=true)
    private void risecode$suppressOverlay(GuiGraphicsExtractor graphics,CallbackInfo ci) {
        if (MenuTheme.photo((Screen)(Object)this)) { ci.cancel(); }
    }
    @Inject(method="extractPanorama",at=@At("HEAD"),cancellable=true)
    private void risecode$panorama(GuiGraphicsExtractor graphics,float delta,CallbackInfo ci) {
        if (MenuTheme.photo((Screen)(Object)this)) { ci.cancel(); }
    }
}
