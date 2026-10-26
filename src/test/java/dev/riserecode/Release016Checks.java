package dev.riserecode;

import dev.riserecode.map.*;
import dev.riserecode.ui.animation.ButtonFeedback;
import java.nio.file.*;
import java.util.*;

/** Regression checks for layer isolation, old exploration and bounded death history. */
public final class Release016Checks {
    private static int checks;
    private Release016Checks(){ }
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    public static void main(String[] args)throws Exception{
        var surface=new MapService.Key("minecraft:overworld",-3,-5,0);
        var cave=new MapService.Key("minecraft:overworld",-3,-5,0,MapLayerPolicy.Layer.CAVE,-16);
        check(!surface.equals(cave),"Surface and cave keys isolated");check(!cave.equals(new MapService.Key(cave.dimension(),-3,-5,0,MapLayerPolicy.Layer.CAVE,-8)),"Stacked altitude keys isolated");
        check(cave.parent().x()==-1&&cave.parent().z()==-2&&cave.parent().altitude()==-16&&cave.parent().layer()==MapLayerPolicy.Layer.CAVE,"Negative parent coordinates preserve layer");
        var policy=new MapLayerPolicy();for(int i=0;i<11;i++){policy.update(true,63);}check(policy.layer(MapLayerPolicy.Mode.AUTO)==MapLayerPolicy.Layer.SURFACE,"Brief overhang ignored");policy.update(true,63);check(policy.layer(MapLayerPolicy.Mode.AUTO)==MapLayerPolicy.Layer.CAVE,"Persistent roof enables cave");
        for(int i=0;i<23;i++){policy.update(false,63);}check(policy.layer(MapLayerPolicy.Mode.AUTO)==MapLayerPolicy.Layer.CAVE,"Cave entrance exit hysteresis");policy.update(false,63);check(policy.layer(MapLayerPolicy.Mode.AUTO)==MapLayerPolicy.Layer.SURFACE,"Sustained sky restores surface");
        check(policy.layer(MapLayerPolicy.Mode.CAVE)==MapLayerPolicy.Layer.CAVE&&policy.layer(MapLayerPolicy.Mode.SURFACE)==MapLayerPolicy.Layer.SURFACE,"Manual layer overrides automatic decision");
        check(MapLayerPolicy.altitude(64,63)==64&&MapLayerPolicy.altitude(64,67)==64,"Altitude boundary hysteresis");check(MapLayerPolicy.altitude(64,60)==60&&MapLayerPolicy.altitude(64,75)==75,"Altitude changes beyond hysteresis band");
        short[] blocks=new short[256],heights=new short[256],edges=new short[32];int[] tints=new int[256],colors=new int[256],detail=new int[4096];byte[] water=new byte[256];Arrays.fill(heights,(short)64);Arrays.fill(edges,(short)63);Arrays.fill(tints,0xFFFFFF);Arrays.fill(colors,0xFF889999);Arrays.fill(detail,0x123456);colors[20]=0;
        var tile=new SurfaceTile(-3,-5,List.of("{\"Name\":\"minecraft:stone\"}"),blocks,heights,tints,water,colors,detail,edges);
        check(tile.slope(0)==3&&tile.slope(17)==0,"Neighbor shading crosses chunk boundary");check(tile.shaded().colors()[20]==0,"Unknown cave columns remain transparent");
        Path folder=Files.createTempDirectory("risecode-016-");Path path=folder.resolve("tile.rsm");tile.write(path);var loaded=SurfaceTile.read(path);check(loaded.detailed()&&loaded.detail(0)==0x123456&&loaded.slope(0)==3,"Texture detail and edge heights persist");
        byte[] old=Files.readAllBytes(path);old[3]=0x31;Files.write(folder.resolve("old.rsm"),old);var migrated=SurfaceTile.read(folder.resolve("old.rsm"));check(!migrated.detailed()&&migrated.x()==-3&&migrated.height(0)==64,"Version one surface exploration remains readable");
        check(tile.bytes()>4096*4&&tile.bytes()<MapService.CPU_LIMIT,"CPU accounting includes detailed pixels");check(MapTextures.GPU_LIMIT==128L*1024*1024,"GPU byte limit");
        var store=new WaypointStore();long time=1_000_000;
        for(int i=0;i<1000;i++){store.death(-i,64,i,"minecraft:overworld",time+i);}
        int archived=store.pendingArchives().values().stream().mapToInt(page->page.getAsJsonArray("waypoints").size()).sum();
        check(archived+store.all().size()==1000,"Every death retained across pages");check(store.all().size()<=256&&store.pendingArchives().values().stream().allMatch(page->page.getAsJsonArray("waypoints").size()==128),"Death pages bound resident history");
        var latest=store.all().getLast();check(store.latest(latest)&&latest.timestamp()==time+999,"Latest death retains timestamp and identity");store.reached(latest.x(),latest.y(),latest.z(),latest.dimension());check(store.all().getLast().reached()&&!store.all().getLast().tracked(),"Reached deaths are retained and untracked");
        var restored=new WaypointStore();restored.load(store.snapshot());check(restored.all().equals(store.all())&&restored.archivePages()==store.archivePages(),"Recent death state and archive index round trip");
        var oldWaypoints=new com.google.gson.JsonObject();oldWaypoints.addProperty("version",1);var values=store.snapshot().getAsJsonArray("waypoints");for(var value:values){value.getAsJsonObject().remove("timestamp");value.getAsJsonObject().remove("reached");}oldWaypoints.add("waypoints",values);restored.load(oldWaypoints);check(restored.all().getLast().timestamp()==0,"Legacy waypoint migration is non-destructive");
        check(ButtonFeedback.response(0,1)==0&&ButtonFeedback.response(1,1)==0,"Spring rests at endpoints");boolean rebound=false;for(int i=0;i<=100;i++){double response=ButtonFeedback.response(i/100.0,.3);check(Double.isFinite(response)&&Math.abs(response)<=1.01,"Spring bounded");rebound|=response<0;}check(rebound,"Press rebounds after compression");
        var themes=new dev.riserecode.ui.theme.ThemeManager(folder.resolve("appearance"),org.slf4j.LoggerFactory.getLogger(Release016Checks.class));themes.load();themes.bind(new dev.riserecode.event.EventBus());
        ((dev.riserecode.setting.DoubleSetting)themes.getSetting("Opening Duration")).setValue(.4);
        ((dev.riserecode.setting.BooleanSetting)themes.getSetting("Dialogs Animation Override")).setValue(true);
        ((dev.riserecode.setting.DoubleSetting)themes.getSetting("Dialogs Pop Duration")).setValue(.75);
        check(dev.riserecode.ui.animation.PopIn.windowDuration("Dialogs")==.75&&dev.riserecode.ui.animation.PopIn.windowDuration("Client")==.4,"Window overrides are independent of global timing");
        @SuppressWarnings("unchecked") var easing=(dev.riserecode.setting.EnumSetting<dev.riserecode.util.MathUtil.Easing>)themes.getSetting("Opening Easing");easing.setValue(dev.riserecode.util.MathUtil.Easing.LINEAR);
        check(dev.riserecode.ui.animation.PopIn.progress(.2,.4)==.5,"Shared entrances honor the configured global easing");
        var spring = new ButtonFeedback(); spring.activateAt(1); spring.advance(.04,.32,.28);
        double position=spring.displacement(),velocity=spring.velocity();spring.activateAt(1);
        check(spring.displacement()==position && spring.velocity()==velocity,"Repeated presses preserve spring position and velocity");
        var coarse=new ButtonFeedback();var fine=new ButtonFeedback();coarse.activateAt(1);fine.activateAt(1);
        coarse.advance(.137,.32,.28);for(int i=0;i<137;i++){fine.advance(.001,.32,.28);}
        check(Math.abs(coarse.displacement()-fine.displacement())<1e-10&&Math.abs(coarse.velocity()-fine.velocity())<1e-9,"Spring is independent of frame rate across release boundary");
        spring.advance(4,.32,.28);check(spring.displacement()==0&&spring.velocity()==0,"Spring settles exactly without an endpoint jump");
        spring.activateAt(1);spring.advance(.04,.32,0);spring.advance(2,.32,0);check(spring.displacement()==0,"Zero rebound settles without oscillation");
        spring.activateAt(1);spring.advance(.04,.32,.28);spring.advance(.01,0,.28);check(spring.displacement()==0&&spring.velocity()==0,"Zero duration resets spring state");
        double boundary=.32*.22;var continuous=new ButtonFeedback();continuous.activateAt(1);continuous.advance(boundary-1e-8,.32,.28);
        position=continuous.displacement();velocity=continuous.velocity();continuous.advance(2e-8,.32,.28);
        check(Math.abs(continuous.displacement()-position)<1e-5&&Math.abs(continuous.velocity()-velocity)<.001,"Compression and rebound meet continuously in position and velocity");
        ((dev.riserecode.setting.DoubleSetting)themes.getSetting("Press Strength")).setValue(0.0);spring.activateAt(1);spring.advance(.04,.32,.28);check(spring.scale()==1&&spring.displacement()==0,"Zero strength clears active feedback");
        ((dev.riserecode.setting.DoubleSetting)themes.getSetting("Press Strength")).setValue(1.0);
        ((dev.riserecode.setting.BooleanSetting)themes.getSetting("Reduced Motion")).setValue(true);
        spring.activateAt(1);spring.advance(.04,.32,.28);check(spring.scale()==1&&spring.displacement()==0,"Reduced Motion clears active feedback");
        var animation=dev.riserecode.ui.animation.Animation.ui(0,1,dev.riserecode.util.MathUtil.Easing.LINEAR).timing(()->9);animation.animateTo(1);animation.update(.001);
        check(animation.getValue()==1&&dev.riserecode.ui.animation.PopIn.windowScale("Dialogs",0)==1,"Reduced Motion wins over independent animation overrides");themes.close();
        var full=new WaypointStore();for(int i=0;i<4096;i++){full.add("Point",i,64,0,"minecraft:overworld");}full.death(0,64,0,"minecraft:overworld");
        check(full.all().size()==4097&&full.all().getLast().death(),"Normal waypoint capacity cannot prevent recording a death");
        System.out.println("Release 0.16 checks passed ("+checks+" assertions).");
    }
}
