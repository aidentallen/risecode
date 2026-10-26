package dev.riserecode.ui.render;

import java.util.ArrayList;
import java.util.Random;

public final class OrderedBatchChecks {
    private record Command(int id,int material,int clip,int x,int y,int w,int h){
        boolean overlaps(Command b){return x<b.x+b.w&&x+w>b.x&&y<b.y+b.h&&y+h>b.y;}
    }
    private record Key(int material,int clip,int x,int y,int w,int h){
        Key(Command c){this(c.material,c.clip,c.x,c.y,c.w,c.h);}
    }
    private OrderedBatchChecks(){ }
    public static int run(){
        Random random=new Random(242);int checks=0;
        var cache=new OrderedBatches.Cache<Key>(16);
        for(int fixture=0;fixture<500;fixture++){
            var original=new ArrayList<Command>();for(int i=0;i<40;i++){original.add(new Command(i,random.nextInt(3),random.nextInt(2),random.nextInt(32),random.nextInt(32),1+random.nextInt(12),1+random.nextInt(12)));}
            var pending=new ArrayList<>(original);var order=new ArrayList<Command>();
            OrderedBatches.emit(pending,(a,b)->a.material==b.material&&a.clip==b.clip,Command::overlaps,run->{for(var value:run){if(value.material!=run.getFirst().material||value.clip!=run.getFirst().clip)throw new AssertionError("mixed batch material/clip");}order.addAll(run);});
            if(!pending.isEmpty()||order.size()!=original.size())throw new AssertionError("lost or retained commands");
            var reference=new ArrayList<>(original);var expected=new ArrayList<Command>();
            while(!reference.isEmpty()){
                var first=reference.getFirst();
                for(int i=0;i<reference.size();){var candidate=reference.get(i);boolean blocked=false;
                    if(first.material==candidate.material&&first.clip==candidate.clip){for(int j=0;j<i;j++)if(reference.get(j).overlaps(candidate)){blocked=true;break;}
                        if(!blocked){expected.add(reference.remove(i));continue;}}
                    i++;
                }
            }
            if(!order.equals(expected))throw new AssertionError("dependency scheduler changed painter output");
            var keys=new ArrayList<>(original.stream().map(Key::new).toList());
            var plan=cache.get(keys,original,(a,b)->a.material==b.material&&a.clip==b.clip,Command::overlaps);
            var fresh=new ArrayList<>(original.stream().map(c->new Command(c.id+100,c.material,c.clip,c.x,c.y,c.w,c.h)).toList());
            var reused=cache.get(keys,fresh,(a,b)->{throw new AssertionError("cached plan rebuilt materials");},(a,b)->{throw new AssertionError("cached plan rebuilt overlap graph");});
            if(plan!=reused)throw new AssertionError("identical geometry failed to reuse plan");
            var freshOrder=new ArrayList<Command>();reused.emit(fresh,freshOrder::addAll);
            if(!fresh.isEmpty()||freshOrder.size()!=order.size())throw new AssertionError("cached plan lost fresh commands");
            for(int i=0;i<order.size();i++)if(freshOrder.get(i).id!=order.get(i).id+100)throw new AssertionError("cached plan retained previous frame payload");
            // Caller-owned key lists cannot mutate an existing cache entry.
            keys.clear();
            if(cache.get(original.stream().map(Key::new).toList(),original,(a,b)->{throw new AssertionError("mutable cache key retained");},Command::overlaps)!=plan)throw new AssertionError("key snapshot changed");
            if(cache.size()>16)throw new AssertionError("unbounded ordering cache");checks+=4;
            int[] positions=new int[40];for(int i=0;i<order.size();i++)positions[order.get(i).id]=i;
            for(int i=0;i<40;i++)for(int j=i+1;j<40;j++)if(original.get(i).overlaps(original.get(j))){if(positions[i]>=positions[j])throw new AssertionError("overlapping translucent commands reordered");checks++;}
        }
        int prior=cache.size();cache.clear();if(prior!=16||cache.size()!=0)throw new AssertionError("ordering cache did not evict/clear");checks++;
        var nullable=new ArrayList<Command>();nullable.add(null);nullable.add(new Command(1,0,0,0,0,1,1));
        var nonNull=new ArrayList<Command>();OrderedBatches.emit(nullable,(a,b)->true,Command::overlaps,nonNull::addAll);
        if(!nullable.isEmpty()||nonNull.size()!=1||nonNull.getFirst().id!=1)throw new AssertionError("null placeholders changed ordering");checks++;
        return checks;
    }
}
