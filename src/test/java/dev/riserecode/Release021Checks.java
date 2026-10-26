package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleClassification;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.module.modules.BlockAnimationsModule;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.render.BlockHighlightGeometry;
import dev.riserecode.render.placement.PlacementMotion;
import dev.riserecode.util.MathUtil;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;

/** Acquisition semantics, oriented geometry, exact pose endpoints and backwards-compatible identity. */
public final class Release021Checks {
    private static int checks;
    private Release021Checks() { }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var g=new BlockHighlightGeometry();var a=List.of(BlockHighlightGeometry.corners(new AABB(0,0,0,1,1,1)));
        long now=1_000_000_000;
        var first=g.target("a",a,a,now,.2,true,false,.2);
        check(first.getFirst().vertices().getFirst().distanceTo(new Vec3(.5,.5,.5))<1e-9,"first acquisition starts at center");
        var mid=g.target("a",a,a,now+100_000_000,.2,true,false,.2);
        check(mid.getFirst().vertices().getFirst().x<.1,"holding target advances the entrance pop without restarting");
        var b=List.of(BlockHighlightGeometry.corners(new AABB(3,0,0,4,1,1)));
        var switched=g.target("b",b,b,now+100_000_000,.2,true,false,.2);
        check(switched.getFirst().vertices().equals(mid.getFirst().vertices()),"switch starts from displayed corners without a new pop");
        var arrived=g.target("b",b,b,now+400_000_000,.2,true,false,.2);
        check(arrived.getFirst().vertices().equals(b.getFirst()),"switch settles at exact destination");
        g.clear();check(g.target("b",b,b,now,.2,true,false,.2).getFirst().vertices().getFirst().x==3.5,"look away re-arms pop");
        g.target("b",b,b,now+300_000_000,0,true,false,.2);
        check(g.target("a",a,a,now+400_000_000,.2,true,true,.2).getFirst().vertices().getFirst().x==.5,"Pop On Switch restores per-block pops");
        var module=new BlockAnimationsModule(new EventBus(),null);
        for(var curve:MathUtil.Easing.values())for(var pivot:PlacementMotion.Pivot.values()){
            var s=new PlacementMotion.Settings(.2,curve,1,.2,.7,.2,.1,.3,25,15,-30,pivot);
            check(PlacementMotion.pose(s,1,.7).equals(new Matrix4f(),.000001f),"every curve/pivot reaches identity");
            check(PlacementMotion.pose(s,0,.7).isFinite(),"initial pose remains finite");
        }
        for(var preset:BlockAnimationsModule.Preset.values()){
            module.preset.setValue(preset);check(module.motionSettings().duration()==.2,"preset retains timing");
            check(PlacementMotion.pose(module.motionSettings(),.5,1).isFinite(),"preset midpoint finite");
        }
        module.scale.setValue(.42);check(module.preset.getValue()==BlockAnimationsModule.Preset.CUSTOM,"editing transform selects Custom");
        var clutch=new NoFallModule(new EventBus(),null);
        check(clutch.getName().equals("Clutch")&&clutch.capabilityId().equals("client.module.nofall"),"Clutch keeps stable identity");
        for(var mode:NoFallModule.Mode.values()){clutch.getMode().setValue(mode);check(clutch.classification()==ModuleClassification.GHOST,"Ghost badge for "+mode);}
        var bus=new EventBus();var manager=new ModuleManager(bus,LoggerFactory.getLogger("021-checks"));
        var renamed=manager.register(new Module("Clutch","",Category.PLAYER,bus,"NoFall"){});
        check(manager.getByName("NoFall").orElseThrow()==renamed&&manager.getByName("Clutch").orElseThrow()==renamed,"old and new lookup aliases");
        var directory=Files.createTempDirectory("risecode-clutch-migration");
        var config=new ConfigManager(directory,manager,()->true,value->{},LoggerFactory.getLogger("021-config"));
        var document=new JsonObject();var modules=new JsonObject();var old=new JsonObject();old.addProperty("enabled",true);old.addProperty("keybind",71);old.addProperty("bindMode","HOLD");modules.add("NoFall",old);document.add("modules",modules);
        config.applyProfile(document);check(renamed.isEnabled()&&renamed.getKeybind()==71&&renamed.getBindMode()==Module.BindMode.HOLD,"legacy profile migrates enabled state and binding");
        check(config.profileSnapshot().getAsJsonObject("modules").has("Clutch")&&!config.profileSnapshot().getAsJsonObject("modules").has("NoFall"),"new saves use canonical name");
        var current=old.deepCopy();current.addProperty("keybind",72);modules.add("Clutch",current);config.applyProfile(document);check(renamed.getKeybind()==72,"canonical entry wins conflicts");
        renamed.setEnabled(false);config.save();
        var rotated=PlacementMotion.pose(new PlacementMotion.Settings(.2,MathUtil.Easing.OUT_QUART,1,1,1,0,0,0,0,45,0,PlacementMotion.Pivot.CENTER),0,0);
        var corners=a.getFirst().stream().map(p->{var v=rotated.transformPosition(new Vector3f((float)p.x,(float)p.y,(float)p.z));return new Vec3(v.x,v.y,v.z);}).toList();
        g.clear();var result=g.target("rotated",a,List.of(corners),now,0,false,false,0);
        check(result.getFirst().vertices().equals(corners),"rotated outlines preserve actual vertices instead of expanded AABBs");
        System.out.println("Release 0.21 checks passed ("+checks+" assertions).");
    }
}
