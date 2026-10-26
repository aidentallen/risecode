package dev.riserecode;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.FovEvent;
import dev.riserecode.module.modules.ZoomModule;
import dev.riserecode.util.CameraEffectsManager;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import com.mojang.blaze3d.platform.InputConstants;

/** Joining-camera regression and the real hold/release zoom path. Test mod only. */
public final class CameraFovClientTest {
    public static volatile boolean ownerOnlyShutdown;
    public static volatile int skippedEmptyShutdownTasks;
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok){throw new AssertionError(message);}}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;mc.gui.setScreen(null);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");
            c.runOnClient(mc->{
                var failures=new java.util.ArrayList<RuntimeException>();
                var bus=new EventBus((event,failure)->failures.add(failure));
                try(var camera=new CameraEffectsManager(bus,mc)){
                    for(float nativeFov:new float[]{0,-0.0F,.25F,70,180}){
                        var event=bus.post(new FovEvent(nativeFov));
                        check(Float.floatToIntBits(event.value())==Float.floatToIntBits(nativeFov),"inactive camera preserves native FOV");
                    }
                    check(failures.isEmpty(),"joining zero FOV does not fail a real camera listener");
                    var token=new Object();camera.request(token,()->3,()->true);
                    bus.post(new FovEvent(0));
                    check(failures.isEmpty(),"unready camera stays safe during zoom entry");
                    camera.release(token);camera.clear();
                    check(bus.post(new FovEvent(90)).value()==90,"release restores unmodified FOV");
                }
                check(bus.getListenerCount()==0,"camera listener ownership closes cleanly");
            });
            var zoom=c.computeOnClient(mc->Client.INSTANCE.getModuleManager().getByClass(ZoomModule.class).orElseThrow());
            c.runOnClient(mc->{zoom.getMagnification().setValue(3.0);zoom.setEnabled(true);mc.gui.setScreen(null);});c.waitTicks(8);
            c.runOnClient(mc->check(Client.INSTANCE.getCameraEffects().magnification()==1,"armed zoom preserves native camera"));
            c.getInput().holdKey(InputConstants.KEY_C);c.waitTicks(10);
            c.runOnClient(mc->{check(zoom.isZoomActive()&&Client.INSTANCE.getCameraEffects().magnification()>2.9,"hold key activates optical zoom");check(Client.INSTANCE.getEventBus().post(new FovEvent(90)).value()<31,"real zoom listener magnifies valid FOV");});
            c.takeScreenshot("0241-camera-zoom");c.getInput().releaseKey(InputConstants.KEY_C);c.waitTicks(10);
            c.runOnClient(mc->{check(Client.INSTANCE.getCameraEffects().magnification()==1,"release finishes zoom out");check(Client.INSTANCE.getEventBus().post(new FovEvent(.25F)).value()==.25F,"release preserves sub-degree native FOV");zoom.setEnabled(false);});
            world.getServer().runOnServer(server->{
                check(server.getPlayerList().getPlayers().stream().allMatch(player->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(player.nameAndId())),"shutdown cleanup has no non-owner test players");
                ownerOnlyShutdown=true;
            });
            c.waitTicks(10);
            try{Thread.sleep(60);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new AssertionError(interrupted);}
        }
        ownerOnlyShutdown=false;
        check(skippedEmptyShutdownTasks>0,"test-only adapter avoids blocking an empty owner cleanup behind Fabric phases");
        Client.INSTANCE.getLogger().info("Camera FOV native checks passed ({} assertions)",checks);
    }
}
