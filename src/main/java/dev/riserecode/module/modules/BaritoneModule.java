package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.integration.BaritoneAccess;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.StringSetting;
import net.minecraft.client.Minecraft;

public final class BaritoneModule extends Module {
    public enum Task { DESTINATION, MINE, FOLLOW, COMMAND }
    public final EnumSetting<Task> task = register(new EnumSetting<>("Task", "Task started only with Start", Task.DESTINATION));
    public final DoubleSetting x = register(DoubleSetting.extended("Destination X", "Block X", 0, -30_000_000, 30_000_000));
    public final DoubleSetting y = register(DoubleSetting.extended("Destination Y", "Block Y", 64, -2048, 2048));
    public final DoubleSetting z = register(DoubleSetting.extended("Destination Z", "Block Z", 0, -30_000_000, 30_000_000));
    public final StringSetting targets = register(new StringSetting("Mining Targets", "Block names separated by spaces", "minecraft:diamond_ore"));
    public final StringSetting player = register(new StringSetting("Follow Player", "Player name", "", 64));
    public final StringSetting input = register(new StringSetting("Command", "Baritone command", "help"));
    private BaritoneAccess access;
    private Object level;
    private String feedback = "";
    public BaritoneModule(EventBus bus) {
        super("Baritone", "Navigation, mining and following through the official API", Category.UTILITY, bus);
        x.setVisibleWhen(() -> task.getValue() == Task.DESTINATION); y.setVisibleWhen(x::isVisible); z.setVisibleWhen(x::isVisible);
        targets.setVisibleWhen(() -> task.getValue() == Task.MINE); player.setVisibleWhen(() -> task.getValue() == Task.FOLLOW); input.setVisibleWhen(() -> task.getValue() == Task.COMMAND);
    }
    public BaritoneAccess access() { if (access == null) { access = BaritoneAccess.create(); } return access; }
    public String status() { return feedback.isEmpty() ? access().status() : feedback; }
    @Override protected void onEnable() { level = Minecraft.getInstance().level; feedback = ""; listen(UpdateEvent.class, event -> {
        Object current = Minecraft.getInstance().level;
        if (current != level) { access().stop(); level = current; feedback = ""; }
    }); }
    @Override protected void onDisable() { access().stop(); level = null; feedback = ""; }
    public void action(String action) {
        try {
            if (!isEnabled()) { throw new IllegalStateException("Enable Baritone before starting a task"); }
            if (!access().available()) { throw new IllegalStateException(access().status()); }
            if (action.equals("Start")) {
                if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) { throw new IllegalStateException("Join a world to start a task"); }
                switch (task.getValue()) { case DESTINATION -> access().go(x.getValue().intValue(), y.getValue().intValue(), z.getValue().intValue()); case MINE -> access().mine(targets.getValue()); case FOLLOW -> access().follow(player.getValue()); case COMMAND -> access().command(input.getValue()); }
            } else if (action.equals("Stop")) { access().stop(); }
            else { access().command(action.toLowerCase(java.util.Locale.ROOT)); }
            feedback = "";
        } catch (IllegalArgumentException | IllegalStateException failure) { feedback = failure.getMessage(); }
    }
    public void forward(String command) {
        String verb = command.strip().toLowerCase(java.util.Locale.ROOT).split("\\s+", 2)[0];
        if (!java.util.Set.of("help", "status", "version", "commands").contains(verb) && !isEnabled()) { dev.riserecode.Client.INSTANCE.getModuleManager().setEnabled(this, true); }
        if (!access().available()) { throw new IllegalArgumentException(access().status()); }
        if (!access().command(command)) { throw new IllegalArgumentException("Unknown Baritone command"); }
    }
}
