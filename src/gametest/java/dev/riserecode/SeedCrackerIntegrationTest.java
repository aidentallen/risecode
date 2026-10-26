package dev.riserecode;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mcfeature.structure.RegionStructure;
import dev.riserecode.event.Render3DEvent;
import dev.riserecode.module.modules.SeedCrackerModule;
import dev.riserecode.seedcracker.*;
import dev.riserecode.seedcracker.cracker.PillarData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/** Packaged startup, native placement rules, observation patterns and module lifetime. */
public final class SeedCrackerIntegrationTest {
    private static final long SEED=-6024798231123456789L;
    private static int checks;
    private static net.minecraft.core.BlockPos located;
    private static List<dev.riserecode.seedcracker.Observation> nativeObservations=List.of();
    private static void check(boolean ok,String label) { checks++; if (!ok) { throw new AssertionError(label); } }
    private static SeedCrackerModule module() { return Client.INSTANCE.getModuleManager().getByClass(SeedCrackerModule.class).orElseThrow(); }
    public static void run(ClientGameTestContext c) {
        c.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay()==null,1200);
        c.runOnClient(mc -> { Client.INSTANCE.getModuleManager().disableAll(); mc.options.pauseOnLostFocus=false; mc.options.enableVsync().set(false); mc.options.framerateLimit().set(90); check(!module().isEnabled(),"disabled by default"); Features.init(dev.riserecode.seedcracker.config.Config.get().getVersion()); check(!module().databaseLookup.getValue() && !module().databaseSubmits.getValue(),"database defaults produce no requests"); });
        try (var world=c.worldBuilder().adjustSettings(settings -> {
            settings.setSeed(Long.toString(SEED)); settings.setGenerateStructures(true); settings.setWorldType(settings.getNormalPresetList().stream().filter(e -> e.preset()!=null && e.preset().is(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)).findFirst().orElseThrow());
        }).create()) {
            c.waitTicks(20);
            world.getServer().runOnServer(server -> {
                CameraFovClientTest.ownerOnlyShutdown=true;
                check(!server.overworld().getChunkSource().getGeneratorState().possibleStructureSets().isEmpty(),"known world generates natural structures"); long seed=server.overworld().getSeed(); check(seed==SEED,"known native world seed");
                check(WorldSeed.toHash(seed)==BiomeManager.obfuscateSeed(seed),"library hash matches native Minecraft 26.2");
                nativeRules(server.overworld(),seed);
            });
            c.runOnClient(mc -> { module().setEnabled(true); check(module().job()==null,"enable never starts cracking"); module().start(); });
            c.waitFor(mc -> module().job().progress().complete(),200);
            c.runOnClient(mc -> { check(module().job().progress().message().startsWith("Insufficient data"),"insufficient data message"); module().clearData(); });
            world.getServer().runCommand("gamemode creative @a"); c.waitTicks(4);
            c.runOnClient(mc -> { mc.player.getAbilities().flying=true; mc.player.onUpdateAbilities(); });
            world.getServer().runCommand("forceload add -16 -16 32 32");
            world.getServer().runCommand("tp @a 8 110 40 180 45"); c.waitTicks(40);
            world.getServer().runOnServer(server -> {
                var level=server.overworld();
                for (int x=-1;x<=2;x++) { for (int z=-1;z<=2;z++) { level.getChunk(x,z); } }
                var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SWAMP_HUT);
                try { check(net.minecraft.server.commands.PlaceCommand.placeStructure(server.createCommandSourceStack(),holder,new net.minecraft.core.BlockPos(0,80,0))==1,"native swamp-hut fixture generated"); }
                catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new AssertionError(failure); }
            }); c.waitTicks(10);
            c.runOnClient(mc -> { module().clearData(); module().rescan(); });
            try { c.waitFor(mc -> !module().data().observations().isEmpty(),600); }
            catch (AssertionError failure) { c.runOnClient(mc -> { Client.INSTANCE.getLogger().error("Finder state: {}",module().status()); diagnose(mc); }); throw failure; }
            c.runOnClient(mc -> {
                check(module().job().progress().complete(),"new observations never start another attempt");
                check(module().data().observations().stream().anyMatch(o -> o.type().equals(Features.SWAMP_HUT.getName())),"native generated swamp hut recognized by upstream pattern");
            });
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")) {
                c.runOnClient(mc -> {
                    var config=net.irisshaders.iris.Iris.getIrisConfig(); config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip")); config.setShadersEnabled(true);
                    try { config.save(); net.irisshaders.iris.Iris.reload(); } catch (java.io.IOException failure) { throw new AssertionError(failure); }
                    check(net.irisshaders.iris.Iris.getCurrentPack().isPresent(),"active Iris shader pack");
                }); c.waitTicks(10);
            }
            c.runOnClient(mc -> { mc.gui.setScreen(Client.INSTANCE.getClickGui()); Client.INSTANCE.getClickGui().selectPage(dev.riserecode.ui.clickgui.ClickGui.Page.SEARCH); Client.INSTANCE.getClickGui().getSearchField().setValue("SeedCracker"); }); c.waitTicks(12);
            c.runOnClient(mc -> { var button=Client.INSTANCE.getClickGui().getModuleButton("SeedCracker"); button.mouseClicked(button.getBounds().x()+10,button.getBounds().y()+10,1); check(button.isExpanded(),"themed module controls expand"); }); c.waitTicks(12); c.runOnClient(mc -> check(mc.gui.screen()==Client.INSTANCE.getClickGui(),"module controls remain the active screen")); c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F2); c.waitTicks(5);
            c.runOnClient(mc -> mc.gui.setScreen(null));
            AtomicInteger lines=new AtomicInteger();
            var subscription=Client.INSTANCE.getEventBus().subscribe(Render3DEvent.class,-100,false,event -> {
                if (event.getLines().size()>0) { lines.set(event.getLines().size()); }
            });
            try {
                for (var mode : SeedCrackerModule.OutlineMode.values()) {
                    c.runOnClient(mc -> { lines.set(0); module().outlines.setValue(mode); }); c.waitTicks(5); c.takeScreenshot("028-outlines-"+mode.name().toLowerCase(java.util.Locale.ROOT));
                    c.runOnClient(mc -> check(mode==SeedCrackerModule.OutlineMode.OFF ? lines.get()==0 : lines.get()>0,"packaged outline geometry "+mode));
                }
            } finally { subscription.close(); }
            c.runOnClient(mc -> {
                module().generation.setValue("isolation-fixture"); // tick applies a new identity
            }); c.waitTicks(2);
            c.runOnClient(mc -> { check(module().data().observations().isEmpty(),"generation profile isolates observations"); module().generation.setValue("default"); }); c.waitTicks(2);
            c.runOnClient(mc -> { check(module().data().observations().isEmpty(),"travel/profile changes never restore implicitly"); module().restore(); check(!module().data().observations().isEmpty(),"explicit matching restore"); });
            // Start a substantial job, then travel: old worker results must be rejected.
            c.runOnClient(mc -> { module().clearData(); for (var o : nativeObservations.stream().limit(5).toList()) { module().data().addBaseData(o.data(),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING); } check(module().job().progress().complete(),"sufficient collection still waits for manual Start"); module().start(); Client.INSTANCE.getEventBus().post(new dev.riserecode.event.ChunkLifecycleEvent(dev.riserecode.event.ChunkLifecycleEvent.Kind.LOAD,0)); check(!module().job().progress().cancelled(),"ordinary chunk load never cancels cracking or resets collection"); });
            world.getServer().runCommand("execute in minecraft:the_end run tp @a 0 90 0");
            c.waitFor(mc -> mc.level!=null && mc.level.dimension()==net.minecraft.world.level.Level.END,600);
            c.runOnClient(mc -> { check(module().job().progress().cancelled(),"dimension travel cancels active job"); check(!module().data().observations().isEmpty(),"compatible structure observations survive dimension travel");
                // Limit the rescan to End pillars so the real central End observation is tested promptly.
                for (var setting : module().getSettings()) { if (setting instanceof dev.riserecode.setting.BooleanSetting bool && setting.getName().startsWith("Find ")) { bool.setValue(setting.getName().equals("Find End Pillars")); } }
                module().rescan();
            });
            c.waitFor(mc -> module().data().pillars().size()==10,1200);
            world.getServer().runOnServer(server -> {
                var expected=net.minecraft.world.level.levelgen.feature.EndSpikeFeature.getSpikesForLevel(server.getLevel(net.minecraft.world.level.Level.END)).stream().map(net.minecraft.world.level.levelgen.feature.EndSpikeFeature.EndSpike::getHeight).toList();
                check(new PillarData(List.of()).getPillarHeights((int)com.seedfinding.mccore.rand.seed.PillarSeed.fromStructureSeed(SEED)).equals(expected),"native 26.2 End height permutation");
            });
            c.runOnClient(mc -> check(new PillarData(module().data().pillars()).test(com.seedfinding.mccore.rand.seed.PillarSeed.fromStructureSeed(SEED)),"real End bedrock observations match known world"));
            for (int i=0;i<6;i++) {
                c.runOnClient(mc -> module().setEnabled(false)); c.runOnClient(mc -> { module().setEnabled(true); check(module().data().observations().isEmpty(),"toggle never restores implicitly"); module().restore(); check(!module().data().pillars().isEmpty(),"End observations saved and restored"); });
            }
            if (Boolean.getBoolean("riserecode.seedCrackerRecovery")) {
                var realPillars=new java.util.concurrent.atomic.AtomicReference<List<Integer>>(); c.runOnClient(mc -> realPillars.set(module().data().pillars()));
                c.runOnClient(mc -> { module().clearData(); module().data().addPillarData(new PillarData(realPillars.get()),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_PILLARS);
                    for (var setting : module().getSettings()) { if (setting instanceof dev.riserecode.setting.BooleanSetting bool && setting.getName().startsWith("Find ")) { bool.setValue(setting.getName().equals("Find End Cities")); } }
                });
                for (int city=0;city<6;city++) {
                    final int index=city;
                    world.getServer().runOnServer(server -> {
                        var end=server.getLevel(net.minecraft.world.level.Level.END);
                        var holder=end.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.END_CITY);
                        var result=end.getChunkSource().getGenerator().findNearestMapStructure(end,net.minecraft.core.HolderSet.direct(holder),new net.minecraft.core.BlockPos(3000+index*5000,80,2000+index*5000),100,false);
                        check(result!=null,"locate real generated End city "+index); located=result.getFirst();
                    });
                    c.runOnClient(mc -> { mc.player.getAbilities().flying=true; mc.player.onUpdateAbilities(); });
                    world.getServer().runCommand("execute in minecraft:the_end run tp @a "+(located.getX()+8)+" "+(Math.max(100,located.getY()+20))+" "+(located.getZ()+16)+" 180 35"); c.waitTicks(30);
                    c.runOnClient(mc -> { check(mc.level.dimension()==net.minecraft.world.level.Level.END,"real End-city observation stays in End dimension"); module().rescan(); });
                    c.waitFor(mc -> module().data().observations().stream().filter(o -> o.type().equals(Features.END_CITY.getName())).count()>=index+1,1600);
                }
                c.takeScreenshot("028-real-end-city");
                c.runOnClient(mc -> { check(CrackingJob.regularBits(module().data().observations())>=32,"real End-city observations supply sufficient bits"); module().start(); });
                c.waitFor(mc -> module().job().progress().complete(),24000); c.waitTicks(2);
                c.runOnClient(mc -> check(module().verifiedSeeds().equals(List.of(SEED)),"real End-city + pillar seed recovery and hash confirmation: "+module().status()));
                // Full, unshortened upstream 48-bit lifting search using native
                // placement observations, then a full 16-bit hashed-seed search.
                c.runOnClient(mc -> {
                    module().clearData();
                    for (var o : nativeObservations) { module().data().addBaseData(o.data(),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING); }
                    module().start();
                    module().data().addBaseData(Features.DESERT_PYRAMID.at(0,0),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING);
                });
                c.waitFor(mc -> module().job().progress().complete(),24000);
                c.waitTicks(2);
                c.runOnClient(mc -> check(module().verifiedSeeds().equals(List.of(SEED)),"full native-placement seed recovery and hash confirmation: "+module().status()));
            }
            c.runOnClient(mc -> { module().setEnabled(false); check(module().verifiedSeeds().isEmpty(),"disable clears active session results"); });
        } finally { CameraFovClientTest.ownerOnlyShutdown=false; }
        Client.INSTANCE.getLogger().info("SeedCracker native checks passed ({} assertions)",checks);
    }
    private static void diagnose(net.minecraft.client.Minecraft mc) {
        try {
            var finder=new dev.riserecode.seedcracker.finder.structure.SwampHutFinder(mc.level,new net.minecraft.world.level.ChunkPos(0,0));
            var field=dev.riserecode.seedcracker.finder.structure.AbstractTempleFinder.class.getDeclaredField("finders"); field.setAccessible(true);
            var structure=dev.riserecode.seedcracker.finder.structure.PieceFinder.class.getDeclaredField("structure"); structure.setAccessible(true);
            int best=Integer.MAX_VALUE; String detail="";
            for (Object piece : (List<?>)field.get(finder)) {
                @SuppressWarnings("unchecked") var pattern=(java.util.Map<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>)structure.get(piece);
                for (int y=0;y<=200;y++) {
                    int mismatches=0; StringBuilder reason=new StringBuilder();
                    for (var entry : pattern.entrySet()) {
                        var at=entry.getKey().offset(0,y,0); var actual=mc.level.getBlockState(at);
                        if (actual.getBlock()!=entry.getValue().getBlock()) { mismatches++; if (mismatches<8) { reason.append(at).append(": ").append(actual).append(" expected ").append(entry.getValue()).append("; "); } }
                    }
                    if (mismatches<best) { best=mismatches; detail="origin y="+y+" "+reason; }
                }
            }
            Client.INSTANCE.getLogger().error("Best hut pattern mismatch count={} {}",best,detail);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void nativeRules(net.minecraft.server.level.ServerLevel level,long seed) {
        var keys=List.of("desert_pyramids","igloos","jungle_temples","swamp_huts","shipwrecks","ocean_monuments","end_cities","trial_chambers");
        List<RegionStructure<?,?>> models=List.of(Features.DESERT_PYRAMID,Features.IGLOO,Features.JUNGLE_PYRAMID,Features.SWAMP_HUT,Features.SHIPWRECK,Features.MONUMENT,Features.END_CITY,Features.TRIAL_CHAMBERS);
        var observations=new ArrayList<Observation>();
        // Initialize without activating collection or a cracking job.
        for (int index=0;index<keys.size();index++) {
            var key=net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET,net.minecraft.resources.Identifier.withDefaultNamespace(keys.get(index)));
            var placement=(RandomSpreadStructurePlacement)level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(key).value().placement();
            for (int region=-8;region<=8;region++) {
                var pos=placement.getPotentialStructureChunk(seed,region*placement.spacing(),(region*3-7)*placement.spacing());
                var model=models.get(index); check(model.at(pos.x(),pos.z()).testStart(seed,new ChunkRand()),"native 26.2 placement matches "+keys.get(index)+" at negative/positive regions");
                if (index<5) { observations.add(new Observation(model.getName(),pos.x(),pos.z())); }
            }
        }
        var sets=level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        var outpostPlacement=(RandomSpreadStructurePlacement)sets.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructureSets.PILLAGER_OUTPOSTS).value().placement();
        for (int region=-10;region<=10;region++) {
            var pos=outpostPlacement.getPotentialStructureChunk(seed,region*32,(region*5)*32);
            var datum=(com.seedfinding.mcfeature.structure.UniformStructure.Data<?>)Features.PILLAGER_OUTPOST.at(pos.x(),pos.z());
            var rand=new ChunkRand(); rand.setRegionSeed(seed,datum.regionX,datum.regionZ,Features.PILLAGER_OUTPOST.getSalt(),Features.PILLAGER_OUTPOST.getVersion());
            check(rand.nextInt(Features.PILLAGER_OUTPOST.getOffset())%4==datum.offsetX%4 && rand.nextInt(Features.PILLAGER_OUTPOST.getOffset())%4==datum.offsetZ%4,"outpost lifting matches native 26.2 spread");
        }
        var treasure=(RandomSpreadStructurePlacement)sets.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructureSets.BURIED_TREASURES).value().placement();
        for (int x=-30;x<=30;x++) {
            for (int z=-5;z<=5;z++) {
                boolean nativeStart=treasure.isStructureChunk(level.getChunkSource().getGeneratorState(),x,z);
                check(Features.BURIED_TREASURE.at(x,z).testStart(seed,new ChunkRand())==nativeStart,"buried-treasure native frequency");
            }
        }
        nativeObservations=List.copyOf(observations);
    }
}
