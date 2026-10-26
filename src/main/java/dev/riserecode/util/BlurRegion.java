package dev.riserecode.util;

/** Texture coordinates, with the same bottom-left origin as the screen-quad shaders. */
public record BlurRegion(double left,double bottom,double right,double top) {
    public BlurRegion union(BlurRegion other){return new BlurRegion(Math.min(left,other.left),Math.min(bottom,other.bottom),Math.max(right,other.right),Math.max(top,other.top));}
    public BlurRegion expand(double x,double y){return new BlurRegion(Math.max(0,left-x),Math.max(0,bottom-y),Math.min(1,right+x),Math.min(1,top+y));}
    public BlurRegion pixels(int width,int height){return new BlurRegion(Math.max(0,Math.floor(left*width))/width,Math.max(0,Math.floor(bottom*height))/height,Math.min(width,Math.ceil(right*width))/width,Math.min(height,Math.ceil(top*height))/height);}
    public boolean empty(){return right<=left||top<=bottom;}
}
