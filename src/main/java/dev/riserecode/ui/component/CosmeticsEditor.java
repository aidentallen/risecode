package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.cosmetics.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.component.setting.*;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Draft wardrobe. Preview and downloading are deliberately independent of equipped appearance. */
public final class CosmeticsEditor extends UIComponent {
    private final CosmeticsManager manager;
    private final Consumer<String> notice;
    private CosmeticLoadout draft;
    private CosmeticCategory category=CosmeticCategory.CAPE;
    private String selected="";
    private SettingsSection itemSection;
    private final Object rotationFocus=new Object(),zoomFocus=new Object();
    private boolean catalog,editing,dragging;
    private double listScroll,settingsScroll,maxList,maxSettings,previousX,yaw=160,zoom=1;
    private double previewTime;
    private boolean previewPaused;
    public void advancePreview(double seconds){if(!previewPaused&&!Theme.bool("Reduced Motion",false)){previewTime+=Math.min(.05,Math.max(0,seconds));}}
    public double previewTime(){return previewTime;}
    public boolean previewPaused(){return previewPaused;}
    public void previewPaused(boolean value){previewPaused=value;}
    public PreviewPose previewPose(){return PreviewPose.values()[pose];}
    public void previewPose(PreviewPose value){pose=value.ordinal();}
    public double previewYaw(){return yaw;}
    public double previewZoom(){return zoom;}
    private double defaultPreviewYaw(){return category==CosmeticCategory.CAPE||category==CosmeticCategory.WINGS||category==CosmeticCategory.TAIL?160:25;}
    public void resetPreviewView(){yaw=defaultPreviewYaw();zoom=1;}

    private int pose;
    public enum PreviewPose { STANDING, WALKING, CROUCHING, FLYING }
    private Bounds previewBounds,listBounds;
    private final TextField search=new TextField(()->"Search cosmetics");
    private final TextField importPath=new TextField(()->"PNG cape path");
    private final List<ActionButton> top=new ArrayList<>(),bottom=new ArrayList<>();
    private final List<UIComponent> controls=new ArrayList<>();
    private final Map<String,DoubleSetting> numbers=new LinkedHashMap<>();
    private ColorSetting primary,secondary;
    private UIComponent captured;
    private final List<ActionButton> rows=new ArrayList<>();
    private final Map<String,ActionButton> rowCache=new HashMap<>();
    private final ActionButton poseButton=new ActionButton(()->Theme.displayName(PreviewPose.values()[pose].name())+" ›",()->openChoice("Preview pose",PreviewPose.values(),PreviewPose.values()[pose],value->pose=value.ordinal()));
    public ActionButton poseButton(){return poseButton;}
    public List<UIComponent> getControls(){return List.copyOf(controls);}
    private <E extends Enum<E>> void openChoice(String title,E[] values,E value,Consumer<E> choose){var mc=Minecraft.getInstance();mc.gui.setScreen(new dev.riserecode.ui.menu.ChoiceScreen<>(mc.gui.screen(),title,List.of(values),value,choose));}
    private boolean importing;
    private String importStatus="";
    private final ActionButton importButton=new ActionButton(()->importing?"Importing…":"Import PNG cape",this::importCape).enabled(()->!importing);
    private final ActionButton importStatusDetails=new ActionButton(()->importStatus.startsWith("Importing")?"Importing cape…":importStatus.startsWith("Import failed")?"Import failed · Details":"Cape import · Details",()->{var mc=Minecraft.getInstance();mc.gui.setScreen(new dev.riserecode.ui.menu.InfoScreen(mc.gui.screen(),"Cape import",importStatus));}).accessibleLabel(()->"Cape import status. "+importStatus);
    private final ActionButton detailsButton=new ActionButton("Import a cape",()->{var mc=Minecraft.getInstance();mc.gui.setScreen(new CosmeticDetailsScreen(mc.gui.screen(),this));});
    private final ActionButton clearSearch=new ActionButton("Clear search",()->{search.setValue("");listScroll=0;KeyboardNavigation.requestFocus(search);});
    private final ActionButton showCatalog=new ActionButton("Show catalog",()->{catalog=true;listScroll=0;});
    private boolean empty;
    private final ActionButton downloadButton;
    private long revision;
    private void download(){var d=manager.library().find(selected);if(manager.library().busy()){manager.library().cancel();}else if(d!=null){manager.library().download(d);}}

    public CosmeticsEditor(CosmeticsManager manager,Consumer<String> notice){
        this.manager=manager;this.notice=notice;draft=manager.outfit();downloadButton=new ActionButton(()->manager.library().busy()?"Cancel download":"Download",this::download);
        top.add(new ActionButton(()->category.label+" >",()->openChoice("Cosmetic category",CosmeticCategory.values(),category,this::category)));
        top.add(new ActionButton(()->catalog?"Catalog":"Installed",()->{catalog=!catalog;listScroll=0;}));
        top.add(new ActionButton("Refresh",()->{manager.retry();manager.library().refresh();}));
        top.add(new ActionButton(()->"Visible: "+(draft.visible()?"On":"Off"),()->draft=new CosmeticLoadout(!draft.visible(),draft.items())));
        bottom.add(new ActionButton("Apply",this::apply).enabled(this::isDirty));bottom.add(new ActionButton("Cancel",this::discard));
        bottom.add(new ActionButton(()->editing?"Browse":"Edit",()->{if(!selected.isEmpty()){editing=!editing;settingsScroll=0;buildControls();}}).enabled(()->!selected.isEmpty()&&draft.items().containsKey(category)));
        bottom.add(new ActionButton("Remove",()->{draft=draft.with(category,null);selected="";editing=false;}));
        search.onChange(v->listScroll=0);
    }
    public boolean isDirty(){return !draft.equals(manager.outfit());}
    public void open(){if(!isDirty()){discard();}}
    public void discard(){FocusedInput.commit();revision++;draft=manager.outfit();selected=Optional.ofNullable(draft.items().get(category)).map(CosmeticLoadout.Item::id).orElse("");editing=false;search.setFocused(false);importPath.setFocused(false);captured=null;}
    public void apply(){FocusedInput.commit();manager.apply(draft);revision++;notice.accept("Cosmetics applied");}
    public boolean isEditing(){return editing;}
    public SettingsSection getItemSection(){return itemSection;}
    public CosmeticLoadout draft(){return draft;}
    public CosmeticCategory category(){return category;}
    public void category(CosmeticCategory value){category=value;yaw=value==CosmeticCategory.CAPE||value==CosmeticCategory.WINGS||value==CosmeticCategory.TAIL?160:25;selected=Optional.ofNullable(draft.items().get(category)).map(CosmeticLoadout.Item::id).orElse("");editing=false;listScroll=0;settingsScroll=0;buildControls();}
    public TextField search(){return search;}
    public Bounds previewBounds(){return previewBounds;}
    public List<ActionButton> toolbar(){return List.copyOf(top);}
    public List<ActionButton> buttons(){return List.copyOf(bottom);}
    public void select(String id){
        var def=manager.library().find(id);if(def==null){return;}if(category!=def.category()){yaw=def.category()==CosmeticCategory.CAPE||def.category()==CosmeticCategory.WINGS||def.category()==CosmeticCategory.TAIL?160:25;}selected=id;category=def.category();settingsScroll=0;
        if(manager.library().available(def)){var old=draft.items().get(category);draft=draft.with(category,old!=null&&old.id().equals(id)?old:CosmeticLoadout.Item.defaults(id));buildControls();manager.request(id);}
    }
    private void buildControls(){
        controls.clear();numbers.clear();itemSection=new SettingsSection("cosmetic:"+selected+":settings",(manager.library().find(selected)==null?"Item":manager.library().find(selected).name())+" · Settings",()->{if(captured!=null){captured.mouseReleased(0,0,0);captured=null;}FocusedInput.commit();});var item=draft.items().get(category);if(item==null){return;}
        primary=new ColorSetting("Primary color","Cosmetic tint",item.primary());secondary=new ColorSetting("Secondary color","Accent tint",item.secondary());
        controls.add(new ColorPicker(primary,this::updateItem));if(category!=CosmeticCategory.CAPE&&category!=CosmeticCategory.COMPANION){controls.add(new ColorPicker(secondary,this::updateItem));}
        number("Opacity",item.opacity(),0,1);number("Scale",item.scale(),.25,3);number("Offset X",item.x(),-1,1);number("Offset Y",item.y(),-1,1);number("Offset Z",item.z(),-1,1);
        if(category!=CosmeticCategory.CAPE&&category!=CosmeticCategory.HEAD&&category!=CosmeticCategory.TRAIL){number("Animation speed",item.speed(),0,4);if(category==CosmeticCategory.AURA||category==CosmeticCategory.PARTICLES){numbers.put("Motion strength",DoubleSetting.extended("Motion strength","",item.motion(),0,3));}else{number("Motion strength",item.motion(),0,3);}}
        else{numbers.put("Animation speed",DoubleSetting.extended("Animation speed","",item.speed(),0,4));numbers.put("Motion strength",DoubleSetting.extended("Motion strength","",item.motion(),0,3));}
        if(category==CosmeticCategory.TRAIL||category==CosmeticCategory.AURA||category==CosmeticCategory.PARTICLES){number("Density",item.density(),0,3);}else{numbers.put("Density",DoubleSetting.extended("Density","",item.density(),0,3));}
        controls.add(new ActionButton("Reset item",()->{draft=draft.with(category,CosmeticLoadout.Item.defaults(item.id()));buildControls();}));
    }
    private void number(String label,double value,double min,double max){var setting=DoubleSetting.extended(label,"Cosmetic appearance",value,min,max);numbers.put(label,setting);controls.add(new Slider(setting,this::updateItem,.01));}
    private double value(String name){return numbers.get(name).getValue();}
    private void updateItem(){var item=draft.items().get(category);if(item==null||numbers.size()<8){return;}draft=draft.with(category,new CosmeticLoadout.Item(item.id(),primary.getValue(),secondary.getValue(),value("Opacity"),value("Scale"),value("Offset X"),value("Offset Y"),value("Offset Z"),value("Animation speed"),value("Motion strength"),value("Density")));}
    public boolean importing(){return importing;}
    /** One pending import and completion policy across inline and retained acquisition routes. */
    public void importCape(Path path,java.util.function.BooleanSupplier ownsImport,Consumer<String> completion){
        if(importing){completion.accept("A cape import is already in progress");return;}importing=true;
        long token=revision;var before=draft;String previous=selected;var previousCategory=category;
        importStatus="Importing cape…";
        manager.library().importCape(path).whenComplete((definition,failure)->Minecraft.getInstance().execute(()->{
            importing=false;String result;
            if(failure!=null){
                Throwable error=failure;while((error instanceof java.util.concurrent.CompletionException||error instanceof java.util.concurrent.ExecutionException)&&error.getCause()!=null){error=error.getCause();}
                String cause=error instanceof java.nio.file.NoSuchFileException missing?"File not found: "+Objects.requireNonNullElse(missing.getFile(),"selected file"):error instanceof java.nio.file.AccessDeniedException denied?"Permission denied: "+Objects.requireNonNullElse(denied.getFile(),"selected file"):error.getMessage();
                result="Import failed: "+(cause==null||cause.isBlank()?error.getClass().getSimpleName():cause);
            }else if(token==revision&&draft.equals(before)&&selected.equals(previous)&&category==previousCategory&&ownsImport.getAsBoolean()){
                select(definition.id());result="Cape imported and selected in draft";
            }else{result="Cape imported to library; outfit was left unchanged";}
            importStatus=result;notice.accept(result);completion.accept(result);
        }));
    }
    private boolean ownsInlineImport(String source){
        var screen=Minecraft.getInstance().gui.screen();var gui=Client.INSTANCE.getClickGui();
        return gui.getPage()==dev.riserecode.ui.clickgui.ClickGui.Page.COSMETICS&&gui.getCosmeticsEditor()==this&&importPath.getValue().equals(source)&&(screen==gui||screen instanceof dev.riserecode.ui.menu.RetainedScreen retained&&retained.retains(gui));
    }
    private void importCape(){
        if(importing){return;}FocusedInput.commit();String source=importPath.getValue();
        try{
            if(source.isBlank()){throw new IllegalArgumentException("Empty PNG path");}var path=Path.of(source);notice.accept("Importing cape…");
            importCape(path,()->ownsInlineImport(source),result->{});
        }catch(RuntimeException invalid){importing=false;importStatus="Import failed: Enter a valid PNG cape path";notice.accept(importStatus);}
    }
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        advancePreview(dt);boolean compactEdit=editing&&height<220;if(compactEdit&&search.isFocused()){search.commit();}if(height<220&&importPath.isFocused()){importPath.commit();}double gap=4,header=compactEdit?0:48,footer=height<220&&!editing?66:43;
        if(!compactEdit){
        search.setBounds(x,y,width*.43-2,20);search.render(g,font,mx,my,dt,alpha);
        top.get(1).setBounds(x+width*.43,y,width*.28-2,20);top.get(2).setBounds(x+width*.71,y,width*.29,20);
        top.get(0).setBounds(x,y+24,width*.48-2,20);top.get(3).setBounds(x+width*.48,y+24,width*.52,20);
        for(var button:top){button.render(g,font,mx,my,dt,alpha);}}
        boolean compactLayout=height<220;double leftWidth=compactLayout?width:width*.49-2;listBounds=new Bounds(x,y+header,leftWidth,Math.max(20,height-header-footer));
        previewBounds=compactLayout?new Bounds(0,0,0,0):new Bounds(x+leftWidth+gap,y+header,width-leftWidth-gap,listBounds.height());
        rows.clear();
        if(!compactLayout){RenderUtil.drawRoundedRect(g,previewBounds.x(),previewBounds.y(),previewBounds.width(),previewBounds.height(),4,Theme.opacity(Theme.SURFACE,alpha));
        preview(g,font,mx,my,alpha);}
        RenderUtil.scissorPush(g,listBounds.x(),listBounds.y(),listBounds.width(),listBounds.height());
        try{
            if(editing){
                itemSection.setBounds(listBounds.x(),listBounds.y(),listBounds.width()-4,SettingsSection.HEADER);itemSection.render(g,font,mx,my,dt,alpha);
                double total=controls.stream().mapToDouble(c->SettingMetrics.height(c,23)+5).sum(),body=itemSection.bodyHeight(total),bodyTop=listBounds.y()+SettingsSection.HEADER,viewport=Math.max(0,listBounds.height()-SettingsSection.HEADER);
                maxSettings=Math.max(0,total-viewport);settingsScroll=Math.clamp(settingsScroll,0,maxSettings);
                if(body>.01){
                    KeyboardNavigation.pushScrollRegion(bounds->{double shift=bounds.y()<bodyTop?bounds.y()-bodyTop:Math.max(0,bounds.y()+bounds.height()-bodyTop-viewport);if(shift!=0){settingsScroll=Math.clamp(settingsScroll+shift,0,maxSettings);}});
                    RenderUtil.scissorPush(g,listBounds.x(),bodyTop,listBounds.width(),Math.min(body,viewport));
                    try{double cy=bodyTop-settingsScroll;for(var control:controls){double ch=SettingMetrics.height(control,23);control.setBounds(listBounds.x(),cy,listBounds.width()-4,ch);if(KeyboardNavigation.collectingOffscreen()||cy+ch>bodyTop&&cy<bodyTop+viewport){control.render(g,font,itemSection.isExpanded()?mx:-1e6,itemSection.isExpanded()&&my>=bodyTop&&my<bodyTop+viewport?my:-1e6,dt,alpha*itemSection.amount(total));}cy+=ch+5;}}
                    finally{RenderUtil.scissorPop(g);KeyboardNavigation.popScrollRegion();}
                }
            }
            else{
                var entries=manager.library().definitions().stream().filter(d->d.category()==category&&(catalog||manager.library().available(d))&&d.name().toLowerCase(Locale.ROOT).contains(search.getValue().toLowerCase(Locale.ROOT))).toList();
                empty=entries.isEmpty();

                double rowHeight=Math.max(39,9*Theme.number("Font Scale",1)*1.5+18);
                maxList=Math.max(0,(empty?9*Theme.number("Font Scale",1)*.7+5+ButtonMetrics.flowHeight(ButtonMetrics.flow(font,List.of("Clear search","Show catalog"),listBounds.width())):entries.size()*rowHeight)-listBounds.height());listScroll=Math.clamp(listScroll,0,maxList);
                KeyboardNavigation.pushScrollRegion(bounds->{double shift=bounds.y()<listBounds.y()?bounds.y()-listBounds.y():Math.max(0,bounds.y()+bounds.height()-listBounds.y()-listBounds.height());if(shift!=0){listScroll=Math.clamp(listScroll+shift,0,maxList);}});
                try{
                if(empty){double line=9*Theme.number("Font Scale",1)*.7+5;RenderUtil.text(g,font,RenderUtil.ellipsize(font,"No matches in "+category.label+" · "+(catalog?"Catalog":"Installed"),listBounds.width()/.7),listBounds.x(),listBounds.y()+2-listScroll,.7,Theme.TEXT);var layout=ButtonMetrics.flow(font,List.of("Clear search","Show catalog"),listBounds.width());for(int i=0;i<2;i++){var b=layout.get(i);var action=i==0?clearSearch:showCatalog;action.setBounds(listBounds.x()+b.x(),listBounds.y()+line+b.y()-listScroll,b.width(),b.height());action.render(g,font,mx,my,dt,alpha);}}
                for(int i=0;i<entries.size();i++){var def=entries.get(i);double cy=listBounds.y()+i*rowHeight-listScroll;if(!KeyboardNavigation.collectingOffscreen()&&(cy+rowHeight<=listBounds.y()||cy>=listBounds.y()+listBounds.height())){continue;}
                    var button=rowCache.computeIfAbsent(def.id(),key->new ActionButton("Preview "+def.name(),()->openPreview(key)));boolean thumbnail=!def.preview().isEmpty()&&CosmeticsEditor.class.getResource("/assets/riserecode/cosmetics/"+def.preview())!=null;
                    double iconSpace=thumbnail?22:0;button.setBounds(listBounds.x()+iconSpace,cy,listBounds.width()-3-iconSpace,Math.max(23,9*Theme.number("Font Scale",1)*.8+10));
                    if(thumbnail){g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,CosmeticsManager.id(def.preview()),(int)listBounds.x(),(int)cy+1,0,0,20,20,128,128,128,128,Theme.opacity(-1,alpha));}button.render(g,font,mx,my>=listBounds.y()&&my<listBounds.y()+listBounds.height()?my:-1e6,dt,alpha);rows.add(button);
                    if(isDraftSelected(def.id())){RenderUtil.drawRoundedBorder(g,listBounds.x(),cy,listBounds.width()-3,23,3,Theme.opacity(Theme.accent(),alpha),1);}
                    String detail=(isDraftSelected(def.id())?"In draft":manager.library().available(def)?"Installed":"Download needed")+" · "+def.creator()+" · "+def.license();
                    if(button.contains(mx,my)&&my>=listBounds.y()&&my<listBounds.y()+listBounds.height()){
                        g.setTooltipForNextFrame(font,List.of(net.minecraft.network.chat.Component.literal(def.name()+" · "+def.creator()),net.minecraft.network.chat.Component.literal(def.license()),net.minecraft.network.chat.Component.literal(def.source())),Optional.empty(),(int)mx,(int)my);
                    }RenderUtil.text(g,font,RenderUtil.ellipsize(font,detail,(listBounds.width()-5)/.7),listBounds.x()+2,cy+Math.max(26,9*Theme.number("Font Scale",1)*.8+13),.7,Theme.opacity(Theme.SECONDARY,alpha));
                }
                }finally{KeyboardNavigation.popScrollRegion();}
            }
        }finally{RenderUtil.scissorPop(g);}
        double maximum=editing?maxSettings:maxList,scroll=editing?settingsScroll:listScroll;
        if(maximum>0){double thumb=Math.max(12,listBounds.height()*listBounds.height()/(listBounds.height()+maximum));RenderUtil.drawRoundedRect(g,listBounds.x()+listBounds.width()-2,listBounds.y()+scroll/maximum*(listBounds.height()-thumb),1.5,thumb,.75,Theme.opacity(Theme.accent(),alpha*.6));}
        if(compactLayout&&!editing){detailsButton.setBounds(x,y+height-63,width,20);detailsButton.render(g,font,mx,my,dt,alpha);}
        double bottomY=y+height-20;for(int i=0;i<bottom.size();i++){var b=bottom.get(i);b.setBounds(x+i*(width/4),bottomY,width/4-3,20);b.render(g,font,mx,my,dt,alpha);}
        String status=(isDirty()?"Unapplied changes":"No unapplied changes")+" · "+manager.library().status();if(manager.failed(selected)){status="Asset unavailable · Refresh to retry";}
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,status,width/.65),x,y+height-34,.65,Theme.opacity(Theme.SECONDARY,alpha));
    }
    public boolean isDraftSelected(String id){var def=manager.library().find(id);if(def==null){return false;}var item=draft.items().get(def.category());return item!=null&&item.id().equals(id);}
    public void openPreview(String id){var def=manager.library().find(id);if(def==null){return;}if(manager.library().available(def)){manager.request(id);}var mc=Minecraft.getInstance();mc.gui.setScreen(CosmeticDetailsScreen.preview(mc.gui.screen(),this,id));}
    public ActionButton detailsButton(){return detailsButton;}
    public long revision(){return revision;}
    public String selectedId(){return selected;}
    public CosmeticsManager manager(){return manager;}
    public void turn(double amount){yaw+=amount;}
    public void zoomBy(double amount){zoom=Math.clamp(zoom+amount,.5,1.6);}
    public void renderAvatar(GuiGraphicsExtractor g,Bounds area){renderAvatar(g,area,"");}
    public void renderAvatar(GuiGraphicsExtractor g,Bounds area,String inspected){
        var shown=draft;var definition=manager.library().find(inspected);if(definition!=null&&manager.library().available(definition)){var item=draft.items().get(definition.category());shown=draft.with(definition.category(),item!=null&&item.id().equals(inspected)?item:CosmeticLoadout.Item.defaults(inspected));shown=new CosmeticLoadout(true,shown.items());}
        var mc=Minecraft.getInstance();var state=new AvatarRenderState();state.entityType=EntityTypes.PLAYER;state.scale=state.ageScale=1;state.boundingBoxWidth=.6F;state.boundingBoxHeight=pose==3?.6F:pose==2?1.5F:1.8F;state.eyeHeight=1.62F;
        state.skin=mc.player==null?DefaultPlayerSkin.getDefaultSkin():mc.player.getSkin();state.showCape=state.showHat=state.showJacket=state.showLeftSleeve=state.showRightSleeve=state.showLeftPants=state.showRightPants=true;
        state.lightCoords=15728880;state.bodyRot=180;state.pose=pose==2?Pose.CROUCHING:pose==3?Pose.FALL_FLYING:Pose.STANDING;state.isCrouching=pose==2;state.isFallFlying=pose==3;state.fallFlyingTimeInTicks=pose==3?100:0;
        state.ageInTicks=(float)previewTime*20;state.walkAnimationPos=(float)previewTime*4;state.walkAnimationSpeed=pose==1?.7F:0;state.capeLean=pose==1?35:0;state.capeFlap=pose==2?25:0;
        var bridge=(CosmeticRenderState)state;bridge.risecode$outfit(shown);bridge.risecode$preview(true);manager.patchCape(state,shown);
        var capePhysics=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.CapePhysicsModule.class).orElseThrow();
        if(capePhysics.isEnabled()){((dev.riserecode.render.LegacyCapeState)state).risecode$legacyCape(new dev.riserecode.render.LegacyCapeMotion.Angles(state.capeFlap,state.capeLean,0));}
        double top=area.y(),bottom=area.y()+area.height();
        var rotation=new Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.toRadians(yaw));
        float size=(float)Math.min(area.width()/3,(bottom-top)/2.5)* (float)zoom;
        if(bottom>top&&size>0){g.entity(state,size,new Vector3f(0,state.boundingBoxHeight/2,0),rotation,new Quaternionf(),(int)area.x(),(int)top,(int)(area.x()+area.width()),(int)bottom);}
    }
    private void preview(GuiGraphicsExtractor g,Font font,double mx,double my,double alpha){
        boolean showImportStatus=category==CosmeticCategory.CAPE&&!importStatus.isEmpty();
        renderAvatar(g,new Bounds(previewBounds.x(),previewBounds.y()+22,previewBounds.width(),Math.max(0,previewBounds.height()-65-(showImportStatus?22:0))));double top=previewBounds.y()+22,bottom=previewBounds.y()+previewBounds.height()-43;
        KeyboardNavigation.control(g,rotationFocus,new Bounds(previewBounds.x()+4,top+2,previewBounds.width()-8,15),alpha,
                () -> "Cosmetic preview, rotation, "+Math.round(yaw),()->yaw=defaultPreviewYaw(),direction->yaw+=direction*15);
        KeyboardNavigation.control(g,zoomFocus,new Bounds(previewBounds.x()+4,top+19,previewBounds.width()-8,15),alpha,
                () -> "Cosmetic preview, zoom, "+zoom,()->zoom=1,direction->zoom=Math.clamp(zoom+direction*.08,.5,1.6));
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Tab: rotate / zoom",(previewBounds.width()-16)/.65),previewBounds.x()+8,top+5,.65,Theme.opacity(Theme.SECONDARY,alpha));
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Arrows: adjust",(previewBounds.width()-16)/.65),previewBounds.x()+8,top+22,.65,Theme.opacity(Theme.SECONDARY,alpha));
        poseButton.setBounds(previewBounds.x()+3,previewBounds.y()+2,previewBounds.width()-6,18);poseButton.render(g,font,mx,my,0.016,alpha);rows.add(poseButton);
        var selectedDef=manager.library().find(selected);
        if(showImportStatus){importStatusDetails.setBounds(previewBounds.x()+3,bottom-20,previewBounds.width()-6,18);importStatusDetails.render(g,font,mx,my,.016,alpha);rows.add(importStatusDetails);}
        if(selectedDef!=null&&!manager.library().available(selectedDef)){
            downloadButton.setBounds(previewBounds.x()+3,bottom+2,previewBounds.width()-6,20);downloadButton.render(g,font,mx,my,.016,alpha);rows.add(downloadButton);
        }else if(category==CosmeticCategory.CAPE){importPath.setBounds(previewBounds.x()+3,bottom+2,previewBounds.width()-6,18);importPath.render(g,font,mx,my,.016,alpha);
            var button=importButton;button.setBounds(previewBounds.x()+3,bottom+23,previewBounds.width()-6,18);button.render(g,font,mx,my,.016,alpha);rows.add(button);
        }else{RenderUtil.text(g,font,"Drag to rotate",previewBounds.x()+5,bottom+5,.6,Theme.opacity(Theme.SECONDARY,alpha));RenderUtil.text(g,font,"Scroll to zoom",previewBounds.x()+5,bottom+17,.6,Theme.opacity(Theme.SECONDARY,alpha));}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(!contains(mx,my)){return false;}FocusedInput.pointer(mx,my);
        if(height<220&&!editing&&detailsButton.mouseClicked(mx,my,button)){return true;}
        if(!editing&&empty){if(clearSearch.mouseClicked(mx,my,button)||showCatalog.mouseClicked(mx,my,button)){return true;}}
        if(!(editing&&height<220)){for(var b:top){if(b.mouseClicked(mx,my,button)){return true;}}}for(var b:bottom){if(b.mouseClicked(mx,my,button)){return true;}}
        for(var b:List.copyOf(rows)){if(b.contains(mx,my)&&(inside(listBounds,mx,my)||inside(previewBounds,mx,my))&&b.mouseClicked(mx,my,button)){return true;}}
        if(!(editing&&height<220)&&search.mouseClicked(mx,my,button)){return true;}if(previewBounds.width()>0&&category==CosmeticCategory.CAPE&&importPath.mouseClicked(mx,my,button)){return true;}
        if(editing&&inside(listBounds,mx,my)){
            if(itemSection.mouseClicked(mx,my,button)){return true;}if(!itemSection.isExpanded()||my<listBounds.y()+SettingsSection.HEADER){return true;}
            double body=itemSection.bodyHeight(controls.stream().mapToDouble(c->SettingMetrics.height(c,23)+5).sum());if(my>=listBounds.y()+SettingsSection.HEADER+Math.min(body,listBounds.height()-SettingsSection.HEADER)){return true;}
            for(var control:controls){if(control.mouseClicked(mx,my,button)){captured=control;return true;}}}
        if(inside(previewBounds,mx,my)&&button==0){dragging=true;previousX=mx;return true;}return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button){if(dragging){yaw+=(mx-previousX)*2;previousX=mx;return true;}return captured!=null&&captured.mouseDragged(mx,my,button);}
    @Override public boolean mouseReleased(double mx,double my,int button){dragging=false;if(captured!=null){captured.mouseReleased(mx,my,button);captured=null;}return true;}
    @Override public boolean mouseScrolled(double mx,double my,double amount){if(!contains(mx,my)){return false;}if(inside(previewBounds,mx,my)){zoom=Math.clamp(zoom+amount*.1,.5,2);}else if(editing){settingsScroll=Math.clamp(settingsScroll-amount*25,0,maxSettings);}else{listScroll=Math.clamp(listScroll-amount*25,0,maxList);}return true;}
    private static boolean inside(Bounds b,double x,double y){return b!=null&&x>=b.x()&&x<b.x()+b.width()&&y>=b.y()&&y<b.y()+b.height();}
    public boolean keyPressed(KeyEvent event){if(event.hasControlDown()&&event.key()==70){FocusedInput.commit();editing=false;search.setFocused(true);search.selectAll();return true;}return false;}
}
