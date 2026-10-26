package dev.riserecode;

import dev.riserecode.performance.*;
import dev.riserecode.performance.PerformancePreferences.Mode;
import dev.riserecode.module.modules.NametagsModule;
import dev.riserecode.ui.clickgui.ClickGui;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Candidate-only native checks. Baseline uses exactly the same measurement harness. */
public final class PerformanceClientChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok){throw new AssertionError(message);}}
    public static void ui(ClientGameTestContext c){
        var previous=c.computeOnClient(mc->Client.INSTANCE.getCosmetics().snapshot());
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.open(null);gui.getSettingsPopup().toggle();});c.waitTicks(4);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var popup=gui.getSettingsPopup();var logical=popup.popupBounds();var shown=popup.displayedPopupBounds();var row=popup.rowBounds(7);double x=shown.x()+(row.centerX()-logical.x())*shown.width()/logical.width(),y=shown.y()+(row.centerY()-logical.y())*shown.height()/logical.height();popup.mouseClicked(x,y,0);check(gui.getPage()==ClickGui.Page.PERFORMANCE,"transformed footer performance row opens in the existing window");});c.waitTicks(5);c.takeScreenshot("024-performance-controls");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getClientSettingsEditor().editor();Client.INSTANCE.getClickGui().keyPressed(new KeyEvent(258,0,0));check("Performance Mode".equals(editor.keyboardSetting()),"Tab focuses mode");Client.INSTANCE.getClickGui().keyPressed(new KeyEvent(262,0,0));check(Client.INSTANCE.getPerformance().mode.getValue()==Mode.CONSERVATIVE,"Right arrow changes preset");Client.INSTANCE.getClickGui().keyPressed(new KeyEvent(264,0,0));Client.INSTANCE.getClickGui().keyPressed(new KeyEvent(32,0,0));check(Client.INSTANCE.getPerformance().mode.getValue()==Mode.CUSTOM&&!Client.INSTANCE.getPerformance().effective().blur(),"keyboard edit copies preset to Custom");});
        for(var mode:Mode.values()){
            c.runOnClient(mc->Client.INSTANCE.getPerformance().mode.setValue(mode));c.waitTicks(3);
            c.runOnClient(mc->{check(PerformancePolicy.frame().equals(Client.INSTANCE.getPerformance().effective()),"extraction captures "+mode);check(Client.INSTANCE.getCosmetics().snapshot().equals(previous),"mode preserves equipped cosmetics");});c.takeScreenshot("024-performance-"+mode.name().toLowerCase(java.util.Locale.ROOT));
        }
        c.runOnClient(mc->{Client.INSTANCE.getPerformance().mode.setValue(Mode.OFF);Client.INSTANCE.getClickGui().onClose();mc.gui.setScreen(null);});
    }
    public static void effects(ClientGameTestContext c,String scene){
        var profile=c.computeOnClient(mc->Client.INSTANCE.getConfigManager().profileSnapshot());boolean recording=c.computeOnClient(mc->Client.INSTANCE.getMaps().recording());
        for(var mode:Mode.values()){
            c.runOnClient(mc->Client.INSTANCE.getPerformance().mode.setValue(mode));c.waitTicks(4);c.takeScreenshot("024-"+scene+"-"+mode.name().toLowerCase(java.util.Locale.ROOT));
            c.runOnClient(mc->{check(Client.INSTANCE.getConfigManager().profileSnapshot().equals(profile),"preset preserves complete profile in "+scene);check(Client.INSTANCE.getMaps().recording()==recording,"preset preserves map recording choice");});
        }
        c.runOnClient(mc->Client.INSTANCE.getPerformance().mode.setValue(Mode.OFF));
    }
    public static void equipment(ClientGameTestContext c){
        var nametags=c.computeOnClient(mc->Client.INSTANCE.getModuleManager().getByClass(NametagsModule.class).orElseThrow());
        long before=c.computeOnClient(mc->nametags.equipmentBuilds());c.waitTicks(10);c.runOnClient(mc->check(nametags.equipmentBuilds()==before,"unchanged player equipment reuses immutable snapshots"));
        c.runOnClient(mc->((net.minecraft.world.entity.LivingEntity)mc.level.getEntity(-1000)).setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.GOLDEN_AXE)));c.waitTicks(2);c.runOnClient(mc->check(nametags.equipmentBuilds()==before+1,"one equipment change rebuilds one snapshot"));
        c.runOnClient(mc->Client.INSTANCE.getEventBus().post(new dev.riserecode.event.RenderResourcesReloadEvent()));c.waitTicks(2);c.runOnClient(mc->check(nametags.equipmentBuilds()>=before+25,"resource generation invalidates equipment snapshots"));
        c.runOnClient(mc->((net.minecraft.world.entity.LivingEntity)mc.level.getEntity(-1000)).setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CLOCK)));c.waitTicks(2);
        long clock=c.computeOnClient(mc->nametags.equipmentBuilds());c.waitTicks(3);c.runOnClient(mc->check(nametags.equipmentBuilds()>clock,"dynamic clock appearance continues refreshing at tick cadence"));
        c.runOnClient(mc->((net.minecraft.world.entity.LivingEntity)mc.level.getEntity(-1000)).setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD)));c.waitTicks(2);
    }
    public static void assets(ClientGameTestContext c){
        c.runOnClient(mc->{
            var manager=Client.INSTANCE.getCosmetics();manager.apply(dev.riserecode.cosmetics.CosmeticLoadout.empty());manager.resourceReload();
            var finished=new java.util.concurrent.CountDownLatch(1);manager.request("pet-bee");manager.library().work(()->{finished.countDown();return null;});
            try{check(finished.await(5,java.util.concurrent.TimeUnit.SECONDS),"asset worker reaches queued completion");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
            check(manager.statistics().pending()>0,"decoded completion awaits client executor");manager.resourceReload();
        });c.waitTicks(2);
        c.runOnClient(mc->check(Client.INSTANCE.getCosmetics().loadedAssets()==0&&Client.INSTANCE.getCosmetics().statistics().pending()==0,"reload rejects late decoded assets and releases pending ownership"));
        c.runOnClient(mc->Client.INSTANCE.getCosmetics().request("pet-bee"));c.waitFor(mc->Client.INSTANCE.getCosmetics().loadedAssets()==1,600);
        c.runOnClient(mc->{var manager=Client.INSTANCE.getCosmetics();var asset=manager.request("pet-bee");check(asset!=null&&manager.statistics().ownedGpuBytes()>0&&manager.statistics().nativeImageBytes()>0&&manager.statistics().decodedCpuBytes()>0,"CPU, native and GPU cosmetic ownership are separate");
            // Inject accounting weights only: keep the real bee texture IDs, allocate no oversized images.
            try{var field=manager.getClass().getDeclaredField("assets");field.setAccessible(true);@SuppressWarnings("unchecked") var cache=(java.util.Map<String,dev.riserecode.cosmetics.CosmeticsManager.Asset>)field.get(manager);
                cache.put("pet-bee",new dev.riserecode.cosmetics.CosmeticsManager.Asset(asset.model(),asset.textures(),asset.sha256(),64L*1024*1024,64L*1024*1024));
                cache.put("unused-fixture-a",new dev.riserecode.cosmetics.CosmeticsManager.Asset(null,java.util.List.of(),"fixture",1024*1024,1024*1024));
                cache.put("unused-fixture-b",new dev.riserecode.cosmetics.CosmeticsManager.Asset(null,java.util.List.of(),"fixture",1024*1024,1024*1024));
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}
            manager.beginFrame();check(manager.loadedAssets()==1&&manager.statistics().activeEntries()>0&&manager.statistics().workingSetFloorBytes()>0&&manager.statistics().byteOverage()>0,"oversized active preview survives between extractions, evicts unused assets first and reports its floor overage");manager.request("pet-bee");manager.resourceReload();check(manager.statistics().entries()==1,"extraction-pinned cosmetic survives reload retirement");});c.waitTicks(2);
        c.runOnClient(mc->check(Client.INSTANCE.getCosmetics().statistics().entries()==0,"next extraction releases retired cosmetic"));
        c.runOnClient(mc->{
            var maps=Client.INSTANCE.getMaps();var textures=maps.textures();textures.beginFrame();var palette=java.util.List.of(dev.riserecode.map.MapColors.encode(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));var rgb=new int[256];java.util.Arrays.fill(rgb,-1);
            var tile=new dev.riserecode.map.SurfaceTile(10000,10000,palette,new short[256],new short[256],rgb,new byte[256],rgb);
            var key=new dev.riserecode.map.MapService.Key(maps.dimension(),10000,10000);var id=textures.texture(key,tile,1);check(id!=null&&textures.ownedSize()>0,"native map upload is tracked");long bytes=textures.bytes();textures.remove(key);check(textures.bytes()==bytes&&textures.ownedSize()>0,"map invalidation retains extraction-pinned GPU texture");textures.beginFrame();check(textures.bytes()==0&&textures.ownedSize()==0,"safe extraction boundary releases retired map texture");
        });
    }
    public static void independentTrails(ClientGameTestContext c){
        c.runOnClient(mc->Client.INSTANCE.getPerformance().mode.setValue(Mode.STRONG));c.waitTicks(10);
        c.runOnClient(mc->{var motion=Client.INSTANCE.getCosmetics().motion();check(motion.sample(.5F)>0,"trail suppression preserves the tail motion sampler");check(!motion.trail().isEmpty(),"trail history remains available for exact Off restoration");Client.INSTANCE.getPerformance().mode.setValue(Mode.OFF);});
    }
    public static void idleTargets(ClientGameTestContext c){
        c.waitTicks(45);c.runOnClient(mc->{check(dev.riserecode.render.SilhouetteOutlineRenderer.maskTargets().isEmpty(),"unrequested silhouette targets retire after forty ticks");check(dev.riserecode.render.ItemOutlineRenderer.getMaskTarget()==null,"unrequested item targets retire after forty ticks");});
        Client.INSTANCE.getLogger().info("Performance native checks passed ({} assertions)",checks);
    }
}
