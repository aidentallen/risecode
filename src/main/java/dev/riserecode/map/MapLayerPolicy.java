package dev.riserecode.map;

/** Stable altitude bands and delayed roof decisions. No world data leaves the client. */
public final class MapLayerPolicy {
    public enum Mode { AUTO, SURFACE, CAVE }
    public enum Layer { SURFACE, CAVE }
    /** Cave data is stored per height band; every altitude inside a band shares its tiles. */
    public static final int BAND=16;
    public static int band(int y){return Math.floorDiv(y,BAND)*BAND;}
    private Layer layer=Layer.SURFACE;
    private int votes,altitude=Integer.MIN_VALUE;
    public void reset(){layer=Layer.SURFACE;votes=0;altitude=Integer.MIN_VALUE;}
    public void update(boolean enclosed,int y){
        if(enclosed==(layer==Layer.CAVE)){votes=0;}else if(++votes>=(enclosed?12:24)){layer=enclosed?Layer.CAVE:Layer.SURFACE;votes=0;}
        altitude=altitude(altitude,y);
    }
    public static int altitude(int previous,int y){return previous==Integer.MIN_VALUE||Math.abs((long)y-previous)>3?y:previous;}
    public Layer layer(Mode mode){return switch(mode){case AUTO->layer;case SURFACE->Layer.SURFACE;case CAVE->Layer.CAVE;};}
    public int altitude(){return altitude==Integer.MIN_VALUE?64:altitude;}
}
