package dev.riserecode.module.modules;

import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.ItemUtil;
import dev.riserecode.util.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Server-visible fall protection and ordinary item placement, with owned reversible input state. */
public final class NoFallModule extends Module {
    public enum Mode { PACKET, AUTO, WATER, LADDER, POWDER_SNOW, COBWEB, SLIME }
    private final EnumSetting<Mode> mode=register(new EnumSetting<>("Mode","Packet protection or a reachable hotbar/offhand clutch",Mode.AUTO));
    private final Minecraft minecraft;
    private volatile boolean packetProtection;
    private volatile Attempt attempt;
    private volatile boolean acknowledged;
    private RotationManager.Lease rotation;
    private ItemUtil.HotbarSelection selection;
    private RotationManager.Rotation aim;
    private boolean requested,interrupted;
    private Mode previousMode=Mode.AUTO;
    private long requestedTick;
    private int preparedSlot=-1;
    private Object observedLevel,observedPlayer;
    private String status="Ready";
    private record Attempt(Mode mode,BlockPos position,BlockHitResult hit,InteractionHand hand,int slot,Item item){}
    public NoFallModule(EventBus bus,Minecraft minecraft){super("Clutch","Fall protection and automatic item clutches",Category.PLAYER,bus,"NoFall");this.minecraft=minecraft;mode.onChange(()->packetProtection=false);}
    public EnumSetting<Mode> getMode(){return mode;}
    public String status(){return status;}
    @Override protected void onEnable(){
        interrupted=false;
        listen(UpdateEvent.class,100,false,this::update);
        listen(UpdateEvent.class,-80,false,this::interact);
        listen(PacketEvent.Send.class,this::packet);
        listen(PacketEvent.Receive.class,event->{Attempt current=attempt;if(current!=null&&event.getPacket() instanceof ClientboundBlockUpdatePacket update&&update.getPos().equals(current.position())){acknowledged=matches(current.mode(),update.getBlockState().getBlock());}});
    }
    private boolean eligible(){var p=minecraft.player;return p!=null&&minecraft.level!=null&&minecraft.gameMode!=null&&minecraft.gui.screen()==null&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&!p.getAbilities().flying&&!p.isFallFlying()&&!p.isPassenger()&&!p.isInWater()&&!p.isInLava()&&!p.onClimbable();}
    private void update(UpdateEvent event){
        var p=minecraft.player;
        if(observedLevel!=minecraft.level||observedPlayer!=p){clear(false);interrupted=false;observedLevel=minecraft.level;observedPlayer=p;}
        if(previousMode!=mode.getValue()){clear(false);previousMode=mode.getValue();}
        if(!eligible()){clear(false);status="Paused";return;}
        packetProtection=mode.getValue()==Mode.PACKET&&!p.onGround()&&p.getDeltaMovement().y<0&&p.fallDistance>=1;
        if(mode.getValue()==Mode.PACKET){release(false);status=packetProtection?"Packet protection active":"Ready";return;}
        if(p.onGround()||p.getDeltaMovement().y>=0){clear(false);interrupted=false;status="Ready";return;}
        if(interrupted){return;}
        if(preparedSlot>=0&&selection==null&&p.getInventory().getSelectedSlot()!=preparedSlot){clear(true);interrupted=true;status="Cancelled: manual slot change";return;}
        if(selection!=null&&selection.wasManuallyOverridden()){clear(true);interrupted=true;status="Cancelled: manual slot change";return;}
        if(rotation!=null&&rotation.isActive()&&aim!=null&&RotationManager.angularDistance(new RotationManager.Rotation(p.getYRot(),p.getXRot()),aim)>8){clear(true);interrupted=true;status="Cancelled: manual camera movement";return;}
        if(requested){
            if(acknowledged){status="Placement confirmed";release(false);return;}
            if(event.getTick()-requestedTick>12&&!acknowledged){status="Placement was not confirmed";release(false);attempt=null;}
            return;
        }
        BlockHitResult landing=predict();
        if(landing==null){release(false);status="No reachable landing";return;}
        Attempt next=choose(landing);
        if(next==null){release(false);status="No usable clutch item or support";return;}
        if(preparedSlot<0){preparedSlot=p.getInventory().getSelectedSlot();}
        attempt=next;aim=RotationManager.toward(p.getEyePosition(),next.hit().getLocation());
        if(rotation==null){rotation=dev.riserecode.Client.INSTANCE.getRotationManager().acquire(this,200);}
        rotation.request(aim,RotationManager.Mode.SNAPPY,1080);status="Preparing "+next.mode().name().toLowerCase(java.util.Locale.ROOT);
    }
    private BlockHitResult predict(){
        var p=minecraft.player;Vec3 position=p.position(),velocity=p.getDeltaMovement();
        for(int i=0;i<30;i++){
            Vec3 next=position.add(velocity);
            if(!minecraft.level.hasChunk(BlockPos.containing(next).getX()>>4,BlockPos.containing(next).getZ()>>4)){return null;}
            var hit=minecraft.level.clip(new ClipContext(position,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(hit.getType()==HitResult.Type.BLOCK){if(minecraft.level.getBlockState(hit.getBlockPos()).is(Blocks.SLIME_BLOCK)&&!p.isShiftKeyDown()){return null;}return hit.getDirection()==Direction.UP&&p.fallDistance+Math.max(0,p.getY()-hit.getLocation().y)>3?hit:null;}
            position=next;velocity=new Vec3(velocity.x*.91,(velocity.y-.08)*.98,velocity.z*.91);
        }
        return null;
    }
    private Attempt choose(BlockHitResult landing){
        Mode[] modes=mode.getValue()==Mode.AUTO?new Mode[]{Mode.WATER,Mode.POWDER_SNOW,Mode.COBWEB,Mode.LADDER,Mode.SLIME}:new Mode[]{mode.getValue()};
        BlockPos place=landing.getBlockPos().above();
        if(!minecraft.level.getBlockState(place).canBeReplaced()){return null;}
        for(Mode candidate:modes){
            if(candidate==Mode.WATER&&minecraft.level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,place)){continue;}
            if(candidate==Mode.SLIME&&minecraft.player.isShiftKeyDown()){continue;}
            BlockHitResult hit=new BlockHitResult(landing.getLocation(),Direction.UP,landing.getBlockPos(),false);
            BlockPos destination=place;
            if(candidate==Mode.LADDER){
                hit=null;
                // Catch the fall on a nearby wall before reaching the floor; a floor-height
                // ladder can be skipped by a fast falling player in a single movement tick.
                BlockPos feet=BlockPos.containing(minecraft.player.position().add(0,-.8,0));
                for(int down=0;down<3&&hit==null;down++){
                    BlockPos ladder=feet.below(down);
                    if(ladder.getY()<place.getY()||!minecraft.level.getBlockState(ladder).canBeReplaced()){continue;}
                    for(Direction side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){
                        BlockPos wall=ladder.relative(side);Direction face=side.getOpposite();
                        if(Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,face).canSurvive(minecraft.level,ladder)){
                            destination=ladder;hit=new BlockHitResult(Vec3.atCenterOf(wall).add(face.getStepX()*.501,0,face.getStepZ()*.501),face,wall,false);break;
                        }
                    }
                }
                if(hit==null){continue;}
            }
            Item item=switch(candidate){case WATER->Items.WATER_BUCKET;case POWDER_SNOW->Items.POWDER_SNOW_BUCKET;case COBWEB->Items.COBWEB;case LADDER->Items.LADDER;case SLIME->Items.SLIME_BLOCK;default->Items.AIR;};
            if(minecraft.player.getOffhandItem().is(item)){return new Attempt(candidate,destination,hit,InteractionHand.OFF_HAND,-1,item);}
            var inventory=minecraft.player.getInventory();int current=inventory.getSelectedSlot();
            if(inventory.getItem(current).is(item)){return new Attempt(candidate,destination,hit,InteractionHand.MAIN_HAND,current,item);}
            for(int slot=0;slot<9;slot++){if(inventory.getItem(slot).is(item)){return new Attempt(candidate,destination,hit,InteractionHand.MAIN_HAND,slot,item);}}
        }
        return null;
    }
    private void interact(UpdateEvent event){
        Attempt current=attempt;if(mode.getValue()==Mode.PACKET||requested||current==null||!eligible()||rotation==null){return;}
        var p=minecraft.player;
        if(p.getEyePosition().distanceToSqr(current.hit().getLocation())>p.blockInteractionRange()*p.blockInteractionRange()){return;}
        if(!dev.riserecode.Client.INSTANCE.getRotationManager().isReady(rotation,aim,3)){return;}
        var face=current.hit().getDirection();var end=current.hit().getLocation().add(-face.getStepX()*.02,-face.getStepY()*.02,-face.getStepZ()*.02);
        var visible=minecraft.level.clip(new ClipContext(p.getEyePosition(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(visible.getType()!=HitResult.Type.BLOCK||!visible.getBlockPos().equals(current.hit().getBlockPos())){clear(false);status="Clutch target is obstructed";return;}
        if(current.hand()==InteractionHand.MAIN_HAND){
            if(!p.getInventory().getItem(current.slot()).is(current.item())){clear(false);return;}
            selection=new ItemUtil.HotbarSelection(p);selection.select(current.slot());
        }else if(!p.getOffhandItem().is(current.item())){clear(false);return;}
        var result=current.mode()==Mode.WATER?minecraft.gameMode.useItem(p,current.hand()):minecraft.gameMode.useItemOn(p,current.hand(),current.hit());
        if(result.consumesAction()){p.swing(current.hand());requested=true;requestedTick=event.getTick();status="Placement requested";}
        else{clear(false);status="Placement rejected";}
    }
    private static boolean matches(Mode mode,Block block){return switch(mode){case WATER->block==Blocks.WATER;case POWDER_SNOW->block==Blocks.POWDER_SNOW;case COBWEB->block==Blocks.COBWEB;case LADDER->block==Blocks.LADDER;case SLIME->block==Blocks.SLIME_BLOCK;default->false;};}
    private void packet(PacketEvent.Send event){
        if(!packetProtection||!(event.getPacket() instanceof ServerboundMovePlayerPacket p)){return;}
        // Preserve every field and variant; queued Netty sends use the immutable tick decision.
        if(p instanceof ServerboundMovePlayerPacket.PosRot){event.setPacket(new ServerboundMovePlayerPacket.PosRot(p.getX(0),p.getY(0),p.getZ(0),p.getYRot(0),p.getXRot(0),true,p.horizontalCollision()));}
        else if(p instanceof ServerboundMovePlayerPacket.Pos){event.setPacket(new ServerboundMovePlayerPacket.Pos(p.getX(0),p.getY(0),p.getZ(0),true,p.horizontalCollision()));}
        else if(p instanceof ServerboundMovePlayerPacket.Rot){event.setPacket(new ServerboundMovePlayerPacket.Rot(p.getYRot(0),p.getXRot(0),true,p.horizontalCollision()));}
        else if(p instanceof ServerboundMovePlayerPacket.StatusOnly){event.setPacket(new ServerboundMovePlayerPacket.StatusOnly(true,p.horizontalCollision()));}
    }
    private void release(boolean preserveCamera){
        float yaw=minecraft.player==null?0:minecraft.player.getYRot(),pitch=minecraft.player==null?0:minecraft.player.getXRot();
        if(selection!=null){if(selection.getPlayer()==minecraft.player){selection.close();}else{selection.abandon();}selection=null;}
        if(rotation!=null){rotation.close();rotation=null;}
        if(preserveCamera&&minecraft.player!=null){minecraft.player.setYRot(yaw);minecraft.player.setXRot(pitch);}
        aim=null;preparedSlot=-1;
    }
    private void clear(boolean preserveCamera){packetProtection=false;release(preserveCamera);attempt=null;requested=false;acknowledged=false;}
    @Override protected void onDisable(){clear(false);status="Disabled";}
}
