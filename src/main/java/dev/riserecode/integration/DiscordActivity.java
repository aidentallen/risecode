package dev.riserecode.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** Local, opt-in Discord IPC. Never reads account credentials or game/server data. */
public final class DiscordActivity implements AutoCloseable {
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"risecode-discord");t.setDaemon(true);return t;});
    private volatile Connection connection;
    private volatile String status="Off";
    private volatile long generation;
    private volatile boolean closed;
    private boolean enabled;
    private String applicationId="";
    private final String version;
    private final Path endpoint;
    public DiscordActivity(String version){this(version,null);}
    DiscordActivity(String version,Path endpoint){this.version=version;this.endpoint=endpoint;}
    public String status(){return status;}
    public synchronized void configure(boolean requested,String id){
        id=id.strip();if(closed || enabled==requested && applicationId.equals(id)){return;}
        enabled=requested;applicationId=id;long current=++generation;
        Connection old=connection;connection=null;
        if(old!=null){worker.execute(()->{try{old.send(1,activity(null));}catch(IOException ignored){/* a disconnected Discord already removed presence */}finally{old.close();}});}
        if(!requested){status="Off";return;}
        if(!id.matches("[0-9]{17,20}")){status=id.isEmpty()?"Application ID required":"Invalid application ID";return;}
        status="Connecting";String clientId=id;worker.execute(()->connect(current,clientId,0));
    }
    private void connect(long current,String id,int attempt){
        if(closed || generation!=current){return;}
        Connection candidate=null;
        try{
            candidate=Connection.open(endpoint);candidate.send(0,handshake(id));JsonObject ready=candidate.read();
            if(!ready.has("evt") || !ready.get("evt").getAsString().equals("READY")){throw new IOException("Discord rejected handshake");}
            candidate.send(1,activity(version));JsonObject reply=candidate.read();
            if(reply.has("evt") && !reply.get("evt").isJsonNull() && reply.get("evt").getAsString().equals("ERROR")){throw new IOException("Discord rejected activity");}
            if(closed||generation!=current){candidate.send(1,activity(null));candidate.close();return;}
            connection=candidate;status="Connected";
            worker.schedule(()->heartbeat(current,id),15,TimeUnit.SECONDS);
        }catch(IOException|RuntimeException failure){
            if(candidate!=null){candidate.close();}
            if(!closed&&generation==current){status="Discord unavailable";worker.schedule(()->connect(current,id,attempt+1),Math.min(30,2L<<Math.min(attempt,4)),TimeUnit.SECONDS);}
        }
    }
    private void heartbeat(long current,String id){
        if(closed||generation!=current){return;}
        Connection active=connection;
        try{if(active==null){throw new IOException("Disconnected");}active.send(1,activity(version));active.read();worker.schedule(()->heartbeat(current,id),15,TimeUnit.SECONDS);}
        catch(IOException|RuntimeException failure){if(active!=null){active.close();}if(!closed&&generation==current){connection=null;status="Discord unavailable";worker.schedule(()->connect(current,id,0),2,TimeUnit.SECONDS);}}
    }
    public static JsonObject handshake(String id){JsonObject json=new JsonObject();json.addProperty("v",1);json.addProperty("client_id",id);return json;}
    public static JsonObject activity(String version){
        JsonObject payload=new JsonObject();payload.addProperty("cmd","SET_ACTIVITY");payload.addProperty("nonce",UUID.randomUUID().toString());
        JsonObject args=new JsonObject();args.addProperty("pid",ProcessHandle.current().pid());
        if(version==null){args.add("activity",com.google.gson.JsonNull.INSTANCE);}else{JsonObject activity=new JsonObject();activity.addProperty("details","Minecraft "+version);activity.addProperty("state","Using risecode");args.add("activity",activity);}
        payload.add("args",args);return payload;
    }
    @Override public synchronized void close(){
        if(closed){return;}configure(false,applicationId);closed=true;generation++;worker.shutdown();
    }
    private static final class Connection implements AutoCloseable {
        private final SocketChannel socket;
        private final RandomAccessFile pipe;
        private Connection(SocketChannel socket,RandomAccessFile pipe){this.socket=socket;this.pipe=pipe;}
        static Connection open(Path endpoint) throws IOException {
            if(endpoint!=null){SocketChannel socket=SocketChannel.open(StandardProtocolFamily.UNIX);try{socket.connect(UnixDomainSocketAddress.of(endpoint));socket.configureBlocking(false);return new Connection(socket,null);}catch(IOException failure){socket.close();throw failure;}}
            if(System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")){
                for(int i=0;i<10;i++){try{return new Connection(null,new RandomAccessFile("\\\\.\\pipe\\discord-ipc-"+i,"rw"));}catch(IOException ignored){/* try the next Discord instance */}}
            }else{
                var roots=new LinkedHashSet<String>();for(String name:List.of("XDG_RUNTIME_DIR","TMPDIR","TMP","TEMP")){String value=System.getenv(name);if(value!=null&&!value.isBlank()){roots.add(value);}}
                roots.add("/tmp");
                for(String root:roots){for(String extra:List.of("","app/com.discordapp.Discord/")){for(int i=0;i<10;i++){
                    SocketChannel socket=null;
                    try{socket=SocketChannel.open(StandardProtocolFamily.UNIX);socket.connect(UnixDomainSocketAddress.of(Path.of(root,extra+"discord-ipc-"+i)));socket.configureBlocking(false);return new Connection(socket,null);}
                    catch(IOException failure){if(socket!=null){socket.close();}}
                }}}
            }
            throw new IOException("Discord IPC not found");
        }
        void send(int opcode,JsonObject data) throws IOException {
            byte[] json=data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteBuffer frame=ByteBuffer.allocate(json.length+8).order(ByteOrder.LITTLE_ENDIAN).putInt(opcode).putInt(json.length).put(json);frame.flip();
            if(pipe!=null){pipe.write(frame.array());return;}
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(frame.hasRemaining()){if(socket.write(frame)==0){pause(deadline);}}
        }
        JsonObject read() throws IOException {
            for(int i=0;i<8;i++){
                ByteBuffer header=readBytes(8).order(ByteOrder.LITTLE_ENDIAN);int opcode=header.getInt(),size=header.getInt();
                if(size<0||size>65536){throw new IOException("Invalid Discord frame size");}ByteBuffer body=readBytes(size);
                if(opcode==2){throw new IOException("Discord closed IPC");}
                if(opcode==3){send(4,JsonParser.parseString(new String(body.array(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());continue;}
                if(opcode==1){return JsonParser.parseString(new String(body.array(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
            }
            throw new IOException("No Discord response");
        }
        private ByteBuffer readBytes(int count) throws IOException {
            ByteBuffer result=ByteBuffer.allocate(count);if(pipe!=null){pipe.readFully(result.array());return result;}
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            while(result.hasRemaining()){int n=socket.read(result);if(n<0){throw new IOException("Discord disconnected");}if(n==0){pause(deadline);}}result.flip();return result;
        }
        private static void pause(long deadline) throws IOException {if(System.nanoTime()>deadline){throw new IOException("Discord IPC timed out");}LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(2));}
        @Override public void close(){try{if(socket!=null){socket.close();}if(pipe!=null){pipe.close();}}catch(IOException ignored){/* already disconnected */}}
    }
}
