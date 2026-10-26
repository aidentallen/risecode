package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.friend.FriendManager;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

public final class FriendCommand extends Command {
    private final FriendManager friends;
    private final CommandManager commands;
    private final Function<String, UUID> resolveUuid;
    public FriendCommand(FriendManager friends, CommandManager commands, Function<String, UUID> resolveUuid) {
        super("friend", "Manage the global friend list", "friend <add|remove> <player> | friend list", "friends");
        this.friends = friends; this.commands = commands; this.resolveUuid = resolveUuid;
    }
    @Override
    public void execute(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            commands.reply(friends.getFriends().isEmpty() ? "No friends added" : "Friends: "
                    + String.join(", ", friends.getFriends().stream().map(FriendManager.Friend::name).toList()));
            return;
        }
        if (args.length != 2) { throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage()); }
        String name = args[1];
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> commands.reply(friends.add(name, resolveUuid.apply(name)) ? "Added friend " + name : name + " is already a friend");
            case "remove", "delete", "del" -> {
                boolean removed;
                try { removed = friends.remove(UUID.fromString(name)); }
                catch (IllegalArgumentException ignored) { removed = friends.remove(name); }
                commands.reply(removed ? "Removed friend " + name : "Friend not found: " + name);
            }
            default -> throw new IllegalArgumentException("Usage: " + commands.getPrefix() + getUsage());
        }
    }
}
