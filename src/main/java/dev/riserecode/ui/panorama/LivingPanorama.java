package dev.riserecode.ui.panorama;

import dev.riserecode.Client;
import dev.riserecode.ui.menu.*;
import dev.riserecode.ui.theme.Theme;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.*;
import org.joml.Vector3f;

/** Title/editor flow owner. Generation IDs reject stale density and reload results. */
public final class LivingPanorama {
    private static final PanoramaRenderer RENDERER=new PanoramaRenderer();
    private static final PanoramaSound SOUND=new PanoramaSound();
    private static final PanoramaInteraction INPUT=new PanoramaInteraction();
    private static final Map<AbstractWidget,Boolean> DISABLED=new IdentityHashMap<>();
    private static ExecutorService worker;
    private static CompletableFuture<PanoramaResources.Prepared> pending;
    private static PanoramaResources.Prepared prepared;
    private static ForestScene.Description description;
    private static volatile long generation;
    private static long previous,lastRender,densityChanged,lastGust;
    private static double densityTrees=-1,densityPlants=-1;
    private static boolean low,failed,scenic,wasEnabled,cycling;
    private static float cycleOffset;
    private static float seconds,fade,menuFade,hour,wind,rain,fog;
    private static float fromHour,fromWind,fromRain,fromFog,targetHour,targetWind,targetRain,targetFog,blend=3;
    private static float cursorWorldX=1000,cursorWorldZ=1000;
    private static double lastPointerX=Double.NaN,lastPointerY=Double.NaN;
    private static String failure="";
    public static PanoramaPreferences preferences(){return Client.INSTANCE.getThemeManager().panorama();}
    public static boolean flow(Screen screen){return screen instanceof TitleScreen||screen instanceof PageAppearanceScreen editor&&editor.target().equals(TitleScreen.class.getName());}
    public static boolean requested(){return Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&preferences().enabled.getValue()&&Minecraft.getInstance().level==null&&flow(Minecraft.getInstance().gui.screen());}
    public static boolean active(){return requested()&&!failed;}
    public static boolean scenic(){return active()&&scenic&&Minecraft.getInstance().gui.screen() instanceof TitleScreen;}
    public static boolean settled(){return fade>=1;}
    public static double menuOpacity(){return 1-menuFade;}
    public static PanoramaRenderer renderer(){return RENDERER;}
    public static PanoramaInteraction interaction(){return INPUT;}
    public static long generation(){return generation;}
    public static String failure(){return failure;}
    public static int ownedSounds(){return SOUND.owned();}
    public static void tick(){
        if(!Client.INSTANCE.isInitialized())return;
        boolean enabled=preferences().enabled.getValue();if(enabled!=wasEnabled){failed=false;failure="";wasEnabled=enabled;}
        if(!requested()){if(worker!=null||RENDERER.ready()||SOUND.owned()>0)close();scenic=false;menuFade=0;}
        else if(!Minecraft.getInstance().isWindowActive()){SOUND.close();INPUT.cancel();previous=0;}
        if(Minecraft.getInstance().gui.screen() instanceof TitleScreen title)syncWidgets(title);
    }
    public static void pointer(double x,double y){
        if(!active())return;var screen=Minecraft.getInstance().gui.screen();
        boolean moved=!Double.isNaN(lastPointerX)&&Math.hypot(x-lastPointerX,y-lastPointerY)>.5;
        if(x==lastPointerX&&y==lastPointerY)return;lastPointerX=x;lastPointerY=y;
        var hit=pick(x,y,false);if(hit!=null){cursorWorldX=hit.x;cursorWorldZ=hit.z;}
        INPUT.pointer(x/Math.max(1,screen.width)*2-1,y/Math.max(1,screen.height)*2-1);
        if(moved&&!Theme.bool("Reduced Motion",false)&&System.nanoTime()-lastGust>55_000_000L){var p=hit;if(p!=null){INPUT.gust(p.x,p.z,seconds,preferences().interaction.getValue().floatValue());lastGust=System.nanoTime();}}
    }
    public static void press(double x,double y){if(active())INPUT.press(x,y);}
    public static void drag(double x,double y){if(active())INPUT.drag(x,y,preferences().sensitivity.getValue());}
    public static boolean release(double x,double y){
        if(!active()||!INPUT.pressed())return false;
        INPUT.drag(x,y,preferences().sensitivity.getValue());
        if(INPUT.release()&&!Theme.bool("Reduced Motion",false)){var p=pick(x,y,true);if(p!=null)INPUT.ripple(p.x,p.z,seconds,preferences().interaction.getValue().floatValue());}return true;
    }
    private static Vector3f pick(double x,double y,boolean water){
        if(description==null)return null;var screen=Minecraft.getInstance().gui.screen();var f=RENDERER.latest();if(screen==null||f==null)return null;
        var inverse=PanoramaRenderer.projection(screen.width/(float)screen.height).mul(PanoramaRenderer.view(f.yaw(),f.pitch(),false)).invert();
        var far=new Vector3f((float)(x/screen.width*2-1),(float)(1-y/screen.height*2),1);inverse.transformProject(far);var eye=new Vector3f(0,7.2f,25);var direction=far.sub(eye).normalize();
        for(float d=.5f;d<145;d+=.3f){var p=new Vector3f(direction).mul(d).add(eye);int bx=(int)Math.floor(p.x),by=(int)Math.floor(p.y),bz=(int)Math.floor(p.z);var kind=description.occupancy().get(ForestScene.key(bx,by,bz));
            if(water){if(p.y<=.12f&&ForestScene.lake(p.x,p.z))return new Vector3f(p.x,.12f,p.z);if(kind!=null&&kind!=ForestScene.Kind.WATER)return null;}
            else if(kind!=null&&kind!=ForestScene.Kind.WATER)return p;
        }
        return water?null:new Vector3f(direction).mul(22).add(eye);
    }
    public static void resetView(){INPUT.reset();}
    public static void toggleScenic(){if(!active()||!(Minecraft.getInstance().gui.screen() instanceof TitleScreen title))return;scenic=!scenic;INPUT.cancel();title.clearFocus();syncWidgets(title);}
    public static void leaveTitle(){scenic=false;menuFade=0;INPUT.cancel();restoreWidgets();}
    private static void restoreWidgets(){DISABLED.forEach((widget,enabled)->widget.active=enabled);DISABLED.clear();}
    public static void syncWidgets(Screen screen){
        if(!(screen instanceof TitleScreen))return;
        if(!scenic)restoreWidgets();
        for(var child:screen.children())if(child instanceof AbstractWidget w){
            if(w instanceof PanoramaControl control){boolean show=active();w.visible=show;w.active=show&&(!scenic||control.returnControl());if(control.returnControl())w.setAlpha(scenic?.26f:1);}
            else if(scenic){DISABLED.putIfAbsent(w,w.active);w.active=false;}
        }
    }
    public static boolean draw(GuiGraphicsExtractor g){
        if(!active())return false;
        try{
            long now=System.nanoTime();float dt=previous==0?0:Math.clamp((now-previous)*1e-9f,0,.1f);previous=now;
            boolean focused=Minecraft.getInstance().isWindowActive();if(!focused){SOUND.close();dt=0;}else seconds+=dt;
            var p=preferences();boolean reduced=Theme.bool("Reduced Motion",false);boolean nextLow=p.quality.getValue()==PanoramaPreferences.Quality.LOW;
            if(densityTrees!=p.trees.getValue()||densityPlants!=p.plants.getValue()||low!=nextLow){densityTrees=p.trees.getValue();densityPlants=p.plants.getValue();low=nextLow;densityChanged=now;cancelBuild();}
            if(worker==null){worker=Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon(true).name("risecode-panorama-prepare").factory());densityChanged=now-300_000_000L;}
            if(pending==null&&prepared==null&&densityChanged!=0&&now-densityChanged>250_000_000L){
                var resources=PanoramaResources.capture();long id=generation;double trees=densityTrees,plants=densityPlants;boolean simplify=low;
                pending=CompletableFuture.supplyAsync(()->PanoramaResources.prepare(resources,trees,plants,simplify,()->id!=generation||Thread.currentThread().isInterrupted()),worker);densityChanged=0;
            }
            if(pending!=null&&pending.isDone()){prepared=pending.join();pending=null;}
            if(prepared!=null&&focused&&RENDERER.upload(prepared)){description=prepared.scene();prepared=null;}
            syncWidgets(Minecraft.getInstance().gui.screen());
            if(!RENDERER.ready())return false;
            float nextHour=p.time.getValue().floatValue(),nextWind=p.wind.getValue().floatValue(),nextRain=p.weather.getValue().floatValue(),nextFog=p.fog.getValue().floatValue();
            if(lastRender==0){hour=fromHour=targetHour=nextHour;wind=fromWind=targetWind=nextWind;rain=fromRain=targetRain=nextRain;fog=fromFog=targetFog=nextFog;}
            if(targetHour!=nextHour||targetWind!=nextWind||targetRain!=nextRain||targetFog!=nextFog){fromHour=hour;fromWind=wind;fromRain=rain;fromFog=fog;targetHour=nextHour;targetWind=nextWind;targetRain=nextRain;targetFog=nextFog;blend=0;}
            blend=Math.min(3,blend+dt);float t=blend/3;t=t*t*(3-2*t);float diff=((targetHour-fromHour+36)%24)-12;
            hour=(fromHour+diff*t+24)%24;wind=fromWind+(targetWind-fromWind)*t;rain=fromRain+(targetRain-fromRain)*t;fog=fromFog+(targetFog-fromFog)*t;
            if(cycling!=p.cycle.getValue()){cycling=p.cycle.getValue();cycleOffset=0;}
            if(cycling)cycleOffset=(cycleOffset+dt*.02f)%24;
            float actualHour=(hour+cycleOffset)%24;
            float night=Math.clamp((.38f-PanoramaRenderer.daylight(actualHour))/.345f,0,1);INPUT.expire(seconds);
            var input=INPUT.snapshot();float yaw=(float)input.yaw()+(reduced?0:(float)input.pointerX()*.7f),pitch=(float)input.pitch()+(reduced?0:(float)input.pointerY()*.35f);
            var policy=dev.riserecode.performance.PerformancePolicy.frame();
            int particles=reduced||policy.menuParticles()?0:Theme.number("Particle Density",70)<=0?0:Math.min(512,(int)(Theme.number("Particle Density",70)*4+128));
            particles=(int)(particles*policy.particleDensity().multiplier());
            particles=switch(Minecraft.getInstance().options.particles().get()){case ALL->particles;case DECREASED->particles/2;case MINIMAL->0;};
            int fps=p.refresh.getValue().fps();boolean due=lastRender==0||fps==0||now-lastRender>=1_000_000_000L/fps;
            if(focused&&due){var frame=new PanoramaRenderer.Frame(reduced?0:seconds,actualHour,wind,rain,fog,night,Math.clamp(yaw,-60,60),Math.clamp(pitch,-15,15),cursorWorldX,cursorWorldZ,p.interaction.getValue().floatValue(),reduced,particles,input,p.quality.getValue());RENDERER.render(frame);lastRender=now;}
            if(focused)SOUND.update(dt,rain,night,p);
            fade=Math.min(1,fade+dt/.8f);float desired=scenic?1:0;menuFade=reduced?desired:menuFade+Math.clamp(desired-menuFade,-dt/.35f,dt/.35f);
            RENDERER.present(g,reduced?1:fade);return true;
        }catch(RuntimeException|LinkageError failureException){failure="Living Panorama unavailable: "+failureException.getClass().getSimpleName();Client.INSTANCE.getLogger().warn("Living Panorama failed; restoring saved backdrop",failureException);close();failed=true;return false;}
    }
    public static boolean blur(GuiGraphicsExtractor g,double x,double y,double w,double h,double corner,double opacity){
        if(!active()||!RENDERER.ready()||RENDERER.texture()==null)return false;
        var texture=RENDERER.blurred();if(texture==null)return false;
        PanoramaBlur.draw(g,texture,x,y,w,h,corner,opacity);return true;
    }
    private static void cancelBuild(){generation++;if(pending!=null){pending.cancel(false);pending=null;}prepared=null;RENDERER.discardStaging();}
    public static void close(){cancelBuild();if(worker!=null){worker.shutdownNow();worker=null;}SOUND.close();RENDERER.close();description=null;lastPointerX=lastPointerY=Double.NaN;cursorWorldX=cursorWorldZ=1000;INPUT.clear();leaveTitle();previous=lastRender=0;seconds=fade=cycleOffset=0;cycling=false;densityTrees=densityPlants=-1;densityChanged=0;}
    public static void reload(){close();failed=false;failure="";}
    private LivingPanorama(){}
}
