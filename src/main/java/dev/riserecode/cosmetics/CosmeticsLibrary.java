package dev.riserecode.cosmetics;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded, cancellable content cache. Downloads never equip an item. */
public final class CosmeticsLibrary implements AutoCloseable {
    public static final String BASE="https://raw.githubusercontent.com/aidentallen/risecode/cosmetics-catalog/v1/";
    @FunctionalInterface public interface Fetcher { byte[] get(String relative,int limit)throws Exception; }
    private final Path root;
    private final Map<String,String> bundledDigests=new HashMap<>();
    private final Fetcher fetcher;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"risecode-cosmetics-io");t.setDaemon(true);return t;});
    private final AtomicLong generation=new AtomicLong();
    private volatile List<CosmeticDefinition> definitions;
    private volatile String status="Offline collection ready";
    private volatile boolean busy,closed;
    private Future<?> job;
    public CosmeticsLibrary(Path root){this(root,CosmeticsLibrary::http);}
    public CosmeticsLibrary(Path root,Fetcher fetcher){
        this.root=root;this.fetcher=fetcher;
        try{definitions=parse(bundled("index.json"));
            for(var d:definitions){if(CosmeticsLibrary.class.getResource("/assets/riserecode/cosmetics/"+d.asset())!=null){bundledDigests.put(d.asset(),d.sha256());}}
            try{if(Files.exists(root.resolve("catalog.json"))){definitions=merge(definitions,parse(Files.readAllBytes(root.resolve("catalog.json"))));}loadImports();}
            catch(IOException|RuntimeException invalid){status="Cached catalog unavailable · using bundled collection";}}
        catch(IOException e){throw new IllegalStateException("Cosmetics catalog unavailable",e);}
    }
    private void loadImports()throws IOException{Path p=root.resolve("imports.json");if(Files.exists(p)){try{definitions=merge(definitions,parse(Files.readAllBytes(p)));}catch(RuntimeException invalid){status="Import metadata unavailable";}}}
    public List<CosmeticDefinition> definitions(){return definitions;}
    private volatile List<CosmeticDefinition> indexedDefinitions;
    private Map<String,CosmeticDefinition> byId=Map.of();
    public CosmeticDefinition find(String id){var current=definitions;if(indexedDefinitions!=current){var next=new LinkedHashMap<String,CosmeticDefinition>();for(var d:current){next.putIfAbsent(d.id(),d);}byId=Map.copyOf(next);indexedDefinitions=current;}return byId.get(id);}
    public String status(){return status;}
    public boolean busy(){return busy;}
    public Path directory(){return root;}
    public static String hash(byte[] data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public static List<CosmeticDefinition> parse(byte[] data){
        if(data.length>1_048_576){throw new IllegalArgumentException("Catalog exceeds 1 MiB");}
        var root=JsonParser.parseString(new String(data,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();if(root.get("version").getAsInt()!=1){throw new IllegalArgumentException("Unsupported catalog");}
        var result=new ArrayList<CosmeticDefinition>();var ids=new HashSet<String>();var gson=new Gson();
        for(var e:root.getAsJsonArray("entries")){var d=gson.fromJson(e,CosmeticDefinition.class);if(!ids.add(d.id())||result.size()>=512){throw new IllegalArgumentException("Catalog duplicate or limit");}result.add(d);}return List.copyOf(result);
    }
    private static List<CosmeticDefinition> merge(List<CosmeticDefinition> old,List<CosmeticDefinition> update){var map=new LinkedHashMap<String,CosmeticDefinition>();old.forEach(d->map.put(d.id(),d));update.forEach(d->map.put(d.id(),d));return List.copyOf(map.values());}
    public static byte[] bundled(String name)throws IOException{
        try(var in=CosmeticsLibrary.class.getResourceAsStream("/assets/riserecode/cosmetics/"+name)){if(in==null){throw new FileNotFoundException(name);}return in.readNBytes(16_777_217);}
    }
    public boolean available(CosmeticDefinition d){if(d==null){return false;}if(d.asset().equals("none")){return true;}
        return d.sha256().equals(bundledDigests.get(d.asset()))||Files.isRegularFile(root.resolve(d.asset()));}
    public byte[] read(CosmeticDefinition d)throws IOException{
        byte[] bytes;if(d.sha256().equals(bundledDigests.get(d.asset()))){bytes=bundled(d.asset());}else{Path path=root.resolve(d.asset());if(Files.size(path)>16_777_216){throw new IOException("Asset size limit");}bytes=Files.readAllBytes(path);}
        if(bytes.length!=d.bytes()||!hash(bytes).equals(d.sha256())){throw new IOException("Cosmetic checksum mismatch");}return bytes;
    }
    public synchronized void refresh(){start(token->{byte[] data=fetcher.get("index.json",1_048_576);var next=parse(data);synchronized(this){check(token);atomic(root.resolve("catalog.json"),data);definitions=merge(definitions,next);status="Catalog updated";}});}
    public synchronized void download(CosmeticDefinition d){start(token->{
        byte[] bytes=fetcher.get(d.asset(),16_777_216);if(bytes.length!=d.bytes()||!hash(bytes).equals(d.sha256())){throw new IOException("Download checksum mismatch");}
        if(d.category()==CosmeticCategory.COMPANION){CosmeticModel.read(bytes);}else{CosmeticModel.png(bytes,2048);}
        synchronized(this){check(token);atomic(root.resolve(d.asset()),bytes);status=d.name()+" downloaded";}
    });}
    @FunctionalInterface private interface Work {void run(long token)throws Exception;}
    private synchronized void start(Work action){if(busy||closed){return;}busy=true;long token=generation.incrementAndGet();status="Working…";
        job=worker.submit(()->{try{action.run(token);}catch(Exception e){if(token==generation.get()){status=Thread.currentThread().isInterrupted()?"Cancelled":safe(e);}}finally{if(token==generation.get()){busy=false;}}});}
    private void check(long token)throws InterruptedException{if(token!=generation.get()||closed||Thread.currentThread().isInterrupted()){throw new InterruptedException("Cancelled");}}
    public synchronized void cancel(){generation.incrementAndGet();if(job!=null){job.cancel(true);}busy=false;status="Cancelled · completed downloads kept";}
    private static String safe(Exception e){return e.getMessage()==null?"Cosmetics operation failed":e.getMessage().substring(0,Math.min(140,e.getMessage().length()));}
    public static void atomic(Path path,byte[] bytes)throws IOException{Files.createDirectories(path.getParent());Path tmp=Files.createTempFile(path.getParent(),"cosmetic-",".tmp");try{Files.write(tmp,bytes);Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(tmp);}}
    public CompletableFuture<CosmeticDefinition> importCape(Path path){return CompletableFuture.supplyAsync(()->{try{
        if(Files.size(path)>16_777_216){throw new IOException("Cape must be below 16 MiB");}byte[] bytes=Files.readAllBytes(path);CosmeticModel.png(bytes,2048);var header=java.nio.ByteBuffer.wrap(bytes);int w=header.getInt(16),h=header.getInt(20);
        if(w<64||w%64!=0||h!=w/2){throw new IOException("Use a 64×32 cape or an integer-scaled equivalent");}
        // Decode before committing the import; a valid header alone is insufficient.
        var image=javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));if(image==null||image.getWidth()!=w||image.getHeight()!=h){throw new IOException("Invalid cape PNG");}
        String hash=hash(bytes);var d=new CosmeticDefinition("import-"+hash,"Imported cape "+hash.substring(0,6),CosmeticCategory.CAPE,"cape","imports/"+hash+".png",hash,bytes.length,"Local import","local","User supplied");
        atomic(root.resolve(d.asset()),bytes);synchronized(this){definitions=merge(definitions,List.of(d));var doc=new com.google.gson.JsonObject();doc.addProperty("version",1);doc.add("entries",new Gson().toJsonTree(definitions.stream().filter(e->e.id().startsWith("import-")).toList()));atomic(root.resolve("imports.json"),new Gson().toJson(doc).getBytes(java.nio.charset.StandardCharsets.UTF_8));}return d;
        }catch(IOException|RuntimeException e){throw new CompletionException(e);}},worker);}
    public <T> CompletableFuture<T> work(Callable<T> work){return CompletableFuture.supplyAsync(()->{try{return work.call();}catch(Exception e){throw new CompletionException(e);}},worker);}
    private static byte[] http(String relative,int limit)throws Exception{
        if(!relative.matches("[a-z0-9_/.-]{1,180}")||relative.contains("..")){throw new IOException("Invalid catalog path");}
        var request=HttpRequest.newBuilder(URI.create(BASE+relative)).timeout(Duration.ofSeconds(30)).header("User-Agent","risecode-cosmetics/1").GET().build();
        try(var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()){
            var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());try(var in=response.body()){
                if(response.statusCode()!=200){throw new IOException("Catalog HTTP "+response.statusCode());}byte[] bytes=in.readNBytes(limit+1);if(bytes.length>limit){throw new IOException("Download exceeds size limit");}return bytes;}}
    }
    @Override public void close(){closed=true;cancel();worker.shutdownNow();}
}
