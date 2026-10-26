package dev.riserecode.integration;

import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** The optional dependency is linked only when Fabric reports it installed. */
public interface BaritoneAccess {
    boolean available();
    String status();
    void go(int x, int y, int z);
    void mine(String targets);
    void follow(String player);
    boolean command(String input);
    List<String> complete(String input);
    void stop();
    static BaritoneAccess create() {
        if (FabricLoader.getInstance().isModLoaded("baritone")) {
            try { return new OfficialBaritoneAccess(); }
            catch (LinkageError | RuntimeException failure) { dev.riserecode.Client.INSTANCE.getLogger().warn("Baritone API unavailable", failure); }
        }
        return new BaritoneAccess() {
            public boolean available() { return false; }
            public String status() { return "Unavailable — install Baritone API Fabric 1.19.0"; }
            private void unavailable() { throw new IllegalStateException(status()); }
            public void go(int x, int y, int z) { unavailable(); }
            public void mine(String targets) { unavailable(); }
            public void follow(String player) { unavailable(); }
            public boolean command(String input) { unavailable(); return false; }
            public List<String> complete(String input) { return List.of(); }
            public void stop() { }
        };
    }
}
