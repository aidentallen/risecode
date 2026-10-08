package dev.riserecode.ui.component;

import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Original vectors on a 24-unit grid with consistent round-capped strokes and optical insets. */
public enum Icon {
    SEARCH, COMBAT, MOVEMENT, PLAYER, RENDER, WORLD, UTILITY, HUD, PROFILES, FRIENDS, THEMES;
    private record Arc(double x,double y,double r,double from,double to) { }
    private static final java.util.Map<Arc,double[]> ARCS = new java.util.HashMap<>();
    public void draw(GuiGraphicsExtractor g, double x, double y, int color) {
        g.pose().pushMatrix(); g.pose().translate((float) x, (float) y); g.pose().scale(10f / 24, 10f / 24);
        dev.riserecode.ui.render.UiBatch.begin(g);
        try { switch (this) {
            case SEARCH -> { arc(g, 10, 10, 7, 0, 360, color); path(g, color, 15,15,21,21); }
            case COMBAT -> { path(g,color,4,3,16,15,20,11,20,3,12,3,8,7); path(g,color,3,20,8,15); path(g,color,3,13,11,21); path(g,color,3,3,15,15); }
            case MOVEMENT -> { arc(g,15,4,1.7,0,360,color); path(g,color,9,8,13,7,16,12,21,12); path(g,color,13,7,10,14,5,20); path(g,color,10,14,16,16,17,21); path(g,color,9,8,5,12); }
            case PLAYER -> { arc(g,12,7,4,0,360,color); arc(g,12,21,8,180,360,color); }
            case FRIENDS -> { arc(g,9,7,3.5,0,360,color); arc(g,8,21,6.5,180,360,color); arc(g,17,8,2.8,-90,90,color); arc(g,17,20,5,-90,0,color); }
            case RENDER -> { arc(g,12,12,3,0,360,color); path(g,color,2,12,6,7,12,5,18,7,22,12,18,17,12,19,6,17,2,12); }
            case WORLD -> { arc(g,12,12,9,0,360,color); path(g,color,3,12,21,12); arc(g,12,12,5,70,290,color); arc(g,12,12,5,-110,110,color); }
            case UTILITY -> { path(g,color,15,3,13,8,17,12,22,10,21,15,17,18,13,17,6,23,1,18,8,11,7,7,10,3,15,3); }
            case HUD -> { rect(g,3,3,7,7,color); rect(g,14,3,7,7,color); rect(g,3,14,7,7,color); rect(g,14,14,7,7,color); }
            case PROFILES -> { rect(g,7,3,14,14,color); path(g,color,3,7,3,21,17,21); path(g,color,11,7,17,7); path(g,color,11,11,17,11); }
            case THEMES -> { path(g,color,5,3,5,21); path(g,color,12,3,12,21); path(g,color,19,3,19,21); rect(g,2.5,7,5,4,color); rect(g,9.5,14,5,4,color); rect(g,16.5,5,5,4,color); }
        }
        } finally { dev.riserecode.ui.render.UiBatch.end(); g.pose().popMatrix(); }
    }
    private static void rect(GuiGraphicsExtractor g,double x,double y,double w,double h,int c) { path(g,c,x,y,x+w,y,x+w,y+h,x,y+h,x,y); }
    private static void path(GuiGraphicsExtractor g,int c,double... points) {
        for(int i=2;i<points.length;i+=2) {
            RenderUtil.line(g,points[i-2],points[i-1],points[i],points[i+1],1.7,c);
            RenderUtil.drawRoundedRect(g,points[i-2]-.85,points[i-1]-.85,1.7,1.7,.85,c);
        }
        RenderUtil.drawRoundedRect(g,points[points.length-2]-.85,points[points.length-1]-.85,1.7,1.7,.85,c);
    }
    private static void arc(GuiGraphicsExtractor g,double x,double y,double r,double from,double to,int c) {
        double[] p=ARCS.computeIfAbsent(new Arc(x,y,r,from,to), ignored -> {
            int n=Math.max(4,(int)Math.ceil((to-from)/15)); double[] points=new double[(n+1)*2];
            for(int i=0;i<=n;i++){ double a=Math.toRadians(from+(to-from)*i/n);points[i*2]=x+Math.cos(a)*r;points[i*2+1]=y+Math.sin(a)*r; } return points;
        }); path(g,c,p);
    }
}
