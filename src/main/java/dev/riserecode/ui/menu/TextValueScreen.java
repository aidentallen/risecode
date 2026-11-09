package dev.riserecode.ui.menu;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Expanded advanced-value draft; wrapped full text remains available without caret scrolling. */
public final class TextValueScreen extends RetainedScreen {
    private EditBox input;
    private EditBoxDraft draft;
    private final Consumer<EditBoxDraft> use;
    private final int maximum;
    private int focused;
    private final ScreenText.Layout layout=new ScreenText.Layout();
    public TextValueScreen(Screen parent,String title,EditBoxDraft draft,int maximum,Consumer<EditBoxDraft> use){super(parent,title);this.draft=draft;this.maximum=maximum;this.use=use;}
    public EditBox input(){return input;}
    private void capture(){if(input!=null){draft=EditBoxDraft.capture(input);focused=children().indexOf(getFocused());}}
    @Override protected void repositionElements(){capture();super.repositionElements();}
    @Override public void removed(){capture();super.removed();}
    @Override protected void init(){
        input=addRenderableWidget(new EditBox(font,16,48,width-32,20,Component.literal(title.getString())));input.setMaxLength(maximum);draft.restore(input);
        addRenderableWidget(Button.builder(Component.literal("Use value"),b->{var value=EditBoxDraft.capture(input);onClose();use.accept(value);}).bounds(16,height-28,Math.min(100,(width-40)/2),20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(width-Math.min(100,(width-40)/2)-16,height-28,Math.min(100,(width-40)/2),20).build());
        addRenderableWidget(Button.builder(Component.literal("Full value"),b->minecraft.gui.setScreen(new InfoScreen(this,title.getString(),input.getValue()))).bounds(16,height-54,100,20).build());
    }
    @Override protected void setInitialFocus(){if(input==null){return;}setInitialFocus(focused>=0&&focused<children().size()?children().get(focused):input);input.setCursorPosition(draft.cursor());input.setHighlightPos(draft.highlight());}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". Edits stay in the waypoint draft until Save. Full value opens all text. "+(input==null?draft.value():input.getValue()));}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){MenuBackdrop.draw(g);dev.riserecode.util.RenderUtil.readabilitySurface(g,8,6,width-16,height-66,8,1);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){super.extractRenderState(g,mx,my,dt);ScreenText.draw(g,font,title.getString(),16,10,.8,0xFFFFFFFF);ScreenText.draw(g,font,"Draft only · Save the waypoint to commit",16,30,.55,0xFFCED8E1);var lines=layout.lines(font,input.getValue(),width-32,.65);double line=ScreenText.lineHeight(.65);int visible=Math.max(0,(int)((height-158)/line));for(int i=0;i<Math.min(visible,lines.size());i++){ScreenText.draw(g,font,lines.get(i),16,80+i*line,.65,0xFFCED8E1);}}
}
