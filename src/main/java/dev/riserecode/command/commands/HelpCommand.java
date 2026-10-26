package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;

public final class HelpCommand extends Command {
    private final CommandManager commands;
    public HelpCommand(CommandManager commands) { super("help", "List local commands", "help", "h"); this.commands = commands; }
    @Override
    public void execute(String[] args) {
        if (args.length != 0) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        for (Command command : commands.getCommands()) {
            commands.reply(commands.getPrefix() + command.getUsage() + " — " + command.getDescription());
        }
    }
}
