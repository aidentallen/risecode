package dev.riserecode.ui.menu;

import dev.riserecode.mixin.ContainerScreenAccessor;
import dev.riserecode.ui.component.Icon;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

/** Decorates vanilla surfaces only; slots, previews, recipes and input remain vanilla. */
public final class ContainerTheme {
    private static final java.util.Map<net.minecraft.client.gui.screens.Screen,Long> PANELS=new java.util.WeakHashMap<>();
    private static long surfaces;
    public static long surfaces(){return surfaces;}
    private ContainerTheme() { }
    public static boolean active() {
        return Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> && MenuTheme.enabled();
    }
    public static boolean panelTexture(String namespace,String path,int width,int height) {
        return namespace.equals("minecraft") && (path.startsWith("textures/gui/container/") || path.equals("textures/gui/recipe_book.png")) && width>=90 && height>=40;
    }
    public static boolean texture(GuiGraphicsExtractor g,Identifier texture,int x,int y,int width,int height,int color) {
        if(!active() || !panelTexture(texture.getNamespace(),texture.getPath(),width,height)){return false;}
        boolean recipe=texture.getPath().equals("textures/gui/recipe_book.png");
        var owner=(AbstractContainerScreen<?>)Minecraft.getInstance().gui.screen();
        if(recipe && (width!=147||height!=166) || !recipe&&width!=((ContainerScreenAccessor)owner).risecode$width()){return false;}
        if(!recipe){if(PANELS.getOrDefault(owner,-1L)==MenuTheme.frameSerial()){return true;}PANELS.put(owner,MenuTheme.frameSerial());var b=(ContainerScreenAccessor)owner;x=b.risecode$left();y=b.risecode$top();width=b.risecode$width();height=b.risecode$height();}
        surfaces++;double alpha=(color>>>24)/255.0;
        RenderUtil.drawDropShadow(g,x,y,width,height,5,Theme.opacity(Theme.SHADOW,alpha*.6));
        RenderUtil.drawPaintRect(g,x,y,width,height,5,Theme.paint("Surface Color"),alpha*Theme.number("Panel Opacity",.87));
        RenderUtil.drawRoundedBorder(g,x,y,width,height,5,Theme.opacity(Theme.accent(),alpha*.32),.6);
        if(!texture.getPath().equals("textures/gui/recipe_book.png")) {
            var screen=(AbstractContainerScreen<?>)Minecraft.getInstance().gui.screen();
            var bounds=(ContainerScreenAccessor)screen;
            for(var slot:screen.getMenu().slots){
                int sx=bounds.risecode$left()+slot.x-1,sy=bounds.risecode$top()+slot.y-1;
                if(slot.isActive() && sx>=x && sy>=y && sx+18<=x+width && sy+18<=y+height){
                    RenderUtil.drawRoundedRect(g,sx,sy,18,18,2,Theme.opacity(Theme.BACKGROUND,alpha*.7));
                    RenderUtil.drawRoundedBorder(g,sx,sy,18,18,2,Theme.opacity(Theme.SECONDARY,alpha*.22),.5);
                }
            }
            var menu=screen.getMenu();int output=menu instanceof net.minecraft.world.inventory.InventoryMenu||menu instanceof net.minecraft.world.inventory.CraftingMenu?0:
                    menu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu||menu instanceof net.minecraft.world.inventory.AnvilMenu||menu instanceof net.minecraft.world.inventory.GrindstoneMenu||menu instanceof net.minecraft.world.inventory.CartographyTableMenu?2:
                    menu instanceof net.minecraft.world.inventory.SmithingMenu?3:menu instanceof net.minecraft.world.inventory.StonecutterMenu?1:-1;
            if(output>=0&&output<menu.slots.size()){
                var slot=menu.slots.get(output);double ax=bounds.risecode$left()+slot.x-14,ay=bounds.risecode$top()+slot.y+8;
                int tint=Theme.opacity(Theme.SECONDARY,alpha*.7);RenderUtil.line(g,ax,ay,ax+9,ay,.8,tint);RenderUtil.line(g,ax+6,ay-3,ax+9,ay,.8,tint);RenderUtil.line(g,ax+6,ay+3,ax+9,ay,.8,tint);
            }
        }
        return true;
    }
    private static boolean hovered(GuiGraphicsExtractor g,int x,int y,int width,int height){
        var mc=Minecraft.getInstance();double mx=mc.mouseHandler.xpos()*g.guiWidth()/mc.getWindow().getScreenWidth(),my=mc.mouseHandler.ypos()*g.guiHeight()/mc.getWindow().getScreenHeight();
        var pose=g.pose();double left=pose.m00()*x+pose.m10()*y+pose.m20(),top=pose.m01()*x+pose.m11()*y+pose.m21();
        return mx>=left&&mx<left+width*pose.m00()&&my>=top&&my<top+height*pose.m11();
    }
    public static boolean progress(Identifier sprite){return active()&&sprite.getNamespace().equals("minecraft")&&sprite.getPath().startsWith("container/")&&sprite.getPath().contains("progress");}
    public static boolean sprite(GuiGraphicsExtractor g,Identifier sprite,int x,int y,int width,int height,int color) {
        if(!active() || !sprite.getNamespace().equals("minecraft")){return false;}
        String path=sprite.getPath();double alpha=(color>>>24)/255.0;
        if(path.equals("container/slot")||path.endsWith("/chest_slots")){return true;}
        boolean hot=hovered(g,x,y,width,height)&&!path.contains("disabled");
        if(path.startsWith("recipe_book/slot") || path.startsWith("recipe_book/tab") || path.contains("slot_highlight") || path.startsWith("container/")&&(path.contains("button")||path.contains("recipe")||path.contains("pattern")||path.contains("effect_background")||path.contains("scrollbar")||path.contains("scroller")||path.contains("/tab_"))) {
            boolean selected=path.contains("selected")&&!path.contains("unselected") || path.contains("highlight"), craftable=path.contains("craftable")&&!path.contains("uncraftable");
            RenderUtil.drawRoundedRect(g,x,y,width,height,3,Theme.opacity(selected||hot?Theme.HOVER:Theme.SURFACE,alpha));
            RenderUtil.drawRoundedBorder(g,x,y,width,height,3,Theme.opacity(selected||craftable||hot?Theme.accent():Theme.SECONDARY,alpha*(selected?.85:.35)),selected?1:.6);
            return true;
        }
        if(path.startsWith("recipe_book/") && (path.contains("forward")||path.contains("backward")||path.contains("filter")||path.contains("craftable")||path.contains("crafting")||path.startsWith("recipe_book/button"))) {
            RenderUtil.drawButton(g,x,y,width,height,path.contains("highlighted")||hot?1:0,alpha);
            if(path.contains("forward")||path.contains("backward")) {
                double dir=path.contains("forward")?1:-1,cx=x+width/2.0,cy=y+height/2.0;
                RenderUtil.line(g,cx-dir*2,cy-3,cx+dir*2,cy,1,Theme.opacity(Theme.TEXT,alpha));
                RenderUtil.line(g,cx+dir*2,cy,cx-dir*2,cy+3,1,Theme.opacity(Theme.TEXT,alpha));
            } else {Icon.PROFILES.drawCentered(g,x,y,width,height,Theme.opacity(path.contains("enabled")?Theme.accent():Theme.TEXT,alpha));}
            return true;
        }
        return false;
    }
}
