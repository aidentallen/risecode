package dev.riserecode.integration;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import java.util.List;
import net.minecraft.world.entity.player.Player;

/** Uses the official public API only; no bundled Baritone implementation. */
final class OfficialBaritoneAccess implements BaritoneAccess {
    private boolean paused;
    private IBaritone api() { return BaritoneAPI.getProvider().getPrimaryBaritone(); }
    public boolean available() { return true; }
    public String status() {
        if (paused) { return "Paused"; }
        var process = api().getPathingControlManager().mostRecentInControl();
        return process.filter(p -> p.isActive()).map(p -> p.displayName()).orElse(api().getPathingBehavior().isPathing() ? "Pathing" : "Idle");
    }
    public void go(int x, int y, int z) { stop(); api().getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z)); }
    public void mine(String targets) {
        String[] names = targets.strip().split("[ ,]+");
        if (targets.isBlank()) { throw new IllegalArgumentException("Enter at least one mining target"); }
        stop(); api().getMineProcess().mineByName(names);
    }
    public void follow(String player) {
        if (player.isBlank()) { throw new IllegalArgumentException("Enter a player name"); }
        stop(); api().getFollowProcess().follow(entity -> entity instanceof Player target && target.getGameProfile().name().equalsIgnoreCase(player));
    }
    public boolean command(String input) {
        boolean result = api().getCommandManager().execute(input);
        if (result && input.strip().equalsIgnoreCase("pause")) { paused = true; api().getInputOverrideHandler().clearAllKeys(); }
        if (result && (input.strip().equalsIgnoreCase("resume") || input.strip().equalsIgnoreCase("cancel"))) { paused = false; }
        return result;
    }
    public List<String> complete(String input) {
        String command=input.stripLeading();var manager=api().getCommandManager();
        return (command.chars().noneMatch(Character::isWhitespace)
                ? new baritone.api.command.helpers.TabCompleteHelper().addCommands(manager).filterPrefix(command).sortAlphabetically().stream()
                : manager.tabComplete(command)).limit(100).toList();
    }
    public void stop() {
        if (paused) { api().getCommandManager().execute("cancel"); }
        api().getPathingBehavior().cancelEverything();api().getPathingBehavior().forceCancel();api().getInputOverrideHandler().clearAllKeys();paused=false;
    }
}
