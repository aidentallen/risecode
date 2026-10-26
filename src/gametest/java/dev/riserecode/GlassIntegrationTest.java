package dev.riserecode;

import com.google.gson.*;
import dev.riserecode.config.SettingCodecs;
import dev.riserecode.module.modules.*;
import dev.riserecode.render.*;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.component.GlassEditorScreen;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.input.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

/** Packaged native geometry, policy, persistence, preview and lifetime checks. */
public final class GlassIntegrationTest {
    private static int checks;
    public static boolean failNextPipeline;
    private static GlassModule module(){return Client.INSTANCE.getModuleManager().getByClass(GlassModule.class).orElseThrow();}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void screen(ClientGameTestContext c,String label){c.waitTicks(5);c.takeScreenshot("027-"+label);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);mc.options.framerateLimit().set(90);policy();
            mc.gui.setScreen(new GlassEditorScreen(module(),null));
        });screen(c,"sample-preview-disabled");
        c.runOnClient(mc->{check(GlassRenderer.draws(View.PREVIEW)>0,"sample preview actually draws Glass");check(!module().isEnabled(),"preview leaves gameplay disabled");check(GlassPreview.ownedGpuBytes()>0,"isolated preview target");mc.gui.setScreen(null);});c.waitTicks(2);
        c.runOnClient(mc->{check(GlassPreview.ownedGpuBytes()==0&&GlassRenderer.ownedGpuBytes()==0,"preview closure frees its targets");});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server->{check(server.getPlayerList().getPlayers().stream().allMatch(p->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(p.nameAndId())),"disposable owner-only world");CameraFovClientTest.ownerOnlyShutdown=true;});
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -6 64 -8 6 64 8 stone");world.getServer().runCommand("fill -6 65 -8 6 70 -8 white_concrete");world.getServer().runCommand("fill -6 65 -8 -4 70 -8 blue_concrete");world.getServer().runCommand("fill 0 65 -8 2 70 -8 blue_concrete");world.getServer().runCommand("tp @a 0.5 65 3.5 180 0");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");c.waitTicks(12);
            c.runOnClient(mc->{mc.gui.setScreen(null);mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();mc.options.setCameraType(CameraType.FIRST_PERSON);module().setEnabled(true);equip();});
            screen(c,"first-person-crystal");
            world.getServer().runCommand("enchant @a minecraft:unbreaking 1");c.waitTicks(5);
            screen(c,"enchanted-native-glint");
            c.runOnClient(mc->module().shared.glint.setValue(false));screen(c,"suppressed-glint");
            c.runOnClient(mc->module().shared.glint.setValue(true));
            c.runOnClient(mc->{check(GlassRenderer.draws()>0&&GlassRenderer.copies()>0,"gameplay surface and scene-copy passes");check(GlassRenderer.status().equals("Ready"),"native shader readiness: "+GlassRenderer.status());});
            exercise(c,"native");depth(c,"native");
            world.getServer().runCommand("item replace entity @a armor.chest with diamond_chestplate[minecraft:trim={material:'minecraft:gold',pattern:'minecraft:sentry'}]");
            c.waitTicks(5);c.runOnClient(mc->{mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);module().shared.originalTexture.setValue(.55);});screen(c,"armor-trim-details");
            world.getServer().runCommand("item replace entity @a armor.chest with minecraft:elytra");c.waitTicks(5);c.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));screen(c,"elytra-chest-slot");
            c.runOnClient(mc->{equip();module().shared.applyPreset(GlassModule.Preset.CRYSTAL);mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);});screen(c,"swing");
            c.getInput().holdKey(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT);screen(c,"crouching");c.getInput().releaseKey(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT);
            c.runOnClient(mc->{mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));mc.player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);});screen(c,"bow-use");
            c.runOnClient(mc->{mc.player.stopUsingItem();equip();});
            world.getServer().runCommand("fill -2 65 1 2 68 5 water");c.waitTicks(5);
            c.runOnClient(mc->{mc.player.setSwimming(true);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});screen(c,"swimming-water");
            world.getServer().runCommand("fill -2 65 1 2 68 5 air");c.waitTicks(5);c.runOnClient(mc->{mc.player.setSwimming(false);mc.options.setCameraType(CameraType.FIRST_PERSON);});
            c.runOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->mc.gui.overlay()==null,1200);c.waitTicks(6);
            c.runOnClient(mc->check(GlassRenderer.status().equals("Ready"),"resource reload rebuilds glass resources"));
            if(Boolean.getBoolean("riserecode.withShaderPack")){
                c.runOnClient(mc->{var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(true);try{config.save();net.irisshaders.iris.Iris.reload();}catch(java.io.IOException e){throw new AssertionError(e);}});
                c.waitFor(mc->ShaderWorldOverlay.active()&&mc.gui.overlay()==null,1200);
                c.runOnClient(mc->{check(GlassIrisAdapter.supported(),"pinned Iris version");
                    boolean previous=net.irisshaders.iris.shadows.ShadowRenderer.ACTIVE;
                    try{net.irisshaders.iris.shadows.ShadowRenderer.ACTIVE=true;check(GlassCapture.surface(GlassCapture.local(),Component.ARM,true,Slot.NONE)==null,"Iris shadow pass preserves native geometry");}finally{net.irisshaders.iris.shadows.ShadowRenderer.ACTIVE=previous;}
                });exercise(c,"iris");depth(c,"iris");
                c.runOnClient(mc->{try{net.irisshaders.iris.Iris.reload();}catch(java.io.IOException e){throw new AssertionError(e);}});c.waitTicks(12);screen(c,"iris-reload");
            }
            c.runOnClient(mc->{failNextPipeline=true;module().setEnabled(false);module().setEnabled(true);});c.waitTicks(4);
            c.runOnClient(mc->check(GlassRenderer.status().startsWith("Native rendering restored"),"pipeline fault restores native rendering and reports status"));screen(c,"native-fallback");
            c.runOnClient(mc->{module().setEnabled(false);module().setEnabled(true);});c.waitTicks(4);
            c.runOnClient(mc->check(GlassRenderer.status().equals("Ready"),"enable retries renderer after failure"));
            timings(c);
            c.runOnClient(mc->{module().setEnabled(false);long d=GlassRenderer.draws();measureStart=d;check(GlassRenderer.ownedGpuBytes()==0,"disable releases gameplay GPU targets");});c.waitTicks(8);
            c.runOnClient(mc->check(GlassRenderer.draws()==measureStart,"disabled Glass has no glass draws"));
            c.runOnClient(mc->{module().setEnabled(true);mc.gui.setScreen(new GlassEditorScreen(module(),null));});screen(c,"equipped-editor");
            c.runOnClient(mc->{var p=mc.player;var camera=mc.options.getCameraType();var stack=p.getMainHandItem().copy();var editor=(GlassEditorScreen)mc.gui.screen();
                var e=new MouseButtonEvent(mc.getWindow().getGuiScaledWidth()*.8,mc.getWindow().getGuiScaledHeight()*.45,new MouseButtonInfo(0,0));
                check(editor.mouseClicked(e,false),"preview accepts rotation");editor.mouseDragged(e,30,10);editor.mouseReleased(e);editor.mouseScrolled(e.x(),e.y(),0,2);
                check(editor.rotation()!=155&&editor.zoom()>1,"rotation and zoom controls");check(mc.player==p&&mc.options.getCameraType()==camera&&ItemStack.matches(stack,p.getMainHandItem()),"preview leaves live entity, camera and equipment untouched");mc.gui.setScreen(null);
            });
            for(int i=0;i<8;i++){c.runOnClient(mc->module().setEnabled(false));c.runOnClient(mc->module().setEnabled(true));c.waitTicks(2);}
            c.runOnClient(mc->{module().setEnabled(false);check(GlassRenderer.ownedGpuBytes()==0,"repeated toggles release resources");Client.INSTANCE.getPerformance().worldResolution.setValue(100.0);});
        }finally{CameraFovClientTest.ownerOnlyShutdown=false;}
        c.runOnClient(mc->{var results=new JsonObject();GlassTimings.save();results.addProperty("assertions",checks);results.addProperty("surfaceDraws",GlassRenderer.draws());results.addProperty("sceneCopies",GlassRenderer.copies());results.addProperty("bloomComposites",GlassRenderer.composites());results.addProperty("allocations",GlassRenderer.allocations());results.addProperty("interpretation","Native screenshot and lifetime validation; warmed submission and frame costs are recorded separately. No live multiplayer or long-session claim.");try{Files.writeString(Path.of("027-validation.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}catch(java.io.IOException e){throw new AssertionError(e);}});
        Client.INSTANCE.getLogger().info("Glass native checks passed ({} assertions)",checks);
    }
    private static void depth(ClientGameTestContext c,String label){
        c.runOnClient(mc->{
            module().selfBody.setValue(false);module().selfArmor.setValue(false);module().arms.setValue(false);module().firstItems.setValue(false);module().thirdItems.setValue(false);module().others.setValue(true);
            Client.INSTANCE.getModuleManager().getByClass(PlayerEspModule.class).orElseThrow().setEnabled(false);
            Client.INSTANCE.getModuleManager().getByClass(EnchantEffectModule.class).orElseThrow().setEnabled(false);
            for(int id=27001;id<=27003;id++)mc.level.removeEntity(id,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            var remote=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new UUID(0,27010),"DepthFixture"));remote.setId(27010);remote.setPos(.5,65,-10.5);mc.level.addEntity(remote);
            mc.options.setCameraType(CameraType.FIRST_PERSON);measureStart=GlassRenderer.draws(View.WORLD);
        });c.waitTicks(8);
        c.runOnClient(mc->check(GlassRenderer.draws(View.WORLD)>measureStart,"occluded geometry reaches Glass execution"));
        check(coverage(c,label+"-occluded")==0,"world depth rejects every occluded glass fragment");
        c.runOnClient(mc->mc.level.getEntity(27010).setPos(.5,65,-5.5));c.waitTicks(8);
        check(coverage(c,label+"-visible")>100,"same geometry renders in front of wall");screen(c,label+"-depth-visible");
        c.runOnClient(mc->{mc.level.removeEntity(27010,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);module().selfBody.setValue(true);module().selfArmor.setValue(true);module().arms.setValue(true);module().firstItems.setValue(true);module().thirdItems.setValue(true);});
    }
    private static int coverage(ClientGameTestContext c,String name){
        var ready=new java.util.concurrent.atomic.AtomicBoolean();var file=Path.of("screenshots","027-coverage-"+name+".png");
        c.runOnClient(mc->{var target=GlassRenderer.coverageTarget(View.WORLD);check(target!=null,"world normal/coverage target exists");net.minecraft.client.Screenshot.takeScreenshot(target,image->{try(image){image.writeToFile(file);ready.set(true);}catch(java.io.IOException e){throw new AssertionError(e);}});});
        c.waitFor(mc->ready.get(),200);
        try{var image=javax.imageio.ImageIO.read(file.toFile());int covered=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)&0x00FFFFFF)!=0)covered++;return covered;}catch(java.io.IOException e){throw new AssertionError(e);}
    }
    private static void timings(ClientGameTestContext c){
        for(int round=0;round<2;round++)for(boolean enabled:new boolean[]{false,true}){
            c.runOnClient(mc->{module().setEnabled(enabled);mc.options.setCameraType(CameraType.FIRST_PERSON);});c.waitTicks(30);
            String label=(enabled?"glass":"disabled")+"-"+round;
            c.runOnClient(mc->GlassTimings.scene(label));c.waitTicks(45);c.runOnClient(mc->GlassTimings.scene(""));
        }
    }
    private static long measureStart;
    private static void equip(){var mc=net.minecraft.client.Minecraft.getInstance();mc.player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));mc.player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));mc.player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));mc.player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));mc.player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));mc.player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));}
    private static void exercise(ClientGameTestContext c,String label){
        for(var preset:GlassModule.Preset.values())if(preset!=GlassModule.Preset.CUSTOM){c.runOnClient(mc->module().shared.applyPreset(preset));screen(c,label+"-"+preset);}
        c.runOnClient(mc->{module().shared.applyPreset(GlassModule.Preset.CRYSTAL);module().body.override.setValue(true);module().body.applyPreset(GlassModule.Preset.VIOLET);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});screen(c,label+"-third-person-mixed-materials");
        c.runOnClient(mc->check(GlassRenderer.draws(View.WORLD)>0&&GlassRenderer.draws(Component.ARMOR)>0,"world body and armor submissions"));
        c.runOnClient(mc->{module().selfBody.setValue(false);module().head.setValue(false);module().mainHand.setValue(false);});screen(c,label+"-native-body-glass-armor");
        c.runOnClient(mc->{module().selfBody.setValue(true);module().head.setValue(true);module().mainHand.setValue(true);module().body.override.setValue(false);mc.options.setCameraType(CameraType.FIRST_PERSON);module().selfArmor.setValue(false);});
        for(var item:List.of(Items.BOW,Items.TRIDENT,Items.FILLED_MAP,Items.GLASS_PANE,Items.DIAMOND_SWORD)){
            c.runOnClient(mc->{
                var stack=new ItemStack(item);
                if(item==Items.FILLED_MAP){
                    var id=new net.minecraft.world.level.saveddata.maps.MapId(27);var data=net.minecraft.world.level.saveddata.maps.MapItemSavedData.createForClient((byte)0,false,mc.level.dimension());
                    for(int z=0;z<128;z++)for(int x=0;x<128;x++)data.colors[z*128+x]=(byte)(((x/16+z/16)%2==0?net.minecraft.world.level.material.MapColor.COLOR_BLUE:net.minecraft.world.level.material.MapColor.SNOW).id*4+2);
                    mc.level.overrideMapData(id,data);stack.set(net.minecraft.core.component.DataComponents.MAP_ID,id);
                }
                mc.player.setItemSlot(EquipmentSlot.MAINHAND,stack);
            });screen(c,label+"-item-"+item.toString().replace(':','-'));
        }
        c.runOnClient(mc->{equip();module().selfArmor.setValue(true);module().firstItems.setValue(false);});screen(c,label+"-native-items-glass-arms");
        c.runOnClient(mc->{module().firstItems.setValue(true);module().arms.setValue(false);});screen(c,label+"-native-arms-glass-items");
        c.runOnClient(mc->{module().arms.setValue(true);Client.INSTANCE.getModuleManager().getByClass(ViewModelModule.class).orElseThrow().setEnabled(true);Client.INSTANCE.getModuleManager().getByClass(EnchantEffectModule.class).orElseThrow().setEnabled(true);
            var remote=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new UUID(0,27001),"GlassFixture"));remote.setId(27001);remote.setPos(.5,65,6.5);remote.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));remote.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));mc.level.addEntity(remote);
            for(int i=0;i<2;i++){
                final int model=i;
                var variant=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new UUID(0,27002+i),"GlassModel"+i)){
                    @Override public net.minecraft.world.entity.player.PlayerSkin getSkin(){return net.minecraft.client.resources.DefaultPlayerSkin.get(new UUID(0,model==0?0:9));}
                };
                variant.setId(27002+i);variant.setPos(-1.5+i*4,65,6.5);mc.level.addEntity(variant);
            }
            module().others.setValue(true);
            var esp=Client.INSTANCE.getModuleManager().getByClass(PlayerEspModule.class).orElseThrow();dev.riserecode.ui.theme.ThemeManager.assign(esp.getSettings().stream().filter(s->s.getName().equals("Mode")).findFirst().orElseThrow(),PlayerEspModule.Mode.OUTLINE);esp.setEnabled(true);
        });screen(c,label+"-other-player-esp-viewmodel");
        for(int scale:new int[]{75,100}){c.runOnClient(mc->Client.INSTANCE.getPerformance().worldResolution.setValue((double)scale));c.waitTicks(8);c.runOnClient(mc->check(GlassRenderer.status().equals("Ready"),"scaled glass renderer: "+GlassRenderer.status()));}
        c.getInput().resizeWindow(1280,800);c.waitTicks(5);screen(c,label+"-resized");c.getInput().resizeWindow(1600,900);c.waitTicks(5);
        c.runOnClient(mc->check(GlassRenderer.status().equals("Ready"),"post-resize renderer ready"));
    }
    private static void policy(){
        var m=module();check(!m.isEnabled()&&!m.others.getValue(),"opt-in self and other-player defaults");check(m.shared.frosted.getValue()==.35&&m.shared.glow.getValue()==.8&&m.shared.shimmer.getValue(),"Crystal defaults");
        var p=m.capture();var material=p.shared();check(p.select(true,Component.BODY,View.WORLD,true,Slot.NONE).equals(material),"body inherits shared");check(p.select(false,Component.BODY,View.WORLD,true,Slot.NONE)==null,"others gated");
        m.body.override.setValue(true);m.body.applyPreset(GlassModule.Preset.VIOLET);var violet=m.capture().body();check(!violet.equals(material),"body override");m.body.override.setValue(false);check(m.capture().body().equals(material),"inheritance restored");m.body.override.setValue(true);check(m.capture().body().equals(violet),"disabled override retained");m.body.override.setValue(false);
        for(var toggle:List.of(m.selfBody,m.arms,m.selfArmor,m.firstItems,m.thirdItems,m.mainHand,m.offHand,m.head,m.chest,m.legs,m.feet)){
            var before=m.capture();toggle.setValue(false);var after=m.capture();check(!after.equals(before),"independent target "+toggle.getName());toggle.setValue(true);
        }
        m.offHand.setValue(false);check(m.capture().select(true,Component.ITEM,View.HAND,false,Slot.NONE)==null,"offhand filter");check(m.capture().select(true,Component.ITEM,View.HAND,true,Slot.NONE)!=null,"main hand preserved");m.offHand.setValue(true);
        m.head.setValue(false);check(m.capture().select(true,Component.ARMOR,View.WORLD,true,Slot.HEAD)==null&&m.capture().select(true,Component.ARMOR,View.WORLD,true,Slot.CHEST)!=null,"per-slot filtering");m.head.setValue(true);
        m.shared.applyPreset(GlassModule.Preset.FROSTED);check(m.shared.preset.getValue()==GlassModule.Preset.FROSTED&&m.selfBody.getValue(),"presets preserve targets");m.shared.refraction.setValue(.61);check(m.shared.preset.getValue()==GlassModule.Preset.CUSTOM,"manual changes mark Custom");
        var saved=new LinkedHashMap<Setting<?>,JsonElement>();for(var s:m.getSettings())saved.put(s,SettingCodecs.snapshot(s));var expected=m.capture();for(var s:m.getSettings())s.reset();saved.forEach(SettingCodecs::apply);check(m.capture().equals(expected)&&m.shared.preset.getValue()==GlassModule.Preset.CUSTOM,"normal settings codec round trip");
        var profile=Client.INSTANCE.getConfigManager().profileSnapshot();
        check(Client.INSTANCE.getProfileManager().saveProfile("GlassRoundTrip"),"profile saved");
        m.shared.applyPreset(GlassModule.Preset.CLEAR);m.offHand.setValue(false);
        check(Client.INSTANCE.getProfileManager().loadProfile("GlassRoundTrip"),"profile loaded");check(m.capture().equals(expected),"profile restores all materials, inheritance and targets");
        check(Client.INSTANCE.getConfigManager().profileSnapshot().getAsJsonObject("modules").get("Glass").equals(profile.getAsJsonObject("modules").get("Glass")),"profile document round trip");
        for(var s:m.getSettings())s.reset();check(m.shared.preset.getValue()==GlassModule.Preset.CRYSTAL&&!m.body.override.getValue(),"reset restores Crystal and inheritance");
    }
    private GlassIntegrationTest() { }
}
