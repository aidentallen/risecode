package dev.riserecode.module.modules;

import dev.riserecode.event.ChunkLifecycleEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.event.SeedWorldEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.mixin.SeedBiomeAccessor;
import dev.riserecode.mixin.SeedLevelDataAccessor;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.seedcracker.CollectionQueue;
import dev.riserecode.seedcracker.CrackingJob;
import dev.riserecode.seedcracker.Features;
import dev.riserecode.seedcracker.SeedCracker;
import dev.riserecode.seedcracker.SeedDatabase;
import dev.riserecode.seedcracker.SeedStore;
import dev.riserecode.seedcracker.cracker.storage.DataStorage;
import dev.riserecode.seedcracker.finder.Finder;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.StringSetting;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;

/** A disabled-by-default World module; all state transitions run on the client thread. */
public final class SeedCrackerModule extends Module {
    public enum OutlineMode { OFF, VISIBLE, THROUGH_WALLS }
    public final EnumSetting<OutlineMode> outlines=register(new EnumSetting<>("Outlines","Structure observations in the current dimension",OutlineMode.VISIBLE));
    public final ColorSetting color=register(new ColorSetting("Outline Color","Structure outline color",0xFF59D5CB));
    public final DoubleSetting workers=register(new DoubleSetting("Cracking Workers","Applied on the next manual Start",CrackingJob.defaultWorkers(),1,16));
    public final BooleanSetting debug=register(new BooleanSetting("Debug","Show collection timing and queue diagnostics",false));
    public final StringSetting generation=register(new StringSetting("Generation Profile","Separate archives when a server uses multiple worlds or generation settings; native 26.2 rules required","default",128));
    public final BooleanSetting databaseLookup=register(new BooleanSetting("Database Lookup","Allow explicit Lookup requests to the upstream Google seed database",false));
    public final BooleanSetting databaseSubmits=register(new BooleanSetting("Allow Submission","Allow manual submission of hash-verified seeds on licensed accounts and servers with more than 10 players",false));
    public final BooleanSetting anonymous=register(new BooleanSetting("Anonymous Submission","Upstream hash flag asks the database to anonymize your username; the service still receives it",false));
    private final Map<Finder.Type,BooleanSetting> finders=new EnumMap<>(Finder.Type.class);
    private final Minecraft mc;
    private final SeedStore store;
    private final DataStorage data=new DataStorage();
    private final CollectionQueue collection=new CollectionQueue();
    private ClientLevel level;
    private SeedStore.Identity identity;
    private CrackingJob job;
    private SeedDatabase database;
    private long databaseRequest, session, jobSession, savedRevision=-1, lastSaveTick;
    private List<Long> structureSeeds=List.of(), verifiedSeeds=List.of();
    private long bitsRevision=-1;
    private double regularBits,liftingBits;
    private String storageError="";
    private String message="Enable to collect. Start cracking manually.";
    private boolean resultAccepted, databasePending, preserveArchive=true;

    public SeedCrackerModule(EventBus bus, Minecraft mc) {
        super("SeedCracker","Collect native 26.2 structure observations; crack only with manual Start.",Category.WORLD,bus);
        this.mc=mc;
        store=new SeedStore(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("riserecode/seedcracker"));
        databaseLookup.onChange(this::cancelDatabase); databaseSubmits.onChange(this::cancelDatabase); anonymous.onChange(this::cancelDatabase);
        for (Finder.Type type : Finder.Type.values()) {
            finders.put(type,register(new BooleanSetting("Find "+label(type),"Collect "+label(type)+" observations",true)));
        }
    }
    public static String label(Finder.Type type) { return switch(type) {
        case DESERT_TEMPLE -> "Desert Pyramids"; case JUNGLE_TEMPLE -> "Jungle Temples"; case BURIED_TREASURE -> "Buried Treasure";
        case END_CITY -> "End Cities"; case END_PILLARS -> "End Pillars"; case SWAMP_HUT -> "Swamp Huts";
        case PILLAGER_OUTPOST -> "Pillager Outposts"; case TRIAL_CHAMBERS -> "Trial Chambers";
        case MONUMENT -> "Ocean Monuments"; case SHIPWRECK -> "Shipwrecks"; case IGLOO -> "Igloos";
    }; }
    @Override protected void onEnable() {
        if (Features.STRUCTURE_TYPES.isEmpty()) { Features.init(dev.riserecode.seedcracker.config.Config.get().getVersion()); }
        SeedCracker.bind(new SeedCracker(data,this::rescan));
        syncFinders();
        listen(UpdateEvent.class,this::tick);
        listen(SeedWorldEvent.class,event -> changeWorld(event.level,true));
        listen(dev.riserecode.event.SeedChunkEvent.class,event -> { changeWorld(mc.level,false); collection.chunk(event.chunk); });
        listen(ChunkLifecycleEvent.class,event -> {
            if (event.kind()==ChunkLifecycleEvent.Kind.UNLOAD) { collection.unload(event.chunk()); }
            else if (event.kind()==ChunkLifecycleEvent.Kind.RESET) { changeWorld(mc.level,true); }
        });
        listen(Render3DEvent.class,this::render);
        changeWorld(mc.level,true);
        if (mc.level!=null && mc.player!=null) { rescan(); }
    }
    @Override protected void onDisable() {
        acceptResult(); stop(); save(); collection.clear(); level=null; identity=null; data.clear();
        structureSeeds=List.of(); verifiedSeeds=List.of(); SeedCracker.bind(null);
        message="Disabled. Saved data requires explicit Restore.";
    }
    private void syncFinders() { finders.forEach((type,setting) -> type.enabled.set(setting.getValue())); }
    private SeedStore.Identity currentIdentity(ClientLevel current) {
        String world;
        if (mc.getSingleplayerServer()!=null) { world="local:"+mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize(); }
        else if (mc.getCurrentServer()!=null) { world="server:"+mc.getCurrentServer().ip.toLowerCase(Locale.ROOT); }
        else { world="server:"+(mc.getConnection()==null ? "unknown" : mc.getConnection().getConnection().getRemoteAddress()); }
        long hash=((SeedBiomeAccessor)current.getBiomeManager()).riserecode$hashedSeed();
        boolean flat=((SeedLevelDataAccessor)current.getLevelData()).riserecode$isFlat();
        return new SeedStore.Identity(world,hash,"26.2/"+(flat?"flat":current.isDebug()?"debug":"native")+"/"+generation.getValue());
    }
    private void changeWorld(ClientLevel current, boolean forceCancel) {
        SeedStore.Identity next=current==null ? null : currentIdentity(current);
        if (!forceCancel && level==current && java.util.Objects.equals(identity,next)) { return; }
        acceptResult(); stop(); save();
        boolean compatible=next!=null && next.equals(identity);
        level=current;
        if (!compatible) { data.clear(); structureSeeds=List.of(); verifiedSeeds=List.of(); savedRevision=-1; preserveArchive=true; }
        identity=next; collection.world(supported()?current:null);
        message=current==null ? "Disconnected. Data saved; use Restore after rejoining." : supported() ?
                "Collecting. Start an attempt manually; Restore loads matching saved data." : "Flat/debug/custom dimensions are unsupported by native structure rules.";
    }
    private boolean supported() {
        return level!=null && !level.isDebug() && !((SeedLevelDataAccessor)level.getLevelData()).riserecode$isFlat() &&
                java.util.Set.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end").contains(level.dimension().identifier().toString());
    }
    private void tick(UpdateEvent event) {
        changeWorld(mc.level,false); syncFinders(); acceptResult();
        if (!databaseLookup.getValue() && !databaseSubmits.getValue() && database!=null) { cancelDatabase(); }
        if (supported()) {
            try { collection.tick(); } catch (RuntimeException failure) { message="Finder failed: "+failure.getMessage(); SeedCracker.LOGGER.error(message,failure); }
        }
        if (event.getTick()-lastSaveTick>=100) { save(); lastSaveTick=event.getTick(); }
    }
    public void start() {
        requireEnabled(); changeWorld(mc.level,false); acceptResult();
        if (!supported()) { message="Join a native Minecraft 26.2 world before Start."; return; }
        if (job!=null && !job.terminated()) { message="An attempt is running or stopping. Stop it and wait before Start."; return; }
        session++; jobSession=session; resultAccepted=false;
        job=new CrackingJob(new CrackingJob.Snapshot(data.observations(),data.pillars(),identity.hash(),structureSeeds),(int)Math.round(workers.getValue()));
        job.start(); message="Manual attempt started using "+data.observations().size()+" observations. New observations are for your next Start.";
    }
    public void stop() {
        session++; if (job!=null && !job.progress().complete()) { job.close(); }
        cancelDatabase(); message="Stopped; collection continues while enabled.";
    }
    private void cancelDatabase() { databaseRequest++; if (database!=null) { database.close(); database=null; } databasePending=false; }
    private void acceptResult() {
        if (job==null || resultAccepted) { return; }
        var progress=job.progress();
        if (!progress.complete()) { return; }
        resultAccepted=true;
        if (jobSession!=session || progress.cancelled() || !isEnabled()) { return; }
        if (!progress.message().startsWith("Insufficient data") && !progress.message().startsWith("Cracking failed")) { structureSeeds=progress.structures(); }
        verifiedSeeds=progress.worlds(); message=progress.message(); savedRevision=-1;
    }
    public void rescan() {
        requireEnabled(); changeWorld(mc.level,false); syncFinders();
        if (!supported() || mc.player==null) { message="Join a supported world before Rescan."; return; }
        int radius=mc.options.renderDistance().get(); ChunkPos center=mc.player.chunkPosition();
        for (int ring=0;ring<=radius;ring++) {
            for (int dx=-ring;dx<=ring;dx++) {
                for (int dz=-ring;dz<=ring;dz++) {
                    if (Math.max(Math.abs(dx),Math.abs(dz))!=ring) { continue; }
                    int x=center.x()+dx,z=center.z()+dz;
                    if (level.hasChunk(x,z)) { collection.chunk(ChunkPos.pack(x,z)); }
                }
            }
        }
        message="Queued loaded chunks. "+collection.pending()+" finder tasks; no cracking was started.";
    }
    public void clearData() { requireEnabled(); stop(); data.clear(); structureSeeds=List.of(); verifiedSeeds=List.of(); collection.world(level); preserveArchive=false; savedRevision=-2; save(); message="Data and results cleared for this world."; }
    public void restore() {
        requireEnabled(); changeWorld(mc.level,false);
        if (identity==null) { message="Join the matching world before Restore."; return; }
        stop();
        try {
            var saved=store.restore(identity); data.replace(saved.observations(),saved.pillars()); structureSeeds=saved.structureSeeds(); verifiedSeeds=saved.verifiedSeeds();
            preserveArchive=false; savedRevision=data.revision(); message="Restored "+data.observations().size()+" observations. Start is still manual.";
        } catch (java.io.IOException failure) { message=failure.getMessage(); }
    }
    private void save() {
        if (identity==null || savedRevision==data.revision()) { return; }
        // An empty fresh session must not overwrite an archive before explicit Restore.
        if (data.observations().isEmpty() && data.pillars().isEmpty() && structureSeeds.isEmpty() && verifiedSeeds.isEmpty() && savedRevision!=-2) { return; }
        try {
            var snapshot=new SeedStore.Saved(1,identity,data.observations(),data.pillars(),structureSeeds,verifiedSeeds);
            // Keep a previous compatible archive until the user explicitly restores
            // or clears it. Saving new collection must never silently erase it.
            if (preserveArchive && java.nio.file.Files.exists(store.path(identity))) {
                var prior=store.restore(identity);
                var merged=new java.util.LinkedHashSet<>(prior.observations()); merged.addAll(snapshot.observations());
                var verified=new java.util.LinkedHashSet<>(prior.verifiedSeeds()); verified.addAll(snapshot.verifiedSeeds());
                snapshot=new SeedStore.Saved(1,identity,List.copyOf(merged),snapshot.pillars().isEmpty()?prior.pillars():snapshot.pillars(),
                        snapshot.structureSeeds().isEmpty()?prior.structureSeeds():snapshot.structureSeeds(),List.copyOf(verified));
            }
            store.save(snapshot); savedRevision=data.revision(); storageError="";
        }
        catch (java.io.IOException | RuntimeException failure) { storageError="Could not save data: "+failure.getMessage(); message=storageError; }
    }
    public void copySeed() {
        requireEnabled(); acceptResult();
        if (verifiedSeeds.size()!=1) { message="Copy Seed requires one hash-verified world seed."; return; }
        mc.keyboardHandler.setClipboard(Long.toString(verifiedSeeds.getFirst())); message="Verified world seed copied.";
    }
    public void lookup() {
        requireEnabled(); changeWorld(mc.level,false);
        if (!databaseLookup.getValue()) { message="Enable Database Lookup before requesting a lookup."; return; }
        if (identity==null || identity.hash()==0) { message="A usable server seed hash is required to verify database results."; return; }
        if (databasePending) { message="A database request is already pending."; return; }
        databasePending=true; long stamp=++databaseRequest;
        if (database==null) { database=new SeedDatabase(); }
        database.lookup(identity.hash()).whenComplete((seeds,failure) -> mc.execute(() -> {
            if (!isEnabled() || stamp!=databaseRequest || !databaseLookup.getValue()) { return; }
            databasePending=false;
            if (failure!=null) { message="Database lookup failed: "+concise(failure); return; }
            if (!seeds.isEmpty()) { verifiedSeeds=seeds; savedRevision=-1; }
            message=seeds.isEmpty()?"Database returned no hash-verified seed.":"Database world seed verified against the server seed hash.";
        }));
    }
    public void submit() {
        requireEnabled(); changeWorld(mc.level,false); acceptResult();
        if (!databaseSubmits.getValue()) { message="Enable Allow Submission before manual submission."; return; }
        if (identity==null || verifiedSeeds.size()!=1 || mc.getConnection()==null || mc.player==null || mc.getConnection().getConnection().isMemoryConnection() || mc.getConnection().getOnlinePlayers().size()<=10) {
            message="Submission requires one verified seed and a multiplayer server with more than 10 online players."; return;
        }
        if (databasePending) { message="A database request is already pending."; return; }
        var user=mc.getUser(); var service=mc.services().sessionService();
        Map<String,String> payload=Map.of("serverIp",mc.getConnection().getConnection().getRemoteAddress().toString(),"dimension",Finder.inferDimension(level.dimensionType()),
                "seed",verifiedSeeds.getFirst()+"L","version","26.2","username",mc.player.getName().getString(),"hash",anonymous.getValue()?"1":"0");
        databasePending=true; long stamp=++databaseRequest;
        if (database==null) { database=new SeedDatabase(); }
        database.submit(payload,() -> {
            try { service.joinServer(user.getProfileId(),user.getAccessToken(),"seedcrackerx"); }
            catch (com.mojang.authlib.exceptions.AuthenticationException failure) { throw new IllegalStateException("Minecraft account authentication failed",failure); }
        }).whenComplete((reply,failure) -> mc.execute(() -> {
            if (!isEnabled() || stamp!=databaseRequest || !databaseSubmits.getValue()) { return; }
            databasePending=false; message=failure==null?reply:"Submission failed: "+concise(failure);
        }));
    }
    private static String concise(Throwable failure) { while (failure.getCause()!=null) { failure=failure.getCause(); } return String.valueOf(failure.getMessage()); }
    public static String submissionDetails() { return "Submission sends server address, dimension, verified seed, version, username and anonymous flag to the upstream service; authenticates your licensed Minecraft account."; }
    private void requireEnabled() { if (!isEnabled()) { throw new IllegalArgumentException("Enable SeedCracker first"); } }
    public List<String> statusLines() {
        var progress=job==null?null:job.progress();
        if (bitsRevision!=data.revision()) { var observations=data.observations(); regularBits=CrackingJob.regularBits(observations); liftingBits=CrackingJob.liftingBits(observations); bitsRevision=data.revision(); }
        String counts=String.format(Locale.ROOT,"%d structures / %d pillars | %.1f regular / %.1f lifting bits",data.observations().size(),data.pillars().size(),regularBits,liftingBits);
        String phase=progress!=null&&!progress.complete()?progress.phase()+" | tested "+progress.tested()+" | structure candidates "+progress.structureCandidates()+" | world candidates "+progress.worldCandidates():"Idle | structure seeds "+structureSeeds.size()+" | verified world seeds "+verifiedSeeds.size();
        String result=verifiedSeeds.isEmpty()?"No verified world seed":verifiedSeeds.size()==1?"Verified world seed: "+verifiedSeeds.getFirst():"Verified world seeds: "+verifiedSeeds;
        var lines=new java.util.ArrayList<>(List.of(counts,phase,result,message,"Collection queue: "+collection.pending()+" | dropped: "+collection.dropped()));
        if (!structureSeeds.isEmpty()) { lines.add("Structure seeds (48-bit): "+structureSeeds.stream().limit(8).toList()+(structureSeeds.size()>8?" …":"")); }
        if (!storageError.isEmpty()) { lines.add(storageError); }
        if (data.observations().size()>=DataStorage.MAX_OBSERVATIONS) { lines.add("Observation limit reached. Clear Data before further collection."); }
        if (debug.getValue()) { lines.add(collection.diagnostics()); }
        return List.copyOf(lines);
    }
    public String status() { return String.join("\n",statusLines()); }
    public DataStorage data() { return data; }
    public CrackingJob job() { return job; }
    public List<Long> verifiedSeeds() { return verifiedSeeds; }
    public SeedStore.Identity identity() { return identity; }
    private void render(Render3DEvent event) {
        if (outlines.getValue()==OutlineMode.OFF || level==null || mc.player==null) { return; }
        double radius=(mc.options.renderDistance().get()+1)*16.0;
        for (var outline : collection.outlines()) {
            if (outline.type().enabled.get() && outline.cuboid().box().getCenter().distanceToSqr(mc.player.position())<=radius*radius) {
                dev.riserecode.render.EspGeometry.box(event,outline.cuboid().box(),color,2,outlines.getValue()==OutlineMode.VISIBLE);
            }
        }
    }
}
