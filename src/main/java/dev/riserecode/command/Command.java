package dev.riserecode.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class Command {
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;

    protected Command(String name, String description, String usage, String... aliases) {
        this.name = normalize(name); this.description = Objects.requireNonNull(description);
        this.usage = Objects.requireNonNull(usage);
        this.aliases = Arrays.stream(aliases).map(Command::normalize).distinct().toList();
    }
    public final String getName() { return name; }
    public final List<String> getAliases() { return aliases; }
    public final String getUsage() { return usage; }
    public final String getDescription() { return description; }
    public abstract void execute(String[] args);

    private static String normalize(String name) {
        String normalized = Objects.requireNonNull(name).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z][a-z0-9_-]*")) { throw new IllegalArgumentException("Invalid command name " + name); }
        return normalized;
    }
}
