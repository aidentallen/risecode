package dev.riserecode.ui.menu;

import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit duplicate-name policy; Save as may create only a new name. */
public final class ProfileSaveScreen extends RetainedScreen {
    private final String original;
    private final Predicate<String> exists,save;
    private EditBox name;
    private String error="";
    public ProfileSaveScreen(Screen parent,String kind,String original,Predicate<String> exists,Predicate<String> save){super(parent,"Replace "+kind+" profile: "+original+"?");this.original=original;this.exists=exists;this.save=save;}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". Save as creates a separate profile. Replace keeps a previous version available in Profiles. "+error);}
    @Override protected void init(){int w=Math.min(280,width-32),left=(width-w)/2;name=addRenderableWidget(new EditBox(font,left,66,w,20,Component.literal("New profile name")));name.setValue(original);name.setMaxLength(40);
        addRenderableWidget(Button.builder(Component.literal("Save as"),b->write(false)).bounds(left,98,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Replace "+original),b->write(true)).bounds(left,124,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left,height-28,w,20).build());setInitialFocus(name);}
    private void write(boolean replace){String target=replace?original:name.getValue();try{if(!replace&&exists.test(target)){error="Choose a different name; this profile already exists.";return;}if(save.test(target)){onClose();}else{error="Could not save; the previous profile is retained.";}}catch(IllegalArgumentException invalid){error=invalid.getMessage();}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);ScreenText.draw(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,title.getString(),(width-32)/.8),16,12,.8,0xFFFFFFFF);ScreenText.draw(g,font,"Previous version can be restored in Profiles.",16,36,.65,0xFFE0E8EF);var lines=ScreenText.wrap(font,error,width-32,.65);for(int i=0;i<Math.min(2,lines.size());i++){ScreenText.draw(g,font,lines.get(i),16,height-64+i*ScreenText.lineHeight(.65),.65,0xFFFF8899);}}
}
