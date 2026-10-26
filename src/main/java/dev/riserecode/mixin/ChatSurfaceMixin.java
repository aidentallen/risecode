package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.module.modules.BetterChatModule;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess","net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"},remap=false)
public abstract class ChatSurfaceMixin {
    @Shadow(remap=false) @Final private GuiGraphicsExtractor graphics;
    @Inject(method="fill",at=@At("HEAD"),cancellable=true)
    private void risecode$surface(int x0,int y0,int x1,int y1,int color,CallbackInfo ci){
        if(Client.INSTANCE.isInitialized()&&Client.INSTANCE.getModuleManager().getByClass(BetterChatModule.class).map(BetterChatModule::isEnabled).orElse(false)){
            double alpha=(color>>>24)/255.0;RenderUtil.drawRoundedRect(graphics,x0,y0,x1-x0,y1-y0,3,Theme.opacity(Theme.SURFACE,alpha));ci.cancel();
        }
    }
}
