package dev.riserecode.ui.menu;

import dev.riserecode.Client;
import dev.riserecode.ui.component.*;
import dev.riserecode.ui.theme.*;
import dev.riserecode.setting.StringSetting;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

/** A modal editor with its own theme, so disabling the parent cannot hide the recovery controls. */
public final class PageAppearanceScreen extends Screen {
    private static Screen suspendedContainer;
    public static boolean suspends(Screen screen){return suspendedContainer==screen;}
    public static void open(Screen owner){if(owner instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>){suspendedContainer=owner;}net.minecraft.client.Minecraft.getInstance().gui.setScreen(new PageAppearanceScreen(owner,MenuTheme.pageId(owner)));}
    private final Screen parent;
    private final String target;
    private ThemeEditor editor;
    private final ActionButton done=new ActionButton("Done",this::onClose);
    private final ActionButton reset=new ActionButton("Reset this page",this::reset);
    private long previous=System.nanoTime();
    private final long opened=System.nanoTime();
    private double visualScale=1;
    private double px(double x){return width-16+(x-width+16)/visualScale;}
    private double py(double y){return 16+(y-16)/visualScale;}
    private boolean choosing;
    public void suspendForChoice(){choosing=true;}
    public void resumeFromChoice(){choosing=false;}
    private String message="";
    private final ActionButton details=new ActionButton(()->message.startsWith("Import failed")?"Import failed · Details":message.startsWith("Image imported")?"Image imported · Details":"Page status · Details",()->minecraft.gui.setScreen(new InfoScreen(this,"Page appearance message",message))).accessibleLabel(()->"Page status, "+message+". Open complete details");
    public ThemeEditor editor(){return editor;}
    public String message(){return message;}
    public ActionButton details(){return details;}
    private void notice(String text){message=text==null?"The operation could not be completed":text;}
    @Override public Component getNarrationMessage(){return Component.literal("Page appearance. "+(message.isEmpty()?"Changes save automatically. Import Image accepts a local PNG path.":message+". Page status opens complete details."));}
    private UIComponent.Bounds panel;
    private double titleY;
    public PageAppearanceScreen(Screen parent,String target){super(Component.literal("Page appearance"));this.parent=parent;this.target=target;}
    public String target(){return target;}
    private void reset(){editor=null;Client.INSTANCE.getThemeManager().resetPage(target);init();}
    @Override protected void init(){
        var manager=Client.INSTANCE.getThemeManager();var page=manager.page(target);
        var controls=new java.util.ArrayList<>(page.controls());
        if(target.equals(net.minecraft.client.gui.screens.TitleScreen.class.getName()))manager.getControls().stream().filter(c->c.group().equals("Living Panorama")).forEach(controls::add);
        if(editor==null){editor=new ThemeEditor(manager,this::notice,controls,true).sectionKey("page:"+target);
        editor.replaceActions("Background File",java.util.List.of(new ActionButton("Import Image",()->{
            FocusedInput.commit();String requested=((StringSetting)page.get("Background File")).getValue();try{
                StringSetting file=(StringSetting)page.get("Background File");
                file.setValue(BrandingLogo.importPng(manager.getDirectory(),java.nio.file.Path.of(file.getValue())));
                ThemeManager.assign(page.get("Background"),ThemeManager.Background.LOCAL_IMAGE);manager.changed();notice("Image imported for this page");
            }catch(java.io.IOException|RuntimeException invalid){notice("Import failed: "+requested+"\n"+java.util.Objects.requireNonNullElse(invalid.getMessage(),invalid.getClass().getSimpleName()));}
        })));
        }
        double w = Math.min(470, width - 32), left = (width - w) / 2;
        titleY = 10;
        double panelTop = titleY + 9 * Theme.number("Font Scale", 1) * .9 + 8;
        double panelBottom = height - 34;
        panel = new UIComponent.Bounds(left - 6, panelTop, w + 12, Math.max(1, panelBottom - panelTop));
        editor.setBounds(left, panelTop + 8, w, Math.max(1, panelBottom - panelTop - 32));
        details.setBounds(left,panelBottom-23,w,ButtonMetrics.HEIGHT);
        double buttonWidth = Math.min(100, (width - 32) / 2.0), gap = 8;
        reset.setBounds(width / 2.0 - gap / 2 - buttonWidth, height - 28, buttonWidth, 22);
        done.setBounds(width / 2.0 + gap / 2, height - 28, buttonWidth, 22);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(dev.riserecode.ui.panorama.LivingPanorama.active()&&Theme.manager().choice("Background",ThemeManager.Background.class)!=ThemeManager.Background.NONE){MenuTheme.drawBackdrop(g);return;}
        String old=PageAppearance.context();PageAppearance.context(target);try{if(Theme.manager().choice("Background",ThemeManager.Background.class)==ThemeManager.Background.NONE){super.extractBackground(g,mx,my,delta);}else{MenuBackdrop.draw(g);}}finally{PageAppearance.context(old);}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        long now=System.nanoTime();double dt=(now-previous)*1e-9;previous=now;
        double progress=dev.riserecode.ui.animation.PopIn.progress((now-opened)*1e-9,dev.riserecode.ui.animation.PopIn.windowDuration("Dialogs"));
        visualScale=.85+.15*progress;
        g.pose().pushMatrix();g.pose().translate(width-16,16);g.pose().scale((float)visualScale,(float)visualScale);g.pose().translate(16-width,-16);
        PageAppearance.editor(()->{
            RenderUtil.drawRoundedRect(g,panel.x(),panel.y(),panel.width(),panel.height(),6,Theme.opacity(Theme.BACKGROUND,.96));
            String title = "Page appearance · " + (parent instanceof dev.riserecode.ui.clickgui.ClickGui gui ? Theme.displayName(gui.getPage().name()) : parent.getTitle().getString());
            title = RenderUtil.ellipsize(font, title, editor.getBounds().width() / .9);
            RenderUtil.text(g,font,title,width / 2.0 - RenderUtil.textWidth(font,title,false) * .9 / 2,titleY,.9,Theme.TEXT);
            editor.render(g,font,px(mx),py(my),dt,1);reset.render(g,font,px(mx),py(my),dt,1);done.render(g,font,px(mx),py(my),dt,1);
            if(!message.isEmpty()){details.render(g,font,px(mx),py(my),dt,1);}
        });g.pose().popMatrix();
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){FocusedInput.pointer(px(e.x()),py(e.y()));return !message.isEmpty()&&details.mouseClicked(px(e.x()),py(e.y()),e.button())||done.mouseClicked(px(e.x()),py(e.y()),e.button())||reset.mouseClicked(px(e.x()),py(e.y()),e.button())||editor.mouseClicked(px(e.x()),py(e.y()),e.button());}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return editor.mouseDragged(px(e.x()),py(e.y()),e.button());}
    @Override public boolean mouseReleased(MouseButtonEvent e){return editor.mouseReleased(px(e.x()),py(e.y()),e.button());}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){return editor.mouseScrolled(px(x),py(y),v);}
    @Override public boolean charTyped(CharacterEvent e){return FocusedInput.character(e);}
    @Override public boolean keyPressed(KeyEvent e){
        if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui&&Client.INSTANCE.getKeybindManager().getClickGuiKey().matches(e)){onClose();gui.onClose();return true;}
        if(FocusedInput.key(e,minecraft.keyboardHandler::getClipboard,minecraft.keyboardHandler::setClipboard)){return true;}
        if(e.isEscape()){onClose();return true;}return super.keyPressed(e);
    }
    @Override public boolean isPauseScreen(){return !(parent instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>);}
    @Override public void tick(){if(parent instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container&&(minecraft.player==null||!minecraft.player.isAlive()||minecraft.player.containerMenu!=container.getMenu())){minecraft.gui.setScreen(null);}}
    @Override public void onClose(){if(suspendedContainer==parent){suspendedContainer=null;}FocusedInput.commit();editor.unfocus();Client.INSTANCE.getThemeManager().save();minecraft.gui.setScreen(parent);}
    @Override public void removed(){if(choosing){return;}if(suspendedContainer==parent){suspendedContainer=null;if(parent instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container&&minecraft.player!=null&&minecraft.player.containerMenu==container.getMenu()){minecraft.player.connection.send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(container.getMenu().containerId));minecraft.player.containerMenu=minecraft.player.inventoryMenu;}parent.removed();}FocusedInput.commit();editor.unfocus();Client.INSTANCE.getThemeManager().save();}
}
