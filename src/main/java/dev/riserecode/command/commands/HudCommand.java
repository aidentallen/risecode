package dev.riserecode.command.commands;

import dev.riserecode.Client;
import dev.riserecode.command.Command;

public final class HudCommand extends Command {
    public HudCommand() { super("hud", "Open the HUD editor", "hud", "hudedit"); }
    @Override public void execute(String[] args) {
        if (args.length != 0) { throw new IllegalArgumentException("Usage: .hud"); }
        Client.INSTANCE.openHudEditor();
    }
}
