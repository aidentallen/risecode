package dev.riserecode;

import dev.riserecode.module.modules.BlockAnimationsModule;
import dev.riserecode.render.placement.PlacementAnimations;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;

/** One pinned BBE chest handoff with actual cached-mesh flags and acknowledged motion. */
public final class ReviewBbeIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static boolean ready(net.minecraft.client.Minecraft mc,BlockPos pos){
        try{return (boolean)mc.level.getBlockEntity(pos).getClass().getMethod("bbe$isTerrainMeshReady").invoke(mc.level.getBlockEntity(pos));}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    public static void run(ClientGameTestContext context){
        context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        context.runOnClient(mc->{check(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betterblockentities"),"Pinned BBE actually loaded");Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;});
        var module=Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElseThrow();
        var clock=new AtomicLong(System.nanoTime());var pos=new BlockPos(0,65,0);
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -5 64 -5 5 64 5 stone");world.getServer().runCommand("setblock 0 65 0 chest[facing=north]");world.getServer().runCommand("tp @a 2.5 65 3.5 145 0");
            context.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();try{Class.forName("betterblockentities.client.gui.config.ConfigCache").getField("updateType").setInt(null,0);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}});
            context.waitFor(mc->mc.level.getBlockEntity(pos)!=null&&ready(mc,pos),400);context.takeScreenshot("review-bbe-native-chest");
            long suppressed=PlacementAnimations.bbeSuppressions();
            context.runOnClient(mc->{PlacementAnimations.clear(true);PlacementAnimations.timeSource(clock::get);module.duration.setValue(1.0);module.setEnabled(true);check(PlacementAnimations.start(pos,mc.level.getBlockState(pos),mc.player.position(),module.motionSettings(),64,64),"BBE-backed chest placement admits native handoff");});
            context.waitFor(mc->PlacementAnimations.at(pos)!=null,400);
            context.runOnClient(mc->{check(PlacementAnimations.at(pos).progress()==0,"Animation starts only after ownership acknowledgment");clock.addAndGet(500_000_000L);});context.waitTicks(4);
            context.runOnClient(mc->{check(PlacementAnimations.at(pos)!=null&&PlacementAnimations.at(pos).progress()==.5,"Full animation duration survives BBE mesh handoff");check(!ready(mc,pos),"Cached BBE mesh relinquishes chest geometry ownership");check(PlacementAnimations.bbeSuppressions()>suppressed,"Actual BBE terrain geometry suppression observed");});context.takeScreenshot("review-bbe-moving-chest");
            context.runOnClient(mc->clock.addAndGet(2_000_000_000L));context.waitFor(mc->PlacementAnimations.pending()==0,400);check(true,"Completed BBE chest animation releases pending ownership");
        }finally{context.runOnClient(mc->{module.setEnabled(false);PlacementAnimations.clear(true);PlacementAnimations.timeSource(System::nanoTime);});}
        Client.INSTANCE.getLogger().info("Review BBE native checks passed ({} assertions)",checks);
    }
}
