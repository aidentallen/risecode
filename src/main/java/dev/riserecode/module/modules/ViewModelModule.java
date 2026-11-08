package dev.riserecode.module.modules;
import com.mojang.math.Axis;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.HeldItemTransformEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;
import net.minecraft.world.item.ItemDisplayContext;
/** Transforms are relative to the vanilla item pose; physical left/right follows handedness. */
public final class ViewModelModule extends Module {
    private record Transform(DoubleSetting x,DoubleSetting y,DoubleSetting z,DoubleSetting rx,DoubleSetting ry,DoubleSetting rz,DoubleSetting scale) { }
    private final Transform left,right;
    private final DoubleSetting shieldHeight=register(DoubleSetting.extended("Shield Height","Raises or lowers a held shield on screen",0,-1,1));
    public ViewModelModule(EventBus bus) { super("ViewModel","Independent position, rotation and scale for each held item.",Category.RENDER,bus);
        left=hand("Left");right=hand("Right"); }
    private DoubleSetting number(String name,double initial,double min,double max) { return register(DoubleSetting.extended(name,name,initial,min,max)); }
    private Transform hand(String hand) { return new Transform(number(hand+" X",0,-2,2),number(hand+" Y",0,-2,2),number(hand+" Z",0,-2,2),number(hand+" Rotation X",0,-180,180),number(hand+" Rotation Y",0,-180,180),number(hand+" Rotation Z",0,-180,180),number(hand+" Scale",1,.1,3)); }
    @Override protected void onEnable() { listen(HeldItemTransformEvent.class,event->{
        if(!event.context().firstPerson()) { return; }
        Transform t=event.context()==ItemDisplayContext.FIRST_PERSON_LEFT_HAND?left:right;var p=event.poses();
        p.translate(t.x.getValue(),t.y.getValue()+(event.stack().has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS)?shieldHeight.getValue():0),t.z.getValue());p.mulPose(Axis.XP.rotationDegrees(t.rx.getValue().floatValue()));p.mulPose(Axis.YP.rotationDegrees(t.ry.getValue().floatValue()));p.mulPose(Axis.ZP.rotationDegrees(t.rz.getValue().floatValue()));float scale=t.scale.getValue().floatValue();p.scale(scale,scale,scale);
    }); }
}
