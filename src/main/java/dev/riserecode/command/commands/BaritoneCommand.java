package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.module.modules.BaritoneModule;

public final class BaritoneCommand extends Command {
    private final BaritoneModule module;
    public BaritoneCommand(BaritoneModule module) { super("baritone", "Forward to the optional official Baritone API", "baritone <command>", "b"); this.module = module; }
    @Override public void execute(String[] args) { module.forward(args.length == 0 ? "help" : String.join(" ", args)); }
}
