package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.module.ModuleManager;

public final class ToggleCommand extends Command {
    private final ModuleManager modules;
    private final CommandManager commands;
    public ToggleCommand(ModuleManager modules, CommandManager commands) {
        super("toggle", "Toggle a module", "toggle <module>", "t"); this.modules = modules; this.commands = commands;
    }
    @Override
    public void execute(String[] args) {
        if (args.length == 0) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        var module = modules.getByName(String.join(" ", args))
                .orElseThrow(() -> new IllegalArgumentException("Unknown module: " + String.join(" ", args)));
        if (!modules.toggle(module)) { throw new IllegalStateException("Could not toggle " + module.getName()); }
        commands.reply(module.getName() + (module.isEnabled() ? " enabled" : " disabled"));
    }
}
