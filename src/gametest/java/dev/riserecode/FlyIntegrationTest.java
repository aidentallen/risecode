package dev.riserecode;

import dev.riserecode.module.modules.FlyModule;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;

/** Client and hosted-server flight authority, restoration and clean save data. */
public final class FlyIntegrationTest {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static FlyModule module() { return Client.INSTANCE.getModuleManager().getByClass(FlyModule.class).orElseThrow(); }

    public static void run(ClientGameTestContext context) {
        context.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady(), 1200);
        context.runOnClient(mc -> { Client.INSTANCE.getModuleManager().disableAll(); mc.options.pauseOnLostFocus = false; });
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @a");
            context.waitTicks(10);
            check(!module().isEnabled(), "Fly starts disabled");
            context.runOnClient(mc -> module().setEnabled(true));
            context.waitTicks(10);
            context.runOnClient(mc -> {
                check(module().isActive(), "Fly session is active");
                check(mc.player.getAbilities().mayfly && mc.player.getAbilities().flying, "client flight granted");
            });
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                check(player.getAbilities().mayfly && player.getAbilities().flying, "hosted world grants server authority");
            });
            checkSavedAbilities(world, "save strips hosted-world flight");
            context.runOnClient(mc -> module().setEnabled(false));
            context.waitTicks(10);
            context.runOnClient(mc ->
                    check(!mc.player.getAbilities().mayfly && !mc.player.getAbilities().flying, "client flight restored"));
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                check(!player.getAbilities().mayfly && !player.getAbilities().flying, "hosted world flight restored");
            });
        }
    }

    /** Saves the world and reads the live player data file; expects vanilla survival abilities. */
    private static void checkSavedAbilities(TestSingleplayerContext world, String message) {
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().get(0);
            server.getPlayerList().saveAll();
            Path file = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID() + ".dat");
            try {
                CompoundTag root = NbtIo.readCompressed(file, new NbtAccounter(4_000_000, 64));
                CompoundTag abilities = root.getCompound("abilities").orElseThrow();
                check(!abilities.getBooleanOr("mayfly", false) && !abilities.getBooleanOr("flying", false), message);
                check(player.getAbilities().mayfly && player.getAbilities().flying,
                        "Saving player data keeps live hosted-world flight");
            } catch (Exception failure) {
                throw new AssertionError(message, failure);
            }
        });
    }
}
