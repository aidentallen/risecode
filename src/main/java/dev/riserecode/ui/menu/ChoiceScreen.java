package dev.riserecode.ui.menu;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Searchable native option list; opening a choice never changes the saved value. */
public final class ChoiceScreen<E extends Enum<E>> extends Screen {
    private final Screen parent;
    private final List<E> options;
    private final E selected;
    private final Consumer<E> choose;
    private EditBox search;
    private final java.util.List<Button> rows=new java.util.ArrayList<>();
    private Button previous,next;
    private int page;
    private boolean returning;
    private static final int PAGE=6;
    public ChoiceScreen(Screen parent,String label,List<E> options,E selected,Consumer<E> choose){
        super(Component.literal(label));this.parent=parent;this.options=List.copyOf(options);this.selected=selected;this.choose=choose;
    }
    private String name(E option){return dev.riserecode.ui.theme.Theme.displayName(option.name());}
    private List<E> filtered(){String query=search.getValue().strip().toLowerCase(java.util.Locale.ROOT);return options.stream().filter(o->name(o).toLowerCase(java.util.Locale.ROOT).contains(query)).toList();}
    @Override protected void init(){
        String query=search==null?"":search.getValue();rows.clear();int left=width/2-120,top=Math.max(28,(height-220)/2);
        search=addRenderableWidget(new EditBox(font,left,top,240,20,Component.literal("Search options")));search.setValue(query);search.setResponder(value->{page=0;refresh();});
        for(int i=0;i<PAGE;i++){final int row=i;rows.add(addRenderableWidget(Button.builder(Component.empty(),b->{var values=filtered();int index=page*PAGE+row;if(index<values.size()){choose.accept(values.get(index));onClose();}}).bounds(left,top+28+i*24,240,20).build()));}
        previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;refresh();}).bounds(left,top+178,75,20).build());
        next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;refresh();}).bounds(left+82,top+178,75,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(left+164,top+178,76,20).build());refresh();setInitialFocus(search);
    }
    private void refresh(){var values=filtered();page=Math.clamp(page,0,Math.max(0,(values.size()-1)/PAGE));for(int i=0;i<rows.size();i++){int index=page*PAGE+i;var button=rows.get(i);button.visible=index<values.size();if(button.visible){var value=values.get(index);button.setMessage(Component.literal((value==selected?"✓ ":"")+name(value)));}}if(previous!=null){previous.active=page>0;next.active=(page+1)*PAGE<values.size();}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);g.text(font,title,width/2-120,Math.max(12,(height-220)/2-15),0xFFFFFFFF);if(filtered().isEmpty()){g.text(font,"No matching options",width/2-110,search.getY()+35,0xFFE0E8EF);}}
    @Override public void tick(){if(parent instanceof PageAppearanceScreen page){page.tick();}}
    @Override public boolean isPauseScreen(){return parent==null||parent.isPauseScreen();}
    @Override public void removed(){if(!returning&&parent instanceof PageAppearanceScreen page){page.resumeFromChoice();page.removed();}}
    @Override public void onClose(){returning=true;if(parent instanceof PageAppearanceScreen page){page.resumeFromChoice();}minecraft.gui.setScreen(parent);}
}
