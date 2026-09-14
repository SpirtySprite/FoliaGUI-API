package com.foliagui.gui;

import com.foliagui.FoliaGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class GuiManager {

    private static final Map<UUID, BaseGui> OPEN = new ConcurrentHashMap<>();

    private GuiManager() {
    }

    @ApiStatus.Internal
    public static void register(@NotNull HumanEntity player, @NotNull BaseGui gui) {
        OPEN.put(player.getUniqueId(), gui);
    }

    @ApiStatus.Internal
    public static void unregister(@NotNull HumanEntity player) {
        OPEN.remove(player.getUniqueId());
    }

    public static @Nullable BaseGui getOpenGui(@NotNull HumanEntity player) {
        return OPEN.get(player.getUniqueId());
    }

    public static boolean hasGuiOpen(@NotNull HumanEntity player) {
        return OPEN.containsKey(player.getUniqueId());
    }

    public static boolean hasAnyScreenOpen(@NotNull HumanEntity player) {
        return hasGuiOpen(player) || AnvilGui.hasSession(player) || SignGui.hasSession(player)
                || MerchantGui.hasSession(player) || ChatPrompt.hasSession(player);
    }

    public static int openCount() {
        return OPEN.size();
    }

    public static void refresh(@NotNull BaseGui gui) {
        gui.update();
    }

    public static void closeAll() {
        boolean enabled = FoliaGUI.plugin().isEnabled();
        for (UUID uuid : OPEN.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (enabled) {
                FoliaGUI.scheduler().runForEntity(player, player::closeInventory, null);
                continue;
            }
            try {
                player.closeInventory();
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static @NotNull Collection<BaseGui> openGuis() {
        return OPEN.values();
    }

    public static @NotNull List<Player> viewersOf(@NotNull BaseGui gui) {
        List<Player> viewers = new ArrayList<>();
        for (Map.Entry<UUID, BaseGui> entry : OPEN.entrySet()) {
            if (entry.getValue() == gui) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    viewers.add(player);
                }
            }
        }
        return viewers;
    }

    public static @NotNull List<BaseGui> openGuisOfType(@NotNull Class<? extends BaseGui> type) {
        List<BaseGui> matches = new ArrayList<>();
        for (BaseGui gui : OPEN.values()) {
            if (type.isInstance(gui)) {
                matches.add(gui);
            }
        }
        return matches;
    }

    public static void closeAll(@NotNull Predicate<BaseGui> filter) {
        for (Map.Entry<UUID, BaseGui> entry : OPEN.entrySet()) {
            if (!filter.test(entry.getValue())) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                FoliaGUI.scheduler().runForEntity(player, player::closeInventory, null);
            }
        }
    }

    public static void clearAll() {
        OPEN.clear();
    }
}
