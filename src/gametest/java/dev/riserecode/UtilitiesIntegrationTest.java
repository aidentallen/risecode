package dev.riserecode;

import dev.riserecode.event.*;
import dev.riserecode.map.*;
import dev.riserecode.module.modules.*;
import dev.riserecode.module.modules.hud.MinimapModule;
import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.RenderUtil;
import dev.riserecode.util.gameplay.TrajectoryPrediction;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;

/** Isolated real-client integration: native hooks, actual local font, surface persistence and gameplay utilities. */
public final class UtilitiesIntegrationTest {
    private static int checks;
    private UtilitiesIntegrationTest(){ }
    @SuppressWarnings("unchecked") private static void selectMinecraftia(ThemeManager themes){((EnumSetting<ThemeManager.Typeface>)themes.getSetting("Font Selection")).setValue(ThemeManager.Typeface.MINECRAFTIA);}
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc -> { mc.options.enableVsync().set(false); mc.options.framerateLimit().set(120); });
        c.setScreen(TitleScreen::new);c.getInput().pressKey(InputConstants.KEY_RSHIFT);c.waitFor(mc->Client.INSTANCE.getClickGui().isOpen(),240);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(ClickGui.NAVIGATION_PAGES.contains(ClickGui.Page.FRIENDS)&&ClickGui.NAVIGATION_PAGES.contains(ClickGui.Page.PROFILES),"Utility pages included in expanded navigation");gui.selectPage(ClickGui.Page.FRIENDS);check(gui.getActivePanel()==null&&gui.getSidebar().isExpanded(),"Friends destination expands utilities");gui.selectPage(ClickGui.Page.PROFILES);check(gui.getSidebar().getSelected()==ClickGui.NAVIGATION_PAGES.indexOf(ClickGui.Page.PROFILES),"Profiles navigation retained");gui.selectPage(ClickGui.Page.SETTINGS);check(!gui.getSidebar().isExpanded(),"Legacy settings action toggles inline utilities");});
        c.waitTicks(10);c.takeScreenshot("utilities-settings-hub");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE));c.waitTicks(8);
        for(double scale:new double[]{1,1.25}){
            c.runOnClient(mc->{var themes=Client.INSTANCE.getThemeManager();selectMinecraftia(themes);((DoubleSetting)themes.getSetting("Font Scale")).setValue(scale);themes.changed();CustomTextRenderer.close();
                check(CustomTextRenderer.available(ThemeManager.Typeface.MINECRAFTIA)&&CustomTextRenderer.isPixelFont(),"Actual Minecraftia TTF loaded");
                check(RenderUtil.textWidth(mc.font,"HH",false)<=18*scale&&RenderUtil.textWidth(mc.font,"HH",false)>=8*scale,"Minecraftia visible metrics normalized");
                var measure=CustomTextRenderer.measure(mc.font,false);double width=0;for(int code:"AVATAR Ω 中 🐸".codePoints().toArray()){width=measure.append(code);}check(width==RenderUtil.textWidth(mc.font,"AVATAR Ω 中 🐸",false),"Unicode fallback measurement agrees with drawing layout");
            });c.waitTicks(8);c.takeScreenshot("utilities-minecraftia-"+(scale==1?"integer":"fractional"));
        }
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -32 62 -32 32 63 32 stone");world.getServer().runCommand("fill -12 64 6 12 64 14 water");world.getServer().runCommand("tp @a 0.5 70 0.5 0 20");
            c.runOnClient(mc->{mc.gui.setScreen(null);Client.INSTANCE.getModuleManager().disableAll();mc.options.renderDistance().set(4);});c.waitFor(mc->Client.INSTANCE.getMaps().ready(),600);c.waitFor(mc->Client.INSTANCE.getMaps().statistics().captured()>=4,600);
            c.waitFor(mc->{var tile=Client.INSTANCE.getMaps().tile(new MapService.Key(Client.INSTANCE.getMaps().dimension(),0,0));if(tile==null){return false;}int color=tile.colors()[8*16];return (color&255)>(color>>16&255);},600);
            c.runOnClient(mc->{var maps=Client.INSTANCE.getMaps();check(maps.recording(),"Map recording defaults on independently of overlays");var p=maps.addAtPlayer();check(p!=null&&maps.waypoints().all().contains(p),"Waypoint at player");maps.waypoints().put(p.tracked(true));maps.waypoints().death(-10,64,-10,maps.dimension());maps.waypoints().death(-11,64,-11,maps.dimension());check(maps.waypoints().all().stream().filter(WaypointStore.Waypoint::death).count()==2,"Repeated deaths retained");
                var snapshot=Client.INSTANCE.getConfigManager().profileSnapshot();check(!snapshot.has("waypoints")&&!snapshot.has("maps"),"Maps and destinations excluded from module profile");
                var stats=maps.statistics();check(stats.cpuTiles()<=512&&stats.pending()<=16&&stats.queued()<=16&&stats.gpuTiles()<=512,"Map work and residency bounded");
                });c.getInput().pressKey(InputConstants.KEY_M);c.waitForScreen(WorldMapScreen.class);check(c.computeOnClient(mc->mc.gui.screen() instanceof WorldMapScreen),"Native M shortcut opens map");c.waitTicks(60);c.takeScreenshot("utilities-world-map");
            c.runOnClient(mc->{Client.INSTANCE.getMaps().resourceReload();});c.waitTicks(10);c.takeScreenshot("utilities-map-recolor");
            c.runOnClient(mc->{var map=(WorldMapScreen)mc.gui.screen();map.mouseScrolled(150,140,0,-15);});c.waitTicks(15);c.takeScreenshot("utilities-world-map-overview");
            c.runOnClient(mc->mc.gui.setScreen(new WaypointScreen(null,Client.INSTANCE.getMaps())));c.waitTicks(8);c.takeScreenshot("utilities-waypoint-manager");
            c.runOnClient(mc->mc.gui.setScreen(null));c.getInput().pressKey(InputConstants.KEY_B);c.waitForScreen(WaypointEditScreen.class);check(c.computeOnClient(mc->mc.gui.screen() instanceof WaypointEditScreen),"Native B shortcut adds waypoint and opens editor");c.takeScreenshot("utilities-waypoint-editor");c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitFor(mc->mc.gui.screen()==null,240);
            c.runOnClient(mc->{mc.gui.setScreen(null);var modules=Client.INSTANCE.getModuleManager();for(String name:List.of("Nametags","Trajectories","AutoTool","FastBreak","BetterChat","BetterTab","Waypoints","Minimap")){var module=modules.getByName(name).orElseThrow();check(!module.isEnabled(),name+" defaults off");int listeners=Client.INSTANCE.getEventBus().getListenerCount();module.setEnabled(true);module.setEnabled(false);check(Client.INSTANCE.getEventBus().getListenerCount()==listeners,name+" releases subscriptions");module.setEnabled(true);}
                var fast=modules.getByClass(FastBreakModule.class).orElseThrow();var event=Client.INSTANCE.getEventBus().post(new MiningEvent(MiningEvent.Phase.CONTINUE,new BlockPos(0,63,0)));check(event.noDelay()&&event.multiplier()==1,"FastBreak safe defaults");fast.multiplier.setValue(2.0);event=Client.INSTANCE.getEventBus().post(new MiningEvent(MiningEvent.Phase.CONTINUE,new BlockPos(0,63,0)));check(event.multiplier()==2,"FastBreak adjustable progress");fast.setEnabled(false);
                check(AutoToolModule.score(new ItemStack(Items.DIAMOND_PICKAXE),Blocks.STONE.defaultBlockState())>AutoToolModule.score(new ItemStack(Items.WOODEN_PICKAXE),Blocks.STONE.defaultBlockState()),"Native tool speed ranking");check(AutoToolModule.score(new ItemStack(Items.DIAMOND_AXE),Blocks.DIAMOND_ORE.defaultBlockState())<0,"Correct-drop rule rejects wrong tools");
                var original=Component.literal("hello "+mc.player.getGameProfile().name()).withStyle(s->s.withColor(0x45AABB).withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create("https://example.com"))).withHoverEvent(new HoverEvent.ShowText(Component.literal("hover"))));
                var chat=Client.INSTANCE.getEventBus().post(new ChatDisplayEvent(new Object(),original,null));check(original.getString().equals("hello "+mc.player.getGameProfile().name())&&chat.display().getString().contains("[⧉]"),"Chat display decoration preserves original");
                check(chat.display().getSiblings().stream().anyMatch(component->component.getString().contains("hello")),"Formatted chat retained");mc.gui.hud.getChat().addClientSystemMessage(original);mc.gui.hud.getChat().addClientSystemMessage(original);
                var signature=new MessageSignature(new byte[MessageSignature.BYTES]);var tag=net.minecraft.client.multiplayer.chat.GuiMessageTag.chatNotSecure();mc.gui.hud.getChat().addPlayerMessage(original,signature,tag);
                var stored=((dev.riserecode.testmixin.ChatStateAccessor)(Object)mc.gui.hud.getChat().storeState()).riserecode$getMessages().getFirst();check(stored.signature()==signature&&stored.tag()==tag&&stored.content().equals(original),"Original signed component, signature and trust tag retained");check(Client.INSTANCE.getEventBus().post(new ChatDisplayEvent.History()).limit()==500,"Bounded configurable chat history");
                mc.player.getInventory().setSelectedItem(new ItemStack(Items.BOW));
                modules.getByClass(MinimapModule.class).orElseThrow().rotate.setValue(false);
            });
            c.runOnClient(mc->{var remote=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MapFriend"));remote.setId(900001);remote.setPos(0.5,64,7.5);remote.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));remote.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));remote.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));mc.level.addEntity(remote);Client.INSTANCE.getFriendManager().add("MapFriend");check(Client.INSTANCE.getEventBus().post(new LabelRenderEvent(remote)).isCancelled(),"Nametags replaces corresponding vanilla player label");check(mc.getEntityRenderDispatcher().extractEntity(remote,0).nameTag==null,"Native player label actually suppressed");});
            c.waitTicks(50);c.takeScreenshot("utilities-overlays");
            c.runOnClient(mc->{
                for(EntityType<?> type:List.of(EntityTypes.ARROW,EntityTypes.TRIDENT,EntityTypes.SNOWBALL,EntityTypes.EGG,EntityTypes.ENDER_PEARL,EntityTypes.EXPERIENCE_BOTTLE,EntityTypes.SPLASH_POTION,EntityTypes.LINGERING_POTION,EntityTypes.WIND_CHARGE)){
                    Projectile p=(Projectile)type.create(mc.level,EntitySpawnReason.COMMAND);if(p==null){throw new AssertionError();}p.setPos(0.5,100,0.5);p.setDeltaMovement(new Vec3(0.2,0.4,0.7));var predicted=TrajectoryPrediction.predict(mc.level,p,10);for(int tick=1;tick<predicted.points().size();tick++){p.tick();check(p.position().distanceTo(predicted.points().get(tick))<0.025,"Native projectile flight "+type+" tick "+tick);}
                }
                for(EntityType<?> type:List.of(EntityTypes.ARROW,EntityTypes.TRIDENT,EntityTypes.SNOWBALL,EntityTypes.EXPERIENCE_BOTTLE,EntityTypes.SPLASH_POTION)){
                    Projectile p=(Projectile)type.create(mc.level,EntitySpawnReason.COMMAND);if(p==null){throw new AssertionError();}p.setPos(0.5,64.45,8.5);p.setDeltaMovement(new Vec3(0.05,0.02,0.05));p.tick();
                    var predicted=TrajectoryPrediction.predict(mc.level,p,5);for(int tick=1;tick<predicted.points().size();tick++){p.tick();check(p.position().distanceTo(predicted.points().get(tick))<0.12,"Native fluid drag and impact "+type+" step "+tick+" actual "+p.position()+" predicted "+predicted.points().get(tick));}
                }
                Projectile impactProjectile=(Projectile)EntityTypes.SNOWBALL.create(mc.level,EntitySpawnReason.COMMAND);if(impactProjectile==null){throw new AssertionError();}impactProjectile.setOwner(mc.player);impactProjectile.setPos(0.5,65,5);impactProjectile.setDeltaMovement(new Vec3(0,0,1.5));var entityPath=TrajectoryPrediction.predict(mc.level,impactProjectile,8);check(entityPath.impact()==net.minecraft.world.phys.HitResult.Type.ENTITY,"Loaded entity collision terminates prediction");for(int tick=1;tick<entityPath.points().size();tick++){impactProjectile.tick();check(impactProjectile.position().distanceTo(entityPath.points().get(tick))<0.12,"Native entity impact agrees with prediction at "+tick+": "+impactProjectile.position()+" vs "+entityPath.points().get(tick));}
                var trajectory=Client.INSTANCE.getModuleManager().getByClass(TrajectoriesModule.class).orElseThrow();check(!trajectory.launch(new ItemStack(Items.SNOWBALL)).isEmpty(),"Held throwable preview");
                var bow=trajectory.launch(new ItemStack(Items.BOW));check(!bow.isEmpty()&&bow.getFirst().getDeltaMovement().length()>2.8,"Charged bow launch velocity");
            });
            c.runOnClient(mc->{
                var list=((dev.riserecode.testmixin.TabStateAccessor)mc.getConnection()).risecode$listed();
                for(int i=0;i<120;i++){var info=new net.minecraft.client.multiplayer.PlayerInfo(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),String.format(java.util.Locale.ROOT,"Player%03d",i)),false);info.setTabListDisplayName(Component.literal("Server Player "+i).withColor(0x74BBEE));list.add(info);}
                var entries=new ArrayList<>(list);var event=Client.INSTANCE.getEventBus().post(new TabDisplayEvent(TabDisplayEvent.Phase.SORT,null,null,entries));check(event.players().equals(entries),"BetterTab defaults to native sorting");
                var tab=Client.INSTANCE.getModuleManager().getByClass(BetterTabModule.class).orElseThrow();tab.sort.setValue(BetterTabModule.Sort.NAME);event=Client.INSTANCE.getEventBus().post(new TabDisplayEvent(TabDisplayEvent.Phase.SORT,null,null,entries));check(event.players().size()>=120,"Large player list retains all entries");tab.rows.setValue(30.0);
            });
            c.getInput().holdKey(InputConstants.KEY_TAB);c.waitTicks(8);c.takeScreenshot("utilities-better-tab");c.getInput().releaseKey(InputConstants.KEY_TAB);
            world.getServer().runCommand("gamemode survival @a");world.getServer().runCommand("setblock 0 64 3 obsidian");world.getServer().runCommand("tp @a 0.5 65 0.5 0 0");c.waitTicks(8);
            c.runOnClient(mc->{
                var auto=Client.INSTANCE.getModuleManager().getByClass(AutoToolModule.class).orElseThrow();var inv=mc.player.getInventory();inv.setSelectedSlot(0);inv.setItem(0,new ItemStack(Items.STICK));inv.setItem(1,new ItemStack(Items.DIAMOND_PICKAXE));
                var pos=new BlockPos(0,64,3);mc.gameMode.startDestroyBlock(pos,Direction.NORTH);check(inv.getSelectedSlot()==1,"Mining hook selects hotbar tool before interaction");mc.gameMode.stopDestroyBlock();check(inv.getSelectedSlot()==0,"Stopping mining restores previous slot");
                mc.gameMode.startDestroyBlock(pos,Direction.NORTH);inv.setSelectedSlot(2);mc.gameMode.stopDestroyBlock();check(inv.getSelectedSlot()==2,"Manual slot selection is not overwritten");
                inv.setSelectedSlot(0);var weak=new ItemStack(Items.DIAMOND_PICKAXE);weak.setDamageValue(weak.getMaxDamage()-1);inv.setItem(0,weak);inv.setItem(1,new ItemStack(Items.IRON_PICKAXE));mc.gameMode.startDestroyBlock(pos,Direction.NORTH);check(inv.getSelectedSlot()==1,"Low-durability protection replaces a faster near-broken held tool");mc.gameMode.stopDestroyBlock();inv.setItem(0,new ItemStack(Items.STICK));
                inv.setSelectedSlot(0);inv.setItem(1,ItemStack.EMPTY);inv.setItem(12,new ItemStack(Items.DIAMOND_PICKAXE));auto.inventoryTools.setValue(true);
                mc.gameMode.startDestroyBlock(pos,Direction.NORTH);check(inv.getSelectedSlot()==0&&inv.getItem(0).is(Items.DIAMOND_PICKAXE)&&inv.getItem(12).is(Items.STICK),"Inventory Tools uses synchronized slot swap");mc.gameMode.stopDestroyBlock();check(inv.getItem(0).is(Items.STICK)&&inv.getItem(12).is(Items.DIAMOND_PICKAXE),"Expected slots restore after mining");
                mc.gameMode.startDestroyBlock(pos,Direction.NORTH);inv.setItem(12,new ItemStack(Items.APPLE));mc.gameMode.stopDestroyBlock();check(inv.getItem(12).is(Items.APPLE),"Manual inventory changes prevent restoration");auto.setEnabled(false);
                var fast=Client.INSTANCE.getModuleManager().getByClass(FastBreakModule.class).orElseThrow();fast.multiplier.setValue(1.0);fast.setEnabled(true);var access=(dev.riserecode.testmixin.MiningStateAccessor)mc.gameMode;access.risecode$delay(5);mc.gameMode.startDestroyBlock(pos,Direction.NORTH);mc.gameMode.continueDestroyBlock(pos,Direction.NORTH);float baseline=access.risecode$progress();check(access.risecode$delay()==0,"Native inter-block delay removed");mc.gameMode.stopDestroyBlock();
                fast.multiplier.setValue(2.0);mc.gameMode.startDestroyBlock(pos,Direction.NORTH);mc.gameMode.continueDestroyBlock(pos,Direction.NORTH);check(access.risecode$progress()>=baseline*1.9,"Native progress acceleration hook");mc.gameMode.stopDestroyBlock();fast.setEnabled(false);
            });
            c.waitTicks(10);
            c.runOnClient(mc->{var chat=Client.INSTANCE.getModuleManager().getByClass(BetterChatModule.class).orElseThrow();chat.setEnabled(false);check(((dev.riserecode.testmixin.ChatLinesAccessor)mc.gui.hud.getChat()).risecode$lines().stream().noneMatch(line->line.parent().content().getString().contains("[⧉]")),"Disabling BetterChat removes existing display decorations");chat.setEnabled(true);});c.waitTicks(2);
            c.runOnClient(mc->{var client=Client.INSTANCE;var mini=client.getModuleManager().getByClass(MinimapModule.class).orElseThrow();var destinations=client.getMaps().waypoints().all();
                client.getModuleManager().setKeybind(mini,InputConstants.KEY_F9);client.getModuleManager().setBindMode(mini,dev.riserecode.module.Module.BindMode.HOLD);
                check(client.getProfileManager().saveProfile("UtilitiesRoundTrip"),"New module profile saves");mini.size.setValue(180.0);client.getModuleManager().setBindMode(mini,dev.riserecode.module.Module.BindMode.TOGGLE);
                check(client.getProfileManager().loadProfile("UtilitiesRoundTrip"),"New module profile loads");check(mini.size.getValue()==130&&mini.getBindMode()==dev.riserecode.module.Module.BindMode.HOLD&&mini.getKeybind()==InputConstants.KEY_F9,"New graphical settings and Hold binding persist");check(client.getMaps().waypoints().all().equals(destinations),"Loading module profiles preserves destinations");
            });
            c.runOnClient(mc->{selectMinecraftia(Client.INSTANCE.getThemeManager());((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting("Font Scale")).setValue(1.25);((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting("Interface Scale")).setValue(1.11);Client.INSTANCE.getThemeManager().changed();Client.INSTANCE.openHudEditor();});c.waitTicks(8);c.takeScreenshot("utilities-minimap-hud-editor");
            c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();mc.gui.setScreen(null);});
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("tp @a 1000 80 -1000");c.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getX()-1000)<1,600);c.waitTicks(8);
            c.runOnClient(mc->{var stats=Client.INSTANCE.getMaps().statistics();check(stats.pending()<=16&&stats.cpuTiles()<=512,"Teleport keeps terrain work bounded");});
            world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0.5 80 0.5");c.waitFor(mc->mc.level!=null&&mc.level.dimension().identifier().toString().equals("minecraft:the_nether"),1200);c.waitTicks(8);
            c.runOnClient(mc->{var maps=Client.INSTANCE.getMaps();check(maps.dimension().equals("minecraft:the_nether"),"Dimension change switches terrain namespace");check(maps.waypoints().all().stream().anyMatch(p->p.dimension().equals("minecraft:overworld")),"Dimension change preserves other destinations");var p=maps.addAtPlayer();check(p!=null&&p.dimension().equals("minecraft:the_nether"),"New waypoint uses current dimension");});
        }
        Client.INSTANCE.getLogger().info("Utilities native checks passed ({} assertions)",checks);
    }
}
