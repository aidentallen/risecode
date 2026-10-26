package dev.riserecode.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** Adjacent runs by default; explicit ordered scopes preserve overlap, native and clip barriers. */
public final class UiBatch {
    private static GuiGraphicsExtractor graphics;
    private static int depth;
    private static boolean ordered,flushing;
    private static final List<Integer> scopeStarts=new ArrayList<>();
    private static final List<Long> scopeFlushes=new ArrayList<>();
    private static long flushSerial;
    private static final List<GuiElementRenderState> pending = new ArrayList<>();
    private record OrderKey(RenderPipeline pipeline,TextureSetup texture,ScreenRectangle clip,ScreenRectangle bounds){
        @Override public boolean equals(Object other){return other instanceof OrderKey key&&pipeline==key.pipeline&&texture.equals(key.texture)&&Objects.equals(clip,key.clip)&&bounds.equals(key.bounds);}
        @Override public int hashCode(){int hash=System.identityHashCode(pipeline);hash=31*hash+texture.hashCode();hash=31*hash+Objects.hashCode(clip);return 31*hash+bounds.hashCode();}
    }
    private static final OrderedBatches.Cache<OrderKey> plans=new OrderedBatches.Cache<>(16);
    public static void clearPlans(){plans.clear();}
    private static long batches;
    private UiBatch() { }
    public static long batches() { return batches; }
    /** Immutable commands without GPU ownership, captured only inside an ordered scope. */
    public static List<GuiElementRenderState> capture(Runnable draw) {
        int start=pending.size();long serial=flushSerial;boolean active=ordered&&depth>0;
        draw.run();
        return active&&flushSerial==serial?List.copyOf(pending.subList(start,pending.size())):null;
    }
    public static boolean replay(GuiGraphicsExtractor g,List<GuiElementRenderState> states) {
        if(!ordered||depth==0||graphics!=g)return false;
        // Opacity has already been captured in each immutable command.
        pending.addAll(states);if(pending.size()>=1024)flush();return true;
    }
    public static void begin(GuiGraphicsExtractor g) {
        if(depth==scopeStarts.size()){scopeStarts.add(0);scopeFlushes.add(0L);}
        scopeStarts.set(depth,pending.size());scopeFlushes.set(depth,flushSerial);
        if (depth++ == 0) { graphics = g; }
    }
    public static void beginOrdered(GuiGraphicsExtractor g){if(depth==0){begin(g);ordered=true;}else{begin(g);}}
    public static void end() {
        if (--depth == 0) { try{flush();}finally{graphics = null;ordered=false;} }
        else if(ordered&&scopeFlushes.get(depth)==flushSerial){compactAdjacent(scopeStarts.get(depth));}
    }
    public static void nativeBarrier(net.minecraft.client.renderer.state.gui.GuiRenderState state){if(!flushing&&ordered&&graphics!=null&&graphics.guiRenderState==state){flush();}}
    /** Ordered commands retain immutable clips; changing the extractor's stack is not a draw. */
    public static void clipBarrier(GuiGraphicsExtractor g){if(!ordered||graphics!=g)flush();}
    public static void submit(GuiGraphicsExtractor g, GuiElementRenderState state) {
        state = dev.riserecode.ui.menu.MenuTransition.capture(state);
        if (depth == 0) { g.guiRenderState.addGuiElement(state); return; }
        if (!ordered&&!pending.isEmpty()) {
            var first = pending.getFirst();
            if (first.pipeline() != state.pipeline() || !first.textureSetup().equals(state.textureSetup()) || !Objects.equals(first.scissorArea(), state.scissorArea())) { flush(); }
        }
        pending.add(state);
        if(ordered&&pending.size()>=1024){flush();}
    }
    public static void flush() {
        if (pending.isEmpty()) { return; }
        flushSerial++;
        flushing=true;
        try{
            if(ordered){
                List<OrderKey> key=new ArrayList<>(pending.size());
                for(var state:pending)key.add(new OrderKey(state.pipeline(),state.textureSetup(),state.scissorArea(),state.bounds()));
                plans.get(key,pending,UiBatch::compatible,(a,b)->a.bounds().intersects(b.bounds())).emit(pending,UiBatch::emit);
            }
            else{emit(pending);pending.clear();}
        }finally{pending.clear();flushing=false;}
    }
    private static boolean compatible(GuiElementRenderState a,GuiElementRenderState b){return a.pipeline()==b.pipeline()&&a.textureSetup().equals(b.textureSetup())&&Objects.equals(a.scissorArea(),b.scissorArea());}
    /** Icons and native fallback stencils already form local batches. Preserve that
     * hierarchy before the wider overlap pass, instead of scanning each tiny primitive. */
    private static void compactAdjacent(int start){
        int end=pending.size();if(end-start<2)return;
        List<GuiElementRenderState> compacted=new ArrayList<>();
        for(int first=start;first<end;){int next=first+1;while(next<end&&compatible(pending.get(first),pending.get(next))){next++;}compacted.add(run(pending.subList(first,next)));first=next;}
        pending.subList(start,end).clear();pending.addAll(compacted);
    }
    private static void emit(List<GuiElementRenderState> states){
        graphics.guiRenderState.addGuiElement(run(states));batches++;
    }
    private static GuiElementRenderState run(List<GuiElementRenderState> states){
        if(states.size()==1)return states.getFirst();
        var first=states.getFirst();int left=Integer.MAX_VALUE,top=Integer.MAX_VALUE,right=Integer.MIN_VALUE,bottom=Integer.MIN_VALUE;
        for(var state:states){var r=state.bounds();left=Math.min(left,r.left());top=Math.min(top,r.top());right=Math.max(right,r.right());bottom=Math.max(bottom,r.bottom());}
        return new Run(first.pipeline(),first.textureSetup(),first.scissorArea(),new ScreenRectangle(left,top,right-left,bottom-top),List.copyOf(states));
    }
    private record Run(RenderPipeline pipeline,TextureSetup textureSetup,ScreenRectangle scissorArea,ScreenRectangle bounds,List<GuiElementRenderState> states) implements dev.riserecode.ui.menu.MenuTransition.Captured {
        public void buildVertices(VertexConsumer vertices) { states.forEach(state -> state.buildVertices(vertices)); }
    }
}
