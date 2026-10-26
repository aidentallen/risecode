package dev.riserecode.ui.menu;

import dev.riserecode.ui.animation.PopIn;
import dev.riserecode.ui.font.StyledText;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public final class TooltipMotion {
    public interface Text { StyledText risecode$layout(); }
    private static long opened,last;
    private static int key;
    private static double alpha=1;
    private TooltipMotion(){ }
    public static double alpha(){return alpha;}
    public static void alpha(double value){alpha=value;}
    public static double progress(Font font,List<ClientTooltipComponent> components){
        int signature=1;for(var component:components){signature=31*signature+(component instanceof Text text?text.risecode$layout().runs().hashCode():component.getClass().hashCode()+component.getWidth(font)+component.getHeight(font));}
        long now=System.nanoTime();if(signature!=key||now-last>250_000_000L){opened=now;key=signature;}last=now;
        return PopIn.progress((now-opened)*1e-9,PopIn.windowDuration("Tooltips"));
    }
}
