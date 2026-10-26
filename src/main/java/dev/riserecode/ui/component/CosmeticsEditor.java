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
    private boolean catalog,editing,dragging;
    private double listScroll,settingsScroll,maxList,maxSettings,previousX,yaw=160,zoom=1;
    private double previewTime;
    private int pose;
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
    private final ActionButton poseButton=new ActionButton(()->new String[]{"Standing","Walking","Crouching","Flying"}[pose],()->pose=(pose+1)%4);
    private final ActionButton importButton=new ActionButton("Import PNG cape",this::importCape);
    private final ActionButton downloadButton;
    private long revision;
    private void download(){var d=manager.library().find(selected);if(manager.library().busy()){manager.library().cancel();}else if(d!=null){manager.library().download(d);}}

    public CosmeticsEditor(CosmeticsManager manager,Consumer<String> notice){
        this.manager=manager;this.notice=notice;draft=manager.outfit();downloadButton=new ActionButton(()->manager.library().busy()?"Cancel download":"Download",this::download);
        top.add(new ActionButton(()->category.label+" >",()->{category(CosmeticCategory.values()[(category.ordinal()+1)%CosmeticCategory.values().length]);}));
        top.add(new ActionButton(()->catalog?"Catalog":"Installed",()->{catalog=!catalog;listScroll=0;}));
        top.add(new ActionButton("Refresh",()->{manager.retry();manager.library().refresh();}));
        top.add(new ActionButton(()->"Visible: "+(draft.visible()?"On":"Off"),()->draft=new CosmeticLoadout(!draft.visible(),draft.items())));
        bottom.add(new ActionButton("Apply",this::apply));bottom.add(new ActionButton("Cancel",this::discard));
        bottom.add(new ActionButton(()->editing?"Browse":"Edit",()->{if(!selected.isEmpty()){editing=!editing;settingsScroll=0;buildControls();}}).enabled(()->!selected.isEmpty()&&draft.items().containsKey(category)));
        bottom.add(new ActionButton("Remove",()->{draft=draft.with(category,null);selected="";editing=false;}));
        search.onChange(v->listScroll=0);
    }
    public void open(){discard();}
    public void discard(){FocusedInput.commit();revision++;draft=manager.outfit();selected=Optional.ofNullable(draft.items().get(category)).map(CosmeticLoadout.Item::id).orElse("");editing=false;search.setFocused(false);importPath.setFocused(false);captured=null;}
    public void apply(){FocusedInput.commit();manager.apply(draft);revision++;notice.accept("Cosmetics applied");}
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
        controls.clear();numbers.clear();itemSection=new SettingsSection("cosmetic:"+selected+":settings","Item Settings",()->{if(captured!=null){captured.mouseReleased(0,0,0);captured=null;}FocusedInput.commit();});var item=draft.items().get(category);if(item==null){return;}
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
    private void importCape(){FocusedInput.commit();long token=revision;try{manager.library().importCape(Path.of(importPath.getValue())).whenComplete((def,error)->Minecraft.getInstance().execute(()->{
        if(error!=null){notice.accept("Could not import cape: "+error.getCause().getMessage());return;}if(token==revision){select(def.id());}notice.accept("Cape imported");}));}catch(RuntimeException invalid){notice.accept("Enter a PNG cape path");}}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        previewTime+=Math.min(.05,Math.max(0,dt));double gap=4,header=48,footer=43;
        search.setBounds(x,y,width*.43-2,20);search.render(g,font,mx,my,dt,alpha);
        top.get(1).setBounds(x+width*.43,y,width*.28-2,20);top.get(2).setBounds(x+width*.71,y,width*.29,20);
        top.get(0).setBounds(x,y+24,width*.48-2,20);top.get(3).setBounds(x+width*.48,y+24,width*.52,20);
        for(var button:top){button.render(g,font,mx,my,dt,alpha);}
        double leftWidth=width*.49-2;listBounds=new Bounds(x,y+header,leftWidth,Math.max(20,height-header-footer));
        previewBounds=new Bounds(x+leftWidth+gap,y+header,width-leftWidth-gap,listBounds.height());
        RenderUtil.drawRoundedRect(g,previewBounds.x(),previewBounds.y(),previewBounds.width(),previewBounds.height(),4,Theme.opacity(Theme.SURFACE,alpha));
        rows.clear();
        preview(g,font,mx,my,alpha);
        RenderUtil.scissorPush(g,listBounds.x(),listBounds.y(),listBounds.width(),listBounds.height());
        try{
            if(editing){
                itemSection.setBounds(listBounds.x(),listBounds.y(),listBounds.width()-4,SettingsSection.HEADER);itemSection.render(g,font,mx,my,dt,alpha);
                double total=controls.stream().mapToDouble(c->(c instanceof ColorPicker picker?picker.getPreferredHeight():c instanceof Slider?26:23)+5).sum();
                double body=itemSection.bodyHeight(total);
                RenderUtil.scissorPush(g,listBounds.x(),listBounds.y()+SettingsSection.HEADER,listBounds.width(),Math.min(body,Math.max(0,listBounds.height()-SettingsSection.HEADER)));
                try{double cy=listBounds.y()+SettingsSection.HEADER-settingsScroll;for(var control:controls){double ch=control instanceof ColorPicker c?c.getPreferredHeight():control instanceof Slider?26:23;control.setBounds(listBounds.x(),cy,listBounds.width()-4,ch);if(cy+ch>listBounds.y()&&cy<listBounds.y()+listBounds.height()){control.render(g,font,mx,my>=listBounds.y()&&my<listBounds.y()+listBounds.height()?my:-1e6,dt,alpha);}cy+=ch+5;}maxSettings=Math.max(0,SettingsSection.HEADER+body-listBounds.height());settingsScroll=Math.clamp(settingsScroll,0,maxSettings);}finally{RenderUtil.scissorPop(g);}}
            else{
                var entries=manager.library().definitions().stream().filter(d->d.category()==category&&(catalog||manager.library().available(d))&&d.name().toLowerCase(Locale.ROOT).contains(search.getValue().toLowerCase(Locale.ROOT))).toList();
                maxList=Math.max(0,entries.size()*37-listBounds.height());listScroll=Math.clamp(listScroll,0,maxList);
                for(int i=0;i<entries.size();i++){var def=entries.get(i);double cy=listBounds.y()+i*37-listScroll;if(cy+33<=listBounds.y()||cy>=listBounds.y()+listBounds.height()){continue;}
                    var button=rowCache.computeIfAbsent(def.id(),key->new ActionButton(def.name(),()->select(key)));boolean thumbnail=!def.preview().isEmpty()&&CosmeticsEditor.class.getResource("/assets/riserecode/cosmetics/"+def.preview())!=null;
                    double iconSpace=thumbnail?22:0;button.setBounds(listBounds.x()+iconSpace,cy,listBounds.width()-3-iconSpace,23);
                    if(thumbnail){g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,CosmeticsManager.id(def.preview()),(int)listBounds.x(),(int)cy+1,0,0,20,20,128,128,128,128,Theme.opacity(-1,alpha));}button.render(g,font,mx,my>=listBounds.y()&&my<listBounds.y()+listBounds.height()?my:-1e6,dt,alpha);rows.add(button);
                    if(def.id().equals(selected)){RenderUtil.drawRoundedBorder(g,listBounds.x(),cy,listBounds.width()-3,23,3,Theme.opacity(Theme.accent(),alpha),1);}
                    String detail=def.creator()+" · "+(manager.library().available(def)?def.license():"Download");
                    if(button.contains(mx,my)&&my>=listBounds.y()&&my<listBounds.y()+listBounds.height()){
                        g.setTooltipForNextFrame(font,List.of(net.minecraft.network.chat.Component.literal(def.name()+" · "+def.creator()),net.minecraft.network.chat.Component.literal(def.license()),net.minecraft.network.chat.Component.literal(def.source())),Optional.empty(),(int)mx,(int)my);
                    }RenderUtil.text(g,font,RenderUtil.ellipsize(font,detail,(listBounds.width()-5)/.52),listBounds.x()+2,cy+26,.52,Theme.opacity(Theme.SECONDARY,alpha));
                }
            }
        }finally{RenderUtil.scissorPop(g);}
        double maximum=editing?maxSettings:maxList,scroll=editing?settingsScroll:listScroll;
        if(maximum>0){double thumb=Math.max(12,listBounds.height()*listBounds.height()/(listBounds.height()+maximum));RenderUtil.drawRoundedRect(g,listBounds.x()+listBounds.width()-2,listBounds.y()+scroll/maximum*(listBounds.height()-thumb),1.5,thumb,.75,Theme.opacity(Theme.accent(),alpha*.6));}
        double bottomY=y+height-20;for(int i=0;i<bottom.size();i++){var b=bottom.get(i);b.setBounds(x+i*(width/4),bottomY,width/4-3,20);b.render(g,font,mx,my,dt,alpha);}
        String status=manager.library().status();if(manager.failed(selected)){status="Asset unavailable · Refresh to retry";}
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,status,width/.58),x,y+height-34,.58,Theme.opacity(Theme.SECONDARY,alpha));
    }
    private void preview(GuiGraphicsExtractor g,Font font,double mx,double my,double alpha){
        var mc=Minecraft.getInstance();var state=new AvatarRenderState();state.entityType=EntityTypes.PLAYER;state.scale=state.ageScale=1;state.boundingBoxWidth=.6F;state.boundingBoxHeight=pose==3?.6F:pose==2?1.5F:1.8F;state.eyeHeight=1.62F;
        state.skin=mc.player==null?DefaultPlayerSkin.getDefaultSkin():mc.player.getSkin();state.showCape=state.showHat=state.showJacket=state.showLeftSleeve=state.showRightSleeve=state.showLeftPants=state.showRightPants=true;
        state.lightCoords=15728880;state.bodyRot=180;state.pose=pose==2?Pose.CROUCHING:pose==3?Pose.FALL_FLYING:Pose.STANDING;state.isCrouching=pose==2;state.isFallFlying=pose==3;state.fallFlyingTimeInTicks=pose==3?100:0;
        state.ageInTicks=(float)previewTime*20;state.walkAnimationPos=(float)previewTime*4;state.walkAnimationSpeed=pose==1?.7F:0;state.capeLean=pose==1?35:0;state.capeFlap=pose==2?25:0;
        var bridge=(CosmeticRenderState)state;bridge.risecode$outfit(draft);bridge.risecode$preview(true);manager.patchCape(state,draft);
        var capePhysics=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.CapePhysicsModule.class).orElseThrow();
        if(capePhysics.isEnabled()){((dev.riserecode.render.LegacyCapeState)state).risecode$legacyCape(new dev.riserecode.render.LegacyCapeMotion.Angles(state.capeFlap,state.capeLean,0));}
        double top=previewBounds.y()+22,bottom=previewBounds.y()+previewBounds.height()-43;
        var rotation=new Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.toRadians(yaw));
        float size=(float)Math.min(previewBounds.width()/3,(bottom-top)/2.5)* (float)zoom;
        g.entity(state,size,new Vector3f(0,state.boundingBoxHeight/2,0),rotation,new Quaternionf(),(int)previewBounds.x(),(int)top,(int)(previewBounds.x()+previewBounds.width()),(int)bottom);

        poseButton.setBounds(previewBounds.x()+3,previewBounds.y()+2,previewBounds.width()-6,18);poseButton.render(g,font,mx,my,0.016,alpha);rows.add(poseButton);
        var selectedDef=manager.library().find(selected);
        if(selectedDef!=null&&!manager.library().available(selectedDef)){
            downloadButton.setBounds(previewBounds.x()+3,bottom+2,previewBounds.width()-6,20);downloadButton.render(g,font,mx,my,.016,alpha);rows.add(downloadButton);
        }else if(category==CosmeticCategory.CAPE){importPath.setBounds(previewBounds.x()+3,bottom+2,previewBounds.width()-6,18);importPath.render(g,font,mx,my,.016,alpha);
            var button=importButton;button.setBounds(previewBounds.x()+3,bottom+23,previewBounds.width()-6,18);button.render(g,font,mx,my,.016,alpha);rows.add(button);
        }else{RenderUtil.text(g,font,"Drag to rotate",previewBounds.x()+5,bottom+5,.6,Theme.opacity(Theme.SECONDARY,alpha));RenderUtil.text(g,font,"Scroll to zoom",previewBounds.x()+5,bottom+17,.6,Theme.opacity(Theme.SECONDARY,alpha));}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(!contains(mx,my)){return false;}FocusedInput.pointer(mx,my);
        for(var b:top){if(b.mouseClicked(mx,my,button)){return true;}}for(var b:bottom){if(b.mouseClicked(mx,my,button)){return true;}}
        for(var b:List.copyOf(rows)){if(b.contains(mx,my)&&(inside(listBounds,mx,my)||inside(previewBounds,mx,my))&&b.mouseClicked(mx,my,button)){return true;}}
        if(search.mouseClicked(mx,my,button)){return true;}if(category==CosmeticCategory.CAPE&&importPath.mouseClicked(mx,my,button)){return true;}
        if(editing&&inside(listBounds,mx,my)){
            if(itemSection.mouseClicked(mx,my,button)){return true;}if(!itemSection.isExpanded()||my<listBounds.y()+SettingsSection.HEADER){return true;}
            for(var control:controls){if(control.getBounds().y()+control.getBounds().height()<=listBounds.y()+listBounds.height()&&control.getBounds().y()>=listBounds.y()+SettingsSection.HEADER&&control.mouseClicked(mx,my,button)){captured=control;return true;}}}
        if(inside(previewBounds,mx,my)&&button==0){dragging=true;previousX=mx;return true;}return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button){if(dragging){yaw+=(mx-previousX)*2;previousX=mx;return true;}return captured!=null&&captured.mouseDragged(mx,my,button);}
    @Override public boolean mouseReleased(double mx,double my,int button){dragging=false;if(captured!=null){captured.mouseReleased(mx,my,button);captured=null;}return true;}
    @Override public boolean mouseScrolled(double mx,double my,double amount){if(!contains(mx,my)){return false;}if(inside(previewBounds,mx,my)){zoom=Math.clamp(zoom+amount*.1,.5,2);}else if(editing){settingsScroll=Math.clamp(settingsScroll-amount*25,0,maxSettings);}else{listScroll=Math.clamp(listScroll-amount*25,0,maxList);}return true;}
    private static boolean inside(Bounds b,double x,double y){return b!=null&&x>=b.x()&&x<b.x()+b.width()&&y>=b.y()&&y<b.y()+b.height();}
    public boolean keyPressed(KeyEvent event){if(event.hasControlDown()&&event.key()==70){search.setFocused(true);search.selectAll();return true;}return false;}
}
