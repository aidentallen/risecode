import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
public class MeasureUxContrast {
 static double luminance(int rgb){double[] c={((rgb>>16)&255)/255.,((rgb>>8)&255)/255.,(rgb&255)/255.};for(int i=0;i<3;i++)c[i]=c[i]<=.04045?c[i]/12.92:Math.pow((c[i]+.055)/1.055,2.4);return c[0]*.2126+c[1]*.7152+c[2]*.0722;}
 public static void main(String[] args)throws Exception{boolean pass=true;System.out.println("[");for(int i=0;i<args.length;i++){var image=ImageIO.read(new File(args[i]));int scale=image.getWidth()/320;System.out.print("{\"file\":\""+new File(args[i]).getName()+"\",\"regions\":[");int index=0;for(int y:new int[]{44,82}){var pixels=new ArrayList<Integer>();for(int py=y*scale;py<(y+24)*scale;py++)for(int x=32*scale;x<225*scale;x++)pixels.add(image.getRGB(x,py));pixels.sort(Comparator.comparingDouble(MeasureUxContrast::luminance));int bg=pixels.get(pixels.size()/2),ink=pixels.getLast();double ratio=(luminance(ink)+.05)/(luminance(bg)+.05);pass&=ratio>=4.5;System.out.printf(java.util.Locale.ROOT,"%s{\"role\":\"%s\",\"background\":\"%06X\",\"ink\":\"%06X\",\"contrast\":%.3f}",index++==0?"":",",y==44?"secondary":"muted",bg&0xFFFFFF,ink&0xFFFFFF,ratio);}System.out.print("]}"+(i+1==args.length?"\n":",\n"));}System.out.println("]");if(!pass)throw new AssertionError("A sampled full-ink/background contrast ratio is below4.5");}
}
