package dev.riserecode.module.modules;
import dev.riserecode.Client;
import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.*;
import dev.riserecode.render.WorldLabels;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import java.util.*;
public final class NametagsModule extends Module {
    public final BooleanSetting names=register(new BooleanSetting("Names","Show player names",true));
    public final BooleanSetting health=register(new BooleanSetting("Health","Show health and absorption",true));
    public final BooleanSetting distance=register(new BooleanSetting("Distance","Show distance",true));
    public final BooleanSetting ping=register(new BooleanSetting("Ping","Show latency",false));
    public final BooleanSetting equipment=register(new BooleanSetting("Equipment","Show armor and held items",true));
    public final BooleanSetting through=register(new BooleanSetting("Through Terrain","Show labels behind terrain",false));
    public final DoubleSetting range=register(DoubleSetting.extended("Range","Maximum distance",128,8,256));
    public final DoubleSetting scale=register(DoubleSetting.extended("Scale","Label scale",1,0.5,3));
    public final ColorSetting color=register(new ColorSetting("Color","Name color",0xFFFFFFFF));
    public final ColorSetting friends=register(new ColorSetting("Friend Color","Friend name color",0xFF67E6B1));
    private final Minecraft mc;
    private static final class Visibility {final long entered;long seen;Visibility(long entered,long seen){this.entered=entered;this.seen=seen;}}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.MAINHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND};
    private record Label(Component text,List<ItemStackRenderState> items,boolean friend,boolean lineOfSight) { }
    private final Map<UUID,Label> labels=new HashMap<>();
    private final Map<UUID,Visibility> visibility=new HashMap<>();
    private net.minecraft.client.multiplayer.ClientLevel visibleLevel;
    private long visibilityFrame,equipmentBuilds,resourceGeneration;
    private static final class Equipment {net.minecraft.world.item.ItemStack[] stacks;List<ItemStackRenderState> states;long appearance,resources,module,renderTick;boolean dynamic,using;long seen;}
    private final Map<UUID,Equipment> equipmentCache=new HashMap<>();
    private long refreshFrame;
    public long equipmentBuilds(){return equipmentBuilds;}
    private List<ItemStackRenderState> items(Player p){
        var value=equipmentCache.computeIfAbsent(p.getUUID(),ignored->new Equipment());value.seen=refreshFrame;
        long appearance=dev.riserecode.ui.theme.Theme.manager()==null?0:dev.riserecode.ui.theme.Theme.manager().getRevision();
        long renderTick=mc.level.getGameTime();boolean using=p.isUsingItem();
        boolean dirty=value.stacks==null||value.appearance!=appearance||value.resources!=resourceGeneration||value.module!=stateRevision()||((value.dynamic||using||value.using)&&value.renderTick!=renderTick);
        if(!dirty){for(int i=0;i<SLOTS.length;i++){if(!net.minecraft.world.item.ItemStack.matches(value.stacks[i],p.getItemBySlot(SLOTS[i]))){dirty=true;break;}}}
        if(dirty){
            var stacks=new net.minecraft.world.item.ItemStack[SLOTS.length];var states=new ArrayList<ItemStackRenderState>();value.dynamic=false;
            for(int i=0;i<SLOTS.length;i++){stacks[i]=p.getItemBySlot(SLOTS[i]).copy();value.dynamic|=stacks[i].is(net.minecraft.world.item.Items.CLOCK)||stacks[i].is(net.minecraft.world.item.Items.COMPASS)||stacks[i].is(net.minecraft.world.item.Items.RECOVERY_COMPASS)||stacks[i].is(net.minecraft.world.item.Items.PLAYER_HEAD);if(equipment.getValue()&&!stacks[i].isEmpty()){var state=new ItemStackRenderState();mc.getItemModelResolver().updateForLiving(state,stacks[i],ItemDisplayContext.GUI,p);states.add(state);}}
            value.stacks=stacks;value.states=List.copyOf(states);value.appearance=appearance;value.resources=resourceGeneration;value.module=stateRevision();value.using=using;value.renderTick=renderTick;equipmentBuilds++;
        }
        return value.states;
    }
    public NametagsModule(EventBus bus,Minecraft mc){super("Nametags","Player labels with health, equipment and friend colors.",Category.RENDER,bus);this.mc=mc;}
    private boolean eligible(Player p){return mc.player!=null&&p!=mc.player&&p.isAlive()&&!p.isSpectator()&&!p.isInvisible()&&mc.player.distanceToSqr(p)<=range.getValue()*range.getValue();}
    @Override protected void onEnable(){listen(LabelRenderEvent.class,e->{if(e.entity() instanceof Player p&&eligible(p)){e.cancel();}});listen(UpdateEvent.class,e->refresh());listen(RenderResourcesReloadEvent.class,e->{resourceGeneration++;});listen(Render3DEvent.class,this::render);}
    @Override protected void onDisable(){visibility.clear();labels.clear();equipmentCache.clear();visibleLevel=null;}
    private void refresh(){
        labels.clear();refreshFrame++;
        if(mc.level==null||mc.player==null){equipmentCache.clear();return;}
        for(var p:mc.level.players()){
            if(!eligible(p)){continue;}
            var label=Component.empty();if(names.getValue()){label.append(p.getDisplayName().copy());}if(health.getValue()){label.append(String.format(Locale.ROOT," %.0f♥",p.getHealth()+p.getAbsorptionAmount()));}if(distance.getValue()){label.append(" "+Math.round(mc.player.distanceTo(p))+"m");}if(ping.getValue()&&mc.getConnection()!=null){var info=mc.getConnection().getPlayerInfo(p.getUUID());if(info!=null){label.append(" "+info.getLatency()+"ms");}}
            boolean friend=Client.INSTANCE.getFriendManager().isFriend(p.getUUID(),p.getGameProfile().name());
            List<ItemStackRenderState> items=items(p);
            labels.put(p.getUUID(),new Label(label,items,friend,through.getValue()||mc.player.hasLineOfSight(p)));
        }
        equipmentCache.values().removeIf(value->value.seen!=refreshFrame);
    }
    private void render(Render3DEvent e){if(mc.level==null||mc.player==null){labels.clear();visibility.clear();equipmentCache.clear();visibleLevel=null;return;}float partial=e.getContext().deltaTracker().getGameTimeDeltaPartialTick(false);
        if(visibleLevel!=mc.level){visibility.clear();equipmentCache.clear();visibleLevel=mc.level;refresh();}
        long now=System.nanoTime();visibilityFrame++;
        for(var p:mc.level.players()){if(!eligible(p)){continue;}
            var cached=labels.get(p.getUUID());if(cached==null){continue;}
            var camera=e.getContext().camera();
            if(!camera.getCullFrustum().isVisible(p.getBoundingBox().inflate(.5,1,.5))||!through.getValue()&&!cached.lineOfSight()){continue;}
            Visibility previous=visibility.computeIfAbsent(p.getUUID(),ignored->new Visibility(now,visibilityFrame));long entered=previous.entered;previous.seen=visibilityFrame;
            double progress=dev.riserecode.ui.animation.PopIn.moduleProgress(this,(now-entered)*1e-9);
            WorldLabels.label(e,p.getPosition(partial).add(0,p.getBbHeight()+0.4,0),cached.text(),dev.riserecode.ui.theme.Theme.opacity(cached.friend()?friends.getValue():color.getValue(),progress),scale.getValue()*popScale(progress)*dev.riserecode.ui.theme.Theme.number("Font Scale",1),through.getValue(),cached.items());
        }
        visibility.values().removeIf(value->value.seen!=visibilityFrame);
    }
}
