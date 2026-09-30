package com.foliagui.gui;

import com.foliagui.FoliaGUIService;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Back-navigation history between GUIs, for one {@code FoliaGUIService}. */
public final class GuiNavigation {

    private final Map<UUID, Deque<BaseGui>> history = new ConcurrentHashMap<>();
    private final FoliaGUIService service;
    private volatile int maxDepth = 32;

    @ApiStatus.Internal
    public GuiNavigation(@NotNull FoliaGUIService service) {
        this.service = service;
    }

    public void open(@NotNull HumanEntity player, @NotNull BaseGui next) {
        BaseGui current = service.guis().getOpenGui(player);
        if (current != null && current != next) {
            history.compute(player.getUniqueId(), (key, stack) -> {
                Deque<BaseGui> entries = stack == null ? new ArrayDeque<>() : stack;
                entries.remove(next);
                entries.remove(current);
                entries.push(current);
                while (entries.size() > maxDepth) {
                    entries.removeLast();
                }
                return entries;
            });
        }
        next.open(player);
    }

    public void maxDepth(int depth) {
        maxDepth = Math.max(1, depth);
    }

    public int depth(@NotNull HumanEntity player) {
        Deque<BaseGui> stack = history.get(player.getUniqueId());
        return stack == null ? 0 : stack.size();
    }

    public void backOrClose(@NotNull HumanEntity player) {
        if (!back(player)) {
            player.closeInventory();
        }
    }

    public boolean back(@NotNull HumanEntity player) {
        BaseGui[] previous = {null};
        history.computeIfPresent(player.getUniqueId(), (key, stack) -> {
            previous[0] = stack.poll();
            return stack.isEmpty() ? null : stack;
        });
        if (previous[0] == null) {
            return false;
        }
        previous[0].open(player);
        return true;
    }

    public boolean hasHistory(@NotNull HumanEntity player) {
        Deque<BaseGui> stack = history.get(player.getUniqueId());
        return stack != null && !stack.isEmpty();
    }

    public void clear(@NotNull HumanEntity player) {
        history.remove(player.getUniqueId());
    }

    @ApiStatus.Internal
    public void clearAll() {
        history.clear();
    }
}
