package com.foliagui.gui;

import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiNavigator {

    private static final Map<UUID, Deque<BaseGui>> HISTORY = new ConcurrentHashMap<>();

    private GuiNavigator() {
    }

    public static void open(@NotNull HumanEntity player, @NotNull BaseGui next) {
        BaseGui current = GuiManager.getOpenGui(player);
        if (current != null && current != next) {
            HISTORY.computeIfAbsent(player.getUniqueId(), key -> new ArrayDeque<>()).push(current);
        }
        next.open(player);
    }

    public static boolean back(@NotNull HumanEntity player) {
        Deque<BaseGui> stack = HISTORY.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        BaseGui previous = stack.pop();
        previous.open(player);
        return true;
    }

    public static boolean hasHistory(@NotNull HumanEntity player) {
        Deque<BaseGui> stack = HISTORY.get(player.getUniqueId());
        return stack != null && !stack.isEmpty();
    }

    public static void clear(@NotNull HumanEntity player) {
        HISTORY.remove(player.getUniqueId());
    }

    public static void clearAll() {
        HISTORY.clear();
    }
}
