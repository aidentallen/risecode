package dev.riserecode;

import dev.riserecode.map.MapLayerPolicy;
import dev.riserecode.map.WorldMapScreen;
import dev.riserecode.module.modules.WorldMapModule;
import dev.riserecode.module.modules.hud.MinimapModule;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Minimap and world map in a real world: radar heads, layers and the context menu, with screenshots to inspect. */
public final class MapIntegrationTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok){throw new AssertionError(message);}}
    private static void shot(ClientGameTestContext c,String name){c.waitTicks(12);c.takeScreenshot("map-"+name);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        var modules=Client.INSTANCE.getModuleManager();
        var worldMap=modules.getByClass(WorldMapModule.class).orElseThrow();
        var minimap=modules.getByClass(MinimapModule.class).orElseThrow();
        check(worldMap.isEnabled(),"the world map works without being switched on first");
        check(MapLayerPolicy.band(63)==48&&MapLayerPolicy.band(64)==64&&MapLayerPolicy.band(-1)==-16,"altitudes share 16 block bands");
        c.runOnClient(mc->{mc.options.pauseOnLostFocus=false;mc.gui.setScreen(null);minimap.setEnabled(true);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            var server=world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set noon");
            server.runCommand("tp @a 0 ~ 0 0 0");
            server.runCommand("summon zombie 6 -60 0 {NoAI:1b}");
            server.runCommand("summon cow -6 -60 2 {NoAI:1b}");
            server.runCommand("summon creeper 0 -60 8 {NoAI:1b}");
            server.runCommand("summon pig 3 -60 -7 {NoAI:1b}");
            server.runCommand("summon chicken -4 -60 -5 {NoAI:1b}");
            server.runCommand("summon skeleton 9 -60 6 {NoAI:1b}");
            server.runCommand("summon villager -9 -60 -3 {NoAI:1b}");
            server.runCommand("summon zombie 2 -40 2 {NoAI:1b}");
            // a room to look at on the layer strip
            server.runCommand("fill -12 40 -12 12 46 12 minecraft:stone");
            server.runCommand("fill -11 41 -11 11 45 11 minecraft:air");
            server.runCommand("fill -11 40 -11 11 40 11 minecraft:diamond_block");
            c.waitTicks(30);
            c.runOnClient(mc->{
                var maps=Client.INSTANCE.getMaps();
                var a=maps.waypoints().add("Base",20,70,-20,maps.dimension());
                maps.waypoints().add("Mine",-500,30,300,maps.dimension());
                maps.waypoints().put(a.tracked(true));
                minimap.size.setValue(150.0);minimap.zoom.setValue(5.0);
                minimap.direction.setValue(true);minimap.time.setValue(true);minimap.light.setValue(true);
            });
            c.waitTicks(240);
            shot(c,"minimap-square");
            c.runOnClient(mc->check(minimap.graphicHeight()>minimap.graphicWidth(),"info lines reserve space below the map"));
            c.runOnClient(mc->{minimap.shape.setValue(MinimapModule.Shape.CIRCLE);minimap.chunkGrid.setValue(true);minimap.slimeChunks.setValue(true);});
            shot(c,"minimap-circle");
            c.runOnClient(mc->{minimap.shape.setValue(MinimapModule.Shape.SQUARE);minimap.chunkGrid.setValue(false);minimap.slimeChunks.setValue(false);minimap.rotate.setValue(false);});c.getInput().holdKey(86);
            shot(c,"minimap-enlarged");c.runOnClient(mc->check(minimap.graphicWidth()>200,"holding the enlarge key grows the minimap"));c.getInput().releaseKey(86);c.waitTicks(4);
            c.runOnClient(mc->{minimap.setEnabled(false);worldMap.entities.setValue(true);worldMap.defaultZoom.setValue(4.0);worldMap.rememberZoom.setValue(false);mc.gui.setScreen(new WorldMapScreen(null,Client.INSTANCE.getMaps()));});
            c.waitTicks(40);
            shot(c,"world-close");
            c.getInput().setCursorPos(700,420);c.getInput().pressMouse(1);
            shot(c,"world-context");
            c.getInput().pressKey(256);c.waitTicks(4);
            c.getInput().pressKey(76);c.getInput().pressKey(71);
            shot(c,"world-sidebar");
            c.getInput().pressKey(76);c.getInput().pressKey(71);c.waitTicks(4);
            c.runOnClient(mc->check(mc.gui.screen() instanceof WorldMapScreen,"the map stays open while using its keys"));
            for(int i=0;i<9;i++){c.getInput().pressKey(267);}
            c.waitTicks(30);
            shot(c,"world-far");
            c.runOnClient(mc->{mc.gui.setScreen(null);});
            c.waitTicks(4);
            server.runCommand("tp @a 0 42 0");
            c.waitTicks(200);
            shot(c,"underground-hud");
            c.runOnClient(mc->{minimap.setEnabled(true);minimap.zoom.setValue(5.0);mc.gui.setScreen(new WorldMapScreen(null,Client.INSTANCE.getMaps()));});
            c.waitTicks(60);
            shot(c,"world-cave-auto");
            c.runOnClient(mc->{var maps=Client.INSTANCE.getMaps();check(maps.tile(maps.key(maps.dimension(),0,0,0,MapLayerPolicy.Mode.CAVE,41))!=null,"the cave band you stand in is captured");});
            c.runOnClient(mc->{var maps=Client.INSTANCE.getMaps();check(maps.key(maps.dimension(),0,0,0,MapLayerPolicy.Mode.CAVE,41).altitude()==32,"a manual altitude resolves to its band");mc.gui.setScreen(null);});
        }
        Client.INSTANCE.getLogger().info("Map integration checks passed ({} assertions)",checks);
    }
}
