package dev.riserecode.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

/** Antialiased signed-distance geometry extracted into the game's modern GUI batching pipeline. */
public final class RenderUtil {
    private static final RenderPipeline SHAPE = UiMesh.pipeline("shape", "shape");
    private static final RenderPipeline WINDOW = UiMesh.pipeline("window", "window");
    private RenderUtil() { }
    /** Floating panels have one paint; the main window's sidebar is not part of this surface. */
    public static void drawPopup(GuiGraphicsExtractor graphics,double x,double y,double width,double height,double opacity){
        drawPaintRect(graphics,x,y,width,height,10,Theme.paint("Background Color"),opacity*Theme.number("Panel Opacity",1));
        drawRoundedBorder(graphics,x,y,width,height,10,Theme.opacity(-1,opacity*Theme.number("Border Opacity",.08)));
    }
    public static void drawWindow(GuiGraphicsExtractor graphics,double x,double y,double opacity){drawWindow(graphics,x,y,428,320,opacity);}
    public static void drawWindow(GuiGraphicsExtractor graphics,double x,double y,double width,double height,double opacity){
        if(Theme.manager()!=null&&(!Theme.manager().colorSetting("Background Color").paint().equals(Theme.manager().colorSetting("Background Color").defaultPaint())
                ||!Theme.manager().colorSetting("Sidebar Color").paint().equals(Theme.manager().colorSetting("Sidebar Color").defaultPaint()))){
            drawPaintRect(graphics,x,y,width,height,10,Theme.paint("Background Color"),opacity*Theme.number("Panel Opacity",1));
            scissorPush(graphics,x,y,107,height);
            try{drawPaintRect(graphics,x,y,width,height,10,Theme.paint("Sidebar Color"),opacity*Theme.number("Panel Opacity",1));}finally{scissorPop(graphics);}
        }else{
            shape(graphics,WINDOW,x,y,width,height,Theme.number("Corner Radius",10),Theme.opacity(-1,opacity*Theme.number("Panel Opacity",1)),Theme.opacity(-1,opacity*Theme.number("Panel Opacity",1)),0);
        }
        drawRoundedBorder(graphics,x,y,width,height,10,Theme.opacity(-1,opacity*Theme.number("Border Opacity",.08)));
    }
    public static void drawRoundedRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        int rgb = color & 0xFFFFFF;
        int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
        boolean surface = (r >= 17 && r <= 25 && g >= 24 && g <= 36 && b >= 30 && b <= 44) || rgb == (Theme.SURFACE & 0xFFFFFF) || rgb == (Theme.PANEL & 0xFFFFFF)
                || rgb == (Theme.BACKGROUND & 0xFFFFFF) || rgb == (Theme.HOVER & 0xFFFFFF);
        if (surface) { color = Theme.opacity(color, Theme.number("Panel Opacity", 1)); }
        ColorSetting.Paint paint = Theme.tokenPaint(color);
        if (paint == null) { shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius), color, color, 0); }
        else { drawPaintShape(graphics, x, y, width, height, Theme.radius(radius), paint, 0); }
        if (surface) {
            drawRoundedBorder(graphics, x, y, width, height, radius,
                    Theme.opacity(0xFFFFFFFF, (color >>> 24) / 255.0 * Theme.number("Border Opacity", 0.08)));
        }
    }
    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawRoundedBorder(graphics, x, y, width, height, radius, color, Theme.number("Border Thickness", 1));
    }
    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color, double thickness) {
        if (thickness <= 0) { return; }
        if (Theme.manager() != null && (color & 0xFFFFFF) == 0xFFFFFF) {
            drawPaintShape(graphics, x, y, width, height, Theme.radius(radius), Theme.paint("Border Color").opacity((color >>> 24) / 255.0), thickness);
        } else { shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius), color, color, thickness); }
    }
    public static void drawDropShadow(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        if(dev.riserecode.performance.PerformancePolicy.frame().softShadows()){if(Theme.manager()!=null&&(color&0xFFFFFF)==0){drawPaintRect(graphics,x,y+3,width,height,radius,Theme.paint("Shadow Color"),Math.min(1,(color>>>24)/96.0));}else{drawRoundedRect(graphics,x,y+3,width,height,radius,color);}return;}
        if (Theme.manager() != null && (color & 0xFFFFFF) == 0) {
            drawPaintShape(graphics, x, y + 3, width, height, Theme.radius(radius), Theme.paint("Shadow Color").opacity(Math.min(1, (color >>> 24) / 96.0)), -12);
        } else { shape(graphics, SHAPE, x, y + 3, width, height, Theme.radius(radius), color, color, -12); }
    }
    public static void drawGlow(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, int color) {
        double size = Theme.number("Glow Radius", 5);
        if (!dev.riserecode.performance.PerformancePolicy.frame().glow() && Theme.bool("Global Glow", true) && size > 0) {
            var clip=graphics.scissorStack.peek();
            if(clip!=null){var source=new net.minecraft.client.gui.navigation.ScreenRectangle((int)Math.floor(x),(int)Math.floor(y),(int)Math.ceil(width),(int)Math.ceil(height)).transformMaxBounds(graphics.pose());if(source.intersection(clip)==null){return;}}
            UiMesh.effect(() -> shape(graphics, SHAPE, x, y, width, height, Theme.radius(radius),
                    Theme.opacity(color, Theme.number("Glow Intensity", 0.2)),
                    Theme.opacity(color, Theme.number("Glow Intensity", 0.2)), -size));
        }
    }
    public static void drawGradientRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, int top, int bottom) {
        shape(graphics, SHAPE, x, y, width, height, 0, top, bottom, 0);
    }
    public static void drawGradientRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, int start, int end, boolean horizontal) {
        UiMesh.quad(graphics, SHAPE, TextureSetup.noTexture(), x, y, width, height,
                (float) (-width / 2), (float) (-height / 2), (float) (width / 2), (float) (height / 2),
                start, end, (int) Math.round(width * 16), (int) Math.round(height * 16), 0, 0, horizontal);
    }
    public static void drawPaintRect(GuiGraphicsExtractor graphics, double x, double y, double width, double height,
            double radius, ColorSetting.Paint paint, double opacity) {
        drawPaintShape(graphics, x, y, width, height, Theme.radius(radius), paint.opacity(opacity), 0);
    }
    public static void drawSurfaceTransition(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, double hover, double opacity) {
        double alpha = opacity * Theme.number("Panel Opacity", 1);
        if (Theme.manager() == null) {
            drawRoundedRect(graphics, x, y, width, height, radius, Theme.opacity(Theme.lerp(Theme.SURFACE, Theme.HOVER, hover), opacity));
            return;
        }
        drawPaintShape(graphics, x, y, width, height, Theme.radius(radius), Theme.paint("Surface Color").opacity(alpha),
                Theme.paint("Hover Color").opacity(alpha), hover, 0);
        drawRoundedBorder(graphics, x, y, width, height, radius, Theme.opacity(0xFFFFFFFF, alpha * Theme.number("Border Opacity", 0.08)));
    }
    public static void drawButton(GuiGraphicsExtractor g,double x,double y,double w,double h,double hover,double alpha) {
        if(dev.riserecode.ui.theme.PageAppearance.plain()){drawRoundedRect(g,x,y,w,h,0,Theme.opacity(Theme.lerp(0xFF555555,0xFF888888,hover),alpha));drawRoundedBorder(g,x,y,w,h,0,Theme.opacity(0xFFAAAAAA,alpha),1);return;}
        drawSurfaceTransition(g,x,y,w,h,4,hover,alpha);
        if (Theme.manager() != null && Theme.manager().choice("Button Style", dev.riserecode.ui.theme.ThemeManager.ButtonStyle.class)
                == dev.riserecode.ui.theme.ThemeManager.ButtonStyle.ACCENT) {
            drawGlow(g,x,y,w,h,4,Theme.opacity(Theme.accent(),alpha*(.2+.8*hover)));
            drawAccentRoundedRect(g,x,y,w,h,4,alpha*(.35+.5*hover));
        }
    }
    private static void drawPaintShape(GuiGraphicsExtractor graphics, double x, double y, double width, double height,
            double radius, ColorSetting.Paint paint, double effect) {
        drawPaintShape(graphics, x, y, width, height, radius, paint, null, 0, effect);
    }
    private static void drawPaintShape(GuiGraphicsExtractor graphics, double x, double y, double width, double height,
            double radius, ColorSetting.Paint paint, ColorSetting.Paint next, double blend, double effect) {
        if (width <= 0 || height <= 0) { return; }
        dev.riserecode.ui.render.PaintMesh.shape(graphics, x, y, width, height, radius, paint, next, blend, effect);
    }
    public static void drawAccentRoundedRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, double opacity) {
        if(dev.riserecode.ui.theme.PageAppearance.plain()){drawRoundedRect(graphics,x,y,width,height,0,Theme.opacity(0xFF707070,opacity));return;}
        boolean horizontal = Theme.manager() == null || Theme.manager().gradientDirection() == ColorUtil.GradientDirection.HORIZONTAL;
        var pose = graphics.pose();
        double sx = pose.m20() + x * pose.m00() + y * pose.m10(), sy = pose.m21() + x * pose.m01() + y * pose.m11();
        double ex = sx + (horizontal ? width * pose.m00() : height * pose.m10());
        double ey = sy + (horizontal ? width * pose.m01() : height * pose.m11());
        UiMesh.quad(graphics, SHAPE, TextureSetup.noTexture(), x - 1, y - 1, width + 2, height + 2,
                (float) (-width / 2 - 1), (float) (-height / 2 - 1), (float) (width / 2 + 1), (float) (height / 2 + 1),
                Theme.opacity(Theme.gradientAt(sx, sy, 0), opacity), Theme.opacity(Theme.gradientAt(ex, ey, 1), opacity),
                (int) Math.round(width * 16), (int) Math.round(height * 16), (int) Math.round(Theme.radius(radius) * 256), 0, horizontal);
    }
    private static void shape(GuiGraphicsExtractor graphics, RenderPipeline pipeline, double x, double y,
            double width, double height, double radius, int top, int bottom, double effect) {
        if (width <= 0 || height <= 0 || (top >>> 24) == 0) { return; }
        double pad = effect < 0 ? -effect + 1 : 1;
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int code = effect < 0 ? -(int) Math.round(-effect * 256) - 1 : (int) Math.round(effect * 256);
        UiMesh.quad(graphics, pipeline, TextureSetup.noTexture(), x - pad, y - pad, width + pad * 2,
                height + pad * 2, (float) (-width / 2 - pad), (float) (-height / 2 - pad),
                (float) (width / 2 + pad), (float) (height / 2 + pad), top, bottom,
                (int) Math.round(width * 16), (int) Math.round(height * 16), (int) Math.round(radius * 256), code);
    }
    public static void drawStringWithShadow(GuiGraphicsExtractor graphics, Font font, String text,
            double x, double y, int color) { drawText(graphics, font, text, x, y, color, false, true); }
    public static void drawText(GuiGraphicsExtractor graphics, Font font, String text, double x, double y,
            int color, boolean title, boolean shadow) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y);
        CustomTextRenderer.draw(graphics, font, text, color, title, shadow, false);
        graphics.pose().popMatrix();
    }
    public static void brandText(GuiGraphicsExtractor g,Font font,String text,double x,double y,double scale,double alpha){
        g.pose().pushMatrix();g.pose().translate((float)x,(float)y);g.pose().scale((float)scale,(float)scale);
        CustomTextRenderer.drawBrand(g,font,text,Theme.opacity(Theme.accent(),alpha));g.pose().popMatrix();
    }
    public static void accentText(GuiGraphicsExtractor graphics, Font font, String text, double x,
            double y, double scale, double opacity) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y); graphics.pose().scale((float) scale, (float) scale);
        CustomTextRenderer.draw(graphics, font, text, Theme.opacity(Theme.accent(), opacity), false, true, true);
        graphics.pose().popMatrix();
    }
    public static void paintText(GuiGraphicsExtractor graphics, Font font, String text, double x, double y,
            double scale, ColorSetting.Paint paint, double opacity, boolean glow) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y); graphics.pose().scale((float) scale, (float) scale);
        CustomTextRenderer.drawPaint(graphics, font, text, paint.opacity(opacity), glow);
        graphics.pose().popMatrix();
    }
    public static void hudText(GuiGraphicsExtractor graphics, Font font, String text, double x, double y, int color) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y);
        CustomTextRenderer.draw(graphics, font, text, color, false, true, false, true);
        graphics.pose().popMatrix();
    }
    public static void text(GuiGraphicsExtractor graphics, Font font, String text, double x, double y,
            double scale, int color) {
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x, (float) y); graphics.pose().scale((float) scale, (float) scale);
        CustomTextRenderer.draw(graphics, font, text, color, false, true, false); graphics.pose().popMatrix();
    }
    public static int textWidth(Font font, String text, boolean title) { return CustomTextRenderer.width(font, text, title); }
    public static void line(GuiGraphicsExtractor graphics, double x1, double y1, double x2, double y2, double thickness, int color) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length == 0) { return; }
        graphics.pose().pushMatrix(); graphics.pose().translate((float) x1, (float) y1);
        graphics.pose().rotate((float) Math.atan2(y2 - y1, x2 - x1));
        drawRoundedRect(graphics, 0, -thickness / 2, length, thickness, thickness / 2, color); graphics.pose().popMatrix();
    }
    private record Elision(Font font,Object face,double scale,long generation,String text,double width) { }
    private static final java.util.Map<Elision,String> ELISIONS=new java.util.LinkedHashMap<>(128,.75f,true);
    public static String ellipsize(Font font, String text, double width) {
        if (textWidth(font, text, false) <= width) { return text; }
        Object face=Theme.manager()==null?null:Theme.manager().effective("Font Selection").getValue();
        var key=new Elision(font,face,Theme.number("Font Scale",1),CustomTextRenderer.generation(),text,width);
        String cached=ELISIONS.get(key);if(cached!=null)return cached;
        String shortened = text;
        while (!shortened.isEmpty() && textWidth(font, shortened + "…", false) > width) {
            shortened = shortened.substring(0, shortened.offsetByCodePoints(shortened.length(), -1));
        }
        String result=shortened+"…";ELISIONS.put(key,result);if(ELISIONS.size()>1024){ELISIONS.remove(ELISIONS.keySet().iterator().next());}return result;
    }
    public static void scissorPush(GuiGraphicsExtractor graphics, double x, double y, double width, double height) {
        dev.riserecode.ui.render.UiBatch.clipBarrier(graphics);
        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    }
    public static void scissorPop(GuiGraphicsExtractor graphics) { dev.riserecode.ui.render.UiBatch.clipBarrier(graphics);graphics.disableScissor(); }
}
