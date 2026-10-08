package dev.riserecode.friend;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Mutations belong to the client thread; immutable snapshots are safe for readers. */
public final class FriendManager {
    public record Friend(String name, UUID uuid) {
        public Friend { validateName(name); }
    }

    private volatile Map<String, Friend> friends = Map.of();
    private Runnable changeListener = () -> { };

    public List<Friend> getFriends() { return List.copyOf(friends.values()); }
    public void setChangeListener(Runnable listener) { changeListener = Objects.requireNonNull(listener); }
    public boolean add(String name) { return add(name, null); }

    public boolean add(String name, UUID uuid) {
        validateName(name);
        Map<String, Friend> next = new LinkedHashMap<>(friends);
        String key = normalize(name);
        Friend previous = next.get(key);
        if (uuid == null && previous != null) { uuid = previous.uuid(); }
        UUID identity = uuid;
        if (identity != null) { next.values().removeIf(friend -> identity.equals(friend.uuid()) && !normalize(friend.name()).equals(key)); }
        Friend updated = new Friend(name, uuid);
        next.put(key, updated);
        if (next.equals(friends)) { return false; }
        friends = Collections.unmodifiableMap(next);
        changeListener.run();
        return true;
    }

    public boolean remove(String name) {
        Map<String, Friend> next = new LinkedHashMap<>(friends);
        if (next.remove(normalize(name)) == null) { return false; }
        friends = Collections.unmodifiableMap(next); changeListener.run(); return true;
    }

    public boolean remove(UUID uuid) {
        Map<String, Friend> next = new LinkedHashMap<>(friends);
        if (!next.values().removeIf(friend -> uuid.equals(friend.uuid()))) { return false; }
        friends = Collections.unmodifiableMap(next); changeListener.run(); return true;
    }

    public boolean isFriend(String name) { return name != null && friends.containsKey(normalize(name)); }
    public boolean isFriend(UUID uuid, String name) {
        Map<String, Friend> snapshot = friends;
        return (name != null && snapshot.containsKey(normalize(name)))
                || (uuid != null && snapshot.values().stream().anyMatch(friend -> uuid.equals(friend.uuid())));
    }

    public void replace(Collection<Friend> entries) {
        Map<String, Friend> next = new LinkedHashMap<>();
        for (Friend friend : entries) {
            Objects.requireNonNull(friend);
            if (friend.uuid() != null) { next.values().removeIf(existing -> friend.uuid().equals(existing.uuid())); }
            next.put(normalize(friend.name()), friend);
        }
        friends = Collections.unmodifiableMap(next); changeListener.run();
    }

    private static void validateName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("Player names must contain 1–16 letters, digits, or underscores");
        }
    }
    private static String normalize(String name) { return Objects.requireNonNull(name).strip().toLowerCase(Locale.ROOT); }
}
