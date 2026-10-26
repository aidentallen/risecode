package dev.riserecode.module.modules;
import dev.riserecode.Client;
import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.*;
import dev.riserecode.render.WorldLabels;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public final class WaypointsModule extends Module {
    public final BooleanSetting labels=register(new BooleanSetting("Labels","Show destination names",true));
    public final BooleanSetting distance=register(new BooleanSetting("Distance","Show distance in meters",true));
    public final BooleanSetting beams=register(new BooleanSetting("Beams","Show vertical destination beams",false));
    public final BooleanSetting through=register(new BooleanSetting("Through Terrain","Show labels behind terrain",true));
    public final DoubleSetting scale=register(DoubleSetting.extended("Scale","Label size",1,0.5,3));
    public final DoubleSetting range=register(DoubleSetting.extended("Range","Maximum overlay distance",4096,64,16000));
    private final Minecraft mc;
    public WaypointsModule(EventBus bus,Minecraft mc){super("Waypoints","In-world destinations from the shared waypoint manager.",Category.WORLD,bus);this.mc=mc;}
    @Override protected void onEnable(){listen(Render3DEvent.class,e->{var maps=Client.INSTANCE.getMaps();if(mc.player==null||maps==null){return;}Vec3 eye=e.getContext().camera().position();for(var p:maps.waypoints().all()){if(!p.visible()||!p.dimension().equals(maps.dimension())){continue;}Vec3 position=new Vec3(p.x()+0.5,p.y()+1,p.z()+0.5);double meters=mc.player.position().distanceTo(position);if(meters>range.getValue()){continue;}if(beams.getValue()){e.addLine(position,position.add(0,80,0),p.color(),p.color(),2,!through.getValue());}if(labels.getValue()){Vec3 draw=eye.add(position.subtract(eye).normalize().scale(Math.min(64,eye.distanceTo(position))));WorldLabels.label(e,draw,Component.literal(p.name()+(distance.getValue()?" "+Math.round(meters)+"m":"")+(p.tracked()?" ◇":"")),p.color(),scale.getValue()*Math.clamp(eye.distanceTo(position)/12,0.05,1),through.getValue(),List.of());}}});}
}
