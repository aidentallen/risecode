package dev.riserecode.render;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Client-owned packed section matches. Search cursors charge both empty sections and matches. */
public final class SectionBlockIndex {
    public record Section(int x, int y, int z) { }
    public record Position(int x, int y, int z) {
    }
    private static final class Node {
        private final Section section;
        private final BitSet bits = new BitSet(4096);
        private Node next, previous;
        private boolean live = true;
        Node(Section section) { this.section = section; }
    }
    private final Map<Section, Node> sections = new HashMap<>();
    private Node first, last, lastSetNode;
    private Section lastSetSection;
    private long revision, epoch;
    public long revision() { return revision; }
    public int sectionCount() { return sections.size(); }
    public void set(int x, int y, int z, boolean match) {
        int sx=x>>4,sy=y>>4,sz=z>>4;
        if(lastSetSection==null||lastSetSection.x!=sx||lastSetSection.y!=sy||lastSetSection.z!=sz){lastSetSection=new Section(sx,sy,sz);lastSetNode=sections.get(lastSetSection);}
        Section section=lastSetSection;Node node=lastSetNode;
        if (node == null) {
            if (!match) { return; }
            node = new Node(section); sections.put(section,node);lastSetNode=node;
            if (last == null) { first=node; } else { last.next=node; node.previous=last; }
            last=node;
        }
        int bit=((y&15)<<8)|((z&15)<<4)|(x&15);
        if (node.bits.get(bit) != match) { node.bits.set(bit,match); revision++; }
    }
    public void removeChunk(int x, int z, int minSection, int count) {
        lastSetSection=null;lastSetNode=null;
        for(int y=minSection;y<minSection+count;y++) {
            Node node=sections.remove(new Section(x,y,z)); if(node==null){continue;}
            node.live=false;
            if(node.previous==null){first=node.next;}else{node.previous.next=node.next;}
            if(node.next==null){last=node.previous;}else{node.next.previous=node.previous;}
            // Keep next reachable for searches already suspended at this node.
            revision++;
        }
    }
    public void clear() { sections.clear(); first=last=lastSetNode=null;lastSetSection=null; revision++; epoch++; }
    public Search search(Position center, int maximum) { return new Search(center,maximum); }
    public final class Search {
        private record Candidate(Position pos,double distance) { }
        private final Position center;
        private final int maximum;
        private final long startedRevision=revision, startedEpoch=epoch;
        private final Node stop=last;
        private Node node=first;
        private int bit;
        private final PriorityQueue<Candidate> nearest = new PriorityQueue<>((a,b)->Double.compare(b.distance,a.distance));
        private final ArrayList<Position> result = new ArrayList<>();
        private boolean done;
        private long work;
        private Search(Position center,int maximum) {
            if(maximum<1||maximum>4096){throw new IllegalArgumentException("Invalid result limit");}
            this.center=center;this.maximum=maximum;
        }
        /** At most budget candidates/section transitions/heap removals, also bounded by a shared deadline. */
        public boolean step(int budget,long deadline) {
            if(startedEpoch!=epoch){done=true;result.clear();nearest.clear();node=null;return true;}
            while(!done && budget-- > 0 && System.nanoTime()<deadline) {
                work++;
                if(node!=null) {
                    if(bit==0&&nearest.size()==maximum&&minimumDistance(node.section)>nearest.element().distance){node=node==stop?null:node.next;continue;}
                    int next=node.live?node.bits.nextSetBit(bit):-1;
                    if(next<0){node=node==stop?null:node.next;bit=0;continue;}
                    bit=next+1;Section s=node.section;
                    int x=(s.x<<4)+(next&15),y=(s.y<<4)+(next>>8),z=(s.z<<4)+((next>>4)&15);
                    double dx=(double)x-center.x,dy=(double)y-center.y,dz=(double)z-center.z,distance=dx*dx+dy*dy+dz*dz;
                    if(nearest.size()<maximum || distance<nearest.element().distance) {
                        if(nearest.size()==maximum){nearest.remove();}
                        nearest.add(new Candidate(new Position(x,y,z),distance));
                    }
                } else if(!nearest.isEmpty()) { result.add(nearest.remove().pos); }
                else { done=true; }
            }
            return done;
        }
        private double minimumDistance(Section section){
            double dx=Math.max(0,Math.max((section.x<<4)-center.x,center.x-((section.x<<4)+15)));
            double dy=Math.max(0,Math.max((section.y<<4)-center.y,center.y-((section.y<<4)+15)));
            double dz=Math.max(0,Math.max((section.z<<4)-center.z,center.z-((section.z<<4)+15)));
            return dx*dx+dy*dy+dz*dz;
        }
        public boolean current() { return startedEpoch==epoch&&startedRevision==revision; }
        public long work() { return work; }
        /** Farthest first; no additional sort or copy at completion. */
        public List<Position> result() { if(!done){throw new IllegalStateException("Search pending");}return java.util.Collections.unmodifiableList(result); }
    }
}
