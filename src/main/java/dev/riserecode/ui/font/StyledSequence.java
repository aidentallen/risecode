package dev.riserecode.ui.font;

import dev.riserecode.Client;
import dev.riserecode.module.modules.BetterChatModule;
import dev.riserecode.ui.animation.PopIn;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** Native chat lines carry their layout, styles and original message tick through rescaling/deletion. */
public record StyledSequence(StyledText layout,int addedTick) implements FormattedCharSequence {
    @Override public boolean accept(FormattedCharSink sink){int offset=0;for(var run:layout.runs()){for(int code:run.text().codePoints().toArray()){if(!sink.accept(offset++,run.style(),code)){return false;}}}return true;}
    public double progress(){
        var module=Client.INSTANCE.getModuleManager().getByClass(BetterChatModule.class).orElse(null);
        if(module==null||!module.isEnabled()){return 1;}
        var mc=Minecraft.getInstance();double age=(mc.gui.hud.getGuiTicks()-addedTick+mc.getDeltaTracker().getGameTimeDeltaPartialTick(false))/20.0;
        return PopIn.moduleProgress(module,age);
    }
    public double scale(){var module=Client.INSTANCE.getModuleManager().getByClass(BetterChatModule.class).orElse(null);return module==null?1:module.popScale(progress());}
}
