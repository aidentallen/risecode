package dev.riserecode.render;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.event.UpdateEvent;
import java.util.Objects;
import java.util.function.LongSupplier;
import net.minecraft.client.Minecraft;

/** One aimed highlight, finalized after providers and before WorldRenderer's immutable snapshot. */
public final class TargetHighlightCoordinator implements AutoCloseable {
    private final Minecraft mc;
    private final EventBus.Subscription render,tick,reload;
    private final BlockHighlightGeometry geometry=new BlockHighlightGeometry();
    private Object world,key;
    private String owner;
    private TargetHighlight.Style from,shown,targetStyle;
    private double previousDuration;
    private long started;
    private LongSupplier clock=System::nanoTime;
    public TargetHighlightCoordinator(EventBus bus,Minecraft mc) {
        this.mc=mc;
        render=bus.subscribe(Render3DEvent.class,Integer.MIN_VALUE+1,false,this::extract);
        tick=bus.subscribe(UpdateEvent.class,event->syncWorld());
        reload=bus.subscribe(dev.riserecode.event.RenderResourcesReloadEvent.class,event->reset());
    }
    public long time() { return clock.getAsLong(); }
    public void timeSource(LongSupplier source) { clock=Objects.requireNonNull(source); }
    private void syncWorld() { if(mc!=null&&world!=mc.level){reset();world=mc.level;} }
    public void reset() { geometry.clear();HighlightMesh.clearCache();key=null;owner=null;from=shown=targetStyle=null; }
    public void release(String module) { if(Objects.equals(owner,module))reset(); }
    public Object targetKey() { return key; }
    private void extract(Render3DEvent event) {
        syncWorld();var request=event.getTargetHighlight();
        if(request==null||request.corners().isEmpty()){reset();return;}
        long now=clock.getAsLong();var motion=request.motion();
        if(!Objects.equals(key,request.key())){
            if(targetStyle!=null&&from!=null){double prior=previousDuration<=0?1:Math.clamp((now-started)*1e-9/previousDuration,0,1);shown=TargetHighlight.Style.blend(from,targetStyle,1-Math.pow(1-prior,3));}
            from=shown==null?request.style():shown;key=request.key();started=now;
        }
        owner=request.owner();targetStyle=request.style();previousDuration=motion.switchingDuration();
        double duration=motion.switchingDuration();double t=duration<=0?1:Math.clamp((now-started)*1e-9/duration,0,1);t=1-Math.pow(1-t,3);
        shown=TargetHighlight.Style.blend(from,request.style(),t);
        var parts=geometry.targetMeshes(request.key(),request.shapeKey(),request.meshes(),now,duration,motion.pop()&&!motion.reduced(),motion.popOnSwitch(),motion.popDuration());
        if(!request.suppressed())for(var part:parts)if(part.opacity()>.001)TargetHighlight.draw(event,part.mesh(),shown,part.opacity(),geometry.fullScale());
    }
    @Override public void close() { render.close();tick.close();reload.close();reset();world=null; }
}
