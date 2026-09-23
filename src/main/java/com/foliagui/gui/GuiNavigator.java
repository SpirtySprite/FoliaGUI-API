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
    private static volatile int maxDepth = 32;

    private GuiNavigator() {
    }

    public static void open(@NotNull HumanEntity player, @NotNull BaseGui next) {
        BaseGui current = GuiManager.getOpenGui(player);
        if (current != null && current != next) {
            HISTORY.compute(player.getUniqueId(), (key, stack) -> {
                Deque<BaseGui> history = stack == null ? new ArrayDeque<>() : stack;
                history.remove(next);
                history.remove(current);
                history.push(current);
                while (history.size() > maxDepth) {
                    history.removeLast();
                }
                return history;
            });
        }
        next.open(player);
    }

    public static void maxDepth(int depth) {
        maxDepth = Math.max(1, depth);
    }

    public static int depth(@NotNull HumanEntity player) {
        Deque<BaseGui> stack = HISTORY.get(player.getUniqueId());
        return stack == null ? 0 : stack.size();
    }

    public static void backOrClose(@NotNull HumanEntity player) {
        if (!back(player)) {
            player.closeInventory();
        }
    }

    public static boolean back(@NotNull HumanEntity player) {
        BaseGui[] previous = {null};
        HISTORY.computeIfPresent(player.getUniqueId(), (key, stack) -> {
            previous[0] = stack.poll();
            return stack.isEmpty() ? null : stack;
        });
        if (previous[0] == null) {
            return false;
        }
        previous[0].open(player);
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
