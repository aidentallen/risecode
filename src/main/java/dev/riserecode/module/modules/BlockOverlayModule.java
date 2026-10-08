package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockOverlayModule extends Module {
    private final Minecraft minecraft;
    private final BooleanSetting fill = register(new BooleanSetting("OverlayEnabled", "Draw filled block shape", true));
    private final ColorSetting fillColor = register(new ColorSetting("FillColor", "Overlay ARGB color", 0x803B82F6));
    private final DoubleSetting fillOpacity = register(new DoubleSetting("FillOpacity", "Additional fill opacity", 0.4, 0, 1));
    private final BooleanSetting outline = register(new BooleanSetting("OutlineEnabled", "Draw shape edges", true));
    private final ColorSetting outlineColor = register(new ColorSetting("OutlineColor", "Edge ARGB color", 0xFF59D5CB));
    private final DoubleSetting width = register(new DoubleSetting("OutlineWidth", "Edge pixels", 2, 0.5, 5));
    private final DoubleSetting outlineOpacity = register(new DoubleSetting("OutlineOpacity", "Additional edge opacity", 1, 0, 1));
    public BlockOverlayModule(EventBus bus, Minecraft mc) {
        super("BlockOverlay", "Independent fill and outline for the targeted block shape.", Category.RENDER, bus); minecraft = mc;
        fillColor.setVisibleWhen(fill::getValue); fillOpacity.setVisibleWhen(fill::getValue);
        outlineColor.setVisibleWhen(outline::getValue); width.setVisibleWhen(outline::getValue); outlineOpacity.setVisibleWhen(outline::getValue);
    }
    @Override protected void onEnable() { listen(Render3DEvent.class, this::extract); }
    private void extract(Render3DEvent event) {
        if (minecraft.level == null || minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) { return; }
        var position = hit.getBlockPos(); var shape = minecraft.level.getBlockState(position).getShape(minecraft.level, position);
        for (AABB bounds : shape.toAabbs()) { box(event, bounds.move(position).inflate(0.002)); }
    }
    private void box(Render3DEvent event, AABB b) {
        Vec3[] v = {new Vec3(b.minX,b.minY,b.minZ), new Vec3(b.maxX,b.minY,b.minZ), new Vec3(b.maxX,b.maxY,b.minZ), new Vec3(b.minX,b.maxY,b.minZ),
                new Vec3(b.minX,b.minY,b.maxZ), new Vec3(b.maxX,b.minY,b.maxZ), new Vec3(b.maxX,b.maxY,b.maxZ), new Vec3(b.minX,b.maxY,b.maxZ)};
        if (fill.getValue()) {
            for (int[] face : new int[][] {{0,3,2,1},{4,5,6,7},{0,1,5,4},{3,7,6,2},{0,4,7,3},{1,2,6,5}}) {
                event.addQuad(v[face[0]], v[face[1]], v[face[2]], v[face[3]], tint(fillColor, v[face[0]], fillOpacity.getValue()), tint(fillColor, v[face[1]], fillOpacity.getValue()), tint(fillColor, v[face[2]], fillOpacity.getValue()), tint(fillColor, v[face[3]], fillOpacity.getValue()));
            }
        }
        if (outline.getValue()) {
            for (int[] edge : new int[][] {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}}) {
                event.addLine(v[edge[0]], v[edge[1]], tint(outlineColor, v[edge[0]], outlineOpacity.getValue()), tint(outlineColor, v[edge[1]], outlineOpacity.getValue()), width.getValue().floatValue());
            }
        }
    }
    private static int tint(ColorSetting setting, Vec3 point, double opacity) { return Theme.opacity(setting.sample((point.x + point.z) * 40, point.y * 40), opacity); }

}
