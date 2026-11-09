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
    private String error="",errorDraft="";
    private EditBoxDraft draft;
    private final int maximum;
    private Button errorDetails;
    private final ScreenText.Layout errorLayout=new ScreenText.Layout();
    @Override protected void repositionElements(){if(name!=null){draft=EditBoxDraft.capture(name);}super.repositionElements();}
    public ProfileSaveScreen(Screen parent,String kind,String original,Predicate<String> exists,Predicate<String> save){super(parent,"Replace "+kind+" profile: "+original+"?");this.original=original;this.exists=exists;this.save=save;this.maximum=kind.equals("appearance")?40:32;}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". Save as creates a separate profile. Replace keeps a previous version available in Profiles. "+error);}
    @Override protected void setInitialFocus(){if(name!=null){setInitialFocus(name);if(draft!=null){draft.restore(name);}}else{super.setInitialFocus();}}
    @Override protected void init(){int w=Math.min(280,width-32),left=(width-w)/2;name=addRenderableWidget(new EditBox(font,left,66,w,20,Component.literal("New profile name")));name.setMaxLength(maximum);if(draft==null){name.setValue(original);}else{draft.restore(name);}name.setResponder(value->{if(!value.equals(errorDraft)){error="";}});
        addRenderableWidget(Button.builder(Component.literal("Save as"),b->write(false)).bounds(left,112,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Replace "+original),b->write(true)).bounds(left,138,w,20).build());
        errorDetails=addRenderableWidget(Button.builder(Component.literal("Full error details"),b->minecraft.gui.setScreen(new InfoScreen(this,"Profile save error",error))).bounds(left,height-56,w,20).build());errorDetails.visible=!error.isEmpty();
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left,height-28,w,20).build());setInitialFocus(name);if(draft!=null){draft.restore(name);}}
    private void write(boolean replace){errorDraft=name.getValue();String target=replace?original:name.getValue();try{if(!replace&&exists.test(target)){error="Choose a different name; this profile already exists.";return;}if(save.test(target)){onClose();}else{error="Could not save; the previous profile is retained.";}}catch(IllegalArgumentException invalid){error=invalid.getMessage();}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);ScreenText.draw(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,title.getString(),(width-32)/.8),16,12,.8,0xFFFFFFFF);ScreenText.draw(g,font,"Previous version can be restored in Profiles.",16,36,.65,0xFFE0E8EF);ScreenText.draw(g,font,"New profile name",(width-Math.min(280,width-32))/2,52,.65,0xFFE0E8EF);ScreenText.draw(g,font,"Letters/numbers, _ or - · up to "+maximum,(width-Math.min(280,width-32))/2,90,.6,0xFFB7CBD9);errorDetails.visible=!error.isEmpty();if(!error.isEmpty()){var lines=errorLayout.lines(font,error,width-32,.65);ScreenText.draw(g,font,lines.getFirst(),16,height-80,.65,0xFFFF8899);}}
}
