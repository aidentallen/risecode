package dev.riserecode.ui.menu;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Searchable native option list; opening a choice never changes the saved value. */
public final class ChoiceScreen<E extends Enum<E>> extends Screen implements MenuTheme.TransientScreen {
    private final Screen parent;
    private final List<E> options;
    private final E selected;
    private final Consumer<E> choose;
    private EditBox search;
    private final java.util.List<Button> rows=new java.util.ArrayList<>();
    private Button previous,next,cancel;
    private EditBoxDraft searchDraft;
    private E focusOption;
    private String focusAction="search";
    private int page;
    private boolean returning;
    private int pageSize=6;
    private int paneWidth(){return Math.min(300,Math.max(120,width-24));}
    public ChoiceScreen(Screen parent,String label,List<E> options,E selected,Consumer<E> choose){
        super(Component.literal(label));this.parent=parent;this.options=List.copyOf(options);this.selected=selected;this.choose=choose;
        dev.riserecode.ui.component.FocusedInput.commit();
        if(parent instanceof PageAppearanceScreen editor){editor.suspendForChoice();}
        if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.suspendForChoice();}
        if(parent instanceof RetainedScreen retained){retained.suspendForChild();}
    }
    private String name(E option){return dev.riserecode.ui.theme.Theme.displayName(option.name());}
    private List<E> filtered(){String query=search.getValue().strip().toLowerCase(java.util.Locale.ROOT);return options.stream().filter(o->name(o).toLowerCase(java.util.Locale.ROOT).contains(query)).toList();}
    @Override protected void repositionElements(){
        if(search!=null){searchDraft=EditBoxDraft.capture(search);focusOption=null;focusAction=getFocused()==previous?"previous":getFocused()==next?"next":getFocused()==cancel?"cancel":"search";int row=rows.indexOf(getFocused()),index=page*pageSize+row;var values=filtered();if(row>=0&&index<values.size()){focusOption=values.get(index);}}
        super.repositionElements();
    }
    @Override protected void setInitialFocus(){
        if(search==null){return;}var values=filtered();int index=focusOption==null?-1:values.indexOf(focusOption);
        if(index>=0){setInitialFocus(rows.get(index%pageSize));}else{var target=switch(focusAction){case "previous"->previous;case "next"->next;case "cancel"->cancel;default->search;};setInitialFocus(target!=null&&target.visible&&target.active?target:search);}
        if(searchDraft!=null){search.setCursorPosition(searchDraft.cursor());search.setHighlightPos(searchDraft.highlight());}
    }
    @Override protected void init(){
        String query=search==null?"":search.getValue();rows.clear();pageSize=Math.clamp((height-112)/24,1,6);int w=paneWidth(),left=(width-w)/2,top=36,footer=height-28,slot=(w-8)/3;
        search=addRenderableWidget(new EditBox(font,left,top,w,20,Component.literal("Search options")));search.setHint(Component.literal("Search options…"));search.setValue(query);search.setResponder(value->{page=0;refresh();});
        for(int i=0;i<pageSize;i++){final int row=i;rows.add(addRenderableWidget(Button.builder(Component.empty(),b->{var values=filtered();int index=page*pageSize+row;if(index<values.size()){choose.accept(values.get(index));onClose();}}).bounds(left,top+28+i*24,w,20).build()));}
        previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;refresh();}).bounds(left,footer,slot,20).build());
        next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;refresh();}).bounds(left+slot+4,footer,slot,20).build());
        cancel=addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left+2*(slot+4),footer,slot,20).build());if(focusOption!=null){int index=filtered().indexOf(focusOption);if(index>=0){page=index/pageSize;}}refresh();setInitialFocus();
    }
    private void refresh(){var values=filtered();page=Math.clamp(page,0,Math.max(0,(values.size()-1)/pageSize));for(int i=0;i<rows.size();i++){int index=page*pageSize+i;var button=rows.get(i);button.visible=index<values.size();if(button.visible){var value=values.get(index);button.setMessage(Component.literal((value==selected?"✓ ":"")+name(value)));}}if(previous!=null){previous.active=page>0;next.active=(page+1)*pageSize<values.size();boolean paged=values.size()>pageSize;previous.visible=next.visible=paged;int w=paneWidth(),slot=(w-8)/3,cw=paged?slot:Math.min(100,w);cancel.setRectangle(cw,20,paged?(width-w)/2+2*(slot+4):(width-cw)/2,height-28);}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);double scale=Math.min(.9,12/(9*dev.riserecode.ui.theme.Theme.number("Font Scale",1)));ScreenText.draw(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,title.getString(),paneWidth()/scale),(width-paneWidth())/2,10,scale,0xFFFFFFFF);ScreenText.draw(g,font,"Search options",(width-paneWidth())/2,24,Math.min(.65,7/(9*dev.riserecode.ui.theme.Theme.number("Font Scale",1))),0xFFB7CBD9);if(filtered().isEmpty()){ScreenText.draw(g,font,"No matching options",(width-paneWidth())/2+8,search.getY()+35,Math.min(.75,1/dev.riserecode.ui.theme.Theme.number("Font Scale",1)),0xFFE0E8EF);}}
    @Override public void tick(){if(parent instanceof PageAppearanceScreen page){page.tick();}}
    @Override public boolean isPauseScreen(){return parent==null||parent.isPauseScreen();}
    @Override public void removed(){if(!returning){if(parent instanceof PageAppearanceScreen page){page.resumeFromChoice();page.removed();}if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.abandonChoice();}if(parent instanceof RetainedScreen retained){retained.resumeFromChild();retained.removed();}}}
    @Override public void onClose(){returning=true;if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.resumeFromChoice();}if(parent instanceof PageAppearanceScreen page){page.resumeFromChoice();}if(parent instanceof RetainedScreen retained){retained.resumeFromChild();}minecraft.gui.setScreen(parent);}
}
