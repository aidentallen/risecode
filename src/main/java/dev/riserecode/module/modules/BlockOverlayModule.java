package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.render.BlockHighlightMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockOverlayModule extends Module {
    public enum OutlineStyle { FULL, CORNERS }
    public enum ColorEffect { CUSTOM, RAINBOW_CYCLE, SPATIAL_RAINBOW }
    private final EnumSetting<ColorEffect> colorEffect = register(new EnumSetting<>("Color Effect", "Outline color animation", ColorEffect.CUSTOM));
    private final DoubleSetting rainbowSpeed = register(new DoubleSetting("Rainbow Speed", "Cycles per second", .18, 0, 12));
    private final DoubleSetting rainbowSpacing = register(new DoubleSetting("Rainbow Spacing", "World blocks per color cycle", 8, .1, 100));
    private final BooleanSetting smooth = register(new BooleanSetting("Smooth Switching", "Move the highlight smoothly between blocks", true));
    private final DoubleSetting transition = register(DoubleSetting.extended("Transition Duration", "Seconds to reach a new block", .15, 0, 5));
    private final BooleanSetting pop = register(new BooleanSetting("Pop Animation", "Grow when acquiring a block after looking away", false));
    private final BooleanSetting popOnSwitch = register(new BooleanSetting("Pop On Switch", "Also pop when switching directly between blocks", false));
    private final DoubleSetting popDuration = register(DoubleSetting.extended("Block Pop Duration", "Seconds to grow to the full shape", .18, 0, 5));
    private final EnumSetting<OutlineStyle> style = register(new EnumSetting<>("Outline Style", "Full edges or corner segments", OutlineStyle.FULL));
    private final DoubleSetting gap = register(DoubleSetting.extended("Edge Gap", "Percentage missing from the center of each edge", 60, 0, 100));
    private final Minecraft minecraft;
    private final BooleanSetting fill = register(new BooleanSetting("OverlayEnabled", "Draw filled block shape", true));
    private final ColorSetting fillColor = register(new ColorSetting("FillColor", "Overlay ARGB color", 0x803B82F6));
    private final DoubleSetting fillOpacity = register(DoubleSetting.extended("FillOpacity", "Additional fill opacity", 0.4, 0, 1));
    private final BooleanSetting outline = register(new BooleanSetting("OutlineEnabled", "Draw shape edges", true));
    private final BooleanSetting through = register(new BooleanSetting("See-through Outline", "Show every edge through blocks, including the pop animation", false));
    private final ColorSetting outlineColor = register(new ColorSetting("OutlineColor", "Edge ARGB color", 0xFF59D5CB));
    private final DoubleSetting width = register(DoubleSetting.extended("OutlineWidth", "Edge pixels", 2, 0.5, 5));
    private final DoubleSetting outlineOpacity = register(DoubleSetting.extended("OutlineOpacity", "Additional edge opacity", 1, 0, 1));
    public BlockOverlayModule(EventBus bus, Minecraft mc) {
        super("BlockOverlay", "Independent fill and outline for the targeted block shape.", Category.RENDER, bus);
        minecraft = mc;
        rainbowSpeed.setVisibleWhen(()->colorEffect.getValue()!=ColorEffect.CUSTOM);
        rainbowSpacing.setVisibleWhen(()->colorEffect.getValue()==ColorEffect.SPATIAL_RAINBOW);
        transition.setVisibleWhen(smooth::getValue); popDuration.setVisibleWhen(pop::getValue); popOnSwitch.setVisibleWhen(pop::getValue);
        through.setVisibleWhen(outline::getValue); style.setVisibleWhen(outline::getValue); gap.setVisibleWhen(()->outline.getValue()&&style.getValue()==OutlineStyle.CORNERS);
        fillColor.setVisibleWhen(fill::getValue); fillOpacity.setVisibleWhen(fill::getValue);
        outlineColor.setVisibleWhen(outline::getValue); width.setVisibleWhen(outline::getValue); outlineOpacity.setVisibleWhen(outline::getValue);
    }
    @Override protected void onEnable() { listen(dev.riserecode.event.BlockOutlineEvent.class,event->event.cancel()); listen(Render3DEvent.class, this::extract); }
    @Override protected void onDisable() { var coordinator=dev.riserecode.Client.INSTANCE.getTargetHighlights();if(coordinator!=null)coordinator.release(capabilityId()); }
    private void extract(Render3DEvent event) {
        if (minecraft.level == null || minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) { return; }
        var position = hit.getBlockPos(); var shape = minecraft.level.getBlockState(position).getShape(minecraft.level, position);

        var placement=dev.riserecode.render.placement.PlacementAnimations.at(position);
        var animations=dev.riserecode.Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElse(null);
        var behavior=animations==null?BlockAnimationsModule.OutlineBehavior.FIXED_POSITION:animations.outlineBehavior.getValue();
        boolean following=placement!=null&&behavior==BlockAnimationsModule.OutlineBehavior.FOLLOW_ANIMATION;
        var mesh=dev.riserecode.render.HighlightMesh.selection(shape);
        var target=mesh.map(point->{var worldPoint=point.add(position.getX(),position.getY(),position.getZ());return following?dev.riserecode.render.placement.PlacementAnimations.transform(placement,worldPoint):worldPoint;});
        var settings=highlightMotion();
        if(following)settings=new dev.riserecode.render.TargetHighlight.Motion(settings.smooth(),settings.duration(),false,settings.popOnSwitch(),settings.popDuration(),settings.reduced());
        event.targetHighlight(dev.riserecode.render.TargetHighlight.Request.mesh(capabilityId(),
                new dev.riserecode.render.TargetHighlight.Key(dev.riserecode.render.TargetHighlight.Kind.BLOCK,position.asLong()),mesh,target,settings,highlightStyle(),
                placement!=null&&placement.progress()<1&&behavior==BlockAnimationsModule.OutlineBehavior.HIDE_UNTIL_SETTLED));
    }
    public dev.riserecode.render.TargetHighlight.Motion highlightMotion() {
        return dev.riserecode.ui.theme.ModuleAppearance.call(capabilityId(),()->new dev.riserecode.render.TargetHighlight.Motion(
                smooth.getValue(),transition.getValue(),pop.getValue(),popOnSwitch.getValue(),popDuration.getValue(),Theme.bool("Reduced Motion",false)));
    }
    public dev.riserecode.render.TargetHighlight.Style highlightStyle() {
        return dev.riserecode.ui.theme.ModuleAppearance.call(capabilityId(),()->spatialStyle(dev.riserecode.render.TargetHighlight.Style.of(
                effectColor(),outline.getValue()?outlineOpacity.getValue():0,width.getValue(),style.getValue()==OutlineStyle.CORNERS?gap.getValue():0,
                through.getValue(),fillColor,fill.getValue()?fillOpacity.getValue():0)));
    }
    private dev.riserecode.render.TargetHighlight.Style spatialStyle(dev.riserecode.render.TargetHighlight.Style style){
        if(colorEffect.getValue()!=ColorEffect.SPATIAL_RAINBOW)return style;
        var edges=style.edges().stream().map(layer->new dev.riserecode.render.TargetHighlight.Layer(layer.paint(),layer.weight(),true)).toList();
        return new dev.riserecode.render.TargetHighlight.Style(edges,style.faces(),style.width(),style.gap(),style.through());
    }
    private ColorSetting effectColor() {
        if (colorEffect.getValue() == ColorEffect.CUSTOM) return outlineColor;
        var color = new ColorSetting("Rainbow", "", outlineColor.getValue());
        if (colorEffect.getValue() == ColorEffect.RAINBOW_CYCLE) {
            double phase = dev.riserecode.util.ColorUtil.time() * rainbowSpeed.getValue();
            color.setValue((outlineColor.getValue() & 0xFF000000) |
                    (dev.riserecode.util.ColorUtil.hsv(phase - Math.floor(phase), .65, 1) & 0xFFFFFF));
        } else {
            color.setMode(ColorSetting.Mode.RAINBOW);
            color.setSpeed(rainbowSpeed.getValue()); color.setSpacing(rainbowSpacing.getValue()*40);
        }
        return color;
    }
    /** Entity inheritance copies visual settings, independent of the block outline's enable toggle. */
    public dev.riserecode.render.TargetHighlight.Style entityStyle() {
        var style=highlightStyle();
        return dev.riserecode.ui.theme.ModuleAppearance.call(capabilityId(),()->spatialStyle(dev.riserecode.render.TargetHighlight.Style.of(
                effectColor(),outlineOpacity.getValue(),width.getValue(),style.gap(),through.getValue(),null,0)));
    }
    private void box(Render3DEvent event, Vec3[] vertices, double alpha, boolean fullScale) {
        dev.riserecode.render.TargetHighlight.draw(event,java.util.List.of(vertices),highlightStyle(),alpha,fullScale);
    }
}
