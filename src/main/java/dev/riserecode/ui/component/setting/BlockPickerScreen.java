package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.ui.menu.MenuTheme;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

/** Searchable native multi-select list. Unavailable stored IDs remain visible and removable. */
public final class BlockPickerScreen extends Screen implements MenuTheme.TransientScreen {
    private final Screen parent;
    private final BlockListSetting setting;
    private final Runnable changed;
    private final List<String> registry;
    private List<String> filtered;
    private final java.util.List<Button> rows=new java.util.ArrayList<>();
    private EditBox search;
    private Button previous,next,selectedView;
    private boolean selectedOnly;
    private final java.util.Map<String,String> names=new java.util.HashMap<>();
    private final java.util.Set<String> known=new java.util.HashSet<>();
    private int page,pageSize;
    public BlockPickerScreen(Screen parent,BlockListSetting setting,Runnable changed){
        super(Component.literal("Select blocks"));this.parent=parent;this.setting=setting;this.changed=changed;
        var ids=new java.util.TreeSet<String>();BuiltInRegistries.BLOCK.keySet().forEach(id->{ids.add(id.toString());known.add(id.toString());});ids.addAll(setting.getValue());for(String id:ids){String key="block."+id.replace(':','.').replace('/','.');String translated=Component.translatable(key).getString();names.put(id,translated.equals(key)?id:translated);}
        registry=ids.stream().sorted(java.util.Comparator.comparing((String id)->names.get(id)).thenComparing(id->id)).toList();filtered=registry;
    }
    private int paneWidth(){return Math.min(420,Math.max(120,width-24));}
    @Override protected void init(){
        String query=search==null?"":search.getValue();rows.clear();pageSize=Math.clamp((height-112)/36,1,12);
        int w=paneWidth(),left=(width-w)/2,slot=(w-8)/3;
        search=addRenderableWidget(new EditBox(font,left,28,w-88,20,Component.literal("Search block names or IDs")));search.setHint(Component.literal("Search names or IDs…"));search.setMaxLength(128);search.setValue(query);
        search.setResponder(value->{page=0;filter();});
        selectedView=addRenderableWidget(Button.builder(Component.literal("Selected"),b->{selectedOnly=!selectedOnly;page=0;filter();}).bounds(left+w-84,28,84,20).build());selectedView.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Review only selected blocks")));
        for(int i=0;i<pageSize;i++){final int row=i;rows.add(addRenderableWidget(Button.builder(Component.empty(),b->{int index=page*pageSize+row;if(index<filtered.size()){setting.toggle(filtered.get(index));changed.run();filter();}}).bounds(left,56+i*36,w,20).build()));}
        previous=addRenderableWidget(Button.builder(Component.literal("Previous"),b->{page--;refresh();}).bounds(left,height-28,slot,20).build());
        next=addRenderableWidget(Button.builder(Component.literal("Next"),b->{page++;refresh();}).bounds(left+slot+4,height-28,slot,20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(left+2*(slot+4),height-28,slot,20).build());filter();setInitialFocus(search);
    }
    private void filter(){String query=search.getValue().strip().toLowerCase(Locale.ROOT);filtered=registry.stream().filter(id->(!selectedOnly||setting.getValue().contains(id))&&(id+" "+names.get(id)).toLowerCase(Locale.ROOT).contains(query)).toList();refresh();}
    private void refresh(){if(selectedView!=null){selectedView.setMessage(Component.literal((selectedOnly?"✓ ":"")+"Selected"));}page=Math.clamp(page,0,Math.max(0,(filtered.size()-1)/pageSize));for(int i=0;i<rows.size();i++){int index=page*pageSize+i;var row=rows.get(i);row.visible=index<filtered.size();if(row.visible){String id=filtered.get(index);row.setMessage(Component.literal((setting.getValue().contains(id)?"✓ ":"○ ")+names.get(id)));row.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(id+(known.contains(id)?"":" · unavailable block, retained in selection"))));}}if(previous!=null){previous.active=page>0;next.active=(page+1)*pageSize<filtered.size();}}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==266||event.key()==267){page+=event.key()==266?-1:1;refresh();return true;}return super.keyPressed(event);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(v!=0){page+=v>0?-1:1;refresh();return true;}return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractRenderState(g,mx,my,delta);for(int i=0;i<rows.size();i++){var row=rows.get(i);int index=page*pageSize+i;if(row.visible&&index<filtered.size()){dev.riserecode.util.RenderUtil.text(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,filtered.get(index),(paneWidth()-12)/.7),row.getX()+6,row.getY()+22,.7,dev.riserecode.ui.theme.Theme.SECONDARY);}}g.text(font,title,(width-paneWidth())/2,12,0xFFFFFFFF);if(filtered.isEmpty()){g.text(font,"No matches. Clear search or turn off Selected.",(width-paneWidth())/2,60,0xFFE0E8EF);}g.text(font,setting.getValue().size()+" selected · Page "+(page+1)+" / "+Math.max(1,(filtered.size()+pageSize-1)/pageSize)+" · PgUp/PgDn",(width-paneWidth())/2,height-42,0xFFE0E8EF);}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return parent==null||parent.isPauseScreen();}
}
