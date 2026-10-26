package dev.riserecode;

import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.util.ColorUtil;

/** Exhaustive target-switch coverage independent of a graphics device or a live player. */
public final class GlassChecks {
    private static int checks;
    private static void check(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    private static boolean on(int mask,int bit){return (mask&(1<<bit))!=0;}
    private static GlassPolicy policy(int m,GlassMaterial shared,GlassMaterial body,GlassMaterial armor,GlassMaterial items){
        return new GlassPolicy(on(m,0),on(m,1),on(m,2),on(m,3),on(m,4),on(m,5),on(m,6),on(m,7),on(m,8),on(m,9),on(m,10),on(m,11),on(m,12),on(m,13),on(m,14),shared,body,armor,items);
    }
    private static GlassMaterial material(int color){var paint=new ColorSetting.Paint(color,color,ColorSetting.Mode.SOLID,ColorUtil.GradientDirection.HORIZONTAL,.18,100);return new GlassMaterial(paint,.22,.35,.25,.8,paint,.85,1.5,.8,10,true,.35,1,.12,true);}
    public static void main(String[] args){
        var shared=material(0xFFFFFFFF);var body=material(0xFFAA99FF);var armor=material(0xFF11CCFF);var items=material(0xFF77FFAA);
        // Every combination of all fifteen target switches: changes to one must not leak
        // into any unrelated component, owner, hand or equipment slot.
        for(int m=0;m<32768;m++){
            var p=policy(m,shared,body,armor,items);
            check((p.select(true,Component.BODY,View.WORLD,true,Slot.NONE)==body)==on(m,0),"self body");
            check((p.select(false,Component.BODY,View.WORLD,true,Slot.NONE)==body)==(on(m,5)&&on(m,6)),"other body");
            for(boolean self:new boolean[]{true,false}){
                for(int slot=0;slot<4;slot++){
                    var selected=p.select(self,Component.ARMOR,View.WORLD,true,Slot.values()[slot+1]);
                    check((selected==armor)==((self?on(m,2):on(m,5)&&on(m,7))&&on(m,11+slot)),"armor slot");
                }
                for(int hand=0;hand<2;hand++){
                    boolean main=hand==0,filter=on(m,main?9:10);
                    check((p.select(self,Component.ITEM,View.WORLD,main,Slot.NONE)==items)==(filter&&(self?on(m,4):on(m,5)&&on(m,8))),"world item");
                    if(self){
                        check((p.select(true,Component.ITEM,View.HAND,main,Slot.NONE)==items)==(filter&&on(m,3)),"first person item");
                        check((p.select(true,Component.ARM,View.HAND,main,Slot.NONE)==body)==(filter&&on(m,1)),"native arm inherits body");
                    }
                }
            }
        }
        try{new GlassMaterial(shared.color(),Double.NaN,.35,.25,.8,shared.edges(),.8,1,.8,10,true,.3,1,.1,true);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("Glass policy checks passed ("+checks+" assertions)");
    }
    private GlassChecks() { }
}
