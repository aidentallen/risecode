import java.util.*;
public class Oracle {
 static bet p; static String name="";static int tick;
 public static void main(String[] args){
  System.out.println("trace,tick,x,y,z,yaw,vx,vz,ground,sneak,walk,ride,partial,pitch,side");
  for(int i=1;i<=3;i++)step("idle",0,64,0,0,0,0,true,false,false,false);
  for(int i=1;i<=16;i++)step("sprint",0,64,i*.3,0,0,.3,true,false,true,false);
  for(int i=1;i<=8;i++)step("strafe",i*.34,64,0,0,.34,0,true,false,true,false);
  double[] ys={64.4,64.7,64.9,64.8,64.5,64.1,64};for(int i=0;i<ys.length;i++)step("jump",0,ys[i],i*.2,0,0,.2,i==6,false,true,false);
  for(double y:new double[]{62,60,57})step("fall",0,y,0,0,0,0,false,false,false,false);
  for(int i=1;i<=6;i++)step("sneak",0,64,i*.08,0,0,.08,true,true,false,false);
  float[] yaws={-179,-140,140,179};for(int i=0;i<4;i++)step("yawwrap",0,64,(i+1)*.3,yaws[i],0,.3,true,false,true,false);
  for(int i=1;i<=5;i++)step("fast-side",i*3.,64,0,0,3,0,true,false,true,false);
  for(int i=1;i<=5;i++)step("fast-forward",0,64,i*3.,0,0,3,true,false,true,false);
  for(double x:new double[]{40,-40,0})step("teleport",x,64,0,0,0,0,true,false,false,false);
  for(int i=1;i<=6;i++)step("ride",0,64,i*.25,0,0,.25,true,false,false,true);
  for(int i=1;i<=8;i++)step("air",i*.2,64+i*.6,0,45,.2,0,false,false,true,false);
 }
 static void step(String trace,double x,double y,double z,float yaw,double vx,double vz,boolean ground,boolean sneak,boolean walk,boolean ride){
  if(!name.equals(trace)){name=trace;tick=0;p=new bet();p.t=p.q=p.bu=p.br=64;p.aI=trace.equals("yawwrap")?179:0;}
  tick++;
  p.p=p.s;p.q=p.t;p.r=p.u;p.bq=p.bt;p.br=p.bu;p.bs=p.bv;p.aJ=p.aI;p.bn=p.bo;p.L=p.M;
  if(walk)p.M=(float)((double)p.M+(double)(float)Math.sqrt((x-p.s)*(x-p.s)+(z-p.u)*(z-p.u))*.6D);
  p.s=x;p.t=y;p.u=z;p.aI=yaw;p.sneak=sneak;
  float target=(float)Math.sqrt(vx*vx+vz*vz);if(target>.1F)target=.1F;if(!ground)target=0;p.bo+=(target-p.bo)*.4F;if(ride)p.bo=0;
  double dx=x-p.bt,dy=y-p.bu,dz=z-p.bv;
  if(dx>10||dx< -10)p.bt=p.bq=x;if(dy>10||dy< -10)p.bu=p.br=y;if(dz>10||dz< -10)p.bv=p.bs=z;
  p.bt+=dx*.25D;p.bu+=dy*.25D;p.bv+=dz*.25D;
  for(float part:new float[]{0,.125F,.5F,.9375F,1}){
   bfl.angles.clear();new bkp(new bln()).a(p,0,0,part,0,0,0,.0625F);
   System.out.println(trace+","+tick+","+x+","+y+","+z+","+yaw+","+vx+","+vz+","+ground+","+sneak+","+walk+","+ride+","+part+","+bfl.angles.get(0)+","+bfl.angles.get(1));
  }
 }
}
class pr {}
class bet extends pr {public double p,q,r,s,t,u,bq,br,bs,bt,bu,bv;public float aJ,aI,bn,bo,L,M;boolean sneak;public boolean a(){return true;}public boolean ax(){return false;}public boolean a(wo v){return true;}public jy k(){return new jy();}public boolean av(){return sneak;}}
interface blb<T extends pr>{void a(T p,float a,float b,float c,float d,float e,float f,float g);boolean b();}
class wo{public static final wo a=new wo();}class jy{}
class bln{public void a(jy t){}public bbr g(){return new bbr();}}
class bbr{public void c(float s){}}
class bfl{static List<Float>angles=new ArrayList<>();public static void c(float a,float b,float c,float d){}public static void E(){}public static void F(){}public static void b(float x,float y,float z){}public static void b(float a,float x,float y,float z){angles.add(a);}}
