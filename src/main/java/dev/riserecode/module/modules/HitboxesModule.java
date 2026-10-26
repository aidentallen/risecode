package dev.riserecode.module.modules;

import dev.riserecode.Client;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.BlockHighlightGeometry;
import dev.riserecode.render.TargetHighlight;
import dev.riserecode.setting.*;
import dev.riserecode.ui.theme.ModuleAppearance;
import dev.riserecode.ui.theme.Theme;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Visual entity bounds; normal Minecraft targeting determines the aimed highlight. */
public final class HitboxesModule extends Module {
    public enum Mode { ONLY_WHEN_LOOKING, ALWAYS }
    private final Minecraft mc;
    private final BlockOverlayModule blocks;
    public final EnumSetting<Mode> mode=register(new EnumSetting<>("Mode","Show the aimed entity or all nearby entity boxes",Mode.ONLY_WHEN_LOOKING));
    public final BooleanSetting players=register(new BooleanSetting("Players","Include players",true));
    public final BooleanSetting mobs=register(new BooleanSetting("Mobs","Include other living entities",true));
    public final BooleanSetting others=register(new BooleanSetting("Other Entities","Include targetable objects such as boats and armor stands",true));
    public final BooleanSetting invisible=register(new BooleanSetting("Include Invisible","Include invisible entities",false));
    public final DoubleSetting range=register(DoubleSetting.extended("Range","Always-mode distance in blocks",32,1,256));
    // Load initialization flags before inheritance switches so profile restoration cannot reseed overrides.
    private final BooleanSetting styleInitialized=register(new BooleanSetting("Style Overrides Initialized","Stored override initialization",false));
    private final BooleanSetting animationInitialized=register(new BooleanSetting("Animation Overrides Initialized","Stored override initialization",false));
    public final BooleanSetting inheritStyle=register(new BooleanSetting("Use BlockOverlay Style","Share outline styling, or reveal individual controls",true));
    public final BooleanSetting inheritAnimation=register(new BooleanSetting("Use BlockOverlay Animation","Share switching and pop settings, or reveal individual controls",true));
    public final ColorSetting color=register(new ColorSetting("OutlineColor","Entity outline color",0xFF59D5CB));
    public final DoubleSetting opacity=register(DoubleSetting.extended("OutlineOpacity","Entity outline opacity",1,0,1));
    public final DoubleSetting width=register(DoubleSetting.extended("OutlineWidth","Entity outline pixels",2,.5,5));
    public final EnumSetting<BlockOverlayModule.OutlineStyle> style=register(new EnumSetting<>("Outline Style","Full edges or corners",BlockOverlayModule.OutlineStyle.FULL));
    public final DoubleSetting gap=register(DoubleSetting.extended("Edge Gap","Percentage missing from each edge",60,0,100));
    public final BooleanSetting through=register(new BooleanSetting("See-through Outline","Show entity edges through geometry",false));
    public final BooleanSetting smooth=register(new BooleanSetting("Smooth Switching","Animate direct target switches",true));
    public final DoubleSetting duration=register(DoubleSetting.extended("Transition Duration","Seconds to reach a new target",.15,0,5));
    public final BooleanSetting pop=register(new BooleanSetting("Pop Animation","Grow a newly acquired entity box from its center",false));
    public final BooleanSetting popOnSwitch=register(new BooleanSetting("Pop On Switch","Also pop on direct target changes",false));
    public final DoubleSetting popDuration=register(DoubleSetting.extended("Hitbox Pop Duration","Seconds to grow to the full hitbox",.18,0,5));
    private final Map<UUID,Long> entrances=new HashMap<>();
    private Object world;
    public HitboxesModule(EventBus bus,Minecraft mc,BlockOverlayModule blocks) {
        super("Hitboxes","Entity hitboxes with shared block-to-entity highlight motion",Category.RENDER,bus);
        this.mc=mc;this.blocks=blocks;
        styleInitialized.setVisibleWhen(()->false);animationInitialized.setVisibleWhen(()->false);
        range.setVisibleWhen(()->mode.getValue()==Mode.ALWAYS);
        for(var setting:List.of(color,opacity,width,style,through))setting.setVisibleWhen(()->!inheritStyle.getValue());
        gap.setVisibleWhen(()->!inheritStyle.getValue()&&style.getValue()==BlockOverlayModule.OutlineStyle.CORNERS);
        smooth.setVisibleWhen(()->!inheritAnimation.getValue()&&mode.getValue()==Mode.ONLY_WHEN_LOOKING);
        duration.setVisibleWhen(()->smooth.isVisible()&&smooth.getValue());
        pop.setVisibleWhen(()->!inheritAnimation.getValue());popDuration.setVisibleWhen(()->pop.isVisible()&&pop.getValue());
        popOnSwitch.setVisibleWhen(()->popDuration.isVisible()&&mode.getValue()==Mode.ONLY_WHEN_LOOKING);
        inheritStyle.onChange(()->{if(!inheritStyle.getValue()&&!styleInitialized.getValue()){
            var inherited=blocks.entityStyle();var layer=inherited.edges().getFirst();color.apply(layer.paint());opacity.setValue(layer.weight());
            width.setValue(inherited.width());through.setValue(inherited.through());
            style.setValue(inherited.gap()>0?BlockOverlayModule.OutlineStyle.CORNERS:BlockOverlayModule.OutlineStyle.FULL);
            gap.setValue(inherited.gap());styleInitialized.setValue(true);
        }});
        inheritAnimation.onChange(()->{if(!inheritAnimation.getValue()&&!animationInitialized.getValue()){
            var inherited=blocks.highlightMotion();smooth.setValue(inherited.smooth());duration.setValue(inherited.duration());
            pop.setValue(inherited.pop());popOnSwitch.setValue(inherited.popOnSwitch());popDuration.setValue(inherited.popDuration());animationInitialized.setValue(true);
        }});
        mode.onChange(this::resetVisuals);
    }
    public TargetHighlight.Style highlightStyle() {
        if(inheritStyle.getValue())return blocks.entityStyle();
        return ModuleAppearance.call(capabilityId(),()->TargetHighlight.Style.of(color,opacity.getValue(),width.getValue(),
                style.getValue()==BlockOverlayModule.OutlineStyle.CORNERS?gap.getValue():0,through.getValue(),null,0));
    }
    public TargetHighlight.Motion highlightMotion() {
        boolean reduced=ModuleAppearance.call(capabilityId(),()->Theme.bool("Reduced Motion",false));
        var inherited=blocks.highlightMotion();
        return inheritAnimation.getValue()?new TargetHighlight.Motion(inherited.smooth(),inherited.duration(),inherited.pop(),inherited.popOnSwitch(),inherited.popDuration(),reduced||inherited.reduced())
                :new TargetHighlight.Motion(smooth.getValue(),duration.getValue(),pop.getValue(),popOnSwitch.getValue(),popDuration.getValue(),reduced);
    }
    public boolean eligible(Entity entity) {
        if(entity==mc.player||entity==mc.getCameraEntity()||entity.isRemoved()||!entity.isAlive()||entity.isSpectator()||!entity.isPickable()||!invisible.getValue()&&entity.isInvisible())return false;
        if(entity instanceof Player)return players.getValue();
        if(entity instanceof net.minecraft.world.entity.decoration.ArmorStand)return others.getValue();
        return entity instanceof LivingEntity?mobs.getValue():others.getValue();
    }
    private void resetVisuals() { entrances.clear();var coordinator=Client.INSTANCE.getTargetHighlights();if(coordinator!=null)coordinator.release(capabilityId()); }
    public int trackedEntrances() { return entrances.size(); }
    @Override protected void onEnable() {
        resetVisuals();world=mc.level;
        listen(Render3DEvent.class,this::extract);
        listen(dev.riserecode.event.RenderResourcesReloadEvent.class,event->resetVisuals());
        listen(dev.riserecode.event.UpdateEvent.class,event->{if(world!=mc.level){resetVisuals();world=mc.level;}});
    }
    @Override protected void onDisable() {resetVisuals();world=null;}
    private void extract(Render3DEvent event) {
        if(world!=mc.level){resetVisuals();world=mc.level;}
        if(mc.level==null||mc.player==null)return;
        float partial=event.getContext().deltaTracker().getGameTimeDeltaPartialTick(false);
        var motion=highlightMotion();var visual=highlightStyle();
        if(mode.getValue()==Mode.ONLY_WHEN_LOOKING){
            if(mc.hitResult instanceof EntityHitResult hit&&eligible(hit.getEntity())){
                var entity=hit.getEntity();
                event.targetHighlight(new TargetHighlight.Request(capabilityId(),new TargetHighlight.Key(TargetHighlight.Kind.ENTITY,entity.getUUID()),
                        TargetHighlight.Kind.ENTITY,List.of(corners(entity,partial)),motion,visual,false));
            }
            return;
        }
        var camera=mc.getCameraEntity()==null?mc.player:mc.getCameraEntity();var seen=new HashSet<UUID>();
        long now=Client.INSTANCE.getTargetHighlights().time();double distance=range.getValue();
        for(var entity:mc.level.getEntities(camera,camera.getBoundingBox().inflate(distance),this::eligible)){
            if(camera.distanceToSqr(entity)>distance*distance)continue;
            var id=entity.getUUID();seen.add(id);long entered=entrances.computeIfAbsent(id,ignored->now);
            double progress=!motion.pop()||motion.reduced()||motion.popDuration()<=0?1:Math.clamp((now-entered)*1e-9/motion.popDuration(),0,1);
            double scale=1-Math.pow(1-progress,3);var vertices=corners(entity,partial);
            if(scale<1){var bounds=entity.getBoundingBox().move(entity.getPosition(partial).subtract(entity.position()));var center=bounds.getCenter();vertices=vertices.stream().map(p->center.add(p.subtract(center).scale(scale))).toList();}
            if(scale>.0001)TargetHighlight.draw(event,vertices,visual,1,true);
        }
        entrances.keySet().retainAll(seen);
    }
    private static List<Vec3> corners(Entity entity,float partial) {
        return BlockHighlightGeometry.corners(entity.getBoundingBox().move(entity.getPosition(partial).subtract(entity.position())).inflate(.002));
    }
}
