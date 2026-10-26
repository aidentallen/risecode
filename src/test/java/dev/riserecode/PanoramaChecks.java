package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.ui.panorama.*;
import dev.riserecode.ui.theme.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import org.slf4j.LoggerFactory;

/** Behavioral contracts independent of GPU/window availability. */
public final class PanoramaChecks {
    private static int checks;
    private static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("panorama-checks-");
        try{
            var manager=new ThemeManager(dir,LoggerFactory.getLogger(PanoramaChecks.class));var p=manager.panorama();
            check(!p.enabled.getValue()&&p.volume.getValue()==.15&&!p.cycle.getValue(),"opt-in quiet defaults");
            var old=new JsonObject();old.addProperty("Background","SPACE");manager.apply(new ThemeProfile(1,old));
            check(!p.enabled.getValue()&&manager.choice("Background",ThemeManager.Background.class)==ThemeManager.Background.SPACE,"old appearance retained");
            p.enabled.setValue(true);p.trees.setValue(.91);p.plants.setValue(.89);p.volume.setValue(.24);p.quality.setValue(PanoramaPreferences.Quality.CINEMATIC);p.sensitivity.setValue(2.0);p.cycle.setValue(true);
            for(var a:PanoramaPreferences.Atmosphere.values()){
                if(a==PanoramaPreferences.Atmosphere.CUSTOM)continue;p.applyPreset(a);
                check(p.atmosphere.getValue()==a,"preset selected");check(p.trees.getValue()==.91&&p.plants.getValue()==.89&&p.volume.getValue()==.24&&p.sensitivity.getValue()==2&&p.quality.getValue()==PanoramaPreferences.Quality.CINEMATIC&&p.cycle.getValue(),"preset preserves independent controls");
                p.wind.setValue(p.wind.getValue()==.9?.8:.9);check(p.atmosphere.getValue()==PanoramaPreferences.Atmosphere.CUSTOM,"environment edit selects Custom");
                var values=manager.snapshot();p.applyPreset(PanoramaPreferences.Atmosphere.SUNLIT_DAY);manager.apply(values);
                check(p.atmosphere.getValue()==PanoramaPreferences.Atmosphere.CUSTOM&&p.wind.getValue()==.9,"Custom round trip survives preset callbacks");
            }
            p.applyPreset(PanoramaPreferences.Atmosphere.GOLDEN_HOUR);manager.save();var loaded=new ThemeManager(dir,LoggerFactory.getLogger(PanoramaChecks.class));loaded.load();
            check(loaded.panorama().atmosphere.getValue()==PanoramaPreferences.Atmosphere.GOLDEN_HOUR&&loaded.panorama().time.getValue()==17.7,"preset persistence atomic");
            p.enabled.setValue(false);check(manager.choice("Background",ThemeManager.Background.class)==ThemeManager.Background.SPACE,"toggle preserves saved background");
            var scene=ForestScene.create(.65,.65,false,()->false);var repeat=ForestScene.create(.65,.65,false,()->false);
            check(scene.equals(repeat),"seeded placements deterministic");
            var bare=ForestScene.create(0,0,false,()->false);var full=ForestScene.create(1,1,false,()->false);
            for(int x=-8;x<9;x++)for(int z=-27;z<3;z++)check(full.occupancy().get(ForestScene.key(x,0,z))==ForestScene.Kind.WATER,"lake opening retained at maximum density");
            for(int y=ForestScene.ground(-13,12)+1;y<ForestScene.ground(-13,12)+10;y++)check(bare.occupancy().get(ForestScene.key(-13,y,12))==full.occupancy().get(ForestScene.key(-13,y,12)),"principal trunk density independent");
            check(full.blocks().size()>bare.blocks().size(),"density adds stable details");
            try{ForestScene.create(1,1,false,()->true);throw new AssertionError("cancel ignored");}catch(CancellationException expected){checks++;}
            var input=new PanoramaInteraction();input.press(10,10);input.drag(13,12,1);check(input.release(),"below threshold remains click");
            input.press(0,0);input.drag(10,0,1);input.drag(0,0,1);check(!input.release(),"returning drag never clicks");
            input.press(0,0);input.drag(100000,-100000,3);var bounds=input.snapshot();check(bounds.yaw()==60&&bounds.pitch()==-15,"camera hard limits");input.reset();check(input.snapshot().yaw()==0&&input.snapshot().pitch()==0,"reset camera");
            for(int i=0;i<100;i++){input.gust(i,0,1,1);input.ripple(i,0,1,1);}var snapshot=input.snapshot();check(snapshot.gusts().size()==8&&snapshot.ripples().size()==12,"bounded impulse pools");
            input.expire(6);check(input.snapshot().gusts().isEmpty()&&input.snapshot().ripples().isEmpty(),"impulses expire");check(snapshot.gusts().size()==8&&snapshot.ripples().size()==12,"immutable frame survives mutation");
            input.press(1,1);input.clear();check(!input.pressed()&&!input.release(),"departure cancels gesture");
            System.out.println("Living Panorama checks passed ("+checks+" assertions)");
        }finally{try(var files=Files.walk(dir)){for(var file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}}
    }
    private PanoramaChecks(){}
}
