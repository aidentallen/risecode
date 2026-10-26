package dev.riserecode.cosmetics;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.Map;

/** Immutable, profile-owned selections. Missing assets retain their identity. */
public record CosmeticLoadout(boolean visible, Map<CosmeticCategory, Item> items) {
    public record Item(String id, int primary, int secondary, double opacity, double scale,
                       double x, double y, double z, double speed, double motion, double density) {
        public Item {
            if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,95}")) { throw new IllegalArgumentException("Invalid cosmetic id"); }
            range(opacity,0,1); range(scale,.25,12); range(x,-4,4); range(y,-4,4); range(z,-4,4);
            range(speed,0,16); range(motion,0,12); range(density,0,12);
        }
        public static Item defaults(String id) {
            int primary=0xFFFFFFFF,secondary=0xFF65E5DB;
            if(id.startsWith("cape-")){primary=0xFF8B9EF2;secondary=0xFF76E2D7;}
            else if(id.startsWith("wings-")){primary=id.endsWith("butterfly")?0xFFAA6F9C:0xFF474153;secondary=id.endsWith("mechanical")?0xFF74CED0:0xFFC7A6E8;}
            else if(id.startsWith("head-")){primary=id.endsWith("halo")||id.endsWith("crown")?0xFFE7C779:0xFF45404D;secondary=0xFFB7A3D6;}
            else if(id.startsWith("tail-")){primary=id.endsWith("fox")?0xFFC58B54:0xFF5B536A;secondary=0xFFE6DDD1;}
            else if(!id.startsWith("pet-")&&!id.startsWith("import-")){primary=0xFF9ADBD7;secondary=0xFFC7A6E8;}
            return new Item(id,primary,secondary,1,1,0,0,0,1,1,1);
        }
        private static void range(double value,double min,double max) { if (!Double.isFinite(value)||value<min||value>max) { throw new IllegalArgumentException("Cosmetic value out of range"); } }
    }
    public CosmeticLoadout { items = Map.copyOf(items); }
    public static CosmeticLoadout empty() { return new CosmeticLoadout(true,Map.of()); }
    public CosmeticLoadout with(CosmeticCategory category,Item item) {
        var next=new EnumMap<CosmeticCategory,Item>(CosmeticCategory.class);next.putAll(items);
        if(item==null){next.remove(category);}else{next.put(category,item);}return new CosmeticLoadout(visible,next);
    }
    public JsonObject json() {
        var root=new JsonObject();root.addProperty("version",1);root.addProperty("visible",visible);var entries=new JsonObject();root.add("items",entries);
        items.forEach((category,item)->{var v=new JsonObject();v.addProperty("id",item.id);v.addProperty("primary",item.primary);v.addProperty("secondary",item.secondary);
            v.addProperty("opacity",item.opacity);v.addProperty("scale",item.scale);v.addProperty("x",item.x);v.addProperty("y",item.y);v.addProperty("z",item.z);
            v.addProperty("speed",item.speed);v.addProperty("motion",item.motion);v.addProperty("density",item.density);entries.add(category.name(),v);});return root;
    }
    public static CosmeticLoadout read(JsonObject root) {
        if(root.get("version").getAsInt()!=1){throw new IllegalArgumentException("Unsupported cosmetics version");}
        var items=new EnumMap<CosmeticCategory,Item>(CosmeticCategory.class);
        root.getAsJsonObject("items").entrySet().forEach(e->{var v=e.getValue().getAsJsonObject();items.put(CosmeticCategory.valueOf(e.getKey()),new Item(v.get("id").getAsString(),
            v.get("primary").getAsInt(),v.get("secondary").getAsInt(),v.get("opacity").getAsDouble(),v.get("scale").getAsDouble(),v.get("x").getAsDouble(),v.get("y").getAsDouble(),v.get("z").getAsDouble(),
            v.get("speed").getAsDouble(),v.get("motion").getAsDouble(),v.get("density").getAsDouble()));});
        return new CosmeticLoadout(root.get("visible").getAsBoolean(),items);
    }
}
