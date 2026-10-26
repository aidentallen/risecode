package dev.riserecode.module.modules;

import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.placement.PlacementAnimations;
import dev.riserecode.render.placement.PlacementMotion;
import dev.riserecode.setting.*;
import dev.riserecode.util.MathUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Placement observations belong to this module; terrain workers only read frozen snapshots. */
public final class BlockAnimationsModule extends Module {
    public enum Preset { SWING, POP, SLIDE, CUSTOM }
    public enum OutlineBehavior { FOLLOW_ANIMATION, HIDE_UNTIL_SETTLED, FIXED_POSITION }
    private final Minecraft mc;
    public final EnumSetting<Preset> preset=register(new EnumSetting<>("Preset","Placement animation style",Preset.SWING));
    public final BooleanSetting yourPlacements=register(new BooleanSetting("Your Placements","Animate blocks you place",true));
    public final BooleanSetting otherPlayers=register(new BooleanSetting("Other Players","Infer nearby player placements; uncertain changes stay static",false));
    public final EnumSetting<OutlineBehavior> outlineBehavior=register(new EnumSetting<>("Outline Behavior","How BlockOverlay follows placement motion",OutlineBehavior.FOLLOW_ANIMATION));
    public final DoubleSetting duration=register(DoubleSetting.extended("Duration","Placement duration in seconds",.2,0,5));
    public final EnumSetting<MathUtil.Easing> easing=register(new EnumSetting<>("Easing","Placement curve",MathUtil.Easing.OUT_QUART));
    public final DoubleSetting strength=register(DoubleSetting.extended("Strength","Overall motion strength",1,0,3));
    public final DoubleSetting range=register(DoubleSetting.extended("Range","Maximum placement distance",32,1,128));
    public final DoubleSetting limit=register(DoubleSetting.extended("Animation Limit","Maximum active blocks; excess blocks render normally",128,1,2048));
    public final DoubleSetting scale=number("Start Scale",.9,0,3),height=number("Height Scale",1,0,3);
    public final DoubleSetting x=number("Translation X",0,-4,4),y=number("Translation Y",.05,-4,4),z=number("Translation Z",.1,-4,4);
    public final DoubleSetting rx=number("Rotation X",20,-360,360),ry=number("Rotation Y",0,-360,360),rz=number("Rotation Z",0,-360,360);
    public final EnumSetting<PlacementMotion.Pivot> pivot=register(new EnumSetting<>("Pivot","Rotation and scaling pivot",PlacementMotion.Pivot.BOTTOM));
    private boolean applying;
    private long tick;
    private Object observedLevel;
    private record Action(int player,Block block,BlockPos hit,BlockPos adjacent,Vec3 eye,long tick) { }
    private record Change(BlockPos pos,BlockState state,long tick) { }
    private final ArrayList<Action> actions=new ArrayList<>();
    private final Map<Long,Change> changes=new HashMap<>();
    private DoubleSetting number(String name,double value,double min,double max){return register(DoubleSetting.extended(name,"Custom placement transform",value,min,max));}
    public BlockAnimationsModule(EventBus bus,Minecraft minecraft){
        super("BlockAnimations","Native placement models, lighting and synchronized outlines",Category.RENDER,bus);mc=minecraft;
        preset.onChange(()->applyPreset(preset.getValue()));
        for(var s:java.util.List.of(scale,height,x,y,z,rx,ry,rz)){s.onChange(()->{if(!applying)preset.setValue(Preset.CUSTOM);});}
        pivot.onChange(()->{if(!applying)preset.setValue(Preset.CUSTOM);});
    }
    private void applyPreset(Preset choice){if(choice==Preset.CUSTOM)return;applying=true;try{
        scale.setValue(choice==Preset.POP?.2:choice==Preset.SWING?.9:1);height.setValue(1.0);
        x.setValue(0.0);y.setValue(choice==Preset.POP?0:.05);z.setValue(choice==Preset.POP?0:choice==Preset.SLIDE?.4:.1);
        rx.setValue(choice==Preset.SWING?20.0:0.0);ry.setValue(0.0);rz.setValue(0.0);pivot.setValue(PlacementMotion.Pivot.BOTTOM);
    }finally{applying=false;}}
    public PlacementMotion.Settings motionSettings(){return new PlacementMotion.Settings(duration.getValue(),easing.getValue(),strength.getValue(),scale.getValue(),height.getValue(),x.getValue(),y.getValue(),z.getValue(),rx.getValue(),ry.getValue(),rz.getValue(),pivot.getValue());}
    public String status(){return PlacementAnimations.pending()+" active · "+(dev.riserecode.render.ShaderWorldOverlay.active()?"shader lighting and shadows · light sources stay static":"native block lighting");}
    @Override protected void onEnable(){
        PlacementAnimations.enable(()->dev.riserecode.ui.theme.ModuleAppearance.call(capabilityId(),()->dev.riserecode.ui.theme.Theme.bool("Reduced Motion",false)));observedLevel=mc.level;
        listen(BlockPlacementEvent.class,event->{if(yourPlacements.getValue()&&event.level==mc.level&&event.player==mc.player)start(event.pos,event.state,event.player.getEyePosition());});
        listen(TerrainChangedEvent.class,event->{if(!otherPlayers.getValue()||event.level()!=mc.level||!event.previous().canBeReplaced()||event.state().isAir()||changes.size()>=256)return;
            changes.put(event.position().asLong(),new Change(event.position(),event.state(),tick));});
        listen(PacketEvent.Receive.class,event->{if(otherPlayers.getValue()&&event.getPacket() instanceof ClientboundAnimatePacket packet
                &&(packet.getAction()==ClientboundAnimatePacket.SWING_MAIN_HAND||packet.getAction()==ClientboundAnimatePacket.SWING_OFF_HAND)){
            int id=packet.getId(),action=packet.getAction();long revision=enabledRevision();mc.execute(()->{if(isEnabled()&&enabledRevision()==revision)observe(id,action);});}});
        listen(UpdateEvent.class,event->{tick=event.getTick();if(observedLevel!=mc.level){actions.clear();changes.clear();observedLevel=mc.level;}
            actions.removeIf(a->tick-a.tick>3);changes.values().removeIf(change->{if(tick-change.tick>3)return true;if(mc.level==null||mc.level.getBlockState(change.pos)!=change.state)return true;
                var matching=new HashSet<Integer>();Action candidate=null;for(var a:actions)if(a.block==change.state.getBlock()&&(a.hit.equals(change.pos)||a.adjacent.equals(change.pos))){matching.add(a.player);candidate=a;}
                if(matching.size()==1&&candidate!=null){start(change.pos,change.state,candidate.eye);return true;}return false;});});
    }
    private void observe(int id,int hand){if(mc.level==null||mc.player==null||!otherPlayers.getValue())return;
        var entity=mc.level.getEntity(id);if(!(entity instanceof net.minecraft.world.entity.player.Player player)||player==mc.player||player.distanceToSqr(mc.player)>range.getValue()*range.getValue())return;
        var item=player.getItemInHand(hand==ClientboundAnimatePacket.SWING_MAIN_HAND?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
        if(!(item.getItem() instanceof BlockItem block)||!(player.pick(player.blockInteractionRange(),1,false) instanceof BlockHitResult hit))return;
        if(actions.size()>=128)actions.removeFirst();actions.add(new Action(id,block.getBlock(),hit.getBlockPos().immutable(),hit.getBlockPos().relative(hit.getDirection()),player.getEyePosition(),tick));
    }
    private void start(BlockPos pos,BlockState state,Vec3 eye){PlacementAnimations.start(pos,state,eye,motionSettings(),range.getValue(),limit.getValue().intValue());}
    @Override protected void onDisable(){actions.clear();changes.clear();PlacementAnimations.stop();}
}
