package dev.riserecode.map;

import com.google.gson.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** World-owned destinations and a bounded recent death window, with immutable archive pages. */
public final class WaypointStore {
    public static final int DEATH_PAGE_SIZE=128;
    public record Waypoint(UUID id,String name,int x,int y,int z,String dimension,String group,int color,boolean visible,boolean tracked,boolean death,long timestamp,boolean reached) {
        public Waypoint(UUID id,String name,int x,int y,int z,String dimension,String group,int color,boolean visible,boolean tracked,boolean death){this(id,name,x,y,z,dimension,group,color,visible,tracked,death,0,false);}
        public Waypoint {
            Objects.requireNonNull(id);Objects.requireNonNull(name);Objects.requireNonNull(dimension);Objects.requireNonNull(group);
            if(name.isBlank()||name.length()>80||group.length()>80||dimension.length()>160||Math.abs((long)x)>30_000_000||Math.abs((long)z)>30_000_000||y < -2048||y>2048||timestamp<0){throw new IllegalArgumentException("Invalid waypoint");}color|=0xFF000000;
        }
        public Waypoint tracked(boolean value){return new Waypoint(id,name,x,y,z,dimension,group,color,visible,value,death,timestamp,reached);}
        public Waypoint visible(boolean value){return new Waypoint(id,name,x,y,z,dimension,group,color,value,tracked,death,timestamp,reached);}
        public Waypoint markReached(){return new Waypoint(id,name,x,y,z,dimension,group,color,visible,false,death,timestamp,true);}
    }
    private final LinkedHashMap<UUID,Waypoint> points=new LinkedHashMap<>();
    private final Map<Integer,JsonObject> pendingArchives=new LinkedHashMap<>();
    private final Map<String,UUID> latest=new LinkedHashMap<>();
    private long revision;
    private int archivePages;
    private boolean hideReached=true;
    public long revision(){return revision;}
    public int archivePages(){return archivePages;}
    public Map<Integer,JsonObject> pendingArchives(){return Map.copyOf(pendingArchives);}
    public void acknowledgeArchives(Set<Integer> pages){pages.forEach(pendingArchives::remove);}
    public boolean hideReached(){return hideReached;}
    public void hideReached(boolean value){hideReached=value;revision++;}
    public List<Waypoint> all(){return List.copyOf(points.values());}
    public List<Waypoint> displayed(){return points.values().stream().filter(p->!hideReached||!p.reached()).toList();}
    public boolean latest(Waypoint p){return p.id().equals(latest.get(p.dimension()));}
    public void put(Waypoint p){if(!p.death()&&!points.containsKey(p.id())&&points.values().stream().filter(point->!point.death()).count()>=4096){throw new IllegalStateException("Waypoint limit reached");}if(!p.equals(points.put(p.id(),p))){revision++;}}
    public void remove(UUID id){Waypoint removed=points.remove(id);if(removed!=null){
        if(id.equals(latest.get(removed.dimension()))){latest.remove(removed.dimension());points.values().stream().filter(p->p.death()&&p.dimension().equals(removed.dimension())).max(Comparator.comparingLong(Waypoint::timestamp)).ifPresent(p->latest.put(p.dimension(),p.id()));}revision++;}}
    public Waypoint add(String name,int x,int y,int z,String dimension){Waypoint p=new Waypoint(UUID.randomUUID(),name,x,y,z,dimension,"",0xFF64D8C4,true,false,false);put(p);return p;}
    public void death(int x,int y,int z,String dimension){death(x,y,z,dimension,System.currentTimeMillis());}
    public void death(int x,int y,int z,String dimension,long time){
        for(Waypoint p:all()){if(p.death()&&p.tracked()&&p.dimension().equals(dimension)){put(p.tracked(false));}}
        String date=DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time));
        Waypoint p=new Waypoint(UUID.randomUUID(),"Death · "+date,x,y,z,dimension,"Deaths",0xFFFF6161,true,true,true,time,false);put(p);latest.put(dimension,p.id());
        List<Waypoint> deaths=all().stream().filter(Waypoint::death).toList();
        if(deaths.size()>DEATH_PAGE_SIZE*2){List<Waypoint> page=deaths.stream().filter(d->!latest(d)).limit(DEATH_PAGE_SIZE).toList();
            pendingArchives.put(archivePages++,document(page));page.forEach(d->points.remove(d.id()));revision++;}
    }
    public void reached(double x,double y,double z,String dimension){for(Waypoint p:all()){if(p.death()&&!p.reached()&&p.dimension().equals(dimension)&&Math.pow(x-p.x(),2)+Math.pow(y-p.y(),2)+Math.pow(z-p.z(),2)<16){put(p.markReached());}}}
    public void clearDeaths(){points.values().removeIf(Waypoint::death);latest.clear();pendingArchives.clear();archivePages=0;revision++;}
    public static JsonObject document(List<Waypoint> values){JsonObject root=new JsonObject();root.addProperty("version",2);JsonArray list=new JsonArray();root.add("waypoints",list);for(Waypoint p:values){list.add(encode(p));}return root;}
    public JsonObject snapshot(){JsonObject root=document(all());root.addProperty("deathPages",archivePages);root.addProperty("hideReached",hideReached);JsonObject recent=new JsonObject();latest.forEach((dimension,id)->recent.addProperty(dimension,id.toString()));root.add("latestDeaths",recent);return root;}
    private static JsonObject encode(Waypoint p){
        JsonObject v=new JsonObject();v.addProperty("id",p.id().toString());v.addProperty("name",p.name());v.addProperty("x",p.x());v.addProperty("y",p.y());v.addProperty("z",p.z());v.addProperty("dimension",p.dimension());v.addProperty("group",p.group());v.addProperty("color",p.color());v.addProperty("visible",p.visible());v.addProperty("tracked",p.tracked());v.addProperty("death",p.death());v.addProperty("timestamp",p.timestamp());v.addProperty("reached",p.reached());return v;
    }
    public void load(JsonObject root){
        LinkedHashMap<UUID,Waypoint> loaded=new LinkedHashMap<>();
        if(root.has("waypoints")){for(JsonElement value:root.getAsJsonArray("waypoints")){JsonObject v=value.getAsJsonObject();Waypoint p=new Waypoint(UUID.fromString(v.get("id").getAsString()),v.get("name").getAsString(),v.get("x").getAsInt(),v.get("y").getAsInt(),v.get("z").getAsInt(),v.get("dimension").getAsString(),v.get("group").getAsString(),v.get("color").getAsInt(),v.get("visible").getAsBoolean(),v.get("tracked").getAsBoolean(),v.get("death").getAsBoolean(),v.has("timestamp")?v.get("timestamp").getAsLong():0,v.has("reached")&&v.get("reached").getAsBoolean());loaded.put(p.id(),p);if(loaded.size()>4352){throw new IllegalArgumentException("Waypoint limit");}}}
        int pages=root.has("deathPages")?root.get("deathPages").getAsInt():0;if(pages<0){throw new IllegalArgumentException("Invalid archive count");}
        Map<String,UUID> recent=new LinkedHashMap<>();if(root.has("latestDeaths")){root.getAsJsonObject("latestDeaths").entrySet().forEach(e->recent.put(e.getKey(),UUID.fromString(e.getValue().getAsString())));}else{loaded.values().stream().filter(Waypoint::death).forEach(p->recent.put(p.dimension(),p.id()));}
        points.clear();points.putAll(loaded);latest.clear();latest.putAll(recent);archivePages=pages;hideReached=!root.has("hideReached")||root.get("hideReached").getAsBoolean();pendingArchives.clear();revision++;
    }
}
