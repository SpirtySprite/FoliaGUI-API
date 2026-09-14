package com.foliagui.gui;

import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class SessionRegistry<T> {

    private final Map<UUID, T> sessions = new ConcurrentHashMap<>();

    void put(@NotNull HumanEntity player, @NotNull T session) {
        sessions.put(player.getUniqueId(), session);
    }

    @Nullable T get(@NotNull HumanEntity player) {
        return sessions.get(player.getUniqueId());
    }

    @Nullable T remove(@NotNull HumanEntity player) {
        return sessions.remove(player.getUniqueId());
    }

    boolean has(@NotNull HumanEntity player) {
        return sessions.containsKey(player.getUniqueId());
    }

    void clear() {
        sessions.clear();
    }

    @NotNull Collection<T> values() {
        return sessions.values();
    }
}
