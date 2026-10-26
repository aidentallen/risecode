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
        return new Style(true, color.getValue(), 1, false, -1, 1, 1, width, 0, dev.riserecode.ui.theme.ModuleAppearance.capture(color.paint()), dev.riserecode.ui.theme.ModuleAppearance.capture(color.paint()));
    }
    public static void box(Render3DEvent event, AABB b, ColorSetting color, double width, boolean visible) {
        for(int[] edge:EDGES){int a=edge[0],c=edge[1];
            double ax=(a==1||a==2||a==5||a==6)?b.maxX:b.minX, ay=(a==2||a==3||a==6||a==7)?b.maxY:b.minY, az=a>=4?b.maxZ:b.minZ;
            double cx=(c==1||c==2||c==5||c==6)?b.maxX:b.minX, cy=(c==2||c==3||c==6||c==7)?b.maxY:b.minY, cz=c>=4?b.maxZ:b.minZ;
            event.addLine(ax,ay,az,cx,cy,cz,color.sample(ax*40,ay*40),color.sample(cx*40,cy*40),(float)width,visible);
        }
    }
}
