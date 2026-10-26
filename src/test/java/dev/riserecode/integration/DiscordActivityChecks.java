package dev.riserecode.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Real local framed IPC, without requiring Discord or an application account. */
public final class DiscordActivityChecks {
    private DiscordActivityChecks(){}
    private static void check(boolean value,String label){if(!value){throw new AssertionError(label);}}
    public static void run()throws Exception{
        String payload=DiscordActivity.activity("26.2").toString();check(payload.contains("Minecraft 26.2")&&!payload.contains("server")&&!payload.contains("world"),"presence contains only client and game");
        var socket=Files.createTempDirectory("discord-020-").resolve("ipc");var failure=new AtomicReference<Throwable>();var published=new CountDownLatch(1);var cleared=new CountDownLatch(1);
        try(var server=ServerSocketChannel.open(StandardProtocolFamily.UNIX)){
            server.bind(UnixDomainSocketAddress.of(socket));
            Thread peer=Thread.ofPlatform().daemon().start(()->{
                try(var channel=server.accept()){
                    JsonObject hello=read(channel);check(hello.get("client_id").getAsString().equals("123456789012345678"),"configured application ID sent");
                    JsonObject ready=new JsonObject();ready.addProperty("evt","READY");write(channel,ready);
                    JsonObject active=read(channel);check(active.getAsJsonObject("args").getAsJsonObject("activity").get("state").getAsString().equals("Using risecode"),"activity was published");write(channel,new JsonObject());published.countDown();
                    JsonObject clear=read(channel);check(clear.getAsJsonObject("args").get("activity").isJsonNull(),"disable clears activity");cleared.countDown();
                }catch(Throwable problem){failure.set(problem);published.countDown();cleared.countDown();}
            });
            try(var activity=new DiscordActivity("26.2",socket)){
                activity.configure(true,"");check(activity.status().equals("Application ID required"),"empty ID does not claim connection");
                activity.configure(true,"invalid");check(activity.status().equals("Invalid application ID"),"invalid ID is rejected");
                activity.configure(true,"123456789012345678");check(published.await(5,TimeUnit.SECONDS),"activity published promptly");
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(!activity.status().equals("Connected")&&System.nanoTime()<deadline){Thread.sleep(5);}
                check(activity.status().equals("Connected"),"successful IPC acknowledged");activity.configure(false,"123456789012345678");check(cleared.await(5,TimeUnit.SECONDS),"activity cleared promptly");check(activity.status().equals("Off"),"disable updates status");
            }
            peer.join(1000);if(failure.get()!=null){throw new AssertionError("Discord test peer failed",failure.get());}
        }finally{Files.deleteIfExists(socket);Files.deleteIfExists(socket.getParent());}
        System.out.println("Discord IPC checks passed (10 assertions).");
    }
    private static JsonObject read(SocketChannel channel)throws IOException{
        ByteBuffer header=readBytes(channel,8).order(ByteOrder.LITTLE_ENDIAN);header.getInt();int length=header.getInt();check(length>=0&&length<65536,"bounded frame");return JsonParser.parseString(new String(readBytes(channel,length).array(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private static ByteBuffer readBytes(SocketChannel channel,int size)throws IOException{var bytes=ByteBuffer.allocate(size);while(bytes.hasRemaining()){if(channel.read(bytes)<0){throw new IOException("Closed test connection");}}bytes.flip();return bytes;}
    private static void write(SocketChannel channel,JsonObject payload)throws IOException{byte[] json=payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);var frame=ByteBuffer.allocate(json.length+8).order(ByteOrder.LITTLE_ENDIAN).putInt(1).putInt(json.length).put(json);frame.flip();while(frame.hasRemaining()){channel.write(frame);}}
}
