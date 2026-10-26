package dev.riserecode.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;

/** Prefix messages are consumed locally, including malformed or unknown commands. */
public final class CommandManager {
    private final Map<String, Command> lookup = new LinkedHashMap<>();
    private final List<Command> commands = new ArrayList<>();
    private final Supplier<String> prefix;
    private final Consumer<String> setPrefix;
    private final Consumer<String> feedback;
    private final Logger logger;

    public CommandManager(Supplier<String> prefix, Consumer<String> setPrefix, Consumer<String> feedback, Logger logger) {
        this.prefix = Objects.requireNonNull(prefix); this.setPrefix = Objects.requireNonNull(setPrefix);
        this.feedback = Objects.requireNonNull(feedback); this.logger = Objects.requireNonNull(logger);
    }
    public String getPrefix() { return prefix.get(); }
    public void setPrefix(String prefix) { setPrefix.accept(prefix); }
    public List<Command> getCommands() { return List.copyOf(commands); }
    public void reply(String message) { feedback.accept(message); }

    public void register(Command command) {
        Objects.requireNonNull(command);
        List<String> names = new ArrayList<>(command.getAliases()); names.add(command.getName());
        for (String name : names) {
            if (lookup.containsKey(name)) { throw new IllegalArgumentException("Duplicate command or alias: " + name); }
        }
        for (String name : names) { lookup.put(name, command); }
        commands.add(command);
    }

    public boolean handleChat(String message) {
        String currentPrefix = getPrefix();
        if (!message.startsWith(currentPrefix)) { return false; }
        try {
            String[] parts = parseArguments(message.substring(currentPrefix.length()));
            if (parts.length == 0) { reply("Use " + currentPrefix + "help for commands."); return true; }
            Command command = lookup.get(parts[0].toLowerCase(Locale.ROOT));
            if (command == null) { reply("Unknown command: " + parts[0] + ". Use " + currentPrefix + "help."); return true; }
            command.execute(Arrays.copyOfRange(parts, 1, parts.length));
        } catch (IllegalArgumentException failure) {
            reply(failure.getMessage());
        } catch (RuntimeException failure) {
            logger.error("Client command failed", failure); reply("Command failed. Check the client log.");
        }
        return true;
    }

    /** Supports whitespace, quoted arguments, empty quotes, and backslash escapes. */
    public static String[] parseArguments(String input) {
        List<String> arguments = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        boolean escaped = false;
        boolean started = false;
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (escaped) { token.append(character); escaped = false; started = true; }
            else if (character == '\\') { escaped = true; started = true; }
            else if (quote != 0) {
                if (character == quote) { quote = 0; } else { token.append(character); }
            } else if (character == '"' || character == '\'') { quote = character; started = true; }
            else if (Character.isWhitespace(character)) {
                if (started) { arguments.add(token.toString()); token.setLength(0); started = false; }
            } else { token.append(character); started = true; }
        }
        if (quote != 0) { throw new IllegalArgumentException("Unclosed quote in command"); }
        if (escaped) { throw new IllegalArgumentException("Command ends with an incomplete escape"); }
        if (started) { arguments.add(token.toString()); }
        return arguments.toArray(String[]::new);
    }
}
