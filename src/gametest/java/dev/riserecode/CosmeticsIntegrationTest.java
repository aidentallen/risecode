package dev.riserecode;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.cosmetics.*;
import dev.riserecode.module.modules.CapePhysicsModule;
import dev.riserecode.ui.clickgui.ClickGui;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CosmeticsIntegrationTest {
    private static int checks;
    private CosmeticsIntegrationTest(){ }
    private static void check(boolean b,String message){checks++;if(!b){throw new AssertionError(message);}}
    private static CosmeticsManager manager(){return Client.INSTANCE.getCosmetics();}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);check(manager().outfit().items().isEmpty(),"starts unequipped");});
        c.setScreen(TitleScreen::new);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.open(mc.gui.screen());gui.selectPage(ClickGui.Page.COSMETICS);});c.waitTicks(20);
        String[] ids={"cape-constellation","wings-dragon","head-halo","tail-fox","pet-bee","trail-stars","aura-magic-ring","particles-hearts"};
        for(int i=0;i<ids.length;i++){
            String id=ids[i];var category=CosmeticCategory.values()[i];c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();editor.discard();editor.category(category);editor.select(id);});
            c.waitFor(mc->manager().request(id)!=null,300);c.waitTicks(8);c.takeScreenshot("cosmetics-"+category.name().toLowerCase(java.util.Locale.ROOT));
            c.runOnClient(mc->check(manager().outfit().items().isEmpty(),"preview never equips: "+category));
        }
        for(String id:new String[]{"pet-ghost","pet-yellowdragon"}){c.runOnClient(mc->Client.INSTANCE.getClickGui().getCosmeticsEditor().select(id));c.waitFor(mc->manager().request(id)!=null,300);c.waitTicks(8);c.takeScreenshot("cosmetics-"+id);}
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();editor.discard();for(String id:ids){editor.select(id);}editor.apply();check(manager().outfit().items().size()==8,"all slots can be equipped together");});c.waitTicks(10);c.takeScreenshot("cosmetics-combined-preview");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();var b=editor.buttons().get(2).getBounds();editor.mouseClicked(b.centerX(),b.centerY(),0);});c.waitTicks(5);c.takeScreenshot("cosmetics-settings");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();var b=editor.buttons().get(2).getBounds();editor.mouseClicked(b.centerX(),b.centerY(),0);});
        for(int i=0;i<3;i++){c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();var b=editor.previewBounds();editor.mouseClicked(b.centerX(),b.y()+8,0);editor.mouseReleased(b.centerX(),b.y()+8,0);});c.waitTicks(4);c.takeScreenshot("cosmetics-pose-"+i);}
        if(Boolean.getBoolean("riserecode.cosmeticsOnline")){
            c.runOnClient(mc->manager().library().refresh());c.waitFor(mc->!manager().library().busy(),1200);
            c.runOnClient(mc->{check(manager().library().status().equals("Catalog updated"),"live project catalog refresh");manager().library().download(manager().library().find("pet-bat"));});c.waitFor(mc->!manager().library().busy(),1200);
            c.runOnClient(mc->{check(manager().library().available(manager().library().find("pet-bat")),"live additional companion download");check(manager().outfit().items().get(CosmeticCategory.COMPANION).id().equals("pet-bee"),"download never equips");var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();editor.category(CosmeticCategory.COMPANION);var b=editor.toolbar().get(1).getBounds();editor.mouseClicked(b.centerX(),b.centerY(),0);editor.select("pet-bat");});
            c.waitFor(mc->manager().request("pet-bat")!=null,300);c.waitTicks(8);c.takeScreenshot("cosmetics-catalog-downloaded");
            c.runOnClient(mc->Client.INSTANCE.getClickGui().getCosmeticsEditor().discard());
        }
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();editor.category(CosmeticCategory.WINGS);editor.select("wings-butterfly");editor.discard();check(editor.draft().equals(manager().outfit()),"Cancel restores applied draft");
            editor.search().setFocused(true);editor.search().setValue("wings");});
        c.getInput().pressKey(InputConstants.KEY_RSHIFT);c.waitTicks(20);c.runOnClient(mc->check(!(mc.gui.screen() instanceof ClickGui),"Right Shift closes focused cosmetics search"));
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -8 63 -8 8 63 16 minecraft:stone");world.getServer().runCommand("tp @a 0.5 64 0.5 0 0");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            c.runOnClient(mc->{mc.gui.setScreen(null);mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);Client.INSTANCE.getModuleManager().getByClass(CapePhysicsModule.class).orElseThrow().setEnabled(true);});c.waitTicks(20);
            c.runOnClient(mc->{
                var state=new AvatarRenderState();mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).extractRenderState(mc.player,state,.5F);
                check(((CosmeticRenderState)state).risecode$outfit()!=null,"local render extraction receives immutable outfit");check(state.skin.cape()!=null,"custom cape works without account cape");
                check(((dev.riserecode.render.LegacyCapeState)state).risecode$legacyCape()!=null,"custom cape integrates with legacy physics");
                var remote=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),"Untouched"));remote.setId(262189);remote.setPos(2,64,0);var other=new AvatarRenderState();mc.getEntityRenderDispatcher().getPlayerRenderer(remote).extractRenderState(remote,other,.5F);
                check(((CosmeticRenderState)other).risecode$outfit()==null,"remote player stays untouched");
            });c.takeScreenshot("cosmetics-world-standing");
            c.getInput().holdKey(InputConstants.KEY_W);c.waitTicks(10);c.takeScreenshot("cosmetics-world-moving");c.getInput().releaseKey(InputConstants.KEY_W);
            c.runOnClient(mc->check(manager().motion().trail().size()>2,"moving player records a cosmetic trail"));
            c.runOnClient(mc->mc.player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE)));c.waitTicks(4);c.takeScreenshot("cosmetics-world-armor");
            c.getInput().holdKey(InputConstants.KEY_LSHIFT);c.waitTicks(6);c.runOnClient(mc->check(mc.player.isCrouching(),"real crouch pose"));c.takeScreenshot("cosmetics-world-crouch");c.getInput().releaseKey(InputConstants.KEY_LSHIFT);
            c.runOnClient(mc->{mc.player.setShiftKeyDown(false);mc.player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));});c.waitTicks(6);c.takeScreenshot("cosmetics-world-elytra");
            c.runOnClient(mc->{mc.player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);mc.player.setInvisible(true);var s=new AvatarRenderState();mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).extractRenderState(mc.player,s,.5F);check(((CosmeticRenderState)s).risecode$outfit()==null,"invisibility hides cosmetics");mc.player.setInvisible(false);
                var saved=Client.INSTANCE.getConfigManager().profileSnapshot();manager().apply(CosmeticLoadout.empty());Client.INSTANCE.getConfigManager().applyProfile(saved);check(manager().outfit().items().size()==8,"native profile restores outfit");
                manager().resourceReload();check(manager().loadedAssets()==0,"reload releases GPU cache");});
            var reload=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reload.isDone()&&mc.gui.overlay()==null,1200);
            c.runOnClient(mc->check(!reload.isCompletedExceptionally(),"actual resource reload succeeds"));
            c.waitFor(mc->manager().request("pet-bee")!=null&&manager().request("cape-constellation")!=null,300);c.waitTicks(5);c.takeScreenshot("cosmetics-world-reloaded");
            c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);manager().apply(CosmeticLoadout.empty());Client.INSTANCE.getModuleManager().getByClass(CapePhysicsModule.class).orElseThrow().setEnabled(false);});
        }
        c.waitTicks(4);c.runOnClient(mc->check(manager().motion().trail().isEmpty(),"disconnect clears motion history"));
        Client.INSTANCE.getLogger().info("Cosmetics native checks passed ({} assertions).",checks);
    }
}
