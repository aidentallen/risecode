package dev.riserecode.ui.component;

import dev.riserecode.resources.*;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

/** Searchable resource draft with stable actions, measured views and logical keyboard reveal. */
public final class ResourcesEditor extends UIComponent {
    private record Card(ResourceEntry entry,ActionButton action) { }
    private record Row(String id,ActionButton up,ActionButton down,ActionButton remove,ActionButton detail) { }
    private final ResourcesManager manager;
    private final Consumer<String> notice;
    private final TextField search=new TextField(()->"Search resources…");
    private final List<String> selected=new ArrayList<>();
    private final List<Card> cards=new ArrayList<>();
    private final List<Row> rows=new ArrayList<>();
    private final List<ActionButton> categoryButtons=new ArrayList<>();
    private final Map<String,ActionButton> cardActions=new HashMap<>(),categoryActions=new HashMap<>();
    private final Map<String,Row> rowActions=new HashMap<>();
    private final ActionButton category,tools,selectedView,cache,stop,refresh,about,apply,cancel,back,choose,clear;
    private final SettingsSection maintenanceSection=new SettingsSection("resources:tools","Catalog Tools",FocusedInput::commit);
    private ResourceCatalog known;
    private String revision="",query="",categoryName="All categories",dragging,focused;
    private ResourceEntry detail;
    private boolean showSelected,categoryMenu,toolsMenu;
    private long menuOpened;
    private Object detailReturnFocus;
    private double returnScroll,menuScale=1,scroll,maxScroll,bodyTop,available,selectedStep=64,selectedHeader=24,detailHeader=24;
    private double lineHeight=13,cardStep=66,cardHeight=60,footerHeight=78;
    private boolean compactCards,compactFooter;
    private List<Bounds> selectedButtons=List.of();
    private final dev.riserecode.ui.animation.Animation scrolling=dev.riserecode.ui.animation.Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING,0,.18,dev.riserecode.util.MathUtil.Easing.OUT_QUART);
    private String detailLayout="";
    private List<String> detailLines=List.of();
    public ResourcesEditor(ResourcesManager manager,Consumer<String> notice){
        this.manager=manager;this.notice=notice;search.setMaximum(160);
        tools=new ActionButton("Tools",()->{toolsMenu=!toolsMenu;categoryMenu=false;detail=null;menuOpened=System.nanoTime();resetScroll();if(!toolsMenu){KeyboardNavigation.requestFocus(toolsButton());}});
        category=new ActionButton(()->categoryName,()->{categoryMenu=!categoryMenu;toolsMenu=false;detail=null;menuOpened=System.nanoTime();resetScroll();});
        selectedView=new ActionButton(()->showSelected?"Browse":"Selected ("+selected.size()+")",()->{showSelected=!showSelected;categoryMenu=toolsMenu=false;detail=null;resetScroll();rebuild();KeyboardNavigation.requestFocus(showSelected&&!rows.isEmpty()?rows.getFirst().detail():!showSelected&&!cards.isEmpty()?cards.getFirst().action():selectedViewButton());});
        cache=new ActionButton(()->manager.library().progress().running()?manager.library().progress().paused()?"Resume":"Pause":manager.library().progress().ready()?"Check cache":"Cache all",()->{if(manager.library().progress().running()){manager.library().pause();}else{manager.library().start(false);}});
        stop=new ActionButton("Stop",()->manager.library().cancel()).enabled(()->manager.library().progress().running());
        refresh=new ActionButton("Refresh",()->manager.library().start(true)).enabled(()->!manager.library().progress().running());
        about=new ActionButton("Vanilla Tweaks",()->notice.accept("Vanilla Tweaks · vanillatweaks.net"));
        apply=new ActionButton(()->manager.busy()?"Loading…":"Apply",this::apply).enabled(()->!manager.busy()&&(selected.isEmpty()||manager.library().progress().ready()));
        cancel=new ActionButton("Cancel",()->{FocusedInput.cancel();resetDraft();notice.accept("Unapplied resource changes discarded");});
        back=new ActionButton("Back",this::closeDetail).accessibleLabel(()->detail==null?"Back":"Back from "+detail.name()+". "+detail.description());
        choose=new ActionButton(()->detail!=null&&selected.contains(detail.id())?"Deselect":"Select",()->{if(detail!=null){toggle(detail);}});
        clear=new ActionButton("Clear",()->{selected.clear();focused=null;rebuild();KeyboardNavigation.requestFocus(selectedView);});resetDraft();
    }
    private ActionButton toolsButton(){return tools;}
    private ActionButton selectedViewButton(){return selectedView;}
    public TextField search(){return search;}
    public void focusSearch(){detail=null;toolsMenu=categoryMenu=showSelected=false;resetScroll();search.setFocused(true);search.selectAll();}
    public boolean isDirty(){return !selected.equals(manager.state().selected());}
    public ResourcesState draft(){return selected.isEmpty()?ResourcesState.empty():new ResourcesState(revision,selected);}
    public ActionButton getToolsButton(){return tools;}
    public SettingsSection getToolsSection(){return maintenanceSection;}
    public double scrollPosition(){return scroll;}
    public double scrollDestination(){return scrolling.getTarget();}
    public double maximumScroll(){return maxScroll;}
    public Bounds getBodyBounds(){return new Bounds(x,bodyTop,width,available);}
    public void open(){if(!manager.library().progress().ready()&&!manager.library().progress().running()){manager.library().start(false);}}
    private void resetScroll(){scroll=0;scrolling.reset(0);}
    public void resetDraft(){selected.clear();selected.addAll(manager.state().selected());revision=manager.state().revision();dragging=null;detail=null;resetScroll();rebuild();}
    public void apply(){FocusedInput.commit();manager.apply(draft()).thenAccept(result->{notice.accept(result.message());if(result.success()){resetDraft();}});}
    private String name(String id){var entry=known==null?null:known.byId().get(id);return entry==null?id:entry.name();}
    public void toggle(ResourceEntry entry){
        var catalog=manager.library().catalog();if(catalog==null){return;}
        if(!revision.equals(catalog.revision())&&selected.stream().anyMatch(id->!catalog.byId().containsKey(id))){notice.accept("Remove unavailable selections before using the refreshed catalog");return;}
        int previousIndex=selected.indexOf(entry.id());
        revision=catalog.revision();if(!selected.remove(entry.id())){selected.addFirst(entry.id());}focused=entry.id();rebuild();
        if(showSelected&&detail!=null&&previousIndex>=0){focused=selected.isEmpty()?null:selected.get(Math.min(previousIndex,selected.size()-1));detailReturnFocus=focused==null?selectedView:rowActions.get(focused).detail();}
        if(!conflicts(entry).isEmpty()){notice.accept("Overlap · use Selected for top-first priority");}
    }
    public void move(String id,int offset){
        int index=selected.indexOf(id);if(index<0){return;}int target=Math.clamp(index+offset,0,selected.size()-1);
        if(index!=target){selected.remove(index);selected.add(target,id);focused=id;rebuild();}
    }
    private void remove(String id){int index=selected.indexOf(id);if(index<0){return;}selected.remove(index);focused=selected.isEmpty()?null:selected.get(Math.min(index,selected.size()-1));rebuild();KeyboardNavigation.requestFocus(focused==null?selectedView:rowActions.get(focused).detail());}
    private void openDetail(String id,Object opener){var entry=known==null?null:known.byId().get(id);if(entry==null){notice.accept("Resource details unavailable");return;}returnScroll=scroll;detailReturnFocus=opener;detail=entry;focused=id;resetScroll();KeyboardNavigation.requestFocus(back);}
    private void closeDetail(){detail=null;scroll=returnScroll;scrolling.reset(returnScroll);KeyboardNavigation.requestFocus(detailReturnFocus==null?selectedView:detailReturnFocus);}
    public void showSelected(){showSelected=true;detail=null;categoryMenu=toolsMenu=false;resetScroll();rebuild();}
    public void setCategory(String name){categoryName=name;categoryMenu=toolsMenu=showSelected=false;detail=null;resetScroll();rebuild();KeyboardNavigation.requestFocus(cards.isEmpty()?category:cards.getFirst().action());}
    private List<String> conflicts(ResourceEntry entry){var catalog=manager.library().catalog();if(catalog==null){return List.of();}return selected.stream().filter(id->!id.equals(entry.id())&&(entry.incompatible().contains(id)||catalog.byId().containsKey(id)&&catalog.byId().get(id).incompatible().contains(entry.id()))).map(this::name).toList();}
    private Row makeRow(String id){
        return new Row(id,new ActionButton("Up",()->{move(id,-1);var row=rowActions.get(id);KeyboardNavigation.requestFocus(selected.indexOf(id)>0?row.up():row.detail());}).accessibleLabel(()->name(id)+", move resource up"),
            new ActionButton("Down",()->{move(id,1);var row=rowActions.get(id);KeyboardNavigation.requestFocus(selected.indexOf(id)+1<selected.size()?row.down():row.detail());}).accessibleLabel(()->name(id)+", move resource down"),
            new ActionButton("×",()->remove(id)).accessibleLabel(()->name(id)+", remove resource"),
            new ActionButton("Details",()->openDetail(id,rowActions.get(id).detail())).accessibleLabel(()->name(id)+", resource details"));
    }
    private void rebuild(){
        known=manager.library().catalog();cards.clear();rows.clear();categoryButtons.clear();var categories=new LinkedHashSet<String>();categories.add("All categories");
        if(known!=null){
            for(var entry:known.entries()){categories.add(entry.category().split(" / ")[0]);categories.add(entry.category());}
            String text=search.getValue().strip().toLowerCase(Locale.ROOT);
            for(var entry:known.entries()){if((categoryName.equals("All categories")||entry.category().equals(categoryName)||entry.category().startsWith(categoryName+" / "))&&entry.searchText().contains(text)){
                var action=cardActions.computeIfAbsent(entry.id(),id->new ActionButton(()->name(id),()->{var current=known==null?null:known.byId().get(id);if(current!=null){toggle(current);}}).tile());cards.add(new Card(entry,action));
            }}
            cardActions.keySet().removeIf(id->!known.byId().containsKey(id));
        }
        for(String name:categories){categoryButtons.add(categoryActions.computeIfAbsent(name,key->new ActionButton(key,()->setCategory(key))));}
        categoryActions.keySet().retainAll(categories);for(String id:selected){rows.add(rowActions.computeIfAbsent(id,this::makeRow));}rowActions.keySet().retainAll(selected);
    }
    private double line(double scale){return Math.ceil(9*Theme.number("Font Scale",1)*scale)+4;}
    private double menuX(double mx){return x+width/2+(mx-x-width/2)/menuScale;}
    private double menuY(double my){double center=bodyTop+available/2;return center+(my-center)/menuScale;}
    private void reveal(Bounds bounds){double shift=bounds.y()<bodyTop?bounds.y()-bodyTop:Math.max(0,bounds.y()+bounds.height()-bodyTop-available);if(shift!=0){scroll=Math.clamp(scroll+shift,0,maxScroll);scrolling.reset(scroll);}}
    private double detailContentHeight(){return (compactCards?line(.78)*2+line(.7):100)+(detailLines.isEmpty()?0:(detailLines.size()-1)*lineHeight+Math.ceil(9*Theme.number("Font Scale",1)*.72)+2);}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        if(known!=manager.library().catalog()||!query.equals(search.getValue())){query=search.getValue();rebuild();}
        double inputHeight=SettingMetrics.fieldHeight();var filters=ButtonMetrics.flow(font,List.of(category.label(),selectedView.label(),tools.label()),width);
        double filterHeight=ButtonMetrics.flowHeight(filters),filterY=y+inputHeight+5;
        boolean detailView=detail!=null&&!toolsMenu&&!categoryMenu;
        if(!detailView){search.setBounds(x,y,width,inputHeight);search.render(g,font,mx,my,dt,alpha);
        var buttons=List.of(category,selectedView,tools);for(int i=0;i<buttons.size();i++){var b=filters.get(i);buttons.get(i).setBounds(x+b.x(),filterY+b.y(),b.width(),b.height());buttons.get(i).render(g,font,mx,my,dt,alpha);}}
        bodyTop=detailView?y:filterY+filterHeight+5;compactFooter=height<240||width<260;footerHeight=compactFooter?ButtonMetrics.HEIGHT+line(.7)+8:ButtonMetrics.HEIGHT+3*line(.7)+10;
        available=Math.max(1,y+height-footerHeight-bodyTop);compactCards=width<260||available<140;lineHeight=line(.72);
        cardHeight=compactCards?Math.max(34,line(.78)+14):Math.max(68,38+line(.71)+line(.7));cardStep=cardHeight+7;
        selectedButtons=ButtonMetrics.flow(font,List.of("Details","Up","Down","×"),Math.max(1,width-12));selectedHeader=available<100?0:Math.max(24,line(.7)+6);selectedStep=Math.max(28,2*line(.75))+ButtonMetrics.flowHeight(selectedButtons)+14;detailHeader=ButtonMetrics.HEIGHT+8;
        int columns=compactCards?1:Math.clamp((int)((width+8)/150),1,6);
        double toolsBody=4*ButtonMetrics.rowHeight()+line(.7)+8;
        if(toolsMenu){maxScroll=Math.max(0,SettingsSection.HEADER+maintenanceSection.bodyHeight(toolsBody)-available);}
        else if(categoryMenu){maxScroll=Math.max(0,categoryButtons.size()*ButtonMetrics.rowHeight()-available);}
        else if(detail!=null){layoutDetail(font);maxScroll=Math.max(0,detailContentHeight()-Math.max(1,available-detailHeader));}
        else if(showSelected){maxScroll=Math.max(0,rows.size()*selectedStep+(selectedHeader==0?Math.max(24,line(.7)+6):0)-Math.max(1,available-selectedHeader));}
        else{maxScroll=Math.max(0,((cards.size()+columns-1)/columns)*cardStep-available);}
        if(scrolling.getValue()>maxScroll){scrolling.reset(maxScroll);}scrolling.animateTo(Math.clamp(scrolling.getTarget(),0,maxScroll));scroll=scrolling.update(dt);
        double rawMx=mx,rawMy=my,baseAlpha=alpha;boolean dropdown=toolsMenu||categoryMenu;
        double progress=dropdown?dev.riserecode.ui.animation.PopIn.progress((System.nanoTime()-menuOpened)*1e-9,dev.riserecode.ui.animation.PopIn.windowDuration("Dropdowns")):1;
        menuScale=dropdown?dev.riserecode.ui.animation.PopIn.windowScale("Dropdowns",progress):1;
        g.pose().pushMatrix();g.pose().translate((float)(x+width/2),(float)(bodyTop+available/2));g.pose().scale((float)menuScale,(float)menuScale);g.pose().translate((float)(-x-width/2),(float)(-bodyTop-available/2));mx=menuX(mx);my=menuY(my);alpha*=progress;
        KeyboardNavigation.pushScrollRegion(this::reveal);RenderUtil.scissorPush(g,x,bodyTop,width,available);
        try{
            if(toolsMenu){
                double top=bodyTop-scroll;maintenanceSection.setBounds(x,top,width,SettingsSection.HEADER);maintenanceSection.render(g,font,mx,my,dt,alpha);double body=maintenanceSection.bodyHeight(toolsBody);
                RenderUtil.scissorPush(g,x,top+SettingsSection.HEADER,width,body);
                try{var toolsList=List.of(cache,stop,refresh,about);for(int i=0;i<toolsList.size();i++){var button=toolsList.get(i);button.setBounds(x,top+SettingsSection.HEADER+i*ButtonMetrics.rowHeight(),width,ButtonMetrics.HEIGHT);button.render(g,font,maintenanceSection.isExpanded()?mx:-1e6,maintenanceSection.isExpanded()?my:-1e6,dt,alpha*maintenanceSection.amount(toolsBody));}RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Cache, pause, cancel or refresh",width/.7),x+4,top+SettingsSection.HEADER+4*ButtonMetrics.rowHeight(),.7,Theme.opacity(Theme.SECONDARY,alpha*maintenanceSection.amount(toolsBody)));}finally{RenderUtil.scissorPop(g);}
            }else if(categoryMenu){for(int i=0;i<categoryButtons.size();i++){var button=categoryButtons.get(i);button.setBounds(x,bodyTop+i*ButtonMetrics.rowHeight()-scroll,width-3,ButtonMetrics.HEIGHT);if(KeyboardNavigation.collectingOffscreen()||button.getBounds().y()+ButtonMetrics.HEIGHT>=bodyTop&&button.getBounds().y()<bodyTop+available){button.render(g,font,mx,my,dt,alpha);}}}
            else if(detail!=null){renderDetail(g,font,mx,my,dt,alpha);}
            else if(showSelected){renderSelected(g,font,mx,my,dt,alpha);}
            else{double cardWidth=(width-(columns-1)*8)/columns;for(int i=0;i<cards.size();i++){var card=cards.get(i);double cx=x+i%columns*(cardWidth+8),cy=bodyTop+i/columns*cardStep-scroll;card.action().setBounds(cx,cy,cardWidth,cardHeight);if(!KeyboardNavigation.collectingOffscreen()&&(cy+cardHeight<bodyTop||cy>=bodyTop+available)){continue;}drawCard(g,font,card,cx,cy,cardWidth,mx,my,alpha);}if(cards.isEmpty()){RenderUtil.text(g,font,RenderUtil.ellipsize(font,known==null?"Loading resource catalog…":"No matching resources",width/.8),x+4,bodyTop+8,.8,Theme.opacity(Theme.SECONDARY,alpha));}}
        }finally{RenderUtil.scissorPop(g);KeyboardNavigation.popScrollRegion();g.pose().popMatrix();}
        mx=rawMx;my=rawMy;alpha=baseAlpha;
        if(maxScroll>0){double thumb=Math.max(12,available*available/(available+maxScroll));RenderUtil.drawRoundedRect(g,x+width-3,bodyTop+scroll/maxScroll*(available-thumb),2,thumb,1,Theme.opacity(Theme.accent(),alpha*.65));}
        drawFooter(g,font,mx,my,dt,alpha);
    }
    private void drawCard(GuiGraphicsExtractor g,Font font,Card card,double cx,double cy,double w,double mx,double my,double alpha){
        var entry=card.entry();boolean picked=selected.contains(entry.id());
        KeyboardNavigation.control(g,card.action(),new Bounds(cx,cy,w,cardHeight),alpha,()->"Resource, "+entry.name()+", "+(selected.contains(entry.id())?"selected":"unselected")+", activate to toggle, arrows for details",card.action()::activate,ignored->openDetail(entry.id(),card.action()));
        if(cy+cardHeight<bodyTop||cy>=bodyTop+available){return;}
        double press=card.action().pressScale();g.pose().pushMatrix();g.pose().translate((float)(cx+w/2),(float)(cy+cardHeight/2));g.pose().scale((float)press,(float)press);g.pose().translate((float)(-cx-w/2),(float)(-cy-cardHeight/2));
        try{
            RenderUtil.drawRoundedRect(g,cx,cy,w,cardHeight,5,Theme.opacity(card.action().contains(mx,my)?Theme.HOVER:Theme.SURFACE,alpha));if(picked){RenderUtil.drawRoundedBorder(g,cx,cy,w,cardHeight,5,Theme.opacity(Theme.accent(),alpha),1);}
            if(compactCards){ResourcesPreviews.draw(g,manager.library(),entry,false,cx+4,cy+4,26,alpha);RenderUtil.text(g,font,RenderUtil.ellipsize(font,(picked?"✓ ":"")+entry.name(),Math.max(1,w-60)/.78),cx+34,cy+(cardHeight-9*Theme.number("Font Scale",1)*.78)/2,.78,Theme.opacity(Theme.TEXT,alpha));RenderUtil.text(g,font,"›",cx+w-18,cy+8,.9,Theme.opacity(Theme.SECONDARY,alpha));}
            else{ResourcesPreviews.draw(g,manager.library(),entry,false,cx+5,cy+4,30,alpha);RenderUtil.text(g,font,RenderUtil.ellipsize(font,picked?"Selected":"› Details",Math.max(1,w-45)/.7),cx+40,cy+7,.7,Theme.opacity(picked?Theme.accent():Theme.SECONDARY,alpha));RenderUtil.text(g,font,RenderUtil.ellipsize(font,entry.name(),(w-10)/.78),cx+5,cy+38,.78,Theme.opacity(Theme.TEXT,alpha));}
            if(card.action().contains(mx,my)){g.setTooltipForNextFrame(font,List.of(net.minecraft.network.chat.Component.literal(entry.name()),net.minecraft.network.chat.Component.literal(entry.description()),net.minecraft.network.chat.Component.literal("Click to toggle · › / right-click for details")),Optional.empty(),(int)mx,(int)my);}
        }finally{g.pose().popMatrix();}
    }
    private void renderSelected(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        double lead=Math.max(24,line(.7)+6),headerY=bodyTop-(selectedHeader==0?scroll:0);
        clear.setBounds(x+width-ButtonMetrics.width(font,"Clear"),headerY,ButtonMetrics.width(font,"Clear"),ButtonMetrics.HEIGHT);clear.render(g,font,mx,my,dt,alpha);RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Top wins · Ctrl+↑/↓",Math.max(1,width-clear.getBounds().width()-8)/.7),x+3,headerY+4,.7,Theme.opacity(Theme.SECONDARY,alpha));
        double viewport=Math.max(0,available-selectedHeader),top=bodyTop+selectedHeader;
        KeyboardNavigation.pushScrollRegion(bounds->{double shift=bounds.y()<top?bounds.y()-top:Math.max(0,bounds.y()+bounds.height()-top-viewport);if(shift!=0){scroll=Math.clamp(scroll+shift,0,maxScroll);scrolling.reset(scroll);}});RenderUtil.scissorPush(g,x,top,width,viewport);
        try{for(int i=0;i<rows.size();i++){var row=rows.get(i);double at=bodyTop+lead+i*selectedStep-scroll,titleHeight=Math.max(28,2*line(.75));var actions=List.of(row.detail(),row.up(),row.down(),row.remove());for(int j=0;j<actions.size();j++){var b=selectedButtons.get(j);actions.get(j).setBounds(x+6+b.x(),at+titleHeight+4+b.y(),b.width(),b.height());}if(!KeyboardNavigation.collectingOffscreen()&&(at+selectedStep<top||at>=top+viewport)){continue;}
            if(at+selectedStep<top||at>=top+viewport){row.up().enabled(()->selected.indexOf(row.id())>0);row.down().enabled(()->selected.indexOf(row.id())+1<selected.size());for(var action:actions){action.render(g,font,mx,my,dt,alpha);}continue;}
            RenderUtil.drawRoundedRect(g,x,at,width-4,selectedStep-5,4,Theme.opacity(row.id().equals(focused)?Theme.HOVER:Theme.SURFACE,alpha));var entry=known==null?null:known.byId().get(row.id());if(entry!=null){ResourcesPreviews.draw(g,manager.library(),entry,false,x+4,at+5,28,alpha);}
            String label=(i+1)+". "+name(row.id());var lines=wrap(font,label,Math.max(1,width-44),.75);for(int j=0;j<Math.min(2,lines.size());j++){RenderUtil.text(g,font,RenderUtil.ellipsize(font,lines.get(j),(width-44)/.75),x+38,at+4+j*line(.75),.75,Theme.opacity(Theme.TEXT,alpha));}
            row.up().enabled(()->selected.indexOf(row.id())>0);row.down().enabled(()->selected.indexOf(row.id())+1<selected.size());for(var action:actions){action.render(g,font,mx,my,dt,alpha);}
        }}finally{RenderUtil.scissorPop(g);KeyboardNavigation.popScrollRegion();}
    }
    private void renderDetail(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        back.setBounds(x,bodyTop,ButtonMetrics.width(font,"Back"),ButtonMetrics.HEIGHT);choose.setBounds(x+width-ButtonMetrics.width(font,choose.label()),bodyTop,ButtonMetrics.width(font,choose.label()),ButtonMetrics.HEIGHT);back.render(g,font,mx,my,dt,alpha);choose.render(g,font,mx,my,dt,alpha);
        double top=bodyTop+detailHeader;RenderUtil.scissorPush(g,x,top,width,Math.max(0,available-detailHeader));
        try{double cy=top-scroll;double textStart;if(compactCards){RenderUtil.text(g,font,RenderUtil.ellipsize(font,detail.name(),(width-8)/.78),x+4,cy+3,.78,Theme.opacity(Theme.TEXT,alpha));RenderUtil.text(g,font,RenderUtil.ellipsize(font,detail.category(),(width-8)/.7),x+4,cy+line(.78),.7,Theme.opacity(Theme.SECONDARY,alpha));textStart=cy+2*line(.78)+line(.7);}
            else{ResourcesPreviews.draw(g,manager.library(),detail,true,x+3,cy,88,alpha);RenderUtil.text(g,font,RenderUtil.ellipsize(font,detail.name(),(width-101)/.78),x+99,cy+5,.78,Theme.opacity(Theme.TEXT,alpha));RenderUtil.text(g,font,RenderUtil.ellipsize(font,detail.category(),(width-101)/.7),x+99,cy+line(.78)+8,.7,Theme.opacity(Theme.SECONDARY,alpha));textStart=cy+100;}
            for(int i=0;i<detailLines.size();i++){RenderUtil.text(g,font,detailLines.get(i),x+4,textStart+i*lineHeight,.72,Theme.opacity(Theme.TEXT,alpha));}
        }finally{RenderUtil.scissorPop(g);}
    }
    private List<String> wrap(Font font,String text,double span,double scale){var result=new ArrayList<String>();String line="";for(String word:text.split("\\s+")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&RenderUtil.textWidth(font,next,false)*scale>span){result.add(line);line=word;}else{line=next;}}if(!line.isEmpty()){result.add(line);}return List.copyOf(result);}
    private void layoutDetail(Font font){String text=detail.description();var overlap=conflicts(detail);if(!overlap.isEmpty()){text+=" Overlaps selected: "+String.join(", ",overlap)+". Top priority wins.";}String key=text+width+Theme.number("Font Scale",1)+dev.riserecode.ui.font.CustomTextRenderer.generation()+font.hashCode();if(!key.equals(detailLayout)){detailLayout=key;detailLines=wrap(font,text,Math.max(1,width-8),.72);}}
    private String applyHelp(){return manager.busy()?"Applying resources…":!selected.isEmpty()&&!manager.library().progress().ready()?"Cache resources in Tools before Apply":isDirty()?"Unapplied changes":"Applied";}
    private void drawFooter(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        var progress=manager.library().progress();String status=manager.busy()?manager.status():progress.phase();if(progress.running()&&progress.total()>0){status=(progress.paused()?"Paused · ":"")+status+" "+progress.completed()+"/"+progress.total();}
        double applyWidth=ButtonMetrics.width(font,apply.label()),cancelWidth=ButtonMetrics.width(font,"Cancel"),buttonsWidth=applyWidth+cancelWidth+5,footerTop=y+height-footerHeight;
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,status,width/.7),x,footerTop,.7,Theme.opacity(Theme.SECONDARY,alpha));
        if(!compactFooter){RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Vanilla Tweaks · vanillatweaks.net",width/.7),x,footerTop+line(.7),.7,Theme.opacity(Theme.SECONDARY,alpha));RenderUtil.text(g,font,RenderUtil.ellipsize(font,applyHelp(),width/.7),x,footerTop+2*line(.7),.7,Theme.opacity(Theme.SECONDARY,alpha));}
        else{RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Vanilla Tweaks",Math.max(1,width-buttonsWidth-5)/.7),x,y+height-15,.7,Theme.opacity(Theme.SECONDARY,alpha));}
        apply.setBounds(x+width-buttonsWidth,y+height-ButtonMetrics.HEIGHT,applyWidth,ButtonMetrics.HEIGHT);cancel.setBounds(x+width-cancelWidth,y+height-ButtonMetrics.HEIGHT,cancelWidth,ButtonMetrics.HEIGHT);apply.render(g,font,mx,my,dt,alpha);cancel.render(g,font,mx,my,dt,alpha);
        if(my>=footerTop&&my<y+height){g.setTooltipForNextFrame(font,List.of(net.minecraft.network.chat.Component.literal(status),net.minecraft.network.chat.Component.literal(applyHelp()),net.minecraft.network.chat.Component.literal("Vanilla Tweaks · vanillatweaks.net")),Optional.empty(),(int)mx,(int)my);}
    }
    public boolean keyPressed(KeyEvent event){
        if(detail!=null&&(event.key()==266||event.key()==267)){double viewport=Math.max(1,available-detailHeader);double step=Math.max(lineHeight,(Math.floor(viewport/lineHeight)-1)*lineHeight);scrolling.animateTo(Math.clamp(scrolling.getTarget()+(event.key()==266?-step:step),0,maxScroll));return true;}
        if(event.isEscape()&&detail!=null){closeDetail();return true;}if(event.isEscape()&&(toolsMenu||categoryMenu)){toolsMenu=categoryMenu=false;resetScroll();KeyboardNavigation.requestFocus(tools);return true;}
        if(showSelected&&detail==null&&!categoryMenu&&!FocusedInput.active()&&(event.key()==265||event.key()==264)&&!selected.isEmpty()){
            if(event.hasControlDown()&&focused!=null){move(focused,event.key()==265?-1:1);KeyboardNavigation.requestFocus(rowActions.get(focused).detail());}
            else{int index=selected.indexOf(focused);focused=selected.get(index<0?0:Math.clamp(index+(event.key()==265?-1:1),0,selected.size()-1));KeyboardNavigation.requestFocus(rowActions.get(focused).detail());}return true;
        }return false;
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(!contains(mx,my)){return false;}if(detail==null){if(search.mouseClicked(mx,my,button)){return true;}for(var action:List.of(selectedView,category,tools)){if(action.mouseClicked(mx,my,button)){return true;}}}for(var action:List.of(apply,cancel)){if(action.mouseClicked(mx,my,button)){return true;}}
        if(my<bodyTop||my>=bodyTop+available){return true;}if(toolsMenu||categoryMenu){mx=menuX(mx);my=menuY(my);}
        if(toolsMenu){if(maintenanceSection.mouseClicked(mx,my,button)){return true;}double total=4*ButtonMetrics.rowHeight()+line(.7)+8;if(!maintenanceSection.isExpanded()||my<maintenanceSection.getBounds().y()+SettingsSection.HEADER||my>=maintenanceSection.getBounds().y()+SettingsSection.HEADER+maintenanceSection.bodyHeight(total)){return true;}for(var action:List.of(cache,stop,refresh,about)){if(action.mouseClicked(mx,my,button)){return true;}}}
        else if(categoryMenu){for(var action:categoryButtons){if(action.mouseClicked(mx,my,button)){return true;}}}
        else if(detail!=null){if(back.mouseClicked(mx,my,button)||choose.mouseClicked(mx,my,button)){return true;}}
        else if(showSelected){if(clear.mouseClicked(mx,my,button)){return true;}if(my<bodyTop+selectedHeader){return true;}for(var row:List.copyOf(rows)){for(var action:List.of(row.detail(),row.up(),row.down(),row.remove())){if(action.mouseClicked(mx,my,button)){return true;}}}int index=(int)((my-bodyTop-Math.max(24,line(.7)+6)+scroll)/selectedStep);if(button==0&&index>=0&&index<selected.size()){dragging=selected.get(index);focused=dragging;return true;}}
        else{for(var card:cards){if(card.action().contains(mx,my)){if(button==1||mx>=card.action().getBounds().x()+card.action().getBounds().width()-24){openDetail(card.entry().id(),card.action());}else{card.action().mouseClicked(mx,my,button);}return true;}}}return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button){if(dragging==null||button!=0||!showSelected){return false;}int index=selected.indexOf(dragging),target=Math.clamp((int)((my-bodyTop-Math.max(24,line(.7)+6)+scroll)/selectedStep),0,selected.size()-1);if(index!=target){selected.remove(index);selected.add(target,dragging);rebuild();}return true;}
    @Override public boolean mouseReleased(double mx,double my,int button){boolean held=dragging!=null;dragging=null;return held;}
    @Override public boolean mouseScrolled(double mx,double my,double amount){if(!contains(mx,my)){return false;}scrolling.animateTo(Math.clamp(scrolling.getTarget()-amount*24,0,maxScroll));return true;}
    public void unfocus(){search.setFocused(false);dragging=null;}
    public void discard(){unfocus();resetDraft();ResourcesPreviews.close();}
}
