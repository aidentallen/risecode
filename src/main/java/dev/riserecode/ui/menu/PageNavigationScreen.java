package dev.riserecode.ui.menu;

import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.clickgui.ClickGui.Page;
import dev.riserecode.ui.theme.Theme;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

/** Full named destinations, using the native scrolling/focus container rather than icon discovery. */
public final class PageNavigationScreen extends RetainedScreen {
    private static final List<Page> SETTINGS=List.of(Page.PROFILES,Page.FRIENDS,Page.CLIENT_SOUNDS,Page.DISCORD_ACTIVITY,Page.GUI_SCALE);
    private final ClickGui owner;
    private final Map<Page,Button> entries=new LinkedHashMap<>();
    private final LinearLayout rows=LinearLayout.vertical().spacing(4);
    private EditBox search;
    private ScrollableLayout list;
    private Button back;
    private Button clear;
    private boolean empty;
    private final ScreenText.Layout emptyText=new ScreenText.Layout();
    public PageNavigationScreen(ClickGui owner){super(owner,"Pages");this.owner=owner;}
    private static String name(Page page){return switch(page){case CLIENT_SOUNDS->"Sounds";case DISCORD_ACTIVITY->"Discord activity";case GUI_SCALE->"Interface scale";case MAP_SETTINGS->"Map settings";default->Theme.displayName(page.name());};}
    private static String group(Page page){return SETTINGS.contains(page)?"Client settings":ClickGui.NAVIGATION_PAGES.indexOf(page)>=11?"Utilities":"Pages";}
    private static String label(Page page){return name(page)+(group(page).equals("Pages")?"":" · "+group(page));}
    @Override public Component getNarrationMessage(){return Component.literal("Pages. Current: "+name(owner.getPage())+". Search or scroll the named destinations. Selecting a destination preserves the usual Apply or discard decision. Back keeps the current page.");}
    @Override protected void setInitialFocus(){if(search!=null){setInitialFocus(search);}}
    @Override protected void init(){
        search=addRenderableWidget(new EditBox(font,0,54,200,20,Component.literal("Find a page")));
        search.setHint(Component.literal("Find a page…"));search.setMaxLength(80);
        clear=addRenderableWidget(Button.builder(Component.literal("Clear"),b->{search.setValue("");setInitialFocus(search);}).bounds(0,60,60,20).build());
        var pages=new ArrayList<>(ClickGui.NAVIGATION_PAGES);pages.addAll(SETTINGS);
        for(Page page:pages){entries.put(page,Button.builder(Component.literal((page==owner.getPage()?"✓ ":"")+label(page)),b->{onClose();owner.selectPage(page);}).width(240).build());}
        list=new ScrollableLayout(minecraft,rows,Math.max(24,height-124),ScrollableLayout.ReserveStrategy.RIGHT);
        list.visitWidgets(this::addRenderableWidget);
        back=addRenderableWidget(Button.builder(Component.literal("Back to "+name(owner.getPage())),b->onClose()).bounds(0,0,220,20).build());
        search.setResponder(value->rebuild());rebuild();position();
    }
    private void rebuild(){
        if(list==null){return;}rows.removeChildren();String query=search.getValue().strip().toLowerCase(Locale.ROOT);empty=true;
        for(var entry:entries.entrySet()){if((name(entry.getKey())+" "+group(entry.getKey())).toLowerCase(Locale.ROOT).contains(query)){rows.addChild(entry.getValue());empty=false;}}
        clear.active=!search.getValue().isEmpty();position();list.visitWidgets(widget->{if(widget instanceof AbstractContainerWidget container){container.setScrollAmount(0);}});
    }
    private void position(){int width=Math.min(420,this.width-32),left=(this.width-width)/2,available=Math.max(24,height-130);
        search.setRectangle(width-68,20,left,60);clear.setRectangle(60,20,left+width-60,60);entries.values().forEach(button->button.setWidth(width-12));
        list.setMinWidth(width-12);list.setMinHeight(available);list.setMaxHeight(available);list.arrangeElements();list.setX(left);list.setY(88);
        back.setRectangle(Math.min(250,width),20,(this.width-Math.min(250,width))/2,height-28);
    }
    @Override protected void repositionElements(){position();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);ScreenText.draw(g,font,"Pages",16,10,.9,0xFFFFFFFF);
        ScreenText.draw(g,font,"Current: "+name(owner.getPage()),16,32,.65,0xFFE0E8EF);
        ScreenText.draw(g,font,"Find a page",(width-Math.min(420,width-32))/2,48,.55,0xFFB7CBD9);
        ScreenText.draw(g,font,"Scroll or Tab to a destination",16,height-40,.5,0xFFB7CBD9);
        if(empty){var lines=emptyText.lines(font,"No matching pages. Clear the search.",width-40,.65);for(int i=0;i<lines.size();i++){ScreenText.draw(g,font,lines.get(i),20,96+i*ScreenText.lineHeight(.65),.65,0xFFE0E8EF);}}
    }
}
