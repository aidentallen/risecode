package dev.riserecode;

import dev.riserecode.module.modules.BaseFinderModule;
import dev.riserecode.scanner.*;
import dev.riserecode.setting.*;
import dev.riserecode.event.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import io.netty.buffer.Unpooled;
import static dev.riserecode.scanner.ChunkHeuristics.*;

/** Real 26.2 packet adapters, delta updates, render extraction and asynchronous lifetime. */
public final class BaseFinderIntegrationTest {
    private static int checks;
    private static BaseScanner isolated;
    private static volatile int lines;
    private static volatile boolean depthOnly;
    private static BaseFinderModule module(){return Client.INSTANCE.getModuleManager().getByClass(BaseFinderModule.class).orElseThrow();}
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static void bool(String name,boolean value){((BooleanSetting)module().getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow()).setValue(value);}
    private static void number(String name,double value){((DoubleSetting)module().getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow()).setValue(value);}
    @SuppressWarnings({"unchecked","rawtypes"}) private static void choice(String name,Enum<?> value){((EnumSetting)module().getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow()).setValue(value);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);mc.options.framerateLimit().set(90);check(!module().isEnabled(),"scanner disabled by default");});
        try(var world=c.worldBuilder().create()){
            c.waitTicks(20);
            world.getServer().runOnServer(server->CameraFovClientTest.ownerOnlyShutdown=true);
            world.getServer().runCommand("gamemode spectator @a");world.getServer().runCommand("tp @a 8 -32 28 180 0");
            world.getServer().runCommand("fill 0 -40 0 15 -24 15 air");c.waitTicks(20);
            c.runOnClient(mc->{number("Sensitivity",6);number("Scan Range",2);bool("False Positive Filter",false);bool("Geodes",false);bool("Growth",false);module().setEnabled(true);});
            world.getServer().runCommand("setblock 5 -32 5 chest");world.getServer().runCommand("setblock 7 -32 5 barrel");
            c.waitFor(mc->module().discoveries().stream().anyMatch(d->d.chunk()==ChunkPos.pack(0,0)&&d.finding().count(0)==2),400);
            c.runOnClient(mc->{check(module().discoveries().stream().anyMatch(d->d.chunk()==0&&d.finding().score()==6),"real server block packets produce exact weighted score");});
            c.waitTicks(8);c.takeScreenshot("basefinder-cards-and-outlines");
            c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_P);c.waitTicks(3);
            c.runOnClient(mc->{check(module().pinnedCards()==1,"native key pins first card");number("Card Timeout",1);});
            c.waitTicks(25);c.runOnClient(mc->check(module().visibleCards()>0,"pinned card survives timeout"));
            c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_DELETE);c.waitTicks(3);
            c.runOnClient(mc->{check(module().visibleCards()==0&&module().pinnedCards()==0,"native clear key dismisses cards and releases pins");number("Card Timeout",30);});
            var renderSubscription=Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class,-100,false,e->{lines=e.getLines().size();depthOnly=e.getLines().stream().allMatch(Render3DEvent.Line::visibleOnly)&&e.getQuads().stream().allMatch(Render3DEvent.Quad::visibleOnly);});
            for(var style:BaseFinderModule.Outline.values()){
                c.runOnClient(mc->choice("Chunk Outline Mode",style));c.waitTicks(3);
                c.runOnClient(mc->{check(lines>0&&lines<5000&&depthOnly,"bounded depth-tested geometry "+style);});
            }
            renderSubscription.close();
            world.getServer().runCommand("setblock 7 -32 5 air");
            c.waitFor(mc->module().discoveries().stream().noneMatch(d->d.chunk()==0),200);
            c.runOnClient(mc->{check(true,"block removal invalidates suspicion");bool("Chests",false);number("Sensitivity",3);});
            c.waitTicks(20);c.runOnClient(mc->check(module().discoveries().stream().noneMatch(d->d.chunk()==0),"container sub-toggle applies to resident chunks"));
            c.runOnClient(mc->{bool("Chests",true);number("Sensitivity",3);});
            c.waitFor(mc->module().discoveries().stream().anyMatch(d->d.chunk()==0),400);
            c.runOnClient(mc->{
                var chunk=mc.level.getChunk(0,0);isolated=new BaseScanner(new ScannerBlocks());
                var copies=Arrays.stream(chunk.getSections()).map(LevelChunkSection::copy).toArray(LevelChunkSection[]::new);
                var o=new Options(Mode.ACTIVITY_CLUES,Method.LIGHT,100,8,12,true,false,false,false,false,false,false,true,false);
                isolated.packet(0,0,new ClientboundLevelChunkWithLightPacket(chunk,mc.level.getLightEngine(),null,null),mc.level.getMinY(),copies.length,mc.level.palettedContainerFactory(),o);
                int section=1+((-32-mc.level.getMinY())>>4);
                // Sparse updates: establish known zero sky, then send only block light.
                isolated.packet(0,0,light(section,true,false),mc.level.getMinY(),copies.length,mc.level.palettedContainerFactory(),o);
                isolated.packet(0,0,light(section,false,false),mc.level.getMinY(),copies.length,mc.level.palettedContainerFactory(),o);
            });
            c.waitFor(mc->!isolated.discoveries().isEmpty(),200);
            c.runOnClient(mc->{check(isolated.discoveries().getFirst().finding().lightClues()==1,"sparse light masks retain zero skylight");check(isolated.failures()==0,"native chunk and biome palettes decode without errors");
                int section=1+((-32-mc.level.getMinY())>>4);
                var o=new Options(Mode.ACTIVITY_CLUES,Method.LIGHT,100,8,12,true,false,false,false,false,false,false,true,false);
                isolated.packet(0,0,light(section,false,true),mc.level.getMinY(),mc.level.getSectionsCount(),mc.level.palettedContainerFactory(),o);});
            c.waitFor(mc->isolated.discoveries().isEmpty(),200);
            c.runOnClient(mc->{check(true,"empty block-light mask clears old light clue");
                var o=new Options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,1,1,12,false,true,true,true,true,false,false,false,false);
                isolated.packet(0,0,new ClientboundLevelChunkWithLightPacket(mc.level.getChunk(0,0),mc.level.getLightEngine(),null,null),mc.level.getMinY(),mc.level.getSectionsCount(),mc.level.palettedContainerFactory(),o);
                isolated.forget(0);check(isolated.discoveries().isEmpty(),"forget removes results immediately");});
            c.waitTicks(8);c.runOnClient(mc->{check(isolated.discoveries().isEmpty(),"inflight work cannot resurrect forgotten chunk");isolated.close();module().setEnabled(false);check(module().discoveries().isEmpty(),"disable clears all discoveries");});
            world.getServer().runCommand("fill 1 -30 3 12 -30 3 amethyst_block");
            c.runOnClient(mc->{bool("Containers",false);bool("Geodes",true);number("Sensitivity",100);choice("Mode",Mode.ACTIVITY_CLUES);choice("Amethyst Method",Method.ANBS_PLUS_SCAN);module().setEnabled(true);});
            c.waitFor(mc->module().discoveries().stream().anyMatch(d->d.chunk()==0&&d.finding().clusters()==1&&d.finding().count(3)==12),400);
            c.runOnClient(mc->{check(true,"native palette decoding finds twelve connected amethyst cells");number("Min Cells",13);});
            c.waitTicks(20);c.runOnClient(mc->{check(module().discoveries().stream().noneMatch(d->d.chunk()==0),"native min-cell setting rejects small geode");number("Min Cells",12);choice("Amethyst Method",Method.BYPASS);});
            c.waitFor(mc->module().discoveries().stream().anyMatch(d->d.chunk()==0&&d.finding().count(3)==12),400);
            c.runOnClient(mc->{check(true,"air-exposed native geode cells feed bypass method");
                var b=new ScannerBlocks();check((b.flags(net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER.defaultBlockState())&(GROWTH|CLUSTER))==(GROWTH|CLUSTER),"fully grown amethyst classification");
                check((b.flags(net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7,7))&GROWTH)!=0,"mature crop classification");
                module().setEnabled(false);
            });
            c.runOnClient(mc->{
                isolated=new BaseScanner(new ScannerBlocks());var o=new Options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,1,1,12,false,true,true,true,true,true,true,true,true);
                for(int i=0;i<600;i++)isolated.packet(i,0,new ClientboundLevelChunkWithLightPacket(mc.level.getChunk(0,0),mc.level.getLightEngine(),null,null),mc.level.getMinY(),mc.level.getSectionsCount(),mc.level.palettedContainerFactory(),o);
                check(isolated.queued()<=16&&isolated.tracked()<=256&&isolated.discoveries().size()<=256,"queue, tickets and discoveries are bounded under burst load");check(isolated.rejected()>0,"overload drops work rather than running on client");isolated.close();
            });
        }finally{c.runOnClient(mc->{if(isolated!=null)isolated.close();module().setEnabled(false);});}
        Client.INSTANCE.getLogger().info("BaseFinder native checks passed: {}",checks);
    }
    private static ClientboundLightUpdatePacket light(int section,boolean zeroSky,boolean clearBlock){
        var buf=new FriendlyByteBuf(Unpooled.buffer());
        try{
            buf.writeVarInt(0);buf.writeVarInt(0);var mask=new BitSet();mask.set(section);
            buf.writeBitSet(new BitSet());buf.writeBitSet(zeroSky||clearBlock?new BitSet():mask);buf.writeBitSet(zeroSky?mask:new BitSet());buf.writeBitSet(clearBlock?mask:new BitSet());
            buf.writeVarInt(0);buf.writeVarInt(zeroSky||clearBlock?0:1);
            if(!zeroSky&&!clearBlock){byte[] data=new byte[2048];int i=index(5,0,5);data[i>>1]=(byte)(5<<((i&1)*4));buf.writeByteArray(data);}
            return ClientboundLightUpdatePacket.STREAM_CODEC.decode(buf);
        }finally{buf.release();}
    }
}
