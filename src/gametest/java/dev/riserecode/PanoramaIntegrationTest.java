package dev.riserecode;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.ui.panorama.*;
import dev.riserecode.ui.theme.*;
import dev.riserecode.ui.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.*;

/** Packaged title scene checks in disposable profiles, without starting a level for the scene. */
public final class PanoramaIntegrationTest {
    private static int checks;
    public static boolean measuring,forceUnfocused,failNextRender;
    private static final ArrayList<double[]> costs=new ArrayList<>();
    private static final JsonObject results=new JsonObject();
    private static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
    private static PanoramaPreferences p(){return LivingPanorama.preferences();}
    public static void cost(long nanos,long bytes,long cpu,boolean updated){if(measuring)costs.add(new double[]{nanos/1e6,bytes,cpu/1e6,updated?1:0});}
    private static MouseButtonEvent event(double x,double y){return new MouseButtonEvent(x,y,new MouseButtonInfo(0,0));}
    private static void ready(ClientGameTestContext c){c.waitFor(mc->!LivingPanorama.failure().isEmpty()||LivingPanorama.renderer().texture()!=null&&LivingPanorama.settled(),1200);c.runOnClient(mc->check(LivingPanorama.failure().isEmpty(),LivingPanorama.failure()));}
    private static void screenshot(ClientGameTestContext c,String name){c.takeScreenshot("026-"+name);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        long startup=System.nanoTime();
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();Theme.manager().reset();mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.options.pauseOnLostFocus=false;mc.gui.setScreen(new TitleScreen());
            check(!p().enabled.getValue(),"opt-in default");check(!mc.hasSingleplayerServer()&&mc.level==null,"no scene world/server");p().enabled.setValue(true);
        });ready(c);results.addProperty("startupUntilFadedMs",(System.nanoTime()-startup)/1e6);screenshot(c,"misty-dawn-menu");
        c.runOnClient(mc->{check(LivingPanorama.renderer().ownedBytes()>0,"scene resources uploaded");check(LivingPanorama.ownedSounds()==3,"owned environmental voices started");});
        if(Boolean.getBoolean("riserecode.withShaderPack")){
            c.runOnClient(mc->{var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(true);try{config.save();net.irisshaders.iris.Iris.reload();}catch(java.io.IOException e){throw new AssertionError(e);}});c.waitTicks(8);ready(c);
        }
        for(var atmosphere:PanoramaPreferences.Atmosphere.values()){
            if(atmosphere==PanoramaPreferences.Atmosphere.CUSTOM)continue;
            c.runOnClient(mc->p().applyPreset(atmosphere));c.waitTicks(75);
            c.runOnClient(mc->{check(p().atmosphere.getValue()==atmosphere,"atmosphere retained");check(LivingPanorama.renderer().latest()!=null,"atmosphere rendered");check(Math.abs(LivingPanorama.renderer().latest().hour()-p().time.getValue())<.2,"three second environment blend completes");});screenshot(c,atmosphere.name().toLowerCase(Locale.ROOT));
        }
        c.runOnClient(mc->{p().applyPreset(PanoramaPreferences.Atmosphere.SUNLIT_DAY);});c.waitTicks(75);
        interaction(c);scenic(c);
        for(var quality:PanoramaPreferences.Quality.values()){
            c.runOnClient(mc->{p().quality.setValue(quality);});c.waitTicks(35);ready(c);
            c.runOnClient(mc->{var texture=LivingPanorama.renderer().texture();int expected=(int)Math.ceil(mc.getWindow().getScreenWidth()*(quality==PanoramaPreferences.Quality.LOW?.75:1));check(texture.getWidth(0)==expected,"quality scene resolution");check(mc.gameRenderer.mainRenderTarget().width==mc.getWindow().getScreenWidth(),"native UI resolution preserved");});
            screenshot(c,"quality-"+quality);c.waitTicks(30);
            long uploads=c.computeOnClient(mc->LivingPanorama.renderer().uploads());long bytes=c.computeOnClient(mc->LivingPanorama.renderer().ownedBytes());long allocations=c.computeOnClient(mc->LivingPanorama.renderer().allocations());long draws=c.computeOnClient(mc->LivingPanorama.renderer().draws());long frames=c.computeOnClient(mc->LivingPanorama.renderer().frames());
            c.runOnClient(mc->{LivingPanorama.renderer().resetMeasurements();costs.clear();measuring=true;});c.waitTicks(100);
            c.runOnClient(mc->{measuring=false;check(LivingPanorama.renderer().uploads()==uploads,"steady scene has no mesh uploads");check(LivingPanorama.renderer().ownedBytes()==bytes&&LivingPanorama.renderer().allocations()==allocations,"steady resource counts bounded");
                var data=new JsonObject();var renderer=LivingPanorama.renderer();data.addProperty("samples",costs.size());data.addProperty("updateSamples",costs.stream().filter(v->v[3]==1).count());data.addProperty("sceneUpdateCpuP95Ms",percentile(costs.stream().filter(v->v[3]==1).mapToDouble(v->v[2]).toArray(),.95));data.addProperty("sceneUpdateAllocatedBytesP95",percentile(costs.stream().filter(v->v[3]==1).mapToDouble(v->v[1]).toArray(),.95));data.addProperty("sceneCpuWallP95Ms",percentile(costs.stream().mapToDouble(v->v[0]).toArray(),.95));data.addProperty("sceneThreadCpuP95Ms",percentile(costs.stream().mapToDouble(v->v[2]).toArray(),.95));data.addProperty("sceneAllocatedBytesP95",percentile(costs.stream().mapToDouble(v->v[1]).toArray(),.95));data.addProperty("rendererCommandCpuP95Ms",percentile(renderer.cpuSamples().stream().mapToDouble(Double::doubleValue).toArray(),.95));float timestampPeriod=com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().timestampPeriod();data.addProperty("gpuTimestampPeriodNs",timestampPeriod);data.addProperty("sceneGpuP95Ms",percentile(renderer.gpuSamples().stream().mapToDouble(v->v*timestampPeriod).toArray(),.95));data.addProperty("drawCallsPerUpdate",(renderer.draws()-draws)/(double)Math.max(1,renderer.frames()-frames));data.addProperty("ownedBytes",bytes);data.addProperty("meshUploadsDuringMeasurement",renderer.uploads()-uploads);results.add(quality.name(),data);
            });
        }
        robustness(c);pack(c);lifecycle(c);
        c.runOnClient(mc->{p().quality.setValue(PanoramaPreferences.Quality.BALANCED);p().trees.setValue(1.0);p().plants.setValue(1.0);});c.waitTicks(60);ready(c);screenshot(c,"maximum-vegetation");
        c.getInput().resizeWindow(2560,1080);c.waitTicks(10);screenshot(c,"ultrawide-menu");c.getInput().pressKey(InputConstants.KEY_F1);c.waitTicks(12);screenshot(c,"ultrawide-scenic");
        for(int i=0;i<24;i++){c.getInput().setCursorPos(1800+Math.sin(i*.4)*250,800+Math.cos(i*.3)*100);c.waitTicks(1);screenshot(c,String.format(Locale.ROOT,"motion-%03d",i));}
        c.getInput().pressKey(InputConstants.KEY_F1);c.waitTicks(10);c.getInput().pressKey(InputConstants.KEY_F11);c.waitTicks(12);screenshot(c,"fullscreen");c.getInput().pressKey(InputConstants.KEY_F11);c.waitTicks(8);
        c.getInput().resizeWindow(2048,1152);c.waitTicks(10);
        c.runOnClient(mc->{ThemeManager.assign(Theme.manager().getSetting("Reduced Motion"),true);});c.waitTicks(4);
        c.runOnClient(mc->{check(LivingPanorama.renderer().latest().reduced()&&LivingPanorama.renderer().latest().particles()==0,"Reduced Motion freezes procedural motion and particles");p().mute.setValue(true);});c.waitTicks(3);
        c.runOnClient(mc->{check(LivingPanorama.ownedSounds()==0,"mute stops owned voices");p().mute.setValue(false);ThemeManager.assign(Theme.manager().getSetting("Reduced Motion"),false);p().enabled.setValue(false);});c.waitTicks(3);
        c.runOnClient(mc->{check(LivingPanorama.renderer().ownedBytes()==0&&LivingPanorama.ownedSounds()==0,"disable releases resources");check(Theme.manager().choice("Background",ThemeManager.Background.class)==ThemeManager.Background.LAKE,"saved backdrop retained");
            results.addProperty("assertions",checks);results.addProperty("interpretation","Shared-host title-scene timings; CPU, GPU and allocations are separate. Warm-up excluded. UI composition and button blur excluded from scene GPU timestamps.");
            try{Files.writeString(Path.of("panorama-costs.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}catch(java.io.IOException e){throw new AssertionError(e);}Theme.manager().reset();
        });Client.INSTANCE.getLogger().info("Living Panorama native checks passed ({} assertions)",checks);
    }
    private static void interaction(ClientGameTestContext c){
        c.runOnClient(mc->{var title=mc.gui.screen();int before=LivingPanorama.interaction().snapshot().ripples().size();var click=event(title.width*.5,title.height*.72);title.mouseClicked(click,false);title.mouseReleased(click);check(LivingPanorama.interaction().snapshot().ripples().size()==before+1,"exposed water click ripples");
            var drag=event(title.width*.7,title.height*.72);title.mouseClicked(drag,false);title.mouseDragged(event(drag.x()+50,drag.y()),50,0);title.mouseReleased(event(drag.x()+50,drag.y()));check(LivingPanorama.interaction().snapshot().ripples().size()==before+1,"drag never also ripples");check(LivingPanorama.interaction().snapshot().yaw()>0,"drag changes view");LivingPanorama.resetView();
            var widget=title.children().stream().filter(child->child instanceof AbstractWidget w&&w.getMessage().getString().contains("Interface")).map(AbstractWidget.class::cast).findFirst().orElseThrow();widget.active=false;title.mouseClicked(event(widget.getX()+5,widget.getY()+5),false);title.mouseReleased(event(widget.getX()+5,widget.getY()+5));check(!LivingPanorama.interaction().pressed(),"disabled visible widget blocks background press");widget.active=true;title.mouseClicked(event(widget.getX()+5,widget.getY()+5),false);check(mc.gui.screen() instanceof dev.riserecode.ui.clickgui.ClickGui,"menu input takes precedence over background");check(!LivingPanorama.interaction().pressed(),"menu click does not start drag");mc.gui.setScreen(new TitleScreen());
        });c.waitTicks(5);ready(c);
    }
    private static void scenic(ClientGameTestContext c){
        c.getInput().pressKey(InputConstants.KEY_F1);c.waitTicks(12);screenshot(c,"take-a-breath");
        c.runOnClient(mc->{check(LivingPanorama.scenic(),"F1 enters scenic mode");var title=mc.gui.screen();check(title.getFocused()==null,"scenic clears focus");check(title.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).noneMatch(w->w.active&&!(w instanceof PanoramaControl control&&control.returnControl())),"hidden widgets inactive");title.keyPressed(new KeyEvent(InputConstants.KEY_TAB,0,0));check(title.getFocused()==null,"Tab cannot focus hidden controls");
            var click=event(title.width*.5,title.height*.5);title.mouseClicked(click,false);title.mouseReleased(click);check(mc.gui.screen()==title,"hidden menu receives no click");
        });c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitTicks(12);c.runOnClient(mc->check(!LivingPanorama.scenic(),"Escape restores menu"));
        c.getInput().pressKey(InputConstants.KEY_F1);c.waitTicks(10);c.getInput().setCursorPos(40,40);c.getInput().holdMouse(0);c.getInput().releaseMouse(0);c.waitTicks(10);c.runOnClient(mc->check(!LivingPanorama.scenic(),"faint return leaf restores menu"));
    }
    private static void robustness(ClientGameTestContext c){
        c.runOnClient(mc->p().cycle.setValue(true));c.waitTicks(5);float cycleHour=c.computeOnClient(mc->LivingPanorama.renderer().latest().hour());c.waitTicks(20);
        c.runOnClient(mc->{check(LivingPanorama.renderer().latest().hour()>cycleHour,"optional cycle advances environmental time");check(p().atmosphere.getValue()==PanoramaPreferences.Atmosphere.SUNLIT_DAY,"cycle does not mutate preset identity");p().cycle.setValue(false);});
        c.runOnClient(mc->{p().refresh.setValue(PanoramaPreferences.Refresh.FPS_30);});c.waitTicks(4);
        long before=c.computeOnClient(mc->LivingPanorama.renderer().frames());c.waitTicks(20);
        c.runOnClient(mc->{long updates=LivingPanorama.renderer().frames()-before;check(updates>20&&updates<40,"30 Hz reuses texture between updates");forceUnfocused=true;});c.waitTicks(3);
        long frozen=c.computeOnClient(mc->LivingPanorama.renderer().frames());c.waitTicks(4);
        c.runOnClient(mc->{check(LivingPanorama.renderer().frames()==frozen&&LivingPanorama.ownedSounds()==0,"focus loss freezes renderer and stops owned sounds");forceUnfocused=false;p().refresh.setValue(PanoramaPreferences.Refresh.FPS_60);});c.waitTicks(4);
        c.runOnClient(mc->{check(LivingPanorama.ownedSounds()==3,"focus return restores quiet ambience");failNextRender=true;});c.waitTicks(3);
        c.runOnClient(mc->{check(!LivingPanorama.failure().isEmpty()&&!LivingPanorama.active()&&p().enabled.getValue(),"render failure retains requested panorama preference");check(LivingPanorama.renderer().ownedBytes()==0&&LivingPanorama.ownedSounds()==0,"render failure releases owned resources");});screenshot(c,"fallback-after-injected-failure");
        c.runOnClient(mc->LivingPanorama.reload());ready(c);
        c.runOnClient(mc->{p().enabled.setValue(false);});c.waitTicks(3);c.runOnClient(mc->{ThemeManager.assign(Theme.manager().getSetting("Background"),ThemeManager.Background.NONE);PageAppearanceScreen.open(mc.gui.screen());p().enabled.setValue(true);});
        c.waitFor(mc->LivingPanorama.renderer().texture()!=null,1200);screenshot(c,"native-panorama-fade");ready(c);
        c.runOnClient(mc->{check(Theme.manager().choice("Background",ThemeManager.Background.class)==ThemeManager.Background.NONE,"native backdrop selection retained during replacement");mc.gui.screen().onClose();ThemeManager.assign(Theme.manager().getSetting("Background"),ThemeManager.Background.LAKE);});
    }
    private static void pack(ClientGameTestContext c){
        var selected=c.computeOnClient(mc->List.copyOf(mc.getResourcePackRepository().getSelectedIds()));var before=c.computeOnClient(mc->PanoramaResources.capture());
        c.runOnClient(mc->{
            try{var dir=mc.getResourcePackDirectory().resolve("panorama-probe");Files.createDirectories(dir.resolve("assets/minecraft/models/block"));int version=net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).major();
                Files.writeString(dir.resolve("pack.mcmeta"),"{\"pack\":{\"description\":\"Panorama model probe\",\"min_format\":"+version+",\"max_format\":"+version+"}}");
                Files.writeString(dir.resolve("assets/minecraft/models/block/oak_log.json"),"{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"minecraft:block/mossy_cobblestone\"}}");
                mc.getResourcePackRepository().reload();var packs=new ArrayList<>(selected);packs.add("file/panorama-probe");mc.getResourcePackRepository().setSelected(packs);
            }catch(java.io.IOException e){throw new AssertionError(e);}
        });var loaded=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->loaded.isDone()&&mc.gui.overlay()==null,1200);ready(c);
        c.runOnClient(mc->{check(mc.getResourcePackRepository().getSelectedIds().contains("file/panorama-probe"),"probe resource pack selected");check(!PanoramaResources.capture().equals(before),"loaded model snapshot follows active pack");});screenshot(c,"resource-pack-models");
        c.runOnClient(mc->mc.getResourcePackRepository().setSelected(selected));var reset=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reset.isDone()&&mc.gui.overlay()==null,1200);ready(c);
    }
    private static void lifecycle(ClientGameTestContext c){
        c.runOnClient(mc->{PageAppearanceScreen.open(mc.gui.screen());});c.waitTicks(6);screenshot(c,"gear-editor");
        c.runOnClient(mc->{check(LivingPanorama.active(),"title gear retains scene flow");mc.gui.screen().onClose();p().trees.setValue(.19);});c.waitTicks(6);
        long generation=c.computeOnClient(mc->LivingPanorama.generation());var future=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->future.isDone()&&mc.gui.overlay()==null,1200);ready(c);
        c.runOnClient(mc->check(LivingPanorama.generation()>generation,"reload cancels obsolete generations"));
        for(int i=0;i<3;i++){
            c.runOnClient(mc->mc.gui.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(mc.gui.screen(),mc.options,false)));c.waitTicks(3);c.runOnClient(mc->{check(LivingPanorama.renderer().ownedBytes()==0&&LivingPanorama.ownedSounds()==0,"departure frees scene and sounds");check(!LivingPanorama.scenic(),"scenic resets on departure");mc.gui.setScreen(new TitleScreen());});ready(c);
        }
        // The feature itself never requests a level; verify ownership also while entering a disposable world.
        c.runOnClient(mc->p().mute.setValue(true));
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            c.runOnClient(mc->{check(!LivingPanorama.active()&&LivingPanorama.renderer().ownedBytes()==0,"world transition releases title renderer");});
        }
        c.runOnClient(mc->{mc.gui.setScreen(new TitleScreen());p().mute.setValue(false);});ready(c);
    }
    private static double percentile(double[] values,double fraction){if(values.length==0)return -1;Arrays.sort(values);return values[(int)Math.min(values.length-1,Math.floor(values.length*fraction))];}
    private PanoramaIntegrationTest(){}
}
