package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.PacketEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * Vanilla flight controls on any connection, with local server authority on a
 * hosted world. Ported from donut-client (MIT); see licenses/donut-client-MIT.txt.
 * Remote vanilla servers keep their own authority and may reject flight.
 */
public final class FlyModule extends Module {
    private static volatile FlyModule instance;
    private final Minecraft minecraft;
    private volatile Session session;
    /** Accessed exclusively on the integrated server thread, including the player-save hook. */
    private Session serverSession;

    public FlyModule(EventBus bus, Minecraft minecraft) {
        super("Fly", "Use vanilla flight controls; hosted worlds grant server authority", Category.MOVEMENT, bus);
        this.minecraft = minecraft;
        instance = this;
    }

    public static boolean enabled() {
        FlyModule module = instance;
        return module != null && module.isEnabled() && module.session != null && !module.session.closed;
    }

    public boolean isActive() {
        Session current = session;
        return current != null && !current.closed && current.matches(minecraft);
    }

    @Override
    protected void onEnable() {
        listen(UpdateEvent.class, this::update);
        listen(PacketEvent.Receive.class, this::receive);
    }

    @Override
    protected void onDisable() {
        closeSession();
    }

    private void update(UpdateEvent event) {
        Minecraft client = event.getMinecraft();
        Session current = session;
        if (current != null && (current.closed || !current.matches(client))) {
            closeSession();
            current = null;
        }
        if (current == null && client.player != null && client.level != null && client.gameMode != null
                && client.player.isAlive() && !client.player.isRemoved()) {
            session = new Session(client, client.getSingleplayerServer());
            current = session;
        }
        if (current != null && !current.closed) {
            current.applyClient(client);
            IntegratedServer server = current.server;
            if (server != null && !current.serverReady) {
                // Capture the local server baseline before the client can send a flying=true packet.
                server.execute(() -> serverTick(server));
            }
        }
    }

    /** Runs on a Netty thread; records the remote server's latest flight permissions for restoration. */
    private void receive(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof ClientboundPlayerAbilitiesPacket packet)) return;
        Session current = session;
        // On a hosted world the baseline comes from the server session, not from packets.
        if (current == null || current.closed || current.server != null) return;
        Minecraft client = minecraft;
        if (!current.matches(client)) return;
        current.clientMode = client.gameMode.getPlayerMode();
        current.clientFlags = new FlightFlags(packet.canFly(), packet.isFlying());
    }

    /** Only the session's server player strips temporary flight from its save data. */
    public static Abilities.Packed abilitiesForSave(Player player, Abilities abilities) {
        FlyModule module = instance;
        Session current = module == null ? null : module.serverSession;
        if (current == null || current.serverPlayer != player) return abilities.pack();
        Abilities saved = new Abilities();
        saved.apply(abilities.pack());
        current.serverBaseline().restore(saved);
        return saved.pack();
    }

    private void closeSession() {
        Session previous = session;
        session = null;
        if (previous == null) return;
        previous.closed = true;
        Minecraft client = minecraft;
        // Restore without sending a previous world's abilities through a new player connection.
        if (client.player == previous.player && client.level == previous.level
                && client.gameMode != null) {
            previous.restoreClient(client);
        }
        if (previous.server != null) {
            previous.server.execute(() -> serverTick(previous.server));
        }
    }

    /** Runs on the integrated server thread; grants, refreshes and restores hosted-world authority. */
    private void serverTick(MinecraftServer server) {
        Session desired = session;
        if (desired != null && (desired.closed || desired.server != server)) desired = null;
        if (serverSession != null && serverSession.server == server && serverSession != desired) {
            serverSession.restoreServer();
            serverSession = null;
        }
        if (desired == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(desired.playerId);
        if (player == null || !player.isAlive() || player.isRemoved()
                || desired.serverPlayer != null && desired.serverPlayer != player) {
            desired.closed = true;
            if (serverSession != null) { serverSession.restoreServer(); serverSession = null; }
            return;
        }
        if (serverSession == null) {
            desired.serverPlayer = player;
            desired.serverMode = player.gameMode();
            desired.serverFlags = FlightFlags.of(player.getAbilities());
            serverSession = desired;
        }
        desired.applyServer();
        desired.serverReady = true;
    }

    private record FlightFlags(boolean mayfly, boolean flying) {
        static FlightFlags of(Abilities abilities) { return new FlightFlags(abilities.mayfly, abilities.flying); }
        static FlightFlags forMode(GameType mode) {
            Abilities abilities = new Abilities();
            mode.updatePlayerAbilities(abilities);
            return of(abilities);
        }
        void restore(Abilities abilities) { abilities.mayfly = mayfly; abilities.flying = flying; }
    }

    private static final class Session {
        final IntegratedServer server;
        final LocalPlayer player;
        final ClientLevel level;
        final UUID playerId;
        volatile boolean closed;
        volatile boolean serverReady;
        GameType clientMode;
        FlightFlags clientFlags;
        GameType serverMode;
        FlightFlags serverFlags;
        ServerPlayer serverPlayer;

        Session(Minecraft client, IntegratedServer server) {
            this.server = server;
            serverReady = server == null;
            player = client.player;
            level = client.level;
            playerId = player.getUUID();
            clientMode = client.gameMode.getPlayerMode();
            clientFlags = FlightFlags.of(player.getAbilities());
            serverFlags = clientFlags;
        }

        boolean matches(Minecraft client) {
            return client.player == player && client.level == level && client.gameMode != null
                    && client.getSingleplayerServer() == server
                    && player.isAlive() && !player.isRemoved();
        }

        void applyClient(Minecraft client) {
            GameType mode = client.gameMode.getPlayerMode();
            if (clientMode != mode) { clientMode = mode; clientFlags = FlightFlags.forMode(mode); }
            Abilities abilities = player.getAbilities();
            boolean changed = !abilities.mayfly || !abilities.flying;
            abilities.mayfly = true;
            abilities.flying = true;
            if (changed) player.onUpdateAbilities();
        }

        void restoreClient(Minecraft client) {
            if (client.player != player || client.level != level || client.gameMode == null) return;
            GameType mode = client.gameMode.getPlayerMode();
            (clientMode == mode ? clientFlags : FlightFlags.forMode(mode)).restore(player.getAbilities());
            player.onUpdateAbilities();
        }

        FlightFlags serverBaseline() {
            GameType mode = serverPlayer.gameMode();
            return serverMode == mode ? serverFlags : FlightFlags.forMode(mode);
        }

        void applyServer() {
            GameType mode = serverPlayer.gameMode();
            if (serverMode != mode) { serverMode = mode; serverFlags = FlightFlags.forMode(mode); }
            Abilities abilities = serverPlayer.getAbilities();
            boolean changed = !abilities.mayfly || !abilities.flying;
            abilities.mayfly = true;
            abilities.flying = true;
            if (changed) serverPlayer.onUpdateAbilities();
        }

        void restoreServer() {
            if (serverPlayer == null) return;
            serverBaseline().restore(serverPlayer.getAbilities());
            serverPlayer.onUpdateAbilities();
        }
    }
}
