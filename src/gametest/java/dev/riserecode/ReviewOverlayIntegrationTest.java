package dev.riserecode;

import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.event.Render3DEvent;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Screenshot;
import net.minecraft.world.phys.Vec3;

/** Pixel acceptance for native custom lines, separate from submission-count and performance evidence. */
public final class ReviewOverlayIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    public static void run(ClientGameTestContext context) {
        context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        context.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;});
        try(var world=context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -6 63 -2 6 72 10 air");
            world.getServer().runCommand("fill -6 63 -2 6 63 10 stone");
            world.getServer().runCommand("fill -5 64 4 5 71 4 stone");
            world.getServer().runCommand("tp @a 0.5 65 0.5 0 0");
            context.waitTicks(20);
            context.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();mc.gui.setScreen(null);});
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")) {
                context.runOnClient(mc->{var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(true);try{config.save();net.irisshaders.iris.Iris.reload();}catch(java.io.IOException failure){throw new AssertionError(failure);}});
                context.waitTicks(40);
                context.runOnClient(mc->check(dev.riserecode.render.ShaderWorldOverlay.active(),"Named Iris shader active for pixel checks"));
            }
            var subscription=Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class,event->{
                event.addLine(new Vec3(-2,66.4,3),new Vec3(2,66.4,3),0xFFFF1717,0xFFFF1717,4,true);
                event.addLine(new Vec3(-2,66.9,6),new Vec3(2,66.9,6),0xFF13FF47,0xFF13FF47,4,true);
                event.addLine(new Vec3(-2,66.0,6),new Vec3(2,66.0,6),0xFFFF13E7,0xFFFF13E7,4,false);
            });
            try {
                context.waitTicks(15);context.takeScreenshot("review-overlay-opaque");
                var image=new AtomicReference<NativeImage>();context.runOnClient(mc->Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image::set));
                context.waitFor(mc->image.get()!=null,200);
                try(var pixels=image.get()) {
                    int red=0,green=0,pink=0;
                    for(int y=0;y<pixels.getHeight();y++){for(int x=0;x<pixels.getWidth();x++){
                        int color=pixels.getPixel(x,y),r=color>>16&255,g=color>>8&255,b=color&255;
                        if(r>240&&g<50&&b<50){red++;}if(g>240&&r<50&&b<110){green++;}if(r>240&&b>210&&g<50){pink++;}
                    }}
                    Client.INSTANCE.getLogger().info("Overlay pixels red={}, occludedGreen={}, throughPink={}",red,green,pink);
                    check(red>20,"Visible native line preserves red color/width");check(pink>20,"Through-wall line preserves pink color/width");check(green<5,"Opaque terrain occludes depth-tested line");
                }
                world.getServer().runCommand("fill -5 64 4 5 71 4 blue_stained_glass");context.waitTicks(15);context.takeScreenshot("review-overlay-glass");
                world.getServer().runCommand("fill -5 64 4 5 71 4 water");context.waitTicks(15);context.takeScreenshot("review-overlay-water");
                world.getServer().runCommand("particle minecraft:cloud 0.5 66 3 2 1 0 0 50 force");context.waitTicks(2);context.takeScreenshot("review-overlay-particles");
                context.runOnClient(mc->mc.reloadResourcePacks());context.waitFor(mc->mc.gui.overlay()==null,1200);context.waitTicks(10);context.takeScreenshot("review-overlay-reloaded");
            } finally {subscription.close();}
        }
        Client.INSTANCE.getLogger().info("Review overlay native checks passed ({} assertions)",checks);
    }
}
