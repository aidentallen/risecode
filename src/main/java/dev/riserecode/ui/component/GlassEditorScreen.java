package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.module.modules.GlassModule;
import dev.riserecode.render.glass.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.component.setting.*;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Themed, live material editor using normal RiseCode color pickers and numeric inputs. */
public final class GlassEditorScreen extends Screen implements KeyboardNavigation.PageScrollable {
    private record Row(Setting<?> setting,UIComponent control) { }
    private record Section(SettingsSection header,List<Row> rows) { }
    private final GlassModule module;
    private final Screen parent;
    private final java.util.function.Supplier<net.minecraft.world.entity.player.PlayerSkin> previewSkin;
    private final List<Section> sections=new ArrayList<>();
    private final ActionButton done,reset;
    private final List<UIComponent> hitControls=new ArrayList<>();
    private UIComponent captured;
    private final Object[] previewFocus={new Object(),new Object(),new Object()};
    private final UIComponent.Bounds[] previewControls=new UIComponent.Bounds[3];
    private boolean fullPreview;
    private final ActionButton viewToggle=new ActionButton(()->fullPreview?"Settings":"Preview",()->{FocusedInput.commit();release();fullPreview=!fullPreview;});
    public void showPreview(boolean value){fullPreview=value;}
    private boolean rotating;
    private double rotation=155,tilt=0,zoom=1,scroll,maxScroll;
    private double panelX,panelY,panelWidth,panelHeight,previewX,previewY,previewW,previewH;
    private long lastTime;
    public GlassEditorScreen(GlassModule module,Screen parent){
        super(Component.literal("Glass · Customize / Preview"));this.module=module;this.parent=parent;
        var mc=net.minecraft.client.Minecraft.getInstance();previewSkin=mc.getSkinManager().createLookup(mc.getGameProfile(),false);
        done=new ActionButton("Done",this::onClose);
        reset=new ActionButton("Reset Glass",()->{FocusedInput.commit();for(var s:module.getSettings())s.reset();changed();});
        section("Targets",module.targets(),null);
        for(var material:List.of(module.shared,module.body,module.armor,module.items)){
            var list=new ArrayList<Setting<?>>();if(material.override!=null)list.add(material.override);list.add(material.preset);
            for(var s:material.settings())if(s!=material.override&&s!=material.preset)list.add(s);
            section(material.label+" material",list,material);
        }
        sections.getFirst().header().setExpanded(true);sections.get(1).header().setExpanded(true);
    }
    private void changed(){Client.INSTANCE.getConfigManager().requestSave();}
    private void section(String name,List<Setting<?>> settings,GlassModule.MaterialSettings material){
        var rows=new ArrayList<Row>();
        for(var s:settings){
            Runnable change=s==(material==null?null:material.preset)?()->{material.applyPreset(material.preset.getValue());changed();}:this::changed;
            UIComponent control;
            if(s instanceof BooleanSetting b)control=new ToggleSwitch(b,change);
            else if(s instanceof DoubleSetting d)control=new Slider(d,change);
            else if(s instanceof ColorSetting c)control=new ColorPicker(c,change,true);
            else if(s instanceof EnumSetting<?> e)control=enumControl(e,change);
            else throw new IllegalArgumentException("Unsupported Glass control");
            rows.add(new Row(s,control));
        }
        sections.add(new Section(new SettingsSection("glass:"+name,name,()->{release();FocusedInput.commit();}),List.copyOf(rows)));
    }
    private static <E extends Enum<E>> UIComponent enumControl(EnumSetting<E> setting,Runnable change){return new EnumSelector<>(setting,change);}
    private static double rowHeight(Row row){return SettingMetrics.height(row.control(),23)+5;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial) { }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        long now=System.nanoTime();double dt=lastTime==0?0:Math.min(.1,(now-lastTime)*1e-9);lastTime=now;
        RenderUtil.drawRoundedRect(g,0,0,width,height,0,0xF5111722);
        double fontScale=Theme.number("Font Scale",1),titleHeight=9*fontScale*1.25+7,lineHeight=9*fontScale*.75+4;
        RenderUtil.accentText(g,font,"Glass",16,10,1.25,1);
        RenderUtil.text(g,font,"Customize / Preview",16,10+titleHeight,.75,Theme.SECONDARY);
        RenderUtil.text(g,font,module.isEnabled()?"Gameplay enabled":"Preview · gameplay disabled",16,10+titleHeight+lineHeight,.7,Theme.SECONDARY);
        boolean compact=width<500;panelWidth=compact?width-24:Math.min(340+60*Math.max(0,fontScale-1),width*.52);panelX=12;panelY=14+titleHeight+lineHeight*2;panelHeight=Math.max(24,height-panelY-46);
        previewX=compact?12:panelX+panelWidth+12;previewY=panelY;previewW=Math.max(24,width-previewX-12);previewH=panelHeight;
        hitControls.clear();
        if(!compact||!fullPreview){
        RenderUtil.drawRoundedRect(g,panelX,panelY,panelWidth,panelHeight,8,Theme.SURFACE);
        KeyboardNavigation.pushScrollRegion(bounds->{double shift=bounds.y()<panelY?bounds.y()-panelY:Math.max(0,bounds.y()+bounds.height()-panelY-panelHeight);if(shift!=0){scroll=Math.clamp(scroll+shift,0,maxScroll);}});
        RenderUtil.scissorPush(g,panelX,panelY,panelWidth,panelHeight);
        double y=panelY+6-scroll;
        try{
            for(var section:sections){
                var header=section.header();header.setBounds(panelX+6,y,panelWidth-12,24);header.render(g,font,mx,my,dt,1);hitControls.add(header);y+=24;
                double content=section.rows().stream().filter(r->r.setting().isVisible()).mapToDouble(GlassEditorScreen::rowHeight).sum();double body=header.bodyHeight(content);
                if(body>0){
                    RenderUtil.scissorPush(g,panelX+6,y,panelWidth-12,body);
                    double ry=y;
                    try{
                        for(var row:section.rows()){
                            if(!row.setting().isVisible())continue;
                            double h=rowHeight(row);row.control().setBounds(panelX+13,ry,panelWidth-26,h-5);
                            row.control().render(g,font,header.isExpanded()?mx:-10000,header.isExpanded()?my:-10000,dt,header.amount(content));
                            if(header.isExpanded()&&ry+h<=y+body+.01)hitControls.add(row.control());ry+=h;
                        }
                    }finally{RenderUtil.scissorPop(g);}
                }
                y+=body+4;
            }
        }finally{RenderUtil.scissorPop(g);KeyboardNavigation.popScrollRegion();}
        maxScroll=Math.max(0,y+scroll-panelY-panelHeight+6);scroll=Math.clamp(scroll,0,maxScroll);
        }
        if(!compact||fullPreview){
        boolean stacked=previewW/3-12<RenderUtil.textWidth(font,"Reset Rotation",false)*.65;
        double hintLine=9*fontScale*.65+4,hintHeight=2*hintLine+4,controlHeight=Math.max(19,9*fontScale*.65+8),controlTop=previewY+previewH-controlHeight*(stacked?3:1);
        double modelTop=previewY+hintHeight,modelBottom=Math.max(modelTop+1,controlTop-4);
        var state=previewState(partial);
        g.entity(state,(float)(Math.min(previewW*.34,(modelBottom-modelTop)*.40)*zoom),new Vector3f(),new Quaternionf().rotateZ((float)Math.PI).rotateX((float)Math.toRadians(tilt)).rotateY((float)Math.toRadians(rotation)),null,(int)previewX,(int)modelTop,(int)(previewX+previewW),(int)modelBottom);
        String[] previewLabels={"Rotation","Tilt","Zoom"};
        for(int i=0;i<3;i++) {
            final int axis=i;double span=(previewW-8)/3;
            var bounds=new UIComponent.Bounds(stacked?previewX+2:previewX+2+i*span,controlTop+(stacked?i*controlHeight:0),stacked?previewW-4:span-3,controlHeight);previewControls[i]=bounds;
            KeyboardNavigation.control(g,previewFocus[i],bounds,1,
                    () -> "Glass preview, "+previewLabels[axis]+", "+(axis==0?rotation:axis==1?tilt:zoom)+", arrows adjust, activate resets",
                    () -> resetPreviewAxis(axis),
                    direction -> {if(axis==0){rotation+=direction*15;}else if(axis==1){tilt=Math.clamp(tilt+direction*5,-35,35);}else{zoom=Math.clamp(zoom+direction*.08,.5,1.6);}});
            boolean hot=bounds.x()<=mx&&mx<bounds.x()+bounds.width()&&bounds.y()<=my&&my<bounds.y()+bounds.height();
            RenderUtil.drawButton(g,bounds.x(),bounds.y(),bounds.width(),bounds.height(),hot?1:0,1);
            String label=RenderUtil.ellipsize(font,"Reset "+previewLabels[i],Math.max(1,bounds.width()-8)/.65);
            RenderUtil.text(g,font,label,bounds.centerX()-RenderUtil.textWidth(font,label,false)*.65/2,bounds.y()+(controlHeight-9*fontScale*.65)/2,.65,Theme.TEXT);
            if(hot){g.setTooltipForNextFrame(font,java.util.List.of(Component.literal(previewLabels[i]+" · arrows adjust · click resets")),java.util.Optional.empty(),mx,my);}
        }
        RenderUtil.text(g,font,"Drag to rotate",previewX+6,previewY+3,.65,Theme.SECONDARY);
        RenderUtil.text(g,font,"Wheel to zoom",previewX+6,previewY+3+hintLine,.65,Theme.SECONDARY);
        }
        if(compact){viewToggle.setBounds(width-150,8,100,24);viewToggle.render(g,font,mx,my,dt,1);}
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,module.status(),(width-190)/.65),16,height-23,.65,Theme.SECONDARY);
        reset.setBounds(width-174,height-35,100,28);done.setBounds(width-70,height-35,60,28);reset.render(g,font,mx,my,dt,1);done.render(g,font,mx,my,dt,1);
    }
    private AvatarRenderState previewState(float partial){
        AvatarRenderState state;
        if(minecraft.player!=null){
            state=new AvatarRenderState();minecraft.getEntityRenderDispatcher().getPlayerRenderer(minecraft.player).extractRenderState(minecraft.player,state,partial);
        }else{
            state=new AvatarRenderState();state.skin=previewSkin.get();state.entityType=net.minecraft.world.entity.EntityTypes.PLAYER;
            state.chestEquipment=sample(Items.DIAMOND_CHESTPLATE,EquipmentSlot.CHEST);state.headEquipment=sample(Items.DIAMOND_HELMET,EquipmentSlot.HEAD);state.legsEquipment=sample(Items.DIAMOND_LEGGINGS,EquipmentSlot.LEGS);state.feetEquipment=sample(Items.DIAMOND_BOOTS,EquipmentSlot.FEET);
            state.mainArm=net.minecraft.world.entity.HumanoidArm.RIGHT;
            // Non-living resolution is safe outside a world, and never changes a live inventory.
            minecraft.getItemModelResolver().updateForTopItem(state.rightHandItemState,sample(Items.DIAMOND_SWORD,null),ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,null,null,0);
            minecraft.getItemModelResolver().updateForTopItem(state.leftHandItemState,sample(Items.SHIELD,null),ItemDisplayContext.THIRD_PERSON_LEFT_HAND,null,null,0);
        }
        state.lightCoords=15728880;state.bodyRot=0;state.yRot=0;state.xRot=0;state.nameTag=null;state.outlineColor=0;state.shadowPieces.clear();
        ((GlassCapture.State)state).risecode$glassOwner(GlassCapture.owner(minecraft.player==null?new UUID(0,27):minecraft.player.getUUID(),true,true));
        return state;
    }
    private static ItemStack sample(Item item,EquipmentSlot slot){
        var components=net.minecraft.core.component.DataComponentMap.builder().set(net.minecraft.core.component.DataComponents.ITEM_MODEL,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
        if(slot!=null)components.set(net.minecraft.core.component.DataComponents.EQUIPPABLE,net.minecraft.world.item.equipment.Equippable.builder(slot).setAsset(net.minecraft.world.item.equipment.EquipmentAssets.DIAMOND).build());
        return new ItemStack(net.minecraft.core.Holder.direct(item,components.build()),1);
    }
    public List<UIComponent> getControls(){return sections.stream().flatMap(section->section.rows().stream()).map(Row::control).toList();}
    private void resetPreviewAxis(int axis){if(axis==0){rotation=155;}else if(axis==1){tilt=0;}else{zoom=1;}}
    public double rotation(){return rotation;}
    public double zoom(){return zoom;}
    private boolean previewContains(double x,double y){return (width>=500||fullPreview)&&x>=previewX&&x<previewX+previewW&&y>=previewY&&y<previewY+previewH;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        FocusedInput.pointer(e.x(),e.y());for(var section:sections)for(var row:section.rows())if(row.control() instanceof ColorPicker picker)picker.pointerFocus(e.x(),e.y());
        if(done.mouseClicked(e.x(),e.y(),e.button())||reset.mouseClicked(e.x(),e.y(),e.button()))return true;
        if(width<500&&viewToggle.mouseClicked(e.x(),e.y(),e.button())){return true;}
        if((width>=500||fullPreview)&&e.button()==0){for(int i=0;i<previewControls.length;i++){var b=previewControls[i];if(b!=null&&e.x()>=b.x()&&e.x()<b.x()+b.width()&&e.y()>=b.y()&&e.y()<b.y()+b.height()){resetPreviewAxis(i);return true;}}}
        if((width>=500||fullPreview)&&previewContains(e.x(),e.y())&&e.button()==0){rotating=true;return true;}
        if(e.x()<panelX||e.x()>panelX+panelWidth||e.y()<panelY||e.y()>panelY+panelHeight)return false;
        if(width<500&&fullPreview){return true;}
        for(var control:hitControls)if(control.mouseClicked(e.x(),e.y(),e.button())){captured=control;return true;}
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(rotating){rotation+=dx*.8;tilt=Math.clamp(tilt+dy*.4,-35,35);return true;}return captured!=null&&captured.mouseDragged(e.x(),e.y(),e.button());}
    @Override public boolean mouseReleased(MouseButtonEvent e){boolean result=rotating||captured!=null;release();done.mouseReleased(e.x(),e.y(),e.button());reset.mouseReleased(e.x(),e.y(),e.button());return result;}
    private void release(){rotating=false;if(captured!=null){captured.mouseReleased(0,0,0);captured=null;}}
    @Override public void scrollPage(int direction){FocusedInput.commit();scroll=Math.clamp(scroll+direction*Math.max(40,panelHeight*.8),0,maxScroll);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(previewContains(x,y)){zoom=Math.clamp(zoom+vertical*.08,.5,1.6);return true;}if(x>=panelX&&x<panelX+panelWidth){scroll=Math.clamp(scroll-vertical*30,0,maxScroll);FocusedInput.commit();return true;}return false;}
    @Override public boolean charTyped(CharacterEvent e){return FocusedInput.character(e);}
    @Override public boolean keyPressed(KeyEvent e){if(FocusedInput.key(e,()->minecraft.keyboardHandler.getClipboard(),v->minecraft.keyboardHandler.setClipboard(v)))return true;return super.keyPressed(e);}
    @Override public void onClose(){FocusedInput.commit();changed();minecraft.gui.setScreen(parent);}
    @Override public void removed(){release();FocusedInput.commit();GlassPreview.close();}
}
