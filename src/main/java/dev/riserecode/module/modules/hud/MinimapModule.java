package dev.riserecode.module.modules.hud;
import dev.riserecode.event.EventBus;
import dev.riserecode.setting.*;
import dev.riserecode.ui.hud.HudContent;
import net.minecraft.client.Minecraft;
import java.util.List;
public final class MinimapModule extends HudModule {
    public enum Shape { SQUARE,CIRCLE }
    public final EnumSetting<Shape> shape=register(new EnumSetting<>("Shape","Minimap outline",Shape.SQUARE));
    public final EnumSetting<dev.riserecode.map.MapLayerPolicy.Mode> layer=register(new EnumSetting<>("Layer","Automatic or manual terrain layer",dev.riserecode.map.MapLayerPolicy.Mode.AUTO));
    public final BooleanSetting autoAltitude=register(new BooleanSetting("Automatic Altitude","Follow player altitude with hysteresis",true));
    public final DoubleSetting altitude=register(DoubleSetting.extended("Cave Altitude","Manual cave altitude",64,-64,320));
    public final DoubleSetting zoom=register(DoubleSetting.extended("Zoom","Map pixels per block",1.5,0.5,6));
    public final DoubleSetting size=register(DoubleSetting.extended("Size","Map size in HUD canvas units",130,80,240));
    public final BooleanSetting rotate=register(new BooleanSetting("Rotate","Keep player facing up",true));
    public final BooleanSetting compass=register(new BooleanSetting("Compass","Show north indicator",true));
    public final BooleanSetting waypoints=register(new BooleanSetting("Waypoints","Show destinations",true));
    public MinimapModule(EventBus bus,Minecraft mc){super("Minimap","Textured explored surface map in the HUD editor.",bus,mc);}
    @Override protected Object contentKey(){return 0;}
    @Override protected List<HudContent.Line> buildContent(){return List.of();}
    @Override public double graphicWidth(){return size.getValue();}
    @Override public double graphicHeight(){return size.getValue();}
    @Override public void drawGraphic(net.minecraft.client.gui.GuiGraphicsExtractor g,double alpha,boolean edit){
        var maps=dev.riserecode.Client.INSTANCE.getMaps();if(minecraft.player==null){dev.riserecode.util.RenderUtil.text(g,minecraft.font,"Minimap",8,8,1,0xFFFFFFFF);return;}
        double rotation=rotate.getValue()?Math.toRadians(180-minecraft.player.getYRot()):0;
        dev.riserecode.map.MapCanvas.draw(g,maps,maps.dimension(),minecraft.player.getX(),minecraft.player.getZ(),0,0,graphicWidth(),graphicHeight(),zoom.getValue(),rotation,waypoints.getValue(),compass.getValue(),alpha,layer.getValue(),autoAltitude.getValue()?Integer.MIN_VALUE:Math.floorDiv(altitude.getValue().intValue(),8)*8,shape.getValue()==Shape.CIRCLE,true);
    }
}
