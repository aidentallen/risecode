package dev.riserecode;

import com.google.gson.*;
import dev.riserecode.setting.*;
import dev.riserecode.testmixin.KeyboardInputInvoker;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;

/** Disposable screenshot/input bridge for an independent live usability review; test JAR only. */
public final class UxInteractiveReviewTest {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static int integer(JsonObject j,String name,int fallback){return j.has(name)?j.get(name).getAsInt():fallback;}
    private static double number(JsonObject j,String name,double fallback){return j.has(name)?j.get(name).getAsDouble():fallback;}
    private static void themeNumber(String name,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value);}
    public static void run(ClientGameTestContext c){
        Path root=Path.of(System.getProperty("riserecode.uxInteractiveDir","/tmp/riserecode-ux-round10/live"));
        try{Files.createDirectories(root.resolve("commands"));Files.createDirectories(root.resolve("frames"));}catch(Exception e){throw new RuntimeException(e);}
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server->{if(!server.getPlayerList().getPlayers().stream().allMatch(p->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(p.nameAndId())))throw new AssertionError("Review world must have only its owner");CameraFovClientTest.ownerOnlyShutdown=true;});
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            c.getInput().resizeWindow(1280,960);
            c.runOnClient(mc->{mc.options.guiScale().set(2);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(60);Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Uncapped Themed Menus")).setValue(false);Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(dev.riserecode.ui.clickgui.ClickGui.Page.SEARCH);});
            c.waitTicks(12);writeState(c,root,0,"ready",null);
            long deadline=System.nanoTime()+1_200_000_000_000L;int sequence=1;boolean finish=false;
            while(!finish&&System.nanoTime()<deadline){
                Path file=root.resolve("commands").resolve(String.format(java.util.Locale.ROOT,"%06d.json",sequence));
                if(!Files.isRegularFile(file)){c.waitTick();continue;}
                String error=null;
                try{
                    JsonObject command=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    String action=command.get("action").getAsString();
                    switch(action){
                        case "move"->c.getInput().setCursorPos(number(command,"x",0),number(command,"y",0));
                        case "click"->{c.getInput().setCursorPos(number(command,"x",0),number(command,"y",0));c.waitTick();c.getInput().pressMouse(integer(command,"button",0));}
                        case "key"->{int key=integer(command,"key",256),mods=integer(command,"modifiers",0);if(mods==0){c.getInput().pressKey(key);}else{c.runOnClient(mc->{var input=(KeyboardInputInvoker)mc.keyboardHandler;input.riserecode$press(mc.getWindow().handle(),1,new KeyEvent(key,0,mods));input.riserecode$press(mc.getWindow().handle(),0,new KeyEvent(key,0,mods));});}}
                        case "type"->c.getInput().typeChars(command.get("text").getAsString());
                        case "scroll"->{if(command.has("x")){c.getInput().setCursorPos(number(command,"x",0),number(command,"y",0));c.waitTick();}c.getInput().scroll(number(command,"amount",0));}
                        case "drag"->{int steps=Math.clamp(integer(command,"steps",8),1,60),button=integer(command,"button",0);double x=number(command,"x",0),y=number(command,"y",0),dx=number(command,"dx",0),dy=number(command,"dy",0);c.getInput().setCursorPos(x,y);c.waitTick();c.getInput().holdMouse(button);for(int i=1;i<=steps;i++){c.getInput().setCursorPos(x+dx*i/steps,y+dy*i/steps);c.waitTick();}c.getInput().releaseMouse(button);}
                        case "resize"->{c.getInput().resizeWindow(integer(command,"width",960),integer(command,"height",720));if(command.has("guiScale")){c.runOnClient(mc->mc.options.guiScale().set(command.get("guiScale").getAsInt()));}}
                        case "preset"->{boolean compact=command.get("name").getAsString().equals("compact");c.getInput().resizeWindow(compact?960:1280,compact?720:960);c.runOnClient(mc->{mc.options.guiScale().set(compact?3:2);themeNumber("Font Scale",compact?2:1);themeNumber("Window Width",compact?360:428);themeNumber("Window Height",compact?240:320);Client.INSTANCE.getClickGui().resetPosition();});}
                        case "wait","snapshot"->{}
                        case "finish"->finish=true;
                        default->throw new IllegalArgumentException("Unknown review action: "+action);
                    }
                    c.waitTicks(Math.clamp(integer(command,"ticks",3),1,40));
                }catch(Exception e){error=e.toString();}
                writeState(c,root,sequence,finish?"finished":"ready",error);sequence++;
            }
            c.runOnClient(mc->mc.gui.setScreen(null));
        }catch(Exception e){throw new RuntimeException(e);}finally{CameraFovClientTest.ownerOnlyShutdown=false;}
    }
    private static void writeState(ClientGameTestContext c,Path root,int sequence,String phase,String error)throws Exception{
        Path captured=c.takeScreenshot("interactive-"+sequence),image=root.resolve("frames").resolve(String.format(java.util.Locale.ROOT,"%06d.png",sequence));Files.copy(captured,image,StandardCopyOption.REPLACE_EXISTING);
        JsonObject state=c.computeOnClient(mc->{var j=new JsonObject();j.addProperty("sequence",sequence);j.addProperty("phase",phase);j.addProperty("screenshot",image.toString());j.addProperty("width",mc.getWindow().getScreenWidth());j.addProperty("height",mc.getWindow().getScreenHeight());j.addProperty("guiScale",mc.getWindow().getGuiScale());j.addProperty("screenTitle",mc.gui.screen()==null?"World":mc.gui.screen().getTitle().getString());j.addProperty("pid",ProcessHandle.current().pid());j.addProperty("capturedNanos",System.nanoTime());if(error!=null)j.addProperty("error",error);return j;});
        Path temporary=root.resolve("state.next");Files.writeString(temporary,GSON.toJson(state));Files.move(temporary,root.resolve("state.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);Files.writeString(root.resolve("observations.jsonl"),state+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
}
