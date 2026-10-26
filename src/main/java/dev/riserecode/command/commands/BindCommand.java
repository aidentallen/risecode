package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.ModuleManager;
import java.util.Arrays;
import java.util.Locale;
import dev.riserecode.module.Module;

public final class BindCommand extends Command {
    private final ModuleManager modules;
    private final KeybindManager keys;
    private final CommandManager commands;
    public BindCommand(ModuleManager modules, KeybindManager keys, CommandManager commands) {
        super("bind", "Assign a key and activation mode; NONE clears it", "bind <module> <key|NONE> [hold|toggle]");
        this.modules = modules; this.keys = keys; this.commands = commands;
    }
    @Override
    public void execute(String[] args) {
        if (args.length < 2) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        int end = args.length; Module.BindMode mode = null;
        String last = args[end - 1].toUpperCase(Locale.ROOT);
        if (end >= 3 && (last.equals("HOLD") || last.equals("TOGGLE"))) { mode = Module.BindMode.valueOf(last); end--; }
        String name = String.join(" ", Arrays.copyOf(args, end - 1));
        var module = modules.getByName(name).orElseThrow(() -> new IllegalArgumentException("Unknown module: " + name));
        keys.bind(module, args[end - 1]);
        if (mode != null) { keys.setMode(module, mode); }
        commands.reply(module.getName() + " bound to " + KeybindManager.keyName(module.getKeybind()) + " (" + module.getBindMode().name().toLowerCase(Locale.ROOT) + ")");
    }
}
