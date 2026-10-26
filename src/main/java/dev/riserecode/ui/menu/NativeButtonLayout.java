package dev.riserecode.ui.menu;

import dev.riserecode.ui.component.ButtonMetrics;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;

/** Fit the actual native widget inside its layout slot; input and focus use the same rectangle. */
public final class NativeButtonLayout {
    private record Bounds(int x,int y,int w,int h){static Bounds of(AbstractWidget w){return new Bounds(w.getX(),w.getY(),w.getWidth(),w.getHeight());}}
    private record Slot(Bounds original,Bounds applied){}
    private static final Map<AbstractWidget,Slot> SLOTS=new WeakHashMap<>();
    private NativeButtonLayout(){}
    public static void compact(AbstractWidget widget){
        if(widget instanceof PageGear||!MenuTheme.enabled()||Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>){return;}
        var current=Bounds.of(widget);var slot=SLOTS.get(widget);
        Bounds original=slot==null||!slot.applied().equals(current)?current:slot.original();
        int width=(int)Math.min(original.w(),widget instanceof SpriteIconButton?ButtonMetrics.ICON_SIZE:ButtonMetrics.width(Minecraft.getInstance().font,widget.getMessage().getString()));
        int height=(int)ButtonMetrics.HEIGHT;
        var screen=Minecraft.getInstance().gui.screen();
        Bounds next=new Bounds(Math.clamp(original.x()+(original.w()-width)/2,0,Math.max(0,screen.width-width)),Math.clamp(original.y()+(original.h()-height)/2,0,Math.max(0,screen.height-height)),width,height);
        widget.setX(next.x());widget.setY(next.y());widget.setWidth(next.w());widget.setHeight(next.h());SLOTS.put(widget,new Slot(original,next));
    }
}
