package dev.riserecode;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.modules.*;
import dev.riserecode.render.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.theme.*;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import org.slf4j.LoggerFactory;

/** Union contours, exposed fills, retarget continuity and consumer-specific appearance contracts. */
public final class Release023Checks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var shape=Shapes.or(Shapes.box(0,0,0,1,.5,1),Shapes.box(0,.5,.5,1,1,1));
        var mesh=HighlightMesh.selection(shape);
        check(mesh.edges().size()==18,"L-shaped union has 18 continuous edges instead of 24 box edges");
        check(HighlightMesh.selection(shape)==mesh,"unchanged shape uses cached local mesh");
        check(new HashSet<>(mesh.edges()).size()==mesh.edges().size(),"no duplicate edges");
        double area=0;for(var face:mesh.faces()){var a=mesh.vertices().get(face.a());var b=mesh.vertices().get(face.b());var d=mesh.vertices().get(face.d());area+=b.subtract(a).cross(d.subtract(a)).length();}
        check(Math.abs(area-5.548096)<1e-8,"fill contains only the exposed surfaces of the padded union");
        check(mesh.vertices().stream().anyMatch(v->Math.abs(v.y-.502)<1e-9),"concave upward faces are padded upward instead of shrinking into the block");
        check(HighlightMesh.selection(Shapes.empty()).vertices().isEmpty(),"empty selections emit no geometry");
        var box=HighlightMesh.box(BlockHighlightGeometry.corners(new AABB(0,0,0,1,1,1)));
        var motion=new BlockHighlightGeometry();long time=1_000_000_000;
        motion.targetMeshes("a","box",List.of(box),time,.2,false,false,.2);
        var complex=mesh.map(p->p.add(4,0,0));motion.targetMeshes("b","L",List.of(complex),time,.2,false,false,.2);
        var mid=motion.targetMeshes("b","L",List.of(complex),time+100_000_000,.2,false,false,.2);
        check(mid.stream().flatMap(p->p.vertices().stream()).mapToDouble(p->p.x).min().orElseThrow()>3,"different topology travels rather than fading in place");
        var rapid=new BlockHighlightGeometry();rapid.targetMeshes("a",0,List.of(box),time,.2,false,false,.2);
        for(int i=1;i<20;i++)rapid.targetMeshes(i,i,List.of(mesh.map(p->p.add(6,0,0))),time+i*16_000_000,.2,false,false,.2);
        var result=rapid.targetMeshes(20,20,List.of(complex),time+320_000_000,.2,false,false,.2);
        check(result.stream().flatMap(p->p.vertices().stream()).mapToDouble(p->p.x).min().orElseThrow()>1,"a target change every frame cannot stall movement");
        check(result.size()<=129,"retarget history is bounded");
        var theme=new ThemeManager(Files.createTempDirectory("appearance023"),LoggerFactory.getLogger("023"));Theme.bind(theme);
        var bus=new EventBus();var blocks=new BlockOverlayModule(bus,null);var hitboxes=new HitboxesModule(bus,null,blocks);
        var crosshair=new CrosshairModule(bus);var clutch=new NoFallModule(bus,null);var map=new dev.riserecode.module.modules.hud.MinimapModule(bus,null);
        for(var module:List.of(blocks,hitboxes,crosshair,clutch,map,new WaypointsModule(bus,null),new dev.riserecode.module.modules.hud.MemoryUsageModule(bus,null))){
            check(ModulePresentation.reviewed(module),module.getName()+" was explicitly reviewed");var local=theme.module(module);
            if(module==crosshair||module==clutch)check(!local.hasControls(),"non-panel module has no unused appearance section");
            if(module==blocks||module==hitboxes)check(local.controls().stream().noneMatch(c->c.group().equals("HUD")),"world outlines have no HUD settings");
        }
        var local=theme.module(map);local.global.setValue(false);
        check(local.controls().stream().noneMatch(c->c.setting().getName().equals("HUD Background")),"Minimap never advertises the unused generic HUD panel");
        ThemeManager.assign(local.get("Minimap Background"),ThemeManager.MapBackground.SOLID);
        check(local.get("Map Solid Color").isVisible()&&!local.get("Minimap Image").isVisible(),"map controls follow the actual background mode");
        var memory=new dev.riserecode.module.modules.hud.MemoryUsageModule(bus,null);var appearance=theme.module(memory);appearance.global.setValue(false);
        var show=memory.getSettings().stream().filter(s->s.getName().equals("ShowBar")).findFirst().orElseThrow();ThemeManager.assign(show,false);
        check(!appearance.get("HUD Bar Track Color").isVisible(),"disabled memory bar hides bar-only controls");
        ThemeManager.assign(appearance.get("Shadow Blur"),3.0);var json=appearance.json();appearance.load(json);
        check(((DoubleSetting)appearance.get("Shadow Blur")).getValue()==3,"dormant legacy overrides survive round trip");
        Theme.bind(null);System.out.println("Release 0.23 checks passed ("+checks+" assertions).");
    }
}
