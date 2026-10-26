package dev.riserecode.ui.menu;

import dev.riserecode.ui.theme.*;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fixed seeded pool, animated analytically; no world particles or per-frame particle allocations. */
public final class BackgroundParticles {
    private static final double[] X=new double[1536],Y=new double[1536],Z=new double[1536];
    static{var random=new java.util.Random(0x72697365L);for(int i=0;i<X.length;i++){X[i]=random.nextDouble();Y[i]=random.nextDouble();Z[i]=random.nextDouble();}}
    private BackgroundParticles(){}
    public static void draw(GuiGraphicsExtractor g,double opacity){draw(g,opacity,0,0,g.guiWidth(),g.guiHeight());}
    public static void draw(GuiGraphicsExtractor g,double opacity,double left,double top,double width,double height){
        if(dev.riserecode.performance.PerformancePolicy.frame().menuParticles()){return;}
        var theme=Theme.manager();var kind=theme.choice("Background Particles",ThemeManager.Particles.class);if(kind==ThemeManager.Particles.NONE){return;}
        boolean pixel=theme.choice("Particle Style",ThemeManager.ParticleStyle.class)==ThemeManager.ParticleStyle.MINECRAFT;
        double time=Theme.bool("Reduced Motion",false)?0:Theme.gradientTime()*Theme.number("Particle Speed",1);
        int count=(int)Theme.number("Particle Density",70),color=theme.color("Particle Color");
        for(int i=0;i<count;i++){
            double speed=.02+Z[i]*.035,wind=Theme.number("Particle Wind",.15);
            double x=((X[i]+time*wind*.02+Math.sin(time*.3+i)*.003)%1+1)%1*width+left;
            double y=((Y[i]+time*speed)%1)*height+top;double size=Theme.number("Particle Size",1.5)*(.6+Z[i]);
            if(pixel){x=Math.floor(x);y=Math.floor(y);size=Math.max(1,Math.rint(size));}
            int tint=Theme.opacity(color,opacity*Theme.number("Particle Opacity",.35)*(.4+.6*Z[i]));
            if(kind==ThemeManager.Particles.RAIN){if(pixel){RenderUtil.drawRoundedRect(g,x,y,Math.max(1,size*.5),size*6,0,tint);}else{RenderUtil.line(g,x,y,x+wind*size*2,y+size*6,Math.max(.5,size*.5),tint);}}
            else if(kind==ThemeManager.Particles.ABSTRACT){RenderUtil.line(g,x,y,x+Math.cos(time+i)*size*4,y+Math.sin(time+i)*size*4,.7,tint);}
            else{RenderUtil.drawRoundedRect(g,x,y,size,size,pixel?0:size/2,tint);}
        }
    }
}
