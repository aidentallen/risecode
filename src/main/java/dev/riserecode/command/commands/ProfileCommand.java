package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.config.ProfileManager;
import java.util.Locale;

public final class ProfileCommand extends Command {
    private final ProfileManager profiles;
    private final CommandManager commands;
    public ProfileCommand(ProfileManager profiles, CommandManager commands) {
        super("profile", "Save or load module configurations", "profile <save|load> <name> | profile list");
        this.profiles = profiles; this.commands = commands;
    }
    @Override
    public void execute(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            var names = profiles.getProfiles(); commands.reply(names.isEmpty() ? "No profiles saved" : "Profiles: " + String.join(", ", names));
            return;
        }
        if (args.length != 2) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> commands.reply(profiles.saveProfile(args[1]) ? "Saved profile " + args[1] : "Could not save profile " + args[1]);
            case "load" -> { commands.reply("Loading profile " + args[1]); profiles.loadProfileAsync(args[1]).thenAccept(success -> commands.reply(success ? "Loaded profile " + args[1] : "Profile missing, invalid, or Resources unavailable: " + args[1])); }
            default -> throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage());
        }
    }
}
