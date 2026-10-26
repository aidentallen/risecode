package dev.riserecode.module.modules;

import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.*;
import dev.riserecode.util.gameplay.TrajectoryPrediction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class TrajectoriesModule extends Module {
    public final BooleanSetting held=register(new BooleanSetting("Held Aim","Preview the held projectile",true));
    public final BooleanSetting flying=register(new BooleanSetting("Flying Projectiles","Preview loaded flying projectile paths",false));
    public final DoubleSetting length=register(DoubleSetting.extended("Prediction Ticks","Maximum path duration",120,20,240));
    public final DoubleSetting width=register(DoubleSetting.extended("Width","Path line width",1.5,0.5,4));
    public final ColorSetting color=register(new ColorSetting("Color","Path color",0xFF69E0C2));
    private final Minecraft mc;private List<TrajectoryPrediction.Path> paths=List.of();
    public List<TrajectoryPrediction.Path> paths(){return paths;}
    public TrajectoriesModule(EventBus bus,Minecraft mc){super("Trajectories","Projectile aim and collision previews.",Category.RENDER,bus);this.mc=mc;}
    @Override protected void onEnable(){listen(UpdateEvent.class,e->update());listen(Render3DEvent.class,e->{for(var path:paths){for(int i=1;i<path.points().size();i++){e.addLine(path.points().get(i-1),path.points().get(i),color.sample(path.points().get(i-1).x*40,path.points().get(i-1).y*40),color.sample(path.points().get(i).x*40,path.points().get(i).y*40),width.getValue().floatValue());}if(path.impact()!=net.minecraft.world.phys.HitResult.Type.MISS&&!path.points().isEmpty()){Vec3 p=path.points().getLast();e.addLine(p.add(-0.16,0,0),p.add(0.16,0,0),color.sample(p.x*40,p.y*40),2);e.addLine(p.add(0,0,-0.16),p.add(0,0,0.16),color.sample(p.x*40,p.y*40),2);}}});}
    private void update(){
        if(mc.player==null||mc.level==null){paths=List.of();return;}List<TrajectoryPrediction.Path> out=new ArrayList<>();
        if(held.getValue()){
            ItemStack stack=mc.player.getMainHandItem();if(!supported(stack)){stack=mc.player.getOffhandItem();}
            if(supported(stack)){for(Projectile p:launch(stack)){out.add(TrajectoryPrediction.predict(mc.level,p,length.getValue().intValue()));}}
        }
        if(flying.getValue()){int count=0;long end=System.nanoTime()+2_000_000;for(Entity entity:mc.level.entitiesForRendering()){if(count>=16||System.nanoTime()>end){break;}if(entity instanceof Projectile p&&TrajectoryPrediction.supported(p)&&p.getDeltaMovement().lengthSqr()>1e-6&&mc.player.distanceToSqr(p)<16384){out.add(TrajectoryPrediction.predict(mc.level,p,length.getValue().intValue()));count++;}}}
        paths=List.copyOf(out);
    }
    private static boolean supported(ItemStack s){return s.is(Items.BOW)||s.is(Items.CROSSBOW)||s.is(Items.TRIDENT)||s.is(Items.SNOWBALL)||s.is(Items.EGG)||s.is(Items.ENDER_PEARL)||s.is(Items.EXPERIENCE_BOTTLE)||s.is(Items.SPLASH_POTION)||s.is(Items.LINGERING_POTION)||s.is(Items.WIND_CHARGE);}
    public List<Projectile> launch(ItemStack stack){
        var player=mc.player;EntityType<?> type;float speed=1.5f,pitch=0;boolean crossbow=stack.is(Items.CROSSBOW);
        if(stack.is(Items.BOW)){float charge=player.isUsingItem()?BowItem.getPowerForTime(player.getTicksUsingItem()):1;if(charge<0.1){return List.of();}speed=charge*3;type=EntityTypes.ARROW;}
        else if(crossbow){if(!CrossbowItem.isCharged(stack)){return List.of();}type=EntityTypes.ARROW;speed=3.15f;}
        else if(stack.is(Items.TRIDENT)){if(player.isUsingItem()&&player.getTicksUsingItem()<10||net.minecraft.world.item.enchantment.EnchantmentHelper.getTridentSpinAttackStrength(stack,player)>0){return List.of();}type=EntityTypes.TRIDENT;speed=2.5f;}
        else if(stack.is(Items.SNOWBALL)){type=EntityTypes.SNOWBALL;}
        else if(stack.is(Items.EGG)){type=EntityTypes.EGG;}
        else if(stack.is(Items.ENDER_PEARL)){type=EntityTypes.ENDER_PEARL;}
        else if(stack.is(Items.EXPERIENCE_BOTTLE)){type=EntityTypes.EXPERIENCE_BOTTLE;speed=0.7f;pitch=-20;}
        else if(stack.is(Items.SPLASH_POTION)){type=EntityTypes.SPLASH_POTION;speed=0.5f;pitch=-20;}
        else if(stack.is(Items.LINGERING_POTION)){type=EntityTypes.LINGERING_POTION;speed=0.5f;pitch=-20;}
        else if(stack.is(Items.WIND_CHARGE)){type=EntityTypes.WIND_CHARGE;speed=1.5f;}else{return List.of();}
        int shots=1;if(crossbow){var charged=stack.get(DataComponents.CHARGED_PROJECTILES);if(charged!=null){shots=Math.min(3,charged.items().size());if(!charged.items().isEmpty()&&charged.items().getFirst().is(Items.FIREWORK_ROCKET)){type=EntityTypes.FIREWORK_ROCKET;speed=1.6f;}}}
        List<Projectile> result=new ArrayList<>();for(int i=0;i<shots;i++){
            Entity created=type==EntityTypes.FIREWORK_ROCKET
                ?new FireworkRocketEntity(mc.level,stack.get(DataComponents.CHARGED_PROJECTILES).items().getFirst().create(),player,player.getX(),player.getEyeY()-0.1,player.getZ(),true)
                :type.create(mc.level,EntitySpawnReason.COMMAND);if(!(created instanceof Projectile p)){continue;}p.setOwner(player);p.setPos(player.getX(),player.getEyeY()-0.1,player.getZ());
            if(crossbow){var up=player.getUpVector(1);var direction=player.getViewVector(1).toVector3f().rotate(new org.joml.Quaternionf().setAngleAxis(Math.toRadians(shots==1?0:(i-1)*10),up.x,up.y,up.z));p.shoot(direction.x,direction.y,direction.z,speed,0);}
            else{p.shootFromRotation(player,player.getXRot(),player.getYRot(),pitch,speed,0);}
            result.add(p);
        }return List.copyOf(result);
    }
    @Override protected void onDisable(){paths=List.of();}
}
