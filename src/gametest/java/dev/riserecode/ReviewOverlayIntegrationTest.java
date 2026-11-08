package dev.riserecode;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.riserecode.event.Render3DEvent;
import java.util.*;
import java.util.concurrent.atomic.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Screenshot;
import net.minecraft.world.phys.Vec3;

/** Fixed-camera color/depth/width/transparency/scaling oracles, independent of submission counts. */
public final class ReviewOverlayIntegrationTest {
    private static int checks;
    public static long shadowPasses;
    private record Pixels(int red,int green,int pink){ }
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static Pixels pixels(ClientGameTestContext context){
        var image=new AtomicReference<NativeImage>();context.runOnClient(mc->Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image::set));context.waitFor(mc->image.get()!=null,200);
        try(var value=image.get()){
            int red=0,green=0,pink=0;
            for(int y=0;y<value.getHeight();y++){for(int x=0;x<value.getWidth();x++){
                int color=value.getPixel(x,y),r=color>>16&255,g=color>>8&255,b=color&255;
                if(r>220&&g<70&&b<70){red++;}if(g>220&&r<70&&b<130){green++;}if(r>220&&b>190&&g<70){pink++;}
            }}return new Pixels(red,green,pink);
        }
    }
    private static Pixels oracle(ClientGameTestContext c,String scene,boolean opaque){
        c.waitTicks(8);c.takeScreenshot("review-overlay-"+scene);var pixels=pixels(c);
        check(pixels.red()>20&&pixels.pink()>20,"Visible and through-wall line colors present: "+scene);
        check(opaque?pixels.green()<5:pixels.green()>20,"Depth-tested line obeys opaque/transparent scene: "+scene+" "+pixels);
        Client.INSTANCE.getLogger().info("Overlay oracle {} {}",scene,pixels);return pixels;
    }
    private static void targets(Set<GpuTexture> seen){
        for(var owner:List.of(dev.riserecode.render.ShaderWorldOverlay.class,dev.riserecode.render.WorldResolution.class)){
            for(String name:List.of("overlay","world","easu")){
                try{var field=owner.getDeclaredField(name);field.setAccessible(true);if(field.get(null) instanceof RenderTarget target){if(target.getColorTexture()!=null){seen.add(target.getColorTexture());}if(target.getDepthTexture()!=null){seen.add(target.getDepthTexture());}}}
                catch(NoSuchFieldException absent){ /* Different explicit owners have different target names. */ }
                catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            }
        }
    }
    public static void run(ClientGameTestContext context){
        context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        context.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;});
        var width=new AtomicInteger(4);var seen=Collections.newSetFromMap(new IdentityHashMap<GpuTexture,Boolean>());
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -6 63 -2 6 72 10 air");world.getServer().runCommand("fill -6 63 -2 6 63 10 stone");world.getServer().runCommand("tp @a 0.5 65 0.5 0 0");context.waitTicks(20);
            context.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();mc.gui.setScreen(null);Client.INSTANCE.getPerformance().upscaler.setValue(dev.riserecode.performance.PerformancePreferences.Upscaler.BILINEAR);});
            boolean iris=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris");
            if(iris){
                context.runOnClient(mc->{var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(true);try{config.save();net.irisshaders.iris.Iris.reload();}catch(java.io.IOException failure){throw new AssertionError(failure);}});
                context.waitTicks(40);context.runOnClient(mc->check(dev.riserecode.render.ShaderWorldOverlay.active(),"Named pinned Iris shader active"));
            }
            var subscription=Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class,event->{
                event.addLine(new Vec3(-2,66.4,3),new Vec3(2,66.4,3),0xFFFF1717,0xFFFF1717,width.get(),true);
                event.addLine(new Vec3(-2,66.9,6),new Vec3(2,66.9,6),0xFF13FF47,0xFF13FF47,width.get(),true);
                event.addLine(new Vec3(-2,66.0,6),new Vec3(2,66.0,6),0xFFFF13E7,0xFFFF13E7,width.get(),false);
            });
            try{
                for(int scale:new int[]{100,75}){
                    context.runOnClient(mc->Client.INSTANCE.getPerformance().worldResolution.setValue((double)scale));context.waitTicks(10);
                    context.runOnClient(mc->check(Math.abs(dev.riserecode.render.WorldResolution.status().actual().pixelScale()-scale/100.0)<.01,"Actual world resolution matches requested test scale "+scale));
                    int previousArea=0;
                    for(int lineWidth:new int[]{1,4,32}){
                        width.set(lineWidth);String label="scale-"+scale+"-width-"+lineWidth;
                        world.getServer().runCommand("fill -5 64 4 5 71 4 stone");
                        var opaque=oracle(context,label+"-opaque",true);
                        check(opaque.red()>previousArea*1.25,"Line coverage grows monotonically with supported width: "+label);previousArea=opaque.red();
                        world.getServer().runCommand("fill -5 64 4 5 71 4 blue_stained_glass");oracle(context,label+"-glass",false);
                        world.getServer().runCommand("fill -5 64 4 5 71 4 water");oracle(context,label+"-water",false);
                        world.getServer().runCommand("particle minecraft:cloud 0.5 66 3 2 1 0 0 50 force");oracle(context,label+"-particles",false);
                    }
                }
                context.runOnClient(mc->targets(seen));long owned=dev.riserecode.render.WorldResolution.ownedGpuBytes();
                for(int reload=0;reload<5;reload++){
                    var future=context.computeOnClient(mc->mc.reloadResourcePacks());context.waitFor(mc->future.isDone()&&mc.gui.overlay()==null,1200);future.join();
                    oracle(context,"reloaded-"+reload,false);
                    context.runOnClient(mc->{targets(seen);check(seen.stream().filter(texture->!texture.isClosed()).count()<=4,"Only current overlay/scaled-world target textures remain live after reload");check(dev.riserecode.render.WorldResolution.ownedGpuBytes()==owned,"Owned world-target byte count remains stable across reloads");});
                }
                if(iris){context.runOnClient(mc->check(shadowPasses>0,"Pinned pack actually invokes native shadow rendering"));}
            }finally{subscription.close();}
        }
        context.waitTicks(3);
        context.runOnClient(mc->{check(dev.riserecode.render.WorldResolution.ownedGpuBytes()==0,"Disconnect releases scaled world targets");check(seen.stream().allMatch(GpuTexture::isClosed),"Disconnect releases all observed overlay/scaling textures");Client.INSTANCE.getPerformance().worldResolution.setValue(100.0);});
        Client.INSTANCE.getLogger().info("Review overlay native checks passed ({} assertions)",checks);
    }
}
