package dev.riserecode;

import dev.riserecode.module.modules.BlockOverlayModule;
import dev.riserecode.module.modules.HitboxesModule;
import dev.riserecode.render.TargetHighlight;
import dev.riserecode.setting.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Native crosshair targeting, continuous highlight geometry, shader pixels and lifecycle cleanup. */
public final class Release022IntegrationTest {
    private static final AtomicLong CLOCK=new AtomicLong(1_000_000_000L);
    private static final BlockPos BLOCK=new BlockPos(-1,66,0), COW=new BlockPos(1,65,0), PLAYER=new BlockPos(-2,65,0);
    private static int checks;
    private static UUID cowId;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static HitboxesModule boxes(){return Client.INSTANCE.getModuleManager().getByClass(HitboxesModule.class).orElseThrow();}
    private static BlockOverlayModule blocks(){return Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow();}
    private static Setting<?> blockSetting(String name){return blocks().getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow();}
    private static void flag(String name,boolean value){((BooleanSetting)blockSetting(name)).setValue(value);}
    private static void number(String name,double value){((DoubleSetting)blockSetting(name)).setValue(value);}
    private static void advance(ClientGameTestContext c,long nanos){c.runOnClient(mc->CLOCK.addAndGet(nanos));c.waitTicks(3);}
    private static void aimBlock(ClientGameTestContext c){c.getInput().lookAt(BLOCK);c.waitFor(mc->mc.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(BLOCK),160);c.waitTicks(3);}
    private static void aimEntity(ClientGameTestContext c,BlockPos position){c.getInput().lookAt(position);c.waitFor(mc->mc.hitResult instanceof EntityHitResult,160);c.waitTicks(3);}
    private static void air(ClientGameTestContext c){c.getInput().lookAt(0,-80);c.waitTicks(3);c.runOnClient(mc->check(mc.hitResult==null||mc.hitResult.getType()==net.minecraft.world.phys.HitResult.Type.MISS,"sky is genuinely empty"));}
    private static java.util.List<dev.riserecode.event.Render3DEvent.Line> lines(){return Client.INSTANCE.getWorldRenderer().getLines();}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();Client.INSTANCE.getTargetHighlights().timeSource(CLOCK::get);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -8 64 -8 8 64 8 stone");
            world.getServer().runCommand("setblock -1 66 0 stone");world.getServer().runCommand("time set noon");world.getServer().runCommand("gamerule advance_time false");
            world.getServer().runCommand("weather clear");world.getServer().runCommand("tp @a 0.5 65 3.0 180 0");
            world.getServer().runCommand("summon cow 1.5 65 0.5 {Tags:[\"hitbox_cow\"],NoAI:1b,Silent:1b,PersistenceRequired:1b}");
            world.getServer().runCommand("summon armor_stand 3.5 65 0.5 {NoGravity:1b}");
            c.waitTicks(15);
            c.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();
                var remote=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new UUID(0,22001),"HitboxFixture"));remote.setId(22001);remote.setPos(-1.5,65,.5);mc.level.addEntity(remote);
                cowId=mc.level.getEntities(mc.player,mc.player.getBoundingBox().inflate(12),e->e.getType().toShortString().equals("cow")).getFirst().getUUID();
                blocks().setEnabled(true);boxes().setEnabled(true);flag("OverlayEnabled",true);flag("OutlineEnabled",true);flag("Pop Animation",true);flag("Pop On Switch",false);flag("Smooth Switching",true);
                number("Transition Duration",1);number("Block Pop Duration",1);number("OutlineWidth",5);((ColorSetting)blockSetting("OutlineColor")).setValue(0xFFFF00FF);
            });
            verify(c,world,"native");
            if(Boolean.getBoolean("riserecode.withShaderPack")){
                c.runOnClient(mc->shaders(true));c.waitFor(mc->dev.riserecode.render.ShaderWorldOverlay.active()&&mc.gui.overlay()==null,900);
                verify(c,world,"shader");
                c.runOnClient(mc->boxes().mode.setValue(HitboxesModule.Mode.ALWAYS));c.waitTicks(3);
                c.runOnClient(mc->check(boxes().trackedEntrances()>0,"real reload fixture contains Always-mode entrance state"));
                var cleared=new java.util.concurrent.atomic.AtomicBoolean();
                var observer=c.computeOnClient(mc->Client.INSTANCE.getEventBus().subscribe(dev.riserecode.event.RenderResourcesReloadEvent.class,Integer.MIN_VALUE,false,event->{
                    check(boxes().trackedEntrances()==0&&Client.INSTANCE.getTargetHighlights().targetKey()==null,"native resource reload clears all transient highlight state before another frame");cleared.set(true);
                }));
                try{var reload=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reload.isDone()&&mc.gui.overlay()==null,900);check(cleared.get(),"actual native reload emitted its cleanup event");}
                finally{c.runOnClient(mc->observer.close());}
                c.runOnClient(mc->boxes().mode.setValue(HitboxesModule.Mode.ONLY_WHEN_LOOKING));
                aimEntity(c,COW);advance(c,2_000_000_000L);c.takeScreenshot("022-shader-reload");check(magenta("shader-reload")>150,"entity outline survives shader resource reload");
                c.runOnClient(mc->shaders(false));c.waitFor(mc->!dev.riserecode.render.ShaderWorldOverlay.active()&&mc.gui.overlay()==null,600);
            }
            // Quit with a live target and populated Always-mode entrance cache.
            c.runOnClient(mc->{boxes().mode.setValue(HitboxesModule.Mode.ALWAYS);});c.waitTicks(3);
            c.runOnClient(mc->check(boxes().trackedEntrances()>0,"disconnect fixture has live entity animation state"));
        }finally{c.runOnClient(mc->Client.INSTANCE.getTargetHighlights().timeSource(System::nanoTime));}
        c.waitTicks(3);c.runOnClient(mc->{check(boxes().trackedEntrances()==0&&Client.INSTANCE.getTargetHighlights().targetKey()==null,"disconnect clears shared and per-entity state");boxes().setEnabled(false);blocks().setEnabled(false);});
        Client.INSTANCE.getLogger().info("Release 0.22 native checks passed ({} assertions).",checks);
    }
    private static void verify(ClientGameTestContext c,TestSingleplayerContext world,String renderer){
        c.runOnClient(mc->{boxes().mode.setValue(HitboxesModule.Mode.ONLY_WHEN_LOOKING);boxes().inheritStyle.setValue(true);boxes().inheritAnimation.setValue(true);boxes().mobs.setValue(true);boxes().players.setValue(true);boxes().others.setValue(true);blocks().setEnabled(true);boxes().setEnabled(true);});
        air(c);c.runOnClient(mc->check(lines().isEmpty()&&Client.INSTANCE.getTargetHighlights().targetKey()==null,"target-only draws nothing in empty space"));
        aimBlock(c);advance(c,2_000_000_000L);
        var previous=c.computeOnClient(mc->lines().getFirst().from());
        aimEntity(c,COW);
        c.runOnClient(mc->{check(lines().size()==12,"block-to-entity emits one twelve-edge box");check(lines().getFirst().from().distanceTo(previous)<.0001,"entity switch starts from the displayed block corners");});
        advance(c,500_000_000L);c.takeScreenshot("022-"+renderer+"-block-to-entity");
        c.runOnClient(mc->check(lines().getFirst().from().distanceTo(previous)>.1,"switch advances toward entity bounds"));
        advance(c,1_000_000_000L);c.takeScreenshot("022-"+renderer+"-entity-settled");check(magenta(renderer+"-entity-settled")>150,"rendered entity hitbox has visible inherited color");
        var cow=c.computeOnClient(mc->mc.level.getEntity(((EntityHitResult)mc.hitResult).getEntity().getId()));
        var originalBounds=c.computeOnClient(mc->cow.getBoundingBox());
        c.runOnClient(mc->{var expected=cow.getBoundingBox().inflate(.002);check(Math.abs(lines().getFirst().from().x-expected.minX)<.001,"settled outline matches the actual entity box");check(Client.INSTANCE.getWorldRenderer().getQuads().isEmpty(),"settled entity has no block fill");});
        var entityPoint=c.computeOnClient(mc->lines().getFirst().from());aimBlock(c);
        c.runOnClient(mc->check(lines().getFirst().from().distanceTo(entityPoint)<.001,"entity-to-block uses the same continuous switch"));advance(c,2_000_000_000L);
        aimEntity(c,PLAYER);advance(c,2_000_000_000L);
        c.runOnClient(mc->check(mc.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof net.minecraft.world.entity.player.Player&&lines().size()==12,"players use normal crosshair selection"));
        var playerPoint=c.computeOnClient(mc->lines().getFirst().from());aimEntity(c,COW);
        c.runOnClient(mc->check(lines().getFirst().from().distanceTo(playerPoint)<.001,"entity-to-entity transition begins at the displayed player box"));advance(c,2_000_000_000L);
        air(c);c.runOnClient(mc->check(lines().isEmpty(),"looking away hides immediately"));aimEntity(c,COW);
        c.runOnClient(mc->check(lines().getFirst().from().distanceTo(cow.getBoundingBox().getCenter())<.001,"new acquisition restarts center-out pop"));
        advance(c,400_000_000L);c.takeScreenshot("022-"+renderer+"-pop");advance(c,2_000_000_000L);
        c.runOnClient(mc->{boxes().inheritStyle.setValue(false);boxes().color.setValue(0xFFFF9000);boxes().width.setValue(9.0);boxes().through.setValue(true);boxes().style.setValue(BlockOverlayModule.OutlineStyle.CORNERS);boxes().gap.setValue(60.0);});
        c.waitTicks(3);c.runOnClient(mc->check(lines().size()==24&&lines().stream().allMatch(l->l.width()==9&&!l.visibleOnly()),"individual style applies corners, width and depth mode"));c.takeScreenshot("022-"+renderer+"-individual-style");
        c.runOnClient(mc->{boxes().inheritStyle.setValue(true);boxes().mobs.setValue(false);});c.waitTicks(3);c.runOnClient(mc->check(lines().isEmpty(),"mob filter hides an aimed cow"));
        c.runOnClient(mc->boxes().mobs.setValue(true));advance(c,2_000_000_000L);
        c.runOnClient(mc->blocks().setEnabled(false));advance(c,2_000_000_000L);c.runOnClient(mc->check(lines().size()==12,"Hitboxes works when BlockOverlay is disabled"));
        c.runOnClient(mc->boxes().setEnabled(false));c.waitTicks(3);c.runOnClient(mc->check(lines().isEmpty(),"disable leaves no hitbox geometry"));
        c.runOnClient(mc->{boxes().setEnabled(true);blocks().setEnabled(true);boxes().mode.setValue(HitboxesModule.Mode.ALWAYS);});air(c);advance(c,2_000_000_000L);
        c.runOnClient(mc->check(lines().size()>=36&&boxes().trackedEntrances()>=3,"Always shows nearby player, mob and object boxes without aiming"));c.takeScreenshot("022-"+renderer+"-always");
        c.runOnClient(mc->{boxes().mobs.setValue(false);boxes().others.setValue(false);});c.waitTicks(3);
        c.runOnClient(mc->{check(lines().size()==12,"Always filters leave only the fixture player");check(cow.getBoundingBox().equals(originalBounds),"visual hitboxes never change real collision bounds");
            boxes().mode.setValue(HitboxesModule.Mode.ONLY_WHEN_LOOKING);boxes().mobs.setValue(true);boxes().others.setValue(true);});
        additional(c,world,renderer);
        air(c);
    }
    private static void additional(ClientGameTestContext c,TestSingleplayerContext world,String renderer){
        c.runOnClient(mc->((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true));
        air(c);aimEntity(c,PLAYER);
        c.runOnClient(mc->{var target=((EntityHitResult)mc.hitResult).getEntity();check(lines().getFirst().from().distanceTo(new Vec3(target.getBoundingBox().minX-.002,target.getBoundingBox().minY-.002,target.getBoundingBox().minZ-.002))<.001,"Reduced Motion snaps directly to entity bounds");
            ((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(false);
            var remote=mc.level.getEntity(22001);remote.setPos(-1.3,65,.5);remote.setOldPosAndRot();remote.setPose(net.minecraft.world.entity.Pose.CROUCHING);remote.refreshDimensions();});
        c.waitTicks(3);advance(c,2_000_000_000L);
        c.runOnClient(mc->{var target=mc.level.getEntity(22001);double actual=target.getBoundingBox().getYsize();double min=lines().stream().mapToDouble(l->Math.min(l.from().y,l.to().y)).min().orElseThrow(),max=lines().stream().mapToDouble(l->Math.max(l.from().y,l.to().y)).max().orElseThrow();
            check(Math.abs(max-min-actual-.004)<.001,"moving and crouching player changes bounds without restarting pop");
            target.setInvisible(true);});c.waitTicks(3);c.runOnClient(mc->check(lines().isEmpty(),"invisible entities are excluded by default"));
        c.runOnClient(mc->boxes().invisible.setValue(true));advance(c,2_000_000_000L);
        c.runOnClient(mc->{check(lines().size()==12,"Include Invisible restores the selected entity box");mc.level.getEntity(22001).setInvisible(false);boxes().invisible.setValue(false);});
        // The actual vanilla picker must select terrain before the cow behind it.
        world.getServer().runCommand("setblock 1 65 1 stone");world.getServer().runCommand("setblock 1 66 1 stone");c.waitTicks(5);
        c.getInput().lookAt(COW);c.waitTicks(4);
        c.runOnClient(mc->check(mc.hitResult instanceof BlockHitResult,"terrain blocks the entity's normal crosshair selection"));
        world.getServer().runCommand("setblock 1 65 1 air");world.getServer().runCommand("setblock 1 66 1 air");c.waitTicks(5);aimEntity(c,COW);advance(c,2_000_000_000L);
        world.getServer().runCommand("tp @e[tag=hitbox_cow] 1.5 65 -15.5");c.waitTicks(8);c.getInput().lookAt(new BlockPos(1,65,-16));c.waitTicks(4);
        c.runOnClient(mc->check(!(mc.hitResult instanceof EntityHitResult),"normal reach does not reveal distant entity hitboxes"));
        world.getServer().runCommand("tp @e[tag=hitbox_cow] 1.5 65 0.5");c.waitTicks(8);
        c.runOnClient(mc->{var remote=mc.level.getEntity(22001);remote.setPos(-1.5,65,.5);remote.setOldPosAndRot();remote.setPose(net.minecraft.world.entity.Pose.STANDING);remote.refreshDimensions();
            boxes().mode.setValue(HitboxesModule.Mode.ALWAYS);});air(c);advance(c,2_000_000_000L);
        c.runOnClient(mc->{check(boxes().trackedEntrances()>0,"Always has entrance state before reload invalidation");Client.INSTANCE.getEventBus().post(new dev.riserecode.event.RenderResourcesReloadEvent());check(boxes().trackedEntrances()==0,"resource application clears per-entity entrance state synchronously");
            boxes().mode.setValue(HitboxesModule.Mode.ONLY_WHEN_LOOKING);});
    }
    private static int magenta(String name){try(var files=Files.list(Path.of("screenshots"))){var file=files.filter(p->p.getFileName().toString().endsWith("022-"+name+".png")).findFirst().orElseThrow();var image=javax.imageio.ImageIO.read(file.toFile());int count=0;
        for(int y=image.getHeight()/8;y<image.getHeight()*7/8;y++)for(int x=image.getWidth()/8;x<image.getWidth()*7/8;x++){int rgb=image.getRGB(x,y);if((rgb>>16&255)>140&&(rgb&255)>140&&(rgb>>8&255)<110)count++;}return count;
    }catch(IOException error){throw new AssertionError(error);}}
    private static void shaders(boolean enabled){var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(enabled);try{config.save();net.irisshaders.iris.Iris.reload();}catch(IOException error){throw new AssertionError(error);}}
}
