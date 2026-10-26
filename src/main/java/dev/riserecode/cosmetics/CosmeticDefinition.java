package dev.riserecode.cosmetics;

/** Identical catalog representation for bundled and downloaded assets. */
public record CosmeticDefinition(String id, String name, CosmeticCategory category, String kind,
        String asset, String sha256, long bytes, String creator, String source, String license, String preview) {
    public CosmeticDefinition(String id,String name,CosmeticCategory category,String kind,String asset,String sha256,long bytes,String creator,String source,String license){this(id,name,category,kind,asset,sha256,bytes,creator,source,license,"");}
    public CosmeticDefinition {
        preview=preview==null?"":preview;
        if(!preview.isEmpty()&&(!preview.matches("previews/[a-z0-9_-]{1,96}\\.png")||preview.contains(".."))){throw new IllegalArgumentException("Invalid cosmetic preview");}
        if(!id.matches("[a-z0-9][a-z0-9_-]{0,95}") || name.length()>96 || !kind.matches("cape|wings|head|tail|companion|trail|aura|particles")
                || !sha256.matches("[a-f0-9]{64}|-") || bytes<0 || bytes>16_777_216
                || !asset.matches("[a-z0-9_/.-]{1,180}") || asset.contains("..")) { throw new IllegalArgumentException("Invalid cosmetic definition"); }
        if(asset.equals("none")?(bytes!=0||!sha256.equals("-")):(bytes==0||sha256.equals("-"))){throw new IllegalArgumentException("Invalid cosmetic asset digest");}
        if(!kind.equals(category.name().toLowerCase(java.util.Locale.ROOT))) { throw new IllegalArgumentException("Cosmetic category mismatch"); }
    }
}
