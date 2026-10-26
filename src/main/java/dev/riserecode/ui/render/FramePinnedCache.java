package dev.riserecode.ui.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Consumer;

/** Bounded extraction cache. Referenced resources survive through drawing, including discard. */
public final class FramePinnedCache<K,V> {
    private int maximum;
    private final Consumer<V> release;
    private final LinkedHashMap<K,V> values=new LinkedHashMap<>(16,.75f,true);
    private final Set<K> pinned=new HashSet<>();
    private final ArrayList<V> retired=new ArrayList<>();
    public FramePinnedCache(int maximum,Consumer<V> release){if(maximum<1){throw new IllegalArgumentException("Invalid capacity");}this.maximum=maximum;this.release=release;}
    public V get(K key){V value=values.get(key);if(value!=null){pinned.add(key);}return value;}
    public int size(){return values.size();}
    public int ownedSize(){return values.size()+retired.size();}
    public int pinnedSize(){return pinned.size()+retired.size();}
    public void maximum(int maximum){if(maximum<1){throw new IllegalArgumentException("Invalid capacity");}this.maximum=maximum;trim();}
    public int overage(){return Math.max(0,ownedSize()-maximum);}
    public java.util.List<V> owned(){var result=new ArrayList<V>(values.values());result.addAll(retired);return java.util.List.copyOf(result);}
    public void remove(K key){V old=values.remove(key);if(old!=null){if(pinned.remove(key)){retired.add(old);}else{release.accept(old);}}}
    public void trim(){while(ownedSize()>maximum){if(!evict()){break;}}}
    private boolean evict(){var iterator=values.entrySet().iterator();while(iterator.hasNext()){var entry=iterator.next();if(!pinned.contains(entry.getKey())){release.accept(entry.getValue());iterator.remove();return true;}}return false;}
    /** A currently drawn working set may temporarily exceed its budget. */
    public void putPinned(K key,V value){if(values.containsKey(key)){remove(key);}values.put(key,value);pinned.add(key);trim();}
    public boolean makeRoom(){
        if(values.size()+retired.size()<maximum){return true;}
        var iterator=values.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();if(!pinned.contains(entry.getKey())){release.accept(entry.getValue());iterator.remove();return true;}
        }
        return false;
    }
    public void put(K key,V value){
        if(values.containsKey(key)||values.size()+retired.size()>=maximum){throw new IllegalStateException("Reserve cache space before upload");}
        values.put(key,value);pinned.add(key);
    }
    public void beginFrame(){retired.forEach(release);retired.clear();pinned.clear();trim();}
    public void discard(){retired.addAll(values.values());values.clear();}
}
