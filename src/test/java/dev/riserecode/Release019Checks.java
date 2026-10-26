package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.ModuleClassification;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.render.BlockHighlightMotion;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.animation.AnimationManager;
import dev.riserecode.ui.animation.ButtonFeedback;
import dev.riserecode.ui.component.ButtonMetrics;
import dev.riserecode.ui.component.SettingsSection;
import dev.riserecode.ui.theme.ModuleAppearance;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.ui.theme.ThemeProfile;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Timing, isolation and persistence regressions without a graphics driver. */
public final class Release019Checks {
    private static int checks;
    private Release019Checks(){}
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static void near(double a,double b,String message){check(Math.abs(a-b)<1e-8,message+": "+a+" / "+b);}
    private static void number(ThemeManager manager,String name,double value){((DoubleSetting)manager.getSetting(name)).setValue(value);}
    public static void main(String[] args)throws Exception{
        var motion=new BlockHighlightMotion();AABB cube=new AABB(0,0,0,1,1,1),next=new AABB(4,1,-3,5,2,-2);long start=1_000_000_000L;
        motion.target("a",List.of(cube),start,.2,false,.18);check(motion.sample(start).parts().size()==1,"first target is visible without movement from origin");
        motion.target("b",List.of(next),start,.2,false,.18);var mid=motion.sample(start+100_000_000L);check(mid.bounds().minX>0&&mid.bounds().minX<4,"switch has an intermediate position");
        double position=mid.bounds().minX;motion.target("c",List.of(new AABB(-3,2,0,-2,3,1)),start+100_000_000L,.2,false,.18);near(motion.sample(start+100_000_000L).bounds().minX,position,"rapid retarget preserves displayed position");
        near(motion.sample(start+500_000_000L).bounds().minX,-3,"transition reaches exact new block");
        var slab=List.of(new AABB(2,0,0,3,.5,1));motion.target("slab",slab,start+600_000_000L,.2,true,.2);near(motion.sample(start+600_000_000L).scale(),0,"pop starts inside the block");
        var popping=motion.sample(start+700_000_000L);check(popping.scale()>0&&popping.scale()<1,"pop grows during smooth travel");
        var settled=motion.sample(start+900_000_000L);check(settled.parts().size()==1&&settled.parts().getFirst().bounds().equals(slab.getFirst()),"slab shape and exact destination preserved");
        for(int i=0;i<1000;i++){motion.target(i,List.of(cube.move(i,0,0)),start+1_000_000_000L+i*1000,.2,false,.2);}check(motion.sample(start+1_001_000_000L).parts().size()<=129,"rapid targeting keeps geometry bounded");
        motion.clear();check(motion.isEmpty()&&motion.sample(start)==null,"target loss clears geometry");motion.target("instant",slab,start,0,true,0);near(motion.sample(start).scale(),1,"zero pop duration is immediate");check(motion.immediate().parts().getFirst().bounds().equals(slab.getFirst()),"reduced-motion snapshot uses target shape");
        var defaultQuad=new dev.riserecode.event.Render3DEvent.Quad(Vec3.ZERO,new Vec3(1,0,0),new Vec3(1,1,0),new Vec3(0,1,0),0xFFFFFFFF);
        check(defaultQuad.visibleOnly(),"existing quad callers retain depth testing");
        var popQuad=new dev.riserecode.event.Render3DEvent.Quad(defaultQuad.a(),defaultQuad.b(),defaultQuad.c(),defaultQuad.d(),-1,-1,-1,-1,false);
        check(!popQuad.visibleOnly(),"pop fill can remain visible inside the target block");
        for(double gap:new double[]{0,10,60,95,100}){
            var segments=BlockHighlightMotion.edge(Vec3.ZERO,new Vec3(2,0,0),gap);double length=segments.stream().mapToDouble(s->s[0].distanceTo(s[1])).sum();near(length,2*(1-gap/100),"corner gap is proportional to edge length");
            if(gap>0&&gap<100){near(segments.get(0)[0].x,0,"first corner remains anchored");near(segments.get(1)[1].x,2,"second corner remains anchored");}
        }
        var manager=new ThemeManager(Files.createTempDirectory("risecode-019-"),LoggerFactory.getLogger("019-check"));Theme.bind(manager);Animation.configureChannels(manager);Animation.configureUi(()->manager.number("Animation Speed"),()->manager.choice("Easing Curve",MathUtil.Easing.class));
        var a=manager.module("client.module.a");var b=manager.module("client.module.b");a.bind(new dev.riserecode.module.modules.hud.FpsModule(new EventBus(),null));b.bind(new dev.riserecode.module.modules.hud.FpsModule(new EventBus(),null));check(a.global.getValue(),"new module inherits appearance");check(a.controls().stream().filter(c->c.setting()!=a.global).noneMatch(c->c.setting().isVisible()),"inherited controls are hidden");
        number(manager,"Font Scale",1.2);a.global.setValue(false);near(((DoubleSetting)a.get("Font Scale")).getValue(),1.2,"first override starts from current globals");((DoubleSetting)a.get("Font Scale")).setValue(1.6);
        ModuleAppearance.with("client.module.a",()->{near(manager.number("Font Scale"),1.6,"module reads own font scale");ModuleAppearance.with("client.module.b",()->near(manager.number("Font Scale"),1.2,"other module stays global"));near(manager.number("Font Scale"),1.6,"nested scope restores parent");});near(manager.number("Font Scale"),1.2,"scope restores globals");
        a.global.setValue(true);number(manager,"Font Scale",1.3);ModuleAppearance.with("client.module.a",()->near(manager.number("Font Scale"),1.3,"inheritance follows later edits"));a.global.setValue(false);near(((DoubleSetting)a.get("Font Scale")).getValue(),1.6,"custom edits survive toggling globals");
        check(a.get("Window Width")==null&&a.get("Sidebar Color")==null,"whole-window options remain global");check(a.get("Background File")!=null&&a.get("Click Sound")!=null&&a.get("HUD Background")!=null,"backgrounds sounds and HUD controls are available");
        int originalAccent=manager.color("Primary Accent");Theme.setAccent(0xFFAA3344);int blended=Theme.accent();check(ModuleAppearance.call("client.module.b",Theme::accent)==blended,"global-mode modules share the live global accent transition");Theme.setAccent(originalAccent);
        int sharedAccent=manager.color("Primary Accent");ThemeManager.assign(a.get("Appearance Preset"),ThemeManager.Preset.ROSE);check(manager.color("Primary Accent")==sharedAccent,"local preset cannot change shared colors");check(((dev.riserecode.setting.ColorSetting)a.get("Primary Accent")).getValue()==ThemeManager.palette(ThemeManager.Preset.ROSE)[0],"local preset updates local palette");
        var snapshot=manager.snapshot();manager.apply(snapshot);check(manager.module("client.module.a")==a,"profile load preserves live editor references");
        var loaded=new ThemeManager(Files.createTempDirectory("risecode-019-load-"),LoggerFactory.getLogger("019-check"));loaded.apply(snapshot);near(((DoubleSetting)loaded.module("client.module.a").get("Font Scale")).getValue(),1.6,"appearance overrides round-trip");check(!loaded.module("client.module.a").global.getValue(),"inheritance mode round-trips");
        JsonObject invalid=snapshot.settings();invalid.addProperty("Font Scale",1.9);invalid.getAsJsonObject("Module Overrides").getAsJsonObject("client.module.a").addProperty("Font Scale",Double.NaN);
        try{manager.apply(new ThemeProfile(1,invalid));throw new AssertionError("invalid module accepted");}catch(IllegalArgumentException expected){checks++;}near(manager.number("Font Scale"),1.3,"invalid override cannot partially change globals");
        var legacy=new dev.riserecode.module.Module("Legacy","",dev.riserecode.module.Category.RENDER,new EventBus()){};legacy.popOverride.setValue(true);legacy.popTiming.setValue(.75);legacy.popStrength.setValue(2.0);manager.module(legacy).migrate(legacy);near(legacy.popDuration(),manager.number("Opening Duration"),"inactive legacy card animations do not override global GUI styling");
        manager.module(legacy).load(new ModuleAppearance(manager).json());near(legacy.popDuration(),manager.number("Opening Duration"),"reset appearance does not re-import legacy settings");
        var migrated=new ThemeManager(Files.createTempDirectory("risecode-019-migrated-"),LoggerFactory.getLogger("019-check"));migrated.apply(manager.snapshot());migrated.module(legacy).migrate(legacy);check(migrated.module(legacy).global.getValue(),"migration marker survives profile round-trip");
        number(manager,"HUD Entrance Duration",2);((DoubleSetting)a.get("HUD Entrance Duration")).setValue(.4);((DoubleSetting)a.get("Animation Speed")).setValue(1.0);
        AnimationManager.ui().clear();Animation animated=ModuleAppearance.call("client.module.a",()->Animation.ui(ThemeManager.Channel.HUD_ENTRANCE,0,.2,MathUtil.Easing.OUT_QUART).animateTo(1));
        AnimationManager.ui().update(.4);near(animated.getValue(),1,"central animation scheduler retains module duration");
        var spring=new ButtonFeedback();spring.pressAt(1);spring.advance(2,.32,.28);check(spring.isHeld()&&spring.displacement()>.999,"held press stays compressed past ordinary timer");spring.releaseAt(1);spring.advance(2,.32,.28);near(spring.displacement(),0,"mouse-up releases spring");
        var fine=new ButtonFeedback();var coarse=new ButtonFeedback();fine.pressAt(1);coarse.pressAt(1);for(int i=0;i<100;i++){fine.advance(.001,.32,.28);}coarse.advance(.1,.32,.28);near(fine.displacement(),coarse.displacement(),"held response independent of frame rate");ButtonFeedback.cancelAll();check(!fine.isHeld()&&!coarse.isHeld(),"focus or screen cancellation clears held state");
        var section=new SettingsSection("test:019","Settings",()->{});check(!section.isExpanded(),"new sections start collapsed");section.setExpanded(true);section.bodyHeight(200);AnimationManager.ui().update(1);near(section.bodyHeight(200),200,"section expansion reaches content height");section.setExpanded(false);section.bodyHeight(200);AnimationManager.ui().update(1);near(section.bodyHeight(200),0,"collapsed section removes content height");
        near(ButtonMetrics.HEIGHT,17,"button height matches tab height");check(ButtonMetrics.width(null,"Long label")>ButtonMetrics.width(null,"Go"),"buttons fit their labels");
        var noFall=new NoFallModule(new EventBus(),null);for(var mode:NoFallModule.Mode.values()){noFall.getMode().setValue(mode);check(noFall.classification()==ModuleClassification.GHOST,"Clutch uses the requested Ghost badge in "+mode);}
        Theme.bind(null);AnimationManager.ui().clear();System.out.println("Release 0.19 checks passed ("+checks+" assertions).");
    }
}
