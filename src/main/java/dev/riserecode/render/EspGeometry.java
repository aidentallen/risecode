package dev.riserecode.render;

import dev.riserecode.event.EnchantmentRenderEvent.Style;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.setting.ColorSetting;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EspGeometry {
    private static final int[][] EDGES = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
    private EspGeometry() { }
    public static Style style(ColorSetting color, double width) {
        return new Style(true, color.getValue(), 1, false, -1, 1, 1, width, 0, color.paint(), color.paint());
    }
    public static void box(Render3DEvent event, AABB b, ColorSetting color, double width, boolean visible) {
        Vec3[] points = {new Vec3(b.minX,b.minY,b.minZ), new Vec3(b.maxX,b.minY,b.minZ), new Vec3(b.maxX,b.maxY,b.minZ), new Vec3(b.minX,b.maxY,b.minZ),
                new Vec3(b.minX,b.minY,b.maxZ), new Vec3(b.maxX,b.minY,b.maxZ), new Vec3(b.maxX,b.maxY,b.maxZ), new Vec3(b.minX,b.maxY,b.maxZ)};
        for (int[] edge : EDGES) { var a = points[edge[0]]; var c = points[edge[1]];
            event.addLine(a, c, color.sample(a.x * 40, a.y * 40), color.sample(c.x * 40, c.y * 40), (float) width, visible);
        }
    }
}
