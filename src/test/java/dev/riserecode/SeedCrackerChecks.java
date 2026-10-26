package dev.riserecode;

import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mccore.rand.seed.PillarSeed;
import dev.riserecode.seedcracker.*;
import dev.riserecode.seedcracker.cracker.PillarData;
import dev.riserecode.seedcracker.cracker.storage.DataStorage;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

/** Archive validation, reversal invariants and cooperative job lifetime. */
public final class SeedCrackerChecks {
    private static int checks;
    private static void check(boolean ok,String label) { checks++; if (!ok) { throw new AssertionError(label); } }
    private static void await(CrackingJob job) throws InterruptedException {
        long deadline=System.nanoTime()+Duration.ofSeconds(5).toNanos();
        while (!job.terminated() && System.nanoTime()<deadline) { Thread.sleep(5); }
        check(job.terminated(),"bounded worker shutdown");
    }
    public static void main(String[] args) throws Exception {
        Features.init(dev.riserecode.seedcracker.config.Config.get().getVersion());
        check(Features.STRUCTURE_TYPES.size()==10,"all ten supported structure models initialized");
        check(CrackingJob.defaultWorkers()>=1 && CrackingJob.defaultWorkers()<=4,"conservative default workers");
        for (long partial : new long[]{0,1,0xFFFFL,0x12345678L,0xFFFFFFFFL}) {
            for (int pillar : new int[]{0,1,12345,65535}) { check(PillarSeed.fromStructureSeed(CrackingJob.reversePillar(partial,pillar))==pillar,"End reversal invariant"); }
        }
        var empty=new CrackingJob(new CrackingJob.Snapshot(List.of(),List.of(),0,List.of()),1); empty.start(); await(empty);
        check(empty.progress().message().startsWith("Insufficient data"),"insufficient input is actionable");
        check(empty.progress().structures().isEmpty() && empty.progress().worlds().isEmpty(),"insufficient input cannot fabricate results");
        var data=new DataStorage();
        check(data.addBaseData(Features.DESERT_PYRAMID.at(-17,21),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING),"collection adds observation");
        check(!data.addBaseData(Features.DESERT_PYRAMID.at(-17,21),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING),"collection deduplicates");
        check(data.observeForOutline(Features.DESERT_PYRAMID.at(-17,21),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING) && data.observations().size()==1,"rescan recreates outlines without duplicating observations");
        check(CrackingJob.regularBits(data.observations())>9 && CrackingJob.liftingBits(data.observations())>9,"regular and lifting bits");
        long seed=-8675309; var id=new SeedStore.Identity("server:fixture",WorldSeed.toHash(seed),"26.2/native/default");
        var store=new SeedStore(Files.createTempDirectory("seedcracker-check-"));
        var heights=new PillarData(List.of()).getPillarHeights(17);
        var saved=new SeedStore.Saved(1,id,data.observations(),heights,List.of(seed & ((1L<<48)-1)),List.of(seed));
        store.save(saved); check(store.restore(id).equals(saved),"atomic save round trip");
        for (var other : List.of(new SeedStore.Identity("server:other",id.hash(),id.generation()),new SeedStore.Identity(id.world(),id.hash()+1,id.generation()),new SeedStore.Identity(id.world(),id.hash(),"different-generation"))) {
            check(!store.path(other).equals(store.path(id)),"world, hash and generation isolate archives");
            try { store.restore(other); throw new AssertionError("restored foreign world"); } catch (java.io.IOException expected) { checks++; }
        }
        try { store.save(new SeedStore.Saved(1,id,List.of(),List.of(),List.of(),List.of(123L))); throw new AssertionError("unverified saved seed"); } catch (IllegalArgumentException expected) { checks++; }
        check(store.restore(id).equals(saved),"failed save preserves previous archive");
        Files.writeString(store.path(id),"{broken");
        try { store.restore(id); throw new AssertionError("corrupt data accepted"); } catch (java.io.IOException expected) { checks++; }
        check(Files.readString(store.path(id)).equals("{broken"),"corrupt archive preserved");
        check(SeedDatabase.parseVerified("server,dim,"+seed+"L,x,x,x,WRONG\nserver,dim,123L,x,x,x,"+id.hash(),id.hash()).equals(List.of(seed)),"database verifies computed hash rather than trusting CSV or address");
        var requests=new java.util.concurrent.atomic.AtomicInteger();
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(),0),0);
        server.createContext("/lookup",exchange -> {
            requests.incrementAndGet(); byte[] body=("server,dim,"+seed+"L,x,x,x,0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length); try (var output=exchange.getResponseBody()) { output.write(body); }
        });
        server.createContext("/submit",exchange -> {
            requests.incrementAndGet(); String payload=new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            byte[] body=(payload.contains("hash=1")?"ok":"missing flag").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length); try (var output=exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            var base=java.net.URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            var authenticated=new java.util.concurrent.atomic.AtomicBoolean();
            try (var database=new SeedDatabase(base.resolve("/lookup"),base.resolve("/submit"))) {
                check(requests.get()==0,"database construction sends no requests");
                check(database.lookup(id.hash()).get(5,java.util.concurrent.TimeUnit.SECONDS).equals(List.of(seed)),"mock lookup verifies results");
                check(database.submit(java.util.Map.of("hash","1","seed",seed+"L"),() -> authenticated.set(true)).get(5,java.util.concurrent.TimeUnit.SECONDS).contains("accepted"),"mock manual submission");
                check(authenticated.get() && requests.get()==2,"submission authenticates before request");
            }
            var gate=new java.util.concurrent.CountDownLatch(1);
            var database=new SeedDatabase(base.resolve("/lookup"),base.resolve("/submit"));
            var pending=database.submit(java.util.Map.of(),() -> { try { gate.await(); } catch (InterruptedException expected) { Thread.currentThread().interrupt(); } });
            database.close(); gate.countDown();
            check(pending.isCompletedExceptionally(),"database cancellation rejects pending action");
            check(requests.get()==2,"cancelled authentication cannot submit");
        } finally { server.stop(0); }
        var observations=new java.util.ArrayList<Observation>();
        // Valid model placements with enough bits, used only to exercise cancellation.
        for (int i=0;i<12;i++) { observations.add(new Observation(Features.DESERT_PYRAMID.getName(),i*32,0)); }
        for (int i=0;i<8;i++) {
            var job=new CrackingJob(new CrackingJob.Snapshot(observations,List.of(),id.hash(),List.of()),2);
            job.start(); job.close(); await(job); check(job.progress().cancelled(),"repeated cancel never publishes a result"); check(job.progress().worlds().isEmpty(),"cancel rejects partial worlds");
        }
        System.out.println("SeedCracker checks passed ("+checks+" assertions)");
    }
}
