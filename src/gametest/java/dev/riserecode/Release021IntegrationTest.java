package dev.riserecode;

import dev.riserecode.module.modules.BlockAnimationsModule;
import dev.riserecode.module.modules.BlockOverlayModule;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.render.placement.PlacementAnimations;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Real item use, upload receipts, oriented highlights and shader lighting pixel comparisons. */
public final class Release021IntegrationTest {
    private static int checks;
    private static final java.util.concurrent.atomic.AtomicLong animationTime=new java.util.concurrent.atomic.AtomicLong(1_000_000_000L);
    private Release021IntegrationTest() { }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static BlockAnimationsModule animations(){return Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElseThrow();}
    private static BlockOverlayModule overlay(){return Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow();}
    private static FullBrightModule fullbright(){return Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();}
    private static dev.riserecode.setting.Setting<?> setting(String name){return overlay().getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow();}
    private static void flag(String name,boolean value){((BooleanSetting)setting(name)).setValue(value);}
    private static void number(String name,double value){((DoubleSetting)setting(name)).setValue(value);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);PlacementAnimations.timeSource(animationTime::get);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -6 64 -6 6 64 6 stone");
            world.getServer().runCommand("time set noon");world.getServer().runCommand("gamerule advance_time false");world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 2.5 65 3.5 145 0");c.waitTicks(12);
            c.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();animations().setEnabled(true);overlay().setEnabled(true);
                animations().duration.setValue(1.5);flag("Smooth Switching",false);flag("Pop Animation",false);flag("OverlayEnabled",false);
                ((ColorSetting)setting("OutlineColor")).setValue(0xFFFF00FF);number("OutlineWidth",3);});
            fixtures(c,world,"native");
            safeguards(c,world);
            remote(c,world);
            if(!Boolean.getBoolean("riserecode.skipPlacementBenchmark"))performance(c,world,"native");
            if(Boolean.getBoolean("riserecode.withShaderPack")){
                c.runOnClient(mc->shaders(true));c.waitFor(mc->dev.riserecode.render.ShaderWorldOverlay.active()&&mc.gui.overlay()==null,900);
                fixtures(c,world,"shader");
                if(!Boolean.getBoolean("riserecode.skipPlacementBenchmark"))performance(c,world,"shader");
                var reload=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reload.isDone()&&mc.gui.overlay()==null,600);
                c.runOnClient(mc->check(PlacementAnimations.pending()==0,"resource reload releases transient models and terrain suppression"));
                brightness(c,world);
                c.runOnClient(mc->shaders(false));c.waitFor(mc->!dev.riserecode.render.ShaderWorldOverlay.active()&&mc.gui.overlay()==null,600);
                c.runOnClient(mc->{check(fullbright().shaderStatus().equals("No shader pack active"),"shader-only mode reports inactive without shaders");fullbright().setEnabled(false);});
            }
            c.runOnClient(mc->{animations().setEnabled(true);animations().duration.setValue(2.0);overlay().setEnabled(false);});
            place(c,world,new BlockPos(0,65,0),Blocks.STONE);
            c.runOnClient(mc->check(PlacementAnimations.pending()>0,"disconnect fixture has an active placement"));
        }
        finally { c.runOnClient(mc->PlacementAnimations.timeSource(System::nanoTime)); }
        c.waitTicks(3);c.runOnClient(mc->{check(PlacementAnimations.pending()==0&&PlacementAnimations.draws().isEmpty(),"disconnect clears active placements without another world render");animations().setEnabled(false);});
        Client.INSTANCE.getLogger().info("Release 0.21 native checks passed ({} assertions).",checks);
    }
    private static void place(ClientGameTestContext c,TestSingleplayerContext world,BlockPos pos,Block block){
        world.getServer().runCommand("setblock "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" air");
        c.waitFor(mc->mc.level.getBlockState(pos).isAir(),160);
        world.getServer().runCommand("item replace entity @a hotbar.0 with "+net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block));
        c.waitFor(mc->mc.player.getInventory().getItem(0).is(block.asItem()),160);
        c.runOnClient(mc->{mc.player.getInventory().setSelectedSlot(0);mc.player.getInventory().setItem(0,new ItemStack(block));
            var support=pos.below();var hit=new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
            check(mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit).consumesAction(),"ordinary item interaction places "+block);
        });
        c.waitFor(mc->mc.level.getBlockState(pos).is(block),160);
    }
    private static void settle(ClientGameTestContext c){c.runOnClient(mc->animationTime.addAndGet(10_000_000_000L));c.waitFor(mc->PlacementAnimations.pending()==0,400);}
    private static void fixtures(ClientGameTestContext c,TestSingleplayerContext world,String mode){
        var pos=new BlockPos(0,65,0);long starts=c.computeOnClient(mc->PlacementAnimations.starts());
        c.runOnClient(mc->{animations().preset.setValue(BlockAnimationsModule.Preset.SWING);animations().duration.setValue(1.5);animations().outlineBehavior.setValue(BlockAnimationsModule.OutlineBehavior.FOLLOW_ANIMATION);});
        place(c,world,pos,Blocks.STONE);c.waitFor(mc->PlacementAnimations.terrainReady(pos),400);c.getInput().lookAt(pos);c.runOnClient(mc->animationTime.addAndGet(200_000_000));c.waitTicks(3);
        c.runOnClient(mc->{check(PlacementAnimations.starts()>starts,"real placement starts native animation");var draw=PlacementAnimations.at(pos);check(draw!=null&&draw.progress()<1,"mid-animation pose exists: pending="+PlacementAnimations.pending()+", block="+mc.level.getBlockState(pos)+", active="+PlacementAnimations.active()+", time="+PlacementAnimations.time());
            var lines=Client.INSTANCE.getWorldRenderer().getLines();check(lines.size()==12,"moving block keeps twelve outline edges");
            var expected=PlacementAnimations.transform(draw,new Vec3(-.002,64.998,-.002));
            check(lines.getFirst().from().distanceTo(expected)<.0001,"outline uses the model's actual rotated pose");
            check(!draw.pose().equals(new org.joml.Matrix4f(),.001f),"visible block pose has nonidentity rotation/scale");});
        c.takeScreenshot("021-"+mode+"-swing-follow");
        c.runOnClient(mc->animations().outlineBehavior.setValue(BlockAnimationsModule.OutlineBehavior.HIDE_UNTIL_SETTLED));c.waitTicks(2);
        c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().isEmpty(),"hide mode removes highlight during placement"));
        c.runOnClient(mc->animations().outlineBehavior.setValue(BlockAnimationsModule.OutlineBehavior.FIXED_POSITION));c.waitTicks(2);
        c.runOnClient(mc->check(Math.abs(Client.INSTANCE.getWorldRenderer().getLines().getFirst().from().x+.002)<.0001,"fixed outline remains at actual block position"));
        c.takeScreenshot("021-"+mode+"-fixed");settle(c);c.waitTicks(4);c.takeScreenshot("021-"+mode+"-settled");
        c.runOnClient(mc->check(PlacementAnimations.modelDraws()>0,"moving model was submitted through native renderer"));
        if(mode.equals("shader"))c.runOnClient(mc->check(PlacementAnimations.shadowDraws()>0,"moving model participates in Iris shadows"));
        for(var style:new BlockAnimationsModule.Preset[]{BlockAnimationsModule.Preset.POP,BlockAnimationsModule.Preset.SLIDE}){
            c.runOnClient(mc->{animations().preset.setValue(style);animations().outlineBehavior.setValue(BlockAnimationsModule.OutlineBehavior.FOLLOW_ANIMATION);});
            place(c,world,pos,style==BlockAnimationsModule.Preset.POP?Blocks.OAK_STAIRS:Blocks.GLASS);c.waitFor(mc->PlacementAnimations.terrainReady(pos),400);c.waitTicks(3);c.takeScreenshot("021-"+mode+"-"+style.name().toLowerCase(java.util.Locale.ROOT));settle(c);
        }
        lightingHandoff(c,world,mode);
        long before=c.computeOnClient(mc->PlacementAnimations.starts());
        place(c,world,pos,Blocks.GLOWSTONE);c.waitTicks(2);
        c.runOnClient(mc->check(mode.equals("shader")?PlacementAnimations.starts()==before:PlacementAnimations.starts()>before,"emissive block policy matches renderer"));settle(c);
        c.runOnClient(mc->{animations().preset.setValue(BlockAnimationsModule.Preset.SWING);animations().duration.setValue(2.0);});
        place(c,world,pos,Blocks.STONE);c.waitTicks(2);c.runOnClient(mc->animations().setEnabled(false));settle(c);
        c.takeScreenshot("021-"+mode+"-disabled-handoff");
        c.runOnClient(mc->{check(PlacementAnimations.pending()==0,"disable restores real terrain after upload");animations().setEnabled(true);});
        place(c,world,pos,Blocks.STONE);c.waitTicks(2);world.getServer().runCommand("setblock 0 65 0 air");
        c.waitFor(mc->PlacementAnimations.pending()==0,160);c.runOnClient(mc->check(PlacementAnimations.pending()==0,"breaking cancels placement and clears suppression"));
        long idle=c.computeOnClient(mc->PlacementAnimations.rebuilds());c.waitTicks(10);c.runOnClient(mc->check(PlacementAnimations.rebuilds()==idle,"idle frames never rebuild placement terrain"));
    }
    private static void lightingHandoff(ClientGameTestContext c,TestSingleplayerContext world,String renderer){
        var pos=new BlockPos(0,65,0);
        c.runOnClient(mc->{overlay().setEnabled(false);animations().scale.setValue(1.0);animations().height.setValue(1.0);
            for(var setting:java.util.List.of(animations().x,animations().y,animations().z,animations().rx,animations().ry,animations().rz))setting.setValue(0.0);});
        place(c,world,pos,Blocks.STONE);c.waitFor(mc->PlacementAnimations.terrainReady(pos),400);shot(c,renderer+"-lighting-moving");
        double moving=luminance(renderer+"-lighting-moving");settle(c);shot(c,renderer+"-lighting-terrain");double terrain=luminance(renderer+"-lighting-terrain");
        check(Math.abs(moving-terrain)<16,"moving model and terrain keep consistent scene lighting: "+moving+" / "+terrain);
        Client.INSTANCE.getLogger().info("Placement lighting comparison {}: moving {}, terrain {}",renderer,moving,terrain);
        c.runOnClient(mc->{overlay().setEnabled(true);animations().preset.setValue(BlockAnimationsModule.Preset.SWING);});
    }
    private static void safeguards(ClientGameTestContext c,TestSingleplayerContext world){
        c.runOnClient(mc->{animations().setEnabled(true);animations().yourPlacements.setValue(false);});
        long starts=c.computeOnClient(mc->PlacementAnimations.starts());
        place(c,world,new BlockPos(0,65,0),Blocks.STONE);c.waitTicks(3);
        c.runOnClient(mc->{check(PlacementAnimations.starts()==starts,"own placements toggle leaves terrain static");animations().yourPlacements.setValue(true);
            var pos=new BlockPos(0,65,0);var state=mc.level.getBlockState(pos);
            ((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);
            check(!PlacementAnimations.start(pos,state,mc.player.getEyePosition(),animations().motionSettings(),32,128),"Reduced Motion skips placement");
            ((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(false);
            var appearance=Client.INSTANCE.getThemeManager().module(animations());
            var global=(BooleanSetting)appearance.controls().getFirst().setting();global.setValue(false);
            ((BooleanSetting)appearance.get("Reduced Motion")).setValue(true);
            check(!PlacementAnimations.start(pos,state,mc.player.getEyePosition(),animations().motionSettings(),32,128),"module Reduced Motion skips placement");
            global.setValue(true);
            check(!PlacementAnimations.start(pos,Blocks.WATER.defaultBlockState(),mc.player.getEyePosition(),animations().motionSettings(),32,128),"fluids stay native");
            check(!PlacementAnimations.start(pos,Blocks.OAK_DOOR.defaultBlockState(),mc.player.getEyePosition(),animations().motionSettings(),32,128),"multipart blocks stay native");
            check(!PlacementAnimations.start(pos,state,mc.player.getEyePosition(),animations().motionSettings(),1,128),"out-of-range placement stays static");
            check(PlacementAnimations.start(pos,state,mc.player.getEyePosition(),animations().motionSettings(),32,1),"first capped placement starts");
            check(!PlacementAnimations.start(pos.east(),state,mc.player.getEyePosition(),animations().motionSettings(),32,1),"limit prevents excess placement");
        });settle(c);
        place(c,world,new BlockPos(0,65,0),Blocks.CHEST);c.waitTicks(4);c.takeScreenshot("021-block-entity-chest");
        c.runOnClient(mc->check(PlacementAnimations.at(new BlockPos(0,65,0))!=null,"single chest owns a moving block-entity pose"));settle(c);
    }
    private static void remote(ClientGameTestContext c,TestSingleplayerContext world){
        var pos=new BlockPos(0,65,0);
        world.getServer().runCommand("setblock 0 65 0 air");c.waitFor(mc->mc.level.getBlockState(pos).isAir(),160);
        c.runOnClient(mc->{animations().otherPlayers.setValue(true);
            for(int id:new int[]{20001,20002}){var player=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new java.util.UUID(0,id),"Placement"+id));
                player.setId(id);player.setPos(.5,65,3.5);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Blocks.STONE));
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,new Vec3(.5,65,.5));mc.level.addEntity(player);}
        });
        long before=c.computeOnClient(mc->PlacementAnimations.starts());
        world.getServer().runCommand("setblock 0 65 0 stone");c.waitTicks(8);
        c.runOnClient(mc->check(PlacementAnimations.starts()==before,"unattributed server edits stay static"));
        world.getServer().runCommand("setblock 0 65 0 air");c.waitFor(mc->mc.level.getBlockState(pos).isAir(),160);
        c.runOnClient(mc->swing(mc,20001));world.getServer().runCommand("setblock 0 65 0 stone");
        c.waitFor(mc->PlacementAnimations.starts()>before,160);
        c.runOnClient(mc->check(PlacementAnimations.starts()==before+1,"one nearby held-block swing matches server placement"));settle(c);c.waitTicks(8);
        world.getServer().runCommand("setblock 0 65 0 air");c.waitFor(mc->mc.level.getBlockState(pos).isAir(),160);
        c.runOnClient(mc->{swing(mc,20001);swing(mc,20002);});world.getServer().runCommand("setblock 0 65 0 stone");c.waitTicks(8);
        c.runOnClient(mc->{check(PlacementAnimations.starts()==before+1,"ambiguous remote placements stay static");animations().otherPlayers.setValue(false);
            mc.level.removeEntity(20001,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);mc.level.removeEntity(20002,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);});
    }
    private static void swing(net.minecraft.client.Minecraft mc,int id){Client.INSTANCE.getEventBus().post(new dev.riserecode.event.PacketEvent.Receive(new net.minecraft.network.protocol.game.ClientboundAnimatePacket(mc.level.getEntity(id),0)));}
    private static void performance(ClientGameTestContext c,TestSingleplayerContext world,String renderer){
        c.runOnClient(mc->{overlay().setEnabled(false);mc.options.framerateLimit().set(260);});settle(c);
        world.getServer().runCommand("fill -3 65 -2 2 65 -1 stone");c.waitTicks(15);
        var baseline=UpgradeBenchmark.measure(c,"021-"+renderer+"-static");
        c.runOnClient(mc->{for(var pos:BlockPos.betweenClosed(new BlockPos(-3,65,-2),new BlockPos(2,65,-1)))PlacementAnimations.start(pos,mc.level.getBlockState(pos),mc.player.getEyePosition(),animations().motionSettings(),32,128);});
        c.waitTicks(8);var moving=UpgradeBenchmark.measure(c,"021-"+renderer+"-12-moving");
        c.runOnClient(mc->check(PlacementAnimations.pending()==12,"benchmark retains twelve moving blocks"));
        Client.INSTANCE.getLogger().info("Placement frame comparison {}: static median/p95 {}/{}, moving {}/{} ms",renderer,baseline[0],baseline[1],moving[0],moving[1]);settle(c);
        world.getServer().runCommand("fill -3 65 -2 2 65 -1 air");c.waitTicks(5);c.runOnClient(mc->{overlay().setEnabled(true);mc.options.framerateLimit().set(120);});
    }
    private static void brightness(ClientGameTestContext c,TestSingleplayerContext world){
        c.runOnClient(mc->{animations().setEnabled(false);overlay().setEnabled(false);});settle(c);
        world.getServer().runCommand("fill -5 64 -5 5 73 5 stone");world.getServer().runCommand("fill -4 65 -4 4 72 4 air");
        world.getServer().runCommand("tp @a 0.5 66 2.5 180 0");world.getServer().runCommand("time set midnight");c.waitTicks(15);
        double gamma=c.computeOnClient(mc->mc.options.gamma().get());
        c.runOnClient(mc->{fullbright().getMode().setValue(FullBrightModule.Mode.SHADER_NIGHT_VISION);fullbright().getShaderStrength().setValue(0.0);fullbright().setEnabled(true);});
        shot(c,"nightvision-zero");double dark=luminance("nightvision-zero");
        c.runOnClient(mc->fullbright().getShaderStrength().setValue(.5));shot(c,"nightvision-half");double half=luminance("nightvision-half");
        c.runOnClient(mc->fullbright().getShaderStrength().setValue(1.0));shot(c,"nightvision-full");double bright=luminance("nightvision-full");
        check(bright>dark+12&&half>dark+4&&bright>=half-3,"shader strength increases real cave pixels: "+dark+" / "+half+" / "+bright);
        c.runOnClient(mc->{check(mc.options.gamma().get()==gamma,"shader mode leaves gamma unchanged");check(!mc.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"shader mode never adds a potion effect");check(fullbright().shaderNightVision(1)==1,"real stronger night vision is preserved");fullbright().setEnabled(false);});
        shot(c,"nightvision-restored");double restored=luminance("nightvision-restored");check(Math.abs(restored-dark)<8,"disable restores dark scene: "+restored+" vs "+dark);
        c.runOnClient(mc->fullbright().setEnabled(true));
    }
    private static void shot(ClientGameTestContext c,String name){c.waitTicks(15);c.takeScreenshot("021-"+name);}
    private static double luminance(String name){try(var files=Files.list(Path.of("screenshots"))){var file=files.filter(p->p.getFileName().toString().endsWith("021-"+name+".png")).findFirst().orElseThrow();var image=javax.imageio.ImageIO.read(file.toFile());long sum=0;int count=0;
        for(int y=image.getHeight()/4;y<image.getHeight()*3/4;y++)for(int x=image.getWidth()/4;x<image.getWidth()*3/4;x++){int rgb=image.getRGB(x,y);sum+=(rgb>>16&255)+(rgb>>8&255)+(rgb&255);count+=3;}return sum/(double)count;
    }catch(IOException error){throw new AssertionError(error);}}
    private static void shaders(boolean enabled){var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(enabled);try{config.save();net.irisshaders.iris.Iris.reload();}catch(IOException error){throw new AssertionError(error);}}
}
