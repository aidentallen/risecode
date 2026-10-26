package dev.riserecode;

import dev.riserecode.util.BlurRegion;
import java.util.Random;

/** Check sampling coverage, including odd dimensions, edges and disjoint moving requesters. */
final class BlurRegionChecks {
    private BlurRegionChecks(){ }
    static int run(){
        int checks=0;Random random=new Random(242);
        for(int n=0;n<400;n++){
            int sw=17+random.nextInt(1300),sh=13+random.nextInt(900),tw=(sw+1)/2,th=(sh+1)/2;
            double x=random.nextDouble(),y=random.nextDouble(),offset=.25+random.nextDouble()*1.25;
            var output=new BlurRegion(x,y,Math.min(1,x+.02),Math.min(1,y+.04)).pixels(tw,th);
            boolean up=(n&1)==0;double support=(up?offset:offset*.5)+1.5;
            var input=output.expand(support/sw,support/sh).pixels(sw,sh);
            for(int i=0;i<20;i++){
                // Actual shader tap plus bilinear half-texel support. Include both output edges.
                double u=i%2==0?output.left():output.right(),v=i%3==0?output.bottom():output.top();
                double dx=(random.nextDouble()*2-1)*(up?offset:offset*.5),dy=(random.nextDouble()*2-1)*(up?offset:offset*.5);
                double sx=Math.clamp(u+(dx+(i%2==0?-.5:.5))/sw,0,1),sy=Math.clamp(v+(dy+(i%2==0?-.5:.5))/sh,0,1);
                if(sx<input.left()-1e-12||sx>input.right()+1e-12||sy<input.bottom()-1e-12||sy>input.top()+1e-12)throw new AssertionError("blur reads unwritten source support");checks++;
            }
            if(output.left()>x||output.bottom()>y||output.right()<Math.min(1,x+.02)||output.top()<Math.min(1,y+.04))throw new AssertionError("outward pixel rounding clips output");checks++;
        }
        var merged=new BlurRegion(.05,.1,.2,.3).union(new BlurRegion(.8,.7,.95,.9));
        if(merged.left()!=.05||merged.bottom()!=.1||merged.right()!=.95||merged.top()!=.9)throw new AssertionError("request union loses a requester");
        for(int code=1;code<=1024;code++){
            float sigma=code/256f;
            // Conservative first omitted distance after atlas quantization/bilinear support.
            float distance=(float)Math.ceil(sigma*6+1)-.75f;
            float z=distance/(sigma*1.41421356237f),t=1/(1+.3275911f*z);
            float erf=1-(((((1.061405429f*t-1.453152027f)*t)+1.421413741f)*t- .284496736f)*t+.254829592f)*t*(float)Math.exp(-z*z);
            if(.5f-.5f*erf!=0)throw new AssertionError("cropped bitmap Gaussian tail is still visible");checks++;
        }
        return checks+1;
    }
}
