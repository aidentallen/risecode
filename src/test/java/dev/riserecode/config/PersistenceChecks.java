package dev.riserecode.config;

import com.google.gson.JsonObject;
import dev.riserecode.event.EventBus;
import dev.riserecode.friend.FriendManager;
import dev.riserecode.module.ModuleManager;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

/** Gates the real writer; tests ownership without races or timing sleeps. */
public final class PersistenceChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok){throw new AssertionError(message);}}
    private PersistenceChecks(){ }
    public static void main(String[] args)throws Exception{
        var directory=Files.createTempDirectory("risecode-persistence-");
        var owner=Thread.currentThread();var queue=new LinkedBlockingQueue<Runnable>();
        java.util.concurrent.Executor client=task->{if(Thread.currentThread()==owner){task.run();}else{queue.add(task);}};
        var writer=java.util.concurrent.Executors.newSingleThreadExecutor();
        var gate=new CountDownLatch(1);var entered=new CountDownLatch(1);
        writer.submit(()->{entered.countDown();try{gate.await();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        check(entered.await(5,TimeUnit.SECONDS),"writer gate installed");
        var logger=LoggerFactory.getLogger("PersistenceChecks");
        try(var config=new ConfigManager(directory,new ModuleManager(new EventBus(),logger),new FriendManager(),()->true,ignored->{},logger,client,writer);
            var profiles=new ProfileManager(config,()->check(Thread.currentThread()==owner,"UI refresh uses client thread"),logger)){
            config.setActiveProfile("before");
            JsonObject borrowed=new JsonObject();borrowed.addProperty("value",1);
            config.bindCosmeticsStore(()->borrowed,ignored->{},ignored->{});
            var save=config.saveAsync().toCompletableFuture();
            save.thenAccept(ok->check(Thread.currentThread()==owner,"save completion uses client thread"));
            config.setActiveProfile("after");borrowed.addProperty("value",2);
            gate.countDown();pump(save,queue);
            var disk=JsonStorage.read(config.getFile());
            check(disk.get("activeProfile").getAsString().equals("before"),"concurrent metadata edits cannot mutate worker snapshot");
            check(disk.getAsJsonObject("cosmetics").get("value").getAsInt()==1,"borrowed snapshot trees cannot mutate worker snapshot");
            check(config.getActiveProfile().equals("after"),"completion preserves later edits");
            check(config.save(),"next save succeeds");
            check(JsonStorage.read(config.getFile()).get("activeProfile").getAsString().equals("after"),"later edit persists on next save");
            for(String name:new String[]{"missing","corrupt","invalid","bad name"}){
                if(!name.equals("missing")&&!name.equals("bad name")){Files.writeString(profiles.getFile(name),name.equals("corrupt")?"{broken":"{\"schemaVersion\":999}");}
                var future=profiles.loadProfileAsync(name);var callback=new CompletableFuture<Boolean>();
                future.whenComplete((ok,failure)->{check(Thread.currentThread()==owner,"profile failure completes on client: "+name);callback.complete(ok);});
                pump(callback,queue);check(!callback.join(),"bad profile rejected: "+name);
                check(config.getActiveProfile().equals("after"),"failed load preserves active profile");
            }
            check(profiles.saveProfile("good"),"valid profile prepared");
            var good=profiles.loadProfileAsync("good");pump(good,queue);check(good.join()&&config.getActiveProfile().equals("good"),"valid profile applies on the client");
            var previousFile=directory.resolve("config.saved");Files.move(config.getFile(),previousFile);Files.createDirectory(config.getFile());
            try{
                var failed=config.saveAsync().toCompletableFuture();failed.thenAccept(ok->check(Thread.currentThread()==owner,"I/O failure completion uses client thread"));pump(failed,queue);check(!failed.join(),"atomic write failure resolves false");
            }finally{Files.delete(config.getFile());Files.move(previousFile,config.getFile());}
            check(config.save(),"save recovers after I/O failure");
            // Closed writer must also report its failure on the client executor.
            writer.shutdown();var rejected=config.saveAsync().toCompletableFuture();pump(rejected,queue);check(!rejected.join(),"rejected save resolves false");
        }finally{
            gate.countDown();writer.shutdownNow();
            try(var files=Files.walk(directory)){for(var file:files.sorted(java.util.Comparator.reverseOrder()).toList()){Files.delete(file);}}
        }
        System.out.println("Persistence remediation checks passed ("+checks+" assertions).");
    }
    private static void pump(CompletableFuture<?> future,LinkedBlockingQueue<Runnable> queue)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(!future.isDone()){
            Runnable task=queue.poll(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
            if(task==null){throw new AssertionError("Completion did not reach client executor");}task.run();
        }
    }
}
