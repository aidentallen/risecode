package dev.riserecode.render;
import dev.riserecode.event.Render3DEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class WorldLabels {
    private WorldLabels() { }
    private record TextKey(Component text,net.minecraft.network.chat.Style font,long generation,String language) { }
    private record Text(net.minecraft.util.FormattedCharSequence sequence,int width) { }
    private static final java.util.Map<TextKey,Text> TEXT=new java.util.LinkedHashMap<>(64,.75f,true){
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<TextKey,Text> entry){return size()>256;}
    };
    private static Text text(Component label){
        var mc=Minecraft.getInstance();var key=new TextKey(label,dev.riserecode.ui.theme.Theme.nativeStyle(false),dev.riserecode.ui.font.CustomTextRenderer.generation(),mc.getLanguageManager().getSelected());
        var cached=TEXT.get(key);if(cached!=null){return cached;}
        var text=dev.riserecode.ui.theme.Theme.nativeText(label).getVisualOrderText();var value=new Text(text,mc.font.width(text));
        TEXT.put(new TextKey(label.copy(),key.font(),key.generation(),key.language()),value);return value;
    }
    public static void clearTextCache(){TEXT.clear();}
    private static final class PanelTypes {
        private static final net.minecraft.client.renderer.rendertype.RenderType DEPTH=panel(false),THROUGH=panel(true);
        private static net.minecraft.client.renderer.rendertype.RenderType panel(boolean through){
            var pipeline=net.minecraft.client.renderer.RenderPipelines.register(com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(dev.riserecode.ui.render.UiMesh.id(through?"pipeline/label_panel_through":"pipeline/label_panel_depth"))
                    .withDepthStencilState(through?java.util.Optional.empty():java.util.Optional.ofNullable(net.minecraft.client.renderer.RenderPipelines.DEBUG_QUADS.getDepthStencilState())).build());
            var setup=net.minecraft.client.renderer.rendertype.RenderSetup.builder(pipeline).setOutputTarget(ShaderWorldOverlay.OUTPUT);
            if(!through){setup.sortOnUpload();}
            return net.minecraft.client.renderer.rendertype.RenderType.create("risecode label panel",setup.createRenderSetup());
        }
    }
    public static void label(Render3DEvent event,Vec3 position,Component label,int color,double scale,boolean through,List<ItemStackRenderState> equipment){
        var prepared=text(label);var text=prepared.sequence();int width=prepared.width();
        var theme=dev.riserecode.ui.theme.Theme.manager();
        int surface=theme.colorSetting("Surface Color").sample(0,0), shadow=theme.color("Shadow Color"),borderColor=theme.color("Border Color");
        double border=Math.clamp(theme.number("Border Thickness")*.6,0,8),borderOpacity=theme.number("Border Opacity");
        event.submit(context->{var camera=context.levelState().cameraRenderState;var pose=context.poseStack();pose.pushPose();try{
            pose.translate(position.x-camera.pos.x,position.y-camera.pos.y,position.z-camera.pos.z);pose.mulPose(camera.orientation);pose.scale((float)(0.025*scale),(float)(-0.025*scale),(float)(0.025*scale));
            int alpha=color>>>24;
            context.submitNodeCollector().submitCustomGeometry(pose,through?PanelTypes.THROUGH:PanelTypes.DEPTH,(matrix,vertices)->{
                for(int layer=0;layer<3;layer++){
                    double pad=layer==0?2:layer==1?border:0,offset=layer==0?1.5:0;
                    double left=-width/2.0-4-pad,right=width/2.0+4+pad,top=-3-pad+offset,bottom=11+pad+offset,radius=3+pad;
                    int base=layer==0?shadow:layer==1?borderColor:surface;
                    int tint=dev.riserecode.ui.theme.Theme.opacity(base,alpha/255.0*(layer==0?.35:layer==1?borderOpacity:.85));
                    float z=-.04f+layer*.01f;
                    for(int i=0;i<20;i++){
                        double a=i*Math.PI/10,b=(i+1)*Math.PI/10;
                        double ax=(Math.cos(a)>0?right-radius:left+radius)+Math.cos(a)*radius,ay=(Math.sin(a)>0?bottom-radius:top+radius)+Math.sin(a)*radius;
                        double bx=(Math.cos(b)>0?right-radius:left+radius)+Math.cos(b)*radius,by=(Math.sin(b)>0?bottom-radius:top+radius)+Math.sin(b)*radius;
                        vertices.addVertex(matrix,0,4,z).setColor(tint);vertices.addVertex(matrix,(float)ax,(float)ay,z).setColor(tint);vertices.addVertex(matrix,(float)bx,(float)by,z).setColor(tint);vertices.addVertex(matrix,0,4,z).setColor(tint);
                    }
                }
            });
            context.submitNodeCollector().submitText(pose,-width/2f,0,text,false,through?Font.DisplayMode.SEE_THROUGH:Font.DisplayMode.NORMAL,0xF000F0,color,0,0);
            if(!equipment.isEmpty()){pose.translate(-equipment.size()*9,-16,0);pose.scale(16,-16,16);for(var item:equipment){item.submit(pose,context.submitNodeCollector(),0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);pose.translate(1.125,0,0);}}
            }finally{pose.popPose();}
        });
    }
}
